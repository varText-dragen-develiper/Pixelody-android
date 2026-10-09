package com.pixelody.app.modules

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.*
import com.pixelody.app.data.model.*
import com.pixelody.app.data.storage.*
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class StudioAccessRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val settings = MobileSettingsStore(context)
    private fun finish() = instrumentation.runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toList().forEach { it.finishAndRemoveTask() }
    }
    private fun open(route: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route")).setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        device.waitForIdle(); Thread.sleep(600)
    }
    private fun find(selector: BySelector): UiObject2 {
        for (up in listOf(true, false)) repeat(14) {
            device.findObject(selector)?.let { return it }
            assertEquals(context.packageName, device.currentPackageName)
            val x = device.displayWidth / 30
            device.swipe(x, if (up) 1400 else 1000, x, if (up) 1000 else 1400, 60)
            device.waitForIdle()
        }
        error("Missing $selector")
    }
    private fun click(node: UiObject2) { val b=node.visibleBounds; device.click(b.centerX(), b.centerY()); device.waitForIdle(); Thread.sleep(700) }
    private fun tap(text: String) = click(find(By.text(text)))
    private fun assertCurrent(id: String) {
        repeat(60) { if (settings.loadLastTrackId() != id) Thread.sleep(100) }
        assertEquals(id, settings.loadLastTrackId())
    }
    private fun track(number: Int): Track {
        val id = "studio-access-fixture-$number"
        val file = File(context.cacheDir, "$id.wav")
        val bytes = 8000 * 2 * 120
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(bytes + 36).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(bytes).array()
        file.outputStream().use { it.write(header); it.write(ByteArray(bytes)) }
        return Track(id=id, title="Access fixture $number", artist="Gesture QA", album="Access QA",
            durationSeconds=120, format="wav", streamUrl=Uri.fromFile(file).toString())
    }
    private fun findFlow(selector: BySelector): UiObject2 {
        val downFirst = selector.toString().contains("Match this song")
        for (up in if (downFirst) listOf(false, true) else listOf(true, false)) repeat(14) {
            device.waitForIdle()
            device.findObject(selector)?.let { node ->
                val isWheelKey = node.contentDescription?.matches(Regex("\\d{1,2}[AB], .+")) == true
                if (!isWheelKey || node.visibleBounds.height() >= 48 * context.resources.displayMetrics.density - 1) return node
            }
            assertEquals(context.packageName, device.currentPackageName)
            val x = device.displayWidth / 4
            device.swipe(x, if (up) 1400 else 1000, x, if (up) 1000 else 1400, 80)
            device.waitForIdle(); Thread.sleep(900)
        }
        error("Missing $selector")
    }
    private fun tapFlow(text: String) = click(findFlow(By.text(text)))
    private fun closeFlow() {
        device.pressBack(); device.waitForIdle(); Thread.sleep(700)
        assertFalse(device.hasObject(By.text("Choose how the next songs unfold. Your current song keeps playing.")))
    }

    @Test fun flowUnifiesModesMatchingAndQueueWithoutInterruptingPlayback() {
        assumeTrue("Isolated app only", context.packageName.endsWith(".studioqa"))
        val prefs = context.getSharedPreferences("pixelody_mobile_settings", 0)
        val original = prefs.all.toMap()
        val tracks = (1..4).map(::track).mapIndexed { index, track -> track.copy(title = when(index) {
            0 -> "Flow fixture Key:8A BPM:120"
            1 -> "Flow fixture Key:9A BPM:128"
            2 -> "Flow fixture Key:2B BPM:170"
            else -> "Flow fixture unknown"
        }) }
        var controller: androidx.media3.session.MediaController? = null
        try {
            finish()
            settings.saveTheme(PixelodyMobileTheme.Studio); settings.resetAppearance()
            settings.saveExperienceMode(AppExperienceMode.Essential)
            settings.saveShuffleMode("Off"); settings.saveShuffleEnabled(false)
            runBlocking { PixelodyPersistenceRepository(context).cacheScannedTracks(tracks) }
            open("library"); findFlow(By.text(tracks[0].title))
            lateinit var future: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>
            instrumentation.runOnMainSync {
                future = androidx.media3.session.MediaController.Builder(context,
                    androidx.media3.session.SessionToken(context, android.content.ComponentName(context,
                        com.pixelody.app.core.playback.PixelodyPlaybackService::class.java)))
                    .setApplicationLooper(android.os.Looper.getMainLooper()).buildAsync()
            }
            controller = future.get(10, java.util.concurrent.TimeUnit.SECONDS)
            val active = controller!!
            instrumentation.runOnMainSync {
                active.setMediaItems(tracks.map { track -> androidx.media3.common.MediaItem.Builder()
                    .setMediaId(track.id).setUri(track.streamUrl).build() }, 0, 37000)
                active.prepare(); active.pause()
            }
            Thread.sleep(1000)
            open("player"); tapFlow("Shuffle"); findFlow(By.text("Flow & shuffle"))
            for (mode in com.pixelody.app.core.playback.FlowShuffleMode.entries) {
                tapFlow(mode.displayName)
                repeat(30) { if(settings.loadShuffleMode() != mode.name) Thread.sleep(100) }
                assertEquals(mode.name, settings.loadShuffleMode())
                instrumentation.runOnMainSync {
                    assertEquals(tracks[0].id, active.currentMediaItem?.mediaId)
                    assertFalse(active.playWhenReady)
                    assertTrue("Mode selection keeps position", kotlin.math.abs(active.currentPosition - 37000) < 1500)
                }
            }
            tapFlow("Flow Shuffle")
            tapFlow("Match by key & tempo")
            click(findFlow(By.desc("8A, A minor")))
            assertTrue(findFlow(By.desc("8A, A minor")).visibleBounds.width() >= 48 * context.resources.displayMetrics.density - 1)
            var selectedKey = findFlow(By.desc("8A, A minor"))
            while (!selectedKey.isClickable && selectedKey.parent != null) selectedKey = selectedKey.parent
            assertTrue("The tapped wheel key is checked", selectedKey.isChecked)
            assertTrue(device.takeScreenshot(File(context.getExternalFilesDir(null), "flow-wheel.png")))
            tapFlow("Match this song · 8A · 120 BPM")
            findFlow(By.text("2 songs in your selected source"))
            closeFlow(); assertCurrent(tracks[0].id)
            tapFlow("Queue"); tapFlow("Flow · Smart · 8A · 120 BPM"); findFlow(By.text("Flow & shuffle")); closeFlow(); tapFlow("Done")
            open("search"); findFlow(By.text("Flow · Smart · 8A · 120 BPM"))
            findFlow(By.text(tracks[1].title))
            assertFalse(device.hasObject(By.text(tracks[2].title)))
            assertFalse(device.hasObject(By.text(tracks[3].title)))
            open("library"); tapFlow("Flow · Smart · 8A · 120 BPM")
            findFlow(By.text("Flow & shuffle")); findFlow(By.text("2 songs in your selected source"))
            var beforeCount = 0
            instrumentation.runOnMainSync { beforeCount = active.mediaItemCount }
            tapFlow("Add to queue")
            Thread.sleep(600)
            instrumentation.runOnMainSync {
                assertEquals(beforeCount + 2, active.mediaItemCount)
                assertEquals(setOf(tracks[0].id, tracks[1].id), (beforeCount until active.mediaItemCount)
                    .map { active.getMediaItemAt(it).mediaId }.toSet())
                assertEquals(tracks[0].id, active.currentMediaItem?.mediaId)
                assertFalse(active.playWhenReady)
            }
            var beforeRemoval = emptyList<String>()
            instrumentation.runOnMainSync { beforeRemoval = (0 until active.mediaItemCount)
                .map { active.getMediaItemAt(it).mediaId } }
            tapFlow("Flow · Smart · 8A · 120 BPM")
            tapFlow("Pure Random")
            Thread.sleep(600)
            instrumentation.runOnMainSync {
                assertEquals(beforeRemoval.size, active.mediaItemCount)
                assertEquals(beforeRemoval.sorted(), (0 until active.mediaItemCount)
                    .map { active.getMediaItemAt(it).mediaId }.sorted())
                assertEquals(tracks[0].id, active.currentMediaItem?.mediaId)
                assertFalse(active.playWhenReady)
            }
            tapFlow("Flow Shuffle"); closeFlow()
            instrumentation.runOnMainSync {
                val lastRepeated = (1 until active.mediaItemCount).last { active.getMediaItemAt(it).mediaId == tracks[1].id }
                active.moveMediaItem(lastRepeated, 1)
                beforeRemoval = (0 until active.mediaItemCount).map { active.getMediaItemAt(it).mediaId }
            }
            Thread.sleep(600)
            open("queue")
            val removalMediaIndex = beforeRemoval.indexOfLast { it == tracks[1].id }
            click(findFlow(By.desc("Remove ${tracks[1].title} from queue, position $removalMediaIndex")))
            instrumentation.runOnMainSync {
                assertEquals(beforeRemoval.filterIndexed { index, _ -> index != removalMediaIndex }, (0 until active.mediaItemCount)
                    .map { active.getMediaItemAt(it).mediaId })
            }
            tapFlow("UNDO")
            instrumentation.runOnMainSync {
                assertEquals(beforeRemoval, (0 until active.mediaItemCount).map { active.getMediaItemAt(it).mediaId })
                assertEquals(tracks[0].id, active.currentMediaItem?.mediaId)
                assertFalse(active.playWhenReady)
            }
            tapFlow("Done")
            tapFlow("Flow · Smart · 8A · 120 BPM"); tapFlow("Save playlist")
            findFlow(By.text("New playlist")); findFlow(By.text("2 tracks"))
            findFlow(By.clazz("android.widget.EditText")).text = "Flow matching QA fixture"
            device.waitForIdle(); tapFlow("Create playlist")
            assertEquals(setOf(tracks[0].id, tracks[1].id), PlaylistStore(context).load()
                .single { it.name == "Flow matching QA fixture" }.trackIds.toSet())
            open("home"); tapFlow("Flow & shuffle")
            tapFlow("Clear matching filters")
            closeFlow(); open("search"); findFlow(By.text("Flow · Smart"))
            findFlow(By.clazz("android.widget.EditText")).text = tracks[3].title
            device.waitForIdle(); findFlow(By.text(tracks[3].title))
            instrumentation.runOnMainSync {
                assertEquals(tracks[0].id, active.currentMediaItem?.mediaId)
                assertFalse(active.playWhenReady)
            }
            tapFlow("Flow · Smart"); tapFlow("Match by key & tempo")
            tapFlow("Match this song · 8A · 120 BPM"); tapFlow("Play matches")
            Thread.sleep(1000)
            instrumentation.runOnMainSync {
                assertEquals(2, active.mediaItemCount)
                assertEquals(setOf(tracks[0].id, tracks[1].id), (0 until active.mediaItemCount)
                    .map { active.getMediaItemAt(it).mediaId }.toSet())
            }
        } catch (failure: Throwable) {
            device.takeScreenshot(File(context.getExternalFilesDir(null), "flow-failure.png"))
            device.dumpWindowHierarchy(File(context.getExternalFilesDir(null), "flow-failure.xml"))
            throw failure
        } finally {
            PlaylistStore(context).load().filter { it.name == "Flow matching QA fixture" }
                .forEach { PlaylistStore(context).deletePlaylist(it.id) }
            controller?.let { active -> instrumentation.runOnMainSync { active.release() } }
            finish()
            context.stopService(Intent(context, com.pixelody.app.core.playback.PixelodyPlaybackService::class.java))
            PixelodyDatabaseHelper(context).use { helper -> tracks.forEach { helper.writableDatabase.delete(
                PixelodyDatabaseHelper.TABLE_CACHED_TRACKS, "id = ?", arrayOf(it.id)) } }
            tracks.forEach { File(Uri.parse(it.streamUrl).path!!).delete() }
            val editor=prefs.edit().clear()
            original.forEach { (key,value) -> when(value) {
                is String -> editor.putString(key,value); is Boolean -> editor.putBoolean(key,value)
                is Int -> editor.putInt(key,value); is Long -> editor.putLong(key,value); is Float -> editor.putFloat(key,value)
            } }
            editor.commit()
        }
    }

    @Test fun homeModeSourcesAndPlayerGesturesUseRealState() {
        assumeTrue("Isolated app only", context.packageName.endsWith(".studioqa"))
        val prefs = context.getSharedPreferences("pixelody_mobile_settings", 0)
        val original = prefs.all.toMap()
        val eq = MobileEqualizerStore(context)
        val tracks = (1..3).map(::track)
        try {
            finish()
            settings.saveTheme(PixelodyMobileTheme.Studio); settings.resetAppearance()
            settings.saveExperienceMode(AppExperienceMode.Essential)
            settings.savePlayerViewMode("Scope")
            settings.saveShuffleMode("Off"); settings.saveShuffleEnabled(false)
            eq.saveGlobalProfile(EqualizerProfile()); eq.saveTrackProfiles(emptyMap()); eq.saveUseMasteringRack(true)
            eq.saveSpatialSettings(SpatialChamberSettings(isEnabled = true))
            runBlocking { PixelodyPersistenceRepository(context).cacheScannedTracks(tracks) }
            open("home")
            find(By.text("Quick access")); find(By.text("Liked songs")); find(By.text("Add music")); find(By.text("Desktop"))
            val mode = find(By.desc("Switch listening mode, currently Essential"))
            assertTrue(mode.visibleBounds.height() >= 48 * context.resources.displayMetrics.density - 1)
            click(mode)
            repeat(50) { if (settings.loadExperienceMode() != AppExperienceMode.Studio) Thread.sleep(100) }
            assertEquals(AppExperienceMode.Studio, settings.loadExperienceMode())
            click(find(By.desc("Switch listening mode, currently Studio")))
            assertEquals(AppExperienceMode.Essential, settings.loadExperienceMode())
            tap("Desktop"); find(By.text("Connect your desktop"))
            open("library"); tap("Access fixture 1"); assertCurrent(tracks[0].id)
            open("player")
            click(find(By.desc("Switch listening mode, currently Essential")))
            assertEquals(AppExperienceMode.Studio, settings.loadExperienceMode())
            assertFalse(device.hasObject(By.text("Sound tools")))
            assertFalse(device.hasObject(By.text("SCOPE")))
            assertFalse(device.hasObject(By.text("Speaker")))
            assertEquals("Scope", settings.loadPlayerViewMode()) // Saved prototype preference is preserved.
            device.wait(Until.findObject(By.desc("Pause")), 5000)?.click(); device.waitForIdle()
            click(find(By.desc("Open advanced equalizer")))
            find(By.text("Mastering is active. Editing EQ switches to the equalizer."))
            tap("Turn off saved room effect")
            assertFalse(eq.loadSpatialSettings().isEnabled)
            tap("Warm"); assertEquals(EqualizerPreset.Warm, eq.loadGlobalProfile().preset)
            assertFalse("Editing EQ hands off from the actual mastering engine", eq.loadUseMasteringRack())
            tap("This song"); tap("Bass")
            assertEquals(EqualizerPreset.Bass, eq.loadTrackProfiles()[tracks[0].id]?.preset)
            assertEquals(EqualizerPreset.Warm, eq.loadGlobalProfile().preset)
            assertTrue(device.takeScreenshot(File(context.getExternalFilesDir(null), "studio-access-eq.png")))
            val curve=find(By.desc("Equalizer frequency response curve")).visibleBounds
            device.swipe(curve.left+curve.width()/2, curve.centerY(), curve.left+curve.width()/2, curve.top+35, 30)
            device.waitForIdle()
            assertEquals(EqualizerPreset.Custom, eq.loadTrackProfiles()[tracks[0].id]?.preset)
            assertCurrent(tracks[0].id)
            tap("Use all-songs EQ"); assertNull(eq.loadTrackProfiles()[tracks[0].id]); tap("Done")
            fun swipeArtwork(left: Boolean, short: Boolean = false) {
                val bounds = find(By.desc("${tracks.first { it.id == settings.loadLastTrackId() }.title} artwork")).visibleBounds
                val distance = if(short) 30 else bounds.width()*3/5
                val start = if(left) bounds.centerX()+distance/2 else bounds.centerX()-distance/2
                val end = if(left) start-distance else start+distance
                device.swipe(start,bounds.centerY(),end,bounds.centerY(),30); device.waitForIdle()
            }
            swipeArtwork(true,short=true); assertCurrent(tracks[0].id)
            swipeArtwork(true); assertCurrent(tracks[1].id)
            swipeArtwork(false); assertCurrent(tracks[0].id)
            assertNotNull(device.findObject(By.desc("Play")))
            find(By.text("Access fixture 1")).let { title ->
                val b=title.visibleBounds
                device.swipe(device.displayWidth*4/5,b.centerY(),device.displayWidth/5,b.centerY(),30)
            }
            device.waitForIdle(); assertCurrent(tracks[1].id)
            // A down gesture retains the existing collapse behavior, and must not skip a song.
            val b=find(By.desc("${tracks[1].title} artwork")).visibleBounds
            device.swipe(b.centerX(),b.top+40,b.centerX(),b.bottom-20,30)
            device.waitForIdle(); assertCurrent(tracks[1].id)
        } finally {
            finish()
            context.stopService(Intent(context, com.pixelody.app.core.playback.PixelodyPlaybackService::class.java))
            PixelodyDatabaseHelper(context).use { helper -> tracks.forEach { helper.writableDatabase.delete(
                PixelodyDatabaseHelper.TABLE_CACHED_TRACKS, "id = ?", arrayOf(it.id)) } }
            tracks.forEach { File(Uri.parse(it.streamUrl).path!!).delete() }
            val editor=prefs.edit().clear()
            original.forEach { (key,value) -> when(value) {
                is String -> editor.putString(key,value); is Boolean -> editor.putBoolean(key,value)
                is Int -> editor.putInt(key,value); is Long -> editor.putLong(key,value); is Float -> editor.putFloat(key,value)
            } }
            editor.commit()
        }
    }
}
