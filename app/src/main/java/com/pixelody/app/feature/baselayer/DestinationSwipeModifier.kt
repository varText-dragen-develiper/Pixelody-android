package com.pixelody.app.feature.baselayer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.animation.core.spring
import androidx.compose.ui.input.pointer.util.VelocityTracker

@Composable
internal fun destinationSwipeModifier(
    enabled: Boolean,
    destination: BaseDestination,
    pager: PagerState,
    onNavigate: (BaseDestination) -> Unit
): Modifier {
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    val gesture = remember(destination, enabled, threshold) { DestinationSwipe(threshold) }
    val latestGesture = rememberUpdatedState(gesture)
    val navigate = rememberUpdatedState(onNavigate)
    val scope = rememberCoroutineScope()
    var applied = remember(gesture) { 0f }
    fun followDrag() {
        val delta = gesture.displacement - applied
        applied = gesture.displacement
        if (kotlin.math.abs(delta) < .01f) return
        scope.launch { pager.scrollBy(-delta) }
    }
    val connection = remember(gesture, pager) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !gesture.childScroll(consumed.x, available.x)) return Offset.Zero
                // Claim only the leftover horizontal motion in a direction we can navigate.
                // Otherwise the child's stretch effect eats subsequent deltas at its edge.
                followDrag()
                val step = if (available.x < 0f) 1 else -1
                return if (destination.swipeNeighbor(step) != null) Offset(available.x, 0f) else Offset.Zero
            }
        }
    }
    if (!enabled) return Modifier
    return Modifier
        .nestedScroll(connection)
        .pointerInput(gesture) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val velocity = VelocityTracker()
                velocity.addPosition(down.uptimeMillis, down.position)
                applied = 0f
                gesture.begin()
                var released = false
                try {
                    do {
                        // Wait until children have handled this event, including their nested scroll.
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.changes.count { it.pressed || it.previousPressed } > 1) gesture.cancel()
                        event.changes.firstOrNull()?.let { velocity.addPosition(it.uptimeMillis, it.position) }
                        val pressed = event.changes.any { it.pressed }
                        if (!pressed) {
                            released = true
                            val generation = gesture.generation
                            // Scrollables process drag deltas in a coroutine. Let their final
                            // post-scroll reach us before evaluating the released gesture.
                            scope.launch {
                                withFrameNanos { }
                                if (latestGesture.value !== gesture) return@launch
                                val target = gesture.finish(generation, velocity.calculateVelocity().x)
                                    ?.let(destination::swipeNeighbor)
                                if (target != null) navigate.value(target)
                                else pager.animateScrollToPage(destination.ordinal,
                                    animationSpec = spring(dampingRatio = 1f, stiffness = 380f))
                            }
                        }
                    } while (pressed)
                } finally {
                    if (!released) {
                        gesture.cancel()
                        scope.launch {
                            if (latestGesture.value === gesture) pager.animateScrollToPage(destination.ordinal,
                                animationSpec = spring(dampingRatio = 1f, stiffness = 380f))
                        }
                    }
                }
            }
        }
        .pointerInput(gesture) {
            // Main-pass drag detection yields automatically when a child consumes movement.
            var ownsDirectDrag = false
            detectHorizontalDragGestures(
                onDragStart = { ownsDirectDrag = true },
                onDragEnd = { ownsDirectDrag = false },
                onDragCancel = { if (ownsDirectDrag) gesture.cancel(); ownsDirectDrag = false },
                onHorizontalDrag = { change, amount ->
                gesture.direct(amount, change.positionChange().y)
                followDrag()
                change.consume()
            })
        }
}
