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

@Composable
fun AlertSettings(vm: CoachViewModel) {
    val context = LocalContext.current
    val enabled by vm.alerts.collectAsStateWithLifecycle()
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
            "Android peut retarder le rappel en veille. La vibration se règle aussi dans les paramètres de notification du téléphone.",
            style = MaterialTheme.typography.bodySmall,
        )
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
