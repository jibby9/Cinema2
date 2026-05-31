package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.example.ui.theme.MyApplicationTheme

class MainActivity : androidx.activity.ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private var castContext: com.google.android.gms.cast.framework.CastContext? = null
    private var sessionManagerListener: com.google.android.gms.cast.framework.SessionManagerListener<com.google.android.gms.cast.framework.CastSession>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize the MainViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Safe setup of CastContext to prevent crashes on non-Google Play Services environment
        try {
            castContext = com.google.android.gms.cast.framework.CastContext.getSharedInstance(this)
            setupCastSessionListener()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Google Cast SDK not available: ${e.localizedMessage}")
        }

        // Observe application lifecycle to trigger EPG refresh on foregrounding
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                android.util.Log.d("MainActivity", "App returned to foreground. Refurbishing EPG records.")
                viewModel.onAppReturnedToForeground()
            }
        })

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

    private fun setupCastSessionListener() {
        sessionManagerListener = object : com.google.android.gms.cast.framework.SessionManagerListener<com.google.android.gms.cast.framework.CastSession> {
            override fun onSessionStarted(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {
                viewModel.setCastSession(session)
            }

            override fun onSessionResumed(session: com.google.android.gms.cast.framework.CastSession, wasSuspended: Boolean) {
                viewModel.setCastSession(session)
            }

            override fun onSessionEnded(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
                viewModel.setCastSession(null)
            }

            override fun onSessionStarting(session: com.google.android.gms.cast.framework.CastSession) {}
            override fun onSessionStartFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {}
            override fun onSessionEnding(session: com.google.android.gms.cast.framework.CastSession) {}
            override fun onSessionResuming(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {}
            override fun onSessionResumeFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {}
            override fun onSessionSuspended(session: com.google.android.gms.cast.framework.CastSession, reason: Int) {}
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            sessionManagerListener?.let {
                castContext?.sessionManager?.addSessionManagerListener(it, com.google.android.gms.cast.framework.CastSession::class.java)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error adding cast session listener", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            sessionManagerListener?.let {
                castContext?.sessionManager?.removeSessionManagerListener(it, com.google.android.gms.cast.framework.CastSession::class.java)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error removing cast session listener", e)
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
