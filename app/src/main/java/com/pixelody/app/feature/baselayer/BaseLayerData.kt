package com.pixelody.app.feature.baselayer

import com.pixelody.app.core.genre.GenreTaxonomyEngine
import com.pixelody.app.data.model.CoverBook
import com.pixelody.app.data.model.CrateBook
import com.pixelody.app.data.model.Playlist
import com.pixelody.app.data.model.Track

/**
 * Everything the base layer shell draws, as one immutable snapshot.
 *
 * The shell takes content as a parameter rather than reaching for a repository,
 * because the port order in the base layer standard builds the
 * structure first and moves features in afterwards. Until the wiring lands, a
 * fixture fills this and the structure can be walked by thumb on a real device —
 * which is the only place the two open questions about it can be answered.
 *
 * Source is derived from which set a track is in rather than stored on the track,
 * because that is how the real library works: the same file served by a host and
 * held on the phone is one track with two origins.
 */
data class BaseCollection(
    val id: String,
    val kindKey: String,
    val name: String,
    val trackIds: List<String>
)

data class BaseLayerData(
    val tracks: List<Track> = emptyList(),
    val collections: List<BaseCollection> = emptyList(),
    val crates: CrateBook = CrateBook(),
    val covers: CoverBook = CoverBook(),
    val phoneTrackIds: Set<String> = emptySet(),
    val hostTrackIds: Set<String> = emptySet(),
    val jamTrackIds: Set<String> = emptySet(),
    val downloadedTrackIds: Set<String> = emptySet(),
    val currentTrackId: String? = null,
    val lastTrackId: String? = null,
    val isPlaying: Boolean = false,
    val queue: List<String> = emptyList(),
    val hostReachable: Boolean = true,
    val source: BaseSource = BaseSource.All,
    val lens: BaseLens = BaseLens.Everything
) {
    private val byId: Map<String, Track> by lazy { tracks.associateBy { it.id } }

    fun track(id: String): Track? = byId[id]

    fun sourceOf(trackId: String): BaseSource = when {
        jamTrackIds.contains(trackId) -> BaseSource.Jam
        hostTrackIds.contains(trackId) -> BaseSource.Host
        phoneTrackIds.contains(trackId) -> BaseSource.Phone
        else -> BaseSource.All
    }

    /**
     * A host track with the host gone keeps its row and says so, rather than being
     * filtered out. Watching a library silently shrink is worse than seeing what is
     * temporarily out of reach — and it is the same rule as a crate slot holding its
     * address when the thing behind it disappears.
     */
    fun isPlayable(trackId: String): Boolean {
        val track = byId[trackId] ?: return false
        if (track.missing) return false
        return hostReachable || sourceOf(trackId) != BaseSource.Host
    }

    fun matchesLens(trackId: String): Boolean {
        val track = byId[trackId] ?: return false
        val sourceOk = source == BaseSource.All || sourceOf(trackId) == source
        val lensOk = when (lens) {
            BaseLens.Everything -> true
            BaseLens.Favourites -> track.favorite
            BaseLens.Downloaded -> downloadedTrackIds.contains(trackId)
            BaseLens.Lossless -> track.lossless
        }
        return sourceOk && lensOk
    }

    fun visibleTracks(): List<Track> = tracks.filter { matchesLens(it.id) }

    fun visibleTrackIds(ids: List<String>): List<String> = ids.filter { matchesLens(it) }

    fun playableTrackIds(ids: List<String>): List<String> = ids.filter { isPlayable(it) }

    fun collection(id: String, kindKey: String? = null): BaseCollection? {
        if (id.isNotEmpty()) {
            val exact = collections.firstOrNull { it.id.equals(id, ignoreCase = true) }
            if (exact != null) return exact
            val cleanId = id.substringAfter(":")
            val stripped = collections.firstOrNull { it.id.substringAfter(":").equals(cleanId, ignoreCase = true) }
            if (stripped != null) return stripped
        }
        if (!kindKey.isNullOrEmpty()) {
            return collections.firstOrNull { it.kindKey.equals(kindKey, ignoreCase = true) }
        }
        return null
    }

    fun collectionsOfKind(kindKey: String): List<BaseCollection> =
        collections.filter { it.kindKey.equals(kindKey, ignoreCase = true) }

    /** What the identity band's lens control says it is currently showing. */
    val lensLabel: String
        get() = if (source == BaseSource.All) lens.label else "${lens.label} · ${source.label}"
}

enum class BaseSource(val key: String, val label: String) {
    All("all", "All sources"),
    Phone("phone", "Phone"),
    Host("host", "Host"),
    Jam("jam", "J.A.M.")
}

enum class BaseLens(val key: String, val label: String) {
    Everything("everything", "Everything"),
    Favourites("favourites", "Favourites"),
    Downloaded("downloaded", "Downloaded"),
    Lossless("lossless", "Lossless")
}

/** The shape of the library the collection screens browse by. */
enum class BaseBrowseShape(val kindKey: String, val label: String) {
    Tracks("track", "Tracks"),
    Playlists("playlist", "Playlists"),
    Albums("album", "Albums"),
    Artists("artist", "Artists"),
    Genres("genre", "Genres")
}

/**
 * Albums, artists, and genres are not stored anywhere — they are groupings implied by the
 * tracks. Deriving them here means the collection screens do not each invent their
 * own version, which is how Library ended up filtering by a chip while Home played
 * the first track of the same album.
 */
fun buildBaseCollections(tracks: List<Track>, playlists: List<Playlist>): List<BaseCollection> {
    val fromPlaylists = playlists.map { playlist ->
        BaseCollection(
            id = playlist.id,
            kindKey = BaseBrowseShape.Playlists.kindKey,
            name = playlist.name,
            trackIds = playlist.trackIds
        )
    }

    val fromAlbums = tracks
        .filter { it.album.isNotBlank() }
        .groupBy { it.album }
        .map { (album, albumTracks) ->
            BaseCollection(
                id = "album:$album",
                kindKey = BaseBrowseShape.Albums.kindKey,
                name = album,
                trackIds = albumTracks.map { it.id }
            )
        }
        .sortedBy { it.name }

    val fromArtists = tracks
        .filter { it.artist.isNotBlank() }
        .groupBy { it.artist }
        .map { (artist, artistTracks) ->
            BaseCollection(
                id = "artist:$artist",
                kindKey = BaseBrowseShape.Artists.kindKey,
                name = artist,
                trackIds = artistTracks.map { it.id }
            )
        }
        .sortedBy { it.name }

    val fromGenres = tracks
        .filter { it.genre.isNotBlank() }
        .groupBy { GenreTaxonomyEngine.canonicalize(it.genre) }
        .map { (genre, genreTracks) ->
            BaseCollection(
                id = "genre:$genre",
                kindKey = BaseBrowseShape.Genres.kindKey,
                name = genre,
                trackIds = genreTracks.map { it.id }
            )
        }
        .sortedBy { it.name }

    return fromPlaylists + fromAlbums + fromArtists + fromGenres
}
