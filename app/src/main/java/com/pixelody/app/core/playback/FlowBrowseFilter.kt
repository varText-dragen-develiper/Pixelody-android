package com.pixelody.app.core.playback

import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.Track
import kotlin.math.abs

/** Metadata hints only; missing analysis must never be replaced with a generated key or tempo. */
object FlowBrowseFilter {
    private val camelot = Regex("""(?:\bKey\s*[:=]\s*|\[)(1[0-2]|[1-9])([AB])\b""", RegexOption.IGNORE_CASE)
    private val namedKey = Regex("""\bKey\s*:\s*([A-G](?:[#♯b♭])?\s*(?:minor|major|min|maj|m)?)\b""", RegexOption.IGNORE_CASE)
    private val tempo = Regex("""\bBPM\s*:?\s*(\d{2,3}(?:\.\d+)?)\b|\b(\d{2,3}(?:\.\d+)?)\s*BPM\b""", RegexOption.IGNORE_CASE)

    fun knownKey(track: Track): CamelotKey? {
        val fields = listOf(track.format, track.title, track.album)
        return fields.firstNotNullOfOrNull { raw ->
            HarmonicKeyEngine.parseKey(raw) ?: camelot.find(raw)?.let {
                CamelotKey.fromCode(it.groupValues[1] + it.groupValues[2])
            } ?: namedKey.find(raw)?.let { HarmonicKeyEngine.parseKey(it.groupValues[1]) }
        }
    }

    fun knownBpm(track: Track): Float? = listOf(track.format, track.title, track.album)
        .firstNotNullOfOrNull { raw -> tempo.find(raw)?.let { match ->
            (match.groupValues[1].ifBlank { match.groupValues[2] }).toFloatOrNull()
                ?.takeIf { it in 20f..300f }
        } }

    fun matches(track: Track, keyCodes: Set<String>?, bpm: Float?, tolerance: Float): Boolean =
        (keyCodes == null || knownKey(track)?.code in keyCodes) &&
            (bpm == null || knownBpm(track)?.let { abs(it - bpm) <= tolerance } == true)
}
