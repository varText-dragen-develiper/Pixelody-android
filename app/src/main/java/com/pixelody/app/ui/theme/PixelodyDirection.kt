package com.pixelody.app.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The artistic direction of record, as tokens.
 *
 * Source: the Android artistic direction, which translates the AK3
 * direction (all three languages at 60/25/15) from the desktop asset
 * catalogue to a 360dp phone frame.
 *
 * Nothing in this file is a preference. Every constant is either measured
 * off the references (catalogue section 1) or derived from one of those
 * measurements by a conversion shown in the comment beside it. Three
 * departures from the desktop numbers are marked DEPARTURE and each says
 * what the phone forced and why.
 *
 * The rule this file exists to make enforceable: a component may not
 * invent a colour, a radius, a stroke weight or a type size. If a value is
 * not here, it is not in the direction.
 */
object PixelodyDirection {

    // ---------------------------------------------------------------
    // Frame
    // ---------------------------------------------------------------
    // The catalogue is a proportional system: linework is a percentage of
    // frame width, bracket arms a percentage of the framed edge, checker
    // cells container/8. percent_of_frame is the invariant; px is not.

    const val FRAME_WIDTH_DP = 360f      // 1080px at density 3.0
    const val FRAME_HEIGHT_DP = 780f     // 2340px at density 3.0
    const val CATALOGUE_FRAME_PX = 1200f // the desktop frame every % below was read off

    // ---------------------------------------------------------------
    // Section 2 - Ground
    // ---------------------------------------------------------------
    // Vertical linear ramp, L12 top -> L50 bottom, zero horizontal
    // component, chroma |a|,|b| < 1.2. A flat black ground is wrong.
    //
    // DEPARTURE 1. The desktop rate of +3.45 luma/100px is NOT carried
    // over. Applied to 2340px it reaches L93 - past the ground range and
    // into the forbidden middle band. The endpoints are the invariant, so
    // the rate is re-derived from frame height:
    //     (50 - 12) / 2340px * 100 = +1.62 luma per 100px
    //                              = +4.87 luma per 100dp

    const val GROUND_LUMA_TOP = 12f
    const val GROUND_LUMA_BOTTOM = 50f
    const val GROUND_RATE_PER_100_DP = 4.87f

    val GroundTop = Color(0xFF0C0C0D)    // luma 12.1, tinted blue by 1
    val GroundBottom = Color(0xFF323235) // luma 50.2, tinted blue by 3

    /**
     * The ground. Bind this to the VIEWPORT, never to content height.
     *
     * A desktop window is a fixed frame; a phone list is three to ten
     * screens tall. A ramp that follows content height becomes a moving
     * gradient under scroll, which is a different asset with a different
     * meaning. Scrolling changes what is on the ground, never the ground.
     */
    fun groundBrush(viewportHeightPx: Float): Brush = Brush.verticalGradient(
        colors = listOf(GroundTop, GroundBottom),
        startY = 0f,
        endY = max(viewportHeightPx, 1f)
    )

    /** Ground luma at a vertical position in the viewport, 0f top .. 1f bottom. */
    fun groundLumaAt(fraction: Float): Float =
        GROUND_LUMA_TOP + (GROUND_LUMA_BOTTOM - GROUND_LUMA_TOP) * fraction.coerceIn(0f, 1f)

    // ---------------------------------------------------------------
    // Section 3 - Ink: two tiers and an empty middle
    // ---------------------------------------------------------------
    // ground L12-L50   carries nothing
    // trace ~L56       1.47:1   never carries information
    // ink   L250-255   17.1:1   carries everything
    //
    // The middle band L60-L240 stays empty of grey. Hierarchy comes from
    // mass and position, not from value. There is no secondary grey text
    // in this direction.

    val Ink = Color(0xFFF6F6F8)          // 12.5:1 at the ramp foot, 19.1:1 at its head
    val EditorialBlack = Color(0xFF050505)

    const val MIDDLE_BAND_LOW = 60f
    const val MIDDLE_BAND_HIGH = 240f

    const val TRACE_CONTRAST = 1.47f

    /** Trace may not enter the middle band, so it cannot exceed L59. */
    const val TRACE_CEILING_LUMA = 59f

    /**
     * Trace holds 1.47:1 against the ground beneath it - so on a ramped
     * ground it is a function of position, not a fixed value.
     *
     * The two laws collide below the ramp midpoint. Holding 1.47:1 against
     * ground L32 or darker needs L60+, which is inside the forbidden
     * middle band. Measured:
     *
     *   y/H 0.00  ground L12  trace L48  1.48:1
     *   y/H 0.51  ground L31  trace L59  1.47:1   <- last position it holds
     *   y/H 1.00  ground L50  trace L59  1.14:1   <- clamped, decaying
     *
     * So TRACE IS A PROPERTY OF THE UPPER SCREEN. It decays through the
     * lower half and is nearly gone at the bottom edge - which is the thumb
     * zone, which is where the controls are. Since trace never carries
     * information the decay is acceptable. What is not acceptable is
     * depending on it down there. See [TRACE_HOLDS_ABOVE].
     */
    fun traceAt(fraction: Float): Color {
        val groundLuma = groundLumaAt(fraction)
        val target = TRACE_CONTRAST * (relativeLuminance(groundLuma) + 0.05f) - 0.05f
        val value = channelForRelativeLuminance(target).coerceAtMost(TRACE_CEILING_LUMA)
        val v = value.roundToInt().coerceIn(0, 255)
        return Color(red = v, green = v, blue = min(v + 1, 255), alpha = 255)
    }

    /**
     * Above this fraction of the viewport the trace tier still holds its
     * 1.47:1 ratio. Below it, trace decays, and:
     *
     * DEPARTURE 2 (the accessibility bound the desktop did not need):
     * the trace tier may never be the sole carrier of a boundary that a
     * control depends on. If removing the trace stroke would leave a target
     * ambiguous, that boundary belongs to ink or to the plate edge.
     */
    const val TRACE_HOLDS_ABOVE = 0.51f

    // ---------------------------------------------------------------
    // The palette - the ONLY source of chromatic value
    // ---------------------------------------------------------------
    // Every value sampled off the three references (catalogue section 1).
    // A chromatic colour that is not in here is not in the direction.
    // Saturated colour occupies small areas; cyan is bounded to <= 7%.

    object Palette {
        val TealDeep = Color(0xFF04090C)
        val TealStep = Color(0xFF0B2028)
        val TealMid = Color(0xFF234955)
        val XeroxCyan = Color(0xFF69A6AA)   // second ink INSIDE art, <= 7% coverage
        val BrightCyan = Color(0xFF43EDE2)
        val Mint = Color(0xFFD5F6E1)
        val PaleBlue = Color(0xFF93B4DD)
        val Lavender = Color(0xFF8D65A8)
        val Rose = Color(0xFFE0879B)
        val HotPink = Color(0xFFDC7187)
        val WarmPatch = Color(0xFF792831)   // one ~2x2 cell patch per composition, never centred

        val all = listOf(
            TealDeep, TealStep, TealMid, XeroxCyan, BrightCyan,
            Mint, PaleBlue, Lavender, Rose, HotPink, WarmPatch
        )
    }

    // ---------------------------------------------------------------
    // Section 5 - Geometry
    // ---------------------------------------------------------------

    /** 0.22-0.30% of frame width -> 0.8-1.1dp at 360dp. The only structural stroke. */
    val Hairline: Dp = 1.dp

    /** The catalogue's 1.5px on a 24px icon grid. */
    val IconHairline: Dp = 0.5.dp

    /** 4dp lattice. Region edges are hard cuts on this grid. */
    val Lattice: Dp = 4.dp

    /** Hatch: 45 degrees, pitch 12-16px -> 4-5.3dp, stripe:gap 1:1, mirrored between instances. */
    val HatchPitchMin: Dp = 4.dp
    val HatchPitchMax: Dp = 5.dp

    /** Bounded-light floor. 44px of a 1200px frame is 13dp here - too small to carry the falloff. */
    val BloomMinDiameter: Dp = 44.dp

    /** Decorative light is confined to the upper third; the rest is thumb-reachable. */
    const val BLOOM_MAX_FRACTION = 0.33f

    const val CHAMFER_ANGLE_FROM_VERTICAL_DEG = 35.3f

    /** tan(35.3 deg) = 0.709, i.e. 1 across : 1.4 down. Never 45 degrees. */
    const val CHAMFER_SLOPE = 0.709f

    /** Depth as a ratio of plate height, so it survives at the mini tier. */
    const val CHAMFER_DEPTH_RATIO = 0.35f

    /** The single plate shape. There is no rounded corner and no circle in this direction. */
    val Plate: Shape = ChamferedPlate()

    // ---------------------------------------------------------------
    // Section 4 - Type: the inverse optical law
    // ---------------------------------------------------------------
    // Stroke-to-cap RISES as size falls. Display lettering is hairline and
    // quiet; microtext is comparatively fat and loud.
    //
    // DEPARTURE 3. A phone cannot hold the ladder's dynamic range. Desktop
    // spans 96:11 = 8.7x. A phone is capped by a 360dp frame and floored by
    // legibility at 11sp, giving 40:11 = 3.6x. Translating proportionally
    // puts Body at 12.66sp and Micro at 4.64sp, collapsing three ranks into
    // one. So: THE SIZE LADDER COMPRESSES, THE WEIGHT LADDER DOES NOT.
    // Roboto carries W100-W900 at every size, so the four ranks keep their
    // full weight spread and stroke/cap becomes a consequence of that
    // choice rather than a copied constant.
    //
    // rank     size   cap dp  % of frame  desktop target   weight
    // Display  40sp   28.4    7.90%       8.00%            W100
    // Title    28sp   19.9    5.53%       5.92%            W200
    // Body     15sp   10.7    2.96%       2.50%            W400
    // Micro    11sp    7.8    2.17%       0.92% (floored)  W700

    const val ROBOTO_CAP_RATIO = 0.711f

    /** Display tracking: inter-glyph gap ~ 0.33 x glyph width; Roboto glyph ~ 0.5em. */
    private const val DISPLAY_TRACKING_FACTOR = 0.165f // 0.33 * 0.5

    val Display = TextStyle(
        fontSize = 40.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.W100,
        letterSpacing = 6.6.sp // 40 * 0.165
    )

    val Title = TextStyle(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.W200,
        letterSpacing = 2.3.sp // 28 * 0.165 * 0.5, half-tracked below display rank
    )

    val Body = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp
    )

    /**
     * The floored rank. 11sp is the smallest size that survives 200% text
     * scaling and is confirmable by eye, so it is the bottom of the ladder
     * and therefore the heaviest.
     *
     * Catalogue section 8 also governs its content: real values only, and
     * word joins use underscores rather than spaces in system labels.
     * Placeholder strings that look like status are the easiest way to lie
     * to a user.
     */
    val Micro = TextStyle(
        fontSize = 11.sp,
        lineHeight = 12.65.sp, // leading 1.15
        fontWeight = FontWeight.W700,
        letterSpacing = 0.2.sp
    )

    // ---------------------------------------------------------------
    // sRGB helpers - the conformance gate carries a twin of these
    // ---------------------------------------------------------------

    /** Relative luminance of a neutral grey given its 8-bit channel value. */
    fun relativeLuminance(channel: Float): Float {
        val c = (channel / 255f).coerceIn(0f, 1f)
        return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    /** Inverse of [relativeLuminance]: the neutral channel value for a target luminance. */
    fun channelForRelativeLuminance(y: Float): Float {
        val yy = y.coerceIn(0f, 1f)
        val c = if (yy <= 0.0031308f) yy * 12.92f else 1.055f * yy.pow(1f / 2.4f) - 0.055f
        return c * 255f
    }

    /** Perceived luma of a colour, on the same 0-255 scale the catalogue uses. */
    fun luma(color: Color): Float =
        (0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue) * 255f

    /** Near-neutral test. The middle-band law governs greys, not saturated colour. */
    fun isNearNeutral(color: Color): Boolean {
        val r = color.red * 255f
        val g = color.green * 255f
        val b = color.blue * 255f
        return (max(r, max(g, b)) - min(r, min(g, b))) <= 24f
    }
}

/**
 * D-1, open: the catalogue does not fix WHICH corner is cut. This defaults
 * to the leading corner in reading order. Change it here, once, or it
 * becomes noise.
 */
enum class PlateCorner { TopStart, TopEnd, BottomStart, BottomEnd }

/**
 * The plate. Exactly one corner cut at 35.3 degrees from vertical; the
 * other three stay square. Never 45 degrees, never more than one corner.
 */
class ChamferedPlate(
    private val corner: PlateCorner = PlateCorner.TopStart,
    private val depthRatio: Float = PixelodyDirection.CHAMFER_DEPTH_RATIO,
    private val maxDropDp: Float = 24f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // The ANGLE is the law; the depth is a ratio. When a narrow plate
        // would push the horizontal leg past half its width, clamp the leg
        // and recompute the drop so 35.3 degrees is preserved exactly.
        val maxDropPx = with(density) { maxDropDp.dp.toPx() }
        var drop = (size.height * depthRatio).coerceAtMost(maxDropPx)
        var run = drop * PixelodyDirection.CHAMFER_SLOPE
        val maxRun = size.width * 0.5f
        if (run > maxRun) {
            run = maxRun
            drop = run / PixelodyDirection.CHAMFER_SLOPE
        }

        val rtl = layoutDirection == LayoutDirection.Rtl
        val resolved = when (corner) {
            PlateCorner.TopStart -> if (rtl) PlateCorner.TopEnd else PlateCorner.TopStart
            PlateCorner.TopEnd -> if (rtl) PlateCorner.TopStart else PlateCorner.TopEnd
            PlateCorner.BottomStart -> if (rtl) PlateCorner.BottomEnd else PlateCorner.BottomStart
            PlateCorner.BottomEnd -> if (rtl) PlateCorner.BottomStart else PlateCorner.BottomEnd
        }

        val w = size.width
        val h = size.height
        val path = Path().apply {
            when (resolved) {
                PlateCorner.TopStart -> {
                    moveTo(0f, drop); lineTo(run, 0f); lineTo(w, 0f)
                    lineTo(w, h); lineTo(0f, h)
                }
                PlateCorner.TopEnd -> {
                    moveTo(0f, 0f); lineTo(w - run, 0f); lineTo(w, drop)
                    lineTo(w, h); lineTo(0f, h)
                }
                PlateCorner.BottomEnd -> {
                    moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h - drop)
                    lineTo(w - run, h); lineTo(0f, h)
                }
                PlateCorner.BottomStart -> {
                    moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h)
                    lineTo(run, h); lineTo(0f, h - drop)
                }
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * The press state.
 *
 * Material's default ripple is a mid-grey radial - a middle-band violation
 * that fires on every tap, on every surface, thousands of times a session.
 * Under this direction the press state is an ink wash, never a grey.
 *
 * This is the one piece of this change that could not be compile-checked in
 * the authoring environment.
 * REVERT THIS ALONE: delete this object and drop the
 * `LocalIndication provides PixelodyPressIndication` line in PixelodyTheme.
 */
object PixelodyPressIndication : IndicationNodeFactory {

    private const val PRESS_ALPHA = 0.10f
    private const val FOCUS_ALPHA = 0.14f

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressNode(interactionSource)

    override fun hashCode(): Int = System.identityHashCode(this)

    override fun equals(other: Any?): Boolean = other === this

    private class PressNode(
        private val interactionSource: InteractionSource
    ) : Modifier.Node(), DrawModifierNode {

        private var pressed = false
        private var focused = false

        override fun onAttach() {
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> pressed = true
                        is PressInteraction.Release -> pressed = false
                        is PressInteraction.Cancel -> pressed = false
                        is FocusInteraction.Focus -> focused = true
                        is FocusInteraction.Unfocus -> focused = false
                    }
                    invalidateDraw()
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            val alpha = when {
                pressed -> PRESS_ALPHA
                focused -> FOCUS_ALPHA
                else -> 0f
            }
            if (alpha > 0f) {
                drawRect(color = PixelodyDirection.Ink.copy(alpha = alpha))
            }
        }
    }
}
