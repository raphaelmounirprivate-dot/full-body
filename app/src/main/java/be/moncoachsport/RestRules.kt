package be.moncoachsport

object RestRules {
    fun toggle(end: Long, paused: Long, now: Long): Pair<Long, Long> = when {
        end > now -> 0L to (end - now)
        end == 0L && paused > 0 -> (now + paused) to 0L
        else -> end to paused
    }

    fun adjust(end: Long, paused: Long, seconds: Int, now: Long): Pair<Long, Long> {
        require(seconds in listOf(30, -15)) { "Ajustement de repos inconnu" }
        val delta = seconds * 1000L
        return when {
            end > now -> (end + delta).coerceIn(now, now + 86_400_000L) to 0L
            end == 0L && paused > 0 -> 0L to (paused + delta).coerceIn(0L, 86_400_000L)
            else -> end to paused // An expired or canceled rest must never restart.
        }
    }
}
