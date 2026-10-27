package be.moncoachsport

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.*

class RestUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(compose.activity)[CoachViewModel::class.java]
    @Before fun reset() {
        vm.eraseAll()
        compose.waitUntil(10000) { vm.message.value=="Toutes les données et préférences ont été supprimées" &&
            vm.databaseState.value.loaded && vm.sessions.value.isEmpty() && vm.databaseState.value.sessions.isEmpty() }
        vm.message.value=""
        compose.waitForIdle()
    }
    private fun openWorkout() {
        compose.onNodeWithText("Commencer ma séance").performClick()
        compose.waitUntil(10000) { vm.active != null }
    }
    @Test fun finishedCardHasNoPauseAndDisappearsAutomatically() {
        openWorkout()
        vm.setTimer(System.currentTimeMillis()+1000)
        compose.waitUntil(5000) { compose.onAllNodesWithText("Repos terminé ✓").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Repos terminé ✓").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pause").assertDoesNotExist()
        compose.waitUntil(6000) { vm.timer.value.first==0L }
        compose.onNodeWithText("Repos terminé ✓").assertDoesNotExist()
    }
    @Test fun timerButtonsAdjustRunningAndPausedRest() {
        openWorkout()
        val end = System.currentTimeMillis()+120000
        vm.setTimer(end)
        compose.waitUntil(5000) { vm.timer.value.first==end }
        compose.onNodeWithText("+30 s").performScrollTo().performClick()
        compose.waitUntil(5000) { vm.timer.value.first==end+30000 }
        compose.onNodeWithText("−15 s").performClick()
        compose.waitUntil(5000) { vm.timer.value.first==end+15000 }
        compose.onNodeWithText("Pause").performClick()
        compose.waitUntil(5000) { vm.timer.value.first==0L && vm.timer.value.second>0 }
        val paused = vm.timer.value.second
        compose.onNodeWithText("+30 s").performClick()
        compose.waitUntil(5000) { vm.timer.value.second==paused+30000 }
        compose.onNodeWithText("Reprendre").performClick()
        compose.waitUntil(5000) { vm.timer.value.first>System.currentTimeMillis() && vm.timer.value.second==0L }
    }
    @Test fun newRecordAppearsOnlyAfterSeriesWasSaved() {
        vm.start("A")
        compose.waitUntil(10000) { vm.active != null }
        vm.log(LoggedSet("F1",0,10.0,12,3,"Presse test","Siège 3"))
        compose.waitUntil(10000) { vm.active?.sets?.size==1 }
        Assert.assertNull(vm.personalRecord.value)
        vm.finish("","0","5","","Vélo","3")
        compose.waitUntil(10000) { vm.completed.size==1 }
        vm.start("A")
        compose.waitUntil(10000) { vm.active != null }
        compose.onNodeWithText("Séance").performClick()
        vm.log(LoggedSet("F1",0,20.0,12,3,"Presse test","Siège 3"))
        compose.waitUntil(10000) { vm.personalRecord.value?.result?.weight==true && vm.active?.sets?.singleOrNull()?.kg==20.0 }
        compose.onAllNodesWithText("Record personnel · charge").onFirst().assertExists()
        compose.onNodeWithText("Fermer le record").performClick()
        compose.waitUntil(5000) { vm.personalRecord.value==null }
    }
}
