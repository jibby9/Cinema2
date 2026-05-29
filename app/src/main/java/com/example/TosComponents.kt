package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Core palette matched with CineFold's Sophisticated Dark UI
private val AubergineDarkBg = Color(0xFF0C0A0F)
private val ObsidianSurface = Color(0xFF141218)
private val CineRed = Color(0xFFED4531)
private val GcmOrange = Color(0xFFFF512F)
private val GcmPink = Color(0xFFDD2476)
private val TextSilver = Color(0xFFE2E8F0)
private val TextMuted = Color(0xFF8A8F98)

@Composable
fun TosScreen(
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasScrolledToEnd by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Determine if the user has read a significant portion or if we should just enable accept immediately
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 50) {
            hasScrolledToEnd = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AubergineDarkBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Aesthetic ambient radial background shape
        Box(
            modifier = Modifier
                .size(350.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CineRed.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
                .align(Alignment.TopCenter)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 620.dp)
                .fillMaxHeight(0.9f)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(24.dp))
                .testTag("tos_card_container")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header (App Branding & Legal Title)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GcmOrange, GcmPink)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "CineFold legal terms",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Please accept the terms before utilizing the media player.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    
                    Divider(color = Color.White.copy(alpha = 0.06f))
                }

                // Scrollable Terms Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TosTextContent()
                    }
                }

                // Acceptance controls aligned at the bottom
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Safety / Security badge text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Locally stores IPTV credentials with hardware-level security",
                            color = Color(0xFF4ADE80).copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Accept Button
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CineRed,
                            contentColor = Color.White,
                            disabledContainerColor = CineRed.copy(alpha = 0.3f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("accept_tos_button")
                    ) {
                        Text(
                            text = "Accept & Continue",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TosDetailsDialog(
    onDismiss: () -> Unit,
    acceptedTimestamp: Long = 0L
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 520.dp)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(20.dp))
                .testTag("tos_review_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = CineRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Terms of Service",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (acceptedTimestamp > 0L) {
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(acceptedTimestamp))
                        Text(
                            text = "Accepted on: $dateFormatted",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TosTextContent()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = CineRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun PermissionsRationaleDialog(
    onDismiss: () -> Unit,
    onGrantTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 440.dp)
                .padding(16.dp)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(20.dp))
                .testTag("permissions_rational_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permission Feature Icon Highlight
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CineRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = CineRed,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title explanation
                Text(
                    text = "Enable Program Alerts?",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Detailed UX description of the exact need
                Text(
                    text = "CineFold asks for Notification settings to send you timely EPG program alerts and show a convenient playback controller in your device status bar while background streaming. Your alert schedule stays completely local and private.",
                    color = TextSilver,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                // Action controls side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Skip button
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text("Later", color = TextSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Confirm Permission trigger
                    Button(
                        onClick = {
                            onDismiss()
                            onGrantTrigger()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CineRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(40.dp)
                    ) {
                        Text("Grant Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TosTextContent() {
    val sections = listOf(
        "1. USER RELATION & SCOPE" to 
                "CineFold is a generic local multimedia aggregator and stream renderer. CineFold DOES NOT host, distribute, verify, or facilitate access to any subscription streams, playlists, channels, or program metadata. All credentials, Xtream server endpoints, and M3U playlist URLs are provided and loaded solely at the user's discretion, and users assume all corresponding liability.",
        "2. STORAGE CRYPTOGRAPHY" to 
                "To protect your IPTV accounts from scanning and malicious access, all login credentials, keys, and source configurations are encrypted transparently prior to hardware storage. Encryption keys are securely generated and isolated inside the hardware-backed Android Keystore system (AES-GCM-256). CineFold never logs, transmits, or uploads plain-text secrets.",
        "3. INTELLECTUAL PROPERTY & STREAMS" to 
                "CineFold does not evaluate the copyright standing of stream providers configured by users. You are legally obligated to verify that your configured server streams comply with your region's licensing and content reproduction laws.",
        "4. TELEMETRY & PRIVACY BOUNDS" to 
                "CineFold values user privacy and operations remain completely user-centric. Stream usage stats, favorite listings, and scheduled reminders are stored in your device's secured SQLite cache and encrypted DataStore files. Zero telemetry, tracking, or playlist data is harvested or exported.",
        "5. LIABILITY & NO WARRANTY" to 
                "The application is provided 'AS IS' without warranty of any kind. Under no circumstances shall development contributors be held liable for damages, service interruptions, or legal repercussions resulting from the operation of custom streams."
    )

    sections.forEach { (title, text) ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = text,
                color = TextSilver,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Justify
            )
        }
    }
}
