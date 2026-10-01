package com.pixelody.app.feature.baselayer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import com.pixelody.app.ui.components.InteractiveFavoriteHeart
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.performTick
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.pixelody.app.R
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import androidx.compose.ui.unit.dp

/**
 * Library and the collection you opened, as content bands only.
 *
 * Play and Shuffle are deliberately not in this file. They belong to the scaffold's
 * surface-action band at the bottom of the screen, because they are tapped every
 * time and the top of a 832dp screen is the worst place a right thumb can reach.
 * Keeping them out of the content keeps that from quietly drifting back.
 */

@Composable
fun BaseLibraryContent(
    data: BaseLayerData,
    shape: BaseBrowseShape,
    onSelectShape: (BaseBrowseShape) -> Unit,
    onOpenCollection: (BaseCollection) -> Unit,
    onPlayFrom: (List<String>) -> Unit,
    onTrackActions: (String) -> Unit,
    onCollectionActions: (BaseCollection) -> Unit,
    onRetryHost: () -> Unit,
    onPhoneOnly: () -> Unit,
    onOpenTools: () -> Unit,
    onToggleFavorite: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {

        if (!data.hostReachable) {
            item {
                BaseFaultStrip(
                    onRetry = onRetryHost,
                    onPhoneOnly = onPhoneOnly,
                    onOpenTools = onOpenTools
                )
            }
        }

        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 10.dp
                )
            ) {
                items(BaseBrowseShape.values().toList()) { candidate ->
                    FilterChip(
                        selected = shape == candidate,
                        onClick = { onSelectShape(candidate) },
                        label = { Text(candidate.label) }
                    )
                }
            }
        }

        if (shape == BaseBrowseShape.Tracks) {
            val visible = data.visibleTracks().map { it.id }
            if (visible.isEmpty()) {
                item { BaseEmptyNote("Nothing in ${data.lensLabel}.") }
            }
            itemsIndexed(visible, key = { _, id -> id }) { index, id ->
                BaseTrackRow(
                    data = data,
                    trackId = id,
                    onPlay = { onPlayFrom(visible.drop(index)) },
                    onActions = { onTrackActions(id) },
                    onToggleFavorite = onToggleFavorite?.let { cb -> { cb(id) } }
                )
            }
        } else {
            val collections = data.collectionsOfKind(shape.kindKey)
            if (collections.isEmpty()) {
                item { BaseEmptyNote("No ${shape.label.lowercase()} yet.") }
            }
            items(collections, key = { it.id }) { collection ->
                BaseCollectionRow(
                    data = data,
                    collection = collection,
                    onOpen = { onOpenCollection(collection) },
                    onActions = { onCollectionActions(collection) }
                )
            }
        }
    }
}

/**
 * H3: an opened thing states what it is called and what kind of thing it is, above
 * the fold, so it is never mistakable for a filtered list.
 */
@Composable
fun BaseCollectionContent(
    data: BaseLayerData,
    collection: BaseCollection,
    onPlayFrom: (List<String>) -> Unit,
    onTrackActions: (String) -> Unit,
    onToggleFavorite: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val visible = data.visibleTrackIds(collection.trackIds)
    val unavailable = collection.trackIds.count { !data.isPlayable(it) }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BaseBadge(text = initialsOf(collection.name), size = 64)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = collection.kindKey.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = countLabel(visible.size, unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            item { BaseEmptyNote("Nothing here in ${data.lensLabel}.") }
        }

        itemsIndexed(visible, key = { _, id -> id }) { index, id ->
            BaseTrackRow(
                data = data,
                trackId = id,
                onPlay = { onPlayFrom(visible.drop(index)) },
                onActions = { onTrackActions(id) },
                onToggleFavorite = onToggleFavorite?.let { cb -> { cb(id) } }
            )
        }
    }
}

private fun countLabel(visible: Int, unavailable: Int): String {
    val songs = if (visible == 1) "1 song" else "$visible songs"
    return if (unavailable > 0) "$songs · $unavailable unavailable" else songs
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaseTrackRow(
    data: BaseLayerData,
    trackId: String,
    onPlay: () -> Unit,
    onActions: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val track = data.track(trackId) ?: return
    val playable = data.isPlayable(trackId)
    val current = data.currentTrackId == trackId
    val source = data.sourceOf(trackId)
    val haptic = LocalHapticFeedback.current
    val theme = com.pixelody.app.ui.theme.LocalPixelodyThemeVariant.current

    val itemData = com.pixelody.app.ui.theme.units.TrackItemData(
        id = track.id,
        title = track.title,
        artist = track.artist,
        album = track.album,
        durationSeconds = track.durationSeconds,
        artworkUrl = track.artworkUrl,
        isLossless = track.lossless,
        isFavorite = track.favorite,
        isMissing = !playable,
        isSelected = current,
        isPlaying = current && data.isPlaying,
        levelLabel = if (track.lossless) "FLAC" else "ROM",
        onClick = { if (playable) onPlay() else onActions() },
        onLongClick = onActions,
        onToggleFavorite = onToggleFavorite
    )
    com.pixelody.app.ui.theme.units.PixelodyTrackItem(
        data = itemData,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaseCollectionRow(
    data: BaseLayerData,
    collection: BaseCollection,
    onOpen: () -> Unit,
    onActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 64.dp)
            .combinedClickable(
                onClickLabel = "Open ${collection.name}",
                onLongClickLabel = "Actions for ${collection.name}",
                role = Role.Button,
                onLongClick = onActions,
                onClick = onOpen
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BaseBadge(text = initialsOf(collection.name), size = 44)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = countLabel(data.visibleTrackIds(collection.trackIds).size, 0),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    onClickLabel = "Actions for ${collection.name}",
                    role = Role.Button
                ) {
                    haptic.performTick()
                    onActions()
                },
            contentAlignment = Alignment.Center
        ) {
            PixelodyTransportGlyph(
                glyph = TransportGlyphType.MoreVertical,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                sizeDp = 14
            )
        }
    }
}

/**
 * A fault shows up in place of the music it took away, with the recovery attached.
 * No error destination, no modal — J06 in its entirety.
 */
@Composable
fun BaseFaultStrip(
    onRetry: () -> Unit,
    onPhoneOnly: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Host is not responding",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Music on this phone still plays. Host tracks stay listed so you can see what is missing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Try again") }
                OutlinedButton(onClick = onPhoneOnly, modifier = Modifier.weight(1f)) {
                    Text("Phone only")
                }
                OutlinedButton(onClick = onOpenTools, modifier = Modifier.weight(1f)) {
                    Text("Tools")
                }
            }
        }
    }
}

@Composable
fun BaseEmptyNote(text: String, modifier: Modifier = Modifier) {
    val theme = LocalPixelodyThemeVariant.current
    val lower = text.lowercase()
    val illustrationRes = when {
        lower.contains("offline") || lower.contains("unplugged") || lower.contains("connect") -> R.drawable.iso_terminal_unplugged
        lower.contains("search") || lower.contains("find") -> R.drawable.iso_oscilloscope_search
        lower.contains("crate") || lower.contains("favorite") || lower.contains("playlist") -> R.drawable.iso_cassette_favorites
        theme == PixelodyMobileTheme.BulkheadTerminal -> R.drawable.iso_terminal_unplugged
        theme == PixelodyMobileTheme.CartridgeQuest -> R.drawable.iso_cassette_favorites
        theme == PixelodyMobileTheme.LoFiCafe -> R.drawable.iso_cassette_favorites
        else -> R.drawable.iso_minidisc_offline
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = illustrationRes),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 14.dp),
            alpha = 0.88f
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BaseBadge(text: String, size: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(size.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = if (size >= 44) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.labelSmall
                },
                maxLines = 1
            )
        }
    }
}

fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return "·"
    val first = words[0].first().uppercaseChar()
    val second = words.getOrNull(1)?.first()?.uppercaseChar()
    return if (second != null) "$first$second" else first.toString()
}
