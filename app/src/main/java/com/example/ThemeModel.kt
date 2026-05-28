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
    val secondaryColor: Color,
    // Premium theme-specific visual polish properties
    val cornerRadiusDp: Int = 8,
    val frameThicknessDp: Int = 4,
    val frameColor: Color = Color(0xFF0F172A),
    val glowColor: Color = Color(0x406366F1),
    val glowRadiusDp: Int = 16,
    val shadowIntensity: Float = 0.5f,
    val vignetteStrength: Float = 0.4f,
    val ambientColorTint: Color = Color.Transparent,
    val backdropImageResId: Int? = null,
    val foregroundImageResId: Int? = null
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
        secondaryColor = Color(0xFF1E1B4B), // Dark Indigo
        cornerRadiusDp = 8,
        frameThicknessDp = 6,
        frameColor = Color(0xFF0C0A09), // Matte black
        glowColor = Color(0x3B6366F1), // Subtly reduced alpha (approx 23%)
        glowRadiusDp = 10, // Noticeably smaller glow radius
        shadowIntensity = 0.4f, // Reduced intensity
        vignetteStrength = 0.7f,
        ambientColorTint = Color(0x1A4F46E5) // Soft Indigo projector tint
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
        secondaryColor = Color(0xFF451A03), // Deep Brown
        cornerRadiusDp = 16,
        frameThicknessDp = 10,
        frameColor = Color(0xFF451A03), // Warm wooden timber frame
        glowColor = Color(0x33F97316), // Subtly reduced alpha (20%)
        glowRadiusDp = 12, // Noticeably smaller glow radius
        shadowIntensity = 0.35f, // Reduced intensity
        vignetteStrength = 0.5f,
        ambientColorTint = Color(0x1F7C2D12) // Warm brown amber tint
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
        secondaryColor = Color(0xFF064E3B), // Dark Stadium Green
        cornerRadiusDp = 4,
        frameThicknessDp = 8,
        frameColor = Color(0xFF1E293B), // Sleek metal frame
        glowColor = Color(0x2E10B981), // Subtly reduced alpha (18%)
        glowRadiusDp = 8, // Noticeably smaller glow radius
        shadowIntensity = 0.25f, // Reduced intensity
        vignetteStrength = 0.3f,
        ambientColorTint = Color(0x1534D399) // Cool green stadium tint
    )

    val all = listOf(Cinema, CosyCabin, SportsArena)

    fun getById(id: String): ThemePreset {
        return all.find { it.id.lowercase() == id.lowercase() } ?: Cinema
    }
}
