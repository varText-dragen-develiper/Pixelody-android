package com.pixelody.app.data.storage

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MobileEqualizerStoreTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var store: MobileEqualizerStore

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        store = MobileEqualizerStore(fakePrefs)
    }

    @Test
    fun loadGlobalProfileReturnsDefaultWhenEmpty() {
        val profile = store.loadGlobalProfile()
        assertNotNull(profile)
        assertTrue(profile.enabled)
        assertEquals(EqualizerPreset.Flat, profile.preset)
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), profile.gainsDb)
    }

    @Test
    fun saveAndLoadGlobalProfilePersistsCorrectly() {
        val customProfile = EqualizerProfile(
            enabled = true,
            preset = EqualizerPreset.Bass,
            gainsDb = listOf(5.0f, 3.5f, 0.0f, -1.0f, 2.0f)
        )
        store.saveGlobalProfile(customProfile)

        val loaded = store.loadGlobalProfile()
        assertTrue(loaded.enabled)
        assertEquals(EqualizerPreset.Bass, loaded.preset)
        assertEquals(5, loaded.gainsDb.size)
        assertEquals(5.0f, loaded.gainsDb[0], 0.001f)
        assertEquals(3.5f, loaded.gainsDb[1], 0.001f)
        assertEquals(0.0f, loaded.gainsDb[2], 0.001f)
        assertEquals(-1.0f, loaded.gainsDb[3], 0.001f)
        assertEquals(2.0f, loaded.gainsDb[4], 0.001f)
        assertTrue(fakePrefs.getLong(MobileEqualizerStore.KEY_EQ_UPDATE_TRIGGER, 0L) > 0L)
    }

    @Test
    fun saveAndLoadTrackProfiles() {
        val track1Profile = EqualizerProfile(
            enabled = true,
            preset = EqualizerPreset.Vocal,
            gainsDb = listOf(-2.0f, 1.0f, 4.0f, 2.5f, 1.0f)
        )
        val track2Profile = EqualizerProfile(
            enabled = false,
            preset = EqualizerPreset.Focus,
            gainsDb = listOf(4.0f, 2.0f, 0.0f, 2.0f, 4.0f)
        )

        val profilesMap = mapOf(
            "trk_001" to track1Profile,
            "trk_002" to track2Profile
        )
        store.saveTrackProfiles(profilesMap)

        val loadedMap = store.loadTrackProfiles()
        assertEquals(2, loadedMap.size)
        assertTrue(loadedMap.containsKey("trk_001"))
        assertTrue(loadedMap.containsKey("trk_002"))

        val p1 = loadedMap["trk_001"]!!
        assertTrue(p1.enabled)
        assertEquals(EqualizerPreset.Vocal, p1.preset)
        assertEquals(4.0f, p1.gainsDb[2], 0.001f)

        val p2 = loadedMap["trk_002"]!!
        assertFalse(p2.enabled)
        assertEquals(EqualizerPreset.Focus, p2.preset)
    }

    @Test
    fun loadGlobalMasteringProfileReturnsDefaultWhenEmpty() {
        val profile = store.loadGlobalMasteringProfile()
        assertNotNull(profile)
        assertTrue(profile.enabled)
        assertEquals(MasteringPreset.AudiophileReference, profile.preset)
        assertEquals(1.0f, profile.spatialWidth, 0.001f)
        assertEquals(0.0f, profile.tubeDrive, 0.001f)
        assertEquals(0.0f, profile.subBassBoostDb, 0.001f)
        assertEquals(-0.5f, profile.limiterThresholdDb, 0.001f)
        assertTrue(profile.autoGainEnabled)
    }

    @Test
    fun saveAndLoadGlobalMasteringProfilePersistsAllParameters() {
        val customMastering = MasteringProfile(
            enabled = true,
            preset = MasteringPreset.AnalogTapeWarmth,
            eqGainsDb = listOf(1.5f, -0.5f, 0.0f, 1.0f, 2.5f),
            eqQFactors = listOf(1.2f, 1.4f, 1.0f, 1.8f, 2.0f),
            tubeDrive = 0.45f,
            tapeWarmth = 0.8f,
            spatialWidth = 1.35f,
            subBassBoostDb = 3.2f,
            limiterThresholdDb = -1.5f,
            autoGainEnabled = false
        )
        store.saveGlobalMasteringProfile(customMastering)

        val loaded = store.loadGlobalMasteringProfile()
        assertTrue(loaded.enabled)
        assertEquals(MasteringPreset.AnalogTapeWarmth, loaded.preset)
        assertEquals(1.5f, loaded.eqGainsDb[0], 0.001f)
        assertEquals(1.2f, loaded.eqQFactors[0], 0.001f)
        assertEquals(0.45f, loaded.tubeDrive, 0.001f)
        assertEquals(0.8f, loaded.tapeWarmth, 0.001f)
        assertEquals(1.35f, loaded.spatialWidth, 0.001f)
        assertEquals(3.2f, loaded.subBassBoostDb, 0.001f)
        assertEquals(-1.5f, loaded.limiterThresholdDb, 0.001f)
        assertFalse(loaded.autoGainEnabled)
    }

    @Test
    fun saveAndLoadTrackMasteringProfiles() {
        val map = mapOf(
            "trk_vinyl" to MasteringProfile(
                enabled = true,
                preset = MasteringPreset.TubeVibeStudio,
                tubeDrive = 0.6f
            )
        )
        store.saveTrackMasteringProfiles(map)

        val loaded = store.loadTrackMasteringProfiles()
        assertEquals(1, loaded.size)
        assertEquals(MasteringPreset.TubeVibeStudio, loaded["trk_vinyl"]?.preset)
        assertEquals(0.6f, loaded["trk_vinyl"]?.tubeDrive ?: 0f, 0.001f)
    }

    @Test
    fun masteringRackToggleAndSessionIdPersistence() {
        assertFalse(store.loadUseMasteringRack())
        store.saveUseMasteringRack(true)
        assertTrue(store.loadUseMasteringRack())

        assertEquals(0, store.loadAudioSessionId())
        store.saveAudioSessionId(42)
        assertEquals(42, store.loadAudioSessionId())
    }

    @Test
    fun saveAndLoadSpatialChamberSettings() {
        val spatial = SpatialChamberSettings(
            isEnabled = true,
            preset = AcousticChamberPreset.CathedralOfEchoes,
            crossfeedMode = BinauralCrossfeedMode.WideAngleMaster,
            speakerPosition = SpatialSpeakerPosition(
                leftAngleDeg = -45f,
                rightAngleDeg = 45f,
                distanceMeters = 2.4f
            ),
            roomVolumeM3 = 3500f,
            wallDamping = WallMaterialDamping.ConcreteStone,
            reverbDecaySeconds = 3.5f,
            earlyReflectionGain = 0.45f,
            diffuseTailGain = 0.38f,
            dryWetMix = 0.85f,
            headphoneProfileCompensation = true
        )
        store.saveSpatialSettings(spatial)

        val loaded = store.loadSpatialSettings()
        assertTrue(loaded.isEnabled)
        assertEquals(AcousticChamberPreset.CathedralOfEchoes, loaded.preset)
        assertEquals(BinauralCrossfeedMode.WideAngleMaster, loaded.crossfeedMode)
        assertEquals(-45f, loaded.speakerPosition.leftAngleDeg, 0.001f)
        assertEquals(45f, loaded.speakerPosition.rightAngleDeg, 0.001f)
        assertEquals(2.4f, loaded.speakerPosition.distanceMeters, 0.001f)
        assertEquals(3500f, loaded.roomVolumeM3, 0.001f)
        assertEquals(WallMaterialDamping.ConcreteStone, loaded.wallDamping)
        assertEquals(3.5f, loaded.reverbDecaySeconds, 0.001f)
        assertEquals(0.45f, loaded.earlyReflectionGain, 0.001f)
        assertEquals(0.38f, loaded.diffuseTailGain, 0.001f)
        assertEquals(0.85f, loaded.dryWetMix, 0.001f)
        assertTrue(loaded.headphoneProfileCompensation)
    }

    @Test
    fun freshInstallDoesNotEnableRetiredRoomProcessing() {
        assertFalse(store.loadSpatialSettings().isEnabled)
    }

    @Test
    fun corruptedJsonRecoversGracefullyToDefaults() {
        fakePrefs.putString(MobileEqualizerStore.KEY_GLOBAL_EQ, "{ broken json }")
        fakePrefs.putString(MobileEqualizerStore.KEY_GLOBAL_MASTERING, "not-a-json")
        fakePrefs.putString(MobileEqualizerStore.KEY_SPATIAL_SETTINGS, "[[")
        fakePrefs.putString(MobileEqualizerStore.KEY_TRACK_EQ, "invalid")

        val eq = store.loadGlobalProfile()
        assertEquals(EqualizerPreset.Flat, eq.preset)

        val mastering = store.loadGlobalMasteringProfile()
        assertEquals(MasteringPreset.AudiophileReference, mastering.preset)

        val spatial = store.loadSpatialSettings()
        assertEquals(AcousticChamberPreset.AbbeyStudioControlRoom, spatial.preset)
        assertFalse(spatial.isEnabled)

        val trackMap = store.loadTrackProfiles()
        assertTrue(trackMap.isEmpty())
    }

    @Test
    fun notifySettingsChangedUpdatesTriggerTimestamp() {
        val before = System.currentTimeMillis()
        store.notifySettingsChanged()
        val trigger = fakePrefs.getLong(MobileEqualizerStore.KEY_EQ_UPDATE_TRIGGER, 0L)
        assertTrue(trigger >= before)
    }
}

/**
 * In-memory test double for Android SharedPreferences
 */
class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any>()

    fun putString(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }

    override fun getAll(): MutableMap<String, *> = HashMap(map)

    override fun getString(key: String?, defValue: String?): String? {
        return (map[key] as? String) ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        return (map[key] as? MutableSet<String>) ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return (map[key] as? Int) ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return (map[key] as? Long) ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return (map[key] as? Float) ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return (map[key] as? Boolean) ?: defValue
    }

    override fun contains(key: String?): Boolean = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(map)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val storage: MutableMap<String, Any>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) storage.clear()
            temp.forEach { (k, v) ->
                if (v == null) storage.remove(k) else storage[k] = v
            }
            temp.clear()
        }
    }
}
