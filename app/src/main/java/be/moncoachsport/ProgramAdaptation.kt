package be.moncoachsport

/** Guide variants and an explicit personal replacement; historical sessions keep their own choices. */
data class PreferredVariant(val template: String, val slot: String, val replacement: String) {
    val key get() = "$template:$slot"
}

data class TrainingActivity(val sessions: Int, val sets: Map<String, Int>, val cardioMinutes: Int)

object ProgramAdaptation {
    val choices = listOf(
        PreferredVariant("B", "F2", "F10"),
        PreferredVariant("C", "F2", "F16"),
        PreferredVariant("A", "F6", "F19"),
        PreferredVariant("B", "F11", ClubMachines.REAR_KICK),
    )

    fun choiceValue(choice: PreferredVariant, prior: Map<String, String>, preferences: Map<String, String>): String =
        preferences[choice.key] ?: prior[choice.slot] ?:
            if (choice.replacement == ClubMachines.REAR_KICK) choice.replacement else choice.slot

    fun alternatives(template: String, prior: Map<String, String>, preferences: Map<String, String>): Map<String, String> =
        prior.toMutableMap().apply {
            choices.filter { it.template == template }.forEach { choice ->
                when (choiceValue(choice, prior, preferences)) {
                    choice.slot -> remove(choice.slot)
                    choice.replacement -> put(choice.slot, choice.replacement)
                }
            }
        }

    // Direct work only. Compound exercises are not counted again for secondary muscles.
    private val groups = mapOf(
        "F1" to "Quadriceps", "F7" to "Quadriceps", "F8" to "Quadriceps", "F18" to "Quadriceps",
        "F2" to "Pectoraux", "F10" to "Pectoraux", "F16" to "Pectoraux",
        "F3" to "Dos", "F9" to "Dos", "F14" to "Dos",
        "F4" to "Ischio-jambiers", "F11" to "Fessiers", ClubMachines.REAR_KICK to "Fessiers", "F5" to "Épaules", "F15" to "Épaules",
        "F6" to "Tronc", "F19" to "Tronc", "F12" to "Biceps", "F13" to "Triceps",
    )

    /** Rolling seven days, finished sessions of the current cycle only, actual validated sets. */
    fun activity(history: List<Training>, cycleStart: Long, now: Long): TrainingActivity {
        val start = maxOf(cycleStart, now - 7L * 24 * 60 * 60 * 1000)
        val completed = history.distinctBy { it.id }.filter {
            it.program == Programs.CYCLE && it.started >= cycleStart &&
                it.finished?.let { end -> end in start..now } == true
        }
        val sets = completed.flatMap { it.sets }.filter { it.done }
            .mapNotNull { groups[it.exerciseId] }.groupingBy { it }.eachCount()
        return TrainingActivity(completed.size, sets, completed.sumOf { it.cardio })
    }

    fun priorPassages(t: Training, history: List<Training>, cycleStart: Long) = history.distinctBy { it.id }.count {
        it.program == t.program && it.template == t.template && it.started >= cycleStart &&
            it.finished?.let { end -> end <= t.started } == true && it.id != t.id
    }
}
