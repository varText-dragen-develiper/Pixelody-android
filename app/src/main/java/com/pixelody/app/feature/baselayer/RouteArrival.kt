package com.pixelody.app.feature.baselayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Small route reveal; Compose respects Android's animation duration scale. */
@Composable
internal fun routeArrival(key: Any): Modifier {
    val progress = remember(key) { Animatable(0f) }
    val distance = with(LocalDensity.current) { 8.dp.toPx() }
    LaunchedEffect(progress) { progress.animateTo(1f, tween(180, easing = FastOutSlowInEasing)) }
    return Modifier.graphicsLayer { alpha = progress.value; translationY = distance * (1f - progress.value) }
}
