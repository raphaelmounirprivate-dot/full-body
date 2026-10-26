package be.moncoachsport

import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    private val exercise = Exercise("F1", "Presse", 2, 10, 15, 120, "rép", "", "", "", "", "")

    private fun passage(machine: String = "Presse 1", reps: Int = 15, rir: Int = 2) =
        List(2) { LoggedSet("F1", it, 40.0, reps, rir, machine, "Siège 3") }

    @Test
    fun rotationCyclesOnlyWithCompletedCount() {
        assertEquals(listOf("A", "B", "C", "A"), (0..3).map(Rules::next))
    }

    @Test
    fun progressionNeedsTwoCompletePassages() {
        assertFalse(Rules.canIncrease(exercise, listOf(passage()), 1))
        assertTrue(Rules.canIncrease(exercise, listOf(passage(), passage()), 1))
    }

    @Test
    fun changedMachineBlocksProgression() {
        assertFalse(Rules.canIncrease(exercise, listOf(passage(), passage("Presse 2")), 1))
    }

    @Test
    fun missingSetOrReserveBlocksProgression() {
        assertFalse(Rules.canIncrease(exercise, listOf(passage(), passage().take(1)), 1))
        assertFalse(Rules.canIncrease(exercise, listOf(passage(), passage(rir = 0)), 1))
    }

    @Test
    fun invalidNumbersRejected() {
        assertFalse(Rules.validate(passage()[0].copy(kg = Double.NaN)))
        assertFalse(Rules.validate(passage()[0].copy(reps = 0)))
        assertFalse(Rules.validate(passage()[0].copy(rir = 11)))
    }

    @Test
    fun timeNeverRecommendsLoadIncrease() {
        assertFalse(Rules.canIncrease(exercise.copy(unit = "s"), listOf(passage(), passage()), 1))
    }
}
