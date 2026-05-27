package com.example

import androidx.compose.ui.graphics.Color

/**
 * Technical specification for a themed ambient viewing backdrop preset.
 */
data class ThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val defaultLeft: Float,
    val defaultTop: Float,
    val defaultWidth: Float,
    val defaultHeight: Float,
    val defaultDimAlpha: Float,
    val defaultSubtitleOffset: Float, // safe-area offset percentage
    val primaryColor: Color,
    val secondaryColor: Color
)

/**
 * Editable screen placement and size configuration represented as float percentages (0.0f - 1.0f).
 */
data class ScreenLayoutSettings(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val dimAlpha: Float,
    val subtitleOffset: Float
)

/**
 * Preset catalog definitions.
 */
object ThemePresets {
    val Cinema = ThemePreset(
        id = "cinema",
        name = "Cinema",
        description = "A premium virtual theater frame with soft projector light and deep, quiet ambient glows.",
        defaultLeft = 0.10f,
        defaultTop = 0.15f,
        defaultWidth = 0.80f,
        defaultHeight = 0.45f,
        defaultDimAlpha = 0.80f,
        defaultSubtitleOffset = 0.05f,
        primaryColor = Color(0xFF6366F1), // Indigo
        secondaryColor = Color(0xFF1E1B4B) // Dark Indigo
    )

    val CosyCabin = ThemePreset(
        id = "cosy_cabin",
        name = "Cosy Cabin",
        description = "A rustic wooden cabin with fireplace hearth heat radiating around the viewport.",
        defaultLeft = 0.15f,
        defaultTop = 0.10f,
        defaultWidth = 0.70f,
        defaultHeight = 0.42f,
        defaultDimAlpha = 0.50f,
        defaultSubtitleOffset = 0.08f,
        primaryColor = Color(0xFFF97316), // Warm Orange
        secondaryColor = Color(0xFF451A03) // Deep Brown
    )

    val SportsArena = ThemePreset(
        id = "sports_arena",
        name = "Sports Arena",
        description = "A giant Jumbotron screen hovering high above field grass turf with floodlight glows.",
        defaultLeft = 0.05f,
        defaultTop = 0.08f,
        defaultWidth = 0.90f,
        defaultHeight = 0.52f,
        defaultDimAlpha = 0.35f,
        defaultSubtitleOffset = 0.03f,
        primaryColor = Color(0xFF10B981), // Emerald Green
        secondaryColor = Color(0xFF064E3B) // Dark Stadium Green
    )

    val all = listOf(Cinema, CosyCabin, SportsArena)

    fun getById(id: String): ThemePreset {
        return all.find { it.id.lowercase() == id.lowercase() } ?: Cinema
    }
}
