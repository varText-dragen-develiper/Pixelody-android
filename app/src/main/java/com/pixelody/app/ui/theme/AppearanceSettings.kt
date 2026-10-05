package com.pixelody.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Device-local overrides for shared surfaces and controls; authored theme motifs remain intact. */
enum class StylePalette(val label: String, val primary: String, val secondary: String, val base: String) {
    Theme("Theme default", "91A7FF", "6ED8DD", "0B0D12"),
    Gold("Studio Gold", "D8B66A", "65D6A1", "050607"),
    Aurora("Aurora", "65D9D1", "B99CFF", "05090B"),
    Ember("Ember", "FF9860", "FFCF86", "090605"),
    Violet("Ultraviolet", "C49BFF", "7CE4C8", "07050B"),
    Mono("Monochrome", "E4E2DC", "AEB2B7", "070707"),
    Cobalt("Cobalt Current", "64A8FF", "63E6D4", "040811"),
    Sakura("Sakura Night", "FF9FC9", "FFD37A", "0B060A"),
    Cafe("Café Night", "C58A4A", "7B866C", "15110F"),
    Verdant("Verdant Signal", "79DF8B", "D5C878", "050A07"),
    Custom("Custom colors", "91A7FF", "6ED8DD", "0B0D12")
}
enum class StyleTypeface(val label: String) { Theme("Follow theme"), Sans("Sans"), Serif("Serif"), Mono("Monospace") }

data class AppearanceSettings(
    val palette: StylePalette = StylePalette.Theme,
    val primary: String = "91A7FF",
    val secondary: String = "6ED8DD",
    val base: String = "0B0D12",
    val textPercent: Int = 100,
    val typeface: StyleTypeface = StyleTypeface.Theme,
    val highContrast: Boolean = false
) {
    fun normalized() = copy(
        primary = normalizeHex(primary)?.takeIf { isReadableAccent(it, normalizeHex(base)?.takeIf(::isReadableBase) ?: "0B0D12") } ?: "91A7FF",
        secondary = normalizeHex(secondary)?.takeIf { isReadableAccent(it, normalizeHex(base)?.takeIf(::isReadableBase) ?: "0B0D12") } ?: "6ED8DD",
        base = normalizeHex(base)?.takeIf { isReadableBase(it) } ?: "0B0D12",
        textPercent = textPercent.takeIf { it in listOf(100, 115, 130, 150) } ?: 100
    )
}

fun normalizeHex(raw: String): String? = raw.trim().removePrefix("#").uppercase().takeIf { it.matches(Regex("[0-9A-F]{6}")) }
fun hexColor(hex: String): Color = Color(0xFF000000L or hex.toLong(16))
fun isReadableBase(hex: String): Boolean = (1.05f / (hexColor(hex).luminance() + 0.05f)) >= 7f
fun colorContrast(a: Color, b: Color): Float = (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)
fun isReadableAccent(accent: String, base: String): Boolean = colorContrast(hexColor(accent), lerp(hexColor(base), Color.White, 0.07f)) >= 4.5f
fun readableAccentInk(accent: Color): Color = if (accent.luminance() > 0.179f) Color.Black else Color.White

/** Desktop Studio tokens originate in https://github.com/varText-dragen-develiper/pixelody/blob/main/src/studio-rebuild.css; presets in https://github.com/varText-dragen-develiper/pixelody/blob/main/src/renderer.js. */
fun AppearanceSettings.specFor(theme: PixelodyMobileTheme): PixelodyThemeSpec {
    val style = normalized()
    var spec = theme.spec
    if (theme == PixelodyMobileTheme.Studio) spec = spec.copy(
        groundTop = Color(0xFF0B0D12), groundBottom = Color(0xFF0B0D12),
        ink = Color(0xFFF5F6FA), panel = Color(0xFF171B24), panelDeep = Color(0xFF1B202B),
        primary = Color(0xFF91A7FF), onPrimary = Color.Black,
        secondary = Color(0xFF6ED8DD), onSecondary = Color.Black,
        tertiary = Color(0xFFA98CFF), onTertiary = Color.Black,
        traceHead = Color(0xFF313848), traceCeiling = Color(0xFF313848)
    )
    if (style.palette != StylePalette.Theme) {
        val custom = style.palette == StylePalette.Custom
        val primary = hexColor(if (custom) style.primary else style.palette.primary)
        val secondary = hexColor(if (custom) style.secondary else style.palette.secondary)
        val base = hexColor(if (custom) style.base else style.palette.base)
        spec = spec.copy(groundTop = base, groundBottom = base,
            panel = lerp(base, Color.White, 0.045f), panelDeep = lerp(base, Color.White, 0.07f),
            primary = primary, onPrimary = readableAccentInk(primary),
            secondary = secondary, onSecondary = readableAccentInk(secondary))
    }
    if (style.highContrast) spec = spec.copy(ink = Color.White,
        traceHead = Color(0xFF9CA7BD), traceCeiling = Color(0xFF9CA7BD))
    return spec
}
