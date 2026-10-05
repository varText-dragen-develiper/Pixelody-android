package com.pixelody.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.util.UUID
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
    fun importImage(context: Context, source: Uri): String? {
        var bitmap: Bitmap? = null
        var target: File? = null
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unreadable image" }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE * 2) sample *= 2
            bitmap = resolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("Unreadable image")
            val orientation = runCatching {
                resolver.openInputStream(source)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            }.getOrNull()
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                }
            }
            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated !== bitmap) { bitmap.recycle(); bitmap = rotated }
            }
            val ratio = MAX_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
            if (ratio < 1f) {
                val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
                if (scaled !== bitmap) { bitmap.recycle(); bitmap = scaled }
            }
            check(coverDir.isDirectory || coverDir.mkdirs())
            target = File(coverDir, "cover-${UUID.randomUUID()}.jpg")
            target.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
            Uri.fromFile(target).toString()
        } catch (_: Exception) {
            target?.delete()
            null
        } finally {
            bitmap?.recycle()
        }
    }

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
