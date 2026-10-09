package com.pixelody.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.data.storage.OfflineMediaStore
import com.pixelody.app.feature.connection.label
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import java.util.Locale

@Composable
internal fun StorageSection(activeTheme: PixelodyMobileTheme, cachedTrackCount: Int, offlineMediaStore: OfflineMediaStore, settingsStore: MobileSettingsStore, onClearOfflineCache: () -> Unit, onPruneUnpinned: () -> Unit, onClearArtwork: () -> Unit) {
    var selectedQuotaBytes by remember { mutableStateOf(settingsStore.loadStorageQuotaBytes()) }
    var autoCacheFavs by remember { mutableStateOf(settingsStore.loadAutoCacheFavorites()) }
    var autoCacheRecent by remember { mutableStateOf(settingsStore.loadAutoCacheRecentCount() > 0) }
    var wifiOnly by remember { mutableStateOf(settingsStore.loadDownloadWifiOnly()) }
    val haptic = LocalHapticFeedback.current

    val audioBytes = remember(cachedTrackCount) { offlineMediaStore.audioCacheSizeBytes() }
    val artBytes = remember(cachedTrackCount) { offlineMediaStore.artCacheSizeBytes() }
    val totalCacheBytes = remember(cachedTrackCount) { offlineMediaStore.totalCacheSizeBytes() }
    val freeDiskBytes = remember(cachedTrackCount) { offlineMediaStore.freeDiskSpaceBytes() }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f GB", bytes / (1024f * 1024f * 1024f))
            bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }

    // Offline Storage Management Section
    Column {
        SectionCard(
            title = "Offline Storage & Cache",
            subtitle = "$cachedTrackCount cached track${if (cachedTrackCount == 1) "" else "s"} • ${formatBytes(totalCacheBytes)} used"
        ) {
            // Storage Bar Breakdown
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Storage Allocation",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val totalDenom = (totalCacheBytes + freeDiskBytes).coerceAtLeast(1L).toFloat()
                val audioWeight = (audioBytes.toFloat() / totalDenom).coerceIn(0.01f, 1f)
                val artWeight = (artBytes.toFloat() / totalDenom).coerceIn(0.005f, 1f)
                val freeWeight = (freeDiskBytes.toFloat() / totalDenom).coerceIn(0.01f, 1f)

                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                        .weight(audioWeight)
                        .fillMaxHeight()
                        .background(Color(0xFF10B981))
                    )
                    Box(
                        modifier = Modifier
                        .weight(artWeight)
                        .fillMaxHeight()
                        .background(Color(0xFF06B6D4))
                    )
                    Box(
                        modifier = Modifier
                        .weight(freeWeight)
                        .fillMaxHeight()
                        .background(Color(0xFF64748B))
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Audio: ${formatBytes(audioBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "Art: ${formatBytes(artBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF06B6D4)
                    )
                    Text(
                        text = "Free: ${formatBytes(freeDiskBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // Storage Quota Selector
            Text(
                text = "Storage Quota Limit (LRU Auto-Eviction)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "When quota is reached, least recently played unpinned tracks are pruned automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            val quotaOptions = listOf(
                "1 GB" to 1L * 1024L * 1024L * 1024L,
                "2 GB" to 2L * 1024L * 1024L * 1024L,
                "5 GB" to 5L * 1024L * 1024L * 1024L,
                "10 GB" to 10L * 1024L * 1024L * 1024L,
                "Max" to MobileSettingsStore.UNLIMITED_QUOTA_BYTES
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quotaOptions) { (label, bytes) ->
                    val isSelected = selectedQuotaBytes == bytes
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedQuotaBytes = bytes
                            settingsStore.saveStorageQuotaBytes(bytes)
                            haptic.performTick()
                        },
                        shape = activeTheme.plate,
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // Auto-Cache Policies
            Text(
                text = "Smart Offline Sync Policies",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Auto-Cache Favorites", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Download newly favorited tracks for offline playback",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoCacheFavs,
                    onCheckedChange = { checked ->
                        autoCacheFavs = checked
                        settingsStore.saveAutoCacheFavorites(checked)
                        haptic.performTick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Auto-Cache Last 20 Played", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Keep recently streamed tracks in local cache",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoCacheRecent,
                    onCheckedChange = { checked ->
                        autoCacheRecent = checked
                        settingsStore.saveAutoCacheRecentCount(if (checked) 20 else 0)
                        haptic.performTick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Download on Wi-Fi Only", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Prevent cellular data usage when downloading",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = { checked ->
                        wifiOnly = checked
                        settingsStore.saveDownloadWifiOnly(checked)
                        haptic.performTick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // Storage Cleanup Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onPruneUnpinned,
                    shape = activeTheme.plate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Prune Unpinned", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onClearArtwork,
                    shape = activeTheme.plate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Art", maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onClearOfflineCache,
                shape = activeTheme.plate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All Offline Downloads")
            }
        }
    }

}
