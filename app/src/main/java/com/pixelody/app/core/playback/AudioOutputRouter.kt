package com.pixelody.app.core.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioDeviceRoute(val label: String, val tag: String, val description: String) {
    Speaker("Loudspeaker", "SPK", "Built-in device loudspeaker"),
    Wired("Wired Hi-Fi", "DAC", "3.5mm jack or USB-C DAC"),
    Bluetooth("Bluetooth", "BT", "Wireless audio stream"),
    Unknown("Audio Output", "OUT", "Default audio endpoint")
}

data class AudioRouteState(
    val currentRoute: AudioDeviceRoute = AudioDeviceRoute.Speaker,
    val deviceName: String = "Loudspeaker",
    val recommendedProfile: EqualizerProfile = EqualizerProfile(preset = EqualizerPreset.Flat),
    val spatialSoundstageWidth: Float = 0.5f, // 0.0 to 1.0
    val isOverrideActive: Boolean = false
)

/**
 * AudioOutputRouter: Monitors active audio output route (Wired Headset, Bluetooth, Loudspeaker)
 * and determines optimal mastering DSP profiles and stereo soundstage width.
 */
class AudioOutputRouter(
    private val context: Context,
    private val onRouteChanged: (AudioRouteState) -> Unit = {}
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _state = MutableStateFlow(AudioRouteState())
    val state: StateFlow<AudioRouteState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateCurrentRoute()
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        }
        runCatching {
            context.registerReceiver(receiver, filter)
        }
        updateCurrentRoute()
    }

    fun updateCurrentRoute() {
        val detected = detectRoute()
        val recommended = mapRouteToProfile(detected.first)
        val soundstage = mapRouteToSoundstage(detected.first)

        val newState = _state.value.copy(
            currentRoute = detected.first,
            deviceName = detected.second,
            recommendedProfile = recommended,
            spatialSoundstageWidth = soundstage
        )
        _state.value = newState
        onRouteChanged(newState)
    }

    fun overrideRoute(route: AudioDeviceRoute) {
        val recommended = mapRouteToProfile(route)
        val soundstage = mapRouteToSoundstage(route)
        val newState = _state.value.copy(
            currentRoute = route,
            deviceName = "${route.label} (Manual)",
            recommendedProfile = recommended,
            spatialSoundstageWidth = soundstage,
            isOverrideActive = true
        )
        _state.value = newState
        onRouteChanged(newState)
    }

    fun resetOverride() {
        _state.value = _state.value.copy(isOverrideActive = false)
        updateCurrentRoute()
    }

    fun release() {
        runCatching {
            context.unregisterReceiver(receiver)
        }
    }

    private fun detectRoute(): Pair<AudioDeviceRoute, String> {
        val am = audioManager ?: return AudioDeviceRoute.Speaker to "Loudspeaker"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        return AudioDeviceRoute.Wired to (device.productName.toString().takeIf { it.isNotBlank() } ?: "Wired Hi-Fi DAC")
                    }
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                        return AudioDeviceRoute.Bluetooth to (device.productName.toString().takeIf { it.isNotBlank() } ?: "Bluetooth Device")
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (am.isWiredHeadsetOn) return AudioDeviceRoute.Wired to "Wired Headset"
            @Suppress("DEPRECATION")
            if (am.isBluetoothA2dpOn) return AudioDeviceRoute.Bluetooth to "Bluetooth Wireless"
        }

        return AudioDeviceRoute.Speaker to "Phone Loudspeaker"
    }

    companion object {
        fun mapRouteToProfile(route: AudioDeviceRoute): EqualizerProfile {
            return when (route) {
                AudioDeviceRoute.Wired -> EqualizerProfile(
                    enabled = true,
                    preset = EqualizerPreset.Flat,
                    gainsDb = EqualizerPreset.Flat.gainsDb
                )
                AudioDeviceRoute.Bluetooth -> EqualizerProfile(
                    enabled = true,
                    preset = EqualizerPreset.Bass,
                    gainsDb = EqualizerPreset.Bass.gainsDb
                )
                AudioDeviceRoute.Speaker -> EqualizerProfile(
                    enabled = true,
                    preset = EqualizerPreset.Vocal,
                    gainsDb = EqualizerPreset.Vocal.gainsDb
                )
                AudioDeviceRoute.Unknown -> EqualizerProfile(
                    enabled = true,
                    preset = EqualizerPreset.Flat,
                    gainsDb = EqualizerPreset.Flat.gainsDb
                )
            }
        }

        fun mapRouteToSoundstage(route: AudioDeviceRoute): Float {
            return when (route) {
                AudioDeviceRoute.Wired -> 0.85f // Wide Hi-Fi soundstage
                AudioDeviceRoute.Bluetooth -> 0.65f // Balanced stereo
                AudioDeviceRoute.Speaker -> 0.20f // Focused center
                AudioDeviceRoute.Unknown -> 0.50f
            }
        }
    }
}
