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

    private val _requestHeaders = MutableStateFlow<Map<String, String>>(emptyMap())
    val requestHeaders: StateFlow<Map<String, String>> = _requestHeaders.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showDebugPanel = MutableStateFlow(true)
    val showDebugPanel: StateFlow<Boolean> = _showDebugPanel.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // Themes states
    private val _activeThemeId = MutableStateFlow("cinema")
    val activeThemeId: StateFlow<String> = _activeThemeId.asStateFlow()

    private val _activeThemePreset = MutableStateFlow(ThemePresets.Cinema)
    val activeThemePreset: StateFlow<ThemePreset> = _activeThemePreset.asStateFlow()

    private val _activeAspectRatioId = MutableStateFlow("free")
    val activeAspectRatioId: StateFlow<String> = _activeAspectRatioId.asStateFlow()

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
            preferenceManager.selectedAspectRatioId.collect { id ->
                _activeAspectRatioId.value = id
            }
        }

        viewModelScope.launch {
            // Keep screen layout settings in sync with whichever theme is currently chosen
            _activeThemeId.collect { id ->
                preferenceManager.getLayoutSettings(id).collect { settings ->
                    _screenLayout.value = settings
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
            preferenceManager.saveSelectedAspectRatio(id)
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Selecting theme: $themeId")
            preferenceManager.saveSelectedTheme(themeId)
        }
    }

    fun updateScreenLayout(left: Float, top: Float, width: Float, height: Float, dimAlpha: Float) {
        viewModelScope.launch {
            val currentPreset = _activeThemePreset.value
            val settings = ScreenLayoutSettings(
                left = left,
                top = top,
                width = width,
                height = height,
                dimAlpha = dimAlpha,
                subtitleOffset = currentPreset.defaultSubtitleOffset
            )
            _screenLayout.value = settings
            preferenceManager.saveLayoutSettings(_activeThemeId.value, settings)
        }
    }

    fun resetActiveThemeLayout() {
        viewModelScope.launch {
            val id = _activeThemeId.value
            Log.d(TAG, "Resetting layout for theme $id")
            preferenceManager.resetThemeToDefault(id)
        }
    }

    fun resetAllToAppDefaults() {
        viewModelScope.launch {
            Log.d(TAG, "Resetting all settings to app defaults")
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
        _playableUri.value = defaultTestVideoUrl
        _errorMessage.value = null
    }

    fun setPlayableUri(uri: String?) {
        _playableUri.value = uri
        if (uri != null) {
            _errorMessage.value = null
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
}
