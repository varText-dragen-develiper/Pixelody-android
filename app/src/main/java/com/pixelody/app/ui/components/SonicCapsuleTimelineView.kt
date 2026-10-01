package com.pixelody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.AudioDnaMetrics
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.ListeningMemoryEntry
import com.pixelody.app.data.model.NarrativeStyle
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.data.model.SonicCapsuleSettings
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.model.WeeklyListeningTrend
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.TransportGlyphType

/**
 * SonicCapsuleTimelineView: Comprehensive interactive UI for Slice 9.
 * Visualizes the daily AI Sonic Capsule, 4-dimension Audio DNA metrics,
 * AI synthesized liner notes, weekly listening streak, and vertical chronological memory timeline.
 */
@Composable
fun SonicCapsuleTimelineView(
    capsule: DailySonicCapsule,
    weeklyTrend: WeeklyListeningTrend,
    settings: SonicCapsuleSettings,
    onUpdateSettings: (SonicCapsuleSettings) -> Unit,
    onPlayCapsuleFlow: (List<Track>) -> Unit,
    onPlayTrackFromTimeline: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moodColor = Color(capsule.primaryMood.accentColorHex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            moodColor.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Header Bar
            SonicCapsuleHeader(
                capsule = capsule,
                onClose = onClose
            )

            // Scrollable Timeline Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. AI Sonic Liner Notes Banner
                item {
                    AiLinerNotesBanner(
                        capsule = capsule,
                        currentStyle = settings.narrativeStyle,
                        onStyleSelected = { newStyle ->
                            onUpdateSettings(settings.copy(narrativeStyle = newStyle))
                        },
                        moodColor = moodColor
                    )
                }

                // 2. Audio DNA Gauges
                item {
                    AudioDnaGaugeGrid(
                        dna = capsule.audioDna,
                        moodColor = moodColor
                    )
                }

                // 3. 7-Day Weekly Habit Horizon
                item {
                    WeeklyListeningHorizonCard(
                        trend = weeklyTrend,
                        streakDays = capsule.streakDays,
                        moodColor = moodColor
                    )
                }

                // 4. Chronological Listening Timeline Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LISTENING TIMELINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${capsule.memoryTimeline.size} chronological session nodes logged today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PixelodyTransportGlyph(
                                    glyph = when (capsule.archetype) {
                                        SonicArchetype.ElectronicExplorer -> TransportGlyphType.LightningCheck
                                        SonicArchetype.VinylPurist -> TransportGlyphType.VinylDisc
                                        SonicArchetype.DeepFlowArchitect -> TransportGlyphType.WaveformBars
                                        SonicArchetype.HiResAudiophile -> TransportGlyphType.DiamondLossless
                                        SonicArchetype.EclecticNomad -> TransportGlyphType.OmniSource
                                    },
                                    color = MaterialTheme.colorScheme.primary,
                                    sizeDp = 12
                                )
                                Text(
                                    text = capsule.archetype.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 5. Timeline Nodes
                if (capsule.memoryTimeline.isEmpty()) {
                    item {
                        EmptyTimelineCard()
                    }
                } else {
                    items(capsule.memoryTimeline, key = { it.id }) { memory ->
                        TimelineMemoryCard(
                            memory = memory,
                            onClick = { onPlayTrackFromTimeline(memory.trackId) }
                        )
                    }
                }

                // Bottom padding for float action bar
                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }

            // Bottom Action Bar: Play Capsule Flow
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onPlayCapsuleFlow(capsule.highlightTracks) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Play,
                            color = MaterialTheme.colorScheme.onPrimary,
                            sizeDp = 18
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (capsule.highlightTracks.isNotEmpty()) "Play Capsule Flow (${capsule.highlightTracks.size})" else "Play Daily Flow",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onClose() },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.Close,
                                color = MaterialTheme.colorScheme.onSurface,
                                sizeDp = 18
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top Header with title, date, primary mood, and close action.
 */
@Composable
private fun SonicCapsuleHeader(
    capsule: DailySonicCapsule,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.DiamondLossless,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        sizeDp = 18
                    )
                }
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SONIC CAPSULE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• ${capsule.dateString}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${capsule.primaryMood.title} • ${capsule.archetype.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(capsule.primaryMood.accentColorHex),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onClose() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                PixelodyTransportGlyph(
                    glyph = TransportGlyphType.Close,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    sizeDp = 14
                )
            }
        }
    }
}

/**
 * AI Sonic Liner Notes banner with multi-style selector tabs.
 */
@Composable
private fun AiLinerNotesBanner(
    capsule: DailySonicCapsule,
    currentStyle: NarrativeStyle,
    onStyleSelected: (NarrativeStyle) -> Unit,
    moodColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, moodColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.LightningCheck,
                        color = moodColor,
                        sizeDp = 14
                    )
                    Text(
                        text = "AI SONIC LINER NOTES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = moodColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = moodColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = currentStyle.name.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = moodColor
                    )
                }
            }

            // AI Text
            Text(
                text = "\"${capsule.aiNarrative}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Style Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NarrativeStyle.values().forEach { style ->
                    val isSelected = style == currentStyle
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onStyleSelected(style) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) moodColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        border = if (isSelected) BorderStroke(1.dp, moodColor) else null
                    ) {
                        Text(
                            text = when (style) {
                                NarrativeStyle.Poetic -> "Poetic"
                                NarrativeStyle.Analytical -> "Studio"
                                NarrativeStyle.DjLinerNotes -> "DJ Notes"
                            },
                            modifier = Modifier.padding(vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4-dimension Audio DNA gauge matrix.
 */
@Composable
private fun AudioDnaGaugeGrid(
    dna: AudioDnaMetrics,
    moodColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "AUDIO DNA PROFILE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioDnaCard(
                title = "LOSSLESS PURITY",
                value = "${(dna.losslessRatio * 100).toInt()}%",
                subtitle = "${dna.bitPerfectPlayCount} Bit-Perfect Tracks",
                accentColor = Color(0xFFE5A93C),
                glyph = TransportGlyphType.DiamondLossless,
                modifier = Modifier.weight(1f)
            )
            if (dna.avgBpm > 0) {
                AudioDnaCard(
                    title = "TEMPO & KEY",
                    value = "~${dna.avgBpm} BPM",
                    subtitle = dna.dominantKey,
                    accentColor = Color(0xFF38BDF8),
                    glyph = TransportGlyphType.Equalizer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioDnaCard(
                title = "LISTENING TIME",
                value = "${dna.totalListeningMinutes} min",
                subtitle = "${dna.tracksPlayedCount} Tracks Logged",
                accentColor = Color(0xFF8B5CF6),
                glyph = TransportGlyphType.WaveformBars,
                modifier = Modifier.weight(1f)
            )
            AudioDnaCard(
                title = if (dna.energyIndex > 0f) "ENERGY & DIVERSITY" else "ARTISTS",
                value = if (dna.energyIndex > 0f) {
                    "${(dna.energyIndex * 100).toInt()}% Energy"
                } else {
                    "${dna.uniqueArtistsCount}"
                },
                subtitle = "${dna.uniqueArtistsCount} Unique Artists",
                accentColor = Color(0xFFEC4899),
                glyph = TransportGlyphType.FlameStreak,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AudioDnaCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    glyph: TransportGlyphType,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                PixelodyTransportGlyph(
                    glyph = glyph,
                    color = accentColor,
                    sizeDp = 12
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 7-Day Weekly Habit Horizon with mini bar chart and streak pill.
 */
@Composable
private fun WeeklyListeningHorizonCard(
    trend: WeeklyListeningTrend,
    streakDays: Int,
    moodColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-DAY LISTENING HABIT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (trend.hasData) {
                            "${trend.totalWeeklyMinutes} mins total this week"
                        } else {
                            "No listening recorded yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (streakDays > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.FlameStreak,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                sizeDp = 14
                            )
                            Text(
                                text = "${streakDays}d Streak",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Mini Bar Chart
            val maxMins = (trend.dailyMinutes.maxOrNull() ?: 60).coerceAtLeast(60).toFloat()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.dailyMinutes.forEachIndexed { index, minutes ->
                    val isToday = index == trend.dailyMinutes.lastIndex
                    val heightRatio = (minutes.toFloat() / maxMins).coerceIn(0.1f, 1f)
                    val dayLabel = trend.dayLabels.getOrElse(index) { "?" }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${minutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height((40 * heightRatio).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (isToday) moodColor else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Chronological Listening Memory Card.
 */
@Composable
private fun TimelineMemoryCard(
    memory: ListeningMemoryEntry,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Time-of-Day Node Icon
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = Color(memory.detectedMood.accentColorHex).copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(
                        glyph = if (memory.wasSkipped) TransportGlyphType.Next else TransportGlyphType.Play,
                        color = Color(memory.detectedMood.accentColorHex),
                        sizeDp = 14
                    )
                }
            }

            // Track Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = memory.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${memory.artist} • ${memory.album.ifBlank { "Single" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Progress ratio indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(memory.completionRatio)
                            .fillMaxHeight()
                            .background(
                                if (memory.wasSkipped) MaterialTheme.colorScheme.error else Color(memory.detectedMood.accentColorHex)
                            )
                    )
                }
            }

            // Right Badges
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (memory.isLossless) Color(0xFFE5A93C).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = memory.formatBadge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (memory.isLossless) Color(0xFFE5A93C) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (memory.replayCount > 1) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${memory.replayCount}x",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else if (memory.wasSkipped) {
                    Text(
                        text = "Skipped",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Placeholder when no timeline entries have been recorded yet.
 */
@Composable
private fun EmptyTimelineCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PixelodyTransportGlyph(
                glyph = TransportGlyphType.WaveformBars,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                sizeDp = 28
            )
            Text(
                text = "Listening Timeline Awaiting Flow",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Start playing your favorite Hi-Res FLAC tracks or vinyl albums to record chronological memory nodes and generate daily AI liner notes.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
