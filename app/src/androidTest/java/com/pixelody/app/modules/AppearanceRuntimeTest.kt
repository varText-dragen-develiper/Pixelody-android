package com.pixelody.app.modules

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.ui.theme.*
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class AppearanceRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val store get() = MobileSettingsStore(context)
    @Before fun setup() {
        assumeTrue("Dedicated fixture only", context.packageName.endsWith(".studioqa"))
        store.saveExperienceMode(AppExperienceMode.Studio)
        store.saveTheme(PixelodyMobileTheme.Studio)
        store.resetAppearance()
        device.wakeUp()
    }
    private fun open(route: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route"))
            .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        device.waitForIdle(); Thread.sleep(1000)
    }
    private fun find(text: String): UiObject2 {
        for (up in listOf(true, false)) repeat(12) {
            device.findObject(By.text(text))?.let { return it }
            assertEquals(context.packageName, device.currentPackageName)
            val x = device.displayWidth / 2
            val low = device.displayHeight * 3 / 5
            val high = device.displayHeight * 9 / 20
            device.swipe(x, if (up) low else high, x, if (up) high else low, 100)
            device.waitForIdle(); Thread.sleep(500)
        }
        if (device.currentPackageName == context.packageName) capture("missing-${text.hashCode()}")
        error("Missing $text")
    }
    private fun tap(text: String) {
        repeat(3) { attempt ->
            try {
                val bounds = find(text).visibleBounds
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
    private fun capture(name: String): File {
        assertEquals(context.packageName, device.currentPackageName)
        val file = File(File(context.getExternalFilesDir(null), "appearance").apply { mkdirs() }, "$name.png")
        assertTrue(device.takeScreenshot(file)); return file
    }
    private fun assertPainted(file: File, color: Int) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        var matches = 0
        for (y in 0 until bitmap.height step 2) for (x in 0 until bitmap.width step 2) {
            if (bitmap.getPixel(x, y) == color) matches++
        }
        bitmap.recycle()
        assertTrue("Color ${Integer.toHexString(color)} painted in UI", matches > 30)
    }
    @Test fun styleAppliesLiveSurvivesRestartAndResetsIndependently() {
        device.setOrientationNatural()
        try {
            open("profile")
            tap("Customize style")
            tap("Cobalt Current")
            assertEquals(StylePalette.Cobalt, store.loadAppearance().palette)
            assertPainted(capture("cobalt-live"), 0xFF64A8FF.toInt())
            tap("130%")
            tap("Serif")
            assertEquals(130, store.loadAppearance().textPercent)
            assertEquals(StyleTypeface.Serif, store.loadAppearance().typeface)
            find("High contrast")
            device.findObject(By.desc("High contrast")).click()
            device.waitForIdle(); Thread.sleep(500)
            assertTrue(store.loadAppearance().highContrast)
            assertPainted(capture("text-130-serif-high-contrast"), 0xFF9CA7BD.toInt())
            open("profile")
            find("Cobalt Current · 130% text · Serif")
            tap("Customize style")
            tap("Edit custom colors")
            find("Apply colors")
            tap("Cancel")
            assertEquals(StylePalette.Cobalt, store.loadAppearance().palette)
            open("home")
            assertPainted(capture("home-cobalt"), 0xFF64A8FF.toInt())
            open("profile")
            tap("Customize style")
            tap("Reset style to theme defaults")
            assertEquals(AppearanceSettings(), store.loadAppearance())
            assertEquals(PixelodyMobileTheme.Studio, store.loadTheme())
            open("home")
            assertPainted(capture("home-studio-default"), 0xFF91A7FF.toInt())
        } finally { device.unfreezeRotation() }
    }
    private fun setHex(index: Int, value: String) {
        val fields = device.findObjects(By.clazz("android.widget.EditText")).sortedBy { it.visibleBounds.top }
        assertEquals("Three editable color fields", 3, fields.size)
        fields[index].click()
        device.waitForIdle(); Thread.sleep(500)
        val focused = device.wait(Until.findObject(By.clazz("android.widget.EditText").focused(true)), 2000)
            ?: error("Color field did not receive focus")
        focused.text = value
        device.waitForIdle(); Thread.sleep(500)
        if (device.hasObject(By.pkg("com.samsung.android.honeyboard"))) device.pressBack()
        device.waitForIdle(); Thread.sleep(500)
        assertTrue("Editable field received $value", device.hasObject(By.textContains(value)))
    }
    @Test fun customMixerValidatesAndAppliesReadableColors() {
        device.setOrientationNatural()
        try {
            open("profile")
            tap("Customize style")
            find("Close style controls")
            tap("Edit custom colors")
            setHex(0, "FFAA66")
            setHex(1, "66DDCC")
            setHex(2, "FFFFFF")
            device.waitForIdle()
            find("Choose a darker base for readable text")
            assertEquals(AppearanceSettings(), store.loadAppearance())
            setHex(2, "101219")
            tap("Apply colors")
            val saved = store.loadAppearance()
            assertEquals(StylePalette.Custom, saved.palette)
            assertEquals("FFAA66", saved.primary)
            assertEquals("66DDCC", saved.secondary)
            assertEquals("101219", saved.base)
            open("home")
            assertPainted(capture("home-custom"), 0xFFFFAA66.toInt())
        } finally { store.resetAppearance(); device.unfreezeRotation() }
    }
    @Test fun largeTypeStyleControlsRemainReachableInLandscape() {
        store.saveAppearance(AppearanceSettings(textPercent = 150, highContrast = true))
        device.setOrientationLeft()
        try {
            open("profile")
            tap("Customize style")
            tap("Edit custom colors")
            find("Apply colors")
            capture("landscape-custom-dialog-150")
            tap("Cancel")
            tap("Reset style to theme defaults")
            assertEquals(AppearanceSettings(), store.loadAppearance())
        } finally { device.unfreezeRotation() }
    }
}
