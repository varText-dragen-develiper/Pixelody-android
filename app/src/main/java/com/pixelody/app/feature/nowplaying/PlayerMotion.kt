package com.pixelody.app.feature.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged

/** Distance commits a deliberate pull; velocity commits a short, purposeful flick. */
internal fun commitsPull(distance: Float, velocity: Float, threshold: Float): Boolean =
    velocity > -650f && (distance >= threshold || (distance >= threshold / 4f && velocity >= 650f))

internal class PlayerMotion {
    val progress = Animatable(0f)
    var presented by mutableStateOf(false)
    var dragging by mutableStateOf(false)
    var dragProgress by mutableFloatStateOf(0f)
    var height by mutableFloatStateOf(1f)
    var releaseVelocity = 0f
    var releasedDrag = false
    private var originOpen = false
    private var distance = 0f
    var open: () -> Unit = {}
    var close: () -> Unit = {}
    val fraction: Float get() = if (dragging) dragProgress else progress.value

    fun begin(upward: Boolean) {
        originOpen = !upward
        distance = 0f
        releaseVelocity = 0f
        dragProgress = if (presented) fraction else 0f
        dragging = true
        presented = true
        if (upward) open()
    }
    fun drag(delta: Float) {
        distance += delta
        dragProgress = (dragProgress - delta / height).coerceIn(0f, 1f)
    }
    fun finish(velocity: Float, threshold: Float, cancelled: Boolean = false) {
        val directedDistance = if (originOpen) distance else -distance
        val directedVelocity = if (originOpen) velocity else -velocity
        val commit = !cancelled && commitsPull(directedDistance, directedVelocity, threshold)
        val targetOpen = if (commit) !originOpen else originOpen
        releasedDrag = true
        releaseVelocity = -velocity / height
        if (targetOpen) open() else close()
        dragging = false
    }
    fun layer(): Modifier = Modifier.onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
        .graphicsLayer { translationY = height * (1f - fraction) }
}

internal val LocalPlayerMotion = staticCompositionLocalOf<PlayerMotion?> { null }

@Composable
internal fun rememberPlayerMotion(isOpen: Boolean, onOpen: () -> Unit, onClose: () -> Unit): PlayerMotion {
    val motion = remember { PlayerMotion() }
    val fallbackHeight = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    if (motion.height <= 1f) motion.height = fallbackHeight
    motion.open = onOpen
    motion.close = onClose
    LaunchedEffect(isOpen, motion.dragging) {
        if (motion.dragging) return@LaunchedEffect
        if (isOpen) motion.presented = true
        if (motion.releasedDrag) {
            motion.progress.snapTo(motion.dragProgress)
            motion.releasedDrag = false
        }
        motion.progress.animateTo(if (isOpen) 1f else 0f,
            spring(dampingRatio = 1f, stiffness = 380f), initialVelocity = motion.releaseVelocity)
        motion.dragProgress = motion.progress.value
        motion.releaseVelocity = 0f
        if (!isOpen) motion.presented = false
    }
    return motion
}
