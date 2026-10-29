package be.moncoachsport

import org.junit.Assert.*
import org.junit.Test

class ProgramAdaptationTest {
    private fun training(id: String = "t", template: String = "A") =
        Training(id, template, 1000, program = Programs.CYCLE)

    @Test fun choicesKeepLearnedVariantsAndPreserveOtherSlots() {
        val prior = mapOf("F7" to "F8", "F2" to "F10")
        assertEquals(prior + ("F11" to ClubMachines.REAR_KICK), ProgramAdaptation.alternatives("B", prior, emptyMap()))
        assertEquals(mapOf("F7" to "F8", "F11" to ClubMachines.REAR_KICK), ProgramAdaptation.alternatives("B", prior, mapOf("B:F2" to "F2")))
        assertEquals(mapOf("F7" to "F8", "F2" to "F10", "F11" to ClubMachines.REAR_KICK),
            ProgramAdaptation.alternatives("B", mapOf("F7" to "F8"), mapOf("B:F2" to "F10", "A:F6" to "F19")))
    }

    @Test fun preferencesDoNotRewriteExistingTrainingOrAddExercises() {
        val old = training()
        val preferences = ProgramAdaptation.choices.associate { it.key to it.replacement }
        val next = old.copy(alternatives = ProgramAdaptation.alternatives("A", old.alternatives, preferences))
        assertEquals("F6", Programs.cyclePlan(old).last().id)
        assertEquals("F19", Programs.cyclePlan(next).last().id)
        listOf("A", "B", "C").forEach { template ->
            val t = training(template = template).copy(alternatives = ProgramAdaptation.alternatives(template, emptyMap(), preferences))
            assertEquals(6, Programs.cyclePlan(t).size)
            assertTrue(Programs.cyclePlan(t).all { it.baseSets == 2 })
        }
    }

    @Test fun activityCountsOnlyRealCompletedSetsInWindowAndCurrentCycle() {
        val now = 100L * 86400000
        val start = now - 3 * 86400000
        val sets = listOf(LoggedSet("F2", 0, 20.0, 10, 3, "m", "s"),
            LoggedSet("F2", 1, 20.0, 10, 3, "m", "s", done = false),
            LoggedSet("F19", 0, 10.0, 12, 3, "m", "s"))
        val valid = training().copy(started = start, finished = now, sets = sets, cardio = 15)
        val history = listOf(valid, valid, valid.copy(id = "pause", finished = null, paused = true),
            valid.copy(id = "old", started = start - 1), valid.copy(id = "future", finished = now + 1),
            valid.copy(id = "legacy", program = Programs.LEGACY),
            valid.copy(id = "stale", started = now - 10 * 86400000, finished = now - 8 * 86400000))
        assertEquals(TrainingActivity(1, mapOf("Pectoraux" to 1, "Tronc" to 1), 15), ProgramAdaptation.activity(history, start, now))
        assertEquals(1, ProgramAdaptation.priorPassages(valid.copy(id = "new", started = now + 1), history.filter { it.finished == now }, start))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownPreferencesAreRejected() {
        FeatureRules.validate(PersonalOptions(preferredVariants = mapOf("C:F15" to "F19")))
    }

    @Test fun progressionUsesTwoComparablePassagesDespiteOtherRepRanges() {
        val catalogue = (1..19).map { Exercise("F$it", "test", 2, 8, 12, 90, "rép", "", "", "", "", "") }
        fun passage(id: String, template: String, end: Long, reps: Int) = training(id, template).copy(finished = end,
            sets = List(2) { LoggedSet("F2", it, 20.0, reps, 4, "Chest", "3") })
        val current = training().copy(started = 9000)
        val p = Programs.cyclePlan(current).first { it.id == "F2" }
        val history = listOf(passage("a", "A", 2000, 12), passage("b", "B", 3000, 10), passage("c", "C", 4000, 12))
        assertTrue(Programs.canIncrease(current, p, history, catalogue, "Chest", "3", "total"))
        assertFalse(Programs.canIncrease(current, p, history + passage("failed", "A", 5000, 10), catalogue, "Chest", "3", "total"))
        assertFalse(Programs.canIncrease(current, p, history, catalogue, "Chest", "4", "total"))
        assertFalse(Programs.canIncrease(current, p, history, catalogue, "Chest", "3", "total", false))
    }
}
