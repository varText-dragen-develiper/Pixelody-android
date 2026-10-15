package com.pixelody.app

import com.pixelody.app.core.playback.*
import com.pixelody.app.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HonestHarmonicMetadataTest {
    private fun track(id: String) = Track(id, "Untitled $id", artist = "Artist $id", format = "FLAC", streamUrl = "content://test/$id")

    @Test fun unknownFeaturesDoNotAcquireHashValuesOrConfidence() {
        (0..100).forEach { index ->
            val telemetry = HarmonicKeyEngine.estimateTrackTelemetry(track("$index"))
            assertNull(telemetry.key); assertNull(telemetry.bpm); assertNull(telemetry.detectedKeyConfidence)
            assertEquals("unknown", telemetry.keyProvenance); assertEquals("unknown", telemetry.bpmProvenance)
        }
    }

    @Test fun taggedFeaturesSurviveAndMalformedFeaturesStayUnknown() {
        val known = HarmonicKeyEngine.estimateTrackTelemetry(track("known").copy(musicalKey = "E♯ minor", bpm = 123.5f))
        assertEquals(CamelotKey.K4A, known.key); assertEquals(123.5f, known.bpm!!, 0f)
        assertEquals("tagged", known.keyProvenance); assertEquals("tagged", known.bpmProvenance)
        assertNull(known.detectedKeyConfidence)
        listOf(null, "H minor", "8C", "13A", "The key of my heart", "CMixolydian").forEach { assertNull(HarmonicKeyEngine.parseKey(it)) }
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach {
            assertNull(HarmonicKeyEngine.estimateTrackTelemetry(track("invalid").copy(bpm = it)).bpm)
        }
    }

    @Test fun enharmonicAndModeNotationParsesDeliberately() {
        mapOf("B# major" to CamelotKey.K8B, "C♭ Major" to CamelotKey.K1B, "Fb" to CamelotKey.K12B,
            "F#m" to CamelotKey.K11A, "G♭ minor" to CamelotKey.K11A, "AM" to CamelotKey.K11B,
            "Am" to CamelotKey.K8A, "Key: k8a" to CamelotKey.K8A, "D MAJOR" to CamelotKey.K10B).forEach { (input, expected) ->
            assertEquals(input, expected, HarmonicKeyEngine.parseKey(input))
        }
    }

    @Test fun explicitLabeledMetadataIsDerivedAndNotDetectedAudio() {
        val telemetry = HarmonicKeyEngine.estimateTrackTelemetry(track("tags").copy(title = "Song Key: F# minor BPM:127.25"))
        assertEquals(CamelotKey.K11A, telemetry.key); assertEquals(127.25f, telemetry.bpm!!, 0f)
        assertEquals("derived", telemetry.keyProvenance); assertNull(telemetry.detectedKeyConfidence)
        assertNull(HarmonicKeyEngine.estimateTrackTelemetry(track("word").copy(title = "Key: Crickets")).key)
    }

    @Test fun unknownToolsHaveNeutralResultsAndCannotInventBridgesOrEnergy() {
        val a = track("a"); val b = track("b"); val c = track("c")
        val analysis = HarmonicKeyEngine.analyzeTransition(HarmonicKeyEngine.estimateTrackTelemetry(a), HarmonicKeyEngine.estimateTrackTelemetry(b))
        assertEquals(HarmonicRelation.Unknown, analysis.relation); assertFalse(analysis.relation.isHarmonic)
        assertEquals(0f, analysis.overallCompatibilityScore, 0f)
        assertNull(analysis.bpmDelta); assertNull(analysis.pitchStretchPercent)
        assertEquals(listOf(b, c, b), HarmonicKeyEngine.harmonicSortQueue(a, listOf(b, c, b)))
        assertNull(HarmonicGpsEngine.findHarmonicJourney(a, b, listOf(c)))
        assertTrue(HarmonicFlowCoordinator.findHarmonicBridgeTracks(a, b, listOf(c)).isEmpty())
        val collection = HarmonicFlowCoordinator.calculateCollectionTelemetry(listOf(a, b))
        assertNull(collection.dominantKey); assertNull(collection.averageBpm); assertNull(collection.harmonicCohesionPercent)
        collection.trajectoryPoints.forEach { assertNull(it.energyLevel); assertNull(it.normalizedWheelPosition) }
    }

    @Test fun unknownTempoLeavesImportedLyricsUnsynced() = runBlocking {
        val result = com.pixelody.app.core.lyrics.AutoLyricsAlignmentEngine.processAndAlign(track("lyrics"), "First line\nSecond line")
        assertFalse(result.isSynced)
        assertEquals(listOf("First line", "Second line"), result.lines.map { it.text })
        assertTrue(result.lines.all { it.timestampMs == -1L })
    }
}
