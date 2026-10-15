package com.pixelody.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.pixelody.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ImageStyleEngineTest {
    @Test fun imageColorsInformDistinctDeterministicRecipes() {
        val red = IntArray(4096) { 0xFFBB2200.toInt() }
        val blue = IntArray(4096) { 0xFF0044BB.toInt() }
        assertEquals(ImageStyleEngine.analyze(red, 64), ImageStyleEngine.analyze(red, 64))
        assertNotEquals(ImageStyleEngine.analyze(red, 64).palettes.first().primary, ImageStyleEngine.analyze(blue, 64).palettes.first().primary)
    }
    @Test fun brightAndBusyImagesSuggestCalmerOpacityAndStrongerShading() {
        val dark = ImageStyleEngine.analyze(IntArray(4096) { 0xFF110500.toInt() }, 64)
        val bright = ImageStyleEngine.analyze(IntArray(4096) { 0xFFFFFFFF.toInt() }, 64)
        val busy = ImageStyleEngine.analyze(IntArray(4096) { if ((it + it / 64) % 2 == 0) -1 else 0xFF000000.toInt() }, 64)
        assertTrue(bright.recommendedOpacity < dark.recommendedOpacity)
        assertTrue(busy.recommendedOpacity < dark.recommendedOpacity)
        assertEquals(ImageSurface.Crisp, bright.palettes.first().surface)
        assertEquals(ImageTypeface.Sans, busy.palettes.first().typeface)
    }
    @Test fun extremeSamplesStayReadableAtFullImageOpacityForEveryFraming() {
        listOf(0xFFFFFFFF, 0xFF000000, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF808080).forEach { rgb ->
            ImageStyleEngine.analyze(IntArray(4096) { rgb.toInt() }, 64).palettes.forEach { image ->
                val spec = AppearanceSettings().withImageStyle(image).specFor(PixelodyMobileTheme.Studio)
                assertTrue(isReadableBase(image.base))
                assertEquals(image.primary, AppearanceSettings().withImageStyle(image).primary)
                assertTrue(colorContrast(spec.ink, spec.panelDeep) >= 7f)
                assertTrue(colorContrast(spec.primary, spec.onPrimary) >= 4.5f)
                assertTrue(colorContrast(spec.secondary, spec.onSecondary) >= 4.5f)
                ImageSurface.entries.forEach { surface ->
                    val worstGround = spec.groundTop.copy(alpha = surface.shade).compositeOver(Color.White)
                    assertTrue("Text over full bright image", colorContrast(spec.ink, worstGround) >= 4.5f)
                    assertTrue("Accent over full bright image", colorContrast(spec.primary, worstGround) >= 4.5f)
                }
            }
        }
    }
    @Test fun styleRoundTripAndSingleScreenResetPreserveOtherPictures() {
        val style = ImageStyleEngine.analyze(IntArray(16) { 0xFFBB6600.toInt() }, 4).palettes.first()
        val old = CoverCodec.decode("v1\nbackground:search|file:///old.jpg||0.3\nplaylist:p|file:///p.jpg|note")
        val styled = old.withStyledBackground(ScreenBackground.Home, "file:///new.jpg", 0.75f, style)
        assertEquals(old, styled.withoutBackground(ScreenBackground.Home))
        assertEquals(styled, CoverCodec.decode(CoverCodec.encode(styled)))
        assertNull(old.entry(ScreenBackground.Search.key)?.imageStyle)
        assertEquals(0.3f, styled.backgroundOpacityFor(ScreenBackground.Search), 0f)
    }
    @Test fun corruptStyleDoesNotLoseImageAndInvalidOpacityStaysFinite() {
        val decoded = CoverCodec.decode("v1\nbackground:home|file:///keep.jpg||0.5|bad,style")
        assertEquals("file:///keep.jpg", decoded.imageFor(ScreenBackground.Home.key, null))
        assertNull(decoded.entry(ScreenBackground.Home.key)?.imageStyle)
        val style = ScreenImageStyle("FFFFFF", "FFFFFF", "FFFFFF")
        assertTrue(isReadableBase(AppearanceSettings().withImageStyle(style).base))
        assertEquals(0.6f, decoded.withStyledBackground(ScreenBackground.Home, "file:///keep.jpg", Float.NaN, style).backgroundOpacityFor(ScreenBackground.Home), 0f)
    }
}
