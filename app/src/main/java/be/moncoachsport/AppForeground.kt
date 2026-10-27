package be.moncoachsport

import java.util.concurrent.atomic.AtomicInteger

internal object AppForeground {
    private val activities = AtomicInteger()
    val visible get() = activities.get() > 0
    fun started() { activities.incrementAndGet() }
    fun stopped() { activities.updateAndGet { (it - 1).coerceAtLeast(0) } }
}
