package com.pixelody.app.core.playlist

import com.pixelody.app.data.model.Playlist
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.storage.PlaylistStore
import java.util.Locale

data class ParsedM3uTrack(
    val durationSeconds: Int = 0,
    val title: String = "",
    val artist: String = "",
    val location: String = ""
)

data class ParsedM3uPlaylist(
    val name: String,
    val tracks: List<ParsedM3uTrack>
)

/**
 * High-fidelity M3U / M3U8 Export and Import Manager.
 * Supports standard RFC M3U and Extended M3U directives (#EXTM3U, #EXTINF, #EXTPLAYLIST).
 */
object M3uPlaylistManager {

    private const val HEADER = "#EXTM3U"
    private const val DIRECTIVE_PLAYLIST = "#EXTPLAYLIST:"
    private const val DIRECTIVE_INF = "#EXTINF:"

    fun exportToM3u(playlistName: String, tracks: List<Track>): String {
        val sb = StringBuilder()
        sb.append(HEADER).append("\n")
        sb.append(DIRECTIVE_PLAYLIST).append(playlistName.trim()).append("\n")

        for (track in tracks) {
            val duration = track.durationSeconds.coerceAtLeast(-1)
            val artistTitle = if (track.artist.isNotBlank()) {
                "${track.artist} - ${track.title}"
            } else {
                track.title
            }
            sb.append(DIRECTIVE_INF).append(duration).append(",").append(artistTitle).append("\n")
            val location = track.streamUrl.ifBlank { "track://${track.id}" }
            sb.append(location).append("\n")
        }
        return sb.toString()
    }

    fun parseM3u(content: String, defaultName: String = "Imported Playlist"): ParsedM3uPlaylist {
        var playlistName = defaultName
        val tracks = mutableListOf<ParsedM3uTrack>()

        var pendingDuration = 0
        var pendingTitle = ""
        var pendingArtist = ""

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            if (line.startsWith(DIRECTIVE_PLAYLIST, ignoreCase = true)) {
                val extracted = line.substring(DIRECTIVE_PLAYLIST.length).trim()
                if (extracted.isNotBlank()) {
                    playlistName = extracted
                }
            } else if (line.startsWith(DIRECTIVE_INF, ignoreCase = true)) {
                val afterInf = line.substring(DIRECTIVE_INF.length).trim()
                val commaIndex = afterInf.indexOf(',')
                if (commaIndex != -1) {
                    val durationStr = afterInf.substring(0, commaIndex).trim()
                    pendingDuration = durationStr.toIntOrNull() ?: 0
                    val artistAndTitle = afterInf.substring(commaIndex + 1).trim()
                    val dashIndex = artistAndTitle.indexOf(" - ")
                    if (dashIndex != -1) {
                        pendingArtist = artistAndTitle.substring(0, dashIndex).trim()
                        pendingTitle = artistAndTitle.substring(dashIndex + 3).trim()
                    } else {
                        pendingArtist = ""
                        pendingTitle = artistAndTitle
                    }
                } else {
                    pendingDuration = afterInf.toIntOrNull() ?: 0
                    pendingTitle = ""
                    pendingArtist = ""
                }
            } else if (!line.startsWith("#")) {
                // Audio file path or URL
                val location = line
                val title = if (pendingTitle.isNotBlank()) {
                    pendingTitle
                } else {
                    location.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
                }
                tracks.add(
                    ParsedM3uTrack(
                        durationSeconds = pendingDuration,
                        title = title,
                        artist = pendingArtist,
                        location = location
                    )
                )
                pendingDuration = 0
                pendingTitle = ""
                pendingArtist = ""
            }
        }

        return ParsedM3uPlaylist(name = playlistName, tracks = tracks)
    }

    fun matchAndImport(
        parsed: ParsedM3uPlaylist,
        knownTracks: List<Track>,
        playlistStore: PlaylistStore
    ): Playlist {
        val matchedTrackIds = mutableListOf<String>()

        for (m3uTrack in parsed.tracks) {
            val matched = knownTracks.firstOrNull { known ->
                val idMatch = m3uTrack.location.equals(known.id, ignoreCase = true) ||
                    m3uTrack.location.equals("track://${known.id}", ignoreCase = true)
                val urlMatch = known.streamUrl.isNotBlank() && m3uTrack.location.equals(known.streamUrl, ignoreCase = true)
                val fileMatch = m3uTrack.location.isNotBlank() && (
                    known.streamUrl.endsWith(m3uTrack.location, ignoreCase = true) ||
                    m3uTrack.location.endsWith(known.streamUrl, ignoreCase = true)
                )
                val titleArtistMatch = m3uTrack.title.isNotBlank() &&
                    known.title.equals(m3uTrack.title, ignoreCase = true) &&
                    (m3uTrack.artist.isBlank() || known.artist.equals(m3uTrack.artist, ignoreCase = true))

                idMatch || urlMatch || fileMatch || titleArtistMatch
            }
            if (matched != null && matched.id !in matchedTrackIds) {
                matchedTrackIds.add(matched.id)
            }
        }

        return playlistStore.createPlaylist(
            name = parsed.name,
            trackIds = matchedTrackIds
        )
    }
}
