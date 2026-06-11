package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    secondary = IndigoSecondary,
    onSecondary = Color.White,
    background = AubergineDarkBg,
    onBackground = TextSilver,
    surface = ObsidianSurface,
    onSurface = TextSilver,
    error = BrightCoral,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SophisticatedDarkColorScheme,
        typography = Typography,
        content = content
    )
}
