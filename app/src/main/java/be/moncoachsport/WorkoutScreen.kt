package be.moncoachsport

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun Workout(vm: CoachViewModel, sessions: List<Training>) {
    val active = sessions.firstOrNull { it.isActive }
    val program by vm.program.collectAsStateWithLifecycle()
    val timer by vm.timer.collectAsStateWithLifecycle()
    val end = timer.first
    val paused = timer.second
    if (active == null) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Heading("Entraînement", "Prêt pour la suite ?")
            Text(Programs.title(program))
            Text("La rotation avance uniquement quand vous terminez une séance.")
            Button(onClick = { vm.start() }) {
                val next = Programs.next(program, sessions)
                Text(
                    "${if (sessions.any { it.program == program && it.template == next && it.paused }) "Reprendre" else "Commencer"} la séance $next"
                )
            }
            SessionChooser(vm, sessions, program, null)
        }
        return
    }
    val exercises = vm.exercisesFor(active.program)
    val plan =
        remember(
            active.program,
            active.template,
            active.week,
            active.alternatives,
            active.extraSets,
            active.skipped,
            exercises,
        ) {
            Programs.plan(active, exercises)
        }
    var expandedSlot by rememberSaveable(active.id) { mutableStateOf(plan.firstOrNull()?.id) }
    val plannedSets =
        plan.sumOf { p ->
            val e = Programs.exercise(p, active, exercises)
            if (e.id in active.skipped) 0 else e.sets
        }
    val completedSets =
        plan.sumOf { p ->
            val e = Programs.exercise(p, active, exercises)
            if (e.id in active.skipped) 0
            else
                (0 until e.sets).count { index ->
                    active.sets.any { it.exerciseId == e.id && it.index == index && it.done }
                }
        }
    val workoutScroll = rememberLazyListState()
    LaunchedEffect(active.id) { workoutScroll.scrollToItem(0) }
    LazyColumn(
        state = workoutScroll,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Séance ${active.template}",
                        Modifier.weight(1f),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    SessionChooser(vm, sessions, active.program, active)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = Color(0xFF0A203B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "${Programs.title(active.program)}${if(active.program==Programs.CYCLE) " · Semaine ${active.week}" else ""}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFC4DDFF),
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "$completedSets / $plannedSets séries",
                                fontWeight = FontWeight.Bold,
                            )
                            Text("${plan.size} exercices", color = Color(0xFFC4DDFF))
                        }
                        LinearProgressIndicator(
                            progress = {
                                if (plannedSets == 0) 0f else completedSets.toFloat() / plannedSets
                            },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFF80B6FF),
                            trackColor = Color(0xFF2B435E),
                        )
                    }
                }
            }
        }
        item {
            var warmupOpen by rememberSaveable(active.id) { mutableStateOf(false) }
            Column {
                TextButton(
                    onClick = { warmupOpen = !warmupOpen },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Préparer mon échauffement",
                        Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    )
                    Icon(
                        if (warmupOpen) Icons.Outlined.KeyboardArrowUp
                        else Icons.Outlined.KeyboardArrowDown,
                        null,
                    )
                }
                if (warmupOpen) WarmupText(active.program)
            }
        }
        if (end > 0 || paused > 0) item { RestTimerCard(vm, end, paused) }
        items(plan, key = { it.id }) { p ->
            val exercise = Programs.exercise(p, active, exercises)
            ExerciseLog(
                exercise,
                p,
                active,
                sessions,
                vm,
                plan.indexOf(p) + 1,
                expandedSlot == p.id,
            ) {
                expandedSlot = if (expandedSlot == p.id) null else p.id
            }
        }
        item { Summary(vm, active) }
    }
}

@Composable
private fun ExerciseLog(
    e: Exercise,
    p: Prescription,
    active: Training,
    sessions: List<Training>,
    vm: CoachViewModel,
    position: Int,
    expanded: Boolean,
    toggle: () -> Unit,
) {
    val previous =
        sessions
            .filter { it.finished != null && it.program == active.program }
            .filter { t -> t.sets.any { it.exerciseId == e.id } }
            .maxByOrNull { it.finished!! }
    val completed =
        (0 until e.sets).count { index ->
            active.sets.any { it.exerciseId == e.id && it.index == index && it.done }
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (expanded) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceContainer
            ),
        border =
            if (expanded) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable(
                        onClickLabel = if (expanded) "Replier ${e.name}" else "Ouvrir ${e.name}",
                        onClick = toggle,
                    )
                    .semantics {
                        contentDescription =
                            if (expanded) "Replier ${e.name}" else "Ouvrir ${e.name}"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ExerciseThumbnail(e, Modifier.size(if (expanded) 96.dp else 72.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "Exercice $position · ${if (e.id in active.skipped) "sauté" else if (completed == e.sets) "terminé" else "$completed / ${e.sets} séries"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        e.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp
                    else Icons.Outlined.KeyboardArrowDown,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "${e.sets} × ${e.min}–${e.max} ${e.unit} · " +
                    if (e.rest > 0)
                        "repos ${p.restMin}${if(p.restMax!=p.restMin) "–${p.restMax}" else ""} s"
                    else "enchaîné en superset",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ExerciseSheetButton(e, compact = true)
            if (!expanded) return@Column
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "RIR : répétitions encore possibles à la fin de la série.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (e.id == "F5")
                Text(
                    if (active.program == Programs.CYCLE) "Par bras ; repos après les deux bras."
                    else "Par bras"
                )
            if (active.program == Programs.LEGACY && e.id in listOf("F12", "F13"))
                Text(
                    "Superset : enchaîner curl et extension. Aucun repos chiffré n’est fourni entre les deux."
                )
            if (active.program == Programs.LEGACY && e.id == "F17")
                Text(
                    "Source : 6 × (30 s à fond / 30 s repos), résistance 4–6. Adaptez à votre capacité."
                )
            if (p.optional) {
                Text("Exercice facultatif")
                OutlinedButton(onClick = { vm.skip(e.id) }) {
                    Text(
                        if (e.id in active.skipped) "Réintégrer cet exercice"
                        else "Sauter cet exercice"
                    )
                }
            }
            val slot = Programs.plan(active, vm.exercisesFor(active.program)).indexOf(p)
            if (active.program == Programs.CYCLE) VariantChoices(active, slot, vm)
            if (
                Programs.extraAllowed(active, slot) &&
                    (active.week in 5..6 || e.id in active.extraSets)
            ) {
                OutlinedButton(onClick = { vm.extra(e.id) }) {
                    Text(
                        if (e.id in active.extraSets) "Revenir à deux séries"
                        else "Ajouter une 3e série facultative"
                    )
                }
            }
            if (e.id in active.skipped) {
                Text("Exercice sauté pour cette séance.")
                return@Column
            }
            repeat(e.sets) { index ->
                val stored = active.sets.firstOrNull { it.exerciseId == e.id && it.index == index }
                val last =
                    previous?.sets?.firstOrNull { it.exerciseId == e.id && it.index == index }
                SetEditor(e, p, index, stored, last, active, sessions, vm) { vm.log(it, active.id) }
            }
            RestControls(p, active, vm)
        }
    }
}
