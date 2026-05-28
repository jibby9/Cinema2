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

    private val _recentChannels = MutableStateFlow<List<IptvChannel>>(emptyList())
    val recentChannels: StateFlow<List<IptvChannel>> = _recentChannels.asStateFlow()

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
    
    private val _customCategoryOrder = MutableStateFlow<List<String>>(emptyList())
    val customCategoryOrder: StateFlow<List<String>> = _customCategoryOrder.asStateFlow()

    private val _hiddenCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenCategoryIds: StateFlow<Set<String>> = _hiddenCategoryIds.asStateFlow()

    val allRawIptvCategories: StateFlow<List<IptvCategory>> = _iptvCategories.asStateFlow()

    val iptvCategories: StateFlow<List<IptvCategory>> = combine(
        _iptvCategories,
        _customCategoryOrder,
        _hiddenCategoryIds
    ) { rawCats, order, hiddenIds ->
        val visibleCats = rawCats.filter { !hiddenIds.contains(it.id) }
        val orderMap = order.withIndex().associate { it.value to it.index }
        visibleCats.sortedWith(
            compareBy<IptvCategory> { orderMap[it.id] ?: Int.MAX_VALUE }
                .thenBy { rawCats.indexOf(it) }
        )
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

    // Local Video Playback states
    private val _recentLocalVideos = MutableStateFlow<List<RecentLocalVideo>>(emptyList())
    val recentLocalVideos: StateFlow<List<RecentLocalVideo>> = _recentLocalVideos.asStateFlow()

    private val _localVideoTitle = MutableStateFlow<String?>(null)
    val localVideoTitle: StateFlow<String?> = _localVideoTitle.asStateFlow()

    // Sports Schedule State fields
    private val sportsRepository: SportsRepository = StaticSportsRepository()

    private val _sportsEvents = MutableStateFlow<List<SportsEvent>>(emptyList())
    val sportsEvents: StateFlow<List<SportsEvent>> = _sportsEvents.asStateFlow()

    private val _selectedSportFilter = MutableStateFlow<String?>(null)
    val selectedSportFilter: StateFlow<String?> = _selectedSportFilter.asStateFlow()

    private val _sportsSearchQuery = MutableStateFlow("")
    val sportsSearchQuery: StateFlow<String> = _sportsSearchQuery.asStateFlow()

    private val _liveNowOnly = MutableStateFlow(false)
    val liveNowOnly: StateFlow<Boolean> = _liveNowOnly.asStateFlow()

    // Sports Ticker Manager integration
    val sportsTickerManager = LiveSportsTickerManager()

    val tickerScore: StateFlow<TickerScore?> = sportsTickerManager.scoreFlow
    val tickerStats: StateFlow<List<TickerStat>> = sportsTickerManager.statsFlow
    val tickerTimeline: StateFlow<List<TickerTimelineEvent>> = sportsTickerManager.timelineFlow
    val tickerSyncConfig: StateFlow<TickerSyncConfig> = sportsTickerManager.syncConfig

    private val _identifiedSportsEvent = MutableStateFlow<SportsEvent?>(null)
    val identifiedSportsEvent: StateFlow<SportsEvent?> = _identifiedSportsEvent.asStateFlow()

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
            preferenceManager.iptvHiddenCategoryIds.collect { hidden ->
                _hiddenCategoryIds.value = hidden
            }
        }

        viewModelScope.launch {
            preferenceManager.localRecentVideosJson.collect { json ->
                _recentLocalVideos.value = deserializeRecentLocalVideos(json)
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

        // ==========================================
        // INITIALIZE PREMIUM FEATURES
        // ==========================================
        _presets.value = LayoutPreset.BuiltInPresets + premiumStore.loadCustomPresets()
        _ambientGlow.value = premiumStore.getAmbientGlow()
        _reminders.value = premiumStore.loadReminders()

        startRemindersPolling()
        refreshSportsEvents()

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
            _identifiedSportsEvent.value = null
            sportsTickerManager.stopTracking()
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
        val current = _currentPlayingChannel.value
        if (current != null && current.id != channel.id) {
            val hist = _recentChannels.value.toMutableList()
            hist.removeAll { it.id == current.id }
            hist.add(0, current)
            if (hist.size > 10) {
                hist.removeAt(hist.lastIndex)
            }
            _recentChannels.value = hist
        }

        _currentPlayingChannel.value = channel
        setPlayableUri(channel.streamUrl)
        
        onActiveChannelOrEpgChanged(channel)

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
        val previous = _recentChannels.value.firstOrNull()
        if (previous != null) {
            playIptvChannel(previous)
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

            // Try rendering instantaneous cached EPG first!
            try {
                val cached = loadEpgFromLocalCache()
                if (cached.isNotEmpty()) {
                    _epgProgrammes.value = cached
                }
            } catch (e: Exception) {
                Log.e(TAG, "Local EPG cache initial load failed", e)
            }

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
                                    saveEpgToLocalCache(eps)
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

    private fun saveEpgToLocalCache(programmes: List<EpgProgramme>) {
        try {
            val file = java.io.File(getApplication<Application>().cacheDir, "epg_cache.json")
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
            Log.d(TAG, "Saved EPG local cache directory: ${programmes.size} records saved.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing EPG to local storage cache", e)
        }
    }

    private fun loadEpgFromLocalCache(): List<EpgProgramme> {
        val list = mutableListOf<EpgProgramme>()
        try {
            val file = java.io.File(getApplication<Application>().cacheDir, "epg_cache.json")
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
                Log.d(TAG, "Loaded EPG from local storage cache: ${list.size} records.")
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

    // --- Local Videos Playback Engine ---

    fun playLocalVideo(uri: String, filename: String) {
        _localVideoTitle.value = filename
        val virtualChannel = IptvChannel(
            id = "local_${uri.hashCode()}",
            name = filename,
            logoUrl = null,
            streamUrl = uri,
            categoryId = "Local Videos",
            isFavorite = false
        )
        _currentPlayingChannel.value = virtualChannel
        _playableUri.value = uri
        _errorMessage.value = null
        
        saveRecentLocalVideo(uri, filename)
    }

    private fun saveRecentLocalVideo(uri: String, filename: String) {
        viewModelScope.launch {
            val current = _recentLocalVideos.value.toMutableList()
            current.removeAll { it.uri == uri }
            current.add(0, RecentLocalVideo(uri, filename))
            val limited = current.take(10)
            _recentLocalVideos.value = limited
            preferenceManager.saveLocalRecentVideosJson(serializeRecentLocalVideos(limited))
        }
    }

    fun removeRecentLocalVideo(uri: String) {
        viewModelScope.launch {
            val current = _recentLocalVideos.value.toMutableList()
            current.removeAll { it.uri == uri }
            _recentLocalVideos.value = current
            preferenceManager.saveLocalRecentVideosJson(serializeRecentLocalVideos(current))
        }
    }

    private fun deserializeRecentLocalVideos(json: String?): List<RecentLocalVideo> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<RecentLocalVideo>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RecentLocalVideo(
                        uri = obj.getString("uri"),
                        title = obj.getString("title")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize recent videos", e)
        }
        return list
    }

    private fun serializeRecentLocalVideos(list: List<RecentLocalVideo>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("uri", item.uri)
            obj.put("title", item.title)
            arr.put(obj)
        }
        return arr.toString()
    }

    // --- Sports Scedules & Channel matching control ---

    fun setSportFilter(sport: String?) {
        _selectedSportFilter.value = sport
    }

    fun setSportsSearchQuery(query: String) {
        _sportsSearchQuery.value = query
    }

    fun toggleLiveNowOnly() {
        _liveNowOnly.value = !_liveNowOnly.value
    }

    fun refreshSportsEvents() {
        sportsRepository.refreshEvents()
        _sportsEvents.value = sportsRepository.getFeaturedEvents()
    }

    fun getMatchingChannelsForEvent(event: SportsEvent): List<ChannelMatchResult> {
        return SportsChannelMatcher.matchEventToChannels(
            event = event,
            channels = _iptvChannels.value,
            categories = allRawIptvCategories.value,
            epgList = _epgProgrammes.value
        )
    }

    fun startTrackingSportEvent(event: SportsEvent) {
        _identifiedSportsEvent.value = event
        sportsTickerManager.startTrackingEvent(event.title)
    }

    fun onActiveChannelOrEpgChanged(activeChan: IptvChannel) {
        viewModelScope.launch {
            // First we need event list
            val events = _sportsEvents.value
            if (events.isEmpty()) return@launch

            var matchedEvent: SportsEvent? = null

            // 1. Try to match currently playing EPG show text
            val channelEpgs = _epgProgrammes.value.filter { it.channelId == activeChan.id || it.channelId == activeChan.epgId }
            val now = System.currentTimeMillis()
            val currentEpgObj = channelEpgs.find { now in it.startMs..it.endMs }

            if (currentEpgObj != null) {
                val title = currentEpgObj.title.lowercase()
                matchedEvent = events.find { event ->
                    val teamA = event.teamA?.lowercase() ?: ""
                    val teamB = event.teamB?.lowercase() ?: ""
                    val titleNorm = event.title.lowercase()
                    (teamA.isNotEmpty() && title.contains(teamA)) || 
                    (teamB.isNotEmpty() && title.contains(teamB)) ||
                    title.contains(titleNorm)
                }
            }

            // 2. If EPG didn't match, check by active channel name keywords
            if (matchedEvent == null) {
                val chanName = activeChan.name.lowercase()
                matchedEvent = events.find { event ->
                    val teamA = event.teamA?.lowercase() ?: ""
                    val teamB = event.teamB?.lowercase() ?: ""
                    (teamA.isNotEmpty() && chanName.contains(teamA)) ||
                    (teamB.isNotEmpty() && chanName.contains(teamB))
                }
            }

            // 3. Fallback: find any LIVE sports event for the matching category
            if (matchedEvent == null) {
                val chanName = activeChan.name.lowercase()
                val isSportChan = chanName.contains("sport") || chanName.contains("f1") || chanName.contains("ufc")
                if (isSportChan) {
                    matchedEvent = events.find { it.getStatus() == "LIVE" }
                }
            }

            // Update tracked event
            matchedEvent?.let {
                _identifiedSportsEvent.value = it
                sportsTickerManager.startTrackingEvent(it.id) // track by id or title in manager
            }
        }
    }
}

data class RecentLocalVideo(
    val uri: String,
    val title: String
)
