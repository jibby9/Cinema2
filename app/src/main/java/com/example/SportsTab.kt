package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily

// Color Constants matching Cinema Player
private val IndigoPrimary = Color(0xFF6366F1)
private val ObsidianSurface = Color(0xFF0F172A)
private val TextMuted = Color(0xFF94A3B8)
private val TextSilver = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val sportsEvents by viewModel.sportsEvents.collectAsState()
    val selectedFilter by viewModel.selectedSportFilter.collectAsState()
    val searchQuery by viewModel.sportsSearchQuery.collectAsState()
    val liveNowOnly by viewModel.liveNowOnly.collectAsState()

    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    // Filter available sport names dynamically from the event database plus static ones
    val staticSports = listOf(
        "All Sports", "Football", "UFC", "Boxing", "Basketball", 
        "Motorsport", "American Football", "Baseball", "Hockey", 
        "Tennis", "Darts", "Snooker"
    )

    // Filtered events calculation
    val filteredEvents = remember(sportsEvents, selectedFilter, searchQuery, liveNowOnly) {
        val currentFilter = selectedFilter
        sportsEvents.filter { event ->
            val matchesSport = if (currentFilter.isNullOrBlank() || currentFilter == "All Sports") {
                true
            } else {
                event.sport.lowercase() == currentFilter.lowercase()
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                event.title.lowercase().contains(searchQuery.lowercase()) ||
                event.competition.lowercase().contains(searchQuery.lowercase()) ||
                (event.description?.lowercase()?.contains(searchQuery.lowercase()) == true)
            }

            val matchesLive = if (liveNowOnly) {
                event.getStatus() == "LIVE"
            } else {
                true
            }

            matchesSport && matchesSearch && matchesLive
        }.sortedBy { event ->
            // Prioritize LIVE events, then by starting time (chronological)
            val statusVal = when(event.getStatus()) {
                "LIVE" -> 0
                "UPCOMING" -> 1
                else -> 2
            }
            statusVal * 10_000_000_000_000L + event.dateTimeMs
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianSurface)
    ) {
        // --- TOOLBAR WITH SEARCH & ACTION CONTROLS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Input Field
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSportsSearchQuery(it) },
                placeholder = { Text("Search teams, fighters, cups...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = IndigoPrimary
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSportsSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            )

            // Live Now Switch button
            FilledIconToggleButton(
                checked = liveNowOnly,
                onCheckedChange = { viewModel.toggleLiveNowOnly() },
                modifier = Modifier
                    .size(48.dp)
                    .then(if (liveNowOnly) Modifier.border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp)) else Modifier),
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = Color.White.copy(alpha = 0.04f),
                    checkedContainerColor = Color.Red.copy(alpha = 0.15f),
                    contentColor = TextMuted,
                    checkedContentColor = Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "Live Events Only",
                    tint = if (liveNowOnly) Color.Red else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Refresh schedule action button
            IconButton(
                onClick = {
                    viewModel.refreshSportsEvents()
                    Toast.makeText(context, "Schedules reloaded", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Schedules",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Diagnostics active logs explorer
            IconButton(
                onClick = {
                    showDiagnosticsDialog = true
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(12.dp))
                    .testTag("sports_diagnostics_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Diagnostics Logs",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Integrity logs dialog popup rendering
        if (showDiagnosticsDialog) {
            val logs = remember { FixtureDiagnostics.getLogs() }
            AlertDialog(
                onDismissRequest = { showDiagnosticsDialog = false },
                confirmButton = {
                    TextButton(onClick = { showDiagnosticsDialog = false }) {
                        Text("Dismiss", color = IndigoPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.padding(end = 8.dp))
                        Text("Fixture Integrity Logs", color = Color.White)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Below is the normalization, deduplication, and confidence grading trace for current sports feeds:", color = TextSilver, fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                                .background(Color.Black, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            if (logs.isEmpty()) {
                                Text("No telemetry logs recorded.", color = TextMuted, fontSize = 11.sp)
                            } else {
                                LazyColumn {
                                    items(logs) { line ->
                                        Text(
                                            text = line,
                                            color = when {
                                                line.contains("REJECTED") -> Color(0xFFEF4444)
                                                line.contains("SUPPRESSED") -> Color(0xFFEF4444)
                                                line.contains("DUPLICATE") -> Color(0xFFF59E0B)
                                                line.contains("DEDUPLICATED") -> Color(0xFFF59E0B)
                                                line.contains("APPROVED") -> Color(0xFF10B981)
                                                else -> TextSilver
                                            },
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                containerColor = ObsidianSurface
            )
        }

        // --- HORIZONTAL SPORTS FILTER CHIPS ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(staticSports) { sport ->
                val isSelected = (selectedFilter == sport) || (sport == "All Sports" && selectedFilter == null)
                val chipColor = if (isSelected) IndigoPrimary else Color.White.copy(alpha = 0.04f)
                val borderStroke = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipColor)
                        .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(20.dp)) else Modifier)
                        .clickable {
                            if (sport == "All Sports") {
                                viewModel.setSportFilter(null)
                            } else {
                                viewModel.setSportFilter(sport)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (sport == "All Sports") {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            val dotColor = when (sport) {
                                "Football" -> Color(0xFF10B981)
                                "UFC", "Boxing" -> Color(0xFFEF4444)
                                "Basketball" -> Color(0xFFF59E0B)
                                "Motorsport" -> Color(0xFF8B5CF6)
                                else -> Color(0xFF3B82F6)
                            }
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(dotColor, CircleShape)
                            )
                        }
                        Text(
                            text = sport,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

        // --- EMPTY STATE PROMPT ---
        if (filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No sports schedules found.",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try adjusting filters, clearing your search search queries, or checking back during live streams.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            // --- EVENT SCHEDULE LIST ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredEvents,
                    key = { it.id }
                ) { event ->
                    SportsEventCard(
                        event = event,
                        viewModel = viewModel,
                        onTuneClick = { channel ->
                            viewModel.startTrackingSportEvent(event)
                            viewModel.playIptvChannel(channel)
                            Toast.makeText(context, "Tuning to ${channel.name}...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SportsEventCard(
    event: SportsEvent,
    viewModel: MainViewModel,
    onTuneClick: (IptvChannel) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val status = event.getStatus()

    // Determine sport-colored glow background accent
    val accentColor = when (event.sport) {
        "Football" -> Color(0xFF10B981)
        "UFC", "Boxing" -> Color(0xFFEF4444)
        "Basketball" -> Color(0xFFF59E0B)
        "Motorsport" -> Color(0xFF8B5CF6)
        else -> Color(0xFF3B82F6)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (isExpanded) 0.05f else 0.03f)
        ),
        border = BorderStroke(
            1.dp, 
            if (isExpanded) accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Sport, League/Competition & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accentColor, CircleShape)
                    )
                    Text(
                        text = "${event.sport.uppercase()} • ${event.competition}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge
                val badgeContainerColor = when (status) {
                    "LIVE" -> Color.Red.copy(alpha = 0.15f)
                    "UPCOMING" -> IndigoPrimary.copy(alpha = 0.15f)
                    else -> Color.White.copy(alpha = 0.05f)
                }
                val badgeContentColor = when (status) {
                    "LIVE" -> Color.Red
                    "UPCOMING" -> IndigoPrimary
                    else -> TextMuted
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeContainerColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (status == "LIVE") {
                            // Pulsing dot simulation
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                        Text(
                            text = status,
                            color = badgeContentColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body Row: Title & Arrow indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Head-to-Head team logos if available, else plain title
                    if (!event.teamA.isNullOrBlank() && !event.teamB.isNullOrBlank() && (!event.teamABadge.isNullOrUrlEmpty() || !event.teamBBadge.isNullOrUrlEmpty())) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Team A
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!event.teamABadge.isNullOrUrlEmpty()) {
                                    AsyncImage(
                                        model = event.teamABadge,
                                        contentDescription = "${event.teamA} Badge",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = event.teamA,
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Team B
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!event.teamBBadge.isNullOrUrlEmpty()) {
                                    AsyncImage(
                                        model = event.teamBBadge,
                                        contentDescription = "${event.teamB} Badge",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = event.teamB,
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // Plain Title view
                        Text(
                            text = event.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = formatSportsEventTime(event.dateTimeMs),
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    if (isExpanded) {
                        if (!event.eventThumb.isNullOrUrlEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = event.eventThumb,
                                contentDescription = "${event.title} Banner",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }

                    if (event.description != null && isExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = event.description,
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand channels",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Expanding channels panel showing IPTV match results
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Matching IPTV Broadcasts",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val matchingChannels = remember(event) {
                        viewModel.getMatchingChannelsForEvent(event)
                    }

                    if (matchingChannels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Unable to find sports channels matching '${event.sport}' or team keywords in current channels playlist. Ensure EPG is fully loaded or reload sources.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            matchingChannels.forEach { match ->
                                MatchingChannelItem(
                                    match = match,
                                    onPlayClick = { onTuneClick(match.channel) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchingChannelItem(
    match: ChannelMatchResult,
    onPlayClick: () -> Unit
) {
    val confidenceColor = when (match.confidence) {
        MatchConfidence.EXACT -> Color(0xFF10B981) // Green
        MatchConfidence.LIKELY -> Color(0xFFF59E0B) // Amber
        MatchConfidence.POSSIBLE -> Color(0xFF3B82F6) // Blue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable { onPlayClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Channel Logo/Badge
            if (!match.channel.logoUrl.isNullOrUrlEmpty()) {
                AsyncImage(
                    model = match.channel.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(IndigoPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = if (match.channel.name.isNotBlank()) match.channel.name.take(1).uppercase() else "?"
                    Text(
                        text = initial,
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.channel.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${match.matchedReason}",
                    color = TextMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Match Confidence tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(confidenceColor.copy(alpha = 0.15f))
                    .border(0.5.dp, confidenceColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = match.confidence.name,
                    color = confidenceColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Tune to channel",
                    tint = IndigoPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// String null-and-empty check helper
private fun String?.isNullOrUrlEmpty(): Boolean {
    return this.isNullOrBlank() || this == "null"
}

// Relative formatted timezone-aware time helper
fun formatSportsEventTime(dateTimeMs: Long): String {
    val date = java.util.Date(dateTimeMs)
    val nowCalendar = java.util.Calendar.getInstance()
    
    val eventCalendar = java.util.Calendar.getInstance().apply {
        time = date
    }

    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val weekdayFormat = java.text.SimpleDateFormat("EEEE, HH:mm", java.util.Locale.getDefault())
    val fullDateFormat = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())

    val isToday = nowCalendar.get(java.util.Calendar.YEAR) == eventCalendar.get(java.util.Calendar.YEAR) &&
            nowCalendar.get(java.util.Calendar.DAY_OF_YEAR) == eventCalendar.get(java.util.Calendar.DAY_OF_YEAR)

    val isTomorrow = nowCalendar.get(java.util.Calendar.YEAR) == eventCalendar.get(java.util.Calendar.YEAR) &&
            (nowCalendar.get(java.util.Calendar.DAY_OF_YEAR) + 1) == eventCalendar.get(java.util.Calendar.DAY_OF_YEAR)

    // Check if within 6 days
    val isWithinUpcomingWeek = !isToday && !isTomorrow && (dateTimeMs - System.currentTimeMillis()) < 6 * 24 * 3600 * 1000L && (dateTimeMs > System.currentTimeMillis())

    return when {
        isToday -> "Today at ${timeFormat.format(date)}"
        isTomorrow -> "Tomorrow at ${timeFormat.format(date)}"
        isWithinUpcomingWeek -> weekdayFormat.format(date)
        else -> fullDateFormat.format(date)
    }
}
