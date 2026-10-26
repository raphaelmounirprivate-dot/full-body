package be.moncoachsport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SessionChooser(
    vm: CoachViewModel,
    sessions: List<Training>,
    program: String,
    current: Training?,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(
        onClick = { open = true },
        modifier =
            Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Changer de séance" },
    ) {
        Text(if (current == null) "Choisir A, B ou C" else "Changer")
        Icon(Icons.Outlined.KeyboardArrowDown, null)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Choisir une séance") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(Programs.title(program), fontWeight = FontWeight.SemiBold)
                    Text(
                        if (current == null) "Choisissez la séance à commencer ou à reprendre."
                        else
                            "La séance en cours reste en pause avec ses séries et ses saisies. Le repos en cours est arrêté."
                    )
                    listOf("A", "B", "C").forEach { template ->
                        val pending =
                            sessions.firstOrNull {
                                it.program == program &&
                                    it.template == template &&
                                    it.finished == null
                            }
                        OutlinedButton(
                            enabled = current?.template != template,
                            onClick = {
                                vm.start(template)
                                open = false
                            },
                            modifier =
                                Modifier.fillMaxWidth().heightIn(min = 64.dp).semantics {
                                    contentDescription = "Choisir la séance $template"
                                },
                        ) {
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    "Séance $template",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    when {
                                        current?.template == template -> "En cours"
                                        pending != null ->
                                            "Reprendre · ${if (program == Programs.CYCLE) "Semaine ${pending.week} · " else ""}${pending.sets.count { it.done }} séries validées"
                                        template == Programs.next(program, sessions) ->
                                            "Commencer · prochaine dans la rotation"
                                        else -> "Commencer cette séance"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Fermer") } },
        )
    }
}
