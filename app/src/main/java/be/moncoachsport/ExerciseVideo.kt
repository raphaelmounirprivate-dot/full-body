package be.moncoachsport

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Links documented by the manufacturer, not an identification of the club's actual machines. */
internal val demonstrationVideos = mapOf(
    "F1" to "l9VvK3zZtf0", "F2" to "a5WPG83JRKo", "F3" to "4HmLoc_PDtM",
    "F4" to "_zmZphBmO0Q", "F9" to "OmvBs0jUSmk", "F11" to "4yBHsqsvhlQ",
    "F15" to "5Dmu9MNAIFE", "F17" to "qIUio2uC2lw", "F18" to "NjCl03dAT6E",
)

@Composable
internal fun ExerciseVideo(e: Exercise, program: String, vm: CoachViewModel) {
    val options by vm.personal.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val key = (if (program == Programs.CYCLE) "cycle" else "legacy") + ":" + e.id
    val default = if (program == Programs.CYCLE) demonstrationVideos[e.id]?.let { "https://youtu.be/$it" } else null
    val url = options.videos[key] ?: default
    var editing by rememberSaveable(key) { mutableStateOf(false) }
    var input by rememberSaveable(key, options.videos[key]) { mutableStateOf(options.videos[key] ?: "") }
    var error by remember { mutableStateOf("") }
    fun open(link: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
            .onFailure { vm.message.value = "Aucune application ne permet d’ouvrir ce lien." }
    }
    Text("Repère personnel : ${options.exerciseLevels[key] ?: "non renseigné"}")
    Row {
        FeatureRules.levels.forEach { level ->
            TextButton(onClick = { vm.setPersonal { it.copy(exerciseLevels = it.exerciseLevels + (key to level)) } },
                modifier = Modifier.weight(1f)) { Text(level) }
        }
    }
    Text("Ce repère est votre choix ; il ne modifie pas le programme.")
    if (url != null) {
        OutlinedButton(onClick = { open(url) }, modifier = Modifier.fillMaxWidth()) { Text("Voir la vidéo YouTube") }
        Text(if (options.videos.containsKey(key)) "Lien personnel." else if (e.id == "F17") "Démonstration Concept2."
            else "Réglages sur un appareil EGYM : modèle à comparer avec celui de votre salle.")
    }
    TextButton(onClick = { editing = !editing }) { Text(if (editing) "Masquer le champ vidéo" else "Choisir ma vidéo YouTube") }
    if (editing) {
        OutlinedTextField(input, { input = it; error = "" }, label = { Text("Lien https://youtu.be/… ou youtube.com/watch…") },
            modifier = Modifier.fillMaxWidth(), isError = error.isNotBlank())
        TextButton(onClick = {
            if (input.isBlank() || FeatureRules.validYoutube(input.trim())) vm.setVideo(program,e.id,input)
            else error = "Saisissez le lien HTTPS d’une vidéo YouTube."
        }) { Text(if (input.isBlank()) "Revenir au lien par défaut" else "Enregistrer ce lien") }
        if (error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
    }
    if (url == null) TextButton(onClick = { open("https://www.youtube.com/results?search_query=" + Uri.encode(e.name + " technique exercice")) }) {
        Text("Rechercher une démonstration sur YouTube")
    }
    Text("YouTube s’ouvre à l’extérieur et nécessite Internet. La fiche illustrée reste disponible hors ligne.",
        style=MaterialTheme.typography.bodySmall)
}
