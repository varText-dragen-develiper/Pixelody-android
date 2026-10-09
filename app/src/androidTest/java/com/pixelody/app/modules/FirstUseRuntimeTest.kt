package com.pixelody.app.modules

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class FirstUseRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun usable(node: UiObject2?): Boolean {
        if (node == null) return false
        val bounds = node.visibleBounds
        val navigationTop = device.findObjects(By.text("Home")).maxByOrNull { it.visibleBounds.bottom }
            ?.parent?.visibleBounds?.top?.takeIf { it > device.displayHeight / 2 } ?: device.displayHeight
        return bounds.width() > 0 && bounds.height() > 0 && bounds.top >= 0 && bounds.bottom < navigationTop
    }
    private fun text(value: String): UiObject2 {
        var result = device.wait(Until.findObject(By.text(value)), 5000)
        repeat(4) {
            if (!usable(result)) {
                assertEquals("Scroll only the test app", context.packageName, device.currentPackageName)
                device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 3, 30)
                device.waitForIdle()
                result = device.findObject(By.text(value))
            }
        }
        return result?.takeIf { usable(it) } ?: error("Missing visible $value above navigation")
    }
    private fun tap(value: String) {
        assertEquals("Keep the test app in the foreground", context.packageName, device.currentPackageName)
        // Compose merges click semantics into ancestors whose bounds can be
        // stale after a scroll. Tap the visible label, as a person would.
        val bounds = text(value).visibleBounds
        check(bounds.width() > 0 && bounds.height() > 0) { "Target is not visible: $value" }
        device.click(bounds.centerX(), bounds.centerY())
        device.waitForIdle()
    }
    private fun home() {
        assumeTrue("Run only on the dedicated empty-library build", context.packageName.endsWith(".firstuse"))
        device.wakeUp()
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        text("Start with your music")
    }
    private fun capture(name: String) {
        assertEquals("Capture only the test app", context.packageName, device.currentPackageName)
        val directory = File(context.getExternalFilesDir(null), "first-use").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(directory, "$name.png")))
    }
    @Test fun emptyLibraryHasARealNextStepAndPermissionDenialIsRecoverable() {
        home()
        assertFalse("Empty Home has no dead playback button", device.hasObject(By.text("Shuffle all")))
        capture("home")
        tap("Add music from this phone")
        text("On This Phone")
        text("Choose folder")
        text("Choose files")
        assertFalse("No unsupported host action ahead of setup", device.hasObject(By.text("Share this phone’s library")))
        capture("add-music")
        tap("Find music on this phone")
        val deny = device.wait(Until.findObject(By.res("com.android.permissioncontroller", "permission_deny_button")), 10000)
            ?: error("Music permission was not requested at the explicit scan action")
        device.click(deny.visibleBounds.centerX(), deny.visibleBounds.centerY())
        assertTrue("Permission dismissal completes before app recovery", device.wait(Until.gone(By.res("com.android.permissioncontroller", "permission_deny_button")), 10000))
        device.waitForIdle()
        text("Music access was not granted. You can still choose specific files or a folder below.")
        capture("permission-denied")
        tap("Choose files")
        assertTrue("A real system picker opens", device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")), 5000)
            || device.hasObject(By.pkg("com.android.documentsui")))
        device.pressBack()
        text("On This Phone")
        device.pressBack()
        text("Start with your music")
    }
    @Test fun deniedCameraAccessKeepsTheInviteFallbackAvailable() {
        home()
        tap("Connect a desktop")
        tap("Scan QR")
        val deny = device.wait(Until.findObject(By.res("com.android.permissioncontroller", "permission_deny_button")), 10000)
            ?: error("Camera permission was not requested at Scan QR")
        device.click(deny.visibleBounds.centerX(), deny.visibleBounds.centerY())
        assertTrue("Camera permission dismissal completes", device.wait(Until.gone(By.res("com.android.permissioncontroller", "permission_deny_button")), 10000))
        device.waitForIdle()
        text("Camera access was not granted. You can paste a desktop invite instead.")
        text("Paste Invite")
        capture("camera-denied")
    }
    @Test fun invalidDesktopInviteOffersRecoveryWithoutAddingDemoMusic() {
        home()
        tap("Connect a desktop")
        text("Connect your desktop")
        assertFalse("No fake Demo action", device.hasObject(By.text("Demo")))
        tap("Paste Invite")
        var field = device.findObject(By.clazz("android.widget.EditText"))
        repeat(4) {
            if (field == null) {
                device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 3, 30)
                device.waitForIdle()
                field = device.findObject(By.clazz("android.widget.EditText"))
            }
        }
        assertEquals(context.packageName, device.currentPackageName)
        val inviteField = field ?: error("Invite input missing")
        val bounds = inviteField.visibleBounds
        device.click(bounds.centerX(), bounds.centerY())
        device.waitForIdle()
        // Use real key input: ACTION_SET_TEXT on a cached Compose node can
        // return without updating the field after focus opens the keyboard.
        device.executeShellCommand("input text not-a-pixelody-invite")
        device.waitForIdle()
        device.pressBack() // dismiss the keyboard before scrolling the form
        device.waitForIdle()
        capture("invite-entry")
        text("not-a-pixelody-invite")
        tap("Use Invite")
        text("That isn't a Pixelody connection invite. Copy a new invite from your desktop, or scan its QR code.")
        assertFalse("Invalid invites never populate a fixture library", device.hasObject(By.text("Shuffle all")))
        capture("invalid-invite")
    }
}
