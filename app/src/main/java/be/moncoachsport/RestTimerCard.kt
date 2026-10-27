package be.moncoachsport

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
    val left = if (end > 0) ((end-now+999)/1000).coerceAtLeast(0) else paused/1000
    Panel(if (finished) "Repos terminé ✓" else "Repos · ${left/60}:${(left%60).toString().padStart(2,'0')}") {
        if (finished) {
            Text("La prochaine série est prête.")
            TextButton(onClick = { vm.dismissRest(end) }) { Text("Fermer") }
        } else {
            Row {
                TextButton(onClick = { vm.adjustRest(-15) }) { Text("−15 s") }
                TextButton(onClick = { vm.adjustRest(30) }) { Text("+30 s") }
            }
            Row {
                TextButton(onClick = {
                    if (end > 0) vm.setTimer(0,(end-System.currentTimeMillis()).coerceAtLeast(0))
                    else vm.setTimer(System.currentTimeMillis()+paused)
                }) { Text(if (end > 0) "Pause" else "Reprendre") }
                TextButton(onClick = { vm.setTimer(0) }) { Text("Fermer") }
            }
        }
    }
}
