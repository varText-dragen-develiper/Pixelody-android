package com.pixelody.app.feature.baselayer

import com.pixelody.app.BuildConfig
import kotlinx.coroutines.CancellationException
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.network.connectionStateFor
import com.pixelody.app.data.network.hostConnectionFailureMessage
import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import com.pixelody.app.core.analytics.SessionCapsuleEngine
import com.pixelody.app.core.analytics.SonicCapsuleTimelineEngine
import com.pixelody.app.core.playback.AcousticTimbreEngine
import com.pixelody.app.data.storage.LoudnessNormalizationMode
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.pixelody.app.core.playback.PixelodyPlaybackService
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
import com.pixelody.app.core.playback.FlowShuffleEngine
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
        else -> "HI-RES"
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(track.title)
        .setArtist(track.artist)
        .setAlbumTitle(track.album)
        .setSubtitle(formatDisplay)
        .setArtworkUri(artworkUrl?.let(Uri::parse))
        .build()
    return MediaItem.Builder()
        .setUri(streamUrl)
        .setMimeType(mediaMimeType(track.format))
        .setMediaId(track.id)
        .setMediaMetadata(metadata)
        .build()
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
/** Below this, a play is a mis-tap rather than a listen and is not recorded. */
private const val MIN_RECORDED_LISTEN_MS = 5_000L
private const val LISTEN_TICK_MS = 1_000L
private const val COMPLETION_FRACTION = 0.9f

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
    val crateStore = remember { CrateStore(context.applicationContext) }
    val coverStore = remember { CoverStore(context.applicationContext) }
    val equalizerStore = remember { MobileEqualizerStore(context.applicationContext) }
    val persistenceRepository = remember { PixelodyPersistenceRepository(context.applicationContext) }
    val sessionCapsuleEngine = remember { SessionCapsuleEngine() }
    val sonicTimelineEngine = remember { SonicCapsuleTimelineEngine() }
    val mediaStoreAudioRepository = remember { MediaStoreAudioRepository(context.applicationContext, persistenceRepository) }
    val metadataStore = remember { TrackMetadataStore(context.applicationContext) }
    val metadataOverrides by metadataStore.overridesFlow.collectAsState()

    var player by remember { mutableStateOf<Player?>(null) }
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

    // Hardware DSP, Mastering, Equalizer & Console state
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
            FlowShuffleMode.values().firstOrNull { it.name == settingsStore.loadShuffleMode() }
                ?: if (settingsStore.loadShuffleEnabled()) FlowShuffleMode.SmartFlow else FlowShuffleMode.Off
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
            return
        }
        val start = activePlayer.currentMediaItemIndex + 1
        queue = (start until count).map { activePlayer.getMediaItemAt(it).mediaId }
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

    fun refreshHostLibrary(details: HostConnectionDetails? = incomingConnectionDetails) {
        if (isConnectingHost) return
        val profile = savedHostStore.load()
        if (details == null && profile == null && !BuildConfig.ENABLE_DEMO_LIBRARY) {
            hostConnectionState = HostConnectionState.Disconnected
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
                hostReachable = false
                hostConnectionState = connectionStateFor(error)
                hostConnectionError = hostConnectionFailureMessage(error)
            } finally {
                isConnectingHost = false
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
            { player = runCatching { controllerFuture.get() }.getOrNull() },
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
                    hostReachable = false
                }
            }
        }
        activePlayer.addListener(listener)
        // Flow owns the linear queue order, including notification/headset Next.
        activePlayer.shuffleModeEnabled = false
        activePlayer.repeatMode = playerRepeatModeFor(repeatMode)
        val activeMediaId = activePlayer.currentMediaItem?.mediaId
        isPlaying = activePlayer.isPlaying
        if (activeMediaId != null) {
            currentTrackId = activeMediaId
            lastTrackId = activeMediaId
        }
        onDispose {
            activePlayer.removeListener(listener)
        }
    }

    // Assemble current BaseLayerData snapshot
    val currentTracks = remember(hostTracks, localTracks, favoriteTrackIds, initialData, metadataOverrides) {
        val combined = (hostTracks + localTracks).distinctBy { it.id }
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
        jamTrackIds = initialData.jamTrackIds,
        downloadedTrackIds = cachedTrackIds,
        currentTrackId = currentTrackId,
        lastTrackId = lastTrackId,
        isPlaying = isPlaying,
        queue = queue,
        hostReachable = hostReachable,
        source = activeSource,
        lens = activeLens
    )

    LaunchedEffect(baseData) {
        onDataChanged(baseData)
    }

    val sessionInsights by sessionCapsuleEngine.insights.collectAsState()
    val dailyCapsule by sonicTimelineEngine.dailyCapsule.collectAsState()
    val weeklyTrend by sonicTimelineEngine.weeklyTrend.collectAsState()

    /**
     * Records what was actually played.
     *
     * Home's capsule and insights used to be manufactured from the library, so
     * they reported every track owned as a track played. Nothing recorded a real
     * listen because neither engine had a call site. This is that call site.
     *
     * It has to sit here rather than in the Player.Listener because the listener
     * is constructed before currentTracks exists, so it cannot resolve an id to
     * a Track. Time is accumulated only while isPlaying, and flushed in finally
     * so a track change, a stop, or this host leaving composition all record the
     * listen that just happened.
     */
    LaunchedEffect(currentTrackId) {
        val id = currentTrackId ?: return@LaunchedEffect
        var playedMs = 0L
        try {
            while (true) {
                delay(LISTEN_TICK_MS)
                if (isPlaying) playedMs += LISTEN_TICK_MS
            }
        } finally {
            if (playedMs >= MIN_RECORDED_LISTEN_MS) {
                val played = currentTracks.firstOrNull { it.id == id }
                if (played != null) {
                    val seconds = (playedMs / 1000L).toInt()
                    val completed = played.durationSeconds > 0 &&
                        seconds >= (played.durationSeconds * COMPLETION_FRACTION).toInt()
                    sonicTimelineEngine.recordTrackListen(
                        track = played,
                        listenedSeconds = seconds,
                        isCompleted = completed,
                        isBitPerfect = played.lossless
                    )
                    sessionCapsuleEngine.recordTrackPlay(played)
                }
            }
        }
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

    fun startPlayback(trackIds: List<String>, startPositionMs: Long, mode: FlowShuffleMode = shuffleMode) {
        val activePlayer = player ?: return
        if (trackIds.isEmpty()) return
        val tracksToPlay = trackIds.mapNotNull { resolveTrack(it) }
        if (tracksToPlay.isEmpty()) return

        queuePlanningJob?.cancel()
        pendingPlayback = trackIds to startPositionMs
        queuePlanningJob = coroutineScope.launch {
            val ordered = withContext(Dispatchers.Default) {
                if (mode == FlowShuffleMode.Off) return@withContext tracksToPlay
                val anchor = tracksToPlay.first()
                listOf(anchor) + FlowShuffleEngine.planQueue(anchor, tracksToPlay, mode, tracksToPlay.size,
                    checkActive = { ensureActive() }).map { it.track }
            }
            if (player !== activePlayer) return@launch
            activePlayer.shuffleModeEnabled = false
            val mediaItems = ordered.map { baseMediaItemFor(it, offlineMediaStore, hostBaseUrl) }
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
        val activePlayer = player
        if (activePlayer != null && activePlayer.hasNextMediaItem()) {
            activePlayer.seekToNextMediaItem()
        } else if (baseData.queue.isNotEmpty()) {
            playTracks(baseData.queue)
        }
    }

    fun addTracksToQueue(trackIds: List<String>) {
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

    fun seek(positionMs: Long) {
        val activePlayer = player ?: return
        activePlayer.seekTo(positionMs)
        playbackPositionState.longValue = positionMs
        settingsStore.saveLastTrackPositionMs(positionMs)
    }

    fun previousTrack() {
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
        if (nextMode == shuffleMode) return
        shuffleMode = nextMode
        queuePlanningJob?.cancel()
        val activePlayer = player
        val requested = pendingPlayback
        if (requested != null) {
            // Changing mode during a large-library plan must retain the Play request.
            startPlayback(requested.first, requested.second, nextMode)
        } else if (nextMode != FlowShuffleMode.Off && activePlayer != null && activePlayer.currentMediaItemIndex >= 0) {
            activePlayer.shuffleModeEnabled = false
            val start = activePlayer.currentMediaItemIndex + 1
            val items = (start until activePlayer.mediaItemCount).map { activePlayer.getMediaItemAt(it) }
            val anchorId = activePlayer.currentMediaItem?.mediaId
            val anchor = resolveTrack(anchorId)
            val tracks = items.map { resolveTrack(it.mediaId) }
            queuePlanningJob = coroutineScope.launch {
                val planned = withContext(Dispatchers.Default) {
                    FlowShuffleEngine.planUpcomingOrder(anchor, tracks, nextMode,
                        checkActive = { ensureActive() })
                }
                // A skip, queue edit, or external controller invalidates this snapshot.
                if (player !== activePlayer || activePlayer.currentMediaItem?.mediaId != anchorId ||
                    activePlayer.currentMediaItemIndex + 1 != start ||
                    (start until activePlayer.mediaItemCount).map { activePlayer.getMediaItemAt(it) } != items) return@launch
                val reordered = planned.map { items[it] }
                activePlayer.removeMediaItems(start, activePlayer.mediaItemCount)
                activePlayer.addMediaItems(reordered)
                syncQueueFromPlayer()
            }
        }
        settingsStore.saveShuffleMode(nextMode.name)
        settingsStore.saveShuffleEnabled(nextMode != FlowShuffleMode.Off)
    }

    fun toggleShuffle() = setShuffleMode(shuffleMode.next())

    fun toggleRepeat() {
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

    fun removeQueueItem(trackId: String) {
        val activePlayer = player
        if (activePlayer == null) {
            queue = queue.filter { it != trackId }
            return
        }
        val start = activePlayer.currentMediaItemIndex + 1
        val index = (start until activePlayer.mediaItemCount)
            .firstOrNull { activePlayer.getMediaItemAt(it).mediaId == trackId }
        if (index != null) activePlayer.removeMediaItem(index)
        syncQueueFromPlayer()
    }

    fun removeQueueItemAt(position: Int) {
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
        val activePlayer = player
        if (activePlayer == null) {
            queue = emptyList()
            return
        }
        val start = activePlayer.currentMediaItemIndex + 1
        val count = activePlayer.mediaItemCount
        if (start < count) activePlayer.removeMediaItems(start, count)
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
        insertIntoQueue(trackId, 0)
    }

    PixelodyBaseShell(
        onShuffleTracks = { ids ->
            val mode = shuffleMode.takeUnless { it == FlowShuffleMode.Off } ?: FlowShuffleMode.SmartFlow
            shuffleMode = mode
            settingsStore.saveShuffleMode(mode.name)
            settingsStore.saveShuffleEnabled(true)
            startPlayback(ids, 0L, mode)
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
        onClearQueue = ::clearQueue,
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
        modifier = modifier
    )
}
