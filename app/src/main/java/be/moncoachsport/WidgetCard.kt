package be.moncoachsport

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun WidgetCard() {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    Panel("Widget sur l’accueil Android") {
        Text("Prochaine séance et objectif de la semaine, sans ouvrir le carnet.")
        if (manager.isRequestPinAppWidgetSupported) OutlinedButton(onClick = {
            manager.requestPinAppWidget(ComponentName(context,WorkoutWidgetProvider::class.java),null,null)
        }, modifier=Modifier.fillMaxWidth()) { Text("Ajouter le widget Android") }
        Text("Vous pouvez aussi maintenir le doigt sur l’accueil du téléphone, choisir Widgets puis Mon Coach Sport. Le widget est redimensionnable.")
    }
}
