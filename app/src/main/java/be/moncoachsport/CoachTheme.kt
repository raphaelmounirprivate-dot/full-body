package be.moncoachsport

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

fun coachColors(dark: Boolean) =
    if (dark)
        darkColorScheme(
            primary = Color(0xFF80B6FF),
            onPrimary = Color(0xFF082346),
            background = Color(0xFF081421),
            surface = Color(0xFF102137),
            surfaceContainer = Color(0xFF162B43),
            surfaceContainerHigh = Color(0xFF203752),
            secondaryContainer = Color(0xFF1D3B63),
            onSecondaryContainer = Color(0xFFC4DDFF),
            onSurface = Color(0xFFF1F6FF),
            onSurfaceVariant = Color(0xFFBDCCE0),
            outline = Color(0xFF526B89),
            outlineVariant = Color(0xFF2B435E),
        )
    else
        lightColorScheme(
            primary = Color(0xFF006CE5),
            onPrimary = Color.White,
            background = Color(0xFFF3F6FB),
            surface = Color.White,
            surfaceContainer = Color(0xFFEFF4FB),
            surfaceContainerHigh = Color(0xFFE5EDFA),
            secondaryContainer = Color(0xFFDFEBFF),
            onSecondaryContainer = Color(0xFF0057B8),
            onSurface = Color(0xFF0D2038),
            onSurfaceVariant = Color(0xFF4D6079),
            outline = Color(0xFF7B8CA4),
            outlineVariant = Color(0xFFD8E2EF),
        )
