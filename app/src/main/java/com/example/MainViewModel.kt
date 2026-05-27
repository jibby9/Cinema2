package com.example

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

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

    // Default video stream URL for test playback
    val defaultTestVideoUrl = "https://sandbox-videos.web.cern.ch/record/2241796"

    // Alternative Fallback URLs for easy switching during development:
    // Fallback Sample: "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
    // Fallback Sample 2 (Sintel): "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"

    private val TAG = "MainViewModel"

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
