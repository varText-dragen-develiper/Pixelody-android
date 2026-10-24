package com.pixelody.app.data

import com.pixelody.app.feature.unskippable.CoverLayout
import com.pixelody.app.feature.unskippable.CoverLook
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnskippableCoverLayoutTest {
    @Test fun listenersPickWinsThenThemeThenOrientation() {
        assertEquals(CoverLayout.Art, CoverLayout.resolve(CoverLayout.Art, CoverLayout.Side, landscape = true))
        assertEquals(CoverLayout.Side, CoverLayout.resolve(CoverLayout.Theme, CoverLayout.Side, landscape = false))
        assertEquals(CoverLayout.Stacked, CoverLayout.resolve(CoverLayout.Theme, CoverLayout.Theme, landscape = false))
        assertEquals(CoverLayout.Side, CoverLayout.resolve(CoverLayout.Theme, CoverLayout.Theme, landscape = true))
    }

    @Test fun unknownSavedValuesFallBackToThemeDefault() {
        assertEquals(CoverLayout.Theme, CoverLayout.of("sideways"))
        assertEquals(CoverLayout.Theme, CoverLayout.of(null))
        assertEquals(CoverLayout.Side, CoverLayout.of("side"))
    }

    @Test fun themeLooksApplyOnThemeDefaultOnly() {
        assertEquals(CoverLook.CartridgeQuest, CoverLook.active(CoverLayout.Theme, PixelodyMobileTheme.CartridgeQuest))
        assertEquals(CoverLook.Obsession, CoverLook.active(CoverLayout.Theme, PixelodyMobileTheme.Obsession))
        assertEquals(CoverLook.LoFiCafe, CoverLook.active(CoverLayout.Theme, PixelodyMobileTheme.LoFiCafe))
        assertNull(CoverLook.active(CoverLayout.Side, PixelodyMobileTheme.Obsession))
        assertNull(CoverLook.active(CoverLayout.Theme, PixelodyMobileTheme.Studio))
    }
}
