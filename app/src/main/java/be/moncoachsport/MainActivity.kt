package be.moncoachsport

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CoachApp() }
    }
}

@Composable
fun CoachApp(vm: CoachViewModel = viewModel()) {
    val dark by vm.dark.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val tabs = listOf("Accueil", "Séance", "Progression", "Exercices")
    val icons =
        listOf(
            Icons.Outlined.Home,
            Icons.Outlined.PlayArrow,
            Icons.Outlined.DateRange,
            Icons.Outlined.List,
        )
    MaterialTheme(colorScheme = coachColors(dark)) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = back?.destination?.route == t,
                            onClick = {
                                nav.navigate(t) {
                                    launchSingleTop = true
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    restoreState = t != "Accueil"
                                }
                            },
                            icon = { Icon(icons[i], null) },
                            label = {
                                Text(
                                    if (
                                        t == "Progression" &&
                                            androidx.compose.ui.platform.LocalDensity.current
                                                .fontScale > 1.2f
                                    )
                                        "Progrès"
                                    else t,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            },
                        )
                    }
                }
            }
        ) { pad ->
            NavHost(nav, startDestination = "Accueil", modifier = Modifier.padding(pad)) {
                composable("Accueil") {
                    CoachHome(
                        vm,
                        sessions,
                        onExercises = { nav.navigate("Exercices") { launchSingleTop = true } },
                        onProgress = { nav.navigate("Progression") { launchSingleTop = true } },
                    ) {
                        vm.start()
                        nav.navigate("Séance")
                    }
                }
                composable("Séance") { Workout(vm, sessions) }
                composable("Progression") { Progress(vm, sessions) }
                composable("Exercices") {
                    val program by vm.program.collectAsStateWithLifecycle()
                    Catalogue(vm.exercisesFor(program), program)
                }
            }
        }
        if (message.isNotBlank())
            AlertDialog(
                onDismissRequest = { vm.message.value = "" },
                title = { Text("Mon Coach Sport") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { vm.message.value = "" }) { Text("Compris") }
                },
            )
    }
}

@Composable
fun Heading(kicker: String, title: String, subtitle: String = "") {
    Text(
        kicker.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
    Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

fun date(ms: Long) = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.FRANCE).format(Date(ms))

@Composable
fun AlertSettings(vm: CoachViewModel) {
    val context = LocalContext.current
    val enabled by vm.alerts.collectAsStateWithLifecycle()
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.setAlerts(granted)
            if (!granted)
                vm.message.value =
                    "Notifications non autorisées. Le minuteur reste disponible dans la séance."
        }
    Panel("Rappel de repos") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Notification et vibration", Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { value ->
                    if (
                        value &&
                            Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                    )
                        request.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.setAlerts(value)
                },
            )
        }
        Text(
            "Android peut retarder le rappel en veille. La vibration se règle aussi dans les paramètres de notification du téléphone.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun GuideExportButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val save =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null)
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.assets
                                .open("guide/Programme_Jims_Namur_images_uniformisees.pdf")
                                .use { input ->
                                    context.contentResolver.openOutputStream(uri)?.use {
                                        input.copyTo(it)
                                    }
                                }
                        }
                    }
                }
        }
    OutlinedButton(
        onClick = { save.launch("Programme_Jims_Namur_images_uniformisees.pdf") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Enregistrer le guide complet · 31 pages")
    }
}

@Composable
private fun LoadChart(values: List<Double>, label: String) {
    val color = MaterialTheme.colorScheme.primary
    if (values.size < 2) {
        Text(
            "Deux passages sont nécessaires pour tracer une courbe.",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    Canvas(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = label }) {
        val range = ((values.maxOrNull() ?: 0.0) - (values.minOrNull() ?: 0.0)).coerceAtLeast(1.0)
        val base = values.minOrNull() ?: 0.0
        val points =
            values.mapIndexed { i, v ->
                Offset(
                    8f + (size.width - 16f) * i / (values.size - 1),
                    size.height - 8f - ((v - base) / range * (size.height - 16f)).toFloat(),
                )
            }
        points.zipWithNext().forEach { (a, b) -> drawLine(color, a, b, strokeWidth = 4f) }
        points.forEach { drawCircle(color, 6f, it) }
    }
}

@Composable
private fun Workout(vm: CoachViewModel, sessions: List<Training>) {
    val active = sessions.firstOrNull { it.finished == null }
    val program by vm.program.collectAsStateWithLifecycle()
    val timer by vm.timer.collectAsStateWithLifecycle()
    val end = timer.first
    val paused = timer.second
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (end > 0 && System.currentTimeMillis() < end) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    val left = if (end > 0) ((end - now + 999) / 1000).coerceAtLeast(0) else paused / 1000
    if (active == null) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Heading("Entraînement", "Prêt pour la suite ?")
            Text(Programs.title(program))
            Text("La rotation avance uniquement quand vous terminez une séance.")
            Button(onClick = { vm.start() }) {
                Text("Commencer la séance ${Programs.next(program,sessions)}")
            }
        }
        return
    }
    val exercises = vm.exercisesFor(active.program)
    val plan = Programs.plan(active, exercises)
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
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Séance ${active.template}",
                        Modifier.weight(1f),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Enregistrée\nlocalement",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        if (end > 0 || paused > 0)
            item {
                Panel("Repos · ${left/60}:${(left%60).toString().padStart(2,'0')}") {
                    Row {
                        TextButton(
                            onClick = {
                                if (end > 0) {
                                    vm.setTimer(
                                        0,
                                        (end - System.currentTimeMillis()).coerceAtLeast(0),
                                    )
                                } else {
                                    vm.setTimer(System.currentTimeMillis() + paused)
                                }
                            }
                        ) {
                            Text(if (end > 0) "Pause" else "Reprendre")
                        }
                        TextButton(onClick = { vm.setTimer(0) }) { Text("Fermer") }
                    }
                }
            }
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
            .lastOrNull { t -> t.sets.any { it.exerciseId == e.id } }
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
            val slot = Programs.plan(active, vm.exercises).indexOf(p)
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
                SetEditor(e, p, index, stored, last, active, sessions, vm) { vm.log(it) }
            }
            RestControls(p, active, vm)
        }
    }
}

@Composable
private fun SetEditor(
    e: Exercise,
    p: Prescription,
    index: Int,
    stored: LoggedSet?,
    previous: LoggedSet?,
    active: Training,
    sessions: List<Training>,
    vm: CoachViewModel,
    save: (LoggedSet) -> Unit,
) {
    val key = "${e.id}-$index"
    var kg by
        rememberSaveable(key, stored) {
            mutableStateOf(
                active.drafts["$key-kg"] ?: (stored?.kg ?: previous?.kg)?.toString() ?: ""
            )
        }
    var reps by
        rememberSaveable(key, stored) {
            mutableStateOf(active.drafts["$key-reps"] ?: (stored?.reps ?: e.min).toString())
        }
    var rir by
        rememberSaveable(key, stored) {
            mutableStateOf(
                active.drafts["$key-rir"]
                    ?: (stored?.rir
                            ?: if (active.program == Programs.CYCLE)
                                Programs.phase(active.week).rirMin
                            else 2)
                        .toString()
            )
        }
    var machine by
        rememberSaveable(key, stored) {
            mutableStateOf(
                active.drafts["$key-machine"] ?: stored?.machine ?: previous?.machine ?: ""
            )
        }
    var setting by
        rememberSaveable(key, stored) {
            mutableStateOf(
                active.drafts["$key-setting"] ?: stored?.setting ?: previous?.setting ?: ""
            )
        }
    var error by remember { mutableStateOf("") }
    var details by rememberSaveable(key) { mutableStateOf(false) }
    var unit by
        rememberSaveable(key, stored) {
            mutableStateOf(
                active.drafts["$key-unit"] ?: stored?.loadUnit ?: previous?.loadUnit ?: "total"
            )
        }
    val haptic by vm.haptics.collectAsStateWithLifecycle()
    val view = androidx.compose.ui.platform.LocalView.current
    val nextPending =
        (0 until e.sets).firstOrNull { i ->
            active.sets.none { it.exerciseId == e.id && it.index == i && it.done }
        }
    var editing by
        rememberSaveable(key, stored, nextPending) {
            mutableStateOf(stored == null && index == nextPending)
        }
    if (stored == null && !editing) {
        TextButton(
            onClick = { editing = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text(
                "Série ${index+1} · à suivre",
                Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
            )
            Icon(Icons.Outlined.KeyboardArrowRight, null)
        }
        return
    }
    if (stored != null && !editing) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClickLabel = "Corriger la série ${index+1}") { editing = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Série ${index+1} · enregistrée", style = MaterialTheme.typography.labelLarge)
                Text(
                    "${stored.kg} kg ${stored.loadUnit} · ${stored.reps} ${e.unit} · RIR ${stored.rir}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Outlined.Edit,
                "Corriger la série ${index+1}",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Série ${index+1}${if(stored!=null) " · enregistrée" else ""}",
            Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = { details = !details }) {
            Text("Machine et réglages", style = MaterialTheme.typography.labelMedium)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(
            kg,
            {
                kg = it
                vm.saveDraft("$key-kg", it)
            },
            "kg $unit",
            Modifier.weight(1f),
        )
        NumberField(
            reps,
            {
                reps = it
                vm.saveDraft("$key-reps", it)
            },
            e.unit,
            Modifier.weight(1f),
        )
        NumberField(
            rir,
            {
                rir = it
                vm.saveDraft("$key-rir", it)
            },
            "RIR",
            Modifier.weight(1f),
        )
    }
    if (details) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("total", "par côté").forEach { value ->
                FilterChip(
                    selected = unit == value,
                    onClick = {
                        unit = value
                        vm.saveDraft("$key-unit", value)
                    },
                    label = { Text("kg $value") },
                )
            }
        }
        OutlinedTextField(
            machine,
            {
                machine = it
                vm.saveDraft("$key-machine", it)
            },
            label = { Text("Machine / identifiant") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            setting,
            {
                setting = it
                vm.saveDraft("$key-setting", it)
            },
            label = { Text("Siège, prise, réglages") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val matched =
        sessions.lastOrNull { t ->
            t.finished != null &&
                t.program == active.program &&
                t.sets.any {
                    it.exerciseId == e.id &&
                        it.machine.trim() == machine.trim() &&
                        it.loadUnit == unit
                }
        }
    if (machine.isNotBlank()) {
        val lastValues =
            matched?.sets?.filter {
                it.exerciseId == e.id && it.machine.trim() == machine.trim() && it.loadUnit == unit
            }
        Text(
            if (lastValues.isNullOrEmpty()) "Aucun passage enregistré sur cette machine."
            else
                "Dernier passage sur cette machine : " +
                    lastValues.joinToString {
                        "${it.kg} kg ${it.loadUnit} × ${it.reps} ${e.unit} · ${it.setting}"
                    },
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (
        index == 0 &&
            Programs.canIncrease(active, p, sessions, vm.exercises, machine, setting, unit)
    )
        Text(
            "Deux passages consécutifs réussis avec la réserve prévue et les mêmes réglages : envisagez le plus petit incrément disponible, sans obligation.",
            color = MaterialTheme.colorScheme.primary,
        )
    Button(
        onClick = {
            val s =
                LoggedSet(
                    e.id,
                    index,
                    if (e.unit == "s") kg.replace(',', '.').toDoubleOrNull() ?: 0.0
                    else kg.replace(',', '.').toDoubleOrNull() ?: -1.0,
                    reps.toIntOrNull() ?: 0,
                    rir.toIntOrNull() ?: -1,
                    machine,
                    setting,
                    loadUnit = unit,
                )
            if (Rules.validate(s)) {
                error = ""
                save(s)
                if (haptic)
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } else error = "Charge ≥ 0, valeur > 0, RIR de 0 à 10."
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Icon(Icons.Outlined.Check, null)
        Spacer(Modifier.width(8.dp))
        Text(if (stored == null) "Valider la série" else "Enregistrer la correction")
    }
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    HorizontalDivider()
}

@Composable
fun NumberField(
    value: String,
    change: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value,
        change,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun Summary(vm: CoachViewModel, t: Training) {
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
                vm.saveDraft("cardioMachine", it)
            },
            label = { Text("Appareil cardio") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                cardio,
                {
                    cardio = it
                    vm.saveDraft("cardio", it)
                },
                "Minutes",
                Modifier.weight(1f),
            )
            NumberField(
                intensity,
                {
                    intensity = it
                    vm.saveDraft("intensity", it)
                },
                "Effort /10",
                Modifier.weight(1f),
            )
        }
        NumberField(
            energy,
            {
                energy = it
                vm.saveDraft("energy", it)
            },
            "Énergie /10",
            Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            pain,
            {
                pain = it
                vm.saveDraft("pain", it)
            },
            label = { Text("Inconfort / douleur") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            notes,
            {
                notes = it
                vm.saveDraft("notes", it)
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
                        vm.finish(notes, cardio, energy, pain, machine, intensity)
                    }
                ) {
                    Text("Terminer")
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Continuer") } },
        )
}

@Composable
private fun ExerciseSheetButton(e: Exercise, compact: Boolean = false) {
    var open by rememberSaveable(e.id) { mutableStateOf(false) }
    if (compact)
        TextButton(
            onClick = { open = true },
            modifier =
                Modifier.heightIn(min = 48.dp).semantics {
                    contentDescription = "Voir la fiche de ${e.name}"
                },
        ) {
            Text("Voir la fiche")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.KeyboardArrowRight, null)
        }
    else
        OutlinedButton(
            onClick = { open = true },
            modifier =
                Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                    contentDescription = "Voir la fiche de ${e.name}"
                },
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Voir la fiche")
        }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(e.name) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExerciseDetails(e)
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Fermer la fiche") } },
        )
    }
}

@Composable
private fun ExerciseDetails(e: Exercise) {
    val context = LocalContext.current
    val bitmap =
        remember(e.image) {
            runCatching {
                    context.assets.open(e.image).use {
                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                }
                .getOrNull()
        }
    var zoom by remember { mutableStateOf(false) }
    bitmap?.let {
        Image(
            it,
            "Illustration de ${e.name}",
            Modifier.fillMaxWidth().heightIn(max = 310.dp).clickable { zoom = true },
        )
    }
    if (e.caption.isNotBlank()) Text(e.caption, style = MaterialTheme.typography.bodySmall)
    Text("Muscles : ${e.muscles}")
    Text("Installer", fontWeight = FontWeight.Bold)
    Text("Réglage : ${e.setup}")
    Text("Bouger", fontWeight = FontWeight.Bold)
    Text(e.instructions)
    Text("Vérifier", fontWeight = FontWeight.Bold)
    Text(
        e.check.ifBlank {
            "Les équipements réels peuvent différer ; faites montrer les sécurités par un coach."
        }
    )
    Text(
        if (e.sourcePage > 0)
            "Guide personnel Jims Namur · ${e.id}, page ${e.sourcePage}. ${e.warning}"
        else "Archive : illustration et consignes de l’ancien PDF de 8 pages.",
        style = MaterialTheme.typography.bodySmall,
    )
    if (e.sourcePage > 0)
        Programs.variants
            .filter { it.replacement == e.id }
            .forEach { Text(it.condition, style = MaterialTheme.typography.bodySmall) }
    if (zoom && bitmap != null)
        AlertDialog(
            onDismissRequest = { zoom = false },
            confirmButton = { TextButton(onClick = { zoom = false }) { Text("Fermer") } },
            text = { Image(bitmap, "Illustration agrandie de ${e.name}", Modifier.fillMaxWidth()) },
        )
}

@Composable
private fun Catalogue(exercises: List<Exercise>, program: String) {
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("Tous") }
    val groups =
        mapOf(
            "Pectoraux" to setOf("F2", "F10", "F16"),
            "Dos" to setOf("F3", "F9", "F14"),
            "Jambes" to setOf("F1", "F4", "F7", "F8", "F11", "F18"),
            "Épaules" to setOf("F5", "F15"),
            "Bras" to setOf("F12", "F13"),
            "Tronc" to setOf("F6", "F19"),
            "Cardio" to setOf("F17"),
        )
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Heading(
                "Votre bibliothèque",
                "Les mouvements.",
                if (program == Programs.CYCLE) "19 fiches illustrées · guide Jims Namur"
                else "Archive · ancien programme de 8 pages",
            )
        }
        item {
            OutlinedTextField(
                query,
                { query = it },
                label = { Text("Rechercher un exercice ou un muscle") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (listOf("Tous") + groups.keys).forEach { label ->
                    FilterChip(
                        selected = group == label,
                        onClick = { group = label },
                        label = { Text(label) },
                    )
                }
            }
        }
        items(
            exercises
                .map {
                    if (program == Programs.CYCLE) it.copy(name = Programs.names.getValue(it.id))
                    else it
                }
                .filter { ("${it.name} ${it.muscles} ${it.id}").contains(query, true) }
                .filter { group == "Tous" || it.id in groups.getValue(group) },
            key = { it.id },
        ) { e ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExerciseThumbnail(e, Modifier.width(105.dp).height(110.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            e.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${e.id} · ${e.muscles}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ExerciseSheetButton(e, compact = true)
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseThumbnail(e: Exercise, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap =
        remember(e.image) {
            context.assets.open(e.image).use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }
    Image(
        bitmap,
        "Aperçu de ${e.name}",
        modifier.background(Color.White, RoundedCornerShape(14.dp)),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun Progress(vm: CoachViewModel, sessions: List<Training>) {
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
                    .onFailure { vm.message.value = "Export impossible : ${it.message}" }
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
                        .onFailure { vm.message.value = "Lecture impossible : ${it.message}" }
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
                vm.exercises.forEach { e ->
                    val groups =
                        complete
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
                                .map { (date, sets) -> date to sets.maxBy { it.second.kg }.second }
                                .takeLast(5)
                        Text(
                            if (setup[3] == Programs.CYCLE) Programs.names.getValue(e.id)
                            else e.name,
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
