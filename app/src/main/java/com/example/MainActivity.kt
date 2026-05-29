package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize the MainViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Parse initial launch intent
        viewModel.handleIntent(intent)

        setContent {
            MyApplicationTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                val showCrashDialog = androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(CrashReporter.listReports(context).isNotEmpty())
                }

                CinemaPlayerScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )

                if (showCrashDialog.value) {
                    CrashReportDialog(
                        onDismiss = { showCrashDialog.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Parse incoming intent dynamically (e.g., when shared from Stremio while player is active)
        viewModel.handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        val hasActiveStream = viewModel.playableUri.value != null
        if (hasActiveStream) {
            enterPipMode()
        }
        super.onUserLeaveHint()
    }

    private fun enterPipMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                try {
                    @Suppress("DEPRECATION")
                    enterPictureInPictureMode()
                } catch (fallbackEx: Exception) {
                    android.util.Log.e("MainActivity", "Failed to enter Picture-in-Picture", fallbackEx)
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setInPictureInPicture(isInPictureInPictureMode)
    }
}
