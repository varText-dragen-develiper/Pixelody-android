package com.pixelody.app.data.storage

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import android.content.Context
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.util.UUID

/** Each test uses its own database name. Never opens or clears the owner's database. */
class TrackMetadataMigrationRuntimeTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext.also {
        assumeTrue("Only run in the isolated shuffle package", it.packageName.endsWith(".shuffleqa"))
    }

    @Test fun openingExistingV1AddsMetadataWithoutDeletingAnyOldTable() {
        val name = "metadata-migration-${UUID.randomUUID()}.db"
        try {
            val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                PixelodyDatabaseHelper(context, name).onCreate(db)
                db.execSQL("INSERT INTO smart_crates VALUES ('crate','Saved crate','{}','[]',1,1)")
                db.execSQL("INSERT INTO cached_tracks VALUES ('track','Title','Artist','Album',120,'FLAC','flac',1,44100,16,NULL,2,NULL,'content://sentinel',1,1)")
                db.execSQL("INSERT INTO custom_dsp_presets VALUES ('dsp','Saved DSP','eq','{}',1)")
                db.execSQL("INSERT INTO capsule_history VALUES ('day','archetype',20,1,2,'Saved notes','{}',1)")
                db.version = 1
            }
            PixelodyDatabaseHelper(context, name).use { helper ->
                val db = helper.writableDatabase
                assertEquals(1, db.version)
                listOf("smart_crates", "cached_tracks", "custom_dsp_presets", "capsule_history").forEach { table ->
                    db.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); assertEquals(table, 1, it.getInt(0)) }
                }
                db.rawQuery("SELECT COUNT(*) FROM cached_track_metadata_v1", null).use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
                try { helper.onUpgrade(db, 1, 2); fail("Unknown upgrade must fail closed") } catch (_: android.database.sqlite.SQLiteException) { }
                db.rawQuery("SELECT name FROM smart_crates WHERE id='crate'", null).use { it.moveToFirst(); assertEquals("Saved crate", it.getString(0)) }
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun futureSchemaIsRejectedAndRetained() {
        val name = "metadata-future-${UUID.randomUUID()}.db"
        try {
            PixelodyDatabaseHelper(context, name).use { helper ->
                helper.writableDatabase.apply { execSQL("INSERT INTO smart_crates VALUES ('future','Keep','{}','[]',1,1)"); version = 99 }
            }
            PixelodyDatabaseHelper(context, name).use { helper ->
                try { helper.writableDatabase; fail("Future schema must not be downgraded") } catch (_: android.database.sqlite.SQLiteException) { }
            }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                assertEquals(99, db.version)
                db.rawQuery("SELECT name FROM smart_crates WHERE id='future'", null).use { it.moveToFirst(); assertEquals("Keep", it.getString(0)) }
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun cachedMetadataSurvivesRepositoryRecreationWithoutSchemaVersionBump() = runBlocking {
        val name = "metadata-roundtrip-${UUID.randomUUID()}.db"
        val tracks = listOf(Track("z", "Z", streamUrl = "content://z", albumArtist = "Various Artists", discNumber = 2, trackNumber = 3,
            albumId = "album-id", recordingId = "recording-id", musicalKey = "G♭ major", bpm = 123.5f, genre = "Jazz"),
            Track("a", "A", streamUrl = "content://a"))
        try {
            PixelodyPersistenceRepository(context, name).apply { cacheScannedTracks(tracks); close() }
            val repo = PixelodyPersistenceRepository(context, name)
            try { assertEquals(tracks, repo.loadCachedScannedTracks()) } finally { repo.close() }
            PixelodyDatabaseHelper(context, name).use { assertEquals(1, it.readableDatabase.version) }
        } finally { context.deleteDatabase(name) }
    }
}
