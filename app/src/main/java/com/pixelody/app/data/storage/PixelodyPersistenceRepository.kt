package com.pixelody.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pixelody.app.data.model.CRATE_SLOT_COUNT
import com.pixelody.app.data.model.Crate
import com.pixelody.app.data.model.CrateCodec
import com.pixelody.app.data.model.CrateSlot
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.MasteringProfile
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class PixelodyDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SMART_CRATES (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                rules_json TEXT NOT NULL,
                slots_json TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CACHED_TRACKS (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT NOT NULL,
                duration_sec INTEGER NOT NULL,
                format TEXT NOT NULL,
                codec TEXT NOT NULL,
                lossless INTEGER NOT NULL,
                sample_rate INTEGER NOT NULL,
                bit_depth INTEGER,
                bitrate INTEGER,
                channels INTEGER NOT NULL,
                artwork_url TEXT,
                stream_url TEXT NOT NULL,
                is_favorite INTEGER NOT NULL,
                last_scanned INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CUSTOM_DSP (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                payload_json TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CAPSULES (
                date_key TEXT PRIMARY KEY,
                archetype TEXT NOT NULL,
                lossless_minutes INTEGER NOT NULL,
                total_tracks INTEGER NOT NULL,
                streak_days INTEGER NOT NULL,
                liner_notes TEXT NOT NULL,
                capsule_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SMART_CRATES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_TRACKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CUSTOM_DSP")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CAPSULES")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "pixelody_audiophile.db"
        const val DATABASE_VERSION = 1

        const val TABLE_SMART_CRATES = "smart_crates"
        const val TABLE_CACHED_TRACKS = "cached_tracks"
        const val TABLE_CUSTOM_DSP = "custom_dsp_presets"
        const val TABLE_CAPSULES = "capsule_history"
    }
}

/**
 * PixelodyPersistenceRepository: High-performance SQLite persistence manager for
 * smart crates, local media cache, DSP presets, and sonic capsule archives.
 */
class PixelodyPersistenceRepository(context: Context) {
    private val dbHelper = PixelodyDatabaseHelper(context)

    // --- Smart Crates Persistence ---

    suspend fun saveSmartCrate(
        crate: Crate,
        rulesJson: String = "{}"
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", crate.id)
                put("name", crate.name)
                put("rules_json", rulesJson)
                put("slots_json", serializeSlots(crate.slots))
                put("created_at", System.currentTimeMillis())
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                PixelodyDatabaseHelper.TABLE_SMART_CRATES,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            ) > 0
        }.getOrDefault(false)
    }

    suspend fun loadAllSmartCrates(): List<Crate> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Crate>()
        runCatching {
            val db = dbHelper.readableDatabase
            db.query(
                PixelodyDatabaseHelper.TABLE_SMART_CRATES,
                arrayOf("id", "name", "slots_json"),
                null,
                null,
                null,
                null,
                "updated_at DESC"
            ).use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow("id")
                val nameCol = cursor.getColumnIndexOrThrow("name")
                val slotsCol = cursor.getColumnIndexOrThrow("slots_json")
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol)
                    val name = cursor.getString(nameCol)
                    val slotsJson = cursor.getString(slotsCol)
                    val slots = deserializeSlots(slotsJson)
                    list.add(Crate(id = id, name = name, slots = slots))
                }
            }
        }
        list
    }

    suspend fun deleteSmartCrate(crateId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val db = dbHelper.writableDatabase
            db.delete(
                PixelodyDatabaseHelper.TABLE_SMART_CRATES,
                "id = ?",
                arrayOf(crateId)
            ) > 0
        }.getOrDefault(false)
    }

    // --- Local Tracks Cache Persistence ---

    suspend fun cacheScannedTracks(tracks: List<Track>): Unit = withContext(Dispatchers.IO) {
        runCatching {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                for (t in tracks) {
                    val values = ContentValues().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("artist", t.artist)
                        put("album", t.album)
                        put("duration_sec", t.durationSeconds)
                        put("format", t.format)
                        put("codec", t.codec)
                        put("lossless", if (t.lossless) 1 else 0)
                        put("sample_rate", t.sampleRate)
                        put("bit_depth", t.bitDepth)
                        put("bitrate", t.bitrate)
                        put("channels", t.channels)
                        put("artwork_url", t.artworkUrl)
                        put("stream_url", t.streamUrl)
                        put("is_favorite", if (t.favorite) 1 else 0)
                        put("last_scanned", System.currentTimeMillis())
                    }
                    db.insertWithOnConflict(
                        PixelodyDatabaseHelper.TABLE_CACHED_TRACKS,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    suspend fun loadCachedScannedTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        runCatching {
            val db = dbHelper.readableDatabase
            db.query(
                PixelodyDatabaseHelper.TABLE_CACHED_TRACKS,
                null,
                null,
                null,
                null,
                null,
                "title ASC"
            ).use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow("id")
                val titleCol = cursor.getColumnIndexOrThrow("title")
                val artistCol = cursor.getColumnIndexOrThrow("artist")
                val albumCol = cursor.getColumnIndexOrThrow("album")
                val durCol = cursor.getColumnIndexOrThrow("duration_sec")
                val formatCol = cursor.getColumnIndexOrThrow("format")
                val codecCol = cursor.getColumnIndexOrThrow("codec")
                val lossCol = cursor.getColumnIndexOrThrow("lossless")
                val sampleCol = cursor.getColumnIndexOrThrow("sample_rate")
                val bitDepthCol = cursor.getColumnIndexOrThrow("bit_depth")
                val bitrateCol = cursor.getColumnIndexOrThrow("bitrate")
                val chanCol = cursor.getColumnIndexOrThrow("channels")
                val artCol = cursor.getColumnIndexOrThrow("artwork_url")
                val streamCol = cursor.getColumnIndexOrThrow("stream_url")
                val favCol = cursor.getColumnIndexOrThrow("is_favorite")

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol)
                    val title = cursor.getString(titleCol)
                    val artist = cursor.getString(artistCol)
                    val album = cursor.getString(albumCol)
                    val durationSeconds = cursor.getInt(durCol)
                    val format = cursor.getString(formatCol)
                    val codec = cursor.getString(codecCol)
                    val lossless = cursor.getInt(lossCol) == 1
                    val sampleRate = cursor.getInt(sampleCol)
                    val bitDepth = if (!cursor.isNull(bitDepthCol)) cursor.getInt(bitDepthCol) else null
                    val bitrate = if (!cursor.isNull(bitrateCol)) cursor.getInt(bitrateCol) else null
                    val channels = cursor.getInt(chanCol)
                    val artworkUrl = cursor.getString(artCol)
                    val streamUrl = cursor.getString(streamCol)
                    val favorite = cursor.getInt(favCol) == 1

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationSeconds = durationSeconds,
                            format = format,
                            codec = codec,
                            lossless = lossless,
                            sampleRate = sampleRate,
                            bitDepth = bitDepth,
                            bitrate = bitrate,
                            channels = channels,
                            replayGainDb = null,
                            artworkUrl = artworkUrl,
                            streamUrl = streamUrl,
                            favorite = favorite,
                            missing = false
                        )
                    )
                }
            }
        }
        tracks
    }

    // --- Custom DSP Presets Persistence ---

    suspend fun saveCustomDspPreset(id: String, name: String, category: String, payloadJson: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", id)
                put("name", name)
                put("category", category)
                put("payload_json", payloadJson)
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                PixelodyDatabaseHelper.TABLE_CUSTOM_DSP,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            ) > 0
        }.getOrDefault(false)
    }

    // --- Serialization Helpers ---

    private fun serializeSlots(slots: List<CrateSlot?>): String {
        val array = JSONArray()
        for (slot in slots) {
            when (slot) {
                null -> array.put("")
                is CrateSlot.Collection -> array.put("c~${slot.kindKey}~${slot.collectionId}")
                is CrateSlot.SingleTrack -> array.put("t~${slot.trackId}")
                is CrateSlot.PlayerView -> array.put("v~${slot.viewName}")
                is CrateSlot.Lens -> array.put("l~${slot.label}~${slot.sourceKey}~${slot.filterKey}")
                is CrateSlot.SessionPreset -> array.put("s~${slot.label}")
                is CrateSlot.Unknown -> array.put(slot.raw)
            }
        }
        return array.toString()
    }

    private fun deserializeSlots(json: String): List<CrateSlot?> {
        val list = mutableListOf<CrateSlot?>()
        runCatching {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val item = array.optString(i, "")
                if (item.isBlank()) {
                    list.add(null)
                } else {
                    val parts = item.split("~")
                    when (parts.firstOrNull()) {
                        "c" -> list.add(if (parts.size >= 3) CrateSlot.Collection(parts[1], parts[2]) else null)
                        "t" -> list.add(if (parts.size >= 2) CrateSlot.SingleTrack(parts[1]) else null)
                        "v" -> list.add(if (parts.size >= 2) CrateSlot.PlayerView(parts[1]) else null)
                        "l" -> list.add(if (parts.size >= 4) CrateSlot.Lens(parts[1], parts[2], parts[3]) else null)
                        "s" -> list.add(if (parts.size >= 2) CrateSlot.SessionPreset(parts[1], emptyMap()) else null)
                        else -> list.add(CrateSlot.Unknown(item))
                    }
                }
            }
        }
        return if (list.size == CRATE_SLOT_COUNT) list else (list + List(CRATE_SLOT_COUNT - list.size) { null }).take(CRATE_SLOT_COUNT)
    }
}
