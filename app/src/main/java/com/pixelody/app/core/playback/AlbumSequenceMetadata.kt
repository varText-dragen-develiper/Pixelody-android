package com.pixelody.app.core.playback

import com.pixelody.app.data.model.Track
import java.text.Normalizer
import java.util.Locale

/** Album grouping is independent of performer when album-level identity is supplied. */
object AlbumSequenceMetadata {
    data class Identity(val kind: String, val artist: String, val album: String)
    data class Unit(val identity: Identity, val tracks: List<Track>, val orderSource: String)

    private fun normalized(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)

    fun identity(track: Track): Identity = when {
        !track.albumId.isNullOrBlank() -> Identity("album-id", "", track.albumId)
        track.album.isNotBlank() -> Identity("album", normalized(track.albumArtist?.takeIf { it.isNotBlank() } ?: track.artist), normalized(track.album))
        else -> Identity("track", "", track.id)
    }

    fun units(tracks: List<Track>): List<Unit> = tracks.groupBy(::identity).map { (identity, members) ->
        // An incomplete order cannot be placed canonically without guessing. Retain every source occurrence.
        val canOrder = members.all { it.trackNumber != null && it.trackNumber > 0 } &&
            (members.all { it.discNumber == null } || members.all { it.discNumber != null && it.discNumber > 0 })
        Unit(identity, if (canOrder) members.sortedWith(compareBy<Track> { it.discNumber ?: 1 }.thenBy { it.trackNumber }) else members,
            if (canOrder) "disc-track-tags" else "source-order")
    }

    /** MediaStore packs disc * 1000 + track; retriever commonly supplies track/total. */
    fun parsePosition(raw: String?): Int? = raw?.substringBefore('/')?.trim()?.toIntOrNull()?.takeIf { it > 0 }
}
