package com.pixelody.app.modules

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.pixelody.app.MainActivity
import com.pixelody.app.data.model.*
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.feature.baselayer.*
import com.pixelody.app.ui.theme.PixelodyTheme
import org.junit.Assert.*
import org.junit.Test

/** Fixtures run only in the isolated package; never change the owner's preferences/library. */
class NavigationTruthRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private fun text(value: String) = device.wait(Until.findObject(By.text(value)), 6000)
        ?: error("Missing visible text: $value")
    private fun tap(value: String) {
        val bounds = text(value).visibleBounds
        assertTrue("$value is on screen", bounds.width() > 0 && bounds.height() > 0)
        device.click(bounds.centerX(), bounds.centerY())
        device.waitForIdle(); Thread.sleep(400)
    }
    private fun page(name: String) = assertTrue("$name page is exposed", device.wait(Until.hasObject(By.desc("$name page")), 6000))
    private fun scrollTap(value: String) {
        repeat(16) {
            val node = device.findObjects(By.text(value)).firstOrNull { it.visibleBounds.height() > 0 }
            if (node != null) {
                val bounds = node.visibleBounds
                device.click(bounds.centerX(), bounds.centerY())
                device.waitForIdle(); Thread.sleep(400)
                return
            }
            device.swipe(device.displayWidth / 30, device.displayHeight * 7 / 10,
                device.displayWidth / 30, device.displayHeight * 9 / 20, 50)
            device.waitForIdle(); Thread.sleep(300)
        }
        device.dumpWindowHierarchy(java.io.File(context.getExternalFilesDir(null), "export-missing.xml"))
        error("Missing scrollable control: $value")
    }
    private fun isolated() {
        assertTrue(context.packageName.endsWith(".studioqa"))
        device.setOrientationNatural()
        MobileSettingsStore(context).resetAppearance()
        MobileSettingsStore(context).saveExperienceMode(AppExperienceMode.Studio)
    }

    @Test fun sourcesRecoverySettingsAndBackRemainCoherent() {
        isolated()
        val settings = MobileSettingsStore(context)
        settings.saveExperienceMode(AppExperienceMode.Studio)
        val phone = Track("local", "Local fixture", streamUrl = "file:///local.wav")
        val remote = Track("remote", "Remote fixture", streamUrl = "http://192.0.2.1/remote.wav")
        val both = Track("both", "Both sources fixture", streamUrl = "file:///both.wav")
        val data = mutableStateOf(BaseLayerData(tracks = listOf(phone, remote, both),
            phoneTrackIds = setOf("local", "both"), hostTrackIds = setOf("remote", "both"), hostReachable = false))
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            instrumentation.runOnMainSync {
                activity.setContent { PixelodyTheme {
                    PixelodyBaseShell(data = data.value, localTracks = listOf(phone, both),
                        onPlayTracks = {}, onTogglePlay = {}, onNext = {},
                        hostConnectionState = HostConnectionState.Offline,
                        onRetryHost = {},
                        onLensChange = {}, onCratesChange = {},
                        onSourceChange = { data.value = data.value.copy(source = it) })
                } }
            }
            page("Home")
            tap("Search"); page("Search")
            text("Offline")
            tap("Library"); page("Library")
            tap("Phone music")
            page("Library")
            assertEquals(BaseSource.Phone, data.value.source)
            assertFalse("Local browsing does not keep a desktop failure wall", device.hasObject(By.text("Host not responding")))
            tap("Home"); page("Home")
            device.wait(Until.findObject(By.desc("Settings and themes")), 6000)?.click() ?: error("Missing settings")
            text("Appearance")
            device.pressBack(); page("Home")
            tap("Search"); page("Search")
            // A short child-row swipe reveals J.A.M. without overscrolling into Library.
            val phoneBounds = text("Phone").visibleBounds
            device.swipe(device.displayWidth * 3 / 4, phoneBounds.centerY(),
                device.displayWidth * 2 / 5, phoneBounds.centerY(), 25)
            device.waitForIdle(); Thread.sleep(400)
            page("Search")
            tap("J.A.M.")
            text("Connect desktop")
            assertFalse(device.hasObject(By.text("Start Hosting")))
            assertFalse(device.hasObject(By.text("J.A.M. Mesh Room")))
            assertEquals("Unavailable J.A.M. does not select an empty source", BaseSource.Phone, data.value.source)
            tap("Back to music"); page("Search")
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test fun emptyLibraryAddsMusicAndUnsupportedHostingIsAbsent() {
        isolated()
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            instrumentation.runOnMainSync {
                activity.setContent { PixelodyTheme {
                    PixelodyBaseShell(data = BaseLayerData(), onPlayTracks = {}, onTogglePlay = {}, onNext = {}, onRetryHost = {},
                        onSourceChange = {}, onLensChange = {}, onCratesChange = {})
                } }
            }
            tap("Library"); page("Library")
            tap("Add music from this phone")
            text("On This Phone")
            text("Choose files")
            device.pressBack(); page("Library")
            instrumentation.runOnMainSync {
                activity.setContent { PixelodyTheme {
                    BasePlaybackHost(initialData = BaseLayerData(), incomingDeepLink = com.pixelody.app.ui.navigation.PixelodyTab.Create)
                } }
            }
            text("Add music")
            assertFalse("No enabled control with an empty host callback", device.hasObject(By.text("Start Phone Host")))
            assertFalse(device.hasObject(By.text("Host From This Device")))
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test fun playlistExportSavesARealDocumentAndCancelReturnsToMusic() {
        isolated()
        val track = Track("export-local", "Export fixture", artist = "Fixture artist", streamUrl = "file:///fixture.wav")
        val collection = BaseCollection("user-pl-export", BaseBrowseShape.Playlists.kindKey,
            "Export QA audit", listOf(track.id))
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        // DocumentsUI retains deleted names; select a unique filename in its actual editor.
        val filename = "/sdcard/Download/pixelody-export-qa-${android.os.SystemClock.uptimeMillis()}.m3u8"
        fun chooseFilename() {
            val field = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 6000)
                ?: error("Missing document filename")
            field.text = filename.substringAfterLast('/')
            device.waitForIdle()
        }
        fun shell(command: String) = instrumentation.uiAutomation.executeShellCommand(command).use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
        }
        try {
            shell("rm -f $filename") // Owned fixture only, in the isolated emulator's Downloads.
            instrumentation.runOnMainSync {
                activity.setContent { PixelodyTheme {
                    PixelodyBaseShell(data = BaseLayerData(tracks = listOf(track), collections = listOf(collection),
                        phoneTrackIds = setOf(track.id)), localTracks = listOf(track),
                        onPlayTracks = {}, onTogglePlay = {}, onNext = {}, onRetryHost = {},
                        onSourceChange = {}, onLensChange = {}, onCratesChange = {})
                } }
            }
            tap("Library"); tap("Playlists"); scrollTap(collection.name); scrollTap("Manage ›")
            tap("Export playlist (M3U8)")
            assertTrue("Android save dialog opens", device.wait(Until.hasObject(By.text("SAVE")), 6000))
            chooseFilename()
            device.pressBack(); device.waitForIdle(); Thread.sleep(400)
            assertFalse("Cancelling does not create a file", shell("ls $filename").contains(filename.substringAfterLast('/')))
            tap("Manage ›"); tap("Export playlist (M3U8)")
            chooseFilename()
            tap("SAVE")
            assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 6000))
            var saved = ""
            repeat(50) {
                if (!saved.startsWith("#EXTM3U")) {
                    saved = shell("cat $filename")
                    Thread.sleep(100) // Wait for DocumentsUI and the activity-result write to finish.
                }
            }
            assertTrue(saved.startsWith("#EXTM3U"))
            assertTrue(saved.contains("#EXTPLAYLIST:${collection.name}"))
            assertTrue(saved.contains("Fixture artist - Export fixture"))
            assertTrue(saved.contains(track.streamUrl))
            text("Manage ›")
        } finally {
            shell("rm -f $filename")
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
