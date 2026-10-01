package com.pixelody.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.theme.PixelodyDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Supported quick-action types available in the radial habit wheel (legacy compatibility).
 */
enum class OrbitActionType {
    PlayNext,
    AddToQueue,
    ToggleFavorite,
    StartFlowRadio,
    ToggleOffline
}

/**
 * Universal Action Identifier across tracks, sources, capsules, and quick pockets.
 */
enum class CabinetActionId {
    // Track actions
    PlayNext,
    AddToQueue,
    ToggleFavorite,
    StartFlowRadio,
    ToggleOffline,

    // Source - Desktop actions
    ConnectDesktop,
    RefreshHostLibrary,
    RescanHostDirectories,
    OpenDesktopPairing,

    // Source - Phone actions
    RescanPhoneStorage,
    PickAudioFiles,
    ClearCacheStorage,
    OpenFileBrowser,

    // Source - Jam actions
    StartJamSession,
    JoinJamSession,
    BroadcastJamPresence,
    ClearJamQueue,

    // Source - All / General actions
    QuickPlayFlow,
    ShuffleAllPool,
    ResetFilters,
    DailySoundCheck
}

/**
 * Targets that can be long-pressed to open a contextual settings cabinet bubble menu.
 */
sealed interface CabinetTarget {
    data class TrackTarget(val track: Track) : CabinetTarget
    data class SourceTarget(
        val scope: SourceScope,
        val isConnected: Boolean = false,
        val isJamActive: Boolean = false,
        val count: Int = 0
    ) : CabinetTarget
    data class CapsuleTarget(
        val streakDays: Int = 3,
        val title: String = "Morning Warmth",
        val subtitle: String = "Acoustic Rise"
    ) : CabinetTarget
    data class QuickStartTarget(
        val category: String = "Quick Start"
    ) : CabinetTarget
}

/**
 * Metadata and visual styling for an action bubble satellite.
 */
data class CabinetActionItem(
    val id: CabinetActionId,
    val glyphType: TransportGlyphType,
    val label: String,
    val subtitle: String,
    val accentColor: Color = PixelodyDirection.Ink,
    val enabled: Boolean = true
)

/**
 * Legacy compatibility alias for OrbitActionItem.
 */
data class OrbitActionItem(
    val type: OrbitActionType,
    val glyphType: TransportGlyphType = TransportGlyphType.PlayNext,
    val label: String,
    val subtitle: String,
    val icon: String = "",
    val accentColor: Color = PixelodyDirection.Ink
)

/**
 * Positioned orbital item with precomputed polar / cartesian coordinates.
 */
data class CabinetPosition(
    val item: CabinetActionItem,
    val offset: Offset,
    val angleDegrees: Float
)

data class OrbitPosition(
    val item: OrbitActionItem,
    val offset: Offset,
    val angleDegrees: Float
)

/**
 * Calculates screen-relative Cartesian coordinates for each cabinet bubble satellite.
 */
fun computeCabinetPositions(
    centerOffset: Offset,
    screenWidth: Float,
    screenHeight: Float,
    items: List<CabinetActionItem>,
    radiusPx: Float,
    safeMarginPx: Float = 44f
): List<CabinetPosition> {
    if (items.isEmpty()) return emptyList()

    val normX = if (screenWidth > 0f) centerOffset.x / screenWidth else 0.5f
    val normY = if (screenHeight > 0f) centerOffset.y / screenHeight else 0.5f

    // Determine arc orientation based on position on screen
    val (startAngleDeg, endAngleDeg) = when {
        // Near left screen edge: fan rightward into screen
        normX < 0.35f -> Pair(-65f, 65f)
        // Near right screen edge: fan leftward into screen
        normX > 0.65f -> Pair(115f, 245f)
        // Near bottom screen edge: fan upward
        normY > 0.65f -> Pair(200f, 340f)
        // Near top screen edge: fan downward
        normY < 0.35f -> Pair(20f, 160f)
        // Center default: upward arch
        else -> Pair(195f, 345f)
    }

    val count = items.size
    val angleStep = if (count > 1) (endAngleDeg - startAngleDeg) / (count - 1) else 0f

    return items.mapIndexed { index, item ->
        val angleDeg = if (count == 1) (startAngleDeg + endAngleDeg) / 2f else startAngleDeg + index * angleStep
        val angleRad = (angleDeg * PI / 180f).toFloat()

        val rawX = centerOffset.x + radiusPx * cos(angleRad)
        val rawY = centerOffset.y + radiusPx * sin(angleRad)

        val clampedX = if (screenWidth > 0f) rawX.coerceIn(safeMarginPx, screenWidth - safeMarginPx) else rawX
        val clampedY = if (screenHeight > 0f) rawY.coerceIn(safeMarginPx, screenHeight - safeMarginPx) else rawY

        CabinetPosition(
            item = item,
            offset = Offset(clampedX, clampedY),
            angleDegrees = angleDeg
        )
    }
}

fun computeOrbitPositions(
    centerOffset: Offset,
    screenWidth: Float,
    screenHeight: Float,
    items: List<OrbitActionItem>,
    radiusPx: Float,
    safeMarginPx: Float = 44f
): List<OrbitPosition> {
    val cabinetItems = items.map {
        CabinetActionItem(
            id = when (it.type) {
                OrbitActionType.PlayNext -> CabinetActionId.PlayNext
                OrbitActionType.AddToQueue -> CabinetActionId.AddToQueue
                OrbitActionType.ToggleFavorite -> CabinetActionId.ToggleFavorite
                OrbitActionType.StartFlowRadio -> CabinetActionId.StartFlowRadio
                OrbitActionType.ToggleOffline -> CabinetActionId.ToggleOffline
            },
            glyphType = it.glyphType,
            label = it.label,
            subtitle = it.subtitle,
            accentColor = it.accentColor
        )
    }
    val positions = computeCabinetPositions(centerOffset, screenWidth, screenHeight, cabinetItems, radiusPx, safeMarginPx)
    return positions.mapIndexed { index, pos ->
        OrbitPosition(
            item = items[index],
            offset = pos.offset,
            angleDegrees = pos.angleDegrees
        )
    }
}

/**
 * Checks whether a touch/drag offset falls within hitRadiusPx of any cabinet satellite.
 */
fun classifyCabinetHover(
    touchOffset: Offset,
    satellites: List<CabinetPosition>,
    hitRadiusPx: Float
): CabinetActionId? {
    var closest: CabinetActionId? = null
    var minDistanceSq = hitRadiusPx * hitRadiusPx

    for (satellite in satellites) {
        val dx = touchOffset.x - satellite.offset.x
        val dy = touchOffset.y - satellite.offset.y
        val distSq = dx * dx + dy * dy
        if (distSq <= minDistanceSq) {
            minDistanceSq = distSq
            closest = satellite.item.id
        }
    }
    return closest
}

fun classifyOrbitHover(
    touchOffset: Offset,
    satellites: List<OrbitPosition>,
    hitRadiusPx: Float
): OrbitActionType? {
    var closest: OrbitActionType? = null
    var minDistanceSq = hitRadiusPx * hitRadiusPx

    for (satellite in satellites) {
        val dx = touchOffset.x - satellite.offset.x
        val dy = touchOffset.y - satellite.offset.y
        val distSq = dx * dx + dy * dy
        if (distSq <= minDistanceSq) {
            minDistanceSq = distSq
            closest = satellite.item.type
        }
    }
    return closest
}

/**
 * Builds contextual action bubbles based on the target type (Track, Source Scope, Capsule, Quick Start).
 */
fun buildCabinetActionItems(
    target: CabinetTarget,
    isFavorite: Boolean = false,
    isOffline: Boolean = false
): List<CabinetActionItem> = when (target) {
    is CabinetTarget.TrackTarget -> listOf(
        CabinetActionItem(
            id = CabinetActionId.PlayNext,
            glyphType = TransportGlyphType.PlayNext,
            label = "Play Next",
            subtitle = "Up next in queue",
            accentColor = PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.AddToQueue,
            glyphType = TransportGlyphType.AddToQueue,
            label = "Add Queue",
            subtitle = "Append to queue",
            accentColor = PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.ToggleFavorite,
            glyphType = if (isFavorite) TransportGlyphType.HeartFilled else TransportGlyphType.Heart,
            label = if (isFavorite) "Favorited" else "Favorite",
            subtitle = if (isFavorite) "In Favorites" else "Save track",
            accentColor = if (isFavorite) PixelodyDirection.Palette.HotPink else PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.StartFlowRadio,
            glyphType = TransportGlyphType.FlowShuffle,
            label = "Smart Flow",
            subtitle = "Harmonic Radio",
            accentColor = PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.ToggleOffline,
            glyphType = if (isOffline) TransportGlyphType.OfflineCheck else TransportGlyphType.Download,
            label = if (isOffline) "Saved" else "Offline",
            subtitle = if (isOffline) "Downloaded" else "Save to device",
            accentColor = if (isOffline) PixelodyDirection.Palette.Mint else PixelodyDirection.Ink
        )
    )
    is CabinetTarget.SourceTarget -> when (target.scope) {
        SourceScope.DesktopHost -> listOf(
            CabinetActionItem(
                id = CabinetActionId.ConnectDesktop,
                glyphType = TransportGlyphType.DesktopHost,
                label = if (target.isConnected) "Reconnect" else "Connect",
                subtitle = if (target.isConnected) "Refresh socket" else "Connect to PC",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.RefreshHostLibrary,
                glyphType = TransportGlyphType.Refresh,
                label = "Soft Refresh",
                subtitle = "Sync library",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.RescanHostDirectories,
                glyphType = TransportGlyphType.WaveformBars,
                label = "Rescan Files",
                subtitle = "Search PC files",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.OpenDesktopPairing,
                glyphType = TransportGlyphType.Settings,
                label = "Host Pair",
                subtitle = "Pairing settings",
                accentColor = PixelodyDirection.Ink
            )
        )
        SourceScope.LocalPhone -> listOf(
            CabinetActionItem(
                id = CabinetActionId.RescanPhoneStorage,
                glyphType = TransportGlyphType.Refresh,
                label = "Rescan Phone",
                subtitle = "Scan MediaStore",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.PickAudioFiles,
                glyphType = TransportGlyphType.Folder,
                label = "Pick Audio",
                subtitle = "Choose folder/file",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.ClearCacheStorage,
                glyphType = TransportGlyphType.Download,
                label = "Cache Store",
                subtitle = "Manage downloads",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.OpenFileBrowser,
                glyphType = TransportGlyphType.PhoneDevice,
                label = "Files Screen",
                subtitle = "Local browser",
                accentColor = PixelodyDirection.Ink
            )
        )
        SourceScope.JamMesh -> listOf(
            CabinetActionItem(
                id = CabinetActionId.StartJamSession,
                glyphType = TransportGlyphType.MeshNetwork,
                label = if (target.isJamActive) "Jam Active" else "Start Jam",
                subtitle = "Host mesh room",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.JoinJamSession,
                glyphType = TransportGlyphType.Broadcast,
                label = "Join Jam",
                subtitle = "Join nearby session",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.BroadcastJamPresence,
                glyphType = TransportGlyphType.Refresh,
                label = "Sync Mesh",
                subtitle = "Broadcast state",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.ClearJamQueue,
                glyphType = TransportGlyphType.Queue,
                label = "Clear Jam",
                subtitle = "Reset session queue",
                accentColor = PixelodyDirection.Ink
            )
        )
        SourceScope.All -> listOf(
            CabinetActionItem(
                id = CabinetActionId.QuickPlayFlow,
                glyphType = TransportGlyphType.FlowShuffle,
                label = "Quick Start",
                subtitle = "Instant playback",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.ShuffleAllPool,
                glyphType = TransportGlyphType.Shuffle,
                label = "Shuffle All",
                subtitle = "All sources pool",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.ResetFilters,
                glyphType = TransportGlyphType.OmniSource,
                label = "Reset Lens",
                subtitle = "Clear active filters",
                accentColor = PixelodyDirection.Ink
            ),
            CabinetActionItem(
                id = CabinetActionId.DailySoundCheck,
                glyphType = TransportGlyphType.Checkmark,
                label = "Sound Check",
                subtitle = "Daily capsule flow",
                accentColor = PixelodyDirection.Ink
            )
        )
    }
    is CabinetTarget.CapsuleTarget -> listOf(
        CabinetActionItem(
            id = CabinetActionId.DailySoundCheck,
            glyphType = TransportGlyphType.Checkmark,
            label = "Sound Check",
            subtitle = "Execute daily ritual",
            accentColor = PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.QuickPlayFlow,
            glyphType = TransportGlyphType.Play,
            label = "Play Capsule",
            subtitle = "Start warmth mix",
            accentColor = PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.ResetFilters,
            glyphType = TransportGlyphType.DiamondLossless,
            label = "Lossless 75%",
            subtitle = "Hi-Res priority",
            accentColor = PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.StartFlowRadio,
            glyphType = TransportGlyphType.FlowShuffle,
            label = "Harmonic Flow",
            subtitle = "Auto-mix queue",
            accentColor = PixelodyDirection.Ink
        )
    )
    is CabinetTarget.QuickStartTarget -> listOf(
        CabinetActionItem(
            id = CabinetActionId.QuickPlayFlow,
            glyphType = TransportGlyphType.Play,
            label = "Instant Flow",
            subtitle = "Play top recommendation",
            accentColor = PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.ShuffleAllPool,
            glyphType = TransportGlyphType.Shuffle,
            label = "Smart Shuffle",
            subtitle = "Dynamic sequence",
            accentColor = PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.AddToQueue,
            glyphType = TransportGlyphType.AddToQueue,
            label = "Queue All",
            subtitle = "Add pool to queue",
            accentColor = PixelodyDirection.Ink
        ),
        CabinetActionItem(
            id = CabinetActionId.ResetFilters,
            glyphType = TransportGlyphType.OmniSource,
            label = "Full Library",
            subtitle = "Open entire catalog",
            accentColor = PixelodyDirection.Ink
        )
    )
}

fun buildOrbitActionItems(
    isFavorite: Boolean,
    isOffline: Boolean
): List<OrbitActionItem> {
    val items = buildCabinetActionItems(CabinetTarget.TrackTarget(Track(id = "", title = "", artist = "")), isFavorite, isOffline)
    return items.map {
        OrbitActionItem(
            type = when (it.id) {
                CabinetActionId.PlayNext -> OrbitActionType.PlayNext
                CabinetActionId.AddToQueue -> OrbitActionType.AddToQueue
                CabinetActionId.ToggleFavorite -> OrbitActionType.ToggleFavorite
                CabinetActionId.StartFlowRadio -> OrbitActionType.StartFlowRadio
                CabinetActionId.ToggleOffline -> OrbitActionType.ToggleOffline
                else -> OrbitActionType.PlayNext
            },
            glyphType = it.glyphType,
            label = it.label,
            subtitle = it.subtitle,
            accentColor = it.accentColor
        )
    }
}

/**
 * Fullscreen Universal Cabinet Bubble Overlay with spring physics, quadrant-aware orbit geometry,
 * vector glyphs, and continuous drag-and-release gesture execution across tracks, sources, and capsules.
 */
@Composable
fun UniversalCabinetOverlay(
    target: CabinetTarget,
    anchorOffset: Offset? = null,
    isFavorite: Boolean = false,
    isOffline: Boolean = false,
    onDismiss: () -> Unit,
    onAction: (CabinetActionId) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current

    BackHandler(enabled = true) {
        onDismiss()
    }

    val actionItems = remember(target, isFavorite, isOffline) {
        buildCabinetActionItems(target = target, isFavorite = isFavorite, isOffline = isOffline)
    }

    // Spring entrance scale & alpha
    val scaleAnim = remember { Animatable(0.4f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        )
    }

    // Interactive Drag / Hover tracking
    var currentTouchOffset by remember { mutableStateOf<Offset?>(null) }
    var hoveredAction by remember { mutableStateOf<CabinetActionId?>(null) }

    // Breathing pulse for center hub
    val infiniteTransition = rememberInfiniteTransition(label = "hub_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * alphaAnim.value))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onDismiss() }
                )
            }
            .pointerInput(actionItems) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentTouchOffset = offset
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchOffset = change.position
                    },
                    onDragEnd = {
                        hoveredAction?.let { action ->
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onAction(action)
                        } ?: onDismiss()
                        currentTouchOffset = null
                        hoveredAction = null
                    },
                    onDragCancel = {
                        currentTouchOffset = null
                        hoveredAction = null
                        onDismiss()
                    }
                )
            }
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        val center = remember(anchorOffset, screenWidth, screenHeight) {
            anchorOffset?.let {
                Offset(
                    it.x.coerceIn(screenWidth * 0.2f, screenWidth * 0.8f),
                    it.y.coerceIn(screenHeight * 0.25f, screenHeight * 0.75f)
                )
            } ?: Offset(screenWidth / 2f, screenHeight / 2f)
        }

        val orbitRadiusPx = with(density) { 110.dp.toPx() }
        val hitRadiusPx = with(density) { 42.dp.toPx() }

        val satellites = remember(center, screenWidth, screenHeight, actionItems, orbitRadiusPx) {
            computeCabinetPositions(
                centerOffset = center,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                items = actionItems,
                radiusPx = orbitRadiusPx
            )
        }

        LaunchedEffect(currentTouchOffset) {
            val touch = currentTouchOffset
            if (touch != null) {
                val newHover = classifyCabinetHover(touch, satellites, hitRadiusPx)
                if (newHover != hoveredAction) {
                    if (newHover != null) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    hoveredAction = newHover
                }
            } else {
                hoveredAction = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                }
                .drawBehind {
                    drawCircle(
                        color = PixelodyDirection.Ink.copy(alpha = 0.08f),
                        radius = orbitRadiusPx,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )

                    currentTouchOffset?.let { touch ->
                        drawLine(
                            brush = Brush.radialGradient(
                                colors = listOf(PixelodyDirection.Ink.copy(alpha = 0.6f), Color.Transparent),
                                center = center,
                                radius = orbitRadiusPx * 1.5f
                            ),
                            start = center,
                            end = touch,
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
        ) {
            // Central Target Anchor Hub
            CentralCabinetHub(
                target = target,
                pulseScale = pulseGlow,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (center.x - 70.dp.toPx()).roundToInt(),
                            (center.y - 70.dp.toPx()).roundToInt()
                        )
                    }
                    .size(140.dp)
            )

            // Orbit Action Satellites
            satellites.forEach { sat ->
                val isHovered = hoveredAction == sat.item.id
                val satScale by animateFloatAsState(
                    targetValue = if (isHovered) 1.28f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "sat_scale_${sat.item.id}"
                )

                CabinetSatelliteNode(
                    position = sat,
                    isHovered = isHovered,
                    scale = satScale,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onAction(sat.item.id)
                    },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (sat.offset.x - 34.dp.toPx()).roundToInt(),
                                (sat.offset.y - 34.dp.toPx()).roundToInt()
                            )
                        }
                        .size(68.dp)
                )
            }
        }
    }
}

/**
 * Legacy compatibility wrapper.
 */
@Composable
fun RadialHabitWheelOverlay(
    track: Track,
    isFavorite: Boolean,
    isOffline: Boolean = false,
    anchorOffset: Offset? = null,
    onDismiss: () -> Unit,
    onAction: (OrbitActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    UniversalCabinetOverlay(
        target = CabinetTarget.TrackTarget(track),
        anchorOffset = anchorOffset,
        isFavorite = isFavorite,
        isOffline = isOffline,
        onDismiss = onDismiss,
        onAction = { actionId ->
            val legacyAction = when (actionId) {
                CabinetActionId.PlayNext -> OrbitActionType.PlayNext
                CabinetActionId.AddToQueue -> OrbitActionType.AddToQueue
                CabinetActionId.ToggleFavorite -> OrbitActionType.ToggleFavorite
                CabinetActionId.StartFlowRadio -> OrbitActionType.StartFlowRadio
                CabinetActionId.ToggleOffline -> OrbitActionType.ToggleOffline
                else -> OrbitActionType.PlayNext
            }
            onAction(legacyAction)
        },
        modifier = modifier
    )
}

/**
 * Center Hub previewing the selected target (Track, Source Lens, Capsule, or Quick Start).
 */
@Composable
private fun CentralCabinetHub(
    target: CabinetTarget,
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    // Source scope is a real distinction the host owns, so it keeps a hue -
    // routed through the colour scheme so it follows the selected theme.
    val losslessAccent = MaterialTheme.colorScheme.secondary
    val hubColor = when (target) {
        is CabinetTarget.TrackTarget -> MaterialTheme.colorScheme.onSurface
        is CabinetTarget.SourceTarget -> when (target.scope) {
            SourceScope.All -> MaterialTheme.colorScheme.primary
            SourceScope.LocalPhone -> MaterialTheme.colorScheme.tertiary
            SourceScope.DesktopHost -> MaterialTheme.colorScheme.secondary
            SourceScope.JamMesh -> MaterialTheme.colorScheme.primary
        }
        is CabinetTarget.CapsuleTarget -> losslessAccent
        is CabinetTarget.QuickStartTarget -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Ambient breathing halo
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            hubColor.copy(alpha = 0.35f),
                            hubColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Core Hub Disc
        Surface(
            modifier = Modifier.size(116.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            border = BorderStroke(1.5.dp, hubColor.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (target) {
                    is CabinetTarget.TrackTarget -> {
                        val track = target.track
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelodyTransportGlyph(
                                glyph = if (track.lossless) TransportGlyphType.DiamondLossless else TransportGlyphType.VinylDisc,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                sizeDp = 18
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = track.title.ifBlank { "Selected Track" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = track.artist.ifBlank { "Pixelody" },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        if (track.lossless) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(losslessAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "LOSSLESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = losslessAccent
                                )
                            }
                        }
                    }
                    is CabinetTarget.SourceTarget -> {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(hubColor.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelodyTransportGlyph(
                                glyph = target.scope.toGlyphType(),
                                color = hubColor,
                                sizeDp = 18
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = target.scope.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val statusSubtitle = when (target.scope) {
                            SourceScope.DesktopHost -> if (target.isConnected) "Connected (${target.count})" else "Disconnected"
                            SourceScope.LocalPhone -> "${target.count} Local Files"
                            SourceScope.JamMesh -> if (target.isJamActive) "Session Active" else "Mesh Ready"
                            SourceScope.All -> "${target.count} Total Tracks"
                        }

                        Text(
                            text = statusSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                    is CabinetTarget.CapsuleTarget -> {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(losslessAccent.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.FlameStreak,
                                color = losslessAccent,
                                sizeDp = 18
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = target.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "${target.streakDays}d Streak • ${target.subtitle}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                    is CabinetTarget.QuickStartTarget -> {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.PixelodyEmblem,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                sizeDp = 18
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = target.category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Quick Cabinet",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual satellite action node rendered along the orbit circumference with vector glyphs.
 */
@Composable
private fun CabinetSatelliteNode(
    position: CabinetPosition,
    isHovered: Boolean,
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = position.item
    val baseColor = item.accentColor

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isHovered) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Surface(
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            color = if (isHovered) baseColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            tonalElevation = if (isHovered) 12.dp else 4.dp,
            border = BorderStroke(
                width = if (isHovered) 2.dp else 1.dp,
                color = if (isHovered) Color.White else baseColor.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PixelodyTransportGlyph(
                    glyph = item.glyphType,
                    color = if (isHovered) Color.White else MaterialTheme.colorScheme.onSurface,
                    sizeDp = if (isHovered) 20 else 18
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isHovered) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isHovered) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
