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
    val foregroundImageResId: Int? = null,
    val isAnimated: Boolean = false,
    val animationType: String? = null,
    val backdropImageUri: String? = null
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

    val Custom = ThemePreset(
        id = "custom",
        name = "Custom Theme",
        description = "Set your own personal backdrop with custom pictures, custom scale, and custom layout positions.",
        defaultLeft = 0.10f,
        defaultTop = 0.15f,
        defaultWidth = 0.80f,
        defaultHeight = 0.45f,
        defaultDimAlpha = 0.70f,
        defaultSubtitleOffset = 0.05f,
        primaryColor = Color(0xFFEC4899), // Pink / Rose Accent
        secondaryColor = Color(0xFF4C0519), // Deep Rose Accent
        cornerRadiusDp = 12,
        frameThicknessDp = 5,
        frameColor = Color(0xFF1E1B20), // Dark grey border with soft warmth
        glowColor = Color(0x3BEC4899), // Neon Rose flow and drop glow
        glowRadiusDp = 12,
        shadowIntensity = 0.45f,
        vignetteStrength = 0.6f,
        ambientColorTint = Color(0x13EC4899), // Muted Pink ambient tint
        backdropImageResId = null,
        foregroundImageResId = null
    )

    val Aurora = ThemePreset(
        id = "aurora",
        name = "Aurora Borealis",
        description = "Ethereal northern lights with moving organic ribbons of neon green, blue, and deep purple.",
        defaultLeft = 0.10f,
        defaultTop = 0.15f,
        defaultWidth = 0.80f,
        defaultHeight = 0.45f,
        defaultDimAlpha = 0.60f,
        defaultSubtitleOffset = 0.05f,
        primaryColor = Color(0xFF10B981), // Green Aurora
        secondaryColor = Color(0xFF4C1D95), // Deep Purple
        cornerRadiusDp = 12,
        frameThicknessDp = 5,
        frameColor = Color(0xFF1E293B),
        glowColor = Color(0x4410B981),
        glowRadiusDp = 16,
        shadowIntensity = 0.45f,
        vignetteStrength = 0.5f,
        ambientColorTint = Color(0x1010B981),
        isAnimated = true,
        animationType = "aurora"
    )

    val Matrix = ThemePreset(
        id = "matrix",
        name = "Matrix Rain",
        description = "Falling green cyber streams of digital rain pulsing on an obsidian terminal backdrop.",
        defaultLeft = 0.10f,
        defaultTop = 0.15f,
        defaultWidth = 0.80f,
        defaultHeight = 0.45f,
        defaultDimAlpha = 0.85f,
        defaultSubtitleOffset = 0.05f,
        primaryColor = Color(0xFF00FF41), // Matrix Green
        secondaryColor = Color(0xFF0D1117), // Deep Dark Grey
        cornerRadiusDp = 0, // Cyber brutalist
        frameThicknessDp = 4,
        frameColor = Color(0xFF0F172A),
        glowColor = Color(0x3B00FF41),
        glowRadiusDp = 10,
        shadowIntensity = 0.6f,
        vignetteStrength = 0.7f,
        ambientColorTint = Color(0x1A00FF41),
        isAnimated = true,
        animationType = "matrix"
    )

    val Stardust = ThemePreset(
        id = "stardust",
        name = "Cosmic Stardust",
        description = "Slow orbital twinkling stars and celestial particles drifting gently across the cosmos.",
        defaultLeft = 0.10f,
        defaultTop = 0.12f,
        defaultWidth = 0.80f,
        defaultHeight = 0.48f,
        defaultDimAlpha = 0.50f,
        defaultSubtitleOffset = 0.05f,
        primaryColor = Color(0xFFA5B4FC), // Lavender / Cosmic Blue
        secondaryColor = Color(0xFF1E1B4B), // Deep Blue Indigo
        cornerRadiusDp = 16,
        frameThicknessDp = 6,
        frameColor = Color(0xFF020617),
        glowColor = Color(0x40818CF8),
        glowRadiusDp = 18,
        shadowIntensity = 0.4f,
        vignetteStrength = 0.4f,
        ambientColorTint = Color(0x15818CF8),
        isAnimated = true,
        animationType = "stardust"
    )

    val allDefaults = listOf(Cinema, CosyCabin, SportsArena, Custom, Aurora, Matrix, Stardust)

    private val customThemesList = java.util.Collections.synchronizedList(mutableListOf<ThemePreset>())

    fun setCustomThemes(themes: List<ThemePreset>) {
        synchronized(customThemesList) {
            customThemesList.clear()
            customThemesList.addAll(themes)
        }
    }

    val all: List<ThemePreset>
        get() = allDefaults + synchronized(customThemesList) { customThemesList.toList() }

    fun getById(id: String): ThemePreset {
        return all.find { it.id.lowercase() == id.lowercase() } ?: Cinema
    }
}

object CustomThemePersistence {
    private const val FILENAME = "custom_themes_db_v2.json"

    fun loadThemes(context: android.content.Context): List<ThemePreset> {
        val file = java.io.File(context.filesDir, FILENAME)
        if (!file.exists()) return emptyList()
        val list = mutableListOf<ThemePreset>()
        try {
            val jsonStr = file.readText()
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ThemePreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", "User custom designed theme."),
                        defaultLeft = obj.optDouble("defaultLeft", 0.10).toFloat(),
                        defaultTop = obj.optDouble("defaultTop", 0.15).toFloat(),
                        defaultWidth = obj.optDouble("defaultWidth", 0.80).toFloat(),
                        defaultHeight = obj.optDouble("defaultHeight", 0.45).toFloat(),
                        defaultDimAlpha = obj.optDouble("defaultDimAlpha", 0.70).toFloat(),
                        defaultSubtitleOffset = obj.optDouble("defaultSubtitleOffset", 0.05).toFloat(),
                        primaryColor = Color(obj.getLong("primaryColor")),
                        secondaryColor = Color(obj.getLong("secondaryColor")),
                        cornerRadiusDp = obj.optInt("cornerRadiusDp", 8),
                        frameThicknessDp = obj.optInt("frameThicknessDp", 4),
                        frameColor = Color(obj.optLong("frameColor", 0xFF0F172AL)),
                        glowColor = Color(obj.optLong("glowColor", 0x406366F1L)),
                        glowRadiusDp = obj.optInt("glowRadiusDp", 16),
                        shadowIntensity = obj.optDouble("shadowIntensity", 0.5).toFloat(),
                        vignetteStrength = obj.optDouble("vignetteStrength", 0.4).toFloat(),
                        ambientColorTint = Color(obj.optLong("ambientColorTint", 0L)),
                        isAnimated = obj.optBoolean("isAnimated", false),
                        animationType = obj.optString("animationType", null),
                        backdropImageUri = obj.optString("backdropImageUri", null)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomThemePersistence", "Error loading custom themes", e)
        }
        return list
    }

    fun saveThemes(context: android.content.Context, themes: List<ThemePreset>) {
        val file = java.io.File(context.filesDir, FILENAME)
        try {
            val array = org.json.JSONArray()
            for (t in themes) {
                val obj = org.json.JSONObject()
                obj.put("id", t.id)
                obj.put("name", t.name)
                obj.put("description", t.description)
                obj.put("defaultLeft", t.defaultLeft.toDouble())
                obj.put("defaultTop", t.defaultTop.toDouble())
                obj.put("defaultWidth", t.defaultWidth.toDouble())
                obj.put("defaultHeight", t.defaultHeight.toDouble())
                obj.put("defaultDimAlpha", t.defaultDimAlpha.toDouble())
                obj.put("defaultSubtitleOffset", t.defaultSubtitleOffset.toDouble())
                obj.put("primaryColor", t.primaryColor.value.toLong())
                obj.put("secondaryColor", t.secondaryColor.value.toLong())
                obj.put("cornerRadiusDp", t.cornerRadiusDp)
                obj.put("frameThicknessDp", t.frameThicknessDp)
                obj.put("frameColor", t.frameColor.value.toLong())
                obj.put("glowColor", t.glowColor.value.toLong())
                obj.put("glowRadiusDp", t.glowRadiusDp)
                obj.put("shadowIntensity", t.shadowIntensity.toDouble())
                obj.put("vignetteStrength", t.vignetteStrength.toDouble())
                obj.put("ambientColorTint", t.ambientColorTint.value.toLong())
                obj.put("isAnimated", t.isAnimated)
                obj.put("animationType", t.animationType)
                if (t.backdropImageUri != null) {
                    obj.put("backdropImageUri", t.backdropImageUri)
                }
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            android.util.Log.e("CustomThemePersistence", "Error saving custom themes", e)
        }
    }
}
