package com.pixelody.app.modules

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test

class ModuleImportNavigationTest {
    @Test fun settingsEntryOpensModulesAndSystemFilePicker() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://profile"))
            .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val entry = device.wait(Until.findObject(By.text("Modules — import or manage")), 15000)
        assertNotNull("Settings must expose the module manager", entry)
        entry.click()
        val importer = device.wait(Until.findObject(By.text("Import downloaded module")), 10000)
        assertNotNull("Module manager must expose Import", importer)
        assertTrue(importer.isEnabled)
        importer.click()
        assertTrue("Import must open the system document picker", device.wait(
            Until.hasObject(By.pkg("com.google.android.documentsui")), 10000) || device.hasObject(By.pkg("com.android.documentsui")))
        device.pressBack()
        assertNotNull(device.wait(Until.findObject(By.text("Import downloaded module")), 10000))
    }
}
