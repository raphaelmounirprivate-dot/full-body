package be.moncoachsport

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class FeatureRulesTest {
    private val catalogue = (1..19).map { Exercise("F$it", "Exercice $it", 2, 10, 15, 120, "rép", "", "", "", "", "") }
    private fun training() = Training("a", "A", 1000, program = Programs.CYCLE)
    @Test fun guidedSequenceFollowsOnlyPersistedValidatedSets() {
        val t = training()
        assertEquals("F1" to 0, FeatureRules.nextSet(t, catalogue))
        val s = LoggedSet("F1", 0, 20.0, 12, 3, "", "")
        assertEquals("F1" to 0, FeatureRules.nextSet(t.copy(sets = listOf(s.copy(done = false))), catalogue))
        assertEquals("F1" to 1, FeatureRules.nextSet(t.copy(sets = listOf(s)), catalogue))
        assertEquals("F2" to 0, FeatureRules.nextSet(t.copy(sets = listOf(s, s.copy(index = 1))), catalogue))
    }
    @Test fun completedAndSkippedPlanHasNoNextSet() {
        val t = training().copy(skipped = setOf("F5"))
        val sets = Programs.plan(t, catalogue).filter { it.id !in t.skipped }.flatMap { p ->
            (0 until Programs.exercise(p,t,catalogue).sets).map { LoggedSet(p.id,it,0.0,10,3,"","") }
        }
        assertNull(FeatureRules.nextSet(t.copy(sets = sets), catalogue))
    }
    @Test fun variantUsesItsOwnIdAndExtraSeriesIsNotLost() {
        val t = training().copy(week = 5, extraSets = setOf("F1"), alternatives = mapOf("F6" to "F19"))
        val sets = listOf(LoggedSet("F1",0,20.0,12,3,"",""), LoggedSet("F1",1,20.0,12,3,"",""))
        assertEquals("F1" to 2, FeatureRules.nextSet(t.copy(sets = sets), catalogue))
        assertTrue(Programs.plan(t,catalogue).any { it.id == "F19" })
    }
    @Test fun pointsUseUniqueFinishedSessionsAndLocalCompletionDate() {
        val zone = ZoneId.of("Europe/Paris")
        val today = LocalDate.of(2026,10,27)
        fun at(d: String) = LocalDate.parse(d).atStartOfDay(zone).toInstant().toEpochMilli()
        val done = training().copy(finished = at("2026-10-26"))
        assertEquals(1, FeatureRules.weeklyCount(listOf(done, done, done.copy(id="paused",finished=null,paused=true),
            done.copy(id="old",finished=at("2026-10-25")), done.copy(id="future",finished=at("2026-10-28"))), today, zone))
    }
    @Test fun measurementRejectsEmptyFutureAndNonFiniteValues() {
        val today = LocalDate.of(2026,10,27)
        listOf(BodyMeasurement("2026-10-27",null,null), BodyMeasurement("2026-10-28",70.0,null),
            BodyMeasurement("2026-10-27",Double.NaN,null), BodyMeasurement("2026-10-27",null,101.0),
            BodyMeasurement("invalid",70.0,null)).forEach {
            assertTrue(runCatching { FeatureRules.validate(it,today) }.isFailure)
        }
        FeatureRules.validate(BodyMeasurement("2026-10-26",70.5,null),today)
        FeatureRules.validate(BodyMeasurement("2026-10-27",null,20.0),today)
    }
    @Test fun videoLinksRejectLookalikeHostsAndExecutableSchemes() {
        assertTrue(FeatureRules.validYoutube("https://youtu.be/qIUio2uC2lw"))
        assertTrue(FeatureRules.validYoutube("https://www.youtube.com/watch?v=qIUio2uC2lw&t=10"))
        listOf("javascript:alert(1)","https://youtube.com.evil.test/watch?v=qIUio2uC2lw",
            "https://youtube.com@evil.test/watch?v=qIUio2uC2lw","http://youtu.be/qIUio2uC2lw",
            "https://www.youtube.com/results?search_query=leg").forEach { assertFalse(FeatureRules.validYoutube(it)) }
    }
    @Test fun homeOrderAndGoalAreValidated() {
        FeatureRules.validate(PersonalOptions())
        assertTrue(runCatching { FeatureRules.validate(PersonalOptions(homeOrder=listOf("stats","stats","last","challenge"))) }.isFailure)
        assertTrue(runCatching { FeatureRules.validate(PersonalOptions(weeklyGoal=0)) }.isFailure)
    }
}
