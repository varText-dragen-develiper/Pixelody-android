package com.pixelody.app.feature.nowplaying

import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingScreenTest {

    @Test
    fun miniPlayerTimeLabel_formatsProperly() {
        assertEquals("1:00 / 3:30", miniPlayerTimeLabel(60_000L, 210_000L))
        assertEquals("0:00 / 4:05", miniPlayerTimeLabel(0L, 245_000L))
        assertEquals("", miniPlayerTimeLabel(10_000L, 0L))
    }

    @Test
    fun equalizerProfile_presetsNormalizesCorrectly() {
        val profile = EqualizerProfile(preset = EqualizerPreset.Bass).normalized()
        assertNotNull(profile)
        assertEquals(5, profile.gainsDb.size)
    }

    @Test
    fun trackModel_attributesPreserved() {
        val track = Track(
            id = "test-1",
            title = "Starlight Drive",
            artist = "Retro Synth",
            album = "Neon Night",
            durationSeconds = 240,
            lossless = true,
            format = "FLAC"
        )
        assertEquals("test-1", track.id)
        assertEquals("Starlight Drive", track.title)
        assertEquals(true, track.lossless)
    }

    @Test
    fun availablePlayerViewModes_distinctModesWithUniqueGlyphs() {
        assertEquals(14, AvailablePlayerViewModes.size)
        // Verify unique IDs
        assertEquals(14, AvailablePlayerViewModes.map { it.id }.distinct().size)
        // Verify 100% unique glyphs across all view modes
        assertEquals(14, AvailablePlayerViewModes.map { it.glyph }.distinct().size)
        // Verify all labels are non-blank
        assertTrue(AvailablePlayerViewModes.all { it.label.isNotBlank() })
    }
}
