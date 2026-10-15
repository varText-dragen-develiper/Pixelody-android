package com.pixelody.app.data

import com.pixelody.app.data.model.CRATE_SLOT_COUNT
import com.pixelody.app.data.model.Crate
import com.pixelody.app.data.model.CrateCodec
import com.pixelody.app.data.model.CrateSlot
import com.pixelody.app.data.model.addingToFirstEmpty
import com.pixelody.app.data.model.rearranged
import com.pixelody.app.data.model.removingAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PixelodyPersistenceRepositoryTest {

    @Test
    fun testSmartCrateSerializationStructure() {
        val slots: List<CrateSlot?> = List(CRATE_SLOT_COUNT) { index ->
            if (index % 2 == 0) CrateSlot.SingleTrack("track_$index") else null
        }
        val crate = Crate(id = "smart_1", name = "24-Bit FLAC Crate", slots = slots)

        assertEquals(9, crate.slots.size)
        assertEquals(5, crate.occupiedCount)
        assertEquals("track_0", (crate.slotAt(0) as CrateSlot.SingleTrack).trackId)
        assertNull(crate.slotAt(1))
        assertEquals("track_2", (crate.slotAt(2) as CrateSlot.SingleTrack).trackId)
    }

    @Test
    fun testCrateSwapPreservesExactAddresses() {
        val slots = List(CRATE_SLOT_COUNT) { index ->
            if (index == 0) CrateSlot.SingleTrack("track_A")
            else if (index == 8) CrateSlot.SingleTrack("track_B")
            else null
        }
        val crate = Crate(id = "c1", name = "Test Crate", slots = slots)

        val swapped = crate.rearranged(from = 0, to = 8)
        assertEquals("track_B", (swapped.slotAt(0) as CrateSlot.SingleTrack).trackId)
        assertEquals("track_A", (swapped.slotAt(8) as CrateSlot.SingleTrack).trackId)
        assertNull(swapped.slotAt(1))
    }

    @Test
    fun testAddingToFirstEmptySlotFillsHoles() {
        val slots = List(CRATE_SLOT_COUNT) { index ->
            if (index == 0) CrateSlot.SingleTrack("first") else null
        }
        val crate = Crate(id = "c2", name = "Hole Crate", slots = slots)

        val updated = crate.addingToFirstEmpty(CrateSlot.SingleTrack("second"))
        assertNotNull(updated)
        assertEquals("second", (updated?.slotAt(1) as CrateSlot.SingleTrack).trackId)
    }
}
