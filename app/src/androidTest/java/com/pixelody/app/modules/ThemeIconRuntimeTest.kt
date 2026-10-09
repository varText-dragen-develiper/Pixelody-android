package com.pixelody.app.modules

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.*
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.ui.brand.*
import com.pixelody.app.ui.theme.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Only the isolated app: no owner theme, logo preference, launcher or library mutation. */
class ThemeIconRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val settings = MobileSettingsStore(context)
    private fun finishActivities() = instrumentation.runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            .toList().forEach { it.finishAndRemoveTask() }
    }
    private fun openProfile() {
        finishActivities()
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://profile")).setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
        device.waitForIdle()
    }
    private fun assertIcon(expected: LauncherIcon) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        var matches = context.packageManager.queryIntentActivities(intent, 0)
        repeat(60) {
            if (matches.size != 1 || matches.single().activityInfo.name != BrandIconSwitcher.component(context, expected).className) {
                Thread.sleep(100); matches = context.packageManager.queryIntentActivities(intent, 0)
            }
        }
        assertEquals("Exactly one enabled launcher", 1, matches.size)
        assertEquals(BrandIconSwitcher.component(context, expected).className, matches.single().activityInfo.name)
        val adaptive = context.resources.getDrawable(matches.single().activityInfo.icon, context.theme) as AdaptiveIconDrawable
        val foreground = adaptive.foreground
        val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
        foreground.setBounds(0, 0, 240, 240); foreground.draw(Canvas(bitmap))
        var pixels = 0
        for (y in 0 until 240) for (x in 0 until 240) if (bitmap.getPixel(x, y) == expected.color.toArgb()) pixels++
        bitmap.recycle()
        assertTrue("Packaged icon paints the expected accent", pixels > 500)
        assertEquals("Icon changes keep the activity foreground", context.packageName, device.currentPackageName)
    }
    private fun find(selector: BySelector): UiObject2 {
        for (up in listOf(true, false)) repeat(20) {
            device.findObject(selector)?.let { return it }
            assertEquals(context.packageName, device.currentPackageName)
            val x = device.displayWidth / 30
            device.swipe(x, if (up) 1400 else 1000, x, if (up) 1000 else 1400, 70)
            device.waitForIdle(); Thread.sleep(150)
        }
        error("Missing $selector")
    }
    @Test fun themesPalettesManualOverrideAndLauncherRemainConsistent() {
        assumeTrue(context.packageName.endsWith(".studioqa"))
        val prefs = context.getSharedPreferences("pixelody_mobile_settings", 0)
        val keys = setOf(MobileSettingsStore.KEY_THEME, MobileSettingsStore.KEY_LOGO_COLOR, MobileSettingsStore.KEY_LOGO_FOLLOWS_THEME)
        val original = prefs.all.filterKeys { it in keys || it.startsWith("style_") }
        try {
            PixelodyMobileTheme.entries.forEach { theme ->
                finishActivities(); settings.resetAppearance(); settings.saveTheme(theme); settings.saveLogoFollowsTheme(true)
                openProfile()
                assertIcon(resolveLauncherIcon(settings.loadLogoPreferences(), theme, settings.loadAppearance()))
            }
            // Palette changes apply live, without recreating the activity or touching the manual color.
            StylePalette.entries.filter { it != StylePalette.Theme && it != StylePalette.Custom }.forEach { palette ->
                settings.saveAppearance(AppearanceSettings(palette = palette))
                assertIcon(resolveLauncherIcon(settings.loadLogoPreferences(), PixelodyMobileTheme.Obsession, settings.loadAppearance()))
            }
            settings.saveAppearance(AppearanceSettings(palette = StylePalette.Custom, primary = "FFFFFF"))
            assertIcon(LauncherIcon(ThemeLauncherIcon.Mono.aliasName, ThemeLauncherIcon.Mono.color))
            find(By.desc("Grape logo")).click(); device.waitForIdle()
            assertFalse(settings.loadLogoFollowsTheme())
            assertIcon(LauncherIcon(PixelodyLogoColor.Grape.aliasName, PixelodyLogoColor.Grape.color))
            settings.saveAppearance(AppearanceSettings(palette = StylePalette.Aurora))
            assertIcon(LauncherIcon(PixelodyLogoColor.Grape.aliasName, PixelodyLogoColor.Grape.color))
            find(By.textContains("Pixelody Studio")).click(); device.waitForIdle()
            assertEquals(PixelodyMobileTheme.Studio, settings.loadTheme())
            assertIcon(LauncherIcon(PixelodyLogoColor.Grape.aliasName, PixelodyLogoColor.Grape.color))
            find(By.desc("Follow theme and palette")).click(); device.waitForIdle()
            assertTrue(settings.loadLogoFollowsTheme())
            assertEquals(PixelodyLogoColor.Grape, settings.loadLogoColor())
            assertIcon(LauncherIcon(ThemeLauncherIcon.Aurora.aliasName, ThemeLauncherIcon.Aurora.color))
            settings.resetAppearance()
            val studio = LauncherIcon(ThemeLauncherIcon.Studio.aliasName, ThemeLauncherIcon.Studio.color)
            assertIcon(studio)
            finishActivities()
            context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(BrandIconSwitcher.component(context, studio)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), 10000))
            assertIcon(studio)
        } finally {
            finishActivities()
            val editor = prefs.edit()
            (prefs.all.keys.filter { it in keys || it.startsWith("style_") } + original.keys).forEach { editor.remove(it) }
            original.forEach { (key, value) -> when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
            } }
            editor.commit()
            BrandIconSwitcher.apply(context, resolveLauncherIcon(settings.loadLogoPreferences(), settings.loadTheme(), settings.loadAppearance()))
        }
    }
}
