package com.pixelody.app.modules

import android.content.Intent
import android.app.ActivityManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.pixelody.app.MainActivity
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.ui.brand.BrandIconSwitcher
import com.pixelody.app.ui.brand.resolveLauncherIcon
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import org.junit.Assert.*
import org.junit.Test

/** Explicit audit AVD opt-in only; never change a physical owner's appearance. */
class PublicThemePickerRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val settings = MobileSettingsStore(context)

    private fun finishActivities() = instrumentation.runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            .toList().forEach { it.finishAndRemoveTask() }
    }

    private fun appearanceFixture(block: () -> Unit) {
        check(context.packageName == "com.pixelody.app")
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Audit emulator only" }
        check(InstrumentationRegistry.getArguments().getString("pixelodyAuditAvd") == "Pixelody_Android_Audit_20261025")
        val prefs = context.getSharedPreferences("pixelody_mobile_settings", 0)
        val keys = setOf(MobileSettingsStore.KEY_THEME, MobileSettingsStore.KEY_LOGO_COLOR, MobileSettingsStore.KEY_LOGO_FOLLOWS_THEME)
        val original = prefs.all.filterKeys { it in keys || it.startsWith("style_") }
        try {
            finishActivities()
            settings.saveTheme(PixelodyMobileTheme.Studio)
            settings.saveLogoFollowsTheme(true)
            settings.resetAppearance()
            BrandIconSwitcher.apply(context, resolveLauncherIcon(settings.loadLogoPreferences(), settings.loadTheme(), settings.loadAppearance()))
            block()
        } finally {
            finishActivities()
            val editor = prefs.edit()
            (prefs.all.keys.filter { it in keys || it.startsWith("style_") } + original.keys).forEach { editor.remove(it) }
            original.forEach { (key, value) -> when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            } }
            check(editor.commit())
            BrandIconSwitcher.apply(context, resolveLauncherIcon(settings.loadLogoPreferences(), settings.loadTheme(), settings.loadAppearance()))
        }
    }

    private fun launchAlias() {
        finishActivities()
        val launch = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        assertNotEquals("Exercise the actual launcher alias", MainActivity::class.java.name, launch.component?.className)
        context.startActivity(Intent(launch).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.desc("Settings and themes")), 20_000))
        device.waitForIdle()
        var entry: String? = null
        var taskId = -1
        instrumentation.runOnMainSync {
            val foreground = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().firstOrNull()
            entry = foreground?.intent?.component?.className
            taskId = foreground?.taskId ?: -1
        }
        assertEquals("Alias launch must hand the foreground task to the stable activity", MainActivity::class.java.name, entry)
        val task = context.getSystemService(ActivityManager::class.java).appTasks
            .single { it.taskInfo.id == taskId }.taskInfo
        assertEquals("The task base must also be stable, not just the current activity Intent",
            MainActivity::class.java.name, task.baseIntent.component?.className)
    }

    private fun click(label: String, description: Boolean = false) {
        val selector = if (description) By.desc(label) else By.text(label)
        var target: UiObject2 = requireNotNull(device.wait(Until.findObject(selector), 10_000)) { "Missing $label" }
        while (!target.isClickable && target.parent != null) target = target.parent
        assertTrue(target.isClickable)
        target.click()
        device.waitForIdle()
    }

    @Test fun pickerOnlyOffersThemesAcceptedByDurableSettings() = appearanceFixture {
        launchAlias()
        click("Settings and themes", description = true)
        assertTrue(device.wait(Until.hasObject(By.text("Pixelody Studio selected")), 10_000))
        PixelodyMobileTheme.selectableThemes.forEach { theme -> assertTrue(theme.displayName, device.hasObject(By.text(theme.displayName))) }
        assertFalse(device.hasObject(By.text("Cartridge Quest")))
        assertFalse(device.hasObject(By.text("Bulkhead Terminal")))
    }

    @Test fun aliasLaunchThemeChangesKeepSettingsVisibleAndSurviveReopen() = appearanceFixture {
        launchAlias()
        click("Settings and themes", description = true)
        listOf(PixelodyMobileTheme.ObsidianGlass, PixelodyMobileTheme.LoFiCafe, PixelodyMobileTheme.Obsession).forEach { theme ->
            click(theme.displayName)
            assertTrue("${theme.displayName} selection remains open", device.wait(Until.hasObject(By.text("${theme.displayName} selected")), 10_000))
            assertEquals(context.packageName, device.currentPackageName)
            assertEquals(theme, settings.loadTheme())
        }
        launchAlias()
        assertEquals(PixelodyMobileTheme.Obsession, settings.loadTheme())
        click("Settings and themes", description = true)
        assertTrue(device.wait(Until.hasObject(By.text("Obsession selected")), 10_000))
        click("Pixelody Studio")
        assertTrue(device.wait(Until.hasObject(By.text("Pixelody Studio selected")), 10_000))
        assertEquals(context.packageName, device.currentPackageName)
        assertEquals(PixelodyMobileTheme.Studio, settings.loadTheme())
    }
}
