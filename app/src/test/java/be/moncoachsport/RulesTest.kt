package be.moncoachsport

import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    @Test fun newCycleBecomesAvailableAfterEightCalendarWeeks() {
        val zone = java.time.ZoneId.of("Europe/Paris")
        val start = java.time.ZonedDateTime.of(2026, 10, 19, 12, 0, 0, 0, zone)
        val before = start.plusDays(55).toInstant().toEpochMilli()
        val after = start.plusDays(56).toInstant().toEpochMilli()
        assertFalse(Rules.cycleHasEnded(start.toInstant().toEpochMilli(), before, zone))
        assertTrue(Rules.cycleHasEnded(start.toInstant().toEpochMilli(), after, zone))
        assertEquals(8, Rules.week(start.toInstant().toEpochMilli(), after, zone))
    }

    @Test fun unstartedCycleNeverOffersRestart() {
        assertFalse(Rules.cycleHasEnded(0, System.currentTimeMillis()))
    }

    private fun passage(machine: String = "Presse 1", reps: Int = 15, rir: Int = 2) =
        List(2) { LoggedSet("F1", it, 40.0, reps, rir, machine, "Siège 3") }

    @Test
    fun rotationCyclesOnlyWithCompletedCount() {
        assertEquals(listOf("A", "B", "C", "A"), (0..3).map(Rules::next))
    }

    @Test
    fun invalidNumbersRejected() {
        assertFalse(Rules.validate(passage()[0].copy(kg = Double.NaN)))
        assertFalse(Rules.validate(passage()[0].copy(reps = 0)))
        assertFalse(Rules.validate(passage()[0].copy(rir = 11)))
    }
}
