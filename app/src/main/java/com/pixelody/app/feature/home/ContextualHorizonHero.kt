package com.pixelody.app.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pixelody.app.core.analytics.SessionSoundInsights
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.SessionCapsuleCard

/**
 * Contextual Horizon Hero that morphs between the Single-Tap Horizon Cue (Mode 1)
 * and the Active Groove Session Console (Mode 2) using fluid Compose spring transitions.
 */
@Composable
fun ContextualHorizonHero(
    surfaceState: DynamicHomeSurfaceState,
    onLaunchHero: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenTurntable: () -> Unit = {},
    onCycleEqualizer: () -> Unit = {},
    modifier: Modifier = Modifier,
    onShowDoc: ((String) -> Unit)? = null
) {
    AnimatedContent(
        targetState = surfaceState.operationalMode,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "heroMorphTransition",
        modifier = modifier.fillMaxWidth()
    ) { mode ->
        when (mode) {
            HomeOperationalMode.ActiveGroove -> {
                if (surfaceState.activeTrack != null) {
                    ActiveGrooveConsole(
                        activeTrack = surfaceState.activeTrack,
                        nextTrack = surfaceState.nextTrack,
                        sessionInsights = surfaceState.sessionInsights,
                        harmonicNextKey = surfaceState.harmonicNextKey,
                        onOpenPlayer = onOpenPlayer,
                        onOpenQueue = onOpenQueue,
                        onOpenTurntable = onOpenTurntable,
                        onCycleEqualizer = onCycleEqualizer
                    )
                }
            }
            else -> {
                if (surfaceState.capsule != null) {
                    SessionCapsuleCard(
                        insights = surfaceState.sessionInsights ?: SessionSoundInsights(),
                        onDailySoundCheck = onLaunchHero,
                        dailyCapsule = surfaceState.capsule,
                        onOpenTimeline = onOpenTimeline,
                        onShowDoc = onShowDoc
                    )
                }
            }
        }
    }
}
