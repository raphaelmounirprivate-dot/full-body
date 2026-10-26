package be.moncoachsport

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.flow.*
import org.json.*

fun readBackup(input: InputStream): String {
    val buffer = ByteArray(8192)
    val output = ByteArrayOutputStream()
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        require(output.size() + n <= 5_000_000) { "Fichier trop volumineux" }
        output.write(buffer, 0, n)
    }
    return output.toString("UTF-8")
}

@Entity(tableName = "sessions")
data class SessionRecord(@PrimaryKey val id: String, val payload: String)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions") fun observe(): Flow<List<SessionRecord>>

    @Query("SELECT * FROM sessions") suspend fun read(): List<SessionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(row: SessionRecord)

    @Query("DELETE FROM sessions") suspend fun clear()

    @Transaction
    suspend fun replace(rows: List<SessionRecord>) {
        clear()
        rows.forEach { put(it) }
    }
}

@Entity(tableName = "weekly_reviews", primaryKeys = ["cycleStart", "week"])
data class WeeklyReview(
    val cycleStart: Long,
    val week: Int,
    val recovery: Int,
    val notes: String,
    val updated: Long,
)

@Dao
interface ReviewDao {
    @Query("SELECT * FROM weekly_reviews ORDER BY cycleStart, week")
    fun observe(): Flow<List<WeeklyReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(review: WeeklyReview)

    @Query("DELETE FROM weekly_reviews") suspend fun clear()
}

@Database(entities = [SessionRecord::class, WeeklyReview::class], version = 2, exportSchema = false)
abstract class SportDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao

    abstract fun reviews(): ReviewDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `weekly_reviews` (`cycleStart` INTEGER NOT NULL, `week` INTEGER NOT NULL, `recovery` INTEGER NOT NULL, `notes` TEXT NOT NULL, `updated` INTEGER NOT NULL, PRIMARY KEY(`cycleStart`, `week`))"
                    )
                }
            }
    }
}

private val Context.preferences by preferencesDataStore(name = "preferences")

object Codec {
    fun encode(t: Training): String =
        JSONObject()
            .apply {
                put("id", t.id)
                put("template", t.template)
                put("started", t.started)
                put("finished", t.finished ?: JSONObject.NULL)
                put("paused", t.paused)
                put("notes", t.notes)
                put("cardio", t.cardio)
                put("energy", t.energy)
                put("discomfort", t.discomfort)
                put("cardioMachine", t.cardioMachine)
                put("intensity", t.intensity)
                put("drafts", JSONObject(t.drafts))
                put("program", t.program)
                put("week", t.week)
                put("skipped", JSONArray(t.skipped.toList()))
                put("extraSets", JSONArray(t.extraSets.toList()))
                put("alternatives", JSONObject(t.alternatives))
                put(
                    "sets",
                    JSONArray().apply {
                        t.sets.forEach { s ->
                            put(
                                JSONObject().apply {
                                    put("exerciseId", s.exerciseId)
                                    put("index", s.index)
                                    put("kg", s.kg)
                                    put("reps", s.reps)
                                    put("rir", s.rir)
                                    put("machine", s.machine)
                                    put("setting", s.setting)
                                    put("done", s.done)
                                    put("loadUnit", s.loadUnit)
                                }
                            )
                        }
                    },
                )
            }
            .toString()

    fun decode(raw: String): Training {
        val j = JSONObject(raw)
        val sets =
            j.getJSONArray("sets").let { a ->
                (0 until a.length()).map { i ->
                    a.getJSONObject(i).let { s ->
                        LoggedSet(
                            s.getString("exerciseId"),
                            s.getInt("index"),
                            s.getDouble("kg"),
                            s.getInt("reps"),
                            s.getInt("rir"),
                            s.getString("machine"),
                            s.getString("setting"),
                            s.getBoolean("done"),
                            s.optString("loadUnit", "total"),
                        )
                    }
                }
            }
        require(sets.all(Rules::validate)) { "Série invalide" }
        require(sets.map { it.exerciseId to it.index }.distinct().size == sets.size) {
            "Séries dupliquées"
        }
        val drafts =
            j.optJSONObject("drafts")?.let { d ->
                d.keys().asSequence().associateWith { d.getString(it) }
            } ?: emptyMap()
        require(drafts.size <= 500 && drafts.values.all { it.length <= 10000 })
        fun ids(key: String) =
            j.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                ?: emptySet()
        val alternatives =
            j.optJSONObject("alternatives")?.let { a ->
                a.keys().asSequence().associateWith { a.getString(it) }
            } ?: emptyMap()
        return Training(
                j.getString("id"),
                j.getString("template"),
                j.getLong("started"),
                if (j.isNull("finished")) null else j.getLong("finished"),
                sets,
                j.getString("notes"),
                j.getInt("cardio"),
                j.getInt("energy"),
                j.getString("discomfort"),
                j.optString("cardioMachine", "Vélo"),
                j.optInt("intensity", 3),
                drafts,
                j.optString("program", Programs.LEGACY),
                j.optInt("week", 1),
                ids("skipped"),
                ids("extraSets"),
                alternatives,
                if (j.has("paused")) j.getBoolean("paused") else false,
            )
            .also {
                require(
                    it.template in listOf("A", "B", "C") &&
                        it.id.isNotBlank() &&
                        it.started > 0 &&
                        (it.finished == null || it.finished >= it.started)
                )
                require(it.cardio in 0..600 && it.energy in 0..10 && it.intensity in 0..10)
                require(it.program in Programs.ids && it.week in 1..8)
                require(!it.paused || it.finished == null) {
                    "Une séance terminée ne peut pas être en pause"
                }
                require(
                    it.alternatives.isEmpty() ||
                        (it.program == Programs.CYCLE &&
                            it.alternatives.all { (slot, replacement) ->
                                Programs.variants.any { v ->
                                    v.template == it.template &&
                                        v.slot == slot &&
                                        v.replacement == replacement
                                }
                            })
                ) {
                    "Variante non reconnue"
                }
                if (it.program == Programs.CYCLE) {
                    val plan = Programs.cyclePlan(it)
                    require(plan.map { it.id }.distinct().size == plan.size) {
                        "Remplacement dupliqué"
                    }
                    require(it.skipped.all { id -> plan.any { p -> p.id == id && p.optional } })
                    require(
                        it.extraSets.all { id ->
                            it.week >= 5 && plan.take(3).any { p -> p.id == id }
                        }
                    )
                    require(
                        it.sets.all { s ->
                            val p = plan.firstOrNull { p -> p.id == s.exerciseId }
                            p != null &&
                                s.exerciseId !in it.skipped &&
                                s.index < 2 + if (s.exerciseId in it.extraSets) 1 else 0
                        }
                    )
                } else
                    require(
                        it.skipped.isEmpty() && it.extraSets.isEmpty() && it.alternatives.isEmpty()
                    )
            }
    }
}

class SportRepository(context: Context) {
    private val db =
        Room.databaseBuilder(context.applicationContext, SportDatabase::class.java, "sport.db")
            .addMigrations(SportDatabase.MIGRATION_1_2)
            .build()
    private val prefs = context.applicationContext.preferences
    val sessions =
        db.sessions().observe().map { rows ->
            rows.map { Codec.decode(it.payload) }.sortedBy { it.started }
        }
    val reviews = db.reviews().observe()
    val program = prefs.data.map { it[stringPreferencesKey("program")] ?: Programs.CYCLE }
    val cycleStart = prefs.data.map { it[longPreferencesKey("cycleStart")] ?: 0L }

    suspend fun preferCurrentGuide() {
        prefs.edit { it[stringPreferencesKey("program")] = Programs.CYCLE }
    }

    val autoRest = prefs.data.map { it[booleanPreferencesKey("autoRest")] ?: true }
    val haptics = prefs.data.map { it[booleanPreferencesKey("haptics")] ?: false }

    suspend fun setAutoRest(value: Boolean) {
        prefs.edit { it[booleanPreferencesKey("autoRest")] = value }
    }

    suspend fun setHaptics(value: Boolean) {
        prefs.edit { it[booleanPreferencesKey("haptics")] = value }
    }

    suspend fun setCycleStart(value: Long) {
        require(value > 0)
        prefs.edit { it[longPreferencesKey("cycleStart")] = value }
    }

    suspend fun saveReview(review: WeeklyReview) {
        require(review.week in 1..8 && review.recovery in 0..10 && review.notes.length <= 10000)
        db.reviews().put(review)
    }

    val dark = prefs.data.map { it[booleanPreferencesKey("dark")] ?: false }
    val alerts = prefs.data.map { it[booleanPreferencesKey("alerts")] ?: false }

    suspend fun setAlerts(value: Boolean) {
        prefs.edit { it[booleanPreferencesKey("alerts")] = value }
    }

    val timer =
        prefs.data.map {
            (it[longPreferencesKey("restEnd")] ?: 0L) to
                (it[longPreferencesKey("restPaused")] ?: 0L)
        }

    suspend fun setTimer(end: Long, paused: Long) {
        prefs.edit {
            it[longPreferencesKey("restEnd")] = end
            it[longPreferencesKey("restPaused")] = paused
        }
    }

    suspend fun setDark(value: Boolean) {
        prefs.edit { it[booleanPreferencesKey("dark")] = value }
    }

    suspend fun save(t: Training) = db.sessions().put(SessionRecord(t.id, Codec.encode(t)))

    suspend fun activate(target: Training) {
        require(target.finished == null) { "Cette séance est déjà terminée" }
        Codec.decode(Codec.encode(target))
        db.withTransaction {
            val data = db.sessions().read().map { Codec.decode(it.payload) }
            require(
                data.none {
                    it.id != target.id &&
                        it.finished == null &&
                        it.program == target.program &&
                        it.template == target.template
                }
            ) {
                "Une séance de ce type attend déjà une reprise"
            }
            val next = data.firstOrNull { it.id == target.id } ?: target
            require(next.finished == null)
            data.filter { it.isActive && it.id != next.id }.forEach { save(it.copy(paused = true)) }
            save(next.copy(paused = false))
        }
    }

    suspend fun erase() =
        db.withTransaction {
            db.sessions().clear()
            db.reviews().clear()
        }

    suspend fun eraseAll() {
        erase()
        prefs.edit { it.clear() }
    }

    fun close() = db.close()

    suspend fun restore(raw: String, exercises: Set<String>) {
        require(raw.length <= 5_000_000) { "Sauvegarde trop volumineuse" }
        val j = JSONObject(raw)
        require(j.getInt("version") in 1..3) { "Version inconnue" }
        val a = j.getJSONArray("sessions")
        require(a.length() <= 10000)
        val data = (0 until a.length()).map { Codec.decode(a.getJSONObject(it).toString()) }
        require(data.map { it.id }.distinct().size == data.size && data.count { it.isActive } <= 1)
        require(j.getInt("version") >= 3 || data.none { it.paused }) {
            "Les séances en pause nécessitent une sauvegarde version 3"
        }
        require(
            data.filter { it.finished == null }.map { it.program to it.template }.distinct().size ==
                data.count { it.finished == null }
        ) {
            "Plusieurs séances non terminées du même type"
        }
        require(data.all { t -> t.sets.all { it.exerciseId in exercises } }) { "Exercice inconnu" }
        val reviewArray = j.optJSONArray("reviews") ?: JSONArray()
        require(reviewArray.length() <= 10000)
        val reviews =
            (0 until reviewArray.length()).map { i ->
                reviewArray.getJSONObject(i).let { r ->
                    WeeklyReview(
                        r.getLong("cycleStart"),
                        r.getInt("week"),
                        r.getInt("recovery"),
                        r.getString("notes"),
                        r.getLong("updated"),
                    )
                }
            }
        require(
            reviews.all {
                it.cycleStart > 0 &&
                    it.week in 1..8 &&
                    it.recovery in 0..10 &&
                    it.notes.length <= 10000 &&
                    it.updated > 0
            }
        )
        require(reviews.map { it.cycleStart to it.week }.distinct().size == reviews.size)
        val preferences = j.optJSONObject("preferences")
        val importedProgram = preferences?.optString("program", Programs.CYCLE) ?: Programs.CYCLE
        val importedStart = preferences?.optLong("cycleStart", 0) ?: 0
        require(importedProgram in Programs.ids && importedStart >= 0)
        db.withTransaction {
            db.sessions().replace(data.map { SessionRecord(it.id, Codec.encode(it)) })
            db.reviews().clear()
            reviews.forEach { db.reviews().put(it) }
        }
        if (preferences != null)
            prefs.edit {
                it[stringPreferencesKey("program")] = importedProgram
                it[longPreferencesKey("cycleStart")] = importedStart
                it[booleanPreferencesKey("dark")] = preferences.optBoolean("dark", false)
                it[booleanPreferencesKey("autoRest")] = preferences.optBoolean("autoRest", true)
                it[booleanPreferencesKey("haptics")] = preferences.optBoolean("haptics", false)
                // Ne jamais réactiver une permission de notification via un import.
            }
    }
}
