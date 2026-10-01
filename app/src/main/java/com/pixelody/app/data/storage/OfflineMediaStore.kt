package com.pixelody.app.data.storage

import android.content.Context
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class CachedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val format: String,
    val codec: String,
    val lossless: Boolean = format.uppercase(Locale.US) in setOf("FLAC", "WAV", "ALAC", "AIFF"),
    val sampleRate: Int = 44100,
    val bitDepth: Int? = if (format.uppercase(Locale.US) in setOf("FLAC", "WAV", "ALAC", "AIFF")) 16 else null,
    val bitrate: Int? = null,
    val channels: Int = 2,
    val replayGainDb: Double? = null,
    val localFilePath: String,
    val localArtworkPath: String?,
    val fileSizeBytes: Long,
    val cachedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = cachedAt,
    val isPinned: Boolean = false
) {
    fun toTrack(): Track {
        return Track(
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
            replayGainDb = replayGainDb,
            artworkUrl = localArtworkPath?.let { "file://$it" },
            streamUrl = "file://$localFilePath",
            favorite = isPinned,
            missing = false
        )
    }
}

/**
 * Manages downloading and caching media streams and artwork to local app files
 * so the companion client can play host music offline without an active network connection.
 * Includes atomic catalog persistence, storage quota tracking, and LRU auto-eviction.
 */
class OfflineMediaStore(private val context: Context) {
    private val audioDir = File(context.filesDir, "pixelody_offline_audio").apply { mkdirs() }
    private val artDir = File(context.filesDir, "pixelody_offline_art").apply { mkdirs() }
    private val catalogFile = File(context.filesDir, "pixelody_offline_catalog.json")

    @Synchronized
    fun listCachedTracks(): List<CachedTrack> {
        if (!catalogFile.exists()) return emptyList()
        return runCatching {
            val jsonArray = JSONArray(catalogFile.readText(Charsets.UTF_8))
            val items = mutableListOf<CachedTrack>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val localFilePath = obj.optString("localFilePath")
                val localFile = File(localFilePath)
                if (localFile.exists() && localFile.length() > 0) {
                    val formatStr = obj.optString("format", "FLAC")
                    val isLossless = obj.optBoolean(
                        "lossless",
                        formatStr.uppercase(Locale.US) in setOf("FLAC", "WAV", "ALAC", "AIFF")
                    )
                    val cachedTimestamp = obj.optLong("cachedAt", System.currentTimeMillis())
                    items.add(
                        CachedTrack(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            artist = obj.optString("artist"),
                            album = obj.optString("album"),
                            durationSeconds = obj.optInt("durationSeconds"),
                            format = formatStr,
                            codec = obj.optString("codec", "flac"),
                            lossless = isLossless,
                            sampleRate = obj.optInt("sampleRate", 44100),
                            bitDepth = if (obj.has("bitDepth") && !obj.isNull("bitDepth")) obj.getInt("bitDepth") else (if (isLossless) 16 else null),
                            bitrate = if (obj.has("bitrate") && !obj.isNull("bitrate")) obj.getInt("bitrate") else null,
                            channels = obj.optInt("channels", 2),
                            replayGainDb = if (obj.has("replayGainDb") && !obj.isNull("replayGainDb")) obj.getDouble("replayGainDb") else null,
                            localFilePath = localFilePath,
                            localArtworkPath = obj.optString("localArtworkPath").takeIf { it.isNotBlank() },
                            fileSizeBytes = obj.optLong("fileSizeBytes", localFile.length()),
                            cachedAt = cachedTimestamp,
                            lastAccessedAt = obj.optLong("lastAccessedAt", cachedTimestamp),
                            isPinned = obj.optBoolean("isPinned", false)
                        )
                    )
                }
            }
            items
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun isCached(trackId: String): Boolean {
        val cached = getCachedTrack(trackId) ?: return false
        return File(cached.localFilePath).exists()
    }

    @Synchronized
    fun getCachedTrack(trackId: String): CachedTrack? {
        return listCachedTracks().firstOrNull { it.id == trackId }
    }

    @Synchronized
    fun markTrackAccessed(trackId: String) {
        val tracks = listCachedTracks().toMutableList()
        val index = tracks.indexOfFirst { it.id == trackId }
        if (index >= 0) {
            val updated = tracks[index].copy(lastAccessedAt = System.currentTimeMillis())
            tracks[index] = updated
            persistCatalog(tracks)
        }
    }

    @Synchronized
    fun setTrackPinned(trackId: String, isPinned: Boolean) {
        val tracks = listCachedTracks().toMutableList()
        val index = tracks.indexOfFirst { it.id == trackId }
        if (index >= 0) {
            val updated = tracks[index].copy(isPinned = isPinned)
            tracks[index] = updated
            persistCatalog(tracks)
        }
    }

    @Synchronized
    fun totalCacheSizeBytes(): Long {
        return listCachedTracks().sumOf { it.fileSizeBytes } + artCacheSizeBytes()
    }

    @Synchronized
    fun audioCacheSizeBytes(): Long {
        return audioDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    @Synchronized
    fun artCacheSizeBytes(): Long {
        return artDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    fun freeDiskSpaceBytes(): Long = runCatching { context.filesDir.freeSpace }.getOrDefault(0L)

    fun totalDiskSpaceBytes(): Long = runCatching { context.filesDir.totalSpace }.getOrDefault(0L)

    /**
     * Evicts least recently accessed unpinned tracks until at least [bytesNeeded] are freed.
     * Returns the list of evicted track IDs.
     */
    @Synchronized
    fun evictOldest(bytesNeeded: Long): List<String> {
        if (bytesNeeded <= 0L) return emptyList()
        val tracks = listCachedTracks().toMutableList()
        // Sort unpinned tracks by lastAccessedAt ascending (oldest first)
        val unpinned = tracks.filterNot { it.isPinned }.sortedBy { it.lastAccessedAt }
        val evictedIds = mutableListOf<String>()
        var freedBytes = 0L

        for (candidate in unpinned) {
            if (freedBytes >= bytesNeeded) break
            val fileSize = candidate.fileSizeBytes
            runCatching { File(candidate.localFilePath).delete() }
            candidate.localArtworkPath?.let { runCatching { File(it).delete() } }
            tracks.removeAll { it.id == candidate.id }
            freedBytes += fileSize
            evictedIds.add(candidate.id)
        }

        if (evictedIds.isNotEmpty()) {
            persistCatalog(tracks)
        }
        return evictedIds
    }

    suspend fun cacheTrack(
        track: Track,
        hostBaseUrl: String,
        token: String,
        isPinned: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): CachedTrack = withContext(Dispatchers.IO) {
        val extension = when (track.format.lowercase(Locale.US)) {
            "flac" -> "flac"
            "wav" -> "wav"
            "mp3" -> "mp3"
            "aac", "m4a" -> "m4a"
            "ogg" -> "ogg"
            "opus" -> "opus"
            else -> "bin"
        }
        val safeId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val targetAudioFile = File(audioDir, "track_$safeId.$extension")
        val streamUrl = if (track.streamUrl.startsWith("http://") || track.streamUrl.startsWith("https://")) {
            track.streamUrl
        } else {
            "${hostBaseUrl.trimEnd('/')}/${track.streamUrl.trimStart('/')}"
        }

        val url = URL(streamUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 30000
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }

        try {
            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Host returned HTTP $responseCode while downloading audio stream.")
            }

            val totalBytes: Long = conn.contentLengthLong.takeIf { it > 0L }
                ?: (track.durationSeconds.toLong() * 44100L * 4L)
            var bytesRead = 0L

            conn.inputStream.use { input ->
                FileOutputStream(targetAudioFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (totalBytes > 0L) {
                            val ratio = (bytesRead.toDouble() / totalBytes.toDouble()).toFloat()
                            onProgress(ratio.coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }

            // Download artwork if available
            var localArtPath: String? = null
            if (!track.artworkUrl.isNullOrBlank()) {
                val artUrlStr = if (track.artworkUrl.startsWith("http://") || track.artworkUrl.startsWith("https://")) {
                    track.artworkUrl
                } else {
                    "${hostBaseUrl.trimEnd('/')}/${track.artworkUrl.trimStart('/')}"
                }
                runCatching {
                    val artTarget = File(artDir, "art_$safeId.jpg")
                    val artConn = (URL(artUrlStr).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (artConn.responseCode in 200..299) {
                        artConn.inputStream.use { input ->
                            FileOutputStream(artTarget).use { output ->
                                input.copyTo(output)
                            }
                        }
                        localArtPath = artTarget.absolutePath
                    }
                    artConn.disconnect()
                }
            }

            val now = System.currentTimeMillis()
            val cached = CachedTrack(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationSeconds = track.durationSeconds,
                format = track.format,
                codec = track.codec,
                lossless = track.lossless,
                sampleRate = track.sampleRate,
                bitDepth = track.bitDepth,
                bitrate = track.bitrate,
                channels = track.channels,
                replayGainDb = track.replayGainDb,
                localFilePath = targetAudioFile.absolutePath,
                localArtworkPath = localArtPath,
                fileSizeBytes = targetAudioFile.length(),
                cachedAt = now,
                lastAccessedAt = now,
                isPinned = isPinned
            )

            saveToCatalog(cached)
            cached
        } finally {
            conn.disconnect()
        }
    }

    @Synchronized
    fun removeCachedTrack(trackId: String) {
        val current = listCachedTracks().toMutableList()
        val index = current.indexOfFirst { it.id == trackId }
        if (index >= 0) {
            val item = current.removeAt(index)
            runCatching { File(item.localFilePath).delete() }
            item.localArtworkPath?.let { runCatching { File(it).delete() } }
            persistCatalog(current)
        }
    }

    @Synchronized
    fun pruneUnpinnedCache(): Int {
        val current = listCachedTracks().toMutableList()
        val unpinned = current.filterNot { it.isPinned }
        for (item in unpinned) {
            runCatching { File(item.localFilePath).delete() }
            item.localArtworkPath?.let { runCatching { File(it).delete() } }
            current.remove(item)
        }
        persistCatalog(current)
        return unpinned.size
    }

    @Synchronized
    fun clearAudioCache() {
        audioDir.listFiles()?.forEach { it.delete() }
        if (catalogFile.exists()) catalogFile.delete()
    }

    @Synchronized
    fun clearArtCache() {
        artDir.listFiles()?.forEach { it.delete() }
        val current = listCachedTracks().map { it.copy(localArtworkPath = null) }
        persistCatalog(current)
    }

    @Synchronized
    fun clearCache() {
        audioDir.listFiles()?.forEach { it.delete() }
        artDir.listFiles()?.forEach { it.delete() }
        if (catalogFile.exists()) catalogFile.delete()
    }

    @Synchronized
    fun clearAllCache() = clearCache()

    @Synchronized
    private fun saveToCatalog(track: CachedTrack) {
        val current = listCachedTracks().filterNot { it.id == track.id }.toMutableList()
        current.add(0, track)
        persistCatalog(current)
    }

    @Synchronized
    private fun persistCatalog(tracks: List<CachedTrack>) {
        val array = JSONArray()
        tracks.forEach { t ->
            val obj = JSONObject()
                .put("id", t.id)
                .put("title", t.title)
                .put("artist", t.artist)
                .put("album", t.album)
                .put("durationSeconds", t.durationSeconds)
                .put("format", t.format)
                .put("codec", t.codec)
                .put("lossless", t.lossless)
                .put("sampleRate", t.sampleRate)
                .put("bitDepth", t.bitDepth ?: JSONObject.NULL)
                .put("bitrate", t.bitrate ?: JSONObject.NULL)
                .put("channels", t.channels)
                .put("replayGainDb", t.replayGainDb ?: JSONObject.NULL)
                .put("localFilePath", t.localFilePath)
                .put("localArtworkPath", t.localArtworkPath ?: "")
                .put("fileSizeBytes", t.fileSizeBytes)
                .put("cachedAt", t.cachedAt)
                .put("lastAccessedAt", t.lastAccessedAt)
                .put("isPinned", t.isPinned)
            array.put(obj)
        }
        val tempFile = File(context.filesDir, "pixelody_offline_catalog.json.tmp")
        tempFile.writeText(array.toString(2), Charsets.UTF_8)
        if (catalogFile.exists()) catalogFile.delete()
        tempFile.renameTo(catalogFile)
    }
}
