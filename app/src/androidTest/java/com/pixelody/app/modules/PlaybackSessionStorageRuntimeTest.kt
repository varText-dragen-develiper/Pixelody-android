package com.pixelody.app.modules

import androidx.test.platform.app.InstrumentationRegistry
import com.pixelody.app.data.storage.PixelodyDatabaseHelper
import com.pixelody.app.data.storage.PlaybackQueueOccurrence
import com.pixelody.app.data.storage.PlaybackSessionCheckpoint
import com.pixelody.app.data.storage.SessionLoadStatus
import com.pixelody.app.data.storage.VersionedPlaybackSessionStore
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/** Only the isolated package may seed fixtures. The regular owner app is never modified. */
class PlaybackSessionStorageRuntimeTest {
    @Test fun dedicatedCheckpointRecoveryPreservesSentinelsInEveryExistingDatabaseTable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Requires an isolated QA package", context.packageName.endsWith(".shuffleqa") || context.packageName.endsWith(".studioqa"))
        val id = "session-storage-sentinel-${UUID.randomUUID()}"
        val helper = PixelodyDatabaseHelper(context)
        val db = helper.writableDatabase
        val initialVersion = db.version
        val tables = listOf(
            PixelodyDatabaseHelper.TABLE_SMART_CRATES to "id",
            PixelodyDatabaseHelper.TABLE_CACHED_TRACKS to "id",
            PixelodyDatabaseHelper.TABLE_CUSTOM_DSP to "id",
            PixelodyDatabaseHelper.TABLE_CAPSULES to "date_key"
        )
        try {
            db.execSQL("INSERT INTO smart_crates VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf(id, "Sentinel crate", "{\"preserve\":true}", "[]", 12, 13))
            db.execSQL("""INSERT INTO cached_tracks
                (id,title,artist,album,duration_sec,format,codec,lossless,sample_rate,bit_depth,bitrate,
                 channels,artwork_url,stream_url,is_favorite,last_scanned)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                arrayOf(id, "Sentinel track", "Artist", "Album", 71, "FLAC", "flac", 1, 48000, 24,
                    null, 2, "content://fixture/art", "content://fixture/song", 1, 14))
            db.execSQL("INSERT INTO custom_dsp_presets VALUES (?, ?, ?, ?, ?)",
                arrayOf(id, "Sentinel DSP", "test", "{\"gain\":-2}", 15))
            db.execSQL("INSERT INTO capsule_history VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf(id, "Sentinel capsule", 6, 7, 8, "Preserve notes", "{\"tracks\":7}", 16))
            fun snapshot(): List<List<String?>> = tables.map { (table, key) ->
                db.rawQuery("SELECT * FROM $table WHERE $key = ?", arrayOf(id)).use { cursor ->
                    assertTrue("Sentinel missing in $table", cursor.moveToFirst())
                    (0 until cursor.columnCount).map { if (cursor.isNull(it)) null else cursor.getString(it) }
                }
            }
            val before = snapshot()
            val directory = File(context.filesDir, id)
            val store = VersionedPlaybackSessionStore(directory)
            val checkpoint = PlaybackSessionCheckpoint(id, 1,
                listOf(PlaybackQueueOccurrence("first", "same-track", "content://fixture/song"),
                    PlaybackQueueOccurrence("second", "same-track", "content://fixture/song")),
                currentIndex = 1, positionMs = 9123, scopeId = "sentinel-scope", playWhenReady = true)
            assertTrue(store.save(checkpoint).saved)
            assertTrue(store.save(checkpoint.copy(revision = 2)).saved)
            store.primaryFile.writeText("{interrupted")
            val restored = VersionedPlaybackSessionStore(directory).load()
            assertEquals(SessionLoadStatus.RECOVERED_BACKUP, restored.status)
            assertEquals(1L, restored.checkpoint!!.revision)
            assertFalse(restored.checkpoint!!.playWhenReady)
            assertEquals(listOf("first", "second"), restored.checkpoint!!.queue.map { it.entryId })
            assertTrue(store.save(checkpoint.copy(revision = 3)).saved)
            val future = """{"schemaVersion":99,"unknown":"preserve"}"""
            store.primaryFile.writeText(future)
            assertTrue(store.load().readOnly)
            assertFalse(store.save(checkpoint.copy(revision = 4)).saved)
            assertEquals(future, store.primaryFile.readText())
            assertEquals(initialVersion, db.version)
            assertEquals("No existing table content may change", before, snapshot())
            // Reopening the real helper must still leave every old table and row intact.
            PixelodyDatabaseHelper(context).use { reopened ->
                assertEquals(initialVersion, reopened.readableDatabase.version)
                tables.forEach { (table, key) ->
                    reopened.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table WHERE $key = ?", arrayOf(id)).use {
                        assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
                    }
                }
            }
        } finally {
            // Only this run's unique rows; never delete any owner's music/settings/history.
            tables.forEach { (table, key) -> db.delete(table, "$key = ?", arrayOf(id)) }
            helper.close()
        }
    }
}
