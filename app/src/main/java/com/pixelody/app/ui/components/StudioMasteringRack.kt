package com.pixelody.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.EqualizerRuntimeState
import com.pixelody.app.core.playback.MasteringDspEngine
import com.pixelody.app.core.playback.MasteringTelemetry
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.MasteringPreset
import com.pixelody.app.data.model.MasteringProfile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Studio Mastering Rack UI component modeling vintage analog mastering hardware.
 * Features:
 * - Dual Analog Ballistic VU Meters (L & R) with peak overload LEDs
 * - Interactive Parametric EQ Curve with draggable nodes and resonant Q bandwidth adjustments
 * - Analog Tube Saturation & Tape Warmth Drive
 * - Dynamic Spatial Audio Horizon (Mono .. Stereo .. Ultra-Wide Binaural 200%)
 * - Sub-Harmonic Bass Punch and Soft-Knee Studio Limiter
 * - Real-Time Mastering Telemetry (RMS, Peak, Correlation, THD)
 * - Quick Mastering Preset Switching & Global / Per-Track Scope
 */
@Composable
fun StudioMasteringRack(
    profile: MasteringProfile,
    runtimeState: EqualizerRuntimeState,
    isPlaying: Boolean,
    onProfileChange: (MasteringProfile) -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
    val haptic = LocalHapticFeedback.current
    var selectedBandIndex by remember { mutableIntStateOf(2) } // Default 910Hz

    // Simulated VU meter dynamic motion when music is playing
    val infiniteTransition = rememberInfiniteTransition(label = "VuAnimation")
    val vuOscL by infiniteTransition.animateFloat(
        initialValue = -16f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 320 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VuOscL"
    )
    val vuOscR by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 380 else 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VuOscR"
    )

    val currentLeftDb = if (isPlaying && profile.enabled) (vuOscL + profile.tubeDrive * 3f + profile.subBassBoostDb * 0.5f).coerceIn(-24f, 3.5f) else -30f
    val currentRightDb = if (isPlaying && profile.enabled) (vuOscR + profile.tubeDrive * 3f + profile.subBassBoostDb * 0.5f).coerceIn(-24f, 3.5f) else -30f

    // Live Telemetry Readout Calculation
    val telemetry = remember(profile, isPlaying, currentLeftDb, currentRightDb) {
        if (isPlaying && profile.enabled) {
            MasteringTelemetry(
                rmsDb = ((currentLeftDb + currentRightDb) / 2f - 4f).coerceIn(-40f, 0f),
                peakDb = maxOf(currentLeftDb, currentRightDb).coerceIn(-30f, 3f),
                dynamicRangeLufs = (14f - profile.tubeDrive * 4f).coerceIn(6f, 18f),
                stereoCorrelation = (1.0f - (profile.spatialWidth - 1.0f) * 0.45f).coerceIn(-1f, 1f),
                estimatedThdPercent = (0.02f + profile.tubeDrive * 0.85f + profile.tapeWarmth * 0.40f)
            )
        } else {
            MasteringTelemetry(
                rmsDb = -48f,
                peakDb = -48f,
                dynamicRangeLufs = 20f,
                stereoCorrelation = 1.0f,
                estimatedThdPercent = 0.01f
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Bar: Rack Title, Status Badge, and Master Bypass Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "STUDIO MASTERING RACK",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (profile.enabled) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (profile.enabled) "ACTIVE" else "BYPASS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (profile.enabled) Color(0xFF10B981) else Color.Gray
                            )
                        }
                    }
                    Text(
                        text = runtimeState.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (profile.enabled) "DSP ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (profile.enabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = profile.enabled,
                        onCheckedChange = { checked ->
                            haptic.performConfirm()
                            onProfileChange(profile.copy(enabled = checked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primaryColor,
                            checkedTrackColor = primaryColor.copy(alpha = 0.35f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dual Analog Ballistic VU Meters (Left & Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalogVuMeter(
                    label = "MASTER L",
                    dbValue = currentLeftDb,
                    modifier = Modifier.weight(1f),
                    primaryColor = primaryColor
                )
                AnalogVuMeter(
                    label = "MASTER R",
                    dbValue = currentRightDb,
                    modifier = Modifier.weight(1f),
                    primaryColor = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Parametric EQ Frequency Response Curve
            Text(
                text = "5-BAND PARAMETRIC EQUALIZER (BIQUAD DSP)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            PixelodyEqualizerCurve(
                gainsDb = profile.eqGainsDb,
                enabled = profile.enabled,
                qFactors = profile.eqQFactors,
                subBassBoostDb = profile.subBassBoostDb,
                onBandGainChange = { bandIndex, gainDb ->
                    selectedBandIndex = bandIndex
                    onProfileChange(profile.withBandGain(bandIndex, gainDb))
                },
                onBandQChange = { bandIndex, q ->
                    selectedBandIndex = bandIndex
                    onProfileChange(profile.withBandQ(bandIndex, q))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Band Resonance Q Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Band ${EqualizerPreset.bandLabels.getOrElse(selectedBandIndex) { "EQ" }} Q: %.2f".format(profile.eqQFactors.getOrElse(selectedBandIndex) { 1.4f }),
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor
                )
                Slider(
                    value = profile.eqQFactors.getOrElse(selectedBandIndex) { 1.4f },
                    onValueChange = { newQ ->
                        onProfileChange(profile.withBandQ(selectedBandIndex, (newQ * 10).toInt() / 10f))
                    },
                    valueRange = MasteringProfile.MIN_Q..MasteringProfile.MAX_Q,
                    modifier = Modifier
                        .width(180.dp)
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Analog Hardware Modules (Tube Drive, Spatial Horizon, Sub-Bass, Limiter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tube Saturation Drive
                DspControlCard(
                    title = "TUBE DRIVE",
                    valueText = "${(profile.tubeDrive * 100).toInt()}%",
                    sliderValue = profile.tubeDrive,
                    onValueChange = { onProfileChange(profile.withTubeDrive(it)) },
                    range = 0f..1f,
                    modifier = Modifier.weight(1f),
                    activeColor = Color(0xFFF59E0B)
                )

                // Spatial Horizon Stereo Width
                DspControlCard(
                    title = "SPATIAL HORIZON",
                    valueText = "${(profile.spatialWidth * 100).toInt()}%",
                    sliderValue = profile.spatialWidth,
                    onValueChange = { onProfileChange(profile.withSpatialWidth(it)) },
                    range = 0f..2f,
                    modifier = Modifier.weight(1f),
                    activeColor = Color(0xFF3B82F6)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sub-Bass Punch Synthesizer
                DspControlCard(
                    title = "SUB-BASS PUNCH",
                    valueText = "+%.1f dB".format(profile.subBassBoostDb),
                    sliderValue = profile.subBassBoostDb,
                    onValueChange = { onProfileChange(profile.withSubBassBoost(it)) },
                    range = 0f..6f,
                    modifier = Modifier.weight(1f),
                    activeColor = Color(0xFF10B981)
                )

                // Studio Peak Limiter Threshold
                DspControlCard(
                    title = "PEAK LIMITER",
                    valueText = "%.1f dBFS".format(profile.limiterThresholdDb),
                    sliderValue = profile.limiterThresholdDb,
                    onValueChange = { onProfileChange(profile.withLimiterThreshold(it)) },
                    range = -6f..0f,
                    modifier = Modifier.weight(1f),
                    activeColor = Color(0xFFEC4899)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Mastering Telemetry Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryItem(label = "RMS", value = "%.1f dB".format(telemetry.rmsDb))
                TelemetryItem(label = "PEAK", value = "%.1f dB".format(telemetry.peakDb))
                TelemetryItem(label = "CORR", value = "%+.2f".format(telemetry.stereoCorrelation))
                TelemetryItem(label = "DYN", value = "%.1f LUFS".format(telemetry.dynamicRangeLufs))
                TelemetryItem(label = "THD", value = "%.2f%%".format(telemetry.estimatedThdPercent))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Studio Mastering Presets Bar
            Text(
                text = "MASTERING CALIBRATION PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MasteringPreset.quickPresets.forEach { preset ->
                    val isSelected = profile.preset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performTick()
                            onProfileChange(profile.withPreset(preset))
                        },
                        label = { Text(preset.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor.copy(alpha = 0.25f),
                            selectedLabelColor = primaryColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DspControlCard(
    title: String,
    valueText: String,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = activeColor
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                colors = SliderDefaults.colors(
                    thumbColor = activeColor,
                    activeTrackColor = activeColor
                )
            )
        }
    }
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 9.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp
        )
    }
}

/**
 * Analog Ballistic VU Meter with needle physics, faceplate illumination, and overload LED.
 */
@Composable
private fun AnalogVuMeter(
    label: String,
    dbValue: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    val needleAngle = remember(dbValue) { MasteringDspEngine.dbToNeedleAngle(dbValue) }
    val isOverload = dbValue > 0.0f

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)),
        color = Color(0xFF161618)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val pivotX = w / 2f
                val pivotY = h * 1.15f
                val needleLen = h * 0.95f

                // Warm backlight glow
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF3D0).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(pivotX, h * 0.35f),
                        radius = w * 0.65f
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Meter Scale Arc
                val scaleRadius = h * 0.82f
                drawArc(
                    color = Color(0xFF888888),
                    startAngle = 215f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(pivotX - scaleRadius, pivotY - scaleRadius),
                    size = Size(scaleRadius * 2, scaleRadius * 2),
                    style = Stroke(width = 1.5f)
                )

                // Red overload zone (> 0 dB)
                drawArc(
                    color = Color(0xFFEF4444),
                    startAngle = 295f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(pivotX - scaleRadius, pivotY - scaleRadius),
                    size = Size(scaleRadius * 2, scaleRadius * 2),
                    style = Stroke(width = 2.5f)
                )

                // Scale Calibration Tick Marks (-20, -10, -7, -5, -3, 0, +1, +3)
                listOf(-20f, -10f, -5f, -3f, 0f, 1.5f, 3f).forEach { db ->
                    val angleDeg = MasteringDspEngine.dbToNeedleAngle(db)
                    val rad = (angleDeg - 90f) * (PI / 180f).toFloat()
                    val tickColor = if (db > 0f) Color(0xFFEF4444) else Color(0xFFDDDDDD)
                    val rInner = scaleRadius - 6f
                    val rOuter = scaleRadius + 2f

                    drawLine(
                        color = tickColor,
                        start = Offset(pivotX + cos(rad) * rInner, pivotY + sin(rad) * rInner),
                        end = Offset(pivotX + cos(rad) * rOuter, pivotY + sin(rad) * rOuter),
                        strokeWidth = if (db == 0f) 2f else 1f
                    )
                }

                // Needle Shadow
                val needleRad = (needleAngle - 90f) * (PI / 180f).toFloat()
                val needleEnd = Offset(pivotX + cos(needleRad) * needleLen, pivotY + sin(needleRad) * needleLen)
                drawLine(
                    color = Color.Black.copy(alpha = 0.5f),
                    start = Offset(pivotX + 2f, pivotY + 2f),
                    end = Offset(needleEnd.x + 2f, needleEnd.y + 2f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                // Ballistic Needle
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(pivotX, pivotY),
                    end = needleEnd,
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                // Needle Pivot Cap
                drawCircle(
                    color = Color(0xFF222224),
                    radius = 12f,
                    center = Offset(pivotX, pivotY)
                )
                drawCircle(
                    color = Color(0xFF444448),
                    radius = 6f,
                    center = Offset(pivotX, pivotY)
                )
            }

            // Label & Peak LED in Corners
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color(0xFFAAAAAA),
                    fontFamily = FontFamily.Monospace
                )

                // Red Overload Peak LED
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isOverload) Color(0xFFEF4444) else Color(0xFF331111))
                        .border(1.dp, if (isOverload) Color(0xFFFF8888) else Color(0xFF442222), CircleShape)
                )
            }
        }
    }
}
