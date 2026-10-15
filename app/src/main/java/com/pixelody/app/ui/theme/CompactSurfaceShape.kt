package com.pixelody.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

internal fun PixelodyMobileTheme.compactSurfaceShape(): Shape = when (this) {
    PixelodyMobileTheme.CartridgeQuest -> RoundedCornerShape(4.dp)
    PixelodyMobileTheme.ObsidianGlass -> RoundedCornerShape(12.dp)
    PixelodyMobileTheme.LoFiCafe -> RoundedCornerShape(6.dp)
    PixelodyMobileTheme.BulkheadTerminal -> RoundedCornerShape(2.dp)
    PixelodyMobileTheme.Obsession -> ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.08f)
    PixelodyMobileTheme.Studio -> RoundedCornerShape(16.dp)
}
