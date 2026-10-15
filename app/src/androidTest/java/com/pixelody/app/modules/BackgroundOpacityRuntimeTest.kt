package com.pixelody.app.modules

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.pixelody.app.data.model.*
import com.pixelody.app.data.storage.*
import com.pixelody.app.ui.theme.*
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** Isolated package only: never changes the owner's images, theme or typography. */
class BackgroundOpacityRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val covers get() = CoverStore(context)
    private val settings get() = MobileSettingsStore(context)
    private lateinit var before: CoverBook
    @Before fun seed() {
        assumeTrue(context.packageName.endsWith(".studioqa"))
        Configurator.getInstance().waitForIdleTimeout = 1000; device.wakeUp(); device.setOrientationNatural()
        settings.saveTheme(PixelodyMobileTheme.Studio)
        settings.saveExperienceMode(AppExperienceMode.Studio)
        settings.resetAppearance()
        val image = File(context.cacheDir, "image-style-fixture.jpg")
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFFBB2200.toInt())
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }; bitmap.recycle()
        before = CoverBook().withImage(ScreenBackground.Home.key, Uri.fromFile(image).toString())
            .withBackgroundOpacity(ScreenBackground.Home, 0.22f)
            .withImage(ScreenBackground.Search.key, Uri.fromFile(image).toString())
            .withImage(playlistCoverKey("p"), Uri.fromFile(image).toString()).withNote("playlist:p", "kept")
        covers.save(before)
    }
    private fun open(route: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route")).setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 15000))
        val ready = when (route) {
            "home" -> By.desc("Home page")
            "search" -> By.desc("Search page")
            "library" -> By.desc("Library page")
            else -> By.text("Settings & Themes")
        }
        assertTrue("Destination $route ready", device.wait(Until.hasObject(ready), 15000))
        device.waitForIdle(2000); Thread.sleep(1200)
    }
    private fun find(text: String): UiObject2 { android.util.Log.i("ImageStyleTest", "Looking for: " + text)
        for (up in listOf(true, false)) repeat(28) {
            device.findObject(By.text(text))?.let { target ->
                val bounds = target.visibleBounds
                val settingsControl = text.contains("background", ignoreCase = true) || text in listOf("Obsession", "Text and contrast", "Personalize in Studio")
                // Pinned Settings navigation and transport can occlude a lazy item's
                // accessibility bounds. Scroll the control into the open content band.
                if (!settingsControl || bounds.top >= device.displayHeight * 23 / 100 && bounds.bottom <= device.displayHeight * 79 / 100) return target
            }
            assertEquals(context.packageName, device.currentPackageName)
            val x = device.displayWidth / 2
            val low = device.displayHeight * 3 / 5; val high = device.displayHeight * 9 / 20
            device.swipe(x, if (up) low else high, x, if (up) high else low, 40)
            device.waitForIdle(1000)
            Thread.sleep(350)
        }
        capture("missing-${text.hashCode()}")
        error("Missing $text")
    }
    private fun tap(text: String) {
        find(text)
        // A lazy item can enter semantics before scroll inertia has settled.
        var bounds = device.findObject(By.text(text)).visibleBounds
        repeat(8) {
            Thread.sleep(200)
            val next = device.findObject(By.text(text))?.visibleBounds ?: return@repeat
            if (next == bounds) return@repeat
            bounds = next
        }
        android.util.Log.i("ImageStyleTest", "Tap $text at $bounds")
        device.click(bounds.centerX(), bounds.centerY()); device.waitForIdle(1000); Thread.sleep(250)
    }
    private fun editor() {
        open("profile"); expandBackgrounds()
        tap("Style Home background")
    }
    private fun expandBackgrounds() {
        tap("Customize backgrounds")
        val expanded = device.wait(Until.hasObject(By.text("Close background controls")), 5000)
        if (!expanded) capture("expand-failed")
        assertTrue("Background controls expanded", expanded)
    }
    private fun choose() {
        tap("Use suggested 75%")
        tap("Continue")
        tap("Use all suggestions")
        tap("Continue")
    }
    private fun capture(name: String): File {
        val file = File(File(context.getExternalFilesDir(null), "image-style").apply { mkdirs() }, "$name.png")
        assertTrue(device.takeScreenshot(file)); return file
    }
    private fun painted(name: String, hex: String) {
        val bitmap = BitmapFactory.decodeFile(capture(name).absolutePath)
        var count = 0
        val color = (0xFF000000L or hex.toLong(16)).toInt()
        for (y in 0 until bitmap.height step 2) for (x in 0 until bitmap.width step 2) if (bitmap.getPixel(x, y) == color) count++
        bitmap.recycle(); assertTrue("$name uses $hex, found $count pixels", count > 20)
    }
    @Test fun draftRequiresReviewAndCancelPreservesTheOriginalRecord() {
        try {
            editor()
            find("Continue")
            assertTrue("Continue button requires opacity review", device.hasObject(By.enabled(false).hasDescendant(By.text("Continue"))))
            choose()
            assertEquals(before, covers.load())
            capture("preview-unapplied")
            tap("Cancel")
            assertEquals(before, covers.load())
            open("home"); painted("cancel-home-original", "91A7FF")
        } finally { device.unfreezeRotation() }
    }
    @Test fun applyPersistsOnlyThisScreenAndAuthoredThemeSuspendsCustomLook() {
        try {
            editor(); choose(); tap("Apply to Home")
            val styled = covers.load()
            val image = styled.entry(ScreenBackground.Home.key)?.imageStyle ?: error("No saved style")
            assertEquals(before.entry(ScreenBackground.Search.key), styled.entry(ScreenBackground.Search.key))
            assertEquals(before.entry("playlist:p"), styled.entry("playlist:p"))
            assertEquals(0.75f, styled.backgroundOpacityFor(ScreenBackground.Home), 0f)
            open("home"); painted("custom-home", image.primary)
            open("search"); painted("search-independent", "91A7FF")
            open("profile"); tap("Obsession")
            assertEquals(PixelodyMobileTheme.Obsession, settings.loadTheme())
            tap("Text and contrast")
            assertFalse(device.hasObject(By.text("Edit custom colors")))
            tap("Personalize in Studio")
            assertEquals(PixelodyMobileTheme.Studio, settings.loadTheme())
            open("home"); painted("studio-return", image.primary)
            assertEquals(styled, covers.load())
            open("profile"); expandBackgrounds(); tap("Reset Home background")
            assertEquals(before.withoutBackground(ScreenBackground.Home), covers.load())
            open("home"); painted("reset-studio", "91A7FF")
        } finally { device.unfreezeRotation() }
    }
    @Test fun draftSurvivesRotationAndLargeTextKeepsApplyReachable() {
        val fontBefore = device.executeShellCommand("settings get system font_scale").trim()
        try {
            editor(); choose()
            device.setOrientationLeft(); device.waitForIdle(1000); Thread.sleep(700)
            val stepLabel = "3 of 3 · Preview & apply"
            val normalLabelHeight = find(stepLabel).visibleBounds.height()
            device.executeShellCommand("settings put system font_scale 2.0")
            val deadline = System.currentTimeMillis() + 15000
            while (System.currentTimeMillis() < deadline &&
                (context.resources.configuration.fontScale < 1.99f ||
                    (device.findObject(By.text(stepLabel))?.visibleBounds?.height() ?: 0) <= normalLabelHeight * 1.25f)) Thread.sleep(250)
            assertTrue("Android applied 200% text", context.resources.configuration.fontScale >= 1.99f)
            assertTrue("Rendered review text grew", find(stepLabel).visibleBounds.height() > normalLabelHeight * 1.25f)
            device.waitForIdle(1000)
            capture("landscape-large-text-preview")
            tap("Apply to Home")
            assertNotNull(covers.load().entry(ScreenBackground.Home.key)?.imageStyle)
        } finally {
            device.executeShellCommand("settings put system font_scale ${fontBefore.toFloatOrNull() ?: 1f}")
            device.unfreezeRotation()
        }
    }
    @Test fun pickedImageIsCopiedAndRemainsADraftUntilApply() {
        // The isolated Android 16 emulator has an English system photo picker.
        // A newest, owned MediaStore image gives its grid a deterministic target.
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "Pixelody-style-fixture.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        val picked = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Fixture URI")
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFFBB2200.toInt())
        context.contentResolver.openOutputStream(picked)!!.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }; bitmap.recycle()
        var sourceDeleted = false
        try {
            open("profile"); expandBackgrounds(); tap("Choose Library background")
            assertTrue("System photo grid ready", device.wait(Until.hasObject(By.descStartsWith("Photo taken on")), 15000))
            val photo = device.findObjects(By.descStartsWith("Photo taken on"))
                .minWith(compareBy<UiObject2> { it.visibleBounds.top }.thenBy { it.visibleBounds.left })
            photo.click()
            assertTrue("Copied image draft opens", device.wait(Until.hasObject(By.text("Library · Custom")), 15000))
            find("Library · Custom")
            choose()
            assertEquals(before, covers.load())
            tap("Apply to Library")
            val entry = covers.load().entry(ScreenBackground.Library.key) ?: error("Missing Library image")
            assertNotNull(entry.imageStyle)
            assertEquals("file", Uri.parse(entry.imageUri).scheme)
            assertTrue(File(Uri.parse(entry.imageUri).path!!).exists())
            val copied = BitmapFactory.decodeFile(Uri.parse(entry.imageUri).path!!)
            val pixel = copied.getPixel(copied.width / 2, copied.height / 2)
            copied.recycle()
            assertTrue("Imported the owned red fixture", android.graphics.Color.red(pixel) > 170 && android.graphics.Color.green(pixel) < 50 && android.graphics.Color.blue(pixel) < 20)
            assertEquals(before.entry(ScreenBackground.Home.key), covers.load().entry(ScreenBackground.Home.key))
            assertEquals(1, context.contentResolver.delete(picked, null, null))
            sourceDeleted = true
            open("profile"); expandBackgrounds(); tap("Style Library background")
            find("Library · Custom")
            tap("Cancel")
            assertEquals(entry, covers.load().entry(ScreenBackground.Library.key))
        } finally {
            if (!sourceDeleted) context.contentResolver.delete(picked, null, null)
            device.unfreezeRotation()
        }
    }
}
