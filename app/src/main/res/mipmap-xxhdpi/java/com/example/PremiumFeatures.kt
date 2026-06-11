package com.example

import android.content.Context
import android.util.Log
import com.squareup.moshi.JsonClass
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data model representing a saved layout configuration for the movable player.
 */
data class LayoutPreset(
    val id: String,
    val name: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val dimAlpha: Float,
    val displayMode: String,
    val themeId: String,
    val miniCorner: Int = 0,
    val isBuiltIn: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("left", left.toDouble())
        obj.put("top", top.toDouble())
        obj.put("width", width.toDouble())
        obj.put("height", height.toDouble())
        obj.put("dimAlpha", dimAlpha.toDouble())
        obj.put("displayMode", displayMode)
        obj.put("themeId", themeId)
        obj.put("miniCorner", miniCorner)
        obj.put("isBuiltIn", isBuiltIn)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): LayoutPreset {
            return LayoutPreset(
                id = obj.getString("id"),
                name = obj.getString("name"),
                left = obj.getDouble("left").toFloat(),
                top = obj.getDouble("top").toFloat(),
                width = obj.getDouble("width").toFloat(),
                height = obj.getDouble("height").toFloat(),
                dimAlpha = obj.optDouble("dimAlpha", 0.5).toFloat(),
                displayMode = obj.optString("displayMode", "adaptive"),
                themeId = obj.optString("themeId", "cinema"),
                miniCorner = obj.optInt("miniCorner", 0),
                isBuiltIn = obj.optBoolean("isBuiltIn", false)
            )
        }

        /**
         * The list of built-in quick presets.
         */
        val BuiltInPresets = listOf(
            LayoutPreset(
                id = "builtin_top_full",
                name = "Top Full Width",
                left = 0.0f,
                top = 0.02f,
                width = 1.0f,
                height = 0.35f,
                dimAlpha = 0.4f,
                displayMode = "adaptive",
                themeId = "cinema",
                isBuiltIn = true
            ),
            LayoutPreset(
                id = "builtin_center_stage",
                name = "Center Stage",
                left = 0.02f,
                top = 0.15f,
                width = 0.96f,
                height = 0.60f,
                dimAlpha = 0.2f,
                displayMode = "adaptive",
                themeId = "cinema",
                isBuiltIn = true
            ),
            LayoutPreset(
                id = "builtin_bottom_cinema",
                name = "Bottom Cinema",
                left = 0.0f,
                top = 0.52f,
                width = 1.0f,
                height = 0.38f,
                dimAlpha = 0.8f,
                displayMode = "adaptive",
                themeId = "cinema",
                isBuiltIn = true
            ),
            LayoutPreset(
                id = "builtin_corner_mini",
                name = "Corner Mini",
                left = 0.65f,
                top = 0.05f,
                width = 0.32f,
                height = 0.24f,
                dimAlpha = 0.1f,
                displayMode = "adaptive",
                themeId = "cinema",
                isBuiltIn = true
            )
        )
    }
}

/**
 * Data model for an EPG live guide reminder.
 */
data class IptvReminder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val channelId: String,
    val channelName: String,
    val channelStreamUrl: String,
    val programTitle: String,
    val programStartMs: Long,
    val programEndMs: Long,
    val leadTimeMinutes: Int // 0, 5, 10
) {
    val triggerTimeMs: Long
        get() = programStartMs - (leadTimeMinutes * 60 * 1000L)

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("channelId", channelId)
        obj.put("channelName", channelName)
        obj.put("channelStreamUrl", channelStreamUrl)
        obj.put("programTitle", programTitle)
        obj.put("programStartMs", programStartMs)
        obj.put("programEndMs", programEndMs)
        obj.put("leadTimeMinutes", leadTimeMinutes)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): IptvReminder {
            return IptvReminder(
                id = obj.getString("id"),
                channelId = obj.getString("channelId"),
                channelName = obj.getString("channelName"),
                channelStreamUrl = obj.getString("channelStreamUrl"),
                programTitle = obj.getString("programTitle"),
                programStartMs = obj.getLong("programStartMs"),
                programEndMs = obj.getLong("programEndMs"),
                leadTimeMinutes = obj.optInt("leadTimeMinutes", 0)
            )
        }
    }
}

/**
 * Options representing the selected ambient backlighting glow intensity.
 */
enum class AmbientGlowSetting {
    OFF,
    SUBTLE,
    MEDIUM
}

/**
 * Persistent helper using SharedPreferences local storage to store presets and reminders.
 * This ensures clean separation and avoids any build or data migration challenges.
 */
class PremiumFeaturesStore(context: Context) {
    private val prefs = context.getSharedPreferences("premium_features_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_PRESETS = "custom_presets_json"
        private const val KEY_LAST_PRESET_ID = "last_preset_id"
        private const val KEY_REMINDERS = "reminders_json"
        private const val KEY_AMBIENT_GLOW = "ambient_glow_intensity"
    }

    /**
     * Gets all custom saved presets.
     */
    fun loadCustomPresets(): List<LayoutPreset> {
        val json = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()
        val list = mutableListOf<LayoutPreset>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                list.add(LayoutPreset.fromJsonObject(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            Log.e("PremiumFeaturesStore", "Error loading custom presets", e)
        }
        return list
    }

    /**
     * Preserves a list of user custom presets.
     */
    fun saveCustomPresets(presets: List<LayoutPreset>) {
        try {
            val arr = JSONArray()
            presets.forEach { arr.put(it.toJsonObject()) }
            prefs.edit().putString(KEY_CUSTOM_PRESETS, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("PremiumFeaturesStore", "Error saving custom presets", e)
        }
    }

    /**
     * Reads/writes last applied layout preset identifier.
     */
    fun getLastPresetId(): String? = prefs.getString(KEY_LAST_PRESET_ID, null)
    fun saveLastPresetId(id: String?) = prefs.edit().putString(KEY_LAST_PRESET_ID, id).apply()

    /**
     * Recalls all scheduled IPTV programme reminders.
     */
    fun loadReminders(): List<IptvReminder> {
        val json = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        val list = mutableListOf<IptvReminder>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                list.add(IptvReminder.fromJsonObject(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            Log.e("PremiumFeaturesStore", "Error loading reminders", e)
        }
        return list
    }

    /**
     * Persists the reminders list to local storage.
     */
    fun saveReminders(reminders: List<IptvReminder>) {
        try {
            val arr = JSONArray()
            reminders.forEach { arr.put(it.toJsonObject()) }
            prefs.edit().putString(KEY_REMINDERS, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("PremiumFeaturesStore", "Error saving reminders", e)
        }
    }

    /**
     * Reads the user-specified ambient light option (defaults to SUBTLE).
     */
    fun getAmbientGlow(): AmbientGlowSetting {
        val name = prefs.getString(KEY_AMBIENT_GLOW, AmbientGlowSetting.SUBTLE.name)
        return try {
            AmbientGlowSetting.valueOf(name ?: AmbientGlowSetting.SUBTLE.name)
        } catch (e: Exception) {
            AmbientGlowSetting.SUBTLE
        }
    }

    /**
     * Saves selected ambient light glow setting.
     */
    fun saveAmbientGlow(setting: AmbientGlowSetting) {
        prefs.edit().putString(KEY_AMBIENT_GLOW, setting.name).apply()
    }
}
