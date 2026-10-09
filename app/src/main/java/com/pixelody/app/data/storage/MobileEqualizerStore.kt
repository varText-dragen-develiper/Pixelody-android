package com.pixelody.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.pixelody.app.data.model.AcousticChamberPreset
import com.pixelody.app.data.model.BinauralCrossfeedMode
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.MasteringPreset
import com.pixelody.app.data.model.MasteringProfile
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.SpatialSpeakerPosition
import com.pixelody.app.data.model.WallMaterialDamping
import org.json.JSONArray
import org.json.JSONObject

class MobileEqualizerStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    fun loadGlobalProfile(): EqualizerProfile {
        val raw = prefs.getString(KEY_GLOBAL_EQ, null) ?: return EqualizerProfile()
        return runCatching { profileFromJson(JSONObject(raw)) }
            .getOrDefault(EqualizerProfile())
    }

    fun saveGlobalProfile(profile: EqualizerProfile) {
        prefs.edit()
            .putString(KEY_GLOBAL_EQ, profileToJson(profile.normalized()).toString())
            .putLong(KEY_EQ_UPDATE_TRIGGER, System.currentTimeMillis())
            .apply()
    }

    fun loadTrackProfiles(): Map<String, EqualizerProfile> {
        val raw = prefs.getString(KEY_TRACK_EQ, null).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val profiles = mutableMapOf<String, EqualizerProfile>()
            root.keys().forEach { trackId ->
                profiles[trackId] = profileFromJson(root.getJSONObject(trackId))
            }
            profiles
        }.getOrDefault(emptyMap())
    }

    fun saveTrackProfiles(profiles: Map<String, EqualizerProfile>) {
        val root = JSONObject()
        profiles.forEach { (trackId, profile) ->
            root.put(trackId, profileToJson(profile.normalized()))
        }
        prefs.edit()
            .putString(KEY_TRACK_EQ, root.toString())
            .putLong(KEY_EQ_UPDATE_TRIGGER, System.currentTimeMillis())
            .apply()
    }

    // --- Studio Mastering Profiles ---

    fun loadGlobalMasteringProfile(): MasteringProfile {
        val raw = prefs.getString(KEY_GLOBAL_MASTERING, null) ?: return MasteringProfile()
        return runCatching { masteringProfileFromJson(JSONObject(raw)) }
            .getOrDefault(MasteringProfile())
    }

    fun saveGlobalMasteringProfile(profile: MasteringProfile) {
        prefs.edit()
            .putString(KEY_GLOBAL_MASTERING, masteringProfileToJson(profile.normalized()).toString())
            .putLong(KEY_EQ_UPDATE_TRIGGER, System.currentTimeMillis())
            .apply()
    }

    fun loadTrackMasteringProfiles(): Map<String, MasteringProfile> {
        val raw = prefs.getString(KEY_TRACK_MASTERING, null).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val profiles = mutableMapOf<String, MasteringProfile>()
            root.keys().forEach { trackId ->
                profiles[trackId] = masteringProfileFromJson(root.getJSONObject(trackId))
            }
            profiles
        }.getOrDefault(emptyMap())
    }

    fun saveTrackMasteringProfiles(profiles: Map<String, MasteringProfile>) {
        val root = JSONObject()
        profiles.forEach { (trackId, profile) ->
            root.put(trackId, masteringProfileToJson(profile.normalized()))
        }
        prefs.edit()
            .putString(KEY_TRACK_MASTERING, root.toString())
            .putLong(KEY_EQ_UPDATE_TRIGGER, System.currentTimeMillis())
            .apply()
    }

    fun loadUseMasteringRack(): Boolean {
        return prefs.getBoolean(KEY_USE_MASTERING, false)
    }

    fun saveUseMasteringRack(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_USE_MASTERING, enabled)
            .putLong(KEY_EQ_UPDATE_TRIGGER, System.currentTimeMillis())
            .apply()
    }

    fun loadAudioSessionId(): Int {
        return prefs.getInt(KEY_AUDIO_SESSION_ID, 0)
    }

    fun saveAudioSessionId(sessionId: Int) {
        if (prefs.getInt(KEY_AUDIO_SESSION_ID, 0) == sessionId) return
        prefs.edit()
            .putInt(KEY_AUDIO_SESSION_ID, sessionId)
            .putLong(KEY_EQ_UPDATE_TRIGGER, System.currentTimeMillis())
            .apply()
    }

    fun notifySettingsChanged() {
        prefs.edit()
            .putLong(KEY_EQ_UPDATE_TRIGGER, System.currentTimeMillis())
            .apply()
    }

    private fun profileToJson(profile: EqualizerProfile): JSONObject {
        val normalized = profile.normalized()
        return JSONObject()
            .put("enabled", normalized.enabled)
            .put("preset", normalized.preset.name)
            .put("gainsDb", JSONArray(normalized.gainsDb))
    }

    private fun profileFromJson(json: JSONObject): EqualizerProfile {
        val preset = EqualizerPreset.values().firstOrNull {
            it.name == json.optString("preset")
        } ?: EqualizerPreset.Flat
        val gains = json.optJSONArray("gainsDb")?.let { array ->
            List(array.length()) { index -> array.optDouble(index, 0.0).toFloat() }
        } ?: preset.gainsDb
        return EqualizerProfile(
            enabled = json.optBoolean("enabled", true),
            preset = preset,
            gainsDb = gains
        ).normalized()
    }

    private fun masteringProfileToJson(profile: MasteringProfile): JSONObject {
        val normalized = profile.normalized()
        return JSONObject()
            .put("enabled", normalized.enabled)
            .put("preset", normalized.preset.name)
            .put("eqGainsDb", JSONArray(normalized.eqGainsDb))
            .put("eqQFactors", JSONArray(normalized.eqQFactors))
            .put("tubeDrive", normalized.tubeDrive.toDouble())
            .put("tapeWarmth", normalized.tapeWarmth.toDouble())
            .put("spatialWidth", normalized.spatialWidth.toDouble())
            .put("subBassBoostDb", normalized.subBassBoostDb.toDouble())
            .put("limiterThresholdDb", normalized.limiterThresholdDb.toDouble())
            .put("autoGainEnabled", normalized.autoGainEnabled)
    }

    private fun masteringProfileFromJson(json: JSONObject): MasteringProfile {
        val preset = MasteringPreset.values().firstOrNull {
            it.name == json.optString("preset")
        } ?: MasteringPreset.AudiophileReference

        val gains = json.optJSONArray("eqGainsDb")?.let { array ->
            List(array.length()) { index -> array.optDouble(index, 0.0).toFloat() }
        } ?: preset.gainsDb

        val qs = json.optJSONArray("eqQFactors")?.let { array ->
            List(array.length()) { index -> array.optDouble(index, 1.4).toFloat() }
        } ?: preset.qFactors

        return MasteringProfile(
            enabled = json.optBoolean("enabled", true),
            preset = preset,
            eqGainsDb = gains,
            eqQFactors = qs,
            tubeDrive = json.optDouble("tubeDrive", preset.tubeDrive.toDouble()).toFloat(),
            tapeWarmth = json.optDouble("tapeWarmth", preset.tapeWarmth.toDouble()).toFloat(),
            spatialWidth = json.optDouble("spatialWidth", preset.spatialWidth.toDouble()).toFloat(),
            subBassBoostDb = json.optDouble("subBassBoostDb", preset.subBassBoostDb.toDouble()).toFloat(),
            limiterThresholdDb = json.optDouble("limiterThresholdDb", preset.limiterThresholdDb.toDouble()).toFloat(),
            autoGainEnabled = json.optBoolean("autoGainEnabled", true)
        ).normalized()
    }

    // --- Spatial Acoustic Chamber Settings ---

    fun loadSpatialSettings(): SpatialChamberSettings {
        val raw = prefs.getString(KEY_SPATIAL_SETTINGS, null) ?: return SpatialChamberSettings(isEnabled = false)
        return runCatching { spatialSettingsFromJson(JSONObject(raw)) }
            .getOrDefault(SpatialChamberSettings(isEnabled = false))
    }

    fun saveSpatialSettings(settings: SpatialChamberSettings) {
        prefs.edit()
            .putString(KEY_SPATIAL_SETTINGS, spatialSettingsToJson(settings).toString())
            .putLong(KEY_EQ_UPDATE_TRIGGER, System.currentTimeMillis())
            .apply()
    }

    private fun spatialSettingsToJson(settings: SpatialChamberSettings): JSONObject {
        return JSONObject()
            .put("isEnabled", settings.isEnabled)
            .put("preset", settings.preset.name)
            .put("crossfeedMode", settings.crossfeedMode.name)
            .put("speakerLeftAngle", settings.speakerPosition.leftAngleDeg.toDouble())
            .put("speakerRightAngle", settings.speakerPosition.rightAngleDeg.toDouble())
            .put("speakerDistance", settings.speakerPosition.distanceMeters.toDouble())
            .put("roomVolumeM3", settings.roomVolumeM3.toDouble())
            .put("wallDamping", settings.wallDamping.name)
            .put("reverbDecaySeconds", settings.reverbDecaySeconds.toDouble())
            .put("earlyReflectionGain", settings.earlyReflectionGain.toDouble())
            .put("diffuseTailGain", settings.diffuseTailGain.toDouble())
            .put("dryWetMix", settings.dryWetMix.toDouble())
            .put("headphoneProfileCompensation", settings.headphoneProfileCompensation)
    }

    private fun spatialSettingsFromJson(json: JSONObject): SpatialChamberSettings {
        val preset = AcousticChamberPreset.values().firstOrNull {
            it.name == json.optString("preset")
        } ?: AcousticChamberPreset.AbbeyStudioControlRoom

        val crossfeed = BinauralCrossfeedMode.values().firstOrNull {
            it.name == json.optString("crossfeedMode")
        } ?: BinauralCrossfeedMode.NaturalNearfield

        val damping = WallMaterialDamping.values().firstOrNull {
            it.name == json.optString("wallDamping")
        } ?: WallMaterialDamping.PorousAcousticFoam

        val speakerPos = SpatialSpeakerPosition(
            leftAngleDeg = json.optDouble("speakerLeftAngle", -preset.defaultSpeakerAngleDeg.toDouble()).toFloat(),
            rightAngleDeg = json.optDouble("speakerRightAngle", preset.defaultSpeakerAngleDeg.toDouble()).toFloat(),
            distanceMeters = json.optDouble("speakerDistance", 1.8).toFloat()
        )

        return SpatialChamberSettings(
            isEnabled = json.optBoolean("isEnabled", true),
            preset = preset,
            crossfeedMode = crossfeed,
            speakerPosition = speakerPos,
            roomVolumeM3 = json.optDouble("roomVolumeM3", preset.defaultVolumeM3.toDouble()).toFloat(),
            wallDamping = damping,
            reverbDecaySeconds = json.optDouble("reverbDecaySeconds", preset.defaultRt60Seconds.toDouble()).toFloat(),
            earlyReflectionGain = json.optDouble("earlyReflectionGain", 0.35).toFloat(),
            diffuseTailGain = json.optDouble("diffuseTailGain", 0.25).toFloat(),
            dryWetMix = json.optDouble("dryWetMix", 0.75).toFloat(),
            headphoneProfileCompensation = json.optBoolean("headphoneProfileCompensation", true)
        )
    }

    companion object {
        const val PREFERENCES_NAME = "pixelody_mobile_settings"
        const val KEY_GLOBAL_EQ = "equalizer_global_profile"
        const val KEY_TRACK_EQ = "equalizer_track_profiles"
        const val KEY_GLOBAL_MASTERING = "mastering_global_profile"
        const val KEY_TRACK_MASTERING = "mastering_track_profiles"
        const val KEY_USE_MASTERING = "use_mastering_rack"
        const val KEY_SPATIAL_SETTINGS = "spatial_chamber_settings"
        const val KEY_AUDIO_SESSION_ID = "playback_audio_session_id"
        const val KEY_EQ_UPDATE_TRIGGER = "eq_update_trigger"
    }
}
