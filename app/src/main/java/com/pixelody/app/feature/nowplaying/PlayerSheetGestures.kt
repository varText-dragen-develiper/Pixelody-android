package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

// Direction locking leaves horizontal track swipes and header scrolling intact.
internal fun Modifier.playerSheetDrag(upward: Boolean, onComplete: () -> Unit): Modifier = composed {
    val complete = rememberUpdatedState(onComplete)
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    pointerInput(upward, threshold) {
        var distance = 0f
        detectVerticalDragGestures(
            onDragStart = { distance = 0f },
            onDragCancel = { distance = 0f },
            onDragEnd = {
                if ((if (upward) -distance else distance) >= threshold) complete.value()
                distance = 0f
            },
            onVerticalDrag = { change, delta ->
                distance += delta
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
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    return remember(threshold) {
        object : NestedScrollConnection {
            private var distance = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f) distance = 0f
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (consumed.y > 0f) distance = 0f
                if (available.y > 0f) {
                    distance += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (distance >= threshold) dismiss.value()
                distance = 0f
                return Velocity.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                distance = 0f
                return Velocity.Zero
            }
        }
    }
}
