package com.pixelody.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.HarmonicFlowCoordinator
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.CollectionHarmonicTelemetry
import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.EnergyContourPoint
import com.pixelody.app.data.model.EnergyContourPreset
import com.pixelody.app.data.model.HarmonicFlowProfile
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.HarmonicTrajectoryPoint
import com.pixelody.app.data.model.QueueSlotHarmonicAffinity
import com.pixelody.app.data.model.Track
import kotlin.math.PI
import kotlin.math.sin

/**
 * HarmonicFlowProfilePicker: 1-Tap switcher across the 3 Unified Harmonic Flow Profiles.
 */
@Composable
fun HarmonicFlowProfilePicker(
    activeProfile: HarmonicFlowProfile,
    onSelectProfile: (HarmonicFlowProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            HarmonicFlowProfile.DeepListening,
            HarmonicFlowProfile.ClubSetFlow,
            HarmonicFlowProfile.SunsetChill
        ).forEach { profile ->
            val isSelected = activeProfile == profile
            val profileColor = Color(profile.colorHex)

            Surface(
                onClick = {
                    haptic.performTick()
                    onSelectProfile(profile)
                },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) profileColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (isSelected) profileColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = profile.tag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) profileColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Text(
                        text = profile.defaultCurve.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * HarmonicFlowHud: Omnipresent Now Playing Flow HUD banner combining Camelot Wheel key tracking,
 * transition countdown, active profile indicators, and tactile 1-tap [MIX NOW] trigger.
 */
@Composable
fun HarmonicFlowHud(
    currentTrack: Track,
    nextTrack: Track?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    activeProfile: HarmonicFlowProfile,
    onTriggerMix: () -> Unit,
    onOpenDjConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val deckA = remember(currentTrack) { HarmonicKeyEngine.estimateTrackTelemetry(currentTrack) }
    val deckB = remember(nextTrack) { nextTrack?.let { HarmonicKeyEngine.estimateTrackTelemetry(it) } }
    val relation = remember(deckA, deckB) {
        if (deckB != null) HarmonicKeyEngine.calculateHarmonicRelation(deckA.key, deckB.key)
        else null
    }

    val remainingSeconds = if (durationMs > 0L) ((durationMs - positionMs) / 1000L).coerceAtLeast(0L) else 99L
    val isNearTransition = remainingSeconds in 1..8 && isPlaying
    val badgeColor = relation?.badgeColorHex?.let { Color(it) } ?: Color(activeProfile.colorHex)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isNearTransition) Color(0xFFF59E0B).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (isNearTransition) Color(0xFFF59E0B) else badgeColor.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenDjConsole() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Key Progression & Transition Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                PixelodyTransportGlyph(
                    glyph = TransportGlyphType.FlowShuffle,
                    color = if (isNearTransition) Color(0xFFF59E0B) else badgeColor,
                    size = 14.dp
                )

                if (deckB != null && relation != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${deckA.key.code} -> ${deckB.key.code}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (isNearTransition) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = badgeColor.copy(alpha = 0.25f),
                                border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = relation.title.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isNearTransition) "AUTO-MIX IN ${remainingSeconds}s • ${nextTrack?.title.orEmpty()}" else "UP NEXT: ${nextTrack?.title.orEmpty()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (isNearTransition) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "${deckA.key.code} • ${activeProfile.tag} • QUEUE END",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            // Tactical 1-Tap [MIX NOW]
            if (nextTrack != null) {
                Surface(
                    onClick = {
                        haptic.performConfirm()
                        onTriggerMix()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isNearTransition) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "MIX NOW",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * QueueSlotHarmonicRibbon: Visual connector rendered between adjacent queue items in QueueScreen.kt
 * displaying live Camelot harmonic affinity, energy delta, and 1-tap [INSERT BRIDGE] if clashing.
 */
@Composable
fun QueueSlotHarmonicRibbon(
    affinity: QueueSlotHarmonicAffinity,
    onInsertBridge: (fromKey: CamelotKey, toKey: CamelotKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val badgeColor = Color(affinity.relation.badgeColorHex)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dot & Connector Line
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )

        // Affinity Tag
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeColor.copy(alpha = 0.15f),
            border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.5f))
        ) {
            Text(
                text = "${affinity.fromKey.code} -> ${affinity.toKey.code} • ${affinity.relation.title}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }

        // Recommended Transition Curve Pill
        Text(
            text = "Curve: ${affinity.recommendedCurve.title}",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.weight(1f))

        // If Clashing -> Show 1-Tap Bridge Generator
        if (affinity.isClash && affinity.suggestedBridgeKey != null) {
            Surface(
                onClick = {
                    haptic.performConfirm()
                    onInsertBridge(affinity.fromKey, affinity.toKey)
                },
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFA78BFA).copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, Color(0xFFA78BFA))
            ) {
                Text(
                    text = "+ BRIDGE (${affinity.suggestedBridgeKey.code})",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFA78BFA),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * HarmonicTrajectorySparkline: Compact vector arc showing the musical key and energy trajectory across any collection.
 * Organized and smoothed to prevent grain overcrowding, equipped with dominant key telemetry, flow cohesion score,
 * and dual-trigger documentation access (long-press and corner '?' button).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HarmonicTrajectorySparkline(
    tracks: List<Track>,
    modifier: Modifier = Modifier,
    onShowDoc: ((String) -> Unit)? = null
) {
    if (tracks.size < 2) return
    val haptic = LocalHapticFeedback.current

    val rawPoints = remember(tracks) { HarmonicFlowCoordinator.calculateTrajectoryPoints(tracks) }
    val telemetry = remember(tracks) { HarmonicFlowCoordinator.calculateCollectionTelemetry(tracks) }

    // Downsample points if there are more than 20 tracks, to ensure grains remain cleanly spaced, non-overlapping beads
    val displayPoints = remember(rawPoints) {
        if (rawPoints.size <= 20) {
            rawPoints
        } else {
            val targetCount = 18
            val step = (rawPoints.size - 1).toFloat() / (targetCount - 1)
            (0 until targetCount).map { i ->
                val index = (i * step).toInt().coerceIn(0, rawPoints.size - 1)
                rawPoints[index]
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (onShowDoc != null) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performTick()
                            onShowDoc("harmonigrains")
                        }
                    )
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Title, Telemetry, and Corner '?' Doc Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                    )
                    Text(
                        text = "HARMONIGRAINS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${telemetry.dominantKey.code} · ${telemetry.harmonicCohesionPercent}% FLOW",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onShowDoc != null) {
                    CabinetDocButton(
                        onClick = { onShowDoc("harmonigrains") },
                        contentDescription = "Harmonigrains documentation"
                    )
                }
            }

            // Smoothed Vector Trajectory Arc Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (displayPoints.size < 2) return@Canvas
                    val stepX = size.width / (displayPoints.size - 1).coerceAtLeast(1)
                    val curvePath = Path()
                    val fillPath = Path()

                    val coordinates = displayPoints.mapIndexed { index, pt ->
                        val x = index * stepX
                        val availableH = size.height - 8.dp.toPx()
                        val y = size.height - 4.dp.toPx() - (pt.energyLevel * availableH)
                        Offset(x, y)
                    }

                    // Build cubic bezier curve
                    coordinates.forEachIndexed { index, pt ->
                        if (index == 0) {
                            curvePath.moveTo(pt.x, pt.y)
                            fillPath.moveTo(pt.x, size.height)
                            fillPath.lineTo(pt.x, pt.y)
                        } else {
                            val prev = coordinates[index - 1]
                            val midX = (prev.x + pt.x) / 2f
                            curvePath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                            fillPath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                        }
                    }

                    // Complete fill path down to baseline
                    val lastPt = coordinates.last()
                    fillPath.lineTo(lastPt.x, size.height)
                    fillPath.close()

                    // Subtle under-curve glow fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.18f),
                                Color(0xFFA78BFA).copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )

                    // Trajectory Line
                    drawPath(
                        path = curvePath,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFFA78BFA), Color(0xFFF59E0B))
                        ),
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw each harmonic grain bead with subtle glow
                    coordinates.forEachIndexed { index, offset ->
                        val pt = displayPoints[index]
                        val beadColor = if (pt.key.mode == CamelotMode.Major) Color(0xFF38BDF8) else Color(0xFFA78BFA)

                        // Outer halo
                        drawCircle(
                            color = beadColor.copy(alpha = 0.25f),
                            radius = 4.dp.toPx(),
                            center = offset
                        )
                        // Core bead
                        drawCircle(
                            color = beadColor,
                            radius = 2.2.dp.toPx(),
                            center = offset
                        )
                    }
                }
            }
        }
    }
}

/**
 * HarmonicTrajectorySculptor: Interactive canvas allowing users to visually sculpt queue energy contour arcs
 * using preset envelopes or interactive drag gestures, then mathematically re-order the queue.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HarmonicTrajectorySculptor(
    queue: List<Track>,
    selectedTrackId: String? = null,
    onApplySculptedQueue: (List<Track>) -> Unit,
    modifier: Modifier = Modifier,
    onShowDoc: ((String) -> Unit)? = null
) {
    if (queue.size < 2) return
    val haptic = LocalHapticFeedback.current
    var selectedPreset by remember { mutableStateOf(EnergyContourPreset.PeakWave) }
    val customPoints = remember(queue.size) {
        mutableStateListOf<Float>().apply {
            val count = queue.size.coerceIn(5, 20)
            for (i in 0 until count) {
                val prog = i.toFloat() / (count - 1).coerceAtLeast(1)
                add(selectedPreset.targetEnergyFunction(prog))
            }
        }
    }

    val actualPoints = remember(queue) { HarmonicFlowCoordinator.calculateTrajectoryPoints(queue) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onShowDoc != null) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performTick()
                            onShowDoc("trajectory_sculptor")
                        }
                    )
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Title and [APPLY CONTOUR] Button
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
                        glyph = TransportGlyphType.FlowShuffle,
                        color = MaterialTheme.colorScheme.primary,
                        size = 14.dp
                    )
                    Text(
                        text = "TRAJECTORY SCULPTOR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (onShowDoc != null) {
                        CabinetDocButton(
                            onClick = { onShowDoc("trajectory_sculptor") },
                            contentDescription = "Trajectory Sculptor documentation"
                        )
                    }
                }

                Surface(
                    onClick = {
                        haptic.performConfirm()
                        val sculpted = HarmonicFlowCoordinator.sculptQueueToTargetCurve(
                            queue = queue,
                            preset = selectedPreset,
                            customPoints = if (selectedPreset == EnergyContourPreset.CustomSculpt) customPoints.toList() else null,
                            anchorTrackId = selectedTrackId
                        )
                        onApplySculptedQueue(sculpted)
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.LightningCheck,
                            color = Color.Black,
                            size = 10.dp
                        )
                        Text(
                            text = "APPLY CONTOUR",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }

            // Preset Curve Switcher Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(EnergyContourPreset.entries) { preset ->
                    val isSelected = selectedPreset == preset
                    Surface(
                        onClick = {
                            haptic.performTick()
                            selectedPreset = preset
                            if (preset != EnergyContourPreset.CustomSculpt) {
                                for (i in customPoints.indices) {
                                    val prog = i.toFloat() / (customPoints.size - 1).coerceAtLeast(1)
                                    customPoints[i] = preset.targetEnergyFunction(prog)
                                }
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = preset.tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Interactive Sculpting Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(selectedPreset, customPoints.size) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            selectedPreset = EnergyContourPreset.CustomSculpt
                            val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                            val normY = (1f - (change.position.y / size.height)).coerceIn(0.1f, 1f)
                            val targetIdx = (normX * (customPoints.size - 1)).toInt().coerceIn(0, customPoints.size - 1)
                            customPoints[targetIdx] = normY
                            if (targetIdx > 0) {
                                customPoints[targetIdx - 1] = (customPoints[targetIdx - 1] + normY) / 2f
                            }
                            if (targetIdx < customPoints.size - 1) {
                                customPoints[targetIdx + 1] = (customPoints[targetIdx + 1] + normY) / 2f
                            }
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Target Contour (Dashed Phosphor Line)
                    val targetPath = Path()
                    val targetPointsCount = customPoints.size
                    val targetStepX = w / (targetPointsCount - 1).coerceAtLeast(1)

                    customPoints.forEachIndexed { i, energy ->
                        val tx = i * targetStepX
                        val ty = h - (energy * h)
                        if (i == 0) targetPath.moveTo(tx, ty) else {
                            val prevTx = (i - 1) * targetStepX
                            val prevTy = h - (customPoints[i - 1] * h)
                            val midTx = (prevTx + tx) / 2f
                            targetPath.cubicTo(midTx, prevTy, midTx, ty, tx, ty)
                        }
                    }

                    drawPath(
                        path = targetPath,
                        color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                            cap = StrokeCap.Round
                        )
                    )

                    // 2. Draw Current Queue Trajectory (Solid Gradient Curve + Key Pips)
                    if (actualPoints.isNotEmpty()) {
                        val actualPath = Path()
                        val actualStepX = w / (actualPoints.size - 1).coerceAtLeast(1)

                        actualPoints.forEachIndexed { i, pt ->
                            val ax = i * actualStepX
                            val ay = h - (pt.energyLevel * h)

                            if (i == 0) actualPath.moveTo(ax, ay) else {
                                val prevAx = (i - 1) * actualStepX
                                val prevAy = h - (actualPoints[i - 1].energyLevel * h)
                                val midAx = (prevAx + ax) / 2f
                                actualPath.cubicTo(midAx, prevAy, midAx, ay, ax, ay)
                            }

                            // Key Node Pip
                            drawCircle(
                                color = pt.key.harmonicColor,
                                radius = 3.dp.toPx(),
                                center = Offset(ax, ay)
                            )
                        }

                        drawPath(
                            path = actualPath,
                            brush = Brush.horizontalGradient(
                                actualPoints.map { it.key.harmonicColor }
                            ),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Legend / Guidance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Text(
                        text = "Target Arc (${selectedPreset.tag})",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "Live Queue Flow",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = Color(0xFF10B981)
                    )
                }

                Text(
                    text = "Drag canvas to sculpt",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * CollectionHarmonicCardBadge: Mini trajectory badge rendered on playlist/album cards in LibraryScreen.kt.
 */
@Composable
fun CollectionHarmonicCardBadge(
    tracks: List<Track>,
    modifier: Modifier = Modifier
) {
    if (tracks.size < 2) return
    val telemetry = remember(tracks) { HarmonicFlowCoordinator.calculateCollectionTelemetry(tracks) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(telemetry.dominantKey.harmonicColor)
            )
            Text(
                text = telemetry.keyRange,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = telemetry.dominantKey.harmonicColor
            )
        }

        Text(
            text = "${telemetry.harmonicCohesionPercent}% Flow",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
