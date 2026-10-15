package com.pixelody.app.core.analytics

import com.pixelody.app.data.model.AudioDnaMetrics
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.ListeningMemoryEntry
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.data.model.SonicMood
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.model.WeeklyListeningTrend
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Factual presentation only: no inferred mood, persona, key, tempo or bit-perfect claim. */
object ListeningLedgerProjection {
    fun toInsights(snapshot: ListeningLedgerSnapshot, sessionId: String? = null): SessionSoundInsights {
        val heard = snapshot.recentEvents.filter {
            it.checkpoint.listenedWallMs > 0 && (sessionId == null || it.occurrence.sessionId == sessionId)
        }
        if (heard.isEmpty()) return SessionSoundInsights()
        val formats = heard.map { it.occurrence.track.format.trim().uppercase(Locale.ROOT) }.filter { it.isNotEmpty() }
        return SessionSoundInsights(
            sessionMinutes = minutes(heard.fold(0L) { total, e -> saturatedAdd(total, e.checkpoint.listenedWallMs) }),
            tracksPlayed = heard.size,
            losslessRatio = heard.count { it.occurrence.track.isLossless }.toFloat() / heard.size,
            dominantFormat = dominant(formats.groupingBy { it }.eachCount().mapValues { it.value.toLong() }),
            timeOfDayMood = "", moodBadge = "", streakDays = 0
        )
    }

    fun toDailyCapsule(
        snapshot: ListeningLedgerSnapshot,
        nowEpochMs: Long,
        zoneId: ZoneId,
        tracks: List<Track> = emptyList()
    ): DailySonicCapsule {
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        val heard = snapshot.recentEvents.filter { e ->
            e.checkpoint.listenedWallMs > 0 && e.endedAtEpochMs <= nowEpochMs &&
                Instant.ofEpochMilli(e.endedAtEpochMs).atZone(zoneId).toLocalDate() == today
        }
        val day = dayAt(snapshot, today, nowEpochMs, zoneId)
        val count = day?.heardCount ?: heard.size.toLong()
        val wall = day?.listenedWallMs ?: heard.fold(0L) { total, e -> saturatedAdd(total, e.checkpoint.listenedWallMs) }
        val lossless = day?.losslessCount ?: heard.count { it.occurrence.track.isLossless }.toLong()
        val artistCount = if (count == heard.size.toLong())
            heard.map { it.occurrence.track.artist }.filter { it.isNotBlank() }.distinct().size else 0
        val memories = heard.asReversed().map { e ->
            val t = e.occurrence.track
            ListeningMemoryEntry(
                id = e.occurrence.eventId, trackId = t.trackId, title = t.title, artist = t.artist,
                album = t.album, artworkUrl = t.artworkUrl, timestampMs = e.endedAtEpochMs,
                listenedSeconds = seconds(e.checkpoint.listenedWallMs),
                totalDurationSeconds = seconds(t.durationMs ?: 0),
                completionRatio = (e.checkpoint.coverageRatio ?: 0.0).toFloat(),
                wasSkipped = e.personalSkip, replayCount = 1,
                formatBadge = t.format.uppercase(Locale.ROOT), isLossless = t.isLossless,
                detectedMood = SonicMood.Unknown, timeOfDaySector = ""
            )
        }
        val highlightIds = heard.asReversed().filterNot { it.personalSkip }.map { it.occurrence.track.trackId }.distinct().take(5)
        val byId = tracks.associateBy { it.id }
        return DailySonicCapsule(
            dateString = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
            archetype = SonicArchetype.Unknown, primaryMood = SonicMood.Unknown,
            aiNarrative = if (count > 0) "Recorded $count ${if (count == 1L) "listen" else "listens"}, ${minutes(wall)} min of playback." else "",
            audioDna = AudioDnaMetrics(
                losslessRatio = if (count > 0) lossless.toFloat() / count else 0f,
                totalListeningMinutes = minutes(wall), tracksPlayedCount = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                uniqueArtistsCount = artistCount, bitPerfectPlayCount = 0
            ),
            highlightTrackIds = highlightIds, highlightTracks = highlightIds.mapNotNull { byId[it] },
            memoryTimeline = memories, streakDays = streak(snapshot, today, nowEpochMs, zoneId)
        )
    }

    /** Mon..Sun for the current local calendar week; future days are empty. */
    fun toWeeklyTrend(snapshot: ListeningLedgerSnapshot, nowEpochMs: Long, zoneId: ZoneId): WeeklyListeningTrend {
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        val monday = today.with(DayOfWeek.MONDAY)
        val days = (0L..6L).map { monday.plusDays(it) }.map { date ->
            if (date > today) null else dayAt(snapshot, date, nowEpochMs, zoneId)
        }
        val formats = mutableMapOf<String, Long>()
        days.filterNotNull().forEach { d -> d.formatCounts.forEach { (format, count) ->
            formats[format] = saturatedAdd(formats[format] ?: 0, count)
        } }
        return WeeklyListeningTrend(
            dailyMinutes = days.map { minutes(it?.listenedWallMs ?: 0) },
            activeStreakDays = streak(snapshot, today, nowEpochMs, zoneId),
            totalWeeklyMinutes = minutes(days.fold(0L) { total, day -> saturatedAdd(total, day?.listenedWallMs ?: 0) }),
            topGenre = "", topFormat = dominant(formats)
        )
    }

    private fun dominant(counts: Map<String, Long>): String = counts.entries
        .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key }).firstOrNull()?.key ?: ""
    private fun minutes(ms: Long): Int = (ms / 60_000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    private fun seconds(ms: Long): Int = (ms / 1_000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    private fun streak(snapshot: ListeningLedgerSnapshot, today: LocalDate, nowEpochMs: Long, zoneId: ZoneId): Int {
        var cursor = if ((dayAt(snapshot, today, nowEpochMs, zoneId)?.listenedWallMs ?: 0) > 0) today else today.minusDays(1)
        var count = 0
        while ((dayAt(snapshot, cursor, nowEpochMs, zoneId)?.listenedWallMs ?: 0) > 0 && count < snapshot.days.size) {
            count++; cursor = cursor.minusDays(1)
        }
        return count
    }

    private fun dayAt(snapshot: ListeningLedgerSnapshot, date: LocalDate, nowEpochMs: Long, zoneId: ZoneId): ListeningDayAggregate? {
        val day = snapshot.days[date.toString()] ?: return null
        if (day.lastEndedAtEpochMs <= nowEpochMs) return day
        // Clock-future detail stays on disk. Withhold that aggregate and show only
        // retained, non-future observations rather than claiming those listens occurred.
        val events = snapshot.recentEvents.filter { e -> e.checkpoint.listenedWallMs > 0 && e.endedAtEpochMs <= nowEpochMs &&
            Instant.ofEpochMilli(e.endedAtEpochMs).atZone(zoneId).toLocalDate() == date }
        return ListeningDayAggregate(date.toString(),
            events.fold(0L) { total, e -> saturatedAdd(total, e.checkpoint.listenedWallMs) }, events.size.toLong(),
            events.count { it.occurrence.track.isLossless }.toLong(),
            events.map { it.occurrence.track.format.trim().uppercase(Locale.ROOT) }.filter { it.isNotEmpty() }
                .groupingBy { it }.eachCount().mapValues { it.value.toLong() })
    }
}
