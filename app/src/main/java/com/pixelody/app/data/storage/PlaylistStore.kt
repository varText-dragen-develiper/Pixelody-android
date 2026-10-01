package com.pixelody.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.pixelody.app.data.model.Playlist
import java.util.UUID

/**
 * Codec for user playlists.
 *
 * Hand-rolled escape-based serialization rather than org.json to ensure 100%
 * testability in local JVM unit tests without android.jar stubbing issues, exactly
 * following CrateCodec.
 *
 * Format per playlist:
 * id|escapedName|commaSeparatedTrackIds|escapedArtworkUrl
 * Playlists separated by newline.
 */
object PlaylistCodec {

    private const val PLAYLIST_SEPARATOR = '\n'
    private const val FIELD_SEPARATOR = '|'
    private const val TRACK_SEPARATOR = ','

    fun encode(playlists: List<Playlist>): String {
        return playlists.joinToString(PLAYLIST_SEPARATOR.toString()) { playlist ->
            val id = escape(playlist.id)
            val name = escape(playlist.name)
            val trackIds = playlist.trackIds.joinToString(TRACK_SEPARATOR.toString()) { escape(it) }
            val artworkUrl = escape(playlist.artworkUrl.orEmpty())
            "$id$FIELD_SEPARATOR$name$FIELD_SEPARATOR$trackIds$FIELD_SEPARATOR$artworkUrl"
        }
    }

    fun decode(raw: String?): List<Playlist> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(PLAYLIST_SEPARATOR).mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val parts = trimmed.split(FIELD_SEPARATOR)
            if (parts.size < 3) return@mapNotNull null

            val id = unescape(parts[0])
            val name = unescape(parts[1])
            val rawTrackIds = parts[2]
            val trackIds = if (rawTrackIds.isBlank()) {
                emptyList()
            } else {
                rawTrackIds.split(TRACK_SEPARATOR).map { unescape(it) }
            }
            val artworkUrl = if (parts.size >= 4) {
                val art = unescape(parts[3])
                if (art.isBlank()) null else art
            } else null

            Playlist(
                id = id,
                name = name,
                trackIds = trackIds,
                artworkUrl = artworkUrl
            )
        }
    }

    private fun escape(value: String): String =
        value.replace("%", "%25")
            .replace("|", "%7C")
            .replace("\n", "%0A")
            .replace(",", "%2C")

    private fun unescape(value: String): String =
        value.replace("%2C", ",")
            .replace("%0A", "\n")
            .replace("%7C", "|")
            .replace("%25", "%")
}

/**
 * Dedicated persistent store for user-created playlists.
 *
 * Backed by SharedPreferences("pixelody_user_playlists").
 * Persists independently from device scans and desktop host connections.
 */
class PlaylistStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences("pixelody_user_playlists", Context.MODE_PRIVATE)
    )

    fun load(): List<Playlist> = PlaylistCodec.decode(prefs.getString(KEY_PLAYLISTS, null))

    fun hasPlaylists(): Boolean = load().isNotEmpty()

    fun getPlaylist(playlistId: String): Playlist? = load().firstOrNull { it.id == playlistId }

    fun save(playlists: List<Playlist>) {
        prefs.edit().putString(KEY_PLAYLISTS, PlaylistCodec.encode(playlists)).apply()
    }

    fun createPlaylist(
        name: String,
        trackIds: List<String> = emptyList(),
        artworkUrl: String? = null
    ): Playlist {
        val trimmedName = name.trim().ifBlank { "Untitled Playlist" }
        val id = "user-pl-${UUID.randomUUID().toString().take(8)}"
        val newPlaylist = Playlist(
            id = id,
            name = trimmedName,
            trackIds = trackIds.distinct(),
            artworkUrl = artworkUrl
        )
        val current = load()
        save(current + newPlaylist)
        return newPlaylist
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String): Boolean {
        val current = load()
        val target = current.firstOrNull { it.id == playlistId } ?: return false
        if (target.trackIds.contains(trackId)) return true // already present
        val updated = target.copy(trackIds = target.trackIds + trackId)
        save(current.map { if (it.id == playlistId) updated else it })
        return true
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String): Boolean {
        val current = load()
        val target = current.firstOrNull { it.id == playlistId } ?: return false
        val updated = target.copy(trackIds = target.trackIds.filter { it != trackId })
        save(current.map { if (it.id == playlistId) updated else it })
        return true
    }

    fun deletePlaylist(playlistId: String): Boolean {
        val current = load()
        val filtered = current.filterNot { it.id == playlistId }
        if (filtered.size == current.size) return false
        save(filtered)
        return true
    }

    fun renamePlaylist(playlistId: String, newName: String): Boolean {
        val trimmed = newName.trim().ifBlank { return false }
        val current = load()
        val target = current.firstOrNull { it.id == playlistId } ?: return false
        val updated = target.copy(name = trimmed)
        save(current.map { if (it.id == playlistId) updated else it })
        return true
    }

    fun clear() {
        prefs.edit().remove(KEY_PLAYLISTS).apply()
    }

    companion object {
        const val KEY_PLAYLISTS = "user_playlists_data"
    }
}
