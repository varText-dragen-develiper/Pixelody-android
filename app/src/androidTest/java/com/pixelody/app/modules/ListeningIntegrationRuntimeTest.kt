package com.pixelody.app.modules

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
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

class ListeningIntegrationRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun open(route: String, restart: Boolean = false) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route")).setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or if (restart) Intent.FLAG_ACTIVITY_CLEAR_TASK else Intent.FLAG_ACTIVITY_SINGLE_TOP))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        device.waitForIdle(); Thread.sleep(1000)
    }
    private fun find(text: String): UiObject2 {
        for (up in listOf(true, false)) repeat(18) {
            device.findObject(By.text(text))?.let { return it }
            assertEquals(context.packageName, device.currentPackageName)
            val x = device.displayWidth / 2; val low = device.displayHeight * 3 / 5; val high = device.displayHeight * 9 / 20
            device.swipe(x, if (up) low else high, x, if (up) high else low, 100)
            device.waitForIdle(); Thread.sleep(300)
        }
        error("Missing $text")
    }
    private fun tap(text: String) {
        val r = find(text).visibleBounds; device.click(r.centerX(), r.centerY())
        device.waitForIdle(); Thread.sleep(500)
    }
    private fun capture(name: String) {
        assertEquals(context.packageName, device.currentPackageName)
        val dir = File(context.getExternalFilesDir(null), "listening-integration").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(dir, "$name.png")))
    }
    private fun silentTrack(id: String, title: String): Track {
        val file = File(context.cacheDir, "$id.wav")
        val bytes = 8000 * 2 * 30
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(bytes + 36).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(bytes).array()
        file.outputStream().use { it.write(header); it.write(ByteArray(bytes)) }
        return Track(id = id, title = title, artist = "Integration fixture", album = "Listening QA",
            durationSeconds = 30, format = "wav", streamUrl = Uri.fromFile(file).toString())
    }
    @Test fun collectionsOpenAsPlaylistsAndOrdinaryPlayRecordsHistory() {
        assumeTrue("Dedicated fixture only", context.packageName.endsWith(".studioqa"))
        device.wakeUp()
        val settings = MobileSettingsStore(context)
        settings.saveTheme(PixelodyMobileTheme.Studio); settings.saveExperienceMode(AppExperienceMode.Essential)
        val store = CrateStore(context); val before = store.load()
        val one = silentTrack("integration-listen-one", "QA listen one")
        val two = silentTrack("integration-listen-two", "QA listen two")
        val crate = Crate("integration-collection", "Unified collection QA",
            listOf(CrateSlot.SingleTrack(one.id), null, CrateSlot.SingleTrack(two.id), CrateSlot.PlayerView("Classic")) + List(5) { null })
        try {
            runBlocking { PixelodyPersistenceRepository(context).cacheScannedTracks(listOf(one, two)) }
            store.save(CrateBook(listOf(crate)))
            open("home", restart = true)
            find("Your playlists"); find("Unified collection QA")
            assertFalse(device.hasObject(By.text("Your crates")))
            capture("home-shared-playlists")
            tap("Unified collection QA")
            find("QA listen one"); find("QA listen two")
            assertFalse(device.hasObject(By.text("Rearrange")))
            capture("collection-normal-track-list")
            tap("QA listen one")
            Thread.sleep(7000) // Let ordinary Media3 playback exceed the five-second real-listen threshold.
            tap("QA listen two")
            assertEquals(two.id, settings.loadLastTrackId())
            open("player")
            find("Listening history"); tap("Show recent plays")
            find("QA listen one · Integration fixture")
            capture("player-integrated-history")
            tap("QA listen one · Integration fixture")
            assertEquals(one.id, settings.loadLastTrackId())
            assertEquals(crate, store.load().crate(crate.id))
            device.pressBack(); device.waitForIdle()
            tap("Manage ›"); tap("Organize collection")
            find("Rearrange"); capture("organization-on-demand")
        } finally {
            context.stopService(Intent(context, com.pixelody.app.core.playback.PixelodyPlaybackService::class.java))
            store.save(before)
            PixelodyDatabaseHelper(context).use { helper ->
                helper.writableDatabase.delete(PixelodyDatabaseHelper.TABLE_CACHED_TRACKS,
                    "id IN (?, ?)", arrayOf(one.id, two.id))
            }
            File(Uri.parse(one.streamUrl).path!!).delete(); File(Uri.parse(two.streamUrl).path!!).delete()
        }
    }
}
