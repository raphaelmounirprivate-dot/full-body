package be.moncoachsport

import org.junit.Assert.*
import org.junit.Test

class ClubMachinesTest {
    private val source = (1..19).map { Exercise("F$it", "Source $it", 2, 10, 15, 90, "rép", "", "", "", "", "source-$it.webp") }
    private val catalogue = ClubMachines.catalogue(source)

    @Test fun newBUsesRearKickWhileOldSnapshotAndSourceKeepHipThrust() {
        val old = Training("old", "B", 1000, program = Programs.CYCLE)
        val next = old.copy(id = "new", alternatives = ProgramAdaptation.alternatives("B", emptyMap(), emptyMap()))
        assertEquals("F11", Programs.plan(old, catalogue)[3].id)
        assertEquals(ClubMachines.REAR_KICK, Programs.plan(next, catalogue)[3].id)
        assertEquals(6, Programs.plan(next, catalogue).size)
        val exercise = Programs.exercise(Programs.plan(next, catalogue)[3], next, catalogue)
        assertEquals(2, exercise.sets)
        assertEquals("rép / jambe", exercise.unit)
        assertEquals(90, exercise.rest)
        assertEquals(source.first { it.id == "F11" }, catalogue.first { it.id == "F11" })
    }

    @Test fun explicitHipChoiceDisablesDefaultRearKick() {
        val choices = ProgramAdaptation.alternatives("B", mapOf("F11" to ClubMachines.REAR_KICK), mapOf("B:F11" to "F11"))
        assertFalse(choices.containsKey("F11"))
        assertFalse(ProgramAdaptation.alternatives("A", emptyMap(), emptyMap()).containsValue(ClubMachines.REAR_KICK))
    }

    @Test fun machineNotesAreSeparateAndRearKickDoesNotReuseHipPictureOrVideo() {
        assertEquals(20, catalogue.size)
        assertTrue(catalogue.first { it.id == "F2" }.accessory.contains("CHEST PRESS Technogym"))
        assertTrue(catalogue.first { it.id == "F9" }.accessory.contains("LOW ROW Technogym"))
        assertTrue(source.all { it.accessory.isEmpty() })
        assertTrue(ClubMachines.rearKick.image.isEmpty())
        assertEquals(0, ClubMachines.rearKick.sourcePage)
    }

    @Test fun rearKickProgressionAndRecordsCannotUseHipThrustHistory() {
        val t = Training("rear", "B", 3000, program = Programs.CYCLE, alternatives = mapOf("F11" to ClubMachines.REAR_KICK))
        val hip = Training("hip", "B", 1000, finished = 2000, program = Programs.CYCLE,
            sets = List(2) { LoggedSet("F11", it, 40.0, 15, 4, "Poste", "1") })
        val p = Programs.plan(t, catalogue)[3]
        assertFalse(Programs.canIncrease(t, p, listOf(hip, hip.copy(id = "hip2")), catalogue, "Poste", "1", "total"))
        val set = LoggedSet(ClubMachines.REAR_KICK, 0, 20.0, 12, 4, "Poste", "1")
        assertFalse(PersonalRecords.evaluate(t, set, listOf(hip), ClubMachines.rearKick).achieved)
    }

    @Test fun videoAndLevelKeysAreScopedToCurrentProgramme() {
        FeatureRules.validate(PersonalOptions(exerciseLevels = mapOf("cycle:R1" to "Débutant"),
            videos = mapOf("cycle:R1" to "https://youtu.be/abcdefghijk"), preferredVariants = mapOf("B:F11" to "R1")))
        assertTrue(runCatching { FeatureRules.validate(PersonalOptions(exerciseLevels = mapOf("legacy:R1" to "Débutant"))) }.isFailure)
    }

    @Test fun activityCountsRearKickWithoutCountingBothLegsTwice() {
        val t = Training("rear", "B", 1000, finished = 2000, program = Programs.CYCLE,
            alternatives = mapOf("F11" to ClubMachines.REAR_KICK), sets = List(2) { LoggedSet("R1", it, 20.0, 12, 3, "Rear", "1") })
        assertEquals(mapOf("Fessiers" to 2), ProgramAdaptation.activity(listOf(t), 1, 3000).sets)
    }
}
