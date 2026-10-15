package com.pixelody.app.ui.brand

import androidx.compose.ui.graphics.Color

/**
 * The logo's purple range. Mirrors https://github.com/varText-dragen-develiper/pixelody/blob/main/src/brand-mark.js and
 * https://github.com/varText-dragen-develiper/pixelody/blob/main/tools/brand/logo-colors.json on desktop; `npm run check` fails if the key
 * or hex of any entry drifts. [aliasName] names the launcher activity-alias
 * in AndroidManifest.xml that carries this colour's icon.
 */
enum class PixelodyLogoColor(
    val key: String,
    val displayName: String,
    val color: Color,
    val aliasName: String
) {
    Ultraviolet("ultraviolet", "Ultraviolet", Color(0xFF6A3DFF), ".LauncherLogoUltraviolet"),
    Electric("electric", "Electric", Color(0xFF7C5CFF), ".LauncherLogoElectric"),
    Indigo("indigo", "Indigo", Color(0xFF5B5BFF), ".LauncherLogoIndigo"),
    Grape("grape", "Grape", Color(0xFF8A3CF0), ".LauncherLogoGrape"),
    Lavender("lavender", "Lavender", Color(0xFFA98CFF), ".LauncherLogoLavender");

    companion object {
        val Default = Ultraviolet

        fun fromKey(key: String?): PixelodyLogoColor = entries.firstOrNull { it.key == key } ?: Default
    }
}
