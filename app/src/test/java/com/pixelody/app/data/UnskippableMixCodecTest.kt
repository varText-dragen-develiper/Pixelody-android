package com.pixelody.app.data

import com.pixelody.app.core.unskippable.UnskippableMix
import com.pixelody.app.data.storage.UnskippableMixCodec
import com.pixelody.app.data.storage.UnskippableMixRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnskippableMixCodecTest {
    @Test fun savedMixesRoundTripWithRecipeAndProgress() {
        val records = listOf(
            UnskippableMixRecord(
                id = "unskippable-1", name = "Unskippable · Rock, loud | late", trackIds = listOf("a", "b,c", "d"),
                recipe = UnskippableMix.normalizeRecipe(UnskippableMix.Recipe(style = "rigid", genre = "Rock", targetMinutes = 45, seed = "s", includedUntaggedIds = listOf("z", "y"))),
                neighbourIds = listOf("d"), createdAtMs = 123L, progressTrackId = "b,c", progressPositionMs = 4200L
            ),
            UnskippableMixRecord(
                id = "unskippable-2", name = "Flow", trackIds = listOf("x"),
                recipe = UnskippableMix.normalizeRecipe(UnskippableMix.Recipe(style = "flow", targetMinutes = 60, seed = "t")),
                lastFinishedAtMs = 99L
            )
        )
        assertEquals(records, UnskippableMixCodec.decode(UnskippableMixCodec.encode(records)))
    }

    @Test fun unreadableOrFutureDataLoadsAsNothingRatherThanCrashing() {
        assertTrue(UnskippableMixCodec.decode("not json").isEmpty())
        assertTrue(UnskippableMixCodec.decode("{\"version\":2,\"mixes\":[]}").isEmpty())
        assertTrue(UnskippableMixCodec.decode(null).isEmpty())
    }
}
