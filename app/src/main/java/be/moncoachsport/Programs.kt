package be.moncoachsport

data class Prescription(
    val id: String,
    val min: Int,
    val max: Int,
    val restMin: Int,
    val restMax: Int = restMin,
    val optional: Boolean = false,
    val unit: String = "rép",
    val baseSets: Int = 2,
)

data class Phase(val rirMin: Int, val rirMax: Int, val cardio: String, val advice: String)

data class ExerciseVariant(
    val template: String,
    val slot: String,
    val replacement: String,
    val condition: String,
    val min: Int? = null,
    val max: Int? = null,
    val restMin: Int? = null,
    val restMax: Int? = null,
    val unit: String? = null,
)

/** Prescriptions du guide personnel de 31 pages, pages 2 à 8 et fiches F1-F19. */
object Programs {
    const val LEGACY = "machines-8-pages"
    const val CYCLE = "cycle-8-semaines"
    val ids = setOf(LEGACY, CYCLE)
    val legacy =
        mapOf(
            "A" to listOf("F8", "F2", "F3", "F4", "F5", "F19"),
            "B" to listOf("F1", "F9", "F10", "F18", "F12", "F13", "F6"),
            "C" to listOf("F7", "F11", "F14", "F15", "F16", "F17"),
        )
    val cycle =
        mapOf(
            "A" to
                listOf(
                    Prescription("F1", 10, 15, 120),
                    Prescription("F2", 8, 12, 90, 120),
                    Prescription("F3", 10, 15, 90, 120),
                    Prescription("F4", 10, 15, 60, 90),
                    Prescription("F5", 12, 15, 60, optional = true),
                    Prescription("F6", 15, 30, 45, 60, unit = "s"),
                ),
            "B" to
                listOf(
                    Prescription("F7", 10, 12, 120),
                    Prescription("F9", 10, 15, 90, 120),
                    Prescription("F2", 10, 15, 90, 120),
                    Prescription("F11", 10, 15, 90),
                    Prescription("F12", 10, 15, 60, 90, true),
                    Prescription("F13", 10, 15, 60, 90, true),
                ),
            "C" to
                listOf(
                    Prescription("F1", 10, 15, 120),
                    Prescription("F14", 10, 15, 90),
                    Prescription("F2", 8, 12, 90, 120),
                    Prescription("F4", 10, 15, 60, 90),
                    Prescription("F3", 10, 15, 90),
                    Prescription("F15", 10, 15, 90, optional = true),
                ),
        )
    val names =
        mapOf(
            "F1" to "Presse à cuisses",
            "F2" to "Chest press guidée",
            "F3" to "Tirage vertical",
            "F4" to "Leg curl assis",
            "F5" to "Élévations latérales à la poulie",
            "F6" to "Planche",
            "F7" to "Squat à levier",
            "F8" to "Hack squat",
            "F9" to "Rowing avec appui poitrine",
            "F10" to "Développé incliné aux poulies",
            "F11" to "Hip thrust machine",
            "F12" to "Curl poulie basse",
            "F13" to "Triceps à la corde",
            "F14" to "Rowing assis à la poulie",
            "F15" to "Développé épaules machine",
            "F16" to "Écarté pectoraux à la poulie",
            "F17" to "Rameur",
            "F18" to "Leg extension",
            "F19" to "Crunch à la poulie haute",
        )
    val variants =
        listOf(
            ExerciseVariant(
                "B",
                "F7",
                "F8",
                "Choisir le squat à levier OU le hack squat, après apprentissage des sécurités (p. 5, 15–16).",
            ),
            ExerciseVariant(
                "B",
                "F7",
                "F1",
                "Presse à la place du squat B : mêmes séries et répétitions prévues (p. 8).",
            ),
            ExerciseVariant(
                "B",
                "F7",
                "F18",
                "Si le squat reste difficile à apprendre. Garder les presses A/C et le hip thrust B ; ne pas remplacer le leg curl (p. 26).",
                12,
                15,
                60,
                90,
            ),
            ExerciseVariant(
                "B",
                "F9",
                "F14",
                "Rowing assis à la place du rowing avec appui, buste maîtrisé. Garder les séries, répétitions et repos du mouvement remplacé (p. 8).",
            ),
            ExerciseVariant(
                "B",
                "F2",
                "F10",
                "Installation à apprendre avec un coach ; la machine guidée reste le choix par défaut (p. 18).",
                10,
                15,
                90,
                120,
            ),
            ExerciseVariant(
                "C",
                "F2",
                "F16",
                "Si le mouvement est appris ; garder les développés en A et B (p. 8, 24).",
                10,
                15,
                90,
                90,
            ),
            ExerciseVariant(
                "A",
                "F6",
                "F19",
                "Autre travail du tronc, fonction différente : pas un équivalent exact de la planche. Faire vérifier le geste (p. 27).",
                12,
                15,
                60,
                90,
                "rép",
            ),
        )

    fun title(id: String) = if (id == CYCLE) "Cycle de 8 semaines" else "Ancien programme · 8 pages"

    fun phase(week: Int) =
        when (week.coerceIn(1, 8)) {
            1,
            2 ->
                Phase(
                    3,
                    4,
                    "8–12 min faciles",
                    "Deux séries de travail. Priorité aux réglages et à la régularité.",
                )
            3,
            4 ->
                Phase(
                    2,
                    3,
                    "12–18 min faciles à modérées",
                    "Deux séries de travail. Gardez une réserve confortable.",
                )
            5,
            6 ->
                Phase(
                    2,
                    3,
                    "15–20 min modérées",
                    "Deux séries suffisent. Une troisième série est facultative sur les trois premiers mouvements, après stabilisation du volume.",
                )
            else ->
                Phase(
                    2,
                    3,
                    "20–25 min modérées",
                    "Gardez le volume stable. Les intervalles restent facultatifs si bien tolérés.",
                )
        }

    fun cyclePlan(t: Training): List<Prescription> =
        cycle.getValue(t.template).map { p ->
            val replacement = t.alternatives[p.id] ?: return@map p
            val v =
                variants.first {
                    it.template == t.template && it.slot == p.id && it.replacement == replacement
                }
            p.copy(
                id = replacement,
                min = v.min ?: p.min,
                max = v.max ?: p.max,
                restMin = v.restMin ?: p.restMin,
                restMax = v.restMax ?: p.restMax,
                unit = v.unit ?: p.unit,
            )
        }

    fun plan(t: Training, exercises: List<Exercise>): List<Prescription> {
        if (t.program == LEGACY)
            return legacy.getValue(t.template).map { id ->
                val e = exercises.first { it.id == id }
                Prescription(id, e.min, e.max, e.rest, e.rest, unit = e.unit, baseSets = e.sets)
            }
        return cyclePlan(t)
    }

    fun exercise(p: Prescription, t: Training, exercises: List<Exercise>): Exercise {
        val e = exercises.first { it.id == p.id }
        return e.copy(
            name = if (t.program == CYCLE) names.getValue(e.id) else e.name,
            sets = p.baseSets + if (p.id in t.extraSets) 1 else 0,
            min = p.min,
            max = p.max,
            rest = p.restMax,
            unit = p.unit,
        )
    }

    fun extraAllowed(t: Training, slot: Int) = t.program == CYCLE && t.week >= 5 && slot in 0..2

    fun next(program: String, sessions: List<Training>): String {
        val last =
            sessions
                .filter { it.program == program && it.finished != null }
                .maxByOrNull { it.finished!! }
        return Rules.next(if (last == null) 0 else listOf("A", "B", "C").indexOf(last.template) + 1)
    }

    fun canIncrease(
        t: Training,
        p: Prescription,
        history: List<Training>,
        exercises: List<Exercise>,
        machine: String,
        setting: String,
        loadUnit: String,
        historyComplete: Boolean = true,
    ): Boolean {
        if (!historyComplete || p.unit == "s") return false
        val prior =
            history
                .filter {
                    it.finished != null &&
                        it.program == t.program &&
                        it.sets.any { s -> s.exerciseId == p.id }
                }
                // A different repetition range is a different prescription, not a failed passage.
                .filter { old ->
                    val previous = plan(old, exercises).firstOrNull { it.id == p.id }
                    previous != null && previous.min == p.min && previous.max == p.max &&
                        previous.unit == p.unit
                }
                .sortedBy { it.finished }
                .takeLast(2)
        if (prior.size < 2 || machine.isBlank() || setting.isBlank()) return false
        val current = exercise(p, t, exercises)
        return prior.all { old ->
            val oldP = plan(old, exercises).firstOrNull { it.id == p.id } ?: return@all false
            val oldE = exercise(oldP, old, exercises)
            val sets = old.sets.filter { it.exerciseId == p.id }
            val rir =
                if (old.program == CYCLE) maxOf(phase(old.week).rirMin, phase(t.week).rirMin) else 1
            oldP.min == p.min &&
                oldP.max == p.max &&
                oldE.sets == current.sets &&
                sets.size == oldE.sets &&
                sets.all {
                    it.done &&
                        it.reps >= p.max &&
                        it.rir >= rir &&
                        it.machine.trim() == machine.trim() &&
                        it.setting.trim() == setting.trim() &&
                        it.loadUnit == loadUnit
                }
        } &&
            prior
                .flatMap { it.sets.filter { s -> s.exerciseId == p.id } }
                .map { it.kg }
                .distinct()
                .size == 1
    }
}
