package be.moncoachsport

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageTest {
    @Test
    fun guideVariantBackupKeepsItsSeries() {
        val session =
            Training(
                "guide-variant",
                "A",
                1000,
                program = Programs.CYCLE,
                alternatives = mapOf("F6" to "F19"),
                sets = listOf(LoggedSet("F19", 0, 10.0, 12, 3, "Poulie", "Corde")),
            )
        assertEquals(session, Codec.decode(Codec.encode(session)))
        assertEquals(session, HistoryCsv.decode(HistoryCsv.encode(listOf(session))).single())
    }

    @Test
    fun roomPreservesAnInterruptedSession() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SportDatabase::class.java).build()
        try {
            val session =
                Training(
                    "test",
                    "A",
                    1000L,
                    sets = listOf(LoggedSet("F1", 0, 20.0, 12, 2, "Presse", "Siège 3")),
                )
            db.sessions().put(SessionRecord(session.id, Codec.encode(session)))
            assertEquals(session, Codec.decode(db.sessions().observe().first().single().payload))
        } finally {
            db.close()
        }
    }

    @Test
    fun corruptedBackupIsRejected() {
        try {
            Codec.decode("{\"id\":\"bad\"}")
            fail("Devrait être refusé")
        } catch (_: org.json.JSONException) {}
    }

    @Test
    fun jsonRoundTripPreservesDraftsAndSettings() {
        val session =
            Training(
                "draft",
                "B",
                2000L,
                sets = listOf(LoggedSet("F9", 0, 32.5, 12, 3, "Rowing 2", "Siège 4")),
                drafts = mapOf("notes" to "Réglage à revoir", "F9-1-kg" to "35,5"),
            )
        assertEquals(session, Codec.decode(Codec.encode(session)))
    }

    @Test
    fun invalidRestoreDoesNotEraseExistingData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SportRepository(context)
        repository.erase()
        repository.save(Training("existing", "A", 2000L))
        try {
            repository.restore("{\"version\":99,\"sessions\":[]}", setOf("F1"))
            fail("Version non reconnue")
        } catch (_: IllegalArgumentException) {}
        assertEquals("existing", repository.sessions.first().single().id)
        repository.erase()
        repository.close()
    }

    @Test
    fun migrationFromVersionOneKeepsSessionPayload() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "sport-migration-test.db"
        context.deleteDatabase(name)
        val session =
            Training(
                "legacy",
                "B",
                1000L,
                sets = listOf(LoggedSet("F1", 0, 40.0, 12, 2, "Presse", "Siège 3")),
            )
        context.openOrCreateDatabase(name, 0, null).use { old ->
            old.execSQL(
                "CREATE TABLE sessions (id TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)"
            )
            old.execSQL(
                "INSERT INTO sessions (id,payload) VALUES (?,?)",
                arrayOf(session.id, Codec.encode(session)),
            )
            old.version = 1
        }
        val migrated =
            Room.databaseBuilder(context, SportDatabase::class.java, name)
                .addMigrations(SportDatabase.MIGRATION_1_2)
                .build()
        try {
            assertEquals(
                session,
                Codec.decode(migrated.sessions().observe().first().single().payload),
            )
            assertTrue(migrated.reviews().observe().first().isEmpty())
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun oldJsonDefaultsToOriginalProgram() {
        val j = org.json.JSONObject(Codec.encode(Training("v1", "A", 1000)))
        listOf("program", "week", "skipped", "extraSets", "alternatives").forEach { j.remove(it) }
        assertEquals(Programs.LEGACY, Codec.decode(j.toString()).program)
    }

    @Test
    fun csvRoundTripKeepsQuotesUnitsAndDrafts() {
        val t =
            Training(
                "csv",
                "A",
                1000L,
                program = Programs.CYCLE,
                sets =
                    listOf(
                        LoggedSet(
                            "F1",
                            0,
                            20.5,
                            15,
                            3,
                            "=Presse;\"1\"",
                            "Siège\n3",
                            done = false,
                            loadUnit = "par côté",
                        )
                    ),
                drafts = mapOf("notes" to "Demain ; repos"),
            )
        assertEquals(listOf(t), HistoryCsv.decode(HistoryCsv.encode(listOf(t))))
    }

    @Test
    fun fullBackupRestoresReviewsAndPreferences() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = SportRepository(context)
        repo.eraseAll()
        val t =
            Training(
                "backup",
                "A",
                1000,
                program = Programs.CYCLE,
                week = 5,
                extraSets = setOf("F1"),
            )
        val backup =
            HistoryCsv.asBackup(listOf(t))
                .put(
                    "preferences",
                    org.json
                        .JSONObject()
                        .put("program", Programs.CYCLE)
                        .put("cycleStart", 1000)
                        .put("dark", true),
                )
                .put(
                    "reviews",
                    org.json
                        .JSONArray()
                        .put(
                            org.json
                                .JSONObject()
                                .put("cycleStart", 1000)
                                .put("week", 1)
                                .put("recovery", 6)
                                .put("notes", "Moins d’options")
                                .put("updated", 2000)
                        ),
                )
        repo.restore(backup.toString(), (1..19).map { "F$it" }.toSet())
        assertEquals(t, repo.sessions.first().single())
        assertEquals(6, repo.reviews.first().single().recovery)
        assertTrue(repo.dark.first())
        assertEquals(1000L, repo.cycleStart.first())
        repo.eraseAll()
        repo.close()
    }

    @Test
    fun invalidOptionInBackupIsRejected() {
        val raw =
            Codec.encode(
                Training("bad-option", "A", 1000, program = Programs.CYCLE, skipped = setOf("F1"))
            )
        try {
            Codec.decode(raw)
            fail("Mouvement obligatoire sauté")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun reminderReceiverPostsALocalNotification() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduler = RestScheduler(context)
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        scheduler.update(System.currentTimeMillis() + 60000, true)
        try {
            RestReceiver().onReceive(context, android.content.Intent())
            repeat(30) {
                if (manager.activeNotifications.none { it.id == 42 }) kotlinx.coroutines.delay(50)
            }
            assertTrue(manager.activeNotifications.any { it.id == 42 })
        } finally {
            scheduler.update(0, false)
            manager.cancel(42)
        }
    }
}
