package com.pixelody.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.core.playback.VinylVaultEngine
import com.pixelody.app.data.model.Track

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * FlippableCrateView: Tactile 3D perspective vinyl crate sleeve flipper.
 */
@Composable
fun FlippableCrateView(
    crateName: String,
    tracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier,
    onEnqueueAll: ((List<Track>) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var activeIndex by remember(tracks.size) { mutableIntStateOf(0) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    if (tracks.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
        ) {
            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "Crate is empty.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    val safeIndex = activeIndex.coerceIn(0, tracks.size - 1)
    val currentTrack = tracks[safeIndex]
    val telemetry = remember(currentTrack.id) { HarmonicKeyEngine.estimateTrackTelemetry(currentTrack) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Crate Name & Sleeve Index
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.20f))
                            .border(1.dp, Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Folder,
                            color = Color(0xFFF59E0B),
                            size = 14.dp
                        )
                    }

                    Column {
                        Text(
                            text = "VINYL VAULT CRATE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = crateName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "Sleeve ${safeIndex + 1} of ${tracks.size}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 3D Perspective Sleeve Showcase with Swipe Gesture
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .pointerInput(safeIndex, tracks.size) {
                        detectHorizontalDragGestures(
                            onDragEnd = { accumulatedDrag = 0f },
                            onDragCancel = { accumulatedDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDrag += dragAmount
                                if (accumulatedDrag < -40f && safeIndex < tracks.size - 1) {
                                    haptic.performTick()
                                    activeIndex = safeIndex + 1
                                    accumulatedDrag = 0f
                                } else if (accumulatedDrag > 40f && safeIndex > 0) {
                                    haptic.performTick()
                                    activeIndex = safeIndex - 1
                                    accumulatedDrag = 0f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = safeIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "SleeveFlipAnimation"
                ) { targetIdx ->
                    val track = tracks[targetIdx]
                    val tel = remember(track.id) { HarmonicKeyEngine.estimateTrackTelemetry(track) }
                    val tiltAngle = VinylVaultEngine.calculateSleeveTiltAngle(targetIdx, safeIndex)

                    Surface(
                        modifier = Modifier
                            .size(150.dp)
                            .graphicsLayer {
                                rotationZ = tiltAngle * 0.2f
                                rotationX = tiltAngle * 0.5f
                                cameraDistance = 8 * density
                            }
                            .clickable {
                                haptic.performConfirm()
                                onPlayTrack(track)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(2.dp, tel.key.harmonicColor.copy(alpha = 0.60f)),
                        shadowElevation = 8.dp
                    ) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            Column(
                                modifier = Modifier.align(Alignment.BottomStart),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = tel.key.harmonicColor.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = tel.key.code,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = tel.key.harmonicColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist.ifBlank { "Unknown Artist" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Flip Navigation & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (safeIndex > 0) {
                            haptic.performTick()
                            activeIndex = safeIndex - 1
                        }
                    },
                    enabled = safeIndex > 0,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("◀ Prev Sleeve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        haptic.performConfirm()
                        onPlayTrack(currentTrack)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Play Sleeve", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }

                OutlinedButton(
                    onClick = {
                        if (safeIndex < tracks.size - 1) {
                            haptic.performTick()
                            activeIndex = safeIndex + 1
                        }
                    },
                    enabled = safeIndex < tracks.size - 1,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Next Sleeve ▶", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FlippableCrateView(
    tracks: List<Track>,
    activeTrack: Track?,
    onSelectTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    FlippableCrateView(
        crateName = activeTrack?.album?.ifBlank { "TEMPORAL CRATE" } ?: "DIGGING CRATE",
        tracks = tracks,
        onPlayTrack = onSelectTrack,
        modifier = modifier
    )
}
