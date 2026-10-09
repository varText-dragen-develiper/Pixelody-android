package com.pixelody.app.modules

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.pixelody.app.data.model.*
import com.pixelody.app.data.storage.*
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Isolated package only: never changes the owner's images or opacity settings. */
class BackgroundOpacityRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun open(route: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route")).setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        device.waitForIdle(); Thread.sleep(700)
    }
    private fun find(selector: BySelector): UiObject2 {
        for (up in listOf(true, false)) repeat(22) {
            device.findObject(selector)?.let { return it }
            assertEquals(context.packageName, device.currentPackageName)
            val x = device.displayWidth / 2
            val low = device.displayHeight * 3 / 5; val high = device.displayHeight * 9 / 20
            device.swipe(x, if (up) low else high, x, if (up) high else low, 100)
            device.waitForIdle(); Thread.sleep(200)
        }
        error("Missing $selector")
    }
    private fun changeOpacity(screen: ScreenBackground, high: Boolean) {
        val bounds = find(By.desc("${screen.label} background opacity")).visibleBounds
        device.swipe(bounds.centerX(), bounds.centerY(), if (high) bounds.right - 1 else bounds.left + 1, bounds.centerY(), 40)
        device.waitForIdle(); Thread.sleep(300)
    }
    private fun capture(name: String): Bitmap {
        val directory = File(context.getExternalFilesDir(null), "background-opacity").apply { mkdirs() }
        val file = File(directory, "$name.png")
        assertTrue(device.takeScreenshot(file))
        return BitmapFactory.decodeFile(file.absolutePath)
    }
    @Test fun slidersPersistIndependentlyAndImageContinuesThroughDock() {
        assumeTrue(context.packageName.endsWith(".studioqa"))
        val covers = CoverStore(context); val before = covers.load()
        val settings = MobileSettingsStore(context)
        val beforeTheme = settings.loadTheme(); val beforeMode = settings.loadExperienceMode()
        val image = File(context.cacheDir, "opacity-fixture.jpg")
        val bitmap = Bitmap.createBitmap(768, 768, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFFFF00FF.toInt())
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }; bitmap.recycle()
        try {
            settings.saveTheme(PixelodyMobileTheme.Studio); settings.saveExperienceMode(AppExperienceMode.Essential)
            covers.save(before.withImage(ScreenBackground.Home.key, Uri.fromFile(image).toString())
                .withBackgroundOpacity(ScreenBackground.Home, 0.18f))
            open("profile"); find(By.text("Customize backgrounds")).click(); device.waitForIdle()
            changeOpacity(ScreenBackground.Home, false)
            assertTrue(covers.load().backgroundOpacityFor(ScreenBackground.Home) < 0.05f)
            open("home"); val hidden = capture("home-zero")
            open("profile"); find(By.text("Customize backgrounds")).click(); device.waitForIdle()
            changeOpacity(ScreenBackground.Home, true)
            assertTrue(covers.load().backgroundOpacityFor(ScreenBackground.Home) > 0.95f)
            changeOpacity(ScreenBackground.Search, false)
            assertTrue(covers.load().backgroundOpacityFor(ScreenBackground.Search) < 0.05f)
            assertTrue(covers.load().backgroundOpacityFor(ScreenBackground.Home) > 0.95f)
            open("home"); val visible = capture("home-full")
            // The same original viewport image reaches the blank left margin of both bands.
            val x = 2
            val upperY = visible.height / 2
            val lowerY = visible.height * 90 / 100
            val upper = android.graphics.Color.red(visible.getPixel(x, upperY)) - android.graphics.Color.red(hidden.getPixel(x, upperY))
            val lower = android.graphics.Color.red(visible.getPixel(x, lowerY)) - android.graphics.Color.red(hidden.getPixel(x, lowerY))
            assertTrue("Image visible above dock: $upper", upper > 20)
            assertTrue("Image visible through dock: $lower", lower > 2)
            assertTrue("Dock shaded more strongly: upper=$upper lower=$lower", lower < upper)
            hidden.recycle(); visible.recycle()
            open("profile"); find(By.text("Customize backgrounds")).click(); device.waitForIdle()
            find(By.text("Reset Home background")).click(); device.waitForIdle()
            assertFalse(covers.load().hasCustomImage(ScreenBackground.Home.key))
            assertTrue(covers.load().backgroundOpacityFor(ScreenBackground.Home) > 0.95f)
            // Theme-only opacity is independently adjustable and survives rebuilding the activity.
            changeOpacity(ScreenBackground.Home, false)
            open("profile"); find(By.text("Customize backgrounds")).click(); device.waitForIdle()
            assertTrue(covers.load().backgroundOpacityFor(ScreenBackground.Home) < 0.05f)
            find(By.textContains("Applies to the selected theme"))
            capture("theme-controls") .recycle()
        } finally {
            covers.save(before); settings.saveTheme(beforeTheme); settings.saveExperienceMode(beforeMode)
            image.delete()
        }
    }
}
