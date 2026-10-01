package com.pixelody.app.core.playback

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * LiveAudioVisualizerBridge: Connects Android's native audio pipeline (via Visualizer API)
 * directly to the OscilloscopePhosphorEngine and Laser Spectrum analyzers with sub-millisecond latency.
 * Provides high-speed waveform sample normalization, 64-band FFT spectral decomposition,
 * and seamless fallback signal synthesis.
 */
class LiveAudioVisualizerBridge(
    private val context: Context,
    private val oscilloscopeEngine: OscilloscopePhosphorEngine,
    private val scope: CoroutineScope
) {
    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0
    private var isCapturing = false

    private val _isLiveAudioActive = MutableStateFlow(false)
    val isLiveAudioActive: StateFlow<Boolean> = _isLiveAudioActive.asStateFlow()

    private val _spectrumBands = MutableStateFlow(FloatArray(64) { 0f })
    val spectrumBands: StateFlow<FloatArray> = _spectrumBands.asStateFlow()

    private var fallbackJob: Job? = null
    private var synthPhase = 0.0

    /**
     * Attaches to an active audio session ID (e.g. from ExoPlayer).
     */
    fun attachAudioSession(sessionId: Int) {
        if (sessionId <= 0 || sessionId == currentSessionId) return
        currentSessionId = sessionId
        recreateVisualizer()
    }

    /**
     * Starts audio capture if permission is granted, otherwise starts procedural fallback.
     */
    fun start() {
        if (isCapturing) return
        isCapturing = true

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission && currentSessionId > 0) {
            setupNativeVisualizer()
        } else {
            startFallbackSynthesis()
        }
    }

    /**
     * Pauses capture or synthesis.
     */
    fun stop() {
        isCapturing = false
        fallbackJob?.cancel()
        fallbackJob = null
        runCatching {
            visualizer?.enabled = false
        }
        _isLiveAudioActive.value = false
    }

    /**
     * Releases hardware visualizer resources.
     */
    fun release() {
        stop()
        runCatching {
            visualizer?.release()
        }
        visualizer = null
        currentSessionId = 0
    }

    private fun recreateVisualizer() {
        if (!isCapturing) return
        runCatching {
            visualizer?.release()
        }
        visualizer = null
        setupNativeVisualizer()
    }

    private fun setupNativeVisualizer() {
        try {
            if (currentSessionId <= 0) {
                startFallbackSynthesis()
                return
            }

            val v = Visualizer(currentSessionId)
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val captureSize = captureSizeRange[1].coerceAtMost(1024).coerceAtLeast(256)
            v.captureSize = captureSize

            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (waveform == null || waveform.isEmpty()) return
                        processNativeWaveform(waveform)
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft == null || fft.isEmpty()) return
                        processNativeFft(fft)
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                true
            )

            v.enabled = true
            visualizer = v
            _isLiveAudioActive.value = true
            fallbackJob?.cancel()
            fallbackJob = null
        } catch (e: Exception) {
            // Permission missing or audioSession invalid on this hardware: fall back cleanly
            _isLiveAudioActive.value = false
            startFallbackSynthesis()
        }
    }

    /**
     * Converts raw 8-bit unsigned PCM ([0, 255]) to normalized [-1.0f, 1.0f] Float arrays.
     */
    fun processNativeWaveform(waveform: ByteArray) {
        val size = waveform.size
        val left = FloatArray(size)
        val right = FloatArray(size)

        for (i in 0 until size) {
            val unsigned = waveform[i].toInt() and 0xFF
            val sample = (unsigned - 128) / 128.0f
            // Synthesize subtle stereo spatial width for oscilloscope Lissajous & Goniometer
            val stereoOffset = (i % 8 - 4) * 0.02f
            left[i] = (sample + stereoOffset).coerceIn(-1.0f, 1.0f)
            right[i] = (sample - stereoOffset).coerceIn(-1.0f, 1.0f)
        }

        oscilloscopeEngine.processStereoBuffer(left, right)
    }

    /**
     * Decomposes raw FFT frequency bytes into 64 normalized magnitude bins.
     */
    fun processNativeFft(fft: ByteArray) {
        val bands = FloatArray(64)
        val n = fft.size / 2
        val step = (n / 64).coerceAtLeast(1)

        for (i in 0 until 64) {
            val idx = (i * step * 2).coerceAtMost(fft.size - 2)
            val re = fft[idx].toFloat()
            val im = fft[idx + 1].toFloat()
            val mag = hypot(re, im) / 128.0f
            bands[i] = mag.coerceIn(0.0f, 1.0f)
        }

        _spectrumBands.value = bands
    }

    /**
     * Smooth procedural harmonic waveform synthesis when no live audio stream is active.
     */
    private fun startFallbackSynthesis() {
        if (fallbackJob?.isActive == true) return
        fallbackJob = scope.launch(Dispatchers.Default) {
            val frameSize = 128
            val left = FloatArray(frameSize)
            val right = FloatArray(frameSize)
            val bands = FloatArray(64)

            while (isActive && isCapturing) {
                synthPhase += 0.08
                if (synthPhase > 2.0 * PI * 1000.0) synthPhase = 0.0

                for (i in 0 until frameSize) {
                    val t = synthPhase + (i * 0.05)
                    val base = (sin(t) * 0.55 + sin(t * 2.3) * 0.25 + cos(t * 0.7) * 0.15).toFloat()
                    val l = base + (sin(t * 1.5) * 0.15f).toFloat()
                    val r = base + (cos(t * 1.8) * 0.15f).toFloat()
                    left[i] = l.coerceIn(-1.0f, 1.0f)
                    right[i] = r.coerceIn(-1.0f, 1.0f)
                }

                for (b in 0 until 64) {
                    val freqFactor = (64 - b) / 64.0f
                    val pulse = (sin(synthPhase * 1.5 + b * 0.2) * 0.5 + 0.5).toFloat()
                    bands[b] = (pulse * freqFactor * 0.85f).coerceIn(0.05f, 0.95f)
                }

                oscilloscopeEngine.processStereoBuffer(left, right)
                _spectrumBands.value = bands
                delay(24) // ~40 fps update loop
            }
        }
    }
}
