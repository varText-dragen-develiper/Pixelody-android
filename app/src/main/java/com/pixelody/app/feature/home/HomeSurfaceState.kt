package com.pixelody.app.feature.home

import com.pixelody.app.core.analytics.SessionSoundInsights
import com.pixelody.app.core.playback.AudioDeviceRoute
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.data.model.SonicMood
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.storage.CircadianPhase
import com.pixelody.app.data.storage.HabitActionType

/**
 * The 4 Living Operational Modes of the Home Surface.
 */
enum class HomeOperationalMode {
    HorizonCue,        // Mode 1: Cold start / Idle intent initiation (<1s to music)
    ActiveGroove,      // Mode 2: Playback in progress session momentum hub
    DiscoveryCrates,   // Mode 3: Curatorial exploration / digging
    HardwareAnchored   // Mode 4: USB DAC / Car Bluetooth / Offline focus
}

/**
 * Contextual quick path button model.
 */
data class HabitQuickPath(
    val actionType: HabitActionType,
    val title: String,
    val subtitle: String,
    val weight: Float,
    val isPrimary: Boolean = false
)

/**
 * Hardware and environment state presentation data.
 */
data class HardwareAnchorState(
    val isUsbDac: Boolean = false,
    val isCarBluetooth: Boolean = false,
    val isOffline: Boolean = false,
    val isMeshActive: Boolean = false,
    val title: String = "",
    val badge: String = "",
    val description: String = ""
)

/**
 * Complete immutable snapshot of the dynamic Home Surface state.
 */
data class DynamicHomeSurfaceState(
    val operationalMode: HomeOperationalMode,
    val circadianPhase: CircadianPhase,
    val greetingTitle: String,
    val greetingSubtitle: String,
    val heroTitle: String,
    val heroSubtitle: String,
    val heroBadge: String,
    val heroActionText: String,
    val capsule: DailySonicCapsule?,
    val sessionInsights: SessionSoundInsights?,
    val hardwareAnchor: HardwareAnchorState?,
    val activeTrack: Track?,
    val nextTrack: Track?,
    val harmonicNextKey: String?,
    val quickPaths: List<HabitQuickPath>,
    val showActiveGrooveConsole: Boolean,
    val showHardwareBanner: Boolean
)
