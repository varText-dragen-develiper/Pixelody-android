package com.pixelody.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.AudioHapticMode
import com.pixelody.app.data.model.AudioHapticSettings
import com.pixelody.app.data.model.HapticPrimitiveType
import com.pixelody.app.data.model.HapticPulseEvent

/**
 * AudioHapticControlRack: Interactive studio control rack for fine-tuning
 * low-end tactile resonance, sub-bass transducers, and mechanical haptics.
 */
@Composable
fun AudioHapticControlRack(
    settings: AudioHapticSettings,
    latestPulse: HapticPulseEvent?,
    onSettingsChange: (AudioHapticSettings) -> Unit,
    onTestPulse: (HapticPrimitiveType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

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
                    glyph = TransportGlyphType.LightningCheck,
                    color = Color(settings.mode.accentColorHex),
                    size = 20.dp
                )
                Text(
                    text = "AUDIO-HAPTIC RESONANCE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Power Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (settings.isEnabled && settings.mode != AudioHapticMode.Off) "RESONATING" else "OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (settings.isEnabled && settings.mode != AudioHapticMode.Off) Color(settings.mode.accentColorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { enabled ->
                        haptic.performTick()
                        onSettingsChange(settings.copy(isEnabled = enabled))
                    },
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                )
            }
        }

        // --- Live Dynamic Tactile Pulse Visualizer ---
        HapticResonanceVisualizer(
            latestPulse = latestPulse,
            mode = settings.mode,
            isEnabled = settings.isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        )

        // --- Mode Description Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(settings.mode.accentColorHex).copy(alpha = 0.12f)
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(settings.mode.accentColorHex).copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(settings.mode.accentColorHex))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${settings.mode.title} • ${settings.mode.subtitle}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(settings.mode.accentColorHex)
                    )
                    Text(
                        text = settings.mode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Mode Selector Chips ---
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AudioHapticMode.entries) { mode ->
                val isSelected = settings.mode == mode
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptic.performTick()
                        onSettingsChange(settings.copy(mode = mode))
                    },
                    label = {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(mode.accentColorHex).copy(alpha = 0.25f),
                        selectedLabelColor = Color(mode.accentColorHex)
                    )
                )
            }
        }

        // --- Sliders ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Tactile Intensity
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Haptic Intensity", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "${(settings.intensity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(settings.mode.accentColorHex)
                    )
                }
                Slider(
                    value = settings.intensity,
                    onValueChange = { onSettingsChange(settings.copy(intensity = it)) },
                    valueRange = 0f..1f
                )
            }

            // Sub-Bass Boost
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sub-Bass Transducer Gain", style = MaterialTheme.typography.labelSmall)
                    Text(
                        String.format("%.1fx", settings.subBassBoost),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(settings.mode.accentColorHex)
                    )
                }
                Slider(
                    value = settings.subBassBoost,
                    onValueChange = { onSettingsChange(settings.copy(subBassBoost = it)) },
                    valueRange = 0.5f..2.0f
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // --- Action Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Turntable Touch Sync Switch Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (settings.turntableHapticsEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .border(
                        1.dp,
                        if (settings.turntableHapticsEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        haptic.performTick()
                        onSettingsChange(settings.copy(turntableHapticsEnabled = !settings.turntableHapticsEnabled))
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.VinylDisc,
                        color = if (settings.turntableHapticsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 16.dp
                    )
                    Text(
                        text = if (settings.turntableHapticsEnabled) "Turntable Sync ON" else "Turntable Sync OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (settings.turntableHapticsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Test Pulse Button
            OutlinedButton(
                onClick = {
                    val primitive = when (settings.mode) {
                        AudioHapticMode.SubBassRumble -> HapticPrimitiveType.LowTick
                        AudioHapticMode.BeatPunch -> HapticPrimitiveType.Click
                        AudioHapticMode.FullSpectrum -> HapticPrimitiveType.QuickRise
                        AudioHapticMode.VinylAcoustic -> HapticPrimitiveType.Spin
                        AudioHapticMode.Off -> HapticPrimitiveType.Tick
                    }
                    onTestPulse(primitive)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.Sparkle,
                        color = Color(settings.mode.accentColorHex),
                        size = 16.dp
                    )
                    Text("Test Pulse", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Animated Canvas rendering expanding concentric tactile resonance waves and audio-reactive glow.
 */
@Composable
private fun HapticResonanceVisualizer(
    latestPulse: HapticPulseEvent?,
    mode: AudioHapticMode,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val rippleAnim = remember { Animatable(0f) }
    val glowAnim = remember { Animatable(0f) }

    LaunchedEffect(latestPulse?.timestamp) {
        if (latestPulse != null && isEnabled) {
            glowAnim.snapTo(latestPulse.amplitude.coerceIn(0.3f, 1f))
            rippleAnim.snapTo(0f)
            rippleAnim.animateTo(1f, animationSpec = tween(durationMillis = 350))
            glowAnim.animateTo(0f, animationSpec = tween(durationMillis = 200))
        }
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
    ) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f

        val accentColor = Color(mode.accentColorHex)

        // Background grid lines
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(centerX, 0f),
            end = Offset(centerX, h),
            strokeWidth = 1f
        )

        // Center Core Indicator
        val coreRadius = 12.dp.toPx() + (glowAnim.value * 8.dp.toPx())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isEnabled) 0.8f else 0.2f),
                    accentColor.copy(alpha = 0f)
                ),
                center = Offset(centerX, centerY),
                radius = coreRadius * 2.2f
            ),
            radius = coreRadius * 2f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = if (isEnabled) accentColor else Color.DarkGray,
            radius = coreRadius * 0.6f,
            center = Offset(centerX, centerY)
        )

        // Expanding Concentric Resonance Waves
        if (rippleAnim.value > 0f && isEnabled) {
            val maxWaveRadius = minOf(centerX, centerY) * 1.5f
            val wave1 = rippleAnim.value * maxWaveRadius
            val wave2 = ((rippleAnim.value - 0.25f).coerceAtLeast(0f) / 0.75f) * maxWaveRadius

            drawCircle(
                color = accentColor.copy(alpha = (1f - rippleAnim.value) * 0.7f),
                radius = wave1,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3.dp.toPx() * (1f - rippleAnim.value))
            )

            if (wave2 > 0f) {
                drawCircle(
                    color = accentColor.copy(alpha = (1f - (rippleAnim.value - 0.25f) / 0.75f) * 0.4f),
                    radius = wave2,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
