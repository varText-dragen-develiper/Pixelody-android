package com.pixelody.app.feature.nowplaying

import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import org.junit.Assert.*
import org.junit.Test

class BasicEqualizerControlsTest {
    @Test fun flatResetClearsEveryBandIncludingHiddenFineAdjustments() {
        val reset = EqualizerProfile(enabled = false, preset = EqualizerPreset.Custom,
            gainsDb = listOf(5f, -5f, 3f, -2f, 2f)).flatTone()
        assertTrue(reset.enabled)
        assertEquals(EqualizerPreset.Flat, reset.preset)
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), reset.gainsDb)
    }
    @Test fun groupedAdjustmentPreservesDetailedShapeAndUnrelatedBands() {
        val original = EqualizerProfile(enabled = false, gainsDb = listOf(3f, 1f, -2f, 4f, 2f))
        val adjusted = original.withToneGain(BasicTone.Bass, 5f)
        assertEquals(listOf(6f, 4f, -2f, 4f, 2f), adjusted.gainsDb)
        assertEquals(5f, adjusted.toneGain(BasicTone.Bass), 0.001f)
        assertTrue(adjusted.enabled)
        assertEquals(EqualizerPreset.Custom, adjusted.preset)
        assertEquals(listOf(3f, 1f, -2f, 4f, 2f), original.gainsDb)
    }

    @Test fun limitsAndShortProfilesRemainSafeForEveryTone() {
        for (tone in BasicTone.entries) {
            for (gain in listOf(-20f, 20f)) {
                val adjusted = EqualizerProfile(gainsDb = listOf(11f)).withToneGain(tone, gain)
                assertEquals(5, adjusted.gainsDb.size)
                assertTrue(adjusted.gainsDb.all { it in -12f..12f })
                assertTrue(adjusted.enabled)
            }
        }
        val treble = EqualizerProfile(gainsDb = listOf(1f, 2f, 3f, 11f, 9f))
            .withToneGain(BasicTone.Treble, 12f)
        assertEquals(listOf(1f, 2f, 3f, 12f, 11f), treble.gainsDb)
    }
}
