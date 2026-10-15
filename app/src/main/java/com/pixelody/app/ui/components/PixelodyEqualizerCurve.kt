package com.pixelody.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelody.app.core.playback.MasteringDspEngine
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.MasteringProfile
import kotlin.math.exp
import kotlin.math.ln

/**
 * Interactive visual frequency response curve for Pixelody's parametric equalizer and studio mastering engine.
 * Renders smooth resonant biquad spline curves through frequency bands with illuminated gradient fill.
 */
@Composable
fun PixelodyEqualizerCurve(
    gainsDb: List<Float>,
    enabled: Boolean,
    qFactors: List<Float>? = null,
    subBassBoostDb: Float = 0f,
    onBandGainChange: ((bandIndex: Int, gainDb: Float) -> Unit)? = null,
    onBandQChange: ((bandIndex: Int, q: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val bandLabels = remember { EqualizerPreset.bandLabels }
    val normalizedGains = remember(gainsDb) {
        if (gainsDb.size >= 5) gainsDb.take(5) else List(5) { 0f }
    }
    val normalizedQs = remember(qFactors) {
        if (qFactors != null && qFactors.size >= 5) qFactors.take(5) else List(5) { 1.4f }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceVariantColor.copy(alpha = 0.35f))
            .padding(12.dp)
            .semantics { contentDescription = "Equalizer frequency response curve" }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .pointerInput(enabled, onBandGainChange) {
                    if (!enabled || onBandGainChange == null) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        if (canvasWidth <= 0 || canvasHeight <= 0) return@detectDragGestures

                        val xFraction = (change.position.x / canvasWidth).coerceIn(0f, 1f)
                        val bandIndex = (xFraction * 5).toInt().coerceIn(0, 4)

                        val yFraction = (change.position.y / canvasHeight).coerceIn(0f, 1f)
                        // Top is +12dB (y=0), Bottom is -12dB (y=1)
                        val newGain = EqualizerProfile.MAX_GAIN_DB - yFraction * (EqualizerProfile.MAX_GAIN_DB - EqualizerProfile.MIN_GAIN_DB)
                        val clampedGain = newGain.coerceIn(EqualizerProfile.MIN_GAIN_DB, EqualizerProfile.MAX_GAIN_DB)

                        haptic.performTick()
                        onBandGainChange(bandIndex, (clampedGain * 2).toInt() / 2f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                if (width <= 0 || height <= 0) return@Canvas

                val zeroY = height * 0.5f
                val topY = height * 0.1f
                val bottomY = height * 0.9f

                // Reference grid lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = outlineColor.copy(alpha = 0.25f),
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )

                // Sub-grid lines for +6dB and -6dB
                val plus6Y = zeroY - (zeroY - topY) * 0.5f
                val minus6Y = zeroY + (bottomY - zeroY) * 0.5f
                drawLine(
                    color = outlineColor.copy(alpha = 0.12f),
                    start = Offset(0f, plus6Y),
                    end = Offset(width, plus6Y),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = outlineColor.copy(alpha = 0.12f),
                    start = Offset(0f, minus6Y),
                    end = Offset(width, minus6Y),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )

                val activeColor = if (enabled) primaryColor else outlineColor.copy(alpha = 0.5f)
                val glowColor = if (enabled) secondaryColor.copy(alpha = 0.35f) else Color.Transparent

                // Build continuous DSP response curve using parametric filter evaluation
                val minLog = ln(20.0)
                val maxLog = ln(20000.0)
                val sampleCount = 64
                val dspProfile = MasteringProfile(
                    enabled = enabled,
                    eqGainsDb = normalizedGains,
                    eqQFactors = normalizedQs,
                    subBassBoostDb = subBassBoostDb
                )

                val curvePath = Path()
                for (s in 0 until sampleCount) {
                    val logF = minLog + (s.toFloat() / (sampleCount - 1)) * (maxLog - minLog)
                    val freq = exp(logF).toFloat()
                    val totalDb = MasteringDspEngine.evaluateParametricResponseDb(freq, dspProfile)
                        .coerceIn(EqualizerProfile.MIN_GAIN_DB, EqualizerProfile.MAX_GAIN_DB)

                    val x = (s.toFloat() / (sampleCount - 1)) * width
                    val gainFraction = (totalDb - EqualizerProfile.MIN_GAIN_DB) / (EqualizerProfile.MAX_GAIN_DB - EqualizerProfile.MIN_GAIN_DB)
                    val y = bottomY - gainFraction * (bottomY - topY)

                    if (s == 0) curvePath.moveTo(x, y)
                    else curvePath.lineTo(x, y)
                }

                // Fill under curve
                val fillPath = Path().apply {
                    addPath(curvePath)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = if (enabled) 0.38f else 0.12f),
                            glowColor,
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw curve stroke
                drawPath(
                    path = curvePath,
                    color = activeColor,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Draw band control anchor nodes
                for (i in 0 until 5) {
                    val f0 = EqualizerPreset.bandCentersHz[i].toFloat()
                    val logF0 = ln(f0.toDouble())
                    val x = (((logF0 - minLog) / (maxLog - minLog)).toFloat() * width).coerceIn(12f, width - 12f)

                    val gain = normalizedGains.getOrElse(i) { 0f }
                    val q = normalizedQs.getOrElse(i) { 1.4f }
                    val gainFraction = (gain - EqualizerProfile.MIN_GAIN_DB) / (EqualizerProfile.MAX_GAIN_DB - EqualizerProfile.MIN_GAIN_DB)
                    val y = bottomY - gainFraction * (bottomY - topY)
                    val nodeCenter = Offset(x, y)

                    val isNeutral = kotlin.math.abs(gain) < 0.5f

                    // Resonant Q bandwidth indicator ring (smaller radius = higher Q, larger = wider Q)
                    if (qFactors != null && enabled) {
                        val qRadius = (24f / q).coerceIn(8f, 32f)
                        drawCircle(
                            color = activeColor.copy(alpha = 0.2f),
                            radius = qRadius,
                            center = nodeCenter,
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                        )
                    }

                    // Outer node glow
                    drawCircle(
                        color = if (enabled) activeColor.copy(alpha = 0.25f) else Color.Transparent,
                        radius = 12f,
                        center = nodeCenter
                    )

                    // Node center dot
                    drawCircle(
                        color = if (isNeutral) activeColor else (if (enabled) secondaryColor else outlineColor),
                        radius = 5.5f,
                        center = nodeCenter
                    )
                }
            }
        }

        // Frequency Band Labels underneath
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            bandLabels.forEachIndexed { index, label ->
                val gain = normalizedGains.getOrElse(index) { 0f }
                val q = normalizedQs.getOrElse(index) { 1.4f }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (enabled) "%+.0fdB".format(gain) else "--",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled && kotlin.math.abs(gain) >= 0.5f) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                    if (qFactors != null && enabled) {
                        Text(
                            text = "Q:%.1f".format(q),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

