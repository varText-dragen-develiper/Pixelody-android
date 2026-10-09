package com.pixelody.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class TrackMetadataOverride(
    val trackId: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val artworkUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String = JSONObject().apply {
        put("trackId", trackId)
        title?.let { put("title", it) }
        artist?.let { put("artist", it) }
        album?.let { put("album", it) }
        genre?.let { put("genre", it) }
        artworkUrl?.let { put("artworkUrl", it) }
        put("updatedAt", updatedAt)
    }.toString()

    companion object {
        fun fromJson(jsonStr: String): TrackMetadataOverride? = runCatching {
            val json = JSONObject(jsonStr)
            TrackMetadataOverride(
                trackId = json.getString("trackId"),
                title = if (json.has("title")) json.getString("title") else null,
                artist = if (json.has("artist")) json.getString("artist") else null,
                album = if (json.has("album")) json.getString("album") else null,
                genre = if (json.has("genre")) json.getString("genre") else null,
                artworkUrl = if (json.has("artworkUrl")) json.getString("artworkUrl") else null,
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }.getOrNull()
    }
}

/**
 * Persistent store for local user metadata overrides (Title, Artist, Album, Genre, Artwork).
 * Backed by SharedPreferences with a reactive StateFlow so UI updates immediately upon save.
 */
class TrackMetadataStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences("pixelody_track_metadata_overrides", Context.MODE_PRIVATE)
    )

    private val _overridesFlow = MutableStateFlow<Map<String, TrackMetadataOverride>>(loadAll())
    val overridesFlow: StateFlow<Map<String, TrackMetadataOverride>> = _overridesFlow.asStateFlow()

    private fun loadAll(): Map<String, TrackMetadataOverride> {
        val all = prefs.all
        val map = mutableMapOf<String, TrackMetadataOverride>()
        for ((key, value) in all) {
            if (value is String) {
                TrackMetadataOverride.fromJson(value)?.let {
                    map[it.trackId] = it
                }
            }
        }
        return map
    }

    fun clearAllOverrides() {
        check(prefs.edit().clear().commit()) { "Track edits could not be cleared" }
        _overridesFlow.value = emptyMap()
    }

    fun getOverride(trackId: String): TrackMetadataOverride? {
        return _overridesFlow.value[trackId] ?: prefs.getString(trackId, null)?.let { TrackMetadataOverride.fromJson(it) }
    }

    fun saveOverride(override: TrackMetadataOverride) {
        prefs.edit().putString(override.trackId, override.toJson()).apply()
        val current = _overridesFlow.value.toMutableMap()
        current[override.trackId] = override
        _overridesFlow.value = current
    }

    fun saveOverride(
        trackId: String,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?,
        artworkUrl: String? = null
    ) {
        saveOverride(
            TrackMetadataOverride(
                trackId = trackId,
                title = title?.trim()?.takeIf { it.isNotBlank() },
                artist = artist?.trim()?.takeIf { it.isNotBlank() },
                album = album?.trim()?.takeIf { it.isNotBlank() },
                genre = genre?.trim()?.takeIf { it.isNotBlank() },
                artworkUrl = artworkUrl?.trim()?.takeIf { it.isNotBlank() }
            )
        )
    }

    fun deleteOverride(trackId: String) {
        prefs.edit().remove(trackId).apply()
        val current = _overridesFlow.value.toMutableMap()
        current.remove(trackId)
        _overridesFlow.value = current
    }

    fun applyOverride(track: Track): Track {
        val override = getOverride(track.id) ?: return track
        return track.copy(
            title = override.title ?: track.title,
            artist = override.artist ?: track.artist,
            album = override.album ?: track.album,
            genre = override.genre ?: track.genre,
            artworkUrl = override.artworkUrl ?: track.artworkUrl
        )
    }

    fun applyOverrides(tracks: List<Track>): List<Track> {
        val overrides = _overridesFlow.value
        if (overrides.isEmpty()) return tracks
        return tracks.map { track ->
            val override = overrides[track.id]
            if (override != null) {
                track.copy(
                    title = override.title ?: track.title,
                    artist = override.artist ?: track.artist,
                    album = override.album ?: track.album,
                    genre = override.genre ?: track.genre,
                    artworkUrl = override.artworkUrl ?: track.artworkUrl
                )
            } else {
                track
            }
        }
    }
}
