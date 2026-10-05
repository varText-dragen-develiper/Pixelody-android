package com.pixelody.app.modules

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.pixelody.app.data.model.*
import com.pixelody.app.data.storage.*
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** Never changes the owner's normal library or images. Gallery fixtures are removed in finally. */
class PersonalImagesRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    @Before fun setup() {
        assumeTrue(context.packageName.endsWith(".studioqa"))
        MobileSettingsStore(context).apply { saveTheme(PixelodyMobileTheme.Studio); saveExperienceMode(AppExperienceMode.Essential) }
        val playlists = PlaylistStore(context)
        val owned = playlists.load().filter { it.name == "Personal image QA" }.map { playlistCoverKey(it.id) }.toSet()
        playlists.save(playlists.load().filter { it.name != "Personal image QA" })
        val covers = CoverStore(context)
        val cleaned = CoverBook(covers.load().entries - owned - playlistCoverKey("qa-independent") - ScreenBackground.Home.key)
        covers.save(cleaned); covers.prune(cleaned)
        device.wakeUp()
    }
    private fun open(route: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route")).setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        device.waitForIdle(); Thread.sleep(1000)
    }
    private fun tap(text: String, click: Boolean = true) {
        for (up in listOf(true, false)) repeat(18) {
            val node = device.findObject(By.text(text))
            if (node != null) {
                val r = node.visibleBounds; if (click) device.click(r.centerX(), r.centerY())
                device.waitForIdle(); Thread.sleep(500); return
            }
            assertEquals(context.packageName, device.currentPackageName)
            val x = device.displayWidth / 2
            val low = device.displayHeight * 3 / 5; val high = device.displayHeight * 9 / 20
            device.swipe(x, if (up) low else high, x, if (up) high else low, 100)
            device.waitForIdle(); Thread.sleep(300)
        }
        error("Missing $text")
    }
    private fun capture(name: String): File {
        val dir = File(context.getExternalFilesDir(null), "personal-images").apply { mkdirs() }
        val file = File(dir, "$name.png"); assertTrue(device.takeScreenshot(file)); return file
    }
    private fun fixture(file: File, width: Int, height: Int) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFFFF00FF.toInt())
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
        bitmap.recycle()
    }
    @Test fun importsBoundPanoramasRespectOrientationAndRejectInvalidImages() {
        val dir = File(context.cacheDir, "image-import-test").apply { mkdirs() }
        val store = CoverStore(context.getSharedPreferences("image-import-test", 0), File(dir, "owned"))
        try {
            val wide = File(dir, "wide.jpg"); fixture(wide, 6000, 60)
            val saved = store.importImage(context, Uri.fromFile(wide))!!
            val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(Uri.parse(saved).path, size)
            assertEquals(768, size.outWidth); assertTrue(size.outHeight in 1..768)
            val rotated = File(dir, "rotated.jpg"); fixture(rotated, 200, 100)
            ExifInterface(rotated.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()); saveAttributes()
            }
            val rotatedUri = store.importImage(context, Uri.fromFile(rotated))!!
            BitmapFactory.decodeFile(Uri.parse(rotatedUri).path, size)
            assertEquals(100, size.outWidth); assertEquals(200, size.outHeight)
            val book = CoverBook().withImage(ScreenBackground.Home.key, saved).withImage(playlistCoverKey("qa"), rotatedUri)
            store.save(book)
            val invalid = File(dir, "invalid.jpg").apply { writeText("not an image") }
            assertNull(store.importImage(context, Uri.fromFile(invalid)))
            assertEquals(book, store.load())
            store.prune(book.withImage(ScreenBackground.Home.key, null))
            assertFalse(File(Uri.parse(saved).path!!).exists())
            assertTrue(File(Uri.parse(rotatedUri).path!!).exists())
        } finally { dir.deleteRecursively(); context.getSharedPreferences("image-import-test", 0).edit().clear().commit() }
    }
    @Test fun backgroundPickerPaintsPersistsCancelsAndResetsIndependently() {
        val store = CoverStore(context)
        val before = store.load()
        val playlists = PlaylistStore(context)
        val beforePlaylists = playlists.load()
        val playlist = playlists.createPlaylist("Personal image QA", listOf("px-track-moonlit-circuit"))
        val source = File(context.cacheDir, "Pixelody-background-QA.jpg"); fixture(source, 768, 768)
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, source.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pixelody-QA")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        })!!
        try {
            resolver.openOutputStream(uri)!!.use { output -> source.inputStream().use { it.copyTo(output) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            val playlistImage = store.importImage(context, Uri.fromFile(source))!!
            store.save(before.withImage(playlistCoverKey("qa-independent"), playlistImage).withImage(ScreenBackground.Home.key, null))
            open("profile"); tap("Customize backgrounds"); tap("Choose Home background")
            device.waitForIdle(); Thread.sleep(2000)
            capture("system-picker")
            // Android's system picker supplies the gallery image; no app storage permission needed.
            val photo = device.findObject(By.descContains("Photo taken"))
                ?: device.findObject(By.text(source.name))
                ?: device.findObject(By.res("com.google.android.providers.media.module", "icon_thumbnail"))
                ?: device.findObject(By.res("com.android.providers.media.module", "icon_thumbnail"))
                ?: error("System picker photo not found")
            photo.click()
            device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("Done|Add"))), 5000)?.click()
            device.waitForIdle(); Thread.sleep(700)
            assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
            repeat(30) { if (!store.load().hasCustomImage(ScreenBackground.Home.key)) Thread.sleep(200) }
            val selected = store.load().imageFor(ScreenBackground.Home.key, null)
            assertNotNull(selected)

            open("home")
            val shot = BitmapFactory.decodeFile(capture("home-custom").absolutePath)
            var colored = 0
            for (y in 0 until shot.height step 4) for (x in 0 until shot.width step 4) {
                val pixel = shot.getPixel(x, y)
                if (android.graphics.Color.red(pixel) in 42..50 && android.graphics.Color.blue(pixel) in 42..50 && android.graphics.Color.green(pixel) < 5) colored++
            }
            shot.recycle(); assertTrue("Dimmed chosen background painted", colored > 200)
            // Select a playlist picture through its menu, then verify the same key paints its Home card.
            tap("Personal image QA"); tap("Manage ›"); tap("Customize cover"); tap("Choose a picture")
            device.waitForIdle(); Thread.sleep(2000)
            device.findObject(By.descContains("Photo taken"))!!.click()
            device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("Done|Add"))), 5000)?.click()
            device.waitForIdle(); Thread.sleep(700)
            assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
            repeat(30) { if (!store.load().hasCustomImage(playlistCoverKey(playlist.id))) Thread.sleep(200) }
            assertTrue(store.load().hasCustomImage(playlistCoverKey(playlist.id)))
            resolver.delete(uri, null, null) // Private copies survive removing the gallery original.
            open("home")
            tap("Personal image QA", click = false)
            val coverShot = BitmapFactory.decodeFile(capture("playlist-cover-home").absolutePath)
            var coverPixels = 0
            for (y in 0 until coverShot.height step 4) for (x in 0 until coverShot.width step 4) {
                val pixel = coverShot.getPixel(x, y)
                if (android.graphics.Color.red(pixel) > 240 && android.graphics.Color.blue(pixel) > 240 && android.graphics.Color.green(pixel) < 10) coverPixels++
            }
            coverShot.recycle(); assertTrue("Chosen playlist picture painted on Home", coverPixels > 200)
            open("profile"); tap("Customize backgrounds"); tap("Change Home background")
            device.pressBack(); device.waitForIdle(); Thread.sleep(500)
            assertEquals(selected, store.load().imageFor(ScreenBackground.Home.key, null))
            tap("Reset Home background")
            assertFalse(store.load().hasCustomImage(ScreenBackground.Home.key))
            assertEquals(playlistImage, store.load().imageFor(playlistCoverKey("qa-independent"), null))
            capture("background-reset")
        } finally {
            runCatching { resolver.delete(uri, null, null) }; source.delete()
            playlists.save(beforePlaylists); store.save(before); store.prune(before)
        }
    }
}
