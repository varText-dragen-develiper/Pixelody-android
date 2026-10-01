package com.pixelody.app.core.genre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreTaxonomyEngineTest {

    @Test
    fun `canonicalize maps aliases correctly`() {
        assertEquals("Lo-Fi Hip Hop", GenreTaxonomyEngine.canonicalize("lofi"))
        assertEquals("Lo-Fi Hip Hop", GenreTaxonomyEngine.canonicalize("lo-fi"))
        assertEquals("Hip-Hop", GenreTaxonomyEngine.canonicalize("hip hop"))
        assertEquals("Hip-Hop", GenreTaxonomyEngine.canonicalize("hiphop"))
        assertEquals("Techno", GenreTaxonomyEngine.canonicalize("techno"))
        assertEquals("Electronic", GenreTaxonomyEngine.canonicalize("edm"))
        assertEquals("Synthwave", GenreTaxonomyEngine.canonicalize("retrowave"))
        assertEquals("R&B", GenreTaxonomyEngine.canonicalize("rnb"))
    }

    @Test
    fun `canonicalize title cases unknown raw genre tags`() {
        assertEquals("Space Disco", GenreTaxonomyEngine.canonicalize("space disco"))
        assertEquals("Atmospheric Chiptune", GenreTaxonomyEngine.canonicalize("atmospheric chiptune"))
        assertEquals("", GenreTaxonomyEngine.canonicalize("   "))
    }

    @Test
    fun `clusterOf resolves correct sonic clusters`() {
        assertEquals(GenreCluster.Electronic, GenreTaxonomyEngine.clusterOf("Synthwave"))
        assertEquals(GenreCluster.Electronic, GenreTaxonomyEngine.clusterOf("ambient"))
        assertEquals(GenreCluster.Rock, GenreTaxonomyEngine.clusterOf("Indie Rock"))
        assertEquals(GenreCluster.Metal, GenreTaxonomyEngine.clusterOf("Industrial Metal"))
        assertEquals(GenreCluster.HipHop, GenreTaxonomyEngine.clusterOf("Lo-Fi Hip Hop"))
        assertEquals(GenreCluster.JazzBlues, GenreTaxonomyEngine.clusterOf("Smooth Jazz"))
        assertEquals(GenreCluster.ClassicalCinematic, GenreTaxonomyEngine.clusterOf("Orchestral"))
        assertEquals(GenreCluster.FolkAcoustic, GenreTaxonomyEngine.clusterOf("Americana"))
    }

    @Test
    fun `genreAffinityScore calculates expected point transitions`() {
        // Exact match -> 6
        assertEquals(6, GenreTaxonomyEngine.genreAffinityScore("Electronic", "electronic"))
        assertEquals(6, GenreTaxonomyEngine.genreAffinityScore("Lo-Fi Hip Hop", "lofi"))

        // Same cluster -> 4
        assertEquals(4, GenreTaxonomyEngine.genreAffinityScore("Synthwave", "Ambient"))
        assertEquals(4, GenreTaxonomyEngine.genreAffinityScore("Hard Rock", "Indie Rock"))

        // Adjacent cluster (Electronic <-> HipHop) -> 1
        assertEquals(1, GenreTaxonomyEngine.genreAffinityScore("Electronic", "Hip-Hop"))

        // Distant / clashing cluster (Metal <-> FolkAcoustic) -> -4
        assertEquals(-4, GenreTaxonomyEngine.genreAffinityScore("Death Metal", "Folk"))

        // Blank or null -> 0
        assertEquals(0, GenreTaxonomyEngine.genreAffinityScore(null, "Electronic"))
        assertEquals(0, GenreTaxonomyEngine.genreAffinityScore("Rock", ""))
    }

    @Test
    fun `suggestGenres returns relevant canonical genres`() {
        val ambientSuggestions = GenreTaxonomyEngine.suggestGenres("amb", limit = 5)
        assertTrue(ambientSuggestions.contains("Ambient"))

        val electroSuggestions = GenreTaxonomyEngine.suggestGenres("electr", limit = 5)
        assertTrue(electroSuggestions.contains("Electronic"))

        val emptyQuerySuggestions = GenreTaxonomyEngine.suggestGenres("", limit = 6)
        assertEquals(6, emptyQuerySuggestions.size)
        assertTrue(emptyQuerySuggestions.contains("Electronic"))
        assertTrue(emptyQuerySuggestions.contains("Ambient"))
    }
}
