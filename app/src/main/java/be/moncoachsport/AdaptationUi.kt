package be.moncoachsport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun AdaptationPanel(vm: CoachViewModel, sessions: List<Training>, cycleStart: Long) {
    val options by vm.personal.collectAsStateWithLifecycle()
    val unreadable by vm.unreadable.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    val activity = ProgramAdaptation.activity(sessions, cycleStart, System.currentTimeMillis())
    Panel("Remise en forme · machines et poulies") {
        Text(ClubMachines.equipmentSummary, style = MaterialTheme.typography.bodySmall)
        Text("Objectif : trois séances par semaine, avec au moins un jour sans musculation entre deux séances. Deux séries de départ ; la prise de muscle viendra progressivement.")
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Adapter mes prochaines séances")
        }
        Text("7 derniers jours · ${activity.sessions} séances terminées du cycle · ${activity.cardioMinutes} min de cardio en salle")
        if (activity.sets.isEmpty()) Text("Le bilan des séries apparaîtra après une séance terminée.")
        else Text(activity.sets.entries.joinToString(" · ") { "${it.key} : ${it.value}" })
        Text("Séries validées de travail direct, par groupe ; les muscles secondaires ne sont pas recomptés. Les séances en cours ou en pause sont exclues.", style = MaterialTheme.typography.bodySmall)
        if (unreadable.isNotEmpty()) Text("Historique incomplet : ces totaux sont partiels.", color = MaterialTheme.colorScheme.error)
        Text("Pour évoluer : observez les répétitions, la maîtrise du geste et la récupération. La semaine 5 n’impose aucune série supplémentaire.")
        Text("À terme, environ 10 séries par groupe et par semaine est un repère d’hypertrophie, pas un minimum obligatoire ni une cible à atteindre dès la reprise.")
        Text("La marche soutenue peut compléter le cardio : repère adulte de 150–300 min d’activité modérée par semaine, toutes activités confondues. Toute activité compte.")
        Text("Repères généraux : ACSM 2026 et OMS. À ajuster selon votre objectif et vos limitations.", style = MaterialTheme.typography.bodySmall)
    }
    if (open) {
        val initial = ProgramAdaptation.choices.associate { choice ->
            val prior = sessions.filter { it.program == Programs.CYCLE && it.template == choice.template && it.finished != null }
                .maxByOrNull { it.finished!! }?.alternatives.orEmpty()
            choice.key to ProgramAdaptation.choiceValue(choice, prior, options.preferredVariants)
        }
        val priorAlternatives = listOf("A", "B", "C").associateWith { template ->
            sessions.filter { it.program == Programs.CYCLE && it.template == template && it.finished != null }
                .maxByOrNull { it.finished!! }?.alternatives.orEmpty()
        }
        AdaptationDialog(initial, priorAlternatives, onDismiss = { open = false }) { values ->
            vm.setPersonal { it.copy(preferredVariants = values) }
            open = false
        }
    }
}

@Composable
internal fun AdaptationDialog(initial: Map<String, String>, priorAlternatives: Map<String, Map<String, String>> = emptyMap(), onDismiss: () -> Unit, onSave: (Map<String, String>) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Mes variantes machines et poulies") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Activez une variante seulement après avoir appris ses réglages et son geste. Chaque choix remplace un mouvement du guide, sans en ajouter.")
                Text("B : le Rear Kick remplace le hip thrust pour les nouvelles séances. C’est une adaptation personnelle, à raison de 10–15 répétitions par jambe. Désactivez ce choix pour garder le hip thrust dans les futures séances.")
                Text("Pour commencer : apprendre l’incliné en B permet de varier la poussée en gardant la chest press en A et C. Les autres choix peuvent attendre.")
                ProgramAdaptation.choices.forEach { choice ->
                    val label = "${choice.template} · ${Programs.names.getValue(choice.replacement)}"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.weight(1f))
                        Switch(selected[choice.key] == choice.replacement, { enabled ->
                            selected = selected + (choice.key to if (enabled) choice.replacement else choice.slot)
                        }, modifier = Modifier.semantics { contentDescription = label })
                    }
                    Text("Remplace : ${Programs.names.getValue(choice.slot)}", style = MaterialTheme.typography.bodySmall)
                }
                Text("Aperçu des prochaines nouvelles séances", style = MaterialTheme.typography.titleSmall)
                listOf("A", "B", "C").forEach { template ->
                    val alternatives = ProgramAdaptation.alternatives(template, priorAlternatives[template].orEmpty(), selected)
                    val t = Training("preview", template, 1, program = Programs.CYCLE, alternatives = alternatives)
                    Text("$template · " + Programs.cyclePlan(t).joinToString(" · ") {
                        Programs.names.getValue(it.id) + if (it.optional) " (facultatif)" else ""
                    })
                }
                Text("A : la planche et le crunch travaillent le tronc différemment. C : l’écarté est un exercice d’isolation ; gardez les développés A et B. Le développé épaules reste facultatif.")
                Text("Les séances actives, en pause et terminées conservent leurs paramètres. Les choix s’appliqueront à la création d’une nouvelle séance. Charges et réglages restent propres à chaque appareil.")
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selected) }) { Text("Enregistrer pour les prochaines séances") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
internal fun ExtraSetControl(t: Training, exerciseId: String, sessions: List<Training>, vm: CoachViewModel) {
    var open by rememberSaveable(t.id, exerciseId) { mutableStateOf(false) }
    val cycleStart by vm.cycleStart.collectAsStateWithLifecycle()
    val removing = exerciseId in t.extraSets
    OutlinedButton(onClick = { if (removing) vm.extra(exerciseId) else open = true }) {
        Text(if (removing) "Revenir à deux séries" else "Évaluer une 3e série facultative")
    }
    if (open) {
        var technique by rememberSaveable(t.id, exerciseId) { mutableStateOf(false) }
        var recovery by rememberSaveable(t.id, exerciseId) { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { open = false }, title = { Text("Avant d’ajouter du volume") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("${ProgramAdaptation.priorPassages(t, sessions, cycleStart)} passages terminés de ${t.template} depuis le début du cycle. Le calendrier seul ne suffit pas pour décider.")
                Text("Gardez deux séries tant que vous progressez. Si le volume est stable et bien toléré, essayez une série sur un seul exercice, puis observez le passage suivant. Gardez la charge et le cardio stables.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(technique, { technique = it }, modifier = Modifier.semantics { contentDescription = "Geste maîtrisé et volume stabilisé" })
                    Text("Geste maîtrisé et volume stabilisé")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(recovery, { recovery = it }, modifier = Modifier.semantics { contentDescription = "Bonne récupération sans douleur inhabituelle" })
                    Text("Bonne récupération sans douleur inhabituelle")
                }
            } },
            confirmButton = { TextButton(enabled = technique && recovery, onClick = {
                vm.extra(exerciseId, recoveryConfirmed = true)
                open = false
            }) { Text("Essayer une troisième série") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Garder deux séries") } },
        )
    }
}
