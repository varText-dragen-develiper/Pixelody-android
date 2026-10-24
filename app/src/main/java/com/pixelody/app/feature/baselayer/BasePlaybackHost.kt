package com.pixelody.app.feature.baselayer

import com.pixelody.app.BuildConfig
import kotlinx.coroutines.CancellationException
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.network.connectionStateFor
import com.pixelody.app.data.network.hostConnectionFailureMessage
import com.pixelody.app.data.network.endsHostAccess
import com.pixelody.app.core.playback.JamSessionCoordinator
import com.pixelody.app.core.playback.removeRemoteHostMedia
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.pixelody.app.core.unskippable.UnskippableMix
import com.pixelody.app.data.storage.TrackMetadataOverride
import com.pixelody.app.data.storage.UnskippableMixStore
import com.pixelody.app.feature.unskippable.UnskippableBuilder
import com.pixelody.app.feature.unskippable.UnskippableCover
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.pixelody.app.core.analytics.ListeningLedgerStore
import com.pixelody.app.core.analytics.ListeningLedgerSnapshot
import com.pixelody.app.core.analytics.ListeningLedgerProjection
import com.pixelody.app.core.playback.AcousticTimbreEngine
import com.pixelody.app.data.storage.LoudnessNormalizationMode
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.pixelody.app.core.playback.PixelodyPlaybackService
import com.pixelody.app.core.playback.PlaybackEvidenceMetadata
import com.pixelody.app.core.playback.playbackFailure
import com.pixelody.app.core.lyrics.LyricsDocument
import com.pixelody.app.core.lyrics.LyricsRepository
import com.pixelody.app.core.lyrics.LyricsSource
import com.pixelody.app.core.playback.SleepTimerController
import com.pixelody.app.core.playback.SleepTimerMode
import com.pixelody.app.core.playback.SleepTimerState
import com.pixelody.app.data.fixtures.FakePixelodyHost
import com.pixelody.app.data.model.addingToFirstEmpty
import com.pixelody.app.data.model.CrateBook
import com.pixelody.app.data.model.CrateStarters
import com.pixelody.app.data.model.HostConnectionDetails
import com.pixelody.app.data.model.Playlist
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.repository.HostRepository
import com.pixelody.app.data.storage.CoverStore
import com.pixelody.app.data.storage.CrateStore
import com.pixelody.app.data.storage.PlaylistStore
import com.pixelody.app.data.storage.MediaStoreAudioRepository
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.data.storage.OfflineMediaStore
import com.pixelody.app.data.storage.PixelodyPersistenceRepository
import com.pixelody.app.data.storage.SavedHostStore
import com.pixelody.app.data.storage.TrackMetadataStore
import com.pixelody.app.data.storage.scanEntireDeviceForAudio
import com.pixelody.app.data.storage.scanFolderForTracks
import com.pixelody.app.data.storage.localTrackFromUri
import com.pixelody.app.core.playback.mediaMimeType
import com.pixelody.app.core.playback.rebaseRemoteMediaUrl
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.core.shuffle.UniversalShuffleMetadata
import com.pixelody.app.data.storage.ShuffleScopeIdentityStore
import com.pixelody.app.core.playback.ShuffleSourceKeys
import androidx.media3.session.SessionCommand
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.feature.nowplaying.ordinaryPlayerViewMode
import com.pixelody.app.data.model.MasteringProfile
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.CassetteTapeSettings
import com.pixelody.app.data.model.StemsIsolatorSettings
import com.pixelody.app.data.model.OscilloscopeSettings
import com.pixelody.app.data.model.AutoDjSettings
import com.pixelody.app.data.model.AudioHapticSettings
import com.pixelody.app.data.model.HiResLosslessSettings
import com.pixelody.app.data.model.RepeatMode
import com.pixelody.app.data.storage.MobileEqualizerStore
import com.pixelody.app.ui.navigation.PixelodyTab
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZoneId
import java.util.UUID

/**
 * Builds a Media3 [MediaItem] from a [Track] entity for playback in the base layer.
 */
fun baseMediaItemFor(
    track: Track,
    offlineMediaStore: OfflineMediaStore,
    hostBaseUrl: String? = null
): MediaItem {
    val cached = offlineMediaStore.getCachedTrack(track.id)
    val rawStreamUrl = track.streamUrl.trim().takeIf { it.isNotBlank() && it != "null" && !it.endsWith("/null") }
        ?: if (!track.missing && track.id.isNotBlank() && hostBaseUrl != null) {
            "${hostBaseUrl.trimEnd('/')}/api/v1/tracks/${track.id}/stream"
        } else ""
    val streamUrl = cached?.let { "file://${it.localFilePath}" }
        ?: (hostBaseUrl?.let { rebaseRemoteMediaUrl(rawStreamUrl, it) } ?: rawStreamUrl)
    val rawArtworkUrl = track.artworkUrl?.trim()?.takeIf { it.isNotBlank() && it != "null" && !it.endsWith("/null") }
    val artworkUrl = cached?.localArtworkPath?.let { "file://$it" }
        ?: (hostBaseUrl?.let { rebaseRemoteMediaUrl(rawArtworkUrl, it) } ?: rawArtworkUrl)
    val formatDisplay = when {
        track.lossless -> "LOSSLESS"
        track.format.isNotBlank() -> track.format.uppercase()
        else -> "AUDIO"
    }
    val evidence = Bundle().apply {
        putLong(PlaybackEvidenceMetadata.DURATION_MS, track.durationSeconds.toLong().coerceAtLeast(0) * 1000)
        putString(PlaybackEvidenceMetadata.FORMAT, track.format)
        putBoolean(PlaybackEvidenceMetadata.LOSSLESS, track.lossless)
        putBoolean(PlaybackEvidenceMetadata.PERSONAL, true)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(track.title)
        .setArtist(track.artist)
        .setAlbumTitle(track.album)
        .setAlbumArtist(track.albumArtist)
        .setDiscNumber(track.discNumber)
        .setTrackNumber(track.trackNumber)
        .setSubtitle(formatDisplay)
        .setArtworkUri(artworkUrl?.let(Uri::parse))
        .setExtras(evidence)
        .build()
    return PlaybackEvidenceMetadata.withEntry(MediaItem.Builder()
        .setUri(streamUrl)
        .setMimeType(mediaMimeType(track.format))
        .setMediaId(track.id)
        .setMediaMetadata(metadata)
        .build())
}

/** Explicit rather than relying on RepeatMode's ordinals happening to match Media3's. */
private fun playerRepeatModeFor(mode: RepeatMode): Int = when (mode) {
    RepeatMode.Off -> Player.REPEAT_MODE_OFF
    RepeatMode.One -> Player.REPEAT_MODE_ONE
    RepeatMode.All -> Player.REPEAT_MODE_ALL
}

/**
 * Connects [PixelodyBaseShell] to live data repositories and the live Media3 [PixelodyPlaybackService].
 *
 * Ingests:
 * - Phone media from [MediaStore] and document picker
 * - Desktop host media from [HostRepository] and [SavedHostStore]
 * - Persistent crates from [CrateStore]
 * - Offline media cache from [OfflineMediaStore]
 * - Playback and cold-start state from [MobileSettingsStore]
 */
@Composable
fun BasePlaybackHost(
    repository: HostRepository = remember { HostRepository(FakePixelodyHost()) },
    initialData: BaseLayerData = if (BuildConfig.ENABLE_DEMO_LIBRARY) sampleBaseLayerData() else BaseLayerData(),
    incomingConnectionDetails: HostConnectionDetails? = null,
    onConnectionDetailsConsumed: () -> Unit = {},
    incomingDeepLink: PixelodyTab? = null,
    incomingSearchQuery: String = "",
    onDeepLinkConsumed: () -> Unit = {},
    activeTheme: PixelodyMobileTheme = PixelodyMobileTheme.Studio,
    onThemeChange: (PixelodyMobileTheme) -> Unit = {},
    onThemeReset: () -> Unit = {},
    modifier: Modifier = Modifier,
    onDataChanged: (BaseLayerData) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsStore = remember { MobileSettingsStore(context.applicationContext) }
    val offlineMediaStore = remember { OfflineMediaStore(context.applicationContext) }
    val savedHostStore = remember { SavedHostStore(context.applicationContext) }
    var savedHostProfile by remember { mutableStateOf(savedHostStore.load()) }
    var availableJamSession by remember { mutableStateOf<com.pixelody.app.data.model.JamSession?>(null) }
    var hostLiveState by remember { mutableStateOf<com.pixelody.app.data.model.LiveState?>(null) }
    var joiningJam by remember { mutableStateOf(false) }
    var jamIssue by remember { mutableStateOf("") }
    val crateStore = remember { CrateStore(context.applicationContext) }
    val coverStore = remember { CoverStore(context.applicationContext) }
    val equalizerStore = remember { MobileEqualizerStore(context.applicationContext) }
    val persistenceRepository = remember { PixelodyPersistenceRepository(context.applicationContext) }
    var listeningSnapshot by remember { mutableStateOf(ListeningLedgerSnapshot()) }
    var listeningNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        val ledger = withContext(Dispatchers.IO) {
            ListeningLedgerStore(File(context.filesDir, "universal-shuffle/listening"))
        }
        while (true) {
            listeningSnapshot = withContext(Dispatchers.IO) { ledger.reload() }
            listeningNow = System.currentTimeMillis()
            delay(2_000)
        }
    }
    val mediaStoreAudioRepository = remember { MediaStoreAudioRepository(context.applicationContext, persistenceRepository) }
    val metadataStore = remember { TrackMetadataStore(context.applicationContext) }
    val metadataOverrides by metadataStore.overridesFlow.collectAsState()

    var player by remember { mutableStateOf<Player?>(null) }
    var pendingLearningExport by remember { mutableStateOf<String?>(null) }
    val exportLearningDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = pendingLearningExport
        pendingLearningExport = null
        if (uri != null && json != null) coroutineScope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } ?: error("No document output") }.isSuccess }
            android.widget.Toast.makeText(context, if (saved) "Listening history exported" else "Learning export could not be saved", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val importLearningDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) coroutineScope.launch {
            val json = withContext(Dispatchers.IO) { runCatching {
                context.contentResolver.openInputStream(uri)!!.bufferedReader().use { reader ->
                    val result = StringBuilder(); val buffer = CharArray(8192)
                    while (true) { val read = reader.read(buffer); if (read < 0) break; require(result.length + read <= 16 * 1024 * 1024); result.append(buffer, 0, read) }
                    result.toString()
                }
            }.getOrNull() }
            val controller = player as? MediaController
            if (json != null && controller != null) {
                val future = controller.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY), Bundle().apply { putString("action", "import-learning"); putString("json", json) })
                val receipt = withContext(Dispatchers.IO) { runCatching { future.get(15, java.util.concurrent.TimeUnit.SECONDS) }.getOrNull() }
                val imported = receipt?.extras?.getString("receipt")?.let { runCatching { org.json.JSONObject(it).optBoolean("ok") }.getOrDefault(false) } == true
                android.widget.Toast.makeText(context, if (imported) "Listening history imported" else "The listening history could not be imported", android.widget.Toast.LENGTH_LONG).show()
            } else android.widget.Toast.makeText(context, "Learning import could not be read", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val playbackPositionState = remember { mutableLongStateOf(0L) }
    var playbackDurationMs by remember { mutableStateOf(0L) }

    // Host data state
    var hostTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var hostPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var hostBaseUrl by remember { mutableStateOf<String?>(null) }
    var hostReachable by remember { mutableStateOf(true) }
    var hostConnectionState by remember { mutableStateOf(HostConnectionState.Disconnected) }
    var hostConnectionError by remember { mutableStateOf("") }
    var isConnectingHost by remember { mutableStateOf(false) }
    var isLoadingLocalLibrary by remember { mutableStateOf(true) }
    val jamCoordinator = remember { JamSessionCoordinator() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Phone / Local data state
    var localTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isScanningDevice by remember { mutableStateOf(false) }
    var deviceScanNotice by remember { mutableStateOf("") }
    var playbackError by remember { mutableStateOf("") }

    // Offline cached IDs and crate store
    var cachedTrackIds by remember { mutableStateOf(offlineMediaStore.listCachedTracks().map { it.id }.toSet()) }
    var crateBook by remember { mutableStateOf(crateStore.load()) }
    var coverBook by remember { mutableStateOf(coverStore.load()) }
    val playlistStore = remember { PlaylistStore(context) }
    var userPlaylists by remember { mutableStateOf(playlistStore.load()) }
    var favoriteTrackIds by remember { mutableStateOf(settingsStore.loadFavoriteTrackIds()) }
    val lyricsRepository = remember { LyricsRepository(context.applicationContext) }
    var currentLyrics by remember { mutableStateOf(LyricsDocument.EMPTY) }
    val sleepTimerController = remember {
        SleepTimerController(
            scope = coroutineScope,
            onTimerExpired = {
                val activePlayer = player
                if (activePlayer != null) {
                    activePlayer.pause()
                    activePlayer.volume = 1.0f
                }
            },
            onVolumeScaleChange = { scale ->
                player?.volume = scale
            }
        )
    }
    val sleepTimerState by sleepTimerController.state.collectAsState()

    // Active lens & source preferences
    var activeSource by remember { mutableStateOf(initialData.source) }
    var activeLens by remember { mutableStateOf(initialData.lens) }
    var activePlayerViewMode by remember { mutableStateOf(ordinaryPlayerViewMode(settingsStore.loadPlayerViewMode())) }
    var currentTrackId by remember { mutableStateOf<String?>(initialData.currentTrackId) }
    var lastTrackId by remember { mutableStateOf<String?>(initialData.lastTrackId) }
    var isPlaying by remember { mutableStateOf(initialData.isPlaying) }
    var queue by remember { mutableStateOf(initialData.queue) }
    var queueEntryIds by remember { mutableStateOf(initialData.queueEntryIds) }
    var queueDecisionCues by remember { mutableStateOf(initialData.queueDecisionCues) }
    var currentEntryId by remember { mutableStateOf(initialData.currentEntryId) }
    var liveNextTrackId by remember { mutableStateOf(initialData.liveNextTrackId) }
    var shuffleWhy by remember { mutableStateOf(initialData.shuffleWhy) }
    // Add to session: the list playing now plus every list linked to it.
    var currentSourceKey by remember { mutableStateOf<String?>(null) }
    var currentSourceName by remember { mutableStateOf("This queue") }
    var sessionLists by remember { mutableStateOf(initialData.sessionLists) }
    var sessionEven by remember { mutableStateOf(initialData.sessionEven) }
    var restoredPlaybackTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

    // Hardware DSP, Mastering, Equalizer & Console state
    var equalizerRuntime by remember { mutableStateOf(com.pixelody.app.core.playback.EqualizerRuntimeState()) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(MobileEqualizerStore.PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == MobileEqualizerStore.KEY_EQ_RUNTIME) equalizerRuntime = equalizerStore.loadRuntimeState()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    var globalEqualizer by remember { mutableStateOf(equalizerStore.loadGlobalProfile()) }
    var trackEqualizers by remember { mutableStateOf(equalizerStore.loadTrackProfiles()) }
    var useMasteringRack by remember { mutableStateOf(equalizerStore.loadUseMasteringRack()) }
    var globalMastering by remember { mutableStateOf(equalizerStore.loadGlobalMasteringProfile()) }
    var spatialSettings by remember { mutableStateOf(equalizerStore.loadSpatialSettings()) }
    var cassetteSettings by remember { mutableStateOf(CassetteTapeSettings()) }
    var stemsSettings by remember { mutableStateOf(StemsIsolatorSettings()) }
    var oscilloscopeSettings by remember { mutableStateOf(OscilloscopeSettings()) }
    var autoDjSettings by remember { mutableStateOf(AutoDjSettings()) }
    var audioHapticSettings by remember { mutableStateOf(AudioHapticSettings()) }
    var hiResSettings by remember { mutableStateOf(HiResLosslessSettings()) }
    var shuffleMode by remember {
        mutableStateOf(
            FlowShuffleMode.fromSaved(settingsStore.loadShuffleMode())
                ?: if (settingsStore.loadShuffleEnabled()) FlowShuffleMode.Balanced else FlowShuffleMode.Off
        )
    }
    val shuffleEnabled = shuffleMode != FlowShuffleMode.Off
    var queuePlanningJob by remember { mutableStateOf<Job?>(null) }
    var pendingPlayback by remember { mutableStateOf<Pair<List<String>, Long>?>(null) }
    var repeatMode by remember {
        mutableStateOf(RepeatMode.values().getOrElse(settingsStore.loadRepeatMode()) { RepeatMode.Off })
    }
    var playbackSpeed by remember { mutableFloatStateOf(settingsStore.loadPlaybackSpeed()) }
    var pitchLocked by remember { mutableStateOf(settingsStore.loadPitchLocked()) }
    var loudnessMode by remember { mutableStateOf(settingsStore.loadLoudnessNormalization()) }
    // Unskippable mixes: saved apart from playlists, opened on their own cover, and
    // played with skip, seek and queue changes locked until the mix ends or you leave.
    val unskippableStore = remember { UnskippableMixStore(context.applicationContext) }
    var unskippableMixes by remember { mutableStateOf(unskippableStore.load()) }
    var unskippableBuilderOpen by remember { mutableStateOf(false) }
    var unskippableBuilderSeed by remember { mutableStateOf<UnskippableMix.Recipe?>(null) }
    // Saveable so a rotation does not drop the cover while the playback lock is still on.
    var unskippableCoverId by rememberSaveable { mutableStateOf<String?>(null) }
    var unskippableActive by rememberSaveable { mutableStateOf(false) }
    var unskippableFinished by rememberSaveable { mutableStateOf(false) }
    var unskippablePrior by rememberSaveable(stateSaver = listSaver<Pair<FlowShuffleMode, RepeatMode>?, String>(
        save = { p -> p?.let { listOf(it.first.name, it.second.name) } ?: emptyList() },
        restore = { l -> if (l.size == 2) FlowShuffleMode.valueOf(l[0]) to RepeatMode.valueOf(l[1]) else null })) { mutableStateOf<Pair<FlowShuffleMode, RepeatMode>?>(null) }
    var playerEndedAt by remember { mutableLongStateOf(0L) }

    fun updateSpeedAndPitch(speed: Float, locked: Boolean) {
        playbackSpeed = speed
        pitchLocked = locked
        settingsStore.savePlaybackSpeed(speed)
        settingsStore.savePitchLocked(locked)
        val pitch = if (locked) 1.0f else speed
        player?.playbackParameters = PlaybackParameters(speed, pitch)
    }

    fun setLoudnessMode(mode: LoudnessNormalizationMode) {
        loudnessMode = mode
        settingsStore.saveLoudnessNormalization(mode)
    }

    /**
     * The queue on screen and the queue that plays were two different lists. They came
     * apart the moment a track finished - nothing trimmed the played item - and taking
     * something out of the queue edited the screen while the player carried on with it.
     * The player's media items are the queue now, and this reads them back.
     */
    fun syncQueueFromPlayer() {
        val activePlayer = player ?: return
        val count = activePlayer.mediaItemCount
        if (count <= 0) {
            queue = emptyList()
            queueEntryIds = emptyList()
            currentEntryId = null
            liveNextTrackId = null
            shuffleWhy = null
            return
        }
        val start = activePlayer.currentMediaItemIndex + 1
        liveNextTrackId = if (start < count) activePlayer.getMediaItemAt(start).mediaMetadata.extras?.getString(PlaybackEvidenceMetadata.LIVE_NEXT) else null
        shuffleWhy = com.pixelody.app.core.playback.ShuffleLanes.whyLine(
            activePlayer.currentMediaItem?.mediaMetadata?.extras?.getString(PlaybackEvidenceMetadata.DECISION),
            if (start >= 2) activePlayer.getMediaItemAt(start - 2).mediaMetadata.title?.toString() else null
        )
        queue = (start until count).map { activePlayer.getMediaItemAt(it).mediaId }
        queueEntryIds = (start until count).map { index ->
            PlaybackEvidenceMetadata.entryId(activePlayer.getMediaItemAt(index)) ?: "legacy-slot-$index"
        }
        currentEntryId = PlaybackEvidenceMetadata.entryId(activePlayer.currentMediaItem)
        queueDecisionCues = (start until count).map { index ->
            val raw = activePlayer.getMediaItemAt(index).mediaMetadata.extras?.getString(PlaybackEvidenceMetadata.DECISION)
            com.pixelody.app.core.playback.ShuffleLanes.queueCue(raw)
        }
        restoredPlaybackTracks = (0 until count).map { index ->
            val item = activePlayer.getMediaItemAt(index)
            val metadata = item.mediaMetadata
            val extras = metadata.extras
            Track(id = item.mediaId, title = metadata.title?.toString().orEmpty(), artist = metadata.artist?.toString().orEmpty(),
                album = metadata.albumTitle?.toString().orEmpty(), albumArtist = metadata.albumArtist?.toString(),
                discNumber = metadata.discNumber, trackNumber = metadata.trackNumber,
                durationSeconds = ((extras?.getLong(PlaybackEvidenceMetadata.DURATION_MS) ?: 0) / 1000).toInt(),
                format = extras?.getString(PlaybackEvidenceMetadata.FORMAT).orEmpty(), sampleRate = 0, channels = 0,
                lossless = extras?.getBoolean(PlaybackEvidenceMetadata.LOSSLESS) ?: false,
                streamUrl = item.localConfiguration?.uri?.toString().orEmpty(), artworkUrl = metadata.artworkUri?.toString())
        }.distinctBy { it.id }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            player?.let { p ->
                playbackPositionState.longValue = p.currentPosition.coerceAtLeast(0L)
                val dur = p.duration
                if (dur > 0L) {
                    playbackDurationMs = dur
                }
            }
            delay(200)
        }
    }

    fun recoverHostConnection(state: HostConnectionState, message: String) {
        hostReachable = false
        hostConnectionState = state
        hostConnectionError = "$message Reconnect when you're ready."
        jamCoordinator.leaveSession()
        availableJamSession = null
        hostLiveState = null
        activeSource = sourceAfterConnectionLoss(activeSource)
        settingsStore.saveLastSourceScope(activeSource.key)
        player?.let { activePlayer ->
            removeRemoteHostMedia(activePlayer)
            currentTrackId = activePlayer.currentMediaItem?.mediaId
            lastTrackId = currentTrackId
            playbackPositionState.longValue = activePlayer.currentPosition.coerceAtLeast(0L)
            playbackDurationMs = activePlayer.duration.coerceAtLeast(0L)
        }
        if (player == null && lastTrackId in hostTracks.map { it.id } && lastTrackId !in cachedTrackIds) {
            lastTrackId = null
        }
        if (state.endsHostAccess()) {
            savedHostStore.clear()
            savedHostProfile = null
            hostBaseUrl = null
        }
    }

    fun refreshHostLibrary(details: HostConnectionDetails? = incomingConnectionDetails) {
        if (isConnectingHost) return
        val profile = savedHostStore.load()
        savedHostProfile = profile
        if (details == null && profile == null && !BuildConfig.ENABLE_DEMO_LIBRARY) {
            recoverHostConnection(HostConnectionState.Disconnected, "Desktop disconnected.")
            hostConnectionError = ""
            return
        }
        if (profile != null) hostBaseUrl = profile.baseUrl
        isConnectingHost = true
        hostConnectionState = HostConnectionState.Connecting
        hostConnectionError = ""
        coroutineScope.launch {
            try {
                val snapshot = when {
                    details != null -> {
                        val result = repository.connectHost(details)
                        savedHostStore.save(repository.savedProfileFor(result, details.baseUrls))
                        savedHostProfile = savedHostStore.load()
                        onConnectionDetailsConsumed()
                        result.snapshot
                    }
                    profile != null -> repository.loadHostLibrary(profile.baseUrls, profile.token)
                    else -> repository.loadFixtureLibrary()
                }
                hostBaseUrl = snapshot.host.baseUrl.takeIf { details != null || profile != null }
                hostTracks = snapshot.tracks
                hostPlaylists = snapshot.playlists
                hostReachable = true
                hostConnectionState = HostConnectionState.Connected
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                recoverHostConnection(connectionStateFor(error), hostConnectionFailureMessage(error))
            } finally {
                isConnectingHost = false
            }
        }
    }

    // Foreground-only liveness: failed polls leave a usable local app, not a half-live session.
    // Recovery is explicit, so a returning PC cannot steal playback or navigation.
    LaunchedEffect(hostBaseUrl, hostConnectionState, lifecycleOwner) {
        val baseUrl = hostBaseUrl ?: return@LaunchedEffect
        if (hostConnectionState != HostConnectionState.Connected) return@LaunchedEffect
        val profile = savedHostStore.load() ?: return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var revision = -1L
            while (true) {
                try {
                    val live = repository.fetchLiveState(baseUrl, profile.token, revision)
                    revision = live.revision
                    if (!live.unchanged) {
                        hostLiveState = live.state
                        val session = live.state?.jamSession
                        availableJamSession = session
                        if (jamCoordinator.session.value.active || session?.currentParticipant != null) {
                            jamCoordinator.updateFromLiveState(session)
                        }
                        if (session?.active != true && activeSource == BaseSource.Jam) {
                            activeSource = BaseSource.Phone
                            settingsStore.saveLastSourceScope(activeSource.key)
                        }
                    }
                    delay(live.pollAfterMs.coerceIn(1_000L, 10_000L))
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    recoverHostConnection(connectionStateFor(error), hostConnectionFailureMessage(error))
                    break
                }
            }
        }
    }

    fun connectHostPayload(payload: String) {
        val details = HostConnectionDetails.fromText(payload)
        if (details == null) {
            hostConnectionError = "That isn't a Pixelody connection invite. Copy a new invite from your desktop, or scan its QR code."
            return
        }
        refreshHostLibrary(details)
    }

    fun joinDesktopJam() {
        val profile = savedHostProfile ?: return
        if (joiningJam || hostConnectionState != HostConnectionState.Connected) return
        joiningJam = true
        jamIssue = ""
        coroutineScope.launch {
            try {
                val result = repository.requestJamJoin(hostBaseUrl ?: profile.baseUrl, profile.token)
                val session = repository.fetchJamSession(hostBaseUrl ?: profile.baseUrl, profile.token)
                if (hostConnectionState != HostConnectionState.Connected || savedHostProfile?.token != profile.token) return@launch
                if (!result.accepted && !result.approvalRequired) jamIssue = "The desktop did not accept the join. Check the session on your desktop and try again."
                availableJamSession = session
                hostLiveState = hostLiveState?.copy(jamSession = session)
                jamCoordinator.updateFromLiveState(session)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                jamIssue = hostConnectionFailureMessage(error)
                // A rejected join is not proof the library connection is lost.
                if (connectionStateFor(error) in listOf(HostConnectionState.NetworkUnavailable,
                        HostConnectionState.HostUnavailable, HostConnectionState.Revoked,
                        HostConnectionState.AuthFailed, HostConnectionState.CredentialExpired)) {
                    recoverHostConnection(connectionStateFor(error), jamIssue)
                }
            } finally {
                joiningJam = false
            }
        }
    }

    // Load Host library on launch or when connection details change
    LaunchedEffect(incomingConnectionDetails) {
        refreshHostLibrary()
    }

    // Cold-start initialization of lastTrackId & preferences
    LaunchedEffect(Unit) {
        val savedTrackId = settingsStore.loadLastTrackId()
        val savedPos = settingsStore.loadLastTrackPositionMs()
        val savedSource = settingsStore.loadLastSourceScope()
        if (savedTrackId != null && currentTrackId == null) {
            lastTrackId = savedTrackId
            activeSource = BaseSource.values().firstOrNull { it.key == savedSource } ?: activeSource
            playbackPositionState.longValue = savedPos
        }
    }

    // Scanner helpers
    fun runDeviceScan() {
        isScanningDevice = true
        deviceScanNotice = "Scanning device for music..."
        coroutineScope.launch(Dispatchers.IO) {
            val scanned = mediaStoreAudioRepository.scanDeviceStorage()
            withContext(Dispatchers.Main) {
                isScanningDevice = false
                if (scanned.isNotEmpty()) {
                    localTracks = (localTracks + scanned).distinctBy { it.id }
                    persistenceRepository.cacheScannedTracks(localTracks)
                    activeSource = BaseSource.Phone
                    settingsStore.saveLastSourceScope(activeSource.key)
                    deviceScanNotice = "Indexed ${scanned.size} track${if (scanned.size == 1) "" else "s"} on device."
                } else {
                    deviceScanNotice = if (mediaStoreAudioRepository.scanStatus.value.startsWith("Scan error:")) "Couldn't read your phone music. Try choosing a folder or specific files." else "No music found. Choose a folder or files if your music is stored elsewhere."
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runDeviceScan()
        } else {
            deviceScanNotice = "Music access was not granted. You can still choose specific files or a folder below."
        }
    }

    fun triggerDeviceScan() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            runDeviceScan()
        } else {
            audioPermissionLauncher.launch(permission)
        }
    }

    fun addLocalMusic(readTracks: () -> List<Track>, emptyNotice: String) {
        isScanningDevice = true
        deviceScanNotice = "Reading your music..."
        coroutineScope.launch {
            try {
                val imported = withContext(Dispatchers.IO) { readTracks() }
                if (imported.isNotEmpty()) {
                    localTracks = (localTracks + imported).distinctBy { it.id }
                    persistenceRepository.cacheScannedTracks(localTracks)
                    activeSource = BaseSource.Phone
                    settingsStore.saveLastSourceScope(activeSource.key)
                    deviceScanNotice = "${imported.size} song${if (imported.size == 1) "" else "s"} added. Open your library to listen."
                } else {
                    deviceScanNotice = emptyNotice
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                deviceScanNotice = "Couldn't read that music. Try another folder or choose specific files."
            } finally {
                isScanningDevice = false
            }
        }
    }

    val localFolderScanner = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) addLocalMusic({ scanFolderForTracks(context, uri) }, "No music found in that folder. Choose another folder or specific files.")
    }
    val localAudioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) addLocalMusic({ uris.mapNotNull { localTrackFromUri(context, it) } }, "Those files couldn't be read as audio. Try another selection.")
    }

    // Restore chosen files first, then merge discovered music without dropping chosen folders.
    LaunchedEffect(Unit) {
        try {
            localTracks = withContext(Dispatchers.IO) { persistenceRepository.loadCachedScannedTracks() }
            val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                val scanned = mediaStoreAudioRepository.scanDeviceStorage()
                localTracks = (localTracks + scanned).distinctBy { it.id }
                persistenceRepository.cacheScannedTracks(localTracks)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            deviceScanNotice = "Couldn't restore all your music. You can choose your files again or retry the phone scan."
        } finally {
            isLoadingLocalLibrary = false
        }
    }

    // Initialize crates if crate book is empty
    LaunchedEffect(hostPlaylists, hostTracks, localTracks) {
        // hasBook() rather than crates.isEmpty(): an empty book that was saved is someone
        // deleting their last crate, and reseeding over it is the app overruling them.
        if (!crateStore.hasBook() && crateBook.crates.isEmpty() &&
            (hostPlaylists.isNotEmpty() || hostTracks.isNotEmpty())
        ) {
            val allTracks = (hostTracks + localTracks).distinctBy { it.id }
            val collections = buildBaseCollections(allTracks, hostPlaylists)
            val albumIds = collections.filter { it.kindKey == BaseBrowseShape.Albums.kindKey }.map { it.id }
            val seeded = CrateStarters.from(hostPlaylists, albumIds)
            crateBook = seeded
            crateStore.save(seeded)
        }
    }

    // Connect to PixelodyPlaybackService via Media3 MediaController
    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PixelodyPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            { player = runCatching { controllerFuture.get() }.getOrNull()
                equalizerRuntime = equalizerStore.loadRuntimeState() },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            val controller = runCatching { if (controllerFuture.isDone) controllerFuture.get() else null }.getOrNull()
            controller?.release()
            controllerFuture.cancel(true)
            player = null
        }
    }

    // Synchronize Media3 Player events
    DisposableEffect(player) {
        val activePlayer = player ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
                if (isPlayingNow) playbackError = ""
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                if (mediaId != null) {
                    currentTrackId = mediaId
                    lastTrackId = mediaId
                    settingsStore.saveLastTrackId(mediaId)
                }
                syncQueueFromPlayer()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                syncQueueFromPlayer()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                playbackPositionState.longValue = activePlayer.currentPosition.coerceAtLeast(0L)
                settingsStore.saveLastTrackPositionMs(playbackPositionState.longValue)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                playbackPositionState.longValue = activePlayer.currentPosition.coerceAtLeast(0L)
                settingsStore.saveLastTrackPositionMs(playbackPositionState.longValue)
                if (playbackState == Player.STATE_ENDED) playerEndedAt = System.currentTimeMillis()
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("PixelodyBasePlayback", "Media3 playback error: ${error.errorCodeName} (${error.errorCode})", error)
                val failure = playbackFailure(error)
                playbackError = failure.message
                if (failure.clearCredential) {
                    savedHostStore.clear()
                    hostBaseUrl = null
                }
                if (failure.connectionState != null) {
                    recoverHostConnection(failure.connectionState, failure.message)
                }
            }
        }
        activePlayer.addListener(listener)
        // Flow owns the linear queue order, including notification/headset Next.
        activePlayer.shuffleModeEnabled = false
        repeatMode = when (activePlayer.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.All
            Player.REPEAT_MODE_ONE -> RepeatMode.One
            else -> RepeatMode.Off
        }
        shuffleMode = FlowShuffleMode.fromSaved(settingsStore.loadShuffleMode()) ?: shuffleMode
        val activeMediaId = activePlayer.currentMediaItem?.mediaId
        isPlaying = activePlayer.isPlaying
        if (activeMediaId != null) {
            currentTrackId = activeMediaId
            lastTrackId = activeMediaId
        }
        syncQueueFromPlayer()
        onDispose {
            activePlayer.removeListener(listener)
        }
    }

    // Assemble current BaseLayerData snapshot
    val currentTracks = remember(hostTracks, localTracks, restoredPlaybackTracks, favoriteTrackIds, initialData, metadataOverrides) {
        val combined = (hostTracks + localTracks + restoredPlaybackTracks).distinctBy { it.id }
        val base = if (combined.isEmpty()) initialData.tracks else (combined + initialData.tracks).distinctBy { it.id }
        val overridden = metadataStore.applyOverrides(base)
        overridden.map { it.copy(favorite = favoriteTrackIds.contains(it.id)) }
    }
    val currentPlaylists = remember(hostPlaylists, userPlaylists, initialData) {
        (userPlaylists + hostPlaylists).distinctBy { it.id }
    }
    val currentCollections = remember(currentTracks, currentPlaylists, initialData) {
        if (currentTracks == initialData.tracks && initialData.collections.isNotEmpty()) {
            initialData.collections
        } else {
            buildBaseCollections(currentTracks, currentPlaylists)
        }
    }
    val phoneTrackIds = remember(localTracks, initialData) {
        if (localTracks.isNotEmpty()) localTracks.map { it.id }.toSet() else initialData.phoneTrackIds
    }
    val hostTrackIds = remember(hostTracks, initialData) {
        if (hostTracks.isNotEmpty()) hostTracks.map { it.id }.toSet() else initialData.hostTrackIds
    }

    /**
     * totalCacheSizeBytes() reads the catalog off disk and stats every cached file. It
     * was an argument evaluated on every recomposition, and the position ticker
     * recomposes this five times a second while music plays - so the app stat'd the
     * whole download cache on the main thread, five times a second, and got slower the
     * more the person downloaded. It only changes when the cache does.
     */
    val cacheSizeBytes = remember(cachedTrackIds) { offlineMediaStore.totalCacheSizeBytes() }

    val activeCrates = if (crateBook.crates.isNotEmpty()) crateBook else initialData.crates
    val musicCollections = remember(currentCollections, activeCrates) { listeningCollections(currentCollections, activeCrates) }
    val baseData = BaseLayerData(
        tracks = currentTracks,
        collections = musicCollections,
        crates = if (crateBook.crates.isNotEmpty()) crateBook else initialData.crates,
        covers = coverBook,
        phoneTrackIds = phoneTrackIds,
        hostTrackIds = hostTrackIds,
        jamTrackIds = availableJamSession?.takeIf { it.active }?.queue.orEmpty().map { it.trackId }.toSet(),
        downloadedTrackIds = cachedTrackIds,
        currentTrackId = currentTrackId,
        lastTrackId = lastTrackId,
        isPlaying = isPlaying,
        queue = queue,
        queueEntryIds = queueEntryIds,
        queueDecisionCues = queueDecisionCues,
        currentEntryId = currentEntryId,
        liveNextTrackId = liveNextTrackId,
        shuffleWhy = shuffleWhy,
        sessionLists = sessionLists,
        sessionEven = sessionEven,
        hostReachable = hostReachable,
        source = activeSource,
        lens = activeLens
    )

    LaunchedEffect(baseData) {
        onDataChanged(baseData)
    }

    // The service records intervals even when this UI is gone. These are projections
    // of its durable facts; disposing or re-creating the screen creates no outcome.
    val sessionInsights = remember(listeningSnapshot) {
        ListeningLedgerProjection.toInsights(listeningSnapshot, listeningSnapshot.recentEvents.lastOrNull()?.occurrence?.sessionId)
    }
    val dailyCapsule = remember(listeningSnapshot, listeningNow, currentTracks) {
        ListeningLedgerProjection.toDailyCapsule(listeningSnapshot, listeningNow, ZoneId.systemDefault(), currentTracks)
    }
    val weeklyTrend = remember(listeningSnapshot, listeningNow) {
        ListeningLedgerProjection.toWeeklyTrend(listeningSnapshot, listeningNow, ZoneId.systemDefault())
    }

    LaunchedEffect(player, currentTrackId, loudnessMode, sleepTimerState.volumeScale, currentTracks) {
        val activePlayer = player ?: return@LaunchedEffect
        val targetLufs = loudnessMode.targetLufs
        val trackGain = if (targetLufs != null) {
            val curr = currentTracks.firstOrNull { it.id == currentTrackId }
            if (curr != null) {
                val prof = AcousticTimbreEngine.estimateTrackLoudness(curr)
                AcousticTimbreEngine.calculateTargetGain(prof.integratedLufs, targetLufs).coerceIn(0.2f, 1.0f)
            } else 1.0f
        } else 1.0f
        activePlayer.volume = (trackGain * sleepTimerState.volumeScale).coerceIn(0.0f, 1.0f)
    }

    LaunchedEffect(player, playbackSpeed, pitchLocked) {
        val activePlayer = player ?: return@LaunchedEffect
        val pitch = if (pitchLocked) 1.0f else playbackSpeed
        activePlayer.playbackParameters = PlaybackParameters(playbackSpeed, pitch)
    }

    val effectiveLyricsTrackId = currentTrackId ?: (if (isPlaying) lastTrackId else null)
    LaunchedEffect(effectiveLyricsTrackId, currentTracks) {
        val id = effectiveLyricsTrackId
        if (id == null || id == "px-track-moonlit-circuit" || id == "t1") {
            currentLyrics = LyricsDocument.EMPTY
            return@LaunchedEffect
        }
        val curr = currentTracks.firstOrNull { it.id == id }
            ?: currentTracks.firstOrNull { it.streamUrl.isNotBlank() && it.streamUrl == id }
            ?: (player?.currentMediaItem?.mediaMetadata?.let { meta ->
                val metaTitle = meta.title?.toString()
                if (!metaTitle.isNullOrBlank()) {
                    currentTracks.firstOrNull { it.title.equals(metaTitle, ignoreCase = true) }
                } else null
            })
        if (curr != null) {
            // Immediately transition to new track placeholder so previous song's lyrics don't linger
            currentLyrics = LyricsDocument(
                title = curr.title,
                artist = curr.artist,
                album = curr.album,
                source = LyricsSource.None
            )
            val loaded = lyricsRepository.loadLyrics(curr)
            if (effectiveLyricsTrackId == id) {
                currentLyrics = loaded
            }
        } else {
            currentLyrics = LyricsDocument.EMPTY
        }
    }

    fun resolveTrack(id: String?): Track? {
        if (id == null) return null
        return baseData.track(id)
            ?: localTracks.firstOrNull { it.id == id }
            ?: hostTracks.firstOrNull { it.id == id }
            ?: initialData.track(id)
    }

    fun unskippableBlocks(): Boolean {
        if (!unskippableActive) return false
        Toast.makeText(context, "Unskippable is on. Leave the mix to skip, seek or change the queue.", Toast.LENGTH_SHORT).show()
        return true
    }

    fun startPlayback(trackIds: List<String>, startPositionMs: Long, mode: FlowShuffleMode = shuffleMode, unskippable: Boolean = false) {
        if (!unskippable && unskippableBlocks()) return
        val activePlayer = player ?: return
        if (trackIds.isEmpty()) return
        val tracksToPlay = trackIds.mapNotNull { resolveTrack(it) }
        if (tracksToPlay.isEmpty()) return

        queuePlanningJob?.cancel()
        pendingPlayback = trackIds to startPositionMs
        queuePlanningJob = coroutineScope.launch {
            val sourceKey = ShuffleSourceKeys.keyFor(tracksToPlay, localTracks, userPlaylists.map { it.id to it.trackIds })
            // A new Play or Shuffle replaces any linked session.
            sessionLists = emptyList()
            sessionEven = false
            val source = try {
                withContext(Dispatchers.IO) {
                    ensureActive()
                    ShuffleScopeIdentityStore(context).source(sourceKey, tracksToPlay.map { it.id })
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                pendingPlayback = null
                playbackError = "Saved queue memory could not be opened. Your previous queue has been kept."
                return@launch
            }
            if (player !== activePlayer) return@launch
            activePlayer.shuffleModeEnabled = false
            val ordered = tracksToPlay
            val sessionId = UUID.randomUUID().toString()
            val listKey = sourceKey ?: source.scopeId
            currentSourceKey = listKey
            currentSourceName = com.pixelody.app.core.playback.ShuffleLanes.listName(listKey, userPlaylists.map { it.id to it.name }, tracksToPlay.first().album)
            val mediaItems = ordered.mapIndexed { index, track ->
                val item = PlaybackEvidenceMetadata.withEntry(baseMediaItemFor(track, offlineMediaStore, hostBaseUrl), source.entryIds[index])
                val extras = Bundle(item.mediaMetadata.extras ?: Bundle()).apply {
                    putString(PlaybackEvidenceMetadata.SESSION_ID, sessionId)
                    putString(PlaybackEvidenceMetadata.SCOPE_ID, source.scopeId)
                    putString(PlaybackEvidenceMetadata.MODE, mode.name)
                    putString(PlaybackEvidenceMetadata.POLICY_ENTRY, UniversalShuffleMetadata.rawEntry(track, source.entryIds[index], listKey).toString())
                }
                item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(extras).build()).build()
            }
            activePlayer.setMediaItems(mediaItems, 0, startPositionMs.coerceAtLeast(0L))
            activePlayer.prepare()
            activePlayer.playWhenReady = true

            val firstTrack = ordered.first()
            currentTrackId = firstTrack.id
            lastTrackId = firstTrack.id
            isPlaying = true
            queue = ordered.drop(1).map { it.id }
            playbackPositionState.longValue = startPositionMs.coerceAtLeast(0L)
            settingsStore.saveLastTrackId(firstTrack.id)
            settingsStore.saveLastTrackPositionMs(startPositionMs.coerceAtLeast(0L))
            pendingPlayback = null
        }
    }

    /** Choosing something new to play starts it at the beginning. */
    fun playTracks(trackIds: List<String>) = startPlayback(trackIds, 0L)

    /**
     * Resume is the one-action journey the whole reach budget was spent on, so it has
     * to mean what it says. It used to run [playTracks], which starts at zero and then
     * saves zero over the stored position - the slot showed where you left off and the
     * track began again. Two cases now: the service already restored the item, in which
     * case play it where it sits; otherwise start it at the saved position.
     */
    fun resumeLast() {
        val resumeId = lastTrackId ?: currentTrackId ?: return
        val activePlayer = player
        if (activePlayer != null && activePlayer.currentMediaItem?.mediaId == resumeId) {
            activePlayer.play()
            isPlaying = true
            return
        }
        startPlayback(listOf(resumeId), settingsStore.loadLastTrackPositionMs())
    }

    fun togglePlay() {
        val activePlayer = player
        if (activePlayer == null) {
            val lastId = baseData.lastTrackId ?: baseData.currentTrackId
            if (lastId != null) playTracks(listOf(lastId))
            return
        }
        if (activePlayer.isPlaying) {
            activePlayer.pause()
            isPlaying = false
            settingsStore.saveLastTrackPositionMs(activePlayer.currentPosition.coerceAtLeast(0L))
        } else {
            if (activePlayer.currentMediaItem != null) {
                activePlayer.play()
                isPlaying = true
            } else {
                val resumeId = baseData.lastTrackId ?: baseData.currentTrackId
                if (resumeId != null) {
                    playTracks(listOf(resumeId))
                }
            }
        }
    }

    fun nextTrack() {
        if (unskippableBlocks()) return
        val activePlayer = player
        if (activePlayer != null && activePlayer.hasNextMediaItem()) {
            activePlayer.seekToNextMediaItem()
        } else if (baseData.queue.isNotEmpty()) {
            playTracks(baseData.queue)
        }
    }

    fun addTracksToQueue(trackIds: List<String>) {
        if (unskippableBlocks()) return
        val tracks = trackIds.mapNotNull(::resolveTrack)
        if (tracks.isEmpty()) return
        val activePlayer = player
        if (activePlayer == null) {
            queue = queue + tracks.map { it.id }
            return
        }
        // One timeline update avoids rescanning the queue after every matched song.
        activePlayer.addMediaItems(tracks.map { baseMediaItemFor(it, offlineMediaStore, hostBaseUrl) })
        syncQueueFromPlayer()
    }

    fun addToQueue(trackId: String) = addTracksToQueue(listOf(trackId))

    fun sessionKeyFor(trackIds: List<String>): String? =
        ShuffleSourceKeys.keyFor(trackIds.mapNotNull(::resolveTrack), localTracks, userPlaylists.map { it.id to it.trackIds })

    fun sessionListName(key: String, albumTitle: String? = null) =
        com.pixelody.app.core.playback.ShuffleLanes.listName(key, userPlaylists.map { it.id to it.name }, albumTitle)

    /** A list can join the shuffle playing now when it is a whole list that isn't already in it. */
    fun canAddToSession(trackIds: List<String>): Boolean {
        val activePlayer = player ?: return false
        if (shuffleMode == FlowShuffleMode.Off || activePlayer.mediaItemCount == 0 || trackIds.isEmpty()) return false
        // After a restart the playing list isn't known until the next Play or Shuffle.
        if (currentSourceKey == null) return false
        val key = sessionKeyFor(trackIds) ?: return false
        return key != currentSourceKey && sessionLists.none { it.sourceKey == key }
    }

    /**
     * Add to session: the list's songs join this shuffle as their own list, so
     * the lists take turns instead of the new songs being queued next.
     */
    fun addCollectionToSession(trackIds: List<String>, name: String?) {
        if (unskippableBlocks()) return
        val activePlayer = player as? MediaController ?: return
        if (!canAddToSession(trackIds)) return
        val key = sessionKeyFor(trackIds) ?: return
        val first = currentSourceKey
        val items = trackIds.mapNotNull(::resolveTrack).map { track ->
            val item = baseMediaItemFor(track, offlineMediaStore, hostBaseUrl)
            val extras = Bundle(item.mediaMetadata.extras ?: Bundle()).apply {
                putBoolean(PlaybackEvidenceMetadata.ADD_TO_SESSION, true)
                putString(PlaybackEvidenceMetadata.POLICY_ENTRY, UniversalShuffleMetadata.rawEntry(track, track.id, key).toString())
            }
            item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(extras).build()).build()
        }
        if (sessionLists.isEmpty()) {
            // A fresh link starts with turns by size; the queue screen can make them even.
            activePlayer.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY), Bundle().apply { putString("action", "airtime"); putBoolean("even", false) })
            sessionLists = listOfNotNull(first?.let { com.pixelody.app.core.playback.SessionList(it, currentSourceName) })
            sessionEven = false
        }
        activePlayer.addMediaItems(items)
        val title = name?.takeIf { it.isNotBlank() } ?: sessionListName(key, items.firstOrNull()?.mediaMetadata?.albumTitle?.toString())
        sessionLists = sessionLists + com.pixelody.app.core.playback.SessionList(key, title)
    }

    fun removeSessionList(key: String) {
        if (unskippableBlocks()) return
        val activePlayer = player as? MediaController ?: return
        activePlayer.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY), Bundle().apply { putString("action", "remove-source"); putString("sourceKey", key) })
        val left = sessionLists.filterNot { it.sourceKey == key }
        if (key == currentSourceKey) {
            currentSourceKey = left.firstOrNull()?.sourceKey
            currentSourceName = left.firstOrNull()?.name ?: "This queue"
        }
        // One list left is an ordinary single-list shuffle again.
        sessionLists = if (left.size > 1) left else emptyList()
    }

    fun setSessionEven(even: Boolean) {
        val activePlayer = player as? MediaController ?: return
        activePlayer.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY), Bundle().apply { putString("action", "airtime"); putBoolean("even", even) })
        sessionEven = even
    }

    fun seek(positionMs: Long) {
        if (unskippableBlocks()) return
        val activePlayer = player ?: return
        activePlayer.seekTo(positionMs)
        playbackPositionState.longValue = positionMs
        settingsStore.saveLastTrackPositionMs(positionMs)
    }

    fun previousTrack() {
        if (unskippableBlocks()) return
        val activePlayer = player
        if (activePlayer != null && activePlayer.hasPreviousMediaItem()) {
            activePlayer.seekToPreviousMediaItem()
        } else if (activePlayer != null && activePlayer.currentPosition > 3000L) {
            activePlayer.seekTo(0L)
            playbackPositionState.longValue = 0L
        }
    }

    fun cycleEqualizerPreset() {
        val nextPreset = EqualizerPreset.nextQuickPreset(globalEqualizer.preset)
        val updated = globalEqualizer.withPreset(nextPreset)
        globalEqualizer = updated
        equalizerStore.saveGlobalProfile(updated)
    }

    /**
     * Replan only upcoming music. Keep the current item, position, pause state, and
     * played history intact. Off freezes the current order; new plays use source order.
     */
    fun setShuffleMode(nextMode: FlowShuffleMode) {
        if (unskippableBlocks()) return
        if (nextMode == shuffleMode) return
        shuffleMode = nextMode
        queuePlanningJob?.cancel()
        val activePlayer = player
        val requested = pendingPlayback
        if (requested != null) {
            // Changing mode during a large-library plan must retain the Play request.
            startPlayback(requested.first, requested.second, nextMode)
        } else if (activePlayer != null) {
            (activePlayer as? MediaController)?.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_MODE, Bundle.EMPTY), Bundle().apply {
                putString("mode", nextMode.name)
            })
        }
        settingsStore.saveShuffleMode(nextMode.name)
        settingsStore.saveShuffleEnabled(nextMode != FlowShuffleMode.Off)
    }

    fun toggleShuffle() = setShuffleMode(shuffleMode.next())

    fun toggleRepeat() {
        if (unskippableBlocks()) return
        val nextMode = when (repeatMode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        repeatMode = nextMode
        player?.let { it.repeatMode = playerRepeatModeFor(nextMode) }
        settingsStore.saveRepeatMode(nextMode.ordinal)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (unskippableBlocks()) return
        if (fromIndex == toIndex) return
        if (fromIndex !in queue.indices || toIndex !in queue.indices) return
        val activePlayer = player
        if (activePlayer == null) {
            val mutable = queue.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            queue = mutable
            return
        }
        val offset = activePlayer.currentMediaItemIndex + 1
        val count = activePlayer.mediaItemCount
        val from = offset + fromIndex
        val to = offset + toIndex
        if (from < count && to < count) {
            activePlayer.moveMediaItem(from, to)
            syncQueueFromPlayer()
        }
    }

    /**
     * Drag-to-reorder hands back the whole list rather than a pair of indices, so this
     * walks it and moves each id into place. QueueScreen has had an onReorderQueue
     * parameter all along and nothing ever passed it.
     */
    fun setQueueOrder(trackIds: List<String>) {
        if (unskippableBlocks()) return
        val activePlayer = player
        if (activePlayer == null) {
            queue = trackIds
            return
        }
        val start = activePlayer.currentMediaItemIndex + 1
        trackIds.forEachIndexed { offset, id ->
            val destination = start + offset
            if (destination < activePlayer.mediaItemCount) {
                val found = (destination until activePlayer.mediaItemCount)
                    .firstOrNull { activePlayer.getMediaItemAt(it).mediaId == id }
                if (found != null && found != destination) {
                    activePlayer.moveMediaItem(found, destination)
                }
            }
        }
        syncQueueFromPlayer()
    }

    /**
     * Undo puts a track back where it was taken from, not on the end. Restoring it to a
     * different position is a quieter kind of wrong than not restoring it at all.
     */
    fun insertIntoQueue(trackId: String, position: Int) {
        if (unskippableBlocks()) return
        val track = resolveTrack(trackId) ?: return
        val activePlayer = player
        if (activePlayer == null) {
            val mutable = queue.toMutableList()
            mutable.add(position.coerceIn(0, mutable.size), trackId)
            queue = mutable
            return
        }
        val start = activePlayer.currentMediaItemIndex + 1
        val index = (start + position).coerceIn(start, activePlayer.mediaItemCount)
        activePlayer.addMediaItem(index, baseMediaItemFor(track, offlineMediaStore, hostBaseUrl))
        syncQueueFromPlayer()
    }

    fun removeQueueItem(entryOrTrackId: String) {
        if (unskippableBlocks()) return
        val activePlayer = player
        if (activePlayer == null) {
            val index = queueEntryIds.indexOf(entryOrTrackId).takeIf { it >= 0 } ?: queue.indexOf(entryOrTrackId)
            if (index >= 0) {
                queue = queue.toMutableList().apply { removeAt(index) }
                if (index in queueEntryIds.indices) queueEntryIds = queueEntryIds.toMutableList().apply { removeAt(index) }
            }
            return
        }
        val start = activePlayer.currentMediaItemIndex + 1
        val index = (start until activePlayer.mediaItemCount)
            .firstOrNull { PlaybackEvidenceMetadata.entryId(activePlayer.getMediaItemAt(it)) == entryOrTrackId }
            ?: (start until activePlayer.mediaItemCount).firstOrNull { activePlayer.getMediaItemAt(it).mediaId == entryOrTrackId }
        if (index != null) activePlayer.removeMediaItem(index)
        syncQueueFromPlayer()
    }

    fun removeQueueItemAt(position: Int) {
        if (unskippableBlocks()) return
        if (position !in queue.indices) return
        val activePlayer = player
        if (activePlayer == null) {
            queue = queue.filterIndexed { index, _ -> index != position }
            return
        }
        val index = activePlayer.currentMediaItemIndex + 1 + position
        if (index in 0 until activePlayer.mediaItemCount) activePlayer.removeMediaItem(index)
        syncQueueFromPlayer()
    }

    fun clearQueue() {
        if (unskippableBlocks()) return
        val activePlayer = player
        if (activePlayer == null) {
            queue = emptyList()
            return
        }
        (activePlayer as? MediaController)?.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY), Bundle().apply { putString("action", "clear-future") })
        syncQueueFromPlayer()
    }

    fun toggleFavorite(trackId: String) {
        settingsStore.toggleTrackFavorite(trackId)
        favoriteTrackIds = settingsStore.loadFavoriteTrackIds()
    }

    fun addToPlaylist(playlistId: String, trackId: String) {
        if (playlistId.startsWith("crate:")) {
            val crate = crateBook.crates.firstOrNull { "crate:${it.id}" == playlistId } ?: return
            val slot = com.pixelody.app.data.model.CrateSlot.SingleTrack(trackId)
            if (crate.holds(slot) || musicCollections.firstOrNull { it.id == playlistId }?.trackIds?.contains(trackId) == true) return
            val updated = crate.addingToFirstEmpty(slot)
            if (updated == null) {
                android.widget.Toast.makeText(context, "This collection is full. Organize it to make room.", android.widget.Toast.LENGTH_LONG).show()
            } else {
                crateBook = crateBook.replacing(updated)
                crateStore.save(crateBook)
            }
            return
        }
        playlistStore.addTrackToPlaylist(playlistId, trackId)
        userPlaylists = playlistStore.load()
    }

    fun createPlaylist(name: String, trackId: String) {
        playlistStore.createPlaylist(name, listOf(trackId))
        userPlaylists = playlistStore.load()
    }

    fun createPlaylistWithTracks(name: String, trackIds: List<String>) {
        playlistStore.createPlaylist(name, trackIds)
        userPlaylists = playlistStore.load()
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        playlistStore.renamePlaylist(playlistId, newName)
        userPlaylists = playlistStore.load()
    }

    fun deletePlaylist(playlistId: String) {
        playlistStore.deletePlaylist(playlistId)
        userPlaylists = playlistStore.load()
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistStore.removeTrackFromPlaylist(playlistId, trackId)
        userPlaylists = playlistStore.load()
    }

    fun playNext(trackId: String) {
        if (unskippableBlocks()) return
        insertIntoQueue(trackId, 0)
    }

    fun playQueueEntry(entryId: String) {
        if (unskippableBlocks()) return
        val active = player ?: return
        val index = (active.currentMediaItemIndex + 1 until active.mediaItemCount)
            .firstOrNull { PlaybackEvidenceMetadata.entryId(active.getMediaItemAt(it)) == entryId } ?: return
        active.seekTo(index, 0)
        active.play()
    }

    fun restoreQueueEntry(trackId: String, entryId: String, position: Int) {
        if (unskippableBlocks()) return
        val active = player ?: return
        val track = resolveTrack(trackId) ?: return
        val start = active.currentMediaItemIndex + 1
        val item = PlaybackEvidenceMetadata.withEntry(baseMediaItemFor(track, offlineMediaStore, hostBaseUrl), entryId)
        active.addMediaItem((start + position).coerceIn(start, active.mediaItemCount), item)
        syncQueueFromPlayer()
    }

    fun sendUnskippableLock(locked: Boolean) {
        (player as? MediaController)?.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY),
            Bundle().apply { putString("action", "unskippable-lock"); putBoolean("locked", locked) })
    }

    fun saveUnskippableProgress() {
        val id = unskippableCoverId ?: return
        if (!unskippableActive) return
        val activePlayer = player ?: return
        val trackId = activePlayer.currentMediaItem?.mediaId ?: return
        val positionMs = activePlayer.currentPosition.coerceAtLeast(0L)
        unskippableStore.update(id) { if (trackId in it.trackIds) it.copy(progressTrackId = trackId, progressPositionMs = positionMs) else it }
        unskippableMixes = unskippableStore.load()
    }

    // The mix plays in order with repeat off; shuffle and repeat come back the way
    // they were when you leave or the mix ends.
    fun beginUnskippable(fromStart: Boolean) {
        val mix = unskippableMixes.firstOrNull { it.id == unskippableCoverId } ?: return
        if (player == null) { Toast.makeText(context, "Playback is still connecting. Try again in a moment.", Toast.LENGTH_SHORT).show(); return }
        val available = { id: String -> resolveTrack(id)?.missing == false }
        val point = UnskippableMix.resumePoint(mix.trackIds, if (fromStart) null else mix.progressTrackId, if (fromStart) 0L else mix.progressPositionMs, available)
        if (point == null) { Toast.makeText(context, "None of this mix's songs are available.", Toast.LENGTH_SHORT).show(); return }
        unskippablePrior = unskippablePrior ?: (shuffleMode to repeatMode)
        shuffleMode = FlowShuffleMode.Off
        repeatMode = RepeatMode.Off
        player?.repeatMode = Player.REPEAT_MODE_OFF
        unskippableFinished = false
        startPlayback(mix.trackIds.drop(point.index).filter(available), point.positionMs, FlowShuffleMode.Off, unskippable = true)
        unskippableActive = true
        sendUnskippableLock(true)
    }

    fun endUnskippableSession() {
        unskippableActive = false
        sendUnskippableLock(false)
        unskippablePrior?.let { (mode, repeat) ->
            shuffleMode = mode
            repeatMode = repeat
            player?.repeatMode = playerRepeatModeFor(repeat)
            (player as? MediaController)?.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_MODE, Bundle.EMPTY), Bundle().apply { putString("mode", mode.name) })
        }
        unskippablePrior = null
    }

    fun leaveUnskippable() {
        if (unskippableActive) {
            saveUnskippableProgress()
            player?.pause()
            isPlaying = false
            endUnskippableSession()
        }
        unskippableCoverId = null
        unskippableFinished = false
    }

    fun finishUnskippable() {
        val id = unskippableCoverId ?: return
        if (!unskippableActive) return
        unskippableStore.update(id) { it.copy(progressTrackId = null, progressPositionMs = 0L, lastFinishedAtMs = System.currentTimeMillis()) }
        unskippableMixes = unskippableStore.load()
        endUnskippableSession()
        unskippableFinished = true
    }

    fun openUnskippable(id: String) {
        if (unskippableCoverId != null && unskippableCoverId != id) leaveUnskippable()
        unskippableCoverId = id
        unskippableFinished = false
    }

    LaunchedEffect(playerEndedAt) {
        if (playerEndedAt > 0L && unskippableActive) finishUnskippable()
    }
    LaunchedEffect(unskippableActive, isPlaying) {
        while (unskippableActive && isPlaying) {
            delay(5000)
            saveUnskippableProgress()
        }
    }

    Box(modifier = modifier) {
    PixelodyBaseShell(
        unskippableMixes = unskippableMixes,
        unskippableDurationOf = { mix -> mix.trackIds.sumOf { (resolveTrack(it)?.durationSeconds ?: 0).toLong() } * 1000L },
        onMakeUnskippable = { unskippableBuilderSeed = null; unskippableBuilderOpen = true },
        onOpenUnskippable = ::openUnskippable,
        onResetDeveloperData = { includeTrackEdits ->
            val resetPlayer = checkNotNull(player) { "Playback is still connecting; try again" }
            queuePlanningJob?.cancel()
            pendingPlayback = null
            resetPlayer.pause()
            resetPlayer.stop()
            resetPlayer.clearMediaItems()
            currentTrackId = null
            queue = emptyList()
            playbackPositionState.longValue = 0L
            lastTrackId = null
            settingsStore.saveLastTrackId(null)
            settingsStore.saveLastTrackPositionMs(0L)
            val controller = checkNotNull(resetPlayer as? MediaController)
            val reset = controller.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY),
                Bundle().apply { putString("action", "reset-developer-data") })
            val result = withContext(Dispatchers.IO) { reset.get(15, java.util.concurrent.TimeUnit.SECONDS) }
            check(result.resultCode == SessionResult.RESULT_SUCCESS) { "Listening data could not be reset" }
            com.pixelody.app.core.playback.HarmonicKeyEngine.clearCache()
            persistenceRepository.clearListeningArchives()
            if (includeTrackEdits) metadataStore.clearAllOverrides()
        },
        onShuffleAction = { action ->
            if (action == "import-learning") importLearningDocument.launch(arrayOf("application/json", "text/plain"))
            else {
                val future = (player as? MediaController)?.sendCustomCommand(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY), Bundle().apply { putString("action", action) })
                if (action == "export-learning" && future != null) coroutineScope.launch {
                    val json = withContext(Dispatchers.IO) { runCatching { future.get(15, java.util.concurrent.TimeUnit.SECONDS).extras.getString("json") }.getOrNull() }
                    if (json != null) { pendingLearningExport = json; exportLearningDocument.launch("pixelody-learning.json") }
                }
            }
        },
        onShuffleTracks = { ids ->
            val mode = shuffleMode.takeUnless { it == FlowShuffleMode.Off } ?: FlowShuffleMode.Balanced
            shuffleMode = mode
            settingsStore.saveShuffleMode(mode.name)
            settingsStore.saveShuffleEnabled(true)
            startPlayback(ids, 0L, mode)
        },
        onPlayInOrder = { ids ->
            shuffleMode = FlowShuffleMode.Off
            settingsStore.saveShuffleMode(FlowShuffleMode.Off.name)
            settingsStore.saveShuffleEnabled(false)
            startPlayback(ids, 0L, FlowShuffleMode.Off)
        },
        data = baseData,
        localTracks = localTracks,
        sessionInsights = sessionInsights,
        dailyCapsule = dailyCapsule,
        weeklyTrend = weeklyTrend,
        onPlayTracks = ::playTracks,
        onTogglePlay = ::togglePlay,
        onToggleFavorite = ::toggleFavorite,
        onAddToPlaylist = ::addToPlaylist,
        onCreatePlaylist = ::createPlaylist,
        onPlayNext = ::playNext,
        onRenamePlaylist = ::renamePlaylist,
        onDeletePlaylist = ::deletePlaylist,
        onRemoveTrackFromPlaylist = ::removeTrackFromPlaylist,
        onCreatePlaylistWithTracks = ::createPlaylistWithTracks,
        sleepTimerState = sleepTimerState,
        onSetSleepTimer = { mode, remainingSecs ->
            sleepTimerController.setTimer(mode, remainingSecs)
        },
        playbackSpeed = playbackSpeed,
        pitchLocked = pitchLocked,
        onSpeedPitchChange = ::updateSpeedAndPitch,
        loudnessMode = loudnessMode,
        onLoudnessModeChange = ::setLoudnessMode,
        lyricsDocument = currentLyrics,
        onResume = ::resumeLast,
        onPrevious = ::previousTrack,
        onNext = ::nextTrack,
        onSeek = ::seek,
        onAddToQueue = ::addToQueue,
        onAddTracksToQueue = ::addTracksToQueue,
        onMoveQueueItem = ::moveQueueItem,
        onReorderQueue = ::setQueueOrder,
        onInsertIntoQueue = ::insertIntoQueue,
        onRemoveQueueItem = ::removeQueueItem,
        onRemoveQueueItemAt = ::removeQueueItemAt,
        onPlayQueueEntry = ::playQueueEntry,
        canAddToSession = ::canAddToSession,
        onAddToSession = ::addCollectionToSession,
        onRemoveSessionList = ::removeSessionList,
        onSessionEven = ::setSessionEven,
        onRestoreQueueEntry = ::restoreQueueEntry,
        onClearQueue = ::clearQueue,
        equalizerRuntimeState = equalizerRuntime,
        equalizerProfile = globalEqualizer,
        onEqualizerChange = { eq ->
            globalEqualizer = eq
            equalizerStore.saveGlobalProfile(eq)
        },
        trackEqualizers = trackEqualizers,
        onTrackEqualizerChange = { id, profile ->
            trackEqualizers = if (profile == null) trackEqualizers - id else trackEqualizers + (id to profile.normalized())
            equalizerStore.saveTrackProfiles(trackEqualizers)
        },
        useMasteringRack = useMasteringRack,
        onUseMasteringRackChange = { enabled ->
            useMasteringRack = enabled
            equalizerStore.saveUseMasteringRack(enabled)
        },
        onCycleEqualizerPreset = ::cycleEqualizerPreset,
        globalMastering = globalMastering,
        onMasteringChange = { m ->
            globalMastering = m
            equalizerStore.saveGlobalMasteringProfile(m)
        },
        spatialSettings = spatialSettings,
        onSpatialSettingsChange = { sp ->
            spatialSettings = sp
            equalizerStore.saveSpatialSettings(sp)
        },
        cassetteSettings = cassetteSettings,
        onCassetteSettingsChange = { cassetteSettings = it },
        stemsSettings = stemsSettings,
        onStemsSettingsChange = { stemsSettings = it },
        oscilloscopeSettings = oscilloscopeSettings,
        onOscilloscopeSettingsChange = { oscilloscopeSettings = it },
        autoDjSettings = autoDjSettings,
        onAutoDjSettingsChange = { autoDjSettings = it },
        audioHapticSettings = audioHapticSettings,
        onAudioHapticSettingsChange = { audioHapticSettings = it },
        hiResSettings = hiResSettings,
        onHiResSettingsChange = { hiResSettings = it },
        positionMs = 0L,
        positionMsProvider = { playbackPositionState.longValue },
        durationMs = playbackDurationMs,
        shuffleEnabled = shuffleEnabled,
        shuffleMode = shuffleMode,
        onToggleShuffle = ::toggleShuffle,
        onShuffleModeChange = ::setShuffleMode,
        repeatMode = repeatMode,
        onToggleRepeat = ::toggleRepeat,
        onSourceChange = { source ->
            activeSource = source
            settingsStore.saveLastSourceScope(source.key)
        },
        onLensChange = { lens ->
            activeLens = lens
        },
        onRetryHost = { refreshHostLibrary() },
        onConnectHost = ::connectHostPayload,
        hostConnectionState = hostConnectionState,
        hostConnectionError = hostConnectionError,
        savedHostProfile = savedHostProfile,
        availableJamSession = availableJamSession,
        hostLiveState = hostLiveState,
        joiningJam = joiningJam,
        jamIssue = jamIssue,
        onJoinDesktopJam = ::joinDesktopJam,
        jamCoordinator = jamCoordinator,
        isLoadingLocalLibrary = isLoadingLocalLibrary,
        onCratesChange = { newCrates ->
            crateBook = newCrates
            crateStore.save(newCrates)
        },
        onCoversChange = { newCovers ->
            coverBook = newCovers
            coverStore.save(newCovers)
            coverStore.prune(newCovers)
        },
        coverStore = coverStore,
        isScanningDevice = isScanningDevice,
        deviceScanNotice = deviceScanNotice,
        playbackError = playbackError,
        onScanDevice = ::triggerDeviceScan,
        onPickFolder = { localFolderScanner.launch(null) },
        onPickFiles = { localAudioPicker.launch(arrayOf("audio/*")) },
        hostBaseUrl = hostBaseUrl,
        offlineCacheSizeBytes = cacheSizeBytes,
        onForgetHost = {
            savedHostStore.clear()
            hostBaseUrl = null
            refreshHostLibrary()
        },
        onClearCache = {
            offlineMediaStore.clearAllCache()
            cachedTrackIds = offlineMediaStore.listCachedTracks().map { it.id }.toSet()
        },
        activeTheme = activeTheme,
        onThemeChange = onThemeChange,
        onResetTheme = onThemeReset,
        activePlayerViewMode = activePlayerViewMode,
        onPlayerViewModeChange = { mode ->
            activePlayerViewMode = ordinaryPlayerViewMode(mode)
            settingsStore.savePlayerViewMode(activePlayerViewMode)
        },
        incomingDeepLink = incomingDeepLink,
        incomingSearchQuery = incomingSearchQuery,
        onDeepLinkConsumed = onDeepLinkConsumed,
        modifier = Modifier.fillMaxSize()
    )
    if (unskippableBuilderOpen) {
        UnskippableBuilder(
            tracks = baseData.tracks,
            seedRecipe = unskippableBuilderSeed,
            uniqueName = { base ->
                val taken = (unskippableMixes.map { it.name } + userPlaylists.map { it.name }).map { it.lowercase() }.toSet()
                generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base $it" }.first { it.lowercase() !in taken }
            },
            onTagGenre = { ids, genre ->
                ids.forEach { id -> metadataStore.saveOverride((metadataStore.getOverride(id) ?: TrackMetadataOverride(id)).copy(genre = genre, updatedAt = System.currentTimeMillis())) }
                Toast.makeText(context, "Tagged ${ids.size} song${if (ids.size == 1) "" else "s"} as $genre.", Toast.LENGTH_SHORT).show()
            },
            onSave = { name, preview ->
                val record = unskippableStore.create(name, preview, System.currentTimeMillis())
                unskippableMixes = unskippableStore.load()
                unskippableBuilderOpen = false
                Toast.makeText(context, "Saved ${record.name}.", Toast.LENGTH_SHORT).show()
                openUnskippable(record.id)
            },
            onDismiss = { unskippableBuilderOpen = false }
        )
    }
    unskippableMixes.firstOrNull { it.id == unskippableCoverId }?.let { mix ->
        UnskippableCover(
            mix = mix,
            trackById = ::resolveTrack,
            currentTrackId = currentTrackId,
            active = unskippableActive,
            finished = unskippableFinished,
            isPlaying = isPlaying,
            progress = { if (playbackDurationMs > 0) (playbackPositionState.longValue.toFloat() / playbackDurationMs).coerceIn(0f, 1f) else 0f },
            onToggle = { if (unskippableActive) togglePlay() else beginUnskippable(fromStart = false) },
            onRestart = { beginUnskippable(fromStart = true) },
            onLeave = ::leaveUnskippable
        )
    }
    }
}
