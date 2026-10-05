package com.pixelody.app.data.storage

import android.content.SharedPreferences
import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MobileSettingsStoreTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var store: MobileSettingsStore

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        store = MobileSettingsStore(fakePrefs)
    }

    @Test
    fun testDefaultThemeIsStudio() {
        val theme = store.loadTheme()
        assertEquals(PixelodyMobileTheme.Studio, theme)
    }

    @Test
    fun testSaveAndLoadTheme() {
        store.saveTheme(PixelodyMobileTheme.Studio)
        assertEquals(PixelodyMobileTheme.Studio, store.loadTheme())
    }

    @Test
    fun testSaveAndLoadAllPortedThemes() {
        for (theme in PixelodyMobileTheme.values()) {
            store.saveTheme(theme)
            assertEquals(theme, store.loadTheme())
        }
    }

    @Test
    fun testResetTheme() {
        store.saveTheme(PixelodyMobileTheme.CartridgeQuest)
        assertEquals(PixelodyMobileTheme.CartridgeQuest, store.loadTheme())

        store.resetTheme()
        assertEquals(PixelodyMobileTheme.Studio, store.loadTheme())
    }

    @Test
    fun testStorageQuotaDefaultsAndPersistence() {
        assertEquals(MobileSettingsStore.DEFAULT_QUOTA_BYTES, store.loadStorageQuotaBytes())

        val tenGb = 10L * 1024L * 1024L * 1024L
        store.saveStorageQuotaBytes(tenGb)
        assertEquals(tenGb, store.loadStorageQuotaBytes())

        store.saveStorageQuotaBytes(MobileSettingsStore.UNLIMITED_QUOTA_BYTES)
        assertEquals(MobileSettingsStore.UNLIMITED_QUOTA_BYTES, store.loadStorageQuotaBytes())
    }

    @Test
    fun testAutoCacheFavorites() {
        assertFalse(store.loadAutoCacheFavorites())

        store.saveAutoCacheFavorites(true)
        assertTrue(store.loadAutoCacheFavorites())

        store.saveAutoCacheFavorites(false)
        assertFalse(store.loadAutoCacheFavorites())
    }

    @Test
    fun testAutoCacheRecentCount() {
        assertEquals(0, store.loadAutoCacheRecentCount())

        store.saveAutoCacheRecentCount(25)
        assertEquals(25, store.loadAutoCacheRecentCount())

        store.saveAutoCacheRecentCount(100)
        assertEquals(100, store.loadAutoCacheRecentCount())
    }

    @Test
    fun testDownloadWifiOnly() {
        assertFalse(store.loadDownloadWifiOnly())

        store.saveDownloadWifiOnly(true)
        assertTrue(store.loadDownloadWifiOnly())

        store.saveDownloadWifiOnly(false)
        assertFalse(store.loadDownloadWifiOnly())
    }

    @Test
    fun testLastTrackIdPersistence() {
        assertEquals(null, store.loadLastTrackId())

        store.saveLastTrackId("track-12345")
        assertEquals("track-12345", store.loadLastTrackId())

        store.saveLastTrackId(null)
        assertEquals(null, store.loadLastTrackId())
    }

    @Test
    fun testLastTrackPositionMsPersistence() {
        assertEquals(0L, store.loadLastTrackPositionMs())

        store.saveLastTrackPositionMs(45200L)
        assertEquals(45200L, store.loadLastTrackPositionMs())

        store.saveLastTrackPositionMs(-500L)
        assertEquals(0L, store.loadLastTrackPositionMs())
    }

    @Test
    fun testLastSourceScopePersistence() {
        assertEquals(null, store.loadLastSourceScope())

        store.saveLastSourceScope("Phone")
        assertEquals("Phone", store.loadLastSourceScope())

        store.saveLastSourceScope(null)
        assertEquals(null, store.loadLastSourceScope())
    }

    @Test
    fun testShufflePreferencesPersistence() {
        assertFalse(store.loadShuffleEnabled())
        assertEquals(null, store.loadShuffleMode())

        store.saveShuffleEnabled(true)
        store.saveShuffleMode("Smart Flow")

        assertTrue(store.loadShuffleEnabled())
        assertEquals("Smart Flow", store.loadShuffleMode())
    }

    @Test
    fun testRepeatModePersistence() {
        assertEquals(0, store.loadRepeatMode())

        store.saveRepeatMode(2)
        assertEquals(2, store.loadRepeatMode())
    }

    @Test
    fun testPlayerViewModePersistence() {
        assertEquals("Classic", store.loadPlayerViewMode())

        store.savePlayerViewMode("Turntable")
        assertEquals("Turntable", store.loadPlayerViewMode())

        store.savePlayerViewMode("Scope")
        assertEquals("Scope", store.loadPlayerViewMode())
    }

    @Test
    fun testFavoriteTrackIdsDefaultsAndToggle() {
        assertTrue(store.loadFavoriteTrackIds().isEmpty())
        assertFalse(store.isTrackFavorite("track_1"))

        val isFav = store.toggleTrackFavorite("track_1")
        assertTrue(isFav)
        assertTrue(store.isTrackFavorite("track_1"))
        assertEquals(setOf("track_1"), store.loadFavoriteTrackIds())

        val isFavAfterSecondToggle = store.toggleTrackFavorite("track_1")
        assertFalse(isFavAfterSecondToggle)
        assertFalse(store.isTrackFavorite("track_1"))
        assertTrue(store.loadFavoriteTrackIds().isEmpty())
    }

    @Test
    fun testSaveAndLoadFavoriteTrackIds() {
        val tracks = setOf("track_a", "track_b", "track_c")
        store.saveFavoriteTrackIds(tracks)
        assertEquals(tracks, store.loadFavoriteTrackIds())
        assertTrue(store.isTrackFavorite("track_b"))
        assertFalse(store.isTrackFavorite("track_x"))
    }

    @Test
    fun testPlaybackSpeedDefaultsAndPersistence() {
        assertEquals(1.0f, store.loadPlaybackSpeed(), 0.001f)

        store.savePlaybackSpeed(1.25f)
        assertEquals(1.25f, store.loadPlaybackSpeed(), 0.001f)

        store.savePlaybackSpeed(0.85f)
        assertEquals(0.85f, store.loadPlaybackSpeed(), 0.001f)
    }

    @Test
    fun testPitchLockedDefaultsAndPersistence() {
        assertTrue(store.loadPitchLocked())

        store.savePitchLocked(false)
        assertFalse(store.loadPitchLocked())

        store.savePitchLocked(true)
        assertTrue(store.loadPitchLocked())
    }

    @Test
    fun testLoudnessNormalizationDefaultsAndPersistence() {
        assertEquals(LoudnessNormalizationMode.Off, store.loadLoudnessNormalization())

        store.saveLoudnessNormalization(LoudnessNormalizationMode.StreamingStandard)
        assertEquals(LoudnessNormalizationMode.StreamingStandard, store.loadLoudnessNormalization())

        store.saveLoudnessNormalization(LoudnessNormalizationMode.AudiophileDynamic)
        assertEquals(LoudnessNormalizationMode.AudiophileDynamic, store.loadLoudnessNormalization())

        store.saveLoudnessNormalization(LoudnessNormalizationMode.Off)
        assertEquals(LoudnessNormalizationMode.Off, store.loadLoudnessNormalization())
    }

    @Test
    fun testAutoResumeOnHeadsetDefaultsAndPersistence() {
        assertFalse(store.loadAutoResumeOnHeadset())
        store.saveAutoResumeOnHeadset(true)
        assertTrue(store.loadAutoResumeOnHeadset())
        store.saveAutoResumeOnHeadset(false)
        assertFalse(store.loadAutoResumeOnHeadset())
    }

    @Test
    fun testExperienceModeDefaultsAndPersistence() {
        assertEquals(AppExperienceMode.Essential, store.loadExperienceMode())

        store.saveExperienceMode(AppExperienceMode.Studio)
        assertEquals(AppExperienceMode.Studio, store.loadExperienceMode())

        store.saveExperienceMode(AppExperienceMode.Essential)
        assertEquals(AppExperienceMode.Essential, store.loadExperienceMode())
    }

    @Test fun appearanceSurvivesStoreRecreationAndResetPreservesOtherSettings() {
        val appearance = com.pixelody.app.ui.theme.AppearanceSettings(
            palette = com.pixelody.app.ui.theme.StylePalette.Custom, primary = "#ffaa66",
            secondary = "66DDCC", base = "101219", textPercent = 130,
            typeface = com.pixelody.app.ui.theme.StyleTypeface.Serif, highContrast = true)
        store.saveTheme(PixelodyMobileTheme.Obsession)
        store.saveDownloadWifiOnly(true)
        store.saveAppearance(appearance)
        val reopened = MobileSettingsStore(fakePrefs)
        assertEquals(appearance.normalized(), reopened.loadAppearance())
        reopened.resetAppearance()
        assertEquals(com.pixelody.app.ui.theme.AppearanceSettings(), reopened.loadAppearance())
        assertEquals(PixelodyMobileTheme.Obsession, reopened.loadTheme())
        assertTrue(reopened.loadDownloadWifiOnly())
    }

    @Test fun corruptAppearanceValuesFallBackToReadableDefaults() {
        fakePrefs.edit().putString("style_palette", "missing")
            .putString("style_primary", "garbage").putString("style_base", "FFFFFF")
            .putInt("style_text_percent", 999).putString("style_typeface", "missing").apply()
        assertEquals(com.pixelody.app.ui.theme.AppearanceSettings(), store.loadAppearance())
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)

        override fun getString(key: String?, defValue: String?): String? =
            (map[key] as? String) ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (map[key] as? MutableSet<String>) ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int =
            (map[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (map[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (map[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (map[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = map.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(map)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
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
}
