package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.util.VelocityTracker

// Direction locking leaves horizontal track swipes and header scrolling intact.
internal fun Modifier.playerSheetDrag(upward: Boolean, onComplete: () -> Unit): Modifier = composed {
    val complete = rememberUpdatedState(onComplete)
    val motion = LocalPlayerMotion.current
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    onGloballyPositioned { coordinates = it }.pointerInput(upward, threshold, motion) {
        var distance = 0f
        var velocity = VelocityTracker()
        detectVerticalDragGestures(
            onDragStart = { distance = 0f; velocity = VelocityTracker(); motion?.begin(upward) },
            onDragCancel = { motion?.finish(0f, threshold, cancelled = true); distance = 0f },
            onDragEnd = {
                if (motion != null) motion.finish(velocity.calculateVelocity().y, threshold)
                else if ((if (upward) -distance else distance) >= threshold) complete.value()
                distance = 0f
            },
            onVerticalDrag = { change, delta ->
                distance += delta
                // Track in root coordinates: the header itself follows this drag.
                velocity.addPosition(change.uptimeMillis, coordinates?.localToRoot(change.position) ?: change.position)
                motion?.drag(delta)
                change.consume()
            }
        )
    }
}

// Only the downward distance left over at the top of a scrollable player closes it.
// Scrolling lyrics/racks, seeking, and flinging content retain their own gestures.
@Composable
internal fun rememberPlayerDismissConnection(onDismiss: () -> Unit): NestedScrollConnection {
    val dismiss = rememberUpdatedState(onDismiss)
    val motion = LocalPlayerMotion.current
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    return remember(threshold, motion) {
        object : NestedScrollConnection {
            private var distance = 0f
            private var ownsPull = false
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f) {
                    if (ownsPull && motion != null) { motion.drag(available.y); return Offset(0f, available.y) }
                    distance = 0f
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (consumed.y > 0f) distance = 0f
                if (available.y > 0f) {
                    if (!ownsPull) { motion?.begin(upward = false); ownsPull = true }
                    distance += available.y
                    motion?.drag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                val claimed = ownsPull
                if (ownsPull && motion != null) motion.finish(available.y, threshold)
                else if (distance >= threshold) dismiss.value()
                distance = 0f
                ownsPull = false
                return if (claimed) available else Velocity.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                distance = 0f
                return Velocity.Zero
            }
        }
    }
}
