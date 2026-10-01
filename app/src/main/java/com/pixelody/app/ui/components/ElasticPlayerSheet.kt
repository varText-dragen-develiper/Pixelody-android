package com.pixelody.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pixelody.app.ui.navigation.SwipeDirection
import com.pixelody.app.ui.navigation.classifyMiniPlayerSwipe
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Spring specification for organic, responsive physical sheet movement.
 */
val ElasticSheetSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Classifies a fling or release gesture on the elastic sheet.
 */
fun classifySheetFling(
    velocityDpPerSec: Float,
    currentFraction: Float,
    isCurrentlyExpanded: Boolean = false,
    expandThreshold: Float = 0.35f,
    collapseThreshold: Float = 0.65f,
    velocityThresholdDpPerSec: Float = 700f
): Boolean {
    return when {
        velocityDpPerSec < -velocityThresholdDpPerSec -> true // Fling upwards -> Expand
        velocityDpPerSec > velocityThresholdDpPerSec -> false // Fling downwards -> Collapse
        isCurrentlyExpanded -> currentFraction >= collapseThreshold
        else -> currentFraction >= expandThreshold
    }
}

/**
 * Interactive Elastic Player Sheet supporting continuous 1:1 finger tracking,
 * spring-based snapping, backdrop scaling/scrim, and top grab handle pull-down.
 */
@Composable
fun ElasticPlayerSheet(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    miniPlayerHeight: Dp = 68.dp,
    bottomBarHeight: Dp = 80.dp,
    hasActiveTrack: Boolean = true,
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    miniPlayerContent: @Composable () -> Unit,
    fullPlayerContent: @Composable () -> Unit,
    bottomBarContent: @Composable () -> Unit = {},
    mainContent: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val fraction = remember { Animatable(if (isExpanded) 1f else 0f) }
    var measuredBottomBarHeightPx by remember { mutableFloatStateOf(0f) }
    var measuredMiniPlayerHeightPx by remember { mutableFloatStateOf(0f) }

    // Sync external isExpanded changes smoothly
    LaunchedEffect(isExpanded) {
        val target = if (isExpanded) 1f else 0f
        if (fraction.targetValue != target) {
            fraction.animateTo(target, ElasticSheetSpringSpec)
        }
    }

    // Intercept system Back when the sheet is expanded or in motion
    BackHandler(enabled = isExpanded) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onExpandedChange(false)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val fallbackMiniPlayerHeightPx = with(density) { miniPlayerHeight.toPx() }
        val fallbackBottomBarHeightPx = with(density) { (bottomBarHeight + navBarBottomInset).toPx() }

        val effectiveBottomBarHeightPx = if (measuredBottomBarHeightPx > 0f) measuredBottomBarHeightPx else fallbackBottomBarHeightPx
        val effectiveMiniPlayerHeightPx = if (measuredMiniPlayerHeightPx > 0f) measuredMiniPlayerHeightPx else fallbackMiniPlayerHeightPx
        val miniPlayerSwipeThresholdPx = with(density) { 56.dp.toPx() }

        // Maximum travel distance from collapsed mini player position to top
        val travelDistancePx = (totalHeightPx - effectiveBottomBarHeightPx - effectiveMiniPlayerHeightPx).coerceAtLeast(1f)

        val currentProgress = fraction.value
        val clampedProgress = currentProgress.coerceIn(0f, 1f)

        // 1. Underlying Main Content with subtle depth scale and dark scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = (1f - (clampedProgress * 0.045f)).coerceIn(0.85f, 1f)
                    scaleX = scale
                    scaleY = scale
                    clip = clampedProgress > 0.01f
                    shape = RoundedCornerShape((clampedProgress * 20f).coerceAtLeast(0f).dp)
                }
        ) {
            mainContent()

            // Scrim darkening layer
            if (clampedProgress > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = (clampedProgress * 0.38f).coerceIn(0f, 1f)))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (clampedProgress > 0.5f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onExpandedChange(false)
                            }
                        }
                )
            }
        }

        // 2. Bottom Navigation Bar (anchored at the very bottom)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .onSizeChanged { size ->
                    if (size.height > 0) measuredBottomBarHeightPx = size.height.toFloat()
                }
                .zIndex(if (clampedProgress > 0.15f) 1f else 3f)
                .graphicsLayer {
                    // Smoothly slide down and fade out when full player opens
                    alpha = (1f - clampedProgress * 2.5f).coerceIn(0f, 1f)
                    translationY = clampedProgress * effectiveBottomBarHeightPx
                }
        ) {
            bottomBarContent()
        }

        // 3. Persistent / Elastic Player Sheet (only when an active track exists)
        if (hasActiveTrack) {
            val sheetOffsetY = ((1f - currentProgress) * travelDistancePx).roundToInt()
            val sheetCornerRadius = ((1f - clampedProgress) * 16f).coerceAtLeast(0f).dp
            val elevationDp = (12f * clampedProgress + 4f).coerceAtLeast(0f).dp

            val sheetDraggableState = rememberDraggableState { deltaY ->
                coroutineScope.launch {
                    val fractionDelta = -deltaY / travelDistancePx
                    fraction.snapTo((fraction.value + fractionDelta).coerceIn(0f, 1f))
                }
            }

            val settleSheet: (Float) -> Unit = { velocityDpPerSec ->
                val commitExpand = classifySheetFling(
                    velocityDpPerSec = velocityDpPerSec,
                    currentFraction = fraction.value,
                    isCurrentlyExpanded = isExpanded
                )
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (commitExpand != isExpanded) {
                    onExpandedChange(commitExpand)
                } else {
                    coroutineScope.launch {
                        fraction.animateTo(if (isExpanded) 1f else 0f, ElasticSheetSpringSpec)
                    }
                }
            }

            val sheetNestedScroll = remember(travelDistancePx, isExpanded) {
                object : NestedScrollConnection {
                    private fun drag(deltaY: Float): Offset {
                        coroutineScope.launch {
                            fraction.snapTo((fraction.value - deltaY / travelDistancePx).coerceIn(0f, 1f))
                        }
                        return Offset(0f, deltaY)
                    }

                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val deltaY = available.y
                        if (deltaY >= 0f || fraction.value >= 1f) return Offset.Zero
                        return drag(deltaY)
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val deltaY = available.y
                        if (deltaY <= 0f || fraction.value <= 0f) return Offset.Zero
                        return drag(deltaY)
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (fraction.value >= 1f || fraction.value <= 0f) return Velocity.Zero
                        settleSheet(with(density) { available.y.toDp().value })
                        return available
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight)
                    .offset { IntOffset(0, sheetOffsetY) }
                    .zIndex(if (clampedProgress > 0.15f) 4f else 2f)
                    .shadow(
                        elevation = elevationDp,
                        shape = RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius)
                    )
                    .clip(RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // MINI PLAYER VIEW (visible when collapsed or in early expansion)
                    if (clampedProgress < 0.65f) {
                        val miniAlpha = ((0.65f - clampedProgress) / 0.65f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(miniPlayerHeight)
                                .align(Alignment.TopCenter)
                                .onSizeChanged { size ->
                                    if (size.height > 0) measuredMiniPlayerHeightPx = size.height.toFloat()
                                }
                                .draggable(
                                    state = sheetDraggableState,
                                    orientation = Orientation.Vertical,
                                    onDragStopped = { velocity ->
                                        settleSheet(with(density) { velocity.toDp().value })
                                    }
                                )
                                .pointerInput(Unit) {
                                    val thresholdPx = miniPlayerSwipeThresholdPx
                                    var travelX = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { travelX = 0f },
                                        onDragEnd = {
                                            when (classifyMiniPlayerSwipe(travelX, 0f, thresholdPx)) {
                                                SwipeDirection.Next -> {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onSkipNext()
                                                }
                                                SwipeDirection.Previous -> {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onSkipPrevious()
                                                }
                                                else -> Unit
                                            }
                                            travelX = 0f
                                        },
                                        onDragCancel = { travelX = 0f },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            travelX += dragAmount
                                        }
                                    )
                                }
                                .graphicsLayer { alpha = miniAlpha }
                        ) {
                            miniPlayerContent()
                        }
                    }

                    // FULL NOW PLAYING VIEW (fades in as progress increases)
                    if (clampedProgress > 0.05f) {
                        val fullAlpha = ((clampedProgress - 0.05f) / 0.60f).coerceIn(0f, 1f)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = fullAlpha }
                        ) {
                            // Tactile Top Grab Handle
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .draggable(
                                        state = sheetDraggableState,
                                        orientation = Orientation.Vertical,
                                        onDragStopped = { velocity ->
                                            settleSheet(with(density) { velocity.toDp().value })
                                        }
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onExpandedChange(false)
                                    }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(4.5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                                )
                            }

                            // Full Player Content
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .nestedScroll(sheetNestedScroll)
                            ) {
                                fullPlayerContent()
                            }
                        }
                    }
                }
            }
        }
    }
}
