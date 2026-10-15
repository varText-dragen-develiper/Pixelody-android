package com.pixelody.app.data.storage

import com.pixelody.app.data.model.Track
import org.json.JSONObject

/** Optional tags only. Missing tags remain unavailable; IDs are never derived from paths. */
object TrackMetadataJson {
    fun append(json: JSONObject, track: Track): JSONObject = json
        .put("albumArtist", track.albumArtist ?: JSONObject.NULL)
        .put("discNumber", track.discNumber ?: JSONObject.NULL)
        .put("trackNumber", track.trackNumber ?: JSONObject.NULL)
        .put("albumId", track.albumId ?: JSONObject.NULL)
        .put("recordingId", track.recordingId ?: JSONObject.NULL)
        .put("musicalKey", track.musicalKey ?: JSONObject.NULL)
        .put("bpm", track.bpm?.takeIf { it.isFinite() } ?: JSONObject.NULL)
        .put("genre", track.genre)

    fun apply(json: JSONObject, track: Track): Track = track.copy(
        albumArtist = text(json, "albumArtist"), discNumber = positiveInt(json, "discNumber"),
        trackNumber = positiveInt(json, "trackNumber"), albumId = text(json, "albumId"),
        recordingId = text(json, "recordingId"), musicalKey = text(json, "musicalKey"),
        // A stored zero is not confused with an absent value. The telemetry reader rejects it as unusable.
        bpm = if (json.has("bpm") && !json.isNull("bpm")) json.optDouble("bpm").toFloat().takeIf { it.isFinite() } else null,
        genre = text(json, "genre") ?: track.genre
    )

    private fun text(json: JSONObject, key: String): String? =
        if (!json.has(key) || json.isNull(key)) null else json.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }

    private fun positiveInt(json: JSONObject, key: String): Int? =
        if (!json.has(key) || json.isNull(key)) null else json.optInt(key).takeIf { it > 0 }
}
