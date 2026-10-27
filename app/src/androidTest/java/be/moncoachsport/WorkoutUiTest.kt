package be.moncoachsport

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WorkoutUiTest {
    @Test fun restartingCycleKeepsCompletedSessionAndItsWeek() {
        val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[CoachViewModel::class.java]
        vm.start("A")
        compose.waitUntil(10000) { vm.active != null }
        vm.log(LoggedSet("F1", 0, 20.0, 12, 3, "Presse", "Siège 3"))
        compose.waitUntil(10000) { vm.active?.sets?.size == 1 }
        vm.finish("Conserver", "0", "5", "", "Vélo", "3")
        compose.waitUntil(10000) { vm.completed.size == 1 }
        val previous = vm.completed.single()
        val old = System.currentTimeMillis() - 57L * 86400000
        vm.setCycleStart(old)
        compose.waitUntil(10000) { vm.cycleStart.value == old }
        compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex))
            .performScrollToNode(hasText("Recommencer en semaine 1"))
        compose.onNodeWithText("Recommencer en semaine 1").performClick()
        compose.onNodeWithText("Recommencer", useUnmergedTree = true).performClick()
        compose.waitUntil(10000) { vm.cycleStart.value > old + 86400000 }
        org.junit.Assert.assertEquals(previous, vm.completed.single())
        org.junit.Assert.assertEquals(1, Rules.week(vm.cycleStart.value, System.currentTimeMillis()))
    }
    private fun chooseSession(template: String) {
        compose.onNodeWithContentDescription("Changer de séance").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Choisir la séance $template").performClick()
        compose.waitUntil(10000) {
            val vm =
                androidx.lifecycle.ViewModelProvider(compose.activity)[CoachViewModel::class.java]
            vm.active?.template == template
        }
        compose.waitForIdle()
    }

    private fun hideKeyboard() {
        compose.runOnUiThread {
            compose.activity
                .getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
            compose.activity.currentFocus?.clearFocus()
        }
        compose.waitForIdle()
    }

    @Test
    fun switchingSessionsPreservesSetsDraftsAndStopsRest() {
        compose.onNodeWithText("Commencer ma séance").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("kg total").fetchSemanticsNodes().isNotEmpty()
        }
        val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[CoachViewModel::class.java]
        compose.onAllNodesWithText("kg total")[0].performTextReplacement("25")
        hideKeyboard()
        compose.onNodeWithText("Valider la série").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.active?.sets?.size == 1 }
        compose.onAllNodesWithText("kg total")[0].performScrollTo().performTextReplacement("28")
        hideKeyboard()
        compose.waitUntil(10000) { vm.active?.drafts?.get("F1-1-kg") == "28" }
        val original = vm.active!!
        chooseSession("B")
        org.junit.Assert.assertEquals(
            original.copy(paused = true),
            vm.sessions.value.first { it.id == original.id },
        )
        org.junit.Assert.assertEquals(0L to 0L, vm.timer.value)
        org.junit.Assert.assertTrue(vm.completed.isEmpty())
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Séance").performClick()
        compose.onNodeWithText("Séance B").assertExists()
        chooseSession("A")
        org.junit.Assert.assertEquals(original, vm.active)
        compose.onNodeWithText("Série 1 · enregistrée").assertExists()
        compose.onAllNodesWithText("kg total")[0].assertTextContains("28")
        org.junit.Assert.assertEquals(2, vm.sessions.value.size)
        vm.saveDraft("F1-1-kg", "999", "a-stale-session")
        vm.saveDraft("test-completion", "ok", original.id)
        compose.waitUntil(10000) { vm.active?.drafts?.get("test-completion") == "ok" }
        org.junit.Assert.assertEquals("28", vm.active!!.drafts["F1-1-kg"])
    }

    @Test
    fun choosingBeforeStartingAndSharedExercisesKeepSeparateDrafts() {
        compose.onNodeWithText("Séance").performClick()
        chooseSession("C")
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("kg total").fetchSemanticsNodes().isNotEmpty()
        }
        openExercise("Tirage vertical")
        compose.onAllNodesWithText("kg total")[0].performTextReplacement("30")
        hideKeyboard()
        chooseSession("A")
        openExercise("Tirage vertical")
        compose.onAllNodesWithText("kg total")[0].assert(hasText("30", substring = true).not())
        chooseSession("C")
        openExercise("Tirage vertical")
        compose.onAllNodesWithText("kg total")[0].assertTextContains("30")
    }

    @Test
    fun correctedGuideShowsTheLegCurlCaption() {
        compose.onNodeWithText("Exercices").performClick()
        compose.onNodeWithText("Jambes").performClick()
        compose
            .onNodeWithContentDescription("Voir la fiche de Leg curl assis")
            .performScrollTo()
            .performClick()
        compose
            .onNodeWithText("ce siège ne représente pas la machine", substring = true)
            .assertExists()
        compose.onNodeWithText("F4, page 12", substring = true).assertExists()
    }

    @Test
    fun guideVariantReplacesPlankWithItsOwnRange() {
        compose.onNodeWithText("Commencer ma séance").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Séance A").fetchSemanticsNodes().isNotEmpty()
        }
        openExercise("Planche")
        compose
            .onNode(
                SemanticsMatcher.keyIsDefined(
                    androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex
                )
            )
            .performScrollToNode(hasText("Remplacer ce mouvement"))
        compose.onNodeWithText("Remplacer ce mouvement").performClick()
        compose.onNodeWithText("Crunch à la poulie haute").performClick()
        compose.waitUntil(10000) {
            compose
                .onAllNodesWithText("2 × 12–15 rép · repos 60–90 s")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose
            .onNodeWithContentDescription("Voir la fiche de Crunch à la poulie haute")
            .assertExists()
    }

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearTestData() {
        val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[CoachViewModel::class.java]
        vm.eraseAll()
        compose.waitUntil(10000) {
            vm.message.value == "Toutes les données et préférences ont été supprimées" &&
                vm.databaseState.value.loaded &&
                vm.databaseState.value.sessions.isEmpty() &&
                vm.sessions.value.isEmpty() &&
                vm.cycleStart.value == 0L
        }
        vm.message.value = ""
        compose.waitForIdle()
    }

    @Test
    fun fourTabsAreAccessible() {
        listOf("Accueil", "Séance", "Progression", "Exercices").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText("Exercices").performClick()
        compose.onNodeWithText("Les mouvements.").assertExists()
    }

    @Test
    fun sessionCanRecordASetAndSurviveActivityRecreation() {
        compose.onNodeWithText("Séance").performClick()
        compose.onNodeWithText("Commencer la séance A").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("kg total").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("kg total")[0].performTextReplacement("20")
        compose.runOnUiThread {
            compose.activity
                .getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
            compose.activity.currentFocus?.clearFocus()
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("Valider la série")[0].performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Série 1 · enregistrée").fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Séance").performClick()
        compose.onNodeWithText("Série 1 · enregistrée").assertExists()
    }

    @Test
    fun optionalMovementCanBeSkippedAndRestored() {
        compose.onNodeWithText("Séance").performClick()
        compose.onNodeWithText("Commencer la séance A").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Séance A").fetchSemanticsNodes().isNotEmpty()
        }
        openExercise("Élévations latérales à la poulie")
        compose
            .onNode(
                SemanticsMatcher.keyIsDefined(
                    androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex
                )
            )
            .performScrollToNode(hasText("Sauter cet exercice"))
        compose.onNodeWithText("Sauter cet exercice").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Réintégrer cet exercice").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Exercice sauté pour cette séance.").performScrollTo()
        compose.onNodeWithText("Exercice sauté pour cette séance.").assertExists()
        compose
            .onNodeWithContentDescription("Voir la fiche de Élévations latérales à la poulie")
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Installer").assertExists()
        compose.onNodeWithText("Fermer la fiche").performClick()
        compose.onNodeWithText("Réintégrer cet exercice").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Sauter cet exercice").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Sauter cet exercice").assertExists()
    }

    @Test
    fun homeTabReturnsHomeAfterStartingAWorkout() {
        compose.onNodeWithText("Commencer ma séance").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Séance A").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Exercices").performClick()
        compose.onNodeWithText("Accueil").performClick()
        compose.onNodeWithText("À votre rythme.").assertIsDisplayed()
        compose.onNodeWithText("Reprendre ma séance").assertExists()
    }

    @Test
    fun exerciseSheetOpensFromWorkoutAndKeepsTheDraft() {
        compose.onNodeWithText("Commencer ma séance").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("kg total").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("kg total")[0].performTextReplacement("25")
        compose.runOnUiThread {
            compose.activity
                .getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
            compose.activity.currentFocus?.clearFocus()
        }
        compose
            .onNodeWithContentDescription("Voir la fiche de Presse à cuisses")
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Installer").assertExists()
        compose.onNodeWithContentDescription("Illustration de Presse à cuisses").assertExists()
        compose.onNodeWithText("Fermer la fiche").performClick()
        compose.onAllNodesWithText("kg total")[0].assertTextContains("25")
    }

    @Test
    fun exerciseSheetAlsoOpensFromCatalogue() {
        compose.onNodeWithText("Exercices").performClick()
        compose.onNodeWithContentDescription("Voir la fiche de Presse à cuisses").performClick()
        compose.onNodeWithText("Installer").assertExists()
        compose.onNodeWithText("Fermer la fiche").performClick()
        compose.onNodeWithText("Les mouvements.").assertExists()
    }

    private fun openExercise(name: String) {
        compose
            .onNode(
                SemanticsMatcher.keyIsDefined(
                    androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex
                )
            )
            .performScrollToNode(hasContentDescription("Ouvrir $name"))
        compose.onNodeWithContentDescription("Ouvrir $name").performClick()
    }

    @Test
    fun foldingExercisesKeepsDraftAndRecordedSeriesCanBeCorrected() {
        compose.onNodeWithText("Commencer ma séance").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("kg total").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("kg total")[0].performTextReplacement("25")
        compose.runOnUiThread {
            compose.activity
                .getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
            compose.activity.currentFocus?.clearFocus()
        }
        openExercise("Chest press guidée")
        openExercise("Presse à cuisses")
        compose.onAllNodesWithText("kg total")[0].assertTextContains("25")
        compose.onAllNodesWithText("Valider la série")[0].performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Série 1 · enregistrée").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Corriger la série 1").performScrollTo().performClick()
        compose.onAllNodesWithText("kg total")[0].assertTextContains("25")
        compose.onAllNodesWithText("kg total")[0].performTextReplacement("30")
        compose.runOnUiThread {
            compose.activity
                .getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
            compose.activity.currentFocus?.clearFocus()
        }
        compose.onNodeWithText("Enregistrer la correction").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose
                .onAllNodesWithText("30.0 kg total", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
