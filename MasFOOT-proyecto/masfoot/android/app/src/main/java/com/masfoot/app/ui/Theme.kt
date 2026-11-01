package com.masfoot.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fondo verde intenso, letras negras, tarjetas en distintas tonalidades de verde.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B3D1E),
    onPrimary = Color.White,
    secondary = Color(0xFF0F6B2F),
    onSecondary = Color.White,
    background = Color(0xFF1FD655),
    onBackground = Color.Black,
    surface = Color(0xFF7DF2A2),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFF5CE08A),
    onSurfaceVariant = Color.Black,
    secondaryContainer = Color(0xFFB6F8CD),
    onSecondaryContainer = Color.Black,
    tertiaryContainer = Color(0xFF3FCB73),
    onTertiaryContainer = Color.Black,
    outline = Color(0xFF0B3D1E),
    error = Color(0xFF8B0000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2EE66B),
    onPrimary = Color.Black,
    secondary = Color(0xFF2EE66B),
    onSecondary = Color.Black,
    background = Color(0xFF0A2412),
    onBackground = Color.White,
    surface = Color(0xFF14532D),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1B6B3A),
    onSurfaceVariant = Color.White,
    secondaryContainer = Color(0xFF1E6B3A),
    onSecondaryContainer = Color.White,
    tertiaryContainer = Color(0xFF238A4A),
    onTertiaryContainer = Color.White,
    outline = Color(0xFF2EE66B),
    error = Color(0xFFFF8A8A),
)

@Composable
fun MasFootTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
