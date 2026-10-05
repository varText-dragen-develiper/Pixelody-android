package com.pixelody.app.ui.theme.units

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.theme.compactSurfaceShape
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontFamily

/**
 * Baseline ThemeUnitKit implementation for Pixelody Studio.
 * Shared rounded surfaces and restrained accents follow the current desktop Studio.
 */
object StudioThemeUnitKit : ThemeUnitKit {
    override val themeId: String = "studio"

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun TrackItem(
        data: TrackItemData,
        modifier: Modifier
    ) {
        val haptic = LocalHapticFeedback.current
        val theme = LocalPixelodyThemeVariant.current
        Card(
            shape = theme.compactSurfaceShape(),
            border = BorderStroke(
                1.dp,
                if (data.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (data.isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            modifier = modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 72.dp)
                .semantics {
                    contentDescription = "${data.title}, ${data.artist}"
                    stateDescription = when {
                        data.isSelected -> "Current track"
                        data.isMissing -> "Unavailable"
                        else -> "Playable"
                    }
                }
                .combinedClickable(
                    onClickLabel = if (data.isMissing) "Show unavailable track ${data.title}" else "Play ${data.title}",
                    role = Role.Button,
                    onClick = {
                        haptic.performTick()
                        data.onClick()
                    },
                    onLongClick = data.onLongClick
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            when {
                                data.isPlaying -> MaterialTheme.colorScheme.primary
                                data.isSelected -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                RemoteArtwork(
                    artworkUrl = data.artworkUrl,
                    title = data.title,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (data.artist.isNotBlank()) data.artist else "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (data.isLossless) {
                    PixelodyTransportGlyph(TransportGlyphType.DiamondLossless,
                        modifier = Modifier.semantics { contentDescription = "Lossless" },
                        color = MaterialTheme.colorScheme.onSurface, sizeDp = 16)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (data.durationSeconds > 0) {
                    val minutes = data.durationSeconds / 60
                    val seconds = data.durationSeconds % 60
                    Text(
                        text = "%d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    override fun TransportControls(
        data: TransportData,
        modifier: Modifier
    ) {
        val haptic = LocalHapticFeedback.current
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    haptic.performTick()
                    data.onSkipPrevious()
                },
                enabled = data.canSkipPrevious,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Previous track" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(TransportGlyphType.Previous, color = MaterialTheme.colorScheme.onSurface, sizeDp = 24)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                onClick = {
                    haptic.performTick()
                    data.onPlayPause()
                },
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier
                    .size(64.dp)
                    .semantics { contentDescription = if (data.isPlaying) "Pause" else "Play" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(if (data.isPlaying) TransportGlyphType.Pause else TransportGlyphType.Play,
                        color = MaterialTheme.colorScheme.onPrimary, sizeDp = 28)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                onClick = {
                    haptic.performTick()
                    data.onSkipNext()
                },
                enabled = data.canSkipNext,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Next track" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(TransportGlyphType.Next, color = MaterialTheme.colorScheme.onSurface, sizeDp = 24)
                }
            }
        }
    }

    @Composable
    override fun HeroStage(
        data: HeroStageData,
        modifier: Modifier
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)), MaterialTheme.shapes.medium)
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (data.badge.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = data.badge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (data.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                }
                if (data.primaryActionLabel != null || data.secondaryActionLabel != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (data.primaryActionLabel != null) {
                            Button(
                                onClick = { data.onPrimaryAction?.invoke() },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(data.primaryActionLabel)
                            }
                        }
                        if (data.secondaryActionLabel != null) {
                            OutlinedButton(
                                onClick = { data.onSecondaryAction?.invoke() },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(data.secondaryActionLabel)
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun QuickStartTile(
        data: QuickTileData,
        modifier: Modifier
    ) {
        val haptic = LocalHapticFeedback.current
        Surface(
            modifier = modifier
                .combinedClickable(
                    enabled = data.enabled,
                    role = Role.Button,
                    onClick = {
                        haptic.performTick()
                        data.onClick()
                    },
                    onLongClick = data.onLongClick
                ),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (data.artworkUrl != null) {
                    RemoteArtwork(
                        artworkUrl = data.artworkUrl,
                        title = data.title,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (data.glyph != null) {
                    val gColor = data.glyphColor ?: MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(gColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = data.glyph,
                            color = gColor,
                            size = 16.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
