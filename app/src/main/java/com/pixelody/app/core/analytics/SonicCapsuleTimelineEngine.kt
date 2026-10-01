package com.pixelody.app.core.analytics

import com.pixelody.app.data.model.AudioDnaMetrics
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.ListeningMemoryEntry
import com.pixelody.app.data.model.NarrativeStyle
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.data.model.SonicCapsuleSettings
import com.pixelody.app.data.model.SonicMood
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.model.WeeklyListeningTrend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * SonicCapsuleTimelineEngine: Intelligent on-device AI sonic analysis engine.
 * Records chronological listening history, computes multi-dimensional Audio DNA metrics,
 * synthesizes contextual AI Sonic Liner Notes, and tracks weekly habit trends.
 */
class SonicCapsuleTimelineEngine(
    initialSettings: SonicCapsuleSettings = SonicCapsuleSettings()
) {
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<SonicCapsuleSettings> = _settings.asStateFlow()

    private val _dailyCapsule = MutableStateFlow(generateInitialCapsule())
    val dailyCapsule: StateFlow<DailySonicCapsule> = _dailyCapsule.asStateFlow()

    private val _weeklyTrend = MutableStateFlow(generateInitialWeeklyTrend())
    val weeklyTrend: StateFlow<WeeklyListeningTrend> = _weeklyTrend.asStateFlow()

    private val rawMemories = mutableListOf<ListeningMemoryEntry>()
    private val memoryTracks = mutableMapOf<String, Track>()
    private var totalSecondsListenedToday: Long = 0L

    fun updateSettings(newSettings: SonicCapsuleSettings) {
        _settings.value = newSettings
        recomputeCapsule()
    }

    /**
     * Records a track playback session node.
     */
    fun recordTrackListen(
        track: Track,
        listenedSeconds: Int,
        isCompleted: Boolean = true,
        isBitPerfect: Boolean = false,
        nowMs: Long = System.currentTimeMillis()
    ) {
        memoryTracks[track.id] = track
        val safeDuration = if (track.durationSeconds > 0) track.durationSeconds else maxOf(listenedSeconds, 180)
        val completionRatio = (listenedSeconds.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
        val wasSkipped = !isCompleted && completionRatio < 0.35f

        val hour = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)
        val sector = when (hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..21 -> "Evening"
            else -> "Late Night"
        }

        val detectedMood = resolveMoodForHourAndTrack(hour, track)

        // Check if track was already recently recorded (replay)
        val existingIndex = rawMemories.indexOfFirst { it.trackId == track.id && (nowMs - it.timestampMs) < 600_000L }
        if (existingIndex >= 0) {
            val existing = rawMemories[existingIndex]
            val updated = existing.copy(
                listenedSeconds = existing.listenedSeconds + listenedSeconds,
                completionRatio = maxOf(existing.completionRatio, completionRatio),
                replayCount = existing.replayCount + 1,
                timestampMs = nowMs
            )
            rawMemories[existingIndex] = updated
        } else {
            val entry = ListeningMemoryEntry(
                id = UUID.randomUUID().toString(),
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkUrl = track.artworkUrl,
                timestampMs = nowMs,
                listenedSeconds = listenedSeconds,
                totalDurationSeconds = safeDuration,
                completionRatio = completionRatio,
                wasSkipped = wasSkipped,
                replayCount = 1,
                formatBadge = track.format.ifBlank { if (track.lossless) "FLAC" else "AAC" }.uppercase(Locale.ROOT),
                isLossless = track.lossless || track.format.equals("flac", true) || track.format.equals("wav", true),
                detectedMood = detectedMood,
                timeOfDaySector = sector
            )
            rawMemories.add(0, entry) // Most recent first
        }

        totalSecondsListenedToday += listenedSeconds

        // Trim timeline to max setting
        val maxEntries = _settings.value.maxTimelineEntries
        if (rawMemories.size > maxEntries) {
            while (rawMemories.size > maxEntries) {
                rawMemories.removeAt(rawMemories.lastIndex)
            }
        }

        recomputeCapsule()
    }

    /**
     * Resolves the primary SonicMood based on time of day and track format.
     */
    private fun resolveMoodForHourAndTrack(hour: Int, track: Track): SonicMood {
        return when {
            track.format.equals("dsd", true) || (track.lossless && track.sampleRate >= 96000) -> SonicMood.AudiophileDiscovery
            hour in 5..11 -> SonicMood.MorningAcoustic
            hour in 12..16 -> SonicMood.MiddayFocus
            hour in 17..21 -> SonicMood.SunsetGroove
            else -> SonicMood.LateNightDrift
        }
    }

    /**
     * Recomputes the daily capsule, Audio DNA, and AI liner notes.
     */
    fun recomputeCapsule() {
        if (rawMemories.isEmpty()) {
            _dailyCapsule.value = generateInitialCapsule()
            return
        }

        val totalTracks = rawMemories.size
        val losslessCount = rawMemories.count { it.isLossless }
        val losslessRatio = if (totalTracks > 0) losslessCount.toFloat() / totalTracks.toFloat() else 0f

        val totalMinutes = (totalSecondsListenedToday / 60L).toInt()
        val uniqueArtists = rawMemories.map { it.artist }.filter { it.isNotBlank() }.distinct().size

        val archetype = determineArchetype(losslessRatio, totalTracks)
        val primaryMood = rawMemories.groupingBy { it.detectedMood }.eachCount().maxByOrNull { it.value }?.key
            ?: SonicMood.MiddayFocus

        // Audio DNA
        // avgBpm, dominantKey and energyIndex stay at zero: this app performs no
        // tempo, key or energy analysis, so there is no honest value to put here.
        // The views omit a stat rather than render a zero.
        val audioDna = AudioDnaMetrics(
            losslessRatio = losslessRatio,
            totalListeningMinutes = totalMinutes,
            tracksPlayedCount = totalTracks,
            uniqueArtistsCount = uniqueArtists,
            bitPerfectPlayCount = losslessCount
        )

        // Highlight tracks (ordered by replay count and completion ratio)
        val highlights = rawMemories
            .filterNot { it.wasSkipped }
            .sortedByDescending { it.replayCount * 10 + (it.completionRatio * 10).toInt() }
            .take(5)
            .mapNotNull { memoryTracks[it.trackId] }

        val aiNarrative = generateAiLinerNote(
            metrics = audioDna,
            archetype = archetype,
            mood = primaryMood,
            style = _settings.value.narrativeStyle
        )

        val dateStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())

        _dailyCapsule.value = DailySonicCapsule(
            dateString = dateStr,
            archetype = archetype,
            primaryMood = primaryMood,
            aiNarrative = aiNarrative,
            audioDna = audioDna,
            highlightTrackIds = highlights.map { it.id },
            highlightTracks = highlights,
            memoryTimeline = rawMemories.toList(),
            streakDays = 0
        )

        updateWeeklyTrend(totalMinutes)
    }

    private fun determineArchetype(losslessRatio: Float, totalTracks: Int): SonicArchetype {
        return when {
            losslessRatio >= 0.8f -> SonicArchetype.HiResAudiophile
            totalTracks > 8 -> SonicArchetype.ElectronicExplorer
            else -> SonicArchetype.DeepFlowArchitect
        }
    }

    /**
     * Synthesizes contextual AI Sonic Liner Notes reflecting the active session.
     */
    fun generateAiLinerNote(
        metrics: AudioDnaMetrics,
        archetype: SonicArchetype,
        mood: SonicMood,
        style: NarrativeStyle
    ): String {
        if (!metrics.hasData) return ""

        val fidelityPercent = (metrics.losslessRatio * 100).toInt()
        val totalMins = metrics.totalListeningMinutes
        val tracks = metrics.tracksPlayedCount
        val artists = metrics.uniqueArtistsCount
        val minutesClause = if (totalMins > 0) " across $totalMins min" else ""

        // Every figure below is measured. The BPM and key clauses this used to
        // carry were constants, so a sentence that read as harmonic analysis was
        // asserting work the app never did.
        return when (style) {
            NarrativeStyle.Poetic -> {
                "Today unfolded across $tracks ${if (tracks == 1) "track" else "tracks"}$minutesClause, carrying a $fidelityPercent% lossless aura under the banner of ${archetype.title}."
            }
            NarrativeStyle.Analytical -> {
                "Session log: $tracks ${if (tracks == 1) "track" else "tracks"}$minutesClause, $fidelityPercent% lossless, $artists ${if (artists == 1) "artist" else "artists"}. Dominant mode: ${mood.title}."
            }
            NarrativeStyle.DjLinerNotes -> {
                "Handoff: $tracks ${if (tracks == 1) "cut" else "cuts"}$minutesClause from $artists ${if (artists == 1) "artist" else "artists"}, $fidelityPercent% lossless, riding ${archetype.title} momentum."
            }
        }
    }

    private fun updateWeeklyTrend(todayMinutes: Int) {
        val current = _weeklyTrend.value
        // The other six days were never recorded, so they stay at zero rather
        // than being filled in. One real bar is worth more than seven invented ones.
        val updatedDays = current.dailyMinutes
            .ifEmpty { List(current.dayLabels.size.coerceAtLeast(7)) { 0 } }
            .toMutableList()
        if (updatedDays.isNotEmpty()) {
            updatedDays[updatedDays.lastIndex] = maxOf(updatedDays.last(), todayMinutes)
        }
        val sum = updatedDays.sum()
        _weeklyTrend.value = current.copy(
            dailyMinutes = updatedDays,
            totalWeeklyMinutes = sum
        )
    }

    /**
     * Clears session memories for testing or fresh daily resets.
     */
    fun clearTimeline() {
        rawMemories.clear()
        memoryTracks.clear()
        totalSecondsListenedToday = 0L
        recomputeCapsule()
    }

    /**
     * An empty capsule carrying only the date, which is the one thing that is
     * true before a single track has been played. hasData stays false until a
     * real listen is recorded, and every surface checks it.
     */
    private fun generateInitialCapsule(): DailySonicCapsule = DailySonicCapsule(
        dateString = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    )

    private fun generateInitialWeeklyTrend(): WeeklyListeningTrend = WeeklyListeningTrend()
}
