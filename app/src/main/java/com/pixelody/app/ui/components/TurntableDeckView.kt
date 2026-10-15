package com.pixelody.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pixelody.app.core.image.ArtworkBitmapCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.storage.SavedHostStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deck operational simulation modes.
 */
enum class DeckMode {
    Turntable,
    Cassette
}

/**
 * Turntable slipmat felt and friction acoustics physics types.
 */
enum class SlipmatType(
    val label: String,
    val frictionDamping: Float,
    val motorTraction: Float,
    val accentColorHex: Long
) {
    RubberTechnics("RUBBER GRIP", 0.90f, 0.20f, 0xFF71717A),
    ButterRugFelt("BUTTER RUG", 0.98f, 0.05f, 0xFF38BDF8),
    CorkAudiophile("CORK ACOUSTIC", 0.94f, 0.12f, 0xFFD97706)
}

/**
 * Turntable Hot Cue needle-drop marker.
 */
data class TurntableHotCue(
    val index: Int,
    val positionMs: Long,
    val label: String = "CUE $index",
    val colorHex: Long = when (index) {
        1 -> 0xFFEF4444
        2 -> 0xFF3B82F6
        3 -> 0xFF10B981
        else -> 0xFFF59E0B
    }
)

@Composable
fun TurntableDeckView(
    isPlaying: Boolean,
    positionMs: Long = 0L,
    durationMs: Long,
    trackTitle: String,
    artistName: String,
    artworkUrl: String? = null,
    onSeek: (Long) -> Unit,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    isTapeSaturationEnabled: Boolean,
    onToggleTapeSaturation: () -> Unit,
    onNeedleDrop: () -> Unit = {},
    modifier: Modifier = Modifier,
    positionMsProvider: (() -> Long)? = null
) {
    val context = LocalContext.current.applicationContext
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var deckMode by remember { mutableStateOf(DeckMode.Turntable) }
    var rpmMode by remember { mutableStateOf("33 RPM") }
    var isMotorPowered by remember { mutableStateOf(true) }
    var selectedSlipmat by remember { mutableStateOf(SlipmatType.RubberTechnics) }
    var hotCues by remember { mutableStateOf<Map<Int, TurntableHotCue>>(emptyMap()) }

    // Load album artwork bitmap for center spindle sticker
    val credentialStore = remember { SavedHostStore(context) }
    val initialBitmap = remember(artworkUrl) { ArtworkBitmapCache.get(artworkUrl) }
    val artworkBitmap by produceState<Bitmap?>(initialValue = initialBitmap, artworkUrl) {
        value = initialBitmap
        if (value == null && !artworkUrl.isNullOrBlank()) {
            value = ArtworkBitmapCache.loadBitmap(context, artworkUrl, credentialStore, maxDimension = 384)
        }
    }

    // Nominal speeds: 33.33 RPM = 1800ms / rev, 45 RPM = 1333ms / rev
    val nominalRevMs = if (rpmMode == "45 RPM") 1333.3f else 1800.0f
    val effectiveRevMs = (nominalRevMs / playbackSpeed.coerceIn(0.5f, 2.0f))

    // Motor Spin / Scratch Physics
    var platterAngle by remember { mutableFloatStateOf(0f) }
    var isScratching by remember { mutableStateOf(false) }
    var scratchAngularVelocity by remember { mutableFloatStateOf(0f) }

    // Continuous motor rotation loop with slipmat friction & inertia
    LaunchedEffect(isPlaying, isMotorPowered, isScratching, effectiveRevMs, selectedSlipmat) {
        var lastTimeNanos = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dtSec = (now - lastTimeNanos) / 1_000_000_000f
            lastTimeNanos = now

            if (!isScratching) {
                if (isPlaying && isMotorPowered) {
                    // Constant motor velocity (degrees / sec)
                    val targetVelocity = (360f / (effectiveRevMs / 1000f))
                    // Slipmat traction dictates recovery speed back to platter speed
                    val traction = selectedSlipmat.motorTraction
                    scratchAngularVelocity = scratchAngularVelocity * (1f - traction) + targetVelocity * traction
                    platterAngle = (platterAngle + scratchAngularVelocity * dtSec) % 360f
                } else if (abs(scratchAngularVelocity) > 0.5f) {
                    // Slipmat friction damping braking
                    scratchAngularVelocity *= selectedSlipmat.frictionDamping
                    platterAngle = (platterAngle + scratchAngularVelocity * dtSec) % 360f
                } else {
                    scratchAngularVelocity = 0f
                }
            }
            delay(16) // ~60 FPS
        }
    }

    // Playback progress (0.0 to 1.0)
    val currentPositionMs = positionMsProvider?.invoke() ?: positionMs
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    // Tonearm tracking: Sweeps from 18 deg (lead-in rim) to 42 deg (inner run-out groove)
    val targetTonearmAngle = 18f + (progress * 24f)
    val animatedTonearmAngle by animateFloatAsState(
        targetValue = targetTonearmAngle,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "tonearm_angle"
    )

    // Tonearm lift/drop state: Drops when active, lifts up when paused/stopped
    val tonearmLiftProgress by animateFloatAsState(
        targetValue = if (isPlaying && isMotorPowered && !isScratching) 0f else 1f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "tonearm_lift"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Deck Header Controls: Mode Selector + Warmth Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Deck Mode Switch (Turntable vs Tape Deck)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    DeckMode.values().forEach { mode ->
                        val isSelected = deckMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    haptic.performTick()
                                    deckMode = mode
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = when (mode) {
                                    DeckMode.Turntable -> "VINYL DECK"
                                    DeckMode.Cassette -> "TAPE DECK"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Analog Tube Warmth / Tape Saturation Switch
                Surface(
                    onClick = {
                        haptic.performTick()
                        onToggleTapeSaturation()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isTapeSaturationEnabled) Color(0xFFD97706).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (isTapeSaturationEnabled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isTapeSaturationEnabled) Color(0xFFF59E0B) else Color(0xFF71717A))
                        )
                        Text(
                            text = if (isTapeSaturationEnabled) "TUBE WARMTH ON" else "TUBE WARMTH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isTapeSaturationEnabled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Deck Stage (Turntable vs Cassette)
            when (deckMode) {
                DeckMode.Turntable -> {
                    // Turntable Secondary Controls (33/45 RPM + Motor Start/Stop)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // RPM Selector
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("33 RPM", "45 RPM").forEach { mode ->
                                val isSelected = rpmMode == mode
                                Surface(
                                    onClick = {
                                        haptic.performTick()
                                        rpmMode = mode
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = mode,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Motor Power Toggle Button
                        Surface(
                            onClick = {
                                haptic.performTick()
                                isMotorPowered = !isMotorPowered
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isMotorPowered) MaterialTheme.colorScheme.surface else Color(0xFFEF4444).copy(alpha = 0.2f),
                            border = BorderStroke(
                                1.dp,
                                if (isMotorPowered) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else Color(0xFFEF4444)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isMotorPowered) Color(0xFF10B981) else Color(0xFFEF4444))
                                )
                                Text(
                                    text = if (isMotorPowered) "MOTOR ON" else "BRAKE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMotorPowered) MaterialTheme.colorScheme.onSurface else Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Turntable Platter + Tonearm Chassis Box
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.22f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF16161A), Color(0xFF0F0F12))
                                )
                            )
                            .border(1.5.dp, Color(0xFF27272A), RoundedCornerShape(16.dp))
                    ) {
                        val chassisWidth = constraints.maxWidth.toFloat()
                        val chassisHeight = constraints.maxHeight.toFloat()
                        val platterDiameter = chassisHeight * 0.92f
                        val platterCenter = Offset(platterDiameter * 0.52f, chassisHeight * 0.50f)
                        val platterRadius = platterDiameter * 0.48f

                        // Strobe Tower Light (Bottom-Left)
                        StrobeLightBeam(
                            isMotorRunning = isPlaying && isMotorPowered,
                            modifier = Modifier
                                .size(42.dp)
                                .align(Alignment.BottomStart)
                                .padding(start = 8.dp, bottom = 8.dp)
                        )

                        // Spinning Vinyl Platter Canvas with Touch Scratch Physics
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(durationMs, effectiveRevMs) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val dx0 = down.position.x - platterCenter.x
                                        val dy0 = down.position.y - platterCenter.y
                                        val dist0 = kotlin.math.sqrt((dx0 * dx0 + dy0 * dy0).toDouble()).toFloat()
                                        if (dist0 > platterRadius) return@awaitEachGesture

                                        var isScratchDragging = false
                                        var lastTouchAngle = (kotlin.math.atan2(dy0.toDouble(), dx0.toDouble()) * 180.0 / Math.PI).toFloat()
                                        var lastTouchTime = System.currentTimeMillis()
                                        var scratchSeekMs = positionMsProvider?.invoke() ?: positionMs

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                if (isScratchDragging) {
                                                    isScratching = false
                                                }
                                                break
                                            }
                                            val pos = change.position
                                            val dx = pos.x - platterCenter.x
                                            val dy = pos.y - platterCenter.y
                                            val dragX = pos.x - down.position.x
                                            val dragY = pos.y - down.position.y
                                            val touchSlop = viewConfiguration.touchSlop

                                            if (!isScratchDragging) {
                                                // If vertical drag is dominant, this is a page scroll! Do NOT consume, let parent LazyColumn scroll smoothly!
                                                if (kotlin.math.abs(dragY) > touchSlop && kotlin.math.abs(dragY) > kotlin.math.abs(dragX) * 1.4f) {
                                                    return@awaitEachGesture
                                                } else if (kotlin.math.abs(dragX) > touchSlop || kotlin.math.abs(dragY) > touchSlop) {
                                                    isScratchDragging = true
                                                    isScratching = true
                                                    haptic.performTick()
                                                    change.consume()
                                                }
                                            } else {
                                                change.consume()
                                                val currentAngle = (kotlin.math.atan2(dy.toDouble(), dx.toDouble()) * 180.0 / Math.PI).toFloat()
                                                val now = System.currentTimeMillis()
                                                val dt = (now - lastTouchTime).coerceAtLeast(1L)

                                                var deltaAngle = currentAngle - lastTouchAngle
                                                if (deltaAngle > 180f) deltaAngle -= 360f
                                                if (deltaAngle < -180f) deltaAngle += 360f

                                                platterAngle = ((platterAngle + deltaAngle) % 360f).toFloat()
                                                scratchAngularVelocity = (deltaAngle / (dt / 1000f)).toFloat()

                                                val deltaSeekMs = ((deltaAngle / 360f) * effectiveRevMs).toLong()
                                                scratchSeekMs = (scratchSeekMs + deltaSeekMs).coerceIn(0L, durationMs.coerceAtLeast(1L))
                                                if (durationMs > 0) {
                                                    onSeek(scratchSeekMs)
                                                }

                                                lastTouchAngle = currentAngle
                                                lastTouchTime = now
                                            }
                                        }
                                        isScratching = false
                                    }
                                }
                        ) {
                            // Draw Platter Beveled Cast Housing
                            drawCircle(
                                color = Color(0xFF222226),
                                radius = platterRadius + 6f,
                                center = platterCenter
                            )
                            drawCircle(
                                color = Color(0xFF141417),
                                radius = platterRadius + 3f,
                                center = platterCenter
                            )

                            // Draw Rotating Vinyl Record
                            drawVinylPlatter(
                                center = platterCenter,
                                radius = platterRadius,
                                angle = platterAngle,
                                rpmMode = rpmMode,
                                isMotorRunning = isPlaying && isMotorPowered,
                                artworkBitmap = artworkBitmap,
                                trackTitle = trackTitle,
                                artistName = artistName,
                                hotCues = hotCues,
                                durationMs = durationMs
                            )
                        }

                        // Tonearm Mechanical Assembly
                        TonearmAssembly(
                            angle = animatedTonearmAngle,
                            liftProgress = tonearmLiftProgress,
                            isPlaying = isPlaying && isMotorPowered,
                            onCueSeek = { cueProgress ->
                                if (durationMs > 0) {
                                    haptic.performTick()
                                    onNeedleDrop()
                                    onSeek((cueProgress * durationMs).toLong())
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                        )

                        // Scratch Hint Ribbon
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isScratching) "SCRATCHING ACTIVE" else "TOUCH PLATTER TO SCRATCH & CUE",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isScratching) Color(0xFF38BDF8) else Color(0xFFA1A1AA)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tactile 4-Pad Hot Cue Matrix (H4 / H10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..4).forEach { cueIdx ->
                            val cue = hotCues[cueIdx]
                            val isAssigned = cue != null
                            val cueColor = Color(cue?.colorHex ?: when (cueIdx) {
                                1 -> 0xFFEF4444
                                2 -> 0xFF3B82F6
                                3 -> 0xFF10B981
                                else -> 0xFFF59E0B
                            })
                            Surface(
                                onClick = {
                                    if (isAssigned && cue != null) {
                                        haptic.performConfirm()
                                        onNeedleDrop()
                                        onSeek(cue.positionMs)
                                    } else if (durationMs > 0) {
                                        haptic.performTick()
                                        hotCues = hotCues + (cueIdx to TurntableHotCue(cueIdx, positionMs))
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAssigned) cueColor.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAssigned) cueColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isAssigned) cueColor else Color(0xFF52525B))
                                        )
                                        Text(
                                            text = "CUE $cueIdx",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAssigned) cueColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val timeText = if (isAssigned && cue != null) {
                                        val m = (cue.positionMs / 1000) / 60
                                        val s = (cue.positionMs / 1000) % 60
                                        "%02d:%02d".format(m, s)
                                    } else {
                                        "+ SET"
                                    }
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.5.sp,
                                        color = if (isAssigned) cueColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3-Way Slipmat Friction Physics Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SLIPMAT FELT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SlipmatType.values().forEach { sm ->
                                val isSelected = selectedSlipmat == sm
                                val smColor = Color(sm.accentColorHex)
                                Surface(
                                    onClick = {
                                        haptic.performTick()
                                        selectedSlipmat = sm
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) smColor.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) smColor else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = sm.label,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) smColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                DeckMode.Cassette -> {
                    // Analog Cassette Deck View Mode
                    CassetteDeckView(
                        isPlaying = isPlaying,
                        progress = progress,
                        trackTitle = trackTitle,
                        artistName = artistName,
                        playbackSpeed = playbackSpeed,
                        isTapeSaturationEnabled = isTapeSaturationEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.30f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Studio Pitch / Tempo Fader (+/- 8.0%)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STUDIO PITCH FADER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val pitchPercent = ((playbackSpeed - 1.0f) * 100f)
                    val pitchStr = if (pitchPercent >= 0) "+%.1f%%".format(pitchPercent) else "%.1f%%".format(pitchPercent)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 0% Center Detent Indicator LED
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (abs(playbackSpeed - 1.0f) < 0.005f) Color(0xFF10B981) else Color(0xFF52525B))
                        )
                        Text(
                            text = "$pitchStr (${"%.2f".format(playbackSpeed)}x)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (playbackSpeed != 1.0f) {
                            Text(
                                text = "RESET",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        haptic.performTick()
                                        onSpeedChange(1.0f)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Slider(
                    value = playbackSpeed,
                    onValueChange = { newSpeed ->
                        onSpeedChange(newSpeed)
                    },
                    valueRange = 0.92f..1.08f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * High-fidelity rendering of the vinyl disc with microgrooves, stroboscopic rim, anisotropic sheen, and album artwork label.
 */
private fun DrawScope.drawVinylPlatter(
    center: Offset,
    radius: Float,
    angle: Float,
    rpmMode: String,
    isMotorRunning: Boolean,
    artworkBitmap: Bitmap?,
    trackTitle: String,
    artistName: String,
    hotCues: Map<Int, TurntableHotCue> = emptyMap(),
    durationMs: Long = 0L
) {
    // 1. Solid Obsidian Vinyl Disc Base
    drawCircle(
        color = Color(0xFF0C0C0E),
        radius = radius,
        center = center
    )

    // 2. 4-Band Stroboscopic Calibration Dots along outer rim
    val strobeRimRadius = radius * 0.96f
    val dotCount = if (rpmMode == "45 RPM") 36 else 48
    for (i in 0 until dotCount) {
        val dotAngle = (i * (360f / dotCount) + angle) * (PI.toFloat() / 180f)
        val dotX = center.x + cos(dotAngle) * strobeRimRadius
        val dotY = center.y + sin(dotAngle) * strobeRimRadius
        drawCircle(
            color = Color(0xFFD4D4D8).copy(alpha = if (i % 2 == 0) 0.65f else 0.30f),
            radius = 1.6f,
            center = Offset(dotX, dotY)
        )
    }

    // Outer lead-in groove glossy ring
    drawCircle(
        color = Color(0xFF18181B),
        radius = radius * 0.92f,
        center = center,
        style = Stroke(width = 2.5f)
    )

    // 3. Realistic Micro-Groove Recording Bands (24 concentric grooved tracks)
    val grooveCount = 24
    for (i in 1..grooveCount) {
        val r = radius * (0.38f + (i.toFloat() / grooveCount) * 0.52f)
        val alpha = when {
            i % 5 == 0 -> 0.45f
            i % 2 == 0 -> 0.25f
            else -> 0.12f
        }
        drawCircle(
            color = Color(0xFF3F3F46).copy(alpha = alpha),
            radius = r,
            center = center,
            style = Stroke(width = if (i % 4 == 0) 1.2f else 0.7f)
        )
    }

    // Hot Cue Radial Grooves & Rotating Marker Pips (H4 / H10)
    hotCues.values.forEach { cue ->
        if (durationMs > 0) {
            val cueFraction = (cue.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val cueRadius = radius * (0.38f + cueFraction * 0.52f)
            val cueColor = Color(cue.colorHex)
            // Color-coded radial groove ring
            drawCircle(
                color = cueColor.copy(alpha = 0.55f),
                radius = cueRadius,
                center = center,
                style = Stroke(width = 1.8f)
            )
            // Rotating needle marker pip
            val pipAngle = (angle + cueFraction * 360f) * (PI.toFloat() / 180f)
            val pipX = center.x + cos(pipAngle) * cueRadius
            val pipY = center.y + sin(pipAngle) * cueRadius
            drawCircle(
                color = cueColor,
                radius = 3.5f,
                center = Offset(pipX, pipY)
            )
            drawCircle(
                color = Color.White,
                radius = 1.5f,
                center = Offset(pipX, pipY)
            )
        }
    }

    // Inner run-out spiral lead groove
    drawCircle(
        color = Color(0xFF27272A),
        radius = radius * 0.37f,
        center = center,
        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 6f), angle))
    )

    // 4. Dual Anisotropic Light Sheen (Butterfly Specular Highlights)
    val sheenRotationRad = (angle * PI.toFloat() / 180f)
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.Transparent,
                Color.White.copy(alpha = 0.18f),
                Color.Transparent,
                Color.White.copy(alpha = 0.12f)
            ),
            center = center
        ),
        radius = radius * 0.92f,
        center = center
    )

    // 5. Center Album Artwork Label
    val labelRadius = radius * 0.35f
    val labelPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(center, labelRadius))
    }

    clipPath(labelPath) {
        if (artworkBitmap != null) {
            // Draw Album Cover in center circular label
            val bmp = artworkBitmap.asImageBitmap()
            drawImage(
                image = bmp,
                dstOffset = IntOffset((center.x - labelRadius).toInt(), (center.y - labelRadius).toInt()),
                dstSize = IntSize((labelRadius * 2).toInt(), (labelRadius * 2).toInt())
            )
            // Dark vignette overlay on artwork
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    center = center,
                    radius = labelRadius
                ),
                radius = labelRadius,
                center = center
            )
        } else {
            // Procedural Dark Studio Label
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE11D48), Color(0xFF881337), Color(0xFF4C0519)),
                    center = center,
                    radius = labelRadius
                ),
                radius = labelRadius,
                center = center
            )
        }
    }

    // Center Label Outer Golden/Silver Trim Ring
    drawCircle(
        color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
        radius = labelRadius,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Spindle Hole & Brushed Metallic Hub Ring
    drawCircle(
        color = Color(0xFF52525B),
        radius = labelRadius * 0.22f,
        center = center
    )
    drawCircle(
        color = Color(0xFFD4D4D8),
        radius = labelRadius * 0.16f,
        center = center,
        style = Stroke(width = 1.5f)
    )
    drawCircle(
        color = Color(0xFF09090B),
        radius = labelRadius * 0.08f,
        center = center
    )
}

/**
 * Optical Strobe Light Beam Tower casting simulated illumination onto the platter rim.
 */
@Composable
private fun StrobeLightBeam(
    isMotorRunning: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isMotorRunning) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            // Prism tower cylinder
            drawRoundRect(
                color = Color(0xFF3F3F46),
                topLeft = Offset(0f, h * 0.35f),
                size = Size(w * 0.55f, h * 0.65f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Red Strobe LED Lens
            drawCircle(
                color = Color(0xFF7F1D1D),
                radius = w * 0.18f,
                center = Offset(w * 0.28f, h * 0.55f)
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "strobe_flicker")
    val strobePulse = infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobe_pulse"
    )

    Canvas(modifier = modifier) {
        val pulseVal = strobePulse.value
        val w = size.width
        val h = size.height
        // Prism tower cylinder
        drawRoundRect(
            color = Color(0xFF3F3F46),
            topLeft = Offset(0f, h * 0.35f),
            size = Size(w * 0.55f, h * 0.65f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Red Strobe LED Lens
        val ledColor = Color(0xFFEF4444).copy(alpha = pulseVal)
        drawCircle(
            color = ledColor,
            radius = w * 0.18f,
            center = Offset(w * 0.28f, h * 0.55f)
        )
        // Projected optical light cone
        val beamPath = Path().apply {
            moveTo(w * 0.28f, h * 0.55f)
            lineTo(w * 1.5f, h * 0.10f)
            lineTo(w * 1.5f, h * 0.90f)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFEF4444).copy(alpha = 0.35f * pulseVal), Color.Transparent),
                startX = w * 0.28f,
                endX = w * 1.5f
            )
        )
    }
}

/**
 * Complete Tonearm Assembly with gimbal bearing pivot, curved titanium wand, headshell, needle-drop shadow, and cueing.
 */
@Composable
private fun TonearmAssembly(
    angle: Float,
    liftProgress: Float,
    isPlaying: Boolean,
    onCueSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pivot = Offset(size.width * 0.86f, size.height * 0.20f)
                    val dx0 = down.position.x - pivot.x
                    val dy0 = down.position.y - pivot.y
                    val distToPivot = kotlin.math.sqrt((dx0 * dx0 + dy0 * dy0).toDouble()).toFloat()
                    val isTonearmHit = down.position.x >= size.width * 0.68f || distToPivot <= size.width * 0.22f
                    if (!isTonearmHit) return@awaitEachGesture

                    var isCueDragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val dragX = change.position.x - down.position.x
                        val dragY = change.position.y - down.position.y
                        val touchSlop = viewConfiguration.touchSlop

                        if (!isCueDragging) {
                            // If vertical drag on non-wand area is dominant, let parent scroll!
                            if (kotlin.math.abs(dragY) > touchSlop && kotlin.math.abs(dragY) > kotlin.math.abs(dragX) * 1.5f && down.position.x < size.width * 0.78f) {
                                return@awaitEachGesture
                            } else if (kotlin.math.abs(dragX) > touchSlop || kotlin.math.abs(dragY) > touchSlop) {
                                isCueDragging = true
                                change.consume()
                                val normalizedProgress = ((change.position.y / size.height) * 1.2f).coerceIn(0f, 1f)
                                onCueSeek(normalizedProgress)
                            }
                        } else {
                            change.consume()
                            val normalizedProgress = ((change.position.y / size.height) * 1.2f).coerceIn(0f, 1f)
                            onCueSeek(normalizedProgress)
                        }
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        val pivot = Offset(w * 0.86f, h * 0.20f)
        val armLength = w * 0.58f

        // Angle math
        val rad = Math.toRadians((angle + 90.0)).toFloat()
        val endX = pivot.x - cos(rad) * armLength
        val endY = pivot.y + sin(rad) * armLength

        // Dynamic needle drop/lift offset: Lifted tonearm casts distant shadow
        val shadowOffset = Offset(-6f * (1f + liftProgress * 1.5f), 10f * (1f + liftProgress * 1.5f))

        // 1. Tonearm Drop Shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.40f / (1f + liftProgress)),
            start = pivot + shadowOffset,
            end = Offset(endX, endY) + shadowOffset,
            strokeWidth = 6.0f,
            cap = StrokeCap.Round
        )

        // 2. Heavy Die-Cast Gimbal Base
        drawCircle(
            color = Color(0xFF27272A),
            radius = 24f,
            center = pivot
        )
        drawCircle(
            color = Color(0xFF52525B),
            radius = 18f,
            center = pivot
        )
        drawCircle(
            color = Color(0xFFD4D4D8),
            radius = 12f,
            center = pivot,
            style = Stroke(width = 2.5f)
        )
        drawCircle(
            color = Color(0xFF18181B),
            radius = 6f,
            center = pivot
        )

        // 3. Brushed Titanium Tonearm Wand
        drawLine(
            color = Color(0xFFE4E4E7),
            start = pivot,
            end = Offset(endX, endY),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )

        // 4. Cartridge Headshell & Stylus
        val headLength = 26f
        val headAngle = rad + 0.38f
        val headX = endX - cos(headAngle) * headLength
        val headY = endY + sin(headAngle) * headLength

        // Headshell body
        drawLine(
            color = Color(0xFFE11D48),
            start = Offset(endX, endY),
            end = Offset(headX, headY),
            strokeWidth = 8.5f,
            cap = StrokeCap.Round
        )

        // Finger lift hook on headshell
        val hookAngle = headAngle - 1.2f
        val hookX = endX - cos(hookAngle) * 14f
        val hookY = endY + sin(hookAngle) * 14f
        drawLine(
            color = Color(0xFFD4D4D8),
            start = Offset(endX, endY),
            end = Offset(hookX, hookY),
            strokeWidth = 2.5f
        )

        // Diamond stylus needle tip LED
        val needleLedColor = when {
            liftProgress > 0.5f -> Color.White
            isPlaying -> Color(0xFF10B981) // Grooving
            else -> Color(0xFFF59E0B) // Cueing
        }
        drawCircle(
            color = needleLedColor,
            radius = 3.5f,
            center = Offset(headX, headY)
        )
    }
}

/**
 * Alternate Analog Cassette Tape Deck Simulation with rotating 6-tooth spools and dynamic VU meters.
 */
@Composable
private fun CassetteDeckView(
    isPlaying: Boolean,
    progress: Float,
    trackTitle: String,
    artistName: String,
    playbackSpeed: Float,
    isTapeSaturationEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tape_spool_spin")
    val spoolAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1600 / playbackSpeed.coerceAtLeast(0.5f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "spool_angle"
    )

    // Dynamic VU Needle Ballistics
    val vuTransition = rememberInfiniteTransition(label = "vu_meters")
    val vuLeftEnergy by vuTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vu_left"
    )
    val vuRightEnergy by vuTransition.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(440, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vu_right"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121214))
            .border(2.dp, Color(0xFF27272A), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Dual Analog VU Meters Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left VU Meter
                AnalogVuMeter(
                    label = "CH-L (dB)",
                    energy = if (isPlaying) vuLeftEnergy else 0.05f,
                    isTapeWarm = isTapeSaturationEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                // Right VU Meter
                AnalogVuMeter(
                    label = "CH-R (dB)",
                    energy = if (isPlaying) vuRightEnergy else 0.05f,
                    isTapeWarm = isTapeSaturationEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transparent Cassette Shell Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E24))
                    .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(12.dp))
            ) {
                // Cassette Spool & Tape Window Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Smoked Acrylic Center Window
                    val windowRect = androidx.compose.ui.geometry.Rect(
                        Offset(w * 0.12f, h * 0.15f),
                        Size(w * 0.76f, h * 0.70f)
                    )
                    drawRoundRect(
                        color = Color(0xFF09090B),
                        topLeft = windowRect.topLeft,
                        size = windowRect.size,
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Spool Centers
                    val leftCenter = Offset(w * 0.32f, h * 0.50f)
                    val rightCenter = Offset(w * 0.68f, h * 0.50f)
                    val maxTapeRadius = h * 0.32f
                    val minTapeRadius = h * 0.14f

                    // Dynamic Tape Pack Thickness: Left empties, Right fills
                    val leftTapeRadius = minTapeRadius + (maxTapeRadius - minTapeRadius) * sqrt(1f - progress)
                    val rightTapeRadius = minTapeRadius + (maxTapeRadius - minTapeRadius) * sqrt(progress)

                    // Magnetic Brown Tape Packs
                    drawCircle(
                        color = Color(0xFF3A2416),
                        radius = leftTapeRadius,
                        center = leftCenter
                    )
                    drawCircle(
                        color = Color(0xFF3A2416),
                        radius = rightTapeRadius,
                        center = rightCenter
                    )

                    // Magnetic Tape Ribbon Bridge
                    drawLine(
                        color = Color(0xFF2C1B10),
                        start = Offset(leftCenter.x, h * 0.82f),
                        end = Offset(rightCenter.x, h * 0.82f),
                        strokeWidth = 5f
                    )

                    // Left & Right 6-Tooth White Nylon Spool Hubs
                    drawCassetteSpool(
                        center = leftCenter,
                        angle = if (isPlaying) spoolAngle else 0f
                    )
                    drawCassetteSpool(
                        center = rightCenter,
                        angle = if (isPlaying) spoolAngle else 0f
                    )
                }

                // Cassette Label Details
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PIXELODY CHROME TYPE II • 90 MIN",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706)
                    )
                    Text(
                        text = "$trackTitle • $artistName",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = Color(0xFFA1A1AA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 6-Tooth Nylon Spool Hub for cassette simulation.
 */
private fun DrawScope.drawCassetteSpool(
    center: Offset,
    angle: Float
) {
    val hubRadius = 24f
    // White Spool Disc
    drawCircle(
        color = Color(0xFFE4E4E7),
        radius = hubRadius,
        center = center
    )
    drawCircle(
        color = Color(0xFF27272A),
        radius = hubRadius * 0.65f,
        center = center
    )

    // 6 Mechanical Teeth
    for (i in 0 until 6) {
        val toothAngle = (i * 60f + angle) * (PI.toFloat() / 180f)
        val tx = center.x + cos(toothAngle) * (hubRadius * 0.78f)
        val ty = center.y + sin(toothAngle) * (hubRadius * 0.78f)
        drawCircle(
            color = Color(0xFFE4E4E7),
            radius = 3.5f,
            center = Offset(tx, ty)
        )
    }

    // Center Spindle Hole
    drawCircle(
        color = Color(0xFF09090B),
        radius = 5f,
        center = center
    )
}

/**
 * Analog Needle VU Meter with amber tube illumination and calibrated scale.
 */
@Composable
private fun AnalogVuMeter(
    label: String,
    energy: Float,
    isTapeWarm: Boolean,
    modifier: Modifier = Modifier
) {
    val meterBg = if (isTapeWarm) Color(0xFF2A1E14) else Color(0xFF18181B)
    val scaleColor = if (isTapeWarm) Color(0xFFF59E0B) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp)),
        color = meterBg
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val w = size.width
            val h = size.height

            // Scale Arc (-20dB to +3dB)
            val arcCenter = Offset(w * 0.5f, h * 1.05f)
            val arcRadius = h * 0.95f

            drawArc(
                color = scaleColor.copy(alpha = 0.35f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = 1.2f)
            )

            // Red Overload Peak Zone (+0dB to +3dB)
            drawArc(
                color = Color(0xFFEF4444).copy(alpha = 0.75f),
                startAngle = 310f,
                sweepAngle = 30f,
                useCenter = false,
                topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = 2.2f)
            )

            // Needle Sweep: 200 deg to 340 deg based on energy
            val needleAngleDeg = 200f + (energy.coerceIn(0f, 1f) * 140f)
            val needleRad = needleAngleDeg * (PI.toFloat() / 180f)
            val needleEnd = Offset(
                arcCenter.x + cos(needleRad) * (arcRadius * 0.88f),
                arcCenter.y + sin(needleRad) * (arcRadius * 0.88f)
            )

            // Needle line
            drawLine(
                color = if (needleAngleDeg > 310f) Color(0xFFEF4444) else Color(0xFFF43F5E),
                start = arcCenter,
                end = needleEnd,
                strokeWidth = 2.0f,
                cap = StrokeCap.Round
            )

            // Meter Pivot Hub
            drawCircle(
                color = Color(0xFF27272A),
                radius = 8f,
                center = arcCenter
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 2.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isTapeWarm) Color(0xFFF59E0B) else Color(0xFFA1A1AA)
            )
        }
    }
}
