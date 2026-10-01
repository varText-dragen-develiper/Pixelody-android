package com.pixelody.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverModelsTest {

    @Test
    fun aChosenPictureWinsOverTheOriginal() {
        val book = CoverBook().withImage("crate:a", "file:///covers/a.jpg")
        assertEquals("file:///covers/a.jpg", book.imageFor("crate:a", "https://host/art.jpg"))
        assertEquals("https://host/art.jpg", book.imageFor("crate:b", "https://host/art.jpg"))
        assertNull(book.imageFor("crate:b", null))
    }

    @Test
    fun resettingThePictureWithNoNoteRemovesTheEntry() {
        val book = CoverBook().withImage("track:1", "file:///a.jpg").withImage("track:1", null)
        assertFalse(book.hasCustomImage("track:1"))
        assertTrue(book.entries.isEmpty())
    }

    @Test
    fun resettingThePictureKeepsTheNote() {
        val book = CoverBook()
            .withImage("track:1", "file:///a.jpg")
            .withNote("track:1", "  road trip  ")
            .withImage("track:1", null)
        assertEquals("road trip", book.noteFor("track:1"))
        assertEquals(1, book.entries.size)
    }

    @Test
    fun entriesSurviveARoundTripIncludingAwkwardCharacters() {
        val book = CoverBook()
            .withImage("album:a|b%c", "file:///covers/x.jpg")
            .withNote("album:a|b%c", "line one\nline two | 100%")
            .withNote("playlist:p", "just a note")
        assertEquals(book, CoverCodec.decode(CoverCodec.encode(book)))
    }

    @Test
    fun badInputDecodesToAnEmptyBookInsteadOfThrowing() {
        assertTrue(CoverCodec.decode(null).entries.isEmpty())
        assertTrue(CoverCodec.decode("garbage").entries.isEmpty())
        assertTrue(CoverCodec.decode("v1\nnot-enough-fields").entries.isEmpty())
    }

    @Test
    fun collectionKeysKeepTheirPrefixAndPlaylistsGetOne() {
        assertEquals("album:Blue", collectionCoverKey("album:Blue"))
        assertEquals("artist:Ione", collectionCoverKey("artist:Ione"))
        assertEquals("genre:Jazz", collectionCoverKey("genre:Jazz"))
        assertEquals("playlist:p1", collectionCoverKey("p1"))
    }
}
