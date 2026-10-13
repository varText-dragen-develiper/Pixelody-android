package com.pixelody.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class AppearanceSettingsTest {
    @Test fun desktopPalettesHaveReadableControlLabelsAndSurfaceText() {
        for (palette in StylePalette.values()) {
            for (theme in PixelodyMobileTheme.values()) {
                val spec = AppearanceSettings(palette = palette).specFor(theme)
                assertTrue("$palette / $theme primary label", colorContrast(spec.primary, spec.onPrimary) >= 4.5f)
                assertTrue("$palette / $theme secondary label", colorContrast(spec.secondary, spec.onSecondary) >= 4.5f)
                assertTrue("$palette / $theme surface text", colorContrast(spec.ink, spec.panelDeep) >= 4.5f)
                if (theme == PixelodyMobileTheme.Studio && palette != StylePalette.Theme) assertTrue("$palette accent", colorContrast(spec.primary, spec.panelDeep) >= 4.5f)
            }
        }
    }
    @Test fun customColorsRejectUnreadableAndMalformedValues() {
        assertNull(normalizeHex("#xyz"))
        assertEquals("AABBCC", normalizeHex(" #aabbcc "))
        assertFalse(isReadableBase("FFFFFF"))
        assertFalse(isReadableAccent("000000", "0B0D12"))
        val style = AppearanceSettings(primary = "000000", base = "FFFFFF", textPercent = 7).normalized()
        assertEquals(AppearanceSettings(), style)
    }
    @Test fun highContrastKeepsAccentIdentityAndRaisesBoundaries() {
        val normal = AppearanceSettings(palette = StylePalette.Cobalt).specFor(PixelodyMobileTheme.Studio)
        val high = AppearanceSettings(palette = StylePalette.Cobalt, highContrast = true).specFor(PixelodyMobileTheme.Studio)
        assertEquals(normal.primary, high.primary)
        assertEquals(Color.White, high.ink)
        assertTrue(colorContrast(high.traceHead, high.panelDeep) >= 3f)
    }
    @Test fun authoredThemesIgnoreStudioMixersWithoutLosingAccessibilityOrSavedChoices() {
        val saved = AppearanceSettings(palette = StylePalette.Ember, typeface = StyleTypeface.Mono, textPercent = 130)
        PixelodyMobileTheme.entries.filter { it != PixelodyMobileTheme.Studio }.forEach { theme ->
            assertEquals(theme.spec, saved.specFor(theme))
            assertEquals(StyleTypeface.Theme, saved.forTheme(theme).typeface)
            assertEquals(130, saved.forTheme(theme).textPercent)
            assertEquals(theme.spec.primary, saved.copy(highContrast = true).specFor(theme).primary)
        }
        assertEquals(StylePalette.Ember, saved.forTheme(PixelodyMobileTheme.Studio).palette)
        assertEquals(StyleTypeface.Mono, saved.forTheme(PixelodyMobileTheme.Studio).typeface)
    }
}
