package com.example

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout

@OptIn(UnstableApi::class)
object SharedPlayerSessionManager {
    private var cachedExoPlayer: ExoPlayer? = null
    var lastUrl: String? = null
    var lastHeaders: Map<String, String>? = null

    @Synchronized
    fun getOrCreatePlayer(context: android.content.Context): ExoPlayer {
        var player = cachedExoPlayer
        if (player == null) {
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent("CinemaPlayer/1.1 (Android; Media3; ExoPlayer)")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(8000) // fail fast on dead IPTV links
                .setReadTimeoutMs(8000)
            val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(defaultDataSourceFactory)

            // Setup ultra-low latency buffering control optimized for live IPTV zapping
            val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    2000,  // minBufferMs (standard is 50s. 2s is highly secure yet instant)
                    10000, // maxBufferMs (standard is 50s. 10s prevents loading massive backlog)
                    1000,  // bufferForPlaybackMs (lowers startup waiting to 1s)
                    1500   // bufferForPlaybackAfterRebufferMs (fast recovery on stutters)
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build().apply {
                    playWhenReady = true
                }
            cachedExoPlayer = player
        }
        return player
    }

    @Synchronized
    fun release() {
        cachedExoPlayer?.release()
        cachedExoPlayer = null
        lastUrl = null
        lastHeaders = null
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    videoUrl: String,
    onPlaybackError: (String) -> Unit,
    modifier: Modifier = Modifier,
    headers: Map<String, String> = emptyMap(),
    displayMode: String = "adaptive",
    useController: Boolean = true,
    onVideoAspectRatioDetected: (Float) -> Unit = {},
    onPlayerInitialized: (ExoPlayer?) -> Unit = {}
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }

    // Retrieve or lazy-initialize the singleton shared ExoPlayer session
    val exoPlayer = remember { SharedPlayerSessionManager.getOrCreatePlayer(context) }

    // Connect lifecycle and release the player when the Composable leaves the composition
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val rawRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    val ratio = rawRatio * videoSize.pixelWidthHeightRatio
                    onVideoAspectRatioDetected(ratio)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false

                var responseCode: Int? = null
                var failingUrl: String? = null

                var cause: Throwable? = error.cause
                while (cause != null) {
                    if (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                        responseCode = cause.responseCode
                        failingUrl = cause.dataSpec?.uri?.toString()
                        break
                    } else if (cause is androidx.media3.datasource.HttpDataSource.HttpDataSourceException) {
                        failingUrl = cause.dataSpec?.uri?.toString()
                    }
                    cause = cause.cause
                }

                val errorDetails = buildString {
                    append(error.localizedMessage ?: error.message ?: "Playback failure")
                    if (responseCode != null) {
                        append("\nHTTP Response Code: $responseCode")
                    }
                    if (!failingUrl.isNullOrBlank()) {
                        append("\nFailing URL: $failingUrl")
                    }
                    if (responseCode == null && failingUrl == null) {
                        append(" (Code: ${error.errorCodeName})")
                    }
                }
                onPlaybackError(errorDetails)
            }
        }
        exoPlayer.addListener(listener)
        // Handover player reference to host UI
        onPlayerInitialized(exoPlayer)

        onDispose {
            exoPlayer.removeListener(listener)
            // Detach player initialized state, but DO NOT release ExoPlayer!
            // This is critical to maintain persistent active playback while opening/closing TV guide!
            onPlayerInitialized(null)
        }
    }

    // Reactively update the play source and apply request headers ONLY when the videoUrl or headers change to something new!
    LaunchedEffect(videoUrl, headers) {
        if (videoUrl.isNotBlank()) {
            val isSameSource = SharedPlayerSessionManager.lastUrl == videoUrl && 
                               SharedPlayerSessionManager.lastHeaders == headers

            if (!isSameSource) {
                isLoading = true
                // Implement zapping debounce to absorb rapid sequential clicks without hitting the network/player repeatedly
                kotlinx.coroutines.delay(200)
                try {
                    val mediaItem = MediaItem.fromUri(videoUrl)
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()

                    SharedPlayerSessionManager.lastUrl = videoUrl
                    SharedPlayerSessionManager.lastHeaders = headers
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    isLoading = false
                    onPlaybackError(e.localizedMessage ?: "Preparation failed")
                }
            } else {
                // Same source, update loading state matching current playback state
                isLoading = exoPlayer.playbackState == Player.STATE_BUFFERING
            }
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Embed the Media3 PlayerView inside Compose using AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    this.useController = useController
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                // Make sure player matches the current instance
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
                playerView.useController = useController
                playerView.resizeMode = when (displayMode.lowercase()) {
                    "fit" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    "zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    "fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
