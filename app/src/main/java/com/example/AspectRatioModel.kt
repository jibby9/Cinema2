package com.example

/**
 * Aspect ratio presets matching user requirements.
 */
enum class AspectRatioPreset(val id: String, val displayName: String, val ratio: Float?) {
    FREE("free", "Free", null),
    RATIO_16_9("16_9", "16:9", 16f / 9f),
    RATIO_21_9("21_9", "21:9", 21f / 9f),
    RATIO_4_3("4_3", "4:3", 4f / 3f),
    RATIO_1_1("1_1", "1:1", 1f / 1f);

    companion object {
        fun getById(id: String): AspectRatioPreset {
            return entries.find { it.id.lowercase() == id.lowercase() } ?: FREE
        }
    }
}
