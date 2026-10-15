package com.pixelody.app.core.shuffle

import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.Track
import org.json.JSONArray
import org.json.JSONObject

/** Maps local catalog evidence to the exact shared normalizer. Caller owns stable entry identity. */
object UniversalShuffleMetadata {
    /** No occurrence IDs are invented here; repeated track IDs must have distinct caller-supplied entry IDs. */
    fun rawEntry(track: Track, entryId: String): JSONObject {
        val telemetry = HarmonicKeyEngine.estimateTrackTelemetry(track)
        val key = telemetry.key?.let {
            // Camelot wheel numbers are not pitch classes. Map through the known chromatic root.
            val pitchClasses = if (it.mode == CamelotMode.Minor) intArrayOf(8, 3, 10, 5, 0, 7, 2, 9, 4, 11, 6, 1)
                else intArrayOf(11, 6, 1, 8, 3, 10, 5, 0, 7, 2, 9, 4)
            JSONObject().put("pitchClass", pitchClasses[it.number - 1]).put("mode", if (it.mode == CamelotMode.Minor) "minor" else "major")
        }
        val features = JSONObject()
            // Keep the decimal representation used by persisted Float tags; widening binary32 adds spurious digits.
            .put("tempo", evidence(telemetry.bpm?.toString()?.toDouble(), telemetry.bpmProvenance))
            .put("key", if (!track.musicalKey.isNullOrBlank()) evidence(track.musicalKey, "tagged") else evidence(key, telemetry.keyProvenance))
            // No audio-energy feature is supplied by Android. A tempo heuristic is not measured energy.
            .put("energy", evidence(null, "unknown"))
        return JSONObject().put("entryId", entryId).put("trackId", track.id)
            .put("recordingId", optional(track.recordingId)).put("artist", track.artist)
            .put("albumArtist", optional(track.albumArtist)).put("album", track.album).put("albumId", optional(track.albumId))
            .put("discNumber", track.discNumber ?: JSONObject.NULL).put("trackNumber", track.trackNumber ?: JSONObject.NULL)
            .put("available", !track.missing && track.streamUrl.isNotBlank()).put("genre", track.genre)
            .put("contextTags", JSONArray()).put("features", features)
    }

    fun normalizedEntry(track: Track, entryId: String, sourceIndex: Int = 0): JSONObject =
        UniversalShufflePolicy.normalizeEntry(rawEntry(track, entryId), sourceIndex)

    fun normalizedEntries(occurrences: List<Pair<String, Track>>): JSONArray = UniversalShufflePolicy.normalizeEntries(
        JSONArray(occurrences.map { (entryId, track) -> rawEntry(track, entryId) }))

    private fun optional(value: String?): Any = value?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL

    private fun evidence(value: Any?, provenance: String): JSONObject = JSONObject()
        .put("value", value ?: JSONObject.NULL).put("provenance", if (value == null) "unknown" else provenance)
        .put("version", 1)
        // Confidence defaults are assigned only by the versioned reference normalizer. This is not detection confidence.
        .put("confidencePermille", JSONObject.NULL)
}
