package com.pixelody.app.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import com.pixelody.app.data.storage.SavedHostStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thread-safe, memory-efficient bitmap cache for artwork.
 * Uses an LruCache sized dynamically to available heap memory, downsamples images with
 * [BitmapFactory.Options.inSampleSize], and utilizes RGB_565 for 50% memory savings without
 * perceptible loss in artwork fidelity.
 */
object ArtworkBitmapCache {

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
    // Use 1/8th of available runtime memory for the bitmap cache (typically 32-64MB)
    private val cacheSizeKb = (maxMemoryKb / 8).coerceIn(16 * 1024, 64 * 1024)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Synchronous cache retrieval for frame-0 instant rendering.
     */
    fun get(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        val cached = memoryCache.get(url)
        return if (cached != null && !cached.isRecycled) cached else null
    }

    /**
     * Thread-safe cache insertion.
     */
    fun put(url: String, bitmap: Bitmap) {
        if (url.isNotBlank() && !bitmap.isRecycled) {
            memoryCache.put(url, bitmap)
        }
    }

    /**
     * Loads and caches an artwork bitmap with downsampling.
     * If already cached, returns immediately.
     */
    suspend fun loadBitmap(
        context: Context,
        url: String?,
        credentialStore: SavedHostStore,
        maxDimension: Int = 512
    ): Bitmap? {
        if (url.isNullOrBlank()) return null
        val cached = get(url)
        if (cached != null) return cached

        return withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(url)
                val bitmap: Bitmap? = when (uri.scheme) {
                    "file" -> uri.path?.let { path -> decodeSampledBitmap(path, maxDimension) }
                    "content" -> decodeSampledContentUri(context, uri, maxDimension)
                    else -> {
                        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 4000
                            readTimeout = 6000
                            runCatching { credentialStore.credentialFor(url) }
                                .getOrNull()
                                ?.takeIf { it.isNotBlank() }
                                ?.let { setRequestProperty("Authorization", "Bearer $it") }
                        }
                        try {
                            connection.inputStream.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        } finally {
                            connection.disconnect()
                        }
                    }
                }
                bitmap?.also { put(url, it) }
            }.getOrNull()
        }
    }

    private fun decodeSampledBitmap(path: String, maxDimension: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeFile(path, options)
    }

    private fun decodeSampledContentUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
