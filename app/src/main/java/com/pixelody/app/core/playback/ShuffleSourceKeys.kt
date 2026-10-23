package com.pixelody.app.core.playback

import com.pixelody.app.data.model.Track

/**
 * Names the list a Play or Shuffle started from, so shuffle stays inside that
 * playlist, album or artist and each keeps its own place between sessions.
 * Anything else (a search result, a hand-picked selection) gets no key and a
 * one-off scope.
 */
object ShuffleSourceKeys {
    fun keyFor(tracks: List<Track>, library: List<Track>, playlists: List<Pair<String, List<String>>>): String? {
        if (tracks.isEmpty()) return null
        val ids = tracks.map { it.id }.sorted()
        if (ids == library.map { it.id }.sorted()) return "local-library"
        playlists.firstOrNull { it.second.sorted() == ids }?.let { return "playlist:${it.first}" }
        val album = tracks.map(AlbumSequenceMetadata::identity).distinct().singleOrNull()
        if (album != null && album.kind != "track" && library.filter { AlbumSequenceMetadata.identity(it) == album }.map { it.id }.sorted() == ids)
            return "album:${album.kind}:${album.artist}:${album.album}"
        val artist = tracks.map { it.artist.trim() }.distinct().singleOrNull()?.takeIf { it.isNotEmpty() }
        if (artist != null && library.filter { it.artist.trim() == artist }.map { it.id }.sorted() == ids) return "artist:$artist"
        return null
    }
}
