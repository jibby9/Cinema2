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

        // Tema: Custom positions & image
        val KEY_CUSTOM_LEFT = floatPreferencesKey("custom_left")
        val KEY_CUSTOM_TOP = floatPreferencesKey("custom_top")
        val KEY_CUSTOM_WIDTH = floatPreferencesKey("custom_width")
        val KEY_CUSTOM_HEIGHT = floatPreferencesKey("custom_height")
        val KEY_CUSTOM_DIM = floatPreferencesKey("custom_dim_alpha")
        val KEY_CUSTOM_BACKGROUND_URI = stringPreferencesKey("custom_background_uri")

        // IPTV Settings
        val KEY_IPTV_MODE_ACTIVE = booleanPreferencesKey("iptv_mode_active")
        val KEY_IPTV_XTREAM_ACCOUNTS_JSON = stringPreferencesKey("iptv_xtream_accounts_json")
        val KEY_IPTV_M3U_PLAYLISTS_JSON = stringPreferencesKey("iptv_m3u_playlists_json")
        val KEY_IPTV_FAVORITES = stringSetPreferencesKey("iptv_favorites")
        val KEY_IPTV_LAST_CHANNEL_ID = stringPreferencesKey("iptv_last_channel_id")
        
        // Custom Category order and hidden groups
        val KEY_IPTV_CATEGORY_ORDER = stringPreferencesKey("iptv_category_order")
        val KEY_IPTV_HIDDEN_CATEGORIES = stringSetPreferencesKey("iptv_hidden_categories")

        // IPTV Sort modes and custom channel order
        val KEY_IPTV_CATEGORY_SORT_MODE = stringPreferencesKey("iptv_category_sort_mode")
        val KEY_IPTV_CHANNEL_SORT_MODE = stringPreferencesKey("iptv_channel_sort_mode")
        val KEY_IPTV_CHANNEL_ORDER = stringPreferencesKey("iptv_channel_order")

        // IPTV History saved
        val KEY_IPTV_HISTORY_JSON = stringPreferencesKey("iptv_history_json")

        // Animation Toggle
        val KEY_ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")

        // Terms of Service Tracking
        val KEY_TOS_ACCEPTED = booleanPreferencesKey("tos_accepted")
        val KEY_TOS_ACCEPTED_TIMESTAMP = longPreferencesKey("tos_accepted_timestamp")

        // Permissions Prompt Tracking
        val KEY_PERMISSIONS_PROMPTED = booleanPreferencesKey("permissions_prompted")
    }

    /**
     * Permissions Onboarding State
     */
    val isPermissionsPrompted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_PERMISSIONS_PROMPTED] ?: false
    }

    suspend fun savePermissionsPrompted(prompted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PERMISSIONS_PROMPTED] = prompted
        }
    }

    /**
     * Terms of Service Acceptance Fields
     */
    val isTosAccepted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_TOS_ACCEPTED] ?: false
    }

    val tosAcceptedTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_TOS_ACCEPTED_TIMESTAMP] ?: 0L
    }

    suspend fun saveTosAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOS_ACCEPTED] = accepted
            if (accepted) {
                preferences[KEY_TOS_ACCEPTED_TIMESTAMP] = System.currentTimeMillis()
            } else {
                preferences.remove(KEY_TOS_ACCEPTED_TIMESTAMP)
            }
        }
    }

    /**
     * IPTV: Stream order and hidden categories
     */
    val iptvCategoryOrder: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_IPTV_CATEGORY_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun saveIptvCategoryOrder(order: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IPTV_CATEGORY_ORDER] = order.joinToString(",")
        }
    }

    val iptvHiddenCategoryIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_IPTV_HIDDEN_CATEGORIES] ?: emptySet()
    }

    suspend fun saveIptvHiddenCategoryIds(hiddenSet: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IPTV_HIDDEN_CATEGORIES] = hiddenSet
        }
    }

    val iptvCategorySortMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_IPTV_CATEGORY_SORT_MODE] ?: "PROVIDER"
    }

    suspend fun saveIptvCategorySortMode(modeName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IPTV_CATEGORY_SORT_MODE] = modeName
        }
    }

    val iptvChannelSortMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_IPTV_CHANNEL_SORT_MODE] ?: "PROVIDER"
    }

    suspend fun saveIptvChannelSortMode(modeName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IPTV_CHANNEL_SORT_MODE] = modeName
        }
    }

    val iptvChannelOrder: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_IPTV_CHANNEL_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun saveIptvChannelOrder(order: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IPTV_CHANNEL_ORDER] = order.joinToString(",")
        }
    }

    /**
     * IPTV History: Serialized list of recently watched IPTV channels
     */
    val iptvHistoryJson: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_IPTV_HISTORY_JSON]
    }

    suspend fun saveIptvHistoryJson(jsonString: String?) {
        context.dataStore.edit { preferences ->
            if (jsonString == null) {
                preferences.remove(KEY_IPTV_HISTORY_JSON)
            } else {
                preferences[KEY_IPTV_HISTORY_JSON] = jsonString
            }
        }
    }

    /**
     * Animation: Check if background animations are enabled (defaults to true)
     */
    val isAnimationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ANIMATION_ENABLED] ?: true
    }

    suspend fun saveAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ANIMATION_ENABLED] = enabled
        }
    }

    /**
     * IPTV: Check if IPTV mode is active instead of the default external player dashboard
     */
    val isIptvModeActive: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IPTV_MODE_ACTIVE] ?: false
    }

    suspend fun saveIptvModeActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IPTV_MODE_ACTIVE] = active
        }
    }

    /**
     * IPTV: Saved Xtream accounts JSON string representation
     */
    val xtreamAccountsJson: Flow<String?> = context.dataStore.data.map { preferences ->
        val saved = preferences[KEY_IPTV_XTREAM_ACCOUNTS_JSON] ?: return@map null
        val decrypted = IptvSecurityManager.decrypt(saved)
        if (decrypted != null) {
            decrypted
        } else {
            // Decryption failed. Check if it starts with [ or { which indicates legacy unencrypted plain-text JSON
            val trimmed = saved.trim()
            if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                saved
            } else {
                null
            }
        }
    }

    suspend fun saveXtreamAccountsJson(jsonString: String?) {
        context.dataStore.edit { preferences ->
            if (jsonString == null) {
                preferences.remove(KEY_IPTV_XTREAM_ACCOUNTS_JSON)
            } else {
                val encrypted = IptvSecurityManager.encrypt(jsonString)
                if (encrypted != null) {
                    preferences[KEY_IPTV_XTREAM_ACCOUNTS_JSON] = encrypted
                } else {
                    preferences[KEY_IPTV_XTREAM_ACCOUNTS_JSON] = jsonString
                }
            }
        }
    }

    /**
     * IPTV: Saved M3U playlists JSON string representation
     */
    val m3uPlaylistsJson: Flow<String?> = context.dataStore.data.map { preferences ->
        val saved = preferences[KEY_IPTV_M3U_PLAYLISTS_JSON] ?: return@map null
        val decrypted = IptvSecurityManager.decrypt(saved)
        if (decrypted != null) {
            decrypted
        } else {
            // Decryption failed. Check if it starts with [ or { which indicates legacy unencrypted plain-text JSON
            val trimmed = saved.trim()
            if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                saved
            } else {
                null
            }
        }
    }

    suspend fun saveM3uPlaylistsJson(jsonString: String?) {
        context.dataStore.edit { preferences ->
            if (jsonString == null) {
                preferences.remove(KEY_IPTV_M3U_PLAYLISTS_JSON)
            } else {
                val encrypted = IptvSecurityManager.encrypt(jsonString)
                if (encrypted != null) {
                    preferences[KEY_IPTV_M3U_PLAYLISTS_JSON] = encrypted
                } else {
                    preferences[KEY_IPTV_M3U_PLAYLISTS_JSON] = jsonString
                }
            }
        }
    }

    /**
     * IPTV: Favorite channel IDs
     */
    val favoriteChannelIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_IPTV_FAVORITES] ?: emptySet()
    }

    suspend fun saveFavoriteChannelIds(favorites: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IPTV_FAVORITES] = favorites
        }
    }

    /**
     * IPTV: Last played channel ID
     */
    val lastChannelId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_IPTV_LAST_CHANNEL_ID]
    }

    suspend fun saveLastChannelId(channelId: String?) {
        context.dataStore.edit { preferences ->
            if (channelId == null) {
                preferences.remove(KEY_IPTV_LAST_CHANNEL_ID)
            } else {
                preferences[KEY_IPTV_LAST_CHANNEL_ID] = channelId
            }
        }
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
     * Emits the custom theme background image URI string (if set).
     */
    val customBackgroundUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_BACKGROUND_URI]
    }

    suspend fun saveCustomBackgroundUri(uriString: String?) {
        context.dataStore.edit { preferences ->
            if (uriString == null) {
                preferences.remove(KEY_CUSTOM_BACKGROUND_URI)
            } else {
                preferences[KEY_CUSTOM_BACKGROUND_URI] = uriString
            }
        }
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
                "custom" -> ScreenLayoutSettings(
                    left = preferences[KEY_CUSTOM_LEFT] ?: preset.defaultLeft,
                    top = preferences[KEY_CUSTOM_TOP] ?: preset.defaultTop,
                    width = preferences[KEY_CUSTOM_WIDTH] ?: preset.defaultWidth,
                    height = preferences[KEY_CUSTOM_HEIGHT] ?: preset.defaultHeight,
                    dimAlpha = preferences[KEY_CUSTOM_DIM] ?: preset.defaultDimAlpha,
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
                "custom" -> {
                    preferences[KEY_CUSTOM_LEFT] = settings.left
                    preferences[KEY_CUSTOM_TOP] = settings.top
                    preferences[KEY_CUSTOM_WIDTH] = settings.width
                    preferences[KEY_CUSTOM_HEIGHT] = settings.height
                    preferences[KEY_CUSTOM_DIM] = settings.dimAlpha
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
                "custom" -> {
                    preferences.remove(KEY_CUSTOM_LEFT)
                    preferences.remove(KEY_CUSTOM_TOP)
                    preferences.remove(KEY_CUSTOM_WIDTH)
                    preferences.remove(KEY_CUSTOM_HEIGHT)
                    preferences.remove(KEY_CUSTOM_DIM)
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
