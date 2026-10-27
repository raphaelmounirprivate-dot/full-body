package be.moncoachsport

import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.widget.RemoteViews
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

data class WorkoutWidgetModel(val title: String, val program: String, val goal: String,
    val completed: Int, val target: Int) {
    companion object {
        fun from(sessions: List<Training>, program: String, target: Int, incomplete: Boolean = false,
            today: LocalDate = LocalDate.now()): WorkoutWidgetModel {
            val active = sessions.firstOrNull { it.isActive }
            val next = Programs.next(program,sessions)
            val pending = sessions.firstOrNull { it.program == program && it.template == next && it.paused }
            val count = FeatureRules.weeklyCount(sessions,today)
            return WorkoutWidgetModel(
                if (incomplete) "Voir mes séances"
                else if (active != null) "Reprendre la séance ${active.template}"
                else if (pending != null) "Reprendre la séance $next"
                else "Prochaine séance $next",
                Programs.title(active?.program ?: program),
                if (incomplete) "Historique incomplet · au moins $count séances cette semaine"
                else "Cette semaine : $count / $target séances",
                count,target)
        }
        val unavailable = WorkoutWidgetModel("Ouvrir mes séances","Mon Coach Sport","Données indisponibles · actualiser",0,3)
    }
}

object WorkoutWidget {
    const val REFRESH = "be.moncoachsport.REFRESH_WIDGET"
    private val mutex = Mutex()
    fun requestUpdate(context: Context) {
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context,WorkoutWidgetProvider::class.java))
        if (ids.isNotEmpty()) context.sendBroadcast(Intent(context,WorkoutWidgetProvider::class.java).setAction(REFRESH))
    }
    suspend fun refresh(context: Context) = mutex.withLock {
        val repository = SportRepository(context)
        try {
            val snapshot = repository.snapshotNow()
            publish(context,WorkoutWidgetModel.from(snapshot.sessions,repository.program.first(),
                repository.personal.first().weeklyGoal,snapshot.unreadable.isNotEmpty()))
        } finally { repository.close() }
    }
    fun views(context: Context, model: WorkoutWidgetModel): RemoteViews =
        RemoteViews(context.packageName,R.layout.workout_widget).apply {
            setTextViewText(R.id.widget_workout,model.title)
            setTextViewText(R.id.widget_program,model.program)
            setTextViewText(R.id.widget_goal,model.goal)
            setProgressBar(R.id.widget_progress,model.target,model.completed.coerceAtMost(model.target),false)
            setContentDescription(R.id.widget_progress,model.goal)
            setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(context,80,
                Intent(context,MainActivity::class.java).putExtra("openWorkout",true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(context,81,
                Intent(context,WorkoutWidgetProvider::class.java).setAction(REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
    fun publish(context: Context, model: WorkoutWidgetModel) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context,WorkoutWidgetProvider::class.java))
        if (ids.isNotEmpty()) manager.updateAppWidget(ids,views(context,model))
    }
}

class WorkoutWidgetProvider : AppWidgetProvider() {
    private fun refresh(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { withTimeout(8000) { WorkoutWidget.refresh(context) } }
            catch (_: Exception) { WorkoutWidget.publish(context,WorkoutWidgetModel.unavailable) }
            finally { pending?.finish() }
        }
    }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context,intent)
        if (intent.action in setOf(WorkoutWidget.REFRESH,Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_DATE_CHANGED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED))
            refresh(context)
    }
}
