package be.moncoachsport

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class ProgramsTest {
    @Test
    fun guideVariantsUseTheirOwnPrescriptions() {
        val fly =
            Programs.plan(training("C").copy(alternatives = mapOf("F2" to "F16")), exercises)
                .first { it.id == "F16" }
        assertEquals(Prescription("F16", 10, 15, 90), fly)
        val curl =
            Programs.plan(training("A").copy(alternatives = mapOf("F6" to "F19")), exercises).last()
        assertEquals(Prescription("F19", 12, 15, 60, 90), curl)
        val extension =
            Programs.plan(training("B").copy(alternatives = mapOf("F7" to "F18")), exercises)
                .first()
        assertEquals(Prescription("F18", 12, 15, 60, 90), extension)
    }

    @Test
    fun plankNeverSuggestsAddingWeight() {
        val current = training()
        val p = Programs.cycle.getValue("A").last()
        val previous =
            List(2) { i ->
                current.copy(
                    id = "plank$i",
                    finished = 2000,
                    sets = List(2) { LoggedSet("F6", it, 0.0, 30, 3, "Tapis", "Sol") },
                )
            }
        assertFalse(Programs.canIncrease(current, p, previous, exercises, "Tapis", "Sol", "total"))
    }

    @Test
    fun rowVariantReplacesOnlyItsSlot() {
        val plan = Programs.plan(training("B").copy(alternatives = mapOf("F9" to "F14")), exercises)
        assertEquals(6, plan.size)
        assertEquals("F14", plan[1].id)
        assertFalse(plan.any { it.id == "F9" })
        assertFalse(Programs.variants.any { it.slot == "F4" && it.replacement == "F18" })
    }

    private val exercises =
        (1..19).map {
            Exercise(
                "F$it",
                "Exercice $it",
                3,
                8,
                12,
                90,
                if (it == 6) "s" else "rép",
                "",
                "",
                "",
                "",
                "",
            )
        }

    private fun training(template: String = "A", week: Int = 1) =
        Training("current", template, 1000L, program = Programs.CYCLE, week = week)

    private fun passage(
        id: String,
        template: String = "A",
        rir: Int = 3,
        machine: String = "Presse 1",
        unit: String = "total",
        week: Int = 1,
    ) =
        training(template, week)
            .copy(
                id = id,
                finished = 2000L,
                sets =
                    List(2) {
                        LoggedSet("F1", it, 40.0, 15, rir, machine, "Siège 3", loadUnit = unit)
                    },
            )

    @Test
    fun cycleAHasExactOrderAndTwoBaseSets() {
        val plan = Programs.plan(training(), exercises)
        assertEquals(listOf("F1", "F2", "F3", "F4", "F5", "F6"), plan.map { it.id })
        assertTrue(plan.all { it.baseSets == 2 })
        assertEquals(listOf("F5"), plan.filter { it.optional }.map { it.id })
        assertEquals(Prescription("F6", 15, 30, 45, 60, unit = "s"), plan.last())
    }

    @Test
    fun chestRangesAreSpecificToEachTemplate() {
        assertEquals(8, Programs.cycle.getValue("A").first { it.id == "F2" }.min)
        assertEquals(10, Programs.cycle.getValue("B").first { it.id == "F2" }.min)
        assertEquals(12, Programs.cycle.getValue("C").first { it.id == "F2" }.max)
    }

    @Test
    fun hackReplacesSquatRatherThanAddingIt() {
        val plan = Programs.plan(training("B").copy(alternatives = mapOf("F7" to "F8")), exercises)
        assertEquals(6, plan.size)
        assertEquals("F8", plan.first().id)
        assertFalse(plan.any { it.id == "F7" })
    }

    @Test
    fun optionalMovementsMatchBrief() {
        assertEquals(
            setOf("F12", "F13"),
            Programs.cycle.getValue("B").filter { it.optional }.map { it.id }.toSet(),
        )
        assertEquals(
            setOf("F15"),
            Programs.cycle.getValue("C").filter { it.optional }.map { it.id }.toSet(),
        )
    }

    @Test
    fun extrasAreRestrictedToFirstThreeFromWeekFive() {
        assertFalse(Programs.extraAllowed(training(week = 4), 0))
        assertTrue(Programs.extraAllowed(training(week = 5), 2))
        assertFalse(Programs.extraAllowed(training(week = 5), 3))
        assertFalse(Programs.extraAllowed(training().copy(program = Programs.LEGACY), 0))
    }

    @Test
    fun rotationsAreIndependentAcrossPrograms() {
        val old = Training("old", "A", 1, finished = 2)
        assertEquals("A", Programs.next(Programs.CYCLE, listOf(old)))
        assertEquals("B", Programs.next(Programs.LEGACY, listOf(old)))
        assertEquals("A", Programs.next(Programs.CYCLE, listOf(training())))
    }

    @Test
    fun calendarWeekSurvivesAutumnClockChange() {
        val zone = ZoneId.of("Europe/Paris")
        val start = ZonedDateTime.of(2026, 10, 19, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val before = ZonedDateTime.of(2026, 10, 25, 23, 30, 0, 0, zone).toInstant().toEpochMilli()
        val next = ZonedDateTime.of(2026, 10, 26, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(1, Rules.week(start, before, zone))
        assertEquals(2, Rules.week(start, next, zone))
        assertEquals(8, Rules.week(start, next + 200L * 86400000, zone))
    }

    @Test
    fun progressionRequiresPlannedReserveInBothPassages() {
        val current = training()
        val p = Programs.cycle.getValue("A").first()
        assertTrue(
            Programs.canIncrease(
                current,
                p,
                listOf(passage("1"), passage("2")),
                exercises,
                "Presse 1",
                "Siège 3",
                "total",
            )
        )
        assertFalse(
            Programs.canIncrease(
                current,
                p,
                listOf(passage("1"), passage("2", rir = 2)),
                exercises,
                "Presse 1",
                "Siège 3",
                "total",
            )
        )
    }

    @Test
    fun changingMachineOrUnitBlocksLoadSuggestion() {
        val current = training()
        val p = Programs.cycle.getValue("A").first()
        assertFalse(
            Programs.canIncrease(
                current,
                p,
                listOf(passage("1"), passage("2", machine = "Presse 2")),
                exercises,
                "Presse 1",
                "Siège 3",
                "total",
            )
        )
        assertFalse(
            Programs.canIncrease(
                current,
                p,
                listOf(passage("1"), passage("2", unit = "par côté")),
                exercises,
                "Presse 1",
                "Siège 3",
                "total",
            )
        )
    }

    @Test
    fun addingAThirdSetRequiresNewEvidence() {
        val p = Programs.cycle.getValue("A").first()
        val current = training(week = 5).copy(extraSets = setOf("F1"))
        assertFalse(
            Programs.canIncrease(
                current,
                p,
                listOf(passage("1"), passage("2")),
                exercises,
                "Presse 1",
                "Siège 3",
                "total",
            )
        )
    }

    @Test
    fun csvParserPreservesQuotedNewlinesAndSemicolons() {
        assertEquals(
            listOf(listOf("a", "b;c", "ligne\n\"deux\"")),
            HistoryCsv.parse("a;\"b;c\";\"ligne\n\"\"deux\"\"\""),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun csvParserRejectsUnterminatedQuote() {
        HistoryCsv.parse("a;\"b")
    }

    @Test(expected = IllegalArgumentException::class)
    fun csvParserRejectsTrailingTextAfterQuote() {
        HistoryCsv.parse("a;\"b\"oops")
    }
}
