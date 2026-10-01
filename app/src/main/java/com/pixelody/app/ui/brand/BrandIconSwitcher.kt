package com.pixelody.app.ui.brand

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Switches the home-screen icon by enabling exactly one launcher
 * activity-alias (one per [PixelodyLogoColor]) and disabling the rest.
 *
 * The chosen alias is enabled before the others are disabled, so there is
 * never a moment with no launcher entry. DONT_KILL_APP keeps the app running;
 * some launchers still take a few seconds to redraw the icon, and a few move
 * the home-screen shortcut, which is how every alias-based icon picker behaves.
 */
object BrandIconSwitcher {
    fun apply(context: Context, chosen: PixelodyLogoColor) {
        val packageManager = context.packageManager
        val packageName = context.packageName
        fun component(color: PixelodyLogoColor) = ComponentName(packageName, packageName + color.aliasName)

        val chosenComponent = component(chosen)
        if (packageManager.getComponentEnabledSetting(chosenComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            packageManager.setComponentEnabledSetting(
                chosenComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        PixelodyLogoColor.entries.filter { it != chosen }.forEach { other ->
            val otherComponent = component(other)
            if (packageManager.getComponentEnabledSetting(otherComponent) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                packageManager.setComponentEnabledSetting(
                    otherComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
