package be.moncoachsport

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*

@Composable
internal fun Summary(vm: CoachViewModel, t: Training) {
    var notes by rememberSaveable(t.id) { mutableStateOf(t.drafts["notes"] ?: t.notes) }
    var cardio by rememberSaveable(t.id) { mutableStateOf(t.drafts["cardio"] ?: "0") }
    var energy by rememberSaveable(t.id) { mutableStateOf(t.drafts["energy"] ?: "5") }
    var pain by rememberSaveable(t.id) { mutableStateOf(t.drafts["pain"] ?: "") }
    var machine by rememberSaveable(t.id) { mutableStateOf(t.drafts["cardioMachine"] ?: "Vélo") }
    var intensity by rememberSaveable(t.id) { mutableStateOf(t.drafts["intensity"] ?: "3") }
    var confirm by remember { mutableStateOf(false) }
    Panel("Bilan de séance") {
        if (t.program == Programs.CYCLE)
            Text(
                "Cardio proposé en semaine ${t.week} : ${Programs.phase(t.week).cardio}. Adaptez à votre récupération."
            )
        OutlinedTextField(
            machine,
            {
                machine = it
                vm.saveDraft("cardioMachine", it, t.id)
            },
            label = { Text("Appareil cardio") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                cardio,
                {
                    cardio = it
                    vm.saveDraft("cardio", it, t.id)
                },
                "Minutes",
                Modifier.weight(1f),
            )
            NumberField(
                intensity,
                {
                    intensity = it
                    vm.saveDraft("intensity", it, t.id)
                },
                "Effort /10",
                Modifier.weight(1f),
            )
        }
        NumberField(
            energy,
            {
                energy = it
                vm.saveDraft("energy", it, t.id)
            },
            "Énergie /10",
            Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            pain,
            {
                pain = it
                vm.saveDraft("pain", it, t.id)
            },
            label = { Text("Inconfort / douleur") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            notes,
            {
                notes = it
                vm.saveDraft("notes", it, t.id)
            },
            label = { Text("Notes / prochaine décision") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Terminer la séance")
        }
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Terminer cette séance ?") },
            text = {
                Text(
                    "Les séries non validées seront ignorées. La rotation passera à la séance suivante."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        vm.finish(notes, cardio, energy, pain, machine, intensity, t.id)
                    }
                ) {
                    Text("Terminer")
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Continuer") } },
        )
}
