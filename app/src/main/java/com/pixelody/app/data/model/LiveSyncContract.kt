package com.pixelody.app.data.model

import java.time.Instant

fun NetworkSessionPlayback.estimatedElapsedSeconds(nowEpochMs: Long = System.currentTimeMillis()): Double {
    val anchored = elapsedSeconds.coerceAtLeast(0.0)
    if (!playing) return anchored.coerceAtMost(durationSeconds.takeIf { it > 0 } ?: Double.MAX_VALUE)
    val anchorMs = runCatching { Instant.parse(positionUpdatedAt).toEpochMilli() }.getOrNull()
        ?: return anchored
    val projected = anchored + ((nowEpochMs - anchorMs).coerceAtLeast(0L) / 1000.0)
    return projected.coerceAtMost(durationSeconds.takeIf { it > 0 } ?: Double.MAX_VALUE)
}
