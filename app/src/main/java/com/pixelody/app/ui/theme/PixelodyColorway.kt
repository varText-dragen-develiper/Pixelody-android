package com.pixelody.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.max

/**
 * A colourway: the material of a theme, on the flagship's anatomy.
 *
 * The theme creation process draws the line these obey. A
 * flagship theme authors its own component geometry, information hierarchy,
 * navigation chrome and adaptive behaviour, and costs 5-8 days each with one
 * candidate active at a time. A colourway keeps all of that and varies the
 * material. These are colourways, and they are labelled as such everywhere the
 * user can see them.
 *
 * What makes them more than a palette swap - and what the four deleted on
 * 2026-09-15 never had - is that each one is expressed through the direction's
 * laws rather than around them:
 *
 *  - its own ground ramp, at the law's L12 -> L50 endpoints in its own hue;
 *  - its own ink, raised to the ink tier in its own hue;
 *  - its own trace tier, derived against its own ground;
 *  - its own sampled palette, with one meaning assigned per saturated colour.
 *
 * Because every ramp shares the law's endpoints, every colourway inherits the
 * same contrast maths: ink lands at 12.2-12.4:1 at the foot of the ramp and
 * 18.9:1 at its head, against Studio's 11.8 and 18.1. Accessibility does not
 * vary by the look the person picked. That is the point of a colourway.
 *
 * Source values were read out of each desktop stylesheet, not eyeballed. Where
 * a value is derived rather than sampled it says so beside it.
 */
data class PixelodyColorway(
    val id: String,

    // Section 2 - the ground ramp, viewport-bound, L12 at the head to L50 at the foot.
    val groundTop: Color,
    val groundBottom: Color,

    // Section 3 - the two ink tiers. traceHead holds 1.47:1 against groundTop;
    // traceCeiling is L59, the last value before the forbidden middle band.
    val ink: Color,
    val traceHead: Color,
    val traceCeiling: Color,
    val editorialBlack: Color,

    // Panel material. Never the background - the ground is the ramp.
    val panel: Color,
    val panelDeep: Color,

    // State ownership. The process doc requires one meaning per saturated
    // colour, assigned before rendering, and selection distinguishable from
    // confirmed playback without relying on colour alone.
    val primary: Color,        // identity and selection
    val confirmed: Color,      // host-confirmed playback
    val secondary: Color,
    val tertiary: Color,
    val error: Color,
    val errorContainer: Color,

    // Source scope, which is a real distinction the host owns.
    val sourcePhone: Color,
    val sourceHost: Color,
    val sourceJam: Color,

    // Content fact: lossless / hi-res.
    val lossless: Color
) {
    /**
     * The ground. Bound to the viewport, never to content height - see
     * [PixelodyDirection.groundBrush].
     */
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
     * means rising with the ground until L59, where the empty middle band stops
     * it. Past [PixelodyDirection.TRACE_HOLDS_ABOVE] it is clamped and decays,
     * which is why nothing a control depends on may rest on it.
     */
    fun traceAt(fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        val hold = PixelodyDirection.TRACE_HOLDS_ABOVE
        return if (f >= hold) traceCeiling else lerp(traceHead, traceCeiling, f / hold)
    }
}

/**
 * The four selectable materials.
 *
 * Studio is the flagship and is unchanged. The other three are translations of
 * desktop themes, chosen because their identity lives in colour and material
 * rather than in a desktop layout mechanic, and because each one occupies a
 * different temperature: cold and decayed, warm and soft, hot and industrial.
 */
object PixelodyColorways {

    /**
     * Pixelody Studio - the flagship. Values unchanged from
     * the Android artistic direction.
     */
    val Studio = PixelodyColorway(
        id = "studio",
        groundTop = Color(0xFF0C0C0D),
        groundBottom = Color(0xFF323235),
        ink = Color(0xFFF6F6F8),
        traceHead = Color(0xFF303034),
        traceCeiling = Color(0xFF3B3B40),
        editorialBlack = Color(0xFF050505),
        panel = PixelodyDirection.Palette.TealDeep,
        panelDeep = PixelodyDirection.Palette.TealStep,
        primary = Color(0xFFF6F6F8),
        confirmed = PixelodyDirection.Palette.Mint,
        secondary = PixelodyDirection.Palette.XeroxCyan,
        tertiary = PixelodyDirection.Palette.PaleBlue,
        error = PixelodyDirection.Palette.HotPink,
        errorContainer = PixelodyDirection.Palette.WarmPatch,
        sourcePhone = PixelodyDirection.Palette.Mint,
        sourceHost = PixelodyDirection.Palette.PaleBlue,
        sourceJam = PixelodyDirection.Palette.Lavender,
        lossless = PixelodyDirection.Palette.BrightCyan
    )

    /**
     * Dead Signal - "an elegant haunted broadcast archive of bone-white
     * instruments, dark machine surfaces, cyan wave motion".
     *
     * Source: `https://github.com/varText-dragen-develiper/pixelody/blob/main/src/dead-signal.css`, eleven named custom properties. Cyan
     * `#1BD7D0` is the dominant accent there by a wide margin - 29 uses against
     * amber's 5 - so it takes identity, and amber takes confirmed playback so
     * the two never collide.
     *
     * One value could not port. `--dead-ash #8C8580` is a near-neutral grey at
     * L134, inside the L60-L240 band the direction keeps empty. It becomes the
     * trace tier, which is the job it was doing.
     */
    val DeadSignal = PixelodyColorway(
        id = "dead-signal",
        groundTop = Color(0xFF0C0C0C),      // --dead-black #020202, raised to L12
        groundBottom = Color(0xFF373131),   // --dead-rot #2A2525, raised to L50
        ink = Color(0xFFFCFBF9),            // --dead-bone #E8E1D6, raised to the ink tier
        traceHead = Color(0xFF303030),
        traceCeiling = Color(0xFF3B3B3B),
        editorialBlack = Color(0xFF020202), // --dead-black, sampled
        panel = Color(0xFF070405),          // --dead-panel
        panelDeep = Color(0xFF100607),      // --dead-blood
        primary = Color(0xFF1BD7D0),        // --dead-cyan
        confirmed = Color(0xFFD09A3A),      // --dead-amber
        secondary = Color(0xFFD09A3A),      // --dead-amber
        tertiary = Color(0xFF12383A),       // --dead-wave
        error = Color(0xFFC31622),          // --dead-red
        errorContainer = Color(0xFF5E1014), // --dead-dried
        sourcePhone = Color(0xFFD09A3A),    // --dead-amber
        sourceHost = Color(0xFF1BD7D0),     // --dead-cyan
        sourceJam = Color(0xFFC31622),      // --dead-red
        lossless = Color(0xFF1BD7D0)        // --dead-cyan
    )

    /**
     * Lo-Fi Cafe - "a rainy walnut listening nook for calm sessions, with dark
     * wood structure and paper-soft content surfaces".
     *
     * Source: `https://github.com/varText-dragen-develiper/pixelody/blob/main/src/lo-fi-cafe.css` and its 2026-09-22 Canvas reconstruction,
     * which agree on every value used here. The only warm colourway, and the
     * only one whose ground carries real chroma - its L12 head is the sampled
     * `--cafe-night` almost exactly.
     *
     * Moss takes confirmed playback because green already reads as playing, and
     * amber takes identity. The source has no red at all, so [error] is derived
     * rather than sampled - it is the one value here without provenance.
     */
    val LoFiCafe = PixelodyColorway(
        id = "lo-fi-cafe",
        groundTop = Color(0xFF100B08),      // --cafe-night, sampled, already L11.8
        groundBottom = Color(0xFF462E1E),   // --cafe-walnut #342216, raised to L50
        ink = Color(0xFFFDFBF7),            // --cafe-paper-soft #ECDFC3, raised to the ink tier
        traceHead = Color(0xFF412D20),
        traceCeiling = Color(0xFF503728),
        editorialBlack = Color(0xFF0A0705),
        panel = Color(0xFF1D130E),          // --cafe-espresso
        panelDeep = Color(0xFF211812),      // --cafe-ink
        primary = Color(0xFFC48A4A),        // --cafe-amber
        confirmed = Color(0xFF5D704F),      // --cafe-moss
        secondary = Color(0xFFE1B775),      // --cafe-honey
        tertiary = Color(0xFF71838A),       // --cafe-rain
        error = Color(0xFFB4543A),          // DERIVED - the source palette has no red
        errorContainer = Color(0xFF3A1D14), // DERIVED, same family
        sourcePhone = Color(0xFF8B9875),    // --cafe-sage
        sourceHost = Color(0xFF71838A),     // --cafe-rain
        sourceJam = Color(0xFFC48A4A),      // --cafe-amber
        lossless = Color(0xFFE1B775)        // --cafe-honey
    )

    /**
     * Bulkhead Terminal - "an unofficial original homage to heavy industrial
     * science-fiction terminals and classic shooter HUD design".
     *
     * Source: `https://github.com/varText-dragen-develiper/pixelody/blob/main/src/bulkhead-terminal-theme.css`, which declares no custom
     * properties, so the values are its most-used literals: mint `#78F09A` 21
     * times, amber `#F6C945` 14, ground `#090C0B` 12.
     *
     * The only colourway with no white ink in its source - a terminal's ink is
     * its phosphor. `--ink` here is the pale mint raised to the tier, so type
     * reads green-white rather than neutral.
     *
     * One value could not port. `#59605C` is a near-neutral grey at L94, inside
     * the empty middle band; it becomes the trace tier.
     */
    val BulkheadTerminal = PixelodyColorway(
        id = "bulkhead-terminal",
        groundTop = Color(0xFF090D0B),      // #050706 / #090C0B, at L12
        groundBottom = Color(0xFF2B3431),   // #171C1A, raised to L50
        ink = Color(0xFFF0FFF4),            // #B0FFC3, raised to the ink tier
        traceHead = Color(0xFF24342C),
        traceCeiling = Color(0xFF2C4036),
        editorialBlack = Color(0xFF050706),
        panel = Color(0xFF111513),
        panelDeep = Color(0xFF07110B),
        primary = Color(0xFFF6C945),        // amber
        confirmed = Color(0xFF78F09A),      // mint phosphor
        secondary = Color(0xFFB0FFC3),      // pale mint
        tertiary = Color(0xFF78F09A),
        error = Color(0xFFFF4C35),          // alert
        errorContainer = Color(0xFF3B1710),
        sourcePhone = Color(0xFF78F09A),
        sourceHost = Color(0xFFB0FFC3),
        sourceJam = Color(0xFFF6C945),
        lossless = Color(0xFFF6C945)
    )

    val all = listOf(Studio, DeadSignal, LoFiCafe, BulkheadTerminal)
}

/**
 * The active material. Components read this rather than the Studio constants on
 * [PixelodyDirection], which remain the law and the default.
 */
val LocalPixelodyColorway = staticCompositionLocalOf { PixelodyColorways.Studio }
