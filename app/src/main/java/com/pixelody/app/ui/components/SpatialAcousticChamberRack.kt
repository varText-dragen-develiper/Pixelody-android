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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.AcousticChamberPreset
import com.pixelody.app.data.model.BinauralCrossfeedMode
import com.pixelody.app.data.model.SpatialAcousticTelemetry
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.SpatialSpeakerPosition
import com.pixelody.app.data.model.WallMaterialDamping
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * SpatialAcousticChamberRack: Full interactive studio rack for 3D Headphone Spatialization.
 * Provides an isometric 3D room floorplan with touch-draggable speaker towers,
 * real-time acoustic reflection paths, preset switcher, and acoustic physics tuning.
 */
@Composable
fun SpatialAcousticChamberRack(
    settings: SpatialChamberSettings,
    telemetry: SpatialAcousticTelemetry,
    onUpdateSettings: (SpatialChamberSettings) -> Unit,
    onApplyPreset: (AcousticChamberPreset) -> Unit,
    onUpdateSpeakerPosition: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(settings.preset.accentColorHex)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            ChamberHeader(
                settings = settings,
                accentColor = accentColor,
                onToggleEnabled = { onUpdateSettings(settings.copy(isEnabled = !settings.isEnabled)) }
            )

            // 1. Isometric 3D Acoustic Chamber Canvas
            IsometricAcousticCanvas(
                settings = settings,
                telemetry = telemetry,
                accentColor = accentColor,
                onSpeakerDragged = { leftAngle, rightAngle, dist ->
                    onUpdateSpeakerPosition(leftAngle, rightAngle, dist)
                },
                onHeadYawDragged = { yaw ->
                    onUpdateSettings(settings.copy(listenerHeadYawDeg = yaw))
                }
            )

            // Headstage 360° Cardinal Quick Orientator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HEAD ORIENTATION: ${settings.listenerHeadYawDeg.toInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "FRONT (0°)" to 0f,
                        "STAGE-L (-90°)" to -90f,
                        "REAR (180°)" to 180f,
                        "STAGE-R (+90°)" to 90f
                    ).forEach { (label, targetYaw) ->
                        val isCurrent = abs(settings.listenerHeadYawDeg - targetYaw) < 5f
                        Surface(
                            onClick = {
                                onUpdateSettings(settings.copy(listenerHeadYawDeg = targetYaw))
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrent) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isCurrent) accentColor else Color.Transparent)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Real-Time Telemetry HUD
            SpatialTelemetryHud(
                telemetry = telemetry,
                accentColor = accentColor
            )

            // 3. Chamber Preset Carousel
            ChamberPresetCarousel(
                selectedPreset = settings.preset,
                onSelectPreset = onApplyPreset
            )

            // 4. Wall Material Damping Picker
            WallMaterialPicker(
                selectedMaterial = settings.wallDamping,
                accentColor = accentColor,
                onSelectMaterial = { onUpdateSettings(settings.copy(wallDamping = it)) }
            )

            // 5. Binaural Crossfeed Mode Selector
            CrossfeedModeSelector(
                selectedMode = settings.crossfeedMode,
                accentColor = accentColor,
                onSelectMode = { onUpdateSettings(settings.copy(crossfeedMode = it)) }
            )

            // 6. Acoustic Dimension Sliders
            AcousticParameterSliders(
                settings = settings,
                accentColor = accentColor,
                onUpdateSettings = onUpdateSettings
            )
        }
    }
}

/**
 * Top Header with title, active preset badge, and master spatializer switch.
 */
@Composable
private fun ChamberHeader(
    settings: SpatialChamberSettings,
    accentColor: Color,
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
                color = accentColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.OmniSource,
                        color = accentColor,
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
                        text = "3D ACOUSTIC CHAMBER",
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
                        color = if (settings.isEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${settings.preset.badgeGlyph} ${settings.preset.title} • ${settings.roomVolumeM3.toInt()}m³",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Switch(
            checked = settings.isEnabled,
            onCheckedChange = { onToggleEnabled() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.4f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/**
 * Interactive Isometric 3D Room Canvas with touch-draggable virtual speakers and 360° listener head compass.
 */
@Composable
private fun IsometricAcousticCanvas(
    settings: SpatialChamberSettings,
    telemetry: SpatialAcousticTelemetry,
    accentColor: Color,
    onSpeakerDragged: (Float, Float, Float) -> Unit,
    onHeadYawDragged: (Float) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rayPulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rayPulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0D14)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var isDraggingHead = false
                        detectDragGestures(
                            onDragStart = { startPos ->
                                val listenerPos = Offset(size.width * 0.50f, size.height * 0.68f)
                                val distToHead = sqrt((startPos.x - listenerPos.x) * (startPos.x - listenerPos.x) + (startPos.y - listenerPos.y) * (startPos.y - listenerPos.y))
                                isDraggingHead = distToHead < size.width * 0.16f
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val listenerPos = Offset(size.width * 0.50f, size.height * 0.68f)
                                val touch = change.position
                                val dx = touch.x - listenerPos.x
                                val dy = touch.y - listenerPos.y

                                if (isDraggingHead) {
                                    // 360 degree head yaw orientation
                                    val angleRad = atan2(dx, -dy)
                                    val rawYawDeg = (angleRad * 180f / PI).toFloat()
                                    onHeadYawDragged(rawYawDeg)
                                } else {
                                    val distPx = sqrt(dx * dx + dy * dy)
                                    val maxRadiusPx = size.width * 0.42f
                                    val normalizedDist = (distPx / maxRadiusPx).coerceIn(0.5f, 5.0f)
                                    val angleRad = atan2(abs(dx), maxOf(-dy, 10f))
                                    val angleDeg = (angleRad * 180f / PI).toFloat().coerceIn(15f, 85f)

                                    if (dx < 0) {
                                        onSpeakerDragged(-angleDeg, settings.speakerPosition.rightAngleDeg, normalizedDist * 1.8f)
                                    } else {
                                        onSpeakerDragged(settings.speakerPosition.leftAngleDeg, angleDeg, normalizedDist * 1.8f)
                                    }
                                }
                            }
                        )
                    }
            ) {
                drawIsometricChamber(
                    settings = settings,
                    telemetry = telemetry,
                    accentColor = accentColor,
                    primaryColor = primaryColor,
                    rayPulsePhase = rayPulsePhase
                )
            }

            // Interactive Drag Guide Overlay
            Text(
                text = "DRAG SPEAKERS FOR ANGLE • DRAG HEAD FOR 360° ORIENTATION",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }
}

private fun DrawScope.drawIsometricChamber(
    settings: SpatialChamberSettings,
    telemetry: SpatialAcousticTelemetry,
    accentColor: Color,
    primaryColor: Color,
    rayPulsePhase: Float
) {
    val w = size.width
    val h = size.height

    // Room boundaries (Isometric Diamond Floorplan)
    val topFloor = Offset(w * 0.50f, h * 0.16f)
    val leftFloor = Offset(w * 0.10f, h * 0.52f)
    val rightFloor = Offset(w * 0.90f, h * 0.52f)
    val bottomFloor = Offset(w * 0.50f, h * 0.88f)

    // Floor Grid
    val floorPath = Path().apply {
        moveTo(topFloor.x, topFloor.y)
        lineTo(rightFloor.x, rightFloor.y)
        lineTo(bottomFloor.x, bottomFloor.y)
        lineTo(leftFloor.x, leftFloor.y)
        close()
    }

    drawPath(
        path = floorPath,
        color = Color(0xFF101522).copy(alpha = 0.85f)
    )
    drawPath(
        path = floorPath,
        color = accentColor.copy(alpha = 0.35f),
        style = Stroke(width = 1.5f)
    )

    // Listener Node (Center head)
    val listenerPos = Offset(w * 0.50f, h * 0.68f)

    // Listener Head Circle
    drawCircle(
        color = Color(0xFF1E293B),
        radius = 16f,
        center = listenerPos
    )
    drawCircle(
        color = accentColor,
        radius = 16f,
        center = listenerPos,
        style = Stroke(width = 2.5f)
    )

    // 360 Headset Arc Rotated by listenerHeadYawDeg
    val yawRad = (settings.listenerHeadYawDeg * PI / 180f).toFloat()
    val noseX = listenerPos.x + sin(yawRad) * 16f
    val noseY = listenerPos.y - cos(yawRad) * 16f
    // Nose pointer tip
    drawLine(
        color = accentColor,
        start = listenerPos,
        end = Offset(noseX, noseY),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // Headset binaural ear cups
    val earLeftX = listenerPos.x - cos(yawRad) * 16f
    val earLeftY = listenerPos.y - sin(yawRad) * 16f
    val earRightX = listenerPos.x + cos(yawRad) * 16f
    val earRightY = listenerPos.y + sin(yawRad) * 16f
    drawCircle(
        color = primaryColor,
        radius = 5f,
        center = Offset(earLeftX, earLeftY)
    )
    drawCircle(
        color = primaryColor,
        radius = 5f,
        center = Offset(earRightX, earRightY)
    )

    // Calculate Speaker positions based on angles
    val leftAngleRad = (abs(settings.speakerPosition.leftAngleDeg) * PI / 180f).toFloat()
    val rightAngleRad = (abs(settings.speakerPosition.rightAngleDeg) * PI / 180f).toFloat()
    val baseDistance = (w * 0.32f) * (settings.speakerPosition.distanceMeters / 1.8f).coerceIn(0.6f, 1.3f)

    val leftSpeakerPos = Offset(
        x = listenerPos.x - (sin(leftAngleRad) * baseDistance),
        y = listenerPos.y - (cos(leftAngleRad) * baseDistance * 0.75f)
    )

    val rightSpeakerPos = Offset(
        x = listenerPos.x + (sin(rightAngleRad) * baseDistance),
        y = listenerPos.y - (cos(rightAngleRad) * baseDistance * 0.75f)
    )

    // Animated Acoustic Reflection Rays
    if (settings.isEnabled) {
        val rayAlpha = (0.7f - (rayPulsePhase * 0.5f)).coerceIn(0.1f, 0.7f)

        // Left Speaker Rays (Direct, Left Wall, Front Wall)
        drawLine(
            color = accentColor.copy(alpha = rayAlpha),
            start = leftSpeakerPos,
            end = listenerPos,
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accentColor.copy(alpha = rayAlpha * 0.5f),
            start = leftSpeakerPos,
            end = leftFloor,
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accentColor.copy(alpha = rayAlpha * 0.5f),
            start = leftFloor,
            end = listenerPos,
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )

        // Right Speaker Rays (Direct, Right Wall, Front Wall)
        drawLine(
            color = accentColor.copy(alpha = rayAlpha),
            start = rightSpeakerPos,
            end = listenerPos,
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accentColor.copy(alpha = rayAlpha * 0.5f),
            start = rightSpeakerPos,
            end = rightFloor,
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accentColor.copy(alpha = rayAlpha * 0.5f),
            start = rightFloor,
            end = listenerPos,
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
    }

    // Draw Left Speaker Tower
    drawSpeakerTower(leftSpeakerPos, -settings.speakerPosition.leftAngleDeg, accentColor)

    // Draw Right Speaker Tower
    drawSpeakerTower(rightSpeakerPos, settings.speakerPosition.rightAngleDeg, accentColor)
}

private fun DrawScope.drawSpeakerTower(pos: Offset, angleDeg: Float, accentColor: Color) {
    // Tower Shadow & Base
    drawCircle(
        color = Color.Black.copy(alpha = 0.5f),
        radius = 20f,
        center = Offset(pos.x, pos.y + 4f)
    )

    // Speaker Cabinet Rectangle
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(pos.x - 14f, pos.y - 20f),
        size = Size(28f, 40f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(pos.x - 14f, pos.y - 20f),
        size = Size(28f, 40f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        style = Stroke(width = 2f)
    )

    // Speaker Woofer Cones
    drawCircle(
        color = accentColor.copy(alpha = 0.4f),
        radius = 7f,
        center = Offset(pos.x, pos.y - 8f)
    )
    drawCircle(
        color = accentColor.copy(alpha = 0.7f),
        radius = 9f,
        center = Offset(pos.x, pos.y + 8f)
    )
}

/**
 * 4-Quadrant Telemetry HUD displaying live ITD, ILD, RT60, and Spatial Width.
 */
@Composable
private fun SpatialTelemetryHud(
    telemetry: SpatialAcousticTelemetry,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HudMetricPill(
            label = "ITD DELAY",
            value = "${telemetry.itdMicroseconds.toInt()} μs",
            subtitle = "Interaural Delay",
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
        HudMetricPill(
            label = "ILD SHADOW",
            value = "${String.format("%.1f", telemetry.ildDb)} dB",
            subtitle = "Head Attenuation",
            accentColor = Color(0xFF38BDF8),
            modifier = Modifier.weight(1f)
        )
        HudMetricPill(
            label = "RT60 DECAY",
            value = "${String.format("%.2f", telemetry.calculatedRt60Seconds)}s",
            subtitle = "Room Reverb",
            accentColor = Color(0xFF8B5CF6),
            modifier = Modifier.weight(1f)
        )
        HudMetricPill(
            label = "SOUNDSTAGE",
            value = "${(telemetry.spatialWidthScore * 100).toInt()}%",
            subtitle = "${telemetry.activeRayCount} Rays",
            accentColor = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HudMetricPill(
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
 * Preset selection carousel.
 */
@Composable
private fun ChamberPresetCarousel(
    selectedPreset: AcousticChamberPreset,
    onSelectPreset: (AcousticChamberPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "ACOUSTIC CHAMBER PRESETS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(AcousticChamberPreset.values(), key = { it.name }) { preset ->
                val isSelected = preset == selectedPreset
                val pColor = Color(preset.accentColorHex)

                Surface(
                    modifier = Modifier
                        .width(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectPreset(preset) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) pColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) pColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
                                text = preset.badgeGlyph,
                                fontSize = 18.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = pColor.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "${preset.defaultRt60Seconds}s RT60",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pColor
                                )
                            }
                        }

                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Text(
                            text = "${preset.defaultVolumeM3.toInt()}m³ • ${preset.defaultDamping.title}",
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
 * Wall Material Damping picker matrix chips.
 */
@Composable
private fun WallMaterialPicker(
    selectedMaterial: WallMaterialDamping,
    accentColor: Color,
    onSelectMaterial: (WallMaterialDamping) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "WALL MATERIAL ABSORPTION MATRIX",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WallMaterialDamping.values(), key = { it.name }) { material ->
                val isSelected = material == selectedMaterial
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectMaterial(material) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = material.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Absorb: ${(material.highFrequencyAbsorption * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                color = accentColor
                            )
                            Text(
                                text = "Warmth: ${material.acousticWarmthFactor}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Binaural Crossfeed Mode Selector chips.
 */
@Composable
private fun CrossfeedModeSelector(
    selectedMode: BinauralCrossfeedMode,
    accentColor: Color,
    onSelectMode: (BinauralCrossfeedMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "BINAURAL CROSSFEED GEOMETRY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BinauralCrossfeedMode.values().forEach { mode ->
                val isSelected = mode == selectedMode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectMode(mode) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    border = if (isSelected) BorderStroke(1.dp, accentColor) else null
                ) {
                    Text(
                        text = when (mode) {
                            BinauralCrossfeedMode.NaturalNearfield -> "60° Nearfield"
                            BinauralCrossfeedMode.WideAngleMaster -> "90° Wide"
                            BinauralCrossfeedMode.IntimateHeadstage -> "45° Intimate"
                            BinauralCrossfeedMode.DirectStereoOff -> "Bypassed"
                        },
                        modifier = Modifier.padding(vertical = 6.dp),
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
 * Fine-tuning sliders for room volume, decay, early reflections, and dry/wet mix.
 */
@Composable
private fun AcousticParameterSliders(
    settings: SpatialChamberSettings,
    accentColor: Color,
    onUpdateSettings: (SpatialChamberSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Room Volume
        AcousticSliderRow(
            label = "Room Volume",
            valueText = "${settings.roomVolumeM3.toInt()} m³",
            value = settings.roomVolumeM3,
            valueRange = 20f..4000f,
            accentColor = accentColor,
            onValueChange = { onUpdateSettings(settings.copy(roomVolumeM3 = it)) }
        )

        // Reverb Decay RT60
        AcousticSliderRow(
            label = "Reverb Decay (RT60)",
            valueText = "${String.format("%.2f", settings.reverbDecaySeconds)} s",
            value = settings.reverbDecaySeconds,
            valueRange = 0.15f..4.5f,
            accentColor = accentColor,
            onValueChange = { onUpdateSettings(settings.copy(reverbDecaySeconds = it)) }
        )

        // Early Reflection Level
        AcousticSliderRow(
            label = "Early Reflection Clarity",
            valueText = "${(settings.earlyReflectionGain * 100).toInt()}%",
            value = settings.earlyReflectionGain,
            valueRange = 0.0f..1.0f,
            accentColor = accentColor,
            onValueChange = { onUpdateSettings(settings.copy(earlyReflectionGain = it)) }
        )

        // Dry / Wet Mix
        AcousticSliderRow(
            label = "Spatial Immersion (Dry/Wet)",
            valueText = "${(settings.dryWetMix * 100).toInt()}% Wet",
            value = settings.dryWetMix,
            valueRange = 0.0f..1.0f,
            accentColor = accentColor,
            onValueChange = { onUpdateSettings(settings.copy(dryWetMix = it)) }
        )
    }
}

@Composable
private fun AcousticSliderRow(
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
