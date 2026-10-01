package com.pixelody.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pixelody.app.data.model.CoverBook
import com.pixelody.app.data.model.CoverCodec
import java.io.File

/**
 * Remembers the pictures and notes a person attached to crates, playlists, albums and
 * tracks. Pictures are copied, scaled down, into the app's own folder so they keep
 * working if the original is moved or deleted.
 */
class CoverStore(
    private val prefs: SharedPreferences,
    private val coverDir: File
) {

    constructor(context: Context) : this(
        context.getSharedPreferences("pixelody_covers", Context.MODE_PRIVATE),
        File(context.filesDir, "pixelody_covers")
    )

    fun load(): CoverBook = CoverCodec.decode(prefs.getString(KEY_BOOK, null))

    fun save(book: CoverBook) {
        prefs.edit().putString(KEY_BOOK, CoverCodec.encode(book)).apply()
    }

    /**
     * Copies [source] into app storage as a square-friendly JPEG no larger than
     * [MAX_EDGE] on its longest side. Returns the new `file:` address, or null if the
     * picture could not be read. Call off the main thread.
     */
    fun importImage(context: Context, source: Uri): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_EDGE && bounds.outHeight / (sample * 2) >= MAX_EDGE) {
            sample *= 2
        }
        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, decode)
        } ?: throw IllegalStateException("picture could not be read")
        coverDir.mkdirs()
        // A new name every time, because the bitmap cache is keyed by address and would
        // otherwise keep showing the picture that was just replaced.
        val target = File(coverDir, "cover-${System.currentTimeMillis()}.jpg")
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        Uri.fromFile(target).toString()
    }.getOrNull()

    /** Deletes picture files no entry points at any more. */
    fun prune(book: CoverBook) {
        val keep = book.entries.values.mapNotNull { it.imageUri }
            .mapNotNull { runCatching { File(Uri.parse(it).path.orEmpty()).name }.getOrNull() }
            .toSet()
        coverDir.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
    }

    companion object {
        const val KEY_BOOK = "cover_book"
        const val MAX_EDGE = 768
    }
}
