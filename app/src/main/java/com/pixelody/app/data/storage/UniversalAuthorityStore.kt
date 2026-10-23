package com.pixelody.app.data.storage

import com.pixelody.app.core.shuffle.UniversalShufflePolicy as Policy
import com.pixelody.app.core.shuffle.UniversalShuffleScopes as Scopes
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.UUID

/**
 * Private authority checkpoints using the existing envelope and atomic two-copy protocol.
 * Both copies must have understood inner versions before either may be overwritten.
 * The adapter serializes calls on its IO executor; this store also locks across instances.
 * The codec's 16 MiB policy / 32 MiB file limits still apply to the inline scope bank.
 */
class UniversalAuthorityStore(directory: File) {
    private val directory = directory.canonicalFile
    val primaryFile = File(this.directory, "universal-authority.json")
    val backupFile = File(this.directory, "universal-authority.last-valid.json")
    val pendingFile = File(this.directory, "universal-authority.pending")
    private val backupPendingFile = File(this.directory, "universal-authority.last-valid.pending")
    private val lockFile = File(this.directory, "universal-authority.lock")
    private val monitor = SessionFileLocks.monitor(lockFile)

    fun load(): SessionLoadResult = synchronized(monitor) {
        try {
            withFileLock {
                val primary = read(primaryFile)
                val backup = read(backupFile)
                val diagnostics = diagnostics(primary, backup).toMutableList()
                if (pendingFile.exists() || backupPendingFile.exists()) diagnostics += SessionDiagnostic("interrupted_write_ignored")
                val selected = primary.takeIf { it.kind == Kind.VALID } ?: backup.takeIf { it.kind == Kind.VALID }
                val status = when {
                    primary.kind == Kind.UNSUPPORTED || backup.kind == Kind.UNSUPPORTED -> SessionLoadStatus.FUTURE_SCHEMA
                    primary.kind == Kind.UNREADABLE || backup.kind == Kind.UNREADABLE -> SessionLoadStatus.IO_ERROR
                    primary.kind == Kind.VALID -> SessionLoadStatus.LOADED
                    backup.kind == Kind.VALID -> SessionLoadStatus.RECOVERED_BACKUP
                    primary.kind != Kind.ABSENT || backup.kind != Kind.ABSENT -> SessionLoadStatus.CORRUPT
                    else -> SessionLoadStatus.EMPTY
                }
                SessionLoadResult(
                    checkpoint = selected?.decoded?.checkpoint,
                    status = status,
                    readOnly = status in setOf(SessionLoadStatus.FUTURE_SCHEMA, SessionLoadStatus.IO_ERROR, SessionLoadStatus.CORRUPT),
                    diagnostics = diagnostics + selected?.decoded?.diagnostics.orEmpty(),
                    discardedPendingStageJson = selected?.decoded?.discardedPendingStageJson
                )
            }
        } catch (_: Exception) {
            SessionLoadResult(status = SessionLoadStatus.IO_ERROR, readOnly = true,
                diagnostics = listOf(SessionDiagnostic("authority_io_error_preserved")))
        }
    }

    fun save(checkpoint: PlaybackSessionCheckpoint): SessionWriteResult = synchronized(monitor) {
        var phase = "read"
        try {
            withFileLock {
                val primary = read(primaryFile)
                val backup = read(backupFile)
                val diagnostics = diagnostics(primary, backup)
                if (primary.kind == Kind.UNSUPPORTED || backup.kind == Kind.UNSUPPORTED ||
                    primary.kind == Kind.UNREADABLE || backup.kind == Kind.UNREADABLE) {
                    return@withFileLock SessionWriteResult(false, true, diagnostics)
                }
                val previous = primary.takeIf { it.kind == Kind.VALID } ?: backup.takeIf { it.kind == Kind.VALID }
                if (previous == null && (primary.kind != Kind.ABSENT || backup.kind != Kind.ABSENT)) {
                    return@withFileLock SessionWriteResult(false, true, diagnostics + SessionDiagnostic("authority_no_valid_copy_preserved"))
                }
                previous?.decoded?.checkpoint?.let { old ->
                    if (old.sessionId == checkpoint.sessionId &&
                        (checkpoint.authorityEpoch < old.authorityEpoch || checkpoint.revision < old.revision)) {
                        return@withFileLock SessionWriteResult(false, diagnostics = listOf(SessionDiagnostic("stale_checkpoint_rejected")))
                    }
                }
                // Validate all new bytes before creating a pending file or touching either committed copy.
                phase = "encode"
                val encoded = PlaybackSessionCodec.encode(checkpoint)
                phase = "validate"
                validateInner(checkpoint)
                phase = "pending-sync"
                syncWrite(pendingFile, encoded)
                if (primary.kind == Kind.CORRUPT) preserveCorrupt(primaryFile)
                if (backup.kind == Kind.CORRUPT) preserveCorrupt(backupFile)
                // An outer-valid but inner-corrupt primary must never displace the good backup.
                if (primary.kind == Kind.VALID) {
                    phase = "backup-sync"
                    syncWrite(backupPendingFile, primary.raw!!)
                    phase = "backup-replace"
                    atomicReplace(backupPendingFile, backupFile)
                }
                phase = "primary-replace"
                atomicReplace(pendingFile, primaryFile)
                syncDirectoryBestEffort()
                SessionWriteResult(true, diagnostics = diagnostics)
            }
        } catch (_: UnsupportedInner) {
            SessionWriteResult(false, true, listOf(SessionDiagnostic("unsupported_authority_checkpoint_rejected")))
        } catch (error: IllegalArgumentException) {
            SessionWriteResult(false, diagnostics = listOf(SessionDiagnostic(
                if (error.message == "Policy state too large" || error.message == "Checkpoint too large")
                    "authority_checkpoint_size_limit_previous_preserved" else "invalid_authority_checkpoint_rejected")))
        } catch (_: JSONException) {
            SessionWriteResult(false, diagnostics = listOf(SessionDiagnostic("invalid_authority_checkpoint_rejected")))
        } catch (error: Exception) {
            // Fixed operation names and exception types expose no private checkpoint values.
            SessionWriteResult(false, diagnostics = listOf(SessionDiagnostic("authority_write_failed_previous_preserved:$phase:${error.javaClass.simpleName}")))
        }
    }

    private enum class Kind { ABSENT, VALID, UNSUPPORTED, CORRUPT, UNREADABLE }
    private data class Candidate(val kind: Kind, val raw: String? = null, val decoded: DecodedPlaybackSession? = null,
        val inner: Boolean = false)
    private class UnsupportedInner : IllegalArgumentException()
    private companion object {
        const val PRIOR_SCHEMA_VERSION = 2
        const val PRIOR_CONFIG_VERSION = 1
    }

    private fun read(file: File): Candidate {
        if (!file.exists()) return Candidate(Kind.ABSENT)
        return try {
            if (file.length() > PlaybackSessionCodec.MAX_FILE_BYTES) return Candidate(Kind.UNREADABLE)
            val bytes = file.readBytes()
            if (bytes.size > PlaybackSessionCodec.MAX_FILE_BYTES) return Candidate(Kind.UNREADABLE)
            val raw = bytes.toString(Charsets.UTF_8)
            val version = runCatching { PlaybackSessionCodec.schemaVersion(raw) }.getOrNull() ?: return Candidate(Kind.CORRUPT)
            if (version > PlaybackSessionCodec.SCHEMA_VERSION) return Candidate(Kind.UNSUPPORTED)
            if (version != PlaybackSessionCodec.SCHEMA_VERSION.toLong()) return Candidate(Kind.CORRUPT)
            val decoded = try { PlaybackSessionCodec.decode(raw) } catch (error: Exception) {
                return Candidate(if (error.message == "Policy state too large" || error.message == "Checkpoint too large") Kind.UNREADABLE else Kind.CORRUPT)
            }
            try {
                validateInner(decoded.checkpoint)
                Candidate(Kind.VALID, raw, decoded)
            } catch (_: UnsupportedInner) {
                Candidate(Kind.UNSUPPORTED, inner = true)
            } catch (_: Exception) {
                Candidate(Kind.CORRUPT, inner = true)
            }
        } catch (_: Exception) { Candidate(Kind.UNREADABLE) }
    }

    private fun validateInner(checkpoint: PlaybackSessionCheckpoint) {
        val wrapper = JSONObject(checkpoint.policyStateJson)
        rejectUnsupportedVersions(wrapper)
        version(wrapper, "adapterVersion", 1)
        val keys = wrapper.keys().asSequence().toSet()
        if ((keys - setOf("adapterVersion", "session", "bank", "intent", "personal")).isNotEmpty()) throw UnsupportedInner()
        val session = wrapper.getJSONObject("session")
        val bank = wrapper.getJSONObject("bank")
        // Scan version headers of every scope before validating structure. A newer scope
        // must block writes even when a separate scope in the same copy is damaged.
        policyVersions(session)
        version(bank, "schemaVersion", Scopes.VERSION)
        val rows = bank.getJSONArray("scopes")
        for (index in 0 until rows.length()) rows.optJSONObject(index)?.optJSONObject("session")?.let(::policyVersions)
        validateSession(session)
        require(rows.length() <= Scopes.MAX_SCOPES) { "Too many scopes" }
        val scopeIds = HashSet<String>()
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val scopeId = opaque(row.get("scopeId"))
            require(scopeIds.add(scopeId) && safeNonnegative(row.opt("savedAtMs"))) { "Invalid scope row" }
            val scopeSession = row.getJSONObject("session")
            validateSession(scopeSession)
            require(scopeSession.getString("scopeId") == scopeId) { "Scope identity mismatch" }
        }
        val evictions = bank.getJSONArray("evictions")
        require(evictions.length() <= Scopes.MAX_EVICTION_DIAGNOSTICS) { "Too many eviction diagnostics" }
        for (index in 0 until evictions.length()) {
            val row = evictions.getJSONObject(index)
            opaque(row.get("scopeId"))
            require(safeNonnegative(row.opt("atMs")) && safeNonnegative(row.opt("unfinishedEntitlements")) &&
                row.opt("completed") is Boolean && row.getBoolean("completed") == (row.getLong("unfinishedEntitlements") == 0L) &&
                row.opt("reason") == "explicit-scope-eviction") { "Invalid eviction diagnostic" }
        }
        require(wrapper.has("intent")) { "Missing intent" }
        if (!wrapper.isNull("intent")) {
            val intent = wrapper.getJSONObject("intent")
            intent.getJSONObject("request")
            require(safeNonnegative(intent.opt("positionMs"))) { "Invalid intent position" }
        }
        val personal = wrapper.getJSONObject("personal")
        personal.keys().forEach { key -> opaque(key); require(personal.get(key) is Boolean) { "Invalid personal origin" } }
        require(checkpoint.sessionId == session.getString("sessionId") && checkpoint.scopeId == session.getString("scopeId") &&
            checkpoint.authorityId == session.getString("authorityId") && checkpoint.authorityEpoch == session.getLong("authorityEpoch") &&
            checkpoint.revision == session.getLong("revision") && checkpoint.shuffleMode == session.getString("mode")) { "Authority envelope identity mismatch" }
        val entries = session.getJSONArray("entries")
        require(checkpoint.queue.size == entries.length() && checkpoint.queue.indices.all { index ->
            checkpoint.queue[index].entryId == entries.getJSONObject(index).getString("entryId") &&
                checkpoint.queue[index].trackId == entries.getJSONObject(index).getString("trackId")
        }) { "Authority queue identity mismatch" }
    }

    // A checkpoint written by the previous policy (v2.1) stays readable and
    // writable: the authority upgrades it through Policy.restore on load.
    private fun prior(session: JSONObject) = session.opt("policyVersion") == Policy.PRIOR_POLICY_VERSION

    private fun policyVersions(session: JSONObject) {
        require(session.has("policyVersion") && !session.isNull("policyVersion")) { "Missing policy version" }
        val prior = prior(session)
        version(session, "schemaVersion", if (prior) PRIOR_SCHEMA_VERSION else Policy.VERSION)
        if (!prior && session.opt("policyVersion") != Policy.POLICY_VERSION) throw UnsupportedInner()
        version(session, "configVersion", if (prior) PRIOR_CONFIG_VERSION else Policy.CONFIG_VERSION)
    }

    private fun validateSession(session: JSONObject) {
        if (!prior(session)) { Policy.validateState(session); return }
        require(Policy.restore(session).optString("status") == "restored") { "Invalid prior session" }
    }

    private fun rejectUnsupportedVersions(wrapper: JSONObject) {
        fun unsupported(value: JSONObject, field: String, expected: Any): Boolean {
            if (!value.has(field) || value.isNull(field)) return false
            val actual = value.opt(field)
            return if (expected is Number) actual !is Number || actual.toDouble() != expected.toDouble() else actual != expected
        }
        fun policyUnsupported(session: JSONObject): Boolean = if (prior(session))
            unsupported(session, "schemaVersion", PRIOR_SCHEMA_VERSION) || unsupported(session, "configVersion", PRIOR_CONFIG_VERSION)
        else unsupported(session, "schemaVersion", Policy.VERSION) ||
            unsupported(session, "policyVersion", Policy.POLICY_VERSION) || unsupported(session, "configVersion", Policy.CONFIG_VERSION)
        if (unsupported(wrapper, "adapterVersion", 1)) throw UnsupportedInner()
        wrapper.optJSONObject("session")?.let { if (policyUnsupported(it)) throw UnsupportedInner() }
        wrapper.optJSONObject("bank")?.let { bank ->
            if (unsupported(bank, "schemaVersion", Scopes.VERSION)) throw UnsupportedInner()
            bank.optJSONArray("scopes")?.let { rows ->
                for (index in 0 until rows.length()) rows.optJSONObject(index)?.optJSONObject("session")?.let {
                    if (policyUnsupported(it)) throw UnsupportedInner()
                }
            }
        }
    }

    private fun version(value: JSONObject, field: String, supported: Int) {
        require(value.has(field) && !value.isNull(field)) { "Missing $field" }
        val actual = value.opt(field)
        if (actual !is Number || actual.toDouble() != supported.toDouble()) throw UnsupportedInner()
    }

    private fun safeNonnegative(value: Any?): Boolean = value is Number && value.toDouble().isFinite() &&
        value.toDouble() in 0.0..Policy.MAX_SAFE_INTEGER.toDouble() && value.toDouble() % 1.0 == 0.0

    private fun opaque(value: Any?): String {
        require(value is String && value.length in 1..1024) { "Invalid opaque identity" }
        value.forEachIndexed { index, ch -> require(!(ch.isHighSurrogate() && (index + 1 == value.length || !value[index + 1].isLowSurrogate()) ||
            ch.isLowSurrogate() && (index == 0 || !value[index - 1].isHighSurrogate()))) { "Invalid opaque identity" } }
        return value
    }

    private fun diagnostics(primary: Candidate, backup: Candidate): List<SessionDiagnostic> = buildList {
        listOf("primary" to primary, "backup" to backup).forEach { (name, candidate) ->
            if (candidate.kind == Kind.CORRUPT) add(SessionDiagnostic("${name}_${if (candidate.inner) "authority_inner" else "checkpoint"}_corrupt_preserved"))
            if (candidate.kind == Kind.UNSUPPORTED) add(SessionDiagnostic("${name}_${if (candidate.inner) "authority_inner" else "checkpoint"}_unsupported_preserved"))
            if (candidate.kind == Kind.UNREADABLE) add(SessionDiagnostic("${name}_authority_unreadable_preserved"))
        }
    }

    private fun <T> withFileLock(block: () -> T): T {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create authority checkpoint directory" }
        return RandomAccessFile(lockFile, "rw").use { lock -> lock.channel.lock().use { block() } }
    }

    private fun syncWrite(file: File, raw: String) = FileOutputStream(file).use { stream ->
        stream.write(raw.toByteArray(Charsets.UTF_8)); stream.fd.sync()
    }

    private fun atomicReplace(from: File, to: File) {
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun preserveCorrupt(file: File) {
        val target = File(directory, "${file.name}.corrupt-${UUID.randomUUID()}")
        Files.copy(file.toPath(), target.toPath())
        RandomAccessFile(target, "rw").use { it.fd.sync() }
    }

    private fun syncDirectoryBestEffort() {
        runCatching { FileChannel.open(directory.toPath(), StandardOpenOption.READ).use { it.force(true) } }
    }

}
