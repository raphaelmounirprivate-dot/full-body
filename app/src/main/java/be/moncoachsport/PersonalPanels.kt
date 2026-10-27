package be.moncoachsport

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

internal val homeLabels = mapOf("stats" to "Mes statistiques", "shortcuts" to "Mes raccourcis",
    "last" to "Dernier entraînement", "challenge" to "Mon objectif")

@Composable
internal fun ChallengeCard(vm: CoachViewModel, sessions: List<Training>) {
    val options by vm.personal.collectAsStateWithLifecycle()
    val count = FeatureRules.weeklyCount(sessions)
    Panel("Mon objectif de la semaine") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Activer les points", Modifier.weight(1f))
            Switch(options.challenges, { value -> vm.setPersonal { it.copy(challenges = value) } },
                modifier=Modifier.semantics { contentDescription="Activer les points" })
        }
        Text("Du lundi à aujourd’hui · objectif personnel, sans classement")
        if (options.challenges) {
            Text("$count / ${options.weeklyGoal} séances terminées · ${count * 10} points")
            LinearProgressIndicator(progress = { (count.toFloat() / options.weeklyGoal).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth())
            if (count >= options.weeklyGoal) Text("Objectif atteint. La récupération fait aussi partie des progrès.")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = options.weeklyGoal > 1, onClick = { vm.setPersonal { it.copy(weeklyGoal = (it.weeklyGoal - 1).coerceAtLeast(1)) } }) { Text("−") }
            Text("Objectif : ${options.weeklyGoal} séances")
            TextButton(enabled = options.weeklyGoal < 7, onClick = { vm.setPersonal { it.copy(weeklyGoal = (it.weeklyGoal + 1).coerceAtMost(7)) } }) { Text("+") }
        }
        Text("10 points par séance terminée, jamais par charge ou par série. Gardez les jours de récupération du programme.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun HomeCardSettings(vm: CoachViewModel) {
    val options by vm.personal.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("Organiser mon accueil") }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Mes cartes d’accueil") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Le programme et les réglages restent accessibles. Déplacez ou masquez les autres cartes.")
            options.homeOrder.forEachIndexed { index, key ->
                Text(homeLabels.getValue(key), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(key !in options.hidden, { visible -> vm.setPersonal {
                        it.copy(hidden = if (visible) it.hidden - key else it.hidden + key)
                    } }, modifier=Modifier.semantics { contentDescription="Afficher " + homeLabels.getValue(key) })
                    Text("Afficher", Modifier.weight(1f))
                }
                Row {
                    TextButton(enabled = index > 0, onClick = { vm.setPersonal {
                        val list = it.homeOrder.toMutableList()
                        val from = list.indexOf(key)
                        if (from > 0) java.util.Collections.swap(list, from, from - 1)
                        it.copy(homeOrder = list)
                    } }) { Text("Monter") }
                    TextButton(enabled = index < options.homeOrder.lastIndex, onClick = { vm.setPersonal {
                        val list = it.homeOrder.toMutableList()
                        val from = list.indexOf(key)
                        if (from < list.lastIndex) java.util.Collections.swap(list, from, from + 1)
                        it.copy(homeOrder = list)
                    } }) { Text("Descendre") }
                }
            }
        } }, confirmButton = { TextButton(onClick = { open = false }) { Text("Fermer") } })
}

@Composable
internal fun BodyPanel(vm: CoachViewModel) {
    val measurements by vm.measurements.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var day by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var kg by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    Panel("Poids et masse grasse") {
        Text("Mesures personnelles facultatives, conservées sur ce téléphone.")
        measurements.lastOrNull()?.let { Text("${it.day} · ${it.kg?.let { v -> "$v kg" } ?: "poids non renseigné"} · ${it.fat?.let { v -> "$v %" } ?: "masse grasse non renseignée"}") }
        OutlinedButton(onClick = { open = true }) { Text("Mes mesures et mes courbes") }
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Mes mesures") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                val d = LocalDate.parse(day)
                DatePickerDialog(context, { _, y, m, date -> day = LocalDate.of(y, m + 1, date).toString() },
                    d.year, d.monthValue - 1, d.dayOfMonth).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
            }) { Text("Date : $day") }
            NumberField(kg, { kg = it }, "Poids (kg), facultatif", Modifier.fillMaxWidth())
            NumberField(fat, { fat = it }, "Masse grasse (%), facultatif", Modifier.fillMaxWidth())
            Text("Saisissez une mesure connue. L’application ne calcule pas la masse grasse à partir du poids.",
                style = MaterialTheme.typography.bodySmall)
            Button(onClick = { vm.saveMeasurement(day, kg, fat) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (measurements.any { it.day == day }) "Remplacer la mesure de cette date" else "Enregistrer la mesure")
            }
            val weights = measurements.filter { it.kg != null }.takeLast(30)
            val fats = measurements.filter { it.fat != null }.takeLast(30)
            Text("Poids (kg)")
            LoadChart(weights.map { it.kg!! }, weights.joinToString { "${it.day} : ${it.kg} kg" })
            Text("Masse grasse (%)")
            LoadChart(fats.map { it.fat!! }, fats.joinToString { "${it.day} : ${it.fat} %" })
            Text("Courbes : 30 dernières mesures disponibles, espacées uniformément. Les valeurs datées figurent ci-dessous.",
                style = MaterialTheme.typography.bodySmall)
            val history = measurements.asReversed()
            val lastPage = ((history.size - 1).coerceAtLeast(0) / 30)
            val currentPage = page.coerceAtMost(lastPage)
            history.drop(currentPage * 30).take(30).forEach { m ->
                Text("${m.day} · ${m.kg ?: "—"} kg · ${m.fat ?: "—"} %")
                Row {
                    TextButton(onClick = { day = m.day; kg = m.kg?.toString() ?: ""; fat = m.fat?.toString() ?: "" }) { Text("Modifier") }
                    TextButton(onClick = { deleting = m.day }) { Text("Supprimer") }
                }
            }
            if (history.size > 30) Row {
                TextButton(enabled=currentPage>0,onClick={page=currentPage-1}) { Text("Plus récent") }
                TextButton(enabled=currentPage<lastPage,onClick={page=currentPage+1}) { Text("Plus ancien") }
            }
        } }, confirmButton = { TextButton(onClick = { open = false }) { Text("Fermer") } })
    deleting?.let { date ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Supprimer la mesure du $date ?") },
            confirmButton = { TextButton(onClick = { vm.deleteMeasurement(date); deleting = null }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Annuler") } })
    }
}
