package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val DayQuestShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), // DayQuest 12dp card radius
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

fun createDayQuestLightScheme(accent: Color = AccentIndigo) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.12f),
    onPrimaryContainer = accent,
    secondary = accent.copy(alpha = 0.85f),
    onSecondary = Color.White,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightCardBackground,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = LightBorder,
    outlineVariant = LightBorder.copy(alpha = 0.6f)
)

fun createDayQuestDarkScheme(accent: Color = AccentIndigo, amoled: Boolean = false) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,
    secondary = accent.copy(alpha = 0.85f),
    onSecondary = Color.White,
    background = if (amoled) Color.Black else DarkBackground,
    onBackground = DarkOnSurface,
    surface = if (amoled) Color(0xFF0A0C10) else DarkCardBackground,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorder.copy(alpha = 0.6f)
)

@Composable
fun DayQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = AccentIndigo,
    amoled: Boolean = false,
    dynamicColor: Boolean = false, // Master prompt: do not assume dynamic is default; accent is default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createDayQuestDarkScheme(accent = accentColor, amoled = amoled)
        else -> createDayQuestLightScheme(accent = accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DayQuestShapes,
        content = content
    )
}
