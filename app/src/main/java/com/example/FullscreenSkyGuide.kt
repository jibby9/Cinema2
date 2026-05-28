package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Match Cinema Player Color Palette
private val IndigoPrimary = Color(0xFF6366F1)
private val IndigoSecondary = Color(0xFF4F46E5)
private val ObsidianSurface = Color(0xFF141218)
private val DeepSlateBackground = Color(0xFF0F0D13)
private val TextSilver = Color(0xFFE2E8F0)
private val TextMuted = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenSkyGuide(
    viewModel: MainViewModel,
    onCloseGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val epgList by viewModel.epgProgrammes.collectAsState()
    val channels by viewModel.iptvChannels.collectAsState()
    val categories by viewModel.iptvCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.iptvSearchQuery.collectAsState()
    val currentPlayingChannel by viewModel.currentPlayingChannel.collectAsState()
    val isEpgLoading by viewModel.isEpgLoading.collectAsState()
    val epgLoadStatus by viewModel.epgLoadStatus.collectAsState()

    var activeDateOffset by remember { mutableStateOf(0) } // 0: Today, 1: Tomorrow, 2: Day+2, etc.
    var showSearchField by remember { mutableStateOf(false) }
    
    // Bottom details HUD state (if nil, we auto-select the current playing channel's airing programme)
    var selectedProgDetail by remember { mutableStateOf<Pair<IptvChannel, EpgProgramme>?>(null) }

    // Dynamic clock inside EPG
    var liveTimeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val clockSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            liveTimeText = clockSdf.format(Date())
            delay(1000L)
        }
    }

    val timelineStartMs = remember(activeDateOffset) {
        val cal = Calendar.getInstance()
        if (activeDateOffset == 0) {
            // For today, start from 4 hours ago to show recent past programmes
            System.currentTimeMillis() - 4 * 3600 * 1000L
        } else {
            cal.add(Calendar.DAY_OF_YEAR, activeDateOffset)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }
    val timelineEndMs = remember(timelineStartMs) {
        timelineStartMs + 24 * 3600 * 1000L // 24-hour window
    }

    val dpPerMinute = 4.dp
    val sharedScrollState = rememberScrollState()

    // 7-Day Date Tabs Dynamically Computed
    val dateTabs = remember {
        val list = mutableListOf<Pair<String, Int>>()
        val cal = Calendar.getInstance()
        val daySdf = SimpleDateFormat("EEE d MMM", Locale.getDefault())
        for (i in 0..6) {
            val label = when (i) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> daySdf.format(cal.time)
            }
            list.add(Pair(label, i))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Filter channels
    val filteredChannels = remember(channels, selectedCategory, searchQuery) {
        channels.filter { channel ->
            val catMatches = when (selectedCategory?.id) {
                null -> true
                "favorites_filter" -> channel.isFavorite
                else -> channel.categoryId == selectedCategory?.id
            }
            val queryMatches = if (searchQuery.isBlank()) {
                true
            } else {
                channel.name.contains(searchQuery, ignoreCase = true)
            }
            catMatches && queryMatches
        }
    }

    // Auto center timeline to CURRENT TIME on load/date change
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val stickyColumnWidth = 120.dp
    val totalWidthDp = 5760.dp // 24h * 60m * 4dp = 5760dp

    val scrollToTime = { targetMs: Long ->
        coroutineScope.launch {
            val dpPerMinutePx = with(density) { dpPerMinute.toPx() }
            val offsetMin = (targetMs - timelineStartMs).toFloat() / 60000.0f
            val programOffsetPx = offsetMin * dpPerMinutePx
            val viewportScrollWidth = with(density) { (600.dp).toPx() } // safe estimated
            val scrollTarget = (programOffsetPx - viewportScrollWidth / 2f).coerceIn(0f, sharedScrollState.maxValue.toFloat())
            sharedScrollState.animateScrollTo(scrollTarget.toInt())
        }
    }

    LaunchedEffect(sharedScrollState.maxValue, activeDateOffset) {
        // If today is selected, auto scroll to current time
        if (activeDateOffset == 0 && sharedScrollState.maxValue > 0) {
            val dpPerMinutePx = with(density) { dpPerMinute.toPx() }
            val offsetMin = (System.currentTimeMillis() - timelineStartMs).toFloat() / 60000.0f
            val programOffsetPx = offsetMin * dpPerMinutePx
            val scrollTarget = (programOffsetPx - 300f).coerceIn(0f, sharedScrollState.maxValue.toFloat())
            sharedScrollState.scrollTo(scrollTarget.toInt())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
    ) {
        // 1. Premium Sky Receiver Top Status Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TV Icon / Receiver Brand
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IndigoPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Column {
                    Text(
                        text = "SKY Receiver EPG",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Set-top-box Live TV Guide",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Interactive Info Status & Reload Feedback
            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = epgLoadStatus != null || isEpgLoading,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (epgLoadStatus?.contains("failed", ignoreCase = true) == true)
                                Color(0xFF4A1515) else IndigoSecondary.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, if (epgLoadStatus?.contains("failed", ignoreCase = true) == true) Color.Red.copy(0.3f) else IndigoPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isEpgLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = IndigoPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = if (epgLoadStatus?.contains("success", ignoreCase = true) == true) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (epgLoadStatus?.contains("success", ignoreCase = true) == true) Color.Green else Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(epgLoadStatus ?: "Updating EPG Listings...", color = TextSilver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Realtime STB Digital Clock & Close Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Clock widget
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = liveTimeText,
                        color = Color.Green,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Manual Reload Button
                IconButton(
                    onClick = { viewModel.reloadEpg() },
                    modifier = Modifier.size(34.dp).testTag("reload_epg_btn"),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.04f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload EPG", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Close Guide Button
                Button(
                    onClick = onCloseGuide,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Watch TV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. 7-Day Calendar Date Selectors Row
        ScrollableTabRow(
            selectedTabIndex = activeDateOffset,
            containerColor = DeepSlateBackground,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeDateOffset]),
                    color = IndigoPrimary
                )
            },
            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.05f)) }
        ) {
            dateTabs.forEach { (label, index) ->
                Tab(
                    selected = activeDateOffset == index,
                    onClick = { activeDateOffset = index },
                    text = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (activeDateOffset == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeDateOffset == index) Color.White else TextSilver.copy(alpha = 0.6f)
                        )
                    }
                )
            }
        }

        // 3. Filters & Quick Jumps HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Hotkeys row for scrolling offset
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                SmallEpgButton(label = "Now", icon = Icons.Default.Timeline, onClick = { scrollToTime(System.currentTimeMillis()) })
                SmallEpgButton(label = "-2h", icon = Icons.Default.ChevronLeft, onClick = { scrollToTime(System.currentTimeMillis() - 2 * 3600000L) })
                SmallEpgButton(label = "+2h", icon = Icons.Default.ChevronRight, onClick = { scrollToTime(System.currentTimeMillis() + 2 * 3600000L) })
                SmallEpgButton(label = "+4h", icon = null, onClick = { scrollToTime(System.currentTimeMillis() + 4 * 3600000L) })
                SmallEpgButton(label = "+8h", icon = null, onClick = { scrollToTime(System.currentTimeMillis() + 8 * 3600000L) })

                Spacer(modifier = Modifier.width(12.dp))

                // inline Search focusable search field button
                IconButton(
                    onClick = { showSearchField = !showSearchField },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.04f))
                ) {
                    Icon(
                        imageVector = if (showSearchField) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search channels",
                        tint = if (searchQuery.isNotBlank()) IndigoPrimary else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showSearchField) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setIptvSearchQuery(it) },
                        placeholder = { Text("Filter guide channels...", fontSize = 11.sp, color = TextMuted) },
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp),
                        modifier = Modifier
                            .width(180.dp)
                            .height(40.dp)
                    )
                }
            }

            // Categories Selector Row
            LazyRow(
                contentPadding = PaddingValues(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val isSelected = selectedCategory?.id == "favorites_filter"
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectIptvCategory(IptvCategory("favorites_filter", "⭐ Favorites")) },
                        label = { Text("⭐ Favorites", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.04f),
                            labelColor = TextSilver
                        )
                    )
                }
                items(categories) { cat ->
                    val isSelected = selectedCategory?.id == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectIptvCategory(cat) },
                        label = { Text(cat.name, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.04f),
                            labelColor = TextSilver
                        )
                    )
                }
            }
        }

        // 4. Time Interval Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            // Fixed corner label
            Box(
                modifier = Modifier
                    .width(stickyColumnWidth)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CHANNEL & NUMBER",
                    color = TextSilver.copy(alpha = 0.8f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Synchronized Scrolled Time ticks
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(sharedScrollState)
            ) {
                EpgTimeHeader(
                    startMs = timelineStartMs,
                    endMs = timelineEndMs,
                    dpPerMinute = dpPerMinute,
                    widthDp = totalWidthDp
                )
            }
        }

        // 5. Channels and Programs Lazy Grid Section
        Box(modifier = Modifier.weight(1f)) {
            if (filteredChannels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No EPG channels match the criteria.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredChannels, key = { it.id }) { ch ->
                        val programmes = remember(epgList, ch.id, timelineStartMs, timelineEndMs) {
                            getProgrammesForChannel(ch, epgList, timelineStartMs, timelineEndMs)
                        }

                        val isPlayingThisCh = currentPlayingChannel?.id == ch.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.02f))
                        ) {
                            // Left Column: Channel Descriptor
                            Box(
                                modifier = Modifier
                                    .width(stickyColumnWidth)
                                    .fillMaxHeight()
                                    .background(
                                        if (isPlayingThisCh) IndigoPrimary.copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.01f)
                                    )
                                    .border(
                                        width = if (isPlayingThisCh) 1.dp else 0.dp,
                                        color = if (isPlayingThisCh) IndigoPrimary.copy(alpha = 0.5f) else Color.Transparent
                                    )
                                    .clickable { viewModel.playIptvChannel(ch) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!ch.logoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ch.logoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(ch.name.take(1).uppercase(), color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = ch.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            // Right Box: Scrolling schedule lists
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .horizontalScroll(sharedScrollState)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(totalWidthDp)
                                        .fillMaxHeight()
                                ) {
                                    programmes.forEach { prog ->
                                        val startDiffMin = (maxOf(prog.startMs, timelineStartMs) - timelineStartMs) / 60000.0
                                        val durationMin = (minOf(prog.endMs, timelineEndMs) - maxOf(prog.startMs, timelineStartMs)) / 60000.0

                                        val offsetDp = (startDiffMin * 4.0).dp
                                        val widthDp = maxOf(25.0, (durationMin * 4.0) - 1.0).dp

                                        val isCurrent = System.currentTimeMillis() in prog.startMs..prog.endMs
                                        val isSelected = selectedProgDetail?.first?.id == ch.id && selectedProgDetail?.second?.startMs == prog.startMs
                                        val isDummy = prog.title.startsWith("No Scheduling Info") || prog.description?.contains("placeholder") == true
                                        val isCurrentAndPlaying = isCurrent && isPlayingThisCh

                                        Card(
                                            modifier = Modifier
                                                .padding(vertical = 4.dp, horizontal = 1.dp)
                                                .offset(x = offsetDp)
                                                .width(widthDp)
                                                .fillMaxHeight()
                                                .clickable {
                                                    selectedProgDetail = Pair(ch, prog)
                                                },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when {
                                                    isSelected -> IndigoPrimary.copy(alpha = 0.5f)
                                                    isCurrentAndPlaying -> IndigoPrimary.copy(alpha = 0.35f)
                                                    isCurrent -> Color(0xFF1E1B4B)
                                                    isDummy -> Color.Black.copy(alpha = 0.3f)
                                                    else -> Color.White.copy(alpha = 0.03f)
                                                }
                                            ),
                                            border = BorderStroke(
                                                width = if (isSelected || isCurrentAndPlaying) 1.5.dp else 1.dp,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isCurrentAndPlaying -> IndigoPrimary
                                                    isCurrent -> IndigoPrimary.copy(alpha = 0.60f)
                                                    isDummy -> Color.White.copy(alpha = 0.01f)
                                                    else -> Color.White.copy(alpha = 0.05f)
                                                }
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = prog.title,
                                                    color = if (isCurrent) Color.White else TextSilver.copy(alpha = 0.8f),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val startTimeLabel = formatShortTime(prog.startMs)
                                                val endTimeLabel = formatShortTime(prog.endMs)
                                                Text(
                                                    text = "$startTimeLabel - $endTimeLabel",
                                                    color = if (isCurrent) IndigoPrimary else TextMuted,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    // Precise dynamic current time line indicator
                                    val nowVal = System.currentTimeMillis()
                                    if (nowVal in timelineStartMs..timelineEndMs) {
                                        val diffMin = (nowVal - timelineStartMs) / 60000.0
                                        val lineOffset = (diffMin * 4.0).dp
                                        Box(
                                            modifier = Modifier
                                                .offset(x = lineOffset)
                                                .width(2.dp)
                                                .fillMaxHeight()
                                                .background(Color.Red.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Premium Sky Set-top-box details HUD panel at bottom
        val detailsToRender = selectedProgDetail ?: defaultActiveProg(filteredChannels, epgList)
        EpgDetailsHudPanel(
            selectedInfo = detailsToRender,
            viewModel = viewModel,
            onTuneChClick = { ch ->
                viewModel.playIptvChannel(ch)
            }
        )
    }
}

// Helper to extract default details program structure
@Composable
private fun defaultActiveProg(
    channels: List<IptvChannel>,
    epgList: List<EpgProgramme>
): Pair<IptvChannel, EpgProgramme>? {
    val firstCh = channels.firstOrNull() ?: return null
    val now = System.currentTimeMillis()
    val progsForFirst = epgList.filter { it.channelId == firstCh.id || it.channelId == firstCh.epgId }
    val currentOrFirst = progsForFirst.find { now in it.startMs..it.endMs } 
        ?: progsForFirst.firstOrNull() 
        ?: EpgProgramme(firstCh.id, "No Information available", now, now + 120 * 60000, "Detailed programming schedules for ${firstCh.name} are currently empty.")
    return Pair(firstCh, currentOrFirst)
}

@Composable
fun EpgDetailsHudPanel(
    selectedInfo: Pair<IptvChannel, EpgProgramme>?,
    viewModel: MainViewModel,
    onTuneChClick: (IptvChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .border(topBorderStroke()),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        if (selectedInfo == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a programme to view scheduling details.", color = TextMuted, fontSize = 11.5.sp)
            }
        } else {
            val (ch, prog) = selectedInfo
            val now = System.currentTimeMillis()
            val isLive = now in prog.startMs..prog.endMs
            val isFuture = prog.startMs > now
            val startLabel = formatShortTime(prog.startMs)
            val endLabel = formatShortTime(prog.endMs)
            val durationMin = ((prog.endMs - prog.startMs) / 60000L).toInt()

            val reminders by viewModel.reminders.collectAsState()
            val existingReminder = reminders.find { it.channelId == ch.id && it.programStartMs == prog.startMs }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info block
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!ch.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ch.logoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(ch.name.take(1).uppercase(), color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(prog.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (isLive) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "${ch.name} | $startLabel - $endLabel ($durationMin Min)",
                            color = IndigoPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = prog.description ?: "No description listing has been supplied for this show.",
                            color = TextSilver.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 13.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Action controls block
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Watch channel button
                        Button(
                            onClick = { onTuneChClick(ch) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tune In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Toggle Favorite Button
                        IconButton(
                            onClick = { viewModel.toggleFavoriteChannel(ch.id) },
                            modifier = Modifier.size(34.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (ch.isFavorite) Color(0xFFFFD700).copy(0.1f) else Color.White.copy(0.04f)
                            )
                        ) {
                            Icon(
                                imageVector = if (ch.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (ch.isFavorite) Color(0xFFFFD700) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Set/Remove Reminder for dynamic user notifications
                    if (isFuture) {
                        if (existingReminder != null) {
                            TextButton(
                                onClick = { viewModel.removeReminder(existingReminder.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(0.8f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel Reminder", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.addReminder(ch, prog, 5) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.08f), contentColor = Color.White),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Reminder", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom top border draw for premium UI divider look
private fun topBorderStroke() = BorderStroke(
    width = 1.dp,
    color = Color.White.copy(alpha = 0.08f)
)

private fun formatShortTime(timeMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMs))
}

private fun getProgrammesForChannel(channel: IptvChannel, epgList: List<EpgProgramme>, timelineStartMs: Long, timelineEndMs: Long): List<EpgProgramme> {
    val channelProgs = epgList.filter { it.channelId == channel.id || (channel.epgId != null && it.channelId == channel.epgId) }
    if (channelProgs.isNotEmpty()) {
        val sorted = channelProgs.filter { it.endMs > timelineStartMs && it.startMs < timelineEndMs }.sortedBy { it.startMs }
        if (sorted.isEmpty()) {
            return listOf(EpgProgramme(channel.id, "No Information", timelineStartMs, timelineEndMs, "No program description details available."))
        }
        val result = mutableListOf<EpgProgramme>()
        var currentStart = timelineStartMs
        for (prog in sorted) {
            if (prog.startMs > currentStart) {
                result.add(EpgProgramme(channel.id, "No Information", currentStart, prog.startMs, "No program description details available."))
            }
            result.add(prog)
            currentStart = maxOf(currentStart, prog.endMs)
        }
        if (currentStart < timelineEndMs) {
            result.add(EpgProgramme(channel.id, "No Information", currentStart, timelineEndMs, "No program description details available."))
        }
        return result
    }

    // Generate virtual STB guide schedule fallback matching IPTV playback
    val synthetic = mutableListOf<EpgProgramme>()
    val twoHoursMs = 2 * 3600 * 1000L
    val roundedStart = (timelineStartMs / twoHoursMs) * twoHoursMs
    var current = roundedStart
    while (current < timelineEndMs) {
        val blockStart = current
        val blockEnd = current + twoHoursMs
        val title = when (((blockStart / twoHoursMs) % 6).toInt()) {
            0 -> "Blockbuster Movie Special"
            1 -> "Late Night Show Selection"
            2 -> "Morning Broadcast & News Feed"
            3 -> "Midday Entertainment Hour"
            4 -> "Afternoon Documentaries Series"
            else -> "Evening Primetime Presentation"
        }
        synthetic.add(
            EpgProgramme(
                channelId = channel.id,
                title = title,
                startMs = blockStart,
                endMs = blockEnd,
                description = "Offline receiver TV scheduler placeholder. Enjoy clear digital live broadcast feeds on Cinema Player."
            )
        )
        current = blockEnd
    }
    return synthetic
}
