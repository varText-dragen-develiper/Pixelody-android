package com.pixelody.app.core.playback

import org.json.JSONObject

/** One list in an Add to session shuffle. */
data class SessionList(val sourceKey: String, val name: String)

/**
 * Words for the queue screen's two-queue rows, matching Windows: why the song
 * playing now was picked, and what each linked list is called.
 */
object ShuffleLanes {
    /** Queue copy stays readable even when newer receipts contain unknown policy keys. */
    fun queueCue(receipt: String?): TransitionCue? {
        val decision = runCatching { JSONObject(receipt ?: return null) }.getOrNull() ?: return null
        val title = when (decision.optString("kind")) {
            "manual" -> "Your queue choice"
            "play-now" -> "Play now"
            "previous" -> "From your history"
            else -> "Upcoming track"
        }
        val details = mutableListOf<String>()
        if (decision.optBoolean("intentionalDuplicate")) details += "You added this track again."
        if ((decision.optJSONArray("relaxations")?.length() ?: 0) > 0) {
            details += "Repeat spacing was reduced to keep the queue moving."
        }
        return TransitionCue(title, details.joinToString(" "))
    }

    /** [receipt] is the current song's decision; [from] the song before it. */
    fun whyLine(receipt: String?, from: String?): String? {
        val decision = runCatching { JSONObject(receipt ?: return null) }.getOrNull() ?: return null
        val kind = decision.optString("kind")
        if (kind == "previous" || kind == "play-now") return null
        if (kind == "manual") return "You queued this one next."
        val last = from?.takeIf { it.isNotBlank() } ?: "the last song"
        val skip = if (decision.isNull("skip")) null else decision.optString("skip")
        if (decision.optString("lane") == "live") return when (skip) {
            "flick" -> "You skipped $last straight away, so this is a wider pick."
            "late" -> "You skipped near the end of $last, so this stays close to it."
            "early", "mid" -> "You skipped $last, so this leans away from it."
            else -> "Picked for after a skip."
        }
        return if (skip == "finish") "$last played through, so this keeps going the same way." else null
    }

    /**
     * A readable name for a source key from [ShuffleSourceKeys]. Album keys are
     * lowercased, so the album's own title is passed in when it's known.
     */
    fun listName(sourceKey: String, playlists: List<Pair<String, String>>, albumTitle: String? = null): String = when {
        sourceKey == "local-library" -> "Library"
        sourceKey.startsWith("playlist:") -> playlists.firstOrNull { "playlist:${it.first}" == sourceKey }?.second ?: "Playlist"
        sourceKey.startsWith("album:") -> albumTitle?.takeIf { it.isNotBlank() } ?: sourceKey.split(':', limit = 4).getOrNull(3)?.takeIf { it.isNotBlank() } ?: "Album"
        sourceKey.startsWith("artist:") -> sourceKey.removePrefix("artist:").ifBlank { "Artist" }
        else -> "This queue"
    }
}
