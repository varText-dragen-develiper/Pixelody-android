package com.pixelody.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.StemChannelState
import com.pixelody.app.data.model.StemChannelTelemetry
import com.pixelody.app.data.model.StemPreset
import com.pixelody.app.data.model.StemType
import com.pixelody.app.data.model.StemsIsolatorSettings
import com.pixelody.app.data.model.StemsIsolatorTelemetry
import kotlin.math.PI
import kotlin.math.sin

/**
 * StemsDjMixingRack: Interactive 4-Channel Real-Time Stems Isolator & DJ Mixing Rack.
 * Provides 4 tactile vertical channel strips (Vocals, Drums, Bass, Instruments) with multi-segment LED peak
 * ladder meters, smooth faders, illuminated Solo/Mute buttons, 3-band parametric EQ, DJ sweep filters,
 * animated stacked stem energy waveform visualizer, master crossfader, and 1-tap DJ presets.
 */
@Composable
fun StemsDjMixingRack(
    settings: StemsIsolatorSettings,
    telemetry: StemsIsolatorTelemetry,
    trackTitle: String,
    trackArtist: String,
    onUpdateSettings: (StemsIsolatorSettings) -> Unit,
    onSetStemGain: (StemType, Float) -> Unit,
    onToggleMute: (StemType) -> Unit,
    onToggleSolo: (StemType) -> Unit,
    onSetStemPan: (StemType, Float) -> Unit,
    onSetStemEq: (StemType, Float, Float, Float) -> Unit,
    onSetStemFilter: (StemType, Float) -> Unit,
    onSetCrossfaderPosition: (Float) -> Unit,
    onApplyPreset: (StemPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val masterAccent = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, masterAccent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            StemsHeader(
                settings = settings,
                activePreset = settings.activePreset,
                onToggleEnabled = { onUpdateSettings(settings.copy(isEnabled = !settings.isEnabled)) }
            )

            // 1. Stacked 4-Layer Animated Stem Energy Waveform Canvas
            StackedStemWaveformCanvas(
                telemetry = telemetry,
                isEnabled = settings.isEnabled
            )

            // 2. 4-Channel Tactile Mixing Strips Console
            FourChannelStripsConsole(
                settings = settings,
                telemetry = telemetry,
                onSetStemGain = onSetStemGain,
                onToggleMute = onToggleMute,
                onToggleSolo = onToggleSolo,
                onSetStemEq = onSetStemEq,
                onSetStemFilter = onSetStemFilter
            )

            // 3. Master DJ Crossfader Section (Deck A vs Deck B)
            MasterDjCrossfader(
                crossfaderPosition = settings.crossfaderPosition,
                deckAGain = telemetry.crossfaderDeckAGain,
                deckBGain = telemetry.crossfaderDeckBGain,
                onSetCrossfaderPosition = onSetCrossfaderPosition
            )

            // 4. Quick DJ Stem Extraction Presets Carousel
            StemPresetCarousel(
                selectedPreset = settings.activePreset,
                onSelectPreset = onApplyPreset
            )

            // 5. Master Output Gain & Limiter Stage
            MasterOutputControls(
                masterGainDb = settings.masterGainDb,
                masterMeterNorm = telemetry.masterMeterNormalized,
                masterPeakDb = telemetry.masterPeakDb,
                onUpdateMasterGain = { onUpdateSettings(settings.copy(masterGainDb = it)) }
            )
        }
    }
}

/**
 * Top Header with Stems Title, active preset chip, and master toggle.
 */
@Composable
private fun StemsHeader(
    settings: StemsIsolatorSettings,
    activePreset: StemPreset,
    onToggleEnabled: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.Sliders,
                        color = MaterialTheme.colorScheme.primary,
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
                        text = "4-CHANNEL STEMS ISOLATOR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (settings.isEnabled) "ACTIVE" else "BYPASS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (settings.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${activePreset.title} • Real-Time Crossover",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Switch(
            checked = settings.isEnabled,
            onCheckedChange = { onToggleEnabled() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/**
 * Stacked 4-Layer Animated Stem Energy Waveform Canvas.
 */
@Composable
private fun StackedStemWaveformCanvas(
    telemetry: StemsIsolatorTelemetry,
    isEnabled: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveOscillation")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF090B10),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawStackedStemWaves(telemetry, isEnabled, wavePhase)
        }
    }
}

private fun DrawScope.drawStackedStemWaves(
    telemetry: StemsIsolatorTelemetry,
    isEnabled: Boolean,
    wavePhaseDeg: Float
) {
    val w = size.width
    val h = size.height

    // Grid reference lines
    drawLine(Color(0xFF1E293B), Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 1f)
    drawLine(Color(0xFF1E293B), Offset(0f, h * 0.50f), Offset(w, h * 0.50f), strokeWidth = 1f)
    drawLine(Color(0xFF1E293B), Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 1f)

    val stems = StemType.values()
    val bandHeight = h / stems.size.toFloat()

    stems.forEachIndexed { index, stem ->
        val stemColor = Color(stem.colorHex)
        val chTelem = telemetry.channelTelemetry[stem] ?: StemChannelTelemetry(stem)
        val energy = if (isEnabled) chTelem.meterNormalized.coerceIn(0.05f, 1.0f) else 0.05f

        val centerY = (index * bandHeight) + (bandHeight / 2f)
        val maxAmp = (bandHeight * 0.42f) * energy

        val path = Path().apply {
            moveTo(0f, centerY)
            val steps = 40
            for (i in 0..steps) {
                val x = (i.toFloat() / steps.toFloat()) * w
                val phaseRad = ((wavePhaseDeg * (index + 1) * 0.65f) + (i * 18f)) * PI / 180.0
                val y = centerY + (sin(phaseRad) * maxAmp).toFloat()
                lineTo(x, y)
            }
        }

        // Draw glowing wave stroke
        drawPath(
            path = path,
            color = stemColor.copy(alpha = if (isEnabled) 0.85f else 0.3f),
            style = Stroke(width = 2.0f, cap = StrokeCap.Round)
        )
    }
}

/**
 * 4-Channel Tactile Mixing Strips Console.
 */
@Composable
private fun FourChannelStripsConsole(
    settings: StemsIsolatorSettings,
    telemetry: StemsIsolatorTelemetry,
    onSetStemGain: (StemType, Float) -> Unit,
    onToggleMute: (StemType) -> Unit,
    onToggleSolo: (StemType) -> Unit,
    onSetStemEq: (StemType, Float, Float, Float) -> Unit,
    onSetStemFilter: (StemType, Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StemType.values().forEach { stemType ->
            val chState = settings.channels[stemType] ?: StemChannelState(stemType)
            val chTelem = telemetry.channelTelemetry[stemType] ?: StemChannelTelemetry(stemType)

            ChannelStripColumn(
                stemType = stemType,
                channelState = chState,
                telemetry = chTelem,
                modifier = Modifier.weight(1f),
                onGainChange = { onSetStemGain(stemType, it) },
                onToggleMute = { onToggleMute(stemType) },
                onToggleSolo = { onToggleSolo(stemType) },
                onEqChange = { l, m, h -> onSetStemEq(stemType, l, m, h) },
                onFilterChange = { onSetStemFilter(stemType, it) }
            )
        }
    }
}

@Composable
private fun ChannelStripColumn(
    stemType: StemType,
    channelState: StemChannelState,
    telemetry: StemChannelTelemetry,
    modifier: Modifier = Modifier,
    onGainChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onEqChange: (Float, Float, Float) -> Unit,
    onFilterChange: (Float) -> Unit
) {
    val stemColor = Color(stemType.colorHex)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F141E),
        border = BorderStroke(
            1.dp,
            if (channelState.isSoloed) stemColor else Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Channel Header Badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = stemColor.copy(alpha = 0.22f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${stemType.badgeGlyph} ${stemType.title.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = stemColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Multi-Segment LED Ladder Meter & Vertical Fader Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LED Ladder Meter Canvas
                LedLadderMeter(
                    meterNormalized = telemetry.meterNormalized,
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight()
                )

                // Vertical Fader Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (channelState.isMuted) "MUTED" else "${if (channelState.gainDb > 0) "+" else ""}${channelState.gainDb.toInt()}dB",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (channelState.isMuted) Color(0xFFEF4444) else stemColor
                    )

                    // Vertical Slider represented horizontally in compact column
                    Slider(
                        value = channelState.gainDb,
                        onValueChange = onGainChange,
                        valueRange = -40f..6f,
                        colors = SliderDefaults.colors(
                            thumbColor = stemColor,
                            activeTrackColor = stemColor,
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.height(24.dp)
                    )

                    Text(
                        text = "FADER",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 7.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // 3. Tactile SOLO (S) and MUTE (M) Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // SOLO Button
                TactileStripButton(
                    label = "S",
                    isActive = channelState.isSoloed,
                    activeColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = onToggleSolo
                )

                // MUTE Button
                TactileStripButton(
                    label = "M",
                    isActive = channelState.isMuted,
                    activeColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = onToggleMute
                )
            }

            // 4. DJ Sweep Filter Knob / Slider (LPF < 0.5 > HPF)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        channelState.filterCutoffNormalized < 0.47f -> "LPF"
                        channelState.filterCutoffNormalized > 0.53f -> "HPF"
                        else -> "FLT OFF"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (channelState.filterCutoffNormalized in 0.47f..0.53f) MaterialTheme.colorScheme.onSurfaceVariant else stemColor
                )

                Slider(
                    value = channelState.filterCutoffNormalized,
                    onValueChange = onFilterChange,
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = stemColor,
                        activeTrackColor = stemColor,
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}

/**
 * 10-Segment LED Ladder Meter.
 */
@Composable
private fun LedLadderMeter(
    meterNormalized: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalSegments = 10
        val segHeight = (size.height / totalSegments.toFloat()) - 1.5f
        val segWidth = size.width

        for (i in 0 until totalSegments) {
            val segIdxFromBottom = totalSegments - 1 - i
            val segThreshold = (segIdxFromBottom.toFloat() / totalSegments.toFloat())
            val isLit = meterNormalized >= segThreshold

            val segColor = when {
                segIdxFromBottom >= 8 -> Color(0xFFEF4444) // Red Clip
                segIdxFromBottom >= 6 -> Color(0xFFF59E0B) // Amber
                else -> Color(0xFF10B981) // Green Normal
            }

            val y = i * (segHeight + 1.5f)
            drawRoundRect(
                color = if (isLit) segColor else segColor.copy(alpha = 0.15f),
                topLeft = Offset(0f, y),
                size = Size(segWidth, segHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

@Composable
private fun TactileStripButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) activeColor.copy(alpha = 0.35f) else Color(0xFF1E293B),
        border = BorderStroke(
            1.dp,
            if (isActive) activeColor else Color(0xFF334155)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Horizontal Master DJ Crossfader with Deck A and Deck B balance.
 */
@Composable
private fun MasterDjCrossfader(
    crossfaderPosition: Float,
    deckAGain: Float,
    deckBGain: Float,
    onSetCrossfaderPosition: (Float) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F141E),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DECK A (DRUMS & BASS)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B)
                )

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSetCrossfaderPosition(0.0f) },
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "CENTER",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "DECK B (VOCALS & INST)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF06B6D4)
                )
            }

            Slider(
                value = crossfaderPosition,
                onValueChange = onSetCrossfaderPosition,
                valueRange = -1.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = Color(0xFF06B6D4),
                    inactiveTrackColor = Color(0xFFF59E0B)
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

/**
 * 1-Tap DJ Presets Carousel.
 */
@Composable
private fun StemPresetCarousel(
    selectedPreset: StemPreset,
    onSelectPreset: (StemPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "DJ STEM EXTRACTION PRESETS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(StemPreset.values(), key = { it.name }) { preset ->
                val isSelected = preset == selectedPreset
                val accentColor = when (preset) {
                    StemPreset.FullMix -> MaterialTheme.colorScheme.primary
                    StemPreset.AcapellaExtract -> Color(0xFF06B6D4)
                    StemPreset.InstrumentalKaraoke -> Color(0xFF10B981)
                    StemPreset.DrumAndBass -> Color(0xFFF59E0B)
                    StemPreset.VocalDuck -> Color(0xFF8B5CF6)
                    StemPreset.BassBoostDrop -> Color(0xFFEC4899)
                }

                Surface(
                    modifier = Modifier
                        .width(165.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectPreset(preset) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = preset.subtitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )

                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Master Output Gain Slider & Master Peak Meter.
 */
@Composable
private fun MasterOutputControls(
    masterGainDb: Float,
    masterMeterNorm: Float,
    masterPeakDb: Float,
    onUpdateMasterGain: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Master Output Level",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${if (masterGainDb > 0) "+" else ""}${String.format("%.1f", masterGainDb)} dB (${String.format("%.1f", masterPeakDb)} dB peak)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = masterGainDb,
            onValueChange = onUpdateMasterGain,
            valueRange = -24f..6f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}
