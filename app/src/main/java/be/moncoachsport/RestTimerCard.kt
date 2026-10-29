package be.moncoachsport

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
internal fun RestTimerCard(vm: CoachViewModel, end: Long, paused: Long) {
    var now by remember(end, paused) { mutableStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(end, paused, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            now = System.currentTimeMillis()
            while (end > now) {
                delay(250)
                now = System.currentTimeMillis()
            }
            if (end > 0) {
                delay(3000)
                vm.dismissRest(end) // Compare-and-clear: never close a newer rest.
            }
        }
    }
    val finished = end > 0 && now >= end
    val left = if (end > 0) ((end - now + 999) / 1000).coerceAtLeast(0) else paused / 1000
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (finished) "Repos terminé ✓"
                    else "Repos · ${left/60}:${(left%60).toString().padStart(2,'0')}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (!finished)
                    TextButton(onClick = { vm.toggleRest(end, paused) }) {
                        Text(if (end > 0) "Pause" else "Reprendre")
                    }
                IconButton(onClick = { vm.cancelRest(end, paused) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Fermer le repos")
                }
            }
            if (!finished)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { vm.adjustRest(-15) }) { Text("−15 s") }
                    TextButton(onClick = { vm.adjustRest(30) }) { Text("+30 s") }
                }
        }
    }
}
