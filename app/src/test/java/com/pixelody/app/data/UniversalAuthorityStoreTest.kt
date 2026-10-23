package com.pixelody.app.data

import com.pixelody.app.core.shuffle.UniversalShufflePolicy as Policy
import com.pixelody.app.core.shuffle.UniversalShuffleScopes as Scopes
import com.pixelody.app.data.storage.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UniversalAuthorityStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun genericReaderAndValidatedWriterSerializeTheSameFileAcrossStoreTypes() {
        val authority = UniversalAuthorityStore(temporary.root)
        val reader = VersionedPlaybackSessionStore(temporary.root, "universal-authority")
        assertTrue(authority.save(checkpoint(3)).saved)
        val start = CountDownLatch(1)
        val threads = Executors.newFixedThreadPool(2)
        try {
            val reads = threads.submit {
                start.await()
                repeat(200) {
                    val result = reader.load()
                    assertEquals(SessionLoadStatus.LOADED, result.status)
                    assertFalse(result.readOnly)
                    assertNotNull(result.checkpoint)
                }
            }
            val writes = threads.submit {
                start.await()
                repeat(40) { index ->
                    val result = authority.save(checkpoint(4L + index))
                    assertTrue(result.diagnostics.toString(), result.saved)
                }
            }
            start.countDown()
            reads.get(30, TimeUnit.SECONDS)
            writes.get(30, TimeUnit.SECONDS)
            assertEquals(43L, reader.load().checkpoint!!.revision)
        } finally { threads.shutdownNow() }
    }

    // An install updating from the v2.1 policy has a v2.1 checkpoint on disk. It must
    // load as an ordinary copy and accept the next save, never freeze shuffle read-only.
    @Test fun priorPolicyCheckpointLoadsAndTheNextSaveReplacesIt() {
        val fixtures = System.getProperty("pixelody.shuffle.fixtures")?.let(::File) ?: generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "docs/api-contract-fixtures/shuffle-universal-v2") }.first { it.isDirectory }
        val prior = JSONObject(File(fixtures, "v21-snapshots.json").readText(Charsets.UTF_8)).getJSONObject("snapshots").getJSONObject("flow")
        assertEquals(Policy.PRIOR_POLICY_VERSION, prior.getString("policyVersion"))
        val entries = prior.getJSONArray("entries")
        val wrapper = JSONObject().put("adapterVersion", 1).put("session", prior).put("bank", Scopes.createBank())
            .put("intent", JSONObject.NULL).put("personal", JSONObject())
        val saved = PlaybackSessionCheckpoint(prior.getString("sessionId"), prior.getLong("revision"),
            (0 until entries.length()).map { PlaybackQueueOccurrence(entries.getJSONObject(it).getString("entryId"), entries.getJSONObject(it).getString("trackId"), "content://fixture/track") },
            currentIndex = 0, positionMs = 0, shuffleMode = prior.getString("mode"), policyStateJson = wrapper.toString(),
            authorityId = prior.getString("authorityId"), authorityEpoch = prior.getLong("authorityEpoch"), scopeId = prior.getString("scopeId"))
        val store = UniversalAuthorityStore(temporary.root)
        writeRaw(store.primaryFile, saved)
        val loaded = store.load()
        assertEquals(SessionLoadStatus.LOADED, loaded.status)
        assertFalse(loaded.readOnly)
        assertEquals("restored", Policy.restore(JSONObject(loaded.checkpoint!!.policyStateJson).getJSONObject("session")).getString("status"))
        val next = store.save(checkpoint(4))
        assertTrue(next.diagnostics.toString(), next.saved)
        assertEquals(Policy.POLICY_VERSION, JSONObject(store.load().checkpoint!!.policyStateJson).getJSONObject("session").getString("policyVersion"))
    }

    private fun checkpoint(revision: Long = 3, scope: String = "playlist:α"): PlaybackSessionCheckpoint {
        val state = Policy.createSession(JSONObject().put("sessionId", "session-$scope").put("scopeId", scope)
            .put("authorityId", "local").put("authorityEpoch", 3).put("seed", "store-tests").put("mode", "balanced")
            .put("entries", JSONArray().put(JSONObject().put("entryId", "entry-1").put("trackId", "track"))
                .put(JSONObject().put("entryId", "entry-2").put("trackId", "track"))))
            .put("revision", revision)
        val bank = Scopes.save(Scopes.createBank(), state, JSONObject().put("nowMs", 25)).getJSONObject("bank")
        val wrapper = JSONObject().put("adapterVersion", 1).put("session", state).put("bank", bank)
            .put("intent", JSONObject.NULL).put("personal", JSONObject().put("entry-1", true).put("entry-2", false))
        return PlaybackSessionCheckpoint("session-$scope", revision,
            listOf(PlaybackQueueOccurrence("entry-1", "track", "content://fixture/track"),
                PlaybackQueueOccurrence("entry-2", "track", "content://fixture/track")),
            currentIndex = 1, positionMs = 4567, shuffleMode = "balanced", policyStateJson = wrapper.toString(),
            authorityId = "local", authorityEpoch = 3, scopeId = scope, playWhenReady = true)
    }

    private fun changed(checkpoint: PlaybackSessionCheckpoint, change: (JSONObject) -> Unit): PlaybackSessionCheckpoint {
        val wrapper = JSONObject(checkpoint.policyStateJson)
        change(wrapper)
        return checkpoint.copy(policyStateJson = wrapper.toString())
    }

    private fun writeRaw(file: File, checkpoint: PlaybackSessionCheckpoint) { file.writeText(PlaybackSessionCodec.encode(checkpoint), Charsets.UTF_8) }
    private fun assertEvidence(directory: File, original: String, prefix: String) = assertTrue(
        directory.listFiles()!!.any { it.name.startsWith(prefix + ".corrupt-") && it.readText(Charsets.UTF_8) == original })

    @Test fun authorityRoundTripsPausedWithBothDuplicateEntriesAndEntireScopeBank() {
        val store = UniversalAuthorityStore(temporary.root)
        val before = changed(checkpoint()) { wrapper ->
            val bank = wrapper.getJSONObject("bank")
            val other = JSONObject(checkpoint(scope = "other-scope").policyStateJson).getJSONObject("session")
            wrapper.put("bank", Scopes.save(bank, other).getJSONObject("bank"))
            wrapper.put("intent", JSONObject().put("request", JSONObject().put("type", "next")).put("positionMs", 123L))
        }.copy(pendingStageJson = """{"attemptId":"never-confirmed"}""")
        assertTrue(store.save(before).saved)
        val loaded = UniversalAuthorityStore(temporary.root).load()
        assertEquals(SessionLoadStatus.LOADED, loaded.status)
        assertFalse(loaded.readOnly)
        assertEquals(before.copy(playWhenReady = false, pendingStageJson = null), loaded.checkpoint)
        assertEquals(before.pendingStageJson, loaded.discardedPendingStageJson)
        assertEquals(2, JSONObject(loaded.checkpoint!!.policyStateJson).getJSONObject("bank").getJSONArray("scopes").length())
        assertEquals(2, loaded.checkpoint!!.queue.count { it.trackId == "track" })
    }

    @Test fun corruptInnerPrimaryRecoversValidBackupAndCannotRotateOverItOnRepair() {
        val store = UniversalAuthorityStore(temporary.root)
        assertTrue(store.save(checkpoint(3)).saved)
        assertTrue(store.save(checkpoint(4)).saved)
        writeRaw(store.primaryFile, changed(checkpoint(4)) { it.getJSONObject("session").put("remaining", JSONArray().put("missing-entry")) })
        val damaged = store.primaryFile.readText(Charsets.UTF_8)
        val goodBackup = store.backupFile.readText(Charsets.UTF_8)
        val recovered = store.load()
        assertEquals(SessionLoadStatus.RECOVERED_BACKUP, recovered.status)
        assertFalse(recovered.readOnly)
        assertEquals(3L, recovered.checkpoint!!.revision)
        assertTrue(recovered.diagnostics.any { it.code == "primary_authority_inner_corrupt_preserved" })
        assertEquals(damaged, store.primaryFile.readText(Charsets.UTF_8))
        assertTrue(UniversalAuthorityStore(temporary.root).save(checkpoint(5)).saved)
        assertEquals(5L, store.load().checkpoint!!.revision)
        assertEquals(goodBackup, store.backupFile.readText(Charsets.UTF_8))
        assertEvidence(temporary.root, damaged, store.primaryFile.name)
    }

    @Test fun corruptInnerBackupIsArchivedBeforeValidPrimaryRotatesIntoItsPath() {
        val store = UniversalAuthorityStore(temporary.root)
        assertTrue(store.save(checkpoint(3)).saved)
        writeRaw(store.backupFile, changed(checkpoint(2)) { it.getJSONObject("bank").getJSONArray("scopes")
            .getJSONObject(0).getJSONObject("session").put("rng", JSONObject()) })
        val damaged = store.backupFile.readText(Charsets.UTF_8)
        val goodPrimary = store.primaryFile.readText(Charsets.UTF_8)
        assertTrue(store.save(checkpoint(4)).saved)
        assertEquals(goodPrimary, store.backupFile.readText(Charsets.UTF_8))
        assertEvidence(temporary.root, damaged, store.backupFile.name)
    }

    @Test fun unsupportedActivePolicyVersionsInEitherCopyAreReadOnlyEvenBeforeLoad() {
        val changes: List<(JSONObject) -> Unit> = listOf(
            { it.getJSONObject("session").put("schemaVersion", Policy.VERSION + 1) },
            { it.getJSONObject("session").put("policyVersion", "universal-shuffle-v99") },
            { it.getJSONObject("session").put("configVersion", 99) },
            { it.put("adapterVersion", 2) },
            { it.getJSONObject("bank").put("schemaVersion", 3) }
        )
        for (backupFuture in listOf(false, true)) changes.forEach { change ->
            val directory = temporary.newFolder()
            val store = UniversalAuthorityStore(directory)
            writeRaw(store.primaryFile, checkpoint(4))
            writeRaw(store.backupFile, checkpoint(3))
            val file = if (backupFuture) store.backupFile else store.primaryFile
            writeRaw(file, changed(checkpoint(if (backupFuture) 3 else 4), change))
            val primaryBefore = store.primaryFile.readText(Charsets.UTF_8)
            val backupBefore = store.backupFile.readText(Charsets.UTF_8)
            val blocked = store.save(checkpoint(5))
            assertFalse(blocked.saved); assertTrue(blocked.readOnly)
            val loaded = store.load()
            assertEquals(SessionLoadStatus.FUTURE_SCHEMA, loaded.status); assertTrue(loaded.readOnly)
            assertEquals(if (backupFuture) 4L else 3L, loaded.checkpoint!!.revision)
            assertEquals(primaryBefore, store.primaryFile.readText(Charsets.UTF_8))
            assertEquals(backupBefore, store.backupFile.readText(Charsets.UTF_8))
            assertFalse(store.pendingFile.exists())
        }
    }

    @Test fun unknownVersionInArchivedScopeBlocksWritesAndIsNeverSilentlyEvicted() {
        for (field in listOf("schemaVersion", "policyVersion", "configVersion")) {
            val directory = temporary.newFolder()
            val store = UniversalAuthorityStore(directory)
            assertTrue(store.save(checkpoint(4)).saved)
            writeRaw(store.backupFile, changed(checkpoint(3)) { wrapper ->
                val state = wrapper.getJSONObject("bank").getJSONArray("scopes").getJSONObject(0).getJSONObject("session")
                state.put(field, if (field == "policyVersion") "future-policy" else 99)
            })
            val original = store.backupFile.readText(Charsets.UTF_8)
            assertEquals(SessionLoadStatus.FUTURE_SCHEMA, store.load().status)
            assertFalse(store.save(checkpoint(5)).saved)
            assertEquals(original, store.backupFile.readText(Charsets.UTF_8))
        }
    }

    @Test fun futureScopeIsDetectedEvenWhenAnotherScopeHasMissingVersionMetadata() {
        val store = UniversalAuthorityStore(temporary.root)
        assertTrue(store.save(checkpoint(3)).saved)
        writeRaw(store.backupFile, changed(checkpoint(2)) { wrapper ->
            val rows = wrapper.getJSONObject("bank").getJSONArray("scopes")
            rows.getJSONObject(0).getJSONObject("session").remove("configVersion")
            val later = JSONObject(checkpoint(scope = "future-scope").policyStateJson).getJSONObject("bank").getJSONArray("scopes").getJSONObject(0)
            later.getJSONObject("session").put("schemaVersion", 99)
            rows.put(later)
        })
        assertEquals(SessionLoadStatus.FUTURE_SCHEMA, store.load().status)
        assertTrue(store.load().readOnly)
        assertFalse(store.save(checkpoint(4)).saved)
    }

    @Test fun malformedOnlyCopiesArePreservedReadOnlyRatherThanReplacedWithDefaults() {
        val store = UniversalAuthorityStore(temporary.root)
        writeRaw(store.primaryFile, changed(checkpoint()) { it.getJSONObject("session").put("rng", JSONObject()) })
        store.backupFile.writeText("{torn", Charsets.UTF_8)
        val original = store.primaryFile.readText(Charsets.UTF_8)
        val loaded = store.load()
        assertEquals(SessionLoadStatus.CORRUPT, loaded.status); assertTrue(loaded.readOnly); assertNull(loaded.checkpoint)
        assertFalse(store.save(checkpoint(4)).saved)
        assertEquals(original, store.primaryFile.readText(Charsets.UTF_8))
        assertEquals("{torn", store.backupFile.readText(Charsets.UTF_8))
    }

    @Test fun invalidProposedInnerStateDoesNotCreatePendingOrTouchCommittedCopies() {
        val store = UniversalAuthorityStore(temporary.root)
        assertTrue(store.save(checkpoint(3)).saved)
        assertTrue(store.save(checkpoint(4)).saved)
        val primary = store.primaryFile.readText(Charsets.UTF_8)
        val backup = store.backupFile.readText(Charsets.UTF_8)
        val invalid = changed(checkpoint(5)) { it.getJSONObject("bank").getJSONArray("scopes").getJSONObject(0)
            .getJSONObject("session").put("remaining", JSONArray().put("unknown")) }
        assertFalse(store.save(invalid).saved)
        assertFalse(store.save(checkpoint(5).copy(sessionId = "misbound")).saved)
        assertEquals(primary, store.primaryFile.readText(Charsets.UTF_8))
        assertEquals(backup, store.backupFile.readText(Charsets.UTF_8))
        assertFalse(store.pendingFile.exists())
    }

    @Test fun inlineBankSizeLimitReportsFailureAndPreservesBothPreviousCopies() {
        val store = UniversalAuthorityStore(temporary.root)
        assertTrue(store.save(checkpoint(3)).saved)
        assertTrue(store.save(checkpoint(4)).saved)
        val primary = store.primaryFile.readText(Charsets.UTF_8)
        val backup = store.backupFile.readText(Charsets.UTF_8)
        val oversized = changed(checkpoint(5)) { it.getJSONObject("session").put("diagnostics", JSONArray().put("x".repeat(16 * 1024 * 1024))) }
        val result = store.save(oversized)
        assertFalse(result.saved)
        assertTrue(result.diagnostics.any { it.code == "authority_checkpoint_size_limit_previous_preserved" })
        assertEquals(primary, store.primaryFile.readText(Charsets.UTF_8))
        assertEquals(backup, store.backupFile.readText(Charsets.UTF_8))
        assertFalse(store.pendingFile.exists())
    }

    @Test fun unreadableOversizedCopyBlocksRotationWithoutReadingOrRemovingItsBytes() {
        val store = UniversalAuthorityStore(temporary.root)
        assertTrue(store.save(checkpoint(3)).saved)
        RandomAccessFile(store.backupFile, "rw").use { it.setLength(PlaybackSessionCodec.MAX_FILE_BYTES.toLong() + 1) }
        val primary = store.primaryFile.readText(Charsets.UTF_8)
        assertEquals(SessionLoadStatus.IO_ERROR, store.load().status)
        assertTrue(store.load().readOnly)
        assertTrue(store.save(checkpoint(4)).readOnly)
        assertEquals(PlaybackSessionCodec.MAX_FILE_BYTES.toLong() + 1, store.backupFile.length())
        assertEquals(primary, store.primaryFile.readText(Charsets.UTF_8))
    }

    @Test fun staleWriteIsComparedAgainstValidBackupRatherThanCorruptPrimary() {
        val store = UniversalAuthorityStore(temporary.root)
        writeRaw(store.backupFile, checkpoint(5))
        writeRaw(store.primaryFile, changed(checkpoint(99)) { it.getJSONObject("session").put("rng", JSONObject()) })
        val original = store.primaryFile.readText(Charsets.UTF_8)
        val rejected = store.save(checkpoint(4))
        assertFalse(rejected.saved)
        assertTrue(rejected.diagnostics.any { it.code == "stale_checkpoint_rejected" })
        assertEquals(original, store.primaryFile.readText(Charsets.UTF_8))
        assertTrue(store.save(checkpoint(6)).saved)
        assertEquals(5L, PlaybackSessionCodec.decode(store.backupFile.readText(Charsets.UTF_8)).checkpoint.revision)
    }

    @Test fun interruptedPendingIsNotPromotedOrMistakenForFutureCommittedAuthority() {
        val store = UniversalAuthorityStore(temporary.root)
        assertEquals(SessionLoadStatus.EMPTY, store.load().status)
        assertTrue(store.save(checkpoint(3)).saved)
        writeRaw(store.pendingFile, changed(checkpoint(99)) { it.getJSONObject("session").put("schemaVersion", 99) })
        val loaded = store.load()
        assertEquals(SessionLoadStatus.LOADED, loaded.status)
        assertFalse(loaded.readOnly); assertEquals(3L, loaded.checkpoint!!.revision)
        assertTrue(loaded.diagnostics.any { it.code == "interrupted_write_ignored" })
    }
}
