package com.pixelody.app.feature.search

import com.pixelody.app.data.model.CoverBook
import com.pixelody.app.data.model.collectionCoverKey
import com.pixelody.app.data.model.trackCoverKey
import com.pixelody.app.data.model.withCover
import com.pixelody.app.ui.components.CoverCard
import com.pixelody.app.ui.components.CoverTrackRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pixelody.app.ui.theme.compactSurfaceShape
import androidx.compose.ui.unit.dp
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.Track
import com.pixelody.app.feature.library.CollectionPreviewCard
import com.pixelody.app.feature.library.CollectionShortcut
import com.pixelody.app.ui.components.CompactTrackPill
import com.pixelody.app.ui.components.HarmonicDiggingRingLens
import com.pixelody.app.ui.components.HarmonicFilterMode
import com.pixelody.app.ui.components.HarmonicTrajectorySparkline
import com.pixelody.app.ui.components.getCompatibleCamelotKeyCodes
import com.pixelody.app.ui.components.HomeSectionHeader
import com.pixelody.app.ui.components.QuickStartTile
import com.pixelody.app.ui.components.ScreenHeader
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.SourceScope
import com.pixelody.app.ui.components.TrackRow
import com.pixelody.app.ui.components.TriSourcePivotBar
import com.pixelody.app.ui.components.filterTracksBySource
import com.pixelody.app.ui.navigation.PixelodyStateTags
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import java.util.Locale

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import com.pixelody.app.ui.components.BatchSelectionActionBar
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick

/* =========================================================================
 * Slice 4 — Search Screen
 * Extracted from PixelodyShell.kt into com.pixelody.app.feature.search.
 * ========================================================================= */

@Composable
internal fun SearchScreen(
    snapshot: LibrarySnapshot?,
    localTracks: List<Track>,
    selectedTrack: Track?,
    onPlayTrack: (Track) -> Unit,
    onOpenHome: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCollection: (String, String) -> Unit = { _, _ -> },
    onOpenQueue: () -> Unit,
    onOpenDevice: () -> Unit,
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
    onLongClickScope: (SourceScope) -> Unit = {},
    onConnectDesktop: () -> Unit = {},
    onStartJam: () -> Unit = {},
    onLongClickQuickStart: (String) -> Unit = {},
    initialQuery: String = "",
    onInitialQueryConsumed: () -> Unit = {},
    sourceScope: SourceScope = SourceScope.All,
    onSourceScopeChange: (SourceScope) -> Unit = {},
    onShowDoc: ((String) -> Unit)? = null,
    experienceMode: com.pixelody.app.data.model.AppExperienceMode = com.pixelody.app.data.model.AppExperienceMode.Essential,
    covers: CoverBook = CoverBook(),
    onCoverActions: (String, String) -> Unit = { _, _ -> }
) {
    val haptic = LocalHapticFeedback.current
    val theme = LocalPixelodyThemeVariant.current
    var query by remember { mutableStateOf("") }
    var intentFilter by rememberSaveable { mutableStateOf("Everything") }
    var harmonicBaseKey by remember { mutableStateOf<CamelotKey?>(null) }
    var harmonicFilterMode by remember { mutableStateOf(HarmonicFilterMode.StrictAdjacent) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            query = initialQuery
            onInitialQueryConsumed()
        }
    }
    val hostTracks = snapshot?.tracks.orEmpty()
    val localTrackIds = remember(localTracks) { localTracks.map { it.id }.toSet() }
    val jamTrackIds = remember(snapshot) { snapshot?.queue?.trackIds.orEmpty().toSet() }
    val trackById = remember(hostTracks, localTracks) { (hostTracks + localTracks).associateBy { it.id } }
    val favoriteIds = remember(snapshot, hostTracks, localTracks) {
        (snapshot?.favorites.orEmpty() + hostTracks.filter { it.favorite }.map { it.id } + localTracks.filter { it.favorite }.map { it.id }).toSet()
    }
    val queuedIds = remember(snapshot) { snapshot?.queue?.trackIds.orEmpty().toSet() }
    val favoriteTracks = remember(hostTracks, localTracks, favoriteIds) {
        (hostTracks + localTracks).filter { it.id in favoriteIds || it.favorite }
    }
    val queuedTracks = remember(snapshot, trackById) {
        snapshot?.queue?.trackIds.orEmpty().mapNotNull { trackById[it] }
    }
    val playableTracks = remember(hostTracks, localTracks) {
        (hostTracks + localTracks).filterNot { it.missing || it.streamUrl.isBlank() }
    }
    val losslessTracks = remember(playableTracks) { playableTracks.filter { it.lossless } }
    val suggestedTracks = remember(selectedTrack, queuedTracks, favoriteTracks, localTracks, hostTracks) {
        (listOfNotNull(selectedTrack) + queuedTracks + favoriteTracks + localTracks + hostTracks)
            .filterNot { it.missing || it.streamUrl.isBlank() }
            .distinctBy { it.id }
            .take(12)
    }
    val albumShortcuts = remember(playableTracks) {
        playableTracks
            .filter { it.album.isNotBlank() }
            .groupBy { it.album }
            .toList()
            .sortedWith(compareByDescending<Pair<String, List<Track>>> { it.second.size }.thenBy { it.first })
            .take(8)
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
            .take(8)
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
    val combinedTracks = remember(hostTracks, localTracks, query, sourceScope, intentFilter, favoriteIds, queuedIds, harmonicBaseKey, harmonicFilterMode) {
        val needle = query.trim().lowercase(Locale.US)
        val allTracks = hostTracks + localTracks
        val sourceTracks = filterTracksBySource(allTracks, sourceScope, localTrackIds, jamTrackIds)
        val intentTracks = when (intentFilter) {
            "Playable" -> sourceTracks.filterNot { it.missing || it.streamUrl.isBlank() }
            "Favorites" -> sourceTracks.filter { it.id in favoriteIds || it.favorite }
            "Lossless" -> sourceTracks.filter { it.lossless }
            "Queued" -> sourceTracks.filter { it.id in queuedIds }
            else -> sourceTracks
        }
        val compatibleKeys = harmonicBaseKey?.let { getCompatibleCamelotKeyCodes(it, harmonicFilterMode) }
        val harmonicFiltered = if (compatibleKeys == null) intentTracks else {
            intentTracks.filter { track ->
                val trackKeyCode = HarmonicKeyEngine.estimateTrackTelemetry(track).key.code
                trackKeyCode in compatibleKeys
            }
        }
        if (needle.isBlank()) harmonicFiltered else harmonicFiltered.filter { track ->
            listOf(track.title, track.artist, track.album, track.codec, track.format)
                .any { it.lowercase(Locale.US).contains(needle) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if (isMultiSelectMode) 140.dp else 24.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Search",
                    subtitle = "Songs, artists, albums."
                )
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    shape = theme.compactSurfaceShape(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PixelodyStateTags.SEARCH_QUERY_FIELD),
                    label = { Text("Search music") },
                    singleLine = true
                )
            }
            item {
                TriSourcePivotBar(
                    selectedScope = sourceScope,
                    onSelectScope = onSourceScopeChange,
                    localCount = localTracks.size,
                    hostConnected = snapshot != null,
                    hostCount = hostTracks.size,
                    jamActive = snapshot?.queue != null,
                    jamCount = queuedTracks.size,
                    onLongClickScope = onLongClickScope,
                    onConnectDesktop = onConnectDesktop,
                    onStartJam = onStartJam
                )
            }
            item {
                LazyRow(
                    modifier = Modifier.testTag(PixelodyStateTags.SEARCH_INTENT_FILTER),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("Everything", "Playable", "Favorites", "Lossless", "Queued")) { filter ->
                        FilterChip(
                            selected = intentFilter == filter,
                            onClick = { intentFilter = filter },
                            shape = theme.compactSurfaceShape(),
                            label = { Text(filter) }
                        )
                    }
                }
            }
            if (experienceMode == com.pixelody.app.data.model.AppExperienceMode.Studio) item {
                HarmonicDiggingRingLens(
                    currentTrack = selectedTrack,
                    selectedBaseKey = harmonicBaseKey,
                    filterMode = harmonicFilterMode,
                    onSelectBaseKey = { harmonicBaseKey = it },
                    onSelectFilterMode = { harmonicFilterMode = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onShowDoc = onShowDoc
                )
            }
            if (query.isBlank() && experienceMode == com.pixelody.app.data.model.AppExperienceMode.Studio) {
                item {
                    HomeSectionHeader(title = "Quick Paths", subtitle = "Resume, narrow the mood, or jump to the right source.")
                    SearchQuickActionShelf(
                        selectedTrack = selectedTrack,
                        playableTracks = playableTracks,
                        favoriteTracks = favoriteTracks,
                        queuedTracks = queuedTracks,
                        losslessTracks = losslessTracks,
                        localTracks = localTracks,
                        onPlayTrack = onPlayTrack,
                        onOpenLibrary = onOpenLibrary,
                        onOpenQueue = onOpenQueue,
                        onOpenDevice = onOpenDevice,
                        onLongClickCategory = onLongClickQuickStart
                    )
                }
            }
            if (query.isBlank() && suggestedTracks.isNotEmpty()) {
                item {
                    HomeSectionHeader(title = "Start here", subtitle = "")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestedTracks, key = { it.id }) { track ->
                            val shown = track.withCover(covers)
                            CoverCard(
                                title = shown.title,
                                caption = shown.artist,
                                note = covers.noteFor(trackCoverKey(track.id)),
                                artworkUrl = shown.artworkUrl,
                                selected = selectedTrack?.id == track.id,
                                onClick = { onPlayTrack(track) },
                                onLongClick = { onCoverActions(trackCoverKey(track.id), track.title) }
                            )
                        }
                    }
                }
            }
            if (query.isBlank() && (albumShortcuts.isNotEmpty() || artistShortcuts.isNotEmpty())) {
                item {
                    HomeSectionHeader(title = "Browse", subtitle = "")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(albumShortcuts + artistShortcuts, key = { it.id }) { shortcut ->
                            val coverKey = collectionCoverKey(shortcut.id)
                            CollectionPreviewCard(
                                title = shortcut.title,
                                subtitle = shortcut.subtitle,
                                artworkUrl = covers.imageFor(coverKey, shortcut.artworkUrl),
                                note = covers.noteFor(coverKey),
                                onLongClick = { onCoverActions(coverKey, shortcut.title) },
                                selected = false,
                                tracks = shortcut.tracks,
                                onClick = {
                                    val kindKey = if (shortcut.id.startsWith("album:")) "album" else "artist"
                                    onOpenCollection(kindKey, shortcut.id)
                                }
                            )
                        }
                    }
                }
            }
            if (combinedTracks.isEmpty()) {
                item {
                    SectionCard(
                        title = "No Music Found",
                        subtitle = if (hostTracks.isEmpty() && localTracks.isEmpty()) {
                            "Connect a host or add files from this phone."
                        } else {
                            "Try another title, artist, album, codec, or source."
                        }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onOpenHome) { Text("Home") }
                            OutlinedButton(onClick = onOpenDevice) { Text("Phone Files") }
                        }
                    }
                }
                return@LazyColumn
            }
            item {
                HomeSectionHeader(
                    title = "Results",
                    subtitle = "${combinedTracks.size} match${if (combinedTracks.size == 1) "" else "es"} across ${sourceScope.label.lowercase(Locale.US)} music."
                )
            }
            if (combinedTracks.size >= 2) {
                item {
                    HarmonicTrajectorySparkline(
                        tracks = combinedTracks,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            items(combinedTracks, key = { "${it.id}-${it.streamUrl}" }) { track ->
                val isSelectedInBatch = selectedTrackIds.contains(track.id)
                val shown = track.withCover(covers)
                CoverTrackRow(
                    title = shown.title,
                    artist = if (shown.lossless) "${shown.artist} · FLAC" else shown.artist,
                    note = covers.noteFor(trackCoverKey(track.id)),
                    artworkUrl = shown.artworkUrl,
                    selected = if (isMultiSelectMode) isSelectedInBatch else selectedTrack?.id == track.id,
                    onClick = {
                        if (isMultiSelectMode) {
                            haptic.performTick()
                            selectedTrackIds = if (isSelectedInBatch) selectedTrackIds - track.id else selectedTrackIds + track.id
                            if (selectedTrackIds.isEmpty()) isMultiSelectMode = false
                        } else {
                            onPlayTrack(track)
                        }
                    },
                    onLongClick = {
                        haptic.performConfirm()
                        isMultiSelectMode = true
                        selectedTrackIds = selectedTrackIds + track.id
                    }
                )
            }
        }

        if (isMultiSelectMode && selectedTrackIds.isNotEmpty()) {
            val selectedTracksList = remember(selectedTrackIds, combinedTracks) {
                combinedTracks.filter { it.id in selectedTrackIds }
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
internal fun SearchQuickActionShelf(
    selectedTrack: Track?,
    playableTracks: List<Track>,
    favoriteTracks: List<Track>,
    queuedTracks: List<Track>,
    losslessTracks: List<Track>,
    localTracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenDevice: () -> Unit,
    onLongClickCategory: ((String) -> Unit)? = null
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            QuickStartTile(
                title = "Resume",
                subtitle = selectedTrack?.title ?: "nothing active",
                enabled = selectedTrack != null,
                onClick = { selectedTrack?.let(onPlayTrack) },
                onLongClick = { onLongClickCategory?.invoke("Resume") }
            )
        }
        item {
            QuickStartTile(
                title = "Play Any",
                subtitle = "${playableTracks.size} ready",
                enabled = playableTracks.isNotEmpty(),
                onClick = { playableTracks.firstOrNull()?.let(onPlayTrack) },
                onLongClick = { onLongClickCategory?.invoke("Play Any") }
            )
        }
        item {
            QuickStartTile(
                title = "Favorites",
                subtitle = "${favoriteTracks.size} saved",
                enabled = favoriteTracks.isNotEmpty(),
                onClick = { favoriteTracks.firstOrNull()?.let(onPlayTrack) },
                onLongClick = { onLongClickCategory?.invoke("Favorites") }
            )
        }
        item {
            QuickStartTile(
                title = "Lossless",
                subtitle = "${losslessTracks.size} tracks",
                enabled = losslessTracks.isNotEmpty(),
                onClick = { losslessTracks.firstOrNull()?.let(onPlayTrack) },
                onLongClick = { onLongClickCategory?.invoke("Lossless") }
            )
        }
        item {
            QuickStartTile(
                title = "Queue",
                subtitle = "${queuedTracks.size} up next",
                enabled = true,
                onClick = onOpenQueue,
                onLongClick = { onLongClickCategory?.invoke("Queue") }
            )
        }
        item {
            QuickStartTile(
                title = "Library",
                subtitle = "browse host",
                enabled = true,
                onClick = onOpenLibrary,
                onLongClick = { onLongClickCategory?.invoke("Library") }
            )
        }
        item {
            QuickStartTile(
                title = "Phone",
                subtitle = "${localTracks.size} files",
                enabled = true,
                onClick = onOpenDevice,
                onLongClick = { onLongClickCategory?.invoke("Phone") }
            )
        }
    }
}
