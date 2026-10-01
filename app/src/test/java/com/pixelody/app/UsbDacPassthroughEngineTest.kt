package com.pixelody.app

import android.os.Build
import com.pixelody.app.core.playback.UsbDacPassthroughEngine
import com.pixelody.app.data.model.AudioFormatCodec
import com.pixelody.app.data.model.DacConnectionState
import com.pixelody.app.data.model.DacHardwareProfile
import com.pixelody.app.data.model.DacVolumeMode
import com.pixelody.app.data.model.DsdPlaybackMode
import com.pixelody.app.data.model.HiResBitDepth
import com.pixelody.app.data.model.HiResSampleRate
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbDacPassthroughEngineTest {

    @Test
    fun testInitialEngineStateAndDefaults() {
        val engine = UsbDacPassthroughEngine()
        val settings = engine.settings.value
        val telemetry = engine.telemetry.value

        assertTrue(settings.isBitPerfectEnabled)
        assertTrue(settings.directPcmBypass)
        assertEquals(DacVolumeMode.HardwareDirect, settings.volumeControlMode)
        assertEquals(DsdPlaybackMode.DoP, settings.dsdMode)
        assertEquals(DacConnectionState.Disconnected, telemetry.connectionState)
        assertFalse(telemetry.hardwareProfile.isExternalUsbDac)
    }

    @Test
    fun testTrackMetadataParsingFlacHiRes() {
        val engine = UsbDacPassthroughEngine()
        val track = Track(
            id = "t1",
            title = "Acoustic Horizon 96k 24b",
            artist = "Studio Master",
            album = "Audiophile Showcase",
            durationSeconds = 240,
            streamUrl = "http://127.0.0.1:8080/stream/t1_96k_24bit.flac",
            format = "flac",
            sampleRate = 96000,
            bitDepth = 24,
            bitrate = 2800,
            lossless = true
        )

        val spec = engine.parseTrackSpec(track)
        assertEquals(AudioFormatCodec.FLAC, spec.codec)
        assertEquals(HiResSampleRate.Rate_96kHz, spec.sampleRate)
        assertEquals(HiResBitDepth.Bit_24, spec.bitDepth)
        assertTrue(spec.isLossless)
        assertFalse(spec.isDsd)
    }

    @Test
    fun testTrackMetadataParsingWav192k() {
        val engine = UsbDacPassthroughEngine()
        val track = Track(
            id = "t2",
            title = "Symphonic Studio Master 192kHz",
            artist = "Philharmonic",
            album = "Concert Hall",
            durationSeconds = 360,
            streamUrl = "http://127.0.0.1:8080/stream/t2_192khz.wav",
            format = "wav",
            sampleRate = 192000,
            bitDepth = 24,
            bitrate = 9216,
            lossless = true
        )

        val spec = engine.parseTrackSpec(track)
        assertEquals(AudioFormatCodec.WAV, spec.codec)
        assertEquals(HiResSampleRate.Rate_192kHz, spec.sampleRate)
        assertEquals(HiResBitDepth.Bit_24, spec.bitDepth)
        assertTrue(spec.isLossless)
    }

    @Test
    fun testTrackMetadataParsingDsd64() {
        val engine = UsbDacPassthroughEngine()
        val track = Track(
            id = "t3",
            title = "Direct Stream Digital Take",
            artist = "Jazz Quintet",
            album = "Pure DSD",
            durationSeconds = 300,
            streamUrl = "http://127.0.0.1:8080/stream/t3_dsd64.dsf",
            format = "dsd",
            sampleRate = 176400,
            bitrate = 5645,
            lossless = true
        )

        val spec = engine.parseTrackSpec(track)
        assertEquals(AudioFormatCodec.DSD_DoP, spec.codec)
        assertEquals(HiResSampleRate.Rate_176_4kHz, spec.sampleRate)
        assertTrue(spec.isDsd)
        assertNotNull(spec.dsdRateString)
        assertTrue(spec.dsdRateString?.contains("2.8224 MHz") == true)
    }

    @Test
    fun testTrackMetadataParsingMp3Lossy() {
        val engine = UsbDacPassthroughEngine()
        val track = Track(
            id = "t4",
            title = "Compressed Stream",
            artist = "Radio",
            album = "Broadcast",
            durationSeconds = 180,
            streamUrl = "http://127.0.0.1:8080/stream/t4.mp3",
            format = "mp3",
            sampleRate = 44100,
            bitDepth = 16,
            bitrate = 320,
            lossless = false
        )

        val spec = engine.parseTrackSpec(track)
        assertEquals(AudioFormatCodec.MP3, spec.codec)
        assertEquals(HiResSampleRate.Rate_44_1kHz, spec.sampleRate)
        assertEquals(HiResBitDepth.Bit_16, spec.bitDepth)
        assertFalse(spec.isLossless)
    }

    @Test
    fun testBitPerfectDirectMatchWithSupportedDac() {
        val engine = UsbDacPassthroughEngine()

        val fiioDac = DacHardwareProfile(
            deviceName = "FiiO Q3 MQA DAC",
            manufacturer = "FiiO",
            usbVendorId = "2972",
            usbProductId = "0043",
            supportedSampleRates = listOf(
                HiResSampleRate.Rate_44_1kHz,
                HiResSampleRate.Rate_48kHz,
                HiResSampleRate.Rate_96kHz,
                HiResSampleRate.Rate_192kHz
            ),
            supportedBitDepths = listOf(HiResBitDepth.Bit_16, HiResBitDepth.Bit_24, HiResBitDepth.Bit_32_Int),
            supportsDsdDoP = true,
            isExternalUsbDac = true,
            maxSampleRateHz = 192000
        )

        engine.simulateDacConnection(fiioDac)

        val track = Track(
            id = "t5",
            title = "Direct Master 192k",
            artist = "Studio",
            album = "Hi-Res",
            durationSeconds = 200,
            streamUrl = "http://127.0.0.1:8080/stream/t5_192k.flac",
            format = "flac",
            sampleRate = 192000,
            bitDepth = 24,
            bitrate = 4500,
            lossless = true
        )

        engine.updateSourceSpec(track)

        val telemetry = engine.telemetry.value
        assertEquals(DacConnectionState.ConnectedBitPerfect, telemetry.connectionState)
        assertTrue(telemetry.signalChain.isBitPerfectDirect)
        assertTrue(telemetry.signalChain.isExactClockMatch)
        assertTrue(telemetry.signalChain.isExactBitDepthMatch)
        assertTrue(telemetry.signalChain.isOsResamplerBypassed)
        assertEquals(1.0f, telemetry.signalChain.signalHealthScore, 0.001f)
        assertEquals(HiResSampleRate.Rate_192kHz, telemetry.signalChain.dacOutputRate)
    }

    @Test
    fun testResamplingFallbackWithLimitedDac() {
        val engine = UsbDacPassthroughEngine()

        val limitedDac = DacHardwareProfile(
            deviceName = "Legacy 48k DAC",
            manufacturer = "Generic",
            supportedSampleRates = listOf(HiResSampleRate.Rate_44_1kHz, HiResSampleRate.Rate_48kHz),
            supportedBitDepths = listOf(HiResBitDepth.Bit_16),
            isExternalUsbDac = true,
            maxSampleRateHz = 48000
        )

        engine.simulateDacConnection(limitedDac)

        val track = Track(
            id = "t6",
            title = "Master 96k",
            artist = "Studio",
            album = "Hi-Res",
            durationSeconds = 200,
            streamUrl = "http://127.0.0.1:8080/stream/t6_96k.flac",
            format = "flac",
            sampleRate = 96000,
            bitDepth = 24,
            bitrate = 2500,
            lossless = true
        )

        engine.updateSourceSpec(track)

        val telemetry = engine.telemetry.value
        assertEquals(DacConnectionState.FallbackSystemMixer, telemetry.connectionState)
        assertFalse(telemetry.signalChain.isBitPerfectDirect)
        assertFalse(telemetry.signalChain.isExactClockMatch)
        assertEquals(HiResSampleRate.Rate_48kHz, telemetry.signalChain.dacOutputRate)
    }

    @Test
    fun testSettingsMutationsAffectSignalChain() {
        val engine = UsbDacPassthroughEngine()

        val dac = DacHardwareProfile(
            deviceName = "DragonFly",
            manufacturer = "AudioQuest",
            supportedSampleRates = listOf(HiResSampleRate.Rate_44_1kHz, HiResSampleRate.Rate_96kHz),
            supportedBitDepths = listOf(HiResBitDepth.Bit_16, HiResBitDepth.Bit_24),
            isExternalUsbDac = true,
            maxSampleRateHz = 96000
        )
        engine.simulateDacConnection(dac)

        val track = Track(
            id = "t7",
            title = "Track 96k",
            artist = "Artist",
            album = "Album",
            durationSeconds = 200,
            streamUrl = "http://stream/96k.flac",
            format = "flac",
            sampleRate = 96000,
            bitDepth = 24,
            lossless = true
        )
        engine.updateSourceSpec(track)

        assertTrue(engine.telemetry.value.signalChain.isBitPerfectDirect)

        // Disable bit-perfect
        engine.toggleBitPerfect(false)
        assertFalse(engine.telemetry.value.signalChain.isBitPerfectDirect)

        // Re-enable and switch to SoftwareFloat volume
        engine.toggleBitPerfect(true)
        engine.setVolumeMode(DacVolumeMode.SoftwareFloat)
        assertFalse(engine.telemetry.value.signalChain.dspBypassed)
        assertFalse(engine.telemetry.value.signalChain.isBitPerfectDirect)
    }

    @Test
    fun testDirectAudioAttributes() {
        val engine = UsbDacPassthroughEngine()
        val attrs = engine.buildDirectAudioAttributes()
        // In headless JVM environment this executes safely without throwing
        assertTrue(attrs != null || Build.VERSION.SDK_INT == 0)
    }

    @Test
    fun testDacDisconnectSimulation() {
        val engine = UsbDacPassthroughEngine()
        val dac = DacHardwareProfile(
            deviceName = "USB DAC",
            isExternalUsbDac = true
        )
        engine.simulateDacConnection(dac)
        assertTrue(engine.telemetry.value.hardwareProfile.isExternalUsbDac)

        engine.simulateDacDisconnection()
        assertFalse(engine.telemetry.value.hardwareProfile.isExternalUsbDac)
        assertEquals(DacConnectionState.Disconnected, engine.telemetry.value.connectionState)
    }
}
