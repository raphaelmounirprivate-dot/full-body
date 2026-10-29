package be.moncoachsport

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FocusedWorkoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(compose.activity)[CoachViewModel::class.java]
    private fun reset() {
        vm.eraseAll()
        compose.waitUntil(10000) { vm.message.value == "Toutes les données et préférences ont été supprimées" &&
            vm.databaseState.value.loaded && vm.sessions.value.isEmpty() && vm.personal.value == PersonalOptions() && vm.autoRest.value && vm.timer.value == (0L to 0L) }
        vm.message.value = ""
        compose.waitForIdle()
    }
    private fun scroll(text: String) = compose.onNode(SemanticsMatcher.keyIsDefined(
        androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex)).performScrollToNode(hasText(text))

    @Test fun singleExerciseAndFixedClockAdvanceAfterTwoPersistedSets() {
        reset()
        assertTrue(vm.personal.value.guided)
        compose.onNodeWithText("Commencer ma séance").performClick()
        compose.waitUntil(10000) { vm.active != null }
        compose.onNodeWithText("Chest press guidée").assertDoesNotExist()
        scroll("Valider → série suivante")
        compose.onNodeWithText("Valider → série suivante").performClick()
        assertTrue(vm.active!!.sets.isEmpty())
        assertEquals(0L to 0L, vm.timer.value)
        scroll("kg total")
        compose.onNodeWithContentDescription("Charge de Presse à cuisses, série 1").performTextReplacement("25")
        scroll("Valider → série suivante")
        compose.onNodeWithText("Valider → série suivante").performClick()
        compose.waitUntil(10000) { vm.active?.sets?.size == 1 && vm.timer.value.first > System.currentTimeMillis() }
        compose.onNodeWithText("+30 s").assertIsDisplayed()
        compose.onNodeWithText("Pause").assertIsDisplayed()
        scroll("kg total")
        compose.onNodeWithContentDescription("Charge de Presse à cuisses, série 2").performTextReplacement("25")
        scroll("Valider → exercice suivant")
        compose.onNodeWithText("Valider → exercice suivant").performClick()
        compose.waitUntil(10000) { vm.active?.sets?.size == 2 }
        scroll("Chest press guidée")
        compose.onNodeWithText("Presse à cuisses").assertDoesNotExist()
        compose.onNodeWithText("Chest press guidée").assertExists()
        scroll("Voir toute la séance et le bilan")
        compose.onNodeWithText("+30 s").assertIsDisplayed()
        compose.onNodeWithText("Pause").assertIsDisplayed()
        val id = vm.active!!.id
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { vm.active?.id == id && vm.personal.value.guided }
        scroll("Chest press guidée")
        compose.onNodeWithText("+30 s").assertIsDisplayed()
        reset()
    }

    @Test fun configuredThirdSetIsCompletedBeforeNextExercise() {
        reset()
        vm.setCycleStart(System.currentTimeMillis() - 28L * 86400000)
        compose.waitUntil(10000) { vm.cycleStart.value > 0 }
        vm.start("A")
        compose.waitUntil(10000) { vm.active != null }
        vm.extra("F1", recoveryConfirmed = true)
        compose.waitUntil(10000) { "F1" in vm.active!!.extraSets }
        repeat(2) { vm.log(LoggedSet("F1", it, 25.0, 12, 3, "Presse", "1")) }
        compose.waitUntil(10000) { vm.active?.sets?.size == 2 }
        assertEquals("F1" to 2, FeatureRules.nextSet(vm.active!!, vm.guideExercises))
        vm.log(LoggedSet("F1", 2, 25.0, 12, 3, "Presse", "1"))
        compose.waitUntil(10000) { vm.active?.sets?.size == 3 }
        assertEquals("F2" to 0, FeatureRules.nextSet(vm.active!!, vm.guideExercises))
        assertTrue(vm.timer.value.first > System.currentTimeMillis())
        reset()
    }

    @Test fun correctingSavedSeriesDoesNotRestartRest() {
        reset()
        vm.start("A")
        compose.waitUntil(10000) { vm.active != null }
        val set = LoggedSet("F1", 0, 25.0, 12, 3, "Presse", "1")
        vm.log(set)
        compose.waitUntil(10000) { vm.active?.sets?.size == 1 && vm.timer.value.first > 0 }
        val end = vm.timer.value.first
        vm.log(set.copy(kg = 30.0))
        compose.waitUntil(10000) { vm.active?.sets?.singleOrNull()?.kg == 30.0 }
        assertEquals(end, vm.timer.value.first)
        assertEquals("F1" to 1, FeatureRules.nextSet(vm.active!!, vm.guideExercises))
        reset()
    }

    @Test fun stalePauseAndCloseCannotChangeNewRest() = runBlocking {
        reset()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SportDatabase::class.java).build()
        val repo = SportRepository(context, db)
        try {
            val first = System.currentTimeMillis() + 120000
            val second = first + 60000
            repo.setTimer(second, 0)
            assertEquals(second to 0L, repo.cancelTimer(first, 0))
            assertEquals(second to 0L, repo.toggleTimer(first, 0))
            val paused = repo.toggleTimer(second, 0)
            assertEquals(0L, paused.first)
            assertTrue(paused.second > 0)
            assertEquals(paused, repo.cancelTimer(second, 0))
            assertEquals(0L to 0L, repo.cancelTimer(0, paused.second))
            val expired = System.currentTimeMillis() - 1000
            repo.setTimer(expired, 0)
            assertEquals(expired to 0L, repo.toggleTimer(expired, 0))
        } finally { repo.setTimer(0, 0); db.close() }
    }

    @Test fun upgradeEnablesFocusOnceAndPreservesLaterChoicesAndSessions() = runBlocking {
        reset()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SportDatabase::class.java).build()
        val repo = SportRepository(context, db)
        val options = PersonalOptions(guided = false, weeklyGoal = 2, level = "Avancé", preferredVariants = mapOf("B:F11" to "F11"))
        val old = Training("migration-kept", "B", 1000, program = Programs.CYCLE, paused = true)
        try {
            repo.save(old)
            context.applicationContext.preferences.edit {
                it.remove(booleanPreferencesKey("focusedWorkoutV1"))
                it[stringPreferencesKey("personal")] = PersonalCodec.encode(options).toString()
                it[booleanPreferencesKey("autoRest")] = false
            }
            repo.preferCurrentGuide()
            assertEquals(options.copy(guided = true), repo.personal.first())
            assertTrue(repo.autoRest.first())
            assertEquals(old, repo.currentSessions().single())
            repo.setPersonal(options)
            repo.setAutoRest(false)
            repo.preferCurrentGuide()
            assertEquals(options, repo.personal.first())
            assertFalse(repo.autoRest.first())
            repo.eraseAll()
            repo.setPersonal(options)
            repo.preferCurrentGuide()
            assertEquals(options, repo.personal.first())
        } finally { db.close(); reset() }
    }
}
