package com.pixelody.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.pixelody.app.data.model.ImageSurface
import com.pixelody.app.data.model.ImageTypeface
import com.pixelody.app.data.model.ScreenImageStyle
import kotlin.math.abs
import kotlin.math.roundToInt

data class ImageStyleAnalysis(
    val brightness: Float,
    val complexity: Float,
    val palettes: List<ScreenImageStyle>,
    val recommendedOpacity: Float
)

/** Bounded, deterministic pixel analysis. Accepts at most a small decoded image sample. */
object ImageStyleEngine {
    fun analyze(pixels: IntArray, width: Int): ImageStyleAnalysis {
        require(pixels.isNotEmpty() && width > 0 && pixels.size <= 16384)
        val bins = FloatArray(24)
        var brightness = 0f
        var changes = 0f
        var comparisons = 0
        var chroma = 0f
        val lumas = FloatArray(pixels.size)
        pixels.forEachIndexed { index, pixel ->
            val r = ((pixel shr 16) and 255) / 255f
            val g = ((pixel shr 8) and 255) / 255f
            val b = (pixel and 255) / 255f
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min
            val saturation = if (max == 0f) 0f else delta / max
            val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
            lumas[index] = luma
            brightness += luma
            chroma += saturation
            if (index % width > 0) { changes += abs(luma - lumas[index - 1]); comparisons++ }
            if (index >= width) { changes += abs(luma - lumas[index - width]); comparisons++ }
            if (saturation > 0.15f && max > 0.12f) {
                val hue = when (max) {
                    r -> ((g - b) / delta + 6f) % 6f
                    g -> (b - r) / delta + 2f
                    else -> (r - g) / delta + 4f
                } * 60f
                bins[(hue / 15f).toInt().coerceIn(0, 23)] += saturation * max
            }
        }
        brightness /= pixels.size
        val complexity = if (comparisons == 0) 0f else (changes / comparisons * 2f).coerceIn(0f, 1f)
        val dominant = bins.indices.maxByOrNull { bins[it] } ?: 14
        val hue = if (bins[dominant] == 0f) 220f else dominant * 15f + 7.5f
        val saturation = if (chroma / pixels.size < 0.12f) 0.08f else 0.42f
        val recommendedSurface = when {
            brightness > 0.7f || complexity > 0.3f -> ImageSurface.Crisp
            brightness < 0.3f && complexity < 0.15f -> ImageSurface.Soft
            else -> ImageSurface.Balanced
        }
        // Busy images need calmer typography; soft fields can carry editorial headings.
        val type = if (complexity < 0.12f && saturation > 0.1f) ImageTypeface.Serif else ImageTypeface.Sans
        fun recipe(h: Float, s: Float) = ScreenImageStyle(
            primary = readable(hsl(h, s, 0.74f)),
            secondary = readable(hsl((h + 35f) % 360f, s * 0.65f, 0.78f)),
            base = hex(hsl(h, s * 0.45f, 0.045f)),
            typeface = type, surface = recommendedSurface
        )
        return ImageStyleAnalysis(brightness, complexity,
            listOf(recipe(hue, saturation), recipe((hue + 180f) % 360f, saturation * 0.8f), recipe(hue, 0.03f)),
            when { brightness > 0.7f || complexity > 0.3f -> 0.45f; brightness < 0.3f -> 0.75f; else -> 0.6f })
    }

    private fun readable(color: Color): String {
        var result = color
        repeat(12) {
            // Conservative upper bound for white imagery under the lightest allowed scrim.
            if (colorContrast(result, Color(0xFF4D4D4D)) < 4.5f) result = lerp(result, Color.White, 0.1f)
        }
        return hex(result)
    }

    private fun hex(color: Color): String = "%06X".format(color.toArgb() and 0xFFFFFF)

    private fun hsl(hue: Float, saturation: Float, light: Float): Color {
        val c = (1 - abs(2 * light - 1)) * saturation
        val h = hue / 60
        val x = c * (1 - abs(h % 2 - 1))
        val (r, g, b) = when {
            h < 1 -> Triple(c, x, 0f)
            h < 2 -> Triple(x, c, 0f)
            h < 3 -> Triple(0f, c, x)
            h < 4 -> Triple(0f, x, c)
            h < 5 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = light - c / 2
        return Color(((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }
}

fun AppearanceSettings.withImageStyle(image: ScreenImageStyle): AppearanceSettings = copy(
    palette = StylePalette.Custom, primary = image.primary, secondary = image.secondary, base = image.base,
    typeface = when (image.typeface) {
        ImageTypeface.Sans -> StyleTypeface.Sans
        ImageTypeface.Serif -> StyleTypeface.Serif
        ImageTypeface.Mono -> StyleTypeface.Mono
    }
).normalized()
