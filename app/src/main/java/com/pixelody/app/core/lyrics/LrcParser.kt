package com.pixelody.app.core.lyrics

import java.util.regex.Pattern

/**
 * High-performance, RFC-compliant LRC format parser supporting standard synchronized LRC,
 * multi-timestamp lines, metadata headers ([ti:], [ar:], [al:], [offset:]), and plain text fallbacks.
 */
object LrcParser {

    private val TIME_TAG_REGEX = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val META_TAG_REGEX = Pattern.compile("^\\[([a-zA-Z]+):(.*)]$")

    /**
     * Parses an LRC formatted string into a structured [LyricsDocument].
     */
    fun parse(lrcContent: String, source: LyricsSource = LyricsSource.SidecarLrc): LyricsDocument {
        if (lrcContent.isBlank()) {
            return LyricsDocument.EMPTY
        }

        val lines = lrcContent.lineSequence()
        var title = ""
        var artist = ""
        var album = ""
        var offsetMs = 0L

        val syncedLines = mutableListOf<LyricLine>()
        val plainTextLines = mutableListOf<String>()
        var foundAnyTimeTag = false

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // Check metadata tag first
            val metaMatcher = META_TAG_REGEX.matcher(trimmed)
            if (metaMatcher.matches()) {
                val key = metaMatcher.group(1)?.lowercase() ?: ""
                val value = metaMatcher.group(2)?.trim() ?: ""
                when (key) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                    "offset" -> offsetMs = value.toLongOrNull() ?: 0L
                }
                continue
            }

            // Extract all time tags from line
            val timeMatcher = TIME_TAG_REGEX.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (timeMatcher.find()) {
                foundAnyTimeTag = true
                val minutes = timeMatcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = timeMatcher.group(2)?.toLongOrNull() ?: 0L
                val subSecondStr = timeMatcher.group(3)
                val millis = when {
                    subSecondStr == null -> 0L
                    subSecondStr.length == 1 -> subSecondStr.toLong() * 100L
                    subSecondStr.length == 2 -> subSecondStr.toLong() * 10L
                    subSecondStr.length >= 3 -> subSecondStr.take(3).toLong()
                    else -> 0L
                }
                val totalMs = (minutes * 60L + seconds) * 1000L + millis
                timestamps.add(totalMs)
                lastMatchEnd = timeMatcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = trimmed.substring(lastMatchEnd).trim()
                val isInst = text.equals("instrumental", ignoreCase = true) ||
                             text.equals("[instrumental]", ignoreCase = true) ||
                             text.equals("music", ignoreCase = true) ||
                             text.equals("♪", ignoreCase = true)
                for (ts in timestamps) {
                    syncedLines.add(LyricLine(timestampMs = ts, text = text, isInstrumental = isInst))
                }
            } else {
                plainTextLines.add(trimmed)
            }
        }

        return if (foundAnyTimeTag && syncedLines.isNotEmpty()) {
            syncedLines.sortBy { it.timestampMs }
            LyricsDocument(
                lines = syncedLines,
                isSynced = true,
                title = title,
                artist = artist,
                album = album,
                offsetMs = offsetMs,
                source = source
            )
        } else if (plainTextLines.isNotEmpty()) {
            val staticLines = plainTextLines.map { LyricLine(timestampMs = -1L, text = it) }
            LyricsDocument(
                lines = staticLines,
                isSynced = false,
                title = title,
                artist = artist,
                album = album,
                offsetMs = 0L,
                source = source
            )
        } else {
            LyricsDocument.EMPTY
        }
    }

    /**
     * Serializes a [LyricsDocument] back into standard RFC LRC text format.
     */
    fun serialize(doc: LyricsDocument): String {
        val sb = StringBuilder()
        if (doc.title.isNotBlank()) sb.append("[ti:").append(doc.title).append("]\n")
        if (doc.artist.isNotBlank()) sb.append("[ar:").append(doc.artist).append("]\n")
        if (doc.album.isNotBlank()) sb.append("[al:").append(doc.album).append("]\n")
        if (doc.offsetMs != 0L) sb.append("[offset:").append(doc.offsetMs).append("]\n")

        for (line in doc.lines) {
            if (doc.isSynced && line.timestampMs >= 0L) {
                sb.append("[").append(line.formattedTimestamp).append("]")
            }
            sb.append(line.text).append("\n")
        }
        return sb.toString().trimEnd()
    }
}
