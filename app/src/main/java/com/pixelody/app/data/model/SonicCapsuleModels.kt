package com.pixelody.app.data.model

/**
 * Sonic Mood archetypes representing musical and atmospheric listening states.
 */
enum class SonicMood(
    val title: String,
    val description: String,
    val accentColorHex: Long
) {
    MorningAcoustic("Morning Warmth", "Bright acoustic melodies and uplifting organic instruments.", 0xFFF59E0B), // Warm Amber
    MiddayFocus("Midday Focus", "Rhythmic progressions and steady electronic focus beats.", 0xFF38BDF8), // Sky Blue
    SunsetGroove("Sunset Harmonics", "Smooth funk, deep grooves, and mellow harmonic transitions.", 0xFFEC4899), // Pink
    LateNightDrift("Late Night Drift", "Deep ambient soundscapes, spatial reverb, and sub-bass textures.", 0xFF8B5CF6), // Royal Purple
    HighEnergyWorkout("High-Octane Momentum", "Fast-tempo driving beats and peak dynamic energy.", 0xFFEF4444), // Crimson
    DeepStudyChill("Deep Study Calm", "Lo-fi textures, vinyl warmth, and low-tempo study frequencies.", 0xFF10B981), // Emerald
    AudiophileDiscovery("Audiophile Masterclass", "Pristine Hi-Res FLAC/DSD recordings and wide dynamic range masters.", 0xFFE5A93C) // Gold
}

/**
 * AI Sonic Archetypes classifying overall daily listening persona.
 */
enum class SonicArchetype(
    val title: String,
    val subtitle: String,
    val archetypeCode: String,
    val glyphName: String
) {
    ElectronicExplorer("Electronic Explorer", "Synthesizers, driving 4/4 kicks, and futuristic textures", "EXP", "LightningCheck"),
    VinylPurist("Vinyl Purist", "Analog grooves, organic warmth, and classic mastering", "VINYL", "VinylDisc"),
    DeepFlowArchitect("Deep Flow Architect", "High-retention extended focus and hypnotic ambient chords", "FLOW", "WaveformBars"),
    HiResAudiophile("Hi-Res Audiophile", "Bit-perfect master tapes, wide dynamic range, and DSD purity", "HIRES", "DiamondLossless"),
    EclecticNomad("Eclectic Nomad", "Multi-genre wanderer spanning diverse Camelot keys and tempos", "NOMAD", "OmniSource")
}

/**
 * AI Narrative generation style preference.
 */
enum class NarrativeStyle(val displayName: String) {
    Poetic("Poetic & Atmospheric"),
    Analytical("Studio Master & Analytics"),
    DjLinerNotes("DJ Liner Notes & Flow")
}

/**
 * Individual chronological listening memory entry.
 */
data class ListeningMemoryEntry(
    val id: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val listenedSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val completionRatio: Float = 0f,
    val wasSkipped: Boolean = false,
    val replayCount: Int = 1,
    val formatBadge: String = "",
    val isLossless: Boolean = false,
    val detectedMood: SonicMood = SonicMood.MiddayFocus,
    val timeOfDaySector: String = ""
)

/**
 * Audio DNA metrics quantifying the sonic profile of a listening session or day.
 */
data class AudioDnaMetrics(
    val losslessRatio: Float = 0f,
    val avgBpm: Int = 0,
    val dominantKey: String = "",
    val energyIndex: Float = 0f,
    val totalListeningMinutes: Int = 0,
    val tracksPlayedCount: Int = 0,
    val uniqueArtistsCount: Int = 0,
    val bitPerfectPlayCount: Int = 0
) {
    val hasData: Boolean get() = tracksPlayedCount > 0
}

/**
 * Complete Daily AI Sonic Capsule summarizing a full day's musical journey.
 */
data class DailySonicCapsule(
    val dateString: String = "",
    val archetype: SonicArchetype = SonicArchetype.HiResAudiophile,
    val primaryMood: SonicMood = SonicMood.LateNightDrift,
    val aiNarrative: String = "",
    val audioDna: AudioDnaMetrics = AudioDnaMetrics(),
    val highlightTrackIds: List<String> = emptyList(),
    val highlightTracks: List<Track> = emptyList(),
    val memoryTimeline: List<ListeningMemoryEntry> = emptyList(),
    val streakDays: Int = 0
) {
    /**
     * False until a real play has been recorded. archetype and primaryMood are
     * non-null types carrying an arbitrary first value, so they mean nothing
     * until this is true - every surface that renders them must check it.
     */
    val hasData: Boolean get() = audioDna.hasData || memoryTimeline.isNotEmpty()
}

/**
 * 7-day listening trend and habit aggregation.
 */
data class WeeklyListeningTrend(
    val dailyMinutes: List<Int> = emptyList(), // Mon..Sun, empty until measured
    val dayLabels: List<String> = listOf("M", "T", "W", "T", "F", "S", "S"),
    val activeStreakDays: Int = 0,
    val totalWeeklyMinutes: Int = 0,
    val topGenre: String = "",
    val topFormat: String = ""
) {
    val hasData: Boolean get() = dailyMinutes.any { it > 0 }
}

/**
 * User preferences for Sonic Capsules.
 */
data class SonicCapsuleSettings(
    val isAiSummaryEnabled: Boolean = true,
    val narrativeStyle: NarrativeStyle = NarrativeStyle.DjLinerNotes,
    val autoDailyCapsuleGeneration: Boolean = true,
    val minMinutesForCapsule: Int = 5,
    val maxTimelineEntries: Int = 50
)
