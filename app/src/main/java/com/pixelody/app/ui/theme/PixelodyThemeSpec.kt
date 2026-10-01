package com.pixelody.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.max

/**
 * A second polarity, declared.
 *
 * The direction keeps luma 60-240 empty. That rule exists for one reason: to
 * stop a second, quieter ink appearing on the dark ground, because a mid-value
 * grey in `onSurfaceVariant` is how a system loses its hierarchy - and it is
 * exactly what got five mobile variants deleted on 2026-09-15.
 *
 * A paper plane is not that. It is a second GROUND with its own polarity: a
 * light surface carrying dark type, both declared together, with its own
 * measured contrast. The law the band protects is the contrast, not the value,
 * and a paper plane satisfies it by inverting rather than by muting.
 *
 * So the band still forbids what it always forbade. What it now permits, and
 * only where a theme declares it, is a plane. The distinction is load-bearing:
 *
 *  - [paper] and [paperTrace] may sit inside the band. Nothing else may.
 *  - [paperInk] is the ONLY ink allowed on [paper], and vice versa. Mixing a
 *    plane's ink with the other plane's ground is the failure this prevents.
 *  - a plane must state its measured ratio, and it is checked, not asserted.
 *
 * Cost, recorded honestly: Lo-Fi Cafe's sampled pair measures 10.2:1, which is
 * 1.6 below the 11.8:1 floor every dark ramp in this set holds. Closing that gap
 * would mean moving `--cafe-ink` from #211812 to #080605 - dropping 19 luma and
 * all but 3 of its 15 chroma, which is to say replacing warm brown with black
 * and losing the reason the plane exists. 10.2:1 clears AAA with 1.5 to spare,
 * so the plane's bound is declared as AAA rather than as parity with the dark
 * plane. That is a departure, and it is written down rather than absorbed.
 */
data class PaperPlane(
    /** The light surface. May sit inside the empty middle band; nothing else may. */
    val paper: Color,
    /** A softer paper for large fills. Same plane, same ink. */
    val paperSoft: Color,
    /** The only ink permitted on [paper]. */
    val paperInk: Color,
    /** Trace within the plane - an alpha wash where the source used one. */
    val paperTrace: Color,
    /** Measured ink-on-paper contrast. Checked by the conformance gate. */
    val measuredContrast: Float
)

/**
 * The material of one theme, expressed through the direction's laws rather than
 * around them.
 *
 * Six themes shipping as six hand-written `darkColorScheme` blocks is the exact
 * shape of the thing that was deleted in September: five palettes and no law any
 * of them could violate. This type is the reason that cannot recur at six. Every
 * theme carries:
 *
 *  - a ground ramp re-solved to the law's luma 12 -> 50 endpoints in its own hue;
 *  - its own ink, raised to the ink tier in its own hue;
 *  - its own trace pair, derived at 1.47:1 against its own ramp head;
 *  - one meaning per saturated colour, assigned before anything renders;
 *  - an explicit on-colour per accent, because a pastel accent at luma 200-240
 *    takes white at 1.1-1.7:1 and that is not a rounding error, it is illegible;
 *  - optionally, a declared [PaperPlane].
 *
 * Because every ramp shares the law's endpoints, every theme inherits the same
 * contrast maths: ink lands at 18.0-18.1:1 at the ramp head and 11.7-12.0:1 at
 * its foot, against Studio's 18.1 and 11.8. Accessibility does not vary by the
 * look the person picked.
 *
 * Provenance for every value is recorded on its palette object. Where a value is
 * derived rather than sampled it says DERIVED beside it, and there are exactly
 * three in the whole set.
 */
data class PixelodyThemeSpec(
    val id: String,

    // The ground ramp, viewport-bound, luma 12 at the head to 50 at the foot.
    val groundTop: Color,
    val groundBottom: Color,

    // The two ink tiers. traceHead holds 1.47:1 against groundTop; traceCeiling
    // is luma 59, the last value before the band this direction keeps empty.
    val ink: Color,
    val traceHead: Color,
    val traceCeiling: Color,
    val editorialBlack: Color,

    // Panel material. Never the background - the ground is the ramp. An alpha
    // value here is intentional: it is how Obsidian Glass is glass.
    val panel: Color,
    val panelDeep: Color,

    // One meaning per saturated colour. Each carries the ink that is legible ON
    // it, solved rather than assumed - see the table in the cycle notes.
    val primary: Color,
    val onPrimary: Color,
    val confirmed: Color,
    val onConfirmed: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,

    // Source scope, a real distinction the host owns.
    val sourcePhone: Color,
    val sourceHost: Color,
    val sourceJam: Color,

    // Content fact: lossless / hi-res.
    val lossless: Color,

    /** Declared second polarity, or null for a single-polarity theme. */
    val paperPlane: PaperPlane? = null
) {
    /** Bound to the viewport, never to content height. */
    fun groundBrush(): Brush = Brush.verticalGradient(listOf(groundTop, groundBottom))

    fun groundBrush(viewportHeightPx: Float): Brush = Brush.verticalGradient(
        colors = listOf(groundTop, groundBottom),
        startY = 0f,
        endY = max(viewportHeightPx, 1f)
    )

    /**
     * Trace at a vertical position, 0f head to 1f foot.
     *
     * Trace is a ratio, not a value, and the ground ramps - so holding 1.47:1
     * means rising with the ground until luma 59, where the empty middle band
     * stops it. Past [PixelodyDirection.TRACE_HOLDS_ABOVE] it is clamped and
     * decays, which is why nothing a control depends on may rest on it.
     */
    fun traceAt(fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        val hold = PixelodyDirection.TRACE_HOLDS_ABOVE
        return if (f >= hold) traceCeiling else lerp(traceHead, traceCeiling, f / hold)
    }
}

/**
 * The six materials.
 *
 * Studio is the flagship and is unchanged. The other five are ports of desktop
 * themes, each taken from `renderer.js`'s own theme table - the single
 * authoritative map the desktop app actually reads at runtime - rather than from
 * five different stylesheets that may disagree with it.
 */
object PixelodyThemeSpecs {

    /** Pixelody Studio - the flagship. Values unchanged from the direction doc. */
    val Studio = PixelodyThemeSpec(
        id = "studio",
        groundTop = PixelodyDirection.GroundTop,
        groundBottom = PixelodyDirection.GroundBottom,
        ink = PixelodyDirection.Ink,
        traceHead = Color(0xFF303034),
        traceCeiling = Color(0xFF3B3B40),
        editorialBlack = PixelodyDirection.EditorialBlack,
        panel = PixelodyDirection.Palette.TealDeep,
        panelDeep = PixelodyDirection.Palette.TealStep,
        primary = PixelodyDirection.Ink,
        onPrimary = PixelodyDirection.EditorialBlack,
        confirmed = PixelodyDirection.Palette.Mint,
        onConfirmed = PixelodyDirection.EditorialBlack,
        secondary = PixelodyDirection.Palette.XeroxCyan,
        onSecondary = PixelodyDirection.EditorialBlack,
        tertiary = PixelodyDirection.Palette.PaleBlue,
        onTertiary = PixelodyDirection.EditorialBlack,
        error = PixelodyDirection.Palette.HotPink,
        onError = PixelodyDirection.EditorialBlack,
        errorContainer = PixelodyDirection.Palette.WarmPatch,
        sourcePhone = PixelodyDirection.Palette.Mint,
        sourceHost = PixelodyDirection.Palette.PaleBlue,
        sourceJam = PixelodyDirection.Palette.Lavender,
        lossless = PixelodyDirection.Palette.BrightCyan
    )

    /**
     * Cartridge Quest - "an original 8/16-bit console music adventure with
     * cartridge libraries, pixel landscapes, RPG selection states, and
     * hardware-like playback controls".
     *
     * Its manifest asks for `corners: pixel-cut`, which is a chamfer - so this
     * theme wants the direction's 35.3 degree plate natively rather than having
     * it imposed. Violet takes identity and green takes confirmed playback, so
     * selection and playback never collide.
     */
    val CartridgeQuest = PixelodyThemeSpec(
        id = "cartridge-quest",
        groundTop = CartridgeQuestPalette.GroundTop,
        groundBottom = CartridgeQuestPalette.GroundBottom,
        ink = CartridgeQuestPalette.Ink,
        traceHead = CartridgeQuestPalette.TraceHead,
        traceCeiling = CartridgeQuestPalette.TraceCeiling,
        editorialBlack = CartridgeQuestPalette.EditorialBlack,
        panel = CartridgeQuestPalette.ChassisGray,
        panelDeep = CartridgeQuestPalette.PlasticHighlight,
        primary = CartridgeQuestPalette.ActionViolet,
        onPrimary = CartridgeQuestPalette.EditorialBlack,     // 4.53:1 - white gives 4.50
        confirmed = CartridgeQuestPalette.PowerGreen,
        onConfirmed = CartridgeQuestPalette.EditorialBlack,    // 9.81:1 - white gives 2.08
        secondary = CartridgeQuestPalette.RewardYellow,
        onSecondary = CartridgeQuestPalette.EditorialBlack,    // 12.84:1 - white gives 1.59
        tertiary = CartridgeQuestPalette.ContactPinGold,
        onTertiary = CartridgeQuestPalette.EditorialBlack,     // 10.92:1
        error = CartridgeQuestPalette.AlertRed,
        onError = CartridgeQuestPalette.EditorialBlack,        // 5.22:1
        errorContainer = Color(0xFF451717),
        sourcePhone = CartridgeQuestPalette.PowerGreen,
        sourceHost = CartridgeQuestPalette.ActionViolet,
        sourceJam = CartridgeQuestPalette.RewardYellow,
        lossless = CartridgeQuestPalette.ContactPinGold
    )

    /**
     * Obsidian Glass - "a premium default theme with black glass panels, rounded
     * mobile-like controls, large glossy artwork, and soft prism/x-ray audio
     * light".
     *
     * Its manifest asks for rounded mobile controls and glass by name, both of
     * which `docs/theme-asset-catalogue.md` §0 listed as never-introduce. That
     * contradiction is resolved in favour of the theme and recorded in the
     * catalogue rather than left to be discovered.
     *
     * Every accent is pastel at luma 196-240. White on any of them is between
     * 1.1 and 1.7:1, so all five carry [ObsidianGlassPalette.GlassInk] instead.
     */
    val ObsidianGlass = PixelodyThemeSpec(
        id = "obsidian-glass",
        groundTop = ObsidianGlassPalette.GroundTop,
        groundBottom = ObsidianGlassPalette.GroundBottom,
        ink = ObsidianGlassPalette.Ink,
        traceHead = ObsidianGlassPalette.TraceHead,
        traceCeiling = ObsidianGlassPalette.TraceCeiling,
        editorialBlack = ObsidianGlassPalette.GlassInk,
        panel = ObsidianGlassPalette.GlassPanel,
        panelDeep = ObsidianGlassPalette.SmokedGraphite,
        primary = ObsidianGlassPalette.PrismCyan,
        onPrimary = ObsidianGlassPalette.GlassInk,             // 13.74:1 - white gives 1.48
        confirmed = ObsidianGlassPalette.PrismMint,
        onConfirmed = ObsidianGlassPalette.GlassInk,           // 18.24:1 - white gives 1.12
        secondary = ObsidianGlassPalette.PrismViolet,
        onSecondary = ObsidianGlassPalette.GlassInk,           // 12.28:1 - white gives 1.66
        tertiary = ObsidianGlassPalette.PrismPink,
        onTertiary = ObsidianGlassPalette.GlassInk,            // 13.71:1 - white gives 1.48
        error = ObsidianGlassPalette.PrismAlarm,
        onError = ObsidianGlassPalette.GlassInk,               // 12.08:1
        errorContainer = Color(0xFF3F0B15),
        sourcePhone = ObsidianGlassPalette.PrismMint,
        sourceHost = ObsidianGlassPalette.PrismCyan,
        sourceJam = ObsidianGlassPalette.PrismViolet,
        lossless = ObsidianGlassPalette.PrismCyan
    )

    /**
     * Lo-Fi Cafe - "a rainy walnut listening nook for calm second-monitor
     * sessions, with dark wood structure, paper-soft content surfaces, amber
     * playback light, moss status accents, muted rain-glass depth".
     *
     * The only theme here with two polarities, and the only one whose ramp head
     * needed no adjustment at all: `--cafe-night` #100B08 measures luma 12.0 as
     * sampled.
     *
     * Moss takes confirmed playback because green already reads as playing, and
     * amber takes identity. Moss is also the one accent in the set that wants
     * white rather than the editorial black - at luma 106 it is dark enough that
     * the ink wins, 5.39:1 against 3.77.
     */
    val LoFiCafe = PixelodyThemeSpec(
        id = "lo-fi-cafe",
        groundTop = LoFiCafePalette.GroundTop,
        groundBottom = LoFiCafePalette.GroundBottom,
        ink = LoFiCafePalette.Ink,
        traceHead = LoFiCafePalette.TraceHead,
        traceCeiling = LoFiCafePalette.TraceCeiling,
        editorialBlack = LoFiCafePalette.EditorialBlack,
        panel = LoFiCafePalette.Espresso,
        panelDeep = LoFiCafePalette.Wood,
        primary = LoFiCafePalette.Amber,
        onPrimary = LoFiCafePalette.EditorialBlack,            // 6.86:1
        confirmed = LoFiCafePalette.Moss,
        onConfirmed = LoFiCafePalette.Ink,                     // 5.39:1 - black gives 3.77
        secondary = LoFiCafePalette.Honey,
        onSecondary = LoFiCafePalette.EditorialBlack,          // 10.88:1
        tertiary = LoFiCafePalette.Rain,
        onTertiary = LoFiCafePalette.EditorialBlack,           // 5.15:1
        error = LoFiCafePalette.Ember,
        onError = LoFiCafePalette.Ink,                         // 4.92:1
        errorContainer = Color(0xFF3A1D14),
        sourcePhone = LoFiCafePalette.Sage,
        sourceHost = LoFiCafePalette.Rain,
        sourceJam = LoFiCafePalette.Amber,
        lossless = LoFiCafePalette.Honey,
        paperPlane = PaperPlane(
            paper = LoFiCafePalette.Paper,
            paperSoft = LoFiCafePalette.PaperSoft,
            paperInk = LoFiCafePalette.PaperInk,
            paperTrace = LoFiCafePalette.PaperTrace,
            measuredContrast = 10.22f
        )
    )

    /**
     * Bulkhead Terminal - "an unofficial original homage to heavy industrial
     * science-fiction terminals and classic shooter HUD design".
     *
     * Its manifest asks for `corners: framed`, so its geometry is square and its
     * identity comes from brackets and rules rather than from a radius.
     *
     * This theme has three saturated colours where Material asks for four accent
     * roles, and its only candidates for a fourth were two near-neutral greys
     * inside the empty middle band. Rather than invent one, [tertiary] reuses
     * the pale mint and the fourth distinction is carried by mass and position -
     * which is the direction's answer to hierarchy anyway.
     */
    val BulkheadTerminal = PixelodyThemeSpec(
        id = "bulkhead-terminal",
        groundTop = BulkheadTerminalPalette.GroundTop,
        groundBottom = BulkheadTerminalPalette.GroundBottom,
        ink = BulkheadTerminalPalette.Ink,
        traceHead = BulkheadTerminalPalette.TraceHead,
        traceCeiling = BulkheadTerminalPalette.TraceCeiling,
        editorialBlack = BulkheadTerminalPalette.EditorialBlack,
        panel = BulkheadTerminalPalette.Panel,
        panelDeep = BulkheadTerminalPalette.PanelDeep,
        primary = BulkheadTerminalPalette.Amber,
        onPrimary = BulkheadTerminalPalette.EditorialBlack,    // 12.98:1 - white gives 1.57
        confirmed = BulkheadTerminalPalette.MintPhosphor,
        onConfirmed = BulkheadTerminalPalette.EditorialBlack,  // 14.31:1 - white gives 1.43
        secondary = BulkheadTerminalPalette.PaleMint,
        onSecondary = BulkheadTerminalPalette.EditorialBlack,  // 17.43:1 - white gives 1.17
        tertiary = BulkheadTerminalPalette.PaleMint,
        onTertiary = BulkheadTerminalPalette.EditorialBlack,
        error = BulkheadTerminalPalette.AlertRed,
        onError = BulkheadTerminalPalette.EditorialBlack,      // 6.16:1
        errorContainer = Color(0xFF3B1710),
        sourcePhone = BulkheadTerminalPalette.MintPhosphor,
        sourceHost = BulkheadTerminalPalette.PaleMint,
        sourceJam = BulkheadTerminalPalette.Amber,
        lossless = BulkheadTerminalPalette.Amber
    )

    /**
     * Obsession - "a restrained psychological-thriller poster interface with
     * bone-white type, original fracture geometry, and crimson reserved for
     * playback and selection".
     *
     * One hue, held back. The restraint IS the theme, so the ground stays
     * near-black and crimson is spent only on state - see [ObsessionPalette] for
     * why ramping the ground through the crimson was both the obvious reading
     * and the wrong one.
     *
     * Its manifest asks for `corners: pixel-cut`, the same chamfer Cartridge
     * Quest wants, and for `motion: pulse` at expressive intensity.
     */
    val Obsession = PixelodyThemeSpec(
        id = "obsession",
        groundTop = ObsessionPalette.GroundTop,
        groundBottom = ObsessionPalette.GroundBottom,
        ink = ObsessionPalette.Ink,
        traceHead = ObsessionPalette.TraceHead,
        traceCeiling = ObsessionPalette.TraceCeiling,
        editorialBlack = ObsessionPalette.EditorialBlack,
        panel = ObsessionPalette.Panel,
        panelDeep = ObsessionPalette.PanelDeep,
        primary = ObsessionPalette.Intent,
        onPrimary = ObsessionPalette.EditorialBlack,           // 6.64:1
        confirmed = ObsessionPalette.Confirmed,
        onConfirmed = ObsessionPalette.Ink,                    // 5.80:1 - black gives 3.52
        secondary = ObsessionPalette.Signal,
        onSecondary = ObsessionPalette.EditorialBlack,         // 4.61:1
        tertiary = ObsessionPalette.Glow,
        onTertiary = ObsessionPalette.Ink,                     // 9.83:1
        error = ObsessionPalette.Alarm,
        onError = ObsessionPalette.EditorialBlack,             // 4.61:1
        errorContainer = ObsessionPalette.Hero,
        sourcePhone = ObsessionPalette.Intent,
        sourceHost = ObsessionPalette.Confirmed,
        sourceJam = ObsessionPalette.Glow,
        lossless = ObsessionPalette.Intent
    )

    val all = listOf(Studio, CartridgeQuest, ObsidianGlass, LoFiCafe, BulkheadTerminal, Obsession)
}
