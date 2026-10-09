package com.pixelody.app.core.analytics

import com.pixelody.app.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class SessionSoundInsights(
    val sessionMinutes: Int = 0,
    val tracksPlayed: Int = 0,
    val losslessRatio: Float = 0.0f,
    val dominantFormat: String = "",
    val timeOfDayMood: String = "",
    val moodBadge: String = "",
    val streakDays: Int = 0
) {
    val hasData: Boolean get() = tracksPlayed > 0
}

class SessionCapsuleEngine {
    private val _insights = MutableStateFlow(SessionSoundInsights())
    val insights: StateFlow<SessionSoundInsights> = _insights.asStateFlow()

    private val playedTracks = mutableListOf<Track>()
    private var sessionStartTimeMs: Long = System.currentTimeMillis()

    fun resetSession() {
        playedTracks.clear()
        sessionStartTimeMs = System.currentTimeMillis()
        _insights.value = SessionSoundInsights()
    }

    fun recordTrackPlay(track: Track) {
        playedTracks.add(track)
        recompute()
    }

    fun updateSessionTime(elapsedMinutes: Int) {
        _insights.value = _insights.value.copy(
            sessionMinutes = maxOf(_insights.value.sessionMinutes, elapsedMinutes)
        )
    }

    private fun recompute() {
        val totalTracks = playedTracks.size
        val losslessCount = playedTracks.count { it.lossless || it.format.equals("FLAC", true) }
        val ratio = if (totalTracks > 0) losslessCount.toFloat() / totalTracks.toFloat() else 0f

        val formatCounts = playedTracks.groupingBy { it.format.uppercase() }.eachCount()
        val topFormat = formatCounts.maxByOrNull { it.value }?.key ?: ""

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (mood, badge) = when (hour) {
            in 5..11 -> "Morning Warmth" to "Acoustic Rise"
            in 12..17 -> "Midday Focus" to "Dynamic Flow"
            in 18..22 -> "Evening Chill" to "Smooth Harmonics"
            else -> "Late Night Drift" to "Deep Ambient"
        }

        val elapsedMins = ((System.currentTimeMillis() - sessionStartTimeMs) / 60000L).toInt().coerceAtLeast(0)

        _insights.value = SessionSoundInsights(
            sessionMinutes = elapsedMins,
            tracksPlayed = totalTracks,
            losslessRatio = ratio,
            dominantFormat = topFormat,
            timeOfDayMood = mood,
            moodBadge = badge,
            streakDays = 0
        )
    }

    companion object {
        fun generateDefaultInsights(): SessionSoundInsights = SessionSoundInsights()
    }
}
