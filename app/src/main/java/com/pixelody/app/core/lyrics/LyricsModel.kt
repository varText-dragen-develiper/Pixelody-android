package com.pixelody.app.core.lyrics

enum class LyricsSource(val label: String) {
    SidecarLrc("Sidecar .lrc file"),
    EmbeddedTag("Embedded Audio Tag"),
    LrcLib("LRCLIB (Community Synced)"),
    AutoAligned("Auto-Aligned (Acoustic & BPM)"),
    Generated("Synthesized / Demo"),
    None("None")
}

data class LyricLine(
    val timestampMs: Long = -1L,
    val text: String = "",
    val isInstrumental: Boolean = false
) {
    val isSynced: Boolean get() = timestampMs >= 0L

    val formattedTimestamp: String
        get() {
            if (timestampMs < 0L) return ""
            val totalSeconds = timestampMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = (timestampMs % 1000) / 10
            return String.format(java.util.Locale.US, "%02d:%02d.%02d", minutes, seconds, millis)
        }
}

data class LyricsDocument(
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val offsetMs: Long = 0L,
    val source: LyricsSource = LyricsSource.None
) {
    val hasLyrics: Boolean get() = lines.isNotEmpty()

    /**
     * Binary search for the index of the lyric line active at [positionMs].
     * Returns -1 if no lyrics are present, or if position is before the first timestamped line.
     */
    fun activeLineIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        val adjustedPosition = positionMs + offsetMs

        var low = 0
        var high = lines.size - 1
        var result = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val line = lines[mid]
            if (line.timestampMs <= adjustedPosition) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    fun nextLineIndex(positionMs: Long): Int {
        val current = activeLineIndex(positionMs)
        return if (current in 0 until (lines.size - 1)) current + 1 else -1
    }

    companion object {
        val EMPTY = LyricsDocument()
    }
}
