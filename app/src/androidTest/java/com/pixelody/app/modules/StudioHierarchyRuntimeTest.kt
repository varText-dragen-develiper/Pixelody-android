package com.pixelody.app.modules

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class StudioHierarchyRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    @Before fun setup() {
        assumeTrue("Use only the dedicated Studio fixture app", context.packageName.endsWith(".studioqa"))
        MobileSettingsStore(context).saveExperienceMode(AppExperienceMode.Studio)
        MobileSettingsStore(context).saveTheme(PixelodyMobileTheme.Studio)
        device.wakeUp()
    }
    private fun open(route: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route"))
            .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        device.waitForIdle()
        Thread.sleep(1000) // Let the Activity transition and saved theme settle before visual capture.
    }
    private val pageScrollX: Int get() = if (device.displayWidth > device.displayHeight) device.displayWidth / 4 else device.displayWidth / 30
    private fun find(value: String): UiObject2 {
        var node = device.wait(Until.findObject(By.textContains(value)), 4000)
        repeat(12) {
            if (node == null) {
                assertEquals(context.packageName, device.currentPackageName)
                device.swipe(pageScrollX, device.displayHeight * 3 / 4, pageScrollX, device.displayHeight / 3, 60)
                device.waitForIdle()
                Thread.sleep(700) // Wait for Compose list flings before reading tap bounds.
                node = device.findObject(By.textContains(value))
            }
        }
        repeat(10) {
            if (node == null) {
                assertEquals(context.packageName, device.currentPackageName)
                device.swipe(pageScrollX, device.displayHeight / 3, pageScrollX, device.displayHeight * 3 / 4, 60)
                device.waitForIdle()
                Thread.sleep(700) // Wait for Compose list flings before reading tap bounds.
                node = device.findObject(By.textContains(value))
            }
        }
        if (node == null && device.currentPackageName == context.packageName) capture("missing-${value.hashCode()}")
        return node ?: error("Missing $value")
    }
    private fun tap(value: String) {
        repeat(3) { attempt ->
            try {
                val bounds = find(value).visibleBounds
                assertEquals(context.packageName, device.currentPackageName)
                // Tap the label inside its control; walking mutable Compose ancestors
                // can return stale nodes or a larger unrelated clickable container.
                device.click(bounds.centerX(), bounds.centerY())
                device.waitForIdle(); Thread.sleep(500)
                return
            } catch (stale: StaleObjectException) {
                if (attempt == 2) throw stale
                device.waitForIdle(); Thread.sleep(300)
            }
        }
    }
    private fun capture(name: String) {
        assertEquals(context.packageName, device.currentPackageName)
        val dir = File(context.getExternalFilesDir(null), "studio-hierarchy").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(dir, "$name.png")))
    }
    @Test fun homePrioritizesMusicAndKeepsTheListeningWorkbenchReachable() {
        open("home")
        find("Listening tools")
        assertFalse(device.hasObject(By.textContains("Choose Your Next Move")))
        capture("home-quiet")
        tap("Listening tools")
        capture("home-tools-after-open")
        assertFalse(device.hasObject(By.textContains("Choose Your Next Move")))
        capture("home-tools")
        open("home")
        tap("Browse filters")
        find("Lossless")
        tap("Lossless")
        tap("Browse filters")
        find("All Sources · Picks: Lossless only")
        capture("home-filter-summary")
    }
    @Test fun libraryKeepsFiltersVisibleAsASummaryWithoutBlockingTheTracks() {
        open("library")
        find("Library controls")
        assertFalse(device.hasObject(By.textContains("HARMONIC CAMELOT DIGGING LENS")))
        capture("library-quiet")
        tap("Library controls")
        find("Flow ·")
        capture("library-tools")
        open("library")
        tap("Library controls")
        tap("Lossless")
        tap("Library controls")
        find("Lossless only")
        assertFalse(device.hasObject(By.textContains("HARMONIC CAMELOT DIGGING LENS")))
        capture("library-filter-summary")
    }
    @Test fun playerKeepsBasicToneVisibleAndAdvancedEqualizerReachable() {
        open("player")
        assertFalse(device.hasObject(By.textContains("Sound tools")))
        find("Tone")
        assertTrue(device.hasObject(By.descStartsWith("Bass,")))
        assertTrue(device.hasObject(By.descStartsWith("Mids,")))
        assertTrue(device.hasObject(By.descStartsWith("Treble,")))
        capture("player-quiet")
        device.wait(Until.findObject(By.desc("Open advanced equalizer")), 5000)!!.click()
        find("Advanced equalizer")
        capture("player-equalizer")
        tap("Done")
    }

    @Test fun albumsOpenTheirTracksFromBothStudioViewsAndEssential() {
        listOf("deck", "shelf", "essential").forEach { view ->
            MobileSettingsStore(context).saveExperienceMode(
                if (view == "essential") AppExperienceMode.Essential else AppExperienceMode.Studio)
            open("home")
            if (view != "essential") {
                find("Albums To Start")
                if (view == "shelf") tap("3D DECK")
            }
            tap("Private Library Tests")
            find("2 tracks")
            find("Moonlit Circuit")
            find("Window Memory")
            assertFalse(device.hasObject(By.text("Copper Wire")))
            capture("album-$view-open")
            device.pressBack()
            device.waitForIdle()
            Thread.sleep(1000)
            if (view == "essential") {
                assertTrue("Album Back returns to Home", device.wait(Until.hasObject(By.desc("Home page")), 5000))
            } else find("Albums To Start")
        }
    }
    @Test fun everyThemeKeepsTheListeningLeadAndToolTargetsUsable() {
        PixelodyMobileTheme.values().forEach { theme ->
            MobileSettingsStore(context).saveTheme(theme)
            open("home")
            find("Your music")
            capture("theme-${theme.name}-home")
            val header = find("Listening tools")
            var target = header
            while (!target.isClickable && target.parent != null) target = target.parent
            assertTrue("A full-size tool target in ${theme.name}", target.visibleBounds.height() >= 48 * context.resources.displayMetrics.density - 1)
            assertFalse(device.hasObject(By.textContains("Choose Your Next Move")))
        }
    }
}
