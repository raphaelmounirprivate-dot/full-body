package be.moncoachsport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun Progress(vm: CoachViewModel, sessions: List<Training>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var delete by remember { mutableStateOf(false) }
    var deleteAll by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf<String?>(null) }
    fun write(uri: android.net.Uri?, csv: Boolean) {
        if (uri != null)
            scope.launch {
                runCatching {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                                it.write(if (csv) vm.exportCsv() else vm.exportJson())
                            }
                        }
                    }
                    .onFailure { vm.message.value = "Export impossible : ${frenchError(it)}" }
            }
    }
    val jsonExport =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) {
            write(it, false)
        }
    val csvExport =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
            write(it, true)
        }
    val import =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                scope.launch {
                    runCatching {
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use(::readBackup)
                            }
                        }
                        .onSuccess { importing = it }
                        .onFailure { vm.message.value = "Lecture impossible : ${frenchError(it)}" }
                }
        }
    val complete = sessions.filter { it.finished != null }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Heading(
                "Le chemin parcouru",
                "Votre progression.",
                "${complete.size} séances · ${complete.sumOf{it.cardio}} min de cardio",
            )
        }
        item {
            Panel("Cette semaine") {
                val week =
                    complete.filter { it.started >= System.currentTimeMillis() - 7 * 86400000L }
                Text(
                    "7 derniers jours : ${week.size} séances, ${week.sumOf{it.cardio}} minutes de cardio."
                )
                Text("La régularité se construit à votre rythme.")
            }
        }
        item { WeeklyReviewPanel(vm) }
        if (complete.isEmpty())
            item { Text("Vos résultats apparaîtront ici après une séance terminée.") }
        items(complete.reversed(), key = { it.id }) { t ->
            Panel("Séance ${t.template} · ${date(t.started)}") {
                Text(
                    Programs.title(t.program) +
                        if (t.program == Programs.CYCLE) " · semaine ${t.week}" else ""
                )
                Text(
                    "${((t.finished!!-t.started)/60000)} min · ${t.sets.size} séries · cardio ${t.cardio} min"
                )
                Text("Énergie ${t.energy}/10 · ${t.cardioMachine} : ${t.intensity}/10")
                if (t.notes.isNotBlank()) Text(t.notes)
                if (t.discomfort.isNotBlank()) Text("Inconfort : ${t.discomfort}")
            }
        }
        item {
            Panel("Charges et répétitions") {
                complete
                    .map { it.program }
                    .distinct()
                    .flatMap { program -> vm.exercisesFor(program).map { program to it } }
                    .forEach { (chartProgram, e) ->
                        val groups =
                            complete
                                .filter { it.program == chartProgram }
                                .flatMap { t ->
                                    t.sets
                                        .filter { it.exerciseId == e.id }
                                        .map { Triple(t.started, it, t.program) }
                                }
                                .groupBy {
                                    listOf(
                                        it.second.machine,
                                        it.second.setting,
                                        it.second.loadUnit,
                                        it.third,
                                    )
                                }
                        groups.forEach { (setup, entries) ->
                            val points =
                                entries
                                    .groupBy { it.first }
                                    .map { (date, sets) ->
                                        date to sets.maxBy { it.second.kg }.second
                                    }
                                    .takeLast(5)
                            Text(
                                if (setup[3] == Programs.CYCLE) e.name else e.name,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${setup[0].ifBlank{"Machine non précisée"}} · ${setup[1]} · kg ${setup[2]} · ${Programs.title(setup[3])}"
                            )
                            Text(
                                points.joinToString("\n") { (time, s) ->
                                    "${date(time).substringBefore('·')}: ${s.kg} kg ${s.loadUnit} × ${s.reps} ${e.unit}"
                                }
                            )
                            Text("Charges (kg ${setup[2]})")
                            LoadChart(
                                points.map { it.second.kg },
                                "Charges de ${e.name}, ${setup[0]}, du plus ancien au plus récent : " +
                                    points.joinToString { it.second.kg.toString() + " kg" },
                            )
                            Text(
                                "${if(e.unit=="s") "Secondes" else "Répétitions"} sur la série de référence"
                            )
                            LoadChart(
                                points.map { it.second.reps.toDouble() },
                                "Valeurs de ${e.name}, du plus ancien au plus récent : " +
                                    points.joinToString { it.second.reps.toString() + " ${e.unit}" },
                            )
                        }
                    }
            }
        }
        item {
            Panel("Vos données restent ici") {
                Text(
                    "Aucun compte, publicité ni télémétrie. JSON : sauvegarde complète. CSV : historique des séances, restaurable avec cette version."
                )
                OutlinedButton(
                    onClick = { jsonExport.launch("mon-coach-sport.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sauvegarder en JSON")
                }
                OutlinedButton(
                    onClick = { csvExport.launch("historique-sport.csv") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exporter les séries en CSV")
                }
                OutlinedButton(
                    onClick = {
                        import.launch(
                            arrayOf(
                                "application/json",
                                "text/csv",
                                "text/comma-separated-values",
                                "text/plain",
                                "application/vnd.ms-excel",
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Importer JSON ou CSV")
                }
                TextButton(onClick = { delete = true }) {
                    Text("Supprimer l’historique", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { deleteAll = true }) {
                    Text(
                        "Effacer toutes mes données et préférences",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
    if (delete)
        AlertDialog(
            onDismissRequest = { delete = false },
            title = { Text("Supprimer toutes les séances ?") },
            text = {
                Text("Cette suppression est définitive. Pensez à exporter votre sauvegarde.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        delete = false
                        vm.erase()
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = { TextButton(onClick = { delete = false }) { Text("Annuler") } },
        )
    if (deleteAll)
        AlertDialog(
            onDismissRequest = { deleteAll = false },
            title = { Text("Tout effacer ?") },
            text = {
                Text(
                    "Séances, brouillons, bilans, réglages et préférences seront effacés définitivement. Exportez une sauvegarde pour les conserver."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteAll = false
                        vm.eraseAll()
                    }
                ) {
                    Text("Tout effacer")
                }
            },
            dismissButton = { TextButton(onClick = { deleteAll = false }) { Text("Annuler") } },
        )
    if (importing != null)
        AlertDialog(
            onDismissRequest = { importing = null },
            title = { Text("Remplacer l’historique ?") },
            text = {
                Text(
                    "La sauvegarde sera validée avant de remplacer vos séances. Exportez les données actuelles si vous souhaitez les conserver."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.restore(importing!!)
                        importing = null
                    }
                ) {
                    Text("Restaurer")
                }
            },
            dismissButton = { TextButton(onClick = { importing = null }) { Text("Annuler") } },
        )
}
