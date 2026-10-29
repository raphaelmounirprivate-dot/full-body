package be.moncoachsport

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class RestAndRecordsTest {
    @Test fun pauseResumePreservesRemainingTimeAndNeverRevivesExpiredRest() {
        assertEquals(0L to 20000L, RestRules.toggle(120000, 0, 100000))
        assertEquals(220000L to 0L, RestRules.toggle(0, 20000, 200000))
        assertEquals(90000L to 0L, RestRules.toggle(90000, 0, 100000))
        assertEquals(0L to 0L, RestRules.toggle(0, 0, 100000))
    }
    private val e = Exercise("F1","Presse",2,10,15,120,"rép","","","","","")
    private val t = Training("current","A",1000,program=Programs.CYCLE)
    private val base = LoggedSet("F1",0,10.0,10,3,"Machine 1","Siège 2")
    private val history = listOf(t.copy(id="old",finished=2000,sets=listOf(base)))
    @Test fun adjustsRunningRestAndFinishesAtZeroWithoutNegativeTime() {
        assertEquals(150000L to 0L,RestRules.adjust(120000,0,30,100000))
        assertEquals(105000L to 0L,RestRules.adjust(120000,0,-15,100000))
        assertEquals(100000L to 0L,RestRules.adjust(105000,0,-15,100000))
    }
    @Test fun pausedAndExpiredRestNeverRestartByAdjustment() {
        assertEquals(0L to 40000L,RestRules.adjust(0,10000,30,100000))
        assertEquals(0L to 0L,RestRules.adjust(0,10000,-15,100000))
        assertEquals(90000L to 0L,RestRules.adjust(90000,0,30,100000))
        assertEquals(0L to 0L,RestRules.adjust(0,0,30,100000))
    }
    @Test fun firstMeasurementAndTiesAreNotRecords() {
        assertFalse(PersonalRecords.evaluate(t,base,emptyList(),e).achieved)
        assertFalse(PersonalRecords.evaluate(t,base,history,e).achieved)
    }
    @Test fun loadRecordAndRepetitionRecordUseDifferentBenchmarks() {
        assertEquals(RecordResult(true,false,false),PersonalRecords.evaluate(t,base.copy(kg=20.0),history,e))
        assertEquals(RecordResult(false,true,false),PersonalRecords.evaluate(t,base.copy(reps=12),history,e))
        assertEquals(RecordResult(true,false,false),PersonalRecords.evaluate(t,base.copy(kg=20.0,reps=20),history,e))
    }
    @Test fun differentProtocolsAndIncompleteHistoryDoNotEarnRecords() {
        listOf(base.copy(machine="Other"),base.copy(setting="Siège 3"),base.copy(loadUnit="par côté"),
            base.copy(exerciseId="F2"),base.copy(machine="")).forEach {
            assertFalse(PersonalRecords.evaluate(t,it.copy(kg=20.0),history,e).achieved)
        }
        assertFalse(PersonalRecords.evaluate(t,base.copy(kg=20.0),history.map { it.copy(program=Programs.LEGACY) },e).achieved)
        assertFalse(PersonalRecords.evaluate(t,base.copy(kg=20.0),history,e,false).achieved)
        assertFalse(PersonalRecords.evaluate(t,base.copy(kg=20.0,done=false),history,e).achieved)
    }
    @Test fun laterTiedSeriesDoesNotEraseAnEarlierRecordAndCorrectionIsComparedProperly() {
        val first = base.copy(kg=20.0)
        val second = first.copy(index=1)
        val active = t.copy(sets=listOf(first,second))
        assertTrue(PersonalRecords.forStored(active,first,history,e).weight)
        assertFalse(PersonalRecords.forStored(active,second,history,e).achieved)
        assertFalse(PersonalRecords.evaluate(active,first.copy(kg=15.0),history,e).achieved)
        assertFalse(PersonalRecords.evaluate(active,first,history,e).achieved)
    }
    @Test fun plankCanEarnADurationRecordButNeverAWeightRecord() {
        val plank = e.copy(unit="s")
        assertEquals(RecordResult(false,false,true),PersonalRecords.evaluate(t,base.copy(reps=45),history,plank))
        assertFalse(PersonalRecords.evaluate(t,base.copy(kg=20.0),history,plank).achieved)
    }
    private val today = LocalDate.of(2026,10,27)
    private val ended = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    @Test fun widgetSuggestsNextFinishedRotationAndUsesCompletionDate() {
        val model = WorkoutWidgetModel.from(listOf(t.copy(template="B",finished=ended)),Programs.CYCLE,3,today=today)
        assertEquals("Prochaine séance C",model.title)
        assertEquals(1,model.completed)
        assertEquals(3,model.target)
    }
    @Test fun widgetResumesActiveOrPausedWorkoutWithoutFinishingIt() {
        assertEquals("Reprendre la séance A",WorkoutWidgetModel.from(listOf(t),Programs.CYCLE,3,today=today).title)
        val paused = t.copy(paused=true)
        val model = WorkoutWidgetModel.from(listOf(paused),Programs.CYCLE,3,today=today)
        assertEquals("Reprendre la séance A",model.title)
        assertEquals(0,model.completed)
    }
    @Test fun widgetStartsAtAAndFlagsUnreadableHistory() {
        assertEquals("Prochaine séance A",WorkoutWidgetModel.from(emptyList(),Programs.CYCLE,3,today=today).title)
        val model = WorkoutWidgetModel.from(emptyList(),Programs.CYCLE,3,true,today)
        assertEquals("Voir mes séances",model.title)
        assertTrue(model.goal.startsWith("Historique incomplet"))
    }
}
