package com.pixelody.app.feature.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import com.pixelody.app.ui.theme.compactSurfaceShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.core.playback.FlowBrowseFilter
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.RepeatMode
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.MetadataStrip
import com.pixelody.app.ui.components.PixelodyNavGlyph
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.StatusChip
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.VuAudioLevelMeter
import com.pixelody.app.ui.components.compactLabel
import com.pixelody.app.ui.components.equalizerSummary
import com.pixelody.app.ui.components.formatDuration
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.components.tactilePress
import com.pixelody.app.ui.navigation.PixelodyStateTags
import com.pixelody.app.ui.navigation.PixelodyTab
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun EqualizerQuickStrip(
    profile: EqualizerProfile,
    trackHasOverride: Boolean,
    onCyclePreset: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = equalizerSummary(profile, trackHasOverride),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (profile.enabled) "Tap preset for the next curve." else "Bypassed",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onCyclePreset) { Text("Preset") }
            TextButton(onClick = onOpenEqualizer) { Text("Tune") }
        }
    }
}

@Composable
internal fun WideListeningPanel(
    track: Track?,
    snapshot: LibrarySnapshot?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    equalizerProfile: EqualizerProfile,
    trackHasEqualizerOverride: Boolean,
    shuffleEnabled: Boolean,
    shuffleMode: FlowShuffleMode = FlowShuffleMode.Off,
    repeatMode: RepeatMode,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCycleEqualizerPreset: () -> Unit,
    onOpenQueue: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.26f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Playing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (track == null) {
                Text(
                    text = snapshot?.host?.hostName ?: "Choose music from Home, Search, or Library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(text = "${snapshot?.tracks?.size ?: 0} host")
                    StatusChip(text = "${snapshot?.queue?.trackIds?.size ?: 0} queued")
                }
                OutlinedButton(onClick = onOpenQueue, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Queue")
                }
            } else {
                RemoteArtwork(
                    artworkUrl = track.artworkUrl,
                    title = track.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(track.artist, track.album).filter { it.isNotBlank() }.joinToString(" / "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                WideProgressBar(
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
                MetadataStrip(track = track)
                EqualizerQuickStrip(
                    profile = equalizerProfile,
                    trackHasOverride = trackHasEqualizerOverride,
                    onCyclePreset = onCycleEqualizerPreset,
                    onOpenEqualizer = onOpenPlayer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenPlayer, modifier = Modifier.weight(1f)) {
                        Text("Open")
                    }
                    OutlinedButton(onClick = onOpenQueue, modifier = Modifier.weight(1f)) {
                        Text("Queue")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPrevious, modifier = Modifier.weight(1f)) {
                        Text("Prev")
                    }
                    OutlinedButton(
                        onClick = onPlayPause,
                        enabled = track.streamUrl.isNotBlank() && !track.missing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }
                    OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f)) {
                        Text("Next")
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = shuffleEnabled,
                            onClick = onToggleShuffle,
                            label = { Text(if (shuffleMode != FlowShuffleMode.Off) shuffleMode.badge else "Shuffle") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = repeatMode != RepeatMode.Off,
                            onClick = onToggleRepeat,
                            label = { Text(repeatMode.compactLabel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun WideProgressBar(
    isPlaying: Boolean,
    positionMs: Long = 0L,
    durationMs: Long,
    positionMsProvider: (() -> Long)? = null
) {
    val currentPosition = positionMsProvider?.invoke() ?: positionMs
    val progress = if (durationMs > 0) {
        (currentPosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.32f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(currentPosition / 1000), style = MaterialTheme.typography.bodySmall)
            Text(formatDuration(durationMs / 1000), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MiniPlayerScrubProgressBar(
    isPlaying: Boolean,
    durationMs: Long,
    positionMsProvider: () -> Long,
    onSeek: ((Long) -> Unit)?,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val currentPosition = positionMsProvider()
    val progress = if (durationMs > 0) {
        (currentPosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val effectiveProgress = scrubFraction ?: progress

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .pointerInput(durationMs) {
                detectTapGestures(
                    onPress = { offset ->
                        if (durationMs > 0 && onSeek != null) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            scrubFraction = fraction
                            haptic.performTick()
                            onSeek((fraction * durationMs).toLong())
                            tryAwaitRelease()
                            scrubFraction = null
                        }
                    }
                )
            }
            .pointerInput(durationMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (durationMs > 0 && onSeek != null) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            scrubFraction = fraction
                            haptic.performTick()
                            onSeek((fraction * durationMs).toLong())
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (durationMs > 0 && onSeek != null) {
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            scrubFraction = fraction
                            onSeek((fraction * durationMs).toLong())
                        }
                    },
                    onDragEnd = { scrubFraction = null },
                    onDragCancel = { scrubFraction = null }
                )
            }
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.38f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(effectiveProgress)
                .height(5.dp)
                .background(if (isPlaying) Color(0xFFE5A93C) else MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun MiniPlayerSubtitle(
    track: Track,
    harmonicTag: String?,
    equalizerProfile: EqualizerProfile,
    trackHasEqualizerOverride: Boolean,
    positionMsProvider: () -> Long,
    durationMs: Long,
    playbackError: String = ""
) {
    val currentPosition = positionMsProvider()
    // A track tapped from Library that fails used to stop with no explanation:
    // the classifier's message only ever reached the Player screen, which is not
    // where the person is standing when they tap a list row. The mini player is.
    if (playbackError.isNotBlank()) {
        Text(
            text = playbackError,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    Text(
        text = listOfNotNull(
            track.artist,
            harmonicTag,
            miniPlayerTimeLabel(currentPosition, durationMs),
            equalizerSummary(equalizerProfile, trackHasEqualizerOverride)
        ).filter { it.isNotBlank() }.joinToString(" • "),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        style = MaterialTheme.typography.bodySmall
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MiniPlayerBar(
    track: Track?,
    previousTrack: Track? = null,
    nextTrack: Track? = null,
    isPlaying: Boolean,
    positionMs: Long = 0L,
    durationMs: Long,
    equalizerProfile: EqualizerProfile,
    trackHasEqualizerOverride: Boolean,
    shuffleEnabled: Boolean,
    shuffleMode: FlowShuffleMode = FlowShuffleMode.Off,
    repeatMode: RepeatMode,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    onCycleEqualizerPreset: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeek: ((Long) -> Unit)? = null,
    onOpenPlayerToView: ((String) -> Unit)? = null,
    positionMsProvider: (() -> Long)? = null,
    experienceMode: com.pixelody.app.data.model.AppExperienceMode = com.pixelody.app.data.model.AppExperienceMode.Essential,
    playbackError: String = "",
    embeddedInDock: Boolean = false
) {
    if (track == null) return
    val effectivePositionMsProvider = positionMsProvider ?: { positionMs }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val largeText = density.fontScale >= 1.5f
    var showQuickConsoleFan by remember { mutableStateOf(false) }
    val isActionable = track.streamUrl.isNotBlank() && !track.missing

    val offsetX = remember { Animatable(0f) }
    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            offsetX.snapTo(offsetX.value + delta)
        }
    }
    val theme = LocalPixelodyThemeVariant.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(theme.compactSurfaceShape())
    ) {
        // Visual Cue Layer behind the sliding player card
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(if (embeddedInDock && offsetX.value == 0f) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Left-revealed Cue: Swiping RIGHT -> Going to Previous Track
            if (offsetX.value > 8f) {
                val cueAlpha = (offsetX.value / with(density) { 50.dp.toPx() }).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = cueAlpha),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = cueAlpha * 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.Previous,
                                color = MaterialTheme.colorScheme.primary,
                                sizeDp = 16
                            )
                            Text(
                                text = previousTrack?.title?.let { "Prev: $it" } ?: "Previous Track",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Right-revealed Cue: Swiping LEFT -> Going to Next Track
            if (offsetX.value < -8f) {
                val cueAlpha = (-offsetX.value / with(density) { 50.dp.toPx() }).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = cueAlpha),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = cueAlpha * 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = nextTrack?.title?.let { "Next: $it" } ?: "Next Track",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.Next,
                                color = MaterialTheme.colorScheme.primary,
                                sizeDp = 16
                            )
                        }
                    }
                }
            }
        }

        // Draggable Foreground Player Surface
        Surface(
            color = if (embeddedInDock) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = if (embeddedInDock) 0.dp else 4.dp,
            shape = theme.compactSurfaceShape(),
            border = if (embeddedInDock) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .playerSheetDrag(upward = true, onComplete = onOpenPlayer)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val thresholdPx = with(density) { 36.dp.toPx() }
                        val flingDistance = with(density) { 280.dp.toPx() }
                        if (offsetX.value < -thresholdPx || velocity < -300f) {
                            haptic.performTick()
                            coroutineScope.launch {
                                offsetX.animateTo(-flingDistance, tween(120))
                                onNext()
                                offsetX.snapTo(flingDistance)
                                offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 450f))
                            }
                        } else if (offsetX.value > thresholdPx || velocity > 300f) {
                            haptic.performTick()
                            coroutineScope.launch {
                                offsetX.animateTo(flingDistance, tween(120))
                                onPrevious()
                                offsetX.snapTo(-flingDistance)
                                offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 450f))
                            }
                        } else {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                            }
                        }
                    }
                )
        ) {
            Column {
                MiniPlayerScrubProgressBar(
                    isPlaying = isPlaying,
                    durationMs = durationMs,
                    positionMsProvider = effectivePositionMsProvider,
                    onSeek = onSeek,
                    haptic = haptic
                )

                if (showQuickConsoleFan) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("Turntable", TransportGlyphType.VinylDisc, "TURNTABLE"),
                            Triple("Stems", TransportGlyphType.Settings, "STEMS"),
                            Triple("Scope", TransportGlyphType.WaveformBars, "SCOPE"),
                            Triple("Mastering", TransportGlyphType.Sliders, "EQ RACK")
                        ).forEach { (mode, glyph, label) ->
                            Surface(
                                onClick = {
                                    haptic.performConfirm()
                                    showQuickConsoleFan = false
                                    onOpenPlayerToView?.invoke(mode) ?: onOpenPlayer()
                                },
                                shape = theme.compactSurfaceShape(),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    PixelodyTransportGlyph(
                                        glyph = glyph,
                                        color = MaterialTheme.colorScheme.primary,
                                        sizeDp = 12
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tap area for track & artwork
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClickLabel = "Open full player for ${track.title}",
                                onLongClickLabel = "Open listening tools",
                                role = Role.Button,
                                onClick = {
                                    haptic.performTick()
                                    onOpenPlayer()
                                },
                                onLongClick = {
                                    haptic.performConfirm()
                                    showQuickConsoleFan = !showQuickConsoleFan
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RemoteArtwork(
                            artworkUrl = track.artworkUrl,
                            title = track.title,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isPlaying) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    VuAudioLevelMeter(isPlaying = true)
                                }
                            }
                            val harmonicTag = remember(track, nextTrack) {
                                if (nextTrack != null) {
                                    val left = FlowBrowseFilter.knownKey(track)
                                    val right = FlowBrowseFilter.knownKey(nextTrack)
                                    if (left != null && right != null) "[${left.code}->${right.code}]" else null
                                } else null
                            }
                            if (experienceMode == com.pixelody.app.data.model.AppExperienceMode.Essential && playbackError.isBlank()) {
                                Text(track.artist.ifBlank { "Unknown artist" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else {
                                MiniPlayerSubtitle(
                                    track = track,
                                    harmonicTag = harmonicTag,
                                    equalizerProfile = equalizerProfile,
                                    trackHasEqualizerOverride = trackHasEqualizerOverride,
                                    positionMsProvider = effectivePositionMsProvider,
                                    durationMs = durationMs,
                                    playbackError = playbackError
                                )
                            }
                        }
                        // The artwork/title remains one large tap target; the quiet arrow
                        // makes opening the player visible without crowding transport.
                        Spacer(modifier = Modifier.width(4.dp))
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.ChevronDown,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            sizeDp = 16,
                            modifier = Modifier.graphicsLayer { rotationZ = 180f }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quick Queue Access Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(PixelodyStateTags.QUEUE_ACCESS)
                            .semantics { contentDescription = "Open queue" }
                            .tactilePress {
                                haptic.performTick()
                                onOpenQueue()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Queue,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            sizeDp = 18
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Direct Play/Pause icon button on MiniPlayer
                    Surface(
                        onClick = {
                            haptic.performConfirm()
                            onPlayPause()
                        },
                        enabled = isActionable,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(PixelodyStateTags.MINI_PLAYER_PLAY_PAUSE)
                            .semantics { contentDescription = if (isPlaying) "Pause" else "Play" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PixelodyTransportGlyph(
                                glyph = if (isPlaying) TransportGlyphType.Pause else TransportGlyphType.Play,
                                color = MaterialTheme.colorScheme.onPrimary,
                                sizeDp = 20
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Direct Next/Skip icon button on MiniPlayer
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(PixelodyStateTags.MINI_PLAYER_NEXT)
                            .semantics { contentDescription = "Next track" }
                            .tactilePress {
                                haptic.performTick()
                                onNext()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Next,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            sizeDp = 18
                        )
                    }
                }
            }
        }
    }
}

internal fun miniPlayerTimeLabel(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return ""
    return "${formatDuration(positionMs / 1000)} / ${formatDuration(durationMs / 1000)}"
}
