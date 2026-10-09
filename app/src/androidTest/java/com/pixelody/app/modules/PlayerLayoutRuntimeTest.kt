package com.pixelody.app.modules

import android.content.Intent
import android.net.Uri
import android.graphics.Rect
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.pixelody.app.data.model.*
import com.pixelody.app.data.storage.*
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Resize only the emulator, never the owner's display or preferences. */
class PlayerLayoutRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private fun settle() { device.waitForIdle(); Thread.sleep(700) }
    private fun find(selector: BySelector): UiObject2 = device.wait(Until.findObject(selector), 10000)
        ?: error("Missing $selector")
    private fun openPlayer() {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://player"))
            .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        settle(); find(By.desc("Close player"))
    }
    @Test fun progressTransportAndToneRemainSeparateAcrossWindowSizes() {
        assumeTrue(context.packageName.endsWith(".studioqa") &&
            (Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish"))
        val settings = MobileSettingsStore(context)
        settings.saveTheme(PixelodyMobileTheme.Studio)
        settings.saveExperienceMode(AppExperienceMode.Essential)
        val track = Track(id = "player-layout-fixture", title = "Player layout fixture",
            artist = "A long artist name to exercise wrapping", album = "Layout QA",
            durationSeconds = 180, streamUrl = "file:///nonexistent-layout-fixture.wav")
        runBlocking { PixelodyPersistenceRepository(context).cacheScannedTracks(listOf(track)) }
        var controller: androidx.media3.session.MediaController? = null
        try {
            openPlayer()
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
                active.setMediaItem(androidx.media3.common.MediaItem.Builder().setMediaId(track.id)
                    .setUri(track.streamUrl).build(), 30000)
                active.pause()
            }
            settle()
            val cases = listOf(
                Triple("phone", "1080x2400", "420"), Triple("compact", "720x1280", "320"),
                Triple("narrow", "640x1136", "320"), Triple("tablet", "1280x1920", "240"),
                Triple("short", "744x960", "320"), Triple("landscape", "1280x720", "240"),
                Triple("large-text", "1080x2400", "420")
            )
            cases.forEach { (name, size, density) ->
                device.executeShellCommand("wm size $size")
                device.executeShellCommand("wm density $density")
                device.executeShellCommand("settings put system font_scale ${if (name == "large-text") "1.6" else "1.0"}")
                Thread.sleep(1200); openPlayer()
                val scrub = find(By.desc("Precision scrubber")).visibleBounds
                val play = find(By.desc("Play")).visibleBounds
                assertFalse("$name progress and Play do not overlap", Rect.intersects(scrub, play))
                assertTrue("$name progress precedes Play", scrub.bottom <= play.top)
                val content = device.findObjects(By.scrollable(true)).map { it.visibleBounds }
                    .filter { it.height() > 40 && it.bottom <= scrub.top }.maxByOrNull { it.height() }
                if (content != null) {
                    // Use the content gutter: slow gestures on artwork intentionally
                    // open track actions, which is a different user journey.
                    device.swipe(content.right - 8, content.bottom - 16, content.right - 8, content.top + 16, 32)
                    settle()
                    assertEquals("$name progress does not move with artwork", scrub,
                        find(By.desc("Precision scrubber")).visibleBounds)
                }
                // In small windows / enlarged text the dock scrolls to expose each
                // control instead of painting it across a neighbour or offscreen.
                listOf("Bass", "Mids", "Treble").forEach { tone ->
                    val minimum = (48 * context.resources.displayMetrics.density).toInt() - 1
                    repeat(6) {
                        if ((device.findObject(By.descStartsWith("$tone,"))?.visibleBounds?.height() ?: 0) < minimum) {
                            device.swipe(device.displayWidth / 2, device.displayHeight * 9 / 10,
                                device.displayWidth / 2, device.displayHeight * 6 / 10, 80)
                            settle()
                        }
                    }
                    val target = find(By.descStartsWith("$tone,")).visibleBounds
                    assertTrue("$name $tone has a full touch target", target.height() >= minimum && target.bottom <= device.displayHeight)
                    device.findObject(By.desc("Play"))?.let {
                        assertFalse("$name $tone and Play separate", Rect.intersects(target, it.visibleBounds))
                    }
                }
                val out = File(context.getExternalFilesDir(null), "player-layout").apply { mkdirs() }
                assertTrue(device.takeScreenshot(File(out, "$name.png")))
                find(By.text("History")).click(); settle(); find(By.text("Listening history"))
                device.findObjects(By.text("Controls")).last().click(); settle(); find(By.text("Playback controls"))
                device.pressBack(); settle()
                assertNotNull(find(By.desc("Close player")))
                find(By.desc("Open advanced equalizer")).click(); settle()
                find(By.text("Advanced equalizer")); device.pressBack(); settle()
                instrumentation.runOnMainSync { assertFalse(active.playWhenReady); assertEquals(track.id, active.currentMediaItem?.mediaId) }
            }
        } catch (failure: Throwable) {
            val out = File(context.getExternalFilesDir(null), "player-layout").apply { mkdirs() }
            device.takeScreenshot(File(out, "failed.png"))
            device.dumpWindowHierarchy(File(out, "failed.xml"))
            throw failure
        } finally {
            device.executeShellCommand("wm size reset"); device.executeShellCommand("wm density reset")
            device.executeShellCommand("settings put system font_scale 1.0")
            instrumentation.runOnMainSync { controller?.release() }
            context.stopService(Intent(context, com.pixelody.app.core.playback.PixelodyPlaybackService::class.java))
            PixelodyDatabaseHelper(context).use {
                it.writableDatabase.delete(PixelodyDatabaseHelper.TABLE_CACHED_TRACKS, "id = ?", arrayOf(track.id))
            }
        }
    }
}
