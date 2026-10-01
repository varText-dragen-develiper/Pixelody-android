package com.pixelody.app.data.storage

import android.content.SharedPreferences
import com.pixelody.app.data.model.Playlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaylistStoreTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var store: PlaylistStore

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        store = PlaylistStore(fakePrefs)
    }

    @Test
    fun codecEncodesAndDecodesEmptyList() {
        val encoded = PlaylistCodec.encode(emptyList())
        val decoded = PlaylistCodec.decode(encoded)
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun codecEncodesAndDecodesPlaylistsWithDelimiters() {
        val playlists = listOf(
            Playlist(
                id = "p1",
                name = "Late Night: Chill, Grooves & Beats | Vol. 1",
                trackIds = listOf("t1", "t2", "t:special|track,3"),
                artworkUrl = "https://example.com/art.jpg"
            ),
            Playlist(
                id = "p2",
                name = "Empty Playlist",
                trackIds = emptyList(),
                artworkUrl = null
            )
        )
        val encoded = PlaylistCodec.encode(playlists)
        val decoded = PlaylistCodec.decode(encoded)

        assertEquals(2, decoded.size)
        assertEquals("p1", decoded[0].id)
        assertEquals("Late Night: Chill, Grooves & Beats | Vol. 1", decoded[0].name)
        assertEquals(listOf("t1", "t2", "t:special|track,3"), decoded[0].trackIds)
        assertEquals("https://example.com/art.jpg", decoded[0].artworkUrl)

        assertEquals("p2", decoded[1].id)
        assertEquals("Empty Playlist", decoded[1].name)
        assertTrue(decoded[1].trackIds.isEmpty())
        assertNull(decoded[1].artworkUrl)
    }

    @Test
    fun codecHandlesCorruptedDataGracefully() {
        val corrupted = "random|garbage|||;;;bad_format"
        val decoded = PlaylistCodec.decode(corrupted)
        assertTrue(decoded.isEmpty() || decoded.all { it.id.isNotBlank() })
    }

    @Test
    fun storeStartsEmpty() {
        assertFalse(store.hasPlaylists())
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun createPlaylistAddsNewPlaylist() {
        val created = store.createPlaylist("Focus Beats", listOf("track_42"))
        assertNotNull(created)
        assertEquals("Focus Beats", created.name)
        assertEquals(listOf("track_42"), created.trackIds)
        assertTrue(store.hasPlaylists())

        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertEquals(created.id, loaded[0].id)
        assertEquals("Focus Beats", loaded[0].name)
        assertEquals(listOf("track_42"), loaded[0].trackIds)
    }

    @Test
    fun addTrackToPlaylistAppendsWithoutDuplicates() {
        val created = store.createPlaylist("Favorites Mix", listOf("track_1"))
        val added = store.addTrackToPlaylist(created.id, "track_2")
        assertTrue(added)

        val updated = store.getPlaylist(created.id)
        assertEquals(listOf("track_1", "track_2"), updated?.trackIds)

        // Adding duplicate should return true and not duplicate
        val addedAgain = store.addTrackToPlaylist(created.id, "track_1")
        assertTrue(addedAgain)

        val updatedAgain = store.getPlaylist(created.id)
        assertEquals(listOf("track_1", "track_2"), updatedAgain?.trackIds)
    }

    @Test
    fun removeTrackFromPlaylistRemovesTrack() {
        val created = store.createPlaylist("Workout", listOf("track_1"))
        store.addTrackToPlaylist(created.id, "track_2")
        store.addTrackToPlaylist(created.id, "track_3")

        val removed = store.removeTrackFromPlaylist(created.id, "track_2")
        assertTrue(removed)

        val updated = store.getPlaylist(created.id)
        assertEquals(listOf("track_1", "track_3"), updated?.trackIds)
    }

    @Test
    fun deletePlaylistRemovesPlaylist() {
        val p1 = store.createPlaylist("Playlist 1", listOf("track_1"))
        val p2 = store.createPlaylist("Playlist 2", listOf("track_2"))
        assertEquals(2, store.load().size)

        val deleted = store.deletePlaylist(p1.id)
        assertTrue(deleted)
        assertEquals(1, store.load().size)
        assertNull(store.getPlaylist(p1.id))
        assertNotNull(store.getPlaylist(p2.id))
    }

    @Test
    fun renamePlaylistUpdatesName() {
        val p = store.createPlaylist("Old Name", listOf("track_1"))
        val renamed = store.renamePlaylist(p.id, "New Brand Name")
        assertTrue(renamed)
        assertEquals("New Brand Name", store.getPlaylist(p.id)?.name)

        // Blank name should fail
        val blankRenamed = store.renamePlaylist(p.id, "   ")
        assertFalse(blankRenamed)
        assertEquals("New Brand Name", store.getPlaylist(p.id)?.name)
    }

    @Test
    fun persistenceSurvivesReinitialization() {
        val p = store.createPlaylist("Road Trip", listOf("track_99"))
        store.addTrackToPlaylist(p.id, "track_100")

        val newStoreInstance = PlaylistStore(fakePrefs)
        val loaded = newStoreInstance.load()
        assertEquals(1, loaded.size)
        assertEquals(p.id, loaded[0].id)
        assertEquals("Road Trip", loaded[0].name)
        assertEquals(listOf("track_99", "track_100"), loaded[0].trackIds)
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
