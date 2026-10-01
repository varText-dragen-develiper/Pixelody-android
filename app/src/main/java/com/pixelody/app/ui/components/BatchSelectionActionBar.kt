package com.pixelody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BatchSelectionActionBar(
    selectedCount: Int,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownloadAll: () -> Unit,
    onFavoriteAll: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Count badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "$selectedCount selected",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Quick Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BatchActionButton(
                    glyph = TransportGlyphType.PlayNext,
                    label = "Next",
                    onClick = {
                        haptic.performConfirm()
                        onPlayNext()
                    }
                )
                BatchActionButton(
                    glyph = TransportGlyphType.AddToQueue,
                    label = "Queue",
                    onClick = {
                        haptic.performTick()
                        onAddToQueue()
                    }
                )
                BatchActionButton(
                    glyph = TransportGlyphType.Download,
                    label = "Save",
                    onClick = {
                        haptic.performTick()
                        onDownloadAll()
                    }
                )
                BatchActionButton(
                    glyph = TransportGlyphType.HeartFilled,
                    label = "Fav",
                    onClick = {
                        haptic.performTick()
                        onFavoriteAll()
                    }
                )
                BatchActionButton(
                    glyph = TransportGlyphType.Close,
                    label = null,
                    onClick = {
                        haptic.performTick()
                        onClearSelection()
                    }
                )
            }
        }
    }
}

@Composable
private fun BatchActionButton(
    glyph: TransportGlyphType,
    label: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PixelodyTransportGlyph(
                glyph = glyph,
                color = MaterialTheme.colorScheme.primary,
                size = 14.dp
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
