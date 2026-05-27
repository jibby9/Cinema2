package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color definitions local to this file to match "Sophisticated Dark" and guarantee error-free builds
private val IndigoPrimary = Color(0xFF6366F1)       // Vibrant Indigo Accent
private val IndigoSecondary = Color(0xFF4F46E5)     // Deep Indigo Accent
private val IndigoBadgeText = Color(0xFFA5B4FC)     // Light violet/indigo
private val AubergineDarkBg = Color(0xFF0D0B0F)     // Core app dark canvas
private val ObsidianSurface = Color(0xFF141218)     // Material 3 dark surface container
private val CinemaBevelCharcoal = Color(0xFF1A1A1A) // Film screen outer frame
private val CoralError = Color(0xFFEF4444)          // Soft red alert accent
private val ConsoleGreen = Color(0xFF34D399)        // Retro console green log highlights
private val TextSilver = Color(0xFFE2E8F0)          // Slate-100 text
private val TextMuted = Color(0xFF64748B)           // Slate-500 text

@Composable
fun CinemaPlayerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val parsedIntent by viewModel.parsedIntent.collectAsState()
    val playableUri by viewModel.playableUri.collectAsState()
    val requestHeaders by viewModel.requestHeaders.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showDebugPanel by viewModel.showDebugPanel.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current

    // Screen classification: if screenWidthDp >= 600, treat as tablet or unfolded foldable inner display.
    val isExpandedLayout = configuration.screenWidthDp >= 600

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AubergineDarkBg)
    ) {
        // 1. Cinema Backdrop Simulation (radial gradient from #2A2438 to transparent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val radialBrush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2A2438).copy(alpha = 0.35f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.25f),
                        radius = size.width * 0.85f
                    )
                    drawRect(brush = radialBrush)
                }
        )

        // 2. Linear/Grid backdrop simulation helper lines (opacity 3%)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(6) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.03f))
                )
            }
        }

        // Main Scaffold Layout containing Top Bar and dynamic adaptive play elements
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CinemaTitleBar(
                    onToggleDebug = { viewModel.toggleDebugPanel() },
                    showDebugPanel = showDebugPanel,
                    isFoldableActive = isExpandedLayout
                )
            }
        ) { paddingValues ->
            if (isExpandedLayout) {
                // Adaptive layout: Split-screen horizontal row for unfolded tablets or premium foldable displays
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Side: Projector Video Player Screen area (Elevated share)
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        CinemaTheaterLayout(
                            playableUri = playableUri,
                            errorMessage = errorMessage,
                            headers = requestHeaders,
                            onPlayTestVideo = { viewModel.playTestVideo() },
                            onClearPlaySource = { viewModel.setPlayableUri(null) },
                            onPlaybackError = { detail -> viewModel.setErrorMessage(detail) }
                        )
                    }

                    // Right Side: Beautiful Dev Log / Intent Details panel (sliding-in collapsible card)
                    AnimatedVisibility(
                        visible = showDebugPanel,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        DebugConsolePanel(
                            parsedIntent = parsedIntent,
                            playableUri = playableUri,
                            errorMessage = errorMessage,
                            onCopyLogs = {
                                val dump = parsedIntent?.rawDetailsDump ?: "No logs captured yet."
                                clipboardManager.setText(AnnotatedString(dump))
                            },
                            onClosePanel = { viewModel.setDebugPanelVisible(false) }
                        )
                    }
                }
            } else {
                // Adaptive layout: Vertical stack for standard portrait phones
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top: Cinema projection layout area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CinemaTheaterLayout(
                            playableUri = playableUri,
                            errorMessage = errorMessage,
                            headers = requestHeaders,
                            onPlayTestVideo = { viewModel.playTestVideo() },
                            onClearPlaySource = { viewModel.setPlayableUri(null) },
                            onPlaybackError = { detail -> viewModel.setErrorMessage(detail) }
                        )
                    }

                    // Bottom: Intent debugging console list
                    AnimatedVisibility(
                        visible = showDebugPanel,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f)
                    ) {
                        DebugConsolePanel(
                            parsedIntent = parsedIntent,
                            playableUri = playableUri,
                            errorMessage = errorMessage,
                            onCopyLogs = {
                                val dump = parsedIntent?.rawDetailsDump ?: "No logs captured yet."
                                clipboardManager.setText(AnnotatedString(dump))
                            },
                            onClosePanel = { viewModel.setDebugPanelVisible(false) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Material 3 style title bar matching "Sophisticated Dark" aesthetic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaTitleBar(
    onToggleDebug: () -> Unit,
    showDebugPanel: Boolean,
    isFoldableActive: Boolean
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Beautiful customized icon container with shadow-like tint
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IndigoSecondary)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Cinema Player",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }
        },
        actions = {
            // Foldable Status active badge
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isFoldableActive) "Foldable: Active" else "Compact Display",
                    color = IndigoBadgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            IconButton(
                onClick = onToggleDebug,
                modifier = Modifier.testTag("debug_console_toggle")
            ) {
                Icon(
                    imageVector = if (showDebugPanel) Icons.Default.BugReport else Icons.Default.Info,
                    contentDescription = "Toggle Debugger Info",
                    tint = if (showDebugPanel) IndigoPrimary else Color.LightGray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}

/**
 * Modern Projection Screen Frame designed with fine cinematic bevels and drop glow.
 */
@Composable
fun CinemaTheaterLayout(
    playableUri: String?,
    errorMessage: String?,
    headers: Map<String, String>,
    onPlayTestVideo: () -> Unit,
    onClearPlaySource: () -> Unit,
    onPlaybackError: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High Contrast screen box simulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .testTag("video_screen_container")
                .clip(RoundedCornerShape(4.dp))
                // Clean rectangular Cinema Screen framework with shadow glows
                .border(4.dp, CinemaBevelCharcoal, RoundedCornerShape(4.dp))
                .drawBehind {
                    // Simulating the elegant outer neon glow shadow in design [0_0_50px_rgba(79,70,229,0.15)]
                    drawRect(
                        color = IndigoPrimary.copy(alpha = 0.08f),
                        topLeft = androidx.compose.ui.geometry.Offset(-8f, -8f),
                        size = androidx.compose.ui.geometry.Size(size.width + 16f, size.height + 16f)
                    )
                }
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (!playableUri.isNullOrBlank()) {
                VideoPlayerView(
                    videoUrl = playableUri,
                    onPlaybackError = onPlaybackError,
                    modifier = Modifier.fillMaxSize(),
                    headers = headers
                )

                // Overlay Controls Corner Release
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = onClearPlaySource,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop streams",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                TheaterFallbackCurtain(
                    errorMessage = errorMessage,
                    onPlayTestVideo = onPlayTestVideo
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Screen visual sub caption
        Text(
            text = "CINEMATIC SCREEN OVERLAY",
            color = Color.White.copy(alpha = 0.35f),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Display corresponding beautiful empty states matching Sophisticated Dark.
 */
@Composable
fun TheaterFallbackCurtain(
    errorMessage: String?,
    onPlayTestVideo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // SVG styled icon rendering
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = "PLAYBACK ERROR OCCURRED",
                color = CoralError,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = TextSilver.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            Text(
                text = "Waiting for Intent...",
                color = TextSilver.copy(alpha = 0.8f),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Supports HTTP, content, & file streaming URIs",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Large Action quick playback button (Indigo background matching layout) -> Rounded Full
        Button(
            onClick = onPlayTestVideo,
            colors = ButtonDefaults.buttonColors(
                containerColor = IndigoPrimary,
                contentColor = Color.White
            ),
            shape = CircleShape,
            modifier = Modifier
                .testTag("play_test_button")
                .height(44.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Test Playback URI",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.25.sp
                )
            )
        }
    }
}

/**
 * Bottom Dev Shell logs console panel showing parsed elements.
 */
@Composable
fun DebugConsolePanel(
    parsedIntent: ParsedIntentInfo?,
    playableUri: String?,
    errorMessage: String?,
    onCopyLogs: () -> Unit,
    onClosePanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Persistent bottom card mimicking Sophisticated Dark tailwind section
    Card(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ObsidianSurface,
            contentColor = TextSilver
        ),
        modifier = modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .testTag("intent_details_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            // Header actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "INTENT DEBUG LOG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )

                    // Double diagnostic pulse led dot layout
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flashing green status indicator
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ConsoleGreen)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy action button
                    IconButton(
                        onClick = onCopyLogs,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy debugging telemetry",
                            tint = TextSilver.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Close action button
                    IconButton(
                        onClick = onClosePanel,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Console",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body display showing raw details
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Token line ACTION VIEW
                val actionName = parsedIntent?.action ?: "android.intent.action.VIEW"
                DebugLogToken(label = "ACTION", value = actionName, valueColor = TextSilver)

                // Token line DATA URI
                val dataVal = playableUri ?: parsedIntent?.dataUri ?: "Awaiting playback transfer..."
                DebugLogToken(
                    label = "DATA",
                    value = dataVal,
                    valueColor = if (playableUri != null) ConsoleGreen else TextSilver.copy(alpha = 0.5f),
                    isUnderline = playableUri != null
                )

                // Token line MIME
                val mimeVal = parsedIntent?.mimeType ?: "video/x-matroska"
                DebugLogToken(label = "MIME", value = mimeVal, valueColor = TextSilver)

                // Token line CLIPDATA
                val clipCount = parsedIntent?.clipDataItems?.size ?: 0
                DebugLogToken(
                    label = "CLIP",
                    value = if (clipCount > 0) "$clipCount item(s) detected" else "null (fallback text parser used)",
                    valueColor = TextMuted
                )

                // If fallback error occurred, render beautiful warning banner block directly mimicking tailwind HTML:
                // "p-3 rounded-2xl bg-red-900/20 border border-red-500/20 flex items-center gap-3"
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CoralError.copy(alpha = 0.15f))
                            .border(1.dp, CoralError.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Alert",
                                tint = CoralError,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Playback warning: $errorMessage",
                                color = TextSilver,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Screen Width: ${LocalConfiguration.current.screenWidthDp}dp",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = TextMuted)
                )
                Text(
                    text = "MIME: http, https, content, file",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = TextMuted)
                )
            }
        }
    }
}

/**
 * Elegant key-value logs row stylized to resemble retro/cinematic dev panels.
 */
@Composable
fun DebugLogToken(
    label: String,
    value: String,
    valueColor: Color,
    isUnderline: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "[$label]",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary
            )

            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = valueColor,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
