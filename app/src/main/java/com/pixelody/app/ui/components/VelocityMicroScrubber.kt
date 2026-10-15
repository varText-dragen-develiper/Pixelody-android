package com.pixelody.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.ui.navigation.PixelodyStateTags

enum class ScrubRate(
    val multiplier: Float,
    val label: String,
    val badgeText: String,
    val stepIntervalMs: Long
) {
    Full(1.0f, "Hi-Speed", "1.0x Full Speed", 5000L),
    Half(0.5f, "Half-Speed", "0.5x Half Speed", 2000L),
    Quarter(0.25f, "Quarter-Speed", "0.25x Precision", 1000L),
    Fine(0.1f, "Fine Micro", "0.1x Micro Seek", 250L);

    companion object {
        fun fromVerticalOffsetDp(offsetDp: Float): ScrubRate = when {
            offsetDp < 35f -> Full
            offsetDp < 80f -> Half
            offsetDp < 140f -> Quarter
            else -> Fine
        }
    }
}

/**
 * Pure helper to compute the fractional progress change based on horizontal delta,
 * total bar width, and active scrub rate multiplier.
 */
fun computeScrubDelta(
    dragDeltaXPx: Float,
    totalWidthPx: Float,
    scrubRate: ScrubRate
): Float {
    if (totalWidthPx <= 0f) return 0f
    return (dragDeltaXPx / totalWidthPx) * scrubRate.multiplier
}

private fun formatTimestamp(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

/**
 * Velocity-Aware Micro-Scrubber with Vertical Offset Precision & Haptic Waveform Ticks.
 */
@Composable
fun VelocityMicroScrubber(
    positionMs: Long = 0L,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    trackBarHeight: Float = 8f,
    positionMsProvider: (() -> Long)? = null
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val currentPositionMs = positionMsProvider?.invoke() ?: positionMs

    var isDragging by remember { mutableStateOf(false) }
    var scrubProgress by remember {
        mutableFloatStateOf(
            if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        )
    }
    var currentScrubRate by remember { mutableStateOf(ScrubRate.Full) }
    var verticalOffsetDp by remember { mutableFloatStateOf(0f) }
    var lastHapticStep by remember { mutableLongStateOf(0L) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }

    // Sync state when not dragging
    LaunchedEffect(currentPositionMs, durationMs, isDragging) {
        if (!isDragging && durationMs > 0) {
            scrubProgress = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
    }

    val hasKnownDuration = durationMs > 0L
    val previewPositionMs = if (durationMs > 0) (scrubProgress * durationMs).toLong() else 0L
    val remainingMs = (durationMs - previewPositionMs).coerceAtLeast(0L)

    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbRadius"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Floating HUD during active scrub
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, activeColor.copy(alpha = 0.6f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(previewPositionMs / 1000),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
                        )
                        Text(
                            text = "(-${formatTimestamp(remainingMs / 1000)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(activeColor.copy(alpha = 0.18f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = currentScrubRate.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeColor
                            )
                        }
                    }
                }
            }
        }

        // Interactive Scrubber Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clipToBounds()
                .testTag(PixelodyStateTags.PLAYER_SCRUBBER)
                .semantics {
                    contentDescription = if (hasKnownDuration) "Precision scrubber" else "Track length unknown, seeking unavailable"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = scrubProgress,
                        range = 0f..1f
                    )
                    if (hasKnownDuration) setProgress { fraction ->
                        onSeek((fraction.coerceIn(0f, 1f) * durationMs).toLong())
                        true
                    }
                }
                .pointerInput(durationMs) {
                    if (durationMs <= 0) return@pointerInput
                    detectTapGestures { offset ->
                        val targetFrac = (offset.x / size.width).coerceIn(0f, 1f)
                        scrubProgress = targetFrac
                        val targetMs = (targetFrac * durationMs).toLong()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSeek(targetMs)
                    }
                }
                .pointerInput(durationMs) {
                    if (durationMs <= 0) return@pointerInput
                    var initialTouchY = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            initialTouchY = startOffset.y
                            verticalOffsetDp = 0f
                            currentScrubRate = ScrubRate.Full
                            lastHapticStep = if (currentScrubRate.stepIntervalMs > 0) {
                                (scrubProgress * durationMs / currentScrubRate.stepIntervalMs).toLong()
                            } else 0L
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            isDragging = false
                            val finalTargetMs = (scrubProgress * durationMs).toLong()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeek(finalTargetMs)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val totalWidth = size.width.toFloat()
                            barWidthPx = totalWidth

                            // Calculate vertical distance from bar
                            val currentY = change.position.y
                            val rawVerticalOffsetPx = (currentY - initialTouchY).coerceAtLeast(0f)
                            val currentOffsetDp = with(density) { rawVerticalOffsetPx.toDp().value }
                            verticalOffsetDp = currentOffsetDp

                            val newRate = ScrubRate.fromVerticalOffsetDp(currentOffsetDp)
                            if (newRate != currentScrubRate) {
                                currentScrubRate = newRate
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            // Apply scaled delta
                            val delta = computeScrubDelta(dragAmount, totalWidth, currentScrubRate)
                            scrubProgress = (scrubProgress + delta).coerceIn(0f, 1f)

                            // Haptic step feedback
                            val currentPosMs = (scrubProgress * durationMs).toLong()
                            val currentStep = if (currentScrubRate.stepIntervalMs > 0) {
                                currentPosMs / currentScrubRate.stepIntervalMs
                            } else 0L
                            if (currentStep != lastHapticStep) {
                                lastHapticStep = currentStep
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                barWidthPx = size.width
                val width = size.width
                val centerY = size.height / 2f
                val trackHeightPx = trackBarHeight.dp.toPx()
                val currentX = width * scrubProgress

                // Background track with ticks
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                    size = Size(width, trackHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                )

                // Waveform / bar notches in track
                val numTicks = 32
                val tickStep = width / numTicks
                for (i in 0..numTicks) {
                    val tickX = i * tickStep
                    val tickHeight = if (i % 4 == 0) trackHeightPx * 0.85f else trackHeightPx * 0.45f
                    val tickColor = if (tickX <= currentX) {
                        activeColor.copy(alpha = 0.5f)
                    } else {
                        Color.White.copy(alpha = 0.15f)
                    }
                    drawLine(
                        color = tickColor,
                        start = Offset(tickX, centerY - tickHeight / 2f),
                        end = Offset(tickX, centerY + tickHeight / 2f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Active progress bar with glowing gradient
                if (currentX > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.85f),
                                activeColor
                            ),
                            startX = 0f,
                            endX = currentX
                        ),
                        topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                        size = Size(currentX, trackHeightPx),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                    )
                }

                // Vertical tension guide ray when pulled down in precision mode
                if (isDragging && verticalOffsetDp > 20f) {
                    val guideLengthPx = with(density) { verticalOffsetDp.dp.toPx() }.coerceAtMost(size.height - centerY)
                    drawLine(
                        color = activeColor.copy(alpha = 0.45f),
                        start = Offset(currentX, centerY),
                        end = Offset(currentX, centerY + guideLengthPx),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }

                // Glowing Scrub Head Outer Ring & Center
                if (hasKnownDuration) {
                    val thumbRadiusPx = thumbRadius.toPx()
                    drawCircle(
                        color = activeColor.copy(alpha = if (isDragging) 0.35f else 0.18f),
                        radius = thumbRadiusPx + 4.dp.toPx(),
                        center = Offset(currentX, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadiusPx,
                        center = Offset(currentX, centerY)
                    )
                    drawCircle(
                        color = activeColor,
                        radius = thumbRadiusPx * 0.55f,
                        center = Offset(currentX, centerY)
                    )
                }
            }
        }

        // Timestamps below track
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayMs = if (isDragging) previewPositionMs else currentPositionMs
            Text(
                text = formatTimestamp(displayMs / 1000),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (hasKnownDuration) formatTimestamp(durationMs / 1000) else "--:--",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
