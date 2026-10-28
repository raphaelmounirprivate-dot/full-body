package be.moncoachsport

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
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

    @Transaction
    @Query("SELECT * FROM sessions")
    fun observeWithDrafts(): Flow<List<SessionWithDrafts>>

    @Query("SELECT * FROM sessions") suspend fun read(): List<SessionRecord>

    @Query("SELECT * FROM sessions WHERE id = :id") suspend fun find(id: String): SessionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(row: SessionRecord)

    @Query("DELETE FROM sessions") suspend fun clear()

    @Transaction
    suspend fun replace(rows: List<SessionRecord>) {
        clear()
        rows.forEach { put(it) }
    }
}

@Entity(tableName = "session_drafts", primaryKeys = ["sessionId", "field"])
data class DraftRecord(val sessionId: String, val field: String, val value: String)

data class SessionWithDrafts(
    @Embedded val session: SessionRecord,
    @Relation(parentColumn = "id", entityColumn = "sessionId") val drafts: List<DraftRecord>,
)

@Dao
interface DraftDao {
    @Query("SELECT * FROM session_drafts") fun observe(): Flow<List<DraftRecord>>

    @Query("SELECT * FROM session_drafts") suspend fun read(): List<DraftRecord>

    @Query("SELECT * FROM session_drafts WHERE sessionId = :id")
    suspend fun readSession(id: String): List<DraftRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(draft: DraftRecord)

    @Query("DELETE FROM session_drafts WHERE sessionId = :id") suspend fun clearSession(id: String)

    @Query("DELETE FROM session_drafts") suspend fun clear()
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

@Database(
    entities = [SessionRecord::class, WeeklyReview::class, DraftRecord::class, BodyMeasurement::class],
    version = 4,
    exportSchema = false,
)
abstract class SportDatabase : RoomDatabase() {
    abstract fun measurements(): MeasurementDao
    abstract fun sessions(): SessionDao

    abstract fun reviews(): ReviewDao

    abstract fun drafts(): DraftDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS body_measurements (day TEXT NOT NULL PRIMARY KEY, kg REAL, fat REAL)")
            }
        }
        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `session_drafts` (`sessionId` TEXT NOT NULL, `field` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`sessionId`, `field`))"
                    )
                }
            }
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

    fun decode(raw: String): Training =
        try {
            decodeFields(raw)
        } catch (e: JSONException) {
            throw IllegalArgumentException(
                "Séance illisible : un champ obligatoire est absent ou mal formé.",
                e,
            )
        }

    private fun decodeFields(raw: String): Training {
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
            (if (j.has("drafts")) j.getJSONObject("drafts") else null)?.let { d ->
                d.keys().asSequence().associateWith { d.getString(it) }
            } ?: emptyMap()
        require(drafts.size <= 500 && drafts.values.all { it.length <= 10000 }) {
            "Trop de brouillons ou texte trop long dans la séance"
        }
        fun ids(key: String) =
            (if (j.has(key)) j.getJSONArray(key) else null)?.let { a ->
                (0 until a.length()).map { a.getString(it) }.toSet()
            } ?: emptySet()
        val alternatives =
            (if (j.has("alternatives")) j.getJSONObject("alternatives") else null)?.let { a ->
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
                if (j.has("intensity")) j.getInt("intensity") else 3,
                drafts,
                j.optString("program", Programs.LEGACY),
                if (j.has("week")) j.getInt("week") else 1,
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
                ) {
                    "Type, identifiant ou dates de séance invalides"
                }
                require(it.cardio in 0..600 && it.energy in 0..10 && it.intensity in 0..10) {
                    "Bilan invalide : minutes de 0 à 600, énergie et effort de 0 à 10"
                }
                require(it.program in Programs.ids && it.week in 1..8) {
                    "Programme inconnu ou semaine hors de la plage 1 à 8"
                }
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
                    require(it.skipped.all { id -> plan.any { p -> p.id == id && p.optional } }) {
                        "Un exercice obligatoire ou inconnu a été marqué comme sauté"
                    }
                    require(
                        it.extraSets.all { id ->
                            it.week >= 5 && plan.take(3).any { p -> p.id == id }
                        }
                    ) {
                        "Troisième série non autorisée pour cet exercice ou cette semaine"
                    }
                    require(
                        it.sets.all { s ->
                            val p = plan.firstOrNull { p -> p.id == s.exerciseId }
                            p != null &&
                                s.exerciseId !in it.skipped &&
                                s.index < 2 + if (s.exerciseId in it.extraSets) 1 else 0
                        }
                    ) {
                        "Une série ne correspond pas aux exercices ou options de la séance"
                    }
                } else
                    require(
                        it.skipped.isEmpty() && it.extraSets.isEmpty() && it.alternatives.isEmpty()
                    ) {
                        "L'ancien programme ne permet pas ces options ou remplacements"
                    }
            }
    }
}

class SportRepository(context: Context, database: SportDatabase? = null) {
    private val db =
        database
            ?: Room.databaseBuilder(
                    context.applicationContext,
                    SportDatabase::class.java,
                    "sport.db",
                )
                .addMigrations(SportDatabase.MIGRATION_1_2, SportDatabase.MIGRATION_2_3, SportDatabase.MIGRATION_3_4)
                .build()
    private val prefs = context.applicationContext.preferences
    val measurements = db.measurements().observe()
    suspend fun readMeasurements() = db.measurements().read()
    suspend fun saveMeasurement(value: BodyMeasurement) {
        FeatureRules.validate(value)
        require(db.measurements().read().let { rows -> rows.size < 10000 || rows.any { it.day == value.day } }) { "Limite de 10 000 mesures atteinte" }
        db.measurements().put(value)
    }
    suspend fun deleteMeasurement(day: String) = db.measurements().delete(day)
    val personal = prefs.data.map {
        it[stringPreferencesKey("personal")]?.let { raw -> PersonalCodec.decode(JSONObject(raw)) } ?: PersonalOptions()
    }
    suspend fun setPersonal(value: PersonalOptions) {
        FeatureRules.validate(value)
        prefs.edit { it[stringPreferencesKey("personal")] = PersonalCodec.encode(value).toString() }
    }
    private val decoder = SessionRowDecoder()
    val snapshots = db.sessions().observeWithDrafts().map { rows ->
        decoder.readAll(rows.map { it.session }, rows.flatMap { it.drafts })
    }.flowOn(Dispatchers.Default)
    val sessions = snapshots.map { it.sessions }.distinctUntilChanged()
    // Only session-table changes: draft keystrokes never refresh a launcher widget.
    val widgetSnapshots = db.sessions().observe().map { decoder.readAll(it) }.flowOn(Dispatchers.Default)

    suspend fun snapshotNow(): SessionReadModel =
        db.withTransaction { decoder.readAll(db.sessions().read(), db.drafts().read()) }

    suspend fun currentSessions(): List<Training> = snapshotNow().sessions

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
        require(value > 0) { "La date de départ du cycle est invalide" }
        prefs.edit { it[longPreferencesKey("cycleStart")] = value }
    }

    suspend fun saveReview(review: WeeklyReview) {
        require(review.week in 1..8 && review.recovery in 0..10 && review.notes.length <= 10000) {
            "Bilan hebdomadaire invalide : semaine de 1 à 8, récupération de 0 à 10, notes limitées à 10 000 caractères"
        }
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

    suspend fun adjustTimer(seconds: Int): Pair<Long, Long> {
        var updated = 0L to 0L
        prefs.edit {
            updated = RestRules.adjust(it[longPreferencesKey("restEnd")] ?: 0L,
                it[longPreferencesKey("restPaused")] ?: 0L, seconds, System.currentTimeMillis())
            it[longPreferencesKey("restEnd")] = updated.first
            it[longPreferencesKey("restPaused")] = updated.second
        }
        return updated
    }

    suspend fun dismissExpiredTimer(expectedEnd: Long): Boolean {
        var dismissed = false
        prefs.edit {
            if (expectedEnd > 0 && it[longPreferencesKey("restEnd")] == expectedEnd &&
                System.currentTimeMillis() >= expectedEnd) {
                it[longPreferencesKey("restEnd")] = 0L
                it[longPreferencesKey("restPaused")] = 0L
                dismissed = true
            }
        }
        return dismissed
    }

    suspend fun setDark(value: Boolean) {
        prefs.edit { it[booleanPreferencesKey("dark")] = value }
    }

    suspend fun save(t: Training) =
        db.withTransaction {
            db.sessions().put(SessionRecord(t.id, Codec.encode(t)))
            db.drafts().clearSession(t.id)
        }

    suspend fun saveDraft(id: String, field: String, value: String) =
        db.withTransaction {
            require(field.isNotBlank() && field.length <= 200 && value.length <= 10000) {
                "Champ de saisie invalide ou texte trop long"
            }
            val row =
                db.sessions().find(id)
                    ?: throw IllegalArgumentException("La séance à modifier n'existe plus")
            require(decoder.read(row).getOrThrow().isActive) { "La séance n'est plus active" }
            val known =
                db.drafts().readSession(id).map { it.field }.toSet() +
                    decoder.read(row).getOrThrow().drafts.keys
            require(field in known || known.size < 500) {
                "La séance contient trop de champs de saisie"
            }
            db.drafts().put(DraftRecord(id, field, value))
        }

    suspend fun activate(target: Training) {
        require(target.finished == null) { "Cette séance est déjà terminée" }
        Codec.decode(Codec.encode(target))
        db.withTransaction {
            val snapshot = snapshotNow()
            require(snapshot.unreadable.none { it.id == target.id }) {
                "Cette séance est illisible : exportez ses données avant toute récupération"
            }
            val data = snapshot.sessions
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
            require(next.finished == null) { "Cette séance est déjà terminée" }
            data.filter { it.isActive && it.id != next.id }.forEach { save(it.copy(paused = true)) }
            save(next.copy(paused = false))
        }
    }

    suspend fun erase() =
        db.withTransaction {
            db.sessions().clear()
            db.drafts().clear()
            db.reviews().clear()
        }

    suspend fun eraseAll() {
        erase()
        db.measurements().clear()
        prefs.edit { it.clear() }
    }

    fun close() = db.close()

    suspend fun restore(raw: String, catalogues: Map<String, Set<String>>) =
        try {
            restoreValidated(raw, catalogues)
        } catch (e: JSONException) {
            throw IllegalArgumentException(
                "Sauvegarde JSON illisible : un champ obligatoire est absent ou mal formé.",
                e,
            )
        }

    private suspend fun restoreValidated(raw: String, catalogues: Map<String, Set<String>>) {
        require(raw.length <= 5_000_000) { "Sauvegarde trop volumineuse" }
        val j = JSONObject(raw)
        require(!j.has("recovery")) {
            "Cette sauvegarde contient des lignes illisibles à récupérer. Aucune donnée n'a été remplacée."
        }
        require(j.getInt("version") in listOf(1, 2, 3, 5)) { "Version inconnue" }
        val personal = if (j.has("personal")) PersonalCodec.decode(j.getJSONObject("personal")) else null
        val measurements = if (j.has("measurements")) PersonalCodec.measurements(j.getJSONArray("measurements")) else null
        require(j.getInt("version") == 5 || (personal == null && measurements == null)) { "Les données personnelles nécessitent le format 5" }
        val a = j.getJSONArray("sessions")
        require(a.length() <= 10000) { "La sauvegarde dépasse la limite de 10 000 séances" }
        val data = (0 until a.length()).map { Codec.decode(a.getJSONObject(it).toString()) }
        require(data.map { it.id }.distinct().size == data.size) {
            "La sauvegarde contient des identifiants de séance dupliqués"
        }
        require(data.count { it.isActive } <= 1) {
            "La sauvegarde contient plusieurs séances actives"
        }
        require(j.getInt("version") >= 3 || data.none { it.paused }) {
            "Les séances en pause nécessitent une sauvegarde version 3"
        }
        require(
            data.filter { it.finished == null }.map { it.program to it.template }.distinct().size ==
                data.count { it.finished == null }
        ) {
            "Plusieurs séances non terminées du même type"
        }
        require(data.all { t -> t.sets.all { it.exerciseId in catalogues[t.program].orEmpty() } }) {
            "Un exercice n'appartient pas au catalogue de son programme"
        }
        val reviewArray = if (j.has("reviews")) j.getJSONArray("reviews") else JSONArray()
        require(reviewArray.length() <= 10000) {
            "La sauvegarde dépasse la limite de 10 000 bilans"
        }
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
        ) {
            "Un bilan hebdomadaire contient une date, une semaine, une note ou une récupération invalide"
        }
        require(reviews.map { it.cycleStart to it.week }.distinct().size == reviews.size) {
            "La sauvegarde contient des bilans hebdomadaires dupliqués"
        }
        val preferences = if (j.has("preferences")) j.getJSONObject("preferences") else null
        val importedProgram = preferences?.optString("program", Programs.CYCLE) ?: Programs.CYCLE
        val importedStart =
            if (preferences?.has("cycleStart") == true) preferences.getLong("cycleStart") else 0
        require(importedProgram in Programs.ids && importedStart >= 0) {
            "Le programme ou la date de départ importés sont invalides"
        }
        if (preferences != null)
            listOf("dark", "autoRest", "haptics").forEach {
                if (preferences.has(it)) preferences.getBoolean(it)
            }
        db.withTransaction {
            db.sessions().replace(data.map { SessionRecord(it.id, Codec.encode(it)) })
            db.drafts().clear()
            db.reviews().clear()
            reviews.forEach { db.reviews().put(it) }
            if (measurements != null) {
                db.measurements().clear()
                measurements.forEach { db.measurements().put(it) }
            }
        }
        if (personal != null) setPersonal(personal.copy(cycleReminder = false,
            preferredVariants = if (j.getJSONObject("personal").has("preferredVariants")) personal.preferredVariants
                else this.personal.first().preferredVariants))
        if (preferences != null)
            prefs.edit {
                it[stringPreferencesKey("program")] = importedProgram
                it[longPreferencesKey("cycleStart")] = importedStart
                it[booleanPreferencesKey("dark")] =
                    (if (preferences.has("dark")) preferences.getBoolean("dark") else false)
                it[booleanPreferencesKey("autoRest")] =
                    (if (preferences.has("autoRest")) preferences.getBoolean("autoRest") else true)
                it[booleanPreferencesKey("haptics")] =
                    (if (preferences.has("haptics")) preferences.getBoolean("haptics") else false)
                // Ne jamais réactiver une permission de notification via un import.
            }
    }
}
