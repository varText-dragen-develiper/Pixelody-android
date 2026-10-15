package com.pixelody.app

import com.pixelody.app.core.playback.AudioDeviceRoute
import com.pixelody.app.core.playback.AudioOutputRouter
import com.pixelody.app.data.model.EqualizerPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOutputRouterTest {

    @Test
    fun `wired route maps to flat and wide soundstage`() {
        val profile = AudioOutputRouter.mapRouteToProfile(AudioDeviceRoute.Wired)
        val soundstage = AudioOutputRouter.mapRouteToSoundstage(AudioDeviceRoute.Wired)

        assertEquals(EqualizerPreset.Flat, profile.preset)
        assertTrue(soundstage >= 0.8f)
    }

    @Test
    fun `bluetooth route maps to bass boost profile`() {
        val profile = AudioOutputRouter.mapRouteToProfile(AudioDeviceRoute.Bluetooth)
        val soundstage = AudioOutputRouter.mapRouteToSoundstage(AudioDeviceRoute.Bluetooth)

        assertEquals(EqualizerPreset.Bass, profile.preset)
        assertTrue(soundstage in 0.5f..0.8f)
    }

    @Test
    fun `speaker route maps to vocal clarity with focused center`() {
        val profile = AudioOutputRouter.mapRouteToProfile(AudioDeviceRoute.Speaker)
        val soundstage = AudioOutputRouter.mapRouteToSoundstage(AudioDeviceRoute.Speaker)

        assertEquals(EqualizerPreset.Vocal, profile.preset)
        assertTrue(soundstage <= 0.35f)
    }
}
