package com.pixelody.app.modules

import android.content.ComponentName
import android.content.Intent
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
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.util.concurrent.ListenableFuture
import com.pixelody.app.core.analytics.ListeningEndReason
import com.pixelody.app.core.analytics.ListeningLedgerSnapshot
import com.pixelody.app.core.analytics.ListeningLedgerStore
import com.pixelody.app.core.playback.PixelodyPlaybackService
import com.pixelody.app.core.playback.PlaybackEvidenceMetadata
import com.pixelody.app.core.shuffle.UniversalShufflePolicy as Policy
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
 * Real service, controller, decoder and production checkpoint/ledger, without an Activity.
 * v2Seed and v2Cold require separate instrumentation invocations with force-stop of ONLY
 * com.pixelody.app.shuffleqa between them. Run other methods AFTER that pair. Native Media3
 * items are a bounded projection; the persisted policy is the full queue authority.
 */
@OptIn(UnstableApi::class)
class UniversalAuthorityRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val evidenceDirectory get() = File(context.filesDir, "universal-authority-runtime")
    private val manifestFile get() = File(evidenceDirectory, "v2-seed.json")
    private val store get() = VersionedPlaybackSessionStore(File(context.filesDir, "universal-shuffle"), "universal-authority")

    @Test fun queueScreenShowsPolicyReasonsAndControlsActualPausedAuthority() {
        isolatedPackageOnly()
        val fixture = fixture("queue-ui", 6)
        val controller = connect()
        val device = UiDevice.getInstance(instrumentation)
        fun tap(label: String) {
            var target = requireNotNull(device.wait(Until.findObject(By.text(label)), 10_000)) { "Visible queue control: $label" }
            // Compose exposes the label as a non-clickable child. Invoke the
            // button's accessibility action rather than relying on a text tap.
            while (!target.isClickable && target.parent != null) target = target.parent
            assertTrue("Clickable queue control: $label", target.isClickable)
            target.click()
            device.waitForIdle()
        }
        fun learningEnabled(expected: Boolean) {
            // Poll the published service state, then verify disk once. Repeated
            // checksummed disk loads contend with the writer's canonical lock.
            awaitCondition("UI learning control publishes enabled=$expected") {
                val response = onMain { controller.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY), Bundle().apply { putString("action", "snapshot") }) }.get(5, TimeUnit.SECONDS)
                assertEquals(SessionResult.RESULT_SUCCESS, response.resultCode)
                val observed = JSONObject(response.extras.getString("learning")!!).getBoolean("enabled")
                if (observed != expected) Thread.sleep(200)
                observed == expected
            }
            val durable = VersionedPlaybackSessionStore(File(context.filesDir, "universal-shuffle"), "universal-learning").load().checkpoint
                ?.policyStateJson?.let { JSONObject(it).getJSONObject("ledger").getBoolean("enabled") }
            assertEquals("Published learning control was saved before publication", expected, durable)
        }
        try {
            startPaused(controller, fixture)
            onMain { controller.repeatMode = Player.REPEAT_MODE_ALL }
            mode(controller, "balanced")
            val before = awaitState(fixture.sessionId, "Balanced paused Repeat All preview is durable") { it.getString("mode") == "balanced" && it.getString("repeat") == "all" && it.optJSONObject("pending") != null }
            var firstReceipt: JSONObject? = null
            awaitCondition("Balanced policy receipts reached the controller projection") {
                firstReceipt = onMain {
                    if (controller.mediaItemCount <= controller.currentMediaItemIndex + 1) null
                    else controller.getMediaItemAt(controller.currentMediaItemIndex + 1).mediaMetadata.extras?.getString(PlaybackEvidenceMetadata.DECISION)?.let(::JSONObject)
                }
                firstReceipt?.getString("mode") == "balanced"
            }
            val expectedReason = firstReceipt!!.getString("kind").replace('-', ' ') + " · " + firstReceipt!!.getString("strategy").replace('-', ' ')
            device.wakeUp()
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://queue")).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(device.wait(Until.hasObject(By.text("Shuffle memory")), 15_000))
            val reasonVisible = device.wait(Until.hasObject(By.textContains(expectedReason)), 10_000)
            val uiEvidence = File(context.getExternalFilesDir(null), "universal-authority-runtime").apply { mkdirs() }
            device.dumpWindowHierarchy(File(uiEvidence, "queue-window.xml"))
            device.takeScreenshot(File(uiEvidence, "queue-before-controls.png"))
            assertTrue("Actual policy reason is visible beneath a queue row", reasonVisible)
            tap("Pause learning"); learningEnabled(false)
            tap("Enable learning"); learningEnabled(true)
            tap("Reshuffle remaining")
            val shuffled = awaitState(fixture.sessionId, "UI reshuffle reaches the authority") { it.getLong("revision") > before.getLong("revision") }
            assertEquals(fixture.entryIds, shuffled.getJSONArray("remaining").strings())
            assertEquals(0L, shuffled.getLong("occurrenceSequence"))
            assertEquals(Policy.canonicalJson(before.getJSONObject("rng")), Policy.canonicalJson(shuffled.getJSONObject("rng")))
            tap("New cycle")
            val cycled = awaitState(fixture.sessionId, "UI New cycle starts a fresh entitlement pool") { it.getJSONObject("cycle").getInt("generation") == 1 }
            assertEquals(fixture.entryIds, cycled.getJSONArray("remaining").strings())
            assertEquals(0L, cycled.getLong("occurrenceSequence"))
            onMain { assertFalse(controller.isPlaying); assertFalse(controller.playWhenReady) }
            val screenshot = File(context.getExternalFilesDir(null), "universal-authority-runtime/queue-v2.png")
            assertTrue(screenshot.parentFile!!.isDirectory || screenshot.parentFile!!.mkdirs())
            assertTrue(device.takeScreenshot(screenshot))
            writeJson(File(evidenceDirectory, "queue-v2.json"), JSONObject().put("sessionId", fixture.sessionId).put("reasonVisible", true)
                .put("pauseEnableLearningDurable", true).put("reshufflePreservesRemainingAndRng", true).put("explicitCycle", 1).put("confirmedOccurrences", 0))
        } finally { releasePaused(controller) }
    }

    @Test fun decoderFailureRetainsFailedEntitlementAndRecoversWithoutTasteEvidence() {
        isolatedPackageOnly()
        val original = fixture("decode-recovery", 2)
        val bad = File(evidenceDirectory, "${original.sessionId}-invalid.wav").apply { writeText("Deliberately invalid audio fixture") }
        val fixture = original.copy(items = listOf(original.items[0].buildUpon().setUri(Uri.fromFile(bad)).build(), original.items[1]))
        val controller = connect()
        try {
            startPlaying(controller, fixture)
            val recovered = awaitConfirmed(controller, fixture, 1, fixture.entryIds[1])
            assertEquals(fixture.entryIds, recovered.getJSONArray("entries").objects().map { it.getString("entryId") })
            assertEquals(listOf(fixture.entryIds[0]), recovered.getJSONArray("remaining").strings())
            assertEquals(listOf(fixture.entryIds[1]), recovered.getJSONArray("history").objects().map { it.getString("entryId") })
            assertTrue(recovered.getJSONArray("diagnostics").strings().contains("activation-failed"))
            pause(controller)
            awaitCondition("Failed decoding is offered but never activated or learned as dislike") {
                val learning = VersionedPlaybackSessionStore(File(context.filesDir, "universal-shuffle"), "universal-learning").load().checkpoint
                    ?.policyStateJson?.let { JSONObject(it).getJSONObject("ledger") }
                val row = learning?.getJSONArray("tracks")?.objects()?.firstOrNull { it.getString("trackId") == fixture.trackIds[0] }
                row != null && row.getLong("offered") > 0 && row.getLong("activated") == 0L && row.getLong("negativeUnits") == 0L &&
                    row.getLong("heardWallMs") == 0L && row.getLong("naturalEnds") == 0L && row.getLong("deliberateSkips") == 0L
            }
            val failedFacts = ledger().recentEvents.filter { it.occurrence.sessionId == fixture.sessionId && it.occurrence.entryId == fixture.entryIds[0] }
            assertTrue(failedFacts.none { it.personalSkip || it.naturalCompletion || it.checkpoint.listenedWallMs > 0 })
        } finally { releasePaused(controller) }
    }

    @Test fun confirmedActivationAndRetainedHistoryConsumeOnce() {
        isolatedPackageOnly()
        val fixture = fixture("activation", 5)
        val controller = connect()
        try {
            startPaused(controller, fixture)
            val prepared = awaitState(fixture.sessionId, "Prepared intent must not consume") {
                it.getJSONArray("history").length() == 0 && it.optJSONObject("pending") != null
            }
            assertEquals(fixture.entryIds, prepared.getJSONArray("remaining").strings())
            onMain { assertFalse(controller.isPlaying); controller.play() }
            val first = awaitConfirmed(controller, fixture, 1, fixture.entryIds[0])
            assertEquals(fixture.entryIds.drop(1), first.getJSONArray("remaining").strings())
            val second = nextConfirmed(controller, fixture, 2, fixture.entryIds[1])
            assertEquals(fixture.entryIds.drop(2), second.getJSONArray("remaining").strings())
            val third = nextConfirmed(controller, fixture, 3, fixture.entryIds[2])
            val history = third.getJSONArray("history")
            assertEquals(fixture.entryIds.take(3), history.objects().map { it.getString("entryId") })
            assertEquals("A catalog ID deliberately occurs twice", history.getJSONObject(0).getString("trackId"), history.getJSONObject(2).getString("trackId"))
            assertNotEquals("Separate genuine activations have separate occurrence IDs", history.getJSONObject(0).getString("occurrenceId"), history.getJSONObject(2).getString("occurrenceId"))
            val retainedHistory = Policy.canonicalJson(history)
            val remaining = third.getJSONArray("remaining").strings()
            val rng = Policy.canonicalJson(third.getJSONObject("rng"))
            onMain { controller.seekToPreviousMediaItem() }
            awaitState(fixture.sessionId, "Confirmed Previous reuses B history") {
                it.getInt("cursor") == 1 && it.isNull("pending") && currentId(it) == fixture.entryIds[1]
            }
            onMain { controller.seekToNextMediaItem() }
            val returned = awaitState(fixture.sessionId, "Confirmed Next reuses A2 retained history") {
                it.getInt("cursor") == 2 && it.isNull("pending") && currentId(it) == fixture.entryIds[2]
            }
            assertEquals(3L, returned.getLong("occurrenceSequence"))
            assertEquals(retainedHistory, Policy.canonicalJson(returned.getJSONArray("history")))
            assertEquals(remaining, returned.getJSONArray("remaining").strings())
            assertEquals(rng, Policy.canonicalJson(returned.getJSONObject("rng")))
            Thread.sleep(350)
            assertEquals("Repeated service events cannot commit twice", 3L, state(fixture.sessionId)!!.getLong("occurrenceSequence"))
            onMain { assertFalse(controller.shuffleModeEnabled) }
        } finally { releasePaused(controller) }
    }

    @Test fun modePreservesRemainingAndClearFutureClearsHiddenPool() {
        isolatedPackageOnly()
        val fixture = fixture("hidden-pool", 48)
        val controller = connect()
        try {
            startPlaying(controller, fixture)
            awaitConfirmed(controller, fixture, 1, fixture.entryIds[0])
            pause(controller)
            val before = state(fixture.sessionId)!!
            val remaining = before.getJSONArray("remaining").strings()
            assertEquals(47, remaining.size)
            onMain { assertTrue("Projection cannot expose all 48 source entries", controller.mediaItemCount < 48) }
            mode(controller, "balanced")
            val changed = awaitState(fixture.sessionId, "Mode command changes the authoritative policy") { it.getString("mode") == "balanced" }
            assertEquals("Mode changes retain every unfinished entitlement", remaining, changed.getJSONArray("remaining").strings())
            assertEquals(Policy.canonicalJson(before.getJSONArray("history")), Policy.canonicalJson(changed.getJSONArray("history")))
            assertEquals(Policy.canonicalJson(before.getJSONObject("rng")), Policy.canonicalJson(changed.getJSONObject("rng")))
            command(controller, "clear-future")
            val cleared = awaitState(fixture.sessionId, "Clear future includes entries beyond the 32-item projection") {
                it.getJSONArray("remaining").length() == 0 && it.getJSONArray("entries").length() == 1
            }
            assertEquals(listOf(fixture.entryIds[0]), cleared.getJSONArray("entries").objects().map { it.getString("entryId") })
            assertEquals(1L, cleared.getLong("occurrenceSequence"))
            assertEquals(listOf(fixture.entryIds[0]), checkpoint(fixture.sessionId)!!.queue.map { it.entryId })
            awaitCondition("Hidden future also disappears from native projection") { onMain { controller.mediaItemCount == 1 } }
            onMain { assertFalse(controller.isPlaying); assertFalse(controller.shuffleModeEnabled) }
        } finally { releasePaused(controller) }
    }

    @Test fun naturalEndAfterSeekDoesNotFabricateCompletion() {
        isolatedPackageOnly()
        val fixture = fixture("natural-seek", 2)
        val controller = connect()
        try {
            startPlaying(controller, fixture)
            awaitConfirmed(controller, fixture, 1, fixture.entryIds[0])
            awaitCondition("Actually decode a short prefix before seeking") { onMain { controller.isPlaying && controller.currentPosition >= 1200 } }
            onMain { controller.seekTo(19_900L) }
            awaitConfirmed(controller, fixture, 2, fixture.entryIds[1])
            var facts: ListeningLedgerSnapshot? = null
            awaitCondition("Actual natural boundary reaches production listening ledger") {
                facts = ledger()
                facts!!.recentEvents.any { it.occurrence.sessionId == fixture.sessionId && it.occurrence.entryId == fixture.entryIds[0] && it.reason == ListeningEndReason.NATURAL_END }
            }
            val ended = facts!!.recentEvents.last { it.occurrence.sessionId == fixture.sessionId && it.occurrence.entryId == fixture.entryIds[0] }
            assertFalse("A natural decoder boundary after seeking is not full coverage", ended.naturalCompletion)
            assertFalse("A decoder boundary is not a personal skip", ended.personalSkip)
            assertTrue("The skipped nineteen-second gap is not heard time", ended.checkpoint.listenedWallMs in 300..6500)
            assertTrue(ended.checkpoint.uniqueCoverageMs in 300..6500)
            assertEquals(0L, facts!!.tracks.getValue(fixture.trackIds[0]).completedCount)
            assertEquals(20_000L, ended.occurrence.track.durationMs)
            assertEquals("WAV", ended.occurrence.track.format)
            assertTrue(ended.occurrence.track.isLossless)
        } finally { releasePaused(controller) }
    }

    @Test fun repeatOneNaturalEndDoesNotConsumeAnotherEntitlement() {
        isolatedPackageOnly()
        val fixture = fixture("repeat-one", 2)
        val controller = connect()
        try {
            startPlaying(controller, fixture)
            val first = awaitConfirmed(controller, fixture, 1, fixture.entryIds[0])
            onMain { controller.repeatMode = Player.REPEAT_MODE_ONE }
            awaitState(fixture.sessionId, "Repeat One reached policy") { it.getString("repeat") == "one" }
            onMain { controller.seekTo(19_900L) }
            val repeated = awaitConfirmed(controller, fixture, 2, fixture.entryIds[0])
            assertEquals(first.getJSONArray("remaining").strings(), repeated.getJSONArray("remaining").strings())
            assertEquals(0, repeated.getJSONObject("cycle").getInt("generation"))
            assertEquals("repeat-one", repeated.getJSONArray("history").getJSONObject(1).getString("kind"))
            assertNotEquals(repeated.getJSONArray("history").getJSONObject(0).getString("occurrenceId"), repeated.getJSONArray("history").getJSONObject(1).getString("occurrenceId"))
            onMain { assertFalse(controller.shuffleModeEnabled) }
        } finally { releasePaused(controller) }
    }

    @Test fun multipleCyclesConsumeEveryDuplicateEntryExactlyOncePerCycle() {
        isolatedPackageOnly()
        val fixture = fixture("cycles", 5)
        val controller = connect()
        try {
            startPlaying(controller, fixture)
            awaitConfirmed(controller, fixture, 1, fixture.entryIds[0])
            onMain { controller.repeatMode = Player.REPEAT_MODE_ALL }
            awaitState(fixture.sessionId, "Repeat All reached policy") { it.getString("repeat") == "all" }
            for (sequence in 2..41) nextConfirmed(controller, fixture, sequence.toLong(), fixture.entryIds[(sequence - 1) % 5])
            val final = state(fixture.sessionId)!!
            val history = final.getJSONArray("history").objects()
            assertEquals(8, final.getJSONObject("cycle").getInt("generation"))
            assertEquals(fixture.entryIds, history.take(5).map { it.getString("entryId") })
            assertEquals(fixture.entryIds, history.drop(5).take(5).map { it.getString("entryId") })
            assertEquals(fixture.entryIds.drop(1), final.getJSONArray("remaining").strings())
            assertEquals(41, history.map { it.getString("occurrenceId") }.distinct().size)
            history.take(40).chunked(5).forEach { cycle -> assertEquals(fixture.entryIds, cycle.map { it.getString("entryId") }) }
            onMain { assertTrue("Media3 stays bounded while all 41 occurrences remain durable", controller.mediaItemCount <= 65) }
            onMain { controller.seekToPreviousMediaItem() }
            val previous = awaitState(fixture.sessionId, "Previous traverses retained history beyond the native prefix window") {
                it.isNull("pending") && it.getInt("cursor") == 39
            }
            assertEquals(41L, previous.getLong("occurrenceSequence"))
            assertEquals(Policy.canonicalJson(final.getJSONObject("rng")), Policy.canonicalJson(previous.getJSONObject("rng")))
            assertEquals(2, history.take(5).count { it.getString("trackId") == fixture.trackIds[0] })
            assertEquals(2, history.drop(5).take(5).count { it.getString("trackId") == fixture.trackIds[0] })
        } finally { releasePaused(controller) }
    }

    @Test fun explicitCyclePreservesOccurrenceAccounting() {
        isolatedPackageOnly()
        val fixture = fixture("explicit-cycle", 5)
        val controller = connect()
        try {
            startPlaying(controller, fixture)
            awaitConfirmed(controller, fixture, 1, fixture.entryIds[0])
            nextConfirmed(controller, fixture, 2, fixture.entryIds[1])
            pause(controller)
            val before = state(fixture.sessionId)!!
            command(controller, "new-cycle")
            val after = awaitState(fixture.sessionId, "Explicit cycle resets entitlement only when requested") { it.getJSONObject("cycle").getInt("generation") == 1 }
            assertEquals(fixture.entryIds, after.getJSONArray("remaining").strings())
            assertEquals(2L, after.getLong("occurrenceSequence"))
            assertEquals(Policy.canonicalJson(before.getJSONArray("history")), Policy.canonicalJson(after.getJSONArray("history")))
            assertEquals(currentId(before), currentId(after))
            onMain { assertFalse(controller.isPlaying) }
        } finally { releasePaused(controller) }
    }

    /** Seed phase: full pool + real history, then a distinct unconfirmed paused intent. */
    @Test fun v2Seed() {
        isolatedPackageOnly()
        val fixture = fixture("restart", 5)
        val controller = connect()
        try {
            // Only this dedicated QA package: explicit cleanup avoids accumulating
            // more than the policy's 32 retained scopes across repeated test runs.
            if (store.load().checkpoint != null) {
                command(controller, "evict-other-scopes")
                awaitCondition("Explicit QA scope cleanup completed before creating a new fixture") {
                    store.load().checkpoint?.policyStateJson?.let { JSONObject(it).getJSONObject("bank").getJSONArray("scopes").length() <= 1 } == true
                }
            }
            startPlaying(controller, fixture)
            awaitConfirmed(controller, fixture, 1, fixture.entryIds[0])
            awaitCondition("Real first occurrence heard before navigation") { onMain { controller.isPlaying && controller.currentPosition >= 1200 } }
            nextConfirmed(controller, fixture, 2, fixture.entryIds[1])
            awaitCondition("Real second occurrence heard before pause") { onMain { controller.isPlaying && controller.currentPosition >= 700 } }
            pause(controller)
            onMain { controller.seekTo(1234L); controller.repeatMode = Player.REPEAT_MODE_ALL }
            mode(controller, "balanced")
            awaitState(fixture.sessionId, "Pause position/repeat/mode persisted") {
                it.getString("mode") == "balanced" && it.getString("repeat") == "all" && it.getLong("lastConfirmedPositionMs") == 1234L && it.isNull("pending")
            }
            val committed = state(fixture.sessionId)!!
            val target = fixture.entryIds[2]
            val nativeIndex = onMain { (0 until controller.mediaItemCount).first { PlaybackEvidenceMetadata.entryId(controller.getMediaItemAt(it)) == target } }
            onMain { controller.seekTo(nativeIndex, 4321L) }
            val selected = awaitState(fixture.sessionId, "Paused A2 selection is persisted as an intent, not an activation") {
                it.optJSONObject("pending")?.getString("entryId") == target && it.getLong("occurrenceSequence") == 2L
            }
            awaitCondition("Controller shows unconfirmed A2 selection paused at intent position") {
                onMain { PlaybackEvidenceMetadata.entryId(controller.currentMediaItem) == target && !controller.isPlaying && !controller.playWhenReady && kotlin.math.abs(controller.currentPosition - 4321) <= 100 }
            }
            assertEquals(committed.getJSONArray("remaining").strings(), selected.getJSONArray("remaining").strings())
            assertEquals(Policy.canonicalJson(committed.getJSONArray("history")), Policy.canonicalJson(selected.getJSONArray("history")))
            assertEquals(Policy.canonicalJson(committed.getJSONObject("rng")), Policy.canonicalJson(selected.getJSONObject("rng")))
            val saved = checkpoint(fixture.sessionId)!!
            assertEquals(fixture.entryIds, saved.queue.map { it.entryId })
            assertEquals(fixture.trackIds, saved.queue.map { it.trackId })
            assertEquals(1, saved.currentIndex)
            assertEquals(1234L, saved.positionMs)
            assertEquals(Player.REPEAT_MODE_ALL, saved.repeatMode)
            val payload = JSONObject(saved.policyStateJson!!)
            assertEquals(1, payload.getInt("adapterVersion"))
            assertEquals(4321L, payload.getJSONObject("intent").getLong("positionMs"))
            var facts: ListeningLedgerSnapshot? = null
            awaitCondition("Finalized real history exists and paused target has no listening activation") {
                facts = ledger()
                facts!!.recentEvents.count { it.occurrence.sessionId == fixture.sessionId } >= 2 && facts!!.pending?.occurrence?.sessionId != fixture.sessionId
            }
            val events = facts!!.recentEvents.filter { it.occurrence.sessionId == fixture.sessionId }
            assertTrue(events.all { it.occurrence.entryId in fixture.entryIds.take(2) && !it.naturalCompletion })
            val manifest = JSONObject().put("seedPid", Process.myPid()).put("sessionId", fixture.sessionId).put("scopeId", fixture.scopeId)
                .put("entryIds", JSONArray(fixture.entryIds)).put("trackIds", JSONArray(fixture.trackIds))
                .put("committedState", durableFields(selected)).put("intentEntryId", target).put("intentPositionMs", 4321)
                .put("confirmedPositionMs", saved.positionMs).put("pendingAttemptId", selected.getJSONObject("pending").getString("attemptId"))
                .put("scopeIds", JSONArray(payload.getJSONObject("bank").getJSONArray("scopes").objects().map { it.getString("scopeId") }))
                .put("listeningFacts", listeningFacts(facts!!, fixture.sessionId))
            writeJson(manifestFile, manifest)
        } finally { releasePaused(controller) }
    }

    /** Cold phase deliberately rejects same-process runs and never calls setMediaItems. */
    @Test fun v2Cold() {
        isolatedPackageOnly()
        assertTrue("Run v2Seed in an earlier isolated instrumentation invocation", manifestFile.isFile)
        val manifest = JSONObject(manifestFile.readText())
        assertNotEquals("Force-stop ONLY com.pixelody.app.shuffleqa between invocations", manifest.getInt("seedPid"), Process.myPid())
        val sessionId = manifest.getString("sessionId")
        val controller = connect()
        try {
            val restored = awaitState(sessionId, "Cold bind durably replaces the old reservation with a fresh paused intent") {
                val pending = it.optJSONObject("pending")
                pending?.getString("entryId") == manifest.getString("intentEntryId") && pending.getString("attemptId") != manifest.getString("pendingAttemptId")
            }
            assertEquals(Policy.canonicalJson(manifest.getJSONObject("committedState")), Policy.canonicalJson(durableFields(restored)))
            assertNotEquals("Old process reservation cannot be reused", manifest.getString("pendingAttemptId"), restored.getJSONObject("pending").getString("attemptId"))
            val saved = checkpoint(sessionId)!!
            val payload = JSONObject(saved.policyStateJson!!)
            assertEquals(manifest.getJSONArray("entryIds").strings(), saved.queue.map { it.entryId })
            assertEquals(manifest.getJSONArray("trackIds").strings(), saved.queue.map { it.trackId })
            assertEquals(manifest.getString("scopeId"), saved.scopeId)
            assertEquals(manifest.getLong("confirmedPositionMs"), saved.positionMs)
            assertEquals(manifest.getJSONArray("scopeIds").strings(), payload.getJSONObject("bank").getJSONArray("scopes").objects().map { it.getString("scopeId") })
            awaitCondition("Paused future intent restored to its own position") {
                onMain { PlaybackEvidenceMetadata.entryId(controller.currentMediaItem) == manifest.getString("intentEntryId") && kotlin.math.abs(controller.currentPosition - manifest.getLong("intentPositionMs")) <= 250 && !controller.playWhenReady && !controller.isPlaying }
            }
            onMain { assertFalse(controller.shuffleModeEnabled); assertEquals(Player.REPEAT_MODE_ALL, controller.repeatMode) }
            val baseline = listeningFacts(ledger(), sessionId)
            assertEquals(Policy.canonicalJson(manifest.getJSONObject("listeningFacts")), Policy.canonicalJson(baseline))
            Thread.sleep(2200)
            assertEquals("Paused cold binding cannot create listening facts", Policy.canonicalJson(baseline), Policy.canonicalJson(listeningFacts(ledger(), sessionId)))
            assertEquals("Paused projection cannot consume or redraw live RNG", Policy.canonicalJson(manifest.getJSONObject("committedState")), Policy.canonicalJson(durableFields(state(sessionId)!!)))
            writeJson(File(evidenceDirectory, "v2-cold.json"), JSONObject().put("seedPid", manifest.getInt("seedPid")).put("coldPid", Process.myPid()).put("sessionId", sessionId)
                .put("fullPoolSize", saved.queue.size).put("confirmedOccurrences", restored.getLong("occurrenceSequence")).put("pausedIntentConsumed", false).put("pausedListeningGrowthMs", 0))
        } finally { releasePaused(controller) }
    }

    private data class Fixture(val sessionId: String, val scopeId: String, val entryIds: List<String>, val trackIds: List<String>, val items: List<MediaItem>)

    private fun fixture(label: String, count: Int): Fixture {
        val runId = "authority-$label-${UUID.randomUUID()}"
        val entries = (0 until count).map { "$runId-entry-${if (it == 0) "A1" else if (it == 2) "A2" else it}" }
        val tracks = (0 until count).map { "$runId-track-${if (it == 2) 0 else it}" }
        val wav = silentWav(runId)
        val scopeId = "$runId-scope"
        val items = tracks.indices.map { index ->
            val extras = Bundle().apply {
                putString(PlaybackEvidenceMetadata.ENTRY_ID, entries[index]); putString(PlaybackEvidenceMetadata.SESSION_ID, runId)
                putString(PlaybackEvidenceMetadata.SCOPE_ID, scopeId); putString(PlaybackEvidenceMetadata.MODE, "off")
                putLong(PlaybackEvidenceMetadata.DURATION_MS, 20_000); putString(PlaybackEvidenceMetadata.FORMAT, "WAV")
                putBoolean(PlaybackEvidenceMetadata.LOSSLESS, true); putBoolean(PlaybackEvidenceMetadata.PERSONAL, true)
            }
            MediaItem.Builder().setMediaId(tracks[index]).setUri(Uri.fromFile(wav)).setMimeType("audio/wav")
                .setMediaMetadata(MediaMetadata.Builder().setTitle("Private fixture $index").setArtist("Generated service fixture")
                    .setAlbumTitle("Private authority QA").setAlbumArtist("Fixture artist").setDiscNumber(1).setTrackNumber(index + 1).setExtras(extras).build()).build()
        }
        return Fixture(runId, scopeId, entries, tracks, items)
    }

    private fun startPaused(controller: MediaController, fixture: Fixture) {
        onMain {
            controller.pause(); controller.repeatMode = Player.REPEAT_MODE_OFF; controller.shuffleModeEnabled = false
            controller.setMediaItems(fixture.items, 0, 0); controller.prepare()
        }
        awaitCondition("Prepared private source rendered while paused") { onMain { PlaybackEvidenceMetadata.entryId(controller.currentMediaItem) == fixture.entryIds[0] && controller.playbackState == Player.STATE_READY && !controller.isPlaying } }
    }

    private fun startPlaying(controller: MediaController, fixture: Fixture) {
        // Deliberately exercise play arriving before async authority setMediaItems renders.
        onMain {
            controller.pause(); controller.repeatMode = Player.REPEAT_MODE_OFF; controller.shuffleModeEnabled = false
            controller.setMediaItems(fixture.items, 0, 0); controller.prepare(); controller.play()
        }
    }

    private fun awaitConfirmed(controller: MediaController, fixture: Fixture, sequence: Long, entry: String): JSONObject {
        awaitCondition("Actual playback of $entry confirmed at sequence $sequence") { onMain { controller.isPlaying && PlaybackEvidenceMetadata.entryId(controller.currentMediaItem) == entry } }
        return awaitState(fixture.sessionId, "Durable confirmed occurrence $sequence") { it.getLong("occurrenceSequence") == sequence && it.isNull("pending") && currentId(it) == entry }
    }

    private fun nextConfirmed(controller: MediaController, fixture: Fixture, sequence: Long, entry: String): JSONObject {
        onMain { controller.seekToNextMediaItem() }
        return awaitConfirmed(controller, fixture, sequence, entry)
    }

    private fun pause(controller: MediaController) {
        onMain { controller.pause() }
        awaitCondition("Real controller paused") { onMain { !controller.playWhenReady && !controller.isPlaying } }
    }

    private fun releasePaused(controller: MediaController) = onMain { controller.pause(); controller.release() }
    private fun mode(controller: MediaController, mode: String) = custom(controller, PlaybackEvidenceMetadata.COMMAND_MODE, Bundle().apply { putString("mode", mode) })
    private fun command(controller: MediaController, action: String) = custom(controller, PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle().apply { putString("action", action) })

    private fun custom(controller: MediaController, name: String, args: Bundle) {
        val future = onMain { controller.sendCustomCommand(SessionCommand(name, Bundle.EMPTY), args) }
        assertEquals("Own-package authority command $name accepted", SessionResult.RESULT_SUCCESS, future.get(15, TimeUnit.SECONDS).resultCode)
    }

    private fun checkpoint(sessionId: String): PlaybackSessionCheckpoint? = store.load().checkpoint?.takeIf { it.sessionId == sessionId }
    private fun state(sessionId: String): JSONObject? = checkpoint(sessionId)?.policyStateJson?.let { JSONObject(it).getJSONObject("session") }
    private fun currentId(state: JSONObject): String? = Policy.currentOccurrence(state)?.getString("entryId")
    private fun awaitState(sessionId: String, description: String, condition: (JSONObject) -> Boolean): JSONObject {
        var result: JSONObject? = null
        awaitCondition(description) { result = state(sessionId); result?.let(condition) == true }
        return result!!
    }

    /** Restore intentionally clears old pending/quarantine/undo and sets paused=true. */
    private fun durableFields(state: JSONObject): JSONObject = JSONObject().apply {
        listOf("schemaVersion", "policyVersion", "configVersion", "sessionId", "scopeId", "authorityId", "authorityEpoch", "revision", "membershipRevision",
            "mode", "repeat", "options", "cycle", "rng", "entries", "remaining", "history", "cursor", "occurrenceSequence", "generatedFuture", "manual", "exclusions", "albumLock",
            "lastConfirmedPositionMs", "eventSequences", "diagnostics", "imported").forEach { put(it, state.get(it)) }
    }

    private fun ledger() = ListeningLedgerStore(File(context.filesDir, "universal-shuffle/listening")).reload()
    private fun listeningFacts(snapshot: ListeningLedgerSnapshot, sessionId: String): JSONObject {
        val events = snapshot.recentEvents.filter { it.occurrence.sessionId == sessionId }
        return JSONObject().put("eventIds", JSONArray(events.map { it.occurrence.eventId })).put("entryIds", JSONArray(events.map { it.occurrence.entryId }))
            .put("listenedWallMs", events.sumOf { it.checkpoint.listenedWallMs }).put("uniqueCoverageMs", events.sumOf { it.checkpoint.uniqueCoverageMs })
            .put("completedCount", events.count { it.naturalCompletion }).put("pending", snapshot.pending?.takeIf { it.occurrence.sessionId == sessionId }?.occurrence?.eventId ?: JSONObject.NULL)
    }

    private fun isolatedPackageOnly() {
        check(context.packageName == "com.pixelody.app.shuffleqa") { "Only the exact dedicated com.pixelody.app.shuffleqa package may run these tests" }
    }

    private fun connect(): MediaController {
        val future: ListenableFuture<MediaController> = onMain {
            MediaController.Builder(context, SessionToken(context, ComponentName(context, PixelodyPlaybackService::class.java)))
                .setApplicationLooper(Looper.getMainLooper()).buildAsync()
        }
        return future.get(15, TimeUnit.SECONDS)
    }

    private fun silentWav(id: String): File {
        assertTrue(evidenceDirectory.isDirectory || evidenceDirectory.mkdirs())
        val file = File(evidenceDirectory, "$id.wav")
        val bytes = 8000 * 2 * 20
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(bytes + 36).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(bytes).array()
        file.outputStream().use { stream -> stream.write(header); stream.write(ByteArray(bytes)); stream.fd.sync() }
        return file
    }

    private fun writeJson(file: File, value: JSONObject) {
        assertTrue(evidenceDirectory.isDirectory || evidenceDirectory.mkdirs())
        file.outputStream().use { stream -> stream.write(value.toString(2).toByteArray(Charsets.UTF_8)); stream.fd.sync() }
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
    private fun <T> onMain(action: () -> T): T {
        val result = AtomicReference<T>()
        instrumentation.runOnMainSync { result.set(action()) }
        return result.get()
    }

    private fun awaitCondition(description: String, timeoutMs: Long = 20_000, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        do {
            if (condition()) return
            Thread.sleep(50)
        } while (SystemClock.elapsedRealtime() < deadline)
        fail(description)
    }
}
