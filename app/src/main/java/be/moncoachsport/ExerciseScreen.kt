package be.moncoachsport

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.*

@Composable
internal fun ExerciseSheetButton(e: Exercise, compact: Boolean = false, program: String = Programs.CYCLE, vm: CoachViewModel? = null) {
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
                    if (vm != null) ExerciseVideo(e, program, vm)
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
    if (e.accessory.isNotBlank() && (e.sourcePage > 0 || e.id == ClubMachines.REAR_KICK))
        Text(e.accessory, style = MaterialTheme.typography.bodyMedium)
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
        if (e.id == ClubMachines.REAR_KICK) "Adaptation personnelle · ${e.id}. ${e.warning}"
        else if (e.sourcePage > 0)
            "Guide personnel Jims Namur · ${e.id}, page ${e.sourcePage}. ${e.warning}"
        else "Archive : illustration et consignes de l’ancien PDF de 8 pages.",
        style = MaterialTheme.typography.bodySmall,
    )
    if (e.sourcePage > 0 || e.id == ClubMachines.REAR_KICK)
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
internal fun Catalogue(exercises: List<Exercise>, program: String, vm: CoachViewModel? = null) {
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("Tous") }
    val groups =
        mapOf(
            "Pectoraux" to setOf("F2", "F10", "F16"),
            "Dos" to setOf("F3", "F9", "F14"),
            "Jambes" to setOf("F1", "F4", "F7", "F8", "F11", "F18", ClubMachines.REAR_KICK),
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
                if (program == Programs.CYCLE) "19 fiches du guide · 1 machine personnelle"
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
                        ExerciseSheetButton(e, compact = true, program = program, vm = vm)
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
            runCatching { context.assets.open(e.image).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
    if (bitmap == null) {
        androidx.compose.foundation.layout.Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp)), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Machine", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else Image(
        bitmap,
        "Aperçu de ${e.name}",
        modifier.background(Color.White, RoundedCornerShape(14.dp)),
        contentScale = ContentScale.Fit,
    )
}
