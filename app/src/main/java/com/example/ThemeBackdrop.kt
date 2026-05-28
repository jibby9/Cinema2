package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
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
    isAnimationEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val themeId = themePreset.id
    val isAnim = isAnimationEnabled && !isEditMode
    
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_effects")
    
    // Projector flicker for cinema or fire heat glow for cabin
    val glowPulse by if (isAnim) {
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val projectorPulse by if (isAnim) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(150, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "projector_pulse"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    // Ethereal flow for Aurora theme
    val auroraProgress by if (isAnim) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "aurora_progress"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Falling code for Matrix Rain theme
    val matrixProgress by if (isAnim) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "matrix_progress"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Slow drift & twinkle for Cosmic Stardust
    val stardustProgress by if (isAnim) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "stardust_progress"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val stardustTwinkle by if (isAnim) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "stardust_twinkle"
        )
    } else {
        remember { mutableStateOf(0.7f) }
    }

    // Star particles list generated deterministically inside a remember block
    val starParticles = remember {
        List(28) {
            val xr = 0.05f + 0.9f * (Math.random().toFloat())
            val yr = 0.05f + 0.9f * (Math.random().toFloat())
            val size = 1.5f + 3f * (Math.random().toFloat())
            val speed = 0.3f + 0.7f * (Math.random().toFloat())
            val cycleOffset = Math.random().toFloat() * 2f * Math.PI.toFloat()
            StarParticle(xr, yr, size, speed, cycleOffset)
        }
    }

    // Decorative opacity is scaled down in Edit Mode to help placement accuracy
    val decorMultiplier = if (isEditMode) 0.25f else 1.0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030204)) // Absolute base deep contrast
    ) {
        when (themeId.lowercase()) {
            "aurora" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // 1. Deep Celestial Background
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF02021E), // Near black space
                                Color(0xFF0D0A2A), // Dark starfield purple
                                Color(0xFF04020F)  // Vacuum bottom
                            )
                        )
                    )

                    // 2. Animated Aurora Ribbons (paths with translucent radial/linear gradients)
                    // Aurora Green Ribbon
                    val p1 = Path()
                    p1.moveTo(0f, height * 0.45f)
                    for (x in 0..width.toInt() step 40) {
                        val phase = (x.toFloat() / width) * 2.5f * Math.PI.toFloat() + auroraProgress
                        val y = height * 0.45f + Math.sin(phase.toDouble()).toFloat() * 120f * decorMultiplier
                        p1.lineTo(x.toFloat(), y)
                    }
                    p1.lineTo(width, height)
                    p1.lineTo(0f, height)
                    p1.close()

                    drawPath(
                        path = p1,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x0010B981),
                                Color(0x3B10B981).copy(alpha = 0.25f * glowPulse * decorMultiplier),
                                Color(0x1B0D9488).copy(alpha = 0.12f * decorMultiplier),
                                Color.Transparent
                            ),
                            startY = height * 0.3f,
                            endY = height
                        )
                    )

                    // Aurora Violet / Indigo Ribbon
                    val p2 = Path()
                    p2.moveTo(0f, height * 0.35f)
                    for (x in 0..width.toInt() step 50) {
                        val phase = (x.toFloat() / width) * 1.8f * Math.PI.toFloat() - auroraProgress * 1.2f
                        val y = height * 0.32f + Math.cos(phase.toDouble()).toFloat() * 90f * decorMultiplier
                        p2.lineTo(x.toFloat(), y)
                    }
                    p2.lineTo(width, height)
                    p2.lineTo(0f, height)
                    p2.close()

                    drawPath(
                        path = p2,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x00A855F7),
                                Color(0x306366F1).copy(alpha = 0.20f * decorMultiplier),
                                Color(0x153B82F6),
                                Color.Transparent
                            ),
                            startY = height * 0.2f,
                            endY = height * 0.9f
                        )
                    )

                    // Warm horizon aurora base glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF14B8A6).copy(alpha = 0.15f * glowPulse * decorMultiplier),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.7f, height * 0.4f),
                            radius = width * 0.5f
                        )
                    )
                }
            }

            "matrix" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Cosmic black monitor base
                    drawRect(color = Color(0xFF020503))

                    // Falling terminal stream lines (18 tracks)
                    val tracksCount = 18
                    val gap = width / tracksCount
                    for (c in 0 until tracksCount) {
                        val offsetFrac = (c * 0.17f) % 1.0f
                        val speedFrac = 0.7f + (c % 5) * 0.2f
                        
                        // Current leading y coordinate
                        val trackingY = (height * (matrixProgress * speedFrac + offsetFrac)) % (height + 300f) - 150f

                        // Draw falling indicator line
                        if (trackingY > -200f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF00FF41).copy(alpha = 0.08f * decorMultiplier),
                                        Color(0xFF00FF41).copy(alpha = 0.40f * decorMultiplier)
                                    ),
                                    startY = trackingY - 180f,
                                    endY = trackingY
                                ),
                                topLeft = Offset(c * gap + gap/2f - 1.5f, trackingY - 180f),
                                size = Size(3f, 180f)
                            )

                            // Bright digital neon drop point
                            drawCircle(
                                color = Color.White.copy(alpha = 0.95f * decorMultiplier),
                                center = Offset(c * gap + gap/2f, trackingY),
                                radius = 2.5f
                            )
                            drawCircle(
                                color = Color(0xFF00FF41).copy(alpha = 0.6f * decorMultiplier),
                                center = Offset(c * gap + gap/2f, trackingY),
                                radius = 6f
                            )
                        }
                    }
                }
            }

            "stardust" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Space Deep Vacuum
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF131032), // star nursery nebula violet
                                Color(0xFF020208)
                            ),
                            center = Offset(width * 0.5f, height * 0.5f),
                            radius = width * 0.75f
                        )
                    )

                    // Draw Twinkling Star Objects with floating orbital drift
                    starParticles.forEachIndexed { idx, star ->
                        val driftX = Math.sin(stardustProgress.toDouble() * 0.3 + star.cycleOffset).toFloat() * 12f * decorMultiplier
                        val driftY = Math.cos(stardustProgress.toDouble() * 0.2 + star.cycleOffset).toFloat() * 12f * decorMultiplier
                        
                        val startX = star.xr * width + driftX
                        val startY = star.yr * height + driftY

                        // Local pulsing scale
                        val phaseTwinkle = Math.sin(stardustProgress.toDouble() * star.speed + star.cycleOffset).toFloat()
                        val scale = (0.4f + 0.6f * ((phaseTwinkle + 1f) / 2f)) * stardustTwinkle * decorMultiplier

                        // Outer glowing gas
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE2E8F0).copy(alpha = 0.35f * scale),
                                    Color.Transparent
                                ),
                                center = Offset(startX, startY),
                                radius = star.size * 5f
                            )
                        )

                        // Deep Core Star
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f * scale),
                            center = Offset(startX, startY),
                            radius = star.size * 0.8f
                        )
                    }
                }
            }

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
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFEC4899).copy(alpha = 0.18f),
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
                                Color(0xFF1E1B4B).copy(alpha = 0.40f),
                                Color(0xFF030204)
                            ),
                            center = Offset(width / 2f, height / 2f),
                            radius = width * 0.85f
                        )
                    )

                    // 2. Real-time Projector beam
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
                                Color(0xFF818CF8).copy(alpha = 0.22f * projectorPulse * decorMultiplier),
                                Color(0xFF4F46E5).copy(alpha = 0.06f * projectorPulse * decorMultiplier),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // 3. Render seat rows outlines silhouette at bottom
                    val rowY = height * 0.92f
                    drawLine(
                        color = Color(0xFF312E81).copy(alpha = 0.20f * decorMultiplier),
                        start = Offset(0f, rowY),
                        end = Offset(width, rowY),
                        strokeWidth = 24f
                    )
                    
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

// Helper models for Stardust particles
private data class StarParticle(
    val xr: Float,
    val yr: Float,
    val size: Float,
    val speed: Float,
    val cycleOffset: Float
)
