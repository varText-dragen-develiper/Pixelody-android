package com.pixelody.app.feature.baselayer

import com.pixelody.app.feature.nowplaying.FlowListeningSheet
import com.pixelody.app.core.playback.FlowBrowseFilter
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.ui.components.HarmonicFilterMode
import com.pixelody.app.ui.components.getCompatibleCamelotKeyCodes
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.CompositionLocalProvider
import com.pixelody.app.feature.nowplaying.LocalPlayerMotion
import com.pixelody.app.feature.nowplaying.rememberPlayerMotion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextField
import com.pixelody.app.data.model.collectionCoverKey
import com.pixelody.app.data.model.ScreenBackground
import com.pixelody.app.ui.components.ScreenImageBackground
import androidx.compose.runtime.rememberUpdatedState
import com.pixelody.app.data.model.CoverBook
import com.pixelody.app.data.model.crateCoverKey
import com.pixelody.app.data.model.trackCoverKey
import com.pixelody.app.data.model.Crate
import com.pixelody.app.data.model.CrateBook
import com.pixelody.app.data.model.CrateSlot
import com.pixelody.app.data.model.nextCrateId
import com.pixelody.app.data.model.addingToFirstEmpty
import com.pixelody.app.data.model.rearranged
import com.pixelody.app.data.model.removingAt
import com.pixelody.app.data.storage.CoverStore
import com.pixelody.app.data.storage.PixelodyPersistenceRepository
import com.pixelody.app.ui.components.CrateSheet
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.components.performConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.pixelody.app.ui.components.ScanlineOverlay
import com.pixelody.app.ui.theme.ObsidianGlassPalette
import com.pixelody.app.ui.theme.LoFiCafePalette
import com.pixelody.app.ui.theme.BulkheadTerminalPalette
import com.pixelody.app.ui.theme.ObsessionPalette
import com.pixelody.app.ui.theme.pixelodyGround

import com.pixelody.app.data.storage.LoudnessNormalizationMode
import com.pixelody.app.core.lyrics.LyricsDocument
import com.pixelody.app.ui.components.LyricsView
import com.pixelody.app.ui.components.TagEditorSheet
import com.pixelody.app.ui.components.CabinetDocButton
import com.pixelody.app.ui.components.FeatureDocumentationSheet
import com.pixelody.app.core.playlist.M3uPlaylistManager
import com.pixelody.app.core.analytics.SessionSoundInsights
import com.pixelody.app.core.playback.AudioDeviceRoute
import com.pixelody.app.core.playback.AudioRouteState
import com.pixelody.app.core.playback.EqualizerRuntimeState
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.core.playback.QueueUndoManager
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.AcousticChamberPreset
import com.pixelody.app.data.model.AudioHapticSettings
import com.pixelody.app.data.model.AutoDjSettings
import com.pixelody.app.data.model.CassetteTapeSettings
import com.pixelody.app.data.model.CrtBeamPersistence
import com.pixelody.app.data.model.CrtPhosphorType
import com.pixelody.app.data.model.DacHardwareProfile
import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.HapticPrimitiveType
import com.pixelody.app.data.model.OscilloscopeDisplayMode
import com.pixelody.app.data.model.StemChannelState
import com.pixelody.app.data.model.StemPreset
import com.pixelody.app.data.model.StemType
import com.pixelody.app.data.model.TapeTransportState
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.HiResLosslessSettings
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.HostProfile
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.LiveState
import com.pixelody.app.data.model.MasteringProfile
import com.pixelody.app.data.model.OscilloscopeSettings
import com.pixelody.app.data.model.Playlist
import com.pixelody.app.data.model.QueueItem
import com.pixelody.app.data.model.QueueSnapshot
import com.pixelody.app.data.model.RepeatMode
import com.pixelody.app.data.model.SavedHostProfile
import com.pixelody.app.data.model.SonicCapsuleSettings
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.StemsIsolatorSettings
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.model.WeeklyListeningTrend
import com.pixelody.app.ui.components.SonicCapsuleTimelineView
import com.pixelody.app.feature.home.HomeScreen
import com.pixelody.app.feature.library.LibraryScreen
import com.pixelody.app.feature.nowplaying.MiniPlayerBar
import com.pixelody.app.feature.nowplaying.NowPlayingScreen
import com.pixelody.app.feature.nowplaying.QueueScreen
import com.pixelody.app.feature.search.SearchScreen
import com.pixelody.app.feature.create.CreateScreen
import com.pixelody.app.feature.profile.ProfileScreen
import com.pixelody.app.feature.device.DeviceLibraryScreen
import com.pixelody.app.core.genre.GenreTaxonomyEngine
import com.pixelody.app.core.hosting.AndroidHostStatus
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.data.storage.OfflineMediaStore
import com.pixelody.app.data.storage.TrackMetadataStore
import com.pixelody.app.data.storage.DownloadQueueManager
import com.pixelody.app.data.storage.DownloadQueueState
import com.pixelody.app.ui.navigation.PixelodyDetailKind
import com.pixelody.app.ui.navigation.PixelodyDetailRoute
import androidx.compose.runtime.collectAsState
import com.pixelody.app.core.playback.JamSessionCoordinator
import com.pixelody.app.data.model.JamSession
import com.pixelody.app.data.model.JamSyncStatus
import com.pixelody.app.ui.components.JamSessionHubSheet
import com.pixelody.app.ui.components.SmartCrateRuleBuilderDialog
import com.pixelody.app.ui.components.SourceScope
import com.pixelody.app.ui.navigation.PixelodyTab
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import com.pixelody.app.core.playback.SleepTimerMode
import com.pixelody.app.core.playback.SleepTimerState

/**
 * The base layer shell.
 *
 * Structure only, per H12 — it inherits whatever theme wraps it and adds no identity
 * of its own. What it is here to prove is the frame: five bands on every destination,
 * a listening slot that never moves, Back that removes exactly one layer, and tapping
 * a name arriving at the thing it named.
 *
 * Content arrives as a [BaseLayerData] snapshot rather than being fetched, so the
 * frame can be walked on a device before any feature is ported into it. Destinations
 * that have not been ported yet say so rather than pretending.
 */
@Composable
fun PixelodyBaseShell(
    data: BaseLayerData,
    localTracks: List<Track> = emptyList(),
    sessionInsights: SessionSoundInsights = SessionSoundInsights(),
    dailyCapsule: DailySonicCapsule = DailySonicCapsule(),
    weeklyTrend: WeeklyListeningTrend = WeeklyListeningTrend(),
    onPlayTracks: (List<String>) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit,
    onSeek: (Long) -> Unit = {},
    onAddToQueue: (String) -> Unit = {},
    onAddTracksToQueue: (List<String>) -> Unit = { ids -> ids.forEach(onAddToQueue) },
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onReorderQueue: (List<String>) -> Unit = {},
    onInsertIntoQueue: (String, Int) -> Unit = { _, _ -> },
    onRemoveQueueItem: (String) -> Unit = {},
    onRemoveQueueItemAt: (Int) -> Unit = { index -> data.queue.getOrNull(index)?.let(onRemoveQueueItem) },
    onClearQueue: () -> Unit = {},
    onResetDeveloperData: (suspend (Boolean) -> Unit)? = null,
    equalizerRuntimeState: EqualizerRuntimeState = EqualizerRuntimeState(),
    equalizerProfile: EqualizerProfile = EqualizerProfile(),
    onEqualizerChange: (EqualizerProfile) -> Unit = {},
    trackEqualizers: Map<String, EqualizerProfile> = emptyMap(),
    onTrackEqualizerChange: (String, EqualizerProfile?) -> Unit = { _, _ -> },
    useMasteringRack: Boolean = false,
    onUseMasteringRackChange: (Boolean) -> Unit = {},
    onCycleEqualizerPreset: () -> Unit = {},
    globalMastering: MasteringProfile = MasteringProfile(),
    onMasteringChange: (MasteringProfile) -> Unit = {},
    spatialSettings: SpatialChamberSettings = SpatialChamberSettings(),
    onSpatialSettingsChange: (SpatialChamberSettings) -> Unit = {},
    cassetteSettings: CassetteTapeSettings = CassetteTapeSettings(),
    onCassetteSettingsChange: (CassetteTapeSettings) -> Unit = {},
    stemsSettings: StemsIsolatorSettings = StemsIsolatorSettings(),
    onStemsSettingsChange: (StemsIsolatorSettings) -> Unit = {},
    oscilloscopeSettings: OscilloscopeSettings = OscilloscopeSettings(),
    onOscilloscopeSettingsChange: (OscilloscopeSettings) -> Unit = {},
    autoDjSettings: AutoDjSettings = AutoDjSettings(),
    onAutoDjSettingsChange: (AutoDjSettings) -> Unit = {},
    audioHapticSettings: AudioHapticSettings = AudioHapticSettings(),
    onAudioHapticSettingsChange: (AudioHapticSettings) -> Unit = {},
    hiResSettings: HiResLosslessSettings = HiResLosslessSettings(),
    onHiResSettingsChange: (HiResLosslessSettings) -> Unit = {},
    positionMs: Long = 0L,
    positionMsProvider: (() -> Long)? = null,
    durationMs: Long = 0L,
    shuffleEnabled: Boolean = false,
    shuffleMode: FlowShuffleMode = FlowShuffleMode.Off,
    onResume: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onShuffleModeChange: (FlowShuffleMode) -> Unit = {},
    onShuffleTracks: (List<String>) -> Unit = onPlayTracks,
    repeatMode: RepeatMode = RepeatMode.Off,
    onToggleRepeat: () -> Unit = {},
    onSourceChange: (BaseSource) -> Unit,
    onLensChange: (BaseLens) -> Unit,
    onRetryHost: () -> Unit,
    onCratesChange: (CrateBook) -> Unit,
    onCoversChange: (CoverBook) -> Unit = {},
    coverStore: CoverStore? = null,
    isScanningDevice: Boolean = false,
    deviceScanNotice: String = "",
    playbackError: String = "",
    onScanDevice: () -> Unit = {},
    onPickFolder: () -> Unit = {},
    onPickFiles: () -> Unit = {},
    hostBaseUrl: String? = null,
    hostConnectionState: HostConnectionState = HostConnectionState.Disconnected,
    hostConnectionError: String = "",
    jamCoordinator: JamSessionCoordinator = remember { JamSessionCoordinator() },
    onConnectHost: (String) -> Unit = {},
    isLoadingLocalLibrary: Boolean = false,
    offlineCacheSizeBytes: Long = 0L,
    onForgetHost: () -> Unit = {},
    onClearCache: () -> Unit = {},
    activeTheme: PixelodyMobileTheme = PixelodyMobileTheme.Studio,
    onThemeChange: (PixelodyMobileTheme) -> Unit = {},
    onResetTheme: () -> Unit = {},
    activePlayerViewMode: String = "Classic",
    onPlayerViewModeChange: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onAddToPlaylist: (String, String) -> Unit = { _, _ -> },
    onCreatePlaylist: (String, String) -> Unit = { _, _ -> },
    onPlayNext: (String) -> Unit = {},
    onRenamePlaylist: (String, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onRemoveTrackFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onCreatePlaylistWithTracks: (String, List<String>) -> Unit = { _, _ -> },
    sleepTimerState: SleepTimerState = SleepTimerState(),
    onSetSleepTimer: (SleepTimerMode, Long) -> Unit = { _, _ -> },
    playbackSpeed: Float = 1.0f,
    pitchLocked: Boolean = true,
    onSpeedPitchChange: (Float, Boolean) -> Unit = { _, _ -> },
    loudnessMode: LoudnessNormalizationMode = LoudnessNormalizationMode.StreamingStandard,
    onLoudnessModeChange: (LoudnessNormalizationMode) -> Unit = {},
    lyricsDocument: LyricsDocument = LyricsDocument.EMPTY,
    incomingDeepLink: PixelodyTab? = null,
    incomingSearchQuery: String = "",
    onDeepLinkConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val effectivePositionMsProvider: () -> Long = positionMsProvider ?: { positionMs }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsStore = remember { MobileSettingsStore(context.applicationContext) }
    val metadataStore = remember { TrackMetadataStore(context.applicationContext) }
    val offlineMediaStore = remember { OfflineMediaStore(context.applicationContext) }
    val downloadQueueManager = remember {
        DownloadQueueManager(
            offlineMediaStore = offlineMediaStore,
            settingsStore = settingsStore,
            scope = coroutineScope,
            hostBaseUrlProvider = { hostBaseUrl }
        )
    }
    val downloadQueueState by downloadQueueManager.queueState.collectAsState()
    val androidHostStatus = AndroidHostStatus(running = false)

    val queueUndoManager = remember { QueueUndoManager() }
    val queueUndoState by queueUndoManager.undoState.collectAsState()
    val jamSession by jamCoordinator.session.collectAsState()
    val jamSyncStatus by jamCoordinator.syncStatus.collectAsState()
    val connectionNotice = remember { androidx.compose.material3.SnackbarHostState() }
    var previousConnection by remember { mutableStateOf(hostConnectionState) }
    var previousJamActive by remember { mutableStateOf(jamSession.active) }

    var state by rememberSaveable(stateSaver = BaseLayerStateSaver) {
        mutableStateOf(BaseLayerState(destination = BaseDestination.Home))
    }
    val destinationPager = rememberPagerState(initialPage = state.destination.ordinal) { BaseDestination.values().size }
    LaunchedEffect(state.destination, state.pushed, state.sheet) {
        destinationPager.animateScrollToPage(state.destination.ordinal,
            animationSpec = spring(dampingRatio = 1f, stiffness = 380f))
    }
    val playerMotion = rememberPlayerMotion(state.sheet == BaseSheet.Player ||
        (state.sheet == BaseSheet.Queue && state.queueReturnsToPlayer),
        onOpen = { state = reduceBaseLayer(state, BaseIntent.OpenSheet(BaseSheet.Player)) },
        onClose = { if (state.sheet == BaseSheet.Player) state = reduceBaseLayer(state, BaseIntent.Back) })
    var showFlow by rememberSaveable { mutableStateOf(false) }
    var flowKeyCode by rememberSaveable { mutableStateOf<String?>(null) }
    var flowFilterName by rememberSaveable { mutableStateOf(HarmonicFilterMode.StrictAdjacent.name) }
    var flowBpm by rememberSaveable { mutableStateOf<Float?>(null) }
    var flowTolerance by rememberSaveable { mutableStateOf(10f) }
    val flowKey = flowKeyCode?.let(CamelotKey::fromCode)
    val flowFilterMode = HarmonicFilterMode.entries.firstOrNull { it.name == flowFilterName }
        ?: HarmonicFilterMode.StrictAdjacent
    var shape by rememberSaveable { mutableStateOf(BaseBrowseShape.Tracks) }
    var query by rememberSaveable { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    var isTapeSaturationEnabled by rememberSaveable { mutableStateOf(false) }
    var currentAudioRoute by rememberSaveable { mutableStateOf(AudioDeviceRoute.Speaker) }

    var listeningDetailsFromPlayer by rememberSaveable { mutableStateOf(false) }
    fun send(intent: BaseIntent) {
        val returnToPlayer = intent == BaseIntent.Back && listeningDetailsFromPlayer &&
            state.overlays.isEmpty() && state.sheet == null && state.pushed.lastOrNull() == BasePush.SonicTimeline
        state = reduceBaseLayer(state, intent)
        if (returnToPlayer) {
            state = reduceBaseLayer(state, BaseIntent.OpenSheet(BaseSheet.Player))
            listeningDetailsFromPlayer = false
        }
        if (intent is BaseIntent.SelectDestination) listeningDetailsFromPlayer = false
    }

    /**
     * Playing a track means playing the list it was sitting in, from there. Handing
     * the player a single id leaves the queue empty and the music stops at the end of
     * one song, which is the difference between a music player and a demonstration of
     * one. The pool is whatever that surface is actually showing.
     */
    fun resolveTrack(id: String?): Track? {
        if (id == null) return null
        return data.track(id) ?: localTracks.firstOrNull { it.id == id }
    }

    fun playFrom(trackId: String, pool: List<String>) {
        val effectivePool = if (pool.contains(trackId)) pool else (listOf(trackId) + pool)
        val index = effectivePool.indexOf(trackId)
        val ids = if (shuffleMode == FlowShuffleMode.Off) effectivePool.drop(index.coerceAtLeast(0))
            else listOf(trackId) + effectivePool.filterNot { it == trackId }
        onPlayTracks(ids)
    }

    var experienceMode by rememberSaveable { mutableStateOf(settingsStore.loadExperienceMode()) }

    val flowKeyCodes = flowKey?.let { getCompatibleCamelotKeyCodes(it, flowFilterMode) }
    fun matchesFlow(track: Track) = FlowBrowseFilter.matches(track, flowKeyCodes, flowBpm, flowTolerance)

    fun playLibraryTrack(trackId: String, pool: List<Track> = data.visibleTracks().filter(::matchesFlow)) {
        val effectivePool = pool
        val playablePool = effectivePool.filter { data.isPlayable(it.id) || localTracks.any { lt -> lt.id == it.id } }
        val current = playablePool.firstOrNull { it.id == trackId } ?: resolveTrack(trackId)
        if (current != null) playFrom(current.id, playablePool.map { it.id })
        else onPlayTracks(listOf(trackId))
    }

    fun shuffleAllFlow() {
        val playablePool = data.visibleTracks().filter { data.isPlayable(it.id) && matchesFlow(it) }
        if (playablePool.isNotEmpty()) {
            val seed = playablePool.random()
            onShuffleTracks(listOf(seed.id) + playablePool.map { it.id }.filterNot { it == seed.id })
        }
    }

    LaunchedEffect(incomingDeepLink, incomingSearchQuery) {
        if (incomingDeepLink != null) {
            when (incomingDeepLink) {
                PixelodyTab.Home -> send(BaseIntent.SelectDestination(BaseDestination.Home))
                PixelodyTab.Search -> send(BaseIntent.SelectDestination(BaseDestination.Search))
                PixelodyTab.Library -> send(BaseIntent.SelectDestination(BaseDestination.Library))
                // The manifest advertises nine hosts. Six of them used to fall through
                // this else and open Home in silence, which is a link that lies.
                PixelodyTab.Create, PixelodyTab.Device -> {
                    send(BaseIntent.SelectDestination(BaseDestination.Library))
                    send(BaseIntent.Push(BasePush.Acquire))
                }
                PixelodyTab.Profile -> {
                    send(BaseIntent.SelectDestination(BaseDestination.Home))
                    send(BaseIntent.Push(BasePush.Appearance))
                }
                PixelodyTab.Sharing -> {
                    send(BaseIntent.SelectDestination(BaseDestination.Home))
                    send(BaseIntent.Push(BasePush.Technical))
                }
                PixelodyTab.Player -> send(BaseIntent.OpenSheet(BaseSheet.Player))
                PixelodyTab.Queue -> send(BaseIntent.OpenSheet(BaseSheet.Queue))
            }
            onDeepLinkConsumed()
        }
        if (incomingSearchQuery.isNotBlank()) {
            query = incomingSearchQuery
            send(BaseIntent.SelectDestination(BaseDestination.Search))
            onDeepLinkConsumed()
        }
    }

    BackHandler(enabled = !state.backLeavesTheApp()) { send(BaseIntent.Back) }

    var connectionSetupRequested by remember { mutableStateOf(false) }
    fun openConnectionSetup() {
        connectionSetupRequested = true
        send(BaseIntent.SelectDestination(BaseDestination.Home))
    }

    val openCollection = (state.pushed.lastOrNull() as? BasePush.Collection)
        ?.let { data.collection(it.collectionId, it.kindKey) }
    val openTrack = (state.pushed.lastOrNull() as? BasePush.TrackDetail)
        ?.let { resolveTrack(it.trackId) }

    val currentTrack = (data.currentTrackId ?: data.lastTrackId)?.let { resolveTrack(it) }
    val nextTrack = data.queue.firstOrNull()?.let { resolveTrack(it) }
    val previousTrack: Track? = null
    val queueEntries = data.queue.mapIndexedNotNull { index, id -> resolveTrack(id)?.let { index to it } }
    val queueTracks = queueEntries.map { it.second }

    val currentHostProfile = remember(hostBaseUrl, hostConnectionState) {
        HostProfile(
            hostId = "pixelody-host-1",
            hostName = if (hostBaseUrl != null) "Studio Workstation" else "Local Phone / Standalone",
            baseUrl = hostBaseUrl ?: "http://127.0.0.1:8080",
            platform = "Android",
            roles = listOf("LibraryHost", "PlaybackDevice"),
            connectionState = hostConnectionState
        )
    }

    val currentQueueSnapshot = remember(data.currentTrackId, data.queue, data.tracks, localTracks, shuffleEnabled, repeatMode) {
        QueueSnapshot(
            currentTrackId = data.currentTrackId,
            trackIds = data.queue,
            shuffle = shuffleEnabled,
            repeatMode = repeatMode,
            items = (listOfNotNull(data.currentTrackId) + data.queue).mapNotNull { id ->
                resolveTrack(id)?.let { track ->
                    QueueItem(
                        queueItemId = "qi-${track.id}",
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        durationSeconds = track.durationSeconds,
                        artworkUrl = track.artworkUrl,
                        playbackStatus = if (track.id == data.currentTrackId) "playing" else "queued"
                    )
                }
            }
        )
    }

    val librarySnapshot = remember(currentHostProfile, data.tracks, localTracks, data.collections, currentQueueSnapshot) {
        val allTracks = (data.tracks + localTracks).distinctBy { it.id }
        LibrarySnapshot(
            host = currentHostProfile,
            tracks = allTracks,
            playlists = data.collections.filter { it.kindKey == BaseBrowseShape.Playlists.kindKey }.map { Playlist(it.id, it.name, it.trackIds, null) },
            favorites = allTracks.filter { it.favorite }.map { it.id },
            queue = currentQueueSnapshot,
            revision = 1L
        )
    }

    val liveState = remember(currentHostProfile, data.currentTrackId, data.isPlaying, data.queue, data.tracks) {
        LiveState(
            revision = 1L,
            hostId = currentHostProfile.hostId,
            hostName = currentHostProfile.hostName,
            visibility = "Local",
            checkedAt = System.currentTimeMillis().toString(),
            pollAfterMs = 5000L,
            permissions = listOf("Playback", "Queue", "Library"),
            playbackCurrentTrackId = data.currentTrackId,
            playing = data.isPlaying,
            queueTrackIds = data.queue,
            favoriteTrackIds = data.tracks.filter { it.favorite }.map { it.id },
            deviceRefresh = null
        )
    }

    val savedHostProfile = remember(hostBaseUrl) {
        hostBaseUrl?.let {
            SavedHostProfile(
                hostId = "host-01",
                hostName = "Studio Workstation",
                baseUrl = it,
                baseUrls = listOf(it),
                token = "session-token",
                platform = "Desktop",
                roles = listOf("LibraryHost"),
                savedAt = System.currentTimeMillis(),
                lastConnectedAt = System.currentTimeMillis()
            )
        }
    }

    // Home's capsule and insights now come from SonicCapsuleTimelineEngine and
    // SessionCapsuleEngine, which record real plays. They used to be built here
    // from data.tracks: the first eight tracks in the library became eight
    // listens, fifteen minutes apart ending now, each played to completion and
    // none skipped, with a mood assigned by whether the index was even - and
    // sessionMinutes was the summed runtime of everything the person owned.

    var copiedDetails by remember { mutableStateOf("") }
    var qrScannerVisible by remember { mutableStateOf(false) }
    var qrScannerError by remember { mutableStateOf("") }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        qrScannerVisible = granted
        qrScannerError = if (granted) "" else "Camera access was not granted. You can paste a desktop invite instead."
    }

    val collectionActions: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? =
        if (openCollection == null) {
            null
        } else {
            {
                val playable = data.playableTrackIds(
                    data.visibleTrackIds(openCollection.trackIds)
                )
                Button(
                    onClick = { onPlayTracks(playable) },
                    enabled = playable.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Play") }
                OutlinedButton(
                    onClick = { onPlayTracks(playable.shuffled()) },
                    enabled = playable.size > 1,
                    modifier = Modifier.weight(1f)
                ) { Text("Shuffle") }
            }
        }

    val backgroundScreen = when {
        state.pushed.lastOrNull() == BasePush.Appearance -> ScreenBackground.Settings
        state.pushed.lastOrNull() is BasePush.Collection -> ScreenBackground.Library
        state.pushed.isNotEmpty() -> null
        state.destination == BaseDestination.Home -> ScreenBackground.Home
        state.destination == BaseDestination.Library -> ScreenBackground.Library
        else -> ScreenBackground.Search
    }
    val backgroundOpacity = backgroundScreen?.let { data.covers.backgroundOpacityFor(it) } ?: 1f
    val backgroundImage = backgroundScreen?.let { data.covers.imageFor(it.key, null) }
    LaunchedEffect(jamSession.active) {
        val ended = previousJamActive && !jamSession.active
        previousJamActive = jamSession.active
        if (ended) {
            state = state.copy(overlays = state.overlays.filterNot { it is BaseOverlay.JamHub })
            if (data.source == BaseSource.Jam) onSourceChange(BaseSource.Phone)
            if (hostConnectionState == HostConnectionState.Connected) {
                connectionNotice.showSnackbar("J.A.M. ended. Back to your music.", duration = androidx.compose.material3.SnackbarDuration.Short)
            }
        }
    }
    LaunchedEffect(hostConnectionState) {
        val lostConnection = previousConnection == HostConnectionState.Connected &&
            hostConnectionState !in listOf(HostConnectionState.Connected, HostConnectionState.Connecting, HostConnectionState.Reconnecting)
        previousConnection = hostConnectionState
        if (lostConnection) {
            state = state.copy(overlays = state.overlays.filterNot { it is BaseOverlay.JamHub })
            val needsPairing = hostConnectionState in listOf(HostConnectionState.Revoked, HostConnectionState.AuthFailed, HostConnectionState.CredentialExpired)
            if (connectionNotice.showSnackbar("Desktop disconnected. Phone music is ready.", if (needsPairing) "Pair again" else "Reconnect",
                    duration = androidx.compose.material3.SnackbarDuration.Short) == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                if (needsPairing) openConnectionSetup() else onRetryHost()
            }
        }
    }
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = if (backgroundImage == null) backgroundOpacity else 1f }.pixelodyGround()) {
            when (activeTheme) {
                PixelodyMobileTheme.CartridgeQuest -> {
                    ScanlineOverlay(
                        modifier = Modifier.fillMaxSize(),
                        lineColor = Color(0x0C000000),
                        spacingDp = 4.dp
                    )
                }
                PixelodyMobileTheme.ObsidianGlass -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ObsidianGlassPalette.PrismCyan.copy(alpha = 0.08f),
                                    ObsidianGlassPalette.PrismViolet.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.85f, size.height * 0.12f),
                                radius = size.width * 0.95f
                            )
                        )
                    }
                }
                PixelodyMobileTheme.LoFiCafe -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    LoFiCafePalette.Amber.copy(alpha = 0.07f),
                                    LoFiCafePalette.Wood.copy(alpha = 0.03f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.15f, size.height * 0.08f),
                                radius = size.width * 0.85f
                            )
                        )
                    }
                }
                PixelodyMobileTheme.BulkheadTerminal -> {
                    ScanlineOverlay(
                        modifier = Modifier.fillMaxSize(),
                        lineColor = Color(0x0E78F09A),
                        spacingDp = 3.dp
                    )
                }
                PixelodyMobileTheme.Obsession -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ObsessionPalette.Signal.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.95f),
                                radius = size.width * 0.7f
                            )
                        )
                    }
                }
                PixelodyMobileTheme.Studio -> Unit // Keep the ground quiet behind music and controls.
            }
        }
        Crossfade(targetState = backgroundImage to backgroundOpacity,
            modifier = Modifier.fillMaxSize(), animationSpec = tween(220), label = "Page background") { (image, opacity) ->
            ScreenImageBackground(image, opacity)
        }
        BaseLayerScaffold(
            modifier = if (playerMotion.presented || state.sheet != null) Modifier.clearAndSetSemantics {} else Modifier,
            selectedDestination = state.destination,
            pagePositionProvider = { destinationPager.currentPage + destinationPager.currentPageOffsetFraction },
            backdropOpacity = backgroundOpacity,
            contentModifier = destinationSwipeModifier(
                enabled = state.pushed.isEmpty() && state.sheet == null && state.overlays.isEmpty() && state.rearrangingCrate == null,
                destination = state.destination,
                pager = destinationPager,
                onNavigate = { send(BaseIntent.SelectDestination(it)) }
            ),
            identity = if (state.pushed.isNotEmpty() || openCollection != null || openTrack != null) {
                {
                    TextButton(onClick = { send(BaseIntent.Back) }) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val lastPushedCollection = state.pushed.lastOrNull() as? BasePush.Collection
                    Text(
                        text = when {
                            openCollection != null -> openCollection.name
                            lastPushedCollection != null -> when (lastPushedCollection.kindKey.lowercase()) {
                                "favorites", "favorite" -> "Favorites"
                                "lossless", "hires" -> "Lossless"
                                "recent", "recents" -> "Recent Tracks"
                                "playlists", "playlist" -> "Playlists"
                                "albums", "album" -> "Albums"
                                "artists", "artist" -> "Artists"
                                "genres", "genre" -> "Genres"
                                else -> lastPushedCollection.collectionId.ifBlank { "Collection" }
                            }
                            openTrack != null -> openTrack.title
                            state.pushed.lastOrNull() == BasePush.Acquire -> "Add Music"
                            state.pushed.lastOrNull() == BasePush.Appearance -> "Settings & Themes"
                            state.pushed.lastOrNull() == BasePush.Technical -> "Add music"
                            state.pushed.lastOrNull() == BasePush.SonicTimeline -> "Listening details"
                            else -> state.destination.label
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (openCollection != null) {
                        TextButton(
                            onClick = {
                                haptic.performTick()
                                send(BaseIntent.ShowOverlay(BaseOverlay.CollectionActions(openCollection.kindKey, openCollection.id)))
                            }
                        ) {
                            Text("Manage ›", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else null,
            surfaceActions = null,
            listeningSlot = {
                if (currentTrack != null) {
                    CompositionLocalProvider(LocalPlayerMotion provides playerMotion) {
                    MiniPlayerBar(
                        embeddedInDock = true,
                        experienceMode = experienceMode,
                        track = currentTrack,
                        previousTrack = previousTrack,
                        nextTrack = nextTrack,
                        isPlaying = data.isPlaying,
                        positionMs = positionMs,
                        positionMsProvider = effectivePositionMsProvider,
                        durationMs = durationMs,
                        equalizerProfile = equalizerProfile,
                        trackHasEqualizerOverride = false,
                        shuffleEnabled = shuffleEnabled,
                        shuffleMode = shuffleMode,
                        repeatMode = repeatMode,
                        onOpenPlayer = { send(BaseIntent.OpenSheet(BaseSheet.Player)) },
                        onPlayPause = onTogglePlay,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onOpenQueue = { send(BaseIntent.OpenSheet(BaseSheet.Queue)) },
                        onCycleEqualizerPreset = onCycleEqualizerPreset,
                        onToggleShuffle = { showFlow = true },
                        onToggleRepeat = onToggleRepeat,
                        onSeek = onSeek,
                        onOpenPlayerToView = { viewMode ->
                            onPlayerViewModeChange(viewMode)
                            send(BaseIntent.OpenSheet(BaseSheet.Player))
                        },
                        playbackError = playbackError
                    )
                    }
                } else if (data.tracks.isNotEmpty() || data.lastTrackId != null) {
                    ListeningSlot(
                        embeddedInDock = true,
                        state = data.listeningSlotState(),
                        onOpenPlayer = { send(BaseIntent.OpenSheet(BaseSheet.Player)) },
                        onSessionTray = { send(BaseIntent.ShowOverlay(BaseOverlay.SessionTray)) },
                        transport = {
                            if (data.currentTrackId != null) {
                                BaseTransportButton(
                                    label = if (data.isPlaying) "Pause" else "Play",
                                    onClick = onTogglePlay
                                )
                                BaseTransportButton(label = "Next", onClick = onNext)
                            } else if (data.lastTrackId != null) {
                                BaseTransportButton(
                                    label = "Resume",
                                    onClick = onResume
                                )
                            }
                        }
                    )
                }
            },
            destinations = {
                BaseDestination.values().forEach { destination ->
                    BaseDestinationTab(
                        destination = destination,
                        selected = state.destination == destination && state.pushed.isEmpty(),
                        onSelect = { send(BaseIntent.SelectDestination(destination)) }
                    )
                }
            },
            railDestinations = {
                BaseDestination.values().forEach { destination ->
                    BaseDestinationRailItem(
                        destination = destination,
                        selected = state.destination == destination && state.pushed.isEmpty(),
                        onSelect = { send(BaseIntent.SelectDestination(destination)) }
                    )
                }
            }
        ) {
            HorizontalPager(state = destinationPager, userScrollEnabled = false,
                pageNestedScrollConnection = remember { object: androidx.compose.ui.input.nestedscroll.NestedScrollConnection {} },
                modifier = Modifier.fillMaxSize()) { page ->
            val pageDestination = BaseDestination.values()[page]
            Box(Modifier.fillMaxSize().then(routeArrival(state.pushed))) {
            when {
                openCollection != null -> {
                    val detailKind = when (openCollection.kindKey.lowercase()) {
                        "playlist", "playlists" -> PixelodyDetailKind.Playlist
                        "album", "albums" -> PixelodyDetailKind.Album
                        "artist", "artists" -> PixelodyDetailKind.Artist
                        "genre", "genres" -> PixelodyDetailKind.Genre
                        "favorites", "favorite" -> PixelodyDetailKind.Favorites
                        "lossless", "hires" -> PixelodyDetailKind.Lossless
                        "recent", "recents" -> PixelodyDetailKind.Recent
                        else -> PixelodyDetailKind.Playlist
                    }
                    LibraryScreen(
                        shuffleMode = shuffleMode,
                        harmonicBaseKey = flowKey,
                        harmonicFilterMode = flowFilterMode,
                        flowBpm = flowBpm,
                        flowTolerance = flowTolerance,
                        onOpenFlow = { showFlow = true },
                        recentTrackIds = dailyCapsule.memoryTimeline.sortedByDescending { it.timestampMs }.map { it.trackId },
                        covers = data.covers,
                        onCoverActions = { key, title -> send(BaseIntent.ShowOverlay(BaseOverlay.CoverActions(key, title))) },
                        snapshot = librarySnapshot,
                        localTracks = localTracks,
                        selectedTrack = currentTrack,
                        cachedTrackIds = data.downloadedTrackIds,
                        onPlayTrack = { track -> playLibraryTrack(track.id, openCollection.trackIds.mapNotNull(::resolveTrack).filter(::matchesFlow)) },
                        onOpenQueue = { send(BaseIntent.OpenSheet(BaseSheet.Queue)) },
                        onOpenSearch = { send(BaseIntent.SelectDestination(BaseDestination.Search)) },
                        equalizerProfile = equalizerProfile,
                        trackHasEqualizerOverride = false,
                        onOpenPlayer = { send(BaseIntent.OpenSheet(BaseSheet.Player)) },
                        onCycleEqualizerPreset = onCycleEqualizerPreset,
                        onLongClickTrack = { track -> send(BaseIntent.ShowOverlay(BaseOverlay.TrackActions(track.id))) },
                        onToggleFavorite = { track -> onToggleFavorite(track.id) },
                        onPlayBatchNext = { tracks -> onPlayTracks(tracks.map { it.id }) },
                        onAddBatchToQueue = { tracks -> tracks.forEach { onAddToQueue(it.id) } },
                        onPairFixture = ::openConnectionSetup,
                        activeDetail = PixelodyDetailRoute(detailKind, openCollection.id),
                        onOpenCollection = { route -> send(BaseIntent.Push(BasePush.Collection(route.kind.storageKey, route.id))) },
                        onCloseCollection = { send(BaseIntent.Back) },
                        connectionState = hostConnectionState,
                        connectionGuidance = if (data.hostReachable) "" else "Desktop host is not responding.",
                        hasSavedHost = hostBaseUrl != null,
                        onRetryHost = onRetryHost,
                        onOpenPhoneFiles = { send(BaseIntent.Push(BasePush.Technical)) },
                        onOpenTechnical = { send(BaseIntent.Push(BasePush.Technical)) },
                        sourceScope = when (data.source) {
                            BaseSource.Phone -> SourceScope.LocalPhone
                            BaseSource.Host -> SourceScope.DesktopHost
                            BaseSource.Jam -> SourceScope.JamMesh
                            BaseSource.All -> SourceScope.All
                        },
                        onSourceScopeChange = { scope ->
                            val newSource = when (scope) {
                                SourceScope.All -> BaseSource.All
                                SourceScope.LocalPhone -> BaseSource.Phone
                                SourceScope.DesktopHost -> BaseSource.Host
                                SourceScope.JamMesh -> BaseSource.Jam
                            }
                            onSourceChange(newSource)
                        },
                        onShowDoc = { topicId -> send(BaseIntent.ShowOverlay(BaseOverlay.Documentation(topicId))) }
                    )
                }

                state.pushed.isNotEmpty() -> when (val pushed = state.pushed.last()) {
                    is BasePush.TrackDetail -> BaseTrackDetailScreen(
                        data = data,
                        trackId = pushed.trackId,
                        onPlayTrack = onPlayTracks,
                        onAddToQueue = onAddToQueue,
                        onOpenAlbum = { albumName ->
                            send(BaseIntent.Push(BasePush.Collection(BaseBrowseShape.Albums.kindKey, "album:$albumName")))
                        },
                        onOpenArtist = { artistName ->
                            send(BaseIntent.Push(BasePush.Collection(BaseBrowseShape.Artists.kindKey, "artist:$artistName")))
                        },
                        onOpenGenre = { genreName ->
                            send(BaseIntent.Push(BasePush.Collection(BaseBrowseShape.Genres.kindKey, "genre:$genreName")))
                        },
                        onAddToCrate = { description, id ->
                            send(BaseIntent.ShowOverlay(BaseOverlay.AddToCrate(description, "track", id)))
                        },
                        onToggleFavorite = onToggleFavorite,
                        onAddToPlaylist = { trackId -> send(BaseIntent.ShowOverlay(BaseOverlay.AddToPlaylist(trackId))) }
                    )
                    is BasePush.Collection -> {
                        val detailKind = when (pushed.kindKey.lowercase()) {
                            "playlist", "playlists" -> PixelodyDetailKind.Playlist
                            "album", "albums" -> PixelodyDetailKind.Album
                            "artist", "artists" -> PixelodyDetailKind.Artist
                            "genre", "genres" -> PixelodyDetailKind.Genre
                            "favorites", "favorite" -> PixelodyDetailKind.Favorites
                            "lossless", "hires" -> PixelodyDetailKind.Lossless
                            "recent", "recents" -> PixelodyDetailKind.Recent
                            else -> PixelodyDetailKind.Playlist
                        }
                        LibraryScreen(
                            shuffleMode = shuffleMode,
                            harmonicBaseKey = flowKey,
                            harmonicFilterMode = flowFilterMode,
                            flowBpm = flowBpm,
                            flowTolerance = flowTolerance,
                            onOpenFlow = { showFlow = true },
                            recentTrackIds = dailyCapsule.memoryTimeline.sortedByDescending { it.timestampMs }.map { it.trackId },
                            covers = data.covers,
                            onCoverActions = { key, title -> send(BaseIntent.ShowOverlay(BaseOverlay.CoverActions(key, title))) },
                            snapshot = librarySnapshot,
                            localTracks = localTracks,
                            selectedTrack = currentTrack,
                            cachedTrackIds = data.downloadedTrackIds,
                            onPlayTrack = { track -> playLibraryTrack(track.id) },
                            onOpenQueue = { send(BaseIntent.OpenSheet(BaseSheet.Queue)) },
                            onOpenSearch = { send(BaseIntent.SelectDestination(BaseDestination.Search)) },
                            equalizerProfile = equalizerProfile,
                            trackHasEqualizerOverride = false,
                            onOpenPlayer = { send(BaseIntent.OpenSheet(BaseSheet.Player)) },
                            onCycleEqualizerPreset = onCycleEqualizerPreset,
                            onLongClickTrack = { track ->
                                val currentPlaylistId = if (detailKind == PixelodyDetailKind.Playlist) pushed.collectionId else null
                                send(BaseIntent.ShowOverlay(BaseOverlay.TrackActions(track.id, playlistId = currentPlaylistId)))
                            },
                            onToggleFavorite = { track -> onToggleFavorite(track.id) },
                            onPlayBatchNext = { tracks -> onPlayTracks(tracks.map { it.id }) },
                            onAddBatchToQueue = { tracks -> tracks.forEach { onAddToQueue(it.id) } },
                            onPairFixture = ::openConnectionSetup,
                            activeDetail = PixelodyDetailRoute(detailKind, pushed.collectionId),
                            onOpenCollection = { route -> send(BaseIntent.Push(BasePush.Collection(route.kind.storageKey, route.id))) },
                            onCloseCollection = { send(BaseIntent.Back) },
                            onCollectionActions = { kindKey, id -> send(BaseIntent.ShowOverlay(BaseOverlay.CollectionActions(kindKey, id))) },
                            connectionState = hostConnectionState,
                            connectionGuidance = if (data.hostReachable) "" else "Desktop host is not responding.",
                            hasSavedHost = hostBaseUrl != null,
                            onRetryHost = onRetryHost,
                            onOpenPhoneFiles = { send(BaseIntent.Push(BasePush.Technical)) },
                            onOpenTechnical = { send(BaseIntent.Push(BasePush.Technical)) },
                            sourceScope = when (data.source) {
                                BaseSource.Phone -> SourceScope.LocalPhone
                                BaseSource.Host -> SourceScope.DesktopHost
                                BaseSource.Jam -> SourceScope.JamMesh
                                BaseSource.All -> SourceScope.All
                            },
                            onSourceScopeChange = { scope ->
                                val newSource = when (scope) {
                                    SourceScope.All -> BaseSource.All
                                    SourceScope.LocalPhone -> BaseSource.Phone
                                    SourceScope.DesktopHost -> BaseSource.Host
                                    SourceScope.JamMesh -> BaseSource.Jam
                                }
                                onSourceChange(newSource)
                            }
                        )
                    }
                    BasePush.Acquire -> CreateScreen(
                        localTrackCount = localTracks.size,
                        androidHostStatus = androidHostStatus,
                        savedHost = savedHostProfile,
                        isScanningDevice = isScanningDevice,
                        isScanningFolder = false,
                        scanNotice = deviceScanNotice,
                        onScanDevice = onScanDevice,
                        onScanFolder = onPickFolder,
                        onPickAudio = onPickFiles,
                        onOpenPair = ::openConnectionSetup,
                        onOpenProfile = { send(BaseIntent.Push(BasePush.Appearance)) },
                        onOpenTechnical = { send(BaseIntent.Push(BasePush.Technical)) },
                        onStartHost = {},
                        onStopHost = {}
                    )
                    BasePush.Appearance -> ProfileScreen(
                        onResetDeveloperData = if (onResetDeveloperData != null) { { includeEdits ->
                            queueUndoManager.clear()
                            onResetDeveloperData(includeEdits)
                        } } else null,
                        covers = data.covers, coverStore = coverStore, onCoversChange = onCoversChange,
                        activeTheme = activeTheme,
                        onThemeChange = onThemeChange,
                        onThemeReset = onResetTheme,
                        snapshot = librarySnapshot,
                        savedHost = savedHostProfile,
                        connectionState = hostConnectionState,
                        localTrackCount = localTracks.size,
                        cachedTrackCount = data.downloadedTrackIds.size,
                        offlineMediaStore = offlineMediaStore,
                        settingsStore = settingsStore,
                        downloadQueueState = downloadQueueState,
                        downloadQueueManager = downloadQueueManager,
                        onClearOfflineCache = onClearCache,
                        onPruneUnpinned = { offlineMediaStore.pruneUnpinnedCache() },
                        onClearArtwork = { offlineMediaStore.clearArtCache() },
                        onOpenDevice = { send(BaseIntent.Push(BasePush.Technical)) },
                        onOpenQueue = { send(BaseIntent.OpenSheet(BaseSheet.Queue)) },
                        onOpenTechnical = { send(BaseIntent.Push(BasePush.Technical)) },
                        onForgetSavedHost = onForgetHost
                    )
                    BasePush.Technical -> DeviceLibraryScreen(
                        localTracks = localTracks,
                        selectedTrack = currentTrack,
                        hostStatus = androidHostStatus,
                        isScanningDevice = isScanningDevice,
                        isScanningFolder = false,
                        scanNotice = deviceScanNotice,
                        onScanDevice = onScanDevice,
                        onScanFolder = onPickFolder,
                        onPickAudio = onPickFiles,
                        onClearLocalTracks = {},
                        onPlayTrack = { track -> playFrom(track.id, localTracks.map { it.id }) },
                        onOpenPlayer = { send(BaseIntent.OpenSheet(BaseSheet.Player)) },
                        onOpenQueue = { send(BaseIntent.OpenSheet(BaseSheet.Queue)) },
                        onOpenLibrary = { onSourceChange(BaseSource.Phone); send(BaseIntent.SelectDestination(BaseDestination.Library)) },
                        equalizerProfile = equalizerProfile,
                        trackHasEqualizerOverride = false,
                        onCycleEqualizerPreset = onCycleEqualizerPreset,
                        onStartHost = {},
                        onStopHost = {}
                    )
                    BasePush.SonicTimeline -> {
                        val activeCapsule = dailyCapsule
                        var capsuleSettings by remember { mutableStateOf(SonicCapsuleSettings()) }
                        SonicCapsuleTimelineView(
                            capsule = activeCapsule,
                            weeklyTrend = weeklyTrend,
                            settings = capsuleSettings,
                            onUpdateSettings = { capsuleSettings = it },
                            onPlayCapsuleFlow = { tracks ->
                                val ids = tracks.map { it.id }.ifEmpty { activeCapsule.highlightTrackIds }
                                if (ids.isNotEmpty()) onPlayTracks(ids)
                            },
                            onPlayTrackFromTimeline = { trackId ->
                                onPlayTracks(listOf(trackId))
                            },
                            onClose = { send(BaseIntent.Back) }
                        )
                    }
                }

                pageDestination == BaseDestination.Library -> LibraryScreen(
                    shuffleMode = shuffleMode,
                    harmonicBaseKey = flowKey,
                    harmonicFilterMode = flowFilterMode,
                    flowBpm = flowBpm,
                    flowTolerance = flowTolerance,
                    onOpenFlow = { showFlow = true },
                    recentTrackIds = dailyCapsule.memoryTimeline.sortedByDescending { it.timestampMs }.map { it.trackId },
                    covers = data.covers,
                    onCoverActions = { key, title -> send(BaseIntent.ShowOverlay(BaseOverlay.CoverActions(key, title))) },
                    snapshot = librarySnapshot,
                    localTracks = localTracks,
                    selectedTrack = currentTrack,
                    cachedTrackIds = data.downloadedTrackIds,
                    onPlayTrack = { track -> playLibraryTrack(track.id) },
                    experienceMode = experienceMode,
                    onExperienceModeChange = { newMode ->
                        experienceMode = newMode
                        settingsStore.saveExperienceMode(newMode)
                    },
                    onShuffleAllFlow = { shuffleAllFlow() },
                    onOpenQueue = { send(BaseIntent.OpenSheet(BaseSheet.Queue)) },
                    onOpenSearch = { send(BaseIntent.SelectDestination(BaseDestination.Search)) },
                    equalizerProfile = equalizerProfile,
                    trackHasEqualizerOverride = false,
                    onOpenPlayer = { send(BaseIntent.OpenSheet(BaseSheet.Player)) },
                    onCycleEqualizerPreset = onCycleEqualizerPreset,
                    onLongClickTrack = { track -> send(BaseIntent.ShowOverlay(BaseOverlay.TrackActions(track.id))) },
                    onToggleFavorite = { track -> onToggleFavorite(track.id) },
                    onPlayBatchNext = { tracks -> onPlayTracks(tracks.map { it.id }) },
                    onAddBatchToQueue = { tracks -> tracks.forEach { onAddToQueue(it.id) } },
                    onPairFixture = ::openConnectionSetup,
                    activeDetail = null,
                    onOpenCollection = { detailRoute ->
                        send(BaseIntent.Push(BasePush.Collection(detailRoute.kind.storageKey, detailRoute.id)))
                    },
                    onCollectionActions = { kindKey, id -> send(BaseIntent.ShowOverlay(BaseOverlay.CollectionActions(kindKey, id))) },
                    connectionState = hostConnectionState,
                    connectionGuidance = if (data.hostReachable) "" else "Desktop host is not responding. Check your Wi-Fi or select Phone source.",
                    hasSavedHost = hostBaseUrl != null,
                    onRetryHost = onRetryHost,
                    onOpenPhoneFiles = { send(BaseIntent.Push(BasePush.Technical)) },
                    onOpenTechnical = { send(BaseIntent.Push(BasePush.Technical)) },
                    sourceScope = when (data.source) {
                        BaseSource.Phone -> SourceScope.LocalPhone
                        BaseSource.Host -> SourceScope.DesktopHost
                        BaseSource.Jam -> SourceScope.JamMesh
                        BaseSource.All -> SourceScope.All
                    },
                    onSourceScopeChange = { scope ->
                        val newSource = when (scope) {
                            SourceScope.All -> BaseSource.All
                            SourceScope.LocalPhone -> BaseSource.Phone
                            SourceScope.DesktopHost -> BaseSource.Host
                            SourceScope.JamMesh -> BaseSource.Jam
                        }
                        onSourceChange(newSource)
                    },
                    onShowDoc = { topicId -> send(BaseIntent.ShowOverlay(BaseOverlay.Documentation(topicId))) }
                )

                pageDestination == BaseDestination.Search -> SearchScreen(
                    shuffleMode = shuffleMode,
                    harmonicBaseKey = flowKey,
                    harmonicFilterMode = flowFilterMode,
                    flowBpm = flowBpm,
                    flowTolerance = flowTolerance,
                    onOpenFlow = { showFlow = true },
                    experienceMode = experienceMode,
                    covers = data.covers,
                    onCoverActions = { key, title -> send(BaseIntent.ShowOverlay(BaseOverlay.CoverActions(key, title))) },
                    snapshot = librarySnapshot,
                    localTracks = localTracks,
                    selectedTrack = currentTrack,
                    onPlayTrack = { track -> playLibraryTrack(track.id) },
                    onOpenHome = { send(BaseIntent.SelectDestination(BaseDestination.Home)) },
                    onOpenLibrary = { send(BaseIntent.SelectDestination(BaseDestination.Library)) },
                    onOpenCollection = { kindKey, id -> send(BaseIntent.Push(BasePush.Collection(kindKey, id))) },
                    onOpenQueue = { send(BaseIntent.OpenSheet(BaseSheet.Queue)) },
                    onOpenDevice = { send(BaseIntent.Push(BasePush.Technical)) },
                    equalizerProfile = equalizerProfile,
                    trackHasEqualizerOverride = false,
                    onOpenPlayer = { send(BaseIntent.OpenSheet(BaseSheet.Player)) },
                    onCycleEqualizerPreset = onCycleEqualizerPreset,
                    onLongClickTrack = { track -> send(BaseIntent.ShowOverlay(BaseOverlay.TrackActions(track.id))) },
                    onToggleFavorite = { track -> onToggleFavorite(track.id) },
                    onPlayBatchNext = { tracks -> onPlayTracks(tracks.map { it.id }) },
                    onAddBatchToQueue = { tracks -> tracks.forEach { onAddToQueue(it.id) } },
                    onConnectDesktop = onRetryHost,
                    onStartJam = { send(BaseIntent.ShowOverlay(BaseOverlay.JamHub)) },
                    initialQuery = query,
                    onInitialQueryConsumed = { query = "" },
                    sourceScope = when (data.source) {
                        BaseSource.Phone -> SourceScope.LocalPhone
                        BaseSource.Host -> SourceScope.DesktopHost
                        BaseSource.Jam -> SourceScope.JamMesh
                        BaseSource.All -> SourceScope.All
                    },
                    onSourceScopeChange = { scope ->
                        val newSource = when (scope) {
                            SourceScope.All -> BaseSource.All
                            SourceScope.LocalPhone -> BaseSource.Phone
                            SourceScope.DesktopHost -> BaseSource.Host
                            SourceScope.JamMesh -> BaseSource.Jam
                        }
                        onSourceChange(newSource)
                    },
                    onShowDoc = { topicId -> send(BaseIntent.ShowOverlay(BaseOverlay.Documentation(topicId))) }
                )

                pageDestination == BaseDestination.Home -> HomeScreen(
                    onOpenFlowCabinet = { showFlow = true },
                    snapshot = librarySnapshot,
                    liveState = liveState,
                    selectedTrack = currentTrack,
                    localTracks = localTracks,
                    localTrackCount = localTracks.size,
                    isPlaying = data.isPlaying,
                    connectionState = hostConnectionState,
                    savedHost = savedHostProfile,
                    copiedDetails = copiedDetails,
                    qrScannerVisible = qrScannerVisible,
                    qrScannerError = qrScannerError.ifBlank { hostConnectionError },
                    isConnecting = hostConnectionState in listOf(HostConnectionState.Connecting, HostConnectionState.Reconnecting),
                    isLoadingMusic = isScanningDevice || isLoadingLocalLibrary,
                    connectionSetupRequested = connectionSetupRequested,
                    onConnectionSetupShown = { connectionSetupRequested = false },
                    error = hostConnectionError,
                    onCopiedDetailsChange = { copiedDetails = it },
                    onStartQrScanner = {
                        qrScannerError = ""
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            qrScannerVisible = true
                        } else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onStopQrScanner = { qrScannerVisible = false },
                    onQrPayloadScanned = { payload -> qrScannerVisible = false; onConnectHost(payload) },
                    onQrScannerError = { qrScannerError = it },
                    onForgetSavedHost = onForgetHost,
                    onUseCopiedDetails = { onConnectHost(copiedDetails) },
                    onPlayTrack = { track -> playLibraryTrack(track.id) },
                    experienceMode = experienceMode,
                    onExperienceModeChange = { newMode ->
                        experienceMode = newMode
                        settingsStore.saveExperienceMode(newMode)
                    },
                    onPlayTrackWithSmartFlow = { trackId -> playLibraryTrack(trackId) },
                    onShuffleAllFlow = { shuffleAllFlow() },
                    onTogglePlayback = onTogglePlay,
                    onOpenLibrary = { send(BaseIntent.SelectDestination(BaseDestination.Library)) },
                    onOpenSearch = { send(BaseIntent.SelectDestination(BaseDestination.Search)) },
                    onOpenQueue = { send(BaseIntent.OpenSheet(BaseSheet.Queue)) },
                    onOpenPlayer = { send(BaseIntent.OpenSheet(BaseSheet.Player)) },
                    onOpenCreate = { send(BaseIntent.Push(BasePush.Acquire)) },
                    onOpenProfile = { send(BaseIntent.Push(BasePush.Appearance)) },
                    onOpenDevice = { send(BaseIntent.Push(BasePush.Technical)) },
                    onOpenSharing = { send(BaseIntent.Push(BasePush.Technical)) },
                    equalizerProfile = equalizerProfile,
                    trackHasEqualizerOverride = false,
                    onCycleEqualizerPreset = onCycleEqualizerPreset,
                    onPairFixture = onRetryHost,
                    sessionInsights = sessionInsights,
                    dailyCapsule = dailyCapsule,
                    crates = data.crates.crates,
                    covers = data.covers,
                    onCoverActions = { key, title -> send(BaseIntent.ShowOverlay(BaseOverlay.CoverActions(key, title))) },
                    onOpenCrate = { crateId -> send(BaseIntent.ShowOverlay(BaseOverlay.OpenCrate(crateId))) },
                    onOpenCollection = { kindKey, id -> send(BaseIntent.Push(BasePush.Collection(kindKey, id))) },
                    onCrateActions = { crateId -> send(BaseIntent.ShowOverlay(BaseOverlay.CrateActions(crateId))) },
                    onDailySoundCheck = {
                        val trackIds = dailyCapsule.highlightTrackIds.ifEmpty { null }
                            ?: data.visibleTracks().take(10).map { it.id }
                        if (trackIds.isNotEmpty()) onPlayTracks(trackIds)
                    },
                    onOpenTimeline = { send(BaseIntent.Push(BasePush.SonicTimeline)) },
                    onPlayBatchTracks = { tracks -> onPlayTracks(tracks.map { it.id }) },
                    onLongClickTrack = { track -> send(BaseIntent.ShowOverlay(BaseOverlay.TrackActions(track.id))) },
                    onLongClickScope = { send(BaseIntent.ShowOverlay(BaseOverlay.SessionTray)) },
                    onConnectDesktop = onRetryHost,
                    onStartJam = { send(BaseIntent.ShowOverlay(BaseOverlay.JamHub)) },
                    onOpenSmartCrateBuilder = { send(BaseIntent.ShowOverlay(BaseOverlay.SmartCrateBuilder)) },
                    sourceScope = when (data.source) {
                        BaseSource.Phone -> SourceScope.LocalPhone
                        BaseSource.Host -> SourceScope.DesktopHost
                        BaseSource.Jam -> SourceScope.JamMesh
                        BaseSource.All -> SourceScope.All
                    },
                    onSourceScopeChange = { scope ->
                        val newSource = when (scope) {
                            SourceScope.All -> BaseSource.All
                            SourceScope.LocalPhone -> BaseSource.Phone
                            SourceScope.DesktopHost -> BaseSource.Host
                            SourceScope.JamMesh -> BaseSource.Jam
                        }
                        onSourceChange(newSource)
                    },
                    onLongClickCapsule = { send(BaseIntent.ShowOverlay(BaseOverlay.Documentation("daily_capsule"))) },
                    onLongClickQuickStart = { title ->
                        if (title.contains("Flow", ignoreCase = true) || title.contains("Harmonic", ignoreCase = true)) {
                            send(BaseIntent.ShowOverlay(BaseOverlay.Documentation("smart_flow")))
                        } else if (title.contains("Hi-Res", ignoreCase = true) || title.contains("Lossless", ignoreCase = true)) {
                            send(BaseIntent.ShowOverlay(BaseOverlay.Documentation("hires_lossless")))
                        } else {
                            send(BaseIntent.ShowOverlay(BaseOverlay.Documentation("smart_flow")))
                        }
                    },
                    onShowDoc = { topicId -> send(BaseIntent.ShowOverlay(BaseOverlay.Documentation(topicId))) }
                )

                else -> BaseNotPortedYet(what = pageDestination.label)
            }
            }
            }
        }

        if (playerMotion.presented || state.sheet == BaseSheet.Queue) {
            when {
                playerMotion.presented && state.sheet != BaseSheet.Queue -> {
                    CompositionLocalProvider(LocalPlayerMotion provides playerMotion) {
                    Box(Modifier.fillMaxSize().pointerInput(playerMotion) {
                        detectTapGestures { playerMotion.close() }
                    })
                    val playerSemantics = if (state.sheet == BaseSheet.Player) Modifier.semantics { paneTitle = "Now playing" }
                        else Modifier.clearAndSetSemantics {}
                    Box(Modifier.fillMaxSize().then(playerMotion.layer()).then(playerSemantics)) {
                    NowPlayingScreen(
                        dailyCapsule = dailyCapsule,
                        onPlayHistoryTrack = { id -> playFrom(id, dailyCapsule.memoryTimeline.sortedByDescending { it.timestampMs }.map { it.trackId }.distinct().filter { data.isPlayable(it) }) },
                        onListeningDetails = { listeningDetailsFromPlayer = true; send(BaseIntent.Back); send(BaseIntent.Push(BasePush.SonicTimeline)) },
                        snapshot = librarySnapshot,
                        selectedTrack = currentTrack,
                        isLocalTrack = currentTrack != null && data.phoneTrackIds.contains(currentTrack.id),
                        isPlaying = data.isPlaying,
                        connectionState = hostConnectionState,
                        playbackError = playbackError,
                        positionMs = positionMs,
                        positionMsProvider = effectivePositionMsProvider,
                        durationMs = durationMs,
                        expandedLayout = false,
                        globalEqualizer = equalizerProfile,
                        trackEqualizer = currentTrack?.id?.let { trackEqualizers[it] },
                        globalMastering = globalMastering,
                        trackMastering = null,
                        useMasteringRack = useMasteringRack,
                        equalizerRuntimeState = equalizerRuntimeState,
                        shuffleEnabled = shuffleEnabled,
                        shuffleMode = shuffleMode,
                        repeatMode = repeatMode,
                        onToggleMasteringRack = onUseMasteringRackChange,
                        onGlobalEqualizerChange = onEqualizerChange,
                        onTrackEqualizerChange = { profile -> currentTrack?.id?.let { onTrackEqualizerChange(it, profile) } },
                        onGlobalMasteringChange = onMasteringChange,
                        onTrackMasteringChange = {},
                        onPlayPause = onTogglePlay,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onToggleShuffle = { showFlow = true },
                        onOpenQueue = { send(BaseIntent.OpenSheet(BaseSheet.Queue)) },
                        onCollapse = { if (state.sheet == BaseSheet.Player) send(BaseIntent.Back) },
                        onToggleRepeat = onToggleRepeat,
                        onToggleFavorite = onToggleFavorite,
                        onAddToPlaylist = { trackId -> send(BaseIntent.ShowOverlay(BaseOverlay.AddToPlaylist(trackId))) },
                        onTrackActions = { trackId -> send(BaseIntent.ShowOverlay(BaseOverlay.TrackActions(trackId))) },
                        sleepTimerActive = sleepTimerState.active,
                        sleepTimerRemaining = sleepTimerState.formattedRemaining,
                        onOpenSleepTimer = { send(BaseIntent.ShowOverlay(BaseOverlay.SleepTimer)) },
                        audioRouteState = AudioRouteState(currentRoute = currentAudioRoute, deviceName = currentAudioRoute.label),
                        onCycleAudioRoute = {
                            haptic.performTick()
                            currentAudioRoute = when (currentAudioRoute) {
                                AudioDeviceRoute.Speaker -> AudioDeviceRoute.Bluetooth
                                AudioDeviceRoute.Bluetooth -> AudioDeviceRoute.Wired
                                AudioDeviceRoute.Wired -> AudioDeviceRoute.Speaker
                                else -> AudioDeviceRoute.Speaker
                            }
                        },
                        playbackSpeed = playbackSpeed,
                        pitchLocked = pitchLocked,
                        onOpenPitchAndSpeed = { send(BaseIntent.ShowOverlay(BaseOverlay.PitchAndSpeed)) },
                        loudnessModeLabel = loudnessMode.label,
                        lyricsDocument = lyricsDocument,
                        onOpenLyrics = {
                            val nextMode = if (activePlayerViewMode == "Lyrics") "Classic" else "Lyrics"
                            onPlayerViewModeChange(nextMode)
                        },
                        onSpeedChange = { onSpeedPitchChange(it, pitchLocked) },
                        isTapeSaturationEnabled = isTapeSaturationEnabled,
                        onToggleTapeSaturation = { isTapeSaturationEnabled = !isTapeSaturationEnabled },
                        onNeedleDropHaptic = { haptic.performTick() },
                        nextTrack = nextTrack,
                        cassetteSettings = cassetteSettings,
                        onCassetteSettingsChange = onCassetteSettingsChange,
                        onSetCassetteTransportState = { trState ->
                            when (trState) {
                                TapeTransportState.Playing -> if (!data.isPlaying) onTogglePlay()
                                TapeTransportState.Paused, TapeTransportState.Stopped -> if (data.isPlaying) onTogglePlay()
                                else -> {}
                            }
                        },
                        stemsSettings = stemsSettings,
                        onStemsSettingsChange = onStemsSettingsChange,
                        onSetStemGain = { type, gain ->
                            val ch = stemsSettings.channels[type] ?: StemChannelState(stemType = type)
                            onStemsSettingsChange(stemsSettings.copy(channels = stemsSettings.channels + (type to ch.copy(gainDb = gain))))
                        },
                        onToggleStemMute = { type ->
                            val ch = stemsSettings.channels[type] ?: StemChannelState(stemType = type)
                            onStemsSettingsChange(stemsSettings.copy(channels = stemsSettings.channels + (type to ch.copy(isMuted = !ch.isMuted))))
                        },
                        onToggleStemSolo = { type ->
                            val ch = stemsSettings.channels[type] ?: StemChannelState(stemType = type)
                            onStemsSettingsChange(stemsSettings.copy(channels = stemsSettings.channels + (type to ch.copy(isSoloed = !ch.isSoloed))))
                        },
                        onSetStemPan = { type, pan ->
                            val ch = stemsSettings.channels[type] ?: StemChannelState(stemType = type)
                            onStemsSettingsChange(stemsSettings.copy(channels = stemsSettings.channels + (type to ch.copy(pan = pan))))
                        },
                        onSetStemEq = { type, high, mid, low ->
                            val ch = stemsSettings.channels[type] ?: StemChannelState(stemType = type)
                            onStemsSettingsChange(stemsSettings.copy(channels = stemsSettings.channels + (type to ch.copy(eqHighDb = high, eqMidDb = mid, eqLowDb = low))))
                        },
                        onSetStemFilter = { type, filter ->
                            val ch = stemsSettings.channels[type] ?: StemChannelState(stemType = type)
                            onStemsSettingsChange(stemsSettings.copy(channels = stemsSettings.channels + (type to ch.copy(filterCutoffNormalized = filter))))
                        },
                        onSetStemsCrossfaderPosition = { pos ->
                            onStemsSettingsChange(stemsSettings.copy(crossfaderPosition = pos))
                        },
                        onApplyStemPreset = { preset ->
                            val updatedChannels = stemsSettings.channels.toMutableMap()
                            updatedChannels[StemType.Vocals] = (updatedChannels[StemType.Vocals] ?: StemChannelState(StemType.Vocals)).copy(gainDb = preset.vocalGainDb, isMuted = preset.vocalMute, isSoloed = false)
                            updatedChannels[StemType.Drums] = (updatedChannels[StemType.Drums] ?: StemChannelState(StemType.Drums)).copy(gainDb = preset.drumsGainDb, isMuted = preset.drumsMute, isSoloed = false)
                            updatedChannels[StemType.Bass] = (updatedChannels[StemType.Bass] ?: StemChannelState(StemType.Bass)).copy(gainDb = preset.bassGainDb, isMuted = preset.bassMute, isSoloed = false)
                            updatedChannels[StemType.Instruments] = (updatedChannels[StemType.Instruments] ?: StemChannelState(StemType.Instruments)).copy(gainDb = preset.instGainDb, isMuted = preset.instMute, isSoloed = false)
                            onStemsSettingsChange(stemsSettings.copy(activePreset = preset, channels = updatedChannels))
                        },
                        oscilloscopeSettings = oscilloscopeSettings,
                        onOscilloscopeSettingsChange = onOscilloscopeSettingsChange,
                        onSetOscilloscopeDisplayMode = { mode -> onOscilloscopeSettingsChange(oscilloscopeSettings.copy(displayMode = mode)) },
                        onSetPhosphorType = { pType -> onOscilloscopeSettingsChange(oscilloscopeSettings.copy(phosphorType = pType)) },
                        onSetOscilloscopePersistence = { p -> onOscilloscopeSettingsChange(oscilloscopeSettings.copy(persistence = p)) },
                        onSetOscilloscopeSensitivity = { s -> onOscilloscopeSettingsChange(oscilloscopeSettings.copy(sensitivityGain = s)) },
                        onUpdateOscilloscope3DRotation = { pitch, yaw ->
                            onOscilloscopeSettingsChange(oscilloscopeSettings.copy(beamGeometry = oscilloscopeSettings.beamGeometry.copy(eulerPitchDeg = pitch, eulerYawDeg = yaw)))
                        },
                        onSetOscilloscopePhaseRotation = { phase ->
                            onOscilloscopeSettingsChange(oscilloscopeSettings.copy(beamGeometry = oscilloscopeSettings.beamGeometry.copy(audioPhaseRotationDeg = phase)))
                        },
                        autoDjSettings = autoDjSettings,
                        onAutoDjSettingsChange = onAutoDjSettingsChange,
                        onTriggerDjTransition = { curve, dur ->
                            haptic.performConfirm()
                            onNext()
                        },
                        onCancelDjTransition = { haptic.performTick() },
                        onHarmonicSortQueue = {
                            haptic.performConfirm()
                            if (data.queue.size > 1) {
                                // HarmonicKeyEngine.harmonicSortQueue is the whole point of the
                                // button. This used to call shuffled(), which is the one
                                // arrangement a harmonic sort is meant to avoid.
                                val upcoming = data.queue.mapNotNull { resolveTrack(it) }
                                val sorted = HarmonicKeyEngine.harmonicSortQueue(
                                    currentTrack = (data.currentTrackId ?: data.lastTrackId)?.let { resolveTrack(it) },
                                    upcomingTracks = upcoming
                                )
                                onClearQueue()
                                sorted.forEach { onAddToQueue(it.id) }
                            }
                        },
                        audioHapticSettings = audioHapticSettings,
                        onAudioHapticSettingsChange = onAudioHapticSettingsChange,
                        onTestHapticPulse = { _ -> haptic.performTick() },
                        hiResSettings = hiResSettings,
                        onHiResSettingsChange = onHiResSettingsChange,
                        onSimulateDac = { prof -> onHiResSettingsChange(hiResSettings.copy(isBitPerfectEnabled = prof.supportsDirectFlag)) },
                        onResetDac = { onHiResSettingsChange(HiResLosslessSettings()) },
                        spatialSettings = spatialSettings,
                        onSpatialSettingsChange = onSpatialSettingsChange,
                        onApplySpatialPreset = { preset ->
                            onSpatialSettingsChange(spatialSettings.copy(
                                preset = preset,
                                roomVolumeM3 = preset.defaultVolumeM3,
                                reverbDecaySeconds = preset.defaultRt60Seconds,
                                wallDamping = preset.defaultDamping
                            ))
                        },
                        onUpdateSpatialSpeakerPosition = { left, right, dist ->
                            onSpatialSettingsChange(spatialSettings.copy(
                                speakerPosition = spatialSettings.speakerPosition.copy(
                                    leftAngleDeg = left,
                                    rightAngleDeg = right,
                                    distanceMeters = dist
                                )
                            ))
                        },
                        playerViewMode = activePlayerViewMode,
                        onPlayerViewModeChange = onPlayerViewModeChange,
                        onOpenPlayerViews = { send(BaseIntent.ShowOverlay(BaseOverlay.PlayerViews)) },
                        onShowDoc = { topicId -> send(BaseIntent.ShowOverlay(BaseOverlay.Documentation(topicId))) },
                        experienceMode = experienceMode,
                        onExperienceModeChange = { newMode ->
                            experienceMode = newMode
                            settingsStore.saveExperienceMode(newMode)
                        }
                    )
                    }
                    }
                }
                state.sheet == BaseSheet.Queue -> {
                    Box(Modifier.fillMaxSize().then(routeArrival(BaseSheet.Queue))) {
                    QueueScreen(
                        flowKey = flowKey, flowBpm = flowBpm,
                        experienceMode = experienceMode,
                        snapshot = librarySnapshot,
                        liveState = liveState,
                        canRemoteControl = true,
                        canWriteQueue = true,
                        selectedTrack = currentTrack,
                        queue = queueTracks,
                        isLocalQueue = true,
                        isPlaying = data.isPlaying,
                        shuffleMode = shuffleMode,
                        onToggleShuffle = { showFlow = true },
                        onPlayTrack = { track -> playFrom(track.id, listOfNotNull(data.currentTrackId) + data.queue) },
                        onOpenPlayer = { send(BaseIntent.OpenSheet(BaseSheet.Player)) },
                        equalizerProfile = equalizerProfile,
                        trackHasEqualizerOverride = false,
                        onCycleEqualizerPreset = onCycleEqualizerPreset,
                        onRemotePlayback = { trackId -> playFrom(trackId, listOfNotNull(data.currentTrackId) + data.queue) },
                        onAddRemoteQueue = { track -> onAddToQueue(track.id) },
                        onRemoveTrack = { track, index ->
                            queueEntries.getOrNull(index)?.first?.let { position ->
                                queueUndoManager.recordDismissal(track, position)
                                onRemoveQueueItemAt(position)
                            }
                        },
                        undoState = queueUndoState,
                        onUndoQueueRemoval = {
                            queueUndoManager.popUndo()?.let { entry ->
                                onInsertIntoQueue(entry.track.id, entry.index)
                            }
                        },
                        onDismissUndo = { queueUndoManager.clear() },
                        onReorderQueue = { tracks -> onReorderQueue(tracks.map { it.id }) },
                        onClearQueue = {
                            queueUndoManager.clear()
                            onClearQueue()
                        },
                        onToggleFavorite = { track -> onToggleFavorite(track.id) },
                        onLongClickTrack = { track -> send(BaseIntent.ShowOverlay(BaseOverlay.TrackActions(track.id))) },
                        onSaveQueueAsPlaylist = {
                            send(BaseIntent.ShowOverlay(BaseOverlay.NamePlaylist(seedTrackIds = data.queue)))
                        },
                        onClose = { send(BaseIntent.Back) },
                        onShowDoc = { topicId -> send(BaseIntent.ShowOverlay(BaseOverlay.Documentation(topicId))) }
                    )
                    }
                }
            }
        }

        if (showFlow) {
            FlowListeningSheet(
                mode = shuffleMode, onModeChange = onShuffleModeChange,
                currentTrack = currentTrack,
                upcoming = data.queue.mapNotNull(::resolveTrack),
                pool = data.visibleTracks().filter { data.isPlayable(it.id) },
                key = flowKey, filterMode = flowFilterMode, bpm = flowBpm, tolerance = flowTolerance,
                onKeyChange = { flowKeyCode = it?.code }, onFilterModeChange = { flowFilterName = it.name },
                onBpmChange = { flowBpm = it }, onToleranceChange = { flowTolerance = it },
                onPlay = { ids ->
                    val seed = if (shuffleMode == FlowShuffleMode.Off) ids.first() else ids.random()
                    onPlayTracks(listOf(seed) + ids.filterNot { it == seed })
                }, onQueue = onAddTracksToQueue,
                onSavePlaylist = { ids -> showFlow = false; send(BaseIntent.ShowOverlay(BaseOverlay.NamePlaylist(seedTrackIds = ids))) },
                onDismiss = { showFlow = false }
            )
        }

        state.overlay?.let { overlay ->
            BaseOverlayHost(
                data = data,
                overlay = overlay,
                rearrangingCrate = state.rearrangingCrate,
                activePlayerViewMode = activePlayerViewMode,
                onPlayerViewModeChange = onPlayerViewModeChange,
                send = { intent -> send(intent) },
                onCratesChange = onCratesChange,
                onCoversChange = onCoversChange,
                coverStore = coverStore,
                onRetryHost = onRetryHost,
                onDismiss = { send(BaseIntent.Back) },
                onSourceChange = onSourceChange,
                onLensChange = onLensChange,
                onPlayTracks = onPlayTracks,
                onAddToQueue = onAddToQueue,
                onOpenCollection = { kindKey, id ->
                    send(BaseIntent.Back)
                    send(BaseIntent.Push(BasePush.Collection(kindKey, id)))
                },
                allTracks = (data.tracks + localTracks).distinctBy { it.id },
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = onAddToPlaylist,
                onCreatePlaylist = onCreatePlaylist,
                onPlayNext = onPlayNext,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = { playlistId ->
                    onDeletePlaylist(playlistId)
                    if (state.pushed.any { it is BasePush.Collection && it.collectionId == playlistId }) {
                        send(BaseIntent.Back)
                    }
                },
                onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                onCreatePlaylistWithTracks = onCreatePlaylistWithTracks,
                sleepTimerState = sleepTimerState,
                onSetSleepTimer = onSetSleepTimer,
                playbackSpeed = playbackSpeed,
                pitchLocked = pitchLocked,
                onSpeedPitchChange = onSpeedPitchChange,
                loudnessMode = loudnessMode,
                onLoudnessModeChange = onLoudnessModeChange,
                lyricsDocument = lyricsDocument,
                onSeek = onSeek,
                durationMs = durationMs,
                positionMsProvider = effectivePositionMsProvider,
                jamCoordinator = jamCoordinator,
                jamSession = jamSession,
                jamSyncStatus = jamSyncStatus,
                metadataStore = metadataStore
            )
        }
        androidx.compose.material3.SnackbarHost(connectionNotice,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 144.dp))
    }
}

@Composable
private fun BaseIdentityTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun BaseTransportButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Said plainly rather than shown as an empty screen. A destination that looks built
 * and does nothing is worse than one that admits where it is in the port order.
 */
@Composable
private fun BaseNotPortedYet(what: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "$what is not in the base layer yet", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "The port order builds the frame first, then moves features in one at a time. See the base layer standard, section 6.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BasePlayerSheet(
    data: BaseLayerData,
    sheet: BaseSheet,
    activePlayerViewMode: String,
    onOpenPlayerViews: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlayTrack: (List<String>) -> Unit,
    onClose: () -> Unit
) {
    val track = data.currentTrackId?.let { data.track(it) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (sheet == BaseSheet.Queue) "Queue" else "Player",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                if (sheet == BaseSheet.Player) {
                    TextButton(onClick = onOpenPlayerViews) {
                        Text(activePlayerViewMode)
                    }
                }
                TextButton(onClick = onClose) {
                    Text(if (sheet == BaseSheet.Queue) "Player" else "Close")
                }
            }

            if (sheet == BaseSheet.Queue) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (data.queue.isEmpty()) {
                        item { BaseEmptyNote("Nothing queued.") }
                    }
                    itemsIndexed(data.queue, key = { index, id -> "$index-$id" }) { index, id ->
                        BaseTrackRow(
                            data = data,
                            trackId = id,
                            onPlay = { onPlayTrack(data.queue.drop(index)) },
                            onActions = {}
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BaseBadge(
                        text = initialsOf(track?.album ?: track?.title ?: "—"),
                        size = 160
                    )
                    Text(
                        text = track?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = track?.artist.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activePlayerViewMode != "Classic" && activePlayerViewMode != "Art") {
                        Text(
                            text = "Mode: $activePlayerViewMode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BaseTransportButton(
                            label = if (data.isPlaying) "Pause" else "Play",
                            onClick = onTogglePlay
                        )
                        BaseTransportButton(label = "Next", onClick = onNext)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedButton(onClick = onOpenQueue, modifier = Modifier.fillMaxWidth()) {
                        Text("Queue · ${data.queue.size}")
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseOverlayHost(
    data: BaseLayerData,
    overlay: BaseOverlay,
    rearrangingCrate: String?,
    activePlayerViewMode: String,
    onPlayerViewModeChange: (String) -> Unit,
    send: (BaseIntent) -> Unit,
    onDismiss: () -> Unit,
    onSourceChange: (BaseSource) -> Unit,
    onLensChange: (BaseLens) -> Unit,
    onPlayTracks: (List<String>) -> Unit,
    onAddToQueue: (String) -> Unit,
    onOpenCollection: (String, String) -> Unit,
    onCratesChange: (CrateBook) -> Unit,
    onCoversChange: (CoverBook) -> Unit = {},
    coverStore: CoverStore? = null,
    onRetryHost: () -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onAddToPlaylist: (String, String) -> Unit = { _, _ -> },
    onCreatePlaylist: (String, String) -> Unit = { _, _ -> },
    onPlayNext: (String) -> Unit = {},
    onRenamePlaylist: (String, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onRemoveTrackFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onCreatePlaylistWithTracks: (String, List<String>) -> Unit = { _, _ -> },
    sleepTimerState: SleepTimerState = SleepTimerState(),
    onSetSleepTimer: (SleepTimerMode, Long) -> Unit = { _, _ -> },
    playbackSpeed: Float = 1.0f,
    pitchLocked: Boolean = true,
    onSpeedPitchChange: (Float, Boolean) -> Unit = { _, _ -> },
    loudnessMode: LoudnessNormalizationMode = LoudnessNormalizationMode.StreamingStandard,
    onLoudnessModeChange: (LoudnessNormalizationMode) -> Unit = {},
    lyricsDocument: LyricsDocument = LyricsDocument.EMPTY,
    onSeek: (Long) -> Unit = {},
    durationMs: Long = 0L,
    positionMsProvider: () -> Long = { 0L },
    allTracks: List<Track> = data.tracks,
    jamCoordinator: JamSessionCoordinator,
    jamSession: JamSession,
    jamSyncStatus: JamSyncStatus,
    metadataStore: TrackMetadataStore? = null
) {
    val context = LocalContext.current
    val effectiveMetadataStore = metadataStore ?: remember { TrackMetadataStore(context.applicationContext) }
    val haptic = LocalHapticFeedback.current
    fun resolveTrack(id: String?): Track? = if (id == null) null else (allTracks.firstOrNull { it.id == id } ?: data.track(id))
    if (overlay is BaseOverlay.JamHub) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            JamSessionHubSheet(
                session = jamSession,
                syncStatus = jamSyncStatus,
                isHost = jamCoordinator.isHost,
                isDj = jamCoordinator.isDj,
                onStartHosting = { jamCoordinator.startHosting() },
                onJoinSession = { code -> jamCoordinator.joinSession(code) },
                onLeaveSession = { jamCoordinator.leaveSession() },
                onVoteTrack = { id, vote -> jamCoordinator.voteTrack(id, vote) },
                onPromoteSuggestion = { id -> jamCoordinator.promoteSuggestion(id) },
                onRemoveQueueItem = { id -> jamCoordinator.removeQueueItem(id) },
                onClearQueue = { jamCoordinator.clearQueue() },
                onSetDj = { devId -> jamCoordinator.setDj(devId) },
                onKickParticipant = { devId -> jamCoordinator.kickParticipant(devId) },
                onUpdatePolicy = { pol -> jamCoordinator.updatePolicy(pol) },
                onResyncClock = { jamCoordinator.triggerClockResync() },
                onClose = onDismiss,
                modifier = Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
            )
        }
        return
    }

    if (overlay is BaseOverlay.Documentation) {
        FeatureDocumentationSheet(
            topicId = overlay.topicId,
            onDismiss = onDismiss
        )
        return
    }

    if (overlay is BaseOverlay.SmartCrateBuilder) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val persistenceRepo = remember { PixelodyPersistenceRepository(context.applicationContext) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            SmartCrateRuleBuilderDialog(
                allTracks = allTracks,
                onSaveCrate = { newCrate ->
                    onCratesChange(data.crates.withCrate(newCrate))
                    coroutineScope.launch {
                        persistenceRepo.saveSmartCrate(newCrate)
                    }
                    onDismiss()
                },
                onShowDoc = { topicId -> send(BaseIntent.ShowOverlay(BaseOverlay.Documentation(topicId))) },
                onDismiss = onDismiss,
                modifier = Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
            )
        }
        return
    }

    var moveFrom by remember { mutableStateOf<Int?>(null) }
    val overlayContext = LocalContext.current

    // Picking a picture for a crate, playlist, album or track. The key of whatever asked
    // is held here while the system picker is open, because the picker returns later.
    val coverScope = rememberCoroutineScope()
    val currentCovers by rememberUpdatedState(data.covers)
    var pendingCoverKey by rememberSaveable { mutableStateOf<String?>(null) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val key = pendingCoverKey
        pendingCoverKey = null
        if (uri != null && key != null && coverStore != null) {
            coverScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    coverStore.importImage(overlayContext.applicationContext, uri)
                }
                if (saved != null) onCoversChange(currentCovers.withImage(key, saved))
                else android.widget.Toast.makeText(overlayContext, "Couldn't read this image. Try another photo.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val openCoverActions: (String, String) -> Unit = { key, title ->
        send(BaseIntent.ShowOverlay(BaseOverlay.CoverActions(key, title)))
    }

    val theme = LocalPixelodyThemeVariant.current
    val sheetShape = when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
        PixelodyMobileTheme.Obsession -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
        PixelodyMobileTheme.BulkheadTerminal -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
        else -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(role = Role.Button, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {},
            shape = sheetShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                // Tactile sheet drag handle indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                )

                when (overlay) {
                    is BaseOverlay.TrackActions -> {
                        TrackActionsSheetContent(
                            trackId = overlay.trackId,
                            playlistId = overlay.playlistId,
                            data = data,
                            allTracks = allTracks,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onToggleFavorite = onToggleFavorite,
                            onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                            onOpenCollection = onOpenCollection,
                            send = send,
                            onDismiss = onDismiss,
                            haptic = haptic
                        )
                    }

                    is BaseOverlay.CollectionActions -> {
                        val collection = data.collection(overlay.collectionId)
                        BaseOverlayTitle(
                            title = collection?.name ?: "Collection",
                            subtitle = overlay.kindKey
                        )
                        val playable = collection
                            ?.let { data.playableTrackIds(it.trackIds) }
                            .orEmpty()
                        BaseOverlayItem("Play") { onDismiss(); onPlayTracks(playable) }
                        BaseOverlayItem("Shuffle") { onDismiss(); onPlayTracks(playable.shuffled()) }
                        BaseOverlayItem("Open") {
                            onOpenCollection(overlay.kindKey, overlay.collectionId)
                        }
                        BaseOverlayItem(
                            label = "Customize cover",
                            subtitle = "Choose a photo or add a note",
                            trailing = "›"
                        ) {
                            openCoverActions(
                                collectionCoverKey(overlay.collectionId),
                                collection?.name ?: "Collection"
                            )
                        }
                        val organizer = data.crates.crates.firstOrNull { "crate:${it.id}" == overlay.collectionId }
                        if (organizer != null) BaseOverlayItem("Organize collection", subtitle = "Folders, saved tools and arrangement") {
                            send(BaseIntent.ShowOverlay(BaseOverlay.CrateActions(organizer.id)))
                        }
                        val isPlaylist = organizer == null && (overlay.kindKey == BaseBrowseShape.Playlists.kindKey ||
                            overlay.kindKey == "playlists" || overlay.collectionId.startsWith("user-pl-"))
                        if (isPlaylist) {
                            BaseOverlayItem(
                                label = "Export playlist (M3U8)",
                                subtitle = "Generate standard M3U8 playlist",
                                trailing = "›"
                            ) {
                                val playlistTracks = collection?.trackIds?.mapNotNull { data.track(it) }.orEmpty()
                                val m3uText = M3uPlaylistManager.exportToM3u(collection?.name ?: "Playlist", playlistTracks)
                                android.widget.Toast.makeText(overlayContext, "Exported ${playlistTracks.size} tracks to M3U8", android.widget.Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            BaseOverlayItem(
                                label = "Rename playlist",
                                subtitle = "Change playlist title",
                                trailing = "›"
                            ) {
                                send(
                                    BaseIntent.ShowOverlay(
                                        BaseOverlay.NamePlaylist(
                                            playlistId = overlay.collectionId,
                                            seedName = collection?.name.orEmpty()
                                        )
                                    )
                                )
                            }
                            BaseOverlayItem(
                                label = "Delete playlist",
                                subtitle = "Permanently remove this playlist",
                                trailing = "✕"
                            ) {
                                send(
                                    BaseIntent.ShowOverlay(
                                        BaseOverlay.ConfirmDeletePlaylist(
                                            playlistId = overlay.collectionId,
                                            playlistName = collection?.name.orEmpty()
                                        )
                                    )
                                )
                            }
                        }
                        BaseOverlayItem("Add to a crate") {
                            send(
                                BaseIntent.ShowOverlay(
                                    BaseOverlay.AddToCrate(
                                        description = collection?.name.orEmpty(),
                                        payloadKind = overlay.kindKey,
                                        payloadId = overlay.collectionId
                                    )
                                )
                            )
                        }
                    }

                    /**
                     * Lens only. Source moved out of here and into the session tray,
                     * because it was reachable from both and H1 allows exactly one
                     * home. What is left is a pointer, which teaches the gesture
                     * instead of duplicating the control.
                     */
                    BaseOverlay.LensPicker -> {
                        BaseOverlayTitle(
                            title = "What you are looking at",
                            subtitle = "One lens, used by every screen that lists music"
                        )
                        BaseLens.values().forEach { candidate ->
                            BaseOverlayItem(
                                label = candidate.label,
                                trailing = if (data.lens == candidate) "•" else ""
                            ) { onLensChange(candidate) }
                        }
                        BaseOverlayItem(
                            label = "Source · ${data.source.label}",
                            subtitle = "Set in the session — long press the player bar",
                            trailing = "›"
                        ) { send(BaseIntent.ShowOverlay(BaseOverlay.SessionTray)) }
                    }

                    BaseOverlay.SessionTray -> {
                        BaseOverlayTitle(
                            title = "This session",
                            subtitle = "Set once, then listen through"
                        )
                        BaseSource.values().forEach { candidate ->
                            BaseOverlayItem(
                                label = candidate.label,
                                trailing = if (data.source == candidate) "•" else ""
                            ) { onSourceChange(candidate) }
                        }
                        BaseOverlayItem(
                            label = if (data.hostReachable) "Host is connected" else "Host is not responding",
                            subtitle = if (data.hostReachable) "" else "Tap to try again"
                        ) {
                            if (!data.hostReachable) {
                                // It says "Tap to try again". It used to only close the tray.
                                onRetryHost()
                                onDismiss()
                            }
                        }
                        BaseOverlayItem(
                            label = "Loudness normalization · ${loudnessMode.label}",
                            subtitle = "EBU R128 automatic gain targeting",
                            trailing = "›"
                        ) {
                            send(BaseIntent.ShowOverlay(BaseOverlay.LoudnessNormalization))
                        }
                    }

                    is BaseOverlay.OpenCrate -> {
                        val crate = data.crates.crate(overlay.crateId)
                            ?: if (overlay.crateId.startsWith("crate-")) {
                                val pool = data.visibleTracks()
                                val curatedTracks = when (overlay.crateId) {
                                    "crate-hires-24bit" -> pool.filter { it.lossless }
                                    "crate-harmonic-flow" -> pool
                                    "crate-vinyl-warmth" -> pool.filter { it.album.isNotBlank() }
                                    "crate-heavy-rotation" -> pool
                                    else -> pool
                                }.take(9)
                                val name = when (overlay.crateId) {
                                    "crate-hires-24bit" -> "24-Bit FLAC Master"
                                    "crate-harmonic-flow" -> "Harmonic Camelot Flow"
                                    "crate-vinyl-warmth" -> "Vinyl Analog Lounge"
                                    "crate-heavy-rotation" -> "Heavy Rotation Digs"
                                    else -> "Smart Crate"
                                }
                                val slots: List<CrateSlot?> = List(9) { index ->
                                    curatedTracks.getOrNull(index)?.let { CrateSlot.SingleTrack(it.id) }
                                }
                                Crate(
                                    id = overlay.crateId,
                                    name = name,
                                    slots = slots
                                )
                            } else null
                        if (crate == null) {
                            BaseOverlayTitle(title = "That crate is gone", subtitle = "")
                            BaseOverlayItem("Close") { onDismiss() }
                        } else {
                            CrateSheet(
                                crate = crate,
                                resolve = { slot -> data.resolveCrateSlot(slot) },
                                rearranging = rearrangingCrate == crate.id,
                                selectedForMove = moveFrom,
                                onPickForMove = { index ->
                                    val from = moveFrom
                                    if (from == null) {
                                        moveFrom = index
                                    } else {
                                        onCratesChange(
                                            data.crates.replacing(crate.rearranged(from, index))
                                        )
                                        moveFrom = null
                                    }
                                },
                                onOpenSlot = { _, slot ->
                                    when (slot) {
                                        is CrateSlot.Collection ->
                                            onOpenCollection(slot.kindKey, slot.collectionId)
                                        is CrateSlot.SingleTrack -> {
                                            onDismiss()
                                            onPlayTracks(listOf(slot.trackId))
                                        }
                                        else -> onDismiss()
                                    }
                                },
                                onPlayAll = {
                                    onDismiss()
                                    onPlayTracks(data.playableInCrate(crate))
                                },
                                onRearrange = {
                                    moveFrom = null
                                    send(
                                        BaseIntent.Rearrange(
                                            if (rearrangingCrate == crate.id) null else crate.id
                                        )
                                    )
                                },
                                onSlotActions = { index ->
                                    send(
                                        BaseIntent.ShowOverlay(
                                            BaseOverlay.SlotActions(crate.id, index)
                                        )
                                    )
                                },
                                onDismiss = onDismiss
                            )
                        }
                    }

                    is BaseOverlay.CrateActions -> {
                        val crate = data.crates.crate(overlay.crateId)
                        BaseOverlayTitle(
                            title = crate?.name ?: "Crate",
                            subtitle = "${crate?.occupiedCount ?: 0} in here"
                        )
                        BaseOverlayItem("Open") {
                            send(BaseIntent.ShowOverlay(BaseOverlay.OpenCrate(overlay.crateId)))
                        }
                        BaseOverlayItem("Play everything") {
                            onDismiss()
                            crate?.let { onPlayTracks(data.playableInCrate(it)) }
                        }
                        BaseOverlayItem(
                            label = "Rearrange",
                            subtitle = "swaps two slots, never shifts a run"
                        ) {
                            send(BaseIntent.Rearrange(overlay.crateId))
                            send(BaseIntent.ShowOverlay(BaseOverlay.OpenCrate(overlay.crateId)))
                        }
                        BaseOverlayItem("Rename") {
                            send(
                                BaseIntent.ShowOverlay(
                                    BaseOverlay.NameCrate(
                                        crateId = overlay.crateId,
                                        seedName = crate?.name.orEmpty()
                                    )
                                )
                            )
                        }
                        BaseOverlayItem(
                            label = "Picture and note",
                            subtitle = "choose your own cover",
                            trailing = "›"
                        ) {
                            openCoverActions(crateCoverKey(overlay.crateId), crate?.name ?: "Crate")
                        }
                        BaseOverlayItem("Remove this crate") {
                            onCratesChange(data.crates.removing(overlay.crateId))
                            onDismiss()
                        }
                    }

                    is BaseOverlay.CoverActions -> {
                        val hasPicture = data.covers.hasCustomImage(overlay.key)
                        val note = data.covers.noteFor(overlay.key)
                        BaseOverlayTitle(
                            title = overlay.title,
                            subtitle = note.ifBlank { "Your picture and note for this" }
                        )
                        BaseOverlayItem(
                            label = if (hasPicture) "Change picture" else "Choose a picture",
                            subtitle = if (coverStore == null) "not available here" else "from your photos and files"
                        ) {
                            if (coverStore != null) {
                                pendingCoverKey = overlay.key
                                coverPicker.launch("image/*")
                            }
                        }
                        if (hasPicture) {
                            BaseOverlayItem(
                                label = "Use the original picture",
                                subtitle = "removes the one you chose"
                            ) {
                                onCoversChange(data.covers.withImage(overlay.key, null))
                            }
                        }
                        BaseOverlayItem(if (note.isBlank()) "Add a note" else "Edit note") {
                            send(BaseIntent.ShowOverlay(BaseOverlay.CoverNote(overlay.key, overlay.title)))
                        }
                        if (note.isNotBlank()) {
                            BaseOverlayItem("Remove note") {
                                onCoversChange(data.covers.withNote(overlay.key, ""))
                            }
                        }
                    }

                    is BaseOverlay.CoverNote -> {
                        var text by remember(overlay) { mutableStateOf(data.covers.noteFor(overlay.key)) }
                        BaseOverlayTitle(title = overlay.title, subtitle = "A short note, shown under the picture")
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it.take(80) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            label = { Text("Note") },
                            maxLines = 2
                        )
                        BaseOverlayItem("Save") {
                            onCoversChange(data.covers.withNote(overlay.key, text))
                            onDismiss()
                        }
                    }

                    is BaseOverlay.SlotActions -> {
                        val crate = data.crates.crate(overlay.crateId)
                        val slot = crate?.slotAt(overlay.slotIndex)
                        val face = slot?.let { data.resolveCrateSlot(it) }
                        BaseOverlayTitle(
                            title = face?.label ?: "Empty slot",
                            subtitle = "Slot ${overlay.slotIndex + 1} of ${crate?.name.orEmpty()}"
                        )
                        if (slot is CrateSlot.Collection) {
                            BaseOverlayItem("Open") {
                                onOpenCollection(slot.kindKey, slot.collectionId)
                            }
                        }
                        BaseOverlayItem(
                            label = "Take out of this crate",
                            subtitle = "slot ${overlay.slotIndex + 1} stays empty — nothing below it moves"
                        ) {
                            if (crate != null) {
                                onCratesChange(
                                    data.crates.replacing(crate.removingAt(overlay.slotIndex))
                                )
                            }
                            onDismiss()
                        }
                    }

                    is BaseOverlay.AddToCrate -> {
                        BaseOverlayTitle(
                            title = "Add to a crate",
                            subtitle = overlay.description
                        )
                        val payload = crateSlotFor(overlay)
                        data.crates.crates.forEach { crate ->
                            val landing = crate.firstEmptySlot
                            BaseOverlayItem(
                                label = crate.name,
                                subtitle = if (landing < 0) {
                                    "full — nine is the ceiling, this wants to be a playlist"
                                } else {
                                    "goes in slot ${landing + 1}"
                                }
                            ) {
                                if (landing >= 0 && payload != null) {
                                    crate.addingToFirstEmpty(payload)?.let {
                                        onCratesChange(data.crates.replacing(it))
                                    }
                                    onDismiss()
                                }
                            }
                        }
                        BaseOverlayItem(
                            label = "New crate",
                            subtitle = "it starts with this in slot 1"
                        ) {
                            send(
                                BaseIntent.ShowOverlay(
                                    BaseOverlay.NameCrate(
                                        crateId = null,
                                        seedName = "",
                                        payloadKind = overlay.payloadKind,
                                        payloadId = overlay.payloadId
                                    )
                                )
                            )
                        }
                        BaseOverlayItem(
                            label = "Build Parametric Smart Crate",
                            subtitle = "BPM match, Camelot harmonic key, & FLAC lossless rule builder",
                            trailing = "›"
                        ) {
                            send(BaseIntent.ShowOverlay(BaseOverlay.SmartCrateBuilder))
                        }
                    }

                    is BaseOverlay.NameCrate -> {
                        var name by remember(overlay) { mutableStateOf(overlay.seedName) }
                        BaseOverlayTitle(
                            title = if (overlay.crateId == null) "Name the crate" else "Rename",
                            subtitle = if (overlay.crateId == null) {
                                "A crate is made by putting something in it, so this one starts with one thing already in slot 1."
                            } else {
                                ""
                            }
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            label = { Text("Crate name") },
                            singleLine = true
                        )
                        BaseOverlayItem(
                            label = if (overlay.crateId == null) "Make it" else "Save",
                            subtitle = if (name.isBlank()) "needs a name" else ""
                        ) {
                            val trimmed = name.trim()
                            if (trimmed.isNotEmpty()) {
                                if (overlay.crateId == null) {
                                    val payload = crateSlotFor(
                                        BaseOverlay.AddToCrate(
                                            description = "",
                                            payloadKind = overlay.payloadKind,
                                            payloadId = overlay.payloadId
                                        )
                                    )
                                    val slots = List(9) { index ->
                                        if (index == 0) payload else null
                                    }
                                    onCratesChange(
                                        data.crates.withCrate(
                                            Crate(
                                                id = nextCrateId(data.crates),
                                                name = trimmed,
                                                slots = slots
                                            )
                                        )
                                    )
                                } else {
                                    onCratesChange(data.crates.renaming(overlay.crateId, trimmed))
                                }
                                onDismiss()
                            }
                        }
                    }

                    is BaseOverlay.PlayerViews -> {
                        BaseOverlayTitle(title = "Player display", subtitle = "Artwork or lyrics")
                        val viewModes = listOf("Classic" to "Artwork", "Lyrics" to "Lyrics")
                        viewModes.forEach { (modeName, modeDesc) ->
                            BaseOverlayItem(
                                label = modeName,
                                subtitle = modeDesc,
                                trailing = if (activePlayerViewMode == modeName) "•" else ""
                            ) {
                                onPlayerViewModeChange(modeName)
                                onDismiss()
                            }
                        }
                    }

                    BaseOverlay.SmartCrateBuilder -> {}
                    BaseOverlay.JamHub -> {}

                    is BaseOverlay.AddToPlaylist -> {
                        val track = resolveTrack(overlay.trackId)
                        BaseOverlayTitle(
                            title = "Add to playlist",
                            subtitle = track?.let { "${it.title} · ${it.artist}" } ?: "Select playlist"
                        )
                        BaseOverlayItem(
                            label = "New playlist",
                            subtitle = "Starts with this track",
                            trailing = "+"
                        ) {
                            send(
                                BaseIntent.ShowOverlay(
                                    BaseOverlay.NamePlaylist(
                                        playlistId = null,
                                        seedName = "",
                                        trackId = overlay.trackId
                                    )
                                )
                            )
                        }
                        val playlists = data.collectionsOfKind(BaseBrowseShape.Playlists.kindKey)
                        if (playlists.isEmpty()) {
                            BaseOverlayItem(
                                label = "No playlists yet",
                                subtitle = "Tap 'New playlist' above to create one"
                            ) {}
                        } else {
                            playlists.forEach { playlist ->
                                val count = playlist.trackIds.size
                                val alreadyIn = playlist.trackIds.contains(overlay.trackId)
                                BaseOverlayItem(
                                    label = playlist.name,
                                    subtitle = if (alreadyIn) "$count tracks · Already in this playlist" else "$count tracks",
                                    trailing = if (alreadyIn) "✓" else ""
                                ) {
                                    onAddToPlaylist(playlist.id, overlay.trackId)
                                    onDismiss()
                                }
                            }
                        }
                    }

                    is BaseOverlay.NamePlaylist -> {
                        var name by remember(overlay) { mutableStateOf(overlay.seedName) }
                        val track = resolveTrack(overlay.trackId)
                        BaseOverlayTitle(
                            title = if (overlay.playlistId == null) "New playlist" else "Rename playlist",
                            subtitle = if (overlay.playlistId == null) {
                                if (overlay.seedTrackIds.isNotEmpty()) {
                                    "${overlay.seedTrackIds.size} track${if (overlay.seedTrackIds.size == 1) "" else "s"}"
                                } else {
                                    track?.let { "Starts with \"${it.title}\"" } ?: "Creates a new playlist"
                                }
                            } else "Rename \"${overlay.seedName}\""
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            label = { Text("Playlist name") },
                            singleLine = true
                        )
                        BaseOverlayItem(
                            label = if (overlay.playlistId == null) "Create playlist" else "Save",
                            subtitle = if (name.isBlank()) "needs a name" else ""
                        ) {
                            val trimmed = name.trim()
                            if (trimmed.isNotEmpty()) {
                                if (overlay.playlistId == null) {
                                    if (overlay.seedTrackIds.isNotEmpty()) {
                                        onCreatePlaylistWithTracks(trimmed, overlay.seedTrackIds)
                                    } else {
                                        onCreatePlaylist(trimmed, overlay.trackId)
                                    }
                                } else {
                                    onRenamePlaylist(overlay.playlistId, trimmed)
                                }
                                onDismiss()
                            }
                        }
                    }

                    is BaseOverlay.ConfirmDeletePlaylist -> {
                        BaseOverlayTitle(
                            title = "Delete playlist?",
                            subtitle = "\"${overlay.playlistName}\" will be permanently deleted."
                        )
                        BaseOverlayItem(
                            label = "Delete",
                            subtitle = "Confirm permanent deletion",
                            trailing = "✕"
                        ) {
                            onDeletePlaylist(overlay.playlistId)
                            onDismiss()
                        }
                        BaseOverlayItem(
                            label = "Cancel",
                            subtitle = "Keep this playlist"
                        ) {
                            onDismiss()
                        }
                    }

                    is BaseOverlay.SleepTimer -> {
                        BaseOverlayTitle(
                            title = "Sleep timer",
                            subtitle = if (sleepTimerState.active) {
                                "Active · ${sleepTimerState.formattedRemaining} remaining"
                            } else {
                                "Gradually lowers volume during last minute before pausing"
                            }
                        )
                        SleepTimerMode.values().forEach { mode ->
                            val isSelected = sleepTimerState.active && sleepTimerState.mode == mode
                            val isOffSelected = !sleepTimerState.active && mode == SleepTimerMode.Off
                            val selected = isSelected || isOffSelected

                            val subtitle = when (mode) {
                                SleepTimerMode.Off -> if (!sleepTimerState.active) "Timer disabled" else "Turn off sleep timer"
                                SleepTimerMode.EndOfTrack -> "Pause playback when current track finishes"
                                else -> "Pause playback in ${mode.label}"
                            }

                            BaseOverlayItem(
                                label = mode.label,
                                subtitle = subtitle,
                                trailing = if (selected) "•" else ""
                            ) {
                                val currentPos = positionMsProvider()
                                val remainingSecs = if (durationMs > currentPos) (durationMs - currentPos) / 1000L else 0L
                                onSetSleepTimer(mode, remainingSecs)
                                onDismiss()
                            }
                        }
                    }

                    is BaseOverlay.PitchAndSpeed -> {
                        BaseOverlayTitle(
                            title = "Playback Speed & Pitch",
                            subtitle = if (pitchLocked) {
                                "Time-stretch pitch lock active (speed changes without pitch shift)"
                            } else {
                                "Analog tape varispeed active (pitch changes naturally with speed)"
                            },
                            onShowDoc = { send(BaseIntent.ShowOverlay(BaseOverlay.Documentation("varispeed"))) }
                        )
                        BaseOverlayItem(
                            label = if (pitchLocked) "Mode · Pitch Lock (Time-Stretch)" else "Mode · Tape Varispeed (Analog)",
                            subtitle = "Tap to toggle between pitch-locked time-stretch and tape varispeed",
                            trailing = if (pitchLocked) "⚡" else "∿"
                        ) {
                            haptic.performTick()
                            onSpeedPitchChange(playbackSpeed, !pitchLocked)
                        }
                        val speedPresets = listOf(0.5f, 0.75f, 0.85f, 1.0f, 1.15f, 1.25f, 1.5f, 2.0f)
                        speedPresets.forEach { speed ->
                            val isCurrent = kotlin.math.abs(playbackSpeed - speed) < 0.02f
                            BaseOverlayItem(
                                label = "${speed}x${if (speed == 1.0f) " (Normal)" else ""}",
                                subtitle = when {
                                    speed < 1.0f -> "Slower tempo for study / transcription"
                                    speed == 1.0f -> "Original master speed"
                                    else -> "Faster tempo"
                                },
                                trailing = if (isCurrent) "•" else ""
                            ) {
                                haptic.performTick()
                                onSpeedPitchChange(speed, pitchLocked)
                            }
                        }
                        BaseOverlayItem(
                            label = "- 0.05x",
                            subtitle = "Fine tune slower",
                            trailing = "‹"
                        ) {
                            haptic.performTick()
                            val nextSpeed = (playbackSpeed - 0.05f).coerceIn(0.25f, 3.0f)
                            onSpeedPitchChange((kotlin.math.round(nextSpeed * 100f) / 100f), pitchLocked)
                        }
                        BaseOverlayItem(
                            label = "+ 0.05x",
                            subtitle = "Fine tune faster",
                            trailing = "›"
                        ) {
                            haptic.performTick()
                            val nextSpeed = (playbackSpeed + 0.05f).coerceIn(0.25f, 3.0f)
                            onSpeedPitchChange((kotlin.math.round(nextSpeed * 100f) / 100f), pitchLocked)
                        }
                        if (playbackSpeed != 1.0f) {
                            BaseOverlayItem(
                                label = "Reset to 1.0x",
                                subtitle = "Restore original playback rate",
                                trailing = "✓"
                            ) {
                                haptic.performTick()
                                onSpeedPitchChange(1.0f, pitchLocked)
                            }
                        }
                    }

                    is BaseOverlay.LoudnessNormalization -> {
                        BaseOverlayTitle(
                            title = "Loudness Normalization",
                            subtitle = "EBU R128 automatic gain targeting across all tracks",
                            onShowDoc = { send(BaseIntent.ShowOverlay(BaseOverlay.Documentation("loudness_normalization"))) }
                        )
                        LoudnessNormalizationMode.values().forEach { mode ->
                            val isSelected = loudnessMode == mode
                            val subtitle = when (mode) {
                                LoudnessNormalizationMode.Off -> "Bit-perfect untouched original track dynamics"
                                LoudnessNormalizationMode.StreamingStandard -> "Standard -14 LUFS (Balanced streaming volume)"
                                LoudnessNormalizationMode.AudiophileDynamic -> "Audiophile -18 LUFS (Maximum headroom & dynamic range)"
                            }
                            BaseOverlayItem(
                                label = mode.label,
                                subtitle = subtitle,
                                trailing = if (isSelected) "•" else ""
                            ) {
                                haptic.performTick()
                                onLoudnessModeChange(mode)
                                onDismiss()
                            }
                        }
                    }

                    is BaseOverlay.Lyrics -> {
                        val currentTrack = (data.currentTrackId ?: data.lastTrackId)?.let { resolveTrack(it) }
                        BaseOverlayTitle(
                            title = "Lyrics · ${currentTrack?.title ?: "Track"}",
                            subtitle = if (lyricsDocument.isSynced) "Time-synchronized flow · Tap any line to seek" else "Static lyrics view"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(460.dp)
                        ) {
                            LyricsView(
                                lyricsDocument = lyricsDocument,
                                positionMs = positionMsProvider(),
                                onSeek = onSeek,
                                currentTrack = currentTrack,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    is BaseOverlay.TagEditor -> {
                        val track = resolveTrack(overlay.trackId)
                        if (track != null) {
                            TagEditorSheet(
                                track = track,
                                onSave = { newTitle, newArtist, newAlbum, newGenre ->
                                    effectiveMetadataStore.saveOverride(
                                        trackId = track.id,
                                        title = newTitle,
                                        artist = newArtist,
                                        album = newAlbum,
                                        genre = newGenre
                                    )
                                },
                                onDismiss = onDismiss
                            )
                        } else {
                            onDismiss()
                        }
                    }

                    is BaseOverlay.Documentation -> {
                        FeatureDocumentationSheet(
                            topicId = overlay.topicId,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

private fun crateSlotFor(overlay: BaseOverlay.AddToCrate): CrateSlot? = when (overlay.payloadKind) {
    "" -> null
    "track" -> CrateSlot.SingleTrack(overlay.payloadId)
    else -> CrateSlot.Collection(overlay.payloadKind, overlay.payloadId)
}

private fun sourceAndFormat(data: BaseLayerData, trackId: String, localTrack: Track? = null): String {
    val track = data.track(trackId) ?: localTrack ?: return ""
    val source = data.sourceOf(trackId).label
    val quality = if (track.lossless) "lossless" else "lossy"
    return "$source · $quality"
}

@Composable
private fun BaseOverlayTitle(
    title: String,
    subtitle: String,
    onShowDoc: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onShowDoc != null) {
            CabinetDocButton(onClick = onShowDoc)
        }
    }
}

@Composable
private fun BaseOverlayItem(
    label: String,
    subtitle: String = "",
    trailing: String = "",
    glyph: TransportGlyphType? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 50.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (glyph != null) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                PixelodyTransportGlyph(
                    glyph = glyph,
                    color = MaterialTheme.colorScheme.primary,
                    size = 18.dp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        when (trailing) {
            "›" -> PixelodyTransportGlyph(
                glyph = TransportGlyphType.ChevronRight,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                size = 16.dp
            )
            "•" -> Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            "✓" -> PixelodyTransportGlyph(
                glyph = TransportGlyphType.Checkmark,
                color = MaterialTheme.colorScheme.primary,
                size = 16.dp
            )
            "✕" -> PixelodyTransportGlyph(
                glyph = TransportGlyphType.Close,
                color = MaterialTheme.colorScheme.error,
                size = 16.dp
            )
            "♥" -> PixelodyTransportGlyph(
                glyph = TransportGlyphType.HeartFilled,
                color = Color(0xFFE53935),
                size = 18.dp
            )
            "♡" -> PixelodyTransportGlyph(
                glyph = TransportGlyphType.Heart,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
            else -> if (trailing.isNotBlank()) {
                Text(text = trailing, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TrackActionsSheetContent(
    trackId: String,
    playlistId: String?,
    data: BaseLayerData,
    allTracks: List<Track> = data.tracks,
    onPlayNext: (String) -> Unit,
    onAddToQueue: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRemoveTrackFromPlaylist: (String, String) -> Unit,
    onOpenCollection: (String, String) -> Unit,
    send: (BaseIntent) -> Unit,
    onDismiss: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val track = allTracks.firstOrNull { it.id == trackId } ?: data.track(trackId)
    val isFav = track?.favorite == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Track Header with Artwork
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteArtwork(
                artworkUrl = data.covers.imageFor(trackCoverKey(trackId), track?.artworkUrl),
                title = track?.title ?: "Track",
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: "Track",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track?.artist.orEmpty().ifBlank { "Unknown Artist" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val formatInfo = sourceAndFormat(data, trackId, track)
                if (formatInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (track?.lossless == true) {
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.DiamondLossless,
                                color = MaterialTheme.colorScheme.primary,
                                size = 11.dp
                            )
                        }
                        Text(
                            text = formatInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                PixelodyTransportGlyph(
                    glyph = TransportGlyphType.Close,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 14.dp
                )
            }
        }

        // Quick Action Tiles Row (Play Next, Queue, Favorite, Playlist)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                icon = TransportGlyphType.PlayNext,
                label = "Play Next",
                onClick = {
                    haptic.performTick()
                    onPlayNext(trackId)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = TransportGlyphType.AddToQueue,
                label = "Queue",
                onClick = {
                    haptic.performTick()
                    onAddToQueue(trackId)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = if (isFav) TransportGlyphType.HeartFilled else TransportGlyphType.Heart,
                label = if (isFav) "Liked" else "Favorite",
                iconTint = if (isFav) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
                onClick = {
                    haptic.performTick()
                    onToggleFavorite(trackId)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = TransportGlyphType.Folder,
                label = "Playlist",
                onClick = {
                    haptic.performTick()
                    send(BaseIntent.ShowOverlay(BaseOverlay.AddToPlaylist(trackId)))
                },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // Navigation & Deep Linking
        val hasNav = (track?.album?.isNotBlank() == true) ||
            (track?.artist?.isNotBlank() == true) ||
            (track?.genre?.isNotBlank() == true)
        if (hasNav) {
            TrackActionSectionHeader("EXPLORE & BROWSE")
            track?.album?.takeIf { it.isNotBlank() }?.let { album ->
                TrackActionRow(
                    icon = TransportGlyphType.VinylDisc,
                    label = "Go to album",
                    subtitle = album
                ) {
                    haptic.performTick()
                    onDismiss()
                    onOpenCollection(BaseBrowseShape.Albums.kindKey, "album:$album")
                }
            }
            track?.artist?.takeIf { it.isNotBlank() }?.let { artist ->
                TrackActionRow(
                    icon = TransportGlyphType.Broadcast,
                    label = "Go to artist",
                    subtitle = artist
                ) {
                    haptic.performTick()
                    onDismiss()
                    onOpenCollection(BaseBrowseShape.Artists.kindKey, "artist:$artist")
                }
            }
            track?.genre?.takeIf { it.isNotBlank() }?.let { genre ->
                val canonical = GenreTaxonomyEngine.canonicalize(genre)
                TrackActionRow(
                    icon = TransportGlyphType.OmniSource,
                    label = "Go to genre",
                    subtitle = canonical
                ) {
                    haptic.performTick()
                    onDismiss()
                    onOpenCollection(BaseBrowseShape.Genres.kindKey, "genre:$canonical")
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }

        // Crates & Collections
        TrackActionSectionHeader("COLLECTIONS & CRATES")
        TrackActionRow(
            icon = TransportGlyphType.Sparkle,
            label = "Add to a crate",
            subtitle = "Assign track to an active crate slot"
        ) {
            haptic.performTick()
            send(
                BaseIntent.ShowOverlay(
                    BaseOverlay.AddToCrate(
                        description = track?.title.orEmpty(),
                        payloadKind = "track",
                        payloadId = trackId
                    )
                )
            )
        }
        TrackActionRow(
            icon = TransportGlyphType.Sparkle,
            label = "Picture and note",
            subtitle = data.covers.noteFor(trackCoverKey(trackId)).ifBlank { "Choose your own cover or add a note" }
        ) {
            haptic.performTick()
            send(
                BaseIntent.ShowOverlay(
                    BaseOverlay.CoverActions(trackCoverKey(trackId), track?.title ?: "Track")
                )
            )
        }
        if (playlistId != null) {
            TrackActionRow(
                icon = TransportGlyphType.Close,
                label = "Remove from playlist",
                subtitle = "Remove track from this playlist",
                iconTint = MaterialTheme.colorScheme.error,
                labelColor = MaterialTheme.colorScheme.error
            ) {
                haptic.performTick()
                onRemoveTrackFromPlaylist(playlistId, trackId)
                onDismiss()
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // Technical Specs & Metadata Tags
        TrackActionSectionHeader("AUDIO & TAGS")
        TrackActionRow(
            icon = TransportGlyphType.Sliders,
            label = "Track details",
            subtitle = sourceAndFormat(data, trackId)
        ) {
            haptic.performTick()
            send(BaseIntent.Back)
            send(BaseIntent.Push(BasePush.TrackDetail(trackId)))
        }
        TrackActionRow(
            icon = TransportGlyphType.Settings,
            label = "Audio metadata & tags",
            subtitle = "View and edit technical tags, ID3, and lyrics"
        ) {
            haptic.performTick()
            send(BaseIntent.ShowOverlay(BaseOverlay.TagEditor(trackId)))
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun QuickActionButton(
    icon: TransportGlyphType,
    label: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = modifier.height(64.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PixelodyTransportGlyph(
                glyph = icon,
                color = iconTint,
                size = 20.dp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrackActionSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun TrackActionRow(
    icon: TransportGlyphType,
    label: String,
    subtitle: String = "",
    iconTint: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    trailingGlyph: TransportGlyphType? = TransportGlyphType.ChevronRight,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            PixelodyTransportGlyph(
                glyph = icon,
                color = iconTint,
                size = 18.dp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = labelColor
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingGlyph != null) {
            PixelodyTransportGlyph(
                glyph = trailingGlyph,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                size = 16.dp
            )
        }
    }
}

/** The three states of the slot, derived once so no surface invents a fourth. */
fun BaseLayerData.listeningSlotState(): ListeningSlotState {
    val current = currentTrackId?.let { track(it) }
    if (current != null) {
        return ListeningSlotState.Occupied(
            title = current.title,
            subtitle = current.artist,
            badge = initialsOf(current.album.ifBlank { current.title }),
            isPlaying = isPlaying
        )
    }
    val last = lastTrackId?.let { track(it) }
    if (last != null) {
        return ListeningSlotState.Resume(
            title = last.title,
            subtitle = "Paused earlier · ${last.artist}",
            badge = initialsOf(last.album.ifBlank { last.title })
        )
    }
    return ListeningSlotState.Silent
}

/**
 * Where the person is survives a process death; a menu they had open does not.
 * Overlays restore empty on purpose — a sheet that reappears by itself after the
 * system killed the process is a surprise, not a restoration.
 */
val BaseLayerStateSaver = listSaver<BaseLayerState, Any>(
    save = { state ->
        val header = listOf<Any>(
            state.destination.ordinal,
            state.sheet?.ordinal ?: -1,
            state.pushed.size
        )
        val pushed = state.pushed.flatMap { push ->
            when (push) {
                is BasePush.Collection -> listOf<Any>("c", push.kindKey, push.collectionId)
                is BasePush.TrackDetail -> listOf<Any>("d", push.trackId, "")
                BasePush.Acquire -> listOf<Any>("a", "", "")
                BasePush.Appearance -> listOf<Any>("p", "", "")
                BasePush.Technical -> listOf<Any>("t", "", "")
                BasePush.SonicTimeline -> listOf<Any>("s", "", "")
            }
        }
        header + pushed + state.queueReturnsToPlayer
    },
    restore = { saved ->
        val destination = BaseDestination.values()
            .getOrElse(saved[0] as Int) { BaseDestination.Home }
        val sheetOrdinal = saved[1] as Int
        val count = saved[2] as Int
        val pushed = (0 until count).mapNotNull { index ->
            val base = 3 + index * 3
            when (saved.getOrNull(base) as? String) {
                "c" -> BasePush.Collection(saved[base + 1] as String, saved[base + 2] as String)
                "d" -> BasePush.TrackDetail(saved[base + 1] as String)
                "a" -> BasePush.Acquire
                "p" -> BasePush.Appearance
                "t" -> BasePush.Technical
                "s" -> BasePush.SonicTimeline
                else -> null
            }
        }
        BaseLayerState(
            destination = destination,
            pushed = pushed,
            sheet = BaseSheet.values().getOrNull(sheetOrdinal),
            overlays = emptyList(),
            // Old saved states used Queue as an unconditional child of Player.
            queueReturnsToPlayer = saved.getOrNull(3 + count * 3) as? Boolean ?: true
        )
    }
)
