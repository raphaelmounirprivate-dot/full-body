package be.moncoachsport

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdaptationTest {
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
    private fun scroll(text: String) = compose.onNode(SemanticsMatcher.keyIsDefined(
        androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex)).performScrollToNode(hasText(text))

    @Test fun editorSavesChoiceAndOnlyNewSessionsUseIt() {
        reset()
        vm.start("B")
        compose.waitUntil(10000) { vm.active?.template == "B" }
        val originalId = vm.active!!.id
        scroll("Adapter mes prochaines séances")
        compose.onNodeWithText("Adapter mes prochaines séances").performClick()
        compose.onNodeWithContentDescription("B · Développé incliné aux poulies").performClick()
        compose.onNodeWithText("Enregistrer pour les prochaines séances").performClick()
        compose.waitUntil(10000) { vm.personal.value.preferredVariants["B:F2"] == "F10" }
        assertFalse(vm.active!!.alternatives.containsKey("F2"))
        vm.start("A")
        compose.waitUntil(10000) { vm.active?.template == "A" }
        vm.start("B")
        compose.waitUntil(10000) { vm.active?.id == originalId }
        assertFalse(vm.active!!.alternatives.containsKey("F2"))
        vm.eraseAll()
        compose.waitUntil(10000) { vm.sessions.value.isEmpty() && vm.personal.value == PersonalOptions() }
        vm.setPersonal { it.copy(preferredVariants = mapOf("B:F2" to "F10")) }
        compose.waitUntil(10000) { vm.personal.value.preferredVariants.isNotEmpty() }
        vm.start("B")
        compose.waitUntil(10000) { vm.active?.alternatives?.get("F2") == "F10" }
        assertEquals(6, Programs.cyclePlan(vm.active!!).size)
        reset()
    }

    @Test fun extraSeriesRequiresRecoveryConfirmationAndCanBeRemoved() {
        reset()
        vm.setCycleStart(System.currentTimeMillis() - 28L * 86400000)
        compose.waitUntil(10000) { vm.cycleStart.value > 0 }
        vm.start("A")
        compose.waitUntil(10000) { vm.active != null }
        vm.extra("F1")
        compose.waitUntil(10000) { vm.message.value?.contains("Confirmez") == true }
        assertTrue(vm.active!!.extraSets.isEmpty())
        vm.message.value = ""
        compose.waitForIdle()
        compose.onNodeWithText("Reprendre ma séance").performClick()
        scroll("Évaluer une 3e série facultative")
        compose.onNodeWithText("Évaluer une 3e série facultative").performClick()
        compose.onNodeWithText("Essayer une troisième série").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Geste maîtrisé et volume stabilisé").performClick()
        compose.onNodeWithContentDescription("Bonne récupération sans douleur inhabituelle").performClick()
        compose.onNodeWithText("Essayer une troisième série").performClick()
        compose.waitUntil(10000) { "F1" in vm.active!!.extraSets }
        scroll("Revenir à deux séries")
        compose.onNodeWithText("Revenir à deux séries").performClick()
        compose.waitUntil(10000) { vm.active!!.extraSets.isEmpty() }
        reset()
    }

    @Test fun preferenceRoundTripOldBackupAndInvalidImportAreSafe() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SportDatabase::class.java).build()
        val repo = SportRepository(context, db)
        val old = repo.personal.first()
        val ids = Programs.ids.associateWith { (1..19).map { "F$it" }.toSet() }
        try {
            val settings = PersonalOptions(preferredVariants = mapOf("B:F2" to "F10"))
            assertEquals(settings, PersonalCodec.decode(PersonalCodec.encode(settings)))
            repo.setPersonal(settings)
            val backup = HistoryCsv.asBackup(emptyList()).put("version", 5)
                .put("personal", PersonalCodec.encode(PersonalOptions()).apply { remove("preferredVariants") })
            repo.restore(backup.toString(), ids)
            assertEquals(settings.preferredVariants, repo.personal.first().preferredVariants)
            backup.getJSONObject("personal").put("preferredVariants", JSONObject().put("C:F15", "F19"))
            assertTrue(runCatching { repo.restore(backup.toString(), ids) }.isFailure)
            assertEquals(settings.preferredVariants, repo.personal.first().preferredVariants)
            backup.getJSONObject("personal").put("preferredVariants", JSONObject())
            repo.restore(backup.toString(), ids)
            assertTrue(repo.personal.first().preferredVariants.isEmpty())
        } finally { repo.setPersonal(old); db.close() }
    }
}
