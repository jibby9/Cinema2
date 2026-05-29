package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Dark Theme Colors matching IPTV app style
private val IndigoPrimary = Color(0xFF6366F1)
private val ObsidianSurface = Color(0xFF0F172A)
private val TextMuted = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSportsTickerUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scoreState by viewModel.tickerScore.collectAsState(null)
    val statsList by viewModel.tickerStats.collectAsState(emptyList())
    val timelineList by viewModel.tickerTimeline.collectAsState(emptyList())
    val syncConfig by viewModel.tickerSyncConfig.collectAsState()
    val identifiedEvent by viewModel.identifiedSportsEvent.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedDetailsTab by remember { mutableStateOf(0) } // 0: Timeline, 1: Stats

    // Animated glow color for currently active sports
    val sportColor = when (scoreState?.sport) {
        "Football" -> Color(0xFF10B981)
        "UFC", "Boxing" -> Color(0xFFEF4444)
        "Basketball" -> Color(0xFFF59E0B)
        "Motorsport" -> Color(0xFF8B5CF6)
        else -> IndigoPrimary
    }

    if (scoreState == null) {
        // Render a subtle info message if player is playing general sport channels but no specific show is correlated yet
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Live Sports Ticker scanning active broadcast for EPG events... Pair a fixture via the Sports tab to calibrate.",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val score = scoreState!!

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        // --- 1. CORE COMPACT SCORE TICKER RIBBON ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Game & Live indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing dot simulation
                var dotVisible by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(800)
                        dotVisible = !dotVisible
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (syncConfig.isEnabled) Color.Red.copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (dotVisible) (if (syncConfig.isEnabled) Color.Red else Color(0xFF10B981)) else Color.Transparent,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (syncConfig.isEnabled) "SYNCED (${syncConfig.delaySeconds}s Delay)" else "REAL-TIME",
                            color = if (syncConfig.isEnabled) Color.Red else Color(0xFF10B981),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "${score.sport.uppercase()} • ${score.gameClock}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Center: Score / Versus Board
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = score.teamA,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 110.dp)
                )

                // Score numbers Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(sportColor.copy(alpha = 0.2f))
                        .border(0.5.dp, sportColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${score.scoreA} - ${score.scoreB}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = score.teamB,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 110.dp)
                )
            }

            // Right: Detail expanders & Settings Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Timeline Toggle Button
                FilledIconButton(
                    onClick = { selectedDetailsTab = 0 },
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (selectedDetailsTab == 0) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                        contentColor = if (selectedDetailsTab == 0) Color.White else TextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Event Timeline",
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Stats Toggle Button
                FilledIconButton(
                    onClick = { selectedDetailsTab = 1 },
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (selectedDetailsTab == 1) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                        contentColor = if (selectedDetailsTab == 1) Color.White else TextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Match Stats",
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Configure Delay Button
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Calibration Settings",
                        tint = if (syncConfig.isEnabled) IndigoPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

        // --- 2. EXTENDED COLLAPSIBLE CONTENT CAROUSEL ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.015f))
                .padding(vertical = 6.dp)
        ) {
            if (selectedDetailsTab == 0) {
                // Event Timeline View
                if (timelineList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Awaiting live match events... Ticker is synchronized and calibrated.",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Display the most recent timeline events from right to left
                        items(timelineList.reversed()) { epgEvent ->
                            val isGoal = epgEvent.title.uppercase() == "GOAL!" || epgEvent.title.contains("KO")
                            val itemBorderColor = if (isGoal) sportColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
                            val itemBgColor = if (isGoal) sportColor.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(itemBgColor)
                                    .border(0.5.dp, itemBorderColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isGoal) sportColor else Color.White.copy(alpha = 0.06f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = epgEvent.gameTime,
                                            color = if (isGoal) Color.Black else TextMuted,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = epgEvent.title,
                                            color = if (isGoal) sportColor else Color.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = epgEvent.detail,
                                            color = TextMuted,
                                            fontSize = 8.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Key Stats View
                if (statsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No stats loaded for this sport currently.",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(statsList) { stat ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.widthIn(min = 90.dp)
                            ) {
                                Text(
                                    text = stat.label,
                                    color = TextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stat.valueA,
                                        color = sportColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(2.dp)
                                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    )
                                    Text(
                                        text = stat.valueB,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 3. SPOILER-FREE DELAY SYNCHRONIZATION DIALOG ---
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = IndigoPrimary
                    )
                    Text(
                        text = "Spoiler-Safe Controls",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Calibrate data delivery to prevent score spoilers! If your IPTV video feed is slightly delayed, shift this slider to sync the score updates.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // Spoiler Safe Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Enable Spoiler-Safe Mode",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = syncConfig.isEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.sportsTickerManager.updateConfig(syncConfig.copy(isEnabled = isChecked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = IndigoPrimary,
                                checkedTrackColor = IndigoPrimary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (syncConfig.isEnabled) {
                        // Slider for configuring delay
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "IPTV Stream Delay",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${syncConfig.delaySeconds} seconds behind",
                                    color = IndigoPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = syncConfig.delaySeconds.toFloat(),
                                onValueChange = { newVal ->
                                    viewModel.sportsTickerManager.updateConfig(syncConfig.copy(delaySeconds = newVal.toInt()))
                                },
                                valueRange = 0f..120f,
                                steps = 24,
                                colors = SliderDefaults.colors(
                                    thumbColor = IndigoPrimary,
                                    activeTrackColor = IndigoPrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                )
                            )
                        }

                        // Steppers for quick +5 / -5 seconds calibration checks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val currentD = syncConfig.delaySeconds
                                    viewModel.sportsTickerManager.updateConfig(
                                        syncConfig.copy(delaySeconds = maxOf(0, currentD - 5))
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Text("-5s Sec", color = Color.White, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val currentD = syncConfig.delaySeconds
                                    viewModel.sportsTickerManager.updateConfig(
                                        syncConfig.copy(delaySeconds = minOf(120, currentD + 5))
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Text("+5s Sec", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        // Instant Calibrate / Recalibration Action
                        Button(
                            onClick = {
                                viewModel.sportsTickerManager.triggerRecalibrate()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recalibrate / Sync Buffers", fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.05f))

                    // Position Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Ticker Display Position",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(TickerPosition.TOP, TickerPosition.BOTTOM).forEach { pos ->
                                val selected = syncConfig.tickerPosition == pos
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) IndigoPrimary else Color.White.copy(alpha = 0.03f))
                                        .clickable {
                                            viewModel.sportsTickerManager.updateConfig(syncConfig.copy(tickerPosition = pos))
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (pos == TickerPosition.TOP) "Above Video Player" else "Below Video Player",
                                        color = if (selected) Color.White else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
