package com.pixelody.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import com.pixelody.app.data.model.withCover
import com.pixelody.app.data.model.CoverBook
import com.pixelody.app.data.model.albumCoverKey
import com.pixelody.app.data.model.crateCoverKey
import com.pixelody.app.data.model.collectionCoverKey
import com.pixelody.app.data.model.playlistCoverKey
import com.pixelody.app.data.model.trackCoverKey
import com.pixelody.app.ui.components.CoverCard
import com.pixelody.app.ui.components.CoverPill
import com.pixelody.app.ui.components.CoverSectionTitle
import com.pixelody.app.ui.components.CoverTrackRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.pixelody.app.ui.components.StudioSectionToggle
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.analytics.SessionSoundInsights
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.Crate
import com.pixelody.app.data.model.CrateSlot
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.LiveState
import com.pixelody.app.data.model.SavedHostProfile
import com.pixelody.app.data.model.Track
import com.pixelody.app.feature.connection.HomePairingPanel
import com.pixelody.app.feature.connection.connectionGuidanceFor
import com.pixelody.app.feature.connection.label
import com.pixelody.app.ui.components.CabinetDocButton
import com.pixelody.app.ui.theme.units.HeroStageData
import com.pixelody.app.ui.theme.units.PixelodyHeroStage
import com.pixelody.app.ui.theme.units.PixelodyQuickStartTile
import com.pixelody.app.ui.theme.units.PixelodyTrackItem
import com.pixelody.app.ui.theme.units.QuickTileData
import com.pixelody.app.ui.theme.units.toItemData
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import com.pixelody.app.ui.components.CompactTrackPill
import com.pixelody.app.ui.components.HomeSectionHeader
import com.pixelody.app.ui.components.PageIdentity
import com.pixelody.app.ui.components.PixelodyNavGlyph
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.QuickStartTile
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.SessionCapsuleCard
import com.pixelody.app.ui.components.SmartPocketFilter
import com.pixelody.app.ui.components.SmartPocketRow
import com.pixelody.app.ui.components.SourceScope
import com.pixelody.app.ui.components.StatusChip
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.TriSourcePivotBar
import com.pixelody.app.ui.components.equalizerSummary
import com.pixelody.app.ui.components.filterTracksBySource
import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.ui.components.ExperienceModeToggle
import com.pixelody.app.ui.components.ExperienceModeQuickChip
import com.pixelody.app.core.analytics.HomeContextEngine
import com.pixelody.app.data.storage.LocalHabitStore
import com.pixelody.app.data.storage.HabitActionType
import androidx.compose.ui.platform.LocalContext
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.navigation.PixelodyStateTags
import com.pixelody.app.ui.navigation.PixelodyTab

@Composable
internal fun HomeScreen(
    snapshot: LibrarySnapshot?,
    liveState: LiveState?,
    selectedTrack: Track?,
    localTracks: List<Track>,
    localTrackCount: Int,
    isPlaying: Boolean,
    connectionState: HostConnectionState,
    savedHost: SavedHostProfile?,
    copiedDetails: String,
    qrScannerVisible: Boolean,
    qrScannerError: String,
    isConnecting: Boolean,
    isLoadingMusic: Boolean = false,
    connectionSetupRequested: Boolean = false,
    onConnectionSetupShown: () -> Unit = {},
    error: String,
    onCopiedDetailsChange: (String) -> Unit,
    onStartQrScanner: () -> Unit,
    onStopQrScanner: () -> Unit,
    onQrPayloadScanned: (String) -> Unit,
    onQrScannerError: (String) -> Unit,
    onForgetSavedHost: () -> Unit,
    onUseCopiedDetails: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onTogglePlayback: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenQueue: () -> Unit = {},
    onOpenSearch: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenSharing: () -> Unit,
    equalizerProfile: EqualizerProfile,
    trackHasEqualizerOverride: Boolean,
    onCycleEqualizerPreset: () -> Unit,
    onPairFixture: () -> Unit,
    onOpenFlowCabinet: () -> Unit = {},
    sessionInsights: SessionSoundInsights = SessionSoundInsights(),
    dailyCapsule: DailySonicCapsule? = null,
    crates: List<Crate> = emptyList(),
    covers: CoverBook = CoverBook(),
    onCoverActions: (String, String) -> Unit = { _, _ -> },
    onOpenCrate: (String) -> Unit = {},
    onOpenCollection: (String, String) -> Unit = { _, _ -> },
    onCrateActions: (String) -> Unit = {},
    onPlayBatchTracks: (List<Track>) -> Unit = {},
    onDailySoundCheck: () -> Unit = {},
    onOpenTimeline: () -> Unit = {},
    onLongClickTrack: (Track) -> Unit = {},
    onLongClickScope: (SourceScope) -> Unit = {},
    onLongClickCapsule: () -> Unit = {},
    onLongClickQuickStart: (String) -> Unit = {},
    onConnectDesktop: () -> Unit = {},
    onStartJam: () -> Unit = {},
    onOpenSmartCrateBuilder: () -> Unit = {},
    sourceScope: SourceScope = SourceScope.All,
    onSourceScopeChange: (SourceScope) -> Unit = {},
    onShowDoc: ((String) -> Unit)? = null,
    experienceMode: AppExperienceMode = AppExperienceMode.Essential,
    onExperienceModeChange: (AppExperienceMode) -> Unit = {},
    onPlayTrackWithSmartFlow: (String) -> Unit = {},
    onShuffleAllFlow: () -> Unit = {}
) {
    var manualInviteVisible by remember { mutableStateOf(false) }
    var showConnectionSetup by remember { mutableStateOf(false) }
    LaunchedEffect(connectionSetupRequested) {
        if (connectionSetupRequested) { showConnectionSetup = true; onConnectionSetupShown() }
    }
    var showBrowseControls by rememberSaveable { mutableStateOf(false) }
    var showListeningTools by rememberSaveable { mutableStateOf(false) }
    var smartFilter by remember { mutableStateOf(SmartPocketFilter.All) }
    val hostTracks = snapshot?.tracks.orEmpty()
    val playlists = snapshot?.playlists.orEmpty()
    val localTrackIds = remember(localTracks) { localTracks.map { it.id }.toSet() }
    val jamTrackIds = remember(snapshot) { snapshot?.queue?.trackIds.orEmpty().toSet() }
    val trackById = remember(snapshot, localTracks) { (hostTracks + localTracks).associateBy { it.id } }
    val favoriteTracks = remember(snapshot, trackById) {
        snapshot?.favorites.orEmpty().mapNotNull { trackById[it] }
    }
    val queuedTracks = remember(snapshot, trackById) {
        snapshot?.queue?.trackIds.orEmpty().mapNotNull { trackById[it] }
    }
    val scopedHostTracks = remember(hostTracks, sourceScope, localTrackIds, jamTrackIds) {
        filterTracksBySource(hostTracks, sourceScope, localTrackIds, jamTrackIds)
    }
    val scopedLocalTracks = remember(localTracks, sourceScope, localTrackIds, jamTrackIds) {
        filterTracksBySource(localTracks, sourceScope, localTrackIds, jamTrackIds)
    }
    val playablePool = remember(scopedHostTracks, scopedLocalTracks) {
        (scopedHostTracks + scopedLocalTracks).filterNot { it.missing || it.streamUrl.isBlank() }
    }
    val playableHostTracks = remember(scopedHostTracks) {
        scopedHostTracks.filterNot { it.missing || it.streamUrl.isBlank() }
    }
    val losslessTracks = remember(playablePool) {
        playablePool.filter { it.lossless }.take(12)
    }
    val albumGroups = remember(playablePool) {
        playablePool
            .filter { it.album.isNotBlank() }
            .groupBy { it.album }
            .toList()
            .sortedWith(compareByDescending<Pair<String, List<Track>>> { it.second.size }.thenBy { it.first })
            .take(10)
    }
    val artistGroups = remember(playablePool) {
        playablePool
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist }
            .toList()
            .sortedWith(compareByDescending<Pair<String, List<Track>>> { it.second.size }.thenBy { it.first })
            .take(10)
    }
    val recentListenIds = dailyCapsule?.memoryTimeline.orEmpty().sortedByDescending { it.timestampMs }.map { it.trackId }.distinct()
    val jumpBackTracks = remember(selectedTrack, queuedTracks, favoriteTracks, scopedLocalTracks, scopedHostTracks, recentListenIds) {
        (listOfNotNull(selectedTrack) + recentListenIds.mapNotNull { trackById[it] } + queuedTracks + favoriteTracks + scopedLocalTracks + scopedHostTracks)
            .distinctBy { it.id }
            .take(10)
    }
    val standoutTracks = remember(favoriteTracks, scopedHostTracks) {
        (favoriteTracks + scopedHostTracks.sortedByDescending { it.durationSeconds })
            .distinctBy { it.id }
            .take(8)
    }
    val recentTracks = remember(scopedHostTracks, scopedLocalTracks, queuedTracks) {
        (queuedTracks + scopedHostTracks + scopedLocalTracks)
            .distinctBy { it.id }
            .drop(if (selectedTrack == null) 0 else 1)
            .take(12)
    }

    val curatedCrates = remember(losslessTracks, standoutTracks, recentTracks, playablePool, albumGroups) {
        listOfNotNull(
            if (losslessTracks.isNotEmpty()) {
                CuratedSmartCrate(
                    id = "crate-hires-24bit",
                    title = "24-Bit FLAC Master",
                    subtitle = "Bit-perfect lossless stream",
                    tag = "HI-RES",
                    glyphType = TransportGlyphType.DiamondLossless,
                    tracks = losslessTracks,
                    moodColorHex = 0xFFE5A93C
                )
            } else null,
            if (albumGroups.isNotEmpty()) {
                val vinylTracks = albumGroups.flatMap { it.second }.take(9)
                CuratedSmartCrate(
                    id = "crate-vinyl-warmth",
                    title = "Vinyl Analog Lounge",
                    subtitle = "Organic dynamics & warm mastering",
                    tag = "VINYL",
                    glyphType = TransportGlyphType.VinylDisc,
                    tracks = vinylTracks,
                    moodColorHex = 0xFFEC4899
                )
            } else null,
            if (standoutTracks.isNotEmpty() || recentTracks.isNotEmpty()) {
                CuratedSmartCrate(
                    id = "crate-heavy-rotation",
                    title = "Heavy Rotation Digs",
                    subtitle = "High rotation session favorites",
                    tag = "ROTN",
                    glyphType = TransportGlyphType.OmniSource,
                    tracks = (standoutTracks + recentTracks).distinctBy { it.id }.take(9),
                    moodColorHex = 0xFF10B981
                )
            } else null
        )
    }

    val context = LocalContext.current
    val habitStore = remember(context) { LocalHabitStore(context) }
    val contextEngine = remember(habitStore) { HomeContextEngine(habitStore) }
    val surfaceState = remember(
        isPlaying,
        selectedTrack,
        queuedTracks,
        playablePool,
        favoriteTracks,
        connectionState,
        dailyCapsule,
        sessionInsights
    ) {
        contextEngine.evaluate(
            isPlaying = isPlaying,
            currentTrack = selectedTrack,
            nextTrack = queuedTracks.firstOrNull(),
            tracks = playablePool,
            favoritesCount = favoriteTracks.size,
            queueCount = queuedTracks.size,
            isOffline = connectionState == HostConnectionState.Disconnected && localTracks.isNotEmpty(),
            isHostConnected = connectionState == HostConnectionState.Connected,
            hostName = snapshot?.host?.hostName ?: "Studio Workstation",
            dailyCapsule = dailyCapsule,
            sessionInsights = sessionInsights
        )
    }

    val displayedPlaylists = remember(playlists) { playlists.take(10) }
    val startState = homeStartState(playablePool.size, (hostTracks + localTracks).count { !it.missing && it.streamUrl.isNotBlank() }, isLoadingMusic, isConnecting)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp)
    ) {

        item(key = "hero_header", contentType = "hero_header") {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                HomeHeroHeader(
                    greetingTitle = surfaceState.greetingTitle,
                    greetingSubtitle = surfaceState.greetingSubtitle,
                    hostName = snapshot?.host?.hostName,
                    isPlaying = isPlaying,
                    connectionState = connectionState,
                    hostTrackCount = hostTracks.size,
                    localTrackCount = localTrackCount,
                    experienceMode = experienceMode,
                    onExperienceModeChange = onExperienceModeChange,
                    onOpenProfile = onOpenProfile
                )
            }
        }
        item(key = "home_access") {
            HomeAccessPanel(
                mode = experienceMode, onModeChange = onExperienceModeChange,
                onOpenFavorites = { onOpenCollection("favorites", "") },
                onOpenRecent = { onOpenCollection("recent", "") },
                onOpenLossless = { onOpenCollection("lossless", "") },
                onOpenPlaylists = { onOpenCollection("playlists", "") },
                onAddMusic = onOpenDevice,
                onConnectDesktop = { showConnectionSetup = !showConnectionSetup },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (startState != HomeStartState.Ready) {
            item(key = "music_setup") {
                HomeMusicSetupCard(state = startState,
                    onAddMusic = onOpenDevice,
                    onShowAll = { onSourceScopeChange(SourceScope.All) },
                    onConnectDesktop = { showConnectionSetup = !showConnectionSetup },
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        if (showConnectionSetup || qrScannerVisible || isConnecting || error.isNotBlank()) {
        item(key = "pairing_panel", contentType = "pairing_panel") {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                HomePairingPanel(
                    snapshot = snapshot,
                    savedHost = savedHost,
                    liveState = liveState,
                    copiedDetails = copiedDetails,
                    qrScannerVisible = qrScannerVisible,
                    qrScannerError = qrScannerError,
                    isConnecting = isConnecting,
                    manualInviteVisible = manualInviteVisible,
                    onManualInviteVisibleChange = { manualInviteVisible = it },
                    onCopiedDetailsChange = onCopiedDetailsChange,
                    onStartQrScanner = onStartQrScanner,
                    onStopQrScanner = onStopQrScanner,
                    onQrPayloadScanned = onQrPayloadScanned,
                    onQrScannerError = onQrScannerError,
                    onForgetSavedHost = onForgetSavedHost,
                    onUseCopiedDetails = onUseCopiedDetails,
                    onPairFixture = onPairFixture,
                    onOpenSharing = onOpenSharing
                )
            }
        }
        }
        if (startState == HomeStartState.Ready) item(key = "flow_settings") {
            TextButton(onClick = onOpenFlowCabinet,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 48.dp)) {
                Text("Flow & shuffle")
            }
        }
        if (experienceMode == AppExperienceMode.Essential && startState == HomeStartState.Ready) {
            item(key = "essential_play_row", contentType = "essential_play_row") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShuffleAllFlow,
                        enabled = playablePool.isNotEmpty(),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("Shuffle all")
                    }
                    FilledTonalButton(
                        onClick = {
                            when (homePlaybackAction(isPlaying, selectedTrack != null, playablePool.isNotEmpty())) {
                                HomePlaybackAction.Toggle -> onTogglePlayback()
                                HomePlaybackAction.Start -> playablePool.firstOrNull()?.let { onPlayTrackWithSmartFlow(it.id) }
                                HomePlaybackAction.Unavailable -> Unit
                            }
                        },
                        enabled = isPlaying || selectedTrack != null || playablePool.isNotEmpty(),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(homePlaybackLabel(isPlaying, selectedTrack != null))
                    }
                }
            }
            item(key = "essential_search", contentType = "search_prompt") {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HomeSearchPrompt(onClick = onOpenSearch)
                }
            }
            if (playlists.isNotEmpty()) {
                item(key = "essential_playlists", contentType = "cover_shelf") {
                    CoverShelf(title = "Your playlists") {
                        items(displayedPlaylists, key = { "playlist_${it.id}" }) { playlist ->
                            val key = collectionCoverKey(playlist.id)
                            val firstTrack = playlist.trackIds.firstOrNull()?.let { trackById[it] }
                            CoverCard(
                                title = playlist.name,
                                caption = "${playlist.trackIds.size} songs",
                                note = covers.noteFor(key),
                                artworkUrl = covers.imageFor(key, playlist.artworkUrl ?: firstTrack?.artworkUrl),
                                onClick = { onOpenCollection("playlist", playlist.id) },
                                onLongClick = { onCoverActions(key, playlist.name) }
                            )
                        }
                    }
                }
            }
            if (albumGroups.isNotEmpty()) {
                item(key = "essential_albums", contentType = "cover_shelf") {
                    CoverShelf(title = "Albums") {
                        items(albumGroups, key = { "album_${it.first}" }) { (album, albumTracks) ->
                            val key = albumCoverKey(album)
                            CoverCard(
                                title = album,
                                caption = "${albumTracks.size} tracks",
                                note = covers.noteFor(key),
                                artworkUrl = covers.imageFor(key, albumTracks.firstOrNull()?.artworkUrl),
                                onClick = { onOpenCollection("album", "album:$album") },
                                onLongClick = { onCoverActions(key, album) }
                            )
                        }
                    }
                }
            }
            if (recentTracks.isNotEmpty()) {
                item(key = "essential_jump_back_in", contentType = "cover_shelf") {
                    CoverShelf(title = "Jump back in") {
                        items(recentTracks.take(12), key = { "recent_${it.id}" }) { track ->
                            val key = trackCoverKey(track.id)
                            CoverCard(
                                title = track.title,
                                caption = track.artist,
                                note = covers.noteFor(key),
                                artworkUrl = covers.imageFor(key, track.artworkUrl),
                                onClick = { onPlayTrackWithSmartFlow(track.id) },
                                onLongClick = { onLongClickTrack(track) }
                            )
                        }
                    }
                }
            }
            val recommendedTracks = (standoutTracks + recentTracks + playablePool).distinctBy { it.id }.take(6)
            if (recommendedTracks.isNotEmpty()) {
                item(key = "essential_tracks_header", contentType = "tracks_header") {
                    CoverSectionTitle(
                        title = "From your library",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(recommendedTracks, key = { "essential_track_${it.id}" }, contentType = { "essential_track_row" }) { track ->
                    val key = trackCoverKey(track.id)
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        CoverTrackRow(
                            title = track.title,
                            artist = track.artist,
                            note = covers.noteFor(key),
                            artworkUrl = covers.imageFor(key, track.artworkUrl),
                            selected = selectedTrack?.id == track.id,
                            onClick = { onPlayTrackWithSmartFlow(track.id) },
                            onLongClick = { onLongClickTrack(track) }
                        )
                    }
                }
            }
        } else if (startState == HomeStartState.Ready) {
        item(key = "studio_listening_lead") {
            StudioListeningLead(track = (selectedTrack ?: playablePool.firstOrNull())?.withCover(covers), isPlaying = isPlaying,
                onPlayPause = { if (selectedTrack != null) onTogglePlayback() else playablePool.firstOrNull()?.let(onPlayTrack) },
                onOpenPlayer = onOpenPlayer, onOpenQueue = onOpenQueue, canOpenPlayer = selectedTrack != null,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        item(key = "studio_browse_controls") {
            StudioSectionToggle("Browse controls", if (smartFilter == SmartPocketFilter.All) sourceScope.label else "${sourceScope.label} · Picks: ${smartFilter.label}", showBrowseControls,
                { showBrowseControls = !showBrowseControls }, "studio:home-browse-controls",
                Modifier.padding(horizontal = 16.dp))
        }
        if (showBrowseControls) {
        item(key = "pivot_bar", contentType = "pivot_bar") {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                TriSourcePivotBar(
                    selectedScope = sourceScope,
                    onSelectScope = { scope ->
                        when (scope) {
                            SourceScope.All -> habitStore.recordAction(HabitActionType.SelectLocalPhoneSource)
                            SourceScope.LocalPhone -> habitStore.recordAction(HabitActionType.SelectLocalPhoneSource)
                            SourceScope.DesktopHost -> habitStore.recordAction(HabitActionType.SelectDesktopHostSource)
                            SourceScope.JamMesh -> habitStore.recordAction(HabitActionType.SelectJamMeshSource)
                        }
                        onSourceScopeChange(scope)
                    },
                    localCount = localTracks.size,
                    hostConnected = connectionState == HostConnectionState.Connected,
                    hostCount = hostTracks.size,
                    jamActive = liveState != null,
                    jamCount = queuedTracks.size,
                    onLongClickScope = onLongClickScope,
                    onConnectDesktop = { showConnectionSetup = true },
                    onStartJam = onStartJam
                )
            }
        }
        item(key = "smart_pocket", contentType = "smart_pocket") {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SmartPocketRow(
                    selectedFilter = smartFilter,
                    onSelectFilter = { filter ->
                        when (filter) {
                            SmartPocketFilter.HiRes -> habitStore.recordAction(HabitActionType.HiResFilter)
                            SmartPocketFilter.HeavyRotation -> habitStore.recordAction(HabitActionType.HeavyRotation)
                            else -> {}
                        }
                        smartFilter = filter
                    }
                )
            }
        }
        item(key = "quick_start", contentType = "quick_start") {
            val displayedPlayable = when (smartFilter) {
                SmartPocketFilter.HiRes -> losslessTracks
                SmartPocketFilter.HeavyRotation -> standoutTracks
                SmartPocketFilter.Recent -> recentTracks
                else -> playablePool
            }
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                HomeQuickStartShelf(
                    quickPaths = surfaceState.quickPaths,
                    playableTracks = displayedPlayable,
                    favoriteTracks = favoriteTracks,
                    queuedTracks = queuedTracks,
                    losslessTracks = losslessTracks,
                    localTracks = localTracks,
                    onPlayTrack = onPlayTrack,
                    onOpenLibrary = onOpenLibrary,
                    onOpenCollection = onOpenCollection,
                    onOpenQueue = onOpenQueue,
                    onOpenDevice = onOpenDevice,
                    onOpenCreate = onOpenCreate,
                    onRecordAction = { habitStore.recordAction(it) },
                    onLongClickCategory = onLongClickQuickStart
                )
            }
        }
        }

        item(key = "studio_listening_tools") {
            StudioSectionToggle("Listening tools", "Flow, history and setup", showListeningTools,
                { showListeningTools = !showListeningTools }, "studio:home-listening-tools",
                Modifier.padding(horizontal = 16.dp))
        }
        if (showListeningTools) {
        if (surfaceState.showHardwareBanner && surfaceState.hardwareAnchor != null && experienceMode == AppExperienceMode.Studio) {
            item(key = "hardware_banner", contentType = "hardware_banner") {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HardwarePresencePill(
                        state = surfaceState.hardwareAnchor,
                        onClick = onOpenProfile
                    )
                }
            }
        }
        item(key = "contextual_horizon", contentType = "contextual_horizon") {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ContextualHorizonHero(
                    surfaceState = surfaceState,
                    onLaunchHero = {
                        habitStore.recordAction(HabitActionType.PlaySomething)
                        playablePool.firstOrNull()?.let(onPlayTrack)
                    },
                    onOpenTimeline = onOpenTimeline,
                    onOpenPlayer = onOpenPlayer,
                    onOpenQueue = onOpenQueue,
                    onOpenTurntable = onOpenPlayer,
                    onCycleEqualizer = onCycleEqualizerPreset,
                    onShowDoc = onShowDoc
                )
            }
        }

        if (losslessTracks.isNotEmpty() || localTracks.isNotEmpty() || playableHostTracks.isNotEmpty()) {
            item(key = "sources_quality", contentType = "sources_quality") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HomeSectionHeader(title = "Sources And Quality", subtitle = "Local-first choices without the debug wall.")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(contentType = "source_tile") {
                            SourceChoiceTile(
                                title = "Lossless",
                                subtitle = "${losslessTracks.size} tracks",
                                enabled = losslessTracks.isNotEmpty(),
                                onClick = { losslessTracks.firstOrNull()?.let(onPlayTrack) }
                            )
                        }
                        item(contentType = "source_tile") {
                            SourceChoiceTile(
                                title = "Host",
                                subtitle = "${playableHostTracks.size} playable",
                                enabled = playableHostTracks.isNotEmpty(),
                                onClick = { playableHostTracks.firstOrNull()?.let(onPlayTrack) }
                            )
                        }
                        item(contentType = "source_tile") {
                            SourceChoiceTile(
                                title = "Phone",
                                subtitle = "${localTracks.size} files",
                                enabled = localTracks.isNotEmpty(),
                                onClick = { localTracks.firstOrNull()?.let(onPlayTrack) }
                            )
                        }
                        item(contentType = "source_tile") {
                            SourceChoiceTile(
                                title = "Add",
                                subtitle = "files or host",
                                enabled = true,
                                onClick = onOpenCreate
                            )
                        }
                    }
                }
            }
        }
        if (curatedCrates.isNotEmpty()) {
            item(key = "discovery_crates", contentType = "discovery_crates") {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    DiscoveryCrateShelf(
                        curatedCrates = curatedCrates,
                        userCrates = emptyList(),
                        trackById = trackById,
                        onPlayCrate = { tracks ->
                            if (tracks.isNotEmpty()) {
                                onPlayBatchTracks(tracks)
                            }
                        },
                        onOpenCrate = onOpenCrate,
                        onCrateActions = onCrateActions,
                        onOpenSmartCrateBuilder = onOpenSmartCrateBuilder,
                        onShowDoc = onShowDoc
                    )
                }
            }
        }
        }
        item(key = "search_prompt", contentType = "search_prompt") {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                HomeSearchPrompt(onClick = onOpenSearch)
            }
        }

        if (playlists.isNotEmpty()) {
            item(key = "playlists", contentType = "cover_shelf") {
                CoverShelf(title = "Your playlists") {
                    items(displayedPlaylists, key = { it.id }) { playlist ->
                        val key = collectionCoverKey(playlist.id)
                        val firstTrack = playlist.trackIds.firstOrNull()?.let { trackById[it] }
                        CoverCard(title = playlist.name, caption = "${playlist.trackIds.size} songs",
                            note = covers.noteFor(key), artworkUrl = covers.imageFor(key, playlist.artworkUrl ?: firstTrack?.artworkUrl),
                            onClick = { onOpenCollection("playlist", playlist.id) },
                            onLongClick = { onCoverActions(key, playlist.name) })
                    }
                }
            }
        }
        if (standoutTracks.isNotEmpty()) {
            item(key = "standouts", contentType = "standouts") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HomeSectionHeader(title = "Standouts", subtitle = "A little larger, still quick to scan.")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(standoutTracks, key = { it.id }, contentType = { "standout_card" }) { track ->
                            FeatureTrackCard(
                                track = track,
                                selected = selectedTrack?.id == track.id,
                                onClick = { onPlayTrack(track) }
                            )
                        }
                    }
                }
            }
        }
        if (albumGroups.isNotEmpty()) {
            item(key = "albums_deck", contentType = "albums_deck") {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    AlbumCarouselDeck(
                        albumGroups = albumGroups,
                        onPlayTrack = onPlayTrack,
                        onOpenCollection = onOpenCollection
                    )
                }
            }
        }
        if (artistGroups.isNotEmpty()) {
            item(key = "artists", contentType = "artists") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HomeSectionHeader(title = "Artists In Rotation", subtitle = "Tap into a voice, then keep moving.")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(artistGroups, key = { it.first }, contentType = { "artist_pill" }) { (artist, tracksForArtist) ->
                            ArtistShortcutPill(
                                artist = artist,
                                tracks = tracksForArtist,
                                onClick = { tracksForArtist.firstOrNull()?.let(onPlayTrack) }
                            )
                        }
                    }
                }
            }
        }

        if (jumpBackTracks.isNotEmpty()) {
            item(key = "jump_back", contentType = "jump_back") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HomeSectionHeader(title = "Jump Back In", subtitle = "Compact, quick, no overthinking.")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(jumpBackTracks, key = { it.id }, contentType = { "jump_back_card" }) { track ->
                            MiniTrackCard(
                                track = track,
                                selected = selectedTrack?.id == track.id,
                                onClick = { onPlayTrack(track) },
                                onLongClick = { onLongClickTrack(track) }
                            )
                        }
                    }
                }
            }
        }
        if (recentTracks.isNotEmpty()) {
            item(key = "recent_library", contentType = "recent_library") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HomeSectionHeader(title = "More From Your Library", subtitle = "Keep browsing without leaving Home.")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentTracks, key = { it.id }, contentType = { "recent_track_pill" }) { track ->
                            CompactTrackPill(
                                track = track,
                                selected = selectedTrack?.id == track.id,
                                onClick = { onPlayTrack(track) },
                                onLongClick = { onLongClickTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            item(key = "error_notice", contentType = "error_notice") {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionCard(
                        title = "Connection Needs Attention",
                        subtitle = connectionState.label
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = connectionGuidanceFor(connectionState, error),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
internal fun HomeHeroHeader(
    greetingTitle: String,
    greetingSubtitle: String,
    hostName: String?,
    isPlaying: Boolean,
    connectionState: HostConnectionState,
    hostTrackCount: Int,
    localTrackCount: Int,
    experienceMode: AppExperienceMode = AppExperienceMode.Essential,
    onExperienceModeChange: (AppExperienceMode) -> Unit = {},
    onOpenProfile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                PageIdentity(PixelodyTab.Home)
                Text(
                    text = when { connectionState == HostConnectionState.Connected && hostName != null && hostName != "Local Phone / Standalone" -> "Connected to $hostName"; localTrackCount > 0 -> "$localTrackCount songs on this phone"; else -> "Your library starts here" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(onClick = onOpenProfile, modifier = Modifier.size(48.dp)
                .semantics { contentDescription = "Settings and themes" },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface, shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(TransportGlyphType.Settings, color = MaterialTheme.colorScheme.onSurface, sizeDp = 22)
                }
            }
        }
    }
}

@Composable
internal fun EssentialHeroActionCard(
    isPlaying: Boolean,
    onShuffleAll: () -> Unit,
    onResumeOrPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelodyHeroStage(
        data = HeroStageData(
            title = "FLOW PLAYER",
            subtitle = "Smart background queue & instant mix",
            badge = if (isPlaying) "PLAYING" else "READY",
            primaryActionLabel = "SHUFFLE PLAY",
            onPrimaryAction = onShuffleAll,
            secondaryActionLabel = if (isPlaying) "PAUSE" else "RESUME",
            onSecondaryAction = onResumeOrPlay,
            isPlaying = isPlaying
        ),
        modifier = modifier
    )
}

@Composable
internal fun EssentialShortcutsGrid(
    favoriteCount: Int,
    recentCount: Int,
    losslessCount: Int,
    playlistCount: Int,
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenLossless: () -> Unit,
    onOpenPlaylists: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EssentialShortcutTile(
                title = "Liked Songs",
                subtitle = "$favoriteCount tracks",
                glyph = TransportGlyphType.HeartFilled,
                glyphColor = Color(0xFFEF4444),
                onClick = onOpenFavorites,
                modifier = Modifier.weight(1f)
            )
            EssentialShortcutTile(
                title = "Recent Tracks",
                subtitle = "$recentCount tracks",
                glyph = TransportGlyphType.OmniSource,
                glyphColor = Color(0xFF38BDF8),
                onClick = onOpenRecent,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EssentialShortcutTile(
                title = "Hi-Res Master",
                subtitle = "$losslessCount FLAC",
                glyph = TransportGlyphType.DiamondLossless,
                glyphColor = Color(0xFFF59E0B),
                onClick = onOpenLossless,
                modifier = Modifier.weight(1f)
            )
            EssentialShortcutTile(
                title = "Playlists",
                subtitle = "$playlistCount collections",
                glyph = TransportGlyphType.Folder,
                glyphColor = Color(0xFFA855F7),
                onClick = onOpenPlaylists,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun EssentialShortcutTile(
    title: String,
    subtitle: String,
    glyph: TransportGlyphType,
    glyphColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelodyQuickStartTile(
        data = QuickTileData(
            title = title,
            subtitle = subtitle,
            glyph = glyph,
            glyphColor = glyphColor,
            onClick = onClick
        ),
        modifier = modifier.height(56.dp)
    )
}

@Composable
internal fun EssentialTrackRow(
    track: Track,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelodyTrackItem(
        data = track.toItemData(
            selected = isSelected,
            isPlaying = isPlaying,
            onClick = onClick
        ),
        modifier = modifier
    )
}

@Composable
internal fun HomeSearchPrompt(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Search music",
                role = Role.Button,
                onClick = onClick
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelodyNavGlyph(tab = PixelodyTab.Search, selected = false)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "What do you want to hear?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun HomeQuickStartShelf(
    quickPaths: List<HabitQuickPath>,
    playableTracks: List<Track>,
    favoriteTracks: List<Track>,
    queuedTracks: List<Track>,
    losslessTracks: List<Track>,
    localTracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCollection: (String, String) -> Unit = { _, _ -> },
    onOpenQueue: () -> Unit = {},
    onOpenDevice: () -> Unit,
    onOpenCreate: () -> Unit,
    onRecordAction: (HabitActionType) -> Unit,
    onLongClickCategory: ((String) -> Unit)? = null
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(quickPaths, key = { it.actionType.name }, contentType = { "quick_path_tile" }) { path ->
            val (tileEnabled, onClick) = when (path.actionType) {
                HabitActionType.PlaySomething -> Pair(
                    playableTracks.isNotEmpty(),
                    {
                        onRecordAction(HabitActionType.PlaySomething)
                        playableTracks.firstOrNull()?.let(onPlayTrack)
                    }
                )
                HabitActionType.ResumeTrack -> Pair(
                    playableTracks.isNotEmpty(),
                    {
                        onRecordAction(HabitActionType.ResumeTrack)
                        playableTracks.firstOrNull()?.let(onPlayTrack)
                    }
                )
                HabitActionType.OpenFavorites -> Pair(
                    favoriteTracks.isNotEmpty(),
                    {
                        onRecordAction(HabitActionType.OpenFavorites)
                        onOpenCollection("favorites", "")
                    }
                )
                HabitActionType.OpenQueue -> Pair(
                    true,
                    {
                        onRecordAction(HabitActionType.OpenQueue)
                        onOpenQueue()
                    }
                )
                HabitActionType.HiResFilter -> Pair(
                    losslessTracks.isNotEmpty(),
                    {
                        onRecordAction(HabitActionType.HiResFilter)
                        onOpenCollection("lossless", "")
                    }
                )
                HabitActionType.SelectLocalPhoneSource -> Pair(
                    localTracks.isNotEmpty(),
                    {
                        onRecordAction(HabitActionType.SelectLocalPhoneSource)
                        localTracks.firstOrNull()?.let(onPlayTrack) ?: onOpenDevice()
                    }
                )
                HabitActionType.AddMusic -> Pair(
                    true,
                    {
                        onRecordAction(HabitActionType.AddMusic)
                        onOpenCreate()
                    }
                )
                HabitActionType.HeavyRotation -> Pair(
                    playableTracks.isNotEmpty(),
                    {
                        onRecordAction(HabitActionType.HeavyRotation)
                        (favoriteTracks + playableTracks).firstOrNull()?.let(onPlayTrack)
                    }
                )
                else -> Pair(
                    playableTracks.isNotEmpty(),
                    {
                        onRecordAction(path.actionType)
                        playableTracks.firstOrNull()?.let(onPlayTrack)
                    }
                )
            }

            QuickStartTile(
                title = path.title,
                subtitle = path.subtitle,
                enabled = tileEnabled,
                onClick = { onClick() },
                onLongClick = { onLongClickCategory?.invoke(path.title) }
            )
        }
    }
}

@Composable
internal fun HomeNowPlayingCard(
    track: Track,
    isPlaying: Boolean,
    equalizerProfile: EqualizerProfile,
    trackHasEqualizerOverride: Boolean,
    onOpenPlayer: () -> Unit,
    onPlayTrack: () -> Unit,
    onCycleEqualizerPreset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Open full player for ${track.title}",
                role = Role.Button,
                onClick = onOpenPlayer
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(artworkUrl = track.artworkUrl, title = track.title, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOf(
                        if (isPlaying) "Playing now" else "Ready to resume",
                        equalizerSummary(equalizerProfile, trackHasEqualizerOverride)
                    ).joinToString(" / "),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onCycleEqualizerPreset) {
                Text("EQ")
            }
            TextButton(onClick = onPlayTrack) { Text(if (isPlaying) "Open" else "Play") }
        }
    }
}

@Composable
internal fun PlaylistShortcutCard(
    name: String,
    trackCount: Int,
    artworkUrl: String?,
    onClick: () -> Unit
) {
    PixelodyQuickStartTile(
        data = QuickTileData(
            title = name,
            subtitle = "$trackCount songs",
            artworkUrl = artworkUrl,
            onClick = onClick
        ),
        modifier = Modifier.width(136.dp)
    )
}

@Composable
internal fun AlbumShortcutCard(
    album: String,
    tracks: List<Track>,
    onClick: () -> Unit
) {
    val firstTrack = tracks.firstOrNull()
    Card(
        modifier = Modifier
            .width(124.dp)
            .clickable(
                onClickLabel = "Open album $album",
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
    ) {
        Column(modifier = Modifier.padding(9.dp)) {
            RemoteArtwork(
                artworkUrl = firstTrack?.artworkUrl,
                title = album,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(album, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("${tracks.size} tracks", maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ArtistShortcutPill(
    artist: String,
    tracks: List<Track>,
    onClick: () -> Unit
) {
    val firstTrack = tracks.firstOrNull()
    Surface(
        modifier = Modifier
            .width(174.dp)
            .clickable(
                onClickLabel = "Open artist $artist",
                role = Role.Button,
                onClick = onClick
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(artworkUrl = firstTrack?.artworkUrl, title = artist, modifier = Modifier.size(38.dp))
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("${tracks.size} track${if (tracks.size == 1) "" else "s"}", maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun SourceChoiceTile(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalPixelodyThemeVariant.current
    Surface(
        modifier = Modifier
            .width(112.dp)
            .height(58.dp)
            .clickable(
                enabled = enabled,
                onClickLabel = title,
                role = Role.Button,
                onClick = onClick
            ),
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
        shape = theme.plate,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.22f else 0.12f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun FeatureTrackCard(
    track: Track,
    selected: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalPixelodyThemeVariant.current
    Card(
        modifier = Modifier
            .width(118.dp)
            .clickable(
                onClickLabel = "Play ${track.title}",
                role = Role.Button,
                onClick = onClick
            ),
        shape = theme.plate,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            RemoteArtwork(
                artworkUrl = track.artworkUrl,
                title = track.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(theme.plate)
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(text = track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            Text(text = track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MiniTrackCard(
    track: Track,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val theme = LocalPixelodyThemeVariant.current
    Card(
        modifier = Modifier
            .width(94.dp)
            .combinedClickable(
                onClickLabel = "Play ${track.title}",
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = theme.plate,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(7.dp)) {
            RemoteArtwork(artworkUrl = track.artworkUrl, title = track.title, modifier = Modifier.size(58.dp).clip(theme.plate))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            Text(text = track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun AlbumCarouselDeck(
    albumGroups: List<Pair<String, List<Track>>>,
    onPlayTrack: (Track) -> Unit,
    onOpenCollection: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDeckMode by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeSectionHeader(
                title = "Albums To Start",
                subtitle = "Choose a pocket of the library, not a whole screen.",
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Surface(
                onClick = {
                    haptic.performTick()
                    isDeckMode = !isDeckMode
                },
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Text(
                    text = if (isDeckMode) "3D DECK" else "SHELF",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isDeckMode) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(albumGroups, key = { it.first }, contentType = { "spatial_album_deck_card" }) { (album, tracksForAlbum) ->
                    SpatialAlbumDeckCard(
                        album = album,
                        tracks = tracksForAlbum,
                        onClick = { onOpenCollection("album", "album:$album") }
                    )
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(albumGroups, key = { it.first }, contentType = { "album_shortcut_card" }) { (album, tracksForAlbum) ->
                    AlbumShortcutCard(
                        album = album,
                        tracks = tracksForAlbum,
                        onClick = { onOpenCollection("album", "album:$album") }
                    )
                }
            }
        }
    }
}

@Composable
internal fun SpatialAlbumDeckCard(
    album: String,
    tracks: List<Track>,
    onClick: () -> Unit
) {
    val firstTrack = tracks.firstOrNull()
    val isLossless = tracks.any { it.lossless }
    val artist = firstTrack?.artist ?: "Unknown Artist"

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(205.dp),
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                RemoteArtwork(
                    artworkUrl = firstTrack?.artworkUrl,
                    title = album,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${tracks.size} TRACKS",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (isLossless) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        color = Color(0xFFE5A93C).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "LOSSLESS",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = album.ifBlank { "Untitled Album" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun HomeActionGrid(
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFlowCabinet: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeSectionHeader(title = "Choose Your Next Move", subtitle = "Short paths for different listening moods.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeActionTile("Library", "Browse saved music", Modifier.weight(1f), onOpenLibrary)
            HomeActionTile("Search", "Find it fast", Modifier.weight(1f), onOpenSearch)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeActionTile("Flow Cabinet", "Pull out shuffle rack", Modifier.weight(1f), onOpenFlowCabinet)
            HomeActionTile("Profile", "Themes and settings", Modifier.weight(1f), onOpenProfile)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeActionTile("Create", "Add or host", Modifier.weight(1f), onOpenCreate)
            HomeActionTile("Phone Files", "Import from this device", Modifier.weight(1f), onOpenDevice)
        }
    }
}

@Composable
internal fun HomeActionTile(
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val theme = LocalPixelodyThemeVariant.current
    Surface(
        modifier = modifier
            .height(64.dp)
            .clickable(
                onClickLabel = title,
                role = Role.Button,
                onClick = onClick
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = theme.plate,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.Center) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun PixelodyTopBar(
    snapshot: LibrarySnapshot?,
    isPlaying: Boolean,
    connectionState: HostConnectionState,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pixelody",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = snapshot?.host?.hostName ?: "Local-first music companion",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isPlaying) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isPlaying && connectionState == HostConnectionState.Connected) "Playing" else connectionState.label,
                    modifier = Modifier
                        .testTag(PixelodyStateTags.CONNECTION_STATUS)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (isPlaying) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                onClick = onOpenProfile,
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "P",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Curated Smart Discovery Crate representing algorithmic or sonic collections.
 */
data class CuratedSmartCrate(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val glyphType: TransportGlyphType,
    val tracks: List<Track>,
    val moodColorHex: Long = 0xFF38BDF8
)

/**
 * Mode 3: Discovery & Smart Crates on Home Screen.
 * Curatorial digging shelves providing one-tap play and deep 9-slot inspection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DiscoveryCrateShelf(
    curatedCrates: List<CuratedSmartCrate>,
    userCrates: List<Crate>,
    trackById: Map<String, Track> = emptyMap(),
    onPlayCrate: (List<Track>) -> Unit,
    onOpenCrate: (String) -> Unit,
    onCrateActions: (String) -> Unit,
    onOpenSmartCrateBuilder: () -> Unit = {},
    modifier: Modifier = Modifier,
    onShowDoc: ((String) -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeSectionHeader(title = "Made for you", subtitle = "Suggestions from your music")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(curatedCrates, key = { it.id }) { collection ->
                CoverCard(title = collection.title, caption = "${collection.tracks.size} songs",
                    artworkUrl = collection.tracks.firstOrNull()?.artworkUrl,
                    onClick = { onPlayCrate(collection.tracks) },
                    onLongClick = { onShowDoc?.invoke("smart_crates") })
            }
        }
        OutlinedButton(onClick = onOpenSmartCrateBuilder, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Create smart playlist")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CuratedCrateCard(
    crate: CuratedSmartCrate,
    onPlay: () -> Unit,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier,
    onShowDoc: ((String) -> Unit)? = null
) {
    val moodColor = Color(crate.moodColorHex)
    val haptic = LocalHapticFeedback.current
    var currentShelf by remember(crate.id) { mutableStateOf(0) }
    val totalShelves = ((crate.tracks.size + 3) / 4).coerceAtLeast(1)
    val currentTracksOffset = currentShelf * 4

    Surface(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onShowDoc != null) {
                    Modifier.combinedClickable(
                        onClick = onInspect,
                        onLongClick = {
                            haptic.performTick()
                            onShowDoc("smart_crates")
                        }
                    )
                } else Modifier
            ),
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, moodColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: vector glyph + tag pill + slot count / shelf indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelodyTransportGlyph(
                        glyph = crate.glyphType,
                        color = moodColor,
                        sizeDp = 14
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = moodColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, moodColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = crate.tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = moodColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (totalShelves > 1) {
                        Text(
                            text = "SHELF ${currentShelf + 1}/$totalShelves",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = moodColor
                        )
                    } else {
                        Text(
                            text = "${crate.tracks.size} TRACKS",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2x2 Quadrant Artwork Box with Z-Depth Tap/Swipe to Flip
            Surface(
                onClick = {
                    if (totalShelves > 1) {
                        haptic.performTick()
                        currentShelf = (currentShelf + 1) % totalShelves
                    } else {
                        onInspect()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val track0 = crate.tracks.getOrNull(currentTracksOffset + 0)
                        val track1 = crate.tracks.getOrNull(currentTracksOffset + 1)
                        QuadrantArtworkCell(track = track0, modifier = Modifier.weight(1f))
                        QuadrantArtworkCell(track = track1, modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val track2 = crate.tracks.getOrNull(currentTracksOffset + 2)
                        val track3 = crate.tracks.getOrNull(currentTracksOffset + 3)
                        QuadrantArtworkCell(track = track2, modifier = Modifier.weight(1f))
                        QuadrantArtworkCell(track = track3, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Title and Subtitle
            Column {
                Text(
                    text = crate.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = crate.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Buttons: Play + Inspect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = {
                        haptic.performTick()
                        onPlay()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = moodColor.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, moodColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Play,
                            color = moodColor,
                            sizeDp = 12
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PLAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = moodColor
                        )
                    }
                }

                Surface(
                    onClick = {
                        haptic.performTick()
                        onInspect()
                    },
                    modifier = Modifier
                        .width(44.dp)
                        .height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.WaveformBars,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            sizeDp = 13
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantArtworkCell(
    track: Track?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        if (track != null) {
            RemoteArtwork(
                artworkUrl = track.artworkUrl,
                title = track.title,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PixelodyTransportGlyph(
                    glyph = TransportGlyphType.WaveformBars,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    sizeDp = 12
                )
            }
        }
    }
}

/** The sideways shelf used for every row of covers on Home: one light title, then covers. */
@Composable
private fun CoverShelf(
    title: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CoverSectionTitle(title = title, modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            content = content
        )
    }
}

/**
 * The quick-pick grid at the top of Home: the four fixed shortcuts, then the first
 * crates, two across. Shortcuts have no picture to change, so only crates open the
 * picture-and-note menu on long press.
 */
@Composable
private fun HomeAccessPanel(
    mode: AppExperienceMode,
    onModeChange: (AppExperienceMode) -> Unit,
    onAddMusic: () -> Unit,
    onConnectDesktop: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenLossless: () -> Unit,
    onOpenPlaylists: () -> Unit
) {
    Surface(modifier = modifier.fillMaxWidth().testTag("home:quick-access"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .65f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .3f))) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Quick access", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Tap the mode to change your tools", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ExperienceModeQuickChip(mode, { onModeChange(mode.toggle()) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoverPill("Liked songs", null, onOpenFavorites, null, Modifier.weight(1f),
                leading = { PixelodyTransportGlyph(TransportGlyphType.HeartFilled, color = MaterialTheme.colorScheme.primary, sizeDp = 22) })
            CoverPill("Recent", null, onOpenRecent, null, Modifier.weight(1f),
                leading = { PixelodyTransportGlyph(TransportGlyphType.OmniSource, color = MaterialTheme.colorScheme.primary, sizeDp = 22) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoverPill("Lossless", null, onOpenLossless, null, Modifier.weight(1f),
                leading = { PixelodyTransportGlyph(TransportGlyphType.DiamondLossless, color = MaterialTheme.colorScheme.primary, sizeDp = 22) })
            CoverPill("Playlists", null, onOpenPlaylists, null, Modifier.weight(1f),
                leading = { PixelodyTransportGlyph(TransportGlyphType.Folder, color = MaterialTheme.colorScheme.primary, sizeDp = 22) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoverPill("Add music", null, onAddMusic, null, Modifier.weight(1f),
                leading = { PixelodyTransportGlyph(TransportGlyphType.Folder, color = MaterialTheme.colorScheme.primary, sizeDp = 22) })
            CoverPill("Desktop", null, onConnectDesktop, null, Modifier.weight(1f),
                leading = { PixelodyTransportGlyph(TransportGlyphType.MeshNetwork, color = MaterialTheme.colorScheme.primary, sizeDp = 22) })
        }
    }
    }
}

/** First picture found among a crate's slots, so a crate with no chosen cover still looks like its contents. */
private fun crateArtwork(
    crate: Crate,
    trackById: Map<String, Track>,
    playlists: List<com.pixelody.app.data.model.Playlist>
): String? {
    crate.slots.forEach { slot ->
        val found = when (slot) {
            is CrateSlot.SingleTrack -> trackById[slot.trackId]?.artworkUrl
            is CrateSlot.Collection -> playlists.firstOrNull { it.id == slot.collectionId }?.let { playlist ->
                playlist.artworkUrl ?: playlist.trackIds.firstNotNullOfOrNull { trackById[it]?.artworkUrl }
            }
            else -> null
        }
        if (!found.isNullOrBlank()) return found
    }
    return null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudioListeningLead(track: Track?, isPlaying: Boolean, onPlayPause: () -> Unit,
    onOpenPlayer: () -> Unit, onOpenQueue: () -> Unit, canOpenPlayer: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RemoteArtwork(track?.artworkUrl, track?.title ?: "Your music", Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track?.title ?: "Choose something to play", style = MaterialTheme.typography.titleLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track?.artist ?: "Your library is ready", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPlayPause, modifier = Modifier.heightIn(min = 48.dp).testTag("studio:home-play")) {
                Text(if (isPlaying) "Pause" else "Play")
            }
            if (canOpenPlayer) TextButton(onClick = onOpenPlayer, modifier = Modifier.heightIn(min = 48.dp)) { Text("Player") }
            TextButton(onClick = onOpenQueue, modifier = Modifier.heightIn(min = 48.dp)) { Text("Queue") }
        }
    }
}
