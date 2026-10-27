package be.moncoachsport

import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.*
import org.junit.Assert.*
import org.junit.Test

class PersonalFeaturesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val ids = Programs.ids.associateWith { (1..19).map { "F$it" }.toSet() }
    @Test fun measurementsOptionsAndVideoRoundTripAndBadImportKeepsEverything() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, SportDatabase::class.java).build()
        val repo = SportRepository(context, db)
        val previous = repo.personal.first()
        try {
            val options = PersonalOptions(guided=true, level="Confirmé", homeOrder=FeatureRules.homeCards.reversed(),
                hidden=setOf("last"), challenges=true, videos=mapOf("cycle:F17" to "https://youtu.be/qIUio2uC2lw"))
            val m = BodyMeasurement("2026-10-20",72.5,18.0)
            repo.setPersonal(options)
            repo.saveMeasurement(m)
            repo.saveMeasurement(m.copy(kg=73.0))
            assertEquals(1, repo.readMeasurements().size)
            val backup = HistoryCsv.asBackup(listOf(Training("saved","A",1000)))
                .put("version",5).put("personal",PersonalCodec.encode(options))
                .put("measurements",PersonalCodec.measurements(listOf(m)))
            repo.restore(backup.toString(), ids)
            assertEquals(m, repo.readMeasurements().single())
            assertEquals(options, repo.personal.first())
            val bad = JSONObject(backup.toString()).put("measurements",JSONArray().put(JSONObject().put("day","bad").put("kg",-1).put("fat",JSONObject.NULL)))
            assertTrue(runCatching { repo.restore(bad.toString(), ids) }.isFailure)
            assertEquals(m, repo.readMeasurements().single())
            assertEquals("saved",repo.currentSessions().single().id)
            assertEquals(options,repo.personal.first())
            repo.restore(HistoryCsv.asBackup(emptyList()).toString(),ids)
            assertEquals(m,repo.readMeasurements().single()) // An old backup has no body data to replace.
            repo.deleteMeasurement(m.day)
            assertTrue(repo.readMeasurements().isEmpty())
            repo.saveMeasurement(m)
            repo.eraseAll()
            assertTrue(repo.readMeasurements().isEmpty())
        } finally { repo.setPersonal(previous); repo.close() }
        Unit
    }
    @Test fun migrationThreeToFourPreservesSessionsAndDrafts() = runBlocking {
        val name = "personal-migration.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name,0,null).use {
            it.execSQL("CREATE TABLE sessions (id TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
            it.execSQL("CREATE TABLE weekly_reviews (cycleStart INTEGER NOT NULL, week INTEGER NOT NULL, recovery INTEGER NOT NULL, notes TEXT NOT NULL, updated INTEGER NOT NULL, PRIMARY KEY(cycleStart,week))")
            it.execSQL("CREATE TABLE session_drafts (sessionId TEXT NOT NULL, field TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY(sessionId,field))")
            it.execSQL("INSERT INTO sessions VALUES (?,?)", arrayOf("old",Codec.encode(Training("old","A",1000))))
            it.execSQL("INSERT INTO session_drafts VALUES ('old','F1-0-kg','25')")
            it.version = 3
        }
        val db = Room.databaseBuilder(context,SportDatabase::class.java,name).addMigrations(SportDatabase.MIGRATION_3_4).build()
        val repo = SportRepository(context,db)
        try {
            assertEquals("25",repo.currentSessions().single().drafts["F1-0-kg"])
            repo.saveMeasurement(BodyMeasurement("2026-10-20",70.0,null))
            assertEquals(70.0,repo.readMeasurements().single().kg!!,0.0)
        } finally { repo.close(); context.deleteDatabase(name) }
    }
    @Test fun staleCanceledAndDuplicateRestBroadcastsAreIgnored() = runBlocking {
        val scheduler = RestScheduler(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val end = System.currentTimeMillis()-100
        scheduler.update(end,true)
        scheduler.deliver(42,end-1)
        assertTrue(manager.activeNotifications.none { it.id==42 })
        scheduler.update(0,false)
        scheduler.deliver(42,end)
        assertTrue(manager.activeNotifications.none { it.id==42 })
        scheduler.update(end,true)
        scheduler.deliver(42,end)
        withTimeout(5000) { while (manager.activeNotifications.none { it.id==42 }) delay(25) }
        assertTrue(manager.activeNotifications.any { it.id==42 })
        manager.cancel(42)
        withTimeout(5000) { while (manager.activeNotifications.any { it.id==42 }) delay(25) }
        scheduler.update(end,true) // Reopening the app must not re-enable an already delivered deadline.
        scheduler.deliver(42,end)
        delay(100)
        assertTrue(manager.activeNotifications.none { it.id==42 })
        scheduler.update(0,false)
    }
    @Test fun realAlarmFiresWithScreenOffAndIdle() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun shell(command: String) {
            instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
                java.io.FileInputStream(fd.fileDescriptor).use { it.readBytes() }
            }
        }
        val scheduler = RestScheduler(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        org.junit.Assume.assumeTrue("Autorisation de rappel précis nécessaire à cet essai", scheduler.preciseAllowed())
        val repo = SportRepository(context)
        val previousAlerts = repo.alerts.first()
        val previousTimer = repo.timer.first()
        try {
            repo.setAlerts(true)
            shell("dumpsys battery unplug")
            shell("input keyevent 223")
            repeat(2) {
                val end = System.currentTimeMillis()+4000
                repo.setTimer(end,0)
                scheduler.update(end,true)
                withTimeout(3000) { while(manager.activeNotifications.any { it.id==42 }) delay(25) }
                shell("dumpsys deviceidle force-idle")
                assertFalse(context.getSystemService(android.os.PowerManager::class.java).isInteractive)
                withTimeout(25000) {
                    while (manager.activeNotifications.none { it.id==42 }) delay(100)
                }
                assertTrue(System.currentTimeMillis()>=end)
            }
        } finally {
            shell("dumpsys deviceidle unforce")
            shell("dumpsys battery reset")
            shell("input keyevent 224")
            shell("wm dismiss-keyguard")
            scheduler.update(0,false)
            repo.setAlerts(previousAlerts)
            repo.setTimer(previousTimer.first,previousTimer.second)
            repo.close()
        }
    }
}
