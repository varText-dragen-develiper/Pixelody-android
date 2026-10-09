package com.pixelody.app.modules

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
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

class NavigationInertiaRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private fun settle() { device.waitForIdle(); Thread.sleep(800) }
    private fun open(route: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route"))
            .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000)); settle()
    }
    private fun find(selector: BySelector): UiObject2 = device.wait(Until.findObject(selector), 6000) ?: error("Missing $selector")
    private fun tap(text: String) { find(By.text(text)).click(); settle() }
    private fun finish() = instrumentation.runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toList().forEach { it.finishAndRemoveTask() }
    }
    private fun selected(name: String): Boolean {
        var node = device.findObjects(By.text(name)).maxByOrNull { it.visibleBounds.bottom } ?: return false
        repeat(4) { if (node.isSelected) return true; node = node.parent ?: node }
        return false
    }
    private fun event(action: Int, down: Long, x: Float, y: Float, time: Long = SystemClock.uptimeMillis()) {
        val input = MotionEvent.obtain(down, time, action, x, y, 0)
        input.source = InputDevice.SOURCE_TOUCHSCREEN
        try { assertTrue(instrumentation.uiAutomation.injectInputEvent(input, true)) } finally { input.recycle() }
    }
    private fun holdDrag(x: Float, y: Float, dx: Float, dy: Float, during: () -> Unit, cancel: Boolean = false) {
        val down = SystemClock.uptimeMillis()
        event(MotionEvent.ACTION_DOWN, down, x, y)
        try {
            (1..6).forEach { step -> Thread.sleep(80); event(MotionEvent.ACTION_MOVE, down, x+dx*step/6f, y+dy*step/6f) }
            Thread.sleep(180); during()
        } finally { event(if(cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP, down, x+dx, y+dy) }
        settle()
    }
    private fun screenshot(name: String) { assertTrue(device.takeScreenshot(File(context.getExternalFilesDir(null), "inertia-$name.png"))) }

    @Test fun pageTracksFingerCancelsAndKeepsSearchState() {
        assumeTrue(context.packageName.endsWith(".studioqa"))
        val settings = MobileSettingsStore(context); val prefs = context.getSharedPreferences("pixelody_mobile_settings", 0); val original = prefs.all.toMap()
        try {
            finish(); settings.saveTheme(PixelodyMobileTheme.Studio); settings.resetAppearance(); settings.saveExperienceMode(AppExperienceMode.Essential)
            open("home"); val left = find(By.text("Quick access")).visibleBounds.left
            holdDrag(device.displayWidth*.75f, device.displayHeight/12f, -device.displayWidth*.22f, 0f, during = {
                assertTrue("Home moves with the held finger", find(By.text("Quick access")).visibleBounds.left < left)
                assertTrue("Destination stays Home until release", selected("Home")); screenshot("page-held")
            }, cancel = true)
            assertTrue("Cancelled page drag returns Home", selected("Home"))
            assertEquals(left, find(By.text("Quick access")).visibleBounds.left)
            tap("Search"); find(By.clazz("android.widget.EditText")).text = "navigation retained query"; settle()
            tap("Library"); tap("Search")
            assertTrue("Search retains its query", device.wait(Until.hasObject(By.text("navigation retained query")), 6000))
            device.pressBack(); settle() // Dismiss keyboard if present.
        } catch (failure: Throwable) { screenshot("failure"); device.dumpWindowHierarchy(File(context.getExternalFilesDir(null), "inertia-failure.xml")); throw failure }
        finally { finish(); restore(prefs, original) }
    }

    @Test fun playerTracksPullSettlesAndRetainsPlayback() {
        assumeTrue(context.packageName.endsWith(".studioqa"))
        val settings = MobileSettingsStore(context); val prefs = context.getSharedPreferences("pixelody_mobile_settings", 0); val original = prefs.all.toMap()
        val id = "inertia-qa-fixture"
        val file = File(context.cacheDir, "$id.wav")
        val bytes = 8000*2*120
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).put("RIFF".toByteArray()).putInt(bytes+36)
            .put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(bytes).array()
        file.outputStream().use { it.write(header); it.write(ByteArray(bytes)) }
        val track = Track(id, "Inertia fixture", artist="Navigation QA", durationSeconds=120, format="wav", streamUrl=Uri.fromFile(file).toString())
        var controller: androidx.media3.session.MediaController? = null
        try {
            finish(); settings.saveTheme(PixelodyMobileTheme.Studio); settings.resetAppearance(); settings.saveExperienceMode(AppExperienceMode.Essential)
            runBlocking { PixelodyPersistenceRepository(context).cacheScannedTracks(listOf(track)) }
            open("home")
            lateinit var future: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>
            instrumentation.runOnMainSync { future=androidx.media3.session.MediaController.Builder(context,
                androidx.media3.session.SessionToken(context, android.content.ComponentName(context, com.pixelody.app.core.playback.PixelodyPlaybackService::class.java)))
                .setApplicationLooper(android.os.Looper.getMainLooper()).buildAsync() }
            controller=future.get(10,java.util.concurrent.TimeUnit.SECONDS); val active=controller!!
            instrumentation.runOnMainSync { active.setMediaItem(androidx.media3.common.MediaItem.Builder().setMediaId(id).setUri(track.streamUrl).build(), 37000); active.prepare(); active.pause() }
            settle()
            find(By.desc("Inertia fixture artwork"))
            val dock=device.findObjects(By.desc("Inertia fixture artwork")).maxBy { it.visibleBounds.bottom }.visibleBounds
            holdDrag(dock.centerX().toFloat(), dock.centerY().toFloat(), 0f, -device.displayHeight*.15f, during={
                screenshot("player-held")
                assertTrue("Player header follows the held pull near the bottom", find(By.desc("Close player")).visibleBounds.top > device.displayHeight * .7f)
            })
            assertTrue("Player settles fully open", find(By.desc("Close player")).visibleBounds.top < device.displayHeight / 4)
            screenshot("player-open")
            fun assertPlayback() { instrumentation.runOnMainSync { assertEquals(id,active.currentMediaItem?.mediaId); assertFalse(active.playWhenReady); assertTrue(kotlin.math.abs(active.currentPosition-37000)<1500) } }
            assertPlayback()
            val close=find(By.desc("Close player")).visibleBounds
            holdDrag(device.displayWidth*.25f,close.centerY().toFloat(),0f,30f,during={ screenshot("player-short-pull") })
            find(By.desc("Close player")); assertPlayback()
            // Hardware-like event timestamps keep this a real fast flick even when
            // the software emulator takes longer to render each injected event.
            val flickDistance = 40 * context.resources.displayMetrics.density
            val flickDown = SystemClock.uptimeMillis() - 100
            val flickX = device.displayWidth / 4f
            val flickY = close.centerY().toFloat()
            event(MotionEvent.ACTION_DOWN, flickDown, flickX, flickY, flickDown)
            try {
                (1..10).forEach { step ->
                    event(MotionEvent.ACTION_MOVE, flickDown, flickX, flickY + flickDistance * step / 10f, flickDown + step * 10)
                }
            } finally { event(MotionEvent.ACTION_UP, flickDown, flickX, flickY + flickDistance, flickDown + 100) }
            settle(); assertFalse("A short purposeful flick closes the player", device.hasObject(By.desc("Close player"))); assertPlayback()
            find(By.desc("Inertia fixture artwork")).click(); settle(); find(By.desc("Close player"))
            device.swipe(device.displayWidth/4,close.centerY(),device.displayWidth/4,close.centerY()+device.displayHeight/3,40);settle()
            assertFalse(device.hasObject(By.desc("Close player"))); assertPlayback()
            find(By.desc("Inertia fixture artwork")).click();settle();find(By.desc("Close player"))
            device.pressBack();settle();assertFalse(device.hasObject(By.desc("Close player")));assertPlayback()
            find(By.desc("Inertia fixture artwork")).click();settle();find(By.desc("Close player")).click(); settle();assertFalse(device.hasObject(By.desc("Close player")));assertPlayback()
        } catch (failure: Throwable) { screenshot("failure"); device.dumpWindowHierarchy(File(context.getExternalFilesDir(null), "inertia-failure.xml")); throw failure }
        finally {
            controller?.let { instrumentation.runOnMainSync { it.release() } }; finish()
            context.stopService(Intent(context, com.pixelody.app.core.playback.PixelodyPlaybackService::class.java))
            PixelodyDatabaseHelper(context).use { it.writableDatabase.delete(PixelodyDatabaseHelper.TABLE_CACHED_TRACKS,"id = ?", arrayOf(id)) }
            file.delete(); restore(prefs, original)
        }
    }
    private fun restore(prefs: android.content.SharedPreferences, original: Map<String, *>) {
        val editor=prefs.edit().clear();original.forEach { (key,value) -> when(value) {
            is String -> editor.putString(key,value); is Boolean -> editor.putBoolean(key,value);is Int -> editor.putInt(key,value)
            is Long -> editor.putLong(key,value);is Float -> editor.putFloat(key,value)
        } };editor.commit()
    }
}
