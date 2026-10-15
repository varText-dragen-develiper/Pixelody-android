package com.pixelody.app.data.model

/** A local, per-screen Studio recipe. No picker grants or bitmap data are persisted. */
enum class ImageTypeface(val label: String) { Sans("Clean"), Serif("Editorial"), Mono("Technical") }
enum class ImageSurface(val label: String, val shade: Float) {
    Soft("Soft", 0.78f), Balanced("Balanced", 0.82f), Crisp("Crisp", 0.86f)
}

data class ScreenImageStyle(
    val primary: String,
    val secondary: String,
    val base: String,
    val typeface: ImageTypeface = ImageTypeface.Sans,
    val surface: ImageSurface = ImageSurface.Balanced
) {
    fun encoded(): String = listOf(primary, secondary, base, typeface.name, surface.name).joinToString(",")

    companion object {
        fun decode(raw: String?): ScreenImageStyle? {
            val fields = raw?.split(',') ?: return null
            if (fields.size != 5 || fields.take(3).any { !it.matches(Regex("[0-9A-F]{6}")) }) return null
            val type = ImageTypeface.entries.firstOrNull { it.name == fields[3] } ?: return null
            val surface = ImageSurface.entries.firstOrNull { it.name == fields[4] } ?: return null
            return ScreenImageStyle(fields[0], fields[1], fields[2], type, surface)
        }
    }
}
