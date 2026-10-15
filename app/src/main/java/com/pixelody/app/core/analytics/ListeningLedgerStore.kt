package com.pixelody.app.core.analytics

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class ListeningTrackAggregate(
    val trackId: String,
    val activatedCount: Long = 0,
    val heardCount: Long = 0,
    val naturalEndCount: Long = 0,
    val completedCount: Long = 0,
    val personalSkipCount: Long = 0,
    val listenedWallMs: Long = 0,
    val observedMediaMs: Long = 0,
    val uniqueCoverageMs: Long = 0,
    val lastExposureAtEpochMs: Long = 0,
    val lastHeardAtEpochMs: Long = 0
)

data class ListeningDayAggregate(
    val dateKey: String,
    val listenedWallMs: Long = 0,
    val heardCount: Long = 0,
    val losslessCount: Long = 0,
    val formatCounts: Map<String, Long> = emptyMap(),
    val lastEndedAtEpochMs: Long = 0
)

data class ListeningLedgerSnapshot(
    val revision: Long = 0,
    val nextOrdinal: Long = 1,
    val finalizedOrdinal: Long = 0,
    val pending: ListeningCheckpoint? = null,
    val recentEvents: List<ListeningEvent> = emptyList(),
    val tracks: Map<String, ListeningTrackAggregate> = emptyMap(),
    val days: Map<String, ListeningDayAggregate> = emptyMap(),
    val readOnly: Boolean = false,
    val diagnostics: List<String> = emptyList()
)

/**
 * Dedicated versioned ledger; never opens or upgrades the existing product database.
 * Call from a single service IO executor. Reader instances may call reload from IO.
 * Recent detail and day summaries are bounded; lifetime per-track exposure totals are
 * retained. At the explicit track bound new recording fails without erasing old totals.
 *
 * The durable ordinal watermark rejects retries even after the original event was
 * pruned. Only the currently reserved occurrence may finalize; event IDs alone cannot
 * append arbitrary historical events. Crash recovery finalizes its last checkpoint as
 * SHUTDOWN, never completion/dislike, before a new confirmed activation is reserved.
 */
class ListeningLedgerStore(
    private val directory: File,
    private val maxRecentEvents: Int = 256,
    private val maxDays: Int = 400,
    private val maxTrackAggregates: Int = 100_000,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val primary = File(directory, "ledger.json")
    private val backup = File(directory, "ledger.previous.json")
    private var state: ListeningLedgerSnapshot
    private var primaryWasValid = false

    init {
        require(maxRecentEvents in 1..4096 && maxDays in 7..3660 && maxTrackAggregates > 0)
        state = readDisk()
    }

    @Synchronized fun snapshot(): ListeningLedgerSnapshot = state
    @Synchronized fun reload(): ListeningLedgerSnapshot = readDisk().also { state = it }

    /** An explicit developer reset retains the ordinal fence against stale outcomes. */
    @Synchronized fun resetHistory() {
        checkWritable()
        val empty = state.copy(finalizedOrdinal = state.nextOrdinal - 1, pending = null,
            recentEvents = emptyList(), tracks = emptyMap(), days = emptyMap())
        commit(empty)
        // Recovery must not resurrect the pre-reset history from the previous copy.
        commit(empty)
    }

    @Synchronized fun reserveOccurrence(
        occurrenceId: String,
        entryId: String,
        sessionId: String,
        track: ListeningTrack,
        startedAtEpochMs: Long,
        personal: Boolean = true
    ): ListeningOccurrence {
        checkWritable()
        state.pending?.let {
            check(it.occurrence.occurrenceId == occurrenceId && it.occurrence.entryId == entryId &&
                it.occurrence.sessionId == sessionId && it.occurrence.track == track &&
                it.occurrence.personal == personal) { "Finish or recover the pending occurrence first" }
            return it.occurrence
        }
        require(occurrenceId.isNotBlank() && entryId.isNotBlank() && sessionId.isNotBlank())
        require(track.trackId.isNotBlank() && (track.durationMs == null || track.durationMs > 0))
        require(startedAtEpochMs >= 0)
        check(state.nextOrdinal < Long.MAX_VALUE) { "Listening ordinal exhausted" }
        check(track.trackId in state.tracks || state.tracks.size < maxTrackAggregates) { "Listening track summary capacity reached" }
        val occurrence = ListeningOccurrence(UUID.randomUUID().toString(), state.nextOrdinal,
            occurrenceId, entryId, sessionId, track, startedAtEpochMs, personal)
        commit(state.copy(nextOrdinal = state.nextOrdinal + 1, pending = ListeningCheckpoint(occurrence)))
        return occurrence
    }

    @Synchronized fun checkpoint(value: ListeningCheckpoint): Boolean {
        checkWritable()
        if (value.occurrence.ordinal <= state.finalizedOrdinal) return false
        val pending = state.pending ?: return false
        require(value.occurrence == pending.occurrence) { "Stale or mismatched listening checkpoint" }
        validateCheckpoint(value)
        require(value.listenedWallMs >= pending.listenedWallMs && value.observedMediaMs >= pending.observedMediaMs)
        require(value.uniqueCoverageMs >= pending.uniqueCoverageMs)
        if (value == pending) return false
        commit(state.copy(pending = value))
        return true
    }

    @Synchronized fun record(event: ListeningEvent): Boolean {
        checkWritable()
        if (event.occurrence.ordinal <= state.finalizedOrdinal) return false
        val pending = state.pending ?: return false
        require(event.occurrence == pending.occurrence) { "Only the reserved occurrence can finalize" }
        validateEvent(event)
        require(event.checkpoint.listenedWallMs >= pending.listenedWallMs &&
            event.checkpoint.observedMediaMs >= pending.observedMediaMs &&
            event.checkpoint.uniqueCoverageMs >= pending.uniqueCoverageMs)
        val cp = event.checkpoint
        val occurrence = event.occurrence
        val heard = cp.listenedWallMs > 0
        val old = state.tracks[occurrence.track.trackId] ?: ListeningTrackAggregate(occurrence.track.trackId)
        val summary = old.copy(
            activatedCount = saturatedAdd(old.activatedCount, 1),
            heardCount = saturatedAdd(old.heardCount, if (heard) 1 else 0),
            naturalEndCount = saturatedAdd(old.naturalEndCount, if (event.reason == ListeningEndReason.NATURAL_END) 1 else 0),
            completedCount = saturatedAdd(old.completedCount, if (event.naturalCompletion) 1 else 0),
            personalSkipCount = saturatedAdd(old.personalSkipCount, if (event.personalSkip) 1 else 0),
            listenedWallMs = saturatedAdd(old.listenedWallMs, cp.listenedWallMs),
            observedMediaMs = saturatedAdd(old.observedMediaMs, cp.observedMediaMs),
            uniqueCoverageMs = saturatedAdd(old.uniqueCoverageMs, cp.uniqueCoverageMs),
            lastExposureAtEpochMs = maxOf(old.lastExposureAtEpochMs, occurrence.startedAtEpochMs),
            lastHeardAtEpochMs = maxOf(old.lastHeardAtEpochMs, if (heard) event.endedAtEpochMs else 0)
        )
        val days = state.days.toMutableMap()
        if (heard) {
            // Rollback/future timestamps are preserved as supplied, not converted to "today".
            val date = Instant.ofEpochMilli(event.endedAtEpochMs).atZone(zoneId).toLocalDate().toString()
            val day = days[date] ?: ListeningDayAggregate(date)
            val formats = day.formatCounts.toMutableMap()
            val format = occurrence.track.format.trim().uppercase(java.util.Locale.ROOT)
            if (format.isNotEmpty()) formats[format] = saturatedAdd(formats[format] ?: 0, 1)
            days[date] = day.copy(listenedWallMs = saturatedAdd(day.listenedWallMs, cp.listenedWallMs),
                heardCount = saturatedAdd(day.heardCount, 1),
                losslessCount = saturatedAdd(day.losslessCount, if (occurrence.track.isLossless) 1 else 0), formatCounts = formats,
                lastEndedAtEpochMs = maxOf(day.lastEndedAtEpochMs, event.endedAtEpochMs))
        }
        commit(state.copy(finalizedOrdinal = occurrence.ordinal, pending = null,
            recentEvents = (state.recentEvents + event).takeLast(maxRecentEvents),
            tracks = state.tracks + (summary.trackId to summary),
            days = days.toSortedMap().entries.toList().takeLast(maxDays).associate { it.toPair() }))
        return true
    }

    /** Recovery must happen once before reserving a new actual playback occurrence. */
    @Synchronized fun recoverInterrupted(nowEpochMs: Long): Boolean {
        val pending = state.pending ?: return false
        return record(ListeningEvent(pending, nowEpochMs.coerceAtLeast(0), ListeningEndReason.SHUTDOWN, false))
    }

    private fun checkWritable() { check(!state.readOnly) { state.diagnostics.joinToString("; ") } }

    private fun readDisk(): ListeningLedgerSnapshot {
        primaryWasValid = false
        if (!primary.exists() && !backup.exists()) return ListeningLedgerSnapshot()
        val errors = mutableListOf<String>()
        unsupportedSchema()?.let { return ListeningLedgerSnapshot(readOnly = true, diagnostics = listOf(it)) }
        for (file in listOf(primary, backup)) {
            if (!file.exists()) continue
            try {
                if (file.length() > MAX_FILE_BYTES) throw IOException("Ledger exceeds size limit")
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val version = json.optInt("schemaVersion", -1)
                if (version != SCHEMA_VERSION) {
                    // A newer primary is authoritative even if an older backup is readable.
                    return ListeningLedgerSnapshot(readOnly = true, diagnostics = errors +
                        "Preserved ${file.name}: unsupported listening schema $version")
                }
                val decoded = decode(json)
                primaryWasValid = file == primary
                return decoded.copy(diagnostics = errors + if (file == backup) listOf("Recovered listening ledger from last valid backup") else emptyList())
            } catch (error: Exception) {
                errors.add("Cannot read ${file.name}: ${error.javaClass.simpleName}")
            }
        }
        return ListeningLedgerSnapshot(readOnly = true, diagnostics = errors + "No valid listening ledger; files preserved")
    }

    private fun commit(proposed: ListeningLedgerSnapshot) {
        checkWritable()
        check(directory.exists() || directory.mkdirs()) { "Cannot create listening ledger directory" }
        // Check BOTH files before rotation; a future backup must not be overwritten.
        unsupportedSchema()?.let { diagnostic ->
            state = state.copy(readOnly = true, diagnostics = state.diagnostics + diagnostic)
            checkWritable()
        }
        if (primary.exists()) {
            val current = runCatching { decode(JSONObject(primary.readText(Charsets.UTF_8))) }.getOrNull()
            primaryWasValid = current != null
            check(current == null || current.revision == state.revision) { "Listening ledger changed externally; reload before writing" }
        }
        val next = proposed.copy(revision = state.revision + 1)
        val encoded = encode(next).toString().toByteArray(Charsets.UTF_8)
        require(encoded.size <= MAX_FILE_BYTES) { "Listening ledger capacity reached; existing data retained" }
        val temporary = File(directory, "ledger.next.json")
        FileOutputStream(temporary).use { it.write(encoded); it.fd.sync() }
        if (primary.exists()) {
            if (primaryWasValid) {
                val backupTemporary = File(directory, "ledger.previous.next.json")
                FileOutputStream(backupTemporary).use { it.write(primary.readBytes()); it.fd.sync() }
                replace(backupTemporary, backup)
            } else {
                // Preserve damaged primary before restoring the backup as writable current state.
                primary.copyTo(File(directory, "ledger.corrupt-${UUID.randomUUID()}.json"))
            }
        }
        replace(temporary, primary)
        primaryWasValid = true
        state = next
    }

    private fun unsupportedSchema(): String? {
        for (file in listOf(primary, backup)) {
            if (!file.exists()) continue
            if (file.length() > MAX_FILE_BYTES) return "Preserved ${file.name}: oversized listening document; writes stopped"
            val json = runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull() ?: continue
            val version = json.optInt("schemaVersion", -1)
            val aggregates = json.optInt("aggregateVersion", -1)
            if (version != SCHEMA_VERSION || (json.has("aggregateVersion") && aggregates != 1))
                return "Preserved ${file.name}: unsupported listening schema $version / aggregates $aggregates"
        }
        return null
    }

    private fun replace(source: File, target: File) {
        try { Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_FILE_BYTES = 32 * 1024 * 1024

        private fun validateCheckpoint(cp: ListeningCheckpoint) {
            val o = cp.occurrence
            require(o.ordinal > 0 && o.eventId.isNotBlank() && o.occurrenceId.isNotBlank() && o.entryId.isNotBlank() && o.sessionId.isNotBlank())
            require(o.track.trackId.isNotBlank() && (o.track.durationMs == null || o.track.durationMs > 0) && o.startedAtEpochMs >= 0)
            require(cp.listenedWallMs >= 0 && cp.observedMediaMs >= 0 && cp.coverage.size <= 512)
            var end = -1L
            cp.coverage.forEach {
                require(it.startMs >= 0 && it.endMs > it.startMs && it.startMs > end)
                require(o.track.durationMs == null || it.endMs <= o.track.durationMs)
                end = it.endMs
            }
            require(cp.uniqueCoverageMs <= cp.observedMediaMs)
        }

        private fun validateEvent(e: ListeningEvent) {
            validateCheckpoint(e.checkpoint)
            require(e.endedAtEpochMs >= 0)
            require(!e.naturalCompletion || (e.reason == ListeningEndReason.NATURAL_END && e.checkpoint.coverageRatio?.let { it >= 0.95 } == true))
            require(e.occurrence.personal || e.reason != ListeningEndReason.PERSONAL_SKIP)
        }

        private fun checkpointJson(cp: ListeningCheckpoint): JSONObject = JSONObject().apply {
            val o = cp.occurrence
            put("eventId", o.eventId); put("ordinal", o.ordinal); put("occurrenceId", o.occurrenceId)
            put("entryId", o.entryId); put("sessionId", o.sessionId); put("startedAtEpochMs", o.startedAtEpochMs); put("personal", o.personal)
            put("track", JSONObject().apply {
                put("trackId", o.track.trackId); put("title", o.track.title); put("artist", o.track.artist); put("album", o.track.album)
                put("artworkUrl", o.track.artworkUrl ?: JSONObject.NULL); put("durationMs", o.track.durationMs ?: JSONObject.NULL)
                put("format", o.track.format); put("isLossless", o.track.isLossless)
            })
            put("listenedWallMs", cp.listenedWallMs); put("observedMediaMs", cp.observedMediaMs); put("coverageTruncated", cp.coverageTruncated)
            put("coverage", JSONArray().apply { cp.coverage.forEach { put(JSONArray().put(it.startMs).put(it.endMs)) } })
        }

        private fun decodeCheckpoint(j: JSONObject): ListeningCheckpoint {
            val t = j.getJSONObject("track")
            val track = ListeningTrack(t.getString("trackId"), t.getString("title"), t.getString("artist"), t.getString("album"),
                if (t.isNull("artworkUrl")) null else t.getString("artworkUrl"),
                if (t.isNull("durationMs")) null else t.getLong("durationMs"), t.getString("format"), t.getBoolean("isLossless"))
            val coverage = j.getJSONArray("coverage")
            require(coverage.length() <= 512)
            return ListeningCheckpoint(ListeningOccurrence(j.getString("eventId"), j.getLong("ordinal"),
                j.getString("occurrenceId"), j.getString("entryId"), j.getString("sessionId"), track,
                j.getLong("startedAtEpochMs"), j.getBoolean("personal")), j.getLong("listenedWallMs"), j.getLong("observedMediaMs"),
                List(coverage.length()) { coverage.getJSONArray(it).let { a -> MediaCoverage(a.getLong(0), a.getLong(1)) } },
                j.getBoolean("coverageTruncated")).also { validateCheckpoint(it) }
        }

        private fun encode(s: ListeningLedgerSnapshot): JSONObject = JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION); put("aggregateVersion", 1); put("revision", s.revision)
            put("nextOrdinal", s.nextOrdinal); put("finalizedOrdinal", s.finalizedOrdinal)
            put("pending", s.pending?.let { checkpointJson(it) } ?: JSONObject.NULL)
            put("recentEvents", JSONArray().apply { s.recentEvents.forEach { e ->
                put(JSONObject().put("checkpoint", checkpointJson(e.checkpoint)).put("endedAtEpochMs", e.endedAtEpochMs)
                    .put("reason", e.reason.name).put("naturalCompletion", e.naturalCompletion))
            } })
            put("tracks", JSONArray().apply { s.tracks.values.sortedBy { it.trackId }.forEach { a ->
                put(JSONObject().put("trackId", a.trackId).put("activatedCount", a.activatedCount).put("heardCount", a.heardCount)
                    .put("naturalEndCount", a.naturalEndCount).put("completedCount", a.completedCount).put("personalSkipCount", a.personalSkipCount)
                    .put("listenedWallMs", a.listenedWallMs).put("observedMediaMs", a.observedMediaMs).put("uniqueCoverageMs", a.uniqueCoverageMs)
                    .put("lastExposureAtEpochMs", a.lastExposureAtEpochMs).put("lastHeardAtEpochMs", a.lastHeardAtEpochMs))
            } })
            put("days", JSONArray().apply { s.days.values.sortedBy { it.dateKey }.forEach { d ->
                put(JSONObject().put("dateKey", d.dateKey).put("listenedWallMs", d.listenedWallMs).put("heardCount", d.heardCount)
                    .put("losslessCount", d.losslessCount).put("formatCounts", JSONObject(d.formatCounts))
                    .put("lastEndedAtEpochMs", d.lastEndedAtEpochMs))
            } })
        }

        private fun decode(j: JSONObject): ListeningLedgerSnapshot {
            require(j.getInt("aggregateVersion") == 1)
            val events = j.getJSONArray("recentEvents")
            val tracks = j.getJSONArray("tracks")
            val days = j.getJSONArray("days")
            require(events.length() <= 4096 && tracks.length() <= 100_000 && days.length() <= 3660)
            val decodedEvents = List(events.length()) { i -> events.getJSONObject(i).let { e ->
                ListeningEvent(decodeCheckpoint(e.getJSONObject("checkpoint")), e.getLong("endedAtEpochMs"),
                    ListeningEndReason.valueOf(e.getString("reason")), e.getBoolean("naturalCompletion")).also { validateEvent(it) }
            } }
            val decodedTracks = List(tracks.length()) { i -> tracks.getJSONObject(i).let { t ->
                ListeningTrackAggregate(t.getString("trackId"), t.getLong("activatedCount"), t.getLong("heardCount"), t.getLong("naturalEndCount"),
                    t.getLong("completedCount"), t.getLong("personalSkipCount"), t.getLong("listenedWallMs"), t.getLong("observedMediaMs"),
                    t.getLong("uniqueCoverageMs"), t.getLong("lastExposureAtEpochMs"), t.getLong("lastHeardAtEpochMs")).also {
                    require(it.trackId.isNotBlank() && listOf(it.activatedCount, it.heardCount, it.naturalEndCount, it.completedCount,
                        it.personalSkipCount, it.listenedWallMs, it.observedMediaMs, it.uniqueCoverageMs, it.lastExposureAtEpochMs,
                        it.lastHeardAtEpochMs).all { n -> n >= 0 })
                    require(it.heardCount <= it.activatedCount && it.completedCount <= it.naturalEndCount && it.naturalEndCount <= it.activatedCount)
                }
            } }
            val decodedDays = List(days.length()) { i -> days.getJSONObject(i).let { d ->
                val f = d.getJSONObject("formatCounts")
                val formats = f.keys().asSequence().associateWith { f.getLong(it).also { n -> require(n >= 0) } }
                ListeningDayAggregate(d.getString("dateKey"), d.getLong("listenedWallMs"), d.getLong("heardCount"), d.getLong("losslessCount"),
                    formats, d.optLong("lastEndedAtEpochMs", 0)).also {
                    LocalDate.parse(it.dateKey)
                    require(it.listenedWallMs >= 0 && it.heardCount >= 0 && it.losslessCount in 0..it.heardCount && it.lastEndedAtEpochMs >= 0)
                }
            } }
            val s = ListeningLedgerSnapshot(j.getLong("revision"), j.getLong("nextOrdinal"), j.getLong("finalizedOrdinal"),
                if (j.isNull("pending")) null else decodeCheckpoint(j.getJSONObject("pending")), decodedEvents,
                decodedTracks.associateBy { it.trackId }, decodedDays.associateBy { it.dateKey })
            require(s.revision >= 0 && s.finalizedOrdinal >= 0 && s.nextOrdinal > s.finalizedOrdinal)
            require(s.pending == null || s.pending.occurrence.ordinal in (s.finalizedOrdinal + 1) until s.nextOrdinal)
            require(decodedEvents.zipWithNext().all { (a, b) -> a.occurrence.ordinal < b.occurrence.ordinal })
            require(decodedEvents.all { it.occurrence.ordinal <= s.finalizedOrdinal })
            require(s.tracks.size == decodedTracks.size && s.days.size == decodedDays.size)
            return s
        }
    }
}
