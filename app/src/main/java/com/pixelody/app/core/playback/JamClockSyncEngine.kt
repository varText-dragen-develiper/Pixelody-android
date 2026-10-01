package com.pixelody.app.core.playback

import com.pixelody.app.data.model.JamClockSyncSample
import com.pixelody.app.data.model.JamSyncMode
import com.pixelody.app.data.model.JamSyncStatus
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Precision time synchronization and ExoPlayer playback speed compensation engine.
 * Maintains sample-accurate multi-speaker alignment across multi-device J.A.M. mesh sessions.
 */
class JamClockSyncEngine(
    private val maxHistorySamples: Int = 12,
    private val inSyncThresholdMs: Long = 25L,
    private val microAdjustMaxThresholdMs: Long = 300L,
    private val maxSpeedAdjustmentFactor: Float = 0.015f // ±1.5% micro-pitch adjustment
) {
    private val samples = ConcurrentLinkedDeque<JamClockSyncSample>()
    @Volatile private var smoothedClockOffsetMs: Double = 0.0
    @Volatile private var smoothedRttMs: Double = 0.0
    @Volatile private var hasInitialSync: Boolean = false
    @Volatile private var syncMode: JamSyncMode = JamSyncMode.Standalone
    @Volatile private var lastSyncTimestamp: Long = 0L

    fun setMode(mode: JamSyncMode) {
        if (syncMode != mode) {
            syncMode = mode
            if (mode == JamSyncMode.Standalone) {
                reset()
            }
        }
    }

    fun getMode(): JamSyncMode = syncMode

    fun reset() {
        samples.clear()
        smoothedClockOffsetMs = 0.0
        smoothedRttMs = 0.0
        hasInitialSync = false
        lastSyncTimestamp = 0L
    }

    /**
     * Records a 4-timestamp NTP-style synchronization exchange:
     * @param t0ClientSendMs local time when client sent ping
     * @param t1ServerReceiveMs host time when host received ping
     * @param t2ServerSendMs host time when host replied pong
     * @param t3ClientReceiveMs local time when client received pong
     */
    fun recordSample(
        t0ClientSendMs: Long,
        t1ServerReceiveMs: Long,
        t2ServerSendMs: Long,
        t3ClientReceiveMs: Long
    ): JamClockSyncSample {
        val sample = JamClockSyncSample(
            t0ClientSendMs = t0ClientSendMs,
            t1ServerReceiveMs = t1ServerReceiveMs,
            t2ServerSendMs = t2ServerSendMs,
            t3ClientReceiveMs = t3ClientReceiveMs
        )
        addSample(sample)
        return sample
    }

    fun addSample(sample: JamClockSyncSample) {
        samples.add(sample)
        while (samples.size > maxHistorySamples) {
            samples.poll()
        }
        recalculateSmoothedOffset()
        lastSyncTimestamp = System.currentTimeMillis()
    }

    private fun recalculateSmoothedOffset() {
        val currentSamples = samples.toList()
        if (currentSamples.isEmpty()) return

        // Calculate median RTT to filter out jitter/outliers
        val sortedRtt = currentSamples.map { it.roundTripTimeMs.toDouble() }.sorted()
        val medianRtt = sortedRtt[sortedRtt.size / 2]
        val maxAcceptableRtt = max(medianRtt * 2.0, 100.0)

        // Filter valid low-jitter samples
        val validSamples = currentSamples.filter { it.roundTripTimeMs <= maxAcceptableRtt }
            .ifEmpty { currentSamples }

        // Weight samples inversely by RTT (lower RTT = higher precision)
        var totalWeight = 0.0
        var weightedOffsetSum = 0.0
        var weightedRttSum = 0.0

        for (sample in validSamples) {
            val rtt = max(sample.roundTripTimeMs.toDouble(), 1.0)
            val weight = 1.0 / rtt
            weightedOffsetSum += sample.rawClockOffsetMs * weight
            weightedRttSum += sample.roundTripTimeMs * weight
            totalWeight += weight
        }

        val estimatedOffset = if (totalWeight > 0.0) weightedOffsetSum / totalWeight else 0.0
        val estimatedRtt = if (totalWeight > 0.0) weightedRttSum / totalWeight else 0.0

        if (!hasInitialSync) {
            smoothedClockOffsetMs = estimatedOffset
            smoothedRttMs = estimatedRtt
            hasInitialSync = true
        } else {
            // Exponential Moving Average filter (alpha = 0.3)
            val alpha = 0.30
            smoothedClockOffsetMs = (alpha * estimatedOffset) + ((1.0 - alpha) * smoothedClockOffsetMs)
            smoothedRttMs = (alpha * estimatedRtt) + ((1.0 - alpha) * smoothedRttMs)
        }
    }

    /**
     * Returns the smoothed clock offset between local time and coordinator time.
     * HostTime = LocalTime + offsetMs
     */
    fun getClockOffsetMs(): Long = smoothedClockOffsetMs.toLong()

    /**
     * Returns the estimated round-trip latency to the coordinator.
     */
    fun getRoundTripTimeMs(): Long = smoothedRttMs.toLong()

    /**
     * Projects what the host playback position should be at this exact local moment.
     */
    fun computeTargetPositionMs(
        anchorPositionMs: Long,
        anchorTimestampMs: Long,
        localNowMs: Long = System.currentTimeMillis(),
        isPlaying: Boolean = true
    ): Long {
        if (!isPlaying) return anchorPositionMs
        val coordinatorNowMs = localNowMs + getClockOffsetMs()
        val elapsedSinceAnchor = max(0L, coordinatorNowMs - anchorTimestampMs)
        return anchorPositionMs + elapsedSinceAnchor
    }

    /**
     * Calculates the sync drift and recommended ExoPlayer playback speed factor.
     *
     * @param localPlayerPositionMs Current ExoPlayer position in ms
     * @param targetPositionMs Projected master timeline position in ms
     * @return Pair of (DriftDeltaMs, SpeedAdjustmentFactor)
     *         where positive driftDelta means local player is ahead of master timeline.
     */
    fun calculatePlaybackAdjustment(
        localPlayerPositionMs: Long,
        targetPositionMs: Long
    ): Pair<Long, Float> {
        val driftDeltaMs = localPlayerPositionMs - targetPositionMs
        val absDrift = abs(driftDeltaMs)

        val speedFactor: Float = when {
            // In-sync zone (<= 25ms): perfect sync, standard 1.000x playback speed
            absDrift <= inSyncThresholdMs -> 1.0f

            // Micro-adjustment zone (25ms .. 300ms): seamless continuous micro-speed adjust
            absDrift <= microAdjustMaxThresholdMs -> {
                val fraction = (absDrift - inSyncThresholdMs).toFloat() / (microAdjustMaxThresholdMs - inSyncThresholdMs).toFloat()
                val deltaFactor = fraction * maxSpeedAdjustmentFactor
                if (driftDeltaMs > 0) {
                    // Local player is ahead -> slightly slow down (e.g. 0.985x)
                    1.0f - deltaFactor
                } else {
                    // Local player is behind -> slightly speed up (e.g. 1.015x)
                    1.0f + deltaFactor
                }
            }

            // Hard resync zone (> 300ms): Keep 1.0f and trigger a hard seek
            else -> 1.0f
        }

        return Pair(driftDeltaMs, speedFactor)
    }

    /**
     * Whether a hard seek is recommended because drift exceeds micro-adjustment tolerance (>300ms).
     */
    fun shouldHardSeek(localPlayerPositionMs: Long, targetPositionMs: Long): Boolean {
        return abs(localPlayerPositionMs - targetPositionMs) > microAdjustMaxThresholdMs
    }

    /**
     * Generates a comprehensive status snapshot for UI feedback and telemetry.
     */
    fun computeSyncStatus(
        localPlayerPositionMs: Long = 0L,
        targetPositionMs: Long = 0L
    ): JamSyncStatus {
        val (drift, speed) = calculatePlaybackAdjustment(localPlayerPositionMs, targetPositionMs)
        val absDrift = abs(drift)

        val quality = when {
            !hasInitialSync -> "Connecting"
            absDrift <= inSyncThresholdMs -> "Synchronized (±${absDrift}ms)"
            absDrift <= microAdjustMaxThresholdMs -> "Aligning (±${absDrift}ms)"
            else -> "Resyncing (${absDrift}ms)"
        }

        return JamSyncStatus(
            mode = syncMode,
            isSynchronized = hasInitialSync && absDrift <= inSyncThresholdMs,
            clockOffsetMs = getClockOffsetMs(),
            roundTripTimeMs = getRoundTripTimeMs(),
            driftDeltaMs = drift,
            playbackSpeedFactor = speed,
            syncQuality = quality,
            lastSyncTimestamp = lastSyncTimestamp
        )
    }
}
