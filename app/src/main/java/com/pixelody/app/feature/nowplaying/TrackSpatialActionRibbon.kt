package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.performConfirm

@Composable
internal fun TrackSpatialActionRibbon(
    track: Track,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onStartFlowRadio: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* prevent dismiss click inside */ },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Track Identity
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RemoteArtwork(
                        artworkUrl = track.artworkUrl,
                        title = track.title,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artist} • ${track.format.uppercase()} ${if (track.lossless) "(Lossless)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Close,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 12.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Actions Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RibbonActionCard(
                        glyph = TransportGlyphType.PlayNext,
                        title = "Play Next",
                        subtitle = "Up next in queue",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performConfirm()
                            onPlayNext()
                        }
                    )
                    RibbonActionCard(
                        glyph = TransportGlyphType.AddToQueue,
                        title = "Add to Queue",
                        subtitle = "Append to end",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performConfirm()
                            onAddToQueue()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RibbonActionCard(
                        glyph = TransportGlyphType.FlowShuffle,
                        title = "Smart Flow",
                        subtitle = "Radio from this song",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performConfirm()
                            onStartFlowRadio()
                        }
                    )
                    RibbonActionCard(
                        glyph = if (isFavorite) TransportGlyphType.HeartFilled else TransportGlyphType.Heart,
                        glyphColor = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        title = if (isFavorite) "Favorited" else "Favorite",
                        subtitle = if (isFavorite) "In Favorites" else "Save to favorites",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performConfirm()
                            onToggleFavorite()
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun RibbonActionCard(
    glyph: TransportGlyphType,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    glyphColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelodyTransportGlyph(
                glyph = glyph,
                color = glyphColor,
                size = 18.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
