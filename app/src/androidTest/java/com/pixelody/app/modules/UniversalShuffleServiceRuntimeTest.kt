package com.pixelody.app.modules

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.pixelody.app.core.analytics.ListeningEndReason
import com.pixelody.app.core.analytics.ListeningLedgerSnapshot
import com.pixelody.app.core.analytics.ListeningLedgerStore
import com.pixelody.app.core.playback.PixelodyPlaybackService
import com.pixelody.app.core.playback.PlaybackEvidenceMetadata
import com.pixelody.app.data.storage.PlaybackSessionCheckpoint
import com.pixelody.app.data.storage.VersionedPlaybackSessionStore
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Real decoder/service/controller tests without launching an Activity. The two named
 * lifecycle phases MUST be separate adb instrumentation invocations with force-stop
 * of ONLY com.pixelody.app.shuffleqa between them. The cold phase checks a different PID.
 * Generated silent PCM verifies media progression/measurement, not audible acceptance.
 */
@OptIn(UnstableApi::class)
class UniversalShuffleServiceRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val evidenceDirectory get() = File(context.filesDir, "universal-shuffle-runtime")
    private val seedManifest get() = File(evidenceDirectory, "process-death-seed.json")
    private val sessionStore get() = VersionedPlaybackSessionStore(File(context.filesDir, "universal-shuffle"))
    private fun ledger() = ListeningLedgerStore(File(context.filesDir, "universal-shuffle/listening")).reload()

    @Test fun seedFullTimelineAndMeasureWithoutAScreen() {
        isolatedPackageOnly()
        val runId = "service-runtime-${UUID.randomUUID()}"
        val sessionId = "$runId-session"
        val scopeId = "$runId-scope"
        val entries = listOf("A1", "B", "A2", "C", "D").map { "$runId-$it" }
        val ids = listOf("A", "B", "A", "C", "D").map { "$runId-track-$it" }
        val files = listOf("A", "B", "C", "D").associateWith { silentWav("$runId-$it") }
        val items = ids.indices.map { index ->
            fixtureItem(ids[index], entries[index], sessionId, scopeId,
                files.getValue(listOf("A", "B", "A", "C", "D")[index]))
        }
        val controller = connect()
        try {
            onMain {
                controller.pause()
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.setMediaItems(items, 0, 0)
                controller.prepare()
                controller.play()
            }
            awaitCondition("Real background service playback progressed for about two seconds") {
                onMain { controller.isPlaying && controller.currentMediaItemIndex == 0 && controller.currentPosition >= 2100 }
            }
            onMain { controller.pause() }
            awaitCondition("Pause reached the real player") { onMain { !controller.isPlaying && !controller.playWhenReady } }
            // Move D into the priority position while retaining both deliberate A entries.
            onMain {
                controller.moveMediaItem(4, 1)
                controller.seekTo(3, 1234)
                controller.repeatMode = Player.REPEAT_MODE_ALL
                controller.shuffleModeEnabled = false
            }
            val expectedEntries = listOf(entries[0], entries[4], entries[1], entries[2], entries[3])
            val expectedIds = listOf(ids[0], ids[4], ids[1], ids[2], ids[3])
            awaitCondition("Edited authoritative timeline and exact duplicate occurrence selected") {
                onMain { entryIds(controller) == expectedEntries && controller.currentMediaItemIndex == 3 }
            }
            var saved: PlaybackSessionCheckpoint? = null
            awaitCondition("Full edited queue/index/position/repeat checkpoint reached disk") {
                saved = sessionStore.load().checkpoint
                saved?.let { it.sessionId == sessionId && it.scopeId == scopeId &&
                    it.queue.map { entry -> entry.entryId } == expectedEntries &&
                    it.queue.map { entry -> entry.trackId } == expectedIds &&
                    it.currentIndex == 3 && kotlin.math.abs(it.positionMs - 1234) <= 100 &&
                    it.repeatMode == Player.REPEAT_MODE_ALL } == true
            }
            var snapshot: ListeningLedgerSnapshot? = null
            awaitCondition("Service-owned real heard interval finalized in durable ledger") {
                snapshot = ledger()
                snapshot!!.recentEvents.any { it.occurrence.sessionId == sessionId && it.occurrence.entryId == entries[0] &&
                    it.checkpoint.listenedWallMs >= 1000 }
            }
            val events = snapshot!!.recentEvents.filter { it.occurrence.sessionId == sessionId }
            assertTrue(events.isNotEmpty())
            assertTrue("Twenty-second fixture was only briefly heard", events.sumOf { it.checkpoint.listenedWallMs } in 1000..8000)
            assertTrue("No fabricated natural completion", events.none { it.naturalCompletion || it.reason == ListeningEndReason.NATURAL_END })
            assertTrue("Only first occurrence actually played", events.all { it.occurrence.entryId == entries[0] })
            assertEquals(0L, snapshot!!.tracks[ids[0]]!!.completedCount)
            assertTrue(events.all { it.occurrence.track.durationMs == 20_000L && it.occurrence.track.format == "WAV" && it.occurrence.track.isLossless })
            assertNull("Paused untouched A2 must not be a listening activation", snapshot!!.pending?.takeIf { it.occurrence.sessionId == sessionId })
            onMain {
                assertFalse(controller.playWhenReady)
                assertFalse(controller.shuffleModeEnabled)
                assertEquals(Player.REPEAT_MODE_ALL, controller.repeatMode)
            }
            val manifest = JSONObject().put("sessionId", sessionId).put("scopeId", scopeId)
                .put("seedPid", Process.myPid()).put("seedAtElapsedMs", SystemClock.elapsedRealtime())
                .put("entryIds", JSONArray(expectedEntries)).put("trackIds", JSONArray(expectedIds))
                .put("currentIndex", 3).put("positionMs", 1234).put("repeatMode", Player.REPEAT_MODE_ALL)
                .put("eventIds", JSONArray(events.map { it.occurrence.eventId }))
                .put("listenedWallMs", events.sumOf { it.checkpoint.listenedWallMs })
                .put("uniqueCoverageMs", events.sumOf { it.checkpoint.uniqueCoverageMs })
                .put("checkpointRevision", saved!!.revision)
            assertTrue(evidenceDirectory.isDirectory || evidenceDirectory.mkdirs())
            seedManifest.outputStream().use { stream -> stream.write(manifest.toString(2).toByteArray(Charsets.UTF_8)); stream.fd.sync() }
        } finally {
            // Leave the authoritative queue intact for the force-stop/cold invocation.
            onMain { controller.pause(); controller.release() }
        }
    }

    @Test fun coldRestoresFullTimelinePaused() {
        isolatedPackageOnly()
        assertTrue("Run the seed phase first in a separate invocation", seedManifest.isFile)
        val manifest = JSONObject(seedManifest.readText())
        assertNotEquals("Force-stop only the isolated target between phase invocations", manifest.getInt("seedPid"), Process.myPid())
        val sessionId = manifest.getString("sessionId")
        val expectedEntries = manifest.getJSONArray("entryIds").strings()
        val expectedIds = manifest.getJSONArray("trackIds").strings()
        val controller = connect()
        try {
            awaitCondition("Cold service restored every occurrence and selected the saved duplicate") {
                onMain { entryIds(controller) == expectedEntries && controller.currentMediaItemIndex == 3 }
            }
            onMain {
                assertEquals(expectedIds, (0 until controller.mediaItemCount).map { controller.getMediaItemAt(it).mediaId })
                assertEquals(expectedEntries[3], PlaybackEvidenceMetadata.entryId(controller.currentMediaItem))
                assertEquals(1234L.toDouble(), controller.currentPosition.toDouble(), 250.0)
                assertFalse("Cold bind must remain paused", controller.playWhenReady)
                assertFalse(controller.isPlaying)
                assertFalse("Native shuffle must not undo planned traversal", controller.shuffleModeEnabled)
                assertEquals(Player.REPEAT_MODE_ALL, controller.repeatMode)
            }
            val before = ledger()
            val beforeEvents = before.recentEvents.filter { it.occurrence.sessionId == sessionId }
            assertEquals(manifest.getJSONArray("eventIds").strings(), beforeEvents.map { it.occurrence.eventId })
            assertEquals(manifest.getLong("listenedWallMs"), beforeEvents.sumOf { it.checkpoint.listenedWallMs })
            assertEquals(manifest.getLong("uniqueCoverageMs"), beforeEvents.sumOf { it.checkpoint.uniqueCoverageMs })
            assertNull(before.pending?.takeIf { it.occurrence.sessionId == sessionId })
            onMain { controller.seekToPreviousMediaItem() }
            awaitCondition("Previous follows the edited timeline to B") {
                onMain { controller.currentMediaItemIndex == 2 && PlaybackEvidenceMetadata.entryId(controller.currentMediaItem) == expectedEntries[2] }
            }
            onMain { controller.seekToNextMediaItem() }
            awaitCondition("Next retains forward traversal to deliberate A2 occurrence") {
                onMain { controller.currentMediaItemIndex == 3 && PlaybackEvidenceMetadata.entryId(controller.currentMediaItem) == expectedEntries[3] }
            }
            Thread.sleep(2200) // Two real service ticks must still measure zero while paused.
            val after = ledger()
            assertEquals("Paused restoration/navigation must not create listening", beforeEvents,
                after.recentEvents.filter { it.occurrence.sessionId == sessionId })
            assertNull(after.pending?.takeIf { it.occurrence.sessionId == sessionId })
            assertEquals("No track totals may grow while paused", before.tracks, after.tracks)
            onMain { assertFalse(controller.playWhenReady); assertFalse(controller.isPlaying) }
            val receipt = JSONObject().put("sessionId", sessionId).put("seedPid", manifest.getInt("seedPid"))
                .put("coldPid", Process.myPid()).put("restoredEntryIds", JSONArray(expectedEntries))
                .put("restoredCurrentIndex", 3).put("pausedNavigationMeasuredGrowthMs", 0)
            File(evidenceDirectory, "process-death-cold-result.json").writeText(receipt.toString(2))
        } finally { onMain { controller.pause(); controller.release() } }
    }

    @Test fun seekToEndDoesNotComplete() {
        isolatedPackageOnly()
        val runId = "seek-runtime-${UUID.randomUUID()}"
        val item = fixtureItem("$runId-track", "$runId-entry", runId, "$runId-scope", silentWav(runId))
        val controller = connect()
        try {
            onMain {
                controller.pause(); controller.repeatMode = Player.REPEAT_MODE_OFF; controller.shuffleModeEnabled = false
                controller.setMediaItems(listOf(item), 0, 0); controller.prepare(); controller.play()
            }
            awaitCondition("Short real listen before seek") { onMain { controller.isPlaying && controller.currentPosition >= 1800 } }
            onMain { controller.seekTo(19_500L) }
            awaitCondition("Decoder reached natural end after explicit seek") { onMain { controller.playbackState == Player.STATE_ENDED } }
            var snapshot: ListeningLedgerSnapshot? = null
            awaitCondition("Natural end has a real service ledger event") {
                snapshot = ledger()
                snapshot!!.recentEvents.any { it.occurrence.sessionId == runId && it.reason == ListeningEndReason.NATURAL_END }
            }
            val event = snapshot!!.recentEvents.last { it.occurrence.sessionId == runId }
            assertEquals(ListeningEndReason.NATURAL_END, event.reason)
            assertFalse("Seeking near the end cannot manufacture completion", event.naturalCompletion)
            assertFalse(event.personalSkip)
            assertTrue("Skipped media gap must not be counted", event.checkpoint.uniqueCoverageMs in 500..6000)
            assertTrue(event.checkpoint.listenedWallMs in 500..6000)
            assertEquals(0L, snapshot!!.tracks["$runId-track"]!!.completedCount)
        } finally { onMain { controller.pause(); controller.release() } }
    }

    @Test fun decodeFailureDoesNotTeachDislike() {
        isolatedPackageOnly()
        val runId = "decode-runtime-${UUID.randomUUID()}"
        assertTrue(evidenceDirectory.isDirectory || evidenceDirectory.mkdirs())
        val badFile = File(evidenceDirectory, "$runId.wav").apply { writeText("This is deliberately not decodable audio.") }
        val item = fixtureItem("$runId-track", "$runId-entry", runId, "$runId-scope", badFile)
        val controller = connect()
        try {
            onMain {
                controller.pause(); controller.repeatMode = Player.REPEAT_MODE_OFF; controller.shuffleModeEnabled = false
                controller.setMediaItems(listOf(item), 0, 0); controller.prepare(); controller.play()
            }
            awaitCondition("Actual decoder error reached controller") { onMain { controller.playerError != null } }
            onMain { controller.pause() }
            Thread.sleep(1200) // Let the service error callback and ledger IO drain.
            val snapshot = ledger()
            val events = snapshot.recentEvents.filter { it.occurrence.sessionId == runId }
            assertTrue("Decode failure cannot become personal dislike/skip", events.none { it.personalSkip })
            assertTrue("Decode failure cannot create completion/heard time", events.none { it.naturalCompletion || it.checkpoint.listenedWallMs > 0 })
            assertNull("Unactivated failure has no personal listening aggregate", snapshot.tracks["$runId-track"])
            assertNull(snapshot.pending?.takeIf { it.occurrence.sessionId == runId })
        } finally { onMain { controller.pause(); controller.release() } }
    }

    private fun isolatedPackageOnly() {
        check(context.packageName == "com.pixelody.app.shuffleqa") { "Only the dedicated .shuffleqa package may run these tests" }
    }

    private fun connect(): MediaController {
        val future: ListenableFuture<MediaController> = onMain {
            MediaController.Builder(context, SessionToken(context, ComponentName(context, PixelodyPlaybackService::class.java)))
                .setApplicationLooper(Looper.getMainLooper()).buildAsync()
        }
        return future.get(15, TimeUnit.SECONDS)
    }

    private fun fixtureItem(trackId: String, entryId: String, sessionId: String, scopeId: String, file: File): MediaItem {
        val extras = Bundle().apply {
            putString(PlaybackEvidenceMetadata.ENTRY_ID, entryId)
            putString(PlaybackEvidenceMetadata.SESSION_ID, sessionId)
            putString(PlaybackEvidenceMetadata.SCOPE_ID, scopeId)
            putLong(PlaybackEvidenceMetadata.DURATION_MS, 20_000)
            putString(PlaybackEvidenceMetadata.FORMAT, "WAV")
            putBoolean(PlaybackEvidenceMetadata.LOSSLESS, true)
            putBoolean(PlaybackEvidenceMetadata.PERSONAL, true)
        }
        return MediaItem.Builder().setMediaId(trackId).setUri(Uri.fromFile(file)).setMimeType("audio/wav")
            .setMediaMetadata(MediaMetadata.Builder().setTitle(trackId).setArtist("Generated service fixture")
                .setAlbumTitle("Private shuffle runtime QA").setAlbumArtist("Fixture artist")
                .setDiscNumber(1).setTrackNumber(1).setExtras(extras).build()).build()
    }

    private fun silentWav(id: String): File {
        assertTrue(evidenceDirectory.isDirectory || evidenceDirectory.mkdirs())
        val file = File(evidenceDirectory, "$id.wav")
        val bytes = 8000 * 2 * 20
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(bytes + 36).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(bytes).array()
        file.outputStream().use { stream -> stream.write(header); stream.write(ByteArray(bytes)) }
        return file
    }

    private fun entryIds(controller: MediaController): List<String?> =
        (0 until controller.mediaItemCount).map { PlaybackEvidenceMetadata.entryId(controller.getMediaItemAt(it)) }

    private fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)

    private fun <T> onMain(action: () -> T): T {
        val result = AtomicReference<T>()
        instrumentation.runOnMainSync { result.set(action()) }
        return result.get()
    }

    private fun awaitCondition(description: String, timeoutMs: Long = 15_000, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        do {
            if (condition()) return
            Thread.sleep(50)
        } while (SystemClock.elapsedRealtime() < deadline)
        fail(description)
    }
}
