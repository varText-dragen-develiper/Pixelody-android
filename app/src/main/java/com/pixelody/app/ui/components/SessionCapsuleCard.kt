package com.pixelody.app.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.analytics.SessionSoundInsights
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant

/**
 * Geometric, audiophile-grade Daily Sonic Capsule presentation.
 * Features zero emojis, clean vector canvas glyphs, editorial liner note quoting,
 * a 3-column structural telemetry grid, and tactile dual-action launch triggers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionCapsuleCard(
    insights: SessionSoundInsights,
    onDailySoundCheck: () -> Unit,
    modifier: Modifier = Modifier,
    dailyCapsule: DailySonicCapsule? = null,
    onOpenTimeline: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onShowDoc: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    // archetype and primaryMood are non-null enums carrying an arbitrary first
    // value, so with no plays recorded they would confidently report a mood and
    // a listening persona for a person who has not played anything.
    val capsuleHasData = dailyCapsule?.hasData ?: insights.hasData
    val streak = dailyCapsule?.streakDays ?: insights.streakDays
    val moodTitle = dailyCapsule?.primaryMood?.title ?: insights.timeOfDayMood
    val archetype = dailyCapsule?.archetype ?: SonicArchetype.HiResAudiophile
    val losslessRatio = dailyCapsule?.audioDna?.losslessRatio ?: insights.losslessRatio
    val sessionMins = dailyCapsule?.audioDna?.totalListeningMinutes ?: insights.sessionMinutes
    val tracksCount = dailyCapsule?.audioDna?.tracksPlayedCount ?: insights.tracksPlayed
    val dominantFormat = when {
        dailyCapsule != null && dailyCapsule.hasData ->
            if (dailyCapsule.audioDna.bitPerfectPlayCount > 0) "FLAC/DSD" else "AAC"
        dailyCapsule != null -> ""
        else -> insights.dominantFormat
    }
    val moodColor = dailyCapsule?.primaryMood?.let { Color(it.accentColorHex) } ?: MaterialTheme.colorScheme.primary

    val archetypeGlyph = when (archetype) {
        SonicArchetype.ElectronicExplorer -> TransportGlyphType.LightningCheck
        SonicArchetype.VinylPurist -> TransportGlyphType.VinylDisc
        SonicArchetype.DeepFlowArchitect -> TransportGlyphType.WaveformBars
        SonicArchetype.HiResAudiophile -> TransportGlyphType.DiamondLossless
        SonicArchetype.EclecticNomad -> TransportGlyphType.OmniSource
    }

    val effectiveLongClick = onLongClick ?: onShowDoc?.let { docCallback ->
        { docCallback("daily_capsule") }
    }
    val theme = LocalPixelodyThemeVariant.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(theme.plate)
            .combinedClickable(
                onClickLabel = "Daily Flow Capsule",
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDailySoundCheck()
                },
                onLongClick = effectiveLongClick?.let { callback ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        callback()
                    }
                }
            ),
        shape = theme.plate,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, moodColor.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Geometric HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = moodColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, moodColor.copy(alpha = 0.40f)),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PixelodyTransportGlyph(
                                glyph = archetypeGlyph,
                                color = moodColor,
                                sizeDp = 12
                            )
                        }
                    }
                    Text(
                        text = "DAILY SONIC CAPSULE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = moodColor,
                        letterSpacing = 1.2.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (streak > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                PixelodyTransportGlyph(
                                    glyph = TransportGlyphType.FlameStreak,
                                    color = MaterialTheme.colorScheme.primary,
                                    sizeDp = 11
                                )
                                Text(
                                    text = "${streak}D STREAK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (onShowDoc != null) {
                        CabinetDocButton(
                            onClick = { onShowDoc("daily_capsule") },
                            contentDescription = "Daily Sonic Capsule documentation"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mood & Archetype Headline
            if (capsuleHasData) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = moodTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = archetype.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                        border = BorderStroke(1.dp, moodColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "${(losslessRatio * 100).toInt()}% LOSSLESS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp,
                            color = moodColor
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No listening recorded yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Play something and this fills in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // AI Liner Note Editorial Box
            if (dailyCapsule != null && dailyCapsule.aiNarrative.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(moodColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "\"${dailyCapsule.aiNarrative}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sleek Inline Telemetry Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.DiamondLossless,
                        color = moodColor.copy(alpha = 0.8f),
                        sizeDp = 12
                    )
                    Text(
                        text = "${sessionMins}m listening",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "$tracksCount played",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = dominantFormat,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tactile Dual-Action Launch Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary: Launch Daily Flow
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(theme.plate)
                        .combinedClickable(
                            onClickLabel = "Launch Daily Flow",
                            role = Role.Button,
                            onClick = {
                                haptic.performConfirm()
                                onDailySoundCheck()
                            }
                        ),
                    shape = theme.plate,
                    color = moodColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, moodColor.copy(alpha = 0.60f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Play,
                            color = moodColor,
                            sizeDp = 13
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Launch Daily Flow",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = moodColor
                        )
                    }
                }

                // Secondary: Timeline & DNA
                if (onOpenTimeline != null) {
                    Surface(
                        modifier = Modifier
                            .clip(theme.plate)
                            .combinedClickable(
                                onClickLabel = "View Timeline",
                                role = Role.Button,
                                onClick = {
                                    haptic.performConfirm()
                                    onOpenTimeline()
                                }
                            ),
                        shape = theme.plate,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.WaveformBars,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                sizeDp = 13
                            )
                            Text(
                                text = "Timeline",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.ChevronRight,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                sizeDp = 10
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeometricTelemetryCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalPixelodyThemeVariant.current
    Surface(
        modifier = modifier,
        shape = theme.plate,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
