package be.moncoachsport

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RearKickTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(compose.activity)[CoachViewModel::class.java]
    private fun reset() {
        vm.eraseAll()
        compose.waitUntil(10000) { vm.message.value == "Toutes les données et préférences ont été supprimées" &&
            vm.databaseState.value.loaded && vm.sessions.value.isEmpty() && vm.personal.value == PersonalOptions() }
        vm.message.value = ""
        vm.setPersonal { it.copy(guided = false) }
        compose.waitUntil(10000) { !vm.personal.value.guided }
        compose.waitForIdle()
    }
    private fun scroll(matcher: SemanticsMatcher) = compose.onNode(SemanticsMatcher.keyIsDefined(
        androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex)).performScrollToNode(matcher)

    @Test fun newBHasSeparateRearKickSheetAndPerLegProtocol() {
        reset()
        vm.start("B")
        compose.waitUntil(10000) { vm.active?.template == "B" }
        assertEquals("R1", vm.active!!.alternatives["F11"])
        assertFalse(vm.exercisesFor(Programs.LEGACY).any { it.id == "R1" })
        compose.onNodeWithText("Reprendre ma séance").performClick()
        scroll(hasContentDescription("Ouvrir Rear Kick machine"))
        compose.onNodeWithContentDescription("Ouvrir Rear Kick machine").performClick()
        scroll(hasText("2 × 10–15 rép / jambe · repos 90 s"))
        compose.onNodeWithText("2 × 10–15 rép / jambe · repos 90 s").assertExists()
        scroll(hasContentDescription("Voir la fiche de Rear Kick machine"))
        compose.onNodeWithContentDescription("Voir la fiche de Rear Kick machine").performClick()
        compose.onNodeWithText("plaque REAR KICK", substring = true).assertExists()
        compose.onNodeWithText("Adaptation personnelle · R1.", substring = true).performScrollTo().assertExists()
        compose.onAllNodesWithText("Archive :", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Fermer la fiche").performClick()
        reset()
    }

    @Test fun pausedHipThrustSessionIsResumedUnchangedAndOnlyNextBUsesRearKick() {
        reset()
        val repo = SportRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val old = Training("old-hip-b", "B", System.currentTimeMillis() - 60000,
            program = Programs.CYCLE, paused = true,
            sets = listOf(LoggedSet("F11", 0, 20.0, 12, 3, "Hip thrust", "1")))
        runBlocking {
            repo.save(old)
            assertEquals(old, repo.currentSessions().single())
        }
        vm.start("B")
        compose.waitUntil(10000) { vm.active?.id == old.id }
        assertEquals(old.copy(paused = false), vm.active)
        assertEquals("F11", Programs.plan(vm.active!!, vm.guideExercises)[3].id)
        vm.start("A")
        compose.waitUntil(10000) { vm.active?.template == "A" }
        vm.start("B")
        compose.waitUntil(10000) { vm.active?.id == old.id }
        assertEquals(old.sets, vm.active!!.sets)
        vm.finish("", "0", "5", "", "Vélo", "3")
        compose.waitUntil(10000) { vm.completed.any { it.id == old.id } }
        vm.start("B")
        compose.waitUntil(10000) { vm.active?.id != old.id && vm.active?.template == "B" }
        assertEquals("R1", vm.active!!.alternatives["F11"])
        assertEquals("F11", vm.completed.first { it.id == old.id }.sets.single().exerciseId)
        vm.chooseVariant("F11", "F11")
        compose.waitUntil(10000) { vm.active?.alternatives?.containsKey("F11") == false }
        assertEquals("F11", Programs.plan(vm.active!!, vm.guideExercises)[3].id)
        reset()
    }

    @Test fun rearKickRoundTripsJsonCsvAndRestoreWithoutReinterpretingHipThrust() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SportDatabase::class.java).build()
        val repo = SportRepository(context, db)
        val rear = Training("rear-b", "B", 1000, program = Programs.CYCLE, paused = true,
            alternatives = mapOf("F11" to "R1"), sets = List(2) { LoggedSet("R1", it, 10.0, 12, 3, "Rear Kick", "Repère 1") })
        val hip = Training("hip-b", "B", 1000, finished = 2000, program = Programs.CYCLE,
            sets = listOf(LoggedSet("F11", 0, 20.0, 12, 3, "Hip thrust", "1")))
        val ids = Programs.ids.associateWith { program -> (1..19).map { "F$it" }.toSet() + if (program == Programs.CYCLE) setOf("R1") else emptySet() }
        try {
            assertEquals(rear, Codec.decode(Codec.encode(rear)))
            val data = listOf(rear, hip)
            assertEquals(data, HistoryCsv.decode(HistoryCsv.encode(data)))
            repo.restore(HistoryCsv.asBackup(data).toString(), ids)
            assertEquals(data.toSet(), repo.currentSessions().toSet())
            val withoutRear = ids.mapValues { it.value - "R1" }
            assertTrue(runCatching { repo.restore(HistoryCsv.asBackup(data).toString(), withoutRear) }.isFailure)
            assertEquals(data.toSet(), repo.currentSessions().toSet())
        } finally { db.close() }
    }
}
