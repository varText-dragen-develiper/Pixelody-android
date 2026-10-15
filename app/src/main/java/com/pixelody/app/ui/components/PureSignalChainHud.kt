package com.pixelody.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.PureSignalMasterEngine
import com.pixelody.app.data.model.AcousticTargetPreset
import com.pixelody.app.data.model.Track

/**
 * PureSignalChainHud: Studio-grade signal chain visualizer and acoustic mastering control banner.
 */
@Composable
fun PureSignalChainHud(
    track: Track?,
    activeOutputDevice: String? = null,
    modifier: Modifier = Modifier,
    onPresetSelected: (AcousticTargetPreset) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var selectedPreset by remember { mutableStateOf(AcousticTargetPreset.StudioFlatReference) }
    val verification = remember(track?.id, activeOutputDevice) {
        if (track != null) {
            PureSignalMasterEngine.verifyBitstreamPath(track, activeOutputDevice)
        } else null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        ),
        border = BorderStroke(1.dp, Color(0xFF4ADE80).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title & Bit-Perfect Badge
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
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4ADE80).copy(alpha = 0.20f))
                            .border(1.dp, Color(0xFF4ADE80), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.DiamondLossless,
                            color = Color(0xFF4ADE80),
                            size = 14.dp
                        )
                    }

                    Column {
                        Text(
                            text = "PURE SIGNAL MASTER",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFF4ADE80)
                        )
                        Text(
                            text = verification?.bitstreamLabel ?: "24-bit / 192kHz Direct FLAC",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (verification?.isBitPerfect != false) Color(0xFF4ADE80).copy(alpha = 0.20f)
                    else Color(0xFFF59E0B).copy(alpha = 0.20f),
                    border = BorderStroke(
                        1.dp,
                        if (verification?.isBitPerfect != false) Color(0xFF4ADE80) else Color(0xFFF59E0B)
                    )
                ) {
                    Text(
                        text = if (verification?.isBitPerfect != false) "BIT-PERFECT DIRECT" else "DSP RESAMPLED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (verification?.isBitPerfect != false) Color(0xFF4ADE80) else Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Signal Chain Nodes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignalChainNodePill(label = verification?.codec ?: "FLAC", sublabel = "${verification?.bitDepth ?: 24}b")
                Text("➔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SignalChainNodePill(label = "PARAMETRIC", sublabel = "10-Band")
                Text("➔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SignalChainNodePill(label = "LIMITER", sublabel = "${selectedPreset.limiterCeilingDb}dB")
                Text("➔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SignalChainNodePill(label = "OUTPUT", sublabel = verification?.outputSink?.take(10) ?: "Direct")
            }

            // Acoustic Preset Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(AcousticTargetPreset.entries) { preset ->
                    val isSelected = selectedPreset == preset
                    Surface(
                        onClick = {
                            haptic.performTick()
                            selectedPreset = preset
                            onPresetSelected(preset)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(preset.colorHex).copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(preset.colorHex) else Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Text(
                            text = preset.tag,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color(preset.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Miniature 10-Band Response Curve
            ParametricCurvePreview(
                bandsDb = selectedPreset.bandsDb,
                color = Color(selectedPreset.colorHex),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )
        }
    }
}

@Composable
private fun SignalChainNodePill(
    label: String,
    sublabel: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(text = sublabel, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4ADE80))
        }
    }
}

@Composable
private fun ParametricCurvePreview(
    bandsDb: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Center 0 dB guide line
        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1f
        )

        if (bandsDb.isNotEmpty()) {
            val path = Path()
            val stepX = w / (bandsDb.size - 1).coerceAtLeast(1)

            bandsDb.forEachIndexed { i, db ->
                val x = i * stepX
                val y = (midY - (db * (h / 12f))).coerceIn(4f, h - 4f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(x, y))
            }

            drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
