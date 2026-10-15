package com.pixelody.app.core.analytics

import kotlin.math.min
import kotlin.math.roundToLong

/** Immutable metadata captured at activation, independent of later library edits. */
data class ListeningTrack(
    val trackId: String,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
    val format: String = "",
    val isLossless: Boolean = false
)

data class ListeningOccurrence(
    val eventId: String,
    val ordinal: Long,
    val occurrenceId: String,
    val entryId: String,
    val sessionId: String,
    val track: ListeningTrack,
    val startedAtEpochMs: Long,
    val personal: Boolean = true
)

enum class ListeningEndReason {
    NATURAL_END, PERSONAL_SKIP, USER_STOP, REPLACED, INFRASTRUCTURE, REMOTE, SHUTDOWN
}

data class MediaCoverage(val startMs: Long, val endMs: Long)

/** No monotonic clock value is persisted: it cannot be reused across a reboot. */
data class ListeningCheckpoint(
    val occurrence: ListeningOccurrence,
    val listenedWallMs: Long = 0,
    val observedMediaMs: Long = 0,
    val coverage: List<MediaCoverage> = emptyList(),
    val coverageTruncated: Boolean = false
) {
    val uniqueCoverageMs: Long get() = coverage.sumOf { it.endMs - it.startMs }
    val coverageRatio: Double? get() = occurrence.track.durationMs?.takeIf { it > 0 }
        ?.let { (uniqueCoverageMs.toDouble() / it).coerceIn(0.0, 1.0) }
}

data class ListeningEvent(
    val checkpoint: ListeningCheckpoint,
    val endedAtEpochMs: Long,
    val reason: ListeningEndReason,
    val naturalCompletion: Boolean
) {
    val occurrence: ListeningOccurrence get() = checkpoint.occurrence
    val personalSkip: Boolean get() = occurrence.personal && reason == ListeningEndReason.PERSONAL_SKIP
}

/**
 * Service-owned measurement, not a preference reducer. Caller supplies elapsedRealtime,
 * actual Media3 isPlaying and playback speed. Sample immediately at play/buffer/focus
 * changes; call discontinuity at every seek before any later periodic sample.
 *
 * Time is accepted only with plausible forward media progress from a playing sample.
 * Repeated sections count as heard wall time but add unique coverage only once. A gap
 * over five seconds is discarded conservatively (e.g. a suspended service callback).
 */
class RealListeningAccumulator(
    private val maxSampleGapMs: Long = 5_000,
    private val positionToleranceMs: Long = 250,
    private val maxCoverageSegments: Int = 512,
    private val completionPermille: Int = 950
) {
    private data class Sample(val elapsed: Long, val position: Long, val playing: Boolean, val speed: Double)
    private var measured: ListeningCheckpoint? = null
    private var previous: Sample? = null
    private var finished: ListeningEvent? = null
    val active: Boolean get() = measured != null && finished == null

    init {
        require(maxSampleGapMs > 0 && positionToleranceMs >= 0 && maxCoverageSegments in 1..512)
        require(completionPermille in 950..1000)
    }

    fun start(
        occurrence: ListeningOccurrence,
        nowElapsedMs: Long,
        positionMs: Long,
        playing: Boolean,
        speed: Float = 1f
    ) {
        check(!active) { "Finish the active listening occurrence before starting another" }
        require(occurrence.ordinal > 0 && occurrence.eventId.isNotBlank() && occurrence.track.trackId.isNotBlank())
        require(occurrence.track.durationMs == null || occurrence.track.durationMs > 0)
        measured = ListeningCheckpoint(occurrence)
        previous = point(nowElapsedMs, positionMs, playing, speed)
        finished = null
    }

    fun sample(nowElapsedMs: Long, positionMs: Long, playing: Boolean, speed: Float = 1f) {
        if (!active) return
        val next = point(nowElapsedMs, positionMs, playing, speed)
        val last = previous
        previous = next
        if (last == null || !last.playing || !last.speed.isFinite() || last.speed <= 0) return
        val elapsed = next.elapsed - last.elapsed
        val progress = next.position - last.position
        if (elapsed !in 1..maxSampleGapMs || progress <= 0) return
        if (progress.toDouble() > elapsed * last.speed + positionToleranceMs) return
        val value = measured ?: return
        val duration = value.occurrence.track.durationMs
        val start = duration?.let { min(last.position, it) } ?: last.position
        val end = duration?.let { min(next.position, it) } ?: next.position
        if (end <= start) return
        val wall = min(elapsed, ((end - start) / last.speed).roundToLong()).coerceAtLeast(0)
        val (coverage, truncated) = mergeCoverage(value.coverage, MediaCoverage(start, end))
        measured = value.copy(
            listenedWallMs = saturatedAdd(value.listenedWallMs, wall),
            observedMediaMs = saturatedAdd(value.observedMediaMs, end - start),
            coverage = coverage,
            coverageTruncated = value.coverageTruncated || truncated
        )
    }

    /** Flush the old position with sample first, then reset here to exclude the seek. */
    fun discontinuity(nowElapsedMs: Long, positionMs: Long, playing: Boolean, speed: Float = 1f) {
        if (active) previous = point(nowElapsedMs, positionMs, playing, speed)
    }

    fun checkpoint(): ListeningCheckpoint? = measured

    /** Repeated finish callbacks return exactly the same outcome and event ID. */
    fun finish(reason: ListeningEndReason, endedAtEpochMs: Long): ListeningEvent? {
        finished?.let { return it }
        val value = measured ?: return null
        val effectiveReason = if (!value.occurrence.personal && reason == ListeningEndReason.PERSONAL_SKIP)
            ListeningEndReason.REMOTE else reason
        return ListeningEvent(
            value, endedAtEpochMs.coerceAtLeast(0), effectiveReason,
            effectiveReason == ListeningEndReason.NATURAL_END &&
                value.coverageRatio?.let { it >= completionPermille / 1000.0 } == true
        ).also { finished = it; previous = null }
    }

    private fun point(elapsed: Long, position: Long, playing: Boolean, speed: Float) =
        Sample(elapsed.coerceAtLeast(0), position.coerceAtLeast(0), playing, speed.toDouble())

    private fun mergeCoverage(old: List<MediaCoverage>, added: MediaCoverage): Pair<List<MediaCoverage>, Boolean> {
        val merged = ArrayList<MediaCoverage>()
        (old + added).sortedBy { it.startMs }.forEach { interval ->
            val tail = merged.lastOrNull()
            if (tail != null && interval.startMs <= tail.endMs)
                merged[merged.lastIndex] = MediaCoverage(tail.startMs, maxOf(tail.endMs, interval.endMs))
            else merged.add(interval)
        }
        // Discard extra isolated coverage; never fill unheard gaps to fit the bound.
        return if (merged.size > maxCoverageSegments) old to true else merged.toList() to false
    }
}

internal fun saturatedAdd(a: Long, b: Long): Long = if (b > Long.MAX_VALUE - a) Long.MAX_VALUE else a + b
