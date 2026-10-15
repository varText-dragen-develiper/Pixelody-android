package com.pixelody.app.data.storage

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class SessionLoadStatus { EMPTY, LOADED, RECOVERED_BACKUP, IMPORTED_LEGACY, FUTURE_SCHEMA, CORRUPT, IO_ERROR }

data class SessionLoadResult(
    val checkpoint: PlaybackSessionCheckpoint? = null,
    val status: SessionLoadStatus,
    val readOnly: Boolean = false,
    val diagnostics: List<SessionDiagnostic> = emptyList(),
    val discardedPendingStageJson: String? = null
)

data class SessionWriteResult(val saved: Boolean, val readOnly: Boolean = false, val diagnostics: List<SessionDiagnostic> = emptyList())

/** JVM file locks throw rather than wait when another channel in this process owns the lock. */
internal object SessionFileLocks {
    private val monitors = ConcurrentHashMap<String, Any>()
    fun monitor(file: File): Any = monitors.getOrPut(file.canonicalPath) { Any() }
}

/**
 * A separate store: never opens PixelodyDatabaseHelper and never changes its version.
 * All operations are blocking; the service should serialize them on its IO dispatcher.
 * A synced pending file is atomically renamed only after the last valid committed file
 * has been backed up. Uncommitted .pending files are never promoted on load.
 */
class VersionedPlaybackSessionStore(directory: File, name: String = "playback-session") {
    init { require(name.matches(Regex("[a-zA-Z0-9_-]+"))) { "Invalid session file name" } }
    private val directory = directory.canonicalFile
    val primaryFile = File(this.directory, "$name.json")
    val backupFile = File(this.directory, "$name.last-valid.json")
    val pendingFile = File(this.directory, "$name.pending")
    private val backupPendingFile = File(this.directory, "$name.last-valid.pending")
    private val lockFile = File(this.directory, "$name.lock")
    private val monitor = SessionFileLocks.monitor(lockFile)

    /** Legacy import is returned only if no committed checkpoint exists. It does not write/erase preferences. */
    fun load(legacyRawJson: String? = null): SessionLoadResult = synchronized(monitor) {
        try {
            withFileLock {
                val primary = read(primaryFile)
                val backup = read(backupFile)
                val diagnostics = diagnostics(primary, backup).toMutableList()
                if (pendingFile.exists() || backupPendingFile.exists()) diagnostics += SessionDiagnostic("interrupted_write_ignored")
                val selected = (primary as? Candidate.Valid) ?: (backup as? Candidate.Valid)
                if (primary is Candidate.Future || backup is Candidate.Future) {
                    // A newer backup also blocks writes; never rotate/delete a newer representation.
                    return@withFileLock result(selected, SessionLoadStatus.FUTURE_SCHEMA, true, diagnostics)
                }
                if (primary is Candidate.Unreadable || backup is Candidate.Unreadable) {
                    return@withFileLock result(selected, SessionLoadStatus.IO_ERROR, true, diagnostics)
                }
                if (selected != null) return@withFileLock result(selected,
                    if (primary is Candidate.Valid) SessionLoadStatus.LOADED else SessionLoadStatus.RECOVERED_BACKUP,
                    false, diagnostics)
                if (primary !is Candidate.Absent || backup !is Candidate.Absent) {
                    return@withFileLock SessionLoadResult(status = SessionLoadStatus.CORRUPT, diagnostics = diagnostics)
                }
                if (legacyRawJson != null) {
                    val imported = runCatching { PlaybackSessionCodec.importLegacy(legacyRawJson) }.getOrNull()
                    if (imported != null) return@withFileLock SessionLoadResult(imported.checkpoint,
                        SessionLoadStatus.IMPORTED_LEGACY, diagnostics = diagnostics + imported.diagnostics)
                    diagnostics += SessionDiagnostic("legacy_invalid_preserved")
                }
                SessionLoadResult(status = SessionLoadStatus.EMPTY, diagnostics = diagnostics)
            }
        } catch (_: Exception) {
            SessionLoadResult(status = SessionLoadStatus.IO_ERROR, readOnly = true,
                diagnostics = listOf(SessionDiagnostic("session_io_error")))
        }
    }

    fun save(checkpoint: PlaybackSessionCheckpoint): SessionWriteResult = synchronized(monitor) {
        try {
            withFileLock {
                val primary = read(primaryFile)
                val backup = read(backupFile)
                if (primary is Candidate.Future || backup is Candidate.Future) {
                    return@withFileLock SessionWriteResult(false, true, listOf(SessionDiagnostic("future_schema_preserved")))
                }
                if (primary is Candidate.Unreadable || backup is Candidate.Unreadable) {
                    return@withFileLock SessionWriteResult(false, true, listOf(SessionDiagnostic("unreadable_state_preserved")))
                }
                val previous = (primary as? Candidate.Valid) ?: (backup as? Candidate.Valid)
                previous?.decoded?.checkpoint?.let { old ->
                    if (old.sessionId == checkpoint.sessionId &&
                        (checkpoint.authorityEpoch < old.authorityEpoch || checkpoint.revision < old.revision)) {
                        return@withFileLock SessionWriteResult(false, diagnostics = listOf(SessionDiagnostic("stale_checkpoint_rejected")))
                    }
                }
                val encoded = PlaybackSessionCodec.encode(checkpoint)
                syncWrite(pendingFile, encoded)
                // Preserve damaged bytes before backup rotation can replace their path.
                if (primary is Candidate.Invalid) preserveCorrupt(primaryFile)
                if (backup is Candidate.Invalid) preserveCorrupt(backupFile)
                if (primary is Candidate.Valid) {
                    syncWrite(backupPendingFile, primary.raw)
                    atomicReplace(backupPendingFile, backupFile)
                }
                atomicReplace(pendingFile, primaryFile)
                syncDirectoryBestEffort()
                SessionWriteResult(true)
            }
        } catch (_: IllegalArgumentException) {
            SessionWriteResult(false, diagnostics = listOf(SessionDiagnostic("invalid_checkpoint_rejected")))
        } catch (_: Exception) {
            SessionWriteResult(false, diagnostics = listOf(SessionDiagnostic("session_write_failed_previous_preserved")))
        }
    }

    private fun result(candidate: Candidate.Valid?, status: SessionLoadStatus, readOnly: Boolean,
        diagnostics: List<SessionDiagnostic>): SessionLoadResult = SessionLoadResult(
        checkpoint = candidate?.decoded?.checkpoint, status = status, readOnly = readOnly,
        diagnostics = diagnostics + candidate?.decoded?.diagnostics.orEmpty(),
        discardedPendingStageJson = candidate?.decoded?.discardedPendingStageJson
    )

    private sealed class Candidate {
        object Absent : Candidate()
        data class Valid(val raw: String, val decoded: DecodedPlaybackSession) : Candidate()
        object Future : Candidate()
        object Invalid : Candidate()
        object Unreadable : Candidate()
    }

    private fun read(file: File): Candidate {
        if (!file.exists()) return Candidate.Absent
        return try {
            // Too large is read-only: do not assume it is corrupt and overwrite a newer file.
            if (file.length() > PlaybackSessionCodec.MAX_FILE_BYTES) return Candidate.Unreadable
            val bytes = file.readBytes()
            if (bytes.size > PlaybackSessionCodec.MAX_FILE_BYTES) return Candidate.Unreadable
            val raw = String(bytes, StandardCharsets.UTF_8)
            val version = runCatching { PlaybackSessionCodec.schemaVersion(raw) }.getOrNull()
                ?: return Candidate.Invalid
            when {
                version > PlaybackSessionCodec.SCHEMA_VERSION -> Candidate.Future
                version != PlaybackSessionCodec.SCHEMA_VERSION.toLong() -> Candidate.Invalid
                else -> runCatching<Candidate> { Candidate.Valid(raw, PlaybackSessionCodec.decode(raw)) }.getOrDefault(Candidate.Invalid)
            }
        } catch (_: Exception) { Candidate.Unreadable }
    }

    private fun diagnostics(primary: Candidate, backup: Candidate): List<SessionDiagnostic> = buildList {
        if (primary is Candidate.Invalid) add(SessionDiagnostic("primary_corrupt_preserved"))
        if (backup is Candidate.Invalid) add(SessionDiagnostic("backup_corrupt_preserved"))
        if (primary is Candidate.Future || backup is Candidate.Future) add(SessionDiagnostic("future_schema_preserved"))
        if (primary is Candidate.Unreadable || backup is Candidate.Unreadable) add(SessionDiagnostic("unreadable_state_preserved"))
    }

    private fun <T> withFileLock(block: () -> T): T {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create checkpoint directory" }
        return RandomAccessFile(lockFile, "rw").use { lock -> lock.channel.lock().use { block() } }
    }

    private fun syncWrite(file: File, raw: String) = FileOutputStream(file).use { stream ->
        stream.write(raw.toByteArray(StandardCharsets.UTF_8))
        stream.fd.sync()
    }

    private fun atomicReplace(from: File, to: File) {
        // Do not fall back to delete+rename: failure leaves a valid committed copy in place.
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun preserveCorrupt(file: File) {
        if (!file.exists()) return
        val target = File(directory, "${file.name}.corrupt-${UUID.randomUUID()}")
        Files.copy(file.toPath(), target.toPath())
        RandomAccessFile(target, "rw").use { it.fd.sync() }
    }

    private fun syncDirectoryBestEffort() {
        // Some Android/filesystem providers do not support opening directories as channels.
        runCatching { FileChannel.open(directory.toPath(), StandardOpenOption.READ).use { it.force(true) } }
    }

}
