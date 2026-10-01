package com.pixelody.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Cover-first building blocks. The picture is what a person recognises, so it comes
 * first and is large; the text is one small, light line that confirms what the
 * picture already said. Colours come from the active theme, so every theme gets the
 * same layout in its own palette.
 *
 * Anything with an image is long-pressable, because long press is where a person
 * changes that image or adds a note.
 */

@Composable
internal fun CoverSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

/** A wide, short tile: cover on the left, one line of text. Used for the quick-pick grid. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CoverPill(
    title: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClickLabel = "Open $title",
                onLongClickLabel = "Change picture or add a note for $title",
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick?.let { callback ->
                    {
                        haptic.performTick()
                        callback()
                    }
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier
                    .size(44.dp),
                contentAlignment = Alignment.Center
            ) { leading() }
        } else {
            RemoteArtwork(
                artworkUrl = artworkUrl,
                title = title,
                modifier = Modifier.size(52.dp)
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** A square (or round, for artists) cover with one small caption underneath. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CoverCard(
    title: String,
    caption: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    note: String = "",
    round: Boolean = false,
    selected: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .width(120.dp)
            .combinedClickable(
                onClickLabel = "Open $title",
                onLongClickLabel = "Change picture or add a note for $title",
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick?.let { callback ->
                    {
                        haptic.performTick()
                        callback()
                    }
                }
            ),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        val artShape = if (round) CircleShape else RoundedCornerShape(6.dp)
        RemoteArtwork(
            artworkUrl = artworkUrl,
            title = title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(artShape)
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, artShape)
                    else Modifier
                )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
        val line = note.ifBlank { caption }
        if (line.isNotBlank()) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/** A track row led by a 48dp cover. The playing track is marked by colour and a dot, not by weight. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CoverTrackRow(
    title: String,
    artist: String,
    artworkUrl: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    note: String = "",
    trailing: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = "Play $title",
                onLongClickLabel = "Options for $title",
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick?.let { callback ->
                    {
                        haptic.performTick()
                        callback()
                    }
                }
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteArtwork(
            artworkUrl = artworkUrl,
            title = title,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) accent else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = note.ifBlank { artist.ifBlank { "Unknown artist" } },
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (trailing != null) trailing()
    }
}
