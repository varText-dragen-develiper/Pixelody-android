package com.pixelody.app.ui.brand

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.pixelody.app.MainActivity

/** Keeps one launcher entry, updating atomically on API 33+ without killing playback. */
object BrandIconSwitcher {
    fun apply(context: Context, chosen: PixelodyLogoColor) = apply(context, LauncherIcon(chosen.aliasName, chosen.color))

    fun component(context: Context, icon: LauncherIcon) = ComponentName(
        context.packageName, MainActivity::class.java.name.substringBeforeLast('.') + icon.aliasName
    )

    fun apply(context: Context, chosen: LauncherIcon) {
        val manager = context.packageManager
        val changes = launcherIcons.sortedBy { if (it == chosen) 0 else 1 }.mapNotNull { icon ->
            val state = if (icon == chosen) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            val component = component(context, icon)
            if (manager.getComponentEnabledSetting(component) == state) null else component to state
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (changes.isNotEmpty()) manager.setComponentEnabledSettings(changes.map { (component, state) ->
                PackageManager.ComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            })
        } else {
            // Enable the replacement before disabling others on older Android versions.
            changes.forEach { (component, state) -> manager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP) }
        }
    }
}
