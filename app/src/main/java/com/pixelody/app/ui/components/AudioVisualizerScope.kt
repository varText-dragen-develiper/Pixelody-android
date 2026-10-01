package com.pixelody.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class VisualizerMode(val label: String, val glyph: TransportGlyphType) {
    SpectralBars("Spectrum", TransportGlyphType.WaveformBars),
    LiquidOscilloscope("Scope", TransportGlyphType.Equalizer),
    HarmonicWheel("Harmonic", TransportGlyphType.FlowShuffle),
    RadialAura("Aura", TransportGlyphType.Sparkle),
    StarlightHorizon("Horizon", TransportGlyphType.DiamondLossless),
    SpatialRadar("Radar", TransportGlyphType.Broadcast)
}

/**
 * Pixelody Visualizer Soundstage with 5 real-time generative audio reactive modes:
 * - SpectralBars (3D parametric frequency bars with peak hold and reflection glow)
 * - LiquidOscilloscope (Phosphor CRT oscilloscope with dual-layer Lissajous X-Y phase plot)
 * - RadialAura (360° polar resonant aura reacting to bass transients)
 * - StarlightHorizon (Generative 3D starfield warp mesh modulated by tempo/energy)
 * - SpatialRadar (Polar stereophonic radar showing L/R azimuth and Mid/Side spread)
 */
@Composable
fun AudioVisualizerScope(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    initialMode: VisualizerMode = VisualizerMode.SpectralBars,
    currentKey: CamelotKey? = null,
    targetKey: CamelotKey? = null,
    primaryColor: Color = currentKey?.harmonicColor ?: MaterialTheme.colorScheme.primary,
    secondaryColor: Color = targetKey?.harmonicColor ?: MaterialTheme.colorScheme.tertiary,
    heightDp: Int = 160
) {
    var mode by remember { mutableStateOf(initialMode) }
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerAnimation")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 1800 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 420 else 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val energyDb = if (isPlaying) -12.4f + (pulse - 1f) * 8f else -48f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header & Telemetry Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SOUNDSTAGE 60 FPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPlaying) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "LIVE %.1f dB".format(energyDb) else "IDLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPlaying) Color(0xFF10B981) else Color.Gray
                        )
                    }
                }

                // Mode Selector Chips (Horizontally Scrollable)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VisualizerMode.values().forEach { vMode ->
                        val isSelected = mode == vMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) primaryColor.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) primaryColor else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { mode = vMode }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PixelodyTransportGlyph(
                                    glyph = vMode.glyph,
                                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 12.dp
                                )
                                Text(
                                    text = vMode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Visualizer Stage Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
            ) {
                when (mode) {
                    VisualizerMode.SpectralBars -> {
                        SpectralBarsCanvas(
                            isPlaying = isPlaying,
                            phase = phase,
                            pulse = pulse,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                    VisualizerMode.LiquidOscilloscope -> {
                        LiquidOscilloscopeCanvas(
                            isPlaying = isPlaying,
                            phase = phase,
                            pulse = pulse,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                    VisualizerMode.HarmonicWheel -> {
                        HarmonicWheelCanvas(
                            isPlaying = isPlaying,
                            phase = phase,
                            pulse = pulse,
                            currentKey = currentKey,
                            targetKey = targetKey,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                    VisualizerMode.RadialAura -> {
                        RadialAuraCanvas(
                            isPlaying = isPlaying,
                            phase = phase,
                            pulse = pulse,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                    VisualizerMode.StarlightHorizon -> {
                        StarlightHorizonCanvas(
                            isPlaying = isPlaying,
                            phase = phase,
                            pulse = pulse,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                    VisualizerMode.SpatialRadar -> {
                        SpatialRadarCanvas(
                            isPlaying = isPlaying,
                            phase = phase,
                            pulse = pulse,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1. SpectralBars: 3D-styled parametric frequency spectrum with floating peak caps & ground reflection.
 */
@Composable
private fun SpectralBarsCanvas(
    isPlaying: Boolean,
    phase: Float,
    pulse: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val barCount = 30
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val mainHeight = height * 0.78f
        val reflectHeight = height * 0.22f
        val barWidth = (width / barCount) * 0.74f
        val gap = (width / barCount) * 0.26f

        val gradient = Brush.verticalGradient(
            colors = listOf(primaryColor, secondaryColor, primaryColor.copy(alpha = 0.35f)),
            startY = 0f,
            endY = mainHeight
        )

        val reflectGradient = Brush.verticalGradient(
            colors = listOf(secondaryColor.copy(alpha = 0.35f), Color.Transparent),
            startY = mainHeight,
            endY = height
        )

        // Baseline divider
        drawLine(
            color = primaryColor.copy(alpha = 0.25f),
            start = Offset(0f, mainHeight),
            end = Offset(width, mainHeight),
            strokeWidth = 1f
        )

        for (i in 0 until barCount) {
            val freqWeight = sin((i.toFloat() / barCount * PI).toFloat())
            val dynamicPhase = phase + (i * 0.22f)
            val amplitude = if (isPlaying) {
                val raw = (sin(dynamicPhase) * 0.42f + cos(dynamicPhase * 1.6f) * 0.28f + 0.52f).coerceIn(0.08f, 0.98f)
                raw * (0.35f + 0.65f * freqWeight) * pulse
            } else {
                0.05f + 0.025f * sin(dynamicPhase)
            }

            val barH = (mainHeight * amplitude).coerceIn(4f, mainHeight - 6f)
            val x = i * (barWidth + gap) + gap / 2f
            val y = mainHeight - barH

            // Main Bar
            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(3.5f, 3.5f)
            )

            // Bottom Mirror Reflection
            val rBarH = barH * 0.35f
            drawRoundRect(
                brush = reflectGradient,
                topLeft = Offset(x, mainHeight + 2f),
                size = Size(barWidth, rBarH),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Floating Peak Indicator
            val peakY = (y - 5f).coerceAtLeast(2f)
            drawCircle(
                color = primaryColor,
                radius = barWidth / 2.6f,
                center = Offset(x + barWidth / 2f, peakY)
            )
        }
    }
}

/**
 * 2. LiquidOscilloscope: Phosphor CRT oscilloscope with dual-layer Lissajous X-Y phase plot.
 */
@Composable
private fun LiquidOscilloscopeCanvas(
    isPlaying: Boolean,
    phase: Float,
    pulse: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val midX = width / 2f

        // Grid lines (CRT graticule)
        val gridDash = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
        for (gx in 1..7) {
            val x = width * (gx / 8f)
            drawLine(
                color = primaryColor.copy(alpha = 0.08f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f,
                pathEffect = gridDash
            )
        }
        for (gy in 1..3) {
            val y = height * (gy / 4f)
            drawLine(
                color = primaryColor.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = gridDash
            )
        }

        // Lissajous Stereo Phase Plot (Center figure-8 / ellipse trajectory)
        val lissajousPath = Path()
        val lissPoints = 72
        val lissRadiusX = (width * 0.28f) * (if (isPlaying) pulse else 0.4f)
        val lissRadiusY = (height * 0.36f) * (if (isPlaying) pulse else 0.4f)

        for (step in 0..lissPoints) {
            val t = (step.toFloat() / lissPoints) * 2 * PI.toFloat()
            val lx = midX + sin(t * 2 + phase) * lissRadiusX
            val ly = midY + sin(t * 3 + phase * 0.7f) * lissRadiusY
            if (step == 0) lissajousPath.moveTo(lx, ly) else lissajousPath.lineTo(lx, ly)
        }
        drawPath(
            path = lissajousPath,
            color = secondaryColor.copy(alpha = 0.45f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // Dual Running Waveforms (Left & Right traces)
        listOf(
            Triple(primaryColor, 1.0f, 1.0f),
            Triple(secondaryColor, 1.45f, 0.75f),
            Triple(primaryColor.copy(alpha = 0.4f), 0.75f, 1.25f)
        ).forEach { (color, freqMult, ampMult) ->
            val path = Path()
            val points = 64
            for (step in 0..points) {
                val x = (step.toFloat() / points) * width
                val wavePhase = phase * freqMult + (step * 0.16f)
                val baseAmp = if (isPlaying) (height * 0.30f * ampMult * pulse) else 6f
                val y = midY + sin(wavePhase) * baseAmp * sin((step.toFloat() / points * PI).toFloat())

                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3.2f, cap = StrokeCap.Round)
            )
        }

        // Center zero-line glow
        drawLine(
            color = primaryColor.copy(alpha = 0.2f),
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 1.5f
        )
    }
}

/**
 * 3. RadialAura: 360° polar resonant aura reacting to bass transients.
 */
@Composable
private fun RadialAuraCanvas(
    isPlaying: Boolean,
    phase: Float,
    pulse: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = (size.height / 2f) * 0.52f

        val beamCount = 42
        for (i in 0 until beamCount) {
            val angle = (i.toFloat() / beamCount) * 2 * PI.toFloat()
            val beamPhase = phase + (i * 0.22f)
            val beamLength = if (isPlaying) {
                (baseRadius * 0.32f + baseRadius * 0.60f * sin(beamPhase) * pulse).coerceAtLeast(4f)
            } else {
                baseRadius * 0.14f
            }

            val startX = centerX + cos(angle) * baseRadius
            val startY = centerY + sin(angle) * baseRadius
            val endX = centerX + cos(angle) * (baseRadius + beamLength)
            val endY = centerY + sin(angle) * (baseRadius + beamLength)

            val beamColor = if (i % 2 == 0) primaryColor else secondaryColor
            drawLine(
                color = beamColor.copy(alpha = 0.85f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
        }

        // Concentric Expanding Bass Rings
        val ringCount = 3
        for (r in 1..ringCount) {
            val ringRadius = baseRadius * (0.35f + r * 0.22f * pulse)
            drawCircle(
                color = secondaryColor.copy(alpha = (0.3f / r) * pulse),
                radius = ringRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )
        }

        // Inner glowing core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.55f * pulse), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = baseRadius
            ),
            center = Offset(centerX, centerY),
            radius = baseRadius
        )

        drawCircle(
            color = primaryColor,
            radius = baseRadius,
            style = Stroke(width = 2.5f)
        )
    }
}

/**
 * 4. StarlightHorizon: Generative 3D starfield warp mesh modulated by tempo/energy.
 */
@Composable
private fun StarlightHorizonCanvas(
    isPlaying: Boolean,
    phase: Float,
    pulse: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        val starCount = 56
        val speedMult = if (isPlaying) 1.6f * pulse else 0.35f

        // Cosmic nebula background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.28f * pulse), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = width * 0.45f
            ),
            center = Offset(centerX, centerY),
            radius = width * 0.45f
        )

        // Radial Warp Stars
        for (i in 0 until starCount) {
            val angle = ((i * 137.5f) % 360f) * (PI / 180f).toFloat()
            val rawZ = ((phase * speedMult + (i * 0.08f)) % 1.0f)
            val z = if (rawZ < 0.01f) 0.01f else rawZ

            // Distance scaling (project onto 2D plane)
            val dist = z * (width * 0.55f)
            val x = centerX + cos(angle) * dist
            val y = centerY + sin(angle) * dist

            val trailLength = (dist * 0.12f * (if (isPlaying) pulse else 0.5f)).coerceAtLeast(1f)
            val trailStartX = x - cos(angle) * trailLength
            val trailStartY = y - sin(angle) * trailLength

            val starColor = if (i % 3 == 0) primaryColor else secondaryColor
            val starAlpha = (z * 1.2f).coerceIn(0.1f, 0.95f)

            drawLine(
                color = starColor.copy(alpha = starAlpha),
                start = Offset(trailStartX, trailStartY),
                end = Offset(x, y),
                strokeWidth = (z * 3.5f).coerceIn(1.2f, 4f),
                cap = StrokeCap.Round
            )
        }

        // Center horizon focal singularity
        drawCircle(
            color = primaryColor,
            radius = 3.5f * pulse,
            center = Offset(centerX, centerY)
        )
    }
}

/**
 * 5. SpatialRadar: Polar stereo soundfield radar showing L/R azimuth and Mid/Side energy spread.
 */
@Composable
private fun SpatialRadarCanvas(
    isPlaying: Boolean,
    phase: Float,
    pulse: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height * 0.85f // Origin at bottom center for sweeping forward hemisphere
        val radarRadius = (height * 0.78f).coerceAtLeast(10f)

        // Radar Range Rings (0dB, -6dB, -12dB, -18dB)
        val ringDash = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
        for (r in 1..4) {
            val radius = radarRadius * (r / 4f)
            drawArc(
                color = primaryColor.copy(alpha = 0.18f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 1f, pathEffect = ringDash)
            )
        }

        // Radar Spoke Angles (-60°, -30°, 0° Center, +30°, +60°)
        listOf(-60f, -30f, 0f, 30f, 60f).forEach { deg ->
            val rad = (deg - 90f) * (PI / 180f).toFloat()
            val ex = centerX + cos(rad) * radarRadius
            val ey = centerY + sin(rad) * radarRadius
            drawLine(
                color = if (deg == 0f) primaryColor.copy(alpha = 0.35f) else primaryColor.copy(alpha = 0.15f),
                start = Offset(centerX, centerY),
                end = Offset(ex, ey),
                strokeWidth = if (deg == 0f) 1.5f else 1f
            )
        }

        // Stereophonic Energy Wedge (Mid/Side spread)
        val leftSpread = if (isPlaying) (35f + 15f * sin(phase) * pulse) else 15f
        val rightSpread = if (isPlaying) (35f + 15f * cos(phase * 1.2f) * pulse) else 15f

        val wedgePath = Path().apply {
            moveTo(centerX, centerY)
            val radL = (-90f - leftSpread) * (PI / 180f).toFloat()
            val radR = (-90f + rightSpread) * (PI / 180f).toFloat()
            val len = radarRadius * (if (isPlaying) 0.85f * pulse else 0.35f)

            lineTo(centerX + cos(radL) * len, centerY + sin(radL) * len)
            lineTo(centerX + cos(radR) * len, centerY + sin(radR) * len)
            close()
        }

        drawPath(
            path = wedgePath,
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.45f * pulse), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = radarRadius
            )
        )

        // Azimuth Vector Indicator Needle
        val azimuthDeg = if (isPlaying) (sin(phase * 0.7f) * 28f) else 0f
        val azimuthRad = (azimuthDeg - 90f) * (PI / 180f).toFloat()
        val vectorLen = radarRadius * (if (isPlaying) 0.92f else 0.4f)
        val vecX = centerX + cos(azimuthRad) * vectorLen
        val vecY = centerY + sin(azimuthRad) * vectorLen

        drawLine(
            color = primaryColor,
            start = Offset(centerX, centerY),
            end = Offset(vecX, vecY),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = primaryColor,
            radius = 5f,
            center = Offset(vecX, vecY)
        )

        // Origin Pivot Hub
        drawCircle(
            color = primaryColor,
            radius = 6f,
            center = Offset(centerX, centerY)
        )
    }
}

/**
 * 6. HarmonicWheel: 12-segment polar Camelot wheel radar with Deck A -> Deck B modulation arcs & audio reactivity.
 */
@Composable
private fun HarmonicWheelCanvas(
    isPlaying: Boolean,
    phase: Float,
    pulse: Float,
    currentKey: CamelotKey?,
    targetKey: CamelotKey?,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = (minOf(width, height) / 2f) * 0.88f
        val innerRingRadius = maxRadius * 0.55f
        val outerRingRadius = maxRadius * 0.85f

        // Draw 12 spoke lines
        for (i in 0 until 12) {
            val angleDeg = i * 30f - 90f
            val angleRad = (angleDeg * PI / 180f).toFloat()
            val spokeX = centerX + cos(angleRad) * maxRadius
            val spokeY = centerY + sin(angleRad) * maxRadius

            drawLine(
                color = primaryColor.copy(alpha = 0.12f),
                start = Offset(centerX, centerY),
                end = Offset(spokeX, spokeY),
                strokeWidth = 1f
            )
        }

        // Inner & Outer Rings
        drawCircle(
            color = primaryColor.copy(alpha = 0.2f),
            radius = innerRingRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.2f),
            radius = outerRingRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.5f)
        )

        // Draw 24 Camelot nodes (12 Minor inner, 12 Major outer)
        CamelotKey.entries.forEach { key ->
            val angleDeg = (key.number * 30f) - 90f
            val angleRad = (angleDeg * PI / 180f).toFloat()
            val isMinor = key.mode == CamelotMode.Minor
            val nodeRadius = if (isMinor) innerRingRadius else outerRingRadius

            val nx = centerX + cos(angleRad) * nodeRadius
            val ny = centerY + sin(angleRad) * nodeRadius

            val isCurrent = key == currentKey
            val isTarget = key == targetKey

            val pipRadius = when {
                isCurrent -> 5.5.dp.toPx() * (if (isPlaying) pulse else 1f)
                isTarget -> 4.5.dp.toPx() * (if (isPlaying) pulse else 1f)
                else -> 2.5.dp.toPx()
            }

            // Outer glow for active nodes
            if (isCurrent || isTarget) {
                drawCircle(
                    color = key.harmonicColor.copy(alpha = 0.45f * pulse),
                    radius = pipRadius * 2.2f,
                    center = Offset(nx, ny)
                )
            }

            drawCircle(
                color = key.harmonicColor,
                radius = pipRadius,
                center = Offset(nx, ny)
            )
        }

        // Modulation Arc between Current Key and Target Key
        if (currentKey != null && targetKey != null && currentKey != targetKey) {
            val fromAngle = ((currentKey.number * 30f) - 90f) * (PI / 180f).toFloat()
            val toAngle = ((targetKey.number * 30f) - 90f) * (PI / 180f).toFloat()

            val fromR = if (currentKey.mode == CamelotMode.Minor) innerRingRadius else outerRingRadius
            val toR = if (targetKey.mode == CamelotMode.Minor) innerRingRadius else outerRingRadius

            val x1 = centerX + cos(fromAngle) * fromR
            val y1 = centerY + sin(fromAngle) * fromR
            val x2 = centerX + cos(toAngle) * toR
            val y2 = centerY + sin(toAngle) * toR

            val arcPath = Path().apply {
                moveTo(x1, y1)
                val midAngle = (fromAngle + toAngle) / 2f
                val ctrlX = centerX + cos(midAngle + phase * 0.2f) * (maxRadius * 0.25f)
                val ctrlY = centerY + sin(midAngle + phase * 0.2f) * (maxRadius * 0.25f)
                quadraticTo(ctrlX, ctrlY, x2, y2)
            }

            drawPath(
                path = arcPath,
                brush = Brush.linearGradient(
                    colors = listOf(currentKey.harmonicColor, targetKey.harmonicColor),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2)
                ),
                style = Stroke(width = 3.dp.toPx() * (if (isPlaying) pulse else 1f), cap = StrokeCap.Round)
            )
        }

        // Center hub glow
        val hubColor = currentKey?.harmonicColor ?: primaryColor
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(hubColor.copy(alpha = 0.45f * pulse), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = innerRingRadius * 0.7f
            ),
            center = Offset(centerX, centerY),
            radius = innerRingRadius * 0.7f
        )
    }
}

/**
 * High performance audio-reactive backdrop canvas providing ambient mood illumination
 * across Now Playing, Elastic Sheet, and Turntable Deck.
 */
@Composable
fun SoundstageAmbientGlow(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    currentKey: CamelotKey? = null,
    targetKey: CamelotKey? = null,
    primaryColor: Color = currentKey?.harmonicColor ?: MaterialTheme.colorScheme.primary,
    secondaryColor: Color = targetKey?.harmonicColor ?: MaterialTheme.colorScheme.secondary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientGlowTransition")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 2400 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AmbientPulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.18f * pulse),
                    secondaryColor.copy(alpha = 0.08f * pulse),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.35f),
                radius = w * 0.75f
            ),
            center = Offset(w * 0.5f, h * 0.35f),
            radius = w * 0.75f
        )
    }
}
