package com.pixelody.app

import com.pixelody.app.core.playback.FlowBrowseFilter
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.Track
import org.junit.Assert.*
import org.junit.Test

class FlowBrowseFilterTest {
    @Test fun unknownMetadataNeverBecomesAMatch() {
        val track = Track("unknown", "Untitled", format = "FLAC", album = "Unknown", streamUrl = "file:///unknown")
        assertNull(FlowBrowseFilter.knownKey(track))
        assertNull(FlowBrowseFilter.knownBpm(track))
        assertNull(FlowBrowseFilter.knownKey(track.copy(title = "Apartment 2B")))
        assertTrue(FlowBrowseFilter.matches(track, null, null, 10f))
        assertFalse(FlowBrowseFilter.matches(track, setOf("8A"), null, 10f))
        assertFalse(FlowBrowseFilter.matches(track, null, 120f, 10f))
    }

    @Test fun taggedKeysAndDecimalTemposAreReadAcrossMetadataFields() {
        assertEquals(CamelotKey.K8A, FlowBrowseFilter.knownKey(Track("a", "Song · Key: A minor")))
        assertEquals(CamelotKey.K9B, FlowBrowseFilter.knownKey(Track("b", "Song", album = "Set [9B]")))
        assertEquals(128.5f, FlowBrowseFilter.knownBpm(Track("c", "Song · 128.5 BPM"))!!, .01f)
        assertEquals(120f, FlowBrowseFilter.knownBpm(Track("d", "Song", format = "FLAC BPM:120"))!!, .01f)
        assertNull(FlowBrowseFilter.knownBpm(Track("e", "Song BPM:999")))
    }

    @Test fun keyAndTempoIntersectAndToleranceIsInclusive() {
        val match = Track("a", "Song Key:9A BPM:130")
        val wrongKey = match.copy(id = "b", title = "Song Key:2B BPM:120")
        val wrongTempo = match.copy(id = "c", title = "Song Key:9A BPM:131")
        assertTrue(FlowBrowseFilter.matches(match, setOf("8A", "9A"), 120f, 10f))
        assertFalse(FlowBrowseFilter.matches(wrongKey, setOf("8A", "9A"), 120f, 10f))
        assertFalse(FlowBrowseFilter.matches(wrongTempo, setOf("8A", "9A"), 120f, 10f))
        assertTrue(FlowBrowseFilter.matches(wrongKey, null, 120f, 10f))
        assertTrue(FlowBrowseFilter.matches(wrongTempo, setOf("8A", "9A"), null, 10f))
    }
}
