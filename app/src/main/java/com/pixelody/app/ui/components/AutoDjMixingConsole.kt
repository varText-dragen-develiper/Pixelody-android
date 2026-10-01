package com.pixelody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.AcousticTimbreEngine
import com.pixelody.app.core.playback.AutoDjTransitionEngine
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.core.playback.MultibandCrossfaderEngine
import com.pixelody.app.core.playback.MusicalPhraseEngine
import com.pixelody.app.data.model.AutoDjSettings
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.DjEnvelopeFrame
import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.HarmonicEnergyMode
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.MultibandDspFrame
import com.pixelody.app.data.model.PhraseZone
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.model.TrackDjTelemetry
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * AutoDjMixingConsole: Comprehensive interactive UI console for harmonic Camelot mixing,
 * dual-deck BPM alignment, multi-curve transition visualizer, and tactical mixing controls.
 */
@Composable
fun AutoDjMixingConsole(
    currentTrack: Track?,
    nextTrack: Track?,
    envelopeFrame: DjEnvelopeFrame,
    settings: AutoDjSettings,
    onSettingsChange: (AutoDjSettings) -> Unit,
    onTriggerTransition: (curve: DjTransitionCurve, durationSeconds: Int) -> Unit,
    onCancelTransition: () -> Unit,
    onHarmonicSortQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedWheelKey by remember { mutableStateOf<CamelotKey?>(null) }

    val deckA = remember(currentTrack) {
        currentTrack?.let { HarmonicKeyEngine.estimateTrackTelemetry(it) }
    }
    val deckB = remember(nextTrack) {
        nextTrack?.let { HarmonicKeyEngine.estimateTrackTelemetry(it) }
    }

    val harmonicAnalysis = remember(deckA, deckB) {
        if (deckA != null && deckB != null) {
            HarmonicKeyEngine.analyzeTransition(deckA, deckB)
        } else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PixelodyTransportGlyph(
                    glyph = TransportGlyphType.VinylDisc,
                    color = MaterialTheme.colorScheme.primary,
                    size = 20.dp
                )
                Text(
                    text = "HARMONIC AUTO-DJ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Auto-DJ Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (settings.isAutoDjEnabled) "AUTO ACTIVE" else "MANUAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (settings.isAutoDjEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = settings.isAutoDjEnabled,
                    onCheckedChange = { enabled ->
                        haptic.performTick()
                        onSettingsChange(settings.copy(isAutoDjEnabled = enabled))
                    },
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                )
            }
        }

        // --- Camelot Harmonic Wheel Visualizer ---
        CamelotWheelCanvas(
            currentKey = deckA?.key,
            nextKey = deckB?.key,
            selectedKey = selectedWheelKey,
            onKeySelected = { key ->
                haptic.performTick()
                selectedWheelKey = if (selectedWheelKey == key) null else key
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        )

        // Selected Wheel Key Info Box (if inspected)
        if (selectedWheelKey != null) {
            val key = selectedWheelKey!!
            val relationToCurrent = deckA?.key?.let { HarmonicKeyEngine.calculateHarmonicRelation(it, key) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${key.code} • ${key.musicalKey}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (relationToCurrent != null) {
                            Text(
                                text = relationToCurrent.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(relationToCurrent.badgeColorHex),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (relationToCurrent != null) {
                        Text(
                            text = relationToCurrent.advice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        var activeEnergyMode by remember { mutableStateOf(HarmonicEnergyMode.HarmonicLock) }

        // --- Harmonic Energy Steering Dial ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HARMONIC ENERGY STEERING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = activeEnergyMode.tag,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(activeEnergyMode.colorHex)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HarmonicEnergyMode.entries.forEach { mode ->
                    val isSelected = activeEnergyMode == mode
                    val modeColor = Color(mode.colorHex)
                    Surface(
                        onClick = {
                            haptic.performTick()
                            activeEnergyMode = mode
                            if (settings.harmonicSortEnabled) {
                                onHarmonicSortQueue()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) modeColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (isSelected) modeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) modeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Text(
                text = activeEnergyMode.subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- Dual Deck Telemetry Readout ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Deck A (Outgoing / Now Playing)
            DeckTelemetryCard(
                deckLabel = "DECK A (CURRENT)",
                telemetry = deckA,
                isCurrent = true,
                gain = envelopeFrame.outgoingGain,
                modifier = Modifier.weight(1f)
            )

            // Deck B (Incoming / Next)
            DeckTelemetryCard(
                deckLabel = "DECK B (NEXT)",
                telemetry = deckB,
                isCurrent = false,
                gain = envelopeFrame.incomingGain,
                pitchDelta = harmonicAnalysis?.pitchStretchPercent,
                modifier = Modifier.weight(1f)
            )
        }

        // --- Harmonic Compatibility Banner ---
        if (harmonicAnalysis != null) {
            val rel = harmonicAnalysis.relation
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(rel.badgeColorHex).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(rel.badgeColorHex).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(rel.badgeColorHex))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${rel.title} • ${(harmonicAnalysis.overallCompatibilityScore * 100).toInt()}% Match",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(rel.badgeColorHex)
                        )
                        Text(
                            text = rel.advice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // --- Musical Phrase & Downbeat Alignment ---
        if (deckA != null) {
            PhraseQuantizationBar(
                bpm = deckA.bpm,
                currentDurationSeconds = currentTrack?.durationSeconds ?: 180,
                progress = envelopeFrame.progress
            )
        }

        // --- Tri-Band Crossover DSP Meters ---
        MultibandDspMeter(
            progress = envelopeFrame.progress,
            curve = settings.preferredCurve
        )

        // --- EBU R128 Loudness & S-Curve Tempo Normalization ---
        if (deckA != null && deckB != null) {
            LoudnessAndTempoBanner(
                deckA = deckA,
                deckB = deckB,
                progress = envelopeFrame.progress
            )
        }

        // --- Transition Curve Visualizer Graph ---
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSITION PROFILE: ${settings.preferredCurve.title.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (envelopeFrame.isTransitioning) {
                    Text(
                        text = "BLENDING (${(envelopeFrame.progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            DjTransitionGraph(
                curve = settings.preferredCurve,
                progress = envelopeFrame.progress,
                isTransitioning = envelopeFrame.isTransitioning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            )
        }

        // --- Transition Curve Selector Chips ---
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DjTransitionCurve.entries) { curve ->
                val isSelected = settings.preferredCurve == curve
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptic.performTick()
                        onSettingsChange(settings.copy(preferredCurve = curve))
                    },
                    label = {
                        Text(
                            text = curve.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // --- Duration Selector & Controls ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transition Time:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(2, 4, 8, 16).forEach { sec ->
                    val isSecSelected = settings.transitionDurationSeconds == sec
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSecSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                1.dp,
                                if (isSecSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                haptic.performTick()
                                onSettingsChange(settings.copy(transitionDurationSeconds = sec))
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${sec}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSecSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // --- Tactical Action Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Harmonic Queue Reorder Button
            OutlinedButton(
                onClick = {
                    haptic.performConfirm()
                    onHarmonicSortQueue()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.FlowShuffle,
                        color = MaterialTheme.colorScheme.primary,
                        size = 16.dp
                    )
                    Text("Harmonic Sort", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Mix Now Trigger Button
            if (envelopeFrame.isTransitioning) {
                Button(
                    onClick = {
                        haptic.performConfirm()
                        onCancelTransition()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Text("Cancel Mix", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        haptic.performConfirm()
                        onTriggerTransition(settings.preferredCurve, settings.transitionDurationSeconds)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Play,
                            color = MaterialTheme.colorScheme.onPrimary,
                            size = 16.dp
                        )
                        Text("MIX NOW", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Visual interactive Camelot Wheel Canvas with 12 sectors, inner minor ring, outer major ring,
 * and illuminated harmonic active keys.
 */
@Composable
fun CamelotWheelCanvas(
    currentKey: CamelotKey?,
    nextKey: CamelotKey?,
    selectedKey: CamelotKey?,
    onKeySelected: (CamelotKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1.2f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = offset.x - centerX
                    val dy = offset.y - centerY
                    val dist = sqrt(dx * dx + dy * dy)
                    val maxRadius = minOf(centerX, centerY) * 0.95f
                    val innerRadius = maxRadius * 0.50f
                    val minRadius = maxRadius * 0.20f

                    if (dist in minRadius..maxRadius) {
                        var angleDeg = (atan2(dy, dx) * 180f / PI.toFloat()) + 90f
                        if (angleDeg < 0) angleDeg += 360f

                        val sectorIdx = ((angleDeg + 15f) % 360f / 30f).toInt() % 12
                        val keyNumber = if (sectorIdx == 0) 12 else sectorIdx
                        val isMinor = dist < innerRadius
                        val mode = if (isMinor) CamelotMode.Minor else CamelotMode.Major
                        val tappedKey = CamelotKey.fromNumberAndMode(keyNumber, mode)
                        onKeySelected(tappedKey)
                    }
                }
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = minOf(centerX, centerY) * 0.95f
        val innerRadius = maxRadius * 0.58f
        val centerRadius = maxRadius * 0.22f

        // Draw Center Hub
        drawCircle(
            color = Color(0xFF1E293B),
            radius = centerRadius,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.3f),
            radius = centerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        val sectorAngle = 30f

        // Draw 12 Sectors
        for (i in 1..12) {
            val startAngle = (i * 30f) - 105f // Centered on hour markers
            val minorKey = CamelotKey.fromNumberAndMode(i, CamelotMode.Minor)
            val majorKey = CamelotKey.fromNumberAndMode(i, CamelotMode.Major)

            // Inner Ring: Minor (A)
            drawWheelSector(
                centerX = centerX,
                centerY = centerY,
                innerRadius = centerRadius,
                outerRadius = innerRadius,
                startAngle = startAngle,
                sweepAngle = sectorAngle - 1f,
                key = minorKey,
                currentKey = currentKey,
                nextKey = nextKey,
                selectedKey = selectedKey
            )

            // Outer Ring: Major (B)
            drawWheelSector(
                centerX = centerX,
                centerY = centerY,
                innerRadius = innerRadius,
                outerRadius = maxRadius,
                startAngle = startAngle,
                sweepAngle = sectorAngle - 1f,
                key = majorKey,
                currentKey = currentKey,
                nextKey = nextKey,
                selectedKey = selectedKey
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWheelSector(
    centerX: Float,
    centerY: Float,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
    key: CamelotKey,
    currentKey: CamelotKey?,
    nextKey: CamelotKey?,
    selectedKey: CamelotKey?
) {
    val isCurrent = key == currentKey
    val isNext = key == nextKey
    val isSelected = key == selectedKey

    val harmonicRel = if (currentKey != null) HarmonicKeyEngine.calculateHarmonicRelation(currentKey, key) else null

    val fillColor = when {
        isCurrent -> Color(0xFF38BDF8).copy(alpha = 0.85f) // Electric Cyan for Deck A
        isNext -> {
            val badgeHex = harmonicRel?.badgeColorHex ?: 0xFF4ADE80
            Color(badgeHex).copy(alpha = 0.85f) // Harmonic color for Deck B
        }
        isSelected -> Color(0xFFFACC15).copy(alpha = 0.65f)
        harmonicRel != null && harmonicRel != HarmonicRelation.DissonantClash -> {
            Color(harmonicRel.badgeColorHex).copy(alpha = 0.20f)
        }
        else -> {
            if (key.mode == CamelotMode.Minor) Color(0xFF1E293B).copy(alpha = 0.6f)
            else Color(0xFF0F172A).copy(alpha = 0.75f)
        }
    }

    val strokeColor = when {
        isCurrent -> Color(0xFF38BDF8)
        isNext -> Color(harmonicRel?.badgeColorHex ?: 0xFF4ADE80)
        isSelected -> Color(0xFFFACC15)
        else -> Color(0xFF334155).copy(alpha = 0.4f)
    }

    val midAngle = (startAngle + sweepAngle / 2f) * (PI / 180.0)
    val midRadius = (innerRadius + outerRadius) / 2f
    val textX = centerX + (midRadius * cos(midAngle)).toFloat()
    val textY = centerY + (midRadius * sin(midAngle)).toFloat()

    // Draw Arc Slice
    drawArc(
        color = fillColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(centerX - outerRadius, centerY - outerRadius),
        size = Size(outerRadius * 2f, outerRadius * 2f),
        style = Stroke(width = outerRadius - innerRadius)
    )

    // Border
    drawArc(
        color = strokeColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(centerX - outerRadius, centerY - outerRadius),
        size = Size(outerRadius * 2f, outerRadius * 2f),
        style = Stroke(width = if (isCurrent || isNext || isSelected) 2.5f else 1f)
    )

    // Draw Text Label
    val paint = android.graphics.Paint().apply {
        color = if (isCurrent || isNext || isSelected) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
        textSize = if (key.mode == CamelotMode.Major) 22f else 18f
        isFakeBoldText = isCurrent || isNext || isSelected
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        key.code,
        textX,
        textY + 7f,
        paint
    )
}

@Composable
fun DeckTelemetryCard(
    deckLabel: String,
    telemetry: TrackDjTelemetry?,
    isCurrent: Boolean,
    gain: Float,
    pitchDelta: Float? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deckLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(gain * 100).toInt()}% Vol",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (telemetry != null) {
                Text(
                    text = telemetry.track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = telemetry.track.artist.ifBlank { "Unknown Artist" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Key Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "${telemetry.key.code} • ${telemetry.key.musicalKey}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // BPM Readout
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${telemetry.bpm.toInt()} BPM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (pitchDelta != null && pitchDelta != 0f) {
                            Text(
                                text = "${if (pitchDelta > 0) "+" else ""}${String.format("%.1f", pitchDelta)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (kotlin.math.abs(pitchDelta) > 4f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No track queued",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Dynamic canvas graph rendering the outgoing (orange) and incoming (cyan)
 * gain / filter curves across transition progress.
 */
@Composable
fun DjTransitionGraph(
    curve: DjTransitionCurve,
    progress: Float,
    isTransitioning: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
    ) {
        val w = size.width
        val h = size.height
        val steps = 60

        val outPath = Path()
        val inPath = Path()

        for (i in 0..steps) {
            val p = i.toFloat() / steps.toFloat()
            val frame = AutoDjTransitionEngine.calculateEnvelopeFrame(p, curve)
            val x = p * w
            val outY = h - (frame.outgoingGain * (h - 10f)) - 5f
            val inY = h - (frame.incomingGain * (h - 10f)) - 5f

            if (i == 0) {
                outPath.moveTo(x, outY)
                inPath.moveTo(x, inY)
            } else {
                outPath.lineTo(x, outY)
                inPath.lineTo(x, inY)
            }
        }

        // Draw Outgoing Gain Curve (Orange/Amber)
        drawPath(
            path = outPath,
            color = Color(0xFFF97316),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Draw Incoming Gain Curve (Cyan/Sky Blue)
        drawPath(
            path = inPath,
            color = Color(0xFF38BDF8),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Draw Active Progress Cursor
        if (isTransitioning) {
            val cursorX = progress * w
            drawLine(
                color = Color.White,
                start = Offset(cursorX, 0f),
                end = Offset(cursorX, h),
                strokeWidth = 2.5f
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(cursorX, h / 2f)
            )
        }
    }
}

/**
 * 16-bar phrase structure and downbeat-locked quantization HUD.
 */
@Composable
fun PhraseQuantizationBar(
    bpm: Float,
    currentDurationSeconds: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val durationMs = (currentDurationSeconds * 1000L).coerceAtLeast(1000L)
    val structure = remember(bpm, durationMs) {
        MusicalPhraseEngine.computePhraseStructure(bpm, durationMs, phraseLengthBars = 16)
    }

    // Simulated position based on progress or current duration
    val simulatedPosMs = ((durationMs * 0.75f) + (progress * structure.barDurationMs * 4)).toLong().coerceIn(0L, durationMs)
    val phraseZone = remember(simulatedPosMs, structure) {
        MusicalPhraseEngine.getPhraseZone(simulatedPosMs, structure)
    }
    val currentBar = remember(simulatedPosMs, bpm) {
        MusicalPhraseEngine.calculateBarInPhrase(simulatedPosMs, bpm, 16)
    }
    val currentBeat = remember(simulatedPosMs, bpm) {
        MusicalPhraseEngine.calculateBeatInBar(simulatedPosMs, bpm)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PHRASE QUANTIZATION (16-BAR)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    fontSize = 9.sp,
                    color = Color(0xFFA78BFA)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFA78BFA).copy(alpha = 0.20f)
                ) {
                    Text(
                        text = phraseZone.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 16-Segment LED Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (bar in 1..16) {
                    val isLit = bar <= currentBar
                    val isDownbeat = bar % 4 == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isLit) {
                                    if (isDownbeat) Color(0xFFA78BFA) else Color(0xFFA78BFA).copy(alpha = 0.65f)
                                } else {
                                    Color.White.copy(alpha = 0.08f)
                                }
                            )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bar $currentBar/16 • Beat $currentBeat/4",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Phase: Downbeat-Locked",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFF4ADE80)
                )
            }
        }
    }
}

/**
 * Real-time 3-band (Low / Mid / High) DSP crossover split meters.
 */
@Composable
fun MultibandDspMeter(
    progress: Float,
    curve: DjTransitionCurve,
    modifier: Modifier = Modifier
) {
    val frame = remember(progress, curve) {
        MultibandCrossfaderEngine.calculateMultibandFrame(progress, curve)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRI-BAND FREQUENCY CROSSOVER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    fontSize = 9.sp,
                    color = Color(0xFF38BDF8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (frame.isVocalDucked) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF43F5E).copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = "VOCAL DUCK -4dB",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF43F5E),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (frame.isBassSwapped) Color(0xFFF59E0B).copy(alpha = 0.25f)
                        else Color(0xFF4ADE80).copy(alpha = 0.20f)
                    ) {
                        Text(
                            text = if (frame.isBassSwapped) "BASS SWAPPED" else "BASS CLEAN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (frame.isBassSwapped) Color(0xFFF59E0B) else Color(0xFF4ADE80),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Frequency Split Gain Meters (Deck A vs Deck B)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Deck A Gains
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "DECK A SPLIT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316)
                    )
                    FrequencyBandMeterRow(label = "LOW", gain = frame.deckALowGain, color = Color(0xFFF97316))
                    FrequencyBandMeterRow(label = "MID", gain = frame.deckAMidGain, color = Color(0xFFFB923C))
                    FrequencyBandMeterRow(label = "HI", gain = frame.deckAHighGain, color = Color(0xFFFDBA74))
                }

                // Deck B Gains
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "DECK B SPLIT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    FrequencyBandMeterRow(label = "LOW", gain = frame.deckBLowGain, color = Color(0xFF38BDF8))
                    FrequencyBandMeterRow(label = "MID", gain = frame.deckBMidGain, color = Color(0xFF60A5FA))
                    FrequencyBandMeterRow(label = "HI", gain = frame.deckBHighGain, color = Color(0xFF93C5FD))
                }
            }
        }
    }
}

@Composable
private fun FrequencyBandMeterRow(
    label: String,
    gain: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(26.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(gain.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Text(
            text = "${(gain * 100).toInt()}%",
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(26.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * EBU R128 LUFS Loudness Normalization & S-Curve Tempo Warping Banner.
 */
@Composable
fun LoudnessAndTempoBanner(
    deckA: TrackDjTelemetry,
    deckB: TrackDjTelemetry,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val loudnessA = remember(deckA.track.id) { AcousticTimbreEngine.estimateTrackLoudness(deckA.track) }
    val loudnessB = remember(deckB.track.id) { AcousticTimbreEngine.estimateTrackLoudness(deckB.track) }
    val gainA = remember(loudnessA) { AcousticTimbreEngine.calculateTargetGain(loudnessA.integratedLufs) }
    val gainB = remember(loudnessB) { AcousticTimbreEngine.calculateTargetGain(loudnessB.integratedLufs) }
    val warpedBpm = remember(deckA.bpm, deckB.bpm, progress) {
        AcousticTimbreEngine.calculateWarpedTempo(deckA.bpm, deckB.bpm, progress)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACOUSTIC MASTERING",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color(0xFF4ADE80)
                )
                Text(
                    text = "EBU R128 (-14 LUFS) • Level: ${(gainA * 100).toInt()}% / ${(gainB * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4ADE80).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${"%.1f".format(warpedBpm)} BPM (S-Curve)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ADE80),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

