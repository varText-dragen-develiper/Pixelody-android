package com.pixelody.app.feature.nowplaying

import com.pixelody.app.ui.components.StudioSectionToggle

import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.ui.components.ExperienceModeQuickChip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.lyrics.LyricsDocument
import com.pixelody.app.ui.components.LyricsView
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.core.playback.LiveAudioVisualizerBridge
import com.pixelody.app.data.storage.MobileEqualizerStore
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
import com.pixelody.app.data.model.EqualizerPreset
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
import com.pixelody.app.core.playback.HarmonicFlowCoordinator
import com.pixelody.app.data.model.HarmonicFlowProfile
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.AudioHapticControlRack
import com.pixelody.app.ui.components.AudioVisualizerScope
import com.pixelody.app.ui.components.AutoDjMixingConsole
import com.pixelody.app.ui.components.CabinetDocButton
import com.pixelody.app.ui.components.HarmonicFlowHud
import com.pixelody.app.ui.components.CassetteTapeDeckView
import com.pixelody.app.ui.components.EmptyState
import com.pixelody.app.ui.components.HiResLosslessHorizonCard
import com.pixelody.app.ui.components.InteractiveFavoriteHeart
import com.pixelody.app.ui.components.MetadataPanel
import com.pixelody.app.ui.components.OscilloscopePhosphorLabView
import com.pixelody.app.ui.components.PixelodyEqualizerCurve
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.ProgressiveDepthHorizon
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.SpatialAcousticChamberRack
import com.pixelody.app.ui.components.StemsDjMixingRack
import com.pixelody.app.ui.components.StudioMasteringRack
import com.pixelody.app.ui.components.TactileSoundstageDeckView
import com.pixelody.app.ui.components.FlippableCrateView
import com.pixelody.app.ui.components.PureSignalChainHud
import com.pixelody.app.ui.components.TransientBeatGridSlicer
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.TurntableDeckView
import com.pixelody.app.ui.components.VelocityMicroScrubber
import com.pixelody.app.ui.components.VuAudioLevelMeter
import com.pixelody.app.ui.components.compactLabel
import com.pixelody.app.ui.components.longLabel
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.components.tactilePress
import com.pixelody.app.ui.navigation.PixelodyStateTags
import com.pixelody.app.ui.navigation.playerSourceStatus
import kotlinx.coroutines.delay

internal data class PlayerViewModeItem(
    val id: String,
    val glyph: TransportGlyphType,
    val label: String
)

internal val AvailablePlayerViewModes = listOf(
    PlayerViewModeItem("Classic", TransportGlyphType.PixelodyEmblem, "Art"),
    PlayerViewModeItem("Soundstage", TransportGlyphType.Broadcast, "Soundstage"),
    PlayerViewModeItem("Dual-Rack", TransportGlyphType.MeshNetwork, "Dual-Rack"),
    PlayerViewModeItem("Turntable", TransportGlyphType.VinylDisc, "Turntable"),
    PlayerViewModeItem("Vinyl-Vault", TransportGlyphType.Folder, "Vinyl-Vault"),
    PlayerViewModeItem("Scope", TransportGlyphType.WaveformBars, "Scope"),
    PlayerViewModeItem("Mastering", TransportGlyphType.Sliders, "Mastering"),
    PlayerViewModeItem("Spatial", TransportGlyphType.OmniSource, "Spatial"),
    PlayerViewModeItem("Tape", TransportGlyphType.Equalizer, "Tape"),
    PlayerViewModeItem("Stems", TransportGlyphType.Settings, "Stems"),
    PlayerViewModeItem("Laser", TransportGlyphType.Sparkle, "Laser"),
    PlayerViewModeItem("Auto-DJ", TransportGlyphType.FlowShuffle, "Auto-DJ"),
    PlayerViewModeItem("Haptics", TransportGlyphType.LightningCheck, "Haptics"),
    PlayerViewModeItem("Hi-Res", TransportGlyphType.DiamondLossless, "Hi-Res")
)

internal enum class EqualizerEditScope {
    Global,
    Track
}


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
    var internalPlayerViewMode by remember(playerViewMode) { mutableStateOf(playerViewMode) }
    var showSoundTools by rememberSaveable { mutableStateOf(false) }
    var expandedCabinets by rememberSaveable { mutableStateOf(setOf<String>()) }

    fun mapModeToCabinet(mode: String): String? = when (mode) {
        "Auto-DJ" -> "Harmonic"
        "Laser", "Scope" -> "Scope"
        "Soundstage", "Spatial" -> "Spatial"
        "Dual-Rack", "Stems" -> "Stems"
        "Turntable" -> "Turntable"
        "Mastering" -> "Mastering"
        "Tape" -> "Tape"
        "Hi-Res" -> "Hi-Res"
        "Vinyl-Vault" -> "Vinyl-Vault"
        "Haptics" -> "Haptics"
        "Classic", "Lyrics" -> null
        else -> mode.takeIf { it.isNotBlank() }
    }

    LaunchedEffect(playerViewMode) {
        if (playerViewMode.isNotBlank()) {
            internalPlayerViewMode = playerViewMode
            mapModeToCabinet(playerViewMode)?.let { mapped ->
                showSoundTools = true
                expandedCabinets = expandedCabinets + mapped
            }
        }
    }

    LaunchedEffect(internalPlayerViewMode) {
        mapModeToCabinet(internalPlayerViewMode)?.let { mapped ->
            showSoundTools = true
            expandedCabinets = expandedCabinets + mapped
        }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val equalizerStore = remember(context) { MobileEqualizerStore(context) }
    val visualizerBridge = remember(context, oscilloscopeEngine) {
        LiveAudioVisualizerBridge(context, oscilloscopeEngine, coroutineScope)
    }

    DisposableEffect(visualizerBridge) {
        onDispose {
            visualizerBridge.release()
        }
    }

    val isScopeActive = isPlaying && ((showSoundTools && expandedCabinets.contains("Scope")) || internalPlayerViewMode in listOf("Scope", "Laser"))
    LaunchedEffect(isScopeActive) {
        if (isScopeActive) {
            val sessionId = equalizerStore.loadAudioSessionId()
            if (sessionId > 0) {
                visualizerBridge.attachAudioSession(sessionId)
            }
            visualizerBridge.start()
        } else {
            visualizerBridge.stop()
        }
    }

    val currentTrack = selectedTrack ?: snapshot?.tracks?.firstOrNull { it.id == snapshot.queue.currentTrackId }
    val currentKey = remember(currentTrack) { currentTrack?.let { HarmonicKeyEngine.estimateTrackTelemetry(it).key } }
    val targetKey = remember(nextTrack) { nextTrack?.let { HarmonicKeyEngine.estimateTrackTelemetry(it).key } }
    val sourceStatus = playerSourceStatus(
        hasTrack = currentTrack != null,
        isLocalTrack = isLocalTrack,
        isPlaying = isPlaying,
        connectionState = connectionState
    )

    if (expandedLayout && currentTrack != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(dismissConnection)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NowPlayingHeader(
                sourceStatus = sourceStatus,
                activeViewMode = internalPlayerViewMode,
                onOpenPlayerViews = onOpenPlayerViews,
                sleepTimerActive = sleepTimerActive,
                sleepTimerRemaining = sleepTimerRemaining,
                onOpenSleepTimer = onOpenSleepTimer,
                playbackSpeed = playbackSpeed,
                pitchLocked = pitchLocked,
                onOpenPitchAndSpeed = onOpenPitchAndSpeed,
                lyricsAvailable = lyricsDocument.hasLyrics,
                onOpenLyrics = onOpenLyrics ?: {
                    internalPlayerViewMode = if (internalPlayerViewMode == "Lyrics") "Classic" else "Lyrics"
                    onPlayerViewModeChange(internalPlayerViewMode)
                },
                experienceMode = experienceMode,
                onExperienceModeChange = onExperienceModeChange,
                onCollapse = onCollapse
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.94f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (internalPlayerViewMode == "Lyrics") {
                        LyricsView(
                            lyricsDocument = lyricsDocument,
                            positionMs = effectivePositionMsProvider(),
                            onSeek = onSeek,
                            currentTrack = currentTrack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        RemoteArtwork(
                            artworkUrl = currentTrack.artworkUrl,
                            title = currentTrack.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        MetadataPanel(track = currentTrack)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item(key = "track_identity", contentType = "track_identity") {
                        NowPlayingTrackIdentity(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            onToggleFavorite = onToggleFavorite?.let { cb -> { cb(currentTrack.id) } },
                            onAddToPlaylist = onAddToPlaylist?.let { cb -> { cb(currentTrack.id) } },
                            onTrackActions = onTrackActions?.let { cb -> { cb(currentTrack.id) } }
                        )
                    }
                    item(key = "track_progress", contentType = "track_progress") {
                        NowPlayingProgress(
                            positionMs = positionMs,
                            durationMs = durationMs,
                            trackDurationSeconds = currentTrack.durationSeconds,
                            onSeek = onSeek,
                            positionMsProvider = effectivePositionMsProvider
                        )
                    }
                    item(key = "transport_controls", contentType = "transport_controls") {
                        NowPlayingTransportControls(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            onPrevious = onPrevious,
                            onPlayPause = onPlayPause,
                            onNext = onNext
                        )
                    }
                    item(key = "mode_chips", contentType = "mode_chips") {
                        NowPlayingModeChips(
                            shuffleEnabled = shuffleEnabled,
                            shuffleMode = shuffleMode,
                            repeatMode = repeatMode,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat,
                            onOpenQueue = onOpenQueue
                        )
                    }
                    item(key = "listening_history") {
                        ListeningHistorySection(dailyCapsule, onPlayHistoryTrack, onListeningDetails)
                    }
                    if (experienceMode == AppExperienceMode.Studio) {
                item(key = "studio_sound_tools", contentType = "studio_sound_tools") {
                    StudioSectionToggle("Sound tools", buildString {
                        val eq = trackEqualizer ?: globalEqualizer
                        append(if (useMasteringRack) "${trackMastering?.preset?.displayName ?: globalMastering.preset.displayName} mastering" else if (eq.enabled) "${eq.preset.displayName} EQ" else "EQ off")
                        if (isTapeSaturationEnabled) append(" · Tape enabled")
                        if (spatialSettings.isEnabled) append(" · Spatial enabled")
                    }, showSoundTools, { showSoundTools = !showSoundTools }, "studio:player-sound-tools", Modifier.padding(horizontal = 16.dp))
                }
            }
            if (experienceMode == AppExperienceMode.Studio && showSoundTools) {
                        audiophileCabinetsSection(
                            track = currentTrack,
                            nextTrack = nextTrack,
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            positionMsProvider = effectivePositionMsProvider,
                            onSeek = onSeek,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            expandedCabinets = expandedCabinets,
                            onToggleCabinet = { id ->
                                expandedCabinets = if (expandedCabinets.contains(id)) {
                                    expandedCabinets - id
                                } else {
                                    expandedCabinets + id
                                }
                            },
                        onExpandAllCabinets = {
                            expandedCabinets = setOf("Harmonic", "Turntable", "Stems", "Mastering", "Tape", "Spatial", "Scope", "Hi-Res", "Vinyl-Vault", "Haptics")
                        },
                        onCollapseAllCabinets = {
                            expandedCabinets = emptySet()
                        },
                        audioRouteState = audioRouteState,
                        playbackSpeed = playbackSpeed,
                        onSpeedChange = onSpeedChange,
                        isTapeSaturationEnabled = isTapeSaturationEnabled,
                        onToggleTapeSaturation = onToggleTapeSaturation,
                        onNeedleDropHaptic = onNeedleDropHaptic,
                        autoDjEnvelope = autoDjEnvelope,
                        autoDjSettings = autoDjSettings,
                        onAutoDjSettingsChange = onAutoDjSettingsChange,
                        onTriggerDjTransition = onTriggerDjTransition,
                        onCancelDjTransition = onCancelDjTransition,
                        onHarmonicSortQueue = onHarmonicSortQueue,
                        audioHapticSettings = audioHapticSettings,
                        latestHapticPulse = latestHapticPulse,
                        onAudioHapticSettingsChange = onAudioHapticSettingsChange,
                        onTestHapticPulse = onTestHapticPulse,
                        dacTelemetry = dacTelemetry,
                        hiResSettings = hiResSettings,
                        onHiResSettingsChange = onHiResSettingsChange,
                        onSimulateDac = onSimulateDac,
                        onResetDac = onResetDac,
                        spatialSettings = spatialSettings,
                        spatialTelemetry = spatialTelemetry,
                        onSpatialSettingsChange = onSpatialSettingsChange,
                        onApplySpatialPreset = onApplySpatialPreset,
                        onUpdateSpatialSpeakerPosition = onUpdateSpatialSpeakerPosition,
                        cassetteSettings = cassetteSettings,
                        cassetteTelemetry = cassetteTelemetry,
                        onCassetteSettingsChange = onCassetteSettingsChange,
                        onSetCassetteTransportState = onSetCassetteTransportState,
                        stemsSettings = stemsSettings,
                        stemsTelemetry = stemsTelemetry,
                        onStemsSettingsChange = onStemsSettingsChange,
                        onSetStemGain = onSetStemGain,
                        onToggleStemMute = onToggleStemMute,
                        onToggleStemSolo = onToggleStemSolo,
                        onSetStemPan = onSetStemPan,
                        onSetStemEq = onSetStemEq,
                        onSetStemFilter = onSetStemFilter,
                        onSetStemsCrossfaderPosition = onSetStemsCrossfaderPosition,
                        onApplyStemPreset = onApplyStemPreset,
                        oscilloscopeSettings = oscilloscopeSettings,
                        oscilloscopeTelemetry = oscilloscopeTelemetry,
                        oscilloscopeEngine = oscilloscopeEngine,
                        onOscilloscopeSettingsChange = onOscilloscopeSettingsChange,
                        onSetOscilloscopeDisplayMode = onSetOscilloscopeDisplayMode,
                        onSetPhosphorType = onSetPhosphorType,
                        onSetOscilloscopePersistence = onSetOscilloscopePersistence,
                        onSetOscilloscopeSensitivity = onSetOscilloscopeSensitivity,
                        onUpdateOscilloscope3DRotation = onUpdateOscilloscope3DRotation,
                        onSetOscilloscopePhaseRotation = onSetOscilloscopePhaseRotation,
                        globalEqualizer = globalEqualizer,
                        trackEqualizer = trackEqualizer,
                        globalMastering = globalMastering,
                        trackMastering = trackMastering,
                        useMasteringRack = useMasteringRack,
                        equalizerRuntimeState = equalizerRuntimeState,
                        onToggleMasteringRack = onToggleMasteringRack,
                        onGlobalEqualizerChange = onGlobalEqualizerChange,
                        onTrackEqualizerChange = onTrackEqualizerChange,
                        onGlobalMasteringChange = onGlobalMasteringChange,
                        onTrackMasteringChange = onTrackMasteringChange,
                        snapshot = snapshot
                        )
                    }
                    if (playbackError.isNotBlank()) {
                        item(key = "playback_error", contentType = "playback_error") {
                            Text(
                                text = playbackError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val haptic = LocalHapticFeedback.current

    ProgressiveDepthHorizon(
        track = currentTrack,
        isPlaying = isPlaying,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(dismissConnection)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                tonalElevation = 3.dp,
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                )
            ) {
                NowPlayingHeader(
                    sourceStatus = sourceStatus,
                    activeViewMode = internalPlayerViewMode,
                    onOpenPlayerViews = onOpenPlayerViews,
                    sleepTimerActive = sleepTimerActive,
                    sleepTimerRemaining = sleepTimerRemaining,
                    onOpenSleepTimer = onOpenSleepTimer,
                    playbackSpeed = playbackSpeed,
                    pitchLocked = pitchLocked,
                    onOpenPitchAndSpeed = onOpenPitchAndSpeed,
                    lyricsAvailable = lyricsDocument.hasLyrics,
                    onOpenLyrics = onOpenLyrics ?: {
                        internalPlayerViewMode = if (internalPlayerViewMode == "Lyrics") "Classic" else "Lyrics"
                        onPlayerViewModeChange(internalPlayerViewMode)
                    },
                    experienceMode = experienceMode,
                    onExperienceModeChange = onExperienceModeChange,
                    onCollapse = onCollapse
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                if (currentTrack == null) {
                    item(key = "empty_state", contentType = "empty_state") { EmptyState(text = "No current track.") }
                    return@LazyColumn
                }
                item(key = "hero_console_${currentTrack.id}", contentType = "hero_console") {
                    if (internalPlayerViewMode == "Lyrics") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(440.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            ) {
                                LyricsView(
                                    lyricsDocument = lyricsDocument,
                                    positionMs = effectivePositionMsProvider(),
                                    onSeek = onSeek,
                                    currentTrack = currentTrack,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            NowPlayingProgress(
                                positionMs = positionMs,
                                durationMs = durationMs,
                                trackDurationSeconds = currentTrack.durationSeconds,
                                onSeek = onSeek,
                                positionMsProvider = effectivePositionMsProvider
                            )
                            NowPlayingTransportControls(
                                track = currentTrack,
                                isPlaying = isPlaying,
                                onPrevious = onPrevious,
                                onPlayPause = onPlayPause,
                                onNext = onNext
                            )
                        }
                    } else {
                        NowPlayingHeroConsole(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            positionMsProvider = effectivePositionMsProvider,
                            shuffleEnabled = shuffleEnabled,
                            shuffleMode = shuffleMode,
                            repeatMode = repeatMode,
                            onSeek = onSeek,
                            onPrevious = onPrevious,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat,
                            onOpenQueue = onOpenQueue,
                            audioRouteState = audioRouteState,
                            onCycleAudioRoute = onCycleAudioRoute,
                            onToggleFavorite = onToggleFavorite?.let { cb -> { cb(currentTrack.id) } },
                            onAddToPlaylist = onAddToPlaylist?.let { cb -> { cb(currentTrack.id) } },
                            onTrackActions = onTrackActions?.let { cb -> { cb(currentTrack.id) } }
                        )
                    }
                }
            item(key = "listening_history") {
                Box(Modifier.padding(horizontal = 16.dp)) { ListeningHistorySection(dailyCapsule, onPlayHistoryTrack, onListeningDetails) }
            }
            if (experienceMode == AppExperienceMode.Studio) {
                item(key = "studio_sound_tools", contentType = "studio_sound_tools") {
                    StudioSectionToggle("Sound tools", buildString {
                        val eq = trackEqualizer ?: globalEqualizer
                        append(if (useMasteringRack) "${trackMastering?.preset?.displayName ?: globalMastering.preset.displayName} mastering" else if (eq.enabled) "${eq.preset.displayName} EQ" else "EQ off")
                        if (isTapeSaturationEnabled) append(" · Tape enabled")
                        if (spatialSettings.isEnabled) append(" · Spatial enabled")
                    }, showSoundTools, { showSoundTools = !showSoundTools }, "studio:player-sound-tools", Modifier.padding(horizontal = 16.dp))
                }
            }
            if (experienceMode == AppExperienceMode.Studio && showSoundTools) {
                audiophileCabinetsSection(
                    track = currentTrack,
                    nextTrack = nextTrack,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    positionMsProvider = effectivePositionMsProvider,
                    onSeek = onSeek,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    expandedCabinets = expandedCabinets,
                    onToggleCabinet = { id ->
                        expandedCabinets = if (expandedCabinets.contains(id)) {
                            expandedCabinets - id
                        } else {
                            expandedCabinets + id
                        }
                    },
                    onExpandAllCabinets = {
                        expandedCabinets = setOf("Harmonic", "Turntable", "Stems", "Mastering", "Tape", "Spatial", "Scope", "Hi-Res", "Vinyl-Vault", "Haptics")
                    },
                    onCollapseAllCabinets = {
                        expandedCabinets = emptySet()
                    },
                    audioRouteState = audioRouteState,
                    playbackSpeed = playbackSpeed,
                    onSpeedChange = onSpeedChange,
                    isTapeSaturationEnabled = isTapeSaturationEnabled,
                    onToggleTapeSaturation = onToggleTapeSaturation,
                    onNeedleDropHaptic = onNeedleDropHaptic,
                    autoDjEnvelope = autoDjEnvelope,
                    autoDjSettings = autoDjSettings,
                    onAutoDjSettingsChange = onAutoDjSettingsChange,
                    onTriggerDjTransition = onTriggerDjTransition,
                    onCancelDjTransition = onCancelDjTransition,
                    onHarmonicSortQueue = onHarmonicSortQueue,
                    audioHapticSettings = audioHapticSettings,
                    latestHapticPulse = latestHapticPulse,
                    onAudioHapticSettingsChange = onAudioHapticSettingsChange,
                    onTestHapticPulse = onTestHapticPulse,
                    dacTelemetry = dacTelemetry,
                    hiResSettings = hiResSettings,
                    onHiResSettingsChange = onHiResSettingsChange,
                    onSimulateDac = onSimulateDac,
                    onResetDac = onResetDac,
                    spatialSettings = spatialSettings,
                    spatialTelemetry = spatialTelemetry,
                    onSpatialSettingsChange = onSpatialSettingsChange,
                    onApplySpatialPreset = onApplySpatialPreset,
                    onUpdateSpatialSpeakerPosition = onUpdateSpatialSpeakerPosition,
                    cassetteSettings = cassetteSettings,
                    cassetteTelemetry = cassetteTelemetry,
                    onCassetteSettingsChange = onCassetteSettingsChange,
                    onSetCassetteTransportState = onSetCassetteTransportState,
                    stemsSettings = stemsSettings,
                    stemsTelemetry = stemsTelemetry,
                    onStemsSettingsChange = onStemsSettingsChange,
                    onSetStemGain = onSetStemGain,
                    onToggleStemMute = onToggleStemMute,
                    onToggleStemSolo = onToggleStemSolo,
                    onSetStemPan = onSetStemPan,
                    onSetStemEq = onSetStemEq,
                    onSetStemFilter = onSetStemFilter,
                    onSetStemsCrossfaderPosition = onSetStemsCrossfaderPosition,
                    onApplyStemPreset = onApplyStemPreset,
                    oscilloscopeSettings = oscilloscopeSettings,
                    oscilloscopeTelemetry = oscilloscopeTelemetry,
                    oscilloscopeEngine = oscilloscopeEngine,
                    onOscilloscopeSettingsChange = onOscilloscopeSettingsChange,
                    onSetOscilloscopeDisplayMode = onSetOscilloscopeDisplayMode,
                    onSetPhosphorType = onSetPhosphorType,
                    onSetOscilloscopePersistence = onSetOscilloscopePersistence,
                    onSetOscilloscopeSensitivity = onSetOscilloscopeSensitivity,
                    onUpdateOscilloscope3DRotation = onUpdateOscilloscope3DRotation,
                    onSetOscilloscopePhaseRotation = onSetOscilloscopePhaseRotation,
                    globalEqualizer = globalEqualizer,
                    trackEqualizer = trackEqualizer,
                    globalMastering = globalMastering,
                    trackMastering = trackMastering,
                    useMasteringRack = useMasteringRack,
                    equalizerRuntimeState = equalizerRuntimeState,
                    onToggleMasteringRack = onToggleMasteringRack,
                    onGlobalEqualizerChange = onGlobalEqualizerChange,
                    onTrackEqualizerChange = onTrackEqualizerChange,
                    onGlobalMasteringChange = onGlobalMasteringChange,
                    onTrackMasteringChange = onTrackMasteringChange,
                    snapshot = snapshot,
                    onShowDoc = onShowDoc
                )
            }
            if (playbackError.isNotBlank()) {
                item(key = "playback_error") {
                    Text(
                        text = playbackError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AudiophileCabinetCard(
    title: String,
    statusText: String,
    glyph: TransportGlyphType,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    docTopicId: String? = null,
    onShowDoc: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp),
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            if (isExpanded) accentColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            haptic.performTick()
                            onToggleExpand()
                        },
                        onLongClick = {
                            if (docTopicId != null && onShowDoc != null) {
                                haptic.performConfirm()
                                onShowDoc(docTopicId)
                            }
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelodyTransportGlyph(
                                glyph = glyph,
                                color = accentColor,
                                sizeDp = 14
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (statusText.isNotBlank()) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (docTopicId != null && onShowDoc != null) {
                        CabinetDocButton(
                            onClick = { onShowDoc(docTopicId) },
                            contentDescription = "$title documentation"
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isExpanded) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (isExpanded) accentColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = if (isExpanded) "ACTIVE" else "EXPAND",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = if (isExpanded) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    PixelodyTransportGlyph(
                        glyph = if (isExpanded) TransportGlyphType.ChevronDown else TransportGlyphType.ChevronRight,
                        color = if (isExpanded) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        sizeDp = 12
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        content()
                    }
                }
            }
        }
    }
}

@Composable
internal fun AudiophileCabinetsHeader(
    expandedCabinets: Set<String>,
    onToggleCabinet: (String) -> Unit,
    onExpandAllCabinets: () -> Unit,
    onCollapseAllCabinets: () -> Unit,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp)
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "STUDIO EXPANSIONS & CABINETS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${expandedCabinets.size} of 10 Cabinets Open",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (expandedCabinets.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            haptic.performTick()
                            onCollapseAllCabinets()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Collapse All",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                    }
                }
                TextButton(
                    onClick = {
                        haptic.performTick()
                        onExpandAllCabinets()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Expand All",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val cabinetQuickItems = listOf(
                Triple("Harmonic", TransportGlyphType.FlowShuffle, "DJ Flow"),
                Triple("Turntable", TransportGlyphType.VinylDisc, "Turntable"),
                Triple("Stems", TransportGlyphType.Settings, "4-Stem"),
                Triple("Mastering", TransportGlyphType.Sliders, "Mastering"),
                Triple("Tape", TransportGlyphType.Equalizer, "Tape Deck"),
                Triple("Spatial", TransportGlyphType.OmniSource, "Spatial 3D"),
                Triple("Scope", TransportGlyphType.WaveformBars, "CRT Scope"),
                Triple("Hi-Res", TransportGlyphType.DiamondLossless, "Hi-Res DAC"),
                Triple("Vinyl-Vault", TransportGlyphType.Folder, "Vault Crate"),
                Triple("Haptics", TransportGlyphType.LightningCheck, "Haptics")
            )
            items(cabinetQuickItems, key = { it.first }) { (id, glyph, label) ->
                val isOpen = expandedCabinets.contains(id)
                Surface(
                    onClick = {
                        haptic.performTick()
                        onToggleCabinet(id)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isOpen) MaterialTheme.colorScheme.primary else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PixelodyTransportGlyph(
                            glyph = glyph,
                            color = if (isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            sizeDp = 12
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isOpen) FontWeight.Bold else FontWeight.Medium,
                            color = if (isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.audiophileCabinetsSection(
    track: Track,
    nextTrack: Track?,
    isPlaying: Boolean,
    positionMs: Long = 0L,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    expandedCabinets: Set<String>,
    onToggleCabinet: (String) -> Unit,
    onExpandAllCabinets: () -> Unit,
    onCollapseAllCabinets: () -> Unit,
    audioRouteState: AudioRouteState,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    isTapeSaturationEnabled: Boolean,
    onToggleTapeSaturation: () -> Unit,
    onNeedleDropHaptic: () -> Unit,
    autoDjEnvelope: DjEnvelopeFrame,
    autoDjSettings: AutoDjSettings,
    onAutoDjSettingsChange: (AutoDjSettings) -> Unit,
    onTriggerDjTransition: (DjTransitionCurve, Int) -> Unit,
    onCancelDjTransition: () -> Unit,
    onHarmonicSortQueue: () -> Unit,
    audioHapticSettings: AudioHapticSettings,
    latestHapticPulse: HapticPulseEvent?,
    onAudioHapticSettingsChange: (AudioHapticSettings) -> Unit,
    onTestHapticPulse: (HapticPrimitiveType) -> Unit,
    dacTelemetry: DacTelemetryState,
    hiResSettings: HiResLosslessSettings,
    onHiResSettingsChange: (HiResLosslessSettings) -> Unit,
    onSimulateDac: (DacHardwareProfile) -> Unit,
    onResetDac: () -> Unit,
    spatialSettings: SpatialChamberSettings,
    spatialTelemetry: SpatialAcousticTelemetry,
    onSpatialSettingsChange: (SpatialChamberSettings) -> Unit,
    onApplySpatialPreset: (AcousticChamberPreset) -> Unit,
    onUpdateSpatialSpeakerPosition: (Float, Float, Float) -> Unit,
    cassetteSettings: CassetteTapeSettings,
    cassetteTelemetry: TapeMagneticsTelemetry,
    onCassetteSettingsChange: (CassetteTapeSettings) -> Unit,
    onSetCassetteTransportState: (TapeTransportState) -> Unit,
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
    globalEqualizer: EqualizerProfile = EqualizerProfile(),
    trackEqualizer: EqualizerProfile? = null,
    globalMastering: MasteringProfile = MasteringProfile(),
    trackMastering: MasteringProfile? = null,
    useMasteringRack: Boolean = false,
    equalizerRuntimeState: EqualizerRuntimeState = EqualizerRuntimeState(),
    onToggleMasteringRack: (Boolean) -> Unit = {},
    onGlobalEqualizerChange: (EqualizerProfile) -> Unit = {},
    onTrackEqualizerChange: (EqualizerProfile?) -> Unit = {},
    onGlobalMasteringChange: (MasteringProfile) -> Unit = {},
    onTrackMasteringChange: (MasteringProfile?) -> Unit = {},
    oscilloscopeSettings: OscilloscopeSettings = OscilloscopeSettings(),
    oscilloscopeTelemetry: OscilloscopeTelemetry = OscilloscopeTelemetry(),
    oscilloscopeEngine: OscilloscopePhosphorEngine = OscilloscopePhosphorEngine(),
    onOscilloscopeSettingsChange: (OscilloscopeSettings) -> Unit = {},
    onSetOscilloscopeDisplayMode: (OscilloscopeDisplayMode) -> Unit = {},
    onSetPhosphorType: (CrtPhosphorType) -> Unit = {},
    onSetOscilloscopePersistence: (CrtBeamPersistence) -> Unit = {},
    onSetOscilloscopeSensitivity: (Float) -> Unit = {},
    onUpdateOscilloscope3DRotation: (Float, Float) -> Unit = { _, _ -> },
    onSetOscilloscopePhaseRotation: (Float) -> Unit = {},
    positionMsProvider: (() -> Long)? = null,
    snapshot: LibrarySnapshot? = null,
    onShowDoc: (String) -> Unit = {}
) {
    item(key = "cabinets_summary", contentType = "cabinets_summary") {
        AudiophileCabinetsHeader(
            expandedCabinets = expandedCabinets,
            onToggleCabinet = onToggleCabinet,
            onExpandAllCabinets = onExpandAllCabinets,
            onCollapseAllCabinets = onCollapseAllCabinets
        )
    }

    // Cabinet 1: Harmonic DJ Flow & Beat Slicer
    item(key = "cabinet_harmonic", contentType = "cabinet_card") {
        val trackTelemetry = remember(track.id) { HarmonicKeyEngine.estimateTrackTelemetry(track) }
        val currentKey = trackTelemetry.key
        AudiophileCabinetCard(
            title = "HARMONIC DJ FLOW & BEAT SLICER",
            statusText = "Key ${currentKey ?: "8A"} • BPM ${trackTelemetry.bpm.toInt()}",
            glyph = TransportGlyphType.FlowShuffle,
            accentColor = MaterialTheme.colorScheme.primary,
            isExpanded = expandedCabinets.contains("Harmonic"),
            onToggleExpand = { onToggleCabinet("Harmonic") },
            docTopicId = "auto_dj",
            onShowDoc = onShowDoc
        ) {
            val haptic = LocalHapticFeedback.current
            val currentPosition = positionMsProvider?.invoke() ?: positionMs
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HarmonicFlowHud(
                    currentTrack = track,
                    nextTrack = nextTrack,
                    positionMs = currentPosition,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    activeProfile = HarmonicFlowCoordinator.activeProfile.value,
                    onTriggerMix = { onTriggerDjTransition(DjTransitionCurve.EqualPower, 8) },
                    onOpenDjConsole = {},
                    modifier = Modifier.fillMaxWidth()
                )
                TransientBeatGridSlicer(
                    bpm = trackTelemetry.bpm.toInt(),
                    positionMs = currentPosition,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth()
                )
                MasteringWaveformVisualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performConfirm()
                            onTriggerDjTransition(DjTransitionCurve.EqualPower, 8)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Trigger Auto-DJ Mix")
                    }
                    OutlinedButton(
                        onClick = {
                            haptic.performConfirm()
                            onHarmonicSortQueue()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sort Queue Harmonics")
                    }
                }
            }
        }
    }

    // Cabinet 2: Analog Vinyl Turntable
    item(key = "cabinet_turntable", contentType = "cabinet_card") {
        AudiophileCabinetCard(
            title = "ANALOG VINYL TURNTABLE",
            statusText = if (isPlaying) "Platter Spinning • ${if (playbackSpeed > 1.1f) "45 RPM" else "33⅓ RPM"}" else "Platter Stopped",
            glyph = TransportGlyphType.VinylDisc,
            accentColor = Color(0xFFE5A93C),
            isExpanded = expandedCabinets.contains("Turntable"),
            onToggleExpand = { onToggleCabinet("Turntable") },
            docTopicId = "turntable",
            onShowDoc = onShowDoc
        ) {
            TurntableDeckView(
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                trackTitle = track.title,
                artistName = track.artist,
                artworkUrl = track.artworkUrl,
                onSeek = onSeek,
                playbackSpeed = playbackSpeed,
                onSpeedChange = onSpeedChange,
                isTapeSaturationEnabled = isTapeSaturationEnabled,
                onToggleTapeSaturation = onToggleTapeSaturation,
                onNeedleDrop = onNeedleDropHaptic,
                positionMsProvider = positionMsProvider
            )
        }
    }

    // Cabinet 3: 4-Stem Audio Isolator & Mixer
    item(key = "cabinet_stems", contentType = "cabinet_card") {
        AudiophileCabinetCard(
            title = "4-STEM ISOLATOR & DJ MIXER",
            statusText = "${stemsSettings.channels.count { !it.value.isMuted }} of 4 Stems Active",
            glyph = TransportGlyphType.Settings,
            accentColor = Color(0xFF4CAF50),
            isExpanded = expandedCabinets.contains("Stems"),
            onToggleExpand = { onToggleCabinet("Stems") },
            docTopicId = "stems_isolator",
            onShowDoc = onShowDoc
        ) {
            StemsDjMixingRack(
                settings = stemsSettings,
                telemetry = stemsTelemetry,
                trackTitle = track.title,
                trackArtist = track.artist,
                onUpdateSettings = onStemsSettingsChange,
                onSetStemGain = onSetStemGain,
                onToggleMute = onToggleStemMute,
                onToggleSolo = onToggleStemSolo,
                onSetStemPan = onSetStemPan,
                onSetStemEq = onSetStemEq,
                onSetStemFilter = onSetStemFilter,
                onSetCrossfaderPosition = onSetStemsCrossfaderPosition,
                onApplyPreset = onApplyStemPreset,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Cabinet 4: Studio Mastering DSP & 10-Band EQ
    item(key = "cabinet_mastering", contentType = "cabinet_card") {
        AudiophileCabinetCard(
            title = "STUDIO MASTERING & PARAMETRIC EQ",
            statusText = if (useMasteringRack) "Mastering DSP Rack (${trackMastering?.preset?.displayName ?: globalMastering.preset.displayName})" else "5-Band EQ (${globalEqualizer.preset.displayName})",
            glyph = TransportGlyphType.Sliders,
            accentColor = Color(0xFFAB47BC),
            isExpanded = expandedCabinets.contains("Mastering"),
            onToggleExpand = { onToggleCabinet("Mastering") },
            docTopicId = "mastering_rack",
            onShowDoc = onShowDoc
        ) {
            EqualizerPanel(
                track = track,
                globalEqualizer = globalEqualizer,
                trackEqualizer = trackEqualizer,
                globalMastering = globalMastering,
                trackMastering = trackMastering,
                useMasteringRack = useMasteringRack,
                isPlaying = isPlaying,
                runtimeState = equalizerRuntimeState,
                onToggleMasteringRack = onToggleMasteringRack,
                onGlobalEqualizerChange = onGlobalEqualizerChange,
                onTrackEqualizerChange = onTrackEqualizerChange,
                onGlobalMasteringChange = onGlobalMasteringChange,
                onTrackMasteringChange = onTrackMasteringChange
            )
        }
    }

    // Cabinet 5: Magnetic Cassette Tape Deck
    item(key = "cabinet_tape", contentType = "cabinet_card") {
        AudiophileCabinetCard(
            title = "MAGNETIC CASSETTE TAPE DECK",
            statusText = if (isTapeSaturationEnabled) "Tape Saturation Warmth ON" else "Type II Chrome • 15 ips",
            glyph = TransportGlyphType.Equalizer,
            accentColor = Color(0xFFFF7043),
            isExpanded = expandedCabinets.contains("Tape"),
            onToggleExpand = { onToggleCabinet("Tape") },
            docTopicId = "cassette_deck",
            onShowDoc = onShowDoc
        ) {
            val currentPosition = positionMsProvider?.invoke() ?: positionMs
            CassetteTapeDeckView(
                settings = cassetteSettings,
                telemetry = cassetteTelemetry,
                trackTitle = track.title,
                trackArtist = track.artist,
                playbackPositionMs = currentPosition,
                trackDurationMs = durationMs,
                onUpdateSettings = onCassetteSettingsChange,
                onSetTransportState = onSetCassetteTransportState,
                onTogglePlayPause = onPlayPause,
                onSeekRewind = { onSeek((currentPosition - 15000L).coerceAtLeast(0L)) },
                onSeekFastForward = { onSeek((currentPosition + 15000L).coerceAtMost(durationMs)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Cabinet 6: 3D Spatial Acoustic Chamber & Soundstage
    item(key = "cabinet_spatial", contentType = "cabinet_card") {
        AudiophileCabinetCard(
            title = "3D SPATIAL CHAMBER & SOUNDSTAGE",
            statusText = "Preset: ${spatialSettings.preset.title} • Binaural",
            glyph = TransportGlyphType.OmniSource,
            accentColor = Color(0xFF26C6DA),
            isExpanded = expandedCabinets.contains("Spatial"),
            onToggleExpand = { onToggleCabinet("Spatial") },
            docTopicId = "spatial_chamber",
            onShowDoc = onShowDoc
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TactileSoundstageDeckView(
                    track = track,
                    modifier = Modifier.fillMaxWidth()
                )
                SpatialAcousticChamberRack(
                    settings = spatialSettings,
                    telemetry = spatialTelemetry,
                    onUpdateSettings = onSpatialSettingsChange,
                    onApplyPreset = onApplySpatialPreset,
                    onUpdateSpeakerPosition = onUpdateSpatialSpeakerPosition,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Cabinet 7: CRT Phosphor Oscilloscope & Vector Scope
    item(key = "cabinet_scope", contentType = "cabinet_card") {
        val trackTelemetry = remember(track.id) { HarmonicKeyEngine.estimateTrackTelemetry(track) }
        val currentKey = trackTelemetry.key
        val targetKey = remember(nextTrack?.id) { nextTrack?.let { HarmonicKeyEngine.estimateTrackTelemetry(it).key } }
        AudiophileCabinetCard(
            title = "CRT PHOSPHOR OSCILLOSCOPE & SCOPE",
            statusText = "Mode: ${oscilloscopeSettings.displayMode.name} • Phosphor: ${oscilloscopeSettings.phosphorType.title}",
            glyph = TransportGlyphType.WaveformBars,
            accentColor = Color(0xFF00E676),
            isExpanded = expandedCabinets.contains("Scope"),
            onToggleExpand = { onToggleCabinet("Scope") },
            docTopicId = "oscilloscope",
            onShowDoc = onShowDoc
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AudioVisualizerScope(
                    isPlaying = isPlaying,
                    currentKey = currentKey,
                    targetKey = targetKey,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
                OscilloscopePhosphorLabView(
                    settings = oscilloscopeSettings,
                    telemetry = oscilloscopeTelemetry,
                    engine = oscilloscopeEngine,
                    onUpdateSettings = onOscilloscopeSettingsChange,
                    onSetDisplayMode = onSetOscilloscopeDisplayMode,
                    onSetPhosphorType = onSetPhosphorType,
                    onSetPersistence = onSetOscilloscopePersistence,
                    onSetSensitivity = onSetOscilloscopeSensitivity,
                    onUpdate3DRotation = onUpdateOscilloscope3DRotation,
                    onSetPhaseRotation = onSetOscilloscopePhaseRotation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Cabinet 8: Bit-Perfect DAC & Signal Chain
    item(key = "cabinet_hires", contentType = "cabinet_card") {
        AudiophileCabinetCard(
            title = "BIT-PERFECT DAC & SIGNAL CHAIN",
            statusText = if (track.lossless) "Lossless • ${track.codec.ifBlank { track.format }.uppercase()}" else "Standard Audio",
            glyph = TransportGlyphType.DiamondLossless,
            accentColor = Color(0xFFE5A93C),
            isExpanded = expandedCabinets.contains("Hi-Res"),
            onToggleExpand = { onToggleCabinet("Hi-Res") },
            docTopicId = "hires_lossless",
            onShowDoc = onShowDoc
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PureSignalChainHud(
                    track = track,
                    activeOutputDevice = audioRouteState.currentRoute.label,
                    modifier = Modifier.fillMaxWidth()
                )
                HiResLosslessHorizonCard(
                    telemetry = dacTelemetry,
                    settings = hiResSettings,
                    onSettingsChange = onHiResSettingsChange,
                    onSimulateDac = onSimulateDac,
                    onResetDac = onResetDac,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Cabinet 9: Vinyl Vault Crate & Track Metadata
    item(key = "cabinet_vault", contentType = "cabinet_card") {
        AudiophileCabinetCard(
            title = "VINYL CRATE & EXTENDED METADATA",
            statusText = "${snapshot?.tracks?.size ?: 1} Tracks in Vault",
            glyph = TransportGlyphType.Folder,
            accentColor = Color(0xFF78909C),
            isExpanded = expandedCabinets.contains("Vinyl-Vault"),
            onToggleExpand = { onToggleCabinet("Vinyl-Vault") },
            docTopicId = "vinyl_vault",
            onShowDoc = onShowDoc
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetadataPanel(track = track)
                val tracksForVault = remember(snapshot, track) {
                    val list = snapshot?.tracks.orEmpty()
                    if (list.isNotEmpty()) list else listOfNotNull(track)
                }
                FlippableCrateView(
                    tracks = tracksForVault,
                    activeTrack = track,
                    onSelectTrack = { onSeek(0L) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Cabinet 10: Tactile Audio Haptics Lab
    item(key = "cabinet_haptics", contentType = "cabinet_card") {
        AudiophileCabinetCard(
            title = "TACTILE AUDIO HAPTICS LAB",
            statusText = if (audioHapticSettings.isEnabled) "Transducer Active" else "Haptics Disabled",
            glyph = TransportGlyphType.LightningCheck,
            accentColor = Color(0xFFFFCA28),
            isExpanded = expandedCabinets.contains("Haptics"),
            onToggleExpand = { onToggleCabinet("Haptics") },
            docTopicId = "audio_haptics",
            onShowDoc = onShowDoc
        ) {
            AudioHapticControlRack(
                settings = audioHapticSettings,
                latestPulse = latestHapticPulse,
                onSettingsChange = onAudioHapticSettingsChange,
                onTestPulse = onTestHapticPulse,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun AudiophileCabinetsRack(
    track: Track,
    nextTrack: Track?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    expandedCabinets: Set<String>,
    onToggleCabinet: (String) -> Unit,
    onExpandAllCabinets: () -> Unit,
    onCollapseAllCabinets: () -> Unit,
    audioRouteState: AudioRouteState,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    isTapeSaturationEnabled: Boolean,
    onToggleTapeSaturation: () -> Unit,
    onNeedleDropHaptic: () -> Unit,
    autoDjEnvelope: DjEnvelopeFrame,
    autoDjSettings: AutoDjSettings,
    onAutoDjSettingsChange: (AutoDjSettings) -> Unit,
    onTriggerDjTransition: (DjTransitionCurve, Int) -> Unit,
    onCancelDjTransition: () -> Unit,
    onHarmonicSortQueue: () -> Unit,
    audioHapticSettings: AudioHapticSettings,
    latestHapticPulse: HapticPulseEvent?,
    onAudioHapticSettingsChange: (AudioHapticSettings) -> Unit,
    onTestHapticPulse: (HapticPrimitiveType) -> Unit,
    dacTelemetry: DacTelemetryState,
    hiResSettings: HiResLosslessSettings,
    onHiResSettingsChange: (HiResLosslessSettings) -> Unit,
    onSimulateDac: (DacHardwareProfile) -> Unit,
    onResetDac: () -> Unit,
    spatialSettings: SpatialChamberSettings,
    spatialTelemetry: SpatialAcousticTelemetry,
    onSpatialSettingsChange: (SpatialChamberSettings) -> Unit,
    onApplySpatialPreset: (AcousticChamberPreset) -> Unit,
    onUpdateSpatialSpeakerPosition: (Float, Float, Float) -> Unit,
    cassetteSettings: CassetteTapeSettings,
    cassetteTelemetry: TapeMagneticsTelemetry,
    onCassetteSettingsChange: (CassetteTapeSettings) -> Unit,
    onSetCassetteTransportState: (TapeTransportState) -> Unit,
    stemsSettings: StemsIsolatorSettings,
    stemsTelemetry: StemsIsolatorTelemetry,
    onStemsSettingsChange: (StemsIsolatorSettings) -> Unit,
    onSetStemGain: (StemType, Float) -> Unit,
    onToggleStemMute: (StemType) -> Unit,
    onToggleStemSolo: (StemType) -> Unit,
    onSetStemPan: (StemType, Float) -> Unit,
    onSetStemEq: (StemType, Float, Float, Float) -> Unit,
    onSetStemFilter: (StemType, Float) -> Unit,
    onSetStemsCrossfaderPosition: (Float) -> Unit,
    onApplyStemPreset: (StemPreset) -> Unit,
    oscilloscopeSettings: OscilloscopeSettings,
    oscilloscopeTelemetry: OscilloscopeTelemetry,
    oscilloscopeEngine: OscilloscopePhosphorEngine,
    onOscilloscopeSettingsChange: (OscilloscopeSettings) -> Unit,
    onSetOscilloscopeDisplayMode: (OscilloscopeDisplayMode) -> Unit,
    onSetPhosphorType: (CrtPhosphorType) -> Unit,
    onSetOscilloscopePersistence: (CrtBeamPersistence) -> Unit,
    onSetOscilloscopeSensitivity: (Float) -> Unit,
    onUpdateOscilloscope3DRotation: (Float, Float) -> Unit,
    onSetOscilloscopePhaseRotation: (Float) -> Unit,
    globalEqualizer: EqualizerProfile,
    trackEqualizer: EqualizerProfile?,
    globalMastering: MasteringProfile,
    trackMastering: MasteringProfile?,
    useMasteringRack: Boolean,
    equalizerRuntimeState: EqualizerRuntimeState,
    onToggleMasteringRack: (Boolean) -> Unit,
    onGlobalEqualizerChange: (EqualizerProfile) -> Unit,
    onTrackEqualizerChange: (EqualizerProfile?) -> Unit,
    onGlobalMasteringChange: (MasteringProfile) -> Unit,
    onTrackMasteringChange: (MasteringProfile?) -> Unit,
    snapshot: LibrarySnapshot?,
    onShowDoc: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AudiophileCabinetsHeader(
            expandedCabinets = expandedCabinets,
            onToggleCabinet = onToggleCabinet,
            onExpandAllCabinets = onExpandAllCabinets,
            onCollapseAllCabinets = onCollapseAllCabinets
        )
        // Render all cabinets in a simple column if invoked outside LazyColumn
        val currentKey = remember(track) { HarmonicKeyEngine.estimateTrackTelemetry(track).key }
        val targetKey = remember(nextTrack) { nextTrack?.let { HarmonicKeyEngine.estimateTrackTelemetry(it).key } }
        val trackTelemetry = remember(track) { HarmonicKeyEngine.estimateTrackTelemetry(track) }

        // Cabinet 1
        AudiophileCabinetCard(
            title = "HARMONIC DJ FLOW & BEAT SLICER",
            statusText = "Key ${currentKey ?: "8A"} • BPM ${trackTelemetry.bpm.toInt()}",
            glyph = TransportGlyphType.FlowShuffle,
            accentColor = MaterialTheme.colorScheme.primary,
            isExpanded = expandedCabinets.contains("Harmonic"),
            onToggleExpand = { onToggleCabinet("Harmonic") },
            docTopicId = "auto_dj",
            onShowDoc = onShowDoc
        ) {
            val haptic = LocalHapticFeedback.current
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HarmonicFlowHud(
                    currentTrack = track,
                    nextTrack = nextTrack,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    activeProfile = HarmonicFlowCoordinator.activeProfile.value,
                    onTriggerMix = { onTriggerDjTransition(DjTransitionCurve.EqualPower, 8) },
                    onOpenDjConsole = {},
                    modifier = Modifier.fillMaxWidth()
                )
                TransientBeatGridSlicer(
                    bpm = trackTelemetry.bpm.toInt(),
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth()
                )
                MasteringWaveformVisualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performConfirm()
                            onTriggerDjTransition(DjTransitionCurve.EqualPower, 8)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Trigger Auto-DJ Mix")
                    }
                    OutlinedButton(
                        onClick = {
                            haptic.performConfirm()
                            onHarmonicSortQueue()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sort Queue Harmonics")
                    }
                }
            }
        }

        // Cabinet 2
        AudiophileCabinetCard(
            title = "ANALOG VINYL TURNTABLE",
            statusText = if (isPlaying) "Platter Spinning • ${if (playbackSpeed > 1.1f) "45 RPM" else "33⅓ RPM"}" else "Platter Stopped",
            glyph = TransportGlyphType.VinylDisc,
            accentColor = Color(0xFFE5A93C),
            isExpanded = expandedCabinets.contains("Turntable"),
            onToggleExpand = { onToggleCabinet("Turntable") },
            docTopicId = "turntable",
            onShowDoc = onShowDoc
        ) {
            TurntableDeckView(
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                trackTitle = track.title,
                artistName = track.artist,
                artworkUrl = track.artworkUrl,
                onSeek = onSeek,
                playbackSpeed = playbackSpeed,
                onSpeedChange = onSpeedChange,
                isTapeSaturationEnabled = isTapeSaturationEnabled,
                onToggleTapeSaturation = onToggleTapeSaturation,
                onNeedleDrop = onNeedleDropHaptic
            )
        }

        // Cabinet 3
        AudiophileCabinetCard(
            title = "4-STEM ISOLATOR & DJ MIXER",
            statusText = "${stemsSettings.channels.count { !it.value.isMuted }} of 4 Stems Active",
            glyph = TransportGlyphType.Settings,
            accentColor = Color(0xFF4CAF50),
            isExpanded = expandedCabinets.contains("Stems"),
            onToggleExpand = { onToggleCabinet("Stems") },
            docTopicId = "stems_isolator",
            onShowDoc = onShowDoc
        ) {
            StemsDjMixingRack(
                settings = stemsSettings,
                telemetry = stemsTelemetry,
                trackTitle = track.title,
                trackArtist = track.artist,
                onUpdateSettings = onStemsSettingsChange,
                onSetStemGain = onSetStemGain,
                onToggleMute = onToggleStemMute,
                onToggleSolo = onToggleStemSolo,
                onSetStemPan = onSetStemPan,
                onSetStemEq = onSetStemEq,
                onSetStemFilter = onSetStemFilter,
                onSetCrossfaderPosition = onSetStemsCrossfaderPosition,
                onApplyPreset = onApplyStemPreset,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Cabinet 4
        AudiophileCabinetCard(
            title = "STUDIO MASTERING & PARAMETRIC EQ",
            statusText = if (useMasteringRack) "Mastering DSP Rack (${trackMastering?.preset?.displayName ?: globalMastering.preset.displayName})" else "5-Band EQ (${globalEqualizer.preset.displayName})",
            glyph = TransportGlyphType.Sliders,
            accentColor = Color(0xFFAB47BC),
            isExpanded = expandedCabinets.contains("Mastering"),
            onToggleExpand = { onToggleCabinet("Mastering") },
            docTopicId = "mastering_rack",
            onShowDoc = onShowDoc
        ) {
            EqualizerPanel(
                track = track,
                globalEqualizer = globalEqualizer,
                trackEqualizer = trackEqualizer,
                globalMastering = globalMastering,
                trackMastering = trackMastering,
                useMasteringRack = useMasteringRack,
                isPlaying = isPlaying,
                runtimeState = equalizerRuntimeState,
                onToggleMasteringRack = onToggleMasteringRack,
                onGlobalEqualizerChange = onGlobalEqualizerChange,
                onTrackEqualizerChange = onTrackEqualizerChange,
                onGlobalMasteringChange = onGlobalMasteringChange,
                onTrackMasteringChange = onTrackMasteringChange
            )
        }

        // Cabinet 5
        AudiophileCabinetCard(
            title = "MAGNETIC CASSETTE TAPE DECK",
            statusText = if (isTapeSaturationEnabled) "Tape Saturation Warmth ON" else "Type II Chrome • 15 ips",
            glyph = TransportGlyphType.Equalizer,
            accentColor = Color(0xFFFF7043),
            isExpanded = expandedCabinets.contains("Tape"),
            onToggleExpand = { onToggleCabinet("Tape") },
            docTopicId = "cassette_deck",
            onShowDoc = onShowDoc
        ) {
            CassetteTapeDeckView(
                settings = cassetteSettings,
                telemetry = cassetteTelemetry,
                trackTitle = track.title,
                trackArtist = track.artist,
                playbackPositionMs = positionMs,
                trackDurationMs = durationMs,
                onUpdateSettings = onCassetteSettingsChange,
                onSetTransportState = onSetCassetteTransportState,
                onTogglePlayPause = onPlayPause,
                onSeekRewind = { onSeek((positionMs - 15000L).coerceAtLeast(0L)) },
                onSeekFastForward = { onSeek((positionMs + 15000L).coerceAtMost(durationMs)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Cabinet 6
        AudiophileCabinetCard(
            title = "3D SPATIAL CHAMBER & SOUNDSTAGE",
            statusText = "Preset: ${spatialSettings.preset.title} • Binaural",
            glyph = TransportGlyphType.OmniSource,
            accentColor = Color(0xFF26C6DA),
            isExpanded = expandedCabinets.contains("Spatial"),
            onToggleExpand = { onToggleCabinet("Spatial") },
            docTopicId = "spatial_chamber",
            onShowDoc = onShowDoc
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TactileSoundstageDeckView(
                    track = track,
                    modifier = Modifier.fillMaxWidth()
                )
                SpatialAcousticChamberRack(
                    settings = spatialSettings,
                    telemetry = spatialTelemetry,
                    onUpdateSettings = onSpatialSettingsChange,
                    onApplyPreset = onApplySpatialPreset,
                    onUpdateSpeakerPosition = onUpdateSpatialSpeakerPosition,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Cabinet 7
        AudiophileCabinetCard(
            title = "CRT PHOSPHOR OSCILLOSCOPE & SCOPE",
            statusText = "Mode: ${oscilloscopeSettings.displayMode.name} • Phosphor: ${oscilloscopeSettings.phosphorType.title}",
            glyph = TransportGlyphType.WaveformBars,
            accentColor = Color(0xFF00E676),
            isExpanded = expandedCabinets.contains("Scope"),
            onToggleExpand = { onToggleCabinet("Scope") },
            docTopicId = "oscilloscope",
            onShowDoc = onShowDoc
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AudioVisualizerScope(
                    isPlaying = isPlaying,
                    currentKey = currentKey,
                    targetKey = targetKey,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
                OscilloscopePhosphorLabView(
                    settings = oscilloscopeSettings,
                    telemetry = oscilloscopeTelemetry,
                    engine = oscilloscopeEngine,
                    onUpdateSettings = onOscilloscopeSettingsChange,
                    onSetDisplayMode = onSetOscilloscopeDisplayMode,
                    onSetPhosphorType = onSetPhosphorType,
                    onSetPersistence = onSetOscilloscopePersistence,
                    onSetSensitivity = onSetOscilloscopeSensitivity,
                    onUpdate3DRotation = onUpdateOscilloscope3DRotation,
                    onSetPhaseRotation = onSetOscilloscopePhaseRotation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Cabinet 8
        AudiophileCabinetCard(
            title = "BIT-PERFECT DAC & SIGNAL CHAIN",
            statusText = if (track.lossless) "Lossless • ${track.codec.ifBlank { track.format }.uppercase()}" else "Standard Audio",
            glyph = TransportGlyphType.DiamondLossless,
            accentColor = Color(0xFFE5A93C),
            isExpanded = expandedCabinets.contains("Hi-Res"),
            onToggleExpand = { onToggleCabinet("Hi-Res") },
            docTopicId = "hires_lossless",
            onShowDoc = onShowDoc
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PureSignalChainHud(
                    track = track,
                    activeOutputDevice = audioRouteState.currentRoute.label,
                    modifier = Modifier.fillMaxWidth()
                )
                HiResLosslessHorizonCard(
                    telemetry = dacTelemetry,
                    settings = hiResSettings,
                    onSettingsChange = onHiResSettingsChange,
                    onSimulateDac = onSimulateDac,
                    onResetDac = onResetDac,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Cabinet 9
        AudiophileCabinetCard(
            title = "VINYL CRATE & EXTENDED METADATA",
            statusText = "${snapshot?.tracks?.size ?: 1} Tracks in Vault",
            glyph = TransportGlyphType.Folder,
            accentColor = Color(0xFF78909C),
            isExpanded = expandedCabinets.contains("Vinyl-Vault"),
            onToggleExpand = { onToggleCabinet("Vinyl-Vault") },
            docTopicId = "vinyl_vault",
            onShowDoc = onShowDoc
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetadataPanel(track = track)
                val tracksForVault = remember(snapshot, track) {
                    val list = snapshot?.tracks.orEmpty()
                    if (list.isNotEmpty()) list else listOfNotNull(track)
                }
                FlippableCrateView(
                    tracks = tracksForVault,
                    activeTrack = track,
                    onSelectTrack = { onSeek(0L) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Cabinet 10
        AudiophileCabinetCard(
            title = "TACTILE AUDIO HAPTICS LAB",
            statusText = if (audioHapticSettings.isEnabled) "Transducer Active" else "Haptics Disabled",
            glyph = TransportGlyphType.LightningCheck,
            accentColor = Color(0xFFFFCA28),
            isExpanded = expandedCabinets.contains("Haptics"),
            onToggleExpand = { onToggleCabinet("Haptics") },
            docTopicId = "audio_haptics",
            onShowDoc = onShowDoc
        ) {
            AudioHapticControlRack(
                settings = audioHapticSettings,
                latestPulse = latestHapticPulse,
                onSettingsChange = onAudioHapticSettingsChange,
                onTestPulse = onTestHapticPulse,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

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
    onCollapse: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .playerSheetDrag(upward = false, onComplete = onCollapse)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Dedicated utility row: Never crowds or skews, optimal vertical breathing space
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExperienceModeQuickChip(
                mode = experienceMode,
                onToggle = { onExperienceModeChange(experienceMode.toggle()) }
            )

            // Source status indicator pill preserving PixelodyStateTags.PLAYER_SOURCE
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
            ) {
                Row(
                    modifier = Modifier
                        .testTag(PixelodyStateTags.PLAYER_SOURCE)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (sourceStatus.contains("Android")) "Device" else if (sourceStatus.contains("host")) "Host" else "Source",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (experienceMode == AppExperienceMode.Studio) {
                Surface(
                    onClick = {
                        haptic.performTick()
                        onOpenPlayerViews()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = activeViewMode.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (onOpenLyrics != null) {
                val isLyrics = activeViewMode == "Lyrics"
                Surface(
                    onClick = {
                        haptic.performTick()
                        onOpenLyrics()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLyrics) Color(0xFF6C9EFF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (isLyrics) Color(0xFF6C9EFF).copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Lyrics",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isLyrics) Color(0xFF6C9EFF) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (onOpenPitchAndSpeed != null) {
                val speedActive = playbackSpeed != 1.0f || !pitchLocked
                val speedLabel = String.format(java.util.Locale.US, "%.2f", playbackSpeed).trimEnd('0').trimEnd('.')
                Surface(
                    onClick = {
                        haptic.performTick()
                        onOpenPitchAndSpeed()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (speedActive) Color(0xFF6C9EFF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (speedActive) Color(0xFF6C9EFF).copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Speed ${speedLabel}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (speedActive) Color(0xFF6C9EFF) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (onOpenSleepTimer != null) {
                Surface(
                    onClick = {
                        haptic.performTick()
                        onOpenSleepTimer()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (sleepTimerActive) Color(0xFFE5A93C).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (sleepTimerActive) Color(0xFFE5A93C).copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (sleepTimerActive) "Timer $sleepTimerRemaining" else "Timer",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (sleepTimerActive) Color(0xFFE5A93C) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Minimize / Close button
        Surface(
            onClick = {
                haptic.performTick()
                onCollapse()
            },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                PixelodyTransportGlyph(
                    glyph = TransportGlyphType.ChevronDown,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 12.dp
                )
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    modifier: Modifier = Modifier.padding(horizontal = 16.dp)
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


            // Track Identity & Format Information
            NowPlayingTrackIdentity(
                track = track,
                isPlaying = isPlaying,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = onAddToPlaylist,
                onTrackActions = onTrackActions,
                modifier = Modifier.fillMaxWidth()
            )

            // Velocity Scrubber Progress
            NowPlayingProgress(
                positionMs = positionMs,
                durationMs = durationMs,
                trackDurationSeconds = track.durationSeconds,
                onSeek = onSeek,
                positionMsProvider = positionMsProvider
            )

            // Primary Tactile Transport Controls (Prev, Play/Pause, Next)
            NowPlayingTransportControls(
                track = track,
                isPlaying = isPlaying,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext
            )

            // Core Playback Mode Chips & Audio Route Output Pill
            NowPlayingModeChips(
                shuffleEnabled = shuffleEnabled,
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onOpenQueue = onOpenQueue,
                audioRouteState = audioRouteState,
                onCycleAudioRoute = onCycleAudioRoute,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
}

@Composable
internal fun MasteringWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.secondary
) {
    if (!isPlaying) {
        Canvas(modifier = modifier.fillMaxWidth().height(42.dp)) {
            val width = size.width
            val height = size.height
            val barCount = 22
            val barSpacing = 3.dp.toPx()
            val totalSpacing = (barCount - 1) * barSpacing
            val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2f)

            for (i in 0 until barCount) {
                val x = i * (barWidth + barSpacing)
                val barHeight = height * 0.08f
                val y = height - barHeight
                val color = if (i % 2 == 0) barColor else accentColor
                drawRoundRect(
                    color = color.copy(alpha = 0.22f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "masteringVisualizer")
    val phase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "phase"
    )
    val pulse = infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(42.dp)) {
        val width = size.width
        val height = size.height
        val barCount = 22
        val barSpacing = 3.dp.toPx()
        val totalSpacing = (barCount - 1) * barSpacing
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2f)
        val currentPhase = phase.value
        val currentPulse = pulse.value

        // Draw frequency bars
        for (i in 0 until barCount) {
            val normalizedIdx = i.toFloat() / barCount
            val sinWave = kotlin.math.sin(normalizedIdx * 4 * Math.PI + currentPhase).toFloat()
            val cosWave = kotlin.math.cos(normalizedIdx * 2 * Math.PI - currentPhase * 1.4f).toFloat()
            val dynamicHeightFraction = ((sinWave * 0.35f + cosWave * 0.25f + 0.45f) * currentPulse).coerceIn(0.12f, 0.98f)

            val x = i * (barWidth + barSpacing)
            val barHeight = height * dynamicHeightFraction
            val y = height - barHeight

            val color = if (i % 2 == 0) barColor else accentColor
            drawRoundRect(
                color = color.copy(alpha = 0.82f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        // Draw glowing Bezier waveform curve across the center
        val path = Path()
        path.moveTo(0f, height * 0.5f)
        val points = 28
        for (p in 0..points) {
            val px = (p.toFloat() / points) * width
            val py = height * 0.5f + (kotlin.math.sin(p.toFloat() * 0.6f + currentPhase * 2f).toFloat() * height * 0.28f * currentPulse)
            path.lineTo(px, py)
        }
        drawPath(
            path = path,
            color = barColor.copy(alpha = 0.92f),
            style = Stroke(width = 2.dp.toPx())
        )
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
internal fun EqualizerPanel(
    track: Track?,
    globalEqualizer: EqualizerProfile,
    trackEqualizer: EqualizerProfile?,
    globalMastering: MasteringProfile = MasteringProfile(),
    trackMastering: MasteringProfile? = null,
    useMasteringRack: Boolean = false,
    isPlaying: Boolean = false,
    runtimeState: EqualizerRuntimeState,
    onToggleMasteringRack: (Boolean) -> Unit = {},
    onGlobalEqualizerChange: (EqualizerProfile) -> Unit,
    onTrackEqualizerChange: (EqualizerProfile?) -> Unit,
    onGlobalMasteringChange: (MasteringProfile) -> Unit = {},
    onTrackMasteringChange: (MasteringProfile?) -> Unit = {}
) {
    var scope by remember(track?.id, trackEqualizer, trackMastering) {
        mutableStateOf(if (trackEqualizer != null || trackMastering != null) EqualizerEditScope.Track else EqualizerEditScope.Global)
    }
    val editingTrack = scope == EqualizerEditScope.Track && track != null
    val editableProfile = if (editingTrack) {
        trackEqualizer ?: globalEqualizer
    } else {
        globalEqualizer
    }.normalized()

    val editableMasteringProfile = if (editingTrack) {
        trackMastering ?: globalMastering
    } else {
        globalMastering
    }.normalized()

    fun commit(profile: EqualizerProfile) {
        if (editingTrack) {
            onTrackEqualizerChange(profile)
        } else {
            onGlobalEqualizerChange(profile)
        }
    }

    fun commitMastering(profile: MasteringProfile) {
        if (editingTrack) {
            onTrackMasteringChange(profile)
        } else {
            onGlobalMasteringChange(profile)
        }
    }

    val haptic = LocalHapticFeedback.current

    SectionCard(
        title = if (useMasteringRack) "Studio Mastering DSP" else "Equalizer",
        subtitle = listOf(
            if (editingTrack) "This track" else "Global",
            if (useMasteringRack) editableMasteringProfile.preset.displayName else editableProfile.preset.displayName,
            runtimeState.message
        ).joinToString(" / ")
    ) {
        // Mode Selector: 5-Band EQ vs Studio Mastering Rack
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !useMasteringRack,
                onClick = {
                    haptic.performTick()
                    onToggleMasteringRack(false)
                },
                label = { Text("5-Band EQ") }
            )
            FilterChip(
                selected = useMasteringRack,
                onClick = {
                    haptic.performTick()
                    onToggleMasteringRack(true)
                },
                label = { Text("Mastering Rack") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (useMasteringRack) {
            StudioMasteringRack(
                profile = editableMasteringProfile,
                runtimeState = runtimeState,
                isPlaying = isPlaying,
                onProfileChange = { commitMastering(it) }
            )
        } else {
            // Visual Spline Frequency Response Curve
            PixelodyEqualizerCurve(
                gainsDb = editableProfile.gainsDb,
                enabled = editableProfile.enabled,
                onBandGainChange = { bandIndex, gainDb ->
                    commit(editableProfile.withBandGain(bandIndex, gainDb))
                },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = scope == EqualizerEditScope.Global,
                        onClick = {
                            haptic.performTick()
                            scope = EqualizerEditScope.Global
                        },
                        label = { Text("Global") }
                    )
                }
                item {
                    FilterChip(
                        selected = editingTrack,
                        enabled = track != null,
                        onClick = {
                            haptic.performTick()
                            scope = EqualizerEditScope.Track
                        },
                        label = { Text(if (trackEqualizer != null) "Track EQ" else "This Track") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EqualizerPreset.quickPresets) { preset ->
                    FilterChip(
                        selected = editableProfile.preset == preset,
                        onClick = {
                            haptic.performTick()
                            commit(editableProfile.withPreset(preset))
                        },
                        label = { Text(preset.displayName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        haptic.performConfirm()
                        commit(editableProfile.copy(enabled = !editableProfile.enabled))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (editableProfile.enabled) "Bypass EQ" else "Turn EQ On")
                }
                if (editingTrack) {
                    OutlinedButton(
                        onClick = {
                            haptic.performTick()
                            if (trackEqualizer == null) {
                                onTrackEqualizerChange(globalEqualizer)
                            } else {
                                onTrackEqualizerChange(null)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (trackEqualizer == null) "Make Track EQ" else "Use Global")
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            EqualizerPreset.bandLabels.forEachIndexed { index, label ->
                EqualizerBandControl(
                    label = label,
                    gainDb = editableProfile.gainsDb.getOrElse(index) { 0f },
                    onGainChange = { gain -> commit(editableProfile.withBandGain(index, gain)) }
                )
            }
        }
    }
}

@Composable
internal fun EqualizerBandControl(
    label: String,
    gainDb: Float,
    onGainChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = "%+.1f dB".format(gainDb),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Slider(
            value = gainDb,
            onValueChange = onGainChange,
            valueRange = EqualizerProfile.MIN_GAIN_DB..EqualizerProfile.MAX_GAIN_DB,
            steps = 23,
            modifier = Modifier.semantics { contentDescription = "$label equalizer gain" }
        )
    }
}
