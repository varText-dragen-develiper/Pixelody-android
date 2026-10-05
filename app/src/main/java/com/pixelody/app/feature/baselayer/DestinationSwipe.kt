package com.pixelody.app.feature.baselayer

import kotlin.math.abs

/** Only direct dragging and a child's unused horizontal scroll count; never fling motion. */
internal class DestinationSwipe(private val threshold: Float) {
    private var active = false
    private var cancelled = false
    private var directX = 0f
    private var directY = 0f
    private var unusedX = 0f
    var generation = 0L
        private set

    fun begin() {
        generation++
        active = true
        cancelled = false
        directX = 0f
        directY = 0f
        unusedX = 0f
    }

    fun direct(x: Float, y: Float) {
        if (active) { directX += x; directY += y }
    }

    fun unusedHorizontal(x: Float) {
        childScroll(0f, x)
    }

    fun childScroll(consumedX: Float, availableX: Float): Boolean {
        if (!active || cancelled) return false
        // A reversal back into a scrollable child abandons the previous edge handoff.
        if (consumedX * unusedX < 0f) unusedX = 0f
        unusedX += availableX
        return true
    }

    fun cancel() { cancelled = true }

    fun finish(expectedGeneration: Long = generation): Int? {
        if (!active || expectedGeneration != generation) return null
        active = false
        if (cancelled) return null
        // Both input paths may observe the same motion. Do not count it twice.
        val x = if (abs(unusedX) > abs(directX)) unusedX else directX
        if (abs(x) < threshold || abs(directY) > abs(x) * 0.8f) return null
        return if (x < 0f) 1 else -1
    }
}

internal fun BaseDestination.swipeNeighbor(step: Int): BaseDestination? =
    BaseDestination.values().getOrNull(ordinal + step)
