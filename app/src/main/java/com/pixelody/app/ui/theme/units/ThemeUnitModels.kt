package com.pixelody.app.ui.theme.units

import androidx.compose.ui.graphics.Color
import com.pixelody.app.data.model.RepeatMode
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.TransportGlyphType

/**
 * Authoritative information contract for a track item row or card.
 * Decouples presentation from data models and domain storage.
 */
data class TrackItemData(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val durationSeconds: Int = 0,
    val artworkUrl: String? = null,
    val isLossless: Boolean = false,
    val isFavorite: Boolean = false,
    val isMissing: Boolean = false,
    val isSelected: Boolean = false,
    val isPlaying: Boolean = false,
    val isOfflineCached: Boolean = false,
    val isDownloading: Boolean = false,
    val isQueued: Boolean = false,
    val downloadProgress: Float = 0f,
    val levelLabel: String = "ROM",
    val onClick: () -> Unit = {},
    val onLongClick: (() -> Unit)? = null,
    val onToggleFavorite: (() -> Unit)? = null,
    val onToggleDownload: (() -> Unit)? = null,
    val onOpenPlayer: (() -> Unit)? = null,
    val onOpenQueue: (() -> Unit)? = null,
    val onCycleEqualizerPreset: (() -> Unit)? = null
)

/**
 * Maps a core [Track] into an immutable [TrackItemData] presentation unit.
 */
fun Track.toItemData(
    selected: Boolean = false,
    isPlaying: Boolean = false,
    isFavorite: Boolean = this.favorite,
    isOfflineCached: Boolean = false,
    isDownloading: Boolean = false,
    isQueued: Boolean = false,
    downloadProgress: Float = 0f,
    levelLabel: String = "ROM",
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onToggleDownload: (() -> Unit)? = null,
    onOpenPlayer: (() -> Unit)? = null,
    onOpenQueue: (() -> Unit)? = null,
    onCycleEqualizerPreset: (() -> Unit)? = null
): TrackItemData = TrackItemData(
    id = this.id,
    title = this.title,
    artist = this.artist,
    album = this.album,
    durationSeconds = this.durationSeconds,
    artworkUrl = this.artworkUrl,
    isLossless = this.lossless,
    isFavorite = isFavorite,
    isMissing = this.missing,
    isSelected = selected,
    isPlaying = isPlaying,
    isOfflineCached = isOfflineCached,
    isDownloading = isDownloading,
    isQueued = isQueued,
    downloadProgress = downloadProgress,
    levelLabel = levelLabel,
    onClick = onClick,
    onLongClick = onLongClick,
    onToggleFavorite = onToggleFavorite,
    onToggleDownload = onToggleDownload,
    onOpenPlayer = onOpenPlayer,
    onOpenQueue = onOpenQueue,
    onCycleEqualizerPreset = onCycleEqualizerPreset
)

/**
 * Authoritative information contract for playback transport controls.
 */
data class TransportData(
    val isPlaying: Boolean,
    val canSkipNext: Boolean = true,
    val canSkipPrevious: Boolean = true,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentTrackTitle: String = "",
    val currentTrackArtist: String = "",
    val isShuffleActive: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val onPlayPause: () -> Unit = {},
    val onSkipNext: () -> Unit = {},
    val onSkipPrevious: () -> Unit = {},
    val onSeek: (Long) -> Unit = {},
    val onToggleShuffle: (() -> Unit)? = null,
    val onCycleRepeat: (() -> Unit)? = null
)

/**
 * Authoritative information contract for hero stage headers.
 */
data class HeroStageData(
    val title: String,
    val subtitle: String = "",
    val badge: String = "",
    val artworkUrl: String? = null,
    val primaryActionLabel: String? = null,
    val onPrimaryAction: (() -> Unit)? = null,
    val secondaryActionLabel: String? = null,
    val onSecondaryAction: (() -> Unit)? = null,
    val isPlaying: Boolean = false
)

/**
 * Authoritative information contract for quick-start and shortcut tiles.
 */
data class QuickTileData(
    val title: String,
    val subtitle: String,
    val enabled: Boolean = true,
    val badge: String? = null,
    val artworkUrl: String? = null,
    val glyph: TransportGlyphType? = null,
    val glyphColor: Color? = null,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null
)
