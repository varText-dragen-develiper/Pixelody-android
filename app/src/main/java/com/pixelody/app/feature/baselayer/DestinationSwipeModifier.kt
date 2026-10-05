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

@Composable
internal fun destinationSwipeModifier(
    enabled: Boolean,
    destination: BaseDestination,
    onNavigate: (BaseDestination) -> Unit
): Modifier {
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    val gesture = remember(destination, enabled, threshold) { DestinationSwipe(threshold) }
    val latestGesture = rememberUpdatedState(gesture)
    val navigate = rememberUpdatedState(onNavigate)
    val scope = rememberCoroutineScope()
    val connection = remember(gesture) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !gesture.childScroll(consumed.x, available.x)) return Offset.Zero
                // Claim only the leftover horizontal motion in a direction we can navigate.
                // Otherwise the child's stretch effect eats subsequent deltas at its edge.
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
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                gesture.begin()
                var released = false
                try {
                    do {
                        // Wait until children have handled this event, including their nested scroll.
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.changes.count { it.pressed || it.previousPressed } > 1) gesture.cancel()
                        val pressed = event.changes.any { it.pressed }
                        if (!pressed) {
                            released = true
                            val generation = gesture.generation
                            // Scrollables process drag deltas in a coroutine. Let their final
                            // post-scroll reach us before evaluating the released gesture.
                            scope.launch {
                                withFrameNanos { }
                                if (latestGesture.value !== gesture) return@launch
                                gesture.finish(generation)?.let { step ->
                                    destination.swipeNeighbor(step)?.let { navigate.value(it) }
                                }
                            }
                        }
                    } while (pressed)
                } finally {
                    if (!released) gesture.cancel()
                }
            }
        }
        .pointerInput(gesture) {
            // Main-pass drag detection yields automatically when a child consumes movement.
            detectHorizontalDragGestures { change, amount ->
                gesture.direct(amount, change.positionChange().y)
                change.consume()
            }
        }
}
