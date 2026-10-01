package com.pixelody.app.core.playback

import com.pixelody.app.data.model.SoundMeshNode
import com.pixelody.app.data.model.SpatialSpeakerRole
import com.pixelody.app.data.model.Track

/**
 * SoundMeshEngine: Precision Time Protocol (PTP) synchronization,
 * multi-device spatial speaker channel assignment, and collaborative crowd queue consensus.
 */
object SoundMeshEngine {

    /**
     * Precision Time Protocol (IEEE 1588 PTP) clock offset calculation.
     * @param t1 Client send timestamp
     * @param t2 Server receive timestamp
     * @param t3 Server transmit timestamp
     * @param t4 Client receive timestamp
     * @return Clock offset in milliseconds between client and server
     */
    fun calculatePtpClockOffset(t1: Long, t2: Long, t3: Long, t4: Long): Long {
        return ((t2 - t1) + (t3 - t4)) / 2
    }

    /**
     * Precision Time Protocol round-trip one-way network propagation delay.
     */
    fun calculateOneWayDelay(t1: Long, t2: Long, t3: Long, t4: Long): Long {
        return ((t4 - t1) - (t3 - t2)) / 2
    }

    /**
     * Dynamically distributes spatial audio roles across all connected nodes in a room.
     */
    fun assignSpatialRoles(nodes: List<SoundMeshNode>): List<SoundMeshNode> {
        if (nodes.isEmpty()) return emptyList()
        if (nodes.size == 1) {
            return listOf(nodes[0].copy(assignedRole = SpatialSpeakerRole.FullStereo, isHostAuthority = true))
        }

        return nodes.mapIndexed { index, node ->
            val role = when (index) {
                0 -> SpatialSpeakerRole.LeftMain
                1 -> SpatialSpeakerRole.RightMain
                2 -> SpatialSpeakerRole.CenterSub
                3 -> SpatialSpeakerRole.VisualizerDisplay
                else -> SpatialSpeakerRole.FullStereo
            }
            node.copy(assignedRole = role, isHostAuthority = (index == 0))
        }
    }

    /**
     * Calculates the crowd consensus queue ranked by vote count from connected peer nodes.
     */
    fun calculateConsensusQueue(
        candidateTracks: List<Track>,
        nodeVotes: Map<String, String> // Map of deviceId to voted trackId
    ): List<Pair<Track, Int>> {
        val voteCounts = mutableMapOf<String, Int>()
        nodeVotes.values.forEach { trackId ->
            voteCounts[trackId] = (voteCounts[trackId] ?: 0) + 1
        }

        return candidateTracks.map { track ->
            track to (voteCounts[track.id] ?: 0)
        }.sortedByDescending { it.second }
    }
}
