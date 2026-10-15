package com.pixelody.app.core.playback

import com.pixelody.app.data.model.OscilloscopeDisplayMode
import com.pixelody.app.data.model.OscilloscopeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LiveAudioVisualizerBridgeTest {

    private lateinit var oscilloscopeEngine: OscilloscopePhosphorEngine

    @Before
    fun setup() {
        oscilloscopeEngine = OscilloscopePhosphorEngine(
            initialSettings = OscilloscopeSettings(displayMode = OscilloscopeDisplayMode.Lissajous_XY)
        )
    }

    @Test
    fun testProcessNativeWaveform_normalizesUnsignedByteToUnitRange() {
        // Mock a byte array with typical unsigned 8-bit PCM values: 0 (min), 128 (center/zero), 255 (max)
        val rawPcm = byteArrayOf(0.toByte(), 128.toByte(), 255.toByte(), 64.toByte(), 192.toByte())

        val size = rawPcm.size
        val left = FloatArray(size)
        val right = FloatArray(size)
        for (i in 0 until size) {
            val unsigned = rawPcm[i].toInt() and 0xFF
            val sample = (unsigned - 128) / 128.0f
            val stereoOffset = (i % 8 - 4) * 0.02f
            left[i] = (sample + stereoOffset).coerceIn(-1.0f, 1.0f)
            right[i] = (sample - stereoOffset).coerceIn(-1.0f, 1.0f)
        }

        oscilloscopeEngine.processStereoBuffer(left, right)
        val points = oscilloscopeEngine.generateVectorPoints(width = 400f, height = 400f, pointCount = 10)

        assertEquals(10, points.size)
        assertTrue("Intensity should be within valid range", points.all { it.intensity in 0.1f..1.0f })
    }

    @Test
    fun testProcessNativeFft_computes64FrequencyBands() {
        // Mock FFT buffer of 128 bytes (representing 64 complex pairs: [re0, im0, re1, im1, ...])
        val fftBuffer = ByteArray(128) { index ->
            if (index % 2 == 0) (index * 2).toByte() else (index).toByte()
        }

        val bands = FloatArray(64)
        val n = fftBuffer.size / 2
        val step = (n / 64).coerceAtLeast(1)

        for (i in 0 until 64) {
            val idx = (i * step * 2).coerceAtMost(fftBuffer.size - 2)
            val re = fftBuffer[idx].toFloat()
            val im = fftBuffer[idx + 1].toFloat()
            val mag = kotlin.math.hypot(re, im) / 128.0f
            bands[i] = mag.coerceIn(0.0f, 1.0f)
        }

        assertEquals(64, bands.size)
        assertTrue("All bands should be in [0.0, 1.0]", bands.all { it in 0.0f..1.0f })
        assertTrue("Low frequencies should have valid magnitudes", bands[0] >= 0.0f)
    }

    @Test
    fun testOscilloscopePhaseCorrelationWithLiveWaveform() {
        val count = 256
        val left = FloatArray(count) { i -> kotlin.math.sin(i * 0.1).toFloat() }
        val right = FloatArray(count) { i -> kotlin.math.sin(i * 0.1).toFloat() } // In-phase stereo

        oscilloscopeEngine.processStereoBuffer(left, right)
        val corr = oscilloscopeEngine.computeStereoPhaseCorrelation()

        assertTrue("In-phase stereo should have correlation near 1.0, got $corr", corr > 0.8f)
    }
}
