package com.pixelody.app.modules

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SurfacePolishRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private fun open(route: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route"))
            .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        SystemClock.sleep(500)
        device.waitForIdle()
        if (route == "profile") text("Profile") else if (route == "home") text("Your music")
    }
    private fun text(value: String): UiObject2 = device.wait(Until.findObject(By.text(value)), 10000)
        ?: error("Missing $value")
    private fun clickText(value: String) {
        var item = text(value)
        while (!item.isClickable && item.parent != null) item = item.parent
        item.click()
        SystemClock.sleep(350)
        device.waitForIdle()
    }
    private fun capture(name: String) {
        SystemClock.sleep(500)
        val dir = File(context.getExternalFilesDir(null), "surface-polish").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(dir, "$name.png")))
    }
    @Test fun primaryDestinationsQueueAndModeTargetsRemainUsable() {
        open("home")
        text("Your music")
        listOf("Essential", "Studio").forEach { value ->
            var target = text(value)
            val minimum = 48 * context.resources.displayMetrics.density
            while (target.visibleBounds.height() < minimum - 1 && target.parent != null) target = target.parent
            assertTrue("$value target height", target.visibleBounds.height() >= minimum - 1)
            assertTrue("$value target width", target.visibleBounds.width() >= minimum - 1)
            assertTrue("$value is a bounded control", target.visibleBounds.height() < minimum * 2)
        }
        capture("studio-home")
        clickText("Search")
        text("Search music")
        assertFalse("Advanced lens belongs in Studio mode", device.hasObject(By.textContains("CAMELOT DIGGING")))
        capture("studio-search")
        clickText("Library")
        text("Search your library")
        capture("studio-library")
        device.wait(Until.findObject(By.desc("Open queue")), 10000)?.click() ?: error("Missing queue")
        text("Queue")
        device.waitForIdle()
        assertFalse("Essential queue keeps analysis contextual", device.hasObject(By.textContains("Harmonic Clash")))
        val remove = device.findObject(By.descStartsWith("Remove ")) ?: error("Missing named remove action")
        assertTrue("Queue removal has a 48dp target", remove.visibleBounds.width() >= 48 * context.resources.displayMetrics.density - 1)
        capture("queue")
        device.pressBack() // Queue collapses to Player by the existing H8 contract.
        SystemClock.sleep(350)
        device.pressBack() // Player closes to the originating destination.
        text("Search your library")
        clickText("Home")
        clickText("Studio")
        text("Your music")
        capture("studio-depth-home")
        clickText("Essential")
    }
    @Test fun everyThemeKeepsHomeAndPlaybackControlsVisible() {
        val names = listOf("Cartridge Quest", "Obsidian Glass", "Lo-Fi Café", "Bulkhead Terminal", "Obsession", "Pixelody Studio")
        names.forEachIndexed { index, name ->
            open("profile")
            repeat(10) {
                if (!device.hasObject(By.text(name))) {
                    val scroll = device.findObject(By.scrollable(true)) ?: error("Missing profile scroll")
                    scroll.setGestureMargin(60)
                    scroll.scroll(Direction.DOWN, .45f)
                }
                device.waitForIdle()
            }
            clickText(name)
            device.pressBack()
            open("home")
            text("Your music")
            assertNotNull("Theme $name retains transport", device.wait(Until.findObject(By.desc("Play")), 10000))
            capture("theme-$index-home")
        }
    }
}
