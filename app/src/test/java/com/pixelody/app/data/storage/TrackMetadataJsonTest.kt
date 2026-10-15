package com.pixelody.app.data.storage

import com.pixelody.app.data.model.Track
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TrackMetadataJsonTest {
    @Test fun metadataRoundTripPreservesKnownFieldsAndNullableAbsence() {
        val original = Track("id", "Title", albumArtist = "Various Artists", discNumber = 2, trackNumber = 4,
            albumId = "album:α / b", recordingId = "recording:1", musicalKey = "D♭ minor", bpm = 128.5f, genre = "Jazz")
        val json = TrackMetadataJson.append(JSONObject(), original).toString()
        assertEquals(original, TrackMetadataJson.apply(JSONObject(json), Track("id", "Title")))
        val unknown = Track("id", "Title")
        assertEquals(unknown, TrackMetadataJson.apply(TrackMetadataJson.append(JSONObject(), unknown), unknown))
        assertEquals(unknown, TrackMetadataJson.apply(JSONObject("{}"), unknown))
    }

    @Test fun nullAndZeroTempoStayDistinctAndCorruptTagsAreUnavailable() {
        val track = Track("id", "Title")
        assertNull(TrackMetadataJson.apply(JSONObject("{\"bpm\":null}"), track).bpm)
        assertEquals(0f, TrackMetadataJson.apply(JSONObject("{\"bpm\":0}"), track).bpm!!, 0f)
        val invalid = TrackMetadataJson.apply(JSONObject("{\"discNumber\":0,\"trackNumber\":-1,\"albumArtist\":null,\"bpm\":\"bad\"}"), track)
        assertNull(invalid.discNumber); assertNull(invalid.trackNumber); assertNull(invalid.albumArtist); assertNull(invalid.bpm)
    }
}
