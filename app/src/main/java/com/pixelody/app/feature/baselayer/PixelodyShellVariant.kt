package com.pixelody.app.feature.baselayer

/**
 * Which shell the app draws.
 * The Base Layer is the canonical, permanent architecture.
 */
enum class PixelodyShellVariant(val launchValue: String) {
    Base("base");

    companion object {
        const val IntentExtra = "pixelody.shell"

        fun fromLaunchValue(value: String?): PixelodyShellVariant = Base
    }
}

