package com.pixelody.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.lyrics.LyricLine
import com.pixelody.app.core.lyrics.LyricsDocument
import com.pixelody.app.data.model.Track

@Composable
fun LyricsView(
    lyricsDocument: LyricsDocument,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    currentTrack: Track? = null
) {
    val haptic = LocalHapticFeedback.current
    var autoScroll by remember { mutableStateOf(true) }
    var userOffsetMs by remember(lyricsDocument.title, lyricsDocument.artist) { mutableLongStateOf(lyricsDocument.offsetMs) }
    val effectiveDoc = remember(lyricsDocument, userOffsetMs) {
        lyricsDocument.copy(offsetMs = userOffsetMs)
    }

    val activeIndex = remember(effectiveDoc, positionMs) {
        effectiveDoc.activeLineIndex(positionMs)
    }

    val listState = rememberLazyListState()

    // Reset scroll position to top when song changes
    LaunchedEffect(lyricsDocument.title, lyricsDocument.artist) {
        listState.scrollToItem(0)
    }

    // Smoothly follow the active lyric line
    LaunchedEffect(activeIndex, autoScroll) {
        if (autoScroll && activeIndex >= 0 && activeIndex < effectiveDoc.lines.size) {
            // Scroll with an offset so the active lyric is roughly centered vertically
            listState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = -300
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Lyrics Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source & sync status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (effectiveDoc.isSynced) "SYNCED" else "STATIC",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (effectiveDoc.isSynced) Color(0xFF6C9EFF) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = effectiveDoc.source.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Controls: Auto-follow toggle & Offset adjust
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (effectiveDoc.isSynced) {
                    Surface(
                        onClick = {
                            haptic.performTick()
                            autoScroll = !autoScroll
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (autoScroll) Color(0xFF6C9EFF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (autoScroll) Color(0xFF6C9EFF).copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = if (autoScroll) "Auto · On" else "Auto · Off",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (autoScroll) Color(0xFF6C9EFF) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Offset adjustments
                    Surface(
                        onClick = {
                            haptic.performTick()
                            userOffsetMs -= 500L
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "-0.5s",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        onClick = {
                            haptic.performTick()
                            userOffsetMs += 500L
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "+0.5s",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        val isTitleMatching = currentTrack == null ||
                effectiveDoc.title.isBlank() ||
                currentTrack.title.isBlank() ||
                effectiveDoc.title.equals(currentTrack.title, ignoreCase = true) ||
                currentTrack.title.contains(effectiveDoc.title, ignoreCase = true) ||
                effectiveDoc.title.contains(currentTrack.title, ignoreCase = true)

        if (!isTitleMatching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF6C9EFF)
                    )
                    Text(
                        text = "Synchronizing acoustic telemetry for ${currentTrack?.title.orEmpty()}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        if (!effectiveDoc.hasLyrics) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "♪",
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Text(
                        text = "No Lyrics Available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "No lyrics found in embedded tags, companion .lrc sidecars, or the LRCLIB community database for \"${currentTrack?.title ?: effectiveDoc.title.ifBlank { "this track" }}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "How to display lyrics:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF6C9EFF)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Place a synchronized '.lrc' file with the same filename in the song's folder, or embed standard USLT / SYLT tags into the audio file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            return
        }

        // Scrollable Lyrics Body with top and bottom edge gradients
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 100.dp, bottom = 220.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(
                    items = effectiveDoc.lines,
                    key = { idx, line -> "lyric_${idx}_${line.timestampMs}" }
                ) { index, line ->
                    val isActive = index == activeIndex
                    val isPast = activeIndex >= 0 && index < activeIndex

                    val textColor by animateColorAsState(
                        targetValue = when {
                            isActive -> MaterialTheme.colorScheme.onSurface
                            isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                        },
                        animationSpec = tween(250),
                        label = "lyric_color"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.05f else 1.0f,
                        animationSpec = tween(250),
                        label = "lyric_scale"
                    )

                    val fontSize = if (isActive) 22.sp else 18.sp
                    val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = line.isSynced) {
                                haptic.performTick()
                                onSeek(line.timestampMs)
                            }
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (effectiveDoc.isSynced && line.isSynced) {
                            Text(
                                text = if (isActive) "›" else " ",
                                color = Color(0xFF6C9EFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                modifier = Modifier.width(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = line.text,
                                color = textColor,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                fontStyle = if (line.isInstrumental) FontStyle.Italic else FontStyle.Normal,
                                lineHeight = 28.sp
                            )
                            if (effectiveDoc.isSynced && line.isSynced && isActive) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = line.formattedTimestamp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF6C9EFF).copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Top fade gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom fade gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }
    }
}
