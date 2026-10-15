package com.pixelody.app

import com.pixelody.app.core.shuffle.UniversalShuffleMetadata
import com.pixelody.app.core.shuffle.UniversalShufflePolicy
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.storage.TrackMetadataJson
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UniversalShuffleMetadataTest {
    @Test fun trackMapperMatchesExactJavaScriptNormalization() {
        val fixture = JSONObject(javaClass.getResourceAsStream("/universal-shuffle-metadata-vectors.json")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() })
        assertEquals(UniversalShufflePolicy.POLICY_VERSION, fixture.getString("policyVersion"))
        val vectors = fixture.getJSONArray("vectors")
        for (index in 0 until vectors.length()) {
            val vector = vectors.getJSONObject(index)
            val json = vector.getJSONObject("track")
            val track = TrackMetadataJson.apply(json, Track(json.getString("id"), json.getString("title"),
                artist = json.optString("artist", ""), album = json.optString("album", ""), format = json.optString("format", ""),
                streamUrl = "content://fixture"))
            val actual = UniversalShuffleMetadata.normalizedEntry(track, vector.getString("entryId"), vector.getInt("sourceIndex"))
            assertEquals("metadata vector $index", UniversalShufflePolicy.canonicalJson(vector.getJSONObject("expected")), UniversalShufflePolicy.canonicalJson(actual))
        }
    }

    @Test fun explicitDuplicateOccurrencesSurviveAndInputIdentityIsNeverDerivedFromPath() {
        val track = Track("catalog-item", "Song", streamUrl = "content://private/123")
        val entries = UniversalShuffleMetadata.normalizedEntries(listOf("entry-1" to track, "entry-2" to track))
        assertEquals(2, entries.length()); assertEquals("entry-1", entries.getJSONObject(0).getString("entryId"))
        assertEquals("entry-2", entries.getJSONObject(1).getString("entryId"))
        assertEquals("catalog-item", entries.getJSONObject(0).getString("trackId"))
        assertFalse(entries.toString().contains("private"))
        assertTrue(entries.getJSONObject(0).getJSONObject("features").getJSONObject("energy").isNull("value"))
    }

    @Test fun duplicateEntryIdentityIsRejectedRatherThanSilentlyDeduplicated() {
        val track = Track("catalog", "Song", streamUrl = "content://fixture")
        try { UniversalShuffleMetadata.normalizedEntries(listOf("same" to track, "same" to track)); fail("Duplicate entryId must fail") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun missingMediaIsUnavailableButRetainsRecoverableCatalogIdentity() {
        val entry = UniversalShuffleMetadata.normalizedEntry(Track("catalog", "Song", missing = true), "entry")
        assertFalse(entry.getBoolean("available")); assertEquals("catalog", entry.getString("trackId"))
        assertTrue(entry.isNull("recordingId")); assertTrue(entry.isNull("artistId")); assertTrue(entry.isNull("albumId"))
    }
}
