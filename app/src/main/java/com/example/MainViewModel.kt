package com.example

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferenceManager = PlayerPreferenceManager(application)

    private val _parsedIntent = MutableStateFlow<ParsedIntentInfo?>(null)
    val parsedIntent: StateFlow<ParsedIntentInfo?> = _parsedIntent.asStateFlow()

    private val _playableUri = MutableStateFlow<String?>(null)
    val playableUri: StateFlow<String?> = _playableUri.asStateFlow()

    private val _currentPlayingChannel = MutableStateFlow<IptvChannel?>(null)
    val currentPlayingChannel: StateFlow<IptvChannel?> = _currentPlayingChannel.asStateFlow()

    private val _requestHeaders = MutableStateFlow<Map<String, String>>(emptyMap())
    val requestHeaders: StateFlow<Map<String, String>> = _requestHeaders.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showDebugPanel = MutableStateFlow(true)
    val showDebugPanel: StateFlow<Boolean> = _showDebugPanel.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _isSettingsLoaded = MutableStateFlow(false)
    val isSettingsLoaded: StateFlow<Boolean> = _isSettingsLoaded.asStateFlow()

    // Themes states
    private val _activeThemeId = MutableStateFlow("cinema")
    val activeThemeId: StateFlow<String> = _activeThemeId.asStateFlow()

    private val _activeThemePreset = MutableStateFlow(ThemePresets.Cinema)
    val activeThemePreset: StateFlow<ThemePreset> = _activeThemePreset.asStateFlow()

    private val _customBgUri = MutableStateFlow<String?>(null)
    val customBgUri: StateFlow<String?> = _customBgUri.asStateFlow()

    private val _activeAspectRatioId = MutableStateFlow("free")
    val activeAspectRatioId: StateFlow<String> = _activeAspectRatioId.asStateFlow()

    private val _activeResizeMode = MutableStateFlow("adaptive")
    val activeResizeMode: StateFlow<String> = _activeResizeMode.asStateFlow()

    private val _screenLayout = MutableStateFlow(
        ScreenLayoutSettings(
            ThemePresets.Cinema.defaultLeft,
            ThemePresets.Cinema.defaultTop,
            ThemePresets.Cinema.defaultWidth,
            ThemePresets.Cinema.defaultHeight,
            ThemePresets.Cinema.defaultDimAlpha,
            ThemePresets.Cinema.defaultSubtitleOffset
        )
    )
    val screenLayout: StateFlow<ScreenLayoutSettings> = _screenLayout.asStateFlow()

    // Default video stream URL for test playback
    val defaultTestVideoUrl = "https://sandbox-videos.web.cern.ch/record/2241796"

    // IPTV States
    private val _isIptvModeActive = MutableStateFlow(false)
    val isIptvModeActive: StateFlow<Boolean> = _isIptvModeActive.asStateFlow()

    private val _activeIptvTab = MutableStateFlow(0)
    val activeIptvTab: StateFlow<Int> = _activeIptvTab.asStateFlow()

    fun setActiveIptvTab(tab: Int) {
        _activeIptvTab.value = tab
    }

    private val _xtreamAccounts = MutableStateFlow<List<XtreamAccount>>(emptyList())
    val xtreamAccounts: StateFlow<List<XtreamAccount>> = _xtreamAccounts.asStateFlow()

    private val _m3uPlaylists = MutableStateFlow<List<M3UConfig>>(emptyList())
    val m3uPlaylists: StateFlow<List<M3UConfig>> = _m3uPlaylists.asStateFlow()

    private val _favoriteChannelIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteChannelIds: StateFlow<Set<String>> = _favoriteChannelIds.asStateFlow()

    private val _lastChannelId = MutableStateFlow<String?>(null)
    val lastChannelId: StateFlow<String?> = _lastChannelId.asStateFlow()

    private val _iptvCategories = MutableStateFlow<List<IptvCategory>>(emptyList())
    val iptvCategories: StateFlow<List<IptvCategory>> = _iptvCategories.asStateFlow()

    private val _iptvChannels = MutableStateFlow<List<IptvChannel>>(emptyList())
    val iptvChannels: StateFlow<List<IptvChannel>> = _iptvChannels.asStateFlow()

    private val _selectedCategory = MutableStateFlow<IptvCategory?>(null)
    val selectedCategory: StateFlow<IptvCategory?> = _selectedCategory.asStateFlow()

    private val _iptvSearchQuery = MutableStateFlow("")
    val iptvSearchQuery: StateFlow<String> = _iptvSearchQuery.asStateFlow()

    private val _isIptvLoading = MutableStateFlow(false)
    val isIptvLoading: StateFlow<Boolean> = _isIptvLoading.asStateFlow()

    private val _iptvErrorMessage = MutableStateFlow<String?>(null)
    val iptvErrorMessage: StateFlow<String?> = _iptvErrorMessage.asStateFlow()

    private val _epgProgrammes = MutableStateFlow<List<EpgProgramme>>(emptyList())
    val epgProgrammes: StateFlow<List<EpgProgramme>> = _epgProgrammes.asStateFlow()

    // Alternative Fallback URLs for easy switching during development:
    // Fallback Sample: "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
    // Fallback Sample 2 (Sintel): "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"

    private val TAG = "MainViewModel"

    init {
        // Collect active theme selection and reactive layout configurations
        viewModelScope.launch {
            preferenceManager.selectedThemeId.collect { id ->
                _activeThemeId.value = id
                _activeThemePreset.value = ThemePresets.getById(id)
            }
        }

        viewModelScope.launch {
            preferenceManager.customBackgroundUri.collect { uri ->
                _customBgUri.value = uri
            }
        }

        viewModelScope.launch {
            preferenceManager.selectedAspectRatioId.collect { id ->
                _activeAspectRatioId.value = id
            }
        }

        viewModelScope.launch {
            preferenceManager.selectedResizeMode.collect { mode ->
                _activeResizeMode.value = mode
            }
        }

        viewModelScope.launch {
            // Keep screen layout settings in sync with whichever theme is currently chosen
            _activeThemeId.collect { id ->
                _isSettingsLoaded.value = false
                preferenceManager.getLayoutSettings(id).collect { settings ->
                    if (_screenLayout.value != settings) {
                        _screenLayout.value = settings
                    }
                    _isSettingsLoaded.value = true
                }
            }
        }

        viewModelScope.launch {
            preferenceManager.isIptvModeActive.collect { active ->
                _isIptvModeActive.value = active
            }
        }

        viewModelScope.launch {
            preferenceManager.xtreamAccountsJson.collect { json ->
                _xtreamAccounts.value = deserializeAccounts(json)
                loadActiveIptvSource()
            }
        }

        viewModelScope.launch {
            preferenceManager.m3uPlaylistsJson.collect { json ->
                _m3uPlaylists.value = deserializeM3uConfigs(json)
                loadActiveIptvSource()
            }
        }

        viewModelScope.launch {
            preferenceManager.favoriteChannelIds.collect { favs ->
                _favoriteChannelIds.value = favs
                _iptvChannels.value = _iptvChannels.value.map {
                    it.copy(isFavorite = favs.contains(it.id))
                }
            }
        }

        viewModelScope.launch {
            preferenceManager.lastChannelId.collect { id ->
                _lastChannelId.value = id
            }
        }
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun selectAspectRatio(id: String) {
        viewModelScope.launch {
            Log.d(TAG, "Selecting aspect ratio: $id")
            _activeAspectRatioId.value = id
            preferenceManager.saveSelectedAspectRatio(id)
        }
    }

    fun selectResizeMode(modeId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Selecting resize mode: $modeId")
            _activeResizeMode.value = modeId
            preferenceManager.saveSelectedResizeMode(modeId)
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Selecting theme: $themeId")
            _isSettingsLoaded.value = false
            _activeThemeId.value = themeId
            _activeThemePreset.value = ThemePresets.getById(themeId)
            preferenceManager.saveSelectedTheme(themeId)
        }
    }

    private var saveSettingsJob: kotlinx.coroutines.Job? = null

    fun updateScreenLayout(left: Float, top: Float, width: Float, height: Float, dimAlpha: Float) {
        val currentPreset = _activeThemePreset.value
        val settings = ScreenLayoutSettings(
            left = left,
            top = top,
            width = width,
            height = height,
            dimAlpha = dimAlpha,
            subtitleOffset = currentPreset.defaultSubtitleOffset
        )
        // 1. Instantly update in-memory state for fluid drag adjustments
        _screenLayout.value = settings

        // 2. Debounce backing up to DataStore (reduces extreme write pressure)
        saveSettingsJob?.cancel()
        saveSettingsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(100) // 100ms is perfectly snappy and avoids UI lag
            preferenceManager.saveLayoutSettings(_activeThemeId.value, settings)
        }
    }

    fun resetActiveThemeLayout() {
        viewModelScope.launch {
            val id = _activeThemeId.value
            Log.d(TAG, "Resetting layout for theme $id")
            val preset = ThemePresets.getById(id)
            val defaultSettings = ScreenLayoutSettings(
                left = preset.defaultLeft,
                top = preset.defaultTop,
                width = preset.defaultWidth,
                height = preset.defaultHeight,
                dimAlpha = preset.defaultDimAlpha,
                subtitleOffset = preset.defaultSubtitleOffset
            )
            // Instantly apply in-memory
            _screenLayout.value = defaultSettings
            _isSettingsLoaded.value = true
            // Save in background
            preferenceManager.resetThemeToDefault(id)
        }
    }

    fun resetAllToAppDefaults() {
        viewModelScope.launch {
            Log.d(TAG, "Resetting all settings to app defaults")
            val preset = ThemePresets.Cinema
            val defaultSettings = ScreenLayoutSettings(
                left = preset.defaultLeft,
                top = preset.defaultTop,
                width = preset.defaultWidth,
                height = preset.defaultHeight,
                dimAlpha = preset.defaultDimAlpha,
                subtitleOffset = preset.defaultSubtitleOffset
            )
            // Instantly apply in-memory
            _screenLayout.value = defaultSettings
            _activeThemeId.value = "cinema"
            _activeThemePreset.value = preset
            _activeAspectRatioId.value = "free"
            _activeResizeMode.value = "adaptive"
            _isSettingsLoaded.value = true
            // Save in background
            preferenceManager.resetAllSettings()
        }
    }

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        Log.d(TAG, "Parsing intent: action=${intent.action}, data=${intent.data}")

        try {
            // Extract custom request headers if present in the intent extras
            _requestHeaders.value = IntentParser.extractHeaders(intent)

            val parsed = IntentParser.parse(intent)
            _parsedIntent.value = parsed

            val uriStr = parsed.resolvedPlayableUri
            if (!uriStr.isNullOrBlank()) {
                _playableUri.value = uriStr
                _errorMessage.value = null
                Log.d(TAG, "Parsed playable URI successfully: $uriStr")
            } else {
                // If we launcher via launcher (MAIN action) with no URI, we don't treat it as failure.
                if (intent.action == Intent.ACTION_MAIN) {
                    _playableUri.value = null
                    _errorMessage.value = null
                } else {
                    _playableUri.value = null
                    _errorMessage.value = "No playable URI received or unsupported intent payload"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse intent", e)
            _errorMessage.value = "Failed to parse stream: ${e.localizedMessage}"
        }
    }

    fun playTestVideo() {
        Log.d(TAG, "Playing test video URL: $defaultTestVideoUrl")
        _requestHeaders.value = emptyMap()
        _currentPlayingChannel.value = null
        _playableUri.value = defaultTestVideoUrl
        _errorMessage.value = null
    }

    fun setPlayableUri(uri: String?) {
        _playableUri.value = uri
        if (uri != null) {
            _errorMessage.value = null
        } else {
            _currentPlayingChannel.value = null
        }
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun toggleDebugPanel() {
        _showDebugPanel.value = !_showDebugPanel.value
    }
    
    fun setDebugPanelVisible(visible: Boolean) {
        _showDebugPanel.value = visible
    }

    fun setCustomBackground(uriString: String?) {
        viewModelScope.launch {
            _customBgUri.value = uriString
            preferenceManager.saveCustomBackgroundUri(uriString)
        }
    }

    fun saveCustomBackgroundFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val targetFile = java.io.File(context.filesDir, "custom_background.jpg")
                    val outputStream = java.io.FileOutputStream(targetFile)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    val path = targetFile.absolutePath
                    setCustomBackground(path)
                    Log.d(TAG, "Successfully copied custom background image to absolute path: $path")
                } else {
                    _errorMessage.value = "Unable to open selected image stream"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving custom background", e)
                _errorMessage.value = "Failed to copy background image: ${e.localizedMessage}"
            }
        }
    }

    // ==========================================
    // IPTV OPERATIONS AND UTILITIES
    // ==========================================

    fun setIptvModeActive(active: Boolean) {
        viewModelScope.launch {
            preferenceManager.saveIptvModeActive(active)
        }
    }

    fun setIptvSearchQuery(query: String) {
        _iptvSearchQuery.value = query
    }

    fun selectIptvCategory(category: IptvCategory?) {
        _selectedCategory.value = category
    }

    fun playIptvChannel(channel: IptvChannel) {
        _currentPlayingChannel.value = channel
        setPlayableUri(channel.streamUrl)
        viewModelScope.launch {
            preferenceManager.saveLastChannelId(channel.id)
        }
        // If Xtream account active, fetch its guide details asynchronously
        val activeXtream = _xtreamAccounts.value.find { it.isActive }
        if (activeXtream != null) {
            fetchXtreamEpgForChannel(channel.id)
        }
    }

    fun playNextIptvChannel(activeList: List<IptvChannel>) {
        if (activeList.isEmpty()) return
        val current = _currentPlayingChannel.value ?: activeList.firstOrNull() ?: return
        val currentIndex = activeList.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) {
            playIptvChannel(activeList.first())
        } else {
            val nextIndex = (currentIndex + 1) % activeList.size
            playIptvChannel(activeList[nextIndex])
        }
    }

    fun playPreviousIptvChannel(activeList: List<IptvChannel>) {
        if (activeList.isEmpty()) return
        val current = _currentPlayingChannel.value ?: activeList.firstOrNull() ?: return
        val currentIndex = activeList.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) {
            playIptvChannel(activeList.last())
        } else {
            val prevIndex = (currentIndex - 1 + activeList.size) % activeList.size
            playIptvChannel(activeList[prevIndex])
        }
    }

    private fun fetchXtreamEpgForChannel(streamId: String) {
        val activeXtream = _xtreamAccounts.value.find { it.isActive } ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val eps = IptvClient.fetchXtreamShortEpg(
                    activeXtream.serverUrl, activeXtream.username, activeXtream.password, streamId
                )
                // Merge into current EPG listings
                val merged = _epgProgrammes.value.toMutableList()
                merged.removeAll { it.channelId == streamId }
                merged.addAll(eps)
                _epgProgrammes.value = merged
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating short EPG for stream $streamId", e)
            }
        }
    }

    fun toggleFavoriteChannel(channelId: String) {
        viewModelScope.launch {
            val currentFavs = _favoriteChannelIds.value.toMutableSet()
            if (currentFavs.contains(channelId)) {
                currentFavs.remove(channelId)
            } else {
                currentFavs.add(channelId)
            }
            preferenceManager.saveFavoriteChannelIds(currentFavs)
        }
    }

    // --- Account Management ---

    fun addXtreamAccount(name: String, url: String, user: String, pass: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isIptvLoading.value = true
            _iptvErrorMessage.value = null
            
            val valid = IptvClient.testXtreamConnection(url, user, pass)
            if (!valid) {
                _iptvErrorMessage.value = "Unable to connect or invalid Xtream credentials"
                _isIptvLoading.value = false
                return@launch
            }

            // Deactivate others
            val currentList = _xtreamAccounts.value.map { it.copy(isActive = false) }.toMutableList()
            // Deactivate m3u configs too
            val currentM3u = _m3uPlaylists.value.map { it.copy(isActive = false) }
            preferenceManager.saveM3uPlaylistsJson(serializeM3uConfigs(currentM3u))

            val newAccount = XtreamAccount(name, url, user, pass, isActive = true)
            currentList.add(newAccount)
            
            preferenceManager.saveXtreamAccountsJson(serializeAccounts(currentList))
            _isIptvLoading.value = false
        }
    }

    fun deleteXtreamAccount(account: XtreamAccount) {
        viewModelScope.launch {
            val updated = _xtreamAccounts.value.filter { it.name != account.name || it.serverUrl != account.serverUrl }
            // If deleting active, check if another should become active, or clear loading state
            if (account.isActive && updated.isNotEmpty()) {
                val nextActive = updated.first().copy(isActive = true)
                val newUpdated = updated.map { if (it.name == nextActive.name && it.serverUrl == nextActive.serverUrl) nextActive else it }
                preferenceManager.saveXtreamAccountsJson(serializeAccounts(newUpdated))
            } else {
                preferenceManager.saveXtreamAccountsJson(if (updated.isEmpty()) null else serializeAccounts(updated))
            }
        }
    }

    fun selectXtreamAccount(account: XtreamAccount) {
        viewModelScope.launch {
            // Set this account as active, mark others inactive
            val updated = _xtreamAccounts.value.map {
                it.copy(isActive = (it.name == account.name && it.serverUrl == account.serverUrl))
            }
            // Deactivate M3Us
            val updatedM3u = _m3uPlaylists.value.map { it.copy(isActive = false) }
            
            preferenceManager.saveM3uPlaylistsJson(serializeM3uConfigs(updatedM3u))
            preferenceManager.saveXtreamAccountsJson(serializeAccounts(updated))
        }
    }

    // --- M3U / Playlist Management ---

    fun addM3uPlaylist(name: String, playlistUrl: String, epgUrl: String?) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isIptvLoading.value = true
            _iptvErrorMessage.value = null

            try {
                // Test stream load
                IptvClient.fetchUrlStream(playlistUrl).use { /* test stream opens */ }
                
                // Deactivate others
                val currentM3uList = _m3uPlaylists.value.map { it.copy(isActive = false) }.toMutableList()
                val currentXtream = _xtreamAccounts.value.map { it.copy(isActive = false) }
                preferenceManager.saveXtreamAccountsJson(serializeAccounts(currentXtream))

                val newConfig = M3UConfig(name, playlistUrl, epgUrl, isActive = true)
                currentM3uList.add(newConfig)

                preferenceManager.saveM3uPlaylistsJson(serializeM3uConfigs(currentM3uList))
            } catch (e: Exception) {
                _iptvErrorMessage.value = "M3U file check failed: ${e.localizedMessage}"
            } finally {
                _isIptvLoading.value = false
            }
        }
    }

    fun deleteM3uPlaylist(config: M3UConfig) {
        viewModelScope.launch {
            val updated = _m3uPlaylists.value.filter { it.name != config.name || it.playlistUrl != config.playlistUrl }
            if (config.isActive && updated.isNotEmpty()) {
                val nextActive = updated.first().copy(isActive = true)
                val newUpdated = updated.map { if (it.name == nextActive.name && it.playlistUrl == nextActive.playlistUrl) nextActive else it }
                preferenceManager.saveM3uPlaylistsJson(serializeM3uConfigs(newUpdated))
            } else {
                preferenceManager.saveM3uPlaylistsJson(if (updated.isEmpty()) null else serializeM3uConfigs(updated))
            }
        }
    }

    fun selectM3uPlaylist(config: M3UConfig) {
        viewModelScope.launch {
            val updated = _m3uPlaylists.value.map {
                it.copy(isActive = (it.name == config.name && it.playlistUrl == config.playlistUrl))
            }
            val updatedXtream = _xtreamAccounts.value.map { it.copy(isActive = false) }
            
            preferenceManager.saveXtreamAccountsJson(serializeAccounts(updatedXtream))
            preferenceManager.saveM3uPlaylistsJson(serializeM3uConfigs(updated))
        }
    }

    // --- Loading channels engine ---

    fun loadActiveIptvSource() {
        val activeXtream = _xtreamAccounts.value.find { it.isActive }
        val activeM3u = _m3uPlaylists.value.find { it.isActive }

        if (activeXtream == null && activeM3u == null) {
            _iptvCategories.value = emptyList()
            _iptvChannels.value = emptyList()
            _selectedCategory.value = null
            _epgProgrammes.value = emptyList()
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isIptvLoading.value = true
            _iptvErrorMessage.value = null

            try {
                if (activeXtream != null) {
                    val cats = IptvClient.fetchXtreamCategories(
                        activeXtream.serverUrl, activeXtream.username, activeXtream.password
                    )
                    val rawChannels = IptvClient.fetchXtreamChannels(
                        activeXtream.serverUrl, activeXtream.username, activeXtream.password
                    )
                    val favorites = _favoriteChannelIds.value
                    val channels = rawChannels.map { it.copy(isFavorite = favorites.contains(it.id)) }

                    _iptvCategories.value = cats
                    _iptvChannels.value = channels
                    
                    val currentCategory = _selectedCategory.value
                    if (currentCategory == null || !cats.any { it.id == currentCategory.id }) {
                        _selectedCategory.value = cats.firstOrNull()
                    }
                } else if (activeM3u != null) {
                    IptvClient.fetchUrlStream(activeM3u.playlistUrl).use { stream ->
                        val (cats, rawChannels) = IptvParser.parseM3u(stream)
                        val favorites = _favoriteChannelIds.value
                        val channels = rawChannels.map { it.copy(isFavorite = favorites.contains(it.id)) }

                        _iptvCategories.value = cats
                        _iptvChannels.value = channels
                        
                        val currentCategory = _selectedCategory.value
                        if (currentCategory == null || !cats.any { it.id == currentCategory.id }) {
                            _selectedCategory.value = cats.firstOrNull()
                        }

                        // Load XMLTV EPG
                        val xmltvUrl = activeM3u.epgUrl
                        if (!xmltvUrl.isNullOrBlank()) {
                            try {
                                IptvClient.fetchUrlStream(xmltvUrl).use { xmlStream ->
                                    val eps = IptvParser.parseXmltv(xmlStream)
                                    _epgProgrammes.value = eps
                                }
                            } catch (epgEx: Exception) {
                                Log.e(TAG, "XMLTV background EPG load failure", epgEx)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "IPTV channel initialization stream error", e)
                _iptvErrorMessage.value = "Stream source error: ${e.localizedMessage}"
            } finally {
                _isIptvLoading.value = false
            }
        }
    }

    // --- JSON Converters ---

    private fun serializeAccounts(accounts: List<XtreamAccount>): String {
        val arr = org.json.JSONArray()
        for (acc in accounts) {
            val obj = org.json.JSONObject()
            obj.put("name", acc.name)
            obj.put("serverUrl", acc.serverUrl)
            obj.put("username", acc.username)
            obj.put("password", acc.password)
            obj.put("isActive", acc.isActive)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeAccounts(json: String?): List<XtreamAccount> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<XtreamAccount>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    XtreamAccount(
                        name = obj.getString("name"),
                        serverUrl = obj.getString("serverUrl"),
                        username = obj.getString("username"),
                        password = obj.getString("password"),
                        isActive = obj.optBoolean("isActive", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Converter accounts error", e)
        }
        return list
    }

    private fun serializeM3uConfigs(configs: List<M3UConfig>): String {
        val arr = org.json.JSONArray()
        for (cfg in configs) {
            val obj = org.json.JSONObject()
            obj.put("name", cfg.name)
            obj.put("playlistUrl", cfg.playlistUrl)
            obj.put("epgUrl", cfg.epgUrl ?: "")
            obj.put("isActive", cfg.isActive)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeM3uConfigs(json: String?): List<M3UConfig> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<M3UConfig>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    M3UConfig(
                        name = obj.getString("name"),
                        playlistUrl = obj.getString("playlistUrl"),
                        epgUrl = obj.optString("epgUrl").takeIf { it.isNotBlank() },
                        isActive = obj.optBoolean("isActive", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Converter playlists error", e)
        }
        return list
    }
}
