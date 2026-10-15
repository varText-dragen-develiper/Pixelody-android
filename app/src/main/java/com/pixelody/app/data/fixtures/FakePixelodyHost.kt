package com.pixelody.app.data.fixtures

import com.pixelody.app.data.api.HostCapabilityDto
import com.pixelody.app.data.api.LibrarySnapshotDto
import com.pixelody.app.data.api.PlaylistDto
import com.pixelody.app.data.api.QueueSnapshotDto
import com.pixelody.app.data.api.TrackDto

class FakePixelodyHost {
    val baseUrl: String = "http://127.0.0.1:47813"

    fun librarySnapshot(): LibrarySnapshotDto {
        val tracks = listOf(
            TrackDto(
                id = "px-track-moonlit-circuit",
                title = "Moonlit Circuit",
                artist = "Local Signal",
                album = "Private Library Tests",
                duration = 264.0,
                format = "flac",
                codec = "FLAC",
                lossless = true,
                sampleRate = 96000,
                bitDepth = 24,
                bitrate = 2840000,
                channels = 2,
                replayGainDb = -3.8,
                artworkUrl = "/api/v1/tracks/px-track-moonlit-circuit/artwork",
                streamUrl = "/api/v1/tracks/px-track-moonlit-circuit/stream",
                favorite = true,
                missing = false,
                genre = "Electronic"
            ),
            TrackDto(
                id = "px-track-window-memory",
                title = "Window Memory",
                artist = "Tape Orchard",
                album = "Private Library Tests",
                duration = 218.0,
                format = "mp3",
                codec = "MPEG Layer III",
                lossless = false,
                sampleRate = 44100,
                bitDepth = null,
                bitrate = 320000,
                channels = 2,
                replayGainDb = -6.1,
                artworkUrl = "/api/v1/tracks/px-track-window-memory/artwork",
                streamUrl = "/api/v1/tracks/px-track-window-memory/stream",
                favorite = false,
                missing = false,
                genre = "Lo-Fi Hip Hop"
            ),
            TrackDto(
                id = "px-track-sleeping-drive",
                title = "Sleeping Drive",
                artist = "Unavailable Source",
                album = "Diagnostics",
                duration = 301.0,
                format = "wav",
                codec = "PCM",
                lossless = true,
                sampleRate = 48000,
                bitDepth = 24,
                bitrate = 2304000,
                channels = 2,
                replayGainDb = null,
                artworkUrl = null,
                streamUrl = "/api/v1/tracks/px-track-sleeping-drive/stream",
                favorite = false,
                missing = true,
                genre = "Ambient"
            )
        )

        return LibrarySnapshotDto(
            host = HostCapabilityDto(
                hostId = "studio-pc",
                hostName = "Pixelody on Studio PC",
                platform = "windows",
                roles = listOf("libraryHost", "jamCoordinator", "playbackDevice"),
                visibility = "local",
                canStream = true,
                canRemoteControl = false
            ),
            tracks = tracks,
            playlists = listOf(
                PlaylistDto(
                    id = "playlist-reference",
                    name = "Reference Checks",
                    trackIds = tracks.map { it.id },
                    artworkUrl = "/api/v1/playlists/playlist-reference/artwork"
                )
            ),
            favorites = tracks.filter { it.favorite }.map { it.id },
            queue = QueueSnapshotDto(
                currentTrackId = tracks.first().id,
                trackIds = tracks.map { it.id },
                shuffle = false,
                repeatMode = "off"
            )
        )
    }
}
