package com.pixelody.app.data

import com.pixelody.app.data.storage.PlaybackQueueOccurrence
import com.pixelody.app.data.storage.PlaybackSessionCheckpoint
import com.pixelody.app.data.storage.PlaybackSessionCodec
import com.pixelody.app.data.storage.SessionLoadStatus
import com.pixelody.app.data.storage.VersionedPlaybackSessionStore
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PlaybackSessionStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun store() = VersionedPlaybackSessionStore(temporary.root)
    private fun checkpoint(revision: Long = 3) = PlaybackSessionCheckpoint(
        sessionId = "session|α", revision = revision, scopeId = "playlist:music|一",
        queue = listOf(
            PlaybackQueueOccurrence("entry|A", "recording/一", "content://local/1", "Song", "Artist", "Album",
                albumArtist = "Various Artists", discNumber = 2, trackNumber = 4, durationMs = 90000,
                durationSeconds = 90, mimeType = "audio/flac", format = "FLAC", lossless = true,
                artworkUri = "file:///private/artwork.jpg"),
            PlaybackQueueOccurrence("entry|B", "recording/二", "https://host.invalid/media/two"),
            PlaybackQueueOccurrence("entry|A-again", "recording/一", "content://local/1")
        ), currentIndex = 2, positionMs = 4567, repeatMode = 2, shuffleMode = "balanced",
        policyStateJson = """{"policyVersion":200,"rng":{"state":4294967295,"draws":23},"entitlement":["entry|B"],"history":[{"entryId":"entry|A","occurrenceId":"played-A"},{"entryId":"entry|B","occurrenceId":"played-B"}],"manual":["entry|A-again"],"futureUnknown":{"null":null,"zero":0}}""",
        currentOccurrenceId = "played-A-again", authorityId = "local-device", authorityEpoch = 5, playWhenReady = true
    )

    @Test fun fullCheckpointRoundTripsPausedIncludingIntentionalDuplicatesAndUnknownPolicy() {
        val before = checkpoint()
        assertTrue(store().save(before).saved)
        val loaded = store().load()
        assertEquals(SessionLoadStatus.LOADED, loaded.status)
        assertEquals(before.copy(playWhenReady = false), loaded.checkpoint)
        assertEquals(3, loaded.checkpoint!!.queue.size)
        assertEquals(2, loaded.checkpoint!!.queue.count { it.trackId == "recording/一" })
        assertFalse(loaded.checkpoint!!.playWhenReady)
        assertEquals(before.policyStateJson, loaded.checkpoint!!.policyStateJson)
    }

    @Test fun pendingActivationIsDiscardedWithoutConsumingPolicyRngHistoryOrEntitlement() {
        val before = checkpoint().copy(pendingStageJson = """{"attemptId":"unfinished","decisionId":"reserved","nextRng":123}""")
        val decoded = PlaybackSessionCodec.decode(PlaybackSessionCodec.encode(before))
        assertNull(decoded.checkpoint.pendingStageJson)
        assertEquals(before.pendingStageJson, decoded.discardedPendingStageJson)
        assertEquals(before.policyStateJson, decoded.checkpoint.policyStateJson)
        assertEquals(before.currentOccurrenceId, decoded.checkpoint.currentOccurrenceId)
        assertTrue(decoded.diagnostics.any { it.code == "pending_activation_discarded" })
    }

    @Test fun corruptPrimaryRestoresLastValidCopyAndPreservesDamagedBytesOnRepair() {
        val store = store()
        assertTrue(store.save(checkpoint(3)).saved)
        assertTrue(store.save(checkpoint(4)).saved)
        store.primaryFile.writeText("{torn")
        val result = store.load()
        assertEquals(SessionLoadStatus.RECOVERED_BACKUP, result.status)
        assertEquals(3L, result.checkpoint!!.revision)
        assertTrue(store.save(checkpoint(5)).saved)
        assertEquals(5L, store.load().checkpoint!!.revision)
        assertTrue(temporary.root.listFiles()!!.any { it.name.contains(".corrupt-") && it.readText() == "{torn" })
        assertEquals(3L, PlaybackSessionCodec.decode(store.backupFile.readText()).checkpoint.revision)
    }

    @Test fun interruptedPendingIsNeverPromotedEvenIfItContainsACompleteNewerCheckpoint() {
        val store = store()
        assertTrue(store.save(checkpoint(3)).saved)
        store.pendingFile.writeText(PlaybackSessionCodec.encode(checkpoint(4).copy(pendingStageJson = "{\"attemptId\":\"later\"}")))
        val loaded = store.load()
        assertEquals(3L, loaded.checkpoint!!.revision)
        assertTrue(loaded.diagnostics.any { it.code == "interrupted_write_ignored" })
        assertTrue(store.save(checkpoint(5)).saved)
        assertEquals(5L, store.load().checkpoint!!.revision)
    }

    @Test fun corruptBackupBytesArePreservedBeforeValidPrimaryRotatesIntoItsPath() {
        val store = store()
        assertTrue(store.save(checkpoint(3)).saved)
        store.backupFile.writeText("{bad backup bytes")
        assertTrue(store.save(checkpoint(4)).saved)
        assertEquals(3L, PlaybackSessionCodec.decode(store.backupFile.readText()).checkpoint.revision)
        assertTrue(temporary.root.listFiles()!!.any {
            it.name.startsWith("playback-session.last-valid.json.corrupt-") && it.readText() == "{bad backup bytes"
        })
    }

    @Test fun interruptedBackupRotationKeepsCommittedPrimary() {
        val store = store()
        assertTrue(store.save(checkpoint(3)).saved)
        File(temporary.root, "playback-session.last-valid.pending").writeText("{torn")
        assertEquals(3L, store.load().checkpoint!!.revision)
        assertTrue(store.load().diagnostics.any { it.code == "interrupted_write_ignored" })
    }

    @Test fun missingPrimaryAfterInterruptionUsesOnlyCommittedBackup() {
        val store = store()
        store.backupFile.writeText(PlaybackSessionCodec.encode(checkpoint(3)))
        store.pendingFile.writeText(PlaybackSessionCodec.encode(checkpoint(4)))
        assertEquals(SessionLoadStatus.RECOVERED_BACKUP, store.load().status)
        assertEquals(3L, store.load().checkpoint!!.revision)
    }

    @Test fun pendingOnlyNeverInventsConfirmedPlayback() {
        val store = store()
        store.pendingFile.writeText(PlaybackSessionCodec.encode(checkpoint(4)))
        assertEquals(SessionLoadStatus.EMPTY, store.load().status)
        assertNull(store.load().checkpoint)
    }

    @Test fun futureSchemaPreservedReadOnlyAndBlocksLegacyAndSave() {
        val store = store()
        val future = """{"schemaVersion":99,"pending":{"newer":"untouched"},"unknown":[1,2,3]}"""
        store.primaryFile.writeText(future)
        val loaded = store.load(legacy())
        assertEquals(SessionLoadStatus.FUTURE_SCHEMA, loaded.status)
        assertTrue(loaded.readOnly)
        assertNull(loaded.checkpoint)
        val saved = store.save(checkpoint())
        assertFalse(saved.saved)
        assertTrue(saved.readOnly)
        assertEquals(future, store.primaryFile.readText())
        assertFalse(store.backupFile.exists())
    }

    @Test fun newerBackupCannotBeOverwrittenByCurrentPrimaryRotation() {
        val store = store()
        assertTrue(store.save(checkpoint()).saved)
        val future = """{"schemaVersion":2,"payloadJson":"new representation"}"""
        store.backupFile.writeText(future)
        val loaded = store.load()
        assertTrue(loaded.readOnly)
        assertEquals(SessionLoadStatus.FUTURE_SCHEMA, loaded.status)
        assertEquals(3L, loaded.checkpoint!!.revision)
        assertFalse(store.save(checkpoint(4)).saved)
        assertEquals(future, store.backupFile.readText())
        assertEquals(3L, PlaybackSessionCodec.decode(store.primaryFile.readText()).checkpoint.revision)
    }

    @Test fun hugeFutureSchemaRemainsReadOnlyInsteadOfBeingMisclassifiedAsCorrupt() {
        val store = store()
        val future = """{"schemaVersion":922337203685477580700,"unknown":"preserve"}"""
        store.primaryFile.writeText(future)
        assertEquals(SessionLoadStatus.FUTURE_SCHEMA, store.load().status)
        assertFalse(store.save(checkpoint()).saved)
        assertEquals(future, store.primaryFile.readText())
    }

    @Test fun checksumTamperingIsCorruptRatherThanSilentDefaultsOrLegacyImport() {
        val store = store()
        val envelope = JSONObject(PlaybackSessionCodec.encode(checkpoint()))
        envelope.put("payloadJson", envelope.getString("payloadJson").replace("4567", "9876"))
        store.primaryFile.writeText(envelope.toString())
        assertEquals(SessionLoadStatus.CORRUPT, store.load(legacy()).status)
        assertNull(store.load().checkpoint)
        assertEquals(envelope.toString(), store.primaryFile.readText())
    }

    @Test fun legacyImportIsStablePausedHonestAndNonDestructive() {
        val store = store()
        val raw = legacy()
        val loaded = store.load(raw)
        assertEquals(SessionLoadStatus.IMPORTED_LEGACY, loaded.status)
        assertEquals(loaded.checkpoint, store.load(raw).checkpoint)
        assertEquals("legacy-single-item-unknown-history", loaded.checkpoint!!.origin)
        assertFalse(loaded.checkpoint!!.playWhenReady)
        assertEquals(1234L, loaded.checkpoint!!.positionMs)
        assertEquals("{}", loaded.checkpoint!!.policyStateJson)
        assertEquals(1, loaded.checkpoint!!.queue.size)
        assertFalse(store.primaryFile.exists())
        assertTrue(store.save(loaded.checkpoint!!).saved)
        assertEquals(SessionLoadStatus.LOADED, store.load(raw).status)
    }

    @Test fun corruptLegacyIsReportedWithoutWritingEmptyDefaults() {
        val store = store()
        val loaded = store.load("{invalid")
        assertEquals(SessionLoadStatus.EMPTY, loaded.status)
        assertTrue(loaded.diagnostics.any { it.code == "legacy_invalid_preserved" })
        assertFalse(store.primaryFile.exists())
    }

    @Test fun invalidUriRetainsOccurrenceMetadataAndCurrentIndexAsTombstone() {
        val before = checkpoint().copy(queue = checkpoint().queue.mapIndexed { index, item ->
            if (index == 2) item.copy(mediaUri = "not a URI", unavailableReason = "lost-source") else item
        })
        val decoded = PlaybackSessionCodec.decode(PlaybackSessionCodec.encode(before))
        assertEquals(2, decoded.checkpoint.currentIndex)
        assertEquals(3, decoded.checkpoint.queue.size)
        assertNull(decoded.checkpoint.queue[2].mediaUri)
        assertEquals("entry|A-again", decoded.checkpoint.queue[2].entryId)
        assertEquals("lost-source", decoded.checkpoint.queue[2].unavailableReason)
        assertEquals(before.policyStateJson, decoded.checkpoint.policyStateJson)
    }

    @Test fun credentialUrisAndArtworkAreExcludedFromStoredBytesWithoutDroppingEntries() {
        val badUris = listOf("https://user:secret@host.invalid/a", "https://host.invalid/a?access_token=secret",
            "https://host.invalid/a?%74oken=secret", "https://host.invalid/a?X-Amz-Signature=secret",
            "https://host.invalid/a#secret", "javascript:secret", "https://host.invalid/a?password=secret")
        badUris.forEach { bad ->
            val before = checkpoint().copy(queue = listOf(PlaybackQueueOccurrence("entry", "track", bad,
                artworkUri = bad)), currentIndex = 0)
            val encoded = PlaybackSessionCodec.encode(before)
            assertFalse(encoded.contains("secret"))
            val after = PlaybackSessionCodec.decode(encoded).checkpoint
            assertNull(after.queue[0].mediaUri)
            assertNull(after.queue[0].artworkUri)
            assertEquals("track", after.queue[0].trackId)
        }
    }

    @Test fun privateJsonCredentialsRejectWriteWhileUnknownFuturePolicyFieldsRemainOpaque() {
        val store = store()
        assertTrue(store.save(checkpoint()).saved)
        val before = store.primaryFile.readText()
        val bad = checkpoint(4).copy(policyStateJson = """{"unknownFuture":{"Authorization":"Bearer secret"}}""")
        assertFalse(store.save(bad).saved)
        assertEquals(before, store.primaryFile.readText())
        assertFalse(store.pendingFile.exists())
        val good = checkpoint(4).copy(policyStateJson = """{"policyVersion":999,"policy":{"unknownFuture":[null,0,false,"α"]}}""")
        assertTrue(store.save(good).saved)
        assertEquals(good.policyStateJson, store.load().checkpoint!!.policyStateJson)
        assertFalse(store.save(checkpoint(5).copy(policyStateJson = """{"mediaUri":"https://host.invalid/a?token=secret"}""")).saved)
        assertFalse(store.save(checkpoint(5).copy(policyStateJson = """{"customHeader":"Bearer secret"}""")).saved)
    }

    @Test fun duplicateEntryIdentityRejectedButRepeatedTrackIdentityPreserved() {
        val store = store()
        assertTrue(store.save(checkpoint()).saved)
        val before = store.primaryFile.readText()
        assertFalse(store.save(checkpoint(4).copy(queue = listOf(checkpoint().queue[0], checkpoint().queue[0]), currentIndex = 0)).saved)
        assertEquals(before, store.primaryFile.readText())
    }

    @Test fun staleCheckpointOrAuthorityEpochCannotOverwriteLaterCommittedState() {
        val store = store()
        assertTrue(store.save(checkpoint(4)).saved)
        assertFalse(store.save(checkpoint(3)).saved)
        assertFalse(store.save(checkpoint(5).copy(authorityEpoch = 4)).saved)
        assertEquals(4L, store.load().checkpoint!!.revision)
        assertTrue(store.save(checkpoint(5)).saved)
    }

    @Test fun invalidIndexAndModesRejectRatherThanSilentlyPointAtAnotherOccurrence() {
        val store = store()
        assertFalse(store.save(checkpoint().copy(currentIndex = 7)).saved)
        assertFalse(store.save(checkpoint().copy(repeatMode = 3)).saved)
        assertFalse(store.save(checkpoint().copy(shuffleMode = "Standard")).saved)
        assertFalse(store.save(checkpoint().copy(positionMs = -1)).saved)
        assertFalse(store.primaryFile.exists())
    }

    @Test fun nullAndZeroMetadataRemainDistinctAndEmptyQueueCheckpointIsValid() {
        val before = checkpoint().copy(queue = listOf(
            PlaybackQueueOccurrence("zero", "track", "file:///zero", durationMs = 0, durationSeconds = 0, discNumber = 0, lossless = false),
            PlaybackQueueOccurrence("unknown", "track", null)
        ), currentIndex = 1)
        val after = PlaybackSessionCodec.decode(PlaybackSessionCodec.encode(before)).checkpoint
        assertEquals(0L, after.queue[0].durationMs)
        assertNull(after.queue[1].durationMs)
        assertEquals(false, after.queue[0].lossless)
        assertNull(after.queue[1].lossless)
        assertTrue(store().save(before.copy(queue = emptyList(), currentIndex = -1, positionMs = 0)).saved)
        assertEquals(emptyList<PlaybackQueueOccurrence>(), store().load().checkpoint!!.queue)
    }

    @Test fun fileNameCannotEscapeDedicatedStoreDirectory() {
        try {
            VersionedPlaybackSessionStore(temporary.root, "../pixelody_audiophile.db")
            fail("Path traversal accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun fullThousandItemPoolPersistsWithoutPreviewTruncation() {
        val before = checkpoint().copy(queue = (0 until 1200).map {
            PlaybackQueueOccurrence("entry-$it", "track-${it % 1000}", "content://fixture/$it")
        }, currentIndex = 1155)
        val store = store()
        assertTrue(store.save(before).saved)
        assertEquals(before.copy(playWhenReady = false), store.load().checkpoint)
    }

    @Test fun separateStoreInstancesSerializeConcurrentWritesAndRejectLateOlderCheckpoints() {
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1L..24L).map { revision -> executor.submit<Boolean> {
                VersionedPlaybackSessionStore(temporary.root).save(checkpoint(revision)).saved
            } }
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
            val loaded = store().load()
            assertEquals(SessionLoadStatus.LOADED, loaded.status)
            assertEquals(24L, loaded.checkpoint!!.revision)
            assertFalse(store().save(checkpoint(1)).saved)
        } finally { executor.shutdownNow() }
    }

    private fun legacy() = """{"mediaId":"old","uri":"content://local/old","title":"Old song","artist":"Artist","album":"Album","positionMs":1234,"playWhenReady":true}"""
}
