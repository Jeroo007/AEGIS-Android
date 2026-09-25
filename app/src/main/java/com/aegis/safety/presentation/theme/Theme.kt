package com.aegis.safety.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AegisPrimary,
    onPrimary = Color.White,
    primaryContainer = AegisPrimary,
    onPrimaryContainer = Color.White,
    secondary = AegisSecondary,
    onSecondary = Color.White,
    tertiary = AegisAccent,
    onTertiary = Color.White,
    error = AegisDanger,
    onError = Color.White,
    background = AegisSurfaceLight,
    onBackground = Color.Black,
    surface = AegisCardLight,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5EA), // iOS light gray
    onSurfaceVariant = Color(0xFF8E8E93), // iOS secondary text
    outline = Color(0xFFC6C6C8) // iOS divider
)

private val DarkColors = darkColorScheme(
    primary = AegisPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = AegisPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = AegisSecondary,
    onSecondary = Color.White,
    tertiary = AegisAccent,
    onTertiary = Color.White,
    error = AegisDangerDark,
    onError = Color.White,
    background = AegisSurfaceDark,
    onBackground = Color.White,
    surface = AegisCardDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E), // iOS dark gray
    onSurfaceVariant = Color(0xFF8E8E93), // iOS secondary text
    outline = Color(0xFF38383A) // iOS dark divider
)

val AegisShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun AegisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AegisTypography,
        shapes = AegisShapes,
        content = content
    )
}