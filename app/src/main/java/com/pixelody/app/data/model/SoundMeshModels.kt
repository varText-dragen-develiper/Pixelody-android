package com.pixelody.app.data.model

/**
 * Spatial acoustic role assigned to a connected device in a multi-speaker Sound Mesh.
 */
enum class SpatialSpeakerRole(val displayName: String, val description: String, val tag: String) {
    FullStereo("Full Stereo", "Standalone stereo full-range driver", "STEREO"),
    LeftMain("Left Channel", "Primary left stereo speaker", "LEFT"),
    RightMain("Right Channel", "Primary right stereo speaker", "RIGHT"),
    CenterSub("Center / Subwoofer", "Low-end sub bass and vocal fill", "SUB/CTR"),
    VisualizerDisplay("Visualizer Display", "Dedicated CRT phosphor / soundstage visualizer", "DISPLAY")
}

/**
 * Connected device node in a distributed Sound Mesh room.
 */
data class SoundMeshNode(
    val deviceId: String,
    val deviceName: String,
    val assignedRole: SpatialSpeakerRole = SpatialSpeakerRole.FullStereo,
    val ptpClockOffsetMs: Long = 0L,
    val networkLatencyMs: Long = 4L,
    val isHostAuthority: Boolean = false,
    val votedTrackId: String? = null
)

/**
 * Complete real-time state of a multi-device Sound Mesh session.
 */
data class SoundMeshSessionState(
    val sessionId: String,
    val sessionName: String,
    val connectedNodes: List<SoundMeshNode> = emptyList(),
    val isSynchronized: Boolean = true,
    val averageNetworkJitterMs: Float = 0.8f
)
