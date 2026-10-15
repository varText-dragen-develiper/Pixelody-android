package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Only artwork/track identity owns this gesture; vertical scroll, sliders and lyrics stay independent. */
internal fun Modifier.playerTrackSwipe(onPrevious: () -> Unit, onNext: () -> Unit): Modifier = composed {
    val previous = rememberUpdatedState(onPrevious)
    val next = rememberUpdatedState(onNext)
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    pointerInput(threshold) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var distance = 0f
            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                distance = overSlop
                change.consume()
            } ?: return@awaitEachGesture
            val completed = horizontalDrag(drag.id) { change ->
                distance += change.positionChange().x
                change.consume()
            }
            if (completed && abs(distance) >= threshold) {
                if (distance < 0) next.value() else previous.value()
            }
        }
    }
}
