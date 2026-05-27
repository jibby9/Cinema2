package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current

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
                    activeThemeName = activeThemePreset.name
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
                    // Left Side: Projector Video Player Screen area with responsive drawn theme environment
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        CinemaTheaterLayout(
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
                            isSettingsLoaded = isSettingsLoaded,
                            onLayoutChanged = { left, top, width, height ->
                                viewModel.updateScreenLayout(left, top, width, height, screenLayout.dimAlpha)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                    }

                    // Right Side: Theme controls & telemetry log panel
                    AnimatedVisibility(
                        visible = showDebugPanel,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
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
            } else {
                // Adaptive layout: Vertical stack for standard portrait phones
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top: Ambient viewport projection area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CinemaTheaterLayout(
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
                            isSettingsLoaded = isSettingsLoaded,
                            onLayoutChanged = { left, top, width, height ->
                                viewModel.updateScreenLayout(left, top, width, height, screenLayout.dimAlpha)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom: Tabbed console interface (Themes, adjustments, and diagnostics)
                    AnimatedVisibility(
                        visible = showDebugPanel,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f)
                    ) {
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

/**
 * Modern Material 3 style title bar matching "Sophisticated Dark" aesthetic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaTitleBar(
    onToggleDebug: () -> Unit,
    showDebugPanel: Boolean,
    isFoldableActive: Boolean,
    activeThemeName: String
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Cinema Player",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                    Text(
                        text = "Theme: $activeThemeName",
                        color = Color.LightGray.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        },
        actions = {
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
    isSettingsLoaded: Boolean = true,
    onLayoutChanged: (left: Float, top: Float, width: Float, height: Float) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    // 1. Live state tracking the detected aspect ratio of the active stream
    var detectedRatio by remember { mutableStateOf(1.7777778f) } // default 16:9

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

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = screenLayout.dimAlpha))
        )

        // 3. Compute the adaptive player dimensions based on target area fraction (~0.65)
        val targetAreaFraction = 0.65f
        val aspectR = detectedRatio.coerceIn(0.3f, 3.5f) // sensible min/max bounds

        val targetArea = targetAreaFraction * containerAreaVal
        val heightInit = kotlin.math.sqrt(targetArea / aspectR)
        val widthInit = heightInit * aspectR

        var calculatedWidth = widthInit
        var calculatedHeight = heightInit

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

        val playerWidth = calculatedWidth.dp
        val playerHeight = calculatedHeight.dp

        // Check if layout is customized. If coordinates match the theme default, we treat it as centered.
        val isCustomized = (screenLayout.left != themePreset.defaultLeft || screenLayout.top != themePreset.defaultTop)

        val activeLeftFraction = if (isCustomized) {
            screenLayout.left
        } else {
            val defaultLeft = (containerWidthVal - calculatedWidth) / 2f
            (defaultLeft / containerWidthVal).coerceIn(0f, 1f)
        }

        val activeTopFraction = if (isCustomized) {
            screenLayout.top
        } else {
            val defaultTop = (containerHeightVal - calculatedHeight) / 2f
            (defaultTop / containerHeightVal).coerceIn(0f, 1f)
        }

        val playerLeft = (activeLeftFraction * containerWidthVal).dp
        val playerTop = (activeTopFraction * containerHeightVal).dp

        val cornerShape = RoundedCornerShape(themePreset.cornerRadiusDp.dp)

        // 4. Draggable drag modification (smooth relative tracking and bounds clamping)
        val dragModifier = if (isEditMode) {
            Modifier.pointerInput(containerWidthVal, containerHeightVal, calculatedWidth, calculatedHeight, activeLeftFraction, activeTopFraction) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val canvasW = canvasWidthPx
                    val canvasH = canvasHeightPx
                    if (canvasW > 10f && canvasH > 10f) {
                        val dLeft = dragAmount.x / canvasW
                        val dTop = dragAmount.y / canvasH

                        val newLeftFrac = activeLeftFraction + dLeft
                        val newTopFrac = activeTopFraction + dTop

                        // Clamping fraction so the entire player stays completely inside screen area bounds
                        val maxLeftFrac = ((containerWidthVal - calculatedWidth) / containerWidthVal).coerceAtLeast(0f)
                        val maxTopFrac = ((containerHeightVal - calculatedHeight) / containerHeightVal).coerceAtLeast(0f)

                        val clampedLeftFrac = newLeftFrac.coerceIn(0f, maxLeftFrac)
                        val clampedTopFrac = newTopFrac.coerceIn(0f, maxTopFrac)

                        onLayoutChanged(
                            clampedLeftFrac,
                            clampedTopFrac,
                            screenLayout.width,
                            screenLayout.height
                        )
                    }
                }
            }
        } else {
            Modifier
        }

        // 5. Centered/Positioned Player container frame matching chosen theme visual assets
        Box(
            modifier = Modifier
                .offset(x = playerLeft, y = playerTop)
                .size(width = playerWidth, height = playerHeight)
                .testTag("video_screen_container")
                .drawBehind {
                    val glowRadius = themePreset.glowRadiusDp.dp.toPx()
                    val glowCol = themePreset.glowColor
                    val shadowFactor = themePreset.shadowIntensity
                    
                    // Immersive drop-glow behind the video stream
                    if (glowRadius > 0f) {
                        val passes = 6
                        for (i in 1..passes) {
                            val scale = 1.0f + (i * 0.03f * (glowRadius / 20f))
                            val alphaFactor = (passes - i + 1).toFloat() / passes
                            drawRoundRect(
                                color = glowCol.copy(alpha = glowCol.alpha * alphaFactor * shadowFactor),
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
                .then(dragModifier),
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
                        modifier = Modifier.fillMaxSize(),
                        headers = headers,
                        onVideoAspectRatioDetected = { ratio ->
                            detectedRatio = ratio
                        }
                    )

                    if (!isEditMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(
                                onClick = onClearPlaySource,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier.size(32.dp)
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
                            Text(
                                text = "AMBIENT VIEWING PRESETS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val activeThemeId by viewModel.activeThemeId.collectAsState()
                                ThemePresets.all.forEach { preset ->
                                    val isSelected = activeThemeId == preset.id
                                    Card(
                                        onClick = { viewModel.selectTheme(preset.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(58.dp)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) preset.primaryColor else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) preset.secondaryColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.02f)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = when (preset.id) {
                                                    "cosy_cabin" -> Icons.Default.Weekend
                                                    "sports_arena" -> Icons.Default.EmojiEvents
                                                    else -> Icons.Default.Movie
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected) preset.primaryColor else Color.LightGray.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = preset.name,
                                                color = if (isSelected) Color.White else Color.LightGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
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
                                text = "SCREEN POSITION & SIZE ADJUSTMENTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )

                            TuningSlider(
                                label = "Screen Width",
                                value = screenLayout.width,
                                range = 0.10f..1.00f,
                                icon = Icons.Default.AspectRatio,
                                activeColor = activeThemePreset.primaryColor,
                                onValueChange = { newW ->
                                    viewModel.updateScreenLayout(
                                        left = screenLayout.left,
                                        top = screenLayout.top,
                                        width = newW,
                                        height = screenLayout.height,
                                        dimAlpha = screenLayout.dimAlpha
                                    )
                                }
                            )

                            TuningSlider(
                                label = "Screen Height",
                                value = screenLayout.height,
                                range = 0.10f..1.00f,
                                icon = Icons.Default.Height,
                                activeColor = activeThemePreset.primaryColor,
                                onValueChange = { newH ->
                                    viewModel.updateScreenLayout(
                                        left = screenLayout.left,
                                        top = screenLayout.top,
                                        width = screenLayout.width,
                                        height = newH,
                                        dimAlpha = screenLayout.dimAlpha
                                    )
                                }
                            )

                            TuningSlider(
                                label = "Horizontal Position (Left)",
                                value = screenLayout.left,
                                range = 0.00f..(1.00f - screenLayout.width).coerceAtLeast(0.01f),
                                icon = Icons.Default.ArrowBack,
                                activeColor = activeThemePreset.primaryColor,
                                onValueChange = { newL ->
                                    viewModel.updateScreenLayout(
                                        left = newL,
                                        top = screenLayout.top,
                                        width = screenLayout.width,
                                        height = screenLayout.height,
                                        dimAlpha = screenLayout.dimAlpha
                                    )
                                }
                            )

                            TuningSlider(
                                label = "Vertical Position (Top)",
                                value = screenLayout.top,
                                range = 0.00f..(1.00f - screenLayout.height).coerceAtLeast(0.01f),
                                icon = Icons.Default.ArrowUpward,
                                activeColor = activeThemePreset.primaryColor,
                                onValueChange = { newT ->
                                    viewModel.updateScreenLayout(
                                        left = screenLayout.left,
                                        top = newT,
                                        width = screenLayout.width,
                                        height = screenLayout.height,
                                        dimAlpha = screenLayout.dimAlpha
                                    )
                                }
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

                        Divider(color = Color.White.copy(alpha = 0.04f))

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
