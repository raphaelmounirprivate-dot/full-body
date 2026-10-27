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
    private suspend fun SportRepository.restore(raw: String, ids: Set<String>) =
        restore(raw, Programs.ids.associateWith { ids })

    @Test
    fun switchingSessionsKeepsAllDataAndOneActiveSession() = runBlocking {
        val repo = SportRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        repo.erase()
        try {
            val a =
                Training(
                    "paused-a",
                    "A",
                    1000,
                    program = Programs.CYCLE,
                    week = 5,
                    extraSets = setOf("F1"),
                    skipped = setOf("F5"),
                    alternatives = mapOf("F6" to "F19"),
                    sets =
                        listOf(
                            LoggedSet(
                                "F1",
                                0,
                                20.0,
                                12,
                                3,
                                "Presse",
                                "Siège 4",
                                loadUnit = "par côté",
                            )
                        ),
                    drafts = mapOf("F1-1-kg" to "22,5", "notes" to "Reprendre demain"),
                )
            val b = Training("active-b", "B", 2000, program = Programs.CYCLE, week = 6)
            repo.activate(a)
            repo.activate(b)
            val switched = repo.sessions.first()
            assertEquals(a.copy(paused = true), switched.first { it.id == a.id })
            assertEquals(b, switched.single { it.isActive })
            repo.activate(switched.first { it.id == a.id })
            repo.activate(a)
            assertEquals(a, repo.sessions.first().single { it.isActive })
            assertEquals(b.copy(paused = true), repo.sessions.first().first { it.id == b.id })
            assertEquals(2, repo.sessions.first().size)
            try {
                repo.activate(a.copy(id = "duplicate"))
                fail("Duplicate pending session accepted")
            } catch (_: IllegalArgumentException) {}
            assertEquals(a, repo.sessions.first().single { it.isActive })
        } finally {
            repo.erase()
            repo.close()
        }
    }

    @Test
    fun pausedSessionsSurviveJsonAndCsvRestore() = runBlocking {
        val repo = SportRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        repo.erase()
        try {
            val sessions =
                listOf(
                    Training(
                        "a",
                        "A",
                        1000,
                        program = Programs.CYCLE,
                        week = 2,
                        paused = true,
                        drafts = mapOf("F1-0-kg" to "25"),
                    ),
                    Training("b", "B", 2000, program = Programs.CYCLE, week = 3),
                )
            val decoded = HistoryCsv.decode(HistoryCsv.encode(sessions))
            assertEquals(sessions, decoded)
            repo.restore(HistoryCsv.asBackup(decoded).toString(), (1..19).map { "F$it" }.toSet())
            assertEquals(sessions, repo.sessions.first())
            repo.activate(repo.sessions.first().first())
            assertEquals("a", repo.sessions.first().single { it.isActive }.id)
            assertEquals(2, repo.sessions.first().single { it.isActive }.week)
        } finally {
            repo.erase()
            repo.close()
        }
    }

    @Test
    fun invalidPausedBackupsAreRejectedBeforeReplacingData() = runBlocking {
        val repo = SportRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        repo.erase()
        val existing = Training("keep", "A", 1000)
        repo.save(existing)
        try {
            val a = Training("a", "A", 1000)
            val invalid =
                listOf(
                    HistoryCsv.asBackup(listOf(a, a.copy(id = "b", template = "B"))),
                    HistoryCsv.asBackup(listOf(a, a.copy(id = "dup", paused = true))),
                    HistoryCsv.asBackup(listOf(a.copy(paused = true, finished = 2000))),
                    HistoryCsv.asBackup(listOf(a.copy(paused = true))).put("version", 2),
                )
            invalid.forEach { backup ->
                try {
                    repo.restore(backup.toString(), setOf("F1"))
                    fail("Invalid backup accepted")
                } catch (_: IllegalArgumentException) {}
                assertEquals(listOf(existing), repo.sessions.first())
            }
        } finally {
            repo.erase()
            repo.close()
        }
    }

    @Test
    fun previousJsonAndCsvFormatsRemainImportable() = runBlocking {
        val repo = SportRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        repo.erase()
        try {
            val t = Training("old", "A", 1000)
            val json = org.json.JSONObject(Codec.encode(t)).apply { remove("paused") }
            val backup =
                org.json
                    .JSONObject()
                    .put("version", 2)
                    .put("sessions", org.json.JSONArray().put(json))
            repo.restore(backup.toString(), setOf("F1"))
            assertEquals(listOf(t), repo.sessions.first())
            val csv =
                HistoryCsv.encode(listOf(t))
                    .split("\r\n")
                    .mapIndexed { i, line ->
                        if (i == 0) line else line.replaceFirst("\"3\";", "\"2\";")
                    }
                    .joinToString("\r\n")
            assertEquals(listOf(t), HistoryCsv.decode(csv))
        } finally {
            repo.erase()
            repo.close()
        }
    }

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
        } catch (_: IllegalArgumentException) {}
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
                .addMigrations(SportDatabase.MIGRATION_1_2, SportDatabase.MIGRATION_2_3, SportDatabase.MIGRATION_3_4)
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
        listOf("program", "week", "skipped", "extraSets", "alternatives", "paused").forEach {
            j.remove(it)
        }
        assertEquals(Programs.LEGACY, Codec.decode(j.toString()).program)
        assertTrue(Codec.decode(j.toString()).isActive)
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
        val end = System.currentTimeMillis() - 1
        val repo = SportRepository(context)
        val previousAlerts = repo.alerts.first()
        val previousTimer = repo.timer.first()
        repo.setAlerts(true)
        repo.setTimer(end,0)
        scheduler.update(end, true)
        try {
            RestReceiver().onReceive(context, android.content.Intent().putExtra("id",42).putExtra("end",end))
            repeat(30) {
                if (manager.activeNotifications.none { it.id == 42 }) kotlinx.coroutines.delay(50)
            }
            assertTrue(manager.activeNotifications.any { it.id == 42 })
        } finally {
            scheduler.update(0, false)
            manager.cancel(42)
            repo.setAlerts(previousAlerts)
            repo.setTimer(previousTimer.first,previousTimer.second)
            repo.close()
        }
    }
}
