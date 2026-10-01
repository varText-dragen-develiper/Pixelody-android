package com.pixelody.app.data.model

/**
 * A crate is nine addresses. Not a list of nine things — nine addresses, some of
 * which are occupied.
 *
 * That distinction is the whole mechanic. A person opens a crate and hits slot 4
 * without reading slot 4, which only works if slot 4 is still slot 4 tomorrow.
 * Every operation here is written so that no occupied address moves unless the
 * person deliberately moves it: removing leaves a hole, adding fills the first
 * hole, and rearranging swaps exactly two addresses rather than shifting a run.
 *
 * Nothing in this file sorts, compacts, or reorders on its own, and nothing ever
 * should. `CrateModelsTest` asserts that as an invariant rather than trusting it.
 */

const val CRATE_SLOT_COUNT = 9
const val CRATE_FACE_COUNT = 4

sealed class CrateSlot {

    data class Collection(val kindKey: String, val collectionId: String) : CrateSlot()

    data class SingleTrack(val trackId: String) : CrateSlot()

    data class PlayerView(val viewName: String) : CrateSlot()

    data class Lens(val label: String, val sourceKey: String, val filterKey: String) : CrateSlot()

    data class SessionPreset(val label: String, val settings: Map<String, String>) : CrateSlot()

    /**
     * A slot written by a version that knew about a kind this one does not. It keeps
     * its address and re-encodes to exactly what it was, so upgrading and then
     * downgrading does not silently empty someone's crate and let the next thing
     * they add take the vacated position.
     */
    data class Unknown(val raw: String) : CrateSlot()
}

data class Crate(
    val id: String,
    val name: String,
    val slots: List<CrateSlot?> = List(CRATE_SLOT_COUNT) { null }
) {
    init {
        require(slots.size == CRATE_SLOT_COUNT) {
            "A crate is always $CRATE_SLOT_COUNT addresses, occupied or not; got ${slots.size}"
        }
    }

    val occupiedCount: Int get() = slots.count { it != null }

    val isFull: Boolean get() = occupiedCount == CRATE_SLOT_COUNT

    val isEmpty: Boolean get() = occupiedCount == 0

    /** -1 when full. */
    val firstEmptySlot: Int get() = slots.indexOfFirst { it == null }

    /**
     * The closed crate's preview, by position and including holes. Showing the first
     * four *occupied* slots instead would make the face change shape whenever a hole
     * further up was filled, which is the recognition cue moving underneath someone.
     */
    val face: List<CrateSlot?> get() = slots.take(CRATE_FACE_COUNT)

    fun slotAt(index: Int): CrateSlot? = slots.getOrNull(index)

    fun holds(slot: CrateSlot): Boolean = slots.any { it == slot }
}

fun Crate.withSlotAt(index: Int, slot: CrateSlot?): Crate {
    if (index !in 0 until CRATE_SLOT_COUNT) return this
    val next = slots.toMutableList()
    next[index] = slot
    return copy(slots = next)
}

/**
 * Returns null when the crate is full. The caller says so out loud rather than
 * quietly dropping the thing or growing past the ceiling.
 */
fun Crate.addingToFirstEmpty(slot: CrateSlot): Crate? {
    val index = firstEmptySlot
    if (index < 0) return null
    return withSlotAt(index, slot)
}

/** Leaves a hole. Every other address is untouched, which is the point. */
fun Crate.removingAt(index: Int): Crate = withSlotAt(index, null)

fun Crate.removingSlot(slot: CrateSlot): Crate {
    val index = slots.indexOfFirst { it == slot }
    return if (index < 0) this else removingAt(index)
}

/**
 * The only operation that moves an occupied address, and it moves exactly two.
 *
 * Most apps shift a whole run on a drag, which invalidates every position after the
 * insertion point. A swap costs the person the two positions they were looking at
 * and nothing else.
 */
fun Crate.rearranged(from: Int, to: Int): Crate {
    if (from == to) return this
    if (from !in 0 until CRATE_SLOT_COUNT) return this
    if (to !in 0 until CRATE_SLOT_COUNT) return this
    val next = slots.toMutableList()
    val moved = next[from]
    next[from] = next[to]
    next[to] = moved
    return copy(slots = next)
}

data class CrateBook(val crates: List<Crate> = emptyList()) {

    fun crate(id: String): Crate? = crates.firstOrNull { it.id == id }

    fun withCrate(crate: Crate): CrateBook =
        if (crates.any { it.id == crate.id }) replacing(crate) else copy(crates = crates + crate)

    fun replacing(crate: Crate): CrateBook =
        copy(crates = crates.map { if (it.id == crate.id) crate else it })

    fun removing(crateId: String): CrateBook =
        copy(crates = crates.filterNot { it.id == crateId })

    fun renaming(crateId: String, name: String): CrateBook =
        copy(crates = crates.map { if (it.id == crateId) it.copy(name = name) else it })

    /** Where a thing would land, named before the person commits to it. */
    fun landingSlot(crateId: String): Int = crate(crateId)?.firstEmptySlot ?: -1
}

/**
 * Text encoding for the crate book.
 *
 * Hand-rolled rather than JSON because `org.json` is stubbed in local unit tests and
 * a codec that cannot be tested off-device is a codec nobody checks. Five characters
 * are reserved and percent-escaped, so a playlist called `a;b|c~d%e` survives a round
 * trip unchanged — which the test asserts, because album and playlist names in this
 * library really do contain punctuation.
 */
object CrateCodec {

    const val VERSION = "v1"

    private const val BOOK_SEPARATOR = '\n'
    private const val FIELD_SEPARATOR = '|'
    private const val SLOT_SEPARATOR = ';'
    private const val PART_SEPARATOR = '~'
    private const val PAIR_SEPARATOR = '='

    fun encodeBook(book: CrateBook): String =
        (listOf(VERSION) + book.crates.map(::encodeCrate)).joinToString(BOOK_SEPARATOR.toString())

    fun decodeBook(raw: String?): CrateBook {
        if (raw.isNullOrBlank()) return CrateBook()
        val lines = raw.split(BOOK_SEPARATOR)
        if (lines.firstOrNull() != VERSION) return CrateBook()
        val crates = lines.drop(1).mapNotNull(::decodeCrate)
        return CrateBook(crates)
    }

    private fun encodeCrate(crate: Crate): String = listOf(
        escape(crate.id),
        escape(crate.name),
        crate.slots.joinToString(SLOT_SEPARATOR.toString()) { encodeSlot(it) }
    ).joinToString(FIELD_SEPARATOR.toString())

    private fun decodeCrate(line: String): Crate? {
        if (line.isBlank()) return null
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size < 3) return null
        val id = unescape(fields[0])
        if (id.isBlank()) return null
        val slots = fields[2].split(SLOT_SEPARATOR).map(::decodeSlot)
        return Crate(id = id, name = unescape(fields[1]), slots = padded(slots))
    }

    /**
     * Truncation and padding both preserve position, so a book written by a build with
     * a different slot count still puts everything at the address it was written to.
     */
    private fun padded(slots: List<CrateSlot?>): List<CrateSlot?> = when {
        slots.size == CRATE_SLOT_COUNT -> slots
        slots.size > CRATE_SLOT_COUNT -> slots.take(CRATE_SLOT_COUNT)
        else -> slots + List(CRATE_SLOT_COUNT - slots.size) { null }
    }

    private fun encodeSlot(slot: CrateSlot?): String = when (slot) {
        null -> ""
        is CrateSlot.Collection -> parts("c", slot.kindKey, slot.collectionId)
        is CrateSlot.SingleTrack -> parts("t", slot.trackId)
        is CrateSlot.PlayerView -> parts("v", slot.viewName)
        is CrateSlot.Lens -> parts("l", slot.label, slot.sourceKey, slot.filterKey)
        is CrateSlot.SessionPreset -> parts(
            "s",
            slot.label,
            *slot.settings.entries
                .sortedBy { it.key }
                .map { "${escape(it.key)}$PAIR_SEPARATOR${escape(it.value)}" }
                .toTypedArray()
        )
        is CrateSlot.Unknown -> slot.raw
    }

    private fun parts(kind: String, vararg values: String): String =
        (listOf(kind) + values.map(::escape)).joinToString(PART_SEPARATOR.toString())

    private fun decodeSlot(raw: String): CrateSlot? {
        if (raw.isEmpty()) return null
        val parts = raw.split(PART_SEPARATOR)
        val kind = parts.firstOrNull() ?: return null
        val values = parts.drop(1).map(::unescape)
        return when (kind) {
            "c" -> if (values.size >= 2) CrateSlot.Collection(values[0], values[1]) else null
            "t" -> values.getOrNull(0)?.let { CrateSlot.SingleTrack(it) }
            "v" -> values.getOrNull(0)?.let { CrateSlot.PlayerView(it) }
            "l" -> if (values.size >= 3) CrateSlot.Lens(values[0], values[1], values[2]) else null
            "s" -> decodeSessionPreset(values)
            else -> CrateSlot.Unknown(raw)
        }
    }

    private fun decodeSessionPreset(values: List<String>): CrateSlot? {
        val label = values.firstOrNull() ?: return null
        val settings = values.drop(1).mapNotNull { pair ->
            val index = pair.indexOf(PAIR_SEPARATOR)
            if (index <= 0) null else pair.substring(0, index) to pair.substring(index + 1)
        }.toMap()
        return CrateSlot.SessionPreset(label, settings)
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '%' -> append("%25")
                BOOK_SEPARATOR -> append("%0A")
                FIELD_SEPARATOR -> append("%7C")
                SLOT_SEPARATOR -> append("%3B")
                PART_SEPARATOR -> append("%7E")
                PAIR_SEPARATOR -> append("%3D")
                else -> append(character)
            }
        }
    }

    private fun unescape(value: String): String {
        if (!value.contains('%')) return value
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character == '%' && index + 2 < value.length) {
                val code = value.substring(index + 1, index + 3).toIntOrNull(16)
                if (code != null) {
                    out.append(code.toChar())
                    index += 3
                    continue
                }
            }
            out.append(character)
            index++
        }
        return out.toString()
    }
}

/**
 * Lowest unused `c<n>`, rather than a timestamp, so the id a crate gets is a fact
 * about the book rather than about the clock — which makes the whole thing testable
 * and keeps two devices from disagreeing about what "the third crate" is called.
 */
fun nextCrateId(book: CrateBook): String {
    val taken = book.crates.map { it.id }.toSet()
    var index = 1
    while (taken.contains("c$index")) index++
    return "c$index"
}

/**
 * First run puts one crate on Home so the surface is not an empty header with a
 * promise attached. One, not three: a fabricated arrangement pretending to be the
 * person's own is worse than an obviously provisional starting point they rename.
 */
object CrateStarters {

    const val STARTER_ID = "starter"
    const val STARTER_NAME = "Start here"

    /**
     * [albumCollectionIds] are collection **ids**, not album names. The first version of
     * this took names and put them straight into a slot, which meant every starter
     * crate shipped with its album slot resolving to "Gone" — a slot is an address and
     * an address that points nowhere is the one thing a crate may not contain. The
     * device found it; the unit test had enshrined the wrong value and agreed with the
     * bug.
     */
    fun from(playlists: List<Playlist>, albumCollectionIds: List<String>): CrateBook {
        val fromPlaylists = playlists
            .sortedByDescending { it.trackIds.size }
            .take(3)
            .map { CrateSlot.Collection(kindKey = "playlist", collectionId = it.id) }

        val fromAlbums = albumCollectionIds
            .take(CRATE_FACE_COUNT - fromPlaylists.size)
            .map { CrateSlot.Collection(kindKey = "album", collectionId = it) }

        val chosen = (fromPlaylists + fromAlbums).take(CRATE_SLOT_COUNT)
        if (chosen.isEmpty()) return CrateBook()

        val slots = List(CRATE_SLOT_COUNT) { index -> chosen.getOrNull(index) }
        return CrateBook(listOf(Crate(id = STARTER_ID, name = STARTER_NAME, slots = slots)))
    }
}
