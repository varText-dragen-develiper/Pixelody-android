package com.pixelody.app.data.storage

import android.content.SharedPreferences
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TrackMetadataStoreTest {

    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(data)
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (data[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class FakeEditor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removes = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removes.add(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearAll) data.clear()
                removes.forEach { data.remove(it) }
                pending.forEach { (k, v) -> data[k] = v }
            }
        }
    }

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var store: TrackMetadataStore

    private fun sampleTrack(id: String = "track_1") = Track(
        id = id,
        title = "Original Title",
        artist = "Original Artist",
        album = "Original Album",
        genre = "Ambient",
        durationSeconds = 200,
        format = "FLAC",
        codec = "FLAC",
        lossless = true,
        sampleRate = 48000,
        bitDepth = 24,
        bitrate = null,
        channels = 2,
        replayGainDb = null,
        artworkUrl = null,
        streamUrl = "http://localhost/audio/$id",
        favorite = false,
        missing = false
    )

    @Before
    fun setup() {
        prefs = FakeSharedPreferences()
        store = TrackMetadataStore(prefs)
    }

    @Test
    fun `saveOverride updates StateFlow and persists to SharedPreferences`() {
        store.saveOverride(
            trackId = "track_1",
            title = "Custom Remaster",
            artist = "New Artist",
            album = "New Album",
            genre = "Electronic"
        )

        val override = store.getOverride("track_1")
        assertNotNull(override)
        assertEquals("Custom Remaster", override?.title)
        assertEquals("New Artist", override?.artist)
        assertEquals("New Album", override?.album)
        assertEquals("Electronic", override?.genre)

        // Verify loaded in new store instance from same prefs
        val reloadedStore = TrackMetadataStore(prefs)
        val persisted = reloadedStore.getOverride("track_1")
        assertNotNull(persisted)
        assertEquals("Custom Remaster", persisted?.title)
        assertEquals("Electronic", persisted?.genre)
    }

    @Test
    fun `applyOverride updates track fields while preserving untouched fields`() {
        store.saveOverride(
            trackId = "track_1",
            title = "Updated Title",
            artist = null,
            album = null,
            genre = "Synthwave"
        )

        val original = sampleTrack("track_1")
        val applied = store.applyOverride(original)

        assertEquals("Updated Title", applied.title)
        assertEquals("Original Artist", applied.artist) // unchanged
        assertEquals("Original Album", applied.album)   // unchanged
        assertEquals("Synthwave", applied.genre)        // overridden
        assertEquals(24, applied.bitDepth)              // untouched audio spec
    }

    @Test
    fun `deleteOverride removes override and restores base track`() {
        store.saveOverride(
            trackId = "track_1",
            title = "Modified",
            artist = "Modified",
            album = "Modified",
            genre = "Rock"
        )
        assertNotNull(store.getOverride("track_1"))

        store.deleteOverride("track_1")
        assertNull(store.getOverride("track_1"))

        val track = sampleTrack("track_1")
        val applied = store.applyOverride(track)
        assertEquals("Original Title", applied.title)
    }
}
