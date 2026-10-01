package com.pixelody.app.core.playback

import com.pixelody.app.data.model.JamDiagnostics
import com.pixelody.app.data.model.JamGuestPolicy
import com.pixelody.app.data.model.JamParticipant
import com.pixelody.app.data.model.JamRole
import com.pixelody.app.data.model.JamSession
import com.pixelody.app.data.model.JamSyncMode
import com.pixelody.app.data.model.JamSyncStatus
import com.pixelody.app.data.model.QueueItem
import com.pixelody.app.data.model.Track
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Coordinates J.A.M. (Joint Audio Mesh) session lifecycle, federated collaborative queue,
 * guest policy enforcement, voting mechanics, and multi-room synchronization.
 */
class JamSessionCoordinator(
    private val localDeviceId: String = UUID.randomUUID().toString().take(12),
    private val localDeviceName: String = "Pixelody Android",
    val clockSyncEngine: JamClockSyncEngine = JamClockSyncEngine()
) {
    private val _session = MutableStateFlow(
        JamSession(
            active = false,
            sessionId = "",
            mode = "mesh",
            status = "inactive",
            permissions = JamGuestPolicy(
                guestsCanView = true,
                guestsCanSuggest = true,
                guestsCanQueue = true,
                guestsCanEditQueue = false,
                guestsCanControlPlayback = false,
                federatedSourcesEnabled = true
            ),
            currentParticipant = null,
            participants = emptyList(),
            queue = emptyList(),
            diagnostics = JamDiagnostics()
        )
    )
    val session: StateFlow<JamSession> = _session.asStateFlow()

    private val _syncStatus = MutableStateFlow(JamSyncStatus())
    val syncStatus: StateFlow<JamSyncStatus> = _syncStatus.asStateFlow()

    val currentRole: JamRole
        get() {
            val participant = _session.value.currentParticipant ?: return JamRole.Guest
            return JamRole.fromId(participant.role)
        }

    val isHost: Boolean
        get() = currentRole == JamRole.Host

    val isDj: Boolean
        get() = currentRole == JamRole.Host || currentRole == JamRole.Dj || _session.value.activeDjDeviceId == localDeviceId

    /**
     * Starts hosting a new J.A.M. session locally.
     */
    fun startHosting(
        roomName: String = "Pixelody Mesh",
        policy: JamGuestPolicy = JamGuestPolicy(
            guestsCanView = true,
            guestsCanSuggest = true,
            guestsCanQueue = true,
            guestsCanEditQueue = false,
            guestsCanControlPlayback = false,
            federatedSourcesEnabled = true
        ),
        initialQueue: List<Track> = emptyList()
    ): JamSession {
        val sessionId = "jam-${UUID.randomUUID().toString().take(8)}"
        val joinCode = generateJoinCode()

        val hostParticipant = JamParticipant(
            deviceId = localDeviceId,
            name = "$localDeviceName (Host)",
            role = JamRole.Host.id,
            status = "active",
            permissions = listOf("all", "admin", "playback", "queue", "policy"),
            pingMs = 0L,
            isLocalDevice = true
        )

        val queueItems = initialQueue.mapIndexed { index, track ->
            track.toQueueItem(
                addedByDeviceId = localDeviceId,
                addedByName = localDeviceName,
                playbackStatus = if (index == 0) "playing" else "queued"
            )
        }

        val newSession = JamSession(
            active = true,
            sessionId = sessionId,
            mode = "mesh-coordinated",
            status = "active",
            permissions = policy,
            currentParticipant = hostParticipant,
            participants = listOf(hostParticipant),
            queue = queueItems,
            diagnostics = JamDiagnostics(
                participantCount = 1,
                pendingParticipants = 0,
                unavailableQueueItems = 0,
                lastIssue = "",
                recommendedAction = "none"
            ),
            joinCode = joinCode,
            activeDjDeviceId = localDeviceId
        )

        clockSyncEngine.setMode(JamSyncMode.CoordinatedHost)
        _session.value = newSession
        updateSyncDiagnostics(0L, 0L)
        return newSession
    }

    /**
     * Joins an existing J.A.M. session.
     */
    fun joinSession(
        session: JamSession,
        asRole: JamRole = JamRole.Guest
    ) {
        if (session.mode == "single-host") {
            updateFromLiveState(session)
            return
        }
        val participant = JamParticipant(
            deviceId = localDeviceId,
            name = localDeviceName,
            role = asRole.id,
            status = "active",
            permissions = permissionsForRole(asRole, session.permissions),
            pingMs = clockSyncEngine.getRoundTripTimeMs(),
            isLocalDevice = true
        )

        val updatedParticipants = (session.participants.filterNot { it.deviceId == localDeviceId } + participant)

        clockSyncEngine.setMode(
            if (asRole == JamRole.Listener) JamSyncMode.SynchronizedListener else JamSyncMode.Standalone
        )

        _session.value = session.copy(
            currentParticipant = participant,
            participants = updatedParticipants,
            diagnostics = session.diagnostics.copy(
                participantCount = updatedParticipants.size
            )
        )
    }

    /**
     * Updates from live state received via WebSocket or Polling.
     */
    fun updateFromLiveState(jamSession: JamSession?) {
        if (jamSession?.mode == "single-host" || _session.value.mode == "single-host") {
            if (jamSession == null || !jamSession.active) {
                leaveSession()
            } else {
                // The Windows host assigns device identity, role, approval, and permissions.
                // Never reconstruct them from this coordinator's unrelated local UUID.
                _session.value = jamSession
                clockSyncEngine.setMode(JamSyncMode.Standalone)
            }
            return
        }
        if (jamSession == null) {
            if (_session.value.active && !isHost) {
                // Remote session ended
                _session.update { it.copy(active = false, status = "disconnected") }
                clockSyncEngine.setMode(JamSyncMode.Standalone)
            }
            return
        }

        _session.update { current ->
            val participant = jamSession.participants.firstOrNull { it.deviceId == localDeviceId }
                ?: current.currentParticipant
                ?: JamParticipant(
                    deviceId = localDeviceId,
                    name = localDeviceName,
                    role = JamRole.Guest.id,
                    status = "active",
                    isLocalDevice = true
                )

            jamSession.copy(
                currentParticipant = participant,
                participants = jamSession.participants.map {
                    if (it.deviceId == localDeviceId) it.copy(isLocalDevice = true) else it
                }
            )
        }
    }

    /**
     * Leaves the current session and resets state.
     */
    fun leaveSession() {
        clockSyncEngine.setMode(JamSyncMode.Standalone)
        clockSyncEngine.reset()
        _session.value = JamSession(
            active = false,
            sessionId = "",
            mode = "mesh",
            status = "inactive",
            permissions = JamGuestPolicy(),
            currentParticipant = null,
            participants = emptyList(),
            queue = emptyList(),
            diagnostics = JamDiagnostics()
        )
        _syncStatus.value = JamSyncStatus()
    }

    // ==========================================
    // Guest Policy & Permission Checks
    // ==========================================

    private fun singleHostPermission(permission: String): Boolean {
        val state = _session.value
        return state.active && state.currentParticipant?.status == "active"
            && state.currentParticipant.permissions.contains(permission)
    }

    fun canSuggestTracks(): Boolean {
        if (_session.value.mode == "single-host") return singleHostPermission("suggest")
        if (!_session.value.active) return false
        if (isHost || isDj) return true
        return _session.value.permissions.guestsCanSuggest
    }

    fun canDirectQueue(): Boolean {
        if (_session.value.mode == "single-host") return singleHostPermission("add")
        if (!_session.value.active) return false
        if (isHost || isDj) return true
        return _session.value.permissions.guestsCanQueue
    }

    fun canEditQueue(): Boolean {
        if (_session.value.mode == "single-host") return singleHostPermission("editQueue")
        if (!_session.value.active) return false
        if (isHost || isDj) return true
        return _session.value.permissions.guestsCanEditQueue
    }

    fun canControlPlayback(): Boolean {
        if (_session.value.mode == "single-host") return singleHostPermission("controlPlayback")
        if (!_session.value.active) return false
        if (isHost || isDj) return true
        return _session.value.permissions.guestsCanControlPlayback
    }

    fun canShareFederatedSource(): Boolean {
        if (_session.value.mode == "single-host") return false
        if (!_session.value.active) return false
        if (isHost) return true
        return _session.value.permissions.federatedSourcesEnabled
    }

    // ==========================================
    // Collaborative Queue & Voting Mechanics
    // ==========================================

    /**
     * Adds a track to the collaborative queue either as direct item or as a suggestion.
     */
    fun queueTrack(
        track: Track,
        asSuggestion: Boolean = !canDirectQueue() && canSuggestTracks(),
        sourceDeviceName: String = localDeviceName
    ): QueueItem? {
        if (!canSuggestTracks() && !canDirectQueue()) return null

        val queueItem = track.toQueueItem(
            addedByDeviceId = localDeviceId,
            addedByName = localDeviceName,
            sourceDeviceName = sourceDeviceName,
            isSuggestion = asSuggestion,
            votes = if (asSuggestion) 1 else 0,
            upvotedBy = if (asSuggestion) listOf(localDeviceId) else emptyList()
        )

        _session.update { current ->
            val updatedQueue = current.queue + queueItem
            current.copy(
                queue = updatedQueue,
                diagnostics = current.diagnostics.copy(
                    unavailableQueueItems = updatedQueue.count { it.availability != "available" }
                )
            )
        }
        return queueItem
    }

    /**
     * Casts an upvote or downvote for a track in the collaborative suggestion queue.
     * @param voteType +1 for upvote, -1 for downvote, 0 to clear
     */
    fun voteForTrack(queueItemId: String, voteType: Int): JamSession {
        _session.update { current ->
            val updatedQueue = current.queue.map { item ->
                if (item.queueItemId != queueItemId) return@map item

                val currentUpvoters = item.upvotedBy.toMutableSet()
                val currentDownvoters = item.downvotedBy.toMutableSet()

                when (voteType) {
                    1 -> {
                        currentUpvoters.add(localDeviceId)
                        currentDownvoters.remove(localDeviceId)
                    }
                    -1 -> {
                        currentDownvoters.add(localDeviceId)
                        currentUpvoters.remove(localDeviceId)
                    }
                    else -> {
                        currentUpvoters.remove(localDeviceId)
                        currentDownvoters.remove(localDeviceId)
                    }
                }

                val totalVotes = currentUpvoters.size - currentDownvoters.size

                // Auto-promotion rule: if votes >= 3 or more than half participants upvoted, promote
                val shouldAutoPromote = item.isSuggestion && (totalVotes >= 3 || totalVotes >= (current.participants.size / 2).coerceAtLeast(2))

                item.copy(
                    votes = totalVotes,
                    upvotedBy = currentUpvoters.toList(),
                    downvotedBy = currentDownvoters.toList(),
                    isSuggestion = if (shouldAutoPromote) false else item.isSuggestion
                )
            }
            current.copy(queue = updatedQueue)
        }
        return _session.value
    }

    /**
     * Promotes a suggestion directly into the active playback queue (DJ / Host only).
     */
    fun promoteSuggestion(queueItemId: String): Boolean {
        if (!isHost && !isDj) return false
        _session.update { current ->
            val updatedQueue = current.queue.map { item ->
                if (item.queueItemId == queueItemId) item.copy(isSuggestion = false) else item
            }
            current.copy(queue = updatedQueue)
        }
        return true
    }

    /**
     * Reorders an item in the queue.
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int): Boolean {
        if (!canEditQueue()) return false
        _session.update { current ->
            val queue = current.queue.toMutableList()
            if (fromIndex !in queue.indices || toIndex !in queue.indices) return@update current
            val item = queue.removeAt(fromIndex)
            queue.add(toIndex, item)
            current.copy(queue = queue)
        }
        return true
    }

    /**
     * Removes an item from the queue.
     */
    fun removeQueueItem(queueItemId: String): Boolean {
        if (!canEditQueue() && !isHost && !isDj) {
            // Guests can only remove tracks they personally added
            val item = _session.value.queue.firstOrNull { it.queueItemId == queueItemId }
            if (item == null || item.addedByDeviceId != localDeviceId) return false
        }
        _session.update { current ->
            val updatedQueue = current.queue.filterNot { it.queueItemId == queueItemId }
            current.copy(
                queue = updatedQueue,
                diagnostics = current.diagnostics.copy(
                    unavailableQueueItems = updatedQueue.count { it.availability != "available" }
                )
            )
        }
        return true
    }

    /**
     * Clears all non-playing tracks from the queue (DJ / Host only).
     */
    fun clearQueue(): Boolean {
        if (!isHost && !isDj) return false
        _session.update { current ->
            val playingOnly = current.queue.filter { it.playbackStatus == "playing" }
            current.copy(queue = playingOnly)
        }
        return true
    }

    // ==========================================
    // Participant & DJ Governance
    // ==========================================

    /**
     * Promotes a participant to Guest DJ.
     */
    fun setDj(deviceId: String): Boolean {
        if (!isHost) return false
        _session.update { current ->
            val updatedParticipants = current.participants.map { participant ->
                if (participant.deviceId == deviceId) {
                    participant.copy(
                        role = JamRole.Dj.id,
                        permissions = listOf("playback", "queue", "suggest", "vote")
                    )
                } else if (participant.role == JamRole.Dj.id) {
                    participant.copy(
                        role = JamRole.Guest.id,
                        permissions = permissionsForRole(JamRole.Guest, current.permissions)
                    )
                } else participant
            }
            current.copy(
                participants = updatedParticipants,
                activeDjDeviceId = deviceId
            )
        }
        return true
    }

    /**
     * Kicks / removes a participant from the mesh (Host only).
     */
    fun kickParticipant(deviceId: String): Boolean {
        if (!isHost || deviceId == localDeviceId) return false
        _session.update { current ->
            val updatedParticipants = current.participants.filterNot { it.deviceId == deviceId }
            current.copy(
                participants = updatedParticipants,
                diagnostics = current.diagnostics.copy(
                    participantCount = updatedParticipants.size,
                    revokedParticipants = current.diagnostics.revokedParticipants + 1
                )
            )
        }
        return true
    }

    /**
     * Updates guest policy switches (Host only).
     */
    fun updateGuestPolicy(policy: JamGuestPolicy): Boolean {
        if (!isHost) return false
        _session.update { current ->
            val updatedParticipants = current.participants.map { participant ->
                if (participant.role == JamRole.Guest.id) {
                    participant.copy(permissions = permissionsForRole(JamRole.Guest, policy))
                } else participant
            }
            current.copy(
                permissions = policy,
                participants = updatedParticipants
            )
        }
        return true
    }

    // ==========================================
    // Clock Sync & Playback Telemetry
    // ==========================================

    fun updateSyncDiagnostics(localPlayerPositionMs: Long, targetPositionMs: Long) {
        val status = clockSyncEngine.computeSyncStatus(localPlayerPositionMs, targetPositionMs)
        _syncStatus.value = status
        _session.update { current ->
            current.copy(
                diagnostics = current.diagnostics.copy(
                    syncDriftMs = status.driftDeltaMs,
                    masterTimelinePositionMs = targetPositionMs
                )
            )
        }
    }

    private fun permissionsForRole(role: JamRole, policy: JamGuestPolicy): List<String> = when (role) {
        JamRole.Host -> listOf("all", "admin", "playback", "queue", "policy")
        JamRole.Dj -> listOf("playback", "queue", "suggest", "vote")
        JamRole.Controller -> listOf("playback", "queue", "view")
        JamRole.Listener -> listOf("listen", "view")
        JamRole.Guest -> buildList {
            add("view")
            if (policy.guestsCanSuggest) add("suggest")
            if (policy.guestsCanQueue) add("queue")
            if (policy.guestsCanEditQueue) add("edit_queue")
            if (policy.guestsCanControlPlayback) add("playback")
            if (policy.federatedSourcesEnabled) add("share_source")
        }
    }

    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun Track.toQueueItem(
        addedByDeviceId: String,
        addedByName: String,
        sourceDeviceName: String = localDeviceName,
        playbackStatus: String = "queued",
        isSuggestion: Boolean = false,
        votes: Int = 0,
        upvotedBy: List<String> = emptyList()
    ): QueueItem = QueueItem(
        queueItemId = "item-${UUID.randomUUID().toString().take(8)}",
        trackId = id,
        title = title,
        artist = artist,
        addedByDeviceId = addedByDeviceId,
        addedByName = addedByName,
        sourceDeviceId = if (streamUrl.startsWith("http")) "desktop-host" else localDeviceId,
        sourceDeviceName = sourceDeviceName,
        sourceLibraryId = "pixelody-library",
        availability = if (missing) "unavailable" else "available",
        cacheState = "none",
        cacheExpiresAt = null,
        playbackStatus = playbackStatus,
        fallbackCandidates = emptyList(),
        votes = votes,
        upvotedBy = upvotedBy,
        downvotedBy = emptyList(),
        isSuggestion = isSuggestion,
        durationSeconds = durationSeconds,
        artworkUrl = artworkUrl
    )

    fun joinSession(code: String, asRole: JamRole = JamRole.Guest) {
        val tempSession = JamSession(
            active = true,
            sessionId = "jam-$code",
            mode = "mesh-coordinated",
            status = "active",
            permissions = JamGuestPolicy(),
            currentParticipant = null,
            participants = emptyList(),
            queue = emptyList(),
            diagnostics = JamDiagnostics(),
            joinCode = code
        )
        joinSession(tempSession, asRole)
    }

    fun voteTrack(queueItemId: String, vote: Int): JamSession = voteForTrack(queueItemId, vote)

    fun updatePolicy(policy: JamGuestPolicy): Boolean = updateGuestPolicy(policy)

    fun triggerClockResync() {
        clockSyncEngine.reset()
    }
}
