package com.pixelody.app.core.playback

import com.pixelody.app.data.model.StemChannelState
import com.pixelody.app.data.model.StemChannelTelemetry
import com.pixelody.app.data.model.StemPreset
import com.pixelody.app.data.model.StemType
import com.pixelody.app.data.model.StemsIsolatorSettings
import com.pixelody.app.data.model.StemsIsolatorTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * StemsIsolatorEngine: Real-Time 4-Channel Audio Stems Demixing & Interactive DJ Mixing Engine.
 * Deconstructs stereo audio streams into Vocals, Drums, Bass, and Instruments using 4-way Linkwitz-Riley
 * crossover band splitting, Mid/Side phase extraction, 3-band channel parametric EQs, DJ sweep filters,
 * solo/mute matrix routing, equal-power crossfader, and real-time LED peak meter ballistics.
 */
class StemsIsolatorEngine(
    initialSettings: StemsIsolatorSettings = StemsIsolatorSettings(),
    private val sampleRateHz: Int = 48000
) {
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<StemsIsolatorSettings> = _settings.asStateFlow()

    private val _telemetry = MutableStateFlow(calculateInitialTelemetry(initialSettings))
    val telemetry: StateFlow<StemsIsolatorTelemetry> = _telemetry.asStateFlow()

    // Biquad filter states for Linkwitz-Riley crossover networks (Left and Right)
    // 1. Bass Low-Pass Filter (220 Hz)
    private var bassLpStateL1 = 0f
    private var bassLpStateL2 = 0f
    private var bassLpStateR1 = 0f
    private var bassLpStateR2 = 0f

    // 2. Drums Band-Pass Filter (200 Hz - 1000 Hz)
    private var drumBpStateL1 = 0f
    private var drumBpStateL2 = 0f
    private var drumBpStateR1 = 0f
    private var drumBpStateR2 = 0f

    // 3. Vocals Band-Pass Formant Filter (800 Hz - 4500 Hz)
    private var vocalBpStateL1 = 0f
    private var vocalBpStateL2 = 0f
    private var vocalBpStateR1 = 0f
    private var vocalBpStateR2 = 0f

    // 4. Instruments High-Pass Filter (4500 Hz)
    private var instHpStateL = 0f
    private var instHpStateR = 0f

    // Per-channel DJ Sweep Filter states
    private val channelFilterStatesL = FloatArray(StemType.values().size)
    private val channelFilterStatesR = FloatArray(StemType.values().size)

    // Per-channel EQ filter states (Low, Mid, High)
    private val channelEqLowL = FloatArray(StemType.values().size)
    private val channelEqLowR = FloatArray(StemType.values().size)
    private val channelEqMidL = FloatArray(StemType.values().size)
    private val channelEqMidR = FloatArray(StemType.values().size)
    private val channelEqHighL = FloatArray(StemType.values().size)
    private val channelEqHighR = FloatArray(StemType.values().size)

    // Metering ballistics states (RMS and Peak hold)
    private val stemRmsValues = FloatArray(StemType.values().size) { 0.001f }
    private val stemPeakValues = FloatArray(StemType.values().size) { 0.001f }
    private var masterRms = 0.001f
    private var masterPeak = 0.001f

    private var sampleCounter = 0

    fun updateSettings(newSettings: StemsIsolatorSettings) {
        _settings.value = newSettings
        refreshTelemetry()
    }

    fun setStemGain(stemType: StemType, gainDb: Float) {
        val current = _settings.value
        val ch = current.channels[stemType] ?: StemChannelState(stemType)
        val updated = current.channels.toMutableMap().apply {
            put(stemType, ch.copy(gainDb = gainDb.coerceIn(-40f, 6f)))
        }
        updateSettings(current.copy(channels = updated))
    }

    fun toggleMute(stemType: StemType) {
        val current = _settings.value
        val ch = current.channels[stemType] ?: StemChannelState(stemType)
        val updated = current.channels.toMutableMap().apply {
            put(stemType, ch.copy(isMuted = !ch.isMuted))
        }
        updateSettings(current.copy(channels = updated))
    }

    fun toggleSolo(stemType: StemType) {
        val current = _settings.value
        val ch = current.channels[stemType] ?: StemChannelState(stemType)
        val updated = current.channels.toMutableMap().apply {
            put(stemType, ch.copy(isSoloed = !ch.isSoloed))
        }
        updateSettings(current.copy(channels = updated))
    }

    fun setStemPan(stemType: StemType, pan: Float) {
        val current = _settings.value
        val ch = current.channels[stemType] ?: StemChannelState(stemType)
        val updated = current.channels.toMutableMap().apply {
            put(stemType, ch.copy(pan = pan.coerceIn(-1.0f, 1.0f)))
        }
        updateSettings(current.copy(channels = updated))
    }

    fun setStemEq(stemType: StemType, lowDb: Float, midDb: Float, highDb: Float) {
        val current = _settings.value
        val ch = current.channels[stemType] ?: StemChannelState(stemType)
        val updated = current.channels.toMutableMap().apply {
            put(stemType, ch.copy(
                eqLowDb = lowDb.coerceIn(-12f, 12f),
                eqMidDb = midDb.coerceIn(-12f, 12f),
                eqHighDb = highDb.coerceIn(-12f, 12f)
            ))
        }
        updateSettings(current.copy(channels = updated))
    }

    fun setStemFilter(stemType: StemType, cutoffNormalized: Float) {
        val current = _settings.value
        val ch = current.channels[stemType] ?: StemChannelState(stemType)
        val updated = current.channels.toMutableMap().apply {
            put(stemType, ch.copy(filterCutoffNormalized = cutoffNormalized.coerceIn(0.0f, 1.0f)))
        }
        updateSettings(current.copy(channels = updated))
    }

    fun setCrossfaderPosition(pos: Float) {
        val current = _settings.value
        updateSettings(current.copy(crossfaderPosition = pos.coerceIn(-1.0f, 1.0f)))
    }

    fun applyPreset(preset: StemPreset) {
        val current = _settings.value
        val updatedChannels = current.channels.toMutableMap()

        StemType.values().forEach { type ->
            val existing = updatedChannels[type] ?: StemChannelState(type)
            val newGain = when (type) {
                StemType.Vocals -> preset.vocalGainDb
                StemType.Drums -> preset.drumsGainDb
                StemType.Bass -> preset.bassGainDb
                StemType.Instruments -> preset.instGainDb
            }
            val newMute = when (type) {
                StemType.Vocals -> preset.vocalMute
                StemType.Drums -> preset.drumsMute
                StemType.Bass -> preset.bassMute
                StemType.Instruments -> preset.instMute
            }
            updatedChannels[type] = existing.copy(
                gainDb = newGain,
                isMuted = newMute,
                isSoloed = false
            )
        }

        updateSettings(current.copy(channels = updatedChannels, activePreset = preset))
    }

    fun toggleEnabled() {
        val current = _settings.value
        updateSettings(current.copy(isEnabled = !current.isEnabled))
    }

    /**
     * Processes a single stereo frame (L, R) through 4-stem demixing, channel strips, and DJ crossfader.
     */
    fun processStereoFrame(leftIn: Float, rightIn: Float): Pair<Float, Float> {
        val s = _settings.value
        if (!s.isEnabled) {
            updateMeters(leftIn, rightIn, FloatArray(4) { 0.1f })
            return leftIn to rightIn
        }

        sampleCounter++

        // 1. Mid / Side Encoding
        val sqrt2Inv = 0.70710678f
        val mid = (leftIn + rightIn) * sqrt2Inv
        val side = (leftIn - rightIn) * sqrt2Inv

        // 2. 4-Way Frequency Demixing Crossover Filters
        // 2a. Sub/Bass Low-Pass (220 Hz)
        val bassAlpha = (1.0f - exp(-2.0 * PI * 220.0 / sampleRateHz)).toFloat()
        bassLpStateL1 += bassAlpha * (leftIn - bassLpStateL1)
        bassLpStateL2 += bassAlpha * (bassLpStateL1 - bassLpStateL2)
        bassLpStateR1 += bassAlpha * (rightIn - bassLpStateR1)
        bassLpStateR2 += bassAlpha * (bassLpStateR1 - bassLpStateR2)

        val rawBassL = bassLpStateL2
        val rawBassR = bassLpStateR2

        // 2b. Drums Band-Pass (180 Hz to 1200 Hz) with dynamic transient punch
        val drumLpAlpha = (1.0f - exp(-2.0 * PI * 1100.0 / sampleRateHz)).toFloat()
        val drumHpAlpha = (1.0f - exp(-2.0 * PI * 180.0 / sampleRateHz)).toFloat()
        drumBpStateL1 += drumLpAlpha * (leftIn - drumBpStateL1)
        drumBpStateL2 += drumHpAlpha * (drumBpStateL1 - drumBpStateL2)
        drumBpStateR1 += drumLpAlpha * (rightIn - drumBpStateR1)
        drumBpStateR2 += drumHpAlpha * (drumBpStateR1 - drumBpStateR2)

        val rawDrumsL = (drumBpStateL1 - drumBpStateL2) * 1.25f
        val rawDrumsR = (drumBpStateR1 - drumBpStateR2) * 1.25f

        // 2c. Vocals Center Band-Pass (800 Hz to 4500 Hz) extracted from Mid channel
        val vocalLpAlpha = (1.0f - exp(-2.0 * PI * 4200.0 / sampleRateHz)).toFloat()
        val vocalHpAlpha = (1.0f - exp(-2.0 * PI * 750.0 / sampleRateHz)).toFloat()
        vocalBpStateL1 += vocalLpAlpha * (mid - vocalBpStateL1)
        vocalBpStateL2 += vocalHpAlpha * (vocalBpStateL1 - vocalBpStateL2)

        val rawVocalCenter = (vocalBpStateL1 - vocalBpStateL2) * 1.4f
        // Synthesize stereo vocal image with center emphasis
        val rawVocalsL = rawVocalCenter * 0.85f + (leftIn * 0.15f)
        val rawVocalsR = rawVocalCenter * 0.85f + (rightIn * 0.15f)

        // 2d. Instruments Harmonic Bed (Side channel + high frequency air > 4500 Hz)
        val instHpAlpha = (1.0f - exp(-2.0 * PI * 4500.0 / sampleRateHz)).toFloat()
        instHpStateL += instHpAlpha * (leftIn - instHpStateL)
        instHpStateR += instHpAlpha * (rightIn - instHpStateR)

        val highAirL = leftIn - instHpStateL
        val highAirR = rightIn - instHpStateR
        val rawInstL = (side * 0.707f) + (highAirL * 0.85f)
        val rawInstR = (-side * 0.707f) + (highAirR * 0.85f)

        val rawStems = arrayOf(
            rawVocalsL to rawVocalsR,
            rawDrumsL to rawDrumsR,
            rawBassL to rawBassR,
            rawInstL to rawInstR
        )

        // 3. Solo / Mute Matrix Logic
        val anySoloed = s.channels.values.any { it.isSoloed }

        // Crossfader gains (Deck A: Drums/Bass, Deck B: Vocals/Instruments)
        val crossPos = s.crossfaderPosition // -1.0 to 1.0
        val crossAngle = ((crossPos + 1.0f) / 2.0f) * (PI / 2.0).toFloat()
        val deckAGain = (cos(crossAngle) * 1.4142f).coerceIn(0.0f, 1.4142f)
        val deckBGain = (sin(crossAngle) * 1.4142f).coerceIn(0.0f, 1.4142f)

        var masterSumL = 0.0f
        var masterSumR = 0.0f
        val stemEnergyLevels = FloatArray(4)

        StemType.values().forEachIndexed { index, type ->
            val ch = s.channels[type] ?: StemChannelState(type)
            val (stemL, stemR) = rawStems[index]

            // Check if stem is audible
            val isAudible = if (anySoloed) (ch.isSoloed && !ch.isMuted) else !ch.isMuted

            if (isAudible) {
                // 3-Band Parametric EQ
                val (eqL, eqR) = processStemEq(index, stemL, stemR, ch.eqLowDb, ch.eqMidDb, ch.eqHighDb)

                // DJ Sweep Filter
                val (filteredL, filteredR) = processStemDjFilter(index, eqL, eqR, ch.filterCutoffNormalized)

                // Channel Gain
                val linearGain = 10.0.pow(ch.gainDb / 20.0).toFloat()

                // Crossfader Deck Gain attribution
                val deckMult = when (type) {
                    StemType.Drums, StemType.Bass -> deckAGain
                    StemType.Vocals, StemType.Instruments -> deckBGain
                }

                // Panning (Constant Power)
                val panAngle = ((ch.pan + 1.0f) / 2.0f) * (PI / 2.0).toFloat()
                val panL = cos(panAngle) * 1.4142f
                val panR = sin(panAngle) * 1.4142f

                val finalStemL = filteredL * linearGain * deckMult * panL
                val finalStemR = filteredR * linearGain * deckMult * panR

                masterSumL += finalStemL
                masterSumR += finalStemR

                stemEnergyLevels[index] = (abs(finalStemL) + abs(finalStemR)) * 0.5f
            } else {
                stemEnergyLevels[index] = 0.0f
            }
        }

        // 4. Master Gain & Soft Limiting
        val masterLinearGain = 10.0.pow(s.masterGainDb / 20.0).toFloat()
        val drivenMasterL = masterSumL * masterLinearGain
        val drivenMasterR = masterSumR * masterLinearGain

        val finalOutL = tanh(drivenMasterL)
        val finalOutR = tanh(drivenMasterR)

        // 5. Update Telemetry & Meters
        updateMeters(finalOutL, finalOutR, stemEnergyLevels)

        return finalOutL to finalOutR
    }

    /**
     * Batch processes arrays of stereo audio samples in-place.
     */
    fun processStereoBuffer(leftChannel: FloatArray, rightChannel: FloatArray) {
        val count = min(leftChannel.size, rightChannel.size)
        for (i in 0 until count) {
            val (l, r) = processStereoFrame(leftChannel[i], rightChannel[i])
            leftChannel[i] = l
            rightChannel[i] = r
        }
    }

    /**
     * 3-Band Parametric EQ: Low shelf (100Hz), Mid bell (1.2kHz), High shelf (8kHz).
     */
    private fun processStemEq(
        stemIdx: Int,
        inL: Float,
        inR: Float,
        lowDb: Float,
        midDb: Float,
        highDb: Float
    ): Pair<Float, Float> {
        if (lowDb == 0f && midDb == 0f && highDb == 0f) return inL to inR

        val lowGain = 10.0.pow(lowDb / 20.0).toFloat()
        val midGain = 10.0.pow(midDb / 20.0).toFloat()
        val highGain = 10.0.pow(highDb / 20.0).toFloat()

        // 1-pole Low Shelf (120 Hz)
        val alphaLow = (1.0f - exp(-2.0 * PI * 120.0 / sampleRateHz)).toFloat()
        channelEqLowL[stemIdx] += alphaLow * (inL - channelEqLowL[stemIdx])
        channelEqLowR[stemIdx] += alphaLow * (inR - channelEqLowR[stemIdx])

        // 1-pole High Shelf (7500 Hz)
        val alphaHigh = (1.0f - exp(-2.0 * PI * 7500.0 / sampleRateHz)).toFloat()
        channelEqHighL[stemIdx] += alphaHigh * (inL - channelEqHighL[stemIdx])
        channelEqHighR[stemIdx] += alphaHigh * (inR - channelEqHighR[stemIdx])

        val lowBandL = channelEqLowL[stemIdx]
        val highBandL = inL - channelEqHighL[stemIdx]
        val midBandL = inL - lowBandL - highBandL

        val lowBandR = channelEqLowR[stemIdx]
        val highBandR = inR - channelEqHighR[stemIdx]
        val midBandR = inR - lowBandR - highBandR

        val outL = (lowBandL * lowGain) + (midBandL * midGain) + (highBandL * highGain)
        val outR = (lowBandR * lowGain) + (midBandR * midGain) + (highBandR * highGain)

        return outL to outR
    }

    /**
     * DJ Sweep Filter: Normalized 0.0 (Full LPF) <-> 0.5 (Bypass) <-> 1.0 (Full HPF).
     */
    private fun processStemDjFilter(
        stemIdx: Int,
        inL: Float,
        inR: Float,
        cutoffNorm: Float
    ): Pair<Float, Float> {
        if (cutoffNorm in 0.47f..0.53f) return inL to inR

        return if (cutoffNorm < 0.47f) {
            // Low Pass Sweep: 200 Hz to 20,000 Hz
            val targetHz = (200.0 * 100.0.pow(cutoffNorm.toDouble() / 0.47)).toFloat().coerceIn(150f, 20000f)
            val alpha = (1.0f - exp(-2.0 * PI * targetHz / sampleRateHz)).toFloat().coerceIn(0.01f, 0.99f)

            channelFilterStatesL[stemIdx] += alpha * (inL - channelFilterStatesL[stemIdx])
            channelFilterStatesR[stemIdx] += alpha * (inR - channelFilterStatesR[stemIdx])

            channelFilterStatesL[stemIdx] to channelFilterStatesR[stemIdx]
        } else {
            // High Pass Sweep: 20 Hz to 7,500 Hz
            val highFrac = (cutoffNorm - 0.53f) / 0.47f
            val targetHz = (20.0 * 375.0.pow(highFrac.toDouble())).toFloat().coerceIn(20f, 8000f)
            val alpha = (1.0f - exp(-2.0 * PI * targetHz / sampleRateHz)).toFloat().coerceIn(0.01f, 0.99f)

            channelFilterStatesL[stemIdx] += alpha * (inL - channelFilterStatesL[stemIdx])
            channelFilterStatesR[stemIdx] += alpha * (inR - channelFilterStatesR[stemIdx])

            (inL - channelFilterStatesL[stemIdx]) to (inR - channelFilterStatesR[stemIdx])
        }
    }

    /**
     * Updates RMS and peak hold meters.
     */
    private fun updateMeters(outL: Float, outR: Float, stemLevels: FloatArray) {
        val masterAmp = (abs(outL) + abs(outR)) * 0.5f
        masterRms = 0.98f * masterRms + 0.02f * masterAmp
        masterPeak = max(masterAmp, masterPeak * 0.96f)

        for (i in stemLevels.indices) {
            val level = stemLevels[i]
            stemRmsValues[i] = 0.98f * stemRmsValues[i] + 0.02f * level
            stemPeakValues[i] = max(level, stemPeakValues[i] * 0.96f)
        }

        if (sampleCounter % 256 == 0) {
            refreshTelemetry()
        }
    }

    private fun refreshTelemetry() {
        val s = _settings.value
        val telemetryMap = mutableMapOf<StemType, StemChannelTelemetry>()

        val masterRmsDb = if (masterRms > 0.0001f) (20f * log10(masterRms)).coerceIn(-48f, 6f) else -48f
        val masterPeakDb = if (masterPeak > 0.0001f) (20f * log10(masterPeak)).coerceIn(-48f, 6f) else -48f
        val masterMeterNorm = ((masterPeakDb + 40f) / 46f).coerceIn(0.0f, 1.0f)

        val crossPos = s.crossfaderPosition
        val crossAngle = ((crossPos + 1.0f) / 2.0f) * (PI / 2.0).toFloat()
        val deckAGain = (cos(crossAngle) * 1.4142f).coerceIn(0.0f, 1.4142f)
        val deckBGain = (sin(crossAngle) * 1.4142f).coerceIn(0.0f, 1.4142f)

        var soloCount = 0

        StemType.values().forEachIndexed { index, type ->
            val ch = s.channels[type] ?: StemChannelState(type)
            if (ch.isSoloed) soloCount++

            val rms = stemRmsValues[index]
            val peak = stemPeakValues[index]
            val rmsDb = if (rms > 0.0001f) (20f * log10(rms)).coerceIn(-48f, 6f) else -48f
            val peakDb = if (peak > 0.0001f) (20f * log10(peak)).coerceIn(-48f, 6f) else -48f
            val meterNorm = ((peakDb + 40f) / 46f).coerceIn(0.0f, 1.0f)

            telemetryMap[type] = StemChannelTelemetry(
                stemType = type,
                rmsDb = rmsDb,
                peakDb = peakDb,
                meterNormalized = meterNorm,
                transientActivity = (peak / max(rms, 0.001f)).coerceIn(0.1f, 3.0f),
                spectralCentroidHz = type.defaultCenterHz
            )
        }

        _telemetry.value = StemsIsolatorTelemetry(
            channelTelemetry = telemetryMap,
            masterRmsDb = masterRmsDb,
            masterPeakDb = masterPeakDb,
            masterMeterNormalized = masterMeterNorm,
            activeSoloCount = soloCount,
            crossfaderDeckAGain = deckAGain,
            crossfaderDeckBGain = deckBGain
        )
    }

    private fun calculateInitialTelemetry(s: StemsIsolatorSettings): StemsIsolatorTelemetry {
        return StemsIsolatorTelemetry(
            channelTelemetry = StemsIsolatorTelemetry.createDefaultTelemetryMap(),
            masterRmsDb = -18f,
            masterPeakDb = -12f,
            masterMeterNormalized = 0.52f,
            activeSoloCount = 0,
            crossfaderDeckAGain = 1.0f,
            crossfaderDeckBGain = 1.0f
        )
    }
}
