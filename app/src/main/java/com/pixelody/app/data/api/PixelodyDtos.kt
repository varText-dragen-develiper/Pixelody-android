package com.pixelody.app.data.api

import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.HostProfile
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.Playlist
import com.pixelody.app.data.model.QueueSnapshot
import com.pixelody.app.data.model.RepeatMode
import com.pixelody.app.data.model.Track

data class HostCapabilityDto(
    val hostId: String,
    val hostName: String,
    val platform: String,
    val roles: List<String>,
    val visibility: String,
    val canStream: Boolean,
    val canRemoteControl: Boolean
)

data class TrackDto(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Double,
    val format: String,
    val codec: String,
    val lossless: Boolean,
    val sampleRate: Int,
    val bitDepth: Int?,
    val bitrate: Int?,
    val channels: Int,
    val replayGainDb: Double?,
    val artworkUrl: String?,
    val streamUrl: String,
    val favorite: Boolean,
    val missing: Boolean,
    val genre: String? = null
)

data class PlaylistDto(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val artworkUrl: String?
)

data class QueueSnapshotDto(
    val currentTrackId: String?,
    val trackIds: List<String>,
    val shuffle: Boolean,
    val repeatMode: String
)

data class LibrarySnapshotDto(
    val host: HostCapabilityDto,
    val tracks: List<TrackDto>,
    val playlists: List<PlaylistDto>,
    val favorites: List<String>,
    val queue: QueueSnapshotDto
)

fun LibrarySnapshotDto.toModel(baseUrl: String): LibrarySnapshot {
    return LibrarySnapshot(
        host = HostProfile(
            hostId = host.hostId,
            hostName = host.hostName,
            baseUrl = baseUrl,
            platform = host.platform,
            roles = host.roles,
            connectionState = HostConnectionState.Connected
        ),
        tracks = tracks.map { track ->
            val artUrl = track.artworkUrl?.takeIf { it.isNotBlank() && it != "null" && !it.endsWith("/null") }
            val stUrl = track.streamUrl.takeIf { it.isNotBlank() && it != "null" && !it.endsWith("/null") }
            Track(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationSeconds = track.duration.toInt(),
                format = track.format,
                codec = track.codec,
                lossless = track.lossless,
                sampleRate = track.sampleRate,
                bitDepth = track.bitDepth,
                bitrate = track.bitrate,
                channels = track.channels,
                replayGainDb = track.replayGainDb,
                artworkUrl = artUrl?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else "${baseUrl.trimEnd('/')}${if (it.startsWith("/")) it else "/$it"}" },
                streamUrl = stUrl?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else "${baseUrl.trimEnd('/')}${if (it.startsWith("/")) it else "/$it"}" }.orEmpty(),
                favorite = track.favorite,
                missing = track.missing,
                genre = track.genre.orEmpty()
            )
        },
        playlists = playlists.map { playlist ->
            val artUrl = playlist.artworkUrl?.takeIf { it.isNotBlank() && it != "null" && !it.endsWith("/null") }
            Playlist(
                id = playlist.id,
                name = playlist.name,
                trackIds = playlist.trackIds,
                artworkUrl = artUrl?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else "${baseUrl.trimEnd('/')}${if (it.startsWith("/")) it else "/$it"}" }
            )
        },
        favorites = favorites,
        queue = QueueSnapshot(
            currentTrackId = queue.currentTrackId,
            trackIds = queue.trackIds,
            shuffle = queue.shuffle,
            repeatMode = when (queue.repeatMode) {
                "one" -> RepeatMode.One
                "all" -> RepeatMode.All
                else -> RepeatMode.Off
            }
        )
    )
}
