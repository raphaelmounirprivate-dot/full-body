package be.moncoachsport

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.*

class GuidedUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(compose.activity)[CoachViewModel::class.java]
    @Before fun reset() {
        vm.eraseAll()
        compose.waitUntil(10000) { vm.message.value == "Toutes les données et préférences ont été supprimées" &&
            vm.databaseState.value.loaded && vm.databaseState.value.sessions.isEmpty() &&
            vm.sessions.value.isEmpty() && vm.personal.value == PersonalOptions() && vm.measurements.value.isEmpty() }
        compose.waitForIdle()
        vm.message.value = ""
    }
    private fun scrollTo(text: String) {
        compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex))
            .performScrollToNode(hasText(text))
    }
    private fun hideKeyboard() {
        compose.runOnUiThread {
            compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0)
            compose.activity.currentFocus?.clearFocus()
        }
    }
    private fun progressTab() = compose.onNodeWithText(
        if (compose.activity.resources.configuration.fontScale > 1.2f) "Progrès" else "Progression"
    ).performClick()
    @Test fun guidedValidationAdvancesOnlyAfterSuccessfulSaveAndCanReturnToFullSession() {
        vm.setPersonal { it.copy(guided=true) }
        compose.waitUntil(10000) { vm.personal.value.guided }
        compose.onNodeWithText("Commencer ma séance").performClick()
        compose.waitUntil(10000) { vm.active != null }
        scrollTo("Valider → série suivante")
        compose.onNodeWithText("Valider → série suivante").performClick()
        compose.waitForIdle()
        Assert.assertTrue(vm.active!!.sets.isEmpty())
        scrollTo("kg total")
        compose.onNodeWithText("kg total").performTextReplacement("25")
        hideKeyboard()
        scrollTo("Valider → série suivante")
        compose.onNodeWithText("Valider → série suivante").performClick()
        compose.waitUntil(10000) { vm.active?.sets?.size == 1 }
        compose.onNodeWithText("Valider → exercice suivant").performScrollTo().assertExists()
        scrollTo("Voir toute la séance et le bilan")
        compose.onNodeWithText("Voir toute la séance et le bilan").performClick()
        compose.waitUntil(10000) { !vm.personal.value.guided }
        Assert.assertEquals(25.0,vm.active!!.sets.single().kg,0.0)
    }
    @Test fun keepScreenIsLimitedToActiveWorkoutAndCanBeDisabled() {
        fun hasKeep(view: android.view.View): Boolean = view.keepScreenOn ||
            (view is android.view.ViewGroup && (0 until view.childCount).any { hasKeep(view.getChildAt(it)) })
        compose.onNodeWithText("Commencer ma séance").performClick()
        compose.waitUntil(10000) { vm.active != null }
        compose.waitForIdle()
        compose.runOnUiThread { Assert.assertTrue(hasKeep(compose.activity.window.decorView)) }
        vm.setPersonal { it.copy(keepScreen=false) }
        compose.waitUntil(10000) { !vm.personal.value.keepScreen }
        compose.waitForIdle()
        compose.runOnUiThread { Assert.assertFalse(hasKeep(compose.activity.window.decorView)) }
        vm.setPersonal { it.copy(keepScreen=true) }
        compose.waitUntil(10000) { vm.personal.value.keepScreen }
        progressTab()
        compose.waitForIdle()
        compose.runOnUiThread { Assert.assertFalse(hasKeep(compose.activity.window.decorView)) }
    }
    @Test fun bodyMeasurementCanBeSavedThroughTheScreen() {
        progressTab()
        scrollTo("Mes mesures et mes courbes")
        compose.onNodeWithText("Mes mesures et mes courbes").performClick()
        compose.onNodeWithText("Poids (kg), facultatif").performTextReplacement("72,5")
        hideKeyboard()
        compose.onNodeWithText("Enregistrer la mesure").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.measurements.value.size == 1 }
        Assert.assertEquals(72.5,vm.measurements.value.single().kg!!,0.0)
    }
}
