package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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

import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector

// Color definitions local to this file to match "Sophisticated Dark" and guarantee error-free builds
private val IndigoPrimary = Color(0xFF6366F1)       // Vibrant Indigo Accent
private val IndigoSecondary = Color(0xFF4F46E5)     // Deep Indigo Accent
private val IndigoBadgeText = Color(0xFFA5B4FC)     // Light violet/indigo
private val AubergineDarkBg = Color(0xFF0C0A0F)     // Core app dark canvas
private val ObsidianSurface = Color(0xFF141218)     // Material 3 dark surface container
private val CinemaBevelCharcoal = Color(0xFF242426) // Film screen outer frame
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

    // Themes & Layout Preferences State
    val activeThemeId by viewModel.activeThemeId.collectAsState()
    val activeThemePreset by viewModel.activeThemePreset.collectAsState()
    val screenLayout by viewModel.screenLayout.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val activeAspectRatioId by viewModel.activeAspectRatioId.collectAsState()
    val activeResizeMode by viewModel.activeResizeMode.collectAsState()
    val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsState()
    val isAnimationEnabled by viewModel.isAnimationEnabled.collectAsState()

    // IPTV state collection
    val isIptvModeActive by viewModel.isIptvModeActive.collectAsState()
    val activeIptvTab by viewModel.activeIptvTab.collectAsState()
    val iptvChannels by viewModel.iptvChannels.collectAsState()
    val epgProgrammes by viewModel.epgProgrammes.collectAsState()
    val currentPlayingChannel by viewModel.currentPlayingChannel.collectAsState()
    val glowIntensitySetting by viewModel.ambientGlow.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val customBackgroundUri by viewModel.customBgUri.collectAsState()
    val activeReminderAlert by viewModel.activeReminderAlert.collectAsState()
    val isInPictureInPicture by viewModel.isInPictureInPicture.collectAsState()

    if (isInPictureInPicture) {
        CinemaTheaterLayout(
            viewModel = viewModel,
            themePreset = activeThemePreset,
            screenLayout = screenLayout.copy(dimAlpha = 0f),
            playableUri = playableUri,
            errorMessage = errorMessage,
            headers = requestHeaders,
            onPlayTestVideo = { viewModel.playTestVideo() },
            onClearPlaySource = { viewModel.setPlayableUri(null) },
            onPlaybackError = { detail -> viewModel.setErrorMessage(detail) },
            isEditMode = false,
            activeAspectRatioId = activeAspectRatioId,
            activeResizeMode = activeResizeMode,
            onSelectResizeMode = { mode -> viewModel.selectResizeMode(mode) },
            isSettingsLoaded = isSettingsLoaded,
            isIptvActive = isIptvModeActive,
            channels = iptvChannels,
            epgList = epgProgrammes,
            currentPlayingChannel = currentPlayingChannel,
            onPlayChannel = { ch -> viewModel.playIptvChannel(ch) },
            onPlayNextChannel = { list -> viewModel.playNextIptvChannel(list) },
            onPlayPreviousChannel = { list -> viewModel.playPreviousIptvChannel(list) },
            onRecallPreviousChannel = { viewModel.recallPreviousIptvChannel() },
            glowIntensitySetting = AmbientGlowSetting.OFF,
            showDebugPanel = false,
            onToggleDebug = {},
            onSelectTab = {},
            onLayoutChanged = { _, _, _, _ -> },
            isEpgGuideMode = false,
            isAnimationEnabled = false,
            onToggleAnimation = {},
            onSelectTheme = {},
            modifier = modifier.fillMaxSize()
        )
        return
    }

    // Screen classification: if screenWidthDp >= 600, treat as tablet or unfolded foldable inner display.
    val isExpandedLayout = configuration.screenWidthDp >= 600

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Core Full-Screen Immersive Theme Backdrop - covering whole folding/inner screen area
        ThemeBackdrop(
            themePreset = activeThemePreset,
            isEditMode = isEditMode,
            customBackgroundUri = customBackgroundUri,
            isAnimationEnabled = isAnimationEnabled,
            modifier = Modifier.fillMaxSize()
        )

        // Main Scaffold Layout containing Top Bar and dynamic adaptive play elements
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CinemaTitleBar(
                    onToggleDebug = { viewModel.toggleDebugPanel() },
                    showDebugPanel = showDebugPanel,
                    isFoldableActive = isExpandedLayout,
                    activeThemeName = activeThemePreset.name,
                    isIptvActive = isIptvModeActive,
                    onToggleIptv = { viewModel.setIptvModeActive(it) }
                )
            }
        ) { paddingValues ->
            val isFullScreenGuide = isIptvModeActive && showDebugPanel && activeIptvTab == 1

            if (isFullScreenGuide) {
                FullscreenSkyGuide(
                    viewModel = viewModel,
                    onCloseGuide = { viewModel.setDebugPanelVisible(false) },
                    mediaPlayerContent = {
                        CinemaTheaterLayout(
                            viewModel = viewModel,
                            themePreset = activeThemePreset,
                            screenLayout = screenLayout.copy(dimAlpha = 0f),
                            playableUri = playableUri,
                            errorMessage = errorMessage,
                            headers = requestHeaders,
                            onPlayTestVideo = { viewModel.playTestVideo() },
                            onClearPlaySource = { viewModel.setPlayableUri(null) },
                            onPlaybackError = { detail -> viewModel.setErrorMessage(detail) },
                            isEditMode = false,
                            activeAspectRatioId = activeAspectRatioId,
                            activeResizeMode = activeResizeMode,
                            onSelectResizeMode = { mode -> viewModel.selectResizeMode(mode) },
                            isSettingsLoaded = isSettingsLoaded,
                            isIptvActive = isIptvModeActive,
                            channels = iptvChannels,
                            epgList = epgProgrammes,
                            currentPlayingChannel = currentPlayingChannel,
                            onPlayChannel = { ch -> viewModel.playIptvChannel(ch) },
                            onPlayNextChannel = { list -> viewModel.playNextIptvChannel(list) },
                            onPlayPreviousChannel = { list -> viewModel.playPreviousIptvChannel(list) },
                            onRecallPreviousChannel = { viewModel.recallPreviousIptvChannel() },
                            glowIntensitySetting = AmbientGlowSetting.OFF,
                            showDebugPanel = showDebugPanel,
                            onToggleDebug = { viewModel.toggleDebugPanel() },
                            onSelectTab = { tab -> viewModel.setActiveIptvTab(tab) },
                            onLayoutChanged = { _, _, _, _ -> },
                            isEpgGuideMode = true,
                            isAnimationEnabled = isAnimationEnabled,
                            onToggleAnimation = { enabled -> viewModel.setAnimationEnabled(enabled) },
                            onSelectTheme = { id -> viewModel.selectTheme(id) },
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                )
            } else if (isExpandedLayout && isIptvModeActive) {
                // PREMIUM FOLD DUAL-PANE SPLIT-SCREEN LAYOUT
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Pane: Cinema Player Container
                    Box(
                        modifier = Modifier
                            .weight(if (showDebugPanel) 0.58f else 1f)
                            .fillMaxHeight()
                    ) {
                        CinemaTheaterLayout(
                            viewModel = viewModel,
                            themePreset = activeThemePreset,
                            screenLayout = screenLayout,
                            playableUri = playableUri,
                            errorMessage = errorMessage,
                            headers = requestHeaders,
                            onPlayTestVideo = { viewModel.playTestVideo() },
                            onClearPlaySource = { viewModel.setPlayableUri(null) },
                            onPlaybackError = { detail -> viewModel.setErrorMessage(detail) },
                            isEditMode = isEditMode,
                            activeAspectRatioId = activeAspectRatioId,
                            activeResizeMode = activeResizeMode,
                            onSelectResizeMode = { mode -> viewModel.selectResizeMode(mode) },
                            isSettingsLoaded = isSettingsLoaded,
                            isIptvActive = isIptvModeActive,
                            channels = iptvChannels,
                            epgList = epgProgrammes,
                            currentPlayingChannel = currentPlayingChannel,
                            onPlayChannel = { ch -> viewModel.playIptvChannel(ch) },
                            onPlayNextChannel = { list -> viewModel.playNextIptvChannel(list) },
                            onPlayPreviousChannel = { list -> viewModel.playPreviousIptvChannel(list) },
                            onRecallPreviousChannel = { viewModel.recallPreviousIptvChannel() },
                            glowIntensitySetting = glowIntensitySetting,
                            showDebugPanel = showDebugPanel,
                            onToggleDebug = { viewModel.toggleDebugPanel() },
                            onSelectTab = { tab -> viewModel.setActiveIptvTab(tab) },
                            onLayoutChanged = { left, top, width, height ->
                                viewModel.updateScreenLayout(left, top, width, height, screenLayout.dimAlpha)
                            },
                            isAnimationEnabled = isAnimationEnabled,
                            onToggleAnimation = { enabled -> viewModel.setAnimationEnabled(enabled) },
                            onSelectTheme = { id -> viewModel.selectTheme(id) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Right Pane: Immersive TV Guide/Dashboard side-panel
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showDebugPanel,
                        enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + androidx.compose.animation.fadeOut(),
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            IptvDashboard(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                // STANDARD OVERLAPPING DRAWER / SIDEBAR LAYOUT (For mobile or non-epg streaming)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // 1. Full-Screen CinemaTheaterLayout rendering behind everything across the complete screen canvas
                    CinemaTheaterLayout(
                        viewModel = viewModel,
                        themePreset = activeThemePreset,
                        screenLayout = screenLayout,
                        playableUri = playableUri,
                        errorMessage = errorMessage,
                        headers = requestHeaders,
                        onPlayTestVideo = { viewModel.playTestVideo() },
                        onClearPlaySource = { viewModel.setPlayableUri(null) },
                        onPlaybackError = { detail -> viewModel.setErrorMessage(detail) },
                        isEditMode = isEditMode,
                        activeAspectRatioId = activeAspectRatioId,
                        activeResizeMode = activeResizeMode,
                        onSelectResizeMode = { mode -> viewModel.selectResizeMode(mode) },
                        isSettingsLoaded = isSettingsLoaded,
                        isIptvActive = isIptvModeActive,
                        channels = iptvChannels,
                        epgList = epgProgrammes,
                        currentPlayingChannel = currentPlayingChannel,
                        onPlayChannel = { ch -> viewModel.playIptvChannel(ch) },
                        onPlayNextChannel = { list -> viewModel.playNextIptvChannel(list) },
                        onPlayPreviousChannel = { list -> viewModel.playPreviousIptvChannel(list) },
                        onRecallPreviousChannel = { viewModel.recallPreviousIptvChannel() },
                        glowIntensitySetting = glowIntensitySetting,
                        showDebugPanel = showDebugPanel,
                        onToggleDebug = { viewModel.toggleDebugPanel() },
                        onSelectTab = { tab -> viewModel.setActiveIptvTab(tab) },
                        onLayoutChanged = { left, top, width, height ->
                            viewModel.updateScreenLayout(left, top, width, height, screenLayout.dimAlpha)
                        },
                        isAnimationEnabled = isAnimationEnabled,
                        onToggleAnimation = { enabled -> viewModel.setAnimationEnabled(enabled) },
                        onSelectTheme = { id -> viewModel.selectTheme(id) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. Overlaid Interactive Console and Settings Panels
                    if (isExpandedLayout) {
                        // Right-side floating sidebar layout for tablet/foldables
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showDebugPanel,
                                enter = androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.fadeOut(),
                                modifier = Modifier
                                    .width(380.dp)
                                    .fillMaxHeight()
                            ) {
                                if (isIptvModeActive) {
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                    ) {
                                        IptvDashboard(
                                            viewModel = viewModel,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    InteractiveConsolePanel(
                                        viewModel = viewModel,
                                        activeThemePreset = activeThemePreset,
                                        screenLayout = screenLayout,
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
                    } else {
                        // Bottom drawer panel overlay for standard device portrait
                        val activeHeightFraction = if (isIptvModeActive) 0.65f else 0.48f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showDebugPanel,
                                enter = slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(activeHeightFraction)
                                    .padding(16.dp)
                            ) {
                                if (isIptvModeActive) {
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                    ) {
                                        IptvDashboard(
                                            viewModel = viewModel,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    InteractiveConsolePanel(
                                        viewModel = viewModel,
                                        activeThemePreset = activeThemePreset,
                                        screenLayout = screenLayout,
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
        }

        // 3. EPG Show Reminder Heads-up prompt overlay dialog
        if (activeReminderAlert != null) {
            val alertInfo = activeReminderAlert
            if (alertInfo != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissReminderAlert() },
                    containerColor = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .border(1.5.dp, IndigoPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("in_app_reminder_alert_dialog"),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Active Reminder",
                                tint = IndigoBadgeText,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Show Reminder",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = alertInfo.programTitle,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Is starting now on channel: ${alertInfo.channelName}",
                                color = TextSilver,
                                fontSize = 11.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.tuneToReminderChannel(alertInfo)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tune In", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    viewModel.setActiveIptvTab(1) // Tab 1 is TV Guide
                                    if (!showDebugPanel) {
                                        viewModel.toggleDebugPanel()
                                    }
                                    viewModel.dismissReminderAlert()
                                }
                            ) {
                                Text("Open Guide", color = IndigoBadgeText, fontSize = 11.5.sp)
                            }
                            TextButton(
                                onClick = { viewModel.dismissReminderAlert() }
                            ) {
                                Text("Dismiss", color = TextSilver, fontSize = 11.5.sp)
                            }
                        }
                    }
                )
            }
        }

        val showEpgSorter by viewModel.showEpgSorter.collectAsState()
        if (showEpgSorter) {
            EPGSorterScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.showEpgSorter(false) },
                modifier = Modifier.fillMaxSize()
            )
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
    isFoldableActive: Boolean,
    activeThemeName: String,
    isIptvActive: Boolean,
    onToggleIptv: (Boolean) -> Unit
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
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isIptvActive) IndigoPrimary else IndigoSecondary)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIptvActive) Icons.Default.Tv else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isIptvActive) "Cinema IPTV Live" else "Cinema Player",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                    Text(
                        text = if (isIptvActive) "Built-in IPTV Engine" else "Theme: $activeThemeName",
                        color = Color.LightGray.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        },
        actions = {
            // IPTV Live Client Quick Toggle Selector
            IconButton(
                onClick = { 
                    onToggleIptv(!isIptvActive)
                    // If turning on, ensure panel is visible to browse streams
                    if (!isIptvActive && !showDebugPanel) {
                        onToggleDebug()
                    }
                },
                modifier = Modifier.testTag("iptv_mode_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = "Toggle IPTV Mode",
                    tint = if (isIptvActive) IndigoPrimary else Color.LightGray,
                    modifier = Modifier.size(22.dp)
                )
            }

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
                    imageVector = if (showDebugPanel) Icons.Default.Tune else Icons.Default.Info,
                    contentDescription = "Toggle Control Panel",
                    tint = if (showDebugPanel) IndigoPrimary else Color.LightGray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}

/**
 * Modern Projection Screen Frame designed with fine cinematic bevels and drop glow.
 * Positions and resizes the ExoPlayer dynamically inside a drawn ambient backdrop based on percentage settings.
 */
@Composable
fun CinemaTheaterLayout(
    viewModel: MainViewModel,
    themePreset: ThemePreset,
    screenLayout: ScreenLayoutSettings,
    playableUri: String?,
    errorMessage: String?,
    headers: Map<String, String>,
    onPlayTestVideo: () -> Unit,
    onClearPlaySource: () -> Unit,
    onPlaybackError: (String) -> Unit,
    isEditMode: Boolean = false,
    activeAspectRatioId: String = "free",
    activeResizeMode: String = "adaptive",
    onSelectResizeMode: (String) -> Unit = {},
    isSettingsLoaded: Boolean = true,
    isIptvActive: Boolean = false,
    channels: List<IptvChannel> = emptyList(),
    epgList: List<EpgProgramme> = emptyList(),
    currentPlayingChannel: IptvChannel? = null,
    onPlayChannel: (IptvChannel) -> Unit = {},
    onPlayNextChannel: (List<IptvChannel>) -> Unit = {},
    onPlayPreviousChannel: (List<IptvChannel>) -> Unit = {},
    onRecallPreviousChannel: () -> Unit = {},
    glowIntensitySetting: AmbientGlowSetting = AmbientGlowSetting.OFF,
    showDebugPanel: Boolean = false,
    onToggleDebug: () -> Unit = {},
    onSelectTab: (Int) -> Unit = {},
    onLayoutChanged: (left: Float, top: Float, width: Float, height: Float) -> Unit = { _, _, _, _ -> },
    isEpgGuideMode: Boolean = false,
    isAnimationEnabled: Boolean = true,
    onToggleAnimation: (Boolean) -> Unit = {},
    onSelectTheme: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 1. Live state tracking the detected aspect ratio of the active stream
    var detectedRatio by remember { mutableStateOf(1.7777778f) } // default 16:9
    var activePlayer by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }

    // Ambient aspect ratio fallback when empty or on startup
    val presetRatio = AspectRatioPreset.getById(activeAspectRatioId).ratio
    val defaultRatioValue = presetRatio ?: 1.7777778f

    // Reactively reset detectedRatio to default when playable source state changes
    LaunchedEffect(playableUri, activeAspectRatioId) {
        if (playableUri.isNullOrBlank()) {
            detectedRatio = defaultRatioValue
        } else if (presetRatio != null) {
            detectedRatio = presetRatio
        }
    }

    var canvasWidthPx by remember { mutableStateOf(1f) }
    var canvasHeightPx by remember { mutableStateOf(1f) }

    // Gentle slow pulse for ambient backlight glow (3.5s cycle)
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 3500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "ambient_glow_pulse"
    )

    // Slow organic color hue shifting for ambient mode (12s cycle)
    val shiftAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 12000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "ambient_hue_shift"
    )

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Transparent)
            .onGloballyPositioned { coordinates ->
                canvasWidthPx = coordinates.size.width.toFloat().coerceAtLeast(1f)
                canvasHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
            },
        contentAlignment = Alignment.Center
    ) {
        val containerWidthVal = maxWidth.value
        val containerHeightVal = maxHeight.value
        val containerAreaVal = containerWidthVal * containerHeightVal

        // 2. Localized backdrop dim overlay based on settings
        if (!isEpgGuideMode && screenLayout.dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = screenLayout.dimAlpha))
            )
        }

        // 3. Compute the adaptive player dimensions based on target area fraction (~0.96f)
        val isMini = !isEpgGuideMode && isIptvActive && playableUri != null && showDebugPanel

        val aspectR = detectedRatio.coerceIn(0.3f, 3.5f) // sensible min/max bounds
        val miniWidthDp = if (maxWidth >= 600.dp) 240.dp else 180.dp
        val miniHeightDp = miniWidthDp / aspectR

        var calculatedWidth = 0f
        var calculatedHeight = 0f

        if (isEpgGuideMode) {
            calculatedWidth = containerWidthVal
            calculatedHeight = containerHeightVal
        } else if (isMini) {
            calculatedWidth = miniWidthDp.value
            calculatedHeight = miniHeightDp.value
        } else {
            val targetAreaFraction = 0.96f
            val targetArea = targetAreaFraction * containerAreaVal
            val heightInit = kotlin.math.sqrt(targetArea / aspectR)
            val widthInit = heightInit * aspectR

            calculatedWidth = widthInit
            calculatedHeight = heightInit

            // Clamp to always fit perfectly within parent container constraints without distortion
            if (calculatedWidth > containerWidthVal) {
                calculatedWidth = containerWidthVal
                calculatedHeight = containerWidthVal / aspectR
            }
            if (calculatedHeight > containerHeightVal) {
                calculatedHeight = containerHeightVal
                calculatedWidth = containerHeightVal * aspectR
            }

            // Apply a safe minimum bound to prevent potential collapsing layouts
            calculatedWidth = calculatedWidth.coerceAtLeast(60f)
            calculatedHeight = calculatedHeight.coerceAtLeast(60f)
        }

        val playerWidth = calculatedWidth.dp
        val playerHeight = calculatedHeight.dp

        // Check if layout is customized. If coordinates match the theme default, we treat it as centered.
        val isCustomized = (screenLayout.left != themePreset.defaultLeft || screenLayout.top != themePreset.defaultTop)

        val maxLeftFrac = ((containerWidthVal - calculatedWidth) / containerWidthVal).coerceAtLeast(0f)
        val maxTopFrac = ((containerHeightVal - calculatedHeight) / containerHeightVal).coerceAtLeast(0f)

        // Lightweight dragging offset tracking separates high-frequency offset writes from persistent storage saves
        var isDragging by remember { mutableStateOf(false) }
        var activeLeftFraction by remember { mutableStateOf(0f) }
        var activeTopFraction by remember { mutableStateOf(0f) }

        // Snapping mini player setup:
        var miniPlayerCorner by remember { mutableStateOf(0) } // 0: Top-Right, 1: Bottom-Right, 2: Bottom-Left, 3: Top-Left
        var miniDragOffsetX by remember { mutableStateOf(0f) }
        var miniDragOffsetY by remember { mutableStateOf(0f) }
        var isMiniDragging by remember { mutableStateOf(false) }

        val density = LocalDensity.current
        val miniWidthPx = with(density) { miniWidthDp.toPx() }
        val miniHeightPx = with(density) { miniHeightDp.toPx() }
        val edgePaddingPx = with(density) { 16.dp.toPx() }
        val topPaddingPx = with(density) { 72.dp.toPx() } // offset from status/title bar
        val bottomPaddingPx = with(density) { 16.dp.toPx() }

        val targetCornerLeftFrac: Float
        val targetCornerTopFrac: Float

        if (isMini) {
            when (miniPlayerCorner) {
                0 -> { // Top-Right
                    targetCornerLeftFrac = (canvasWidthPx - miniWidthPx - edgePaddingPx) / canvasWidthPx
                    targetCornerTopFrac = topPaddingPx / canvasHeightPx
                }
                1 -> { // Bottom-Right
                    targetCornerLeftFrac = (canvasWidthPx - miniWidthPx - edgePaddingPx) / canvasWidthPx
                    targetCornerTopFrac = (canvasHeightPx - miniHeightPx - bottomPaddingPx) / canvasHeightPx
                }
                2 -> { // Bottom-Left
                    targetCornerLeftFrac = edgePaddingPx / canvasWidthPx
                    targetCornerTopFrac = (canvasHeightPx - miniHeightPx - bottomPaddingPx) / canvasHeightPx
                }
                else -> { // Top-Left
                    targetCornerLeftFrac = edgePaddingPx / canvasWidthPx
                    targetCornerTopFrac = topPaddingPx / canvasHeightPx
                }
            }
        } else {
            targetCornerLeftFrac = 0f
            targetCornerTopFrac = 0f
        }

        // Proactively synchronize local dragging states when view layout modifications or screen dimensions update
        LaunchedEffect(screenLayout.left, screenLayout.top, containerWidthVal, containerHeightVal, calculatedWidth, calculatedHeight, isDragging, isMini) {
            if (!isDragging && !isMini) {
                activeLeftFraction = if (isCustomized) {
                    screenLayout.left.coerceIn(0f, maxLeftFrac)
                } else {
                    val defaultLeft = (containerWidthVal - calculatedWidth) / 2f
                    (defaultLeft / containerWidthVal).coerceIn(0f, maxLeftFrac)
                }

                activeTopFraction = if (isCustomized) {
                    screenLayout.top.coerceIn(0f, maxTopFrac)
                } else {
                    val defaultTop = (containerHeightVal - calculatedHeight) / 2f
                    (defaultTop / containerHeightVal).coerceIn(0f, maxTopFrac)
                }
            }
        }

        // Compute fractions for offset
        val finalLeftFraction = if (isEpgGuideMode) {
            0f
        } else if (isMini) {
            if (isMiniDragging) {
                ((targetCornerLeftFrac * canvasWidthPx + miniDragOffsetX).coerceIn(0f, canvasWidthPx - miniWidthPx) / canvasWidthPx)
            } else {
                targetCornerLeftFrac.coerceIn(0f, (canvasWidthPx - miniWidthPx) / canvasWidthPx)
            }
        } else {
            activeLeftFraction
        }

        val finalTopFraction = if (isEpgGuideMode) {
            0f
        } else if (isMini) {
            if (isMiniDragging) {
                ((targetCornerTopFrac * canvasHeightPx + miniDragOffsetY).coerceIn(0f, canvasHeightPx - miniHeightPx) / canvasHeightPx)
            } else {
                targetCornerTopFrac.coerceIn(0f, (canvasHeightPx - miniHeightPx) / canvasHeightPx)
            }
        } else {
            activeTopFraction
        }

        val cornerShape = RoundedCornerShape(themePreset.cornerRadiusDp.dp)

        // 4. Draggable drag modification (smooth relative tracking and bounds clamping)
        val dragModifier = if (isEditMode && !isEpgGuideMode) {
            Modifier.pointerInput(containerWidthVal, containerHeightVal, calculatedWidth, calculatedHeight) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        onLayoutChanged(
                            activeLeftFraction,
                            activeTopFraction,
                            screenLayout.width,
                            screenLayout.height
                        )
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val canvasW = canvasWidthPx
                        val canvasH = canvasHeightPx
                        if (canvasW > 10f && canvasH > 10f) {
                            val dLeft = dragAmount.x / canvasW
                            val dTop = dragAmount.y / canvasH

                            val newLeftFrac = activeLeftFraction + dLeft
                            val newTopFrac = activeTopFraction + dTop

                            activeLeftFraction = newLeftFrac.coerceIn(0f, maxLeftFrac)
                            activeTopFraction = newTopFrac.coerceIn(0f, maxTopFrac)
                        }
                    }
                )
            }
        } else {
            Modifier
        }

        val miniDragModifier = if (isMini && !isEpgGuideMode) {
            Modifier.pointerInput(canvasWidthPx, canvasHeightPx, miniWidthPx, miniHeightPx) {
                detectDragGestures(
                    onDragStart = {
                        isMiniDragging = true
                        miniDragOffsetX = 0f
                        miniDragOffsetY = 0f
                    },
                    onDragEnd = {
                        isMiniDragging = false
                        val finalLeftPx = targetCornerLeftFrac * canvasWidthPx + miniDragOffsetX
                        val finalTopPx = targetCornerTopFrac * canvasHeightPx + miniDragOffsetY

                        val centerPX = finalLeftPx + miniWidthPx / 2f
                        val centerPY = finalTopPx + miniHeightPx / 2f
                        val snapToRight = centerPX > canvasWidthPx / 2f
                        val snapToBottom = centerPY > canvasHeightPx / 2f

                        miniPlayerCorner = when {
                            !snapToRight && !snapToBottom -> 3 // Top-Left
                            snapToRight && !snapToBottom -> 0  // Top-Right
                            !snapToRight && snapToBottom -> 2  // Bottom-Left
                            else -> 1                          // Bottom-Right
                        }
                        miniDragOffsetX = 0f
                        miniDragOffsetY = 0f
                    },
                    onDragCancel = {
                        isMiniDragging = false
                        miniDragOffsetX = 0f
                        miniDragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        miniDragOffsetX += dragAmount.x
                        miniDragOffsetY += dragAmount.y
                    }
                )
            }
        } else {
            Modifier
        }

        val activeDragModifier = if (isMini) miniDragModifier else dragModifier

        // 5. Centered/Positioned Player container frame matching chosen theme visual assets
        Box(
            modifier = Modifier
                .zIndex(if (isMini) 10f else 1f)
                .offset {
                    IntOffset(
                        x = (finalLeftFraction * canvasWidthPx).roundToInt(),
                        y = (finalTopFraction * canvasHeightPx).roundToInt()
                    )
                }
                .size(width = playerWidth, height = playerHeight)
                .testTag("video_screen_container")
                .drawBehind {
                    if (glowIntensitySetting != AmbientGlowSetting.OFF) {
                        val isMedium = glowIntensitySetting == AmbientGlowSetting.MEDIUM
                        val baseGlow = themePreset.glowRadiusDp.dp.toPx()
                        val glowRadius = if (isMedium) baseGlow * 1.6f else baseGlow

                        val baseColor = themePreset.glowColor
                        val accentColor = themePreset.primaryColor
                        val blendedColor = if (isMedium) {
                            val blendFactor = (pulseAnimation - 0.88f) / (1.12f - 0.88f)
                            androidx.compose.ui.graphics.lerp(baseColor, accentColor, blendFactor.coerceIn(0f, 1f))
                        } else {
                            baseColor
                        }

                        val shadowFactor = themePreset.shadowIntensity * (if (isMedium) 1.15f else 0.82f) * pulseAnimation

                        if (glowRadius > 0f) {
                            val passes = if (isMedium) 5 else 4
                            for (i in 1..passes) {
                                val scale = 1.0f + (i * 0.016f * (glowRadius / 20f)) * (if (isMedium) 1.2f else 1.0f)
                                val alphaFactor = (passes - i + 1).toFloat() / passes
                                drawRoundRect(
                                    color = blendedColor.copy(alpha = blendedColor.alpha * alphaFactor * shadowFactor * 0.55f),
                                    topLeft = androidx.compose.ui.geometry.Offset(
                                        -(size.width * (scale - 1f) / 2f),
                                        -(size.height * (scale - 1f) / 2f)
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        size.width * scale,
                                        size.height * scale
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                        themePreset.cornerRadiusDp.dp.toPx() * scale,
                                        themePreset.cornerRadiusDp.dp.toPx() * scale
                                    )
                                )
                            }
                        }
                    }
                }
                .clip(cornerShape)
                .background(themePreset.frameColor)
                .then(
                    if (isEditMode) {
                        Modifier.border(
                            width = 2.5.dp,
                            color = themePreset.primaryColor,
                            shape = cornerShape
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = cornerShape
                        )
                    }
                )
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    
                    when (themePreset.id.lowercase()) {
                        "cosy_cabin" -> {
                            val grainColor = Color(0xFF5C2D16).copy(alpha = 0.35f)
                            for (i in listOf(4f, 8f, 12f)) {
                                drawRoundRect(
                                    color = grainColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(i, i),
                                    size = androidx.compose.ui.geometry.Size(width - (i * 2), height - (i * 2)),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                        themePreset.cornerRadiusDp.dp.toPx() - i,
                                        themePreset.cornerRadiusDp.dp.toPx() - i
                                    ),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                                )
                            }
                        }
                        "sports_arena" -> {
                            val neonLineColor = Color(0xFF10B981).copy(alpha = 0.35f)
                            drawRoundRect(
                                color = neonLineColor,
                                topLeft = androidx.compose.ui.geometry.Offset(3f, 3f),
                                size = androidx.compose.ui.geometry.Size(width - 6f, height - 6f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                    themePreset.cornerRadiusDp.dp.toPx() - 3f,
                                    themePreset.cornerRadiusDp.dp.toPx() - 3f
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                            )
                        }
                        "cinema" -> {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.04f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.35f)
                                     )
                                )
                            )
                        }
                    }
                }
                .then(activeDragModifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(themePreset.frameThicknessDp.dp)
                    .clip(RoundedCornerShape((themePreset.cornerRadiusDp - 3).coerceAtLeast(2).dp))
                    .background(Color.Black)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape((themePreset.cornerRadiusDp - 3).coerceAtLeast(2).dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!playableUri.isNullOrBlank()) {
                    VideoPlayerView(
                        videoUrl = playableUri,
                        onPlaybackError = onPlaybackError,
                        modifier = Modifier
                            .fillMaxSize(),
                        headers = headers,
                        displayMode = activeResizeMode,
                        useController = !isIptvActive,
                        onVideoAspectRatioDetected = { ratio ->
                            detectedRatio = ratio
                        },
                        onPlayerInitialized = { activePlayer = it }
                    )

                    // Overlay / Tap handling inside player for IPTV mode
                    if (isIptvActive && !isMini) {
                        var isOverlayVisible by remember { mutableStateOf(false) }
                        var userInteractionTrigger by remember { mutableStateOf(0) }

                        LaunchedEffect(isOverlayVisible, userInteractionTrigger) {
                            if (isOverlayVisible) {
                                kotlinx.coroutines.delay(5000)
                                isOverlayVisible = false
                            }
                        }

                        // Invisible clickable region over player to capture taps and toggle IPTV overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) {
                                    isOverlayVisible = !isOverlayVisible
                                    userInteractionTrigger++
                                }
                        )

                        // If overlay visible, render Info Overlay & Channel Controls
                        AnimatedVisibility(
                            visible = isOverlayVisible,
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                        ) {
                            LiveInfoOverlay(
                                currentPlayingChannel = currentPlayingChannel,
                                channels = channels,
                                epgList = epgList,
                                onPlayChannel = onPlayChannel,
                                onPlayNextChannel = onPlayNextChannel,
                                onPlayPreviousChannel = onPlayPreviousChannel,
                                onRecallPreviousChannel = onRecallPreviousChannel,
                                onToggleDebug = onToggleDebug,
                                onSelectTab = onSelectTab,
                                showDebugPanel = showDebugPanel,
                                onDismiss = { isOverlayVisible = false },
                                onInteraction = { userInteractionTrigger++ }
                            )
                        }
                    }

                    if (isMini) {
                        var isMiniControlsVisible by remember { mutableStateOf(false) }

                        // Clicking mini player toggles corner overlay buttons
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) {
                                    isMiniControlsVisible = !isMiniControlsVisible
                                }
                        )

                        if (isMiniControlsVisible) {
                            MiniPlayerControls(
                                miniPlayerCorner = miniPlayerCorner,
                                onCycleCorner = { miniPlayerCorner = (miniPlayerCorner + 1) % 4 },
                                onRestoreFullScreen = {
                                    onToggleDebug()
                                },
                                onStopStream = onClearPlaySource,
                                onDismiss = { isMiniControlsVisible = false }
                            )
                        }
                    }

                    if (!isEditMode && !isMini) {
                        var isPlayerSettingsDialogVisible by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Settings Cog Button
                                Box {
                                    IconButton(
                                        onClick = { isPlayerSettingsDialogVisible = true },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = Color.Black.copy(alpha = 0.65f)
                                        ),
                                        modifier = Modifier.size(32.dp).testTag("settings_cog")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Playback display settings",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    if (isPlayerSettingsDialogVisible) {
                                        AlertDialog(
                                            onDismissRequest = { isPlayerSettingsDialogVisible = false },
                                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                                            modifier = Modifier
                                                .fillMaxWidth(0.9f)
                                                .widthIn(max = 500.dp)
                                                .padding(16.dp)
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                                .testTag("player_settings_dialog"),
                                            containerColor = ObsidianSurface,
                                            shape = RoundedCornerShape(16.dp),
                                            title = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = null,
                                                        tint = themePreset.primaryColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = "Player & Environment Settings",
                                                        color = Color.White,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            },
                                            text = {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    // SECTION 1: Resizing Modes
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(
                                                            text = "1. ASPECT RESIZE MODE",
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp
                                                        )
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            listOf(
                                                                "adaptive" to "Adaptive",
                                                                "fit" to "Fit",
                                                                "zoom" to "Zoom",
                                                                "fill" to "Stretch"
                                                            ).forEach { (mode, title) ->
                                                                val isSelected = activeResizeMode == mode
                                                                Button(
                                                                    onClick = { onSelectResizeMode(mode) },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (isSelected) themePreset.primaryColor else Color.White.copy(alpha = 0.05f),
                                                                        contentColor = if (isSelected) Color.White else TextSilver
                                                                    ),
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(36.dp)
                                                                ) {
                                                                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // SECTION 2: Subtitle Settings
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(
                                                            text = "2. SUBTITLES & CAPTIONS",
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp
                                                        )

                                                        if (activePlayer == null) {
                                                            Card(
                                                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text(
                                                                    text = "Player initializing...",
                                                                    color = TextMuted,
                                                                    fontSize = 12.sp,
                                                                    modifier = Modifier.padding(10.dp)
                                                                )
                                                            }
                                                        } else {
                                                            // Extract available tracks
                                                            val textTrackOptions = remember(activePlayer, activePlayer?.currentTracks) {
                                                                val list = mutableListOf<SubtitleTrackInfo>()
                                                                activePlayer?.currentTracks?.groups?.forEach { group ->
                                                                    if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                                                                        for (i in 0 until group.length) {
                                                                            val isSelected = group.isTrackSelected(i)
                                                                            val format = group.getTrackFormat(i)
                                                                            val name = format.label ?: format.language?.uppercase() ?: "Track ${list.size + 1}"
                                                                            list.add(SubtitleTrackInfo(group = group, trackIndex = i, label = name, isSelected = isSelected))
                                                                        }
                                                                    }
                                                                }
                                                                list
                                                            }

                                                            // Off & Auto default modes
                                                            val isSubtitlesDisabled = activePlayer?.trackSelectionParameters?.disabledTrackTypes?.contains(androidx.media3.common.C.TRACK_TYPE_TEXT) == true
                                                            val hasNoOverrides = activePlayer?.trackSelectionParameters?.overrides?.keys?.any { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT } == false
                                                            val isAutoSelected = !isSubtitlesDisabled && hasNoOverrides

                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                // Subtitles OFF Button
                                                                Button(
                                                                    onClick = {
                                                                        activePlayer?.trackSelectionParameters = activePlayer!!.trackSelectionParameters
                                                                            .buildUpon()
                                                                            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                                                                            .build()
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (isSubtitlesDisabled) themePreset.primaryColor else Color.White.copy(alpha = 0.05f),
                                                                        contentColor = Color.White
                                                                    ),
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                                    modifier = Modifier.weight(1f).height(36.dp)
                                                                ) {
                                                                    Text("No Subtitles (Off)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                }

                                                                // Subtitles Auto Button
                                                                Button(
                                                                    onClick = {
                                                                        activePlayer?.trackSelectionParameters = activePlayer!!.trackSelectionParameters
                                                                            .buildUpon()
                                                                            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                                                            .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                                                                            .build()
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (isAutoSelected) themePreset.primaryColor else Color.White.copy(alpha = 0.05f),
                                                                        contentColor = Color.White
                                                                    ),
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                                    modifier = Modifier.weight(1f).height(36.dp)
                                                                ) {
                                                                    Text("Auto (Default)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }

                                                            if (textTrackOptions.isEmpty()) {
                                                                Text(
                                                                    text = "No subtitle tracks available for this stream",
                                                                    color = TextMuted,
                                                                    fontSize = 11.sp,
                                                                    modifier = Modifier.padding(top = 4.dp).testTag("no_subtitles_text")
                                                                )
                                                            } else {
                                                                Text(
                                                                    text = "Detected Tracks:",
                                                                    color = Color.White.copy(alpha = 0.7f),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    modifier = Modifier.padding(top = 4.dp)
                                                                )
                                                                // Scrollable list of actual embedded tracks
                                                                androidx.compose.foundation.lazy.LazyRow(
                                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    items(textTrackOptions.size) { index ->
                                                                        val track = textTrackOptions[index]
                                                                        val selected = !isSubtitlesDisabled && track.isSelected
                                                                        Button(
                                                                            onClick = {
                                                                                activePlayer?.trackSelectionParameters = activePlayer!!.trackSelectionParameters
                                                                                    .buildUpon()
                                                                                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                                                                    .setOverrideForType(
                                                                                        androidx.media3.common.TrackSelectionOverride(
                                                                                            track.group.mediaTrackGroup,
                                                                                            track.trackIndex
                                                                                        )
                                                                                    )
                                                                                    .build()
                                                                            },
                                                                            colors = ButtonDefaults.buttonColors(
                                                                                containerColor = if (selected) themePreset.primaryColor else Color.White.copy(alpha = 0.05f),
                                                                                contentColor = Color.White
                                                                            ),
                                                                            shape = RoundedCornerShape(8.dp),
                                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                                            modifier = Modifier.height(34.dp)
                                                                        ) {
                                                                            Text("✏️ ${track.label}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // SECTION 3: Themes & Animation
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(
                                                            text = "3. ENVIRONMENT PRESETS & ANIMATIONS",
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp
                                                        )

                                                        // Compact Horizontal Theme Bar (Scrollable)
                                                        androidx.compose.foundation.lazy.LazyRow(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            contentPadding = PaddingValues(vertical = 4.dp)
                                                        ) {
                                                            val allThemes = ThemePresets.all
                                                            items(allThemes.size) { index ->
                                                                val preset = allThemes[index]
                                                                val isSelected = themePreset.id == preset.id
                                                                Button(
                                                                    onClick = { onSelectTheme(preset.id) },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (isSelected) themePreset.primaryColor else Color.White.copy(alpha = 0.05f),
                                                                        contentColor = Color.White
                                                                    ),
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    modifier = Modifier.height(36.dp)
                                                                ) {
                                                                    Text(
                                                                        text = if (preset.isAnimated) "✨ ${preset.name}" else preset.name,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(2.dp))

                                                        // Animation Switch Toggle
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.White.copy(alpha = 0.02f))
                                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    "Live Animated Backdrops",
                                                                    color = Color.White,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                Text(
                                                                    "Conserves battery and GPU when off.",
                                                                    color = TextMuted,
                                                                    fontSize = 10.sp
                                                                )
                                                            }
                                                            Switch(
                                                                checked = isAnimationEnabled,
                                                                onCheckedChange = onToggleAnimation,
                                                                colors = SwitchDefaults.colors(
                                                                    checkedThumbColor = Color.White,
                                                                    checkedTrackColor = themePreset.primaryColor,
                                                                    uncheckedThumbColor = TextMuted,
                                                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                                                ),
                                                                modifier = Modifier.testTag("animated_backdrop_switch")
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                TextButton(onClick = { isPlayerSettingsDialogVisible = false }) {
                                                    Text("Dismiss", color = themePreset.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }
                                        )
                                    }
                                }

                                // Close Button
                                IconButton(
                                    onClick = onClearPlaySource,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.65f)
                                    ),
                                    modifier = Modifier.size(32.dp).testTag("close_player_source")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop streams",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    TheaterFallbackCurtain(
                        errorMessage = errorMessage,
                        onPlayTestVideo = onPlayTestVideo,
                        primaryColor = themePreset.primaryColor
                    )
                }

                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(themePreset.primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "DRAG TO POSITION",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.6.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Ambient information overlay caption
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "${themePreset.name.uppercase()} ENVIRONMENT",
                color = Color.White.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            )
        }
    }
}

@Composable
fun LiveInfoOverlay(
    currentPlayingChannel: IptvChannel?,
    channels: List<IptvChannel>,
    epgList: List<EpgProgramme>,
    onPlayChannel: (IptvChannel) -> Unit,
    onPlayNextChannel: (List<IptvChannel>) -> Unit,
    onPlayPreviousChannel: (List<IptvChannel>) -> Unit,
    onRecallPreviousChannel: () -> Unit,
    onToggleDebug: () -> Unit,
    onSelectTab: (Int) -> Unit,
    showDebugPanel: Boolean,
    onDismiss: () -> Unit,
    onInteraction: () -> Unit
) {
    if (currentPlayingChannel == null) return

    val (currentProg, nextProg) = getNowAndNextForChannel(currentPlayingChannel, epgList)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { onInteraction() }, // Intercept child click to avoid dismiss
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F1115).copy(alpha = 0.92f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top header: Channel info + optional logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IndigoPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp),
                            border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = currentPlayingChannel.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentPlayingChannel.categoryId.takeIf { it.isNotBlank() } ?: "All Channels",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // TV Guide Quick Access & Close Info button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onSelectTab(1) // Tab 1 is TV Guide tab
                                if (!showDebugPanel) {
                                    onToggleDebug() // Open EPG side/bottom drawer panel
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoPrimary.copy(alpha = 0.2f),
                                contentColor = IndigoPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp).testTag("guide_button_overlay")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TV Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close overlay",
                                tint = Color.LightGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Now playing segment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                contentColor = Color(0xFFEF4444)
                            ) {
                                Text(
                                    text = "NOW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = currentProg.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!currentProg.description.isNullOrBlank()) {
                            Text(
                                text = currentProg.description ?: "",
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Times duration
                    Text(
                        text = "${formatEpgTime(currentProg.startMs)} - ${formatEpgTime(currentProg.endMs)}",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Progress Bar for Now Playing programme
                val nowVal = System.currentTimeMillis()
                val totalDuration = (currentProg.endMs - currentProg.startMs).toFloat()
                val elapsed = (nowVal - currentProg.startMs).toFloat()
                val progressFraction = if (totalDuration > 0f) (elapsed / totalDuration).coerceIn(0f, 1f) else 0f

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = IndigoPrimary,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Up Next segment
                if (nextProg != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                contentColor = Color.LightGray
                            ) {
                                Text(
                                    text = "NEXT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = nextProg.title,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = formatEpgTime(nextProg.startMs),
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Channel Control buttons (Surfing)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onPlayPreviousChannel(channels)
                            onInteraction()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("prev_channel_overlay_button"),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.04f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous channel",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            onRecallPreviousChannel()
                            onInteraction()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("recall_channel_overlay_button"),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = IndigoPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recall last channel",
                            tint = IndigoPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "CHANNEL SURFING",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onPlayNextChannel(channels)
                            onInteraction()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("next_channel_overlay_button"),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.04f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next channel",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerControls(
    miniPlayerCorner: Int,
    onCycleCorner: () -> Unit,
    onRestoreFullScreen: () -> Unit,
    onStopStream: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Restore Button
            IconButton(
                onClick = {
                    onRestoreFullScreen()
                    onDismiss()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = IndigoPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("mini_player_restore")
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Restore Full Screen",
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cycle Corner / Position Button
                IconButton(
                    onClick = onCycleCorner,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("mini_player_cycle_corner")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Snap to next corner",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Stop Stream / Close Button
                IconButton(
                    onClick = {
                        onStopStream()
                        onDismiss()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("mini_player_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Player",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun formatEpgTime(timeMs: Long): String {
    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timeMs))
}

fun getNowAndNextForChannel(
    channel: IptvChannel,
    epgList: List<EpgProgramme>
): Pair<EpgProgramme, EpgProgramme?> {
    val now = System.currentTimeMillis()
    val channelEpgs = epgList.filter { it.channelId == channel.id }.sortedBy { it.startMs }
    
    val current = channelEpgs.find { now in it.startMs..it.endMs }
        ?: EpgProgramme(
            channelId = channel.id,
            title = "Live Broadcast",
            startMs = now - 3600000L,
            endMs = now + 4 * 3600000L,
            description = "Click the 'TV Guide' button to browse detailed programming schedules."
        )

    val next = channelEpgs.find { it.startMs >= current.endMs }
        ?: channelEpgs.find { it.startMs > now }

    return Pair(current, next)
}

private fun checkRatioResize(
    newW: Float,
    newH: Float,
    left: Float,
    top: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    ratio: Float?
): Pair<Float, Float> {
    if (ratio == null) {
        return Pair(
            newW.coerceIn(0.10f, 1.0f - left),
            newH.coerceIn(0.10f, 1.0f - top)
        )
    } else {
        var w = newW.coerceIn(0.10f, 1.0f - left)
        var h = (w * canvasWidth) / (ratio * canvasHeight)

        if (h > 1.0f - top) {
            h = 1.0f - top
            w = (h * canvasHeight * ratio) / canvasWidth
        }
        if (h < 0.10f) {
            h = 0.10f
            w = (h * canvasHeight * ratio) / canvasWidth
        }
        if (w > 1.0f - left) {
            w = 1.0f - left
            h = (w * canvasWidth) / (ratio * canvasHeight)
        }
        return Pair(w, h)
    }
}

private fun checkRatioResizeTopLeft(
    newLeft: Float,
    newTop: Float,
    fixedRight: Float,
    fixedBottom: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    ratio: Float?
): ScreenLayoutSettings {
    if (ratio == null) {
        val clampLeft = newLeft.coerceIn(0f, fixedRight - 0.10f)
        val clampTop = newTop.coerceIn(0f, fixedBottom - 0.10f)
        return ScreenLayoutSettings(
            left = clampLeft,
            top = clampTop,
            width = fixedRight - clampLeft,
            height = fixedBottom - clampTop,
            dimAlpha = 0f,
            subtitleOffset = 0f
        )
    } else {
        val proposedLeft = newLeft.coerceIn(0f, fixedRight - 0.10f)
        val proposedW = fixedRight - proposedLeft
        var h = (proposedW * canvasWidth) / (ratio * canvasHeight)

        if (h > fixedBottom) {
            h = fixedBottom
        }
        if (h < 0.10f) {
            h = 0.10f
        }
        var w = (h * canvasHeight * ratio) / canvasWidth
        if (w > fixedRight) {
            w = fixedRight
            h = (w * canvasWidth) / (ratio * canvasHeight)
        }

        val actualLeft = fixedRight - w
        val actualTop = fixedBottom - h
        return ScreenLayoutSettings(
            left = actualLeft,
            top = actualTop,
            width = w,
            height = h,
            dimAlpha = 0f,
            subtitleOffset = 0f
        )
    }
}

private fun checkRatioResizeTopRight(
    newRight: Float,
    newTop: Float,
    fixedLeft: Float,
    fixedBottom: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    ratio: Float?
): ScreenLayoutSettings {
    if (ratio == null) {
        val clampRight = newRight.coerceIn(fixedLeft + 0.10f, 1.0f)
        val clampTop = newTop.coerceIn(0f, fixedBottom - 0.10f)
        return ScreenLayoutSettings(
            left = fixedLeft,
            top = clampTop,
            width = clampRight - fixedLeft,
            height = fixedBottom - clampTop,
            dimAlpha = 0f,
            subtitleOffset = 0f
        )
    } else {
        val proposedRight = newRight.coerceIn(fixedLeft + 0.10f, 1.0f)
        val proposedW = proposedRight - fixedLeft
        var h = (proposedW * canvasWidth) / (ratio * canvasHeight)

        if (h > fixedBottom) {
            h = fixedBottom
        }
        if (h < 0.10f) {
            h = 0.10f
        }
        var w = (h * canvasHeight * ratio) / canvasWidth
        if (w > 1.0f - fixedLeft) {
            w = 1.0f - fixedLeft
            h = (w * canvasWidth) / (ratio * canvasHeight)
        }

        val actualTop = fixedBottom - h
        return ScreenLayoutSettings(
            left = fixedLeft,
            top = actualTop,
            width = w,
            height = h,
            dimAlpha = 0f,
            subtitleOffset = 0f
        )
    }
}

private fun checkRatioResizeBottomLeft(
    newLeft: Float,
    newBottom: Float,
    fixedRight: Float,
    fixedTop: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    ratio: Float?
): ScreenLayoutSettings {
    if (ratio == null) {
        val clampLeft = newLeft.coerceIn(0f, fixedRight - 0.10f)
        val clampBottom = newBottom.coerceIn(fixedTop + 0.10f, 1.0f)
        return ScreenLayoutSettings(
            left = clampLeft,
            top = fixedTop,
            width = fixedRight - clampLeft,
            height = clampBottom - fixedTop,
            dimAlpha = 0f,
            subtitleOffset = 0f
        )
    } else {
        val proposedLeft = newLeft.coerceIn(0f, fixedRight - 0.10f)
        val proposedW = fixedRight - proposedLeft
        var h = (proposedW * canvasWidth) / (ratio * canvasHeight)

        if (h > 1.0f - fixedTop) {
            h = 1.0f - fixedTop
        }
        if (h < 0.10f) {
            h = 0.10f
        }
        var w = (h * canvasHeight * ratio) / canvasWidth
        if (w > fixedRight) {
            w = fixedRight
            h = (w * canvasWidth) / (ratio * canvasHeight)
        }

        val actualLeft = fixedRight - w
        return ScreenLayoutSettings(
            left = actualLeft,
            top = fixedTop,
            width = w,
            height = h,
            dimAlpha = 0f,
            subtitleOffset = 0f
        )
    }
}

/**
 * Display corresponding beautiful empty states with customizable theme colors.
 */
@Composable
fun TheaterFallbackCurtain(
    errorMessage: String?,
    onPlayTestVideo: () -> Unit,
    primaryColor: Color = Color(0xFF6366F1)
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = errorMessage,
                color = TextSilver.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
                maxLines = 4
            )
        } else {
            Text(
                text = "No stream selected",
                color = TextSilver.copy(alpha = 0.75f),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Awaiting custom URI payload or external intent",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onPlayTestVideo,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = Color.White
            ),
            shape = CircleShape,
            modifier = Modifier
                .testTag("play_test_button")
                .height(38.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Test Playback URI",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.25.sp
                )
            )
        }
    }
}

/**
 * Enhanced console container displaying customizable sliders, themes picker, and diagnostics.
 */
@Composable
fun InteractiveConsolePanel(
    viewModel: MainViewModel,
    activeThemePreset: ThemePreset,
    screenLayout: ScreenLayoutSettings,
    parsedIntent: ParsedIntentInfo?,
    playableUri: String?,
    errorMessage: String?,
    onCopyLogs: () -> Unit,
    onClosePanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val isEditMode by viewModel.isEditMode.collectAsState()
    val activeAspectRatioId by viewModel.activeAspectRatioId.collectAsState()
    val customBackgroundUri by viewModel.customBgUri.collectAsState()
    val activeThemeId by viewModel.activeThemeId.collectAsState()

    var showThemeDesignerDialog by remember { mutableStateOf(false) }
    var designerBackdropUri by remember { mutableStateOf<String?>(null) }
    val designerPhotoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                designerBackdropUri = uri.toString()
            }
        }
    )

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.saveCustomBackgroundFromUri(uri)
            }
        }
    )

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ObsidianSurface,
            contentColor = TextSilver
        ),
        modifier = modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .testTag("intent_details_card")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Display Toggle Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black.copy(alpha = 0.3f),
                contentColor = IndigoPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = activeThemePreset.primaryColor
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Tuning & Style", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = TextMuted
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Debug Diagnostics", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = TextMuted
                )
            }

            // Body content depends on selected tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    // Tuning Tab Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section: Theme Presets
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AMBIENT VIEWING PRESETS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                )
                                Button(
                                    onClick = { showThemeDesignerDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp).testTag("trigger_theme_designer")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Theme", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            val themes = ThemePresets.all
                            val chunkedThemes = themes.chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                chunkedThemes.forEach { rowThemes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowThemes.forEach { preset ->
                                            val isSelected = activeThemeId == preset.id
                                            val isUserTheme = preset.id.startsWith("custom_")
                                            Card(
                                                onClick = { viewModel.selectTheme(preset.id) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(60.dp)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) preset.primaryColor else Color.White.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .testTag("theme_card_${preset.id}"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) preset.secondaryColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.02f)
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = when (preset.id) {
                                                                "cosy_cabin" -> Icons.Default.Weekend
                                                                "sports_arena" -> Icons.Default.EmojiEvents
                                                                "custom" -> Icons.Default.Image
                                                                "aurora" -> Icons.Default.Brush
                                                                "matrix" -> Icons.Default.Palette
                                                                "stardust" -> Icons.Default.AutoAwesome
                                                                else -> if (isUserTheme) Icons.Default.Palette else Icons.Default.Movie
                                                            },
                                                            contentDescription = null,
                                                            tint = if (isSelected) preset.primaryColor else Color.LightGray.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = if (preset.isAnimated) "✨ ${preset.name}" else preset.name,
                                                                color = if (isSelected) Color.White else Color.LightGray,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = if (isUserTheme) "My Custom Theme" else "Preset Theme",
                                                                color = Color.White.copy(alpha = 0.4f),
                                                                fontSize = 8.5.sp
                                                            )
                                                        }
                                                    }
                                                    if (isUserTheme) {
                                                        IconButton(
                                                            onClick = { viewModel.deleteCustomTheme(preset.id) },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Custom Theme",
                                                                tint = Color.LightGray.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (rowThemes.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            Text(
                                text = activeThemePreset.description,
                                color = TextMuted,
                                fontSize = 10.5.sp,
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                            )

                            if (activeThemeId.lowercase().startsWith("custom")) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom visual preview box
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!customBackgroundUri.isNullOrBlank()) {
                                            coil.compose.AsyncImage(
                                                model = customBackgroundUri,
                                                contentDescription = "Custom photo background thumbnail preview",
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "No image selected",
                                                tint = Color.White.copy(alpha = 0.25f),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    // Controls Column
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (!customBackgroundUri.isNullOrBlank()) "Custom Image Loaded" else "No Background Selected",
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (!customBackgroundUri.isNullOrBlank()) "Your custom image is set as the background backdrop." else "Pick an image from library to act as ambient projection wall.",
                                            color = TextMuted,
                                            fontSize = 9.5.sp,
                                            lineHeight = 11.5.sp
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Pick trigger button
                                            Button(
                                                onClick = {
                                                    photoPickerLauncher.launch(
                                                        androidx.activity.result.PickVisualMediaRequest(
                                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                                        )
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = activeThemePreset.primaryColor,
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                                modifier = Modifier.height(28.dp).testTag("select_custom_background_btn")
                                            ) {
                                                Text("Choose Image", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Clear trigger button
                                            if (!customBackgroundUri.isNullOrBlank()) {
                                                Button(
                                                    onClick = {
                                                        viewModel.setCustomBackground(null)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color.White.copy(alpha = 0.08f),
                                                        contentColor = Color.LightGray
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                                    modifier = Modifier.height(28.dp).testTag("remove_custom_background_btn")
                                                ) {
                                                    Text("Remove", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.04f))

                        // Section: Screen Customization & Aspect Ratio
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "SCREEN CUSTOMIZATION CONSOLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )

                            // Edit Mode Toggle Switch
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (isEditMode) activeThemePreset.primaryColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isEditMode) activeThemePreset.primaryColor.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = if (isEditMode) activeThemePreset.primaryColor else Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Interactive Edit Screen Mode",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Drag or resize the video player directly",
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isEditMode,
                                        onCheckedChange = { viewModel.setEditMode(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = activeThemePreset.primaryColor,
                                            uncheckedThumbColor = Color.LightGray,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.testTag("edit_mode_switch")
                                    )
                                }
                            }

                            // Aspect Ratio Presets Row
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ASPECT RATIO PRESET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.0.sp,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val ratioOptions = listOf(
                                    AspectRatioPreset.FREE,
                                    AspectRatioPreset.RATIO_16_9,
                                    AspectRatioPreset.RATIO_21_9,
                                    AspectRatioPreset.RATIO_4_3,
                                    AspectRatioPreset.RATIO_1_1
                                )
                                ratioOptions.forEach { preset ->
                                    val isRatioSelected = activeAspectRatioId == preset.id
                                    Card(
                                        onClick = { viewModel.selectAspectRatio(preset.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .border(
                                                width = if (isRatioSelected) 1.5.dp else 1.dp,
                                                color = if (isRatioSelected) activeThemePreset.primaryColor else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .testTag("ratio_preset_${preset.id}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isRatioSelected) activeThemePreset.primaryColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f)
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = preset.displayName,
                                                color = if (isRatioSelected) Color.White else Color.LightGray,
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isRatioSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            // Display Real-time Position Variables
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val selectPresetObj = AspectRatioPreset.getById(activeAspectRatioId)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("X POS", fontSize = 8.sp, color = TextMuted)
                                        Text("${(screenLayout.left * 100).toInt()}%", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Y POS", fontSize = 8.sp, color = TextMuted)
                                        Text("${(screenLayout.top * 100).toInt()}%", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("WIDTH", fontSize = 8.sp, color = TextMuted)
                                        Text("${(screenLayout.width * 100).toInt()}%", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("HEIGHT", fontSize = 8.sp, color = TextMuted)
                                        Text("${(screenLayout.height * 100).toInt()}%", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("PRESET", fontSize = 8.sp, color = TextMuted)
                                        Text(selectPresetObj.displayName, fontSize = 11.sp, color = activeThemePreset.primaryColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.04f))

                        // Section: Screen Position Sliders
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "AMBIENT STYLE ADJUSTMENTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )

                            TuningSlider(
                                label = "Ambience Dimming",
                                value = screenLayout.dimAlpha,
                                range = 0.00f..0.95f,
                                icon = Icons.Default.BrightnessMedium,
                                activeColor = activeThemePreset.primaryColor,
                                onValueChange = { newDim ->
                                    viewModel.updateScreenLayout(
                                        left = screenLayout.left,
                                        top = screenLayout.top,
                                        width = screenLayout.width,
                                        height = screenLayout.height,
                                        dimAlpha = newDim
                                    )
                                }
                            )
                        }

                        // ==========================================
                        // SAVED LAYOUT PRESETS
                        // ==========================================
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "SAVED LAYOUT PRESETS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )

                            val presetsList by viewModel.presets.collectAsState()

                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presetsList.size) { index ->
                                    val preset = presetsList[index]
                                    var showRenameDialog by remember { mutableStateOf(false) }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.04f))
                                            .border(
                                                1.dp,
                                                if (preset.isBuiltIn) IndigoPrimary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.12f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.applyLayoutPreset(preset) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (preset.isBuiltIn) Icons.Default.DashboardCustomize else Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = if (preset.isBuiltIn) IndigoBadgeText else activeThemePreset.primaryColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = preset.name,
                                                color = TextSilver,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (!preset.isBuiltIn) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Rename",
                                                    tint = TextSilver.copy(alpha = 0.6f),
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clickable { showRenameDialog = true }
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = CoralError.copy(alpha = 0.8f),
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clickable { viewModel.deletePreset(preset.id) }
                                                )
                                            }
                                        }

                                        if (showRenameDialog) {
                                            var tempName by remember { mutableStateOf(preset.name) }
                                            AlertDialog(
                                                onDismissRequest = { showRenameDialog = false },
                                                title = { Text("Rename Preset", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                                                text = {
                                                    OutlinedTextField(
                                                        value = tempName,
                                                        onValueChange = { tempName = it },
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White,
                                                            focusedBorderColor = IndigoPrimary,
                                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                                        ),
                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                                    )
                                                },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        viewModel.renamePreset(preset.id, tempName)
                                                        showRenameDialog = false
                                                    }) { Text("Rename", color = IndigoPrimary) }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel", color = TextSilver) }
                                                },
                                                containerColor = ObsidianSurface
                                            )
                                        }
                                    }
                                }
                            }

                            var customPresetName by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customPresetName,
                                    onValueChange = { customPresetName = it },
                                    placeholder = { Text("Custom preset name...", fontSize = 10.5.sp, color = TextMuted) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = activeThemePreset.primaryColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    maxLines = 1,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Button(
                                    onClick = {
                                        if (customPresetName.isNotBlank()) {
                                            viewModel.saveCurrentLayoutAsPreset(customPresetName)
                                            customPresetName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeThemePreset.primaryColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxHeight(),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Active", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = Color.White.copy(alpha = 0.04f))
                        Spacer(modifier = Modifier.height(4.dp))

                        // ==========================================
                        // AMBIENT BACKLIGHTING GLOW
                        // ==========================================
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "AMBIENT BACKLIGHTING GLOW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )

                            val glowIntensity by viewModel.ambientGlow.collectAsState()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AmbientGlowSetting.values().forEach { option ->
                                    val isSelected = glowIntensity == option
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) activeThemePreset.primaryColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f))
                                            .border(
                                                1.dp,
                                                if (isSelected) activeThemePreset.primaryColor else Color.White.copy(alpha = 0.05f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.setAmbientGlow(option) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option.name,
                                            color = if (isSelected) Color.White else TextSilver,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = Color.White.copy(alpha = 0.04f))
                        Spacer(modifier = Modifier.height(4.dp))

                        // Reset Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.resetActiveThemeLayout() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = activeThemePreset.primaryColor
                                ),
                                border = borderStroke(0.8.dp, activeThemePreset.primaryColor.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(imageVector = Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Layout", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetAllToAppDefaults() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.LightGray
                                ),
                                border = borderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    // Diagnostics Tab Content (The original view details)
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
                                    text = "INTENT METADATA LOG",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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

                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val actionName = parsedIntent?.action ?: "android.intent.action.VIEW"
                            DebugLogToken(label = "ACTION", value = actionName, valueColor = TextSilver)

                            val dataVal = playableUri ?: parsedIntent?.dataUri ?: "Awaiting playback transfer..."
                            DebugLogToken(
                                label = "DATA",
                                value = dataVal,
                                valueColor = if (playableUri != null) ConsoleGreen else TextSilver.copy(alpha = 0.5f),
                                isUnderline = playableUri != null
                            )

                            val mimeVal = parsedIntent?.mimeType ?: "video/x-matroska"
                            DebugLogToken(label = "MIME", value = mimeVal, valueColor = TextSilver)

                            val clipCount = parsedIntent?.clipDataItems?.size ?: 0
                            DebugLogToken(
                                label = "CLIP",
                                value = if (clipCount > 0) "$clipCount item(s) detected" else "null (fallback text parser used)",
                                valueColor = TextMuted
                            )

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
                                            text = "Playback warning Details:\n$errorMessage",
                                            color = TextSilver,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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
        }
    }

    if (showThemeDesignerDialog) {
        var designerThemeName by remember { mutableStateOf("") }
        var designerPrimaryColor by remember { mutableStateOf(0xFF6366F1) } // Indigo Default
        var designerSecondaryColor by remember { mutableStateOf(0xFF1E1B4B) } // Dark Indigo Default
        var designerIsAnimated by remember { mutableStateOf(false) }

        val colorOptions = listOf(
            Pair("Indigo", Pair(0xFF6366F1, 0xFF1E1B4B)),
            Pair("Ruby Crimson", Pair(0xFFEF4444, 0xFF450A0A)),
            Pair("Neon Pink", Pair(0xFFEC4899, 0xFF4D0519)),
            Pair("Solar Amber", Pair(0xFFF59E0B, 0xFF451A03)),
            Pair("Teal Forest", Pair(0xFF10B981, 0xFF064E3B)),
            Pair("Cyan Wave", Pair(0xFF06B6D4, 0xFF083344)),
            Pair("Velvet Purple", Pair(0xFF8B5CF6, 0xFF2E1065))
        )

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showThemeDesignerDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131015)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Theme Designer",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showThemeDesignerDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close Designer", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Name field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "THEME TITLE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        OutlinedTextField(
                            value = designerThemeName,
                            onValueChange = { designerThemeName = it },
                            placeholder = { Text("e.g. My Cosmic Vibe", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(designerPrimaryColor),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                cursorColor = Color(designerPrimaryColor)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("designer_theme_name_input"),
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp)
                        )
                    }

                    // Color chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "CHOOSE AMBIENT COLOR PALETTE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            colorOptions.take(4).forEach { opt ->
                                val isColorSelected = designerPrimaryColor == opt.second.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(opt.second.first).copy(alpha = 0.15f))
                                        .border(
                                            width = if (isColorSelected) 2.dp else 1.dp,
                                            color = if (isColorSelected) Color(opt.second.first) else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            designerPrimaryColor = opt.second.first
                                            designerSecondaryColor = opt.second.second
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color(opt.second.first))
                                    )
                                    if (isColorSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            colorOptions.drop(4).forEach { opt ->
                                val isColorSelected = designerPrimaryColor == opt.second.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(opt.second.first).copy(alpha = 0.15f))
                                        .border(
                                            width = if (isColorSelected) 2.dp else 1.dp,
                                            color = if (isColorSelected) Color(opt.second.first) else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            designerPrimaryColor = opt.second.first
                                            designerSecondaryColor = opt.second.second
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color(opt.second.first))
                                    )
                                    if (isColorSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // Background Photo Picker inside designer
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "WALL PHOTO BACKGROUND (OPTIONAL)",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (designerBackdropUri != null) {
                                    coil.compose.AsyncImage(
                                        model = designerBackdropUri,
                                        contentDescription = "Custom preview",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.20f), modifier = Modifier.size(14.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Image Background",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (designerBackdropUri != null) "Custom photo specified" else "Project ambient wall texture",
                                    color = TextMuted,
                                    fontSize = 8.5.sp
                                )
                            }
                            Button(
                                onClick = {
                                    designerPhotoLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(designerPrimaryColor),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Pick File", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Animated effect
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Responsive Light Animation", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Add shifting neon light ribbons to backdrop", color = TextMuted, fontSize = 8.5.sp)
                        }
                        Switch(
                            checked = designerIsAnimated,
                            onCheckedChange = { designerIsAnimated = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(designerPrimaryColor),
                                checkedTrackColor = Color(designerPrimaryColor).copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Bottom Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showThemeDesignerDialog = false },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (designerThemeName.isNotBlank()) {
                                    viewModel.createAndSaveCustomTheme(
                                        name = designerThemeName,
                                        primaryColorHex = designerPrimaryColor.toLong(),
                                        secondaryColorHex = designerSecondaryColor.toLong(),
                                        isAnimated = designerIsAnimated,
                                        backdropImageUri = designerBackdropUri
                                    )
                                    showThemeDesignerDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(designerPrimaryColor),
                                contentColor = Color.White,
                                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                                disabledContentColor = Color.White.copy(alpha = 0.20f)
                            ),
                            enabled = designerThemeName.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("save_custom_theme_btn")
                        ) {
                            Text("Save Theme", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper Border Stroke generator.
 */
@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
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

/**
 * Elegant slider element with fine-aligned labeling.
 */
@Composable
fun TuningSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    onValueChange: (Float) -> Unit,
    formatLabel: (Float) -> String = { "${(it * 100).toInt()}%" }
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = activeColor.copy(alpha = 0.82f),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = formatLabel(value),
                color = activeColor,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.08f)
            ),
            modifier = Modifier.height(30.dp)
        )
    }
}

private data class SubtitleTrackInfo(
    val group: androidx.media3.common.Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

