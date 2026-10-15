package com.pixelody.app.core.lyrics

import android.content.Context
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.core.playback.MusicalPhraseEngine
import com.pixelody.app.data.model.MusicalPhraseStructure
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Optional approximate timing for supplied words using known tagged tempo.
 * This does not detect vocals, decode acoustic peaks, or recover missing lyrics.
 */
object AutoLyricsAlignmentEngine {

    /**
     * Preserve supplied text untimed when tempo or duration is unavailable.
     * Known tags permit heuristic bar timing, labeled AutoAligned.
     */
    suspend fun processAndAlign(
        track: Track,
        rawText: String? = null,
        context: Context? = null
    ): LyricsDocument = withContext(Dispatchers.Default) {
        val lines = parseRawLyricsIntoLines(rawText.orEmpty())
        val untimed = LyricsDocument(lines = lines.map { LyricLine(text = it) }, title = track.title,
            artist = track.artist, album = track.album, source = LyricsSource.None)
        // Tags cannot recover words or vocal timings from music. Keep missing text
        // and unavailable tempo/duration unknown instead of synthesizing telemetry.
        if (lines.isEmpty() || track.durationSeconds <= 0) return@withContext untimed
        val durationMs = track.durationSeconds * 1000L
        val telemetry = HarmonicKeyEngine.estimateTrackTelemetry(track)
        val bpm = telemetry.bpm ?: return@withContext untimed
        val phraseStructure = MusicalPhraseEngine.computePhraseStructure(bpm, durationMs, phraseLengthBars = 16)

        // Empty until a real decoded-audio analysis provider is supplied.
        val peakProfile = extractOrSynthesizePeakProfile(track, durationMs, bpm, context)

        alignLinesToAcoustics(
            track = track,
            lines = lines,
            structure = phraseStructure,
            peakProfile = peakProfile,
            durationMs = durationMs
        )
    }

    /**
     * Compatibility API: no acoustic evidence is available from metadata or encoded bytes.
     */
    @Suppress("UNUSED_PARAMETER")
    fun extractOrSynthesizePeakProfile(
        track: Track,
        durationMs: Long,
        bpm: Float,
        context: Context?
    ): FloatArray = floatArrayOf() // No decoded PCM analysis is available here.
    /**
     * Splits and sanitizes raw lyric text into structured lines.
     */
    fun parseRawLyricsIntoLines(raw: String): List<String> {
        val cleaned = mutableListOf<String>()
        val rawLines = raw.lines()

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            // Strip any leading LRC timestamps if present
            val stripped = trimmed.replace(Regex("""^\[\d{2}:\d{2}(?:\.\d{2,3})?\]"""), "").trim()
            if (stripped.isNotBlank()) {
                cleaned.add(stripped)
            }
        }
        return cleaned
    }

    /**
     * Aligns lyric lines to audio downbeats, phrase boundaries, and peak energy regions.
     */
    fun alignLinesToAcoustics(
        track: Track,
        lines: List<String>,
        structure: MusicalPhraseStructure,
        peakProfile: FloatArray,
        durationMs: Long
    ): LyricsDocument {
        if (lines.isEmpty()) {
            return LyricsDocument(
                lines = emptyList(),
                isSynced = false,
                title = track.title,
                artist = track.artist,
                album = track.album,
                source = LyricsSource.None
            )
        }

        val resultLines = mutableListOf<LyricLine>()

        // 1. Initial Instrumental Intro
        val introEndMs = structure.introEndMs.coerceAtLeast(3000L).coerceAtMost(durationMs / 4)
        resultLines.add(
            LyricLine(
                timestampMs = 0L,
                text = "[Instrumental Intro]",
                isInstrumental = true
            )
        )

        val outroStartMs = structure.outroStartMs.coerceIn(introEndMs + 10000L, durationMs - 5000L)
        val singingWindowMs = (outroStartMs - introEndMs).coerceAtLeast(10000L)

        // Filter and categorize vocal lines vs section headers
        val vocalLines = lines.filter { it.isNotBlank() }
        val lineCount = vocalLines.size.coerceAtLeast(1)

        val barMs = structure.barDurationMs
        val averageLineDurationMs = (singingWindowMs.toDouble() / lineCount.toDouble()).roundToLong().coerceAtLeast(barMs)

        var currentCursorMs = introEndMs

        for (i in 0 until lineCount) {
            val text = vocalLines[i]
            val isHeaderOrMarker = text.startsWith("[") && text.endsWith("]") ||
                    text.startsWith("•") ||
                    text.contains("Chorus", ignoreCase = true) ||
                    text.contains("Verse", ignoreCase = true) ||
                    text.contains("Solo", ignoreCase = true) ||
                    text.contains("Bridge", ignoreCase = true)

            // Quantize candidate timestamp to nearest musical bar downbeat
            val quantizedBarMs = MusicalPhraseEngine.quantizeToNearestBar(currentCursorMs, structure.bpm, structure.beatsPerBar)

            // Correlate with peak energy: look ahead 1 bar to latch onto nearby peak onset
            val peakAdjustedMs = findPeakOnsetInWindow(
                baseTimeMs = quantizedBarMs,
                windowMs = barMs,
                peakProfile = peakProfile,
                durationMs = durationMs
            )

            val safeTimestamp = peakAdjustedMs.coerceIn(introEndMs, outroStartMs - 2000L)

            // Ensure strictly monotonic timestamps
            val lastTs = resultLines.lastOrNull()?.timestampMs ?: 0L
            val finalTimestamp = if (safeTimestamp <= lastTs) lastTs + min(1500L, barMs) else safeTimestamp

            resultLines.add(
                LyricLine(
                    timestampMs = finalTimestamp,
                    text = text,
                    isInstrumental = isHeaderOrMarker && (text.contains("Instrumental", true) || text.contains("Solo", true))
                )
            )

            // Syllable/weight-based progression
            val syllables = countRoughSyllables(text)
            val barSpan = when {
                syllables > 14 -> 3
                syllables > 7 -> 2
                else -> 1
            }
            val lineStepMs = (barSpan * barMs).coerceAtLeast(1500L)
            currentCursorMs = finalTimestamp + lineStepMs

            if (currentCursorMs >= outroStartMs - 3000L && i < lineCount - 1) {
                // Compress pacing smoothly if approaching outro boundary
                currentCursorMs = finalTimestamp + barMs
            }
        }

        // Final Outro line
        val lastTimestamp = resultLines.lastOrNull()?.timestampMs ?: introEndMs
        val safeOutroTs = max(lastTimestamp + barMs, outroStartMs)
        if (safeOutroTs < durationMs) {
            resultLines.add(
                LyricLine(
                    timestampMs = safeOutroTs,
                    text = "[Outro — Fade to silence]",
                    isInstrumental = true
                )
            )
        }

        return LyricsDocument(
            lines = resultLines,
            isSynced = true,
            title = track.title,
            artist = track.artist,
            album = track.album,
            offsetMs = 0L,
            source = LyricsSource.AutoAligned
        )
    }

    /**
     * Inspects the peak envelope window around [baseTimeMs] and returns the peak onset timestamp.
     */
    private fun findPeakOnsetInWindow(
        baseTimeMs: Long,
        windowMs: Long,
        peakProfile: FloatArray,
        durationMs: Long
    ): Long {
        if (peakProfile.isEmpty() || durationMs <= 0L) return baseTimeMs

        val samplePoints = peakProfile.size
        val startIndex = ((baseTimeMs.toDouble() / durationMs.toDouble()) * samplePoints).toInt().coerceIn(0, samplePoints - 1)
        val windowPoints = ((windowMs.toDouble() / durationMs.toDouble()) * samplePoints).toInt().coerceIn(1, 4)

        var maxEnergy = -1.0f
        var peakOffsetPoints = 0

        for (p in 0 until windowPoints) {
            val idx = (startIndex + p).coerceAtMost(samplePoints - 1)
            val energy = peakProfile[idx]
            if (energy > maxEnergy) {
                maxEnergy = energy
                peakOffsetPoints = p
            }
        }

        val pointDurationMs = durationMs / samplePoints
        return baseTimeMs + (peakOffsetPoints * pointDurationMs)
    }

    private fun countRoughSyllables(text: String): Int {
        val vowels = setOf('a', 'e', 'i', 'o', 'u', 'y', 'A', 'E', 'I', 'O', 'U', 'Y')
        var count = 0
        var prevIsVowel = false
        for (char in text) {
            val isVowel = char in vowels
            if (isVowel && !prevIsVowel) count++
            prevIsVowel = isVowel
        }
        return count.coerceAtLeast(1)
    }

    /**
     * Sanitizes metadata strings to remove file extensions, ripper tags, track numbers, and torrent hashes.
     */
    fun sanitizeMetadataString(raw: String): String {
        var s = raw.trim()
        // Strip leading track numbers: "01 - ", "01. ", "01 ", "01-", "040-1738784-"
        s = s.replace(Regex("""^\d{1,4}[-._\s]+"""), "")
        // Strip common audio extensions
        s = s.replace(Regex("""\.(mp3|flac|wav|m4a|aac|ogg|opus|wma)$""", RegexOption.IGNORE_CASE), "")
        // Strip common ripper / web source tags
        s = s.replace(Regex("""\s*[-–—]\s*(Jamendo|Bandcamp|Soundcloud|YouTube).*$""", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("""\s*\(Official.*?\)|\[Official.*?\]|\(Lyrics.*?\)|\[Lyrics.*?\]|\(Audio\)|\[Audio\]|\(Video\)|\[Video\]""", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("""\s*[-–—]\s*\d{5,}\s*[-–—].*$"""), "")
        s = s.replace(Regex("""\s*[-–—]\s*\d{5,}.*$"""), "")
        s = s.replace(Regex("""_+"""), " ")
        return s.trim()
    }

    private fun isInstrumentalTrack(track: Track): Boolean {
        val combined = "${track.title} ${track.album} ${track.genre}".lowercase(java.util.Locale.US)
        val instrumentalKeywords = listOf(
            "lofi", "lo-fi", "chill", "ambient", "instrumental", "soundtrack", "ost",
            "beat", "beats", "synthwave", "retrowave", "classical", "piano",
            "dnb", "drum and bass", "techno", "house", "trance", "electronic",
            "dubstep", "trap beat", "orchestral", "acoustic guitar", "downtempo"
        )
        return instrumentalKeywords.any { combined.contains(it) }
    }

    /** Compatibility API: missing words remain unavailable. */
    @Suppress("UNUSED_PARAMETER")
    fun generateContextualLyricLines(
        track: Track,
        bpm: Float,
        structure: MusicalPhraseStructure
    ): List<String> = emptyList() // Metadata is not evidence of song words or vocal sections.
}
