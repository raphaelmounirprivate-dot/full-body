package be.moncoachsport

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MaintenanceTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val ids = Programs.ids.associateWith { (1..19).map { "F$it" }.toSet() }

    @Test
    fun cacheRevalidatesOnlyChangedRowsAndPreservesRawFailures() {
        var calls = 0
        val decoder = SessionRowDecoder {
            calls++
            Codec.decode(it)
        }
        val good = SessionRecord("a", Codec.encode(Training("a", "A", 1000)))
        val bad = SessionRecord("bad", "{broken\n\"future-data\"")
        val rows = listOf(good, bad)
        val first = decoder.readAll(rows)
        assertEquals(listOf("a"), first.sessions.map { it.id })
        assertEquals(bad.payload, first.unreadable.single().payload)
        val typed = decoder.readAll(rows, listOf(DraftRecord("a", "F1-0-kg", "35")))
        assertEquals("35", typed.sessions.single().drafts["F1-0-kg"])
        decoder.readAll(rows)
        assertEquals(2, calls)
        decoder.readAll(
            listOf(
                good.copy(
                    payload = Codec.encode(Training("a", "A", 1000, notes = "Nouvelle note"))
                ),
                bad,
            )
        )
        assertEquals(3, calls)
    }

    @Test
    fun viewModelStartsAndExportsRawRowsDespiteCorruptSession(): Unit = runBlocking {
        val seed = SportRepository(context)
        seed.eraseAll()
        seed.close()
        val raw = "{future-version\nnot-readable"
        val db =
            Room.databaseBuilder(context, SportDatabase::class.java, "sport.db")
                .addMigrations(SportDatabase.MIGRATION_1_2, SportDatabase.MIGRATION_2_3, SportDatabase.MIGRATION_3_4)
                .build()
        db.sessions().put(SessionRecord("bad", raw))
        db.sessions().put(SessionRecord("good", Codec.encode(Training("good", "A", 1000))))
        db.close()
        val store = ViewModelStore()
        try {
            val vm =
                ViewModelProvider(
                    store,
                    ViewModelProvider.AndroidViewModelFactory(
                        context.applicationContext as Application
                    ),
                )[CoachViewModel::class.java]
            withTimeout(10000) { vm.unreadable.first { it.isNotEmpty() } }
            withTimeout(10000) { vm.sessions.first { it.any { t -> t.id == "good" } } }
            val exported = JSONObject(vm.exportJson())
            assertEquals(4, exported.getInt("version")) // Les anciens APK refusent ce fichier de récupération.
            val rows = exported.getJSONObject("recovery").getJSONArray("rows")
            assertTrue(
                (0 until rows.length()).any { rows.getJSONObject(it).getString("payload") == raw }
            )
            assertEquals("good", exported.getJSONArray("sessions").getJSONObject(0).getString("id"))
            vm.start("B")
            withTimeout(10000) {
                vm.sessions.first { it.any { t -> t.isActive && t.template == "B" } }
            }
            assertEquals(1, vm.unreadable.value.size)
            vm.eraseAll()
            withTimeout(10000) {
                vm.message.first { it == "Toutes les données et préférences ont été supprimées" }
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        }
    }

    @Test
    fun migrationTwoToThreeKeepsPayloadAndWritesDraftsSeparately() = runBlocking {
        val name = "audit-migration-v2.db"
        context.deleteDatabase(name)
        val original = Training("old", "A", 1000, drafts = mapOf("F1-0-kg" to "20"))
        val payload = Codec.encode(original)
        context.openOrCreateDatabase(name, 0, null).use {
            it.execSQL(
                "CREATE TABLE sessions (id TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)"
            )
            it.execSQL(
                "CREATE TABLE weekly_reviews (cycleStart INTEGER NOT NULL, week INTEGER NOT NULL, recovery INTEGER NOT NULL, notes TEXT NOT NULL, updated INTEGER NOT NULL, PRIMARY KEY(cycleStart,week))"
            )
            it.execSQL("INSERT INTO sessions VALUES (?, ?)", arrayOf(original.id, payload))
            it.version = 2
        }
        val db =
            Room.databaseBuilder(context, SportDatabase::class.java, name)
                .addMigrations(SportDatabase.MIGRATION_2_3, SportDatabase.MIGRATION_3_4)
                .build()
        val repo = SportRepository(context, db)
        try {
            assertEquals(original, repo.currentSessions().single())
            repo.saveDraft("old", "F1-0-kg", "25")
            repo.saveDraft("old", "F1-0-kg", "30")
            assertEquals(payload, db.sessions().read().single().payload)
            assertEquals(1, db.drafts().read().size)
            assertEquals("30", repo.currentSessions().single().drafts["F1-0-kg"])
            repo.activate(Training("b", "B", 2000))
            repo.activate(original)
            assertEquals("30", repo.currentSessions().single { it.isActive }.drafts["F1-0-kg"])
        } finally {
            repo.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun restoreValidatesAgainstEachProgramsOwnCatalogue() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, SportDatabase::class.java).build()
        val repo = SportRepository(context, db)
        val legacy =
            Training("old", "A", 1000, sets = listOf(LoggedSet("F1", 0, 20.0, 10, 2, "", "")))
        try {
            repo.save(legacy)
            try {
                repo.restore(
                    HistoryCsv.asBackup(listOf(legacy)).toString(),
                    mapOf(Programs.LEGACY to setOf("F2"), Programs.CYCLE to setOf("F1")),
                )
                fail("Le catalogue d'un autre programme ne doit pas autoriser l'exercice")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.contains("catalogue"))
            }
            assertEquals(listOf(legacy), repo.currentSessions())
            repo.restore(HistoryCsv.asBackup(listOf(legacy)).toString(), ids)
            assertEquals(listOf(legacy), repo.currentSessions())
        } finally {
            repo.close()
        }
    }

    @Test
    fun csvEscapesControlPrefixesAndRestoresExactValues() {
        listOf("\t=1", "\r@A", "\tnormal", "\rnormal", "\n=2", "'literal", "  +SUM(A1)").forEach {
            value ->
            val t =
                Training(
                    "csv",
                    "A",
                    1000,
                    sets = listOf(LoggedSet("F1", 0, 20.0, 10, 2, value, value)),
                )
            val encoded = HistoryCsv.encode(listOf(t))
            assertTrue(HistoryCsv.parse(encoded)[1][12].startsWith("'"))
            assertEquals(listOf(t), HistoryCsv.decode(encoded))
        }
    }

    @Test
    fun invalidImportsExplainErrorsInFrenchBeforeReplacingData() = runBlocking {
        val repo =
            SportRepository(
                context,
                Room.inMemoryDatabaseBuilder(context, SportDatabase::class.java).build(),
            )
        val existing = Training("keep", "A", 1000)
        repo.save(existing)
        try {
            val invalid =
                listOf(
                    "{",
                    "{\"version\":3}",
                    HistoryCsv.asBackup(listOf(existing.copy(week = 99))).toString(),
                    HistoryCsv.asBackup(listOf(existing, existing)).toString(),
                    HistoryCsv.asBackup(listOf(existing)).put("recovery", JSONObject()).toString(),
                HistoryCsv.asBackup(listOf(existing.copy(id = "incoming"))).put("preferences", JSONObject().put("dark", "invalide")).toString(),
                )
            invalid.forEach { raw ->
                try {
                    repo.restore(raw, ids)
                    fail("Import invalide accepté")
                } catch (e: IllegalArgumentException) {
                    assertFalse(e.message!!.contains("Failed requirement"))
                    assertTrue(listOf("sauvegarde", "programme", "identifiant", "séance").any { e.message!!.lowercase().contains(it) })
                }
                assertEquals(listOf(existing), repo.currentSessions())
            }
        } finally {
            repo.close()
        }
    }
}
