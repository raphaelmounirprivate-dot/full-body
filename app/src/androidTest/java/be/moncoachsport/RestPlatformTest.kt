package be.moncoachsport

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.*
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class RestPlatformTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private suspend fun waitNotification(id: Int) = withTimeout(5000) {
        while (manager.activeNotifications.none { it.id==id }) delay(25)
        manager.activeNotifications.first { it.id==id }.notification
    }
    @Test fun countdownUsesNativeChronometerAndIsCanceledDuringPause() = runBlocking {
        val scheduler = RestScheduler(context)
        val end = System.currentTimeMillis()+60000
        try {
            scheduler.update(end,true)
            val notification = waitNotification(44)
            assertEquals(end,notification.`when`)
            assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
            assertEquals(Notification.VISIBILITY_PUBLIC,notification.visibility)
            assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertEquals(NotificationManager.IMPORTANCE_LOW,manager.getNotificationChannel("rest_countdown").importance)
            scheduler.update(0,false)
            withTimeout(5000) { while(manager.activeNotifications.any { it.id==44 }) delay(25) }
        } finally { scheduler.update(0,false) }
        Unit
    }
    @Test fun timerAdjustmentsPersistAndAnOldCompletionCannotClearANewRest() = runBlocking {
        val repository = SportRepository(context)
        val previous = repository.timer.first()
        try {
            val end = System.currentTimeMillis()+120000
            repository.setTimer(end,0)
            assertEquals(end+30000,repository.adjustTimer(30).first)
            assertEquals(end+15000,repository.adjustTimer(-15).first)
            assertFalse(repository.dismissExpiredTimer(end-200000))
            assertEquals(end+15000,repository.timer.first().first)
            repository.setTimer(0,20000)
            assertEquals(0L to 50000L,repository.adjustTimer(30))
            val expired = System.currentTimeMillis()-1
            repository.setTimer(expired,0)
            assertEquals(expired to 0L,repository.adjustTimer(30))
            assertTrue(repository.dismissExpiredTimer(expired))
            assertEquals(0L to 0L,repository.timer.first())
        } finally { repository.setTimer(previous.first,previous.second); repository.close() }
        Unit
    }
    @Test fun completionRemovesCountdownAndNeverNotifiesTwice() = runBlocking {
        val scheduler = RestScheduler(context)
        val end = System.currentTimeMillis()+1000
        try {
            scheduler.update(end,true)
            waitNotification(44)
            delay((end-System.currentTimeMillis()+50).coerceAtLeast(0))
            scheduler.finishInApp(end,true)
            waitNotification(42)
            withTimeout(5000) { while(manager.activeNotifications.any { it.id==44 }) delay(25) }
            manager.cancel(42)
            withTimeout(5000) { while(manager.activeNotifications.any { it.id==42 }) delay(25) }
            scheduler.finishInApp(end,true)
            delay(100)
            assertTrue(manager.activeNotifications.none { it.id==42 })
        } finally { scheduler.update(0,false) }
        Unit
    }
    @Test fun widgetProviderIsRegisteredAndRemoteViewsRenderGoalAndWorkout() {
        val component = ComponentName(context,WorkoutWidgetProvider::class.java)
        val info = AppWidgetManager.getInstance(context).installedProviders.firstOrNull { it.provider==component }
        assertNotNull(info)
        assertEquals(1800000,info!!.updatePeriodMillis)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val model = WorkoutWidgetModel("Prochaine séance B","Cycle de 8 semaines","Cette semaine : 2 / 3 séances",2,3)
            val view = WorkoutWidget.views(context,model).apply(context,FrameLayout(context))
            assertEquals(model.title,view.findViewById<TextView>(R.id.widget_workout).text.toString())
            assertEquals(model.goal,view.findViewById<TextView>(R.id.widget_goal).text.toString())
            assertEquals(2,view.findViewById<ProgressBar>(R.id.widget_progress).progress)
            assertTrue(view.findViewById<Button>(R.id.widget_refresh).hasOnClickListeners())
            assertTrue(view.hasOnClickListeners())
        }
    }
}
