package be.moncoachsport

data class RecordResult(val weight: Boolean, val repetitions: Boolean, val seconds: Boolean) {
    val achieved get() = weight || repetitions || seconds
    fun label() = listOfNotNull(if (weight) "charge" else null, if (repetitions) "répétitions" else null,
        if (seconds) "durée" else null).joinToString(" et ")
}
data class RecordNotice(val sessionId: String, val exerciseName: String, val result: RecordResult, val token: Long)

object PersonalRecords {
    fun evaluate(t: Training, set: LoggedSet, history: List<Training>, exercise: Exercise,
        historyComplete: Boolean = true): RecordResult {
        val none = RecordResult(false,false,false)
        if (!historyComplete || !set.done || !Rules.validate(set) ||
            set.machine.isBlank() || set.setting.isBlank() || set.exerciseId != exercise.id) return none
        if (t.sets.any { it.exerciseId == set.exerciseId && it.index == set.index && it == set }) return none
        val baseline = history.filter { it.id != t.id && it.program == t.program && it.finished != null }
            .flatMap { it.sets } + t.sets.filterNot { it.exerciseId == set.exerciseId && it.index == set.index }
        val comparable = baseline.filter {
            it.done && it.exerciseId == set.exerciseId && it.machine.trim() == set.machine.trim() &&
                it.setting.trim() == set.setting.trim() && it.loadUnit == set.loadUnit
        }
        if (comparable.isEmpty()) return none // First measurement establishes a baseline, not a record.
        val atLoad = comparable.filter { it.kg == set.kg }
        val reps = atLoad.isNotEmpty() && set.reps > atLoad.maxOf { it.reps }
        return RecordResult(exercise.unit != "s" && set.kg > comparable.maxOf { it.kg },
            exercise.unit != "s" && reps, exercise.unit == "s" && reps)
    }
    fun forStored(t: Training, set: LoggedSet, history: List<Training>, exercise: Exercise,
        historyComplete: Boolean = true): RecordResult {
        val position = t.sets.indexOfFirst { it.exerciseId == set.exerciseId && it.index == set.index }
        if (position < 0) return RecordResult(false,false,false)
        return evaluate(t.copy(sets=t.sets.take(position)),set,history,exercise,historyComplete)
    }
}
