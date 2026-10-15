package com.pixelody.app.core.analytics

import com.pixelody.app.core.shuffle.UniversalShuffleLearning as Learning
import com.pixelody.app.data.storage.PlaybackSessionCheckpoint
import com.pixelody.app.data.storage.PlaybackSessionCodec
import com.pixelody.app.data.storage.VersionedPlaybackSessionStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * Actual audio-service samples -> exact shared learning events. One FIFO writer, no
 * wall-clock reads, UI lifetime, metadata estimates or synthetic coverage. The separate
 * factual history recorder remains independent of this optional personal learning.
 */
class UniversalListeningRecorder(directory: File, private val onDiagnostic: (String) -> Unit = {}) {
    private val io = Executors.newSingleThreadExecutor { work -> Thread(work, "Pixelody-universal-learning").apply { isDaemon = true } }
    private val store = UniversalLearningStore(directory)
    private var ledger = Learning.createLedger()
    private var producerId = UUID.randomUUID().toString()
    private var sequence = 0L
    private val clockId = UUID.randomUUID().toString()
    private var active: Active? = null
    private var previous: ListeningPlaybackSample? = null
    private var recovered = false
    private var naturalReplayEntry: Pair<String, String>? = null
    @Volatile private var closed = false
    @Volatile private var published = Learning.createLedger(JSONObject().put("enabled", false)).toString()
    @Volatile private var publishedReadOnly = false
    @Volatile private var publishedSignals = SignalSnapshot(false, 0, emptyMap())
    private data class Active(val id: String, val entryId: String, val sessionId: String, val trackId: String, val personal: Boolean)
    private data class SignalSnapshot(val enabled: Boolean, val clockFloorMs: Long, val rows: Map<String, JSONObject>)

    init { dispatch {
        val loaded = store.load()
        ledger = loaded.ledger; producerId = loaded.producerId; sequence = loaded.sequence
        publishedReadOnly = store.readOnly
        publish()
    } }

    fun snapshot(): JSONObject = JSONObject(published)
    fun readOnly(): Boolean = publishedReadOnly
    fun signals(trackIds: List<String>, nowMs: Long): JSONObject {
        val captured = if (publishedReadOnly) SignalSnapshot(false, 0, emptyMap()) else publishedSignals
        val context = JSONObject().put("nowMs", nowMs)
        // trackSignals is the frozen reference implementation and scans its tracks.
        // The IO writer publishes an index over private immutable rows; give the
        // policy just the matching row so context construction stays O(eligible
        // tracks) and does not parse the full ledger on the player thread.
        val selected = JSONObject().put("schemaVersion", Learning.VERSION)
            .put("policyVersion", Learning.POLICY_VERSION).put("enabled", captured.enabled)
            .put("clockFloorMs", captured.clockFloorMs)
        val signals = JSONArray()
        trackIds.distinct().forEach { trackId ->
            selected.put("tracks", JSONArray().apply { captured.rows[trackId]?.let { put(it) } })
            signals.put(Learning.trackSignals(selected, trackId, context).put("trackId", trackId))
        }
        return JSONObject().put("nowMs", nowMs).put("signals", signals)
    }

    /** Called on service creation even if restoration has no playable current item. */
    fun recover(nowEpochMs: Long): CompletableFuture<JSONObject> = dispatch { recoverOnce(nowEpochMs); snapshot() }
    fun offered(trackId: String, nowMs: Long, decisionId: String? = null): CompletableFuture<JSONObject> = dispatch {
        recoverOnce(nowMs)
        val fields = JSONObject().put("trackId", trackId)
        if (decisionId != null) {
            // Structural tuple serialization, never delimiter-based identity comparison.
            val bytes = JSONArray().put("offered").put(decisionId).put(trackId).toString().toByteArray(Charsets.UTF_8)
            fields.put("eventId", MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
        }
        if (ledger.getBoolean("enabled")) emit("offered", fields, nowMs)
        snapshot()
    }
    fun sample(sample: ListeningPlaybackSample): CompletableFuture<JSONObject> = dispatch { observe(sample); snapshot() }
    fun discontinuity(oldPositionMs: Long, sample: ListeningPlaybackSample, endReason: ListeningEndReason? = null): CompletableFuture<JSONObject> = dispatch {
        recoverOnce(sample.epochMs)
        flush(sample.copy(positionMs = oldPositionMs))
        if (endReason != null) finish(endReason, sample.epochMs)
        previous = sample
        observe(sample)
        snapshot()
    }
    fun end(sample: ListeningPlaybackSample?, reason: ListeningEndReason, nowEpochMs: Long = sample?.epochMs ?: 0): CompletableFuture<JSONObject> = dispatch {
        recoverOnce(nowEpochMs)
        if (sample != null && matches(sample)) flush(sample)
        finish(reason, nowEpochMs)
        snapshot()
    }

    fun setEnabled(enabled: Boolean, nowMs: Long): CompletableFuture<JSONObject> = dispatch {
        recoverOnce(nowMs)
        emit("learning-enabled", JSONObject().put("enabled", enabled), nowMs)
        active = null; previous = null
        snapshot()
    }
    fun resetLearning(nowMs: Long): CompletableFuture<JSONObject> = dispatch {
        recoverOnce(nowMs); emit("learning-reset", JSONObject(), nowMs); active = null; previous = null; snapshot()
    }
    fun deleteHistory(nowMs: Long): CompletableFuture<JSONObject> = dispatch {
        recoverOnce(nowMs); emit("history-delete", JSONObject(), nowMs); snapshot()
    }
    fun deleteTrack(trackId: String, nowMs: Long): CompletableFuture<JSONObject> = dispatch {
        recoverOnce(nowMs); emit("track-delete", JSONObject().put("trackId", trackId), nowMs)
        if (active?.trackId == trackId) { active = null; previous = null }
        snapshot()
    }
    fun preference(trackId: String, value: Int, nowMs: Long): CompletableFuture<JSONObject> = dispatch {
        recoverOnce(nowMs); emit("preference", JSONObject().put("trackId", trackId).put("value", value), nowMs); snapshot()
    }
    fun exportPrivate(includePrivate: Boolean = false): CompletableFuture<JSONObject> = dispatch {
        Learning.exportLedger(ledger, JSONObject().put("includePrivate", includePrivate))
    }
    fun importPrivate(value: Any?, nowMs: Long): CompletableFuture<JSONObject> = dispatch {
        check(!store.readOnly) { "Future or damaged learning state is preserved read-only" }
        val result = Learning.importLedger(value)
        if (result.getBoolean("ok")) {
            val imported = result.getJSONObject("ledger")
            val freshProducer = UUID.randomUUID().toString()
            store.save(imported, freshProducer, 0)
            ledger = imported; producerId = freshProducer; sequence = 0; active = null; previous = null; recovered = false
            publish(); recoverOnce(nowMs)
        }
        // The accepted result contains the imported object: do not expose the
        // writer's live ledger or the private signal index to a mutable caller.
        JSONObject(result.toString())
    }
    fun deleteLedger(nowMs: Long): CompletableFuture<JSONObject> = dispatch {
        val fresh = Learning.deleteLedger(ledger, JSONObject().put("nowMs", nowMs))
        val freshProducer = UUID.randomUUID().toString()
        store.save(fresh, freshProducer, 0)
        ledger = fresh; producerId = freshProducer; sequence = 0; active = null; previous = null; recovered = true
        publish(); snapshot()
    }

    private fun observe(sample: ListeningPlaybackSample) {
        recoverOnce(sample.epochMs)
        if (store.readOnly || !ledger.getBoolean("enabled")) { active = null; previous = null; return }
        if (active != null && !matches(sample)) finish(ListeningEndReason.REPLACED, sample.epochMs)
        if (active == null) {
            if (!sample.playing || sample.entryId.isBlank() || sample.sessionId.isBlank() || sample.track.trackId.isBlank()) return
            val occurrenceId = UUID.randomUUID().toString()
            val duration = sample.track.durationMs?.takeIf { it in 1..Learning.CONFIG.getValue("maxDurationMs") }
            if (!emit("activated", JSONObject().put("trackId", sample.track.trackId).put("occurrenceId", occurrenceId)
                    .put("durationMs", duration ?: JSONObject.NULL).put("personal", sample.personal)
                    .put("replay", naturalReplayEntry == (sample.entryId to sample.sessionId)), sample.epochMs)) return
            naturalReplayEntry = null
            active = Active(occurrenceId, sample.entryId, sample.sessionId, sample.track.trackId, sample.personal)
            previous = sample
        } else flush(sample)
    }
    private fun matches(sample: ListeningPlaybackSample): Boolean = active?.let {
        it.entryId == sample.entryId && it.sessionId == sample.sessionId && it.trackId == sample.track.trackId && it.personal == sample.personal
    } == true
    private fun flush(sample: ListeningPlaybackSample) {
        val current = active ?: return
        val old = previous ?: return
        previous = sample
        val elapsed = sample.elapsedMs - old.elapsedMs
        // A delayed handler cannot prove uninterrupted playback; discard gaps conservatively.
        if (!old.playing || elapsed !in 1..5_000 || !old.speed.isFinite()) return
        val speed = (old.speed * 1000).roundToInt()
        if (speed !in 250..4000 || old.positionMs !in 0..86_400_000 || sample.positionMs !in 0..86_400_000) return
        emit("interval", JSONObject().put("trackId", current.trackId).put("occurrenceId", current.id)
            .put("personal", current.personal).put("monotonicClockId", clockId)
            .put("monotonicStartMs", old.elapsedMs).put("monotonicEndMs", sample.elapsedMs)
            .put("mediaStartMs", old.positionMs).put("mediaEndMs", sample.positionMs)
            .put("speedPermille", speed).put("playing", true).put("discontinuity", false), sample.epochMs)
    }
    private fun finish(reason: ListeningEndReason, nowMs: Long) {
        val current = active ?: return
        val outcome = when (reason) {
            ListeningEndReason.NATURAL_END -> "natural-end"
            ListeningEndReason.PERSONAL_SKIP -> "deliberate-skip"
            else -> "interrupted"
        }
        val label = when (reason) {
            ListeningEndReason.NATURAL_END -> "natural-end"
            ListeningEndReason.PERSONAL_SKIP -> if (current.personal) "manual-skip" else "remote-action"
            ListeningEndReason.USER_STOP -> "pause"
            ListeningEndReason.REPLACED -> "track-change"
            ListeningEndReason.INFRASTRUCTURE -> "playback-error"
            ListeningEndReason.REMOTE -> "remote-action"
            ListeningEndReason.SHUTDOWN -> "shutdown"
        }
        emit("outcome", JSONObject().put("trackId", current.trackId).put("occurrenceId", current.id)
            .put("personal", current.personal).put("outcome", outcome).put("reason", label)
            .put("userInitiated", reason == ListeningEndReason.PERSONAL_SKIP && current.personal), nowMs)
        naturalReplayEntry = if (reason == ListeningEndReason.NATURAL_END) current.entryId to current.sessionId else null
        active = null; previous = null
    }
    private fun recoverOnce(nowMs: Long) {
        if (recovered || store.readOnly) return
        val occurrences = ledger.getJSONArray("occurrences")
        val open = (0 until occurrences.length()).map { occurrences.getJSONObject(it) }.filter { !it.getBoolean("ended") }
        open.forEach { emit("outcome", JSONObject().put("trackId", it.getString("trackId")).put("occurrenceId", it.getString("occurrenceId"))
            .put("outcome", "interrupted").put("reason", "crash"), nowMs) }
        recovered = true
    }
    private fun emit(type: String, fields: JSONObject, nowMs: Long): Boolean {
        check(!store.readOnly) { "Future or damaged learning state is preserved read-only" }
        check(sequence < 9_007_199_254_740_991L) { "Learning producer sequence exhausted" }
        val event = JSONObject(fields.toString()).put("schemaVersion", Learning.VERSION).put("type", type)
            .put("eventId", fields.optString("eventId", UUID.randomUUID().toString())).put("producerId", producerId).put("sequence", sequence + 1)
            .put("learningEpoch", ledger.getLong("learningEpoch")).put("timestampMs", nowMs)
        val next = Learning.reduce(ledger, event, JSONObject().put("nowMs", nowMs))
        if (next === ledger) return false
        store.save(next, producerId, sequence + 1)
        ledger = next; sequence++; publish()
        return true
    }
    private fun publish() {
        val tracks = ledger.getJSONArray("tracks")
        val rows = HashMap<String, JSONObject>(tracks.length())
        repeat(tracks.length()) { val row = tracks.getJSONObject(it); rows[row.getString("trackId")] = row }
        publishedSignals = SignalSnapshot(ledger.getBoolean("enabled"), ledger.getLong("clockFloorMs"), rows)
        published = ledger.toString(); publishedReadOnly = store.readOnly
    }
    private fun <T> dispatch(block: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        if (closed) { future.completeExceptionally(IllegalStateException("Learning recorder closed")); return future }
        io.execute { try { future.complete(block()) } catch (error: Exception) {
            publishedReadOnly = store.readOnly; onDiagnostic(error.javaClass.simpleName); future.completeExceptionally(error)
        } }
        return future
    }
    fun close(sample: ListeningPlaybackSample?, nowEpochMs: Long = sample?.epochMs ?: 0) {
        if (closed) return
        end(sample, ListeningEndReason.SHUTDOWN, nowEpochMs)
        closed = true; io.shutdown()
    }
    fun awaitIdle(timeoutMs: Long = 5_000) {
        if (closed) check(io.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) else io.submit {}.get(timeoutMs, TimeUnit.MILLISECONDS)
    }
}

/** Validates both inner learning schemas before using the shared atomic envelope store. */
private class UniversalLearningStore(directory: File) {
    private val files = VersionedPlaybackSessionStore(directory, "universal-learning")
    var readOnly = false; private set
    private var revision = 0L
    data class Loaded(val ledger: JSONObject, val producerId: String, val sequence: Long)
    private data class Candidate(val file: File, val checkpoint: PlaybackSessionCheckpoint?, val loaded: Loaded?, val invalidInner: Boolean = false)
    fun load(): Loaded {
        val envelope = files.load()
        readOnly = envelope.readOnly
        val candidates = inspect()
        val selected = candidates.firstOrNull { it.loaded != null }
        revision = selected?.checkpoint?.revision ?: 0
        if (selected != null) return selected.loaded!!
        if (candidates.any { it.file.exists() }) readOnly = true
        return Loaded(Learning.createLedger(JSONObject().put("enabled", !readOnly)), UUID.randomUUID().toString(), 0)
    }
    fun save(ledger: JSONObject, producerId: String, sequence: Long) {
        check(!readOnly) { "Learning state is read-only" }
        val candidates = inspect()
        check(!readOnly) { "Future learning state preserved" }
        // A valid outer envelope with invalid inner JSON must not displace a valid
        // inner backup. Quarantine it before the shared store rotates its backup.
        candidates.filter { it.invalidInner }.forEach {
            Files.move(it.file.toPath(), File(it.file.parentFile, "${it.file.name}.corrupt-${UUID.randomUUID()}").toPath(), StandardCopyOption.ATOMIC_MOVE)
        }
        val wrapper = JSONObject().put("bridgeVersion", 1).put("producerId", producerId).put("sequence", sequence).put("ledger", ledger)
        check(Learning.importLedger(ledger).getBoolean("ok")) { "Invalid generated learning state" }
        val checkpoint = PlaybackSessionCheckpoint("universal-learning", revision + 1, emptyList(), policyStateJson = wrapper.toString())
        val saved = files.save(checkpoint)
        if (!saved.saved) { readOnly = saved.readOnly; error(saved.diagnostics.joinToString { it.code }) }
        revision++
    }
    private fun inspect(): List<Candidate> = listOf(files.primaryFile, files.backupFile).map { file ->
        if (!file.exists()) return@map Candidate(file, null, null)
        if (file.length() > PlaybackSessionCodec.MAX_FILE_BYTES) { readOnly = true; return@map Candidate(file, null, null) }
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrElse { readOnly = true; return@map Candidate(file, null, null) }
        val version = runCatching { PlaybackSessionCodec.schemaVersion(raw) }.getOrNull()
        if (version != null && version > PlaybackSessionCodec.SCHEMA_VERSION) { readOnly = true; return@map Candidate(file, null, null) }
        val checkpoint = runCatching { PlaybackSessionCodec.decode(raw).checkpoint }.getOrNull() ?: return@map Candidate(file, null, null)
        val wrapper = runCatching { JSONObject(checkpoint.policyStateJson) }.getOrNull() ?: return@map Candidate(file, checkpoint, null, true)
        val bridgeVersion = wrapper.opt("bridgeVersion")
        if (bridgeVersion !is Number || bridgeVersion.toDouble() != 1.0 ||
            wrapper.keys().asSequence().toSet() != setOf("bridgeVersion", "producerId", "sequence", "ledger")) {
            readOnly = true; return@map Candidate(file, checkpoint, null)
        }
        val imported = Learning.importLedger(wrapper.opt("ledger"))
        if (!imported.getBoolean("ok")) {
            if (imported.getString("reason") == "unsupported-version") readOnly = true
            return@map Candidate(file, checkpoint, null, !readOnly)
        }
        val sequence = wrapper.opt("sequence")
        val producer = wrapper.opt("producerId")
        val sequenceValid = sequence is Number && sequence.toDouble().isFinite() && sequence.toDouble() % 1 == 0.0 && sequence.toDouble() in 0.0..9_007_199_254_740_991.0
        val producerValid = Learning.normalizeEvent(JSONObject().put("schemaVersion", 2).put("eventId", "validate")
            .put("type", "offered").put("trackId", "validate").put("producerId", producer ?: JSONObject.NULL).put("sequence", 1), JSONObject().put("nowMs", 0)) != null
        if (!sequenceValid || !producerValid) return@map Candidate(file, checkpoint, null, true)
        val decodedLedger = imported.getJSONObject("ledger")
        val producers = decodedLedger.getJSONArray("producers")
        val watermark = (0 until producers.length()).map { producers.getJSONObject(it) }.find { it.getString("producerId") == producer }
        val seq = (sequence as Number).toLong()
        if ((seq == 0L && watermark != null) || (seq > 0 && (watermark == null || watermark.getLong("through") != seq || watermark.getJSONArray("pending").length() != 0)))
            return@map Candidate(file, checkpoint, null, true)
        Candidate(file, checkpoint, Loaded(decodedLedger, producer as String, seq))
    }
}
