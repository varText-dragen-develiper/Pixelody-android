package com.pixelody.app.core.playback

import android.media.audiofx.BassBoost
import android.os.Build
import android.media.audiofx.DynamicsProcessing
import androidx.annotation.RequiresApi
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import com.pixelody.app.data.model.AcousticChamberPreset
import com.pixelody.app.data.model.BinauralCrossfeedMode
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.MasteringProfile
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.WallMaterialDamping
import kotlin.math.abs
import kotlin.math.roundToInt

data class EqualizerRuntimeState(
    val active: Boolean = false,
    val audioSessionId: Int = 0,
    val platformBandCount: Int = 0,
    val isMasteringActive: Boolean = false,
    val isSpatialActive: Boolean = false,
    val engine: String = "",
    val appliedGainsDb: List<Float> = emptyList(),
    val message: String = "Waiting for playback"
)

class AndroidEqualizerController {
    private var equalizer: Equalizer? = null
    private var dynamics: DynamicsProcessing? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var sessionId: Int = 0

    fun applyEffects(
        audioSessionId: Int,
        eqProfile: EqualizerProfile,
        masteringProfile: MasteringProfile,
        useMastering: Boolean,
        spatialSettings: SpatialChamberSettings
    ): EqualizerRuntimeState {
        if (audioSessionId <= 0) {
            release()
            return EqualizerRuntimeState(message = "Start playback to attach Audio Effects")
        }

        if (Build.VERSION.SDK_INT >= 28 && dynamics != null && sessionId == audioSessionId) {
            return applyDynamics(audioSessionId, eqProfile, masteringProfile, useMastering, spatialSettings)
        }
        val activeEqualizer = equalizerFor(audioSessionId)
        if (activeEqualizer == null) {
            return if (Build.VERSION.SDK_INT >= 28) {
                applyDynamics(audioSessionId, eqProfile, masteringProfile, useMastering, spatialSettings)
            } else EqualizerRuntimeState(audioSessionId = audioSessionId,
                message = "Equalizer unavailable on this output")
        }

        return runCatching {
            check(activeEqualizer.hasControl()) { "Another app controls this equalizer" }
            val isEqEnabled = if (useMastering) masteringProfile.enabled else eqProfile.enabled
            val isSpatialEnabled = spatialSettings.isEnabled && spatialSettings.dryWetMix > 0.05f

            // 1. Calculate Base Gains and add Room Material Damping Coloration
            val baseGains = if (useMastering) masteringProfile.normalized().eqGainsDb else eqProfile.normalized().gainsDb
            val combinedGains = if (isSpatialEnabled) {
                applyWallDampingToGains(baseGains, spatialSettings.wallDamping)
            } else {
                baseGains
            }

            activeEqualizer.enabled = isEqEnabled || isSpatialEnabled
            if (activeEqualizer.enabled) {
                applyBandLevels(activeEqualizer, combinedGains)
            }

            // 2. Hardware BassBoost (Mastering sub-bass punch or Spatial room low-end resonance)
            val subBassDb = if (useMastering && masteringProfile.enabled) masteringProfile.subBassBoostDb else 0f
            val spatialBassBoost = if (isSpatialEnabled && (spatialSettings.preset == AcousticChamberPreset.TokyoVinylBar || spatialSettings.preset == AcousticChamberPreset.CyberpunkAlleyway)) 350 else 0
            attachBassBoost(audioSessionId, isEqEnabled || isSpatialEnabled, subBassDb, spatialBassBoost)

            // 3. Hardware Virtualizer (Binaural Crossfeed / Wide Soundstage)
            attachVirtualizer(
                audioSessionId = audioSessionId,
                useMastering = useMastering,
                masteringProfile = masteringProfile,
                spatialSettings = spatialSettings
            )

            // 4. Hardware Reverb (Acoustic Chamber simulation)
            attachPresetReverb(audioSessionId, spatialSettings)

            val statusMessage = buildStatusMessage(useMastering, masteringProfile, eqProfile, spatialSettings)

            EqualizerRuntimeState(
                active = isEqEnabled || isSpatialEnabled,
                engine = "Equalizer",
                appliedGainsDb = (0 until activeEqualizer.numberOfBands.toInt()).map {
                    activeEqualizer.getBandLevel(it.toShort()) / 100f
                },
                audioSessionId = audioSessionId,
                platformBandCount = activeEqualizer.numberOfBands.toInt(),
                isMasteringActive = useMastering && masteringProfile.enabled,
                isSpatialActive = isSpatialEnabled,
                message = statusMessage
            )
        }.getOrElse { error ->
            runCatching { activeEqualizer.enabled = false }
            EqualizerRuntimeState(
                audioSessionId = audioSessionId,
                message = error.message ?: "Equalizer could not be applied"
            )
        }
    }

    @RequiresApi(28)
    private fun applyDynamics(audioSessionId: Int, eqProfile: EqualizerProfile,
        masteringProfile: MasteringProfile, useMastering: Boolean,
        spatialSettings: SpatialChamberSettings): EqualizerRuntimeState {
        return runCatching {
            val effect = dynamics ?: DynamicsProcessing(1000, audioSessionId,
                DynamicsProcessing.Config.Builder(DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, true, 5, false, 0, false, 0, false).build()).also {
                dynamics = it; sessionId = audioSessionId
            }
            check(effect.hasControl()) { "Another app controls this equalizer" }
            val eqEnabled = if (useMastering) masteringProfile.enabled else eqProfile.enabled
            val spatialEnabled = spatialSettings.isEnabled && spatialSettings.dryWetMix > 0.05f
            val base = if (!eqEnabled) List(5) { 0f } else if (useMastering)
                masteringProfile.normalized().eqGainsDb else eqProfile.normalized().gainsDb
            val gains = if (spatialEnabled) applyWallDampingToGains(base, spatialSettings.wallDamping) else base
            // Contiguous frequency regions around the five existing EQ centers.
            val cutoffs = listOf(117f, 457f, 1810f, 7100f, 22000f)
            gains.forEachIndexed { band, gain ->
                effect.setPreEqBandAllChannelsTo(band, DynamicsProcessing.EqBand(true, cutoffs[band], gain))
                for (channel in 0 until effect.channelCount) {
                    check(abs(effect.getPreEqBandByChannelIndex(channel, band).gain - gain) < 0.01f) {
                        "Android did not apply equalizer band ${band + 1}"
                    }
                }
            }
            effect.enabled = eqEnabled || spatialEnabled
            attachBassBoost(audioSessionId, eqEnabled || spatialEnabled,
                if (useMastering && masteringProfile.enabled) masteringProfile.subBassBoostDb else 0f)
            attachVirtualizer(audioSessionId, useMastering, masteringProfile, spatialSettings)
            attachPresetReverb(audioSessionId, spatialSettings)
            EqualizerRuntimeState(active = effect.enabled, audioSessionId = audioSessionId,
                platformBandCount = 5, engine = "DynamicsProcessing",
                appliedGainsDb = (0 until 5).map { effect.getPreEqBandByChannelIndex(0, it).gain },
                isMasteringActive = useMastering && masteringProfile.enabled,
                isSpatialActive = spatialEnabled,
                message = buildStatusMessage(useMastering, masteringProfile, eqProfile, spatialSettings))
        }.getOrElse { error ->
            runCatching { dynamics?.enabled = false }
            EqualizerRuntimeState(audioSessionId = audioSessionId,
                message = error.message ?: "Equalizer unavailable on this output")
        }
    }

    fun applyProfile(audioSessionId: Int, profile: EqualizerProfile): EqualizerRuntimeState {
        return applyEffects(
            audioSessionId = audioSessionId,
            eqProfile = profile,
            masteringProfile = MasteringProfile(),
            useMastering = false,
            spatialSettings = SpatialChamberSettings(isEnabled = false)
        )
    }

    fun applyMasteringProfile(audioSessionId: Int, profile: MasteringProfile): EqualizerRuntimeState {
        return applyEffects(
            audioSessionId = audioSessionId,
            eqProfile = EqualizerProfile(),
            masteringProfile = profile,
            useMastering = true,
            spatialSettings = SpatialChamberSettings(isEnabled = false)
        )
    }

    fun applySpatialSettings(audioSessionId: Int, settings: SpatialChamberSettings): EqualizerRuntimeState {
        return applyEffects(
            audioSessionId = audioSessionId,
            eqProfile = EqualizerProfile(),
            masteringProfile = MasteringProfile(),
            useMastering = false,
            spatialSettings = settings
        )
    }

    fun release() {
        runCatching { equalizer?.release() }
        if (Build.VERSION.SDK_INT >= 28) runCatching { dynamics?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { presetReverb?.release() }
        equalizer = null
        dynamics = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        sessionId = 0
    }

    private fun equalizerFor(audioSessionId: Int): Equalizer? {
        if (equalizer != null && sessionId == audioSessionId) return equalizer
        release()
        return runCatching {
            Equalizer(1000, audioSessionId).also {
                equalizer = it
                sessionId = audioSessionId
            }
        }.recoverCatching {
            Equalizer(0, audioSessionId).also {
                equalizer = it
                sessionId = audioSessionId
            }
        }.getOrNull()
    }

    private fun attachBassBoost(audioSessionId: Int, enabled: Boolean, subBassDb: Float, fallbackStrength: Int = 0) {
        runCatching {
            if (bassBoost == null || sessionId != audioSessionId) {
                runCatching { bassBoost?.release() }
                bassBoost = runCatching { BassBoost(1000, audioSessionId) }
                    .recoverCatching { BassBoost(0, audioSessionId) }
                    .getOrNull()
            }
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    val hasSubBass = subBassDb > 0.05f
                    val hasFallback = fallbackStrength > 0
                    val shouldEnable = enabled && (hasSubBass || hasFallback)
                    bb.enabled = shouldEnable
                    if (shouldEnable) {
                        val strength = if (hasSubBass) {
                            ((subBassDb / MasteringProfile.MAX_SUB_BASS_DB) * 1000f).roundToInt()
                        } else {
                            fallbackStrength
                        }.coerceIn(0, 1000).toShort()
                        bb.setStrength(strength)
                    }
                }
            }
        }
    }

    private fun attachVirtualizer(
        audioSessionId: Int,
        useMastering: Boolean,
        masteringProfile: MasteringProfile,
        spatialSettings: SpatialChamberSettings
    ) {
        runCatching {
            if (virtualizer == null || sessionId != audioSessionId) {
                runCatching { virtualizer?.release() }
                virtualizer = runCatching { Virtualizer(1000, audioSessionId) }
                    .recoverCatching { Virtualizer(0, audioSessionId) }
                    .getOrNull()
            }
            virtualizer?.let { virt ->
                if (virt.strengthSupported) {
                    val isSpatialActive = spatialSettings.isEnabled &&
                            spatialSettings.dryWetMix > 0.05f &&
                            spatialSettings.crossfeedMode != BinauralCrossfeedMode.DirectStereoOff

                    val isMasteringSpatial = useMastering &&
                            masteringProfile.enabled &&
                            masteringProfile.spatialWidth > 1.05f

                    val shouldEnable = isSpatialActive || isMasteringSpatial
                    virt.enabled = shouldEnable

                    if (shouldEnable) {
                        val strength = if (isSpatialActive) {
                            val baseModeStrength = when (spatialSettings.crossfeedMode) {
                                BinauralCrossfeedMode.NaturalNearfield -> 650f
                                BinauralCrossfeedMode.WideAngleMaster -> 1000f
                                BinauralCrossfeedMode.IntimateHeadstage -> 400f
                                BinauralCrossfeedMode.DirectStereoOff -> 0f
                            }
                            val angleBonus = (abs(spatialSettings.speakerPosition.rightAngleDeg) / 45f) * 150f
                            ((baseModeStrength + angleBonus) * spatialSettings.dryWetMix)
                                .roundToInt().coerceIn(0, 1000).toShort()
                        } else {
                            (((masteringProfile.spatialWidth - 1.0f) / (MasteringProfile.MAX_SPATIAL_WIDTH - 1.0f)) * 1000f)
                                .roundToInt().coerceIn(0, 1000).toShort()
                        }
                        virt.setStrength(strength)
                    }
                }
            }
        }
    }

    private fun attachPresetReverb(audioSessionId: Int, spatialSettings: SpatialChamberSettings) {
        runCatching {
            if (presetReverb == null || sessionId != audioSessionId) {
                runCatching { presetReverb?.release() }
                presetReverb = runCatching { PresetReverb(1000, audioSessionId) }
                    .recoverCatching { PresetReverb(0, audioSessionId) }
                    .getOrNull()
            }
            presetReverb?.let { rev ->
                val shouldEnable = spatialSettings.isEnabled && spatialSettings.dryWetMix > 0.05f
                rev.enabled = shouldEnable
                if (shouldEnable) {
                    val targetPreset = when (spatialSettings.preset) {
                        AcousticChamberPreset.MinimalistTeahouse -> PresetReverb.PRESET_SMALLROOM
                        AcousticChamberPreset.TokyoVinylBar -> PresetReverb.PRESET_MEDIUMROOM
                        AcousticChamberPreset.AbbeyStudioControlRoom -> PresetReverb.PRESET_PLATE
                        AcousticChamberPreset.CyberpunkAlleyway -> PresetReverb.PRESET_LARGEHALL
                        AcousticChamberPreset.CathedralOfEchoes -> PresetReverb.PRESET_LARGEHALL
                        AcousticChamberPreset.CustomStudio -> {
                            if (spatialSettings.reverbDecaySeconds > 2.0f) PresetReverb.PRESET_LARGEHALL
                            else if (spatialSettings.reverbDecaySeconds > 0.8f) PresetReverb.PRESET_MEDIUMROOM
                            else PresetReverb.PRESET_SMALLROOM
                        }
                    }
                    runCatching { rev.preset = targetPreset }
                } else {
                    runCatching { rev.preset = PresetReverb.PRESET_NONE }
                }
            }
        }
    }

    private fun applyWallDampingToGains(baseGains: List<Float>, damping: WallMaterialDamping): List<Float> {
        val gains = baseGains.toMutableList()
        while (gains.size < 5) gains.add(0f)

        // Adjust 5 bands [60Hz, 250Hz, 910Hz, 3.6kHz, 14kHz]
        when (damping) {
            WallMaterialDamping.TeakWood -> {
                gains[1] = (gains[1] + 1.2f).coerceIn(-12f, 12f) // +1.2dB at 250Hz warm low-mids
                gains[4] = (gains[4] - 0.5f).coerceIn(-12f, 12f) // -0.5dB at 14kHz gentle rolloff
            }
            WallMaterialDamping.VelvetCurtain -> {
                gains[3] = (gains[3] - 1.2f).coerceIn(-12f, 12f) // -1.2dB at 3.6kHz
                gains[4] = (gains[4] - 2.5f).coerceIn(-12f, 12f) // -2.5dB at 14kHz high absorption
            }
            WallMaterialDamping.BrushedAluminum -> {
                gains[3] = (gains[3] + 1.0f).coerceIn(-12f, 12f) // +1.0dB at 3.6kHz
                gains[4] = (gains[4] + 2.0f).coerceIn(-12f, 12f) // +2.0dB at 14kHz high reflection
            }
            WallMaterialDamping.ConcreteStone -> {
                gains[2] = (gains[2] + 1.5f).coerceIn(-12f, 12f) // +1.5dB at 910Hz mid resonance
                gains[3] = (gains[3] + 0.8f).coerceIn(-12f, 12f) // +0.8dB at 3.6kHz
            }
            WallMaterialDamping.PorousAcousticFoam -> {
                // Linear studio reference
            }
        }
        return gains
    }

    private fun applyBandLevels(equalizer: Equalizer, gains: List<Float>) {
        val bandCount = equalizer.numberOfBands.toInt()
        val range = equalizer.bandLevelRange
        val minLevel = range.getOrNull(0)?.toInt() ?: -1200
        val maxLevel = range.getOrNull(1)?.toInt() ?: 1200

        for (bandIndex in 0 until bandCount) {
            val band = bandIndex.toShort()
            val platformCenterHz = (equalizer.getCenterFreq(band) / 1000).coerceAtLeast(20)
            val userBandIndex = EqualizerPreset.bandCentersHz
                .indices
                .minBy { index -> abs(EqualizerPreset.bandCentersHz[index] - platformCenterHz) }
            val targetGain = gains.getOrElse(userBandIndex) { 0f }
            val levelMb = (targetGain * 100f)
                .roundToInt()
                .coerceIn(minLevel, maxLevel)
                .toShort()
            equalizer.setBandLevel(band, levelMb)
            check(abs(equalizer.getBandLevel(band).toInt() - levelMb.toInt()) <= 1) {
                "Android did not apply equalizer band ${bandIndex + 1}"
            }
        }
    }

    private fun buildStatusMessage(
        useMastering: Boolean,
        masteringProfile: MasteringProfile,
        eqProfile: EqualizerProfile,
        spatialSettings: SpatialChamberSettings
    ): String {
        val parts = mutableListOf<String>()
        if (spatialSettings.isEnabled && spatialSettings.dryWetMix > 0.05f) {
            parts.add("3D Spatial (${spatialSettings.preset.title})")
        }
        if (useMastering && masteringProfile.enabled) {
            parts.add("Mastering (${masteringProfile.preset.displayName})")
        } else if (!useMastering && eqProfile.enabled) {
            parts.add("EQ (${eqProfile.preset.displayName})")
        }
        return if (parts.isNotEmpty()) parts.joinToString(" + ") else "AudioFX Bypassed"
    }
}
