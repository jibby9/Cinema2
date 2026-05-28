package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// Matches Cinema Player colors to guarantee consistent visual harmony
private val IndigoPrimary = Color(0xFF6366F1)
private val IndigoSecondary = Color(0xFF4F46E5)
private val ObsidianSurface = Color(0xFF141218)
private val TextSilver = Color(0xFFE2E8F0)
private val TextMuted = Color(0xFF64748B)

@Composable
fun IptvDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Channels, 1: TV Guide, 2: Setup/Sources
    
    val xtreamAccounts by viewModel.xtreamAccounts.collectAsState()
    val m3uPlaylists by viewModel.m3uPlaylists.collectAsState()
    val isIptvLoading by viewModel.isIptvLoading.collectAsState()
    val iptvErrorMessage by viewModel.iptvErrorMessage.collectAsState()

    val countsActive = xtreamAccounts.any { it.isActive } || m3uPlaylists.any { it.isActive }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianSurface)
    ) {
        // Mode Subheader with Segmented Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = ObsidianSurface,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = IndigoPrimary
                )
            }
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Channels", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("iptv_tab_channels")
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("TV Guide", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("iptv_tab_guide")
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Sources", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.SettingsInputComponent, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("iptv_tab_sources")
            )
        }

        // Global status loading or error overlay
        if (isIptvLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            ) {
                LinearProgressIndicator(
                    color = IndigoPrimary,
                    trackColor = ObsidianSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        iptvErrorMessage?.let { err ->
            Snackbar(
                containerColor = Color(0xFF3B1010),
                contentColor = Color.White,
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    Text(err, fontSize = 11.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Active layout contents
        Box(modifier = Modifier.weight(1f)) {
            if (!countsActive && activeTab != 2) {
                // If no active sources exist, show prompt to set up
                IptvEmptyStatePrompt(onActionClick = { activeTab = 2 })
            } else {
                when (activeTab) {
                    0 -> IptvChannelsTab(viewModel = viewModel)
                    1 -> IptvGuideTab(viewModel = viewModel)
                    2 -> IptvSetupTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun IptvEmptyStatePrompt(
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.SettingsInputComponent,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = "Welcome to IPTV Mode",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Add an Xtream Server Login credentials or import an M3U playlist URL to load your live television streams, channel lists, and EPG schedules.",
                color = TextSilver.copy(alpha = 0.65f),
                fontSize = pxToSp(14f),
                lineHeight = 16.5.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("setup_iptv_first_btn")
            ) {
                Text("Add IPTV Source", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvChannelsTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.iptvCategories.collectAsState()
    val channels by viewModel.iptvChannels.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.iptvSearchQuery.collectAsState()
    val epgList by viewModel.epgProgrammes.collectAsState()

    var showSearchField by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        
        // Search & Option Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedCategory?.name ?: "All Channels",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { showSearchField = !showSearchField }
            ) {
                Icon(
                    imageVector = if (showSearchField) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search channels",
                    tint = if (searchQuery.isNotBlank()) IndigoPrimary else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Inline input box
        AnimatedVisibility(
            visible = showSearchField,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setIptvSearchQuery(it) },
                placeholder = { Text("Filter channels by keyword...", fontSize = 12.sp, color = TextMuted) },
                maxLines = 1,
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setIptvSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(10.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(48.dp)
                    .testTag("iptv_channel_search")
            )
        }

        // Categories Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Include dynamic Favorites filter
            item {
                val isSelected = selectedCategory?.id == "favorites_filter"
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.selectIptvCategory(IptvCategory("favorites_filter", "⭐ Favorites"))
                    },
                    label = { Text("⭐ Favorites", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = TextSilver
                    ),
                    modifier = Modifier.testTag("iptv_cat_fav")
                )
            }

            items(categories) { cat ->
                val isSelected = selectedCategory?.id == cat.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectIptvCategory(cat) },
                    label = { Text(cat.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = TextSilver
                    ),
                    modifier = Modifier.testTag("iptv_cat_${cat.id}")
                )
            }
        }

        // Channels Lazy Column
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

        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedCategory?.id == "favorites_filter") "No favorite channels saved yet. Tap the ⭐ in live streams to add!" else "No channels found matching the search criteria.",
                    color = TextMuted,
                    fontSize = pxToSp(13f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredChannels, key = { it.id }) { ch ->
                    // Find active EPG
                    val activeEpg = remember(epgList, ch.id) {
                        findActiveEpg(epgList, ch.id, ch.epgId)
                    }

                    IptvChannelListItem(
                        channel = ch,
                        activeEpg = activeEpg,
                        onChannelClick = { viewModel.playIptvChannel(ch) },
                        onFavoriteToggle = { viewModel.toggleFavoriteChannel(ch.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun IptvChannelListItem(
    channel: IptvChannel,
    activeEpg: EpgProgramme?,
    onChannelClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .clickable { onChannelClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Logo preview box
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = channel.name.take(1).uppercase(),
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Title and description details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = channel.name,
                color = TextSilver,
                fontSize = pxToSp(14f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (activeEpg != null) {
                Text(
                    text = "LIVE: " + activeEpg.title,
                    color = IndigoPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Live Stream",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1
                )
            }
        }

        // Favorite Toggle Icon
        IconButton(
            onClick = onFavoriteToggle
        ) {
            Icon(
                imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Toggle favorite status",
                tint = if (channel.isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvGuideTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val epgList by viewModel.epgProgrammes.collectAsState()
    val channels by viewModel.iptvChannels.collectAsState()
    val categories by viewModel.iptvCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.iptvSearchQuery.collectAsState()

    var showSearchField by remember { mutableStateOf(false) }
    var selectedProgDetail by remember { mutableStateOf<Pair<IptvChannel, EpgProgramme>?>(null) }

    val now = remember { System.currentTimeMillis() }
    val timelineStartMs = remember(now) { now - 2 * 3600 * 1000L }
    val timelineEndMs = remember(timelineStartMs) { timelineStartMs + 24 * 3600 * 1000L }

    val dpPerMinute = 4.dp
    val sharedScrollState = rememberScrollState()

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

    Column(modifier = modifier.fillMaxSize().background(ObsidianSurface)) {
        // Guide filter header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "EPG: " + (selectedCategory?.name ?: "All Channels"),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { showSearchField = !showSearchField }
            ) {
                Icon(
                    imageVector = if (showSearchField) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search guide",
                    tint = if (searchQuery.isNotBlank()) IndigoPrimary else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Inline search field
        AnimatedVisibility(
            visible = showSearchField,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setIptvSearchQuery(it) },
                placeholder = { Text("Filter guide channels...", fontSize = 12.sp, color = TextMuted) },
                maxLines = 1,
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setIptvSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(10.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(48.dp)
            )
        }

        // Category selection chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val isSelected = selectedCategory?.id == "favorites_filter"
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.selectIptvCategory(IptvCategory("favorites_filter", "⭐ Favorites"))
                    },
                    label = { Text("⭐ Favorites", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = TextSilver
                    )
                )
            }

            items(categories) { cat ->
                val isSelected = selectedCategory?.id == cat.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectIptvCategory(cat) },
                    label = { Text(cat.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = TextSilver
                    )
                )
            }
        }

        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No channels match the filter criteria.",
                    color = TextMuted,
                    fontSize = pxToSp(13f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // The beautiful STB EPG Grid Layout!
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val density = LocalDensity.current
                val coroutineScope = rememberCoroutineScope()

                val dpPerMinutePx = with(density) { dpPerMinute.toPx() }
                val stickyColumnWidth = 110.dp
                val stickyColumnWidthPx = with(density) { stickyColumnWidth.toPx() }
                val totalWidthDp = 5760.dp

                // Formulate scrollToTime centering logic
                val scrollToTime = { targetMs: Long ->
                    coroutineScope.launch {
                        val offsetMin = (targetMs - timelineStartMs).toFloat() / 60000.0f
                        val programOffsetPx = offsetMin * dpPerMinutePx
                        val viewportScrollWidth = with(density) { (maxWidth - stickyColumnWidth).toPx() }
                        val scrollTarget = (programOffsetPx - viewportScrollWidth / 2.0f).coerceIn(0f, sharedScrollState.maxValue.toFloat())
                        sharedScrollState.animateScrollTo(scrollTarget.toInt())
                    }
                }

                // Auto center now on load
                LaunchedEffect(sharedScrollState.maxValue) {
                    if (sharedScrollState.maxValue > 0) {
                        val offsetMin = (System.currentTimeMillis() - timelineStartMs).toFloat() / 60000.0f
                        val programOffsetPx = offsetMin * dpPerMinutePx
                        val viewportScrollWidth = with(density) { (maxWidth - stickyColumnWidth).toPx() }
                        val scrollTarget = (programOffsetPx - viewportScrollWidth / 2.0f).coerceIn(0f, sharedScrollState.maxValue.toFloat())
                        sharedScrollState.scrollTo(scrollTarget.toInt())
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Quick Jump and Controls row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallEpgButton(label = "Now", icon = Icons.Default.Timeline, onClick = { scrollToTime(System.currentTimeMillis()) })
                        SmallEpgButton(label = "-2h", icon = Icons.Default.ChevronLeft, onClick = { scrollToTime(System.currentTimeMillis() - 2 * 3600000L) })
                        SmallEpgButton(label = "+2h", icon = Icons.Default.ChevronRight, onClick = { scrollToTime(System.currentTimeMillis() + 2 * 3600000L) })
                        SmallEpgButton(label = "+4h", icon = null, onClick = { scrollToTime(System.currentTimeMillis() + 4 * 3600000L) })
                        SmallEpgButton(label = "+8h", icon = null, onClick = { scrollToTime(System.currentTimeMillis() + 8 * 3600000L) })
                    }

                    // Sticky header row: corner CHANNELS block + timeline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        // Corner
                        Box(
                            modifier = Modifier
                                .width(stickyColumnWidth)
                                .fillMaxHeight()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(1.dp, Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CHANNELS", color = TextSilver, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }

                        // Timeline Header
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

                    // Main Channel listings & grid
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredChannels, key = { it.id }) { ch ->
                                val programmes = remember(epgList, ch.id, timelineStartMs, timelineEndMs) {
                                    getProgrammesForChannel(ch, epgList, timelineStartMs, timelineEndMs)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .border(1.dp, Color.White.copy(alpha = 0.03f))
                                ) {
                                    // Sticky Channel descriptor
                                    Box(
                                        modifier = Modifier
                                            .width(stickyColumnWidth)
                                            .fillMaxHeight()
                                            .background(Color.White.copy(alpha = 0.02f))
                                            .clickable { viewModel.playIptvChannel(ch) }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Logo preview
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
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
                                                    Text(ch.name.take(1).uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Name
                                            Text(
                                                text = ch.name,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 11.sp
                                            )
                                        }
                                    }

                                    // Horizontally scrolling timeline for current channel
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .horizontalScroll(sharedScrollState)
                                    ) {
                                        // Simple continuous box
                                        Box(
                                            modifier = Modifier
                                                .width(totalWidthDp)
                                                .fillMaxHeight()
                                        ) {
                                            // Display all programs relative to startMs
                                            programmes.forEach { prog ->
                                                val startDiffMin = (maxOf(prog.startMs, timelineStartMs) - timelineStartMs) / 60000.0
                                                val durationMin = (minOf(prog.endMs, timelineEndMs) - maxOf(prog.startMs, timelineStartMs)) / 60000.0

                                                val offsetDp = (startDiffMin * 4.0).dp
                                                val widthDp = maxOf(20.0, (durationMin * 4.0) - 1.0).dp // -1dp for visible program spacing gap

                                                val nowVal = System.currentTimeMillis()
                                                val isCurrent = nowVal in prog.startMs..prog.endMs
                                                val isDummy = prog.title.startsWith("No Scheduling Info") || prog.description?.contains("placeholder") == true

                                                Card(
                                                    modifier = Modifier
                                                        .padding(vertical = 4.dp, horizontal = 1.dp)
                                                        .offset(x = offsetDp)
                                                        .width(widthDp)
                                                        .fillMaxHeight()
                                                        .clickable {
                                                            if (!isDummy) {
                                                                selectedProgDetail = Pair(ch, prog)
                                                            } else {
                                                                // Tune directly
                                                                viewModel.playIptvChannel(ch)
                                                            }
                                                        },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = when {
                                                            isCurrent -> IndigoPrimary.copy(alpha = 0.22f)
                                                            isDummy -> Color.Black.copy(alpha = 0.4f)
                                                            else -> Color.White.copy(alpha = 0.04f)
                                                        }
                                                    ),
                                                    border = BorderStroke(
                                                        width = 1.dp,
                                                        color = when {
                                                            isCurrent -> IndigoPrimary.copy(alpha = 0.7f)
                                                            isDummy -> Color.White.copy(alpha = 0.02f)
                                                            else -> Color.White.copy(alpha = 0.08f)
                                                        }
                                                    )
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                                        verticalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = prog.title,
                                                            color = if (isCurrent) Color.White else TextSilver.copy(alpha = 0.8f),
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            val startTimeLabel = formatShortTime(prog.startMs)
                                                            val endTimeLabel = formatShortTime(prog.endMs)
                                                            Text(
                                                                text = "$startTimeLabel - $endTimeLabel",
                                                                color = if (isCurrent) IndigoPrimary else TextMuted,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )

                                                            if (isCurrent && !isDummy) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color.Red)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Vertical pointer showing precise current time
                                            val curTime = System.currentTimeMillis()
                                            if (curTime in timelineStartMs..timelineEndMs) {
                                                val curDiffMin = (curTime - timelineStartMs) / 60000.0
                                                val lineOffset = (curDiffMin * 4.0).dp

                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = lineOffset)
                                                        .width(2.dp)
                                                        .fillMaxHeight()
                                                        .background(Color.Red.copy(alpha = 0.65f))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Programme Details dialog overlay
    selectedProgDetail?.let { (channel, programme) ->
        ProgrammeDetailsDialog(
            channel = channel,
            programme = programme,
            onDismiss = { selectedProgDetail = null },
            onTuneClick = {
                viewModel.playIptvChannel(channel)
                selectedProgDetail = null
            },
            onFavoriteToggle = {
                viewModel.toggleFavoriteChannel(channel.id)
            }
        )
    }
}

@Composable
fun SmallEpgButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.05f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier.height(30.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = IndigoPrimary)
            }
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EpgTimeHeader(
    startMs: Long,
    endMs: Long,
    dpPerMinute: androidx.compose.ui.unit.Dp,
    widthDp: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(widthDp)
            .fillMaxHeight()
    ) {
        // Subtle horizontal grid line along the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.1f))
        )

        // Render 30 minute ticks
        val intervalMs = 30 * 60 * 1000L
        val roundedStart = (startMs / intervalMs) * intervalMs
        var currentTick = roundedStart

        while (currentTick <= endMs) {
            if (currentTick >= startMs) {
                val tickDiffMin = (currentTick - startMs) / 60000.0
                val offsetDp = (tickDiffMin * 4.0).dp

                // Fine indicator line tick
                Box(
                    modifier = Modifier
                        .offset(x = offsetDp)
                        .width(1.dp)
                        .height(6.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.25f))
                )

                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val timeLabel = sdf.format(java.util.Date(currentTick))

                // Show hour labels
                Text(
                    text = timeLabel,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .offset(x = offsetDp + 4.dp, y = 8.dp)
                )
            }
            currentTick += intervalMs
        }

        // Timeline Header Current Time dynamic Pointer tick mark
        val now = System.currentTimeMillis()
        if (now in startMs..endMs) {
            val nowDiffMin = (now - startMs) / 60000.0
            val lineOffset = (nowDiffMin * 4.0).dp

            // Draw a tiny red circle indicator representing "now" at the top of grid line
            Box(
                modifier = Modifier
                    .offset(x = lineOffset - 4.dp, y = 2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }
    }
}

@Composable
fun ProgrammeDetailsDialog(
    channel: IptvChannel,
    programme: EpgProgramme,
    onDismiss: () -> Unit,
    onTuneClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isLive = now in programme.startMs..programme.endMs

    // Formatting times
    val startLabel = remember(programme.startMs) { formatShortTime(programme.startMs) }
    val endLabel = remember(programme.endMs) { formatShortTime(programme.endMs) }
    val durationMin = remember(programme.startMs, programme.endMs) {
        ((programme.endMs - programme.startMs) / 60000L).toInt()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = ObsidianSurface,
        titleContentColor = Color.White,
        modifier = modifier
            .testTag("epg_details_dialog")
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Channel Logo
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(channel.name.take(1).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Column {
                    Text(
                        text = channel.name,
                        color = TextSilver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "EPG Channel Entry",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Show LIVE badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Red)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("UPCOMING", color = TextSilver, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text(
                        text = "$startLabel - $endLabel ($durationMin Minutes)",
                        color = IndigoPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Programme Title
                Text(
                    text = programme.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                )

                // Progress tracker if airing live
                if (isLive) {
                    val progressFraction = remember(programme.startMs, programme.endMs, now) {
                        val elapsed = now - programme.startMs
                        val total = programme.endMs - programme.startMs
                        if (total > 0) (elapsed.toFloat() / total).coerceIn(0f, 1f) else 0f
                    }
                    val percentStr = (progressFraction * 100).toInt().toString() + "%"
                    val minLeft = ((programme.endMs - now) / 60000L).coerceAtLeast(0)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Current Progress", color = TextMuted, fontSize = 10.sp)
                            Text("$percentStr ($minLeft min left)", color = TextSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            color = IndigoPrimary,
                            trackColor = Color.White.copy(alpha = 0.06f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                        )
                    }
                }

                // Description body
                Text(
                    text = programme.description ?: "No description schedule records available for this selected programme.",
                    color = TextSilver.copy(alpha = 0.7f),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onTuneClick,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("epg_tune_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Tune In Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close", color = TextSilver, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    )
}

private fun getFilledProgrammes(channelProgs: List<EpgProgramme>, startMs: Long, endMs: Long, channelId: String): List<EpgProgramme> {
    val sorted = channelProgs.filter { it.endMs > startMs && it.startMs < endMs }.sortedBy { it.startMs }
    if (sorted.isEmpty()) {
        return listOf(EpgProgramme(channelId, "No Scheduling Info", startMs, endMs, "No description available."))
    }
    val result = mutableListOf<EpgProgramme>()
    var currentStart = startMs
    for (prog in sorted) {
        if (prog.startMs > currentStart) {
            // there is a gap!
            result.add(EpgProgramme(channelId, "No Scheduling Info", currentStart, prog.startMs, "No description available."))
        }
        result.add(prog)
        currentStart = maxOf(currentStart, prog.endMs)
    }
    if (currentStart < endMs) {
        result.add(EpgProgramme(channelId, "No Scheduling Info", currentStart, endMs, "No description available."))
    }
    return result
}

private fun getProgrammesForChannel(channel: IptvChannel, epgList: List<EpgProgramme>, timelineStartMs: Long, timelineEndMs: Long): List<EpgProgramme> {
    val channelProgs = epgList.filter { it.channelId == channel.id || (channel.epgId != null && it.channelId == channel.epgId) }
    if (channelProgs.isNotEmpty()) {
        return getFilledProgrammes(channelProgs, timelineStartMs, timelineEndMs, channel.id)
    }

    // Generate synthetic/mock scheduling info for fallback to make guide 100% interactive!
    val synthetic = mutableListOf<EpgProgramme>()
    val twoHoursMs = 2 * 3600 * 1000L
    // Round start to even 2 hours
    val roundedStart = (timelineStartMs / twoHoursMs) * twoHoursMs
    var current = roundedStart
    while (current < timelineEndMs) {
        val blockStart = current
        val blockEnd = current + twoHoursMs
        val title = when (((blockStart / twoHoursMs) % 6).toInt()) {
            0 -> "Prime Time Special Broadcast"
            1 -> "Late Night Feature Show"
            2 -> "Early Morning News & Updates"
            3 -> "Midday Live Entertainment"
            4 -> "Afternoon Special Selection"
            else -> "Dinner Feature Presentation"
        }
        synthetic.add(
            EpgProgramme(
                channelId = channel.id,
                title = title,
                startMs = blockStart,
                endMs = blockEnd,
                description = "This is a virtual STB guide broadcast placeholder. Enjoy high-definition live streaming channels on Cinema Player."
            )
        )
        current = blockEnd
    }
    return synthetic
}

@Composable
fun IptvSetupTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val xtreamAccounts by viewModel.xtreamAccounts.collectAsState()
    val m3uPlaylists by viewModel.m3uPlaylists.collectAsState()

    var showAddForm by remember { mutableStateOf(false) }
    var useXtreamForm by remember { mutableStateOf(true) } // true: Xtream credentials, false: M3U playlist

    // Form states
    var nameInput by remember { mutableStateOf("") }
    var serverUrlInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var playlistUrlInput by remember { mutableStateOf("") }
    var xmltvUrlInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        // Form trigger button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IndigoSecondary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddForm = !showAddForm }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showAddForm) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showAddForm) "Collapse Form View" else "Add New IPTV Source Feed",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Expandable input form
        item {
            AnimatedVisibility(
                visible = showAddForm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Feed source type toggles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (useXtreamForm) IndigoSecondary else Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { useXtreamForm = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Xtream Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (!useXtreamForm) IndigoSecondary else Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { useXtreamForm = false }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("M3U Playlist URL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                    }

                    // Fields
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Label Name (e.g. My TV)", fontSize = 11.sp) },
                        colors = formFieldColors(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().testTag("iptv_field_label")
                    )

                    if (useXtreamForm) {
                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { serverUrlInput = it },
                            label = { Text("Server URL (http://domain.com:port)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = formFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth().testTag("iptv_field_server")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("Username", fontSize = 11.sp) },
                                colors = formFieldColors(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.weight(1f).testTag("iptv_field_user")
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password", fontSize = 11.sp) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = formFieldColors(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.weight(1f).testTag("iptv_field_pass")
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = playlistUrlInput,
                            onValueChange = { playlistUrlInput = it },
                            label = { Text("M3U / M3U8 Playlist URL link", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = formFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth().testTag("iptv_field_m3u")
                        )

                        OutlinedTextField(
                            value = xmltvUrlInput,
                            onValueChange = { xmltvUrlInput = it },
                            label = { Text("Optional: XMLTV / EPG URL link", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = formFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth().testTag("iptv_field_xmltv")
                        )
                    }

                    // Form action trigger button
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                if (useXtreamForm) {
                                    if (serverUrlInput.isNotBlank() && usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        viewModel.addXtreamAccount(nameInput, serverUrlInput, usernameInput, passwordInput)
                                        // Reset fields
                                        nameInput = ""
                                        serverUrlInput = ""
                                        usernameInput = ""
                                        passwordInput = ""
                                        showAddForm = false
                                    }
                                } else {
                                    if (playlistUrlInput.isNotBlank()) {
                                        viewModel.addM3uPlaylist(nameInput, playlistUrlInput, xmltvUrlInput.takeIf { it.isNotBlank() })
                                        nameInput = ""
                                        playlistUrlInput = ""
                                        xmltvUrlInput = ""
                                        showAddForm = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("iptv_submit_btn")
                    ) {
                        Text("Add Source Profile", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }

        // Active Xtream profiles header
        if (xtreamAccounts.isNotEmpty()) {
            item {
                Text("Xtream Codes Logins", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            }
            items(xtreamAccounts) { account ->
                IptvSourceItem(
                    name = account.name,
                    subtext = account.serverUrl,
                    isActive = account.isActive,
                    onSelect = { viewModel.selectXtreamAccount(account) },
                    onDelete = { viewModel.deleteXtreamAccount(account) }
                )
            }
        }

        // Active M3U configs header
        if (m3uPlaylists.isNotEmpty()) {
            item {
                Text("M3U Local Playlists", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            }
            items(m3uPlaylists) { config ->
                IptvSourceItem(
                    name = config.name,
                    subtext = config.playlistUrl,
                    isActive = config.isActive,
                    onSelect = { viewModel.selectM3uPlaylist(config) },
                    onDelete = { viewModel.deleteM3uPlaylist(config) }
                )
            }
        }
    }
}

@Composable
fun IptvSourceItem(
    name: String,
    subtext: String,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) IndigoSecondary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f))
            .border(
                1.dp,
                if (isActive) IndigoPrimary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.04f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isActive) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                }
                Text(name, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text(subtext, color = TextMuted, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete account source", tint = Color.Red.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = IndigoPrimary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
    focusedLabelColor = IndigoPrimary,
    unfocusedLabelColor = TextMuted,
    focusedContainerColor = Color.Black.copy(alpha = 0.15f),
    unfocusedContainerColor = Color.Black.copy(alpha = 0.15f)
)

// Dynamic timezone and intervals helpers
private fun findActiveEpg(epgList: List<EpgProgramme>, channelId: String, epgChannelId: String?): EpgProgramme? {
    val id = epgChannelId ?: channelId
    val now = System.currentTimeMillis()
    return epgList.firstOrNull { it.channelId == id && now >= it.startMs && now <= it.endMs }
}

private fun formatInterval(startMs: Long, endMs: Long): String {
    val start = formatShortTime(startMs)
    val end = formatShortTime(endMs)
    return "$start - $end"
}

private fun formatShortTime(timeMs: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timeMs))
}

private fun pxToSp(px: Float): androidx.compose.ui.unit.TextUnit {
    return (px * 0.85).sp
}
