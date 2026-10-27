package be.moncoachsport

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@Composable
fun AlertSettings(vm: CoachViewModel) {
    val context = LocalContext.current
    val enabled by vm.alerts.collectAsStateWithLifecycle()
    val options by vm.personal.collectAsStateWithLifecycle()
    var precise by remember { mutableStateOf(RestScheduler(context).preciseAllowed()) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                precise = RestScheduler(context).preciseAllowed()
                RestScheduler(context).reconcile()
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.setAlerts(granted)
            if (!granted)
                vm.message.value =
                    "Notifications non autorisées. Le minuteur reste disponible dans la séance."
        }
    Panel("Rappel de repos") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Notification et vibration", Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { value ->
                    if (
                        value &&
                            Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                    )
                        request.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.setAlerts(value)
                },
            )
        }
        Text(
            if (precise) "Rappels précis autorisés. Le prochain repos apparaît parmi les alarmes Android. Son et vibration se règlent aussi dans les paramètres Android."
            else "Sans autorisation de rappel précis, Android peut retarder la notification en veille.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (!precise && Build.VERSION.SDK_INT >= 31) OutlinedButton(onClick = {
            runCatching {
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:" + context.packageName)))
            }.onFailure { vm.message.value = "Ouvrez les paramètres Android > Applications > Accès spécial > Alarmes et rappels." }
        }) { Text("Autoriser les rappels précis") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rappel de fin de cycle", Modifier.weight(1f))
            Switch(options.cycleReminder, { value -> vm.setPersonal { it.copy(cycleReminder = value) } },
                modifier=Modifier.semantics { contentDescription="Rappel de fin de cycle" })
        }
        if (options.cycleReminder && !enabled) Text("Activez aussi les notifications ci-dessus.")
    }
}

@Composable
fun WorkoutSettings(vm: CoachViewModel) {
    val options by vm.personal.collectAsStateWithLifecycle()
    Panel("Confort pendant la séance") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Garder l’écran allumé", Modifier.weight(1f))
            Switch(options.keepScreen, { value -> vm.setPersonal { it.copy(keepScreen = value) } },
                modifier=Modifier.semantics { contentDescription="Garder l’écran allumé" })
        }
        Text("Uniquement sur l’écran de séance avec un entraînement actif. Le verrouillage manuel reste possible.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enchaînement guidé", Modifier.weight(1f))
            Switch(options.guided, { value -> vm.setPersonal { it.copy(guided = value) } },
                modifier=Modifier.semantics { contentDescription="Enchaînement guidé" })
        }
        Text("Après validation réussie : prochaine série, exercice suivant puis bilan. Le repos reste disponible.")
        Text("Mon niveau (indicatif)")
        FeatureRules.levels.forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(options.level == level, { vm.setPersonal { it.copy(level = level) } },
                    modifier=Modifier.semantics { contentDescription="Mon niveau : $level" })
                Text(level)
            }
        }
        Text("Ce choix ne modifie ni les séries ni les charges du guide.")
    }
}

@Composable
fun GuideExportButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val save =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null)
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.assets
                                .open("guide/Programme_Jims_Namur_images_uniformisees.pdf")
                                .use { input ->
                                    context.contentResolver.openOutputStream(uri)?.use {
                                        input.copyTo(it)
                                    }
                                }
                        }
                    }
                }
        }
    OutlinedButton(
        onClick = { save.launch("Programme_Jims_Namur_images_uniformisees.pdf") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Enregistrer le guide complet · 31 pages")
    }
}
