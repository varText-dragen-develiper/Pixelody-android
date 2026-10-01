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
import com.pixelody.app.data.model.CassetteShellTheme
import com.pixelody.app.data.model.CassetteTapeSettings
import com.pixelody.app.data.model.TapeFormulation
import com.pixelody.app.data.model.TapeMagneticsTelemetry
import com.pixelody.app.data.model.TapeNoiseReduction
import com.pixelody.app.data.model.TapeTransportState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * CassetteTapeDeckView: Interactive Retro Cassette Deck & Reel-to-Reel Magnetics Studio.
 * Includes animated dual-spool tape transport with rotating 6-tooth gear hubs, dynamic tape pack
 * volume conservation, cassette shell theme rendering, dual analog needle VU meters with ballistics,
 * tactile mechanical piano-key transport buttons, and tape formulation physics controls.
 */
@Composable
fun CassetteTapeDeckView(
    settings: CassetteTapeSettings,
    telemetry: TapeMagneticsTelemetry,
    trackTitle: String,
    trackArtist: String,
    playbackPositionMs: Long,
    trackDurationMs: Long,
    onUpdateSettings: (CassetteTapeSettings) -> Unit,
    onSetTransportState: (TapeTransportState) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekRewind: () -> Unit,
    onSeekFastForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val harmonicColor = Color(settings.formulation.harmonicColorHex)
    val themeAccent = Color(settings.shellTheme.accentColorHex)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, harmonicColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            TapeDeckHeader(
                settings = settings,
                harmonicColor = harmonicColor,
                onToggleEnabled = { onUpdateSettings(settings.copy(isEnabled = !settings.isEnabled)) }
            )

            // 1. Dual Analog Illuminated VU Meters
            DualAnalogVuMeters(
                telemetry = telemetry,
                harmonicColor = harmonicColor
            )

            // 2. Animated Cassette Tape Shell & Dual-Spool Transport Canvas
            CassetteTapeCanvas(
                settings = settings,
                telemetry = telemetry,
                trackTitle = trackTitle,
                trackArtist = trackArtist,
                playbackPositionMs = playbackPositionMs,
                trackDurationMs = trackDurationMs
            )

            // 3. Tactile Mechanical Piano-Key Transport Row
            MechanicalPianoKeyTransport(
                transportState = telemetry.transportState,
                harmonicColor = harmonicColor,
                onSetTransportState = onSetTransportState,
                onTogglePlayPause = onTogglePlayPause,
                onSeekRewind = onSeekRewind,
                onSeekFastForward = onSeekFastForward
            )

            // 4. Magnetics Telemetry HUD
            MagneticsTelemetryHud(
                telemetry = telemetry,
                harmonicColor = harmonicColor
            )

            // 5. Tape Formulation Carousel
            TapeFormulationCarousel(
                selectedFormulation = settings.formulation,
                onSelectFormulation = { onUpdateSettings(settings.copy(formulation = it, driveGain = it.defaultDriveGain)) }
            )

            // 6. Noise Reduction Companding Selector
            NoiseReductionSelector(
                selectedNr = settings.noiseReduction,
                harmonicColor = harmonicColor,
                onSelectNr = { onUpdateSettings(settings.copy(noiseReduction = it)) }
            )

            // 7. Cassette Shell Aesthetic Theme Picker
            CassetteShellThemePicker(
                selectedTheme = settings.shellTheme,
                harmonicColor = harmonicColor,
                onSelectTheme = { onUpdateSettings(settings.copy(shellTheme = it)) }
            )

            // 8. Magnetic Physics Fine-Tuning Sliders
            MagneticsParameterSliders(
                settings = settings,
                harmonicColor = harmonicColor,
                onUpdateSettings = onUpdateSettings
            )
        }
    }
}

/**
 * Deck Header with formulation badge and bypass switch.
 */
@Composable
private fun TapeDeckHeader(
    settings: CassetteTapeSettings,
    harmonicColor: Color,
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
                color = harmonicColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.VinylDisc,
                        color = harmonicColor,
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
                        text = "MAGNETIC TAPE STUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (settings.isEnabled) "MAGNETICS ON" else "BYPASS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (settings.isEnabled) harmonicColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${settings.formulation.title} • ${settings.noiseReduction.badgeText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = harmonicColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Switch(
            checked = settings.isEnabled,
            onCheckedChange = { onToggleEnabled() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = harmonicColor,
                checkedTrackColor = harmonicColor.copy(alpha = 0.4f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/**
 * Dual Analog Illuminated VU Meters with ballistic needle response.
 */
@Composable
private fun DualAnalogVuMeters(
    telemetry: TapeMagneticsTelemetry,
    harmonicColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF14120E),
        border = BorderStroke(1.dp, Color(0xFF3E3628))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left VU Meter
            AnalogVuMeterUnit(
                channelLabel = "LEFT (CH 1)",
                needleNormalized = telemetry.leftVuNeedleNormalized,
                modifier = Modifier.weight(1f)
            )

            // Right VU Meter
            AnalogVuMeterUnit(
                channelLabel = "RIGHT (CH 2)",
                needleNormalized = telemetry.rightVuNeedleNormalized,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AnalogVuMeterUnit(
    channelLabel: String,
    needleNormalized: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFAF6E9),
        border = BorderStroke(1.dp, Color(0xFFB5A88E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawVuMeterFace(needleNormalized)
            }

            Text(
                text = channelLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = Color(0xFF4A3E2C),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
            )
        }
    }
}

private fun DrawScope.drawVuMeterFace(needleNormalized: Float) {
    val w = size.width
    val h = size.height

    // Warm backlit glow gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFDF5), Color(0xFFE8DFCA))
        )
    )

    val pivot = Offset(w * 0.50f, h * 1.15f)
    val radius = h * 0.95f

    // Scale Arcs (Black normal zone, Red peak zone > 0 VU)
    val startAngle = 215f
    val normalSweep = 70f
    val redSweep = 40f

    // Scale Arc Line
    drawArc(
        color = Color(0xFF33291F),
        startAngle = startAngle,
        sweepAngle = normalSweep,
        useCenter = false,
        topLeft = Offset(pivot.x - radius, pivot.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 1.5f)
    )
    drawArc(
        color = Color(0xFFDC2626),
        startAngle = startAngle + normalSweep,
        sweepAngle = redSweep,
        useCenter = false,
        topLeft = Offset(pivot.x - radius, pivot.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 2.5f)
    )

    // Tick marks
    val tickSteps = 8
    for (i in 0..tickSteps) {
        val frac = i.toFloat() / tickSteps
        val angleDeg = startAngle + (frac * (normalSweep + redSweep))
        val rad = angleDeg * PI / 180.0
        val isRed = frac > 0.65f
        val tickColor = if (isRed) Color(0xFFDC2626) else Color(0xFF33291F)

        val rOuter = radius
        val rInner = radius - (if (i % 2 == 0) 7f else 4f)

        val p1 = Offset((pivot.x + rInner * cos(rad)).toFloat(), (pivot.y + rInner * sin(rad)).toFloat())
        val p2 = Offset((pivot.x + rOuter * cos(rad)).toFloat(), (pivot.y + rOuter * sin(rad)).toFloat())

        drawLine(tickColor, p1, p2, strokeWidth = if (i % 2 == 0) 1.5f else 1.0f)
    }

    // Needle Calculation (Map 0.0 -> 1.0 to angle)
    val clampedNeedle = needleNormalized.coerceIn(0.0f, 1.25f)
    val needleAngleDeg = startAngle + (clampedNeedle * (normalSweep + redSweep) * 0.85f)
    val needleRad = needleAngleDeg * PI / 180.0

    val needleTip = Offset(
        (pivot.x + (radius - 2f) * cos(needleRad)).toFloat(),
        (pivot.y + (radius - 2f) * sin(needleRad)).toFloat()
    )

    // Needle shadow
    drawLine(
        color = Color(0x33000000),
        start = Offset(pivot.x + 2f, pivot.y + 2f),
        end = Offset(needleTip.x + 2f, needleTip.y + 2f),
        strokeWidth = 2.0f,
        cap = StrokeCap.Round
    )

    // Needle
    drawLine(
        color = Color(0xFFB91C1C),
        start = pivot,
        end = needleTip,
        strokeWidth = 1.6f,
        cap = StrokeCap.Round
    )

    // Pivot Cap
    drawCircle(
        color = Color(0xFF262626),
        radius = 5.5f,
        center = Offset(w * 0.50f, h * 0.96f)
    )
}

/**
 * Cassette Tape Body & Dual Rotating Spools Canvas.
 */
@Composable
private fun CassetteTapeCanvas(
    settings: CassetteTapeSettings,
    telemetry: TapeMagneticsTelemetry,
    trackTitle: String,
    trackArtist: String,
    playbackPositionMs: Long,
    trackDurationMs: Long
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tapeSpin")
    val spinPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spoolAngle"
    )

    val shellTheme = settings.shellTheme
    val bodyColor = Color(shellTheme.bodyColorHex)
    val labelColor = Color(shellTheme.labelColorHex)
    val hubColor = Color(shellTheme.spoolHubColorHex)
    val accentColor = Color(shellTheme.accentColorHex)
    val windowBorderColor = Color(shellTheme.windowBorderColorHex)

    // Calculate dynamic rotation angle based on RPM and transport speed
    val speedMult = telemetry.transportState.motorSpeedMultiplier
    val supplyAngle = (spinPhase * (telemetry.spoolPhysics.supplyRpm / 45f) * speedMult) % 360f
    val takeupAngle = (spinPhase * (telemetry.spoolPhysics.takeupRpm / 45f) * speedMult) % 360f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.58f)
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0C0E14),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCassetteTapeShell(
                    bodyColor = bodyColor,
                    labelColor = labelColor,
                    hubColor = hubColor,
                    accentColor = accentColor,
                    windowBorderColor = windowBorderColor,
                    supplyRadiusRatio = telemetry.spoolPhysics.supplyRadiusRatio,
                    takeupRadiusRatio = telemetry.spoolPhysics.takeupRadiusRatio,
                    supplyAngleDeg = supplyAngle,
                    takeupAngleDeg = takeupAngle
                )
            }

            // Cassette Sticker Title & Track Info Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
                    .fillMaxWidth(0.72f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SIDE A",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                    Text(
                        text = settings.formulation.subtitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = settings.noiseReduction.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = trackTitle.ifBlank { "Cassette Master Track" },
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = trackArtist.ifBlank { "Analog Studio Session" },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tape Counter 3-Digit Display at Top-Right
            val counterProgress = if (trackDurationMs > 0L) {
                ((playbackPositionMs.toFloat() / trackDurationMs.toFloat()) * 999f).toInt().coerceIn(0, 999)
            } else 0

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 16.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black,
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Text(
                    text = String.format("%03d", counterProgress),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawCassetteTapeShell(
    bodyColor: Color,
    labelColor: Color,
    hubColor: Color,
    accentColor: Color,
    windowBorderColor: Color,
    supplyRadiusRatio: Float,
    takeupRadiusRatio: Float,
    supplyAngleDeg: Float,
    takeupAngleDeg: Float
) {
    val w = size.width
    val h = size.height

    // 1. Outer Cassette Shell
    val shellPadding = 12f
    val shellRect = Size(w - (shellPadding * 2f), h - (shellPadding * 2f))

    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(shellPadding, shellPadding),
        size = shellRect,
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = windowBorderColor.copy(alpha = 0.5f),
        topLeft = Offset(shellPadding, shellPadding),
        size = shellRect,
        cornerRadius = CornerRadius(14f, 14f),
        style = Stroke(width = 2.0f)
    )

    // Corner Screw Holes (4 screws)
    val screwOffset = 22f
    val screws = listOf(
        Offset(screwOffset, screwOffset),
        Offset(w - screwOffset, screwOffset),
        Offset(screwOffset, h - screwOffset),
        Offset(w - screwOffset, h - screwOffset)
    )
    screws.forEach { sPos ->
        drawCircle(Color(0xFF0F172A), radius = 4.5f, center = sPos)
        drawCircle(Color(0xFF64748B), radius = 4.5f, center = sPos, style = Stroke(width = 1f))
        drawLine(Color(0xFF94A3B8), Offset(sPos.x - 2.5f, sPos.y), Offset(sPos.x + 2.5f, sPos.y), strokeWidth = 1f)
    }

    // 2. Center J-Card / Label Section
    val labelWidth = w * 0.76f
    val labelHeight = h * 0.72f
    val labelTopLeft = Offset((w - labelWidth) / 2f, h * 0.08f)

    drawRoundRect(
        color = labelColor,
        topLeft = labelTopLeft,
        size = Size(labelWidth, labelHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // 3. Clear Center Viewing Window
    val windowWidth = w * 0.54f
    val windowHeight = h * 0.38f
    val windowTopLeft = Offset((w - windowWidth) / 2f, h * 0.36f)

    drawRoundRect(
        color = Color(0xFF080B10).copy(alpha = 0.92f),
        topLeft = windowTopLeft,
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = windowBorderColor,
        topLeft = windowTopLeft,
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2f)
    )

    // Center Spool Hub Coordinates
    val leftSpoolCenter = Offset(w * 0.34f, h * 0.55f)
    val rightSpoolCenter = Offset(w * 0.66f, h * 0.55f)

    val minHubRadius = w * 0.065f
    val maxPackRadius = w * 0.125f

    val supplyPackRadius = minHubRadius + (maxPackRadius - minHubRadius) * supplyRadiusRatio
    val takeupPackRadius = minHubRadius + (maxPackRadius - minHubRadius) * takeupRadiusRatio

    // 4. Dynamic Magnetic Oxide Tape Packs (Dark Brown / Bronze)
    val tapeColor = Color(0xFF221711)
    drawCircle(color = tapeColor, radius = supplyPackRadius, center = leftSpoolCenter)
    drawCircle(color = Color(0xFF3D2B20), radius = supplyPackRadius, center = leftSpoolCenter, style = Stroke(width = 1.5f))

    drawCircle(color = tapeColor, radius = takeupPackRadius, center = rightSpoolCenter)
    drawCircle(color = Color(0xFF3D2B20), radius = takeupPackRadius, center = rightSpoolCenter, style = Stroke(width = 1.5f))

    // Tape Path Ribbon along the bottom
    val bottomGuideLeft = Offset(w * 0.22f, h * 0.82f)
    val bottomGuideRight = Offset(w * 0.78f, h * 0.82f)
    val tapeRibbonPath = Path().apply {
        moveTo(leftSpoolCenter.x, leftSpoolCenter.y + supplyPackRadius)
        lineTo(bottomGuideLeft.x, bottomGuideLeft.y)
        lineTo(bottomGuideRight.x, bottomGuideRight.y)
        lineTo(rightSpoolCenter.x, rightSpoolCenter.y + takeupPackRadius)
    }
    drawPath(tapeRibbonPath, color = Color(0xFF1B110B), style = Stroke(width = 4.0f))

    // 5. 6-Tooth Rotating Gear Spools
    drawRotatingGearSpool(leftSpoolCenter, minHubRadius, supplyAngleDeg, hubColor, accentColor)
    drawRotatingGearSpool(rightSpoolCenter, minHubRadius, takeupAngleDeg, hubColor, accentColor)

    // 6. Bottom Trapezoid Head Chamber
    val trapTop = h * 0.78f
    val trapBottom = h - shellPadding
    val trapPath = Path().apply {
        moveTo(w * 0.20f, trapTop)
        lineTo(w * 0.80f, trapTop)
        lineTo(w * 0.74f, trapBottom)
        lineTo(w * 0.26f, trapBottom)
        close()
    }
    drawPath(trapPath, color = Color(0xFF0F172A).copy(alpha = 0.95f))
    drawPath(trapPath, color = windowBorderColor.copy(alpha = 0.4f), style = Stroke(width = 1.5f))

    // Magnetic Playback Head Node & Guide Rollers
    drawCircle(Color(0xFFE2E8F0), radius = 6f, center = Offset(w * 0.50f, h * 0.88f))
    drawCircle(Color(0xFF64748B), radius = 4.5f, center = Offset(w * 0.32f, h * 0.88f))
    drawCircle(Color(0xFF64748B), radius = 4.5f, center = Offset(w * 0.68f, h * 0.88f))
}

private fun DrawScope.drawRotatingGearSpool(
    center: Offset,
    hubRadius: Float,
    angleDeg: Float,
    hubColor: Color,
    accentColor: Color
) {
    // White / Theme Hub Ring
    drawCircle(color = hubColor, radius = hubRadius, center = center)
    drawCircle(color = Color(0xFF0F172A), radius = hubRadius * 0.55f, center = center)

    // 6 Prongs / Gear Teeth
    val teeth = 6
    val toothLength = hubRadius * 0.42f
    val toothWidth = 3.5f

    for (i in 0 until teeth) {
        val totalAngle = (angleDeg + (i * 60f)) * PI / 180.0
        val pInner = Offset(
            (center.x + (hubRadius * 0.45f) * cos(totalAngle)).toFloat(),
            (center.y + (hubRadius * 0.45f) * sin(totalAngle)).toFloat()
        )
        val pOuter = Offset(
            (center.x + (hubRadius * 0.45f + toothLength) * cos(totalAngle)).toFloat(),
            (center.y + (hubRadius * 0.45f + toothLength) * sin(totalAngle)).toFloat()
        )

        drawLine(
            color = accentColor,
            start = pInner,
            end = pOuter,
            strokeWidth = toothWidth,
            cap = StrokeCap.Round
        )
    }

    // Center Spindle Hole
    drawCircle(color = Color.Black, radius = hubRadius * 0.28f, center = center)
}

/**
 * Vintage Piano-Key Transport Row.
 */
@Composable
private fun MechanicalPianoKeyTransport(
    transportState: TapeTransportState,
    harmonicColor: Color,
    onSetTransportState: (TapeTransportState) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekRewind: () -> Unit,
    onSeekFastForward: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // EJECT
        PianoKeyButton(
            label = "EJECT",
            glyph = TransportGlyphType.Close,
            isActive = transportState == TapeTransportState.Ejected,
            harmonicColor = harmonicColor,
            modifier = Modifier.weight(1f),
            onClick = { onSetTransportState(TapeTransportState.Ejected) }
        )

        // REWIND
        PianoKeyButton(
            label = "REWIND",
            glyph = TransportGlyphType.Previous,
            isActive = transportState == TapeTransportState.Rewinding,
            harmonicColor = harmonicColor,
            modifier = Modifier.weight(1f),
            onClick = {
                onSetTransportState(TapeTransportState.Rewinding)
                onSeekRewind()
            }
        )

        // PLAY
        PianoKeyButton(
            label = "PLAY",
            glyph = TransportGlyphType.Play,
            isActive = transportState == TapeTransportState.Playing,
            harmonicColor = harmonicColor,
            modifier = Modifier.weight(1.2f),
            onClick = {
                onSetTransportState(TapeTransportState.Playing)
                if (transportState != TapeTransportState.Playing) onTogglePlayPause()
            }
        )

        // FFWD
        PianoKeyButton(
            label = "FFWD",
            glyph = TransportGlyphType.Next,
            isActive = transportState == TapeTransportState.FastForwarding,
            harmonicColor = harmonicColor,
            modifier = Modifier.weight(1f),
            onClick = {
                onSetTransportState(TapeTransportState.FastForwarding)
                onSeekFastForward()
            }
        )

        // PAUSE
        PianoKeyButton(
            label = "PAUSE",
            glyph = TransportGlyphType.Pause,
            isActive = transportState == TapeTransportState.Paused,
            harmonicColor = harmonicColor,
            modifier = Modifier.weight(1f),
            onClick = {
                onSetTransportState(TapeTransportState.Paused)
                if (transportState == TapeTransportState.Playing) onTogglePlayPause()
            }
        )

        // STOP
        PianoKeyButton(
            label = "STOP",
            glyph = TransportGlyphType.Close,
            isActive = transportState == TapeTransportState.Stopped,
            harmonicColor = harmonicColor,
            modifier = Modifier.weight(1f),
            onClick = {
                onSetTransportState(TapeTransportState.Stopped)
            }
        )
    }
}

@Composable
private fun PianoKeyButton(
    label: String,
    glyph: TransportGlyphType,
    isActive: Boolean,
    harmonicColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) harmonicColor.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(
            1.5.dp,
            if (isActive) harmonicColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PixelodyTransportGlyph(
                glyph = glyph,
                color = if (isActive) harmonicColor else MaterialTheme.colorScheme.onSurface,
                sizeDp = 14
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 7.5.sp,
                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                color = if (isActive) harmonicColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 4-Quadrant Magnetics Telemetry HUD.
 */
@Composable
private fun MagneticsTelemetryHud(
    telemetry: TapeMagneticsTelemetry,
    harmonicColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MagneticsMetricPill(
            label = "SATURATION",
            value = "${String.format("%.1f", telemetry.saturationThdPercent)}% THD",
            subtitle = "tanh Hysteresis",
            accentColor = harmonicColor,
            modifier = Modifier.weight(1f)
        )
        MagneticsMetricPill(
            label = "WOW & FLUTTER",
            value = "${String.format("%.2f", (telemetry.wowModulationMs + telemetry.flutterModulationMs) * 1000f)}μs",
            subtitle = "Capstan Motor",
            accentColor = Color(0xFF38BDF8),
            modifier = Modifier.weight(1f)
        )
        MagneticsMetricPill(
            label = "TAPE TENSION",
            value = "${telemetry.spoolPhysics.tapeTensionGrams.toInt()} g",
            subtitle = "${telemetry.spoolPhysics.supplyRpm.toInt()} RPM Spool",
            accentColor = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
        MagneticsMetricPill(
            label = "SNR NOISE",
            value = "${telemetry.effectiveSnrDb.toInt()} dB",
            subtitle = telemetry.transportState.title,
            accentColor = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MagneticsMetricPill(
    label: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tape Formulation Selection Carousel.
 */
@Composable
private fun TapeFormulationCarousel(
    selectedFormulation: TapeFormulation,
    onSelectFormulation: (TapeFormulation) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "TAPE FORMULATION & BIAS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(TapeFormulation.values(), key = { it.name }) { formulation ->
                val isSelected = formulation == selectedFormulation
                val fColor = Color(formulation.harmonicColorHex)

                Surface(
                    modifier = Modifier
                        .width(175.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectFormulation(formulation) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) fColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) fColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
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
                                text = formulation.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = fColor
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = fColor.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "${formulation.noiseFloorDb.toInt()}dB",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fColor
                                )
                            }
                        }

                        Text(
                            text = formulation.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Text(
                            text = "+${formulation.lowEndBumpDb}dB bump • ${(formulation.highFreqRolloffHz / 1000f).toInt()}kHz",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Noise Reduction Companding selector.
 */
@Composable
private fun NoiseReductionSelector(
    selectedNr: TapeNoiseReduction,
    harmonicColor: Color,
    onSelectNr: (TapeNoiseReduction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "NOISE REDUCTION COMPANDING",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TapeNoiseReduction.values().forEach { nr ->
                val isSelected = nr == selectedNr
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectNr(nr) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) harmonicColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    border = if (isSelected) BorderStroke(1.dp, harmonicColor) else null
                ) {
                    Text(
                        text = nr.badgeText,
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Cassette Shell Theme Picker chips.
 */
@Composable
private fun CassetteShellThemePicker(
    selectedTheme: CassetteShellTheme,
    harmonicColor: Color,
    onSelectTheme: (CassetteShellTheme) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "CASSETTE SHELL THEME",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CassetteShellTheme.values(), key = { it.name }) { theme ->
                val isSelected = theme == selectedTheme
                val tAccent = Color(theme.accentColorHex)

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectTheme(theme) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) tAccent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    border = if (isSelected) BorderStroke(1.dp, tAccent) else null
                ) {
                    Text(
                        text = theme.title,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Magnetic parameter fine tuning sliders.
 */
@Composable
private fun MagneticsParameterSliders(
    settings: CassetteTapeSettings,
    harmonicColor: Color,
    onUpdateSettings: (CassetteTapeSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Drive Saturation Gain
        MagneticsSliderRow(
            label = "Input Drive (Tape Saturation)",
            valueText = "${String.format("%.2f", settings.driveGain)}x",
            value = settings.driveGain,
            valueRange = 0.2f..3.0f,
            accentColor = harmonicColor,
            onValueChange = { onUpdateSettings(settings.copy(driveGain = it)) }
        )

        // Wow & Flutter Intensity
        MagneticsSliderRow(
            label = "Motor Wow & Flutter",
            valueText = "${(settings.wowFlutterIntensity * 100).toInt()}%",
            value = settings.wowFlutterIntensity,
            valueRange = 0.0f..1.0f,
            accentColor = Color(0xFF38BDF8),
            onValueChange = { onUpdateSettings(settings.copy(wowFlutterIntensity = it)) }
        )

        // Tape Head Azimuth Skew
        MagneticsSliderRow(
            label = "Head Azimuth Phase Skew",
            valueText = "${String.format("%.2f", settings.azimuthSkewMs)} ms",
            value = settings.azimuthSkewMs,
            valueRange = -0.3f..0.3f,
            accentColor = Color(0xFF8B5CF6),
            onValueChange = { onUpdateSettings(settings.copy(azimuthSkewMs = it)) }
        )

        // Tape Wear & Oxide Degradation
        MagneticsSliderRow(
            label = "Oxide Wear & Harmonic Dirt",
            valueText = "${(settings.tapeWear * 100).toInt()}%",
            value = settings.tapeWear,
            valueRange = 0.0f..1.0f,
            accentColor = Color(0xFFEF4444),
            onValueChange = { onUpdateSettings(settings.copy(tapeWear = it)) }
        )

        // Tape Hiss Volume
        MagneticsSliderRow(
            label = "Analog Tape Hiss",
            valueText = "${(settings.hissVolumeGain * 100).toInt()}%",
            value = settings.hissVolumeGain,
            valueRange = 0.0f..1.0f,
            accentColor = Color(0xFF10B981),
            onValueChange = { onUpdateSettings(settings.copy(hissVolumeGain = it)) }
        )
    }
}

@Composable
private fun MagneticsSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}
