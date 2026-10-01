package com.pixelody.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import com.pixelody.app.ui.theme.groundColors
import com.pixelody.app.ui.theme.PixelodyDirection
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import com.pixelody.app.ui.theme.ObsessionPalette
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Pure function to derive a pair of harmonic ambient aura colors from track metadata or theme defaults.
 */
fun computeHarmonicAuraColors(
    track: Track?,
    defaultPrimary: Color,
    defaultSecondary: Color
): Pair<Color, Color> {
    if (track == null) return Pair(defaultPrimary, defaultSecondary)

    val seed = abs((track.id + track.title + track.artist + track.album).hashCode())
    val hue1 = (seed % 360).toFloat()
    val hue2 = ((seed / 360 + 130) % 360).toFloat() // Complementary offset (~130 deg)

    val primaryAura = if (track.lossless) {
        Color(0xFFE5A93C) // Lossless warm amber gold signature
    } else {
        colorFromHsv(hue1, 0.72f, 0.88f)
    }

    val secondaryAura = colorFromHsv(hue2, 0.65f, 0.80f)
    return Pair(primaryAura, secondaryAura)
}

/**
 * Pure function to compute the breathing aura opacity based on playback state and oscillation phase.
 */
fun computeAuraBreathingAlpha(
    isPlaying: Boolean,
    cyclePhase: Float
): Float {
    return if (isPlaying) {
        val sineWave = (sin(cyclePhase) * 0.5f + 0.5f) // [0.0, 1.0]
        0.15f + sineWave * 0.13f // Breathes between 0.15 and 0.28
    } else {
        0.12f // Static subtle resting glow
    }
}

/**
 * Pure function to calculate parallax Y position.
 */
fun computeParallaxOrigin(
    baseY: Float,
    scrollOffsetPx: Float,
    parallaxFactor: Float = 0.12f
): Float {
    return baseY - scrollOffsetPx * parallaxFactor
}

/**
 * Helper to construct a Compose Color from HSV values.
 */
private fun colorFromHsv(hue: Float, saturation: Float, value: Float): Color {
    val h = (hue % 360 + 360) % 360 / 60f
    val c = value * saturation
    val x = c * (1 - abs(h % 2 - 1))
    val m = value - c

    val (r, g, b) = when (h.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

/**
 * Progressive Depth Horizon: Multi-Orb Atmospheric Ambient Aura Background with Spatial Parallax.
 */
@Composable
fun ProgressiveDepthHorizon(
    track: Track?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    parallaxOffsetY: Float = 0f,
    content: @Composable () -> Unit
) {
    // The ground follows the selected theme. Bound to the viewport, never to
    // content height - a ramp that follows content becomes a moving gradient
    // under scroll, which is a different asset with a different meaning.
    val theme = LocalPixelodyThemeVariant.current
    val groundRamp = theme.groundColors()

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = groundRamp,
                    startY = 0f,
                    endY = if (height > 0f) height else 1f
                )
            )

            if (width > 0f && height > 0f) {
                val center = Offset(
                    x = width * 0.28f,
                    y = computeParallaxOrigin(height * 0.17f, parallaxOffsetY)
                )
                val radius = (width * 0.33f)
                    .coerceAtLeast(PixelodyDirection.BloomMinDiameter.toPx() / 2f)

                drawCircle(
                    brush = Brush.radialGradient(
                        *boundedBodyStops(),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius,
                    alpha = BOUNDED_BODY_ALPHA
                )

                if (theme == PixelodyMobileTheme.Obsession) {
                    val crimsonCenter = Offset(width * 0.5f, height * 0.85f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ObsessionPalette.Signal.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = crimsonCenter,
                            radius = width * 0.7f
                        ),
                        center = crimsonCenter,
                        radius = width * 0.7f
                    )
                }
            }
        }

        if (theme == PixelodyMobileTheme.BulkheadTerminal) {
            ScanlineOverlay(
                modifier = Modifier.fillMaxSize(),
                lineColor = Color(0x1078F09A),
                spacingDp = 3.dp
            )
        } else if (theme == PixelodyMobileTheme.CartridgeQuest) {
            ScanlineOverlay(
                modifier = Modifier.fillMaxSize(),
                lineColor = Color(0x0C000000),
                spacingDp = 4.dp
            )
        }

        content()
    }
}

private const val BOUNDED_BODY_ALPHA = 0.26f
private const val BODY_PEAK_RADIUS = 0.82f
private const val BODY_PEAK_LUMA = 230f
private const val BODY_FALLOFF_EXPONENT = 1.70f

private fun boundedBodyStops(steps: Int = 24): Array<Pair<Float, Color>> =
    Array(steps + 1) { i ->
        val t = i / steps.toFloat()
        val luma = if (t <= BODY_PEAK_RADIUS) {
            1f + (BODY_PEAK_LUMA - 1f) * (t / BODY_PEAK_RADIUS)
        } else {
            val remaining = ((1f - t) / (1f - BODY_PEAK_RADIUS)).coerceIn(0f, 1f)
            BODY_PEAK_LUMA * remaining.pow(BODY_FALLOFF_EXPONENT)
        }
        val u = (luma / 255f).coerceIn(0f, 1f)
        t to Color(
            red = u.pow(0.074f),
            green = u.pow(0.553f),
            blue = u.pow(0.414f),
            alpha = u
        )
    }
