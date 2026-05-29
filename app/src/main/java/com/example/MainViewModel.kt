package com.example

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferenceManager = PlayerPreferenceManager(application)

    // ==========================================
    // PREMIUM FEATURES STATES
    // ==========================================
    private val premiumStore = PremiumFeaturesStore(application)

    private val _presets = MutableStateFlow<List<LayoutPreset>>(emptyList())
    val presets: StateFlow<List<LayoutPreset>> = _presets.asStateFlow()

    private val _ambientGlow = MutableStateFlow(AmbientGlowSetting.SUBTLE)
    val ambientGlow: StateFlow<AmbientGlowSetting> = _ambientGlow.asStateFlow()

    private val _reminders = MutableStateFlow<List<IptvReminder>>(emptyList())
    val reminders: StateFlow<List<IptvReminder>> = _reminders.asStateFlow()

    private val _activeReminderAlert = MutableStateFlow<IptvReminder?>(null)
    val activeReminderAlert: StateFlow<IptvReminder?> = _activeReminderAlert.asStateFlow()

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

    private val _isTosAccepted = MutableStateFlow(false)
    val isTosAccepted: StateFlow<Boolean> = _isTosAccepted.asStateFlow()

    private val _tosAcceptedTimestamp = MutableStateFlow(0L)
    val tosAcceptedTimestamp: StateFlow<Long> = _tosAcceptedTimestamp.asStateFlow()

    fun acceptTos() {
        viewModelScope.launch {
            preferenceManager.saveTosAccepted(true)
        }
    }

    private val _isPermissionsPrompted = MutableStateFlow(false)
    val isPermissionsPrompted: StateFlow<Boolean> = _isPermissionsPrompted.asStateFlow()

    fun dismissPermissionsPrompt() {
        viewModelScope.launch {
            preferenceManager.savePermissionsPrompted(true)
        }
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
    
    private val _customCategoryOrder = MutableStateFlow<List<String>>(emptyList())
    val customCategoryOrder: StateFlow<List<String>> = _customCategoryOrder.asStateFlow()

    private val _hiddenCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenCategoryIds: StateFlow<Set<String>> = _hiddenCategoryIds.asStateFlow()

    private val _categorySortMode = MutableStateFlow(CategorySortMode.PROVIDER)
    val categorySortMode: StateFlow<CategorySortMode> = _categorySortMode.asStateFlow()

    private val _showEpgSorter = MutableStateFlow(false)
    val showEpgSorter: StateFlow<Boolean> = _showEpgSorter.asStateFlow()

    private val _channelSortMode = MutableStateFlow(ChannelSortMode.PROVIDER)
    val channelSortMode: StateFlow<ChannelSortMode> = _channelSortMode.asStateFlow()

    private val _customChannelOrder = MutableStateFlow<List<String>>(emptyList())
    val customChannelOrder: StateFlow<List<String>> = _customChannelOrder.asStateFlow()

    val allRawIptvCategories: StateFlow<List<IptvCategory>> = _iptvCategories.asStateFlow()

    val iptvCategories: StateFlow<List<IptvCategory>> = combine(
        _iptvCategories,
        _customCategoryOrder,
        _hiddenCategoryIds,
        _categorySortMode
    ) { rawCats, order, hiddenIds, sortMode ->
        val visibleCats = rawCats.filter { !hiddenIds.contains(it.id) }
        when (sortMode) {
            CategorySortMode.PROVIDER -> {
                visibleCats
            }
            CategorySortMode.CUSTOM -> {
                val orderMap = order.withIndex().associate { it.value to it.index }
                visibleCats.sortedWith(
                    compareBy<IptvCategory> { orderMap[it.id] ?: Int.MAX_VALUE }
                        .thenBy { rawCats.indexOf(it) }
                )
            }
            CategorySortMode.NAME_AZ -> {
                visibleCats.sortedBy { it.name.lowercase() }
            }
            CategorySortMode.FAVORITES_FIRST -> {
                visibleCats.sortedWith(
                    compareByDescending<IptvCategory> { it.name.contains("Favorites", ignoreCase = true) || it.id.contains("fav", ignoreCase = true) }
                        .thenBy { it.name.lowercase() }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _isEpgLoading = MutableStateFlow(false)
    val isEpgLoading: StateFlow<Boolean> = _isEpgLoading.asStateFlow()

    private val _epgLoadStatus = MutableStateFlow<String?>(null)
    val epgLoadStatus: StateFlow<String?> = _epgLoadStatus.asStateFlow()

    private val _isAnimationEnabled = MutableStateFlow(true)
    val isAnimationEnabled: StateFlow<Boolean> = _isAnimationEnabled.asStateFlow()

    // IPTV History state
    private val _iptvHistory = MutableStateFlow<List<IptvHistoryItem>>(emptyList())
    val iptvHistory: StateFlow<List<IptvHistoryItem>> = _iptvHistory.asStateFlow()

    // Picture-in-Picture state
    private val _isInPictureInPicture = MutableStateFlow(false)
    val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture.asStateFlow()

    fun setInPictureInPicture(inPiP: Boolean) {
        _isInPictureInPicture.value = inPiP
        if (inPiP) {
            _showDebugPanel.value = false // Hide nonessential UI while in PiP
        }
    }

    // Alternative Fallback URLs for easy switching during development:
    // Fallback Sample: "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
    // Fallback Sample 2 (Sintel): "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"

    private val TAG = "MainViewModel"

    init {
        updateLastRefreshTime()
        // Load custom themes from storage at VM start
        try {
            val loadedCustomThemes = CustomThemePersistence.loadThemes(application)
            ThemePresets.setCustomThemes(loadedCustomThemes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading custom themes in init block", e)
        }

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
                if (active) {
                    // Defer cold start loading slightly to allow core UI setup to render smoothly first
                    kotlinx.coroutines.delay(400)
                    loadActiveIptvSource()
                }
            }
        }

        viewModelScope.launch {
            preferenceManager.isAnimationEnabled.collect { enabled ->
                _isAnimationEnabled.value = enabled
            }
        }

        viewModelScope.launch {
            preferenceManager.iptvCategoryOrder.collect { order ->
                _customCategoryOrder.value = order
            }
        }

        viewModelScope.launch {
            preferenceManager.iptvCategorySortMode.collect { modeStr ->
                _categorySortMode.value = try {
                    CategorySortMode.valueOf(modeStr)
                } catch (e: Exception) {
                    CategorySortMode.PROVIDER
                }
            }
        }

        viewModelScope.launch {
            preferenceManager.iptvChannelSortMode.collect { modeStr ->
                _channelSortMode.value = try {
                    ChannelSortMode.valueOf(modeStr)
                } catch (e: Exception) {
                    ChannelSortMode.PROVIDER
                }
            }
        }

        viewModelScope.launch {
            preferenceManager.iptvChannelOrder.collect { order ->
                _customChannelOrder.value = order
            }
        }

        viewModelScope.launch {
            preferenceManager.iptvHiddenCategoryIds.collect { hidden ->
                _hiddenCategoryIds.value = hidden
            }
        }

        viewModelScope.launch {
            preferenceManager.iptvHistoryJson.collect { json ->
                _iptvHistory.value = deserializeIptvHistory(json)
            }
        }

        viewModelScope.launch {
            preferenceManager.xtreamAccountsJson.collect { json ->
                _xtreamAccounts.value = deserializeAccounts(json)
                if (_isIptvModeActive.value) {
                    loadActiveIptvSource()
                }
            }
        }

        viewModelScope.launch {
            preferenceManager.m3uPlaylistsJson.collect { json ->
                _m3uPlaylists.value = deserializeM3uConfigs(json)
                if (_isIptvModeActive.value) {
                    loadActiveIptvSource()
                }
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

        viewModelScope.launch {
            preferenceManager.isTosAccepted.collect { accepted ->
                _isTosAccepted.value = accepted
            }
        }

        viewModelScope.launch {
            preferenceManager.tosAcceptedTimestamp.collect { ts ->
                _tosAcceptedTimestamp.value = ts
            }
        }

        viewModelScope.launch {
            preferenceManager.isPermissionsPrompted.collect { prompted ->
                _isPermissionsPrompted.value = prompted
            }
        }

        // ==========================================
        // INITIALIZE PREMIUM FEATURES
        // ==========================================
        _presets.value = LayoutPreset.BuiltInPresets + premiumStore.loadCustomPresets()
        _ambientGlow.value = premiumStore.getAmbientGlow()
        _reminders.value = premiumStore.loadReminders()

        startRemindersPolling()

        viewModelScope.launch {
            val lastPresetId = premiumStore.getLastPresetId()
            if (lastPresetId != null) {
                val found = _presets.value.find { it.id == lastPresetId }
                if (found != null) {
                    kotlinx.coroutines.delay(800)
                    applyLayoutPreset(found)
                }
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

    fun setAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isAnimationEnabled.value = enabled
            preferenceManager.saveAnimationEnabled(enabled)
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

    fun createAndSaveCustomTheme(
        name: String,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        isAnimated: Boolean,
        backdropImageUri: String?
    ) {
        viewModelScope.launch {
            val id = "custom_" + System.currentTimeMillis()
            val newPreset = ThemePreset(
                id = id,
                name = name,
                description = "User custom designed theme '$name'.",
                defaultLeft = ThemePresets.Custom.defaultLeft,
                defaultTop = ThemePresets.Custom.defaultTop,
                defaultWidth = ThemePresets.Custom.defaultWidth,
                defaultHeight = ThemePresets.Custom.defaultHeight,
                defaultDimAlpha = ThemePresets.Custom.defaultDimAlpha,
                defaultSubtitleOffset = ThemePresets.Custom.defaultSubtitleOffset,
                primaryColor = Color(primaryColorHex),
                secondaryColor = Color(secondaryColorHex),
                cornerRadiusDp = 12,
                frameThicknessDp = 5,
                frameColor = Color(0xFF1E1B20),
                glowColor = Color(primaryColorHex).copy(alpha = 0.25f),
                glowRadiusDp = 12,
                shadowIntensity = 0.45f,
                vignetteStrength = 0.6f,
                ambientColorTint = Color(primaryColorHex).copy(alpha = 0.08f),
                isAnimated = isAnimated,
                animationType = if (isAnimated) "aurora" else null,
                backdropImageUri = backdropImageUri
            )

            val currentCustoms = CustomThemePersistence.loadThemes(getApplication()).toMutableList()
            currentCustoms.add(newPreset)
            CustomThemePersistence.saveThemes(getApplication(), currentCustoms)
            ThemePresets.setCustomThemes(currentCustoms)
            
            // Auto select the newly created theme!
            selectTheme(id)
        }
    }

    fun deleteCustomTheme(id: String) {
        viewModelScope.launch {
            val currentCustoms = CustomThemePersistence.loadThemes(getApplication()).filter { it.id != id }
            CustomThemePersistence.saveThemes(getApplication(), currentCustoms)
            ThemePresets.setCustomThemes(currentCustoms)
            if (_activeThemeId.value == id) {
                selectTheme("cinema")
            }
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
            if (active) {
                val lastId = _lastChannelId.value
                if (lastId != null && _currentPlayingChannel.value == null) {
                    val match = _iptvChannels.value.find { it.id == lastId }
                    if (match != null) {
                        playIptvChannel(match)
                    }
                }
            }
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
        
        addToIptvHistory(channel)

        viewModelScope.launch {
            preferenceManager.saveLastChannelId(channel.id)
        }
        // If Xtream account active, fetch its guide details asynchronously
        val activeXtream = _xtreamAccounts.value.find { it.isActive }
        if (activeXtream != null) {
            fetchXtreamEpgForChannel(channel.id)
        }
    }

    fun recallPreviousIptvChannel() {
        val list = _iptvHistory.value
        // If current is playing, previous is index 1. If not, index 0.
        val currentPlaying = _currentPlayingChannel.value
        val index = if (currentPlaying != null && list.isNotEmpty() && list[0].id == currentPlaying.id) 1 else 0
        if (index < list.size) {
            val favs = _favoriteChannelIds.value
            val item = list[index]
            playIptvChannel(item.toIptvChannel(favs.contains(item.id)))
        }
    }

    // ==========================================
    // PREMIUM FEATURES IMPLEMENTATIONS
    // ==========================================

    fun setAmbientGlow(setting: AmbientGlowSetting) {
        _ambientGlow.value = setting
        premiumStore.saveAmbientGlow(setting)
    }

    fun applyLayoutPreset(preset: LayoutPreset) {
        viewModelScope.launch {
            Log.d(TAG, "Applying layout preset: ${preset.name}")
            val settings = ScreenLayoutSettings(
                left = preset.left,
                top = preset.top,
                width = preset.width,
                height = preset.height,
                dimAlpha = preset.dimAlpha,
                subtitleOffset = 0.5f
            )
            _screenLayout.value = settings
            
            // Sync with Theme and datastore
            preferenceManager.saveLayoutSettings(preset.themeId, settings)
            preferenceManager.saveSelectedTheme(preset.themeId)
            preferenceManager.saveSelectedResizeMode(preset.displayMode)
            
            // Store last as tracking
            premiumStore.saveLastPresetId(preset.id)
        }
    }

    fun saveCurrentLayoutAsPreset(name: String) {
        viewModelScope.launch {
            val currentLayout = _screenLayout.value
            val currentTheme = _activeThemeId.value
            val currentMode = _activeResizeMode.value
            
            val newPreset = LayoutPreset(
                id = "custom_" + System.currentTimeMillis(),
                name = name,
                left = currentLayout.left,
                top = currentLayout.top,
                width = currentLayout.width,
                height = currentLayout.height,
                dimAlpha = currentLayout.dimAlpha,
                displayMode = currentMode,
                themeId = currentTheme,
                isBuiltIn = false
            )
            
            val customOnly = premiumStore.loadCustomPresets().toMutableList()
            customOnly.removeIf { it.name.lowercase() == name.lowercase() }
            customOnly.add(newPreset)
            premiumStore.saveCustomPresets(customOnly)
            
            _presets.value = LayoutPreset.BuiltInPresets + customOnly
            premiumStore.saveLastPresetId(newPreset.id)
            Log.d(TAG, "Saved custom layout preset: $name")
        }
    }

    fun renamePreset(id: String, newName: String) {
        viewModelScope.launch {
            val customOnly = premiumStore.loadCustomPresets().map {
                if (it.id == id) it.copy(name = newName) else it
            }
            premiumStore.saveCustomPresets(customOnly)
            _presets.value = LayoutPreset.BuiltInPresets + customOnly
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch {
            val customOnly = premiumStore.loadCustomPresets().filter { it.id != id }
            premiumStore.saveCustomPresets(customOnly)
            _presets.value = LayoutPreset.BuiltInPresets + customOnly
            
            if (premiumStore.getLastPresetId() == id) {
                premiumStore.saveLastPresetId(null)
            }
        }
    }

    fun addReminder(channel: IptvChannel, prog: EpgProgramme, leadTimeMin: Int) {
        viewModelScope.launch {
            val newReminder = IptvReminder(
                channelId = channel.id,
                channelName = channel.name,
                channelStreamUrl = channel.streamUrl,
                programTitle = prog.title,
                programStartMs = prog.startMs,
                programEndMs = prog.endMs,
                leadTimeMinutes = leadTimeMin
            )
            val currentList = _reminders.value.toMutableList()
            currentList.removeAll { it.channelId == channel.id && it.programStartMs == prog.startMs }
            currentList.add(newReminder)
            
            premiumStore.saveReminders(currentList)
            _reminders.value = currentList
            Log.d(TAG, "Scheduled reminder: ${prog.title}")
        }
    }

    fun removeReminder(id: String) {
        viewModelScope.launch {
            val currentList = _reminders.value.filter { it.id != id }
            premiumStore.saveReminders(currentList)
            _reminders.value = currentList
        }
    }

    fun dismissReminderAlert() {
        _activeReminderAlert.value = null
    }

    fun tuneToReminderChannel(reminder: IptvReminder) {
        viewModelScope.launch {
            val channel = _iptvChannels.value.find { it.id == reminder.channelId }
            if (channel != null) {
                playIptvChannel(channel)
            } else {
                val lightChannel = IptvChannel(
                    id = reminder.channelId,
                    name = reminder.channelName,
                    streamUrl = reminder.channelStreamUrl
                )
                playIptvChannel(lightChannel)
            }
            _activeReminderAlert.value = null
        }
    }

    private fun startRemindersPolling() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000) // check every 10 seconds
                val now = System.currentTimeMillis()
                val activeList = _reminders.value
                val triggered = activeList.find { now >= it.triggerTimeMs && now < it.programEndMs }
                if (triggered != null) {
                    _activeReminderAlert.value = triggered
                    val nonTriggered = activeList.filter { it.id != triggered.id }
                    premiumStore.saveReminders(nonTriggered)
                    _reminders.value = nonTriggered
                } else {
                    val nonExpired = activeList.filter { now < it.programEndMs }
                    if (nonExpired.size != activeList.size) {
                        premiumStore.saveReminders(nonExpired)
                        _reminders.value = nonExpired
                    }
                }
            }
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

            val trimmedName = name.trim()
            val trimmedUrl = playlistUrl.trim()

            if (trimmedName.isBlank()) {
                _iptvErrorMessage.value = "Configuration error: Playlist name cannot be empty."
                _isIptvLoading.value = false
                return@launch
            }
            if (trimmedUrl.isBlank()) {
                _iptvErrorMessage.value = "Configuration error: Playlist URL cannot be empty."
                _isIptvLoading.value = false
                return@launch
            }

            if (!trimmedUrl.startsWith("http://", ignoreCase = true) && 
                !trimmedUrl.startsWith("https://", ignoreCase = true)) {
                _iptvErrorMessage.value = "Validation error: Invalid URL scheme. M3U URL must start with http:// or https://"
                _isIptvLoading.value = false
                return@launch
            }

            try {
                // Test playlist network stream connectivity and timeout bounds
                IptvClient.fetchUrlStream(trimmedUrl).use { /* verify stream can be reached safely */ }
                
                // Deactivate others
                val currentM3uList = _m3uPlaylists.value.map { it.copy(isActive = false) }.toMutableList()
                val currentXtream = _xtreamAccounts.value.map { it.copy(isActive = false) }
                preferenceManager.saveXtreamAccountsJson(serializeAccounts(currentXtream))

                val newConfig = M3UConfig(trimmedName, trimmedUrl, epgUrl?.trim()?.takeIf { it.isNotBlank() }, isActive = true)
                currentM3uList.add(newConfig)

                preferenceManager.saveM3uPlaylistsJson(serializeM3uConfigs(currentM3uList))
                
                // Triggers immediate loading of new source
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    loadActiveIptvSource()
                }
            } catch (e: Exception) {
                Log.e(TAG, "M3U connection or parse initialization test failed", e)
                _iptvErrorMessage.value = "Connection check failed: Unable to fetch source. Please verify URL is active."
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

    private var activeLoadJob: kotlinx.coroutines.Job? = null

    private fun getActiveSourceId(): String? {
        val activeXtream = _xtreamAccounts.value.find { it.isActive }
        if (activeXtream != null) {
            return "xtream_" + java.util.UUID.nameUUIDFromBytes("${activeXtream.serverUrl}_${activeXtream.username}".toByteArray()).toString()
        }
        val activeM3u = _m3uPlaylists.value.find { it.isActive }
        if (activeM3u != null) {
            return "m3u_" + java.util.UUID.nameUUIDFromBytes(activeM3u.playlistUrl.toByteArray()).toString()
        }
        return null
    }

    private fun saveChannelsToLocalCache(sourceId: String, categories: List<IptvCategory>, channels: List<IptvChannel>) {
        try {
            val file = java.io.File(getApplication<Application>().cacheDir, "iptv_cache_$sourceId.json")
            val root = org.json.JSONObject()
            
            val catsArr = org.json.JSONArray()
            for (c in categories) {
                val obj = org.json.JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("type", c.type)
                catsArr.put(obj)
            }
            root.put("categories", catsArr)
            
            val chanArr = org.json.JSONArray()
            for (ch in channels) {
                val obj = org.json.JSONObject()
                obj.put("id", ch.id)
                obj.put("name", ch.name)
                obj.put("logoUrl", ch.logoUrl ?: "")
                obj.put("streamUrl", ch.streamUrl)
                obj.put("categoryId", ch.categoryId)
                obj.put("epgId", ch.epgId ?: "")
                chanArr.put(obj)
            }
            root.put("channels", chanArr)
            
            file.writeText(root.toString())
            Log.d(TAG, "Cached IPTV source $sourceId locally: ${categories.size} categories and ${channels.size} channels.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing IPTV channels to cache", e)
        }
    }

    private fun loadChannelsFromLocalCache(sourceId: String): Pair<List<IptvCategory>, List<IptvChannel>>? {
        try {
            val file = java.io.File(getApplication<Application>().cacheDir, "iptv_cache_$sourceId.json")
            if (!file.exists()) return null
            
            val json = file.readText()
            val root = org.json.JSONObject(json)
            
            val catsArr = root.getJSONArray("categories")
            val categories = mutableListOf<IptvCategory>()
            for (i in 0 until catsArr.length()) {
                val obj = catsArr.getJSONObject(i)
                categories.add(
                    IptvCategory(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        type = obj.optString("type", "live")
                    )
                )
            }
            
            val chanArr = root.getJSONArray("channels")
            val channels = mutableListOf<IptvChannel>()
            val favorites = _favoriteChannelIds.value
            for (i in 0 until chanArr.length()) {
                val obj = chanArr.getJSONObject(i)
                val id = obj.getString("id")
                channels.add(
                    IptvChannel(
                        id = id,
                        name = obj.getString("name"),
                        logoUrl = obj.optString("logoUrl").takeIf { it.isNotBlank() },
                        streamUrl = obj.getString("streamUrl"),
                        categoryId = obj.optString("categoryId", ""),
                        epgId = obj.optString("epgId").takeIf { it.isNotBlank() },
                        isFavorite = favorites.contains(id)
                    )
                )
            }
            return Pair(categories, channels)
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading IPTV channels from cache", e)
            return null
        }
    }

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

        // Validate account details before spinning up dispatchers
        if (activeXtream != null) {
            if (activeXtream.serverUrl.isBlank() || activeXtream.username.isBlank()) {
                _iptvErrorMessage.value = "Configuration error: Active Xtream Server URL or Username is blank."
                return
            }
        }
        if (activeM3u != null) {
            if (activeM3u.playlistUrl.isBlank()) {
                _iptvErrorMessage.value = "Configuration error: Active M3U playlist URL is blank."
                return
            }
        }

        activeLoadJob?.cancel()
        activeLoadJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isIptvLoading.value = true
            _iptvErrorMessage.value = null

            // 1. Try rendering instantaneous cached EPG first!
            try {
                val cached = loadEpgFromLocalCache()
                if (cached.isNotEmpty()) {
                    _epgProgrammes.value = cached
                }
            } catch (e: Exception) {
                Log.e(TAG, "Local EPG cache initial load failed", e)
            }

            // 2. Try to load local source cache (channels & categories) for instant UI
            val sourceId = getActiveSourceId()
            var hasLoadedFromCache = false
            if (sourceId != null) {
                try {
                    val cachedSource = loadChannelsFromLocalCache(sourceId)
                    if (cachedSource != null && cachedSource.second.isNotEmpty()) {
                        val (cats, channels) = cachedSource
                        _iptvCategories.value = cats
                        _iptvChannels.value = channels
                        
                        val currentCategory = _selectedCategory.value
                        if (currentCategory == null || !cats.any { it.id == currentCategory.id }) {
                            _selectedCategory.value = cats.firstOrNull()
                        }
                        hasLoadedFromCache = true
                        
                        // Auto-resume channel if returning/loading
                        val lastId = _lastChannelId.value
                        if (lastId != null && _currentPlayingChannel.value == null && _isIptvModeActive.value) {
                            val match = channels.find { it.id == lastId }
                            if (match != null) {
                                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    playIptvChannel(match)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load channel cache on startup", e)
                }
            }

            // 3. Progressive fetch from network with robust, crash-proof exception handling
            try {
                if (activeXtream != null) {
                    val rawCats = IptvClient.fetchXtreamCategories(
                        activeXtream.serverUrl, activeXtream.username, activeXtream.password
                    )
                    val rawChannels = IptvClient.fetchXtreamChannels(
                        activeXtream.serverUrl, activeXtream.username, activeXtream.password
                    )
                    val favorites = _favoriteChannelIds.value
                    
                    var verifiedCats = rawCats
                    var channels = rawChannels.map { it.copy(isFavorite = favorites.contains(it.id)) }

                    if (channels.isEmpty() && !hasLoadedFromCache) {
                        _iptvErrorMessage.value = "This Xtream source currently has no online channels. Please check credentials or URL."
                    } else if (channels.isNotEmpty()) {
                        if (verifiedCats.isEmpty()) {
                            // Create a fallback category so channels aren't hidden
                            val fallbackCat = IptvCategory(id = "xtream_default", name = "Default Category", type = "live")
                            verifiedCats = listOf(fallbackCat)
                            channels = channels.map { it.copy(categoryId = "xtream_default") }
                        }

                        _iptvCategories.value = verifiedCats
                        _iptvChannels.value = channels
                        
                        if (sourceId != null) {
                            saveChannelsToLocalCache(sourceId, verifiedCats, channels)
                        }

                        val currentCategory = _selectedCategory.value
                        if (currentCategory == null || !verifiedCats.any { it.id == currentCategory.id }) {
                            _selectedCategory.value = verifiedCats.firstOrNull()
                        }

                        // Auto-resume channel if returning/loading
                        val lastId = _lastChannelId.value
                        if (lastId != null && _currentPlayingChannel.value == null && _isIptvModeActive.value) {
                            val match = channels.find { it.id == lastId }
                            if (match != null) {
                                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    playIptvChannel(match)
                                }
                            }
                        }
                    }
                } else if (activeM3u != null) {
                    val urlToFetch = activeM3u.playlistUrl.trim()
                    if (!urlToFetch.startsWith("http://", ignoreCase = true) && 
                        !urlToFetch.startsWith("https://", ignoreCase = true)) {
                        throw Exception("Unsupported URI scheme. URLs must start with http:// or https://")
                    }

                    IptvClient.fetchUrlStream(urlToFetch).use { stream ->
                        val (cats, rawChannels) = IptvParser.parseM3u(stream)
                        val favorites = _favoriteChannelIds.value
                        var channels = rawChannels.map { it.copy(isFavorite = favorites.contains(it.id)) }
                        var verifiedCats = cats

                        if (channels.isEmpty() && !hasLoadedFromCache) {
                            _iptvErrorMessage.value = "No valid stream channels found in this playlist. Ensure lines have proper stream links."
                        } else if (channels.isNotEmpty()) {
                            if (verifiedCats.isEmpty()) {
                                // Create a fallback category
                                val fallbackCat = IptvCategory(id = "m3u_default", name = "All Channels", type = "live")
                                verifiedCats = listOf(fallbackCat)
                                channels = channels.map { it.copy(categoryId = "m3u_default") }
                            }

                            _iptvCategories.value = verifiedCats
                            _iptvChannels.value = channels
                            
                            if (sourceId != null) {
                                saveChannelsToLocalCache(sourceId, verifiedCats, channels)
                            }

                            val currentCategory = _selectedCategory.value
                            if (currentCategory == null || !verifiedCats.any { it.id == currentCategory.id }) {
                                _selectedCategory.value = verifiedCats.firstOrNull()
                            }

                            // Auto-resume channel if returning/loading
                            val lastId = _lastChannelId.value
                            if (lastId != null && _currentPlayingChannel.value == null && _isIptvModeActive.value) {
                                val match = channels.find { it.id == lastId }
                                if (match != null) {
                                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                        playIptvChannel(match)
                                    }
                                }
                            }
                        }

                        // Load XMLTV EPG
                        val xmltvUrl = activeM3u.epgUrl
                        if (!xmltvUrl.isNullOrBlank() && 
                            (xmltvUrl.startsWith("http://", ignoreCase = true) || xmltvUrl.startsWith("https://", ignoreCase = true))) {
                            try {
                                IptvClient.fetchUrlStream(xmltvUrl).use { xmlStream ->
                                    val eps = IptvParser.parseXmltv(xmlStream)
                                    _epgProgrammes.value = eps
                                    saveEpgToLocalCache(eps)
                                }
                            } catch (epgEx: Exception) {
                                Log.e(TAG, "XMLTV background EPG load failure", epgEx)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "IPTV source loading job cancelled intentionally.")
                    throw e
                }
                Log.e(TAG, "IPTV channel initialization stream/network error", e)
                if (!hasLoadedFromCache) {
                    _iptvErrorMessage.value = "Import Failed: Unable to read playlist content (${e.localizedMessage})."
                }
            } finally {
                _isIptvLoading.value = false
            }
        }
    }

    private val _lastEpgRefreshTime = MutableStateFlow<Long>(0L)
    val lastEpgRefreshTime: StateFlow<Long> = _lastEpgRefreshTime.asStateFlow()

    fun updateLastRefreshTime() {
        _lastEpgRefreshTime.value = EpgRefreshManager.getLastRefreshTimeGlobal(getApplication())
    }

    fun onAppReturnedToForeground() {
        viewModelScope.launch {
            Log.d(TAG, "onAppReturnedToForeground triggered")
            // Immediate warm restore from local cache (handles other sources background refreshed)
            val sourceId = getActiveSourceId()
            if (sourceId != null) {
                val cached = loadEpgFromLocalCache(sourceId)
                if (cached.isNotEmpty()) {
                    _epgProgrammes.value = cached
                }
            }
            updateLastRefreshTime()

            // Trigger actual network refresh (this obeys internal 15-minute throttle)
            val success = EpgRefreshManager.refreshAllEpgSources(getApplication(), force = false)
            if (success) {
                val activeId = getActiveSourceId()
                if (activeId != null) {
                    val updatedCached = loadEpgFromLocalCache(activeId)
                    _epgProgrammes.value = updatedCached
                }
                updateLastRefreshTime()
            }
        }
    }

    private fun saveEpgToLocalCache(programmes: List<EpgProgramme>, sourceId: String? = null) {
        try {
            val id = sourceId ?: getActiveSourceId() ?: "default"
            val file = java.io.File(getApplication<Application>().cacheDir, "epg_cache_$id.json")
            val arr = org.json.JSONArray()
            for (p in programmes) {
                val obj = org.json.JSONObject()
                obj.put("channelId", p.channelId)
                obj.put("title", p.title)
                obj.put("startMs", p.startMs)
                obj.put("endMs", p.endMs)
                obj.put("description", p.description ?: "")
                arr.put(obj)
            }
            file.writeText(arr.toString())
            Log.d(TAG, "Saved EPG local cache for $id: ${programmes.size} records saved.")

            // Legacy fallback syncing
            val legacyFile = java.io.File(getApplication<Application>().cacheDir, "epg_cache.json")
            legacyFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing EPG to local storage cache", e)
        }
    }

    private fun loadEpgFromLocalCache(sourceId: String? = null): List<EpgProgramme> {
        val list = mutableListOf<EpgProgramme>()
        try {
            val id = sourceId ?: getActiveSourceId() ?: "default"
            var file = java.io.File(getApplication<Application>().cacheDir, "epg_cache_$id.json")
            if (!file.exists()) {
                file = java.io.File(getApplication<Application>().cacheDir, "epg_cache.json")
            }
            if (file.exists()) {
                val json = file.readText()
                val arr = org.json.JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        EpgProgramme(
                            channelId = obj.getString("channelId"),
                            title = obj.getString("title"),
                            startMs = obj.getLong("startMs"),
                            endMs = obj.getLong("endMs"),
                            description = obj.optString("description").takeIf { it.isNotBlank() }
                        )
                    )
                }
                Log.d(TAG, "Loaded EPG from local storage cache for $id: ${list.size} records.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching cached EPG lists", e)
        }
        return list
    }

    fun reloadEpg() {
        val activeM3u = _m3uPlaylists.value.find { it.isActive }
        val activeXtream = _xtreamAccounts.value.find { it.isActive }
        if (activeM3u == null && activeXtream == null) {
            _epgLoadStatus.value = "No active source to reload/refresh EPG."
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isEpgLoading.value = true
            _epgLoadStatus.value = "Refetching and updating EPG database..."
            try {
                var loaded = emptyList<EpgProgramme>()
                if (activeM3u != null) {
                    val xmltvUrl = activeM3u.epgUrl
                    if (!xmltvUrl.isNullOrBlank()) {
                        IptvClient.fetchUrlStream(xmltvUrl).use { xmlStream ->
                            loaded = IptvParser.parseXmltv(xmlStream)
                        }
                    } else {
                        throw Exception("Active playlist does not contain an EPG / XMLTV feed.")
                    }
                } else if (activeXtream != null) {
                    val directXmlEpgUrl = "${activeXtream.serverUrl}/xmltv.php?username=${activeXtream.username}&password=${activeXtream.password}"
                    try {
                        IptvClient.fetchUrlStream(directXmlEpgUrl).use { xmlStream ->
                            loaded = IptvParser.parseXmltv(xmlStream)
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Direct XMLTV reload failed, trying short EPG batch", ex)
                        val combined = mutableListOf<EpgProgramme>()
                        val chs = _iptvChannels.value.take(50)
                        for (ch in chs) {
                            try {
                                val batch = IptvClient.fetchXtreamShortEpg(
                                    activeXtream.serverUrl, activeXtream.username, activeXtream.password, ch.id
                                )
                                combined.addAll(batch)
                            } catch (e: Exception) { /* skip */ }
                        }
                        loaded = combined
                    }
                }

                if (loaded.isNotEmpty()) {
                    _epgProgrammes.value = loaded
                    saveEpgToLocalCache(loaded)
                    EpgRefreshManager.setLastRefreshTimeGlobal(getApplication(), System.currentTimeMillis())
                    val activeId = getActiveSourceId()
                    if (activeId != null) {
                        EpgRefreshManager.setLastRefreshTimeForSource(getApplication(), activeId, System.currentTimeMillis())
                    }
                    updateLastRefreshTime()
                    _epgLoadStatus.value = "EPG Reloaded successfully! (${loaded.size} shows loaded)"
                } else {
                    _epgLoadStatus.value = "Reload completed: No scheduling records recovered."
                }
            } catch (e: Exception) {
                _epgLoadStatus.value = "Failed refreshing EPG: ${e.localizedMessage}"
            } finally {
                _isEpgLoading.value = false
                // Auto reset loading status label in 5 seconds
                kotlinx.coroutines.delay(5000L)
                _epgLoadStatus.value = null
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

    // --- Custom Categories Management ---

    fun showEpgSorter(show: Boolean) {
        _showEpgSorter.value = show
    }

    fun setCategorySortMode(mode: CategorySortMode) {
        viewModelScope.launch {
            _categorySortMode.value = mode
            preferenceManager.saveIptvCategorySortMode(mode.name)
        }
    }

    fun setChannelSortMode(mode: ChannelSortMode) {
        viewModelScope.launch {
            _channelSortMode.value = mode
            preferenceManager.saveIptvChannelSortMode(mode.name)
        }
    }

    fun updateIptvChannelOrder(newOrder: List<String>) {
        viewModelScope.launch {
            _customChannelOrder.value = newOrder
            preferenceManager.saveIptvChannelOrder(newOrder)
        }
    }

    fun updateIptvCategoryOrder(newOrder: List<String>) {
        viewModelScope.launch {
            _customCategoryOrder.value = newOrder
            preferenceManager.saveIptvCategoryOrder(newOrder)
        }
    }

    fun toggleIptvCategoryVisibility(categoryId: String) {
        viewModelScope.launch {
            val currentHidden = _hiddenCategoryIds.value.toMutableSet()
            if (currentHidden.contains(categoryId)) {
                currentHidden.remove(categoryId)
            } else {
                currentHidden.add(categoryId)
            }
            _hiddenCategoryIds.value = currentHidden
            preferenceManager.saveIptvHiddenCategoryIds(currentHidden)

            val currentSelected = _selectedCategory.value
            if (currentSelected != null && currentSelected.id == categoryId) {
                val visibleList = _iptvCategories.value.filter { !currentHidden.contains(it.id) }
                _selectedCategory.value = visibleList.firstOrNull()
            }
        }
    }

    fun resetIptvCategoriesToDefault() {
        viewModelScope.launch {
            _customCategoryOrder.value = emptyList()
            _hiddenCategoryIds.value = emptySet()
            preferenceManager.saveIptvCategoryOrder(emptyList())
            preferenceManager.saveIptvHiddenCategoryIds(emptySet())
        }
    }

    // --- IPTV Channel History Engine ---

    fun addToIptvHistory(channel: IptvChannel) {
        viewModelScope.launch {
            val current = _iptvHistory.value.toMutableList()
            current.removeAll { it.id == channel.id }
            current.add(
                0,
                IptvHistoryItem(
                    id = channel.id,
                    name = channel.name,
                    logoUrl = channel.logoUrl,
                    streamUrl = channel.streamUrl,
                    categoryId = channel.categoryId,
                    epgId = channel.epgId,
                    lastWatchedTimestamp = System.currentTimeMillis()
                )
            )
            val limited = current.take(50)
            _iptvHistory.value = limited
            preferenceManager.saveIptvHistoryJson(serializeIptvHistory(limited))
        }
    }

    fun deleteHistoryItem(channelId: String) {
        viewModelScope.launch {
            val current = _iptvHistory.value.toMutableList()
            current.removeAll { it.id == channelId }
            _iptvHistory.value = current
            preferenceManager.saveIptvHistoryJson(serializeIptvHistory(current))
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            _iptvHistory.value = emptyList()
            preferenceManager.saveIptvHistoryJson(null)
        }
    }

    private fun deserializeIptvHistory(json: String?): List<IptvHistoryItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<IptvHistoryItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    IptvHistoryItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        logoUrl = if (obj.isNull("logoUrl")) null else obj.getString("logoUrl"),
                        streamUrl = obj.getString("streamUrl"),
                        categoryId = if (obj.isNull("categoryId")) "" else obj.getString("categoryId"),
                        epgId = if (obj.isNull("epgId")) null else obj.getString("epgId"),
                        lastWatchedTimestamp = obj.optLong("lastWatchedTimestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize IPTV history", e)
        }
        return list
    }

    private fun serializeIptvHistory(list: List<IptvHistoryItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("logoUrl", item.logoUrl)
            obj.put("streamUrl", item.streamUrl)
            obj.put("categoryId", item.categoryId)
            obj.put("epgId", item.epgId)
            obj.put("lastWatchedTimestamp", item.lastWatchedTimestamp)
            arr.put(obj)
        }
        return arr.toString()
    }
}

data class RecentLocalVideo(
    val uri: String,
    val title: String
)
