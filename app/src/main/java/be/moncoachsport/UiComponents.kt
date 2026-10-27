package be.moncoachsport

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
internal fun LoadChart(values: List<Double>, label: String) {
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
