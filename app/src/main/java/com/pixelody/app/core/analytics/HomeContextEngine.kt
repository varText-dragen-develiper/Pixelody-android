package com.pixelody.app.core.analytics

import com.pixelody.app.core.playback.AudioDeviceRoute
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.data.model.SonicMood
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.storage.CircadianPhase
import com.pixelody.app.data.storage.HabitActionType
import com.pixelody.app.data.storage.LocalHabitStore
import com.pixelody.app.feature.home.DynamicHomeSurfaceState
import com.pixelody.app.feature.home.HabitQuickPath
import com.pixelody.app.feature.home.HardwareAnchorState
import com.pixelody.app.feature.home.HomeOperationalMode
import java.util.Calendar

/**
 * The Perpetual Habituation & Context Evaluation Engine for the Home Surface.
 * Evaluates hardware peripherals, playback momentum, circadian rhythm, and on-device
 * habituation weights to compose the dynamic living Home screen.
 */
class HomeContextEngine(
    private val habitStore: LocalHabitStore
) {

    fun evaluate(
        isPlaying: Boolean,
        currentTrack: Track?,
        nextTrack: Track?,
        tracks: List<Track>,
        favoritesCount: Int,
        queueCount: Int,
        audioRoute: AudioDeviceRoute = AudioDeviceRoute.Speaker,
        isOffline: Boolean = false,
        isHostConnected: Boolean = false,
        hostName: String = "Studio Workstation",
        dailyCapsule: DailySonicCapsule? = null,
        sessionInsights: SessionSoundInsights? = null,
        calendar: Calendar = Calendar.getInstance()
    ): DynamicHomeSurfaceState {
        val phase = CircadianPhase.current(calendar)

        // 1. Determine Hardware Anchor State
        val isUsbDac = audioRoute == AudioDeviceRoute.Wired && (currentTrack?.lossless == true || (currentTrack?.sampleRate ?: 0) >= 48000)
        val hardwareAnchor = when {
            isUsbDac -> HardwareAnchorState(
                isUsbDac = true,
                title = "Hi-Res Bit-Perfect DAC Active",
                badge = "${currentTrack?.sampleRate ?: 96000}Hz / ${currentTrack?.bitDepth ?: 24}-bit",
                description = "Direct hardware audio driver passthrough enabled."
            )
            isOffline -> HardwareAnchorState(
                isOffline = true,
                title = "Offline Local Media Mode",
                badge = "${tracks.size} tracks",
                description = "Zero network dependencies. Playing directly from device flash."
            )
            else -> null
        }

        // 2. Determine Operational Mode
        val mode = when {
            hardwareAnchor?.isUsbDac == true -> HomeOperationalMode.HardwareAnchored
            isPlaying && currentTrack != null -> HomeOperationalMode.ActiveGroove
            hardwareAnchor?.isOffline == true -> HomeOperationalMode.HardwareAnchored
            else -> HomeOperationalMode.HorizonCue
        }

        // 3. Greeting & Atmospheric Theme
        val (greetingTitle, greetingSubtitle) = when (phase) {
            CircadianPhase.Morning -> "Morning Horizon" to "Clean acoustic starts and crisp morning focus."
            CircadianPhase.Afternoon -> "Daylight Flow" to "High-momentum rhythm for productive sessions."
            CircadianPhase.Evening -> "Sunset Atmosphere" to "Warm vinyl saturation and mellow harmonic drift."
            CircadianPhase.Night -> "Late Night Deep Listen" to "Audiophile hi-res immersion and quiet dynamic range."
        }

        // 4. Hero Capsule & Single-Tap Cue
        val heroTitle = when (mode) {
            HomeOperationalMode.ActiveGroove -> currentTrack?.title ?: "Active Audio Groove"
            HomeOperationalMode.HardwareAnchored -> hardwareAnchor?.title ?: "Hardware Direct Audio"
            HomeOperationalMode.HorizonCue -> dailyCapsule?.primaryMood?.name ?: "${phase.displayName} Flow"
            HomeOperationalMode.DiscoveryCrates -> "Crate Discovery"
        }

        val heroSubtitle = when (mode) {
            HomeOperationalMode.ActiveGroove -> "${currentTrack?.artist} • ${currentTrack?.format ?: "FLAC"}"
            HomeOperationalMode.HardwareAnchored -> hardwareAnchor?.description ?: "Studio Mastered Audio"
            HomeOperationalMode.HorizonCue -> dailyCapsule?.aiNarrative ?: greetingSubtitle
            HomeOperationalMode.DiscoveryCrates -> "Curated by key and sonic texture."
        }

        val heroBadge = when (mode) {
            HomeOperationalMode.ActiveGroove -> if (currentTrack?.lossless == true) "BIT-PERFECT LOSSLESS" else "ACTIVE GROOVE"
            HomeOperationalMode.HardwareAnchored -> hardwareAnchor?.badge ?: "DAC DIRECT"
            HomeOperationalMode.HorizonCue -> "${dailyCapsule?.streakDays ?: 7}D STREAK"
            HomeOperationalMode.DiscoveryCrates -> "CURATED"
        }

        val heroActionText = when (mode) {
            HomeOperationalMode.ActiveGroove -> "Inspect Groove & Decks"
            HomeOperationalMode.HardwareAnchored -> "Tune Mastering & EQ"
            HomeOperationalMode.HorizonCue -> "1-Tap Launch Horizon"
            HomeOperationalMode.DiscoveryCrates -> "Dig Crates"
        }

        // 5. Contextually Ranked Quick Paths
        val quickPaths = computeContextualQuickPaths(
            phase = phase,
            mode = mode,
            tracksCount = tracks.size,
            favoritesCount = favoritesCount,
            queueCount = queueCount,
            currentTrack = currentTrack
        )

        // 6. Harmonic Key Match (if next track is lined up)
        val harmonicMatch = if (currentTrack != null && nextTrack != null) {
            "Harmonic Match • Key Alignment"
        } else null

        return DynamicHomeSurfaceState(
            operationalMode = mode,
            circadianPhase = phase,
            greetingTitle = greetingTitle,
            greetingSubtitle = greetingSubtitle,
            heroTitle = heroTitle,
            heroSubtitle = heroSubtitle,
            heroBadge = heroBadge,
            heroActionText = heroActionText,
            capsule = dailyCapsule,
            sessionInsights = sessionInsights,
            hardwareAnchor = hardwareAnchor,
            activeTrack = currentTrack,
            nextTrack = nextTrack,
            harmonicNextKey = harmonicMatch,
            quickPaths = quickPaths,
            showActiveGrooveConsole = isPlaying && currentTrack != null,
            showHardwareBanner = hardwareAnchor != null
        )
    }

    private fun computeContextualQuickPaths(
        phase: CircadianPhase,
        mode: HomeOperationalMode,
        tracksCount: Int,
        favoritesCount: Int,
        queueCount: Int,
        currentTrack: Track?
    ): List<HabitQuickPath> {
        val candidates = mutableListOf<HabitQuickPath>()

        // 1. Play Something / Smart Flow
        val playWeight = habitStore.getActionWeight(HabitActionType.PlaySomething, phase)
        candidates.add(
            HabitQuickPath(
                actionType = HabitActionType.PlaySomething,
                title = "Play Something",
                subtitle = "$tracksCount ready",
                weight = if (mode == HomeOperationalMode.ActiveGroove) playWeight * 0.4f else playWeight * 1.2f,
                isPrimary = mode == HomeOperationalMode.HorizonCue
            )
        )

        // 2. Favorites
        val favWeight = habitStore.getActionWeight(HabitActionType.OpenFavorites, phase)
        candidates.add(
            HabitQuickPath(
                actionType = HabitActionType.OpenFavorites,
                title = "Favorites",
                subtitle = "$favoritesCount saved",
                weight = favWeight
            )
        )

        // 3. Queue (Higher weight if playing)
        val queueWeight = habitStore.getActionWeight(HabitActionType.OpenQueue, phase)
        val adjustedQueueWeight = if (mode == HomeOperationalMode.ActiveGroove) queueWeight * 2.0f else queueWeight * 0.7f
        candidates.add(
            HabitQuickPath(
                actionType = HabitActionType.OpenQueue,
                title = "Queue Flow",
                subtitle = if (queueCount > 0) "$queueCount lined up" else "Empty queue",
                weight = adjustedQueueWeight,
                isPrimary = mode == HomeOperationalMode.ActiveGroove
            )
        )

        // 4. Smart Flow Shuffle
        val flowWeight = habitStore.getActionWeight(HabitActionType.SmartFlowShuffle, phase)
        candidates.add(
            HabitQuickPath(
                actionType = HabitActionType.SmartFlowShuffle,
                title = "Smart Flow",
                subtitle = "Harmonic pacing",
                weight = flowWeight * 1.1f
            )
        )

        // 5. Hi-Res Audiophile filter
        val hiResWeight = habitStore.getActionWeight(HabitActionType.HiResFilter, phase)
        candidates.add(
            HabitQuickPath(
                actionType = HabitActionType.HiResFilter,
                title = "Hi-Res Lossless",
                subtitle = "Studio master",
                weight = hiResWeight
            )
        )

        // 6. Local Phone Files
        val phoneWeight = habitStore.getActionWeight(HabitActionType.SelectLocalPhoneSource, phase)
        candidates.add(
            HabitQuickPath(
                actionType = HabitActionType.SelectLocalPhoneSource,
                title = "Phone Storage",
                subtitle = "Offline flash",
                weight = phoneWeight
            )
        )

        // 7. Add Music / Host Setup
        val addWeight = habitStore.getActionWeight(HabitActionType.AddMusic, phase)
        candidates.add(
            HabitQuickPath(
                actionType = HabitActionType.AddMusic,
                title = "Add Music",
                subtitle = "Files or host",
                weight = addWeight * 0.8f
            )
        )

        // 8. Turntable / EQ (elevated when active groove)
        if (mode == HomeOperationalMode.ActiveGroove || mode == HomeOperationalMode.HardwareAnchored) {
            val deckWeight = habitStore.getActionWeight(HabitActionType.OpenTurntable, phase) * 1.5f
            candidates.add(
                HabitQuickPath(
                    actionType = HabitActionType.OpenTurntable,
                    title = "Turntable Deck",
                    subtitle = "Vinyl saturation",
                    weight = deckWeight
                )
            )
        }

        return candidates.sortedByDescending { it.weight }.take(6)
    }
}
