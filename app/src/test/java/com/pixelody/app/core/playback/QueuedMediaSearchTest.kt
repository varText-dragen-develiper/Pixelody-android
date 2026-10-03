package com.pixelody.app.core.playback

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class QueuedMediaSearchTest {
    private val queue = listOf(
        QueuedMediaSearchText("Night Train", "Alice", "Blue Hour"),
        QueuedMediaSearchText("Morning Train", "Bob", "New Day"),
        QueuedMediaSearchText("Night Train", "Alice", "Live"),
    )

    @Test
    fun `voice terms match across title artist and album without reordering duplicates`() {
        assertEquals(listOf(0, 2), queuedMediaSearchIndexes(" NIGHT   alice ", queue))
        assertEquals(listOf(0), queuedMediaSearchIndexes("blue hour", queue))
    }

    @Test
    fun `an unknown query produces no replacement music`() {
        assertEquals(emptyList<Int>(), queuedMediaSearchIndexes("missing artist", queue))
    }

    @Test
    fun `a blank voice request retains the queued selection`() {
        assertEquals(listOf(0, 1, 2), queuedMediaSearchIndexes(" \t\n ", queue))
        assertEquals(emptyList<Int>(), queuedMediaSearchIndexes("", emptyList()))
    }

    @Test
    fun `matching does not depend on the device locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(listOf(0, 2), queuedMediaSearchIndexes("ALICE", queue))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
