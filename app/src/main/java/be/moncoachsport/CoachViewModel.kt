package be.moncoachsport

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.*

class CoachViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SportRepository(app)
    private val scheduler = RestScheduler(app)
    private val mutex = Mutex()

    private fun loadExercises(path: String): List<Exercise> =
        JSONArray(getApplication<Application>().assets.open(path).bufferedReader().readText())
            .let { a ->
                (0 until a.length()).map {
                    a.getJSONObject(it).let { j ->
                        Exercise(
                            j.getString("id"),
                            j.getString("name"),
                            j.getInt("sets"),
                            j.getInt("min"),
                            j.getInt("max"),
                            j.getInt("rest"),
                            j.getString("unit"),
                            j.getString("muscles"),
                            j.getString("setup"),
                            j.getString("accessory"),
                            j.getString("instructions"),
                            j.getString("image"),
                            j.optString("check"),
                            j.optString("caption"),
                            j.optString("warning"),
                            j.optInt("sourcePage"),
                        )
                    }
                }
            }

    private val legacyExercises = loadExercises("exercises.json")
    val guideExercises = loadExercises("guide/exercises.json")

    fun exercisesFor(program: String) =
        if (program == Programs.CYCLE) guideExercises else legacyExercises

    val guideSections =
        JSONArray(app.assets.open("guide/sections.json").bufferedReader().readText()).let { a ->
            (0 until a.length()).map {
                a.getJSONObject(it).let { j ->
                    GuideSection(j.getInt("page"), j.getString("title"), j.getString("text"))
                }
            }
        }
    internal val databaseState =
        repository.snapshots.stateIn(viewModelScope, SharingStarted.Eagerly, SessionReadModel())
    val sessions =
        databaseState
            .map { it.sessions }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val unreadable =
        databaseState
            .map { it.unreadable }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dark = repository.dark.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val program = repository.program.stateIn(viewModelScope, SharingStarted.Eagerly, Programs.CYCLE)
    val cycleStart = repository.cycleStart.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val reviews = repository.reviews.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val autoRest = repository.autoRest.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val haptics = repository.haptics.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAutoRest(value: Boolean) = action { repository.setAutoRest(value) }

    fun setHaptics(value: Boolean) = action { repository.setHaptics(value) }

    fun setCycleStart(value: Long) = action {
        require(
            repository.currentSessions().none {
                it.program == Programs.CYCLE && it.finished == null
            }
        ) {
            "Terminez la séance du cycle avant de changer sa date de départ."
        }
        require(value <= System.currentTimeMillis()) {
            "Choisissez une date passée ou aujourd’hui."
        }
        repository.setCycleStart(value)
    }

    fun saveReview(week: Int, recovery: String, notes: String) = action {
        val start = repository.cycleStart.first()
        require(start > 0) { "Commencez le cycle avant d’enregistrer un bilan." }
        val value = recovery.toIntOrNull()
        require(value != null && value in 0..10) { "Récupération : nombre de 0 à 10." }
        repository.saveReview(WeeklyReview(start, week, value, notes, System.currentTimeMillis()))
        message.value = "Bilan hebdomadaire enregistré"
    }

    val alerts = repository.alerts.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAlerts(value: Boolean) = action {
        repository.setAlerts(value)
        scheduler.update(timer.value.first, value)
    }

    val timer = repository.timer.stateIn(viewModelScope, SharingStarted.Eagerly, 0L to 0L)

    fun setTimer(end: Long, paused: Long = 0L) = action {
        repository.setTimer(end, paused)
        scheduler.update(end, alerts.value)
    }

    val message = MutableStateFlow("")
    private val ready = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.preferCurrentGuide()
            repository.currentSessions()
            ready.value = true
        }
    }

    val active
        get() = sessions.value.firstOrNull { it.isActive }

    val completed
        get() = sessions.value.filter { it.finished != null }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutex.withLock {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message.value = frenchError(e)
                }
            }
        }
    }

    fun start(requestedTemplate: String? = null) = action {
        ready.first { it }
        require(requestedTemplate == null || requestedTemplate in listOf("A", "B", "C")) {
            "Séance inconnue"
        }
        val data = repository.currentSessions()
        val current = data.firstOrNull { it.isActive }
        if (requestedTemplate == null && current != null) return@action
        val selected = current?.program ?: repository.program.first()
        val template = requestedTemplate ?: Programs.next(selected, data)
        if (current?.template == template) return@action
        val pending =
            data.firstOrNull {
                it.program == selected && it.template == template && it.finished == null
            }
        if (pending != null) {
            repository.setTimer(0, 0)
            scheduler.update(0, false)
            repository.activate(pending)
            return@action
        }
        val now = System.currentTimeMillis()
        var start = repository.cycleStart.first()
        if (selected == Programs.CYCLE && start == 0L) {
            start = now
            repository.setCycleStart(now)
        }
        val week = if (selected == Programs.CYCLE) Rules.week(start, now) else 1
        val prior =
            data
                .filter { it.program == selected && it.template == template && it.finished != null }
                .maxByOrNull { it.finished!! }
        val alternatives =
            if (selected == Programs.CYCLE) prior?.alternatives.orEmpty() else emptyMap()
        val allowed =
            Programs.cycle.getValue(template).take(3).map { alternatives[it.id] ?: it.id }.toSet()
        val extras =
            if (selected == Programs.CYCLE && week >= 7)
                prior?.extraSets.orEmpty().intersect(allowed)
            else emptySet()
        repository.setTimer(0, 0)
        scheduler.update(0, false)
        repository.activate(
            Training(
                UUID.randomUUID().toString(),
                template,
                now,
                program = selected,
                week = week,
                extraSets = extras,
                alternatives = alternatives,
            )
        )
    }

    fun log(set: LoggedSet, sessionId: String? = null) = action {
        require(Rules.validate(set)) { "Vérifiez charge, répétitions et RIR (0–10)." }
        val t = repository.currentSessions().firstOrNull { it.isActive } ?: return@action
        if (sessionId != null && t.id != sessionId) return@action
        val catalogue = exercisesFor(t.program)
        val p = Programs.plan(t, catalogue).firstOrNull { it.id == set.exerciseId }
        require(
            p != null &&
                set.exerciseId !in t.skipped &&
                set.index < Programs.exercise(p, t, catalogue).sets
        ) {
            "Série hors du programme de cette séance"
        }
        repository.save(
            t.copy(
                sets =
                    t.sets.filterNot { it.exerciseId == set.exerciseId && it.index == set.index } +
                        set
            )
        )
        if (repository.autoRest.first() && p.restMax > 0) {
            val seconds =
                t.drafts["${p.id}-rest"]?.toIntOrNull()?.takeIf { it in p.restMin..p.restMax }
                    ?: p.restMax
            val end = System.currentTimeMillis() + seconds * 1000L
            repository.setTimer(end, 0)
            scheduler.update(end, repository.alerts.first())
        }
    }

    fun skip(id: String) = action {
        val t = repository.currentSessions().firstOrNull { it.isActive } ?: return@action
        require(Programs.plan(t, exercisesFor(t.program)).any { it.id == id && it.optional }) {
            "Cet exercice n'est pas facultatif dans cette séance"
        }
        require(t.sets.none { it.exerciseId == id }) {
            "Cet exercice contient déjà des séries : elles sont conservées."
        }
        repository.save(t.copy(skipped = if (id in t.skipped) t.skipped - id else t.skipped + id))
    }

    fun extra(id: String) = action {
        val t = repository.currentSessions().firstOrNull { it.isActive } ?: return@action
        val slot = Programs.plan(t, exercisesFor(t.program)).indexOfFirst { it.id == id }
        require(Programs.extraAllowed(t, slot)) {
            "Troisième série non autorisée pour cet exercice ou cette semaine"
        }
        val removing = id in t.extraSets
        require(removing || t.week in 5..6) { "Semaines 7–8 : conserver un volume stable." }
        require(!removing || t.sets.none { it.exerciseId == id && it.index == 2 }) {
            "La troisième série est déjà enregistrée : elle est conservée."
        }
        repository.save(t.copy(extraSets = if (removing) t.extraSets - id else t.extraSets + id))
    }

    fun chooseVariant(slot: String, replacement: String) = action {
        val t = repository.currentSessions().firstOrNull { it.isActive } ?: return@action
        require(t.program == Programs.CYCLE) {
            "Les remplacements ne sont disponibles que dans le cycle de huit semaines"
        }
        require(Programs.cycle.getValue(t.template).any { it.id == slot }) { "Mouvement inconnu" }
        require(
            replacement == slot ||
                Programs.variants.any {
                    it.template == t.template && it.slot == slot && it.replacement == replacement
                }
        ) {
            "Ce remplacement n'est pas autorisé pour cet emplacement"
        }
        val current = t.alternatives[slot] ?: slot
        require(t.sets.none { it.exerciseId == current }) {
            "Le mouvement est déjà commencé. Ses séries et paramètres sont conservés."
        }
        val choices =
            if (replacement == slot) t.alternatives - slot
            else t.alternatives + (slot to replacement)
        repository.save(
            t.copy(
                alternatives = choices,
                extraSets =
                    if (current in t.extraSets) t.extraSets - current + replacement else t.extraSets,
            )
        )
    }

    fun saveDraft(key: String, value: String, sessionId: String? = null) = action {
        require(value.length <= 10000) { "Texte trop long" }
        val t = repository.currentSessions().firstOrNull { it.isActive } ?: return@action
        if (sessionId != null && t.id != sessionId) return@action
        repository.saveDraft(t.id, key, value)
    }

    fun finish(
        notes: String,
        cardio: String,
        energy: String,
        discomfort: String,
        machine: String,
        intensity: String,
        sessionId: String? = null,
    ) = action {
        val t = repository.currentSessions().firstOrNull { it.isActive } ?: return@action
        if (sessionId != null && t.id != sessionId) return@action
        require(t.sets.any { it.done }) { "Validez au moins une série avant de terminer." }
        val minutes = cardio.toIntOrNull()
        val e = energy.toIntOrNull()
        val i = intensity.toIntOrNull()
        require(
            minutes != null &&
                minutes in 0..600 &&
                e != null &&
                e in 0..10 &&
                i != null &&
                i in 0..10
        ) {
            "Bilan invalide : minutes 0–600, énergie et intensité 0–10."
        }
        repository.save(
            t.copy(
                finished = System.currentTimeMillis(),
                notes = notes,
                cardio = minutes,
                energy = e,
                discomfort = discomfort,
                cardioMachine = machine,
                intensity = i,
            )
        )
        repository.setTimer(0, 0)
        scheduler.update(0, false)
    }

    fun toggleTheme() = action { repository.setDark(!dark.value) }

    fun erase() = action {
        repository.erase()
        repository.setTimer(0, 0)
        scheduler.update(0, false)
        message.value = "Historique supprimé"
    }

    fun eraseAll() = action {
        repository.eraseAll()
        scheduler.update(0, false)
        message.value = "Toutes les données et préférences ont été supprimées"
    }

    suspend fun exportJson(): String {
        val snapshot = repository.snapshotNow()
        return JSONObject()
            .put("version", if (snapshot.unreadable.isEmpty()) 3 else 4)
            .put(
                "sessions",
                JSONArray().apply {
                    snapshot.sessions.forEach { put(JSONObject(Codec.encode(it))) }
                },
            )
            .put(
                "preferences",
                JSONObject()
                    .put("program", program.value)
                    .put("cycleStart", cycleStart.value)
                    .put("dark", dark.value)
                    .put("autoRest", autoRest.value)
                    .put("haptics", haptics.value),
            )
            .put(
                "reviews",
                JSONArray().apply {
                    reviews.value.forEach {
                        put(
                            JSONObject()
                                .put("cycleStart", it.cycleStart)
                                .put("week", it.week)
                                .put("recovery", it.recovery)
                                .put("notes", it.notes)
                                .put("updated", it.updated)
                        )
                    }
                },
            )
            .apply {
                if (snapshot.unreadable.isNotEmpty())
                    put(
                        "recovery",
                        JSONObject()
                            .put(
                                "rows",
                                JSONArray().apply {
                                    snapshot.rows.forEach {
                                        put(
                                            JSONObject().put("id", it.id).put("payload", it.payload)
                                        )
                                    }
                                },
                            )
                            .put(
                                "drafts",
                                JSONArray().apply {
                                    snapshot.draftRows.forEach {
                                        put(
                                            JSONObject()
                                                .put("sessionId", it.sessionId)
                                                .put("field", it.field)
                                                .put("value", it.value)
                                        )
                                    }
                                },
                            )
                            .put(
                                "unreadable",
                                JSONArray().apply {
                                    snapshot.unreadable.forEach {
                                        put(JSONObject().put("id", it.id).put("reason", it.reason))
                                    }
                                },
                            ),
                    )
            }
            .toString(2)
    }

    suspend fun exportCsv(): String {
        return HistoryCsv.encode(repository.currentSessions())
    }

    fun restore(raw: String) = action {
        val backup =
            if (raw.trimStart().startsWith("{")) raw
            else {
                val parsed = HistoryCsv.asBackup(HistoryCsv.decode(raw))
                parsed.put("reviews", JSONObject(exportJson()).getJSONArray("reviews"))
                parsed.toString()
            }
        repository.restore(
            backup,
            Programs.ids.associateWith { p -> exercisesFor(p).map { it.id }.toSet() },
        )
        repository.preferCurrentGuide()
        repository.setTimer(0, 0)
        scheduler.update(0, false)
        message.value = "Sauvegarde restaurée"
    }

    fun restartCycle() = action {
        require(
            repository.currentSessions().none {
                it.program == Programs.CYCLE && it.finished == null
            }
        ) {
            "Reprenez et terminez les séances du cycle en pause avant de recommencer en semaine 1."
        }
        repository.setCycleStart(System.currentTimeMillis())
        repository.setTimer(0, 0)
        scheduler.update(0, false)
        message.value = "Nouveau cycle commencé en semaine 1. L'historique est conservé."
    }

    override fun onCleared() {
        viewModelScope.cancel()
        repository.close()
        super.onCleared()
    }
}
