package com.pixelody.app.ui.brand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.pixelody.app.ui.theme.AppearanceSettings
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import com.pixelody.app.ui.theme.specFor

/** Generated artwork colors mirror effective theme/preset accents. Manual choices stay separate. */
enum class ThemeLauncherIcon(val key: String, val color: Color, val aliasName: String) {
    Studio("theme_studio", Color(0xFF91A7FF), ".LauncherThemeStudio"),
    Quest("theme_quest", Color(0xFF7B68C9), ".LauncherThemeQuest"),
    Glass("theme_glass", Color(0xFF9FDCFF), ".LauncherThemeGlass"),
    Cafe("theme_cafe", Color(0xFFC48A4A), ".LauncherThemeCafe"),
    Terminal("theme_terminal", Color(0xFFF6C945), ".LauncherThemeTerminal"),
    Obsession("theme_obsession", Color(0xFFFF5865), ".LauncherThemeObsession"),
    Gold("theme_gold", Color(0xFFD8B66A), ".LauncherThemeGold"),
    Aurora("theme_aurora", Color(0xFF65D9D1), ".LauncherThemeAurora"),
    Ember("theme_ember", Color(0xFFFF9860), ".LauncherThemeEmber"),
    Violet("theme_violet", Color(0xFFC49BFF), ".LauncherThemeViolet"),
    Mono("theme_mono", Color(0xFFE4E2DC), ".LauncherThemeMono"),
    Cobalt("theme_cobalt", Color(0xFF64A8FF), ".LauncherThemeCobalt"),
    Sakura("theme_sakura", Color(0xFFFF9FC9), ".LauncherThemeSakura"),
    CafeNight("theme_cafenight", Color(0xFFC58A4A), ".LauncherThemeCafeNight"),
    Verdant("theme_verdant", Color(0xFF79DF8B), ".LauncherThemeVerdant");
}

data class LogoPreferences(val color: PixelodyLogoColor, val followsTheme: Boolean)
data class LauncherIcon(val aliasName: String, val color: Color)

val launcherIcons: List<LauncherIcon> = PixelodyLogoColor.entries.map { LauncherIcon(it.aliasName, it.color) } +
    ThemeLauncherIcon.entries.map { LauncherIcon(it.aliasName, it.color) }

fun resolveLauncherIcon(preferences: LogoPreferences, theme: PixelodyMobileTheme, appearance: AppearanceSettings): LauncherIcon {
    if (!preferences.followsTheme) return LauncherIcon(preferences.color.aliasName, preferences.color.color)
    val accent = appearance.specFor(theme).primary.toArgb()
    // Exact for every authored theme/preset. Arbitrary custom RGB uses the closest baked shade.
    return launcherIcons.minBy { colorDistance(accent, it.color.toArgb()) }
}

private fun colorDistance(a: Int, b: Int): Long {
    val ar = a ushr 16 and 255; val br = b ushr 16 and 255
    val redMean = (ar + br) / 2
    val dr = ar - br; val dg = (a ushr 8 and 255) - (b ushr 8 and 255); val db = (a and 255) - (b and 255)
    return ((512L + redMean) * dr * dr / 256) + 4L * dg * dg + ((767L - redMean) * db * db / 256)
}
