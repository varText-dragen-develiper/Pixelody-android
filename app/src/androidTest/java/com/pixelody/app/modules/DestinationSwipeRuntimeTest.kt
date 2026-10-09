package com.pixelody.app.modules

import android.content.Intent
import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.data.model.Crate
import com.pixelody.app.data.model.CrateBook
import com.pixelody.app.data.model.CrateCodec
import com.pixelody.app.data.storage.CrateStore
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class DestinationSwipeRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun settle() { device.waitForIdle(); Thread.sleep(350) }
    private fun open(route: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://$route"))
            .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        settle()
    }
    private fun destinationSelected(name: String): Boolean {
        val label = device.findObjects(By.text(name)).maxByOrNull { it.visibleBounds.bottom }
            ?: return false
        var node = label
        var selected = false
        repeat(4) { selected = selected || node.isSelected; node = node.parent ?: node }
        return selected
    }
    private fun assertDestination(name: String) {
        settle()
        assertTrue("Expected selected destination $name", destinationSelected(name))
    }
    private fun swipe(left: Boolean, y: Int = device.displayHeight / 3) {
        assertEquals(context.packageName, device.currentPackageName)
        val start = if (left) device.displayWidth * 4 / 5 else device.displayWidth / 5
        val end = if (left) device.displayWidth / 5 else device.displayWidth * 4 / 5
        device.swipe(start, y, end, y, 40)
        settle()
    }

    @Test fun mainScreensRespectChildScrollingAndNavigationLayers() {
        assumeTrue("Use only the isolated test app", context.packageName.endsWith(".studioqa"))
        val settings = MobileSettingsStore(context)
        val originalMode = settings.loadExperienceMode()
        val originalTheme = settings.loadTheme()
        val cratePrefs = context.getSharedPreferences("pixelody_crates", Context.MODE_PRIVATE)
        val originalCrates = cratePrefs.getString(CrateStore.KEY_BOOK, null)
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .toList().forEach { it.finishAndRemoveTask() }
            }
            settings.saveExperienceMode(AppExperienceMode.Essential)
            settings.saveTheme(PixelodyMobileTheme.Studio)
            assertTrue(cratePrefs.edit().putString(CrateStore.KEY_BOOK, CrateCodec.encodeBook(
                CrateBook((1..10).map { Crate("swipe-qa-$it", "Swipe playlist $it", List(9) { null }) })
            )).commit())
            device.wakeUp()
            open("home")
            // Use the page header, away from horizontal shelves and text entry.
            val headerY = device.displayHeight / 12
            swipe(true, headerY); assertDestination("Search")
            swipe(true, headerY); assertDestination("Library")
            swipe(true, headerY); assertDestination("Library") // No wrap.
            swipe(false, headerY); assertDestination("Search")
            swipe(false, headerY); assertDestination("Home")
            swipe(false, headerY); assertDestination("Home")

            // Vertical scrolling and a short drag never change destination.
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 5,
                device.displayWidth / 2, device.displayHeight / 3, 40)
            assertDestination("Home")
            device.swipe(device.displayWidth / 2, headerY, device.displayWidth / 2 - 30, headerY, 25)
            assertDestination("Home")

            open("home")
            var title = device.findObject(By.text("Your playlists"))
            repeat(12) {
                if (title == null) {
                    device.swipe(device.displayWidth / 30, device.displayHeight * 3 / 5,
                        device.displayWidth / 30, device.displayHeight * 9 / 20, 70)
                    settle()
                    title = device.findObject(By.text("Your playlists"))
                }
            }
            checkNotNull(title) { "Missing playlist shelf" }
            // Bring the cards' labels above the listening slot, rather than testing a clipped card.
            device.swipe(device.displayWidth / 30, device.displayHeight * 3 / 5,
                device.displayWidth / 30, device.displayHeight / 2, 70)
            settle()
            val titleBottom = device.findObject(By.text("Your playlists"))!!.visibleBounds.bottom
            val row = device.findObjects(By.scrollable(true)).filter {
                val r = it.visibleBounds
                r.width() > r.height() && r.top >= titleBottom && r.top < titleBottom + 100
            }.firstOrNull() ?: error("Missing horizontal playlist scroller")
            val bounds = row.visibleBounds
            val rowY = bounds.centerY()
            fun visibleLabels(): List<Pair<String, Int>> {
                if (android.os.Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation.clearCache()
                val output = java.io.ByteArrayOutputStream()
                device.dumpWindowHierarchy(output)
                val parser = android.util.Xml.newPullParser()
                parser.setInput(java.io.ByteArrayInputStream(output.toByteArray()), "UTF-8")
                val labels = mutableListOf<Pair<String, Int>>()
                while (parser.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "node") {
                        val text = parser.getAttributeValue(null, "text").orEmpty()
                        val coordinates = Regex("[0-9]+").findAll(parser.getAttributeValue(null, "bounds").orEmpty())
                            .map { it.value.toInt() }.toList()
                        if (text.isNotBlank() && coordinates.size == 4 && coordinates[1] >= bounds.top && coordinates[3] <= bounds.bottom) {
                            labels += text to coordinates[0]
                        }
                    }
                    parser.next()
                }
                return labels
            }
            val beforeLabels = visibleLabels()
            assertTrue("Need visible shelf labels", beforeLabels.isNotEmpty())
            device.swipe(device.displayWidth * 3 / 4, rowY, device.displayWidth / 2, rowY, 40)
            assertDestination("Home") // The shelf has more cards in this direction.
            assertNotEquals("The child shelf should have scrolled", beforeLabels, visibleLabels())
            // Keep dragging across the same shelf until its end hands off to Search.
            var handedOff = false
            repeat(24) {
                if (!handedOff) {
                    swipe(true, rowY)
                    handedOff = destinationSelected("Search")
                }
            }
            assertTrue("Shelf end must hand unused horizontal movement to the screen", handedOff)
            assertDestination("Search")

            // A pushed collection keeps its own context instead of replacing its parent tab.
            open("home")
            val liked = device.wait(Until.findObject(By.text("Liked songs")), 5000) ?: error("Missing liked songs")
            liked.click(); settle()
            assertTrue(device.hasObject(By.text("Back")))
            swipe(true)
            assertTrue("Collection detail must remain open", device.hasObject(By.text("Back")))
        } catch (failure: Throwable) {
            val dir = File(context.getExternalFilesDir(null), "destination-swipe").apply { mkdirs() }
            device.takeScreenshot(File(dir, "failure.png"))
            device.dumpWindowHierarchy(File(dir, "failure.xml"))
            throw failure
        } finally {
            val editor = cratePrefs.edit()
            if (originalCrates == null) editor.remove(CrateStore.KEY_BOOK)
            else editor.putString(CrateStore.KEY_BOOK, originalCrates)
            editor.commit()
            settings.saveExperienceMode(originalMode)
            settings.saveTheme(originalTheme)
        }
    }
}
