package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import android.util.Log

@Composable
fun ThemeBackdrop(
    themePreset: ThemePreset,
    isEditMode: Boolean = false,
    customBackgroundUri: String? = null,
    modifier: Modifier = Modifier
) {
    val themeId = themePreset.id
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_effects")
    
    // Projector flicker for cinema or fire heat glow for cabin
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val projectorPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "projector_pulse"
    )

    // Decorative opacity is scaled down in Edit Mode to help placement accuracy
    val decorMultiplier = if (isEditMode) 0.25f else 1.0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030204)) // Absolute base deep contrast
    ) {
        when (themeId.lowercase()) {
            "cosy_cabin" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // 1. Warm wooden panelling background gradient (slightly subdued in edit mode)
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1C0A02), // Dark wood shadow
                                Color(0xFF2E1205), // Timber brown
                                Color(0xFF1F0B03)  // Deep brown corner
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(width, height)
                        ),
                        alpha = if (isEditMode) 0.5f else 1.0f
                    )

                    // 2. Cabin wood logs / planks grid simulation
                    val totalPlanks = 8
                    val plankHeight = height / totalPlanks
                    for (i in 1 until totalPlanks) {
                        val y = i * plankHeight
                        drawLine(
                            color = Color(0xFF0D0300).copy(alpha = 0.8f * decorMultiplier),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 3f
                        )
                        // Bevel highlights
                        drawLine(
                            color = Color(0xFF421C0B).copy(alpha = 0.4f * decorMultiplier),
                            start = Offset(0f, y + 2f),
                            end = Offset(width, y + 2f),
                            strokeWidth = 1f
                        )
                    }

                    // Log notches vertical dividers
                    for (x in listOf(width * 0.15f, width * 0.85f)) {
                        drawLine(
                            color = Color(0xFF0D0300).copy(alpha = 0.9f * decorMultiplier),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = Color(0xFF5C2D16).copy(alpha = 0.3f * decorMultiplier),
                            start = Offset(x + 4f, 0f),
                            end = Offset(x + 4f, height),
                            strokeWidth = 2f
                        )
                    }

                    // 3. Fireplace glow: radiating warm hearth firelight (Bottom Center)
                    val hearthCenter = Offset(width * 0.5f, height)
                    val fireplaceGlow = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEA580C).copy(alpha = 0.55f * glowPulse * decorMultiplier), // Vibrant Orange
                            Color(0xFF7C2D12).copy(alpha = 0.25f * decorMultiplier),             // Burnt Umber
                            Color.Transparent
                        ),
                        center = hearthCenter,
                        radius = width * 0.70f
                    )
                    drawRect(brush = fireplaceGlow)

                    // Warm corner highlights
                    drawCircle(
                        color = Color(0xFFF97316).copy(alpha = 0.12f * glowPulse * decorMultiplier),
                        center = Offset(0f, height),
                        radius = width * 0.3f
                    )
                    drawCircle(
                        color = Color(0xFFF97316).copy(alpha = 0.12f * glowPulse * decorMultiplier),
                        center = Offset(width, height),
                        radius = width * 0.3f
                    )
                }
            }

            "sports_arena" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // 1. Deep stadium field background (Emerald night) (slightly subdued in edit mode)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF021D12), // Deep stadium dark sky
                                Color(0xFF064E3B), // Soft sports field green
                                Color(0xFF042F22)  // Low shadow grass
                            )
                        ),
                        alpha = if (isEditMode) 0.5f else 1.0f
                    )

                    // 2. Linear turf details (mowed grass lines)
                    val stripCount = 12
                    val stripWidth = width / stripCount
                    for (i in 0 until stripCount) {
                        if (i % 2 == 0) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.02f * decorMultiplier),
                                topLeft = Offset(i * stripWidth, 0f),
                                size = Size(stripWidth, height)
                            )
                        }
                    }

                    // Soccer / stadium chalk lines
                    // Midfield circle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f * decorMultiplier),
                        center = Offset(width / 2f, height),
                        radius = width * 0.22f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                    // Touchline bottom
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f * decorMultiplier),
                        start = Offset(0f, height - 10f),
                        end = Offset(width, height - 10f),
                        strokeWidth = 4f
                    )

                    // 3. Floodlight beam sports lighting (Corner Spotlights)
                    val leftSpotlight = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(width * 0.45f, height)
                        lineTo(width * 0.15f, height)
                        close()
                    }
                    drawPath(
                        path = leftSpotlight,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE0F2FE).copy(alpha = 0.40f * decorMultiplier), // Ice Blue Floodlight
                                Color(0xFF10B981).copy(alpha = 0.05f * decorMultiplier), // Stadium glow blend
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(width * 0.3f, height)
                        )
                    )

                    val rightSpotlight = Path().apply {
                        moveTo(width, 0f)
                        lineTo(width * 0.85f, height)
                        lineTo(width * 0.55f, height)
                        close()
                    }
                    drawPath(
                        path = rightSpotlight,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE0F2FE).copy(alpha = 0.40f * decorMultiplier),
                                Color(0xFF10B981).copy(alpha = 0.05f * decorMultiplier),
                                Color.Transparent
                            ),
                            start = Offset(width, 0f),
                            end = Offset(width * 0.7f, height)
                        )
                    )

                    // Little bright flares in upper corners representing stadium matrix lights
                    drawCircle(
                        color = Color(0xFFF8FAFC).copy(alpha = decorMultiplier),
                        center = Offset(15f, 15f),
                        radius = 12f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = decorMultiplier), Color.Transparent),
                            center = Offset(15f, 15f),
                            radius = 60f
                        )
                    )

                    drawCircle(
                        color = Color(0xFFF8FAFC).copy(alpha = decorMultiplier),
                        center = Offset(width - 15f, 15f),
                        radius = 12f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = decorMultiplier), Color.Transparent),
                            center = Offset(width - 15f, 15f),
                            radius = 60f
                        )
                    )
                }
            }

            "custom" -> {
                if (!customBackgroundUri.isNullOrBlank()) {
                    // Modern Coil AsyncImage for loading custom background image from internal storage or URI
                    coil.compose.AsyncImage(
                        model = customBackgroundUri,
                        contentDescription = "Custom theme backdrop background",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { errorState ->
                            Log.e("ThemeBackdrop", "Failed to load custom background image", errorState.result.throwable)
                        }
                    )
                } else {
                    // Fallback background layout if custom background is not loaded
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFEC4899).copy(alpha = 0.18f), // Soft dark pink ambient glow
                                    Color(0xFF030204)
                                ),
                                center = Offset(width / 2f, height / 2f),
                                radius = width * 0.85f
                            )
                        )
                    }
                }
            }

            else -> { // Default "cinema"
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // 1. Deep Aubergine / Indigo premium dark cinema theater walls
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E1B4B).copy(alpha = 0.40f), // Soft dark purple ambient glow
                                Color(0xFF030204)
                            ),
                            center = Offset(width / 2f, height / 2f),
                            radius = width * 0.85f
                        )
                    )

                    // 2. Real-time Projector beam (shining down from top-center of the theater!)
                    val projectorBeam = Path().apply {
                        moveTo(width / 2f - 40f, 0f)
                        lineTo(width / 2f + 40f, 0f)
                        lineTo(width * 0.95f, height)
                        lineTo(width * 0.05f, height)
                        close()
                    }
                    drawPath(
                        path = projectorBeam,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF818CF8).copy(alpha = 0.22f * projectorPulse * decorMultiplier), // Indigo projector bulbs
                                Color(0xFF4F46E5).copy(alpha = 0.06f * projectorPulse * decorMultiplier), // Light violet scatter
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // 3. Render seat rows outlines silhouette at the bottom (opacity 25%)
                    val rowY = height * 0.92f
                    drawLine(
                        color = Color(0xFF312E81).copy(alpha = 0.20f * decorMultiplier),
                        start = Offset(0f, rowY),
                        end = Offset(width, rowY),
                        strokeWidth = 24f
                    )
                    // Chair silhouettes
                    val chairWidth = 32f
                    val chairSpacing = 16f
                    var currentX = chairSpacing
                    while (currentX < width) {
                        drawRoundRect(
                            color = Color(0xFF1E1B4B).copy(alpha = 0.35f * decorMultiplier),
                            topLeft = Offset(currentX, rowY - 12f),
                            size = Size(chairWidth, 20f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        currentX += chairWidth + chairSpacing
                    }
                }
            }
        }

        // 4. Overlap Ambient Theme Color Tint Layer
        val finalTint = themePreset.ambientColorTint.copy(
            alpha = themePreset.ambientColorTint.alpha * if (isEditMode) 0.3f else 1.0f
        )
        if (finalTint != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(finalTint)
            )
        }

        // 5. Draw gorgeous Premium Cinematic Vignette Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val strength = themePreset.vignetteStrength * if (isEditMode) 0.4f else 1.0f

            if (strength > 0f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = strength * 0.3f),
                            1.0f to Color.Black.copy(alpha = strength)
                        ),
                        center = Offset(width / 2f, height / 2f),
                        radius = java.lang.Math.hypot(width.toDouble(), height.toDouble()).toFloat() * 0.65f
                    )
                )
            }
        }
    }
}
