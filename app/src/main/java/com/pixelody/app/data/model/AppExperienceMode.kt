package com.pixelody.app.data.model

/**
 * Operating experience modes for Pixelody:
 * - Essential: Ultra-streamlined, high abstraction, Spotify-like simplicity with zero DSP clutter.
 * - Studio: Full audiophile deep-dive with 10 DSP cabinets, telemetry, and parametric digging tools.
 */
enum class AppExperienceMode(val label: String, val badge: String, val description: String) {
    Essential(
        label = "Essential",
        badge = "⚡",
        description = "Streamlined, minimal & automated"
    ),
    Studio(
        label = "Studio",
        badge = "✦",
        description = "Audiophile DSP, telemetry & racks"
    );

    fun toggle(): AppExperienceMode = when (this) {
        Essential -> Studio
        Studio -> Essential
    }
}
