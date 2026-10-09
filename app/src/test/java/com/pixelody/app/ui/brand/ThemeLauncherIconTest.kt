package com.pixelody.app.ui.brand

import androidx.compose.ui.graphics.Color
import com.pixelody.app.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class ThemeLauncherIconTest {
    private val automatic = LogoPreferences(PixelodyLogoColor.Grape, true)
    @Test fun everyThemeHasAnExactAutomaticIconIncludingStudio() {
        PixelodyMobileTheme.entries.forEach { theme ->
            assertEquals(theme.accentColor, resolveLauncherIcon(automatic, theme, AppearanceSettings()).color)
        }
    }
    @Test fun everyPresetMatchesExactlyAcrossEveryTheme() {
        PixelodyMobileTheme.entries.forEach { theme ->
            StylePalette.entries.filter { it != StylePalette.Theme && it != StylePalette.Custom }.forEach { palette ->
                val style = AppearanceSettings(palette = palette)
                assertEquals(style.specFor(theme).primary, resolveLauncherIcon(automatic, theme, style).color)
            }
        }
    }
    @Test fun explicitLogoChoiceWinsOverThemeAndCustomAccent() {
        PixelodyLogoColor.entries.forEach { chosen ->
            val resolved = resolveLauncherIcon(LogoPreferences(chosen, false), PixelodyMobileTheme.Obsession,
                AppearanceSettings(palette = StylePalette.Custom, primary = "FFFFFF"))
            assertEquals(chosen.aliasName, resolved.aliasName)
            assertEquals(chosen.color, resolved.color)
        }
    }
    @Test fun customColorUsesExactAvailableColorOrClosestShadeDeterministically() {
        val theme = PixelodyMobileTheme.Studio
        val exact = AppearanceSettings(palette = StylePalette.Custom, primary = "FF9860")
        assertEquals(hexColor("FF9860"), resolveLauncherIcon(automatic, theme, exact).color)
        val white = AppearanceSettings(palette = StylePalette.Custom, primary = "FFFFFF")
        assertEquals(ThemeLauncherIcon.Mono.color, resolveLauncherIcon(automatic, theme, white).color)
    }
    @Test fun inAppMarkUsesExactEffectiveAccentAndHonorsManualOverride() {
        val custom = Color(0xFFFFAAAA)
        assertEquals(custom, resolveLogoMarkColor(PixelodyLogoColor.Grape, true, PixelodyMobileTheme.Studio, custom))
        assertEquals(PixelodyLogoColor.Grape.color, resolveLogoMarkColor(PixelodyLogoColor.Grape, false, PixelodyMobileTheme.Obsession, custom))
    }
}
