package be.moncoachsport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    override fun onStart() { super.onStart(); AppForeground.started() }
    override fun onStop() { AppForeground.stopped(); super.onStop() }
    private var workoutRequest by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("openWorkout", false)) workoutRequest++
        setContent { CoachApp(workoutRequest = workoutRequest) }
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("openWorkout", false)) workoutRequest++
    }
}

@Composable
fun CoachApp(vm: CoachViewModel = viewModel(), workoutRequest: Int = 0) {
    val dark by vm.dark.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    LaunchedEffect(workoutRequest) { if (workoutRequest > 0) nav.navigate("Séance") { launchSingleTop = true } }
    val back by nav.currentBackStackEntryAsState()
    val tabs = listOf("Accueil", "Séance", "Progression", "Exercices")
    val icons =
        listOf(
            Icons.Outlined.Home,
            Icons.Outlined.PlayArrow,
            Icons.Outlined.DateRange,
            Icons.Outlined.List,
        )
    MaterialTheme(colorScheme = coachColors(dark)) {
        Scaffold(
            topBar = { RecoveryBanner(vm) },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = back?.destination?.route == t,
                            onClick = {
                                nav.navigate(t) {
                                    launchSingleTop = true
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    restoreState = t != "Accueil"
                                }
                            },
                            icon = { Icon(icons[i], null) },
                            label = {
                                Text(
                                    if (
                                        t == "Progression" &&
                                            androidx.compose.ui.platform.LocalDensity.current
                                                .fontScale > 1.2f
                                    )
                                        "Progrès"
                                    else t,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            },
                        )
                    }
                }
            },
        ) { pad ->
            NavHost(nav, startDestination = "Accueil", modifier = Modifier.padding(pad)) {
                composable("Accueil") {
                    CoachHome(
                        vm,
                        sessions,
                        onExercises = { nav.navigate("Exercices") { launchSingleTop = true } },
                        onProgress = { nav.navigate("Progression") { launchSingleTop = true } },
                    ) {
                        vm.start()
                        nav.navigate("Séance")
                    }
                }
                composable("Séance") { Workout(vm, sessions) }
                composable("Progression") { Progress(vm, sessions) }
                composable("Exercices") {
                    val program by vm.program.collectAsStateWithLifecycle()
                    Catalogue(vm.exercisesFor(program), program, vm)
                }
            }
        }
        if (message.isNotBlank())
            AlertDialog(
                onDismissRequest = { vm.message.value = "" },
                title = { Text("Mon Coach Sport") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { vm.message.value = "" }) { Text("Compris") }
                },
            )
    }
}
