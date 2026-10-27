package be.moncoachsport

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column

@Composable
internal fun RecordBanner(vm: CoachViewModel, notice: RecordNotice) {
    Snackbar(action = { TextButton(onClick={vm.dismissRecord(notice.token)}) { Text("Fermer le record") } }) {
        Column {
            Text("Record personnel · ${notice.result.label()}")
            Text(notice.exerciseName)
        }
    }
}
