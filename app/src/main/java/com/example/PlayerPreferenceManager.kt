package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cinema_player_settings")

class PlayerPreferenceManager(private val context: Context) {

    companion object {
        val KEY_SELECTED_THEME_ID = stringPreferencesKey("selected_theme_id")
        val KEY_SELECTED_ASPECT_RATIO = stringPreferencesKey("selected_aspect_ratio")
        val KEY_SELECTED_RESIZE_MODE = stringPreferencesKey("selected_resize_mode")

        // Tema: Cinema positions
        val KEY_CINEMA_LEFT = floatPreferencesKey("cinema_left")
        val KEY_CINEMA_TOP = floatPreferencesKey("cinema_top")
        val KEY_CINEMA_WIDTH = floatPreferencesKey("cinema_width")
        val KEY_CINEMA_HEIGHT = floatPreferencesKey("cinema_height")
        val KEY_CINEMA_DIM = floatPreferencesKey("cinema_dim_alpha")

        // Tema: Cosy Cabin positions
        val KEY_CABIN_LEFT = floatPreferencesKey("cabin_left")
        val KEY_CABIN_TOP = floatPreferencesKey("cabin_top")
        val KEY_CABIN_WIDTH = floatPreferencesKey("cabin_width")
        val KEY_CABIN_HEIGHT = floatPreferencesKey("cabin_height")
        val KEY_CABIN_DIM = floatPreferencesKey("cabin_dim_alpha")

        // Tema: Sports Arena positions
        val KEY_SPORTS_LEFT = floatPreferencesKey("sports_left")
        val KEY_SPORTS_TOP = floatPreferencesKey("sports_top")
        val KEY_SPORTS_WIDTH = floatPreferencesKey("sports_width")
        val KEY_SPORTS_HEIGHT = floatPreferencesKey("sports_height")
        val KEY_SPORTS_DIM = floatPreferencesKey("sports_dim_alpha")
    }

    /**
     * Emits the currently selected theme ID (defaults to "cinema").
     */
    val selectedThemeId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_THEME_ID] ?: "cinema"
    }

    /**
     * Emits the currently selected aspect ratio preset (defaults to "free").
     */
    val selectedAspectRatioId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_ASPECT_RATIO] ?: "free"
    }

    /**
     * Emits the currently selected resize mode for the video player (defaults to "adaptive").
     */
    val selectedResizeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_RESIZE_MODE] ?: "adaptive"
    }

    /**
     * Emits the specific layout settings for the given theme ID.
     */
    fun getLayoutSettings(themeId: String): Flow<ScreenLayoutSettings> {
        val preset = ThemePresets.getById(themeId)
        return context.dataStore.data.map { preferences ->
            when (themeId.lowercase()) {
                "cosy_cabin" -> ScreenLayoutSettings(
                    left = preferences[KEY_CABIN_LEFT] ?: preset.defaultLeft,
                    top = preferences[KEY_CABIN_TOP] ?: preset.defaultTop,
                    width = preferences[KEY_CABIN_WIDTH] ?: preset.defaultWidth,
                    height = preferences[KEY_CABIN_HEIGHT] ?: preset.defaultHeight,
                    dimAlpha = preferences[KEY_CABIN_DIM] ?: preset.defaultDimAlpha,
                    subtitleOffset = preset.defaultSubtitleOffset
                )
                "sports_arena" -> ScreenLayoutSettings(
                    left = preferences[KEY_SPORTS_LEFT] ?: preset.defaultLeft,
                    top = preferences[KEY_SPORTS_TOP] ?: preset.defaultTop,
                    width = preferences[KEY_SPORTS_WIDTH] ?: preset.defaultWidth,
                    height = preferences[KEY_SPORTS_HEIGHT] ?: preset.defaultHeight,
                    dimAlpha = preferences[KEY_SPORTS_DIM] ?: preset.defaultDimAlpha,
                    subtitleOffset = preset.defaultSubtitleOffset
                )
                else -> ScreenLayoutSettings(
                    left = preferences[KEY_CINEMA_LEFT] ?: preset.defaultLeft,
                    top = preferences[KEY_CINEMA_TOP] ?: preset.defaultTop,
                    width = preferences[KEY_CINEMA_WIDTH] ?: preset.defaultWidth,
                    height = preferences[KEY_CINEMA_HEIGHT] ?: preset.defaultHeight,
                    dimAlpha = preferences[KEY_CINEMA_DIM] ?: preset.defaultDimAlpha,
                    subtitleOffset = preset.defaultSubtitleOffset
                )
            }
        }
    }

    /**
     * Saves the chosen active theme.
     */
    suspend fun saveSelectedTheme(themeId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_THEME_ID] = themeId
        }
    }

    /**
     * Saves the chosen aspect ratio preset.
     */
    suspend fun saveSelectedAspectRatio(aspectRatioId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_ASPECT_RATIO] = aspectRatioId
        }
    }

    /**
     * Saves the chosen resize mode preset.
     */
    suspend fun saveSelectedResizeMode(resizeModeId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_RESIZE_MODE] = resizeModeId
        }
    }

    /**
     * Updates customized layout coordinates for the given theme ID.
     */
    suspend fun saveLayoutSettings(themeId: String, settings: ScreenLayoutSettings) {
        context.dataStore.edit { preferences ->
            when (themeId.lowercase()) {
                "cosy_cabin" -> {
                    preferences[KEY_CABIN_LEFT] = settings.left
                    preferences[KEY_CABIN_TOP] = settings.top
                    preferences[KEY_CABIN_WIDTH] = settings.width
                    preferences[KEY_CABIN_HEIGHT] = settings.height
                    preferences[KEY_CABIN_DIM] = settings.dimAlpha
                }
                "sports_arena" -> {
                    preferences[KEY_SPORTS_LEFT] = settings.left
                    preferences[KEY_SPORTS_TOP] = settings.top
                    preferences[KEY_SPORTS_WIDTH] = settings.width
                    preferences[KEY_SPORTS_HEIGHT] = settings.height
                    preferences[KEY_SPORTS_DIM] = settings.dimAlpha
                }
                else -> {
                    preferences[KEY_CINEMA_LEFT] = settings.left
                    preferences[KEY_CINEMA_TOP] = settings.top
                    preferences[KEY_CINEMA_WIDTH] = settings.width
                    preferences[KEY_CINEMA_HEIGHT] = settings.height
                    preferences[KEY_CINEMA_DIM] = settings.dimAlpha
                }
            }
        }
    }

    /**
     * Resets layout parameters for the specified active theme to defaults.
     */
    suspend fun resetThemeToDefault(themeId: String) {
        context.dataStore.edit { preferences ->
            when (themeId.lowercase()) {
                "cosy_cabin" -> {
                    preferences.remove(KEY_CABIN_LEFT)
                    preferences.remove(KEY_CABIN_TOP)
                    preferences.remove(KEY_CABIN_WIDTH)
                    preferences.remove(KEY_CABIN_HEIGHT)
                    preferences.remove(KEY_CABIN_DIM)
                }
                "sports_arena" -> {
                    preferences.remove(KEY_SPORTS_LEFT)
                    preferences.remove(KEY_SPORTS_TOP)
                    preferences.remove(KEY_SPORTS_WIDTH)
                    preferences.remove(KEY_SPORTS_HEIGHT)
                    preferences.remove(KEY_SPORTS_DIM)
                }
                else -> {
                    preferences.remove(KEY_CINEMA_LEFT)
                    preferences.remove(KEY_CINEMA_TOP)
                    preferences.remove(KEY_CINEMA_WIDTH)
                    preferences.remove(KEY_CINEMA_HEIGHT)
                    preferences.remove(KEY_CINEMA_DIM)
                }
            }
        }
    }

    /**
     * Clears all settings from preferences, reverting to universal app defaults.
     */
    suspend fun resetAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
