package be.moncoachsport

data class Exercise(
    val id: String,
    val name: String,
    val sets: Int,
    val min: Int,
    val max: Int,
    val rest: Int,
    val unit: String,
    val muscles: String,
    val setup: String,
    val accessory: String,
    val instructions: String,
    val image: String,
    val check: String = "",
    val caption: String = "",
    val warning: String = "",
    val sourcePage: Int = 0,
)

data class GuideSection(val page: Int, val title: String, val text: String)

data class LoggedSet(
    val exerciseId: String,
    val index: Int,
    val kg: Double,
    val reps: Int,
    val rir: Int,
    val machine: String,
    val setting: String,
    val done: Boolean = true,
    val loadUnit: String = "total",
)

data class Training(
    val id: String,
    val template: String,
    val started: Long,
    val finished: Long? = null,
    val sets: List<LoggedSet> = emptyList(),
    val notes: String = "",
    val cardio: Int = 0,
    val energy: Int = 5,
    val discomfort: String = "",
    val cardioMachine: String = "Vélo",
    val intensity: Int = 3,
    val drafts: Map<String, String> = emptyMap(),
    val program: String = Programs.LEGACY,
    val week: Int = 1,
    val skipped: Set<String> = emptySet(),
    val extraSets: Set<String> = emptySet(),
    val alternatives: Map<String, String> = emptyMap(),
    val paused: Boolean = false,
) {
    val isActive: Boolean
        get() = finished == null && !paused
}

object Rules {
    fun cycleHasEnded(
        start: Long,
        now: Long,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): Boolean {
        if (start <= 0) return false
        val first = java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
        val current = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(first, current) >= 56
    }

    fun next(completed: Int) = listOf("A", "B", "C")[completed % 3]

    fun week(
        start: Long,
        now: Long,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): Int {
        val first = java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
        val current = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (java.time.temporal.ChronoUnit.DAYS.between(first, current).coerceAtLeast(0) / 7 + 1)
            .toInt()
            .coerceAtMost(8)
    }

    fun validate(set: LoggedSet): Boolean =
        set.kg.isFinite() &&
            set.kg in 0.0..1500.0 &&
            set.reps in 1..3600 &&
            set.rir in 0..10 &&
            set.index in 0..9 &&
            set.loadUnit in setOf("total", "par côté")
}
