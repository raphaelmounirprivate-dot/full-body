package be.moncoachsport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun RecoveryBanner(vm: CoachViewModel) {
    val unreadable by vm.unreadable.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null)
                scope.launch {
                    try {
                        val content = vm.exportJson()
                        withContext(Dispatchers.IO) {
                            val stream =
                                context.contentResolver.openOutputStream(uri)
                                    ?: error("Destination inaccessible")
                            stream.bufferedWriter().use { it.write(content) }
                        }
                        vm.message.value =
                            "Sauvegarde enregistrée avec les lignes brutes à récupérer."
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        vm.message.value = frenchError(e)
                    }
                }
        }
    if (unreadable.isNotEmpty())
        Surface(color = MaterialTheme.colorScheme.errorContainer) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "${unreadable.size} séance(s) illisible(s) mises de côté",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "Les autres séances restent accessibles. Les lignes brutes sont conservées.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { export.launch("Mon-Coach-Sport-recuperation.json") }) {
                    Text("Sauvegarder les données brutes")
                }
            }
        }
}
