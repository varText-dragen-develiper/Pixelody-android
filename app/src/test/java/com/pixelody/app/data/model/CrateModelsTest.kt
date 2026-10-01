package com.pixelody.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The claim a crate makes is that slot 4 is still slot 4 tomorrow. That is not a
 * thing anyone can check by looking at a screen, so it is checked here instead.
 */
class CrateModelsTest {

    private fun slot(n: Int) = CrateSlot.SingleTrack("track-$n")

    private fun crateOf(vararg occupied: Pair<Int, CrateSlot>): Crate {
        val slots = MutableList<CrateSlot?>(CRATE_SLOT_COUNT) { null }
        occupied.forEach { (index, value) -> slots[index] = value }
        return Crate(id = "c", name = "Crate", slots = slots)
    }

    @Test
    fun aCrateIsAlwaysNineAddresses() {
        assertEquals(CRATE_SLOT_COUNT, Crate(id = "c", name = "Crate").slots.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aCrateRefusesToBeAnyOtherSize() {
        Crate(id = "c", name = "Crate", slots = listOf(null, null))
    }

    @Test
    fun removingLeavesAHoleAndMovesNothingElse() {
        val before = crateOf(0 to slot(1), 1 to slot(2), 2 to slot(3), 5 to slot(6))
        val after = before.removingAt(1)

        assertNull(after.slotAt(1))
        assertEquals(slot(1), after.slotAt(0))
        assertEquals(slot(3), after.slotAt(2))
        assertEquals(slot(6), after.slotAt(5))
        assertEquals(3, after.occupiedCount)
    }

    @Test
    fun everyAddressExceptTheRemovedOneIsIdentical() {
        val before = crateOf(0 to slot(1), 3 to slot(4), 8 to slot(9))
        val after = before.removingAt(3)

        (0 until CRATE_SLOT_COUNT).filter { it != 3 }.forEach { index ->
            assertEquals("slot $index", before.slotAt(index), after.slotAt(index))
        }
    }

    @Test
    fun addingFillsTheFirstHoleAndSaysSoBeforehand() {
        val before = crateOf(0 to slot(1), 2 to slot(3))
        assertEquals(1, before.firstEmptySlot)

        val after = before.addingToFirstEmpty(slot(9))
        assertNotNull(after)
        assertEquals(slot(9), after!!.slotAt(1))
        assertEquals(slot(1), after.slotAt(0))
        assertEquals(slot(3), after.slotAt(2))
    }

    @Test
    fun aFullCrateRefusesRatherThanGrowingOrDropping() {
        val slots = List<CrateSlot?>(CRATE_SLOT_COUNT) { slot(it) }
        val full = Crate(id = "c", name = "Crate", slots = slots)

        assertTrue(full.isFull)
        assertEquals(-1, full.firstEmptySlot)
        assertNull(full.addingToFirstEmpty(slot(99)))
    }

    @Test
    fun rearrangingSwapsExactlyTwoAddresses() {
        val before = crateOf(0 to slot(1), 1 to slot(2), 2 to slot(3), 4 to slot(5))
        val after = before.rearranged(0, 4)

        assertEquals(slot(5), after.slotAt(0))
        assertEquals(slot(1), after.slotAt(4))
        (0 until CRATE_SLOT_COUNT).filter { it != 0 && it != 4 }.forEach { index ->
            assertEquals("slot $index", before.slotAt(index), after.slotAt(index))
        }
    }

    @Test
    fun rearrangingOutOfRangeChangesNothing() {
        val before = crateOf(0 to slot(1))
        assertEquals(before, before.rearranged(0, CRATE_SLOT_COUNT))
        assertEquals(before, before.rearranged(-1, 0))
        assertEquals(before, before.rearranged(3, 3))
    }

    @Test
    fun theFaceIsTheFirstFourAddressesIncludingHoles() {
        val crate = crateOf(0 to slot(1), 3 to slot(4), 5 to slot(6))
        val face = crate.face

        assertEquals(CRATE_FACE_COUNT, face.size)
        assertEquals(slot(1), face[0])
        assertNull(face[1])
        assertNull(face[2])
        assertEquals(slot(4), face[3])
    }

    @Test
    fun theBookNamesTheLandingSlotBeforeAnythingIsCommitted() {
        val book = CrateBook(listOf(crateOf(0 to slot(1), 1 to slot(2))))
        assertEquals(2, book.landingSlot("c"))
        assertEquals(-1, book.landingSlot("nope"))
    }

    @Test
    fun namesWithReservedPunctuationSurviveARoundTrip() {
        val awkward = "a;b|c~d%e=f\ng"
        val book = CrateBook(
            listOf(
                Crate(
                    id = awkward,
                    name = awkward,
                    slots = List(CRATE_SLOT_COUNT) { index ->
                        if (index == 0) CrateSlot.Collection("album", awkward) else null
                    }
                )
            )
        )

        val restored = CrateCodec.decodeBook(CrateCodec.encodeBook(book))
        assertEquals(1, restored.crates.size)
        assertEquals(awkward, restored.crates[0].id)
        assertEquals(awkward, restored.crates[0].name)
        assertEquals(CrateSlot.Collection("album", awkward), restored.crates[0].slotAt(0))
    }

    @Test
    fun everySlotKindSurvivesARoundTripAtItsOwnAddress() {
        val slots = listOf<CrateSlot?>(
            CrateSlot.Collection("playlist", "p1"),
            null,
            CrateSlot.SingleTrack("t7"),
            CrateSlot.PlayerView("Turntable"),
            null,
            CrateSlot.Lens("Phone only", "phone", "everything"),
            CrateSlot.SessionPreset("Flow shuffle", mapOf("shuffle" to "flow", "repeat" to "off")),
            null,
            null
        )
        val book = CrateBook(listOf(Crate(id = "c", name = "Crate", slots = slots)))

        val restored = CrateCodec.decodeBook(CrateCodec.encodeBook(book))
        assertEquals(slots, restored.crates[0].slots)
    }

    @Test
    fun aSlotKindThisBuildDoesNotKnowKeepsItsAddress() {
        val written = "v1\nc|Crate|c~playlist~p1;q~something~new;;;;;;"
        val restored = CrateCodec.decodeBook(written)

        val unknown = restored.crates[0].slotAt(1)
        assertTrue(unknown is CrateSlot.Unknown)
        assertEquals(CrateSlot.Collection("playlist", "p1"), restored.crates[0].slotAt(0))

        val rewritten = CrateCodec.encodeBook(restored)
        assertEquals(restored, CrateCodec.decodeBook(rewritten))
        assertTrue(rewritten.contains("q~something~new"))
    }

    @Test
    fun rubbishDecodesToAnEmptyBookInsteadOfThrowing() {
        assertEquals(CrateBook(), CrateCodec.decodeBook(null))
        assertEquals(CrateBook(), CrateCodec.decodeBook(""))
        assertEquals(CrateBook(), CrateCodec.decodeBook("not a crate book"))
        assertEquals(CrateBook(), CrateCodec.decodeBook("v9\nc|Crate|"))
    }

    @Test
    fun aShortOrLongSlotRunStillLandsAtTheRightAddresses() {
        val short = CrateCodec.decodeBook("v1\nc|Crate|c~album~a1;;t~t3")
        assertEquals(CRATE_SLOT_COUNT, short.crates[0].slots.size)
        assertEquals(CrateSlot.Collection("album", "a1"), short.crates[0].slotAt(0))
        assertNull(short.crates[0].slotAt(1))
        assertEquals(CrateSlot.SingleTrack("t3"), short.crates[0].slotAt(2))

        val long = CrateCodec.decodeBook("v1\nc|Crate|" + (1..14).joinToString(";") { "t~t$it" })
        assertEquals(CRATE_SLOT_COUNT, long.crates[0].slots.size)
        assertEquals(CrateSlot.SingleTrack("t1"), long.crates[0].slotAt(0))
        assertEquals(CrateSlot.SingleTrack("t9"), long.crates[0].slotAt(8))
    }

    @Test
    fun bookEditsLeaveOtherCratesAlone() {
        val a = crateOf(0 to slot(1)).copy(id = "a", name = "A")
        val b = crateOf(0 to slot(2)).copy(id = "b", name = "B")
        val book = CrateBook(listOf(a, b))

        assertEquals(listOf(a), book.removing("b").crates)
        assertEquals("Renamed", book.renaming("a", "Renamed").crate("a")?.name)
        assertEquals(b, book.renaming("a", "Renamed").crate("b"))
    }

    @Test
    fun aNewCrateTakesTheLowestUnusedName() {
        assertEquals("c1", nextCrateId(CrateBook()))

        val book = CrateBook(
            listOf(
                crateOf().copy(id = "c1"),
                crateOf().copy(id = "c3")
            )
        )
        assertEquals("c2", nextCrateId(book))

        val filled = CrateBook(book.crates + crateOf().copy(id = "c2"))
        assertEquals("c4", nextCrateId(filled))
    }

    @Test
    fun aNewCrateIdNeverCollidesWithOneSomebodyNamedThemselves() {
        val book = CrateBook(listOf(crateOf().copy(id = "cooking"), crateOf().copy(id = "c1")))
        assertEquals("c2", nextCrateId(book))
    }

    @Test
    fun startersProduceNothingWhenThereIsNothingToStartFrom() {
        assertEquals(CrateBook(), CrateStarters.from(emptyList(), emptyList()))
    }

    @Test
    fun startersPutTheBiggestPlaylistsFirstAndLeaveTheRestOfTheCrateEmpty() {
        val playlists = listOf(
            Playlist(id = "p1", name = "Small", trackIds = listOf("t1"), artworkUrl = null),
            Playlist(id = "p2", name = "Big", trackIds = listOf("t1", "t2", "t3"), artworkUrl = null)
        )

        val book = CrateStarters.from(playlists, listOf("album:Lowlight"))
        val crate = book.crates.single()

        assertEquals(CrateStarters.STARTER_ID, crate.id)
        assertEquals(CrateSlot.Collection("playlist", "p2"), crate.slotAt(0))
        assertEquals(CrateSlot.Collection("playlist", "p1"), crate.slotAt(1))
        assertEquals(CrateSlot.Collection("album", "album:Lowlight"), crate.slotAt(2))
        assertNull(crate.slotAt(3))
        assertFalse(crate.isFull)
    }
}
