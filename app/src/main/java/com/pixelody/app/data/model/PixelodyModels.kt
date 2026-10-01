package com.pixelody.app.data.model

enum class HostConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Offline,
    Revoked,
    AuthFailed,
    PermissionDenied,
    Unreachable,
    Reconnecting,
    CredentialExpired,
    NetworkUnavailable,
    HostUnavailable
}

enum class NetworkMode {
    Auto,
    LocalOnly,
    DirectRemote,
    SelfHostedRelay,
    ManagedRelayFallback
}

enum class HostingVisibility {
    Off,
    ThisDeviceOnly,
    LocalNetwork,
    RemoteAllowed
}

enum class DeviceRole {
    LibraryHost,
    JamCoordinator,
    PlaybackDevice,
    Controller,
    Guest
}

enum class SourceAvailability {
    Live,
    PreparingCache,
    CachedForSession,
    Disconnected,
    PermissionRevoked,
    CacheExpired,
    Unavailable,
    Unknown
}

enum class JamCacheState {
    None,
    Preparing,
    Ready,
    Expired,
    Deleted
}

enum class RepeatMode {
    Off,
    One,
    All
}

data class HostProfile(
    val hostId: String,
    val hostName: String,
    val baseUrl: String,
    val platform: String,
    val roles: List<String>,
    val connectionState: HostConnectionState,
    val capabilities: HostCapabilities? = null
)

data class HostCapabilities(
    val hostId: String,
    val hostName: String,
    val platform: String,
    val roles: List<String>,
    val visibility: String,
    val canStream: Boolean,
    val canRemoteControl: Boolean,
    val canJamCoordinate: Boolean,
    val supportsPolling: Boolean,
    val supportsWebSocket: Boolean,
    val liveEndpoint: String,
    val recommendedPollMs: Long,
    val commandPermissions: List<String>,
    val requiresForegroundService: Boolean,
    val batterySensitive: Boolean,
    val lanExposureEnabled: Boolean,
    val maxPageSize: Int
)

data class Track(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val durationSeconds: Int = 0,
    val format: String = "",
    val codec: String = "",
    val lossless: Boolean = false,
    val sampleRate: Int = 44100,
    val bitDepth: Int? = null,
    val bitrate: Int? = null,
    val channels: Int = 2,
    val replayGainDb: Double? = null,
    val artworkUrl: String? = null,
    val streamUrl: String = "",
    val favorite: Boolean = false,
    val missing: Boolean = false,
    val genre: String = ""
)

data class TrackPage(
    val tracks: List<Track>,
    val offset: Int,
    val limit: Int,
    val total: Int,
    val nextOffset: Int?,
    val revision: Long
)

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val artworkUrl: String? = null
)

data class QueueSnapshot(
    val currentTrackId: String?,
    val trackIds: List<String>,
    val shuffle: Boolean,
    val repeatMode: RepeatMode,
    val items: List<QueueItem> = emptyList()
)

data class LibrarySnapshot(
    val host: HostProfile,
    val tracks: List<Track>,
    val playlists: List<Playlist>,
    val favorites: List<String>,
    val queue: QueueSnapshot,
    val revision: Long = 0,
    val revisions: SyncRevisions = SyncRevisions()
)

data class QueueItem(
    val queueItemId: String,
    val trackId: String,
    val title: String,
    val artist: String = "",
    val addedByDeviceId: String = "",
    val addedByName: String = "",
    val sourceDeviceId: String = "",
    val sourceDeviceName: String = "",
    val sourceLibraryId: String = "",
    val availability: String = "available",
    val cacheState: String = "none",
    val cacheExpiresAt: Long? = null,
    val playbackStatus: String = "queued",
    val fallbackCandidates: List<String> = emptyList(),
    val votes: Int = 0,
    val upvotedBy: List<String> = emptyList(),
    val downvotedBy: List<String> = emptyList(),
    val isSuggestion: Boolean = false,
    val durationSeconds: Int = 0,
    val artworkUrl: String? = null
)

enum class JamRole(val id: String, val displayName: String) {
    Host("host", "Host / Coordinator"),
    Dj("dj", "Guest DJ"),
    Guest("guest", "Guest"),
    Listener("listener", "Synced Speaker"),
    Controller("controller", "Remote Controller");

    companion object {
        fun fromId(id: String?): JamRole =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: Guest
    }
}

data class JamClockSyncSample(
    val t0ClientSendMs: Long,
    val t1ServerReceiveMs: Long,
    val t2ServerSendMs: Long,
    val t3ClientReceiveMs: Long
) {
    val roundTripTimeMs: Long
        get() = (t3ClientReceiveMs - t0ClientSendMs) - (t2ServerSendMs - t1ServerReceiveMs).coerceAtLeast(0L)

    val rawClockOffsetMs: Long
        get() = ((t1ServerReceiveMs - t0ClientSendMs) + (t2ServerSendMs - t3ClientReceiveMs)) / 2L
}

enum class JamSyncMode {
    Standalone,
    CoordinatedHost,
    SynchronizedListener
}

data class JamSyncStatus(
    val mode: JamSyncMode = JamSyncMode.Standalone,
    val isSynchronized: Boolean = false,
    val clockOffsetMs: Long = 0L,
    val roundTripTimeMs: Long = 0L,
    val driftDeltaMs: Long = 0L,
    val playbackSpeedFactor: Float = 1.0f,
    val syncQuality: String = "Good",
    val lastSyncTimestamp: Long = 0L
)

data class JamVoteState(
    val queueItemId: String,
    val votes: Int,
    val userVote: Int // +1 (upvote), -1 (downvote), 0 (none)
)

data class LiveState(
    val revision: Long,
    val hostId: String,
    val hostName: String,
    val visibility: String,
    val checkedAt: String,
    val pollAfterMs: Long,
    val permissions: List<String>,
    val playbackCurrentTrackId: String?,
    val playing: Boolean,
    val queueTrackIds: List<String>,
    val favoriteTrackIds: List<String>,
    val deviceRefresh: DeviceRefreshInstruction?,
    val revisions: SyncRevisions = SyncRevisions(),
    val networkSession: NetworkSession? = null,
    val jamSession: JamSession? = null
)

data class SyncRevisions(
    val overall: Long = 0,
    val library: Long = 0,
    val playback: Long = 0,
    val queue: Long = 0,
    val favorites: Long = 0,
    val permissions: Long = 0,
    val deviceRefresh: Long = 0,
    val jam: Long = 0
)

data class JamSession(
    val active: Boolean,
    val sessionId: String,
    val mode: String,
    val status: String,
    val permissions: JamGuestPolicy = JamGuestPolicy(),
    val currentParticipant: JamParticipant? = null,
    val participants: List<JamParticipant> = emptyList(),
    val queue: List<QueueItem> = emptyList(),
    val diagnostics: JamDiagnostics = JamDiagnostics(),
    val joinCode: String = "",
    val activeDjDeviceId: String? = null
)

data class JamGuestPolicy(
    val guestsCanView: Boolean = true,
    val guestsCanSuggest: Boolean = true,
    val guestsCanQueue: Boolean = false,
    val guestsCanEditQueue: Boolean = false,
    val guestsCanControlPlayback: Boolean = false,
    val federatedSourcesEnabled: Boolean = false
)

data class JamParticipant(
    val deviceId: String,
    val name: String,
    val role: String,
    val status: String,
    val permissions: List<String> = emptyList(),
    val pingMs: Long = 0L,
    val isLocalDevice: Boolean = false
)

data class JamDiagnostics(
    val participantCount: Int = 0,
    val pendingParticipants: Int = 0,
    val revokedParticipants: Int = 0,
    val unavailableQueueItems: Int = 0,
    val lastIssue: String = "",
    val recommendedAction: String = "none",
    val syncDriftMs: Long = 0L,
    val masterTimelinePositionMs: Long = 0L
)

data class LivePollResult(
    val unchanged: Boolean,
    val revision: Long,
    val pollAfterMs: Long,
    val state: LiveState?
)

data class NetworkSession(
    val kind: String,
    val sessionId: String,
    val host: NetworkSessionHost,
    val sync: NetworkSessionSync,
    val playback: NetworkSessionPlayback,
    val shuffle: FlowShuffleSnapshot = FlowShuffleSnapshot(),
    val queue: NetworkSessionQueue,
    val diagnostics: NetworkSessionDiagnostics
)

data class FlowShuffleSnapshot(
    val version: Int = 1,
    val enabled: Boolean = false,
    val mode: String = "flow",
    val session: FlowShuffleSession = FlowShuffleSession(),
    val plan: FlowShufflePlan = FlowShufflePlan()
)

data class FlowShuffleSession(
    val horizon: Int = 6,
    val energyShape: String = "auto",
    val albumPolicy: String = "track",
    val preserveAlbumRuns: Boolean = false
)

data class FlowShufflePlan(
    val style: String = "flow",
    val seed: String = "",
    val generation: Int = 0,
    val cursor: Int = -1,
    val sourceQueueIds: List<String> = emptyList(),
    val order: List<String> = emptyList(),
    val future: List<String> = emptyList(),
    val priorityIds: List<String> = emptyList()
)

data class NetworkSessionHost(
    val id: String,
    val name: String,
    val platform: String,
    val visibility: String,
    val roles: List<String>
)

data class NetworkSessionSync(
    val revision: Long,
    val revisions: SyncRevisions = SyncRevisions(),
    val generatedAt: String,
    val checkedAt: String,
    val pollAfterMs: Long
)

data class NetworkSessionPlayback(
    val state: String,
    val currentTrackId: String?,
    val currentTrack: NetworkTrackSummary?,
    val playing: Boolean,
    val elapsedSeconds: Double,
    val durationSeconds: Double,
    val positionUpdatedAt: String,
    val estimatedStartedAt: String
)

data class NetworkSessionQueue(
    val trackIds: List<String>,
    val currentIndex: Int,
    val upcomingTrackIds: List<String>,
    val upcomingTracks: List<NetworkTrackSummary>
)

data class NetworkSessionDiagnostics(
    val activeDevices: Int,
    val lastError: String,
    val recentCommandCount: Int
)

data class NetworkTrackSummary(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Double?,
    val format: String,
    val codec: String,
    val lossless: Boolean,
    val sampleRate: Int?,
    val bitDepth: Int?,
    val missing: Boolean
)

data class DeviceRefreshInstruction(
    val id: String,
    val action: String,
    val scope: String,
    val reason: String,
    val requestedAt: String,
    val requestedByDeviceName: String
)

data class TrustedDevice(
    val id: String,
    val name: String,
    val permissions: List<String>,
    val tokenPreview: String,
    val publicKey: String,
    val createdAt: String,
    val lastSeenAt: String,
    val revokedAt: String,
    val status: String,
    val pairingMethod: String
)

data class SavedHostProfile(
    val hostId: String,
    val hostName: String,
    val baseUrl: String,
    val baseUrls: List<String>,
    val token: String,
    val platform: String,
    val roles: List<String>,
    val savedAt: Long,
    val lastConnectedAt: Long
)
