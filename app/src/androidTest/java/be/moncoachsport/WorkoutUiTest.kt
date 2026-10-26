package be.moncoachsport

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WorkoutUiTest {
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
        compose
            .onNode(
                SemanticsMatcher.keyIsDefined(
                    androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex
                )
            )
            .performScrollToNode(hasText("Sauter cet exercice"))
        compose.onNodeWithText("Sauter cet exercice").performClick()
        compose.onNodeWithText("Exercice sauté pour cette séance.").assertExists()
        compose
            .onNodeWithContentDescription("Voir la fiche de Élévations latérales à la poulie")
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Installer").assertExists()
        compose.onNodeWithText("Fermer la fiche").performClick()
        compose.onNodeWithText("Réintégrer cet exercice").performClick()
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
}
