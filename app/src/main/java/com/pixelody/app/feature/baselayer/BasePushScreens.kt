package com.pixelody.app.feature.baselayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelody.app.core.genre.GenreTaxonomyEngine
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.theme.PixelodyMobileTheme

/**
 * Screen for adding music to the device library.
 */
@Composable
fun BaseAcquireScreen(
    isScanning: Boolean,
    scanNotice: String,
    onScanDevice: () -> Unit,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Add music from this phone or connect to a host.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (scanNotice.isNotBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                        Text(
                            text = scanNotice,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            BaseSectionHeader("Phone storage")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanDevice,
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isScanning) "Scanning device..." else "Scan entire device")
                }
                OutlinedButton(
                    onClick = onPickFolder,
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select music folder")
                }
                OutlinedButton(
                    onClick = onPickFiles,
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select audio files")
                }
            }
        }

        item {
            BaseSectionHeader("Host & LAN sync")
        }

        item {
            OutlinedButton(
                onClick = onOpenTools,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connection tools & pairing")
            }
        }
    }
}

/**
 * Technical screen providing host telemetry, connection testing, and cache management.
 */
@Composable
fun BaseTechnicalScreen(
    hostReachable: Boolean,
    hostBaseUrl: String?,
    localTrackCount: Int,
    cachedTrackCount: Int,
    cacheSizeBytes: Long,
    onReconnectHost: () -> Unit,
    onForgetHost: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BaseSectionHeader("Host connection")
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hostBaseUrl == null) "No host configured"
                            else if (hostReachable) "Connected"
                            else "Unreachable",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hostReachable) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }

                    if (!hostBaseUrl.isNullOrBlank()) {
                        Text(
                            text = "Host URL: $hostBaseUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onReconnectHost,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reconnect")
                        }
                        if (hostBaseUrl != null) {
                            OutlinedButton(
                                onClick = onForgetHost,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Forget")
                            }
                        }
                    }
                }
            }
        }

        item {
            BaseSectionHeader("Device & cache storage")
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Local phone tracks", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "$localTrackCount songs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Offline cached tracks", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "$cachedTrackCount songs (${formatBytes(cacheSizeBytes)})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (cachedTrackCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onClearCache,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear offline cache")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Appearance preferences screen.
 */
@Composable
fun BaseAppearanceScreen(
    currentTheme: PixelodyMobileTheme,
    onResetTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BaseSectionHeader("Active theme")
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = currentTheme.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentTheme.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onResetTheme,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to default")
            }
        }
    }
}

/**
 * Track detail screen providing full technical metadata, source provenance, album/artist links, and actions.
 */
@Composable
fun BaseTrackDetailScreen(
    data: BaseLayerData,
    trackId: String,
    onPlayTrack: (List<String>) -> Unit,
    onAddToQueue: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenGenre: (String) -> Unit = {},
    onAddToCrate: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onAddToPlaylist: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = data.track(trackId)
    if (track == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Track not found", style = MaterialTheme.typography.titleMedium)
            Text(
                "This track is no longer available in the active library snapshot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val isPlayable = data.isPlayable(trackId)
    val source = data.sourceOf(trackId)
    val isDownloaded = data.downloadedTrackIds.contains(trackId)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RemoteArtwork(
                    artworkUrl = track.artworkUrl,
                    title = track.title,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (track.artist.isNotBlank()) {
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onPlayTrack(listOf(trackId)) },
                    enabled = isPlayable,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play")
                }
                OutlinedButton(
                    onClick = { onAddToQueue(trackId) },
                    enabled = isPlayable,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add to queue")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onToggleFavorite(trackId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (track.favorite) "Unlike (♥)" else "Like (♡)")
                }
                OutlinedButton(
                    onClick = { onAddToPlaylist(trackId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Playlist")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { onAddToCrate(track.title, trackId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to a crate")
            }
        }

        item {
            BaseSectionHeader("Origin & availability")
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "Source", value = source.label)
                    DetailRow(
                        label = "Playable now",
                        value = if (isPlayable) "Yes" else "No (host unreachable)"
                    )
                    DetailRow(
                        label = "Offline cache",
                        value = if (isDownloaded) "Cached locally" else "Remote / un-cached"
                    )
                    if (track.favorite) {
                        DetailRow(label = "Favorited", value = "Yes")
                    }
                }
            }
        }

        item {
            BaseSectionHeader("Audio & format")
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(
                        label = "Quality",
                        value = if (track.lossless) "Lossless" else "Lossy"
                    )
                    if (track.format.isNotBlank()) {
                        DetailRow(label = "Format", value = track.format.uppercase())
                    }
                    if (track.codec.isNotBlank()) {
                        DetailRow(label = "Codec", value = track.codec.uppercase())
                    }
                    DetailRow(
                        label = "Sample rate",
                        value = "${"%.1f".format(track.sampleRate / 1000.0)} kHz"
                    )
                    track.bitDepth?.let {
                        DetailRow(label = "Bit depth", value = "$it-bit")
                    }
                    track.bitrate?.let {
                        val displayBitrate = if (it >= 1000) it / 1000 else it
                        DetailRow(label = "Bitrate", value = "$displayBitrate kbps")
                    }
                    DetailRow(
                        label = "Channels",
                        value = if (track.channels == 2) "Stereo (2 ch)" else "${track.channels} channels"
                    )
                    if (track.durationSeconds > 0) {
                        val m = track.durationSeconds / 60
                        val s = track.durationSeconds % 60
                        DetailRow(label = "Duration", value = "%d:%02d".format(m, s))
                    }
                    track.replayGainDb?.let {
                        DetailRow(label = "ReplayGain", value = "${"%.1f".format(it)} dB")
                    }
                }
            }
        }

        if (track.album.isNotBlank() || track.artist.isNotBlank() || track.genre.isNotBlank()) {
            item {
                BaseSectionHeader("Associations")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (track.album.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onOpenAlbum(track.album) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to album — ${track.album}")
                        }
                    }
                    if (track.artist.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onOpenArtist(track.artist) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to artist — ${track.artist}")
                        }
                    }
                    if (track.genre.isNotBlank()) {
                        val canonicalGenre = GenreTaxonomyEngine.canonicalize(track.genre)
                        OutlinedButton(
                            onClick = { onOpenGenre(canonicalGenre) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to genre — $canonicalGenre")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}
