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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.OscilloscopePhosphorEngine
import com.pixelody.app.data.model.CrtBeamPersistence
import com.pixelody.app.data.model.CrtPhosphorType
import com.pixelody.app.data.model.OscilloscopeDisplayMode
import com.pixelody.app.data.model.OscilloscopeSettings
import com.pixelody.app.data.model.OscilloscopeTelemetry
import com.pixelody.app.data.model.VectorPoint3D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * OscilloscopePhosphorLabView: Vector Laser Oscilloscope & CRT Phosphor Art Lab.
 * Features an authentic CRT electron beam simulation with multi-pass phosphor bloom,
 * calibrated 8x10 graticule grid, 6 vector generation modes, 6 chemical phosphor types,
 * interactive 3D Euler touch rotation, and stereoscopic phase telemetry.
 */
@Composable
fun OscilloscopePhosphorLabView(
    settings: OscilloscopeSettings,
    telemetry: OscilloscopeTelemetry,
    engine: OscilloscopePhosphorEngine,
    onUpdateSettings: (OscilloscopeSettings) -> Unit,
    onSetDisplayMode: (OscilloscopeDisplayMode) -> Unit,
    onSetPhosphorType: (CrtPhosphorType) -> Unit,
    onSetPersistence: (CrtBeamPersistence) -> Unit,
    onSetSensitivity: (Float) -> Unit,
    onUpdate3DRotation: (Float, Float) -> Unit,
    onSetPhaseRotation: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val phosphorPrimary = Color(settings.phosphorType.primaryColorHex)
    val phosphorBloom = Color(settings.phosphorType.bloomGlowColorHex)
    val phosphorTrail = Color(settings.phosphorType.trailColorHex)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, phosphorPrimary.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title, Power Switch, and Chemical Composition Badge
            OscilloscopeHeader(
                settings = settings,
                phosphorPrimary = phosphorPrimary,
                onTogglePower = { onUpdateSettings(settings.copy(isEnabled = !settings.isEnabled)) }
            )

            // Main Interactive CRT Screen Canvas
            CrtScreenDisplay(
                settings = settings,
                engine = engine,
                phosphorPrimary = phosphorPrimary,
                phosphorBloom = phosphorBloom,
                phosphorTrail = phosphorTrail,
                onUpdate3DRotation = onUpdate3DRotation,
                onSetPhaseRotation = onSetPhaseRotation
            )

            // Real-Time Stereoscopic Phase & Vector Telemetry HUD
            OscilloscopeTelemetryBar(
                telemetry = telemetry,
                phosphorPrimary = phosphorPrimary
            )

            // Display Mode Selector (6 modes)
            OscilloscopeModeSelector(
                selectedMode = settings.displayMode,
                phosphorPrimary = phosphorPrimary,
                onSelectMode = onSetDisplayMode
            )

            // CRT Phosphor Chemical Type Selector (6 types)
            PhosphorTypeSelector(
                selectedType = settings.phosphorType,
                onSelectType = onSetPhosphorType
            )

            // Beam Persistence & Physics Tuning Sliders
            BeamPhysicsControls(
                settings = settings,
                phosphorPrimary = phosphorPrimary,
                onSetSensitivity = onSetSensitivity,
                onSetPersistence = onSetPersistence,
                onUpdateSettings = onUpdateSettings
            )
        }
    }
}

/**
 * Top Header with Oscilloscope Title, CRT Phosphor badge, and power toggle.
 */
@Composable
private fun OscilloscopeHeader(
    settings: OscilloscopeSettings,
    phosphorPrimary: Color,
    onTogglePower: () -> Unit
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
                shape = RoundedCornerShape(8.dp),
                color = phosphorPrimary.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, phosphorPrimary.copy(alpha = 0.6f))
            ) {
                Box(modifier = Modifier.padding(6.dp)) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.WaveformBars,
                        color = phosphorPrimary,
                        size = 20.dp
                    )
                }
            }

            Column {
                Text(
                    text = "VECTOR LASER OSCILLOSCOPE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${settings.phosphorType.title} • ${settings.displayMode.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = phosphorPrimary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (settings.isEnabled) "ACTIVE" else "BYPASS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (settings.isEnabled) phosphorPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = settings.isEnabled,
                onCheckedChange = { onTogglePower() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = phosphorPrimary,
                    checkedTrackColor = phosphorPrimary.copy(alpha = 0.35f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

/**
 * CRT Screen Canvas with authentic electron beam drawing, phosphor bloom,
 * calibrated graticule grid, and touch drag rotation.
 */
@Composable
private fun CrtScreenDisplay(
    settings: OscilloscopeSettings,
    engine: OscilloscopePhosphorEngine,
    phosphorPrimary: Color,
    phosphorBloom: Color,
    phosphorTrail: Color,
    onUpdate3DRotation: (Float, Float) -> Unit,
    onSetPhaseRotation: (Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CrtScanline")
    val scanlinePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlineY"
    )

    var currentPitch by remember { mutableFloatStateOf(settings.beamGeometry.eulerPitchDeg) }
    var currentYaw by remember { mutableFloatStateOf(settings.beamGeometry.eulerYawDeg) }
    var currentPhaseDeg by remember { mutableFloatStateOf(settings.beamGeometry.audioPhaseRotationDeg) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.18f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF070B09))
            .pointerInput(settings.displayMode) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (settings.displayMode == OscilloscopeDisplayMode.VectorLaserSynth_3D) {
                        currentPitch = (currentPitch - dragAmount.y * 0.45f).coerceIn(-85f, 85f)
                        currentYaw = (currentYaw + dragAmount.x * 0.45f) % 360f
                        onUpdate3DRotation(currentPitch, currentYaw)
                    } else if (settings.displayMode == OscilloscopeDisplayMode.Lissajous_XY ||
                        settings.displayMode == OscilloscopeDisplayMode.CRT_VectorSpirals
                    ) {
                        currentPhaseDeg = (currentPhaseDeg + dragAmount.x * 0.5f) % 360f
                        onSetPhaseRotation(currentPhaseDeg)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            // 1. Deep Phosphor CRT Background Glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        phosphorTrail.copy(alpha = 0.22f),
                        Color(0xFF040605)
                    ),
                    center = Offset(centerX, centerY),
                    radius = w * 0.7f
                )
            )

            // 2. CRT Glass Curvature Corner Shadow
            if (settings.enableCrtCurvature) {
                drawCrtGlassVignette(w, h)
            }

            // 3. Calibrated 8x10 Graticule Division Grid
            if (settings.enableGraticuleOverlay) {
                drawGraticuleGrid(
                    w = w,
                    h = h,
                    divisions = settings.beamGeometry.graticuleDivisions,
                    gridColor = phosphorPrimary.copy(alpha = 0.18f),
                    subTickColor = phosphorPrimary.copy(alpha = 0.35f)
                )
            }

            // 4. Generate & Render Vector Points
            if (settings.isEnabled) {
                val points = engine.generateVectorPoints(w, h, pointCount = 384)
                if (points.isNotEmpty()) {
                    drawVectorTrace(
                        points = points,
                        settings = settings,
                        primaryColor = phosphorPrimary,
                        bloomColor = phosphorBloom,
                        trailColor = phosphorTrail
                    )
                }
            } else {
                // Standby flat baseline sweep
                val baselineY = centerY
                drawLine(
                    color = phosphorPrimary.copy(alpha = 0.4f),
                    start = Offset(0f, baselineY),
                    end = Offset(w, baselineY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // 5. CRT Scanline Raster Lines
            drawScanlines(
                w = w,
                h = h,
                phase = scanlinePhase,
                intensity = settings.beamGeometry.scanlineIntensity
            )
        }

        // Overlay Interactive Guide Hint
        if (settings.displayMode == OscilloscopeDisplayMode.VectorLaserSynth_3D) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, phosphorPrimary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "DRAG TO ROTATE 3D EULER MESH",
                    style = MaterialTheme.typography.labelSmall,
                    color = phosphorPrimary,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        } else if (settings.displayMode == OscilloscopeDisplayMode.Lissajous_XY) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, phosphorPrimary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "DRAG TO ROTATE PHASE ORBIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = phosphorPrimary,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Draws the vector path with multi-pass phosphor bloom glow and hot electron core.
 */
private fun DrawScope.drawVectorTrace(
    points: List<VectorPoint3D>,
    settings: OscilloscopeSettings,
    primaryColor: Color,
    bloomColor: Color,
    trailColor: Color
) {
    if (points.size < 2) return

    val path = Path()
    path.moveTo(points[0].x, points[0].y)

    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        // Smooth Bezier midpoints
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticTo(prev.x, prev.y, midX, midY)
    }

    val beamThickness = settings.beamGeometry.beamThicknessPx.dp.toPx()

    // Pass 1: Outer Phosphor Decay Trail / Halo (Bloom)
    if (settings.enablePhosphorBloom) {
        drawPath(
            path = path,
            color = trailColor.copy(alpha = 0.28f * settings.beamGeometry.bloomIntensity),
            style = Stroke(
                width = beamThickness * 5.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Pass 2: Mid Phosphor Bloom
        drawPath(
            path = path,
            color = bloomColor.copy(alpha = 0.55f * settings.beamGeometry.bloomIntensity),
            style = Stroke(
                width = beamThickness * 2.8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // Pass 3: Focused High-Energy Electron Beam Core
    drawPath(
        path = path,
        color = primaryColor.copy(alpha = 0.95f * settings.beamGeometry.beamBrightness),
        style = Stroke(
            width = beamThickness * 1.15f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Pass 4: Ultra-Hot White Electron Beam Core Center
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.65f),
        style = Stroke(
            width = beamThickness * 0.55f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Draws the calibrated 8x10 graticule grid with center crosshair reticle and axis ticks.
 */
private fun DrawScope.drawGraticuleGrid(
    w: Float,
    h: Float,
    divisions: Int,
    gridColor: Color,
    subTickColor: Color
) {
    val hDivs = divisions + 2 // 10 horizontal divisions
    val vDivs = divisions // 8 vertical divisions

    val stepX = w / hDivs
    val stepY = h / vDivs

    // Vertical grid lines
    for (i in 1 until hDivs) {
        val x = i * stepX
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Horizontal grid lines
    for (j in 1 until vDivs) {
        val y = j * stepY
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Central crosshair axis lines with high visibility
    val centerX = w / 2f
    val centerY = h / 2f

    drawLine(
        color = subTickColor,
        start = Offset(centerX, 0f),
        end = Offset(centerX, h),
        strokeWidth = 1.5.dp.toPx()
    )

    drawLine(
        color = subTickColor,
        start = Offset(0f, centerY),
        end = Offset(w, centerY),
        strokeWidth = 1.5.dp.toPx()
    )

    // Sub-division ticks on center axes (5 ticks per division)
    val tickSize = 3.dp.toPx()
    val subDivCountX = hDivs * 5
    val subStepX = w / subDivCountX
    for (i in 0..subDivCountX) {
        val x = i * subStepX
        drawLine(
            color = subTickColor,
            start = Offset(x, centerY - tickSize),
            end = Offset(x, centerY + tickSize),
            strokeWidth = 1.dp.toPx()
        )
    }

    val subDivCountY = vDivs * 5
    val subStepY = h / subDivCountY
    for (j in 0..subDivCountY) {
        val y = j * subStepY
        drawLine(
            color = subTickColor,
            start = Offset(centerX - tickSize, y),
            end = Offset(centerX + tickSize, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

/**
 * Draws CRT glass vignette curvature shadows around screen perimeter.
 */
private fun DrawScope.drawCrtGlassVignette(w: Float, h: Float) {
    val cornerRadius = 16.dp.toPx()
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.55f)
            ),
            center = Offset(w / 2f, h / 2f),
            radius = w * 0.72f
        ),
        size = Size(w, h),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )
}

/**
 * Draws horizontal CRT electron scanlines across canvas.
 */
private fun DrawScope.drawScanlines(w: Float, h: Float, phase: Float, intensity: Float) {
    val scanlineSpacing = 4.dp.toPx()
    val lineCount = (h / scanlineSpacing).toInt()
    val alpha = (0.12f * intensity).coerceIn(0.02f, 0.35f)

    for (i in 0 until lineCount step 2) {
        val y = i * scanlineSpacing
        drawLine(
            color = Color.Black.copy(alpha = alpha),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

/**
 * Real-time stereoscopic phase correlation, peak deflection, and energy telemetry.
 */
@Composable
private fun OscilloscopeTelemetryBar(
    telemetry: OscilloscopeTelemetry,
    phosphorPrimary: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Phase Correlation (-1.0 Anti-Phase to +1.0 Mono)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PHASE CORRELATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val phaseLabel = when {
                    telemetry.phaseCoherenceScore > 0.75f -> "MONO COHERENT"
                    telemetry.phaseCoherenceScore > 0.25f -> "STEREO WIDE"
                    telemetry.phaseCoherenceScore > -0.25f -> "DIFFUSE / AMBIENT"
                    else -> "ANTI-PHASE CANCEL"
                }
                Text(
                    text = "${"%.2f".format(telemetry.phaseCoherenceScore)} • $phaseLabel",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (telemetry.phaseCoherenceScore >= 0f) phosphorPrimary else Color(0xFFEF4444)
                )
            }

            // Phase Correlation Progress Bar (-1.0 to +1.0)
            val normalizedCorrelation = ((telemetry.phaseCoherenceScore + 1.0f) / 2.0f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(normalizedCorrelation)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFEF4444),
                                    Color(0xFFF59E0B),
                                    phosphorPrimary
                                )
                            )
                        )
                )
            }

            // Row 2: Mid vs Side Energy & Beam Intensity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem(
                    label = "MID / CENTER",
                    value = "${(telemetry.centerEnergyRatio * 100).toInt()}%",
                    color = phosphorPrimary
                )
                TelemetryMetricItem(
                    label = "SIDE / SPREAD",
                    value = "${(telemetry.sideEnergyRatio * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.secondary
                )
                TelemetryMetricItem(
                    label = "PEAK BEAM",
                    value = "${"%.2f".format(telemetry.peakBeamDeflection)} V",
                    color = phosphorPrimary
                )
                TelemetryMetricItem(
                    label = "ECCENTRICITY",
                    value = "${"%.2f".format(telemetry.lissajousEccentricity)}",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Display Mode Carousel / Chips.
 */
@Composable
private fun OscilloscopeModeSelector(
    selectedMode: OscilloscopeDisplayMode,
    phosphorPrimary: Color,
    onSelectMode: (OscilloscopeDisplayMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "VECTOR SYNTH DISPLAY MODES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(OscilloscopeDisplayMode.entries) { mode ->
                val isSelected = mode == selectedMode
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) phosphorPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) phosphorPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.clickable { onSelectMode(mode) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) phosphorPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = mode.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Phosphor chemical composition picker with authentic CRT luminescence palette.
 */
@Composable
private fun PhosphorTypeSelector(
    selectedType: CrtPhosphorType,
    onSelectType: (CrtPhosphorType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "CRT PHOSPHOR CHEMICAL COMPOSITION",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CrtPhosphorType.entries) { type ->
                val isSelected = type == selectedType
                val chipColor = Color(type.primaryColorHex)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) chipColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) chipColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.clickable { onSelectType(type) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(chipColor)
                        )
                        Column {
                            Text(
                                text = type.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = type.compositionName,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Beam physics and decay persistence tuning controls.
 */
@Composable
private fun BeamPhysicsControls(
    settings: OscilloscopeSettings,
    phosphorPrimary: Color,
    onSetSensitivity: (Float) -> Unit,
    onSetPersistence: (CrtBeamPersistence) -> Unit,
    onUpdateSettings: (OscilloscopeSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Sensitivity Gain Slider
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DEFLECTION SENSITIVITY (VOLTS/DIV)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${"%.2f".format(settings.sensitivityGain)}x",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = phosphorPrimary
                )
            }
            Slider(
                value = settings.sensitivityGain,
                onValueChange = onSetSensitivity,
                valueRange = 0.2f..3.5f,
                colors = SliderDefaults.colors(
                    thumbColor = phosphorPrimary,
                    activeTrackColor = phosphorPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        // Persistence Preset Row
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "PHOSPHOR DECAY PERSISTENCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CrtBeamPersistence.entries.forEach { persistence ->
                    val isSelected = persistence == settings.persistence
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSetPersistence(persistence) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) phosphorPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) phosphorPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = persistence.title.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) phosphorPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Quick Toggles: Graticule, Bloom, CRT Curvature
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TuningChipToggle(
                label = "GRATICULE",
                isChecked = settings.enableGraticuleOverlay,
                accentColor = phosphorPrimary,
                onToggle = { onUpdateSettings(settings.copy(enableGraticuleOverlay = !settings.enableGraticuleOverlay)) }
            )
            TuningChipToggle(
                label = "BLOOM GLOW",
                isChecked = settings.enablePhosphorBloom,
                accentColor = phosphorPrimary,
                onToggle = { onUpdateSettings(settings.copy(enablePhosphorBloom = !settings.enablePhosphorBloom)) }
            )
            TuningChipToggle(
                label = "CRT CURVE",
                isChecked = settings.enableCrtCurvature,
                accentColor = phosphorPrimary,
                onToggle = { onUpdateSettings(settings.copy(enableCrtCurvature = !settings.enableCrtCurvature)) }
            )
        }
    }
}

@Composable
private fun TuningChipToggle(
    label: String,
    isChecked: Boolean,
    accentColor: Color,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isChecked) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isChecked) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) accentColor else MaterialTheme.colorScheme.outline)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                color = if (isChecked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
