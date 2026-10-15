package com.pixelody.app.feature.nowplaying


import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.ui.components.ExperienceModeQuickChip
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.shadow
import com.pixelody.app.R
import com.pixelody.app.ui.components.ScanlineOverlay
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import com.pixelody.app.ui.theme.ChamferedPlate
import com.pixelody.app.ui.theme.PlateCorner
import com.pixelody.app.ui.theme.CartridgeQuestPalette
import com.pixelody.app.ui.theme.LoFiCafePalette
import com.pixelody.app.ui.theme.BulkheadTerminalPalette
import com.pixelody.app.ui.theme.ObsessionPalette
import com.pixelody.app.ui.theme.ObsidianGlassPalette
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.lyrics.LyricsDocument
import com.pixelody.app.ui.components.LyricsView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelody.app.core.playback.AudioDeviceRoute
import com.pixelody.app.core.playback.AudioRouteState
import com.pixelody.app.core.playback.EqualizerRuntimeState
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.core.playback.OscilloscopePhosphorEngine
import com.pixelody.app.data.model.AcousticChamberPreset
import com.pixelody.app.data.model.AudioHapticSettings
import com.pixelody.app.data.model.AutoDjSettings
import com.pixelody.app.data.model.CassetteTapeSettings
import com.pixelody.app.data.model.CrtBeamPersistence
import com.pixelody.app.data.model.CrtPhosphorType
import com.pixelody.app.data.model.DacHardwareProfile
import com.pixelody.app.data.model.DacTelemetryState
import com.pixelody.app.data.model.DjEnvelopeFrame
import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.HapticPrimitiveType
import com.pixelody.app.data.model.HapticPulseEvent
import com.pixelody.app.data.model.HiResLosslessSettings
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.MasteringProfile
import com.pixelody.app.data.model.OscilloscopeDisplayMode
import com.pixelody.app.data.model.OscilloscopeSettings
import com.pixelody.app.data.model.OscilloscopeTelemetry
import com.pixelody.app.data.model.RepeatMode
import com.pixelody.app.data.model.SpatialAcousticTelemetry
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.StemPreset
import com.pixelody.app.data.model.StemType
import com.pixelody.app.data.model.StemsIsolatorSettings
import com.pixelody.app.data.model.StemsIsolatorTelemetry
import com.pixelody.app.data.model.TapeMagneticsTelemetry
import com.pixelody.app.data.model.TapeTransportState
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.EmptyState
import com.pixelody.app.ui.components.InteractiveFavoriteHeart
import com.pixelody.app.ui.components.MetadataPanel
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.ProgressiveDepthHorizon
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.VelocityMicroScrubber
import com.pixelody.app.ui.components.VuAudioLevelMeter
import com.pixelody.app.ui.components.longLabel
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.navigation.PixelodyStateTags
import com.pixelody.app.ui.navigation.playerSourceStatus

internal data class PlayerViewModeItem(
    val id: String,
    val glyph: TransportGlyphType,
    val label: String
)

internal val AvailablePlayerViewModes = listOf(
    PlayerViewModeItem("Classic", TransportGlyphType.PixelodyEmblem, "Art"),
    PlayerViewModeItem("Lyrics", TransportGlyphType.WaveformBars, "Lyrics")
)

/** Retired views in old settings/crates open the ordinary player without reviving a rack. */
internal fun ordinaryPlayerViewMode(mode: String): String = if (mode == "Lyrics") "Lyrics" else "Classic"

@Composable
internal fun NowPlayingScreen(
    snapshot: LibrarySnapshot?,
    selectedTrack: Track?,
    isLocalTrack: Boolean,
    isPlaying: Boolean,
    connectionState: HostConnectionState,
    playbackError: String,
    positionMs: Long,
    durationMs: Long,
    expandedLayout: Boolean,
    globalEqualizer: EqualizerProfile,
    trackEqualizer: EqualizerProfile?,
    globalMastering: MasteringProfile = MasteringProfile(),
    trackMastering: MasteringProfile? = null,
    useMasteringRack: Boolean = false,
    equalizerRuntimeState: EqualizerRuntimeState,
    shuffleEnabled: Boolean,
    shuffleMode: FlowShuffleMode = FlowShuffleMode.Off,
    repeatMode: RepeatMode,
    onToggleMasteringRack: (Boolean) -> Unit = {},
    onGlobalEqualizerChange: (EqualizerProfile) -> Unit,
    onTrackEqualizerChange: (EqualizerProfile?) -> Unit,
    onGlobalMasteringChange: (MasteringProfile) -> Unit = {},
    onTrackMasteringChange: (MasteringProfile?) -> Unit = {},
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onCollapse: () -> Unit,
    onToggleRepeat: () -> Unit,
    audioRouteState: AudioRouteState = AudioRouteState(),
    onCycleAudioRoute: () -> Unit = {},
    playbackSpeed: Float = 1.0f,
    onSpeedChange: (Float) -> Unit = {},
    isTapeSaturationEnabled: Boolean = false,
    onToggleTapeSaturation: () -> Unit = {},
    nextTrack: Track? = null,
    autoDjEnvelope: DjEnvelopeFrame = DjEnvelopeFrame(),
    autoDjSettings: AutoDjSettings = AutoDjSettings(),
    onAutoDjSettingsChange: (AutoDjSettings) -> Unit = {},
    onTriggerDjTransition: (DjTransitionCurve, Int) -> Unit = { _, _ -> },
    onCancelDjTransition: () -> Unit = {},
    onHarmonicSortQueue: () -> Unit = {},
    audioHapticSettings: AudioHapticSettings = AudioHapticSettings(),
    latestHapticPulse: HapticPulseEvent? = null,
    onAudioHapticSettingsChange: (AudioHapticSettings) -> Unit = {},
    onTestHapticPulse: (HapticPrimitiveType) -> Unit = {},
    onNeedleDropHaptic: () -> Unit = {},
    dacTelemetry: DacTelemetryState = DacTelemetryState(),
    hiResSettings: HiResLosslessSettings = HiResLosslessSettings(),
    onHiResSettingsChange: (HiResLosslessSettings) -> Unit = {},
    onSimulateDac: (DacHardwareProfile) -> Unit = {},
    onResetDac: () -> Unit = {},
    spatialSettings: SpatialChamberSettings = SpatialChamberSettings(),
    spatialTelemetry: SpatialAcousticTelemetry = SpatialAcousticTelemetry(),
    onSpatialSettingsChange: (SpatialChamberSettings) -> Unit = {},
    onApplySpatialPreset: (AcousticChamberPreset) -> Unit = {},
    onUpdateSpatialSpeakerPosition: (Float, Float, Float) -> Unit = { _, _, _ -> },
    cassetteSettings: CassetteTapeSettings = CassetteTapeSettings(),
    cassetteTelemetry: TapeMagneticsTelemetry = TapeMagneticsTelemetry(),
    onCassetteSettingsChange: (CassetteTapeSettings) -> Unit = {},
    onSetCassetteTransportState: (TapeTransportState) -> Unit = {},
    onUpdateTapeProgress: (Long, Long) -> Unit = { _, _ -> },
    stemsSettings: StemsIsolatorSettings = StemsIsolatorSettings(),
    stemsTelemetry: StemsIsolatorTelemetry = StemsIsolatorTelemetry(),
    onStemsSettingsChange: (StemsIsolatorSettings) -> Unit = {},
    onSetStemGain: (StemType, Float) -> Unit = { _, _ -> },
    onToggleStemMute: (StemType) -> Unit = {},
    onToggleStemSolo: (StemType) -> Unit = {},
    onSetStemPan: (StemType, Float) -> Unit = { _, _ -> },
    onSetStemEq: (StemType, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onSetStemFilter: (StemType, Float) -> Unit = { _, _ -> },
    onSetStemsCrossfaderPosition: (Float) -> Unit = {},
    onApplyStemPreset: (StemPreset) -> Unit = {},
    oscilloscopeSettings: OscilloscopeSettings = OscilloscopeSettings(),
    oscilloscopeTelemetry: OscilloscopeTelemetry = OscilloscopeTelemetry(),
    oscilloscopeEngine: OscilloscopePhosphorEngine = remember { OscilloscopePhosphorEngine() },
    onOscilloscopeSettingsChange: (OscilloscopeSettings) -> Unit = {},
    onSetOscilloscopeDisplayMode: (OscilloscopeDisplayMode) -> Unit = {},
    onSetPhosphorType: (CrtPhosphorType) -> Unit = {},
    onSetOscilloscopePersistence: (CrtBeamPersistence) -> Unit = {},
    onSetOscilloscopeSensitivity: (Float) -> Unit = {},
    onUpdateOscilloscope3DRotation: (Float, Float) -> Unit = { _, _ -> },
    onSetOscilloscopePhaseRotation: (Float) -> Unit = {},
    dailyCapsule: com.pixelody.app.data.model.DailySonicCapsule = com.pixelody.app.data.model.DailySonicCapsule(),
    onPlayHistoryTrack: (String) -> Unit = {},
    onListeningDetails: () -> Unit = {},
    playerViewMode: String = "Classic",
    onPlayerViewModeChange: (String) -> Unit = {},
    onOpenPlayerViews: () -> Unit = {},
    onToggleFavorite: ((String) -> Unit)? = null,
    onAddToPlaylist: ((String) -> Unit)? = null,
    onTrackActions: ((String) -> Unit)? = null,
    sleepTimerActive: Boolean = false,
    sleepTimerRemaining: String = "",
    onOpenSleepTimer: (() -> Unit)? = null,
    pitchLocked: Boolean = true,
    onOpenPitchAndSpeed: (() -> Unit)? = null,
    loudnessModeLabel: String = "",
    lyricsDocument: LyricsDocument = LyricsDocument.EMPTY,
    onOpenLyrics: (() -> Unit)? = null,
    positionMsProvider: (() -> Long)? = null,
    onShowDoc: (String) -> Unit = {},
    experienceMode: AppExperienceMode = AppExperienceMode.Essential,
    onExperienceModeChange: (AppExperienceMode) -> Unit = {}
) {
    val dismissConnection = rememberPlayerDismissConnection(onCollapse)
    val effectivePositionMsProvider = positionMsProvider ?: { positionMs }
    var internalPlayerViewMode by remember(playerViewMode) { mutableStateOf(ordinaryPlayerViewMode(playerViewMode)) }
    val currentTrack = selectedTrack ?: snapshot?.tracks?.firstOrNull { it.id == snapshot.queue.currentTrackId }
    val sourceStatus = playerSourceStatus(
        hasTrack = currentTrack != null,
        isLocalTrack = isLocalTrack,
        isPlaying = isPlaying,
        connectionState = connectionState
    )

    var advancedEqualizerVisible by rememberSaveable { mutableStateOf(false) }
    if (advancedEqualizerVisible) {
        QuickEqualizerSheet(currentTrack?.id.orEmpty(), globalEqualizer, trackEqualizer, useMasteringRack,
            onToggleMasteringRack, onGlobalEqualizerChange, onTrackEqualizerChange,
            onDismiss = { advancedEqualizerVisible = false },
            runtimeState = equalizerRuntimeState,
            roomEffectActive = spatialSettings.isEnabled,
            onDisableRoomEffect = { onSpatialSettingsChange(spatialSettings.copy(isEnabled = false)) })
    }

    ProgressiveDepthHorizon(
        track = currentTrack, isPlaying = isPlaying,
        modifier = Modifier.fillMaxSize().nestedScroll(dismissConnection)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The dock reserves real layout space. Short windows can scroll the dock
            // independently; artwork and lyrics never paint over its hit targets.
            val dockLimit = maxHeight * 0.60f
            val wide = maxWidth >= 640.dp && maxHeight >= 400.dp
            Column(Modifier.fillMaxSize()) {
                NowPlayingHeader(
                    sourceStatus = sourceStatus, activeViewMode = internalPlayerViewMode,
                    sleepTimerActive = sleepTimerActive, sleepTimerRemaining = sleepTimerRemaining,
                    onOpenSleepTimer = onOpenSleepTimer, playbackSpeed = playbackSpeed,
                    pitchLocked = pitchLocked, onOpenPitchAndSpeed = onOpenPitchAndSpeed,
                    onOpenLyrics = onOpenLyrics ?: {
                        internalPlayerViewMode = if (internalPlayerViewMode == "Lyrics") "Classic" else "Lyrics"
                        onPlayerViewModeChange(internalPlayerViewMode)
                    },
                    experienceMode = experienceMode, onExperienceModeChange = onExperienceModeChange,
                    onOpenEqualizer = { advancedEqualizerVisible = true }, onCollapse = onCollapse,
                    dailyCapsule = dailyCapsule, onPlayHistoryTrack = onPlayHistoryTrack,
                    onListeningDetails = onListeningDetails
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds().testTag("player:content"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (currentTrack == null) {
                        item { EmptyState(text = "No current track.") }
                    } else {
                        item(key = "player_identity") {
                            if (wide) {
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    RemoteArtwork(currentTrack.artworkUrl, currentTrack.title,
                                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                                            .playerTrackSwipe(onPrevious, onNext).testTag("player:swipe-artwork"))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        NowPlayingTrackIdentity(currentTrack, isPlaying,
                                            onToggleFavorite?.let { cb -> { cb(currentTrack.id) } },
                                            onAddToPlaylist?.let { cb -> { cb(currentTrack.id) } },
                                            onTrackActions?.let { cb -> { cb(currentTrack.id) } },
                                            Modifier.playerTrackSwipe(onPrevious, onNext))
                                        PlayerGestureCue()
                                    }
                                }
                            } else {
                                if (internalPlayerViewMode != "Lyrics") {
                                    NowPlayingHeroConsole(
                                        track = currentTrack, isPlaying = isPlaying, durationMs = durationMs,
                                        shuffleEnabled = shuffleEnabled, shuffleMode = shuffleMode, repeatMode = repeatMode,
                                        onSeek = onSeek, onPrevious = onPrevious, onPlayPause = onPlayPause, onNext = onNext,
                                        onToggleShuffle = onToggleShuffle, onToggleRepeat = onToggleRepeat, onOpenQueue = onOpenQueue,
                                        onToggleFavorite = onToggleFavorite?.let { cb -> { cb(currentTrack.id) } },
                                        onAddToPlaylist = onAddToPlaylist?.let { cb -> { cb(currentTrack.id) } },
                                        onTrackActions = onTrackActions?.let { cb -> { cb(currentTrack.id) } },
                                        modifier = Modifier.fillMaxWidth(), showTransportControls = false, showProgress = false,
                                        showPlaybackChoices = false
                                    )
                                } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    NowPlayingTrackIdentity(currentTrack, isPlaying,
                                        onToggleFavorite?.let { cb -> { cb(currentTrack.id) } },
                                        onAddToPlaylist?.let { cb -> { cb(currentTrack.id) } },
                                        onTrackActions?.let { cb -> { cb(currentTrack.id) } },
                                        Modifier.playerTrackSwipe(onPrevious, onNext))
                                }
                                }
                            }
                        }
                        if (internalPlayerViewMode == "Lyrics") item(key = "lyrics") {
                            LyricsView(lyricsDocument = lyricsDocument,
                                positionMs = effectivePositionMsProvider(), onSeek = onSeek,
                                currentTrack = currentTrack,
                                modifier = Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(16.dp)))
                        }
                        item(key = "playback_choices") {
                            NowPlayingModeChips(shuffleEnabled, shuffleMode, repeatMode,
                                onToggleShuffle, onToggleRepeat, onOpenQueue)
                        }
                    }
                    if (playbackError.isNotBlank()) item(key = "playback_error") {
                        Text(playbackError, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth().heightIn(max = dockLimit)
                        .clipToBounds().testTag("player:dock")) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        currentTrack?.let { track ->
                            Box(Modifier.padding(horizontal = 16.dp).testTag("player:progress")) {
                                NowPlayingProgress(positionMs, durationMs, track.durationSeconds, onSeek,
                                    effectivePositionMsProvider)
                            }
                            NowPlayingTransportControls(track, isPlaying, onPrevious, onPlayPause, onNext)
                        }
                        BasicEqualizerControls(globalEqualizer, trackEqualizer, useMasteringRack,
                            onToggleMasteringRack, onGlobalEqualizerChange, onTrackEqualizerChange,
                            onResetEffects = { onSpatialSettingsChange(spatialSettings.copy(isEnabled = false)) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun NowPlayingHeader(
    sourceStatus: String,
    activeViewMode: String = "Classic",
    onOpenPlayerViews: () -> Unit = {},
    sleepTimerActive: Boolean = false,
    sleepTimerRemaining: String = "",
    onOpenSleepTimer: (() -> Unit)? = null,
    playbackSpeed: Float = 1.0f,
    pitchLocked: Boolean = true,
    onOpenPitchAndSpeed: (() -> Unit)? = null,
    lyricsAvailable: Boolean = false,
    onOpenLyrics: (() -> Unit)? = null,
    experienceMode: AppExperienceMode = AppExperienceMode.Essential,
    onExperienceModeChange: (AppExperienceMode) -> Unit = {},
    onOpenEqualizer: (() -> Unit)? = null,
    dailyCapsule: com.pixelody.app.data.model.DailySonicCapsule = com.pixelody.app.data.model.DailySonicCapsule(),
    onPlayHistoryTrack: (String) -> Unit = {},
    onListeningDetails: () -> Unit = {},
    onCollapse: () -> Unit
) {
    var drawer by rememberSaveable { mutableStateOf<String?>(null) }
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        Row(Modifier.fillMaxWidth().playerSheetDrag(upward = false, onComplete = onCollapse)
            .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onOpenEqualizer != null) TextButton(onClick = onOpenEqualizer,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("player:quick-equalizer")
                        .semantics { contentDescription = "Open advanced equalizer" }) { Text("EQ") }
                TextButton(onClick = { drawer = "Controls" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Controls") }
                TextButton(onClick = { drawer = "History" }, modifier = Modifier.heightIn(min = 48.dp)
                    .testTag("player:history")) { Text("History") }
            }
            Surface(onClick = onCollapse, shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(48.dp).semantics { contentDescription = "Close player" }) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(TransportGlyphType.ChevronDown,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, sizeDp = 20)
                }
            }
        }
    }
    if (drawer != null) ModalBottomSheet(onDismissRequest = { drawer = null }) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Controls", "History").forEach { page ->
                        FilterChip(selected = drawer == page, onClick = { drawer = page }, label = { Text(page) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }
            if (drawer == "History") item {
                ListeningHistorySection(dailyCapsule, onPlay = { id -> drawer = null; onPlayHistoryTrack(id) },
                    onDetails = { drawer = null; onListeningDetails() })
            } else item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Playback controls", style = MaterialTheme.typography.titleMedium)
                    Text(sourceStatus, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(PixelodyStateTags.PLAYER_SOURCE))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExperienceModeQuickChip(mode = experienceMode,
                            onToggle = { onExperienceModeChange(experienceMode.toggle()) })
                        if (onOpenEqualizer != null) TextButton(onClick = { drawer = null; onOpenEqualizer() },
                            modifier = Modifier.heightIn(min = 48.dp)
                                .semantics { contentDescription = "Open advanced equalizer" }) { Text("Equalizer") }
                        if (onOpenLyrics != null) FilterChip(selected = activeViewMode == "Lyrics",
                            onClick = { drawer = null; onOpenLyrics() }, label = { Text("Lyrics") },
                            modifier = Modifier.heightIn(min = 48.dp))
                        if (onOpenPitchAndSpeed != null) TextButton(onClick = { drawer = null; onOpenPitchAndSpeed() },
                            modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Speed ${String.format(java.util.Locale.US, "%.2f", playbackSpeed).trimEnd('0').trimEnd('.')}x")
                        }
                        if (onOpenSleepTimer != null) TextButton(onClick = { drawer = null; onOpenSleepTimer() },
                            modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(if (sleepTimerActive) "Timer $sleepTimerRemaining" else "Sleep timer")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NowPlayingHeroConsole(
    track: Track,
    isPlaying: Boolean,
    positionMs: Long = 0L,
    durationMs: Long,
    shuffleEnabled: Boolean,
    shuffleMode: FlowShuffleMode = FlowShuffleMode.Off,
    repeatMode: RepeatMode,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    audioRouteState: AudioRouteState = AudioRouteState(),
    onCycleAudioRoute: () -> Unit = {},
    onToggleFavorite: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onTrackActions: (() -> Unit)? = null,
    nextTrack: Track? = null,
    autoDjSettings: AutoDjSettings = AutoDjSettings(),
    onTriggerDjTransition: (() -> Unit)? = null,
    onOpenDjConsole: (() -> Unit)? = null,
    positionMsProvider: (() -> Long)? = null,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp),
    showTransportControls: Boolean = true,
    showProgress: Boolean = true,
    showPlaybackChoices: Boolean = true
) {
    val theme = LocalPixelodyThemeVariant.current
    val haptic = LocalHapticFeedback.current

    val consoleShape = when (theme) {
        PixelodyMobileTheme.Obsession -> ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.16f)
        PixelodyMobileTheme.CartridgeQuest -> RoundedCornerShape(6.dp)
        PixelodyMobileTheme.ObsidianGlass -> RoundedCornerShape(22.dp)
        PixelodyMobileTheme.LoFiCafe -> RoundedCornerShape(14.dp)
        PixelodyMobileTheme.BulkheadTerminal -> RoundedCornerShape(4.dp)
        PixelodyMobileTheme.Studio -> RoundedCornerShape(10.dp)
    }

    val artworkShape = when (theme) {
        PixelodyMobileTheme.Obsession -> ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.12f)
        PixelodyMobileTheme.CartridgeQuest -> RoundedCornerShape(4.dp)
        PixelodyMobileTheme.ObsidianGlass -> RoundedCornerShape(16.dp)
        PixelodyMobileTheme.LoFiCafe -> RoundedCornerShape(8.dp)
        PixelodyMobileTheme.BulkheadTerminal -> RoundedCornerShape(2.dp)
        PixelodyMobileTheme.Studio -> RoundedCornerShape(8.dp)
    }

    val consoleColor = when (theme) {
        PixelodyMobileTheme.Obsession -> ObsessionPalette.PanelDeep
        PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestPalette.RecessedBay
        PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassPalette.GlassPanel
        PixelodyMobileTheme.LoFiCafe -> LoFiCafePalette.Walnut
        PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalPalette.PanelDeep
        PixelodyMobileTheme.Studio -> MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    }

    val consoleBorder = when (theme) {
        PixelodyMobileTheme.Obsession -> BorderStroke(1.dp, ObsessionPalette.Fracture)
        PixelodyMobileTheme.CartridgeQuest -> BorderStroke(1.dp, CartridgeQuestPalette.PlasticHighlight)
        PixelodyMobileTheme.ObsidianGlass -> BorderStroke(1.dp, ObsidianGlassPalette.GlassBorder)
        PixelodyMobileTheme.LoFiCafe -> BorderStroke(1.5.dp, LoFiCafePalette.Wood)
        PixelodyMobileTheme.BulkheadTerminal -> BorderStroke(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.5f))
        PixelodyMobileTheme.Studio -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    }

    val consoleShadowElevation = when (theme) {
        PixelodyMobileTheme.Obsession -> 6.dp
        PixelodyMobileTheme.ObsidianGlass -> 6.dp
        PixelodyMobileTheme.LoFiCafe -> 5.dp
        else -> 3.dp
    }

    Surface(
        color = consoleColor,
        shape = consoleShape,
        border = consoleBorder,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(consoleShadowElevation, consoleShape)
            .clip(consoleShape)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            when (theme) {
                PixelodyMobileTheme.Obsession -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_skeleton_key),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ObsessionPalette.Signal),
                        alpha = 0.08f,
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp)
                    )
                }
                PixelodyMobileTheme.BulkheadTerminal -> {
                    ScanlineOverlay(
                        modifier = Modifier.matchParentSize(),
                        lineColor = Color(0x1278F09A),
                        spacingDp = 3.dp
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_corner_bracket),
                        contentDescription = null,
                        alpha = 0.45f,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    )
                }
                PixelodyMobileTheme.CartridgeQuest -> {
                    ScanlineOverlay(
                        modifier = Modifier.matchParentSize(),
                        lineColor = Color(0x0C000000),
                        spacingDp = 4.dp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.TopCenter)
                            .background(CartridgeQuestPalette.ContactPinGold, RoundedCornerShape(1.dp))
                    )
                }
                PixelodyMobileTheme.LoFiCafe -> {
                    Image(
                        painter = painterResource(id = R.drawable.cassette_spool_gear),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(LoFiCafePalette.Amber),
                        alpha = 0.10f,
                        modifier = Modifier
                            .size(68.dp)
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                    )
                }
                PixelodyMobileTheme.ObsidianGlass -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_sparkle_four_point),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ObsidianGlassPalette.PrismMint),
                        alpha = 0.35f,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                    )
                }
                PixelodyMobileTheme.Studio -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_corner_bracket),
                        contentDescription = null,
                        alpha = 0.35f,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Artwork Centerpiece - long-pressable for track actions & metadata
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .playerTrackSwipe(onPrevious, onNext)
                        .testTag("player:swipe-artwork")
                        .then(
                            if (onTrackActions != null) {
                                Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        haptic.performConfirm()
                                        onTrackActions()
                                    }
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    RemoteArtwork(
                        artworkUrl = track.artworkUrl,
                        title = track.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .clip(artworkShape)
                    )
                }


            PlayerGestureCue()

            // Track Identity & Format Information
            NowPlayingTrackIdentity(
                track = track,
                isPlaying = isPlaying,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = onAddToPlaylist,
                onTrackActions = onTrackActions,
                modifier = Modifier.fillMaxWidth().playerTrackSwipe(onPrevious, onNext)
            )

            // Velocity Scrubber Progress
            if (showProgress) NowPlayingProgress(
                positionMs = positionMs,
                durationMs = durationMs,
                trackDurationSeconds = track.durationSeconds,
                onSeek = onSeek,
                positionMsProvider = positionMsProvider
            )

            // Primary Tactile Transport Controls (Prev, Play/Pause, Next)
            if (showTransportControls) NowPlayingTransportControls(
                track = track,
                isPlaying = isPlaying,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext
            )

            // Ordinary playback choices; Android manages the real hardware route.
            if (showPlaybackChoices) NowPlayingModeChips(
                shuffleEnabled = shuffleEnabled,
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onOpenQueue = onOpenQueue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NowPlayingTrackIdentity(
    track: Track,
    isPlaying: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onTrackActions: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier.then(
            if (onTrackActions != null) {
                Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptic.performConfirm()
                        onTrackActions()
                    }
                )
            } else Modifier
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (onToggleFavorite != null) {
                InteractiveFavoriteHeart(
                    isFavorite = track.favorite,
                    onToggleFavorite = onToggleFavorite
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (onTrackActions != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClickLabel = "Actions for ${track.title}",
                            role = androidx.compose.ui.semantics.Role.Button
                        ) {
                            haptic.performTick()
                            onTrackActions()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.MoreVertical,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        sizeDp = 18
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            VuAudioLevelMeter(isPlaying = isPlaying)
        }
        Text(
            text = listOf(track.artist, track.album).filter { it.isNotBlank() }.joinToString(" / "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (track.lossless) {
                Surface(
                    color = Color(0xFFE5A93C).copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color(0xFFE5A93C).copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.DiamondLossless,
                            color = Color(0xFFE5A93C),
                            size = 10.dp
                        )
                        Text(
                            text = "LOSSLESS HI-RES (${track.codec.ifBlank { track.format }.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE5A93C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            if (onAddToPlaylist != null) {
                Surface(
                    onClick = onAddToPlaylist,
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.AddToQueue,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 11.dp
                        )
                        Text(
                            text = "+ PLAYLIST",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NowPlayingProgress(
    positionMs: Long = 0L,
    durationMs: Long,
    trackDurationSeconds: Int = 0,
    onSeek: (Long) -> Unit,
    positionMsProvider: (() -> Long)? = null
) {
    val effectiveDurationMs = if (durationMs > 0L) durationMs else (trackDurationSeconds.toLong() * 1000L).coerceAtLeast(0L)
    VelocityMicroScrubber(
        positionMs = positionMs,
        durationMs = effectiveDurationMs,
        onSeek = onSeek,
        positionMsProvider = positionMsProvider,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun NowPlayingTransportControls(
    track: Track,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val isActionable = track.streamUrl.isNotBlank() && !track.missing

    com.pixelody.app.ui.theme.units.PixelodyTransportControls(
        data = com.pixelody.app.ui.theme.units.TransportData(
            isPlaying = isPlaying,
            canSkipNext = isActionable,
            canSkipPrevious = true,
            onPlayPause = onPlayPause,
            onSkipNext = onNext,
            onSkipPrevious = onPrevious
        )
    )
}


@Composable
internal fun NowPlayingModeChips(
    shuffleEnabled: Boolean,
    shuffleMode: FlowShuffleMode = FlowShuffleMode.Off,
    repeatMode: RepeatMode,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    audioRouteState: AudioRouteState? = null,
    onCycleAudioRoute: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val shuffleLabel = if (shuffleMode != FlowShuffleMode.Off) shuffleMode.badge else "Shuffle"

    if (largeText) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = shuffleEnabled,
                onClick = {
                    haptic.performTick()
                    onToggleShuffle()
                },
                leadingIcon = {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.Shuffle,
                        color = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        sizeDp = 16
                    )
                },
                label = { Text(shuffleLabel) }
            )
            FilterChip(
                selected = repeatMode != RepeatMode.Off,
                onClick = {
                    haptic.performTick()
                    onToggleRepeat()
                },
                leadingIcon = {
                    PixelodyTransportGlyph(
                        glyph = when (repeatMode) {
                            RepeatMode.One -> TransportGlyphType.RepeatOne
                            else -> TransportGlyphType.Repeat
                        },
                        color = if (repeatMode != RepeatMode.Off) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        sizeDp = 16
                    )
                },
                label = { Text(repeatMode.longLabel) }
            )
            FilterChip(
                selected = false,
                onClick = {
                    haptic.performTick()
                    onOpenQueue()
                },
                leadingIcon = {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.Queue,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        sizeDp = 16
                    )
                },
                label = { Text("Queue") }
            )
            if (audioRouteState != null && onCycleAudioRoute != null) {
                Surface(
                    onClick = {
                        haptic.performTick()
                        onCycleAudioRoute()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelodyTransportGlyph(
                            glyph = when (audioRouteState.currentRoute) {
                                AudioDeviceRoute.Speaker -> TransportGlyphType.Equalizer
                                AudioDeviceRoute.Wired -> TransportGlyphType.DiamondLossless
                                AudioDeviceRoute.Bluetooth -> TransportGlyphType.MeshNetwork
                                AudioDeviceRoute.Unknown -> TransportGlyphType.OmniSource
                            },
                            color = MaterialTheme.colorScheme.primary,
                            size = 14.dp
                        )
                        Text(
                            text = audioRouteState.currentRoute.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        return
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = shuffleEnabled,
                onClick = {
                    haptic.performTick()
                    onToggleShuffle()
                },
                leadingIcon = {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.Shuffle,
                        color = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        sizeDp = 14
                    )
                },
                label = { Text(shuffleLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            )
        }
        item {
            FilterChip(
                selected = repeatMode != RepeatMode.Off,
                onClick = {
                    haptic.performTick()
                    onToggleRepeat()
                },
                leadingIcon = {
                    PixelodyTransportGlyph(
                        glyph = when (repeatMode) {
                            RepeatMode.One -> TransportGlyphType.RepeatOne
                            else -> TransportGlyphType.Repeat
                        },
                        color = if (repeatMode != RepeatMode.Off) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        sizeDp = 14
                    )
                },
                label = {
                    Text(
                        text = when (repeatMode) {
                            RepeatMode.One -> "Repeat 1"
                            RepeatMode.All -> "Repeat All"
                            RepeatMode.Off -> "Repeat"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            )
        }
        item {
            FilterChip(
                selected = false,
                onClick = {
                    haptic.performTick()
                    onOpenQueue()
                },
                leadingIcon = {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.Queue,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        sizeDp = 14
                    )
                },
                label = { Text("Queue", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            )
        }
        if (audioRouteState != null && onCycleAudioRoute != null) {
            item {
                Surface(
                    onClick = {
                        haptic.performTick()
                        onCycleAudioRoute()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelodyTransportGlyph(
                            glyph = when (audioRouteState.currentRoute) {
                                AudioDeviceRoute.Speaker -> TransportGlyphType.Equalizer
                                AudioDeviceRoute.Wired -> TransportGlyphType.DiamondLossless
                                AudioDeviceRoute.Bluetooth -> TransportGlyphType.MeshNetwork
                                AudioDeviceRoute.Unknown -> TransportGlyphType.OmniSource
                            },
                            color = MaterialTheme.colorScheme.primary,
                            size = 12.dp
                        )
                        Text(
                            text = audioRouteState.currentRoute.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerGestureCue() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        PixelodyTransportGlyph(TransportGlyphType.Previous, color = MaterialTheme.colorScheme.primary, sizeDp = 14)
        Text("Swipe artwork to change songs", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        PixelodyTransportGlyph(TransportGlyphType.Next, color = MaterialTheme.colorScheme.primary, sizeDp = 14)
    }
}
