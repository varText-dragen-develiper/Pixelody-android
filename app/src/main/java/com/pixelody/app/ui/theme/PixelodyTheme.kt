package com.pixelody.app.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.pixelody.app.ui.theme.units.LocalThemeUnitKit
import com.pixelody.app.ui.theme.units.themeUnitKitFor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Pixelody Mobile Themes.
 *
 * Each theme faithfully emulates an authoritative desktop theme from the
 * Pixelody desktop catalog:
 * - [Studio]: The core AK3 audio hardware direction (60/25/15, Xerox cyan).
 * - [CartridgeQuest]: 8/16-bit console adventure, cartridge library, RPG commands.
 * - [ObsidianGlass]: Smoked dark acrylic, floating glass pills, prism refractions.
 * - [LoFiCafe]: Rainy walnut nook, dark wood structure, paper-soft content planes.
 * - [BulkheadTerminal]: Industrial sci-fi terminal, phosphor ink, framed geometry.
 * - [Obsession]: Restrained thriller poster, bone type, crimson held back for state.
 *
 * Every value each theme uses comes from its [PixelodyThemeSpec], so none of
 * them can invent a mid-grey, miss the ink tier or ship an illegible on-colour
 * without a direction conformance check catching it. Six
 * hand-written `darkColorScheme` blocks would be the same shape as the five
 * variants deleted on 2026-09-15; the spec is why that cannot happen again here.
 */
enum class PixelodyMobileTheme(
    val id: String,
    val displayName: String,
    val badge: String,
    val summary: String,
    val spec: PixelodyThemeSpec,
    /**
     * Geometry is authored per theme, but the ANGLE is never negotiable: every
     * plate cuts one corner at 35.3 degrees from vertical, and a theme varies
     * only how deep that cut runs. Material's shape slots take a
     * CornerBasedShape and so cannot hold this - components adopt it directly.
     */
    val plate: Shape
) {
    Studio(
        id = "studio",
        displayName = "Pixelody Studio",
        badge = "STUDIO",
        summary = "Technical ground, two-tier ink, bounded light. 60/25/15.",
        spec = PixelodyThemeSpecs.Studio,
        plate = PixelodyDirection.Plate
    ),
    CartridgeQuest(
        id = "cartridge-quest",
        displayName = "Cartridge Quest",
        badge = "8BIT",
        summary = "8/16-bit console adventure, cartridge stacks, hardware controls.",
        spec = PixelodyThemeSpecs.CartridgeQuest,
        // Manifest: corners "pixel-cut". A deeper cut reads as moulded plastic.
        plate = ChamferedPlate(depthRatio = 0.42f)
    ),
    ObsidianGlass(
        id = "obsidian-glass",
        displayName = "Obsidian Glass",
        badge = "GLASS",
        summary = "Smoked acrylic glass, floating frosted pills, prism refraction.",
        spec = PixelodyThemeSpecs.ObsidianGlass,
        // Manifest: corners "soft". The only theme whose identity is the radius,
        // so its plate is the shallowest cut in the set - nearly a square.
        plate = ChamferedPlate(depthRatio = 0.12f)
    ),
    LoFiCafe(
        id = "lo-fi-cafe",
        displayName = "Lo-Fi Café",
        badge = "CAFÉ",
        summary = "Rainy walnut nook, dark wood structure, paper-soft content planes.",
        spec = PixelodyThemeSpecs.LoFiCafe,
        // Manifest: corners "soft", controls "ornamental".
        plate = ChamferedPlate(depthRatio = 0.22f)
    ),
    BulkheadTerminal(
        id = "bulkhead-terminal",
        displayName = "Bulkhead Terminal",
        badge = "TERM",
        summary = "Industrial terminal HUD, phosphor ink, framed instrument geometry.",
        spec = PixelodyThemeSpecs.BulkheadTerminal,
        // Manifest: corners "framed". Identity comes from brackets and rules, so
        // the cut is present but shallow and the frame does the talking.
        plate = ChamferedPlate(depthRatio = 0.18f)
    ),
    Obsession(
        id = "obsession",
        displayName = "Obsession",
        badge = "OBSESS",
        summary = "Restrained thriller poster, bone type, crimson held back for state.",
        spec = PixelodyThemeSpecs.Obsession,
        // Manifest: corners "pixel-cut". The deepest cut in the set - this is the
        // theme's original fracture geometry, the one asset of its own that ports.
        plate = ChamferedPlate(depthRatio = 0.5f)
    );

    /** Read by the settings picker's accent chip. One source of truth. */
    val accentColor: Color get() = spec.primary
}

/** Provides the active [PixelodyMobileTheme] down the Compose tree. */
val LocalPixelodyThemeVariant = staticCompositionLocalOf { PixelodyMobileTheme.Studio }

// ---------------------------------------------------------------------------
// Colour schemes, generated from the spec
// ---------------------------------------------------------------------------

/**
 * Material asks for 21 colour roles. Three of them - onSurfaceVariant, outline
 * and outlineVariant - are mid-greys by construction, and the middle band
 * luma 60-240 is exactly what this direction keeps empty. So the roles are not
 * filled with Material's idea of them; they are mapped onto the two ink tiers,
 * and every one resolves to ground, trace, ink, or a colour sampled from the
 * theme's own source.
 *
 * The ground itself is painted by [pixelodyGround], because the ground is a ramp
 * and a ColorScheme slot can only hold one value.
 */
private fun schemeFor(s: PixelodyThemeSpec): ColorScheme = darkColorScheme(
    primary = s.primary,
    onPrimary = s.onPrimary,
    primaryContainer = s.panelDeep,
    onPrimaryContainer = s.ink,

    secondary = s.secondary,
    onSecondary = s.onSecondary,
    secondaryContainer = s.panel,
    onSecondaryContainer = s.ink,

    tertiary = s.tertiary,
    onTertiary = s.onTertiary,
    tertiaryContainer = s.panelDeep,
    onTertiaryContainer = s.ink,

    background = s.groundTop,
    onBackground = s.ink,

    surface = s.groundTop,
    onSurface = s.ink,

    // The role that made this direction impossible inside Material: a second,
    // quieter ink. There is no quieter ink. Hierarchy comes from mass and
    // position, not from value, so this is ink at full strength.
    surfaceVariant = s.panel,
    onSurfaceVariant = s.ink,

    // Trace at the head of the ramp, where it holds its 1.47:1. No boundary a
    // control depends on may rely on it - see PixelodyDirection.TRACE_HOLDS_ABOVE.
    outline = s.traceAt(0f),
    outlineVariant = s.traceAt(0f),

    error = s.error,
    onError = s.onError,
    errorContainer = s.errorContainer,
    onErrorContainer = s.ink,

    // The seven surface-container roles Material fills from its own dark tokens.
    // Left at their defaults, every Card, Sheet and Menu would go on painting
    // Material's greys no matter what the rest of this scheme says - M3's Card
    // reads surfaceContainerHighest, not surface.
    surfaceDim = s.groundTop,
    surfaceBright = s.groundBottom,
    surfaceContainerLowest = s.editorialBlack,
    surfaceContainerLow = s.groundTop,
    surfaceContainer = s.panel,
    surfaceContainerHigh = s.panelDeep,
    surfaceContainerHighest = s.panelDeep,

    inverseSurface = s.ink,
    inverseOnSurface = s.groundTop,
    inversePrimary = s.groundTop,

    scrim = s.editorialBlack
)

// ---------------------------------------------------------------------------
// Typography Ranks
// ---------------------------------------------------------------------------

/**
 * Four ranks across Material's fifteen slots, so existing
 * `MaterialTheme.typography.*` calls snap on without a call site changing.
 *
 * A theme may author its letterforms. It may NOT author sizes or weights: those
 * carry the 11sp legibility floor and the inverse optical law (W100 at display,
 * W700 at micro), and both are direction-level, not theme-level. So a theme
 * varies [family] and nothing else.
 *
 * The three source manifests that name a font file - GeistPixel-Circle.woff2,
 * KenneyFuture.ttf, SpaceGrotesk-Variable.ttf - are not wired up. woff2 does not
 * load in Compose at all, and shipping the other two means adding font assets
 * and verifying their licences. System families stand in, and that substitution
 * is recorded rather than hidden.
 */
private fun typographyOf(family: FontFamily?): Typography {
    fun a(style: TextStyle) =
        if (family == null) style else style.copy(fontFamily = family)
    return Typography(
        displayLarge = a(PixelodyDirection.Display),
        displayMedium = a(PixelodyDirection.Display),
        displaySmall = a(PixelodyDirection.Display),

        headlineLarge = a(PixelodyDirection.Title),
        headlineMedium = a(PixelodyDirection.Title),
        headlineSmall = a(PixelodyDirection.Title),

        titleLarge = a(PixelodyDirection.Title),
        titleMedium = a(PixelodyDirection.Body),
        titleSmall = a(PixelodyDirection.Body),

        bodyLarge = a(PixelodyDirection.Body),
        bodyMedium = a(PixelodyDirection.Body),
        bodySmall = a(PixelodyDirection.Body),

        labelLarge = a(PixelodyDirection.Micro),
        labelMedium = a(PixelodyDirection.Micro),
        labelSmall = a(PixelodyDirection.Micro)
    )
}

private val PixelodyTypography = typographyOf(null)

private val CartridgeQuestTypography = Typography(
    displayLarge = PixelodyDirection.Display.copy(fontFamily = FontFamily.Monospace),
    displayMedium = PixelodyDirection.Display.copy(fontFamily = FontFamily.Monospace),
    displaySmall = PixelodyDirection.Display.copy(fontFamily = FontFamily.Monospace),

    headlineLarge = PixelodyDirection.Title.copy(fontFamily = FontFamily.Monospace),
    headlineMedium = PixelodyDirection.Title.copy(fontFamily = FontFamily.Monospace),
    headlineSmall = PixelodyDirection.Title.copy(fontFamily = FontFamily.Monospace),

    titleLarge = PixelodyDirection.Title.copy(fontFamily = FontFamily.Monospace),
    titleMedium = PixelodyDirection.Body.copy(fontFamily = FontFamily.Monospace),
    titleSmall = PixelodyDirection.Body.copy(fontFamily = FontFamily.Monospace),

    bodyLarge = PixelodyDirection.Body,
    bodyMedium = PixelodyDirection.Body,
    bodySmall = PixelodyDirection.Body.copy(fontFamily = FontFamily.Monospace),

    labelLarge = PixelodyDirection.Micro.copy(fontFamily = FontFamily.Monospace),
    labelMedium = PixelodyDirection.Micro.copy(fontFamily = FontFamily.Monospace),
    labelSmall = PixelodyDirection.Micro.copy(fontFamily = FontFamily.Monospace)
)

/** Manifest: typography profile "technical". A terminal is monospaced throughout. */
private val BulkheadTerminalTypography = typographyOf(FontFamily.Monospace)

/** Manifest: typography profile "editorial". Stand-in for a text serif. */
private val LoFiCafeTypography = typographyOf(FontFamily.Serif)

// ---------------------------------------------------------------------------
// Shapes
// ---------------------------------------------------------------------------
private val PixelodyShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

private val CartridgeQuestShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

private val ObsidianGlassShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Manifest: corners "soft". Warmer than glass, nothing like a pill. */
private val LoFiCafeShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(5.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

/** Manifest: corners "framed" and "pixel-cut" respectively - both square here,
 *  because their geometry lives in [PixelodyMobileTheme.plate], not in a radius. */
private val SquareShapes = PixelodyShapes

// ---------------------------------------------------------------------------
// Ground Ramps
// ---------------------------------------------------------------------------
fun PixelodyMobileTheme.groundColors(): List<Color> = listOf(spec.groundTop, spec.groundBottom)

/**
 * Paints the viewport ground ramp according to the selected theme.
 * Always bind to the viewport height, never to scrolling content height.
 */
fun Modifier.pixelodyGround(theme: PixelodyMobileTheme = PixelodyMobileTheme.Studio): Modifier = this.background(
    Brush.verticalGradient(theme.groundColors())
)

@Composable
fun PixelodyTheme(
    variant: PixelodyMobileTheme = PixelodyMobileTheme.Studio,
    content: @Composable () -> Unit
) {
    val typography = when (variant) {
        PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestTypography
        PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalTypography
        PixelodyMobileTheme.LoFiCafe -> LoFiCafeTypography
        else -> PixelodyTypography
    }

    val shapes = when (variant) {
        PixelodyMobileTheme.Studio -> PixelodyShapes
        PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestShapes
        PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassShapes
        PixelodyMobileTheme.LoFiCafe -> LoFiCafeShapes
        PixelodyMobileTheme.BulkheadTerminal -> SquareShapes
        PixelodyMobileTheme.Obsession -> SquareShapes
    }

    val unitKit = themeUnitKitFor(variant)

    CompositionLocalProvider(
        LocalIndication provides PixelodyPressIndication,
        LocalPixelodyThemeVariant provides variant,
        LocalThemeUnitKit provides unitKit,
        LocalContentColor provides variant.spec.ink
    ) {
        MaterialTheme(
            colorScheme = schemeFor(variant.spec),
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
