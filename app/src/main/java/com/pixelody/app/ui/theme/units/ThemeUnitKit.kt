package com.pixelody.app.ui.theme.units

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Contract for a theme's visual unit kit.
 *
 * Each theme provides an implementation of this interface, deciding how each
 * semantic information unit is physically and stylistically rendered on screen.
 */
interface ThemeUnitKit {
    val themeId: String

    @Composable
    fun TrackItem(
        data: TrackItemData,
        modifier: Modifier
    )

    @Composable
    fun TransportControls(
        data: TransportData,
        modifier: Modifier
    )

    @Composable
    fun HeroStage(
        data: HeroStageData,
        modifier: Modifier
    )

    @Composable
    fun QuickStartTile(
        data: QuickTileData,
        modifier: Modifier
    )
}

/** Provides the active [ThemeUnitKit] down the Compose tree. */
val LocalThemeUnitKit = staticCompositionLocalOf<ThemeUnitKit> {
    StudioThemeUnitKit
}

/**
 * Resolves the appropriate [ThemeUnitKit] for a given [com.pixelody.app.ui.theme.PixelodyMobileTheme].
 */
fun themeUnitKitFor(theme: com.pixelody.app.ui.theme.PixelodyMobileTheme): ThemeUnitKit = when (theme) {
    com.pixelody.app.ui.theme.PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestThemeUnitKit
    com.pixelody.app.ui.theme.PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassThemeUnitKit
    com.pixelody.app.ui.theme.PixelodyMobileTheme.LoFiCafe -> LoFiCafeThemeUnitKit
    com.pixelody.app.ui.theme.PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalThemeUnitKit
    com.pixelody.app.ui.theme.PixelodyMobileTheme.Obsession -> ObsessionThemeUnitKit
    else -> StudioThemeUnitKit
}

/**
 * Standard facade composable for a track item row/card across all screens.
 * Dispatches to the active theme's [ThemeUnitKit].
 */
@Composable
fun PixelodyTrackItem(
    data: TrackItemData,
    modifier: Modifier = Modifier
) {
    LocalThemeUnitKit.current.TrackItem(data = data, modifier = modifier)
}

/**
 * Standard facade composable for playback transport controls across all screens.
 * Dispatches to the active theme's [ThemeUnitKit].
 */
@Composable
fun PixelodyTransportControls(
    data: TransportData,
    modifier: Modifier = Modifier
) {
    LocalThemeUnitKit.current.TransportControls(data = data, modifier = modifier)
}

/**
 * Standard facade composable for hero stage headers across all screens.
 * Dispatches to the active theme's [ThemeUnitKit].
 */
@Composable
fun PixelodyHeroStage(
    data: HeroStageData,
    modifier: Modifier = Modifier
) {
    LocalThemeUnitKit.current.HeroStage(data = data, modifier = modifier)
}

/**
 * Standard facade composable for quick start shortcut tiles.
 * Dispatches to the active theme's [ThemeUnitKit].
 */
@Composable
fun PixelodyQuickStartTile(
    data: QuickTileData,
    modifier: Modifier = Modifier
) {
    LocalThemeUnitKit.current.QuickStartTile(data = data, modifier = modifier)
}
