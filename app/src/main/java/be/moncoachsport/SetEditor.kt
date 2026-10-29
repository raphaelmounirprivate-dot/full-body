package be.moncoachsport

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@Composable
internal fun SetEditor(
    e: Exercise,
    p: Prescription,
    index: Int,
    stored: LoggedSet?,
    previous: LoggedSet?,
    active: Training,
    sessions: List<Training>,
    vm: CoachViewModel,
    actionLabel: String? = null,
    save: (LoggedSet) -> Unit,
) {
    val key = "${e.id}-$index"
    val stateKey = "${active.id}-$key"
    val history = remember(sessions) { sessions.filter { it.finished != null } }
    var kg by
        rememberSaveable(stateKey, stored) {
            mutableStateOf(
                active.drafts["$key-kg"] ?: (stored?.kg ?: previous?.kg)?.toString() ?: ""
            )
        }
    var reps by
        rememberSaveable(stateKey, stored) {
            mutableStateOf(active.drafts["$key-reps"] ?: (stored?.reps ?: e.min).toString())
        }
    var rir by
        rememberSaveable(stateKey, stored) {
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
        rememberSaveable(stateKey, stored) {
            mutableStateOf(
                active.drafts["$key-machine"] ?: stored?.machine ?: previous?.machine ?: ""
            )
        }
    var setting by
        rememberSaveable(stateKey, stored) {
            mutableStateOf(
                active.drafts["$key-setting"] ?: stored?.setting ?: previous?.setting ?: ""
            )
        }
    var error by remember { mutableStateOf("") }
    var details by rememberSaveable(stateKey) { mutableStateOf(false) }
    var unit by
        rememberSaveable(stateKey, stored) {
            mutableStateOf(
                active.drafts["$key-unit"] ?: stored?.loadUnit ?: previous?.loadUnit ?: "total"
            )
        }
    val haptic by vm.haptics.collectAsStateWithLifecycle()
    val view = androidx.compose.ui.platform.LocalView.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val nextPending =
        (0 until e.sets).firstOrNull { i ->
            active.sets.none { it.exerciseId == e.id && it.index == i && it.done }
        }
    var editing by
        rememberSaveable(stateKey, stored, nextPending) {
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
                val unreadable by vm.unreadable.collectAsStateWithLifecycle()
                val record = remember(active.sets, history, unreadable.isEmpty()) {
                    PersonalRecords.forStored(active,stored,history,e,unreadable.isEmpty())
                }
                if (record.achieved) Text("Record personnel · ${record.label()}",
                    color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)
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
                vm.saveDraft("$key-kg", it, active.id)
            },
            "kg $unit",
            Modifier.weight(1f).semantics { contentDescription = "Charge de ${e.name}, série ${index+1}" },
        )
        NumberField(
            reps,
            {
                reps = it
                vm.saveDraft("$key-reps", it, active.id)
            },
            e.unit,
            Modifier.weight(1f),
        )
        NumberField(
            rir,
            {
                rir = it
                vm.saveDraft("$key-rir", it, active.id)
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
                        vm.saveDraft("$key-unit", value, active.id)
                    },
                    label = { Text("kg $value") },
                )
            }
        }
        OutlinedTextField(
            machine,
            {
                machine = it
                vm.saveDraft("$key-machine", it, active.id)
            },
            label = { Text("Machine / identifiant") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            setting,
            {
                setting = it
                vm.saveDraft("$key-setting", it, active.id)
            },
            label = { Text("Siège, prise, réglages") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val unreadable by vm.unreadable.collectAsStateWithLifecycle()
    val policy = active.copy(drafts = emptyMap())
    val canIncrease =
        remember(policy, p, history, machine, setting, unit, unreadable.isEmpty()) {
            Programs.canIncrease(
                policy,
                p,
                history,
                vm.exercisesFor(active.program),
                machine,
                setting,
                unit,
                historyComplete = unreadable.isEmpty(),
            )
        }
    val matched =
        remember(history, machine, unit, e.id, active.program) {
            history
                .filter { t ->
                    t.finished != null &&
                        t.program == active.program &&
                        t.sets.any {
                            it.exerciseId == e.id &&
                                it.machine.trim() == machine.trim() &&
                                it.loadUnit == unit
                        }
                }
                .maxByOrNull { it.finished!! }
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
    if (index == 0 && canIncrease)
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
                focus.clearFocus()
                keyboard?.hide()
                save(s)
                if (haptic)
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } else error = "Charge ≥ 0, valeur > 0, RIR de 0 à 10."
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Icon(Icons.Outlined.Check, null)
        Spacer(Modifier.width(8.dp))
        Text(actionLabel ?: if (stored == null) "Valider la série" else "Enregistrer la correction")
    }
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    HorizontalDivider()
}
