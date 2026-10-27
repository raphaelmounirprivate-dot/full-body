package be.moncoachsport

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Calendar

@Composable
fun CoachHome(
    vm: CoachViewModel,
    sessions: List<Training>,
    onExercises: () -> Unit = {},
    onProgress: () -> Unit = {},
    start: () -> Unit,
) {
    val program by vm.program.collectAsStateWithLifecycle()
    val cycleStart by vm.cycleStart.collectAsStateWithLifecycle()
    val active = sessions.firstOrNull { it.isActive }
    val completed = sessions.filter { it.finished != null }
    val currentWeek =
        if (cycleStart == 0L) 1 else Rules.week(cycleStart, System.currentTimeMillis())
    val next = active?.template ?: Programs.next(program, sessions)
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Heading(
                "Jims · Namur",
                "À votre rythme.",
                "Votre carnet de musculation, toujours avec vous.",
            )
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color(0xFF0A203B),
                        contentColor = Color.White,
                    ),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "PROGRAMME",
                                fontSize = 11.sp,
                                color = Color(0xFF89BCFF),
                                fontWeight = FontWeight.Bold,
                            )
                            Text("Full Body", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (active != null) "Votre séance vous attend"
                                else "Prochaine séance · $next",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        ExerciseThumbnail(
                            vm.guideExercises.first { it.id == "F2" },
                            Modifier.width(128.dp).height(120.dp),
                        )
                    }
                    Text(Programs.title(active?.program ?: program))
                    if ((active?.program ?: program) == Programs.CYCLE)
                        Text("Semaine ${active?.week ?: currentWeek} / 8 · rotation A / B / C")
                    Button(
                        onClick = start,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text(if (active != null) "Reprendre ma séance" else "Commencer ma séance")
                    }
                    active
                        ?.takeIf { it.program != program }
                        ?.let {
                            Text(
                                "Cette ancienne séance garde ses paramètres. Les suivantes utiliseront le guide corrigé."
                            )
                        }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${completed.size} séances")
                Text("${completed.sumOf { it.cardio }} min cardio")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onExercises,
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Exercices illustrés")
                }
                OutlinedButton(
                    onClick = onProgress,
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Ma progression")
                }
            }
        }
        item { ProgramSelector(vm, program, cycleStart) }
        item { GuideSafety(vm.guideSections) }
        if (program == Programs.CYCLE) item { PhaseCard(currentWeek) }
        item {
            Panel("Bien préparer la séance") {
                WarmupText(active?.program ?: program)
                Text(
                    "Laissez au moins un jour sans musculation entre deux séances. L’échauffement ne compte pas dans les séries de travail."
                )
            }
        }
        item {
            Panel("Dernier entraînement") {
                Text(
                    completed
                        .maxByOrNull { it.finished!! }
                        ?.let {
                            "Séance ${it.template} · ${date(it.started)}\n${Programs.title(it.program)} · ${it.sets.size} séries"
                        } ?: "Votre historique commencera après votre première séance."
                )
            }
        }
        item {
            Panel("En cas de fatigue") {
                Text(
                    "Si la récupération est insuffisante, retirez les exercices facultatifs et adaptez le volume. Aucune hausse de charge n’est imposée."
                )
            }
        }
        item {
            TextButton(onClick = { vm.toggleTheme() }) { Text("Changer le thème clair / sombre") }
        }
        item { TrainingSettings(vm) }
        item { AlertSettings(vm) }
        item { GuideExportButton() }
    }
}

@Composable
private fun ProgramSelector(vm: CoachViewModel, program: String, start: Long) {
    val context = LocalContext.current
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    var restart by remember { mutableStateOf(false) }
    Panel("Mon programme") {
        Text("Guide personnel Jims Namur · 31 pages")
        if (program == Programs.CYCLE) {
            if (Rules.cycleHasEnded(start, System.currentTimeMillis())) {
                val pending = sessions.any { it.program == Programs.CYCLE && it.finished == null }
                Text(
                    "Les huit semaines calendaires sont écoulées. Votre historique reste conservé."
                )
                Button(onClick = { restart = true }, enabled = !pending) {
                    Text("Recommencer en semaine 1")
                }
                if (pending)
                    Text(
                        "Reprenez et terminez les séances du cycle encore en pause avant de recommencer.",
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
            Text(
                if (start == 0L) "La semaine 1 commence à votre première séance du cycle."
                else "Départ : ${date(start).substringBefore('·')}"
            )
            OutlinedButton(
                onClick = {
                    val calendar =
                        Calendar.getInstance().apply { if (start > 0) timeInMillis = start }
                    DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val picked =
                                    Calendar.getInstance().apply {
                                        set(year, month, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                vm.setCycleStart(picked.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        )
                        .apply { datePicker.maxDate = System.currentTimeMillis() }
                        .show()
                }
            ) {
                Text("Régler la date de départ du cycle")
            }
            Text(
                "Les séances enregistrées conservent leur semaine et leurs paramètres.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (restart)
        AlertDialog(
            onDismissRequest = { restart = false },
            title = { Text("Recommencer un cycle ?") },
            text = {
                Text(
                    "La date de départ passe à aujourd'hui. Les anciennes séances et leurs semaines restent conservées."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        restart = false
                        vm.restartCycle()
                    }
                ) {
                    Text("Recommencer")
                }
            },
            dismissButton = { TextButton(onClick = { restart = false }) { Text("Annuler") } },
        )
}

@Composable
fun WarmupText(program: String) {
    Text(
        if (program == Programs.CYCLE)
            "5–7 min à vélo ou à l’elliptique, 1–2 séries faciles pour les jambes, puis une série légère sur la première poussée et le premier tirage."
        else
            "8–10 min : 5 min faciles à vélo, à l’elliptique ou au rameur, puis 1–2 séries légères sur la première machine."
    )
}

@Composable
fun RestControls(p: Prescription, t: Training, vm: CoachViewModel) {
    if (p.restMax == 0) return
    val selected =
        t.drafts["${p.id}-rest"]?.toIntOrNull()?.takeIf { it in p.restMin..p.restMax } ?: p.restMax
    if (p.restMin != p.restMax) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(p.restMin, p.restMax).forEach { seconds ->
                FilterChip(
                    selected = selected == seconds,
                    onClick = { vm.saveDraft("${p.id}-rest", seconds.toString()) },
                    label = { Text("$seconds s") },
                )
            }
        }
    }
    TextButton(onClick = { vm.setTimer(System.currentTimeMillis() + selected * 1000L) }) {
        Text("Démarrer $selected s de repos")
    }
}

@Composable
fun PhaseCard(week: Int) {
    val phase = Programs.phase(week)
    Panel("Semaine $week / 8") {
        Text("RIR ${phase.rirMin}–${phase.rirMax} · cardio ${phase.cardio}")
        Text(phase.advice)
        Text(
            "Réserve : 3–4 répétitions en semaines 1–2, puis environ 2–3 (guide, p. 2).",
            style = MaterialTheme.typography.bodySmall,
        )
        if (week >= 5)
            Text(
                "Une seule augmentation à la fois : ajouter d’abord une série sur un seul exercice, observer le passage suivant ; garder le cardio stable et ne pas augmenter aussi sa charge (p. 3)."
            )
        if (week >= 7)
            Text(
                "Intervalles facultatifs : remplacent le cardio continu, au maximum une fois par semaine, dès S7. 5 min faciles ; 4 cycles de 30 s soutenues contrôlées / 90 s faciles ; 3–5 min faciles. Effort autour de 7/10, jamais maximal. Plus tard, 5–6 cycles si bien tolérés (p. 7)."
            )
    }
}

@Composable
fun GuideSafety(sections: List<GuideSection>) {
    var open by rememberSaveable { mutableStateOf(false) }
    Panel("Avant de commencer") {
        Text(
            "Fais montrer les réglages et les sécurités par un coach. Signale une blessure ou une limitation connue pour adapter les mouvements."
        )
        Text(
            "Arrête en cas de douleur thoracique, malaise ou essoufflement inhabituel ; demande une aide médicale adaptée.",
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = { open = true }) { Text("Lire les consignes du guide") }
    }
    if (open)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Repères du guide") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    sections.forEach { section ->
                        Text("${section.title} · p. ${section.page}", fontWeight = FontWeight.Bold)
                        Text(section.text)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Fermer") } },
        )
}

@Composable
fun VariantChoices(t: Training, index: Int, vm: CoachViewModel) {
    val original = Programs.cycle.getValue(t.template)[index]
    val variants = Programs.variants.filter { it.template == t.template && it.slot == original.id }
    if (variants.isEmpty()) return
    var open by rememberSaveable(t.id, original.id) { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("Remplacer ce mouvement") }
    if (open)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Choisir un remplacement") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Le remplacement prend la place du mouvement, il ne s’ajoute pas. Commencer léger et suivre séparément la nouvelle machine (guide, p. 8)."
                    )
                    OutlinedButton(
                        onClick = {
                            vm.chooseVariant(original.id, original.id)
                            open = false
                        }
                    ) {
                        Text("Conserver ${Programs.names.getValue(original.id)}")
                    }
                    variants.forEach { v ->
                        Text(v.condition, style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = {
                                vm.chooseVariant(v.slot, v.replacement)
                                open = false
                            }
                        ) {
                            Text(Programs.names.getValue(v.replacement))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Annuler") } },
        )
}

@Composable
private fun TrainingSettings(vm: CoachViewModel) {
    val auto by vm.autoRest.collectAsStateWithLifecycle()
    val haptic by vm.haptics.collectAsStateWithLifecycle()
    Panel("Pendant la séance") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Repos automatique", Modifier.weight(1f))
            Switch(auto, { vm.setAutoRest(it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Retour haptique à la validation", Modifier.weight(1f))
            Switch(haptic, { vm.setHaptics(it) })
        }
    }
}

@Composable
fun WeeklyReviewPanel(vm: CoachViewModel) {
    val start by vm.cycleStart.collectAsStateWithLifecycle()
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val week = if (start == 0L) 1 else Rules.week(start, System.currentTimeMillis())
    val existing = reviews.firstOrNull { it.cycleStart == start && it.week == week }
    var recovery by
        rememberSaveable(start, week, existing) {
            mutableStateOf(existing?.recovery?.toString() ?: "5")
        }
    var notes by rememberSaveable(start, week, existing) { mutableStateOf(existing?.notes ?: "") }
    Panel("Bilan du cycle · semaine $week") {
        NumberField(recovery, { recovery = it }, "Récupération /10", Modifier.fillMaxWidth())
        OutlinedTextField(
            notes,
            { notes = it },
            label = { Text("Ressenti et prochaine décision") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = { vm.saveReview(week, recovery, notes) }, enabled = start > 0) {
            Text("Enregistrer le bilan hebdomadaire")
        }
        if (start == 0L) Text("Disponible après le démarrage du cycle.")
        reviews
            .filter { it.cycleStart == start && it.week != week }
            .forEach { Text("Semaine ${it.week} · récupération ${it.recovery}/10 · ${it.notes}") }
    }
}
