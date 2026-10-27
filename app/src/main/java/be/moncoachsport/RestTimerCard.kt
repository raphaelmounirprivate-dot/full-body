package be.moncoachsport

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
internal fun RestTimerCard(vm: CoachViewModel, end: Long, paused: Long) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end, paused) {
        now = System.currentTimeMillis()
        while (end > now) {
            delay(500)
            now = System.currentTimeMillis()
        }
    }
    val left = if (end > 0) ((end - now + 999) / 1000).coerceAtLeast(0) else paused / 1000
    Panel("Repos · ${left/60}:${(left%60).toString().padStart(2, '0')}") {
        Row {
            TextButton(
                onClick = {
                    if (end > 0) vm.setTimer(0, (end - System.currentTimeMillis()).coerceAtLeast(0))
                    else vm.setTimer(System.currentTimeMillis() + paused)
                }
            ) {
                Text(if (end > 0) "Pause" else "Reprendre")
            }
            TextButton(onClick = { vm.setTimer(0) }) { Text("Fermer") }
        }
    }
}
