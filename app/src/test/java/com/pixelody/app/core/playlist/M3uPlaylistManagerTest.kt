package com.pixelody.app.core.playlist

import com.pixelody.app.data.model.Track
import com.pixelody.app.data.storage.PlaylistStore
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class M3uPlaylistManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var playlistStore: PlaylistStore

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        playlistStore = PlaylistStore(fakePrefs)
    }

    @Test
    fun testExportToM3u() {
        val tracks = listOf(
            Track(id = "trk1", title = "Comfortably Numb", artist = "Pink Floyd", durationSeconds = 382, streamUrl = "https://example.com/stream/trk1"),
            Track(id = "trk2", title = "Time", artist = "Pink Floyd", durationSeconds = 425, streamUrl = "file:///storage/Time.flac")
        )

        val m3u = M3uPlaylistManager.exportToM3u("Classic Rock", tracks)
        assertTrue(m3u.startsWith("#EXTM3U\n#EXTPLAYLIST:Classic Rock"))
        assertTrue(m3u.contains("#EXTINF:382,Pink Floyd - Comfortably Numb\nhttps://example.com/stream/trk1"))
        assertTrue(m3u.contains("#EXTINF:425,Pink Floyd - Time\nfile:///storage/Time.flac"))
    }

    @Test
    fun testParseM3uExtended() {
        val content = """
            #EXTM3U
            #EXTPLAYLIST:Synthwave Favorites
            #EXTINF:240,Kavinsky - Nightcall
            https://stream.server/nightcall.mp3
            #EXTINF:180,Daft Punk - Technologic
            /sdcard/Music/technologic.flac
        """.trimIndent()

        val parsed = M3uPlaylistManager.parseM3u(content)
        assertEquals("Synthwave Favorites", parsed.name)
        assertEquals(2, parsed.tracks.size)

        assertEquals("Nightcall", parsed.tracks[0].title)
        assertEquals("Kavinsky", parsed.tracks[0].artist)
        assertEquals(240, parsed.tracks[0].durationSeconds)
        assertEquals("https://stream.server/nightcall.mp3", parsed.tracks[0].location)

        assertEquals("Technologic", parsed.tracks[1].title)
        assertEquals("Daft Punk", parsed.tracks[1].artist)
        assertEquals(180, parsed.tracks[1].durationSeconds)
        assertEquals("/sdcard/Music/technologic.flac", parsed.tracks[1].location)
    }

    @Test
    fun testParseM3uSimplePaths() {
        val content = """
            /sdcard/Music/SongA.mp3
            /sdcard/Music/SongB.flac
        """.trimIndent()

        val parsed = M3uPlaylistManager.parseM3u(content, defaultName = "Local Songs")
        assertEquals("Local Songs", parsed.name)
        assertEquals(2, parsed.tracks.size)
        assertEquals("SongA", parsed.tracks[0].title)
        assertEquals("/sdcard/Music/SongA.mp3", parsed.tracks[0].location)
    }

    @Test
    fun testMatchAndImport() {
        val known = listOf(
            Track(id = "trk1", title = "Nightcall", artist = "Kavinsky", streamUrl = "https://stream.server/nightcall.mp3"),
            Track(id = "trk2", title = "Technologic", artist = "Daft Punk", streamUrl = "/sdcard/Music/technologic.flac"),
            Track(id = "trk3", title = "Unrelated", artist = "Other")
        )

        val m3uContent = """
            #EXTM3U
            #EXTPLAYLIST:Retro
            #EXTINF:240,Kavinsky - Nightcall
            https://stream.server/nightcall.mp3
            #EXTINF:180,Daft Punk - Technologic
            /sdcard/Music/technologic.flac
        """.trimIndent()

        val parsed = M3uPlaylistManager.parseM3u(m3uContent)
        val imported = M3uPlaylistManager.matchAndImport(parsed, known, playlistStore)

        assertEquals("Retro", imported.name)
        assertEquals(listOf("trk1", "trk2"), imported.trackIds)
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)
        override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private var clear = false
            override fun putString(key: String?, value: String?): SharedPreferences.Editor { if (key != null) temp[key] = value; return this }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor { if (key != null) temp[key] = null; return this }
            override fun clear(): SharedPreferences.Editor { clear = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clear) storage.clear()
                temp.forEach { (k, v) -> if (v == null) storage.remove(k) else storage[k] = v }
                temp.clear()
            }
        }
    }
}
