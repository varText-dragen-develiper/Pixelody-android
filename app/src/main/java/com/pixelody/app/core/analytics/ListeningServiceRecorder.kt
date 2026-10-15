package com.pixelody.app.core.analytics

import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Captured on the player thread; the recorder never reads a Player from its IO thread. */
data class ListeningPlaybackSample(
    val entryId: String,
    val sessionId: String,
    val track: ListeningTrack,
    val positionMs: Long,
    val elapsedMs: Long,
    val epochMs: Long,
    val playing: Boolean,
    val speed: Float = 1f,
    val personal: Boolean = true
)

/** One service lifetime, one serialized writer. No disk work or UI lifetime owns heard time. */
class ListeningServiceRecorder(
    directory: File,
    private val onDiagnostic: (String) -> Unit = {}
) {
    private val io = Executors.newSingleThreadExecutor { task ->
        Thread(task, "Pixelody-listening-ledger").apply { isDaemon = true }
    }
    private val ledger by lazy { ListeningLedgerStore(directory) }
    private var accumulator = RealListeningAccumulator()
    private var activeEntry: String? = null
    private var activeSession: String? = null
    private var lastCheckpointElapsed = 0L
    private var recovered = false
    @Volatile private var closed = false

    fun recover(nowEpochMs: Long) = enqueue { recoverIfNeeded(nowEpochMs) }

    fun resetHistory(): java.util.concurrent.CompletableFuture<Unit> {
        val result = java.util.concurrent.CompletableFuture<Unit>()
        if (closed) return result.apply { completeExceptionally(IllegalStateException("Recorder is closed")) }
        io.execute {
            runCatching {
                ledger.resetHistory()
                accumulator = RealListeningAccumulator()
                activeEntry = null
                activeSession = null
                recovered = true
            }.fold({ result.complete(Unit) }, { result.completeExceptionally(it) })
        }
        return result
    }

    fun sample(sample: ListeningPlaybackSample) = enqueue { observe(sample) }

    fun discontinuity(
        oldPositionMs: Long,
        sample: ListeningPlaybackSample,
        endReason: ListeningEndReason? = null
    ) = enqueue {
        accumulator.sample(sample.elapsedMs, oldPositionMs, sample.playing, sample.speed)
        if (endReason != null) finish(endReason, sample.epochMs)
        accumulator.discontinuity(sample.elapsedMs, sample.positionMs, sample.playing, sample.speed)
        observe(sample)
    }

    fun end(sample: ListeningPlaybackSample?, reason: ListeningEndReason) = enqueue {
        if (sample != null) accumulator.sample(sample.elapsedMs, sample.positionMs, sample.playing, sample.speed)
        finish(reason, sample?.epochMs ?: System.currentTimeMillis())
    }

    private fun observe(sample: ListeningPlaybackSample) {
        if (!recovered) {
            ledger.recoverInterrupted(sample.epochMs)
            recovered = true
        }
        if (accumulator.active && (activeEntry != sample.entryId || activeSession != sample.sessionId))
            finish(ListeningEndReason.REPLACED, sample.epochMs)
        if (!accumulator.active) {
            if (!sample.playing || sample.entryId.isBlank() || sample.track.trackId.isBlank()) return
            val occurrence = ledger.reserveOccurrence(UUID.randomUUID().toString(), sample.entryId,
                sample.sessionId, sample.track, sample.epochMs.coerceAtLeast(0), sample.personal)
            accumulator.start(occurrence, sample.elapsedMs, sample.positionMs, sample.playing, sample.speed)
            activeEntry = sample.entryId
            activeSession = sample.sessionId
            lastCheckpointElapsed = sample.elapsedMs
        } else {
            accumulator.sample(sample.elapsedMs, sample.positionMs, sample.playing, sample.speed)
        }
        if (sample.elapsedMs - lastCheckpointElapsed >= 5_000 || !sample.playing) {
            accumulator.checkpoint()?.let(ledger::checkpoint)
            lastCheckpointElapsed = sample.elapsedMs
        }
    }

    private fun recoverIfNeeded(nowEpochMs: Long) {
        if (!recovered) { ledger.recoverInterrupted(nowEpochMs); recovered = true }
    }

    private fun finish(reason: ListeningEndReason, epochMs: Long) {
        if (!accumulator.active) return
        accumulator.finish(reason, epochMs)?.let(ledger::record)
        activeEntry = null
        activeSession = null
    }

    private fun enqueue(action: () -> Unit) {
        if (closed) return
        io.execute { runCatching(action).onFailure { onDiagnostic(it.javaClass.simpleName) } }
    }

    /** Drain measured facts in FIFO order; no main-thread fsync or blocking shutdown. */
    fun close(sample: ListeningPlaybackSample?) {
        if (closed) return
        end(sample, ListeningEndReason.SHUTDOWN)
        closed = true
        io.shutdown()
    }

    /** Used by lifecycle verification after releasing the controller, off the main thread. */
    fun awaitIdle(timeoutMs: Long = 5_000) {
        if (closed) check(io.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS))
        else io.submit {}.get(timeoutMs, TimeUnit.MILLISECONDS)
    }
}
