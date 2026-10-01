package com.pixelody.app

import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HarmonicKeyEngineTest {

    @Test
    fun `parseKey accurately parses Camelot codes`() {
        assertEquals(CamelotKey.K8A, HarmonicKeyEngine.parseKey("8A"))
        assertEquals(CamelotKey.K8B, HarmonicKeyEngine.parseKey("8b"))
        assertEquals(CamelotKey.K1A, HarmonicKeyEngine.parseKey("1A"))
        assertEquals(CamelotKey.K12A, HarmonicKeyEngine.parseKey("12A"))
        assertEquals(CamelotKey.K11B, HarmonicKeyEngine.parseKey("K11B"))
    }

    @Test
    fun `parseKey accurately parses standard musical keys`() {
        // Minor tonalities
        assertEquals(CamelotKey.K8A, HarmonicKeyEngine.parseKey("Am"))
        assertEquals(CamelotKey.K8A, HarmonicKeyEngine.parseKey("A minor"))
        assertEquals(CamelotKey.K5A, HarmonicKeyEngine.parseKey("Cm"))
        assertEquals(CamelotKey.K7A, HarmonicKeyEngine.parseKey("D minor"))
        assertEquals(CamelotKey.K9A, HarmonicKeyEngine.parseKey("Em"))
        assertEquals(CamelotKey.K11A, HarmonicKeyEngine.parseKey("F#m"))

        // Major tonalities
        assertEquals(CamelotKey.K8B, HarmonicKeyEngine.parseKey("C"))
        assertEquals(CamelotKey.K8B, HarmonicKeyEngine.parseKey("C major"))
        assertEquals(CamelotKey.K9B, HarmonicKeyEngine.parseKey("G"))
        assertEquals(CamelotKey.K10B, HarmonicKeyEngine.parseKey("D maj"))
        assertEquals(CamelotKey.K1B, HarmonicKeyEngine.parseKey("B major"))
        assertEquals(CamelotKey.K7B, HarmonicKeyEngine.parseKey("F"))
    }

    @Test
    fun `parseKey returns null for invalid input`() {
        assertNull(HarmonicKeyEngine.parseKey(null))
        assertNull(HarmonicKeyEngine.parseKey(""))
        assertNull(HarmonicKeyEngine.parseKey("   "))
        assertNull(HarmonicKeyEngine.parseKey("XYZ123"))
    }

    @Test
    fun `calculateHarmonicRelation classifies exact match correctly`() {
        assertEquals(HarmonicRelation.ExactMatch, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K8A))
        assertEquals(HarmonicRelation.ExactMatch, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K1B, CamelotKey.K1B))
        assertEquals(1.0f, HarmonicRelation.ExactMatch.score, 0.001f)
    }

    @Test
    fun `calculateHarmonicRelation classifies relative major minor correctly`() {
        // 8A (A minor) <-> 8B (C major)
        assertEquals(HarmonicRelation.RelativeMajorMinor, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K8B))
        assertEquals(HarmonicRelation.RelativeMajorMinor, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K1B, CamelotKey.K1A))
        assertEquals(0.95f, HarmonicRelation.RelativeMajorMinor.score, 0.001f)
    }

    @Test
    fun `calculateHarmonicRelation classifies adjacent steps including wheel wraparound`() {
        // 8A -> 7A (D minor) and 8A -> 9A (E minor)
        assertEquals(HarmonicRelation.AdjacentStep, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K7A))
        assertEquals(HarmonicRelation.AdjacentStep, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K9A))

        // Wheel wraparound: 12A -> 1A and 1A -> 12A
        assertEquals(HarmonicRelation.AdjacentStep, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K12A, CamelotKey.K1A))
        assertEquals(HarmonicRelation.AdjacentStep, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K1A, CamelotKey.K12A))
    }

    @Test
    fun `calculateHarmonicRelation classifies energy boost and drop correctly`() {
        // +2 Energy Boost
        assertEquals(HarmonicRelation.EnergyBoost, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K10A))
        assertEquals(HarmonicRelation.EnergyBoost, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K11A, CamelotKey.K1A))

        // -2 Energy Drop
        assertEquals(HarmonicRelation.EnergyDrop, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K6A))
        assertEquals(HarmonicRelation.EnergyDrop, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K1A, CamelotKey.K11A))
    }

    @Test
    fun `calculateHarmonicRelation classifies diagonal pivot and clash correctly`() {
        // Diagonal: ±1 on wheel with opposite mode (8A -> 7B or 9B)
        assertEquals(HarmonicRelation.DiagonalStep, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K7B))
        assertEquals(HarmonicRelation.DiagonalStep, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K9B))

        // Clash: > 2 steps or incompatible
        assertEquals(HarmonicRelation.DissonantClash, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K3A))
        assertEquals(HarmonicRelation.DissonantClash, HarmonicKeyEngine.calculateHarmonicRelation(CamelotKey.K8A, CamelotKey.K4B))
    }

    @Test
    fun `calculatePitchStretch handles tempo differences and half-time ratios`() {
        val (delta1, stretch1) = HarmonicKeyEngine.calculatePitchStretch(120f, 126f)
        assertEquals(6.0f, delta1, 0.01f)
        assertEquals(5.0f, stretch1, 0.01f)

        // Half-time / Double-time detection (70 BPM vs 140 BPM)
        val (_, stretchHalf) = HarmonicKeyEngine.calculatePitchStretch(140f, 70f)
        assertEquals(0.0f, stretchHalf, 0.01f)
    }

    @Test
    fun `harmonicSortQueue produces smooth harmonic transitions`() {
        val t1 = Track(id = "1", title = "Track 1 [8A]", format = "8A")
        val t2 = Track(id = "2", title = "Track 2 [3A]", format = "3A")
        val t3 = Track(id = "3", title = "Track 3 [8B]", format = "8B")
        val t4 = Track(id = "4", title = "Track 4 [9A]", format = "9A")

        val sorted = HarmonicKeyEngine.harmonicSortQueue(t1, listOf(t2, t3, t4))
        assertEquals(3, sorted.size)
        // First track in queue after 8A should be either 8B (Relative) or 9A (Adjacent), not 3A (Clash)
        val firstKey = HarmonicKeyEngine.parseKey(sorted[0].format)
        assertTrue(firstKey == CamelotKey.K8B || firstKey == CamelotKey.K9A)
    }
}
