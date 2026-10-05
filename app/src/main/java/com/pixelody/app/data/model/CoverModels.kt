package com.pixelody.app.data.model

/**
 * A person's own picture and note for something that has an image: a crate, a
 * playlist, an album, a track.
 *
 * The key says what it is attached to (`crate:<id>`, `track:<id>`, `playlist:<id>`,
 * `album:<name>`). Because it is only a key, a missing playlist or a renamed crate
 * costs nothing: the entry just never matches anything and can be left alone.
 *
 * [imageUri] is a `file:` address inside the app's own storage, never the picker's
 * original, so deleting the photo from the gallery does not blank the cover.
 */
data class CoverEntry(
    val key: String,
    val imageUri: String? = null,
    val note: String = ""
) {
    val isEmpty: Boolean get() = imageUri.isNullOrBlank() && note.isBlank()
}

data class CoverBook(val entries: Map<String, CoverEntry> = emptyMap()) {

    fun entry(key: String): CoverEntry? = entries[key]

    /** The person's picture if they chose one, otherwise whatever the item already had. */
    fun imageFor(key: String, fallback: String?): String? =
        entries[key]?.imageUri?.takeIf { it.isNotBlank() } ?: fallback

    fun noteFor(key: String): String = entries[key]?.note.orEmpty()

    fun hasCustomImage(key: String): Boolean = !entries[key]?.imageUri.isNullOrBlank()

    fun withImage(key: String, imageUri: String?): CoverBook =
        updated(key) { it.copy(imageUri = imageUri?.takeIf { uri -> uri.isNotBlank() }) }

    fun withNote(key: String, note: String): CoverBook =
        updated(key) { it.copy(note = note.trim()) }

    private fun updated(key: String, change: (CoverEntry) -> CoverEntry): CoverBook {
        val next = change(entries[key] ?: CoverEntry(key))
        return if (next.isEmpty) CoverBook(entries - key) else CoverBook(entries + (key to next))
    }
}

/**
 * One line per entry: `key|imageUri|note`, with the three reserved characters
 * percent-escaped so a note containing a pipe or a line break survives a round trip.
 * Anything that fails to decode is dropped rather than thrown, for the same reason
 * crates do it: a bad character should not become a crash on launch.
 */
object CoverCodec {

    const val VERSION = "v1"

    fun encode(book: CoverBook): String =
        (listOf(VERSION) + book.entries.values.map { entry ->
            listOf(escape(entry.key), escape(entry.imageUri.orEmpty()), escape(entry.note))
                .joinToString("|")
        }).joinToString("\n")

    fun decode(raw: String?): CoverBook {
        if (raw.isNullOrBlank()) return CoverBook()
        val lines = raw.split("\n")
        if (lines.firstOrNull() != VERSION) return CoverBook()
        val entries = lines.drop(1).mapNotNull { line ->
            val fields = line.split("|")
            if (fields.size < 3) return@mapNotNull null
            val key = unescape(fields[0])
            if (key.isBlank()) return@mapNotNull null
            CoverEntry(
                key = key,
                imageUri = unescape(fields[1]).ifBlank { null },
                note = unescape(fields[2])
            ).takeUnless { it.isEmpty }
        }
        // Earlier collection menus wrote plural prefixes; shelves use canonical singular keys.
        // Prefer an explicit canonical entry if both versions survived in saved data.
        val canonical = entries.filter { it.key == normalizedCollectionCoverKey(it.key) }.associateBy { it.key }
        val migrated = entries.associate { entry ->
            val key = normalizedCollectionCoverKey(entry.key)
            key to entry.copy(key = key)
        }
        return CoverBook(migrated + canonical)
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '%' -> append("%25")
                '\n' -> append("%0A")
                '|' -> append("%7C")
                else -> append(character)
            }
        }
    }

    private fun unescape(value: String): String = value
        .replace("%0A", "\n")
        .replace("%7C", "|")
        .replace("%25", "%")
}

fun crateCoverKey(crateId: String) = "crate:$crateId"
fun trackCoverKey(trackId: String) = "track:$trackId"
fun playlistCoverKey(playlistId: String) = "playlist:$playlistId"
fun albumCoverKey(album: String) = "album:$album"

/** Albums, artists and genres carry their own prefix; anything else is a playlist id. */
fun collectionCoverKey(collectionId: String): String =
    if (collectionId.startsWith("crate:") || collectionId.startsWith("playlist:") || collectionId.startsWith("album:") || collectionId.startsWith("artist:") || collectionId.startsWith("genre:")) {
        collectionId
    } else {
        playlistCoverKey(collectionId)
    }

/** The track as it should be drawn, with the person's own picture in place of the original. */
fun Track.withCover(covers: CoverBook): Track {
    val chosen = covers.imageFor(trackCoverKey(id), artworkUrl)
    return if (chosen == artworkUrl) this else copy(artworkUrl = chosen)
}

/** Independent background choices; cover and note resets cannot affect these keys. */
enum class ScreenBackground(val route: String, val label: String) {
    Home("home", "Home"), Library("library", "Library"), Search("search", "Search"),
    Settings("settings", "Settings");
    val key: String get() = "background:$route"
}

private fun normalizedCollectionCoverKey(key: String): String {
    val prefix = key.substringBefore(':')
    return if (prefix in setOf("playlists", "albums", "artists", "genres")) {
        val id = key.substringAfter(':')
        if (id.startsWith("$prefix:")) key else if (id.startsWith("album:") || id.startsWith("artist:") || id.startsWith("genre:") || id.startsWith("playlist:")) id
        else "${prefix.removeSuffix("s")}:$id"
    } else key
}
