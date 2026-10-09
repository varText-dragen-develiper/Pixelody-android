package com.pixelody.app.feature.library

import com.pixelody.app.core.playback.FlowBrowseFilter
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.feature.nowplaying.FlowEntry
import com.pixelody.app.ui.components.StudioSectionToggle

import com.pixelody.app.data.model.CoverBook
import com.pixelody.app.data.model.collectionCoverKey
import com.pixelody.app.data.model.trackCoverKey
import com.pixelody.app.data.model.withCover
import com.pixelody.app.ui.components.CoverCard
import com.pixelody.app.ui.components.CoverTrackRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pixelody.app.ui.theme.compactSurfaceShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pixelody.app.core.genre.GenreTaxonomyEngine
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.storage.DownloadQueueState
import com.pixelody.app.feature.connection.hostSourceNeedsRecovery
import com.pixelody.app.feature.connection.label
import com.pixelody.app.ui.components.AlphabetScrubber
import com.pixelody.app.ui.components.BatchSelectionActionBar
import com.pixelody.app.ui.components.EmptyState
import com.pixelody.app.ui.components.FlippableCrateView
import com.pixelody.app.ui.components.HarmonicFilterMode
import com.pixelody.app.ui.components.getCompatibleCamelotKeyCodes
import com.pixelody.app.ui.components.HomeSectionHeader
import com.pixelody.app.ui.components.HostStatusCard
import com.pixelody.app.ui.components.LibraryScopeSummary
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.QuickStartTile
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.PageIdentity
import com.pixelody.app.ui.navigation.PixelodyTab
import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.ui.components.ExperienceModeQuickChip
import com.pixelody.app.ui.components.SmartPocketFilter
import com.pixelody.app.ui.components.SmartPocketRow
import com.pixelody.app.ui.components.SourceRecoveryCard
import com.pixelody.app.ui.components.SourceScope
import com.pixelody.app.ui.components.TrackRow
import com.pixelody.app.ui.components.TriSourcePivotBar
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.filterTracksBySource
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.navigation.PixelodyDetailKind
import com.pixelody.app.ui.navigation.PixelodyDetailRoute
import com.pixelody.app.ui.navigation.PixelodyStateTags
import kotlinx.coroutines.launch
import java.util.Locale

/* =========================================================================
 * Slice 3 — Library Screen
 * Extracted from PixelodyShell.kt into com.pixelody.app.feature.library.
 * ========================================================================= */

internal data class CollectionShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val tracks: List<Track>
)

internal fun matchesCollectionId(candidateId: String, selectedId: String): Boolean {
    if (candidateId.isEmpty() || selectedId.isEmpty()) return false
    if (candidateId.equals(selectedId, ignoreCase = true)) return true
    val cleanCandidate = candidateId.substringAfter(":")
    val cleanSelected = selectedId.substringAfter(":")
    return cleanCandidate.equals(cleanSelected, ignoreCase = true)
}

@Composable
internal fun LibraryScreen(
    snapshot: LibrarySnapshot?,
    localTracks: List<Track> = emptyList(),
    selectedTrack: Track?,
    cachedTrackIds: Set<String> = emptySet(),
    downloadQueueState: DownloadQueueState = DownloadQueueState(),
    onDownloadTrack: (Track) -> Unit = {},
    onRemoveDownload: (String) -> Unit = {},
    onPlayTrack: (Track) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSearch: () -> Unit,
    equalizerProfile: EqualizerProfile,
    trackHasEqualizerOverride: Boolean,
    onOpenPlayer: () -> Unit,
    onCycleEqualizerPreset: () -> Unit,
    onLongClickTrack: (Track) -> Unit = {},
    onToggleFavorite: (Track) -> Unit = {},
    onPlayBatchNext: (List<Track>) -> Unit = {},
    onAddBatchToQueue: (List<Track>) -> Unit = {},
    onBatchDownloadTracks: (List<Track>) -> Unit = {},
    onBatchToggleFavorites: (List<Track>) -> Unit = {},
    onPairFixture: () -> Unit = {},
    activeDetail: PixelodyDetailRoute? = null,
    onOpenCollection: (PixelodyDetailRoute) -> Unit = {},
    onCloseCollection: () -> Unit = {},
    connectionState: HostConnectionState = HostConnectionState.Disconnected,
    connectionGuidance: String = "",
    hasSavedHost: Boolean = false,
    onRetryHost: () -> Unit = {},
    onOpenPhoneFiles: () -> Unit = {},
    onOpenTechnical: () -> Unit = {},
    sourceScope: SourceScope = SourceScope.All,
    onSourceScopeChange: (SourceScope) -> Unit = {},
    onCollectionActions: (String, String) -> Unit = { _, _ -> },
    recentTrackIds: List<String> = emptyList(),
    covers: CoverBook = CoverBook(),
    onCoverActions: (String, String) -> Unit = { _, _ -> },
    onShowDoc: ((String) -> Unit)? = null,
    experienceMode: AppExperienceMode = AppExperienceMode.Essential,
    onExperienceModeChange: (AppExperienceMode) -> Unit = {},
    onShuffleAllFlow: () -> Unit = {},
    shuffleMode: FlowShuffleMode = FlowShuffleMode.Off,
    harmonicBaseKey: CamelotKey? = null,
    harmonicFilterMode: HarmonicFilterMode = HarmonicFilterMode.StrictAdjacent,
    flowBpm: Float? = null,
    flowTolerance: Float = 10f,
    onOpenFlow: () -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var browseMode by rememberSaveable { mutableStateOf("Tracks") }
    val browseModeDetailKind = when (browseMode) {
        "Playlists" -> PixelodyDetailKind.Playlist
        "Albums" -> PixelodyDetailKind.Album
        "Artists" -> PixelodyDetailKind.Artist
        "Genres" -> PixelodyDetailKind.Genre
        "Favorites" -> PixelodyDetailKind.Favorites
        "Lossless" -> PixelodyDetailKind.Lossless
        else -> null
    }
    // A collection pushed from elsewhere - Home, a crate slot, "Go to album", "Go to genre" - arrives
    // with its kind while browseMode is still whatever it was last set to, so the takeIf
    // below dropped it and left you in an unfiltered Library with no detail and nothing
    // saying why. The incoming route now selects its own browse mode first.
    LaunchedEffect(activeDetail) {
        val incoming = when (activeDetail?.kind) {
            PixelodyDetailKind.Playlist -> "Playlists"
            PixelodyDetailKind.Album -> "Albums"
            PixelodyDetailKind.Artist -> "Artists"
            PixelodyDetailKind.Genre -> "Genres"
            PixelodyDetailKind.Favorites -> "Favorites"
            PixelodyDetailKind.Lossless -> "Lossless"
            else -> null
        }
        if (incoming != null && incoming != browseMode) browseMode = incoming
    }
    val selectedCollection = activeDetail
        ?.takeIf { it.kind == browseModeDetailKind }
        ?.id
        .orEmpty()
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    val haptic = LocalHapticFeedback.current
    val allTracks = remember(snapshot, localTracks) {
        (snapshot?.tracks.orEmpty() + localTracks).distinctBy { it.id }
    }
    val localTrackIds = remember(localTracks) { localTracks.map { it.id }.toSet() }
    val jamTrackIds = remember(snapshot) { snapshot?.queue?.trackIds.orEmpty().toSet() }
    val tracks = remember(allTracks, sourceScope, localTrackIds, jamTrackIds) {
        filterTracksBySource(allTracks, sourceScope, localTrackIds, jamTrackIds)
    }
    val playlists = snapshot?.playlists.orEmpty()
    val trackById = remember(allTracks) { allTracks.associateBy { it.id } }
    val favoriteIds = remember(snapshot, allTracks) {
        (snapshot?.favorites.orEmpty() + allTracks.filter { it.favorite }.map { it.id }).toSet()
    }
    val favoriteTracks = remember(tracks, favoriteIds) {
        tracks.filter { it.id in favoriteIds || it.favorite }
    }
    val queuedTracks = remember(snapshot, trackById) {
        snapshot?.queue?.trackIds.orEmpty().mapNotNull { trackById[it] }
    }
    val playableTracks = remember(tracks) {
        tracks.filterNot { it.missing || it.streamUrl.isBlank() }
    }
    val losslessTracks = remember(playableTracks) {
        playableTracks.filter { it.lossless }
    }
    val playlistShortcuts = remember(playlists, trackById) {
        playlists.map { playlist ->
            val playlistTracks = playlist.trackIds.mapNotNull { trackById[it] }
            CollectionShortcut(
                id = playlist.id,
                title = playlist.name,
                subtitle = "${playlist.trackIds.size} track${if (playlist.trackIds.size == 1) "" else "s"}",
                artworkUrl = playlist.artworkUrl ?: playlistTracks.firstOrNull()?.artworkUrl,
                tracks = playlistTracks
            )
        }
    }
    val albumShortcuts = remember(playableTracks) {
        playableTracks
            .filter { it.album.isNotBlank() }
            .groupBy { it.album }
            .toList()
            .sortedWith(compareByDescending<Pair<String, List<Track>>> { it.second.size }.thenBy { it.first })
            .map { (album, tracksForAlbum) ->
                CollectionShortcut(
                    id = "album:$album",
                    title = album,
                    subtitle = "${tracksForAlbum.size} track${if (tracksForAlbum.size == 1) "" else "s"}",
                    artworkUrl = tracksForAlbum.firstOrNull()?.artworkUrl,
                    tracks = tracksForAlbum
                )
            }
    }
    val artistShortcuts = remember(playableTracks) {
        playableTracks
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist }
            .toList()
            .sortedWith(compareByDescending<Pair<String, List<Track>>> { it.second.size }.thenBy { it.first })
            .map { (artist, tracksForArtist) ->
                CollectionShortcut(
                    id = "artist:$artist",
                    title = artist,
                    subtitle = "${tracksForArtist.size} track${if (tracksForArtist.size == 1) "" else "s"}",
                    artworkUrl = tracksForArtist.firstOrNull()?.artworkUrl,
                    tracks = tracksForArtist
                )
            }
    }
    val genreShortcuts = remember(playableTracks) {
        playableTracks
            .filter { it.genre.isNotBlank() }
            .groupBy { GenreTaxonomyEngine.canonicalize(it.genre) }
            .toList()
            .sortedWith(compareByDescending<Pair<String, List<Track>>> { it.second.size }.thenBy { it.first })
            .map { (genre, tracksForGenre) ->
                CollectionShortcut(
                    id = "genre:$genre",
                    title = genre,
                    subtitle = "${tracksForGenre.size} track${if (tracksForGenre.size == 1) "" else "s"}",
                    artworkUrl = tracksForGenre.firstOrNull()?.artworkUrl,
                    tracks = tracksForGenre
                )
            }
    }
    val activeCollectionShortcuts = when (browseMode) {
        "Playlists" -> playlistShortcuts
        "Albums" -> albumShortcuts
        "Artists" -> artistShortcuts
        "Genres" -> genreShortcuts
        else -> emptyList()
    }
    val selectedCollectionTitle = activeCollectionShortcuts
        .firstOrNull { matchesCollectionId(it.id, selectedCollection) }
        ?.title
        .orEmpty()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showLibraryTools by rememberSaveable { mutableStateOf(false) }
    var smartFilter by remember { mutableStateOf(SmartPocketFilter.All) }
    LaunchedEffect(activeDetail) {
        if (activeDetail?.kind == PixelodyDetailKind.Recent) {
            if (browseMode != "Tracks") browseMode = "Tracks"
            smartFilter = SmartPocketFilter.Recent
        }
    }
    var selectedGenreFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val availableGenresWithCounts = remember(tracks) {
        tracks
            .filter { it.genre.isNotBlank() }
            .groupBy { GenreTaxonomyEngine.canonicalize(it.genre) }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    val baseScopedTracks = when (browseMode) {
        "Playlists" -> {
            val cleanSelected = selectedCollection.substringAfter(":")
            val playlist = playlists.firstOrNull { it.id.equals(selectedCollection, ignoreCase = true) || it.id.equals(cleanSelected, ignoreCase = true) }
            if (playlist == null) tracks else tracks.filter { it.id in playlist.trackIds }
        }
        "Albums" -> if (selectedCollection.isBlank()) tracks else {
            val cleanSelected = selectedCollection.substringAfter(":")
            tracks.filter { it.album.equals(cleanSelected, ignoreCase = true) }
        }
        "Artists" -> if (selectedCollection.isBlank()) tracks else {
            val cleanSelected = selectedCollection.substringAfter(":")
            tracks.filter { it.artist.equals(cleanSelected, ignoreCase = true) }
        }
        "Genres" -> if (selectedCollection.isBlank()) tracks else {
            val target = selectedCollection.removePrefix("genre:").trim()
            tracks.filter { GenreTaxonomyEngine.canonicalize(it.genre).equals(target, ignoreCase = true) }
        }
        "Favorites" -> tracks.filter { it.id in favoriteIds || it.favorite }
        "Lossless" -> tracks.filter { it.lossless }
        "Offline" -> tracks.filter { it.id in cachedTrackIds }
        else -> tracks
    }

    val genreFilteredTracks = if (selectedGenreFilter == null || browseMode == "Genres") {
        baseScopedTracks
    } else {
        baseScopedTracks.filter { GenreTaxonomyEngine.canonicalize(it.genre).equals(selectedGenreFilter, ignoreCase = true) }
    }

    val pocketFilteredTracks = when (smartFilter) {
        SmartPocketFilter.HiRes -> genreFilteredTracks.filter { it.lossless }
        SmartPocketFilter.HeavyRotation -> genreFilteredTracks.filter { it.id in favoriteIds || it.favorite }
        SmartPocketFilter.Recent -> recentTrackIds.distinct().mapNotNull { id -> genreFilteredTracks.firstOrNull { it.id == id } }
        else -> genreFilteredTracks
    }

    val compatibleKeys = harmonicBaseKey?.let { getCompatibleCamelotKeyCodes(it, harmonicFilterMode) }
    val harmonicScopedTracks = if (compatibleKeys == null && flowBpm == null) pocketFilteredTracks else {
        pocketFilteredTracks.filter { track ->
            FlowBrowseFilter.matches(track, compatibleKeys, flowBpm, flowTolerance)
        }
    }

    var sortMode by rememberSaveable { mutableStateOf(TrackSortMode.Default) }

    val filteredTracks = remember(harmonicScopedTracks, query) {
        val needle = query.trim().lowercase(Locale.US)
        if (needle.isBlank()) harmonicScopedTracks else harmonicScopedTracks.filter { track ->
            listOf(track.title, track.artist, track.album, track.codec, track.format)
                .any { it.lowercase(Locale.US).contains(needle) }
        }
    }
    val sortedTracks = remember(filteredTracks, sortMode) {
        sortMode.sortTracks(filteredTracks)
    }
    val playableVisibleTracks = sortedTracks.filterNot { it.missing || it.streamUrl.isBlank() }
    val libraryUnCachedCount = sortedTracks.count { it.id !in cachedTrackIds && !localTrackIds.contains(it.id) }
    val showsBatchDownloadBanner = filteredTracks.isNotEmpty() && !isMultiSelectMode &&
        libraryUnCachedCount > 0 && (selectedCollection.isNotBlank() || browseMode == "Favorites")
    val showsCollectionShortcuts = activeCollectionShortcuts.isNotEmpty() && browseModeDetailKind != null
    val showsSourceRecovery = hostSourceNeedsRecovery(connectionState)
    LaunchedEffect(activeDetail) {
        if (activeDetail != null && sortedTracks.isNotEmpty()) {
            val totalItems = snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > 0 }
            val firstTrackIndex = (totalItems - sortedTracks.size).coerceAtLeast(0)
            listState.animateScrollToItem(firstTrackIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, end = if (browseMode == "Tracks") 28.dp else 0.dp, bottom = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        PageIdentity(PixelodyTab.Library)
                        Text(
                            text = "${allTracks.size} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    ExperienceModeQuickChip(
                        mode = experienceMode,
                        onToggle = { onExperienceModeChange(experienceMode.toggle()) }
                    )
                }
            }
            if (allTracks.isEmpty()) {
                item { EmptyState(text = "Your library is ready for music. Add files from this phone or connect your Pixelody desktop.") }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenPhoneFiles,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add music from this phone", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onPairFixture,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Connect desktop")
                        }
                    }
                }
            } else {
                if (experienceMode == AppExperienceMode.Essential) {
                    item(key = "essential_library_shuffle") {
                        val activeTheme = com.pixelody.app.ui.theme.LocalPixelodyThemeVariant.current
                        Surface(
                            onClick = {
                                haptic.performConfirm()
                                onShuffleAllFlow()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .height(48.dp),
                            shape = activeTheme.compactSurfaceShape(),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PixelodyTransportGlyph(
                                    glyph = TransportGlyphType.FlowShuffle,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    size = 16.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHUFFLE ALL",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                    }

                }
                if (showsSourceRecovery) {
                    item {
                        SourceRecoveryCard(
                            stateLabel = connectionState.label,
                            guidance = connectionGuidance,
                            isRetrying = connectionState == HostConnectionState.Connecting ||
                                connectionState == HostConnectionState.Reconnecting,
                            canRetry = hasSavedHost,
                            onRetry = onRetryHost,
                            onUsePhoneMusic = onOpenPhoneFiles,
                            onOpenConnectionTools = onOpenTechnical
                        )
                    }
                }
            if (experienceMode == AppExperienceMode.Studio) item {
                StudioSectionToggle("Library controls", buildString {
                    append("${filteredTracks.size} tracks · ${sourceScope.label}")
                    if (smartFilter != SmartPocketFilter.All) append(" · ${smartFilter.label}")
                    selectedGenreFilter?.let { append(" · $it") }
                    if (sortMode != TrackSortMode.Default) append(" · ${sortMode.label}")
                    harmonicBaseKey?.let { append(" · Key ${it.code}") }
                }, showLibraryTools, { showLibraryTools = !showLibraryTools }, "studio:library-controls", Modifier.padding(horizontal = 16.dp))
            }
                if (snapshot != null && experienceMode == AppExperienceMode.Studio && showLibraryTools) {
                item { HostStatusCard(snapshot = snapshot) }
            }
            if (experienceMode == AppExperienceMode.Studio && showLibraryTools) {
                item {
                    TriSourcePivotBar(
                        selectedScope = sourceScope,
                        onSelectScope = onSourceScopeChange,
                        localCount = localTracks.size,
                        hostConnected = connectionState == HostConnectionState.Connected,
                        hostCount = snapshot?.tracks?.size ?: 0,
                        jamActive = snapshot?.queue != null,
                        jamCount = queuedTracks.size,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PixelodyStateTags.LIBRARY_SEARCH_FIELD),
                    label = { Text("Search your library") },
                    singleLine = true
                )
            }
            if (showLibraryTools) item {
                SmartPocketRow(
                    selectedFilter = smartFilter,
                    onSelectFilter = { smartFilter = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                FlowEntry(shuffleMode, harmonicBaseKey, flowBpm, onOpenFlow)
            }
            if (experienceMode == AppExperienceMode.Studio && showLibraryTools) item {
                LibraryQuickActionShelf(
                    playableTracks = playableTracks,
                    favoriteTracks = favoriteTracks,
                    queuedTracks = queuedTracks,
                    losslessTracks = losslessTracks,
                    onPlayTrack = onPlayTrack,
                    onShowFavorites = {
                        browseMode = "Favorites"
                        onCloseCollection()
                    },
                    onShowLossless = {
                        browseMode = "Lossless"
                        onCloseCollection()
                    },
                    onOpenQueue = onOpenQueue,
                    onOpenSearch = onOpenSearch
                )
            }
            item {
                val browseModeRowState = rememberLazyListState()
                val browseModes = remember { listOf("Tracks", "Playlists", "Albums", "Artists", "Genres", "Favorites", "Lossless", "Offline", "Vinyl Vault") }
                LaunchedEffect(browseMode) {
                    val index = browseModes.indexOf(browseMode)
                    if (index >= 0) {
                        browseModeRowState.animateScrollToItem(index + 1)
                    }
                }
                LazyRow(
                    state = browseModeRowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PixelodyStateTags.LIBRARY_BROWSE_MODE),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = isMultiSelectMode,
                            onClick = {
                                haptic.performTick()
                                isMultiSelectMode = !isMultiSelectMode
                                if (!isMultiSelectMode) selectedTrackIds = emptySet()
                            },
                            label = { Text(if (isMultiSelectMode && selectedTrackIds.isNotEmpty()) "${selectedTrackIds.size} Selected" else if (isMultiSelectMode) "Selecting" else "Select") }
                        )
                    }
                    items(
                        items = listOf("Tracks", "Playlists", "Albums", "Artists", "Genres", "Favorites", "Lossless", "Offline", "Vinyl Vault"),
                        key = { it }
                    ) { mode ->
                        FilterChip(
                            selected = browseMode == mode,
                            onClick = {
                                haptic.performTick()
                                browseMode = mode
                                onCloseCollection()
                            },
                            label = { Text(if (mode == "Offline") "Offline (${cachedTrackIds.size})" else mode) }
                        )
                    }
                }
            }

            if (showLibraryTools && availableGenresWithCounts.isNotEmpty() && browseMode != "Genres") {
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "GENRE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        item {
                            FilterChip(
                                selected = selectedGenreFilter == null,
                                onClick = {
                                    haptic.performTick()
                                    selectedGenreFilter = null
                                },
                                label = { Text("All", fontSize = 12.sp) }
                            )
                        }
                        items(availableGenresWithCounts, key = { it.first }) { (genreName, count) ->
                            FilterChip(
                                selected = selectedGenreFilter == genreName,
                                onClick = {
                                    haptic.performTick()
                                    selectedGenreFilter = if (selectedGenreFilter == genreName) null else genreName
                                },
                                label = { Text("$genreName ($count)", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            if (showLibraryTools) item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PixelodyStateTags.LIBRARY_SORT_MODE),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SORT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(
                        items = TrackSortMode.values(),
                        key = { it.name }
                    ) { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = {
                                haptic.performTick()
                                sortMode = mode
                            },
                            label = { Text("${mode.badge} ${mode.label}", fontSize = 12.sp) }
                        )
                    }
                }
            }

            if (showsCollectionShortcuts) {
                item {
                    val collectionRowState = rememberLazyListState()
                    LaunchedEffect(selectedCollection) {
                        if (selectedCollection.isNotBlank()) {
                            val index = activeCollectionShortcuts.indexOfFirst { matchesCollectionId(it.id, selectedCollection) }
                            if (index >= 0) {
                                collectionRowState.animateScrollToItem((index + 1).coerceAtLeast(0))
                            }
                        }
                    }
                    LazyRow(
                        state = collectionRowState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(PixelodyStateTags.LIBRARY_COLLECTIONS),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCollection.isBlank(),
                                onClick = {
                                    haptic.performTick()
                                    onCloseCollection()
                                },
                                label = { Text("All ${browseMode.lowercase()}") }
                            )
                        }
                        items(activeCollectionShortcuts, key = { it.id }) { shortcut ->
                            val coverKey = collectionCoverKey(shortcut.id)
                            val isOpen = matchesCollectionId(shortcut.id, selectedCollection)
                            CoverCard(
                                title = shortcut.title,
                                caption = shortcut.subtitle,
                                note = covers.noteFor(coverKey),
                                artworkUrl = covers.imageFor(coverKey, shortcut.artworkUrl),
                                selected = isOpen,
                                round = browseMode == "Artists",
                                onClick = {
                                    haptic.performTick()
                                    if (isOpen) {
                                        onCloseCollection()
                                    } else {
                                        onOpenCollection(
                                            PixelodyDetailRoute(browseModeDetailKind, shortcut.id)
                                        )
                                    }
                                },
                                onLongClick = { onCoverActions(coverKey, shortcut.title) }
                            )
                        }
                    }
                }
            }

            // Batch Download Banner for Collections or Favorites
            if (filteredTracks.isNotEmpty() && !isMultiSelectMode) {
                val unCachedCount = libraryUnCachedCount
                if (unCachedCount > 0 && (selectedCollection.isNotBlank() || browseMode == "Favorites")) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (browseMode == "Favorites") "Offline Favorites" else selectedCollectionTitle.ifBlank { "Collection" },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$unCachedCount track${if (unCachedCount == 1) "" else "s"} not yet downloaded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        val toDownload = filteredTracks.filter { it.id !in cachedTrackIds && !localTrackIds.contains(it.id) }
                                        onBatchDownloadTracks(toDownload)
                                        haptic.performConfirm()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        PixelodyTransportGlyph(
                                            glyph = TransportGlyphType.Download,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            size = 12.dp
                                        )
                                        Text("Download All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredTracks.isEmpty()) {
                item { EmptyState(text = if (browseMode == "Offline") "No tracks downloaded for offline yet. Tap the download icon on any track." else "No matching music in this view.") }
                return@LazyColumn
            }
            if (browseMode == "Vinyl Vault") {
                item {
                    FlippableCrateView(
                        tracks = filteredTracks,
                        activeTrack = selectedTrack,
                        onSelectTrack = onPlayTrack,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )
                }
            }
            if (browseMode == "Playlists" && selectedCollection.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playlist: $selectedCollectionTitle",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = {
                                haptic.performTick()
                                onCollectionActions("playlists", selectedCollection)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Manage ›", fontSize = 12.sp)
                        }
                    }
                }
            }
            if (experienceMode == AppExperienceMode.Essential) item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(buildString {
                        append("${filteredTracks.size} tracks")
                        if (smartFilter != SmartPocketFilter.All) append(" · ${smartFilter.badge}")
                        selectedGenreFilter?.let { append(" · $it") }
                        if (sortMode != TrackSortMode.Default) append(" · ${sortMode.label}")
                    }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = { showLibraryTools = !showLibraryTools }) {
                        Text(if (showLibraryTools) "Hide filters" else "Filter and sort")
                    }
                }
            }
            if (experienceMode == AppExperienceMode.Studio && showLibraryTools) item {
                LibraryScopeSummary(
                    browseMode = browseMode,
                    selectedCollectionTitle = selectedCollectionTitle,
                    resultCount = filteredTracks.size,
                    playableCount = playableVisibleTracks.size,
                    smartFilter = smartFilter,
                    tracks = playableVisibleTracks,
                    onPlayFirst = { playableVisibleTracks.firstOrNull()?.let(onPlayTrack) },
                    onShuffle = { playableVisibleTracks.shuffled().firstOrNull()?.let(onPlayTrack) },
                    onOpenQueue = onOpenQueue,
                    onFlowFromHere = onOpenFlow,
                    onShowDoc = onShowDoc
                )
            }
            items(sortedTracks, key = { it.id }) { track ->
                val isCached = track.id in cachedTrackIds
                val isDownloading = track.id == downloadQueueState.activeTaskId
                val isQueued = downloadQueueState.queuedTasks.any { it.track.id == track.id }
                val progress = if (isDownloading) downloadQueueState.activeProgress else 0f
                val isSelectedInBatch = track.id in selectedTrackIds
                if (experienceMode == AppExperienceMode.Essential) {
                    val shown = track.withCover(covers)
                    CoverTrackRow(
                        title = shown.title,
                        artist = if (shown.lossless) "${shown.artist} · Lossless" else shown.artist,
                        note = covers.noteFor(trackCoverKey(track.id)),
                        artworkUrl = shown.artworkUrl,
                        selected = if (isMultiSelectMode) isSelectedInBatch else selectedTrack?.id == track.id,
                        onClick = {
                            if (isMultiSelectMode) {
                                haptic.performTick()
                                selectedTrackIds = if (isSelectedInBatch) selectedTrackIds - track.id else selectedTrackIds + track.id
                            } else {
                                onPlayTrack(track)
                            }
                        },
                        onLongClick = {
                            if (!isMultiSelectMode) {
                                onLongClickTrack(track)
                            } else {
                                haptic.performTick()
                                selectedTrackIds = if (isSelectedInBatch) selectedTrackIds - track.id else selectedTrackIds + track.id
                            }
                        },
                        trailing = {
                            val glyph = when {
                                isCached -> TransportGlyphType.OfflineCheck
                                isDownloading || isQueued -> TransportGlyphType.Download
                                else -> null
                            }
                            if (glyph != null) {
                                PixelodyTransportGlyph(
                                    glyph = glyph,
                                    color = MaterialTheme.colorScheme.primary,
                                    size = 16.dp
                                )
                            }
                        }
                    )
                } else TrackRow(
                    track = track.withCover(covers),
                    selected = if (isMultiSelectMode) isSelectedInBatch else selectedTrack?.id == track.id,
                    onClick = {
                        if (isMultiSelectMode) {
                            haptic.performTick()
                            selectedTrackIds = if (isSelectedInBatch) selectedTrackIds - track.id else selectedTrackIds + track.id
                        } else {
                            onPlayTrack(track)
                        }
                    },
                    onLongClick = {
                        if (!isMultiSelectMode) {
                            onLongClickTrack(track)
                        } else {
                            haptic.performTick()
                            selectedTrackIds = if (isSelectedInBatch) selectedTrackIds - track.id else selectedTrackIds + track.id
                        }
                    },
                    isFavorite = track.id in favoriteIds || track.favorite,
                    onToggleFavorite = { onToggleFavorite(track) },
                    equalizerProfile = equalizerProfile,
                    trackHasEqualizerOverride = trackHasEqualizerOverride,
                    isOfflineCached = isCached,
                    isDownloading = isDownloading,
                    isQueued = isQueued,
                    downloadProgress = progress,
                    onToggleDownload = {
                        if (isCached) onRemoveDownload(track.id)
                        else onDownloadTrack(track)
                    },
                    onOpenPlayer = onOpenPlayer,
                    onOpenQueue = onOpenQueue,
                    onCycleEqualizerPreset = onCycleEqualizerPreset
                )
            }
        }
    }

        // Fast Alphabet Scrubber Jump Ribbon
        if (filteredTracks.size > 5 && !isMultiSelectMode) {
            AlphabetScrubber(
                modifier = Modifier.align(Alignment.CenterEnd),
                onLetterSelected = { char ->
                    val targetIdx = filteredTracks.indexOfFirst { track ->
                        val initial = track.title.trim().firstOrNull()?.uppercaseChar() ?: '#'
                        if (char == '#') !initial.isLetter() else initial == char
                    }
                    if (targetIdx >= 0) {
                        // Offset by header items: header (0), status (1), search (2), pockets (3), quick shelf (4), filter chips (5), scope summary (6 or 7)
                        val headerOffset = (listState.layoutInfo.totalItemsCount - sortedTracks.size).coerceAtLeast(0)
                        coroutineScope.launch {
                            listState.scrollToItem(headerOffset + targetIdx)
                        }
                    }
                }
            )
        }

        if (isMultiSelectMode && selectedTrackIds.isNotEmpty()) {
            val selectedTracksList = remember(selectedTrackIds, filteredTracks) {
                filteredTracks.filter { it.id in selectedTrackIds }
            }
            BatchSelectionActionBar(
                selectedCount = selectedTrackIds.size,
                onPlayNext = {
                    onPlayBatchNext(selectedTracksList)
                    isMultiSelectMode = false
                    selectedTrackIds = emptySet()
                },
                onAddToQueue = {
                    onAddBatchToQueue(selectedTracksList)
                    isMultiSelectMode = false
                    selectedTrackIds = emptySet()
                },
                onDownloadAll = {
                    onBatchDownloadTracks(selectedTracksList)
                    isMultiSelectMode = false
                    selectedTrackIds = emptySet()
                },
                onFavoriteAll = {
                    onBatchToggleFavorites(selectedTracksList)
                    isMultiSelectMode = false
                    selectedTrackIds = emptySet()
                },
                onClearSelection = {
                    isMultiSelectMode = false
                    selectedTrackIds = emptySet()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp)
                    .zIndex(10f)
            )
        }
    }
}

@Composable
internal fun LibraryQuickActionShelf(
    playableTracks: List<Track>,
    favoriteTracks: List<Track>,
    queuedTracks: List<Track>,
    losslessTracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onShowFavorites: () -> Unit,
    onShowLossless: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSearch: () -> Unit
) {
    HomeSectionHeader(title = "Start From Library", subtitle = "Fast routes before the full catalog list.")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            QuickStartTile(
                title = "Shuffle All",
                subtitle = "${playableTracks.size} ready",
                enabled = playableTracks.isNotEmpty(),
                onClick = { playableTracks.shuffled().firstOrNull()?.let(onPlayTrack) }
            )
        }
        item {
            QuickStartTile(
                title = "Favorites",
                subtitle = "${favoriteTracks.size} saved",
                enabled = favoriteTracks.isNotEmpty(),
                onClick = onShowFavorites
            )
        }
        item {
            QuickStartTile(
                title = "Lossless",
                subtitle = "${losslessTracks.size} tracks",
                enabled = losslessTracks.isNotEmpty(),
                onClick = onShowLossless
            )
        }
        item {
            QuickStartTile(
                title = "Queue",
                subtitle = "${queuedTracks.size} lined",
                enabled = true,
                onClick = onOpenQueue
            )
        }
        item {
            QuickStartTile(
                title = "Search",
                subtitle = "host + phone",
                enabled = true,
                onClick = onOpenSearch
            )
        }
    }
}

@Composable
internal fun CollectionPreviewCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    selected: Boolean,
    tracks: List<Track> = emptyList(),
    note: String = "",
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CoverCard(
            title = title,
            caption = subtitle,
            note = note,
            artworkUrl = artworkUrl,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick
        )
        if (tracks.size >= 2) {
        }
    }
}
