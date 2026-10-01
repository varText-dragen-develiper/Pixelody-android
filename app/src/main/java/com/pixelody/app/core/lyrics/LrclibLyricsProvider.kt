package com.pixelody.app.core.lyrics

import android.content.Context
import android.util.Log
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * LrclibLyricsProvider: Fetches community-verified synchronized and plain lyrics
 * from LRCLIB (https://lrclib.net), a free open-source (GPL-3.0) lyrics database.
 */
object LrclibLyricsProvider {

    private const val TAG = "LrclibLyricsProvider"
    private const val BASE_URL = "https://lrclib.net/api"
    private const val USER_AGENT = "Pixelody/0.2.0 (https://github.com/pixelody/pixelody; open-source-gpl3)"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Attempts to fetch lyrics for [track] from LRCLIB.
     * 1. Tries exact match via /api/get
     * 2. Falls back to search via /api/search if exact match returns 404
     * 3. If synced lyrics are returned, parses them directly.
     * 4. If plain lyrics are returned, aligns them to audio downbeats and peaks using [AutoLyricsAlignmentEngine].
     */
    suspend fun fetchLyrics(
        track: Track,
        context: Context? = null
    ): LyricsDocument? = withContext(Dispatchers.IO) {
        var sanitizedTitle = AutoLyricsAlignmentEngine.sanitizeMetadataString(track.title).ifBlank { track.title.trim() }
        var sanitizedArtist = AutoLyricsAlignmentEngine.sanitizeMetadataString(track.artist).ifBlank { track.artist.trim() }

        // Filter out common local scanner placeholder artist names
        if (sanitizedArtist.equals("On this phone", ignoreCase = true) ||
            sanitizedArtist.equals("<unknown>", ignoreCase = true) ||
            sanitizedArtist.equals("Unknown Artist", ignoreCase = true) ||
            sanitizedArtist.equals("Unknown", ignoreCase = true)
        ) {
            sanitizedArtist = ""
        }

        // If artist is missing or title contains " - ", try splitting "Artist - Title"
        if (sanitizedArtist.isBlank() && sanitizedTitle.contains(" - ")) {
            val parts = sanitizedTitle.split(" - ", limit = 2)
            sanitizedArtist = parts[0].trim()
            sanitizedTitle = parts[1].trim()
        }

        // Also check if streamUrl filename contains "Artist - Title" (e.g. Queen - Bohemian Rhapsody.mp3)
        if (sanitizedArtist.isBlank() && track.streamUrl.isNotBlank()) {
            val fileName = track.streamUrl.substringAfterLast('/').substringBeforeLast('.')
            val decodedName = runCatching { java.net.URLDecoder.decode(fileName, "UTF-8") }.getOrDefault(fileName)
            val cleanName = AutoLyricsAlignmentEngine.sanitizeMetadataString(decodedName)
            if (cleanName.contains(" - ")) {
                val parts = cleanName.split(" - ", limit = 2)
                sanitizedArtist = parts[0].trim()
                sanitizedTitle = parts[1].trim()
            }
        }

        if (sanitizedTitle.isBlank()) {
            return@withContext null
        }

        Log.i(TAG, "Fetching lyrics from LRCLIB for '$sanitizedTitle' by '$sanitizedArtist' (duration: ${track.durationSeconds}s)")

        // 1. Try exact signature fetch via /api/get (omit album_name to avoid playlist/rip mismatches)
        val exactRecord = fetchExact(sanitizedTitle, sanitizedArtist, track.durationSeconds)
        if (exactRecord != null) {
            val doc = convertRecordToDocument(track, exactRecord, context)
            if (doc != null && doc.hasLyrics) {
                Log.i(TAG, "LRCLIB exact match found for '$sanitizedTitle'")
                return@withContext doc
            }
        }

        // 2. Fallback to /api/search with track_name and artist_name
        var searchRecord = searchBestMatch(sanitizedTitle, sanitizedArtist, track.durationSeconds)
        if (searchRecord != null) {
            val doc = convertRecordToDocument(track, searchRecord, context)
            if (doc != null && doc.hasLyrics) {
                Log.i(TAG, "LRCLIB search match found for '$sanitizedTitle'")
                return@withContext doc
            }
        }

        // 3. Fallback to freeform search: /api/search?q="title artist"
        val query = if (sanitizedArtist.isNotBlank()) "$sanitizedTitle $sanitizedArtist" else sanitizedTitle
        searchRecord = searchByQuery(query, sanitizedTitle, track.durationSeconds)
        if (searchRecord != null) {
            val doc = convertRecordToDocument(track, searchRecord, context)
            if (doc != null && doc.hasLyrics) {
                Log.i(TAG, "LRCLIB query search match found for '$query'")
                return@withContext doc
            }
        }

        Log.w(TAG, "LRCLIB: No lyrics found in database for '$sanitizedTitle' by '$sanitizedArtist'")
        null
    }

    private fun fetchExact(
        title: String,
        artist: String,
        durationSeconds: Int
    ): JSONObject? {
        val urlBuilder = "$BASE_URL/get".toHttpUrlOrNull()?.newBuilder() ?: return null
        urlBuilder.addQueryParameter("track_name", title)
        if (artist.isNotBlank()) {
            urlBuilder.addQueryParameter("artist_name", artist)
        }
        if (durationSeconds > 0) {
            urlBuilder.addQueryParameter("duration", durationSeconds.toString())
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", USER_AGENT)
            .header("Lrclib-Client", "Pixelody (GPL-3.0)")
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) JSONObject(body) else null
                } else {
                    null
                }
            }
        }.onFailure {
            Log.e(TAG, "LRCLIB /get network error: ${it.message}")
        }.getOrNull()
    }

    private fun searchBestMatch(
        title: String,
        artist: String,
        durationSeconds: Int
    ): JSONObject? {
        val urlBuilder = "$BASE_URL/search".toHttpUrlOrNull()?.newBuilder() ?: return null
        urlBuilder.addQueryParameter("track_name", title)
        if (artist.isNotBlank()) {
            urlBuilder.addQueryParameter("artist_name", artist)
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", USER_AGENT)
            .header("Lrclib-Client", "Pixelody (GPL-3.0)")
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val array = JSONArray(body)
                        pickBestRecordFromArray(array, title, durationSeconds)
                    } else null
                } else null
            }
        }.onFailure {
            Log.e(TAG, "LRCLIB /search network error: ${it.message}")
        }.getOrNull()
    }

    private fun searchByQuery(
        query: String,
        targetTitle: String,
        durationSeconds: Int
    ): JSONObject? {
        val urlBuilder = "$BASE_URL/search".toHttpUrlOrNull()?.newBuilder() ?: return null
        urlBuilder.addQueryParameter("q", query)

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", USER_AGENT)
            .header("Lrclib-Client", "Pixelody (GPL-3.0)")
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val array = JSONArray(body)
                        pickBestRecordFromArray(array, targetTitle, durationSeconds)
                    } else null
                } else null
            }
        }.onFailure {
            Log.e(TAG, "LRCLIB query search error: ${it.message}")
        }.getOrNull()
    }

    internal fun pickBestRecordFromArray(
        array: JSONArray,
        targetTitle: String,
        durationSeconds: Int
    ): JSONObject? {
        if (array.length() == 0) return null

        var bestScore = -1
        var bestObj: JSONObject? = null

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val synced = obj.optString("syncedLyrics").trim()
            val plain = obj.optString("plainLyrics").trim()
            val hasContent = synced.isNotEmpty() || plain.isNotEmpty()
            if (!hasContent) continue

            var score = 10
            if (synced.isNotEmpty()) score += 50

            val recDuration = obj.optDouble("duration", 0.0)
            if (durationSeconds > 0 && recDuration > 0) {
                val diff = abs(recDuration - durationSeconds.toDouble())
                if (diff <= 3.0) score += 40
                else if (diff <= 10.0) score += 20
                else if (diff > 30.0) score -= 20
            }

            val trackName = obj.optString("trackName").trim()
            if (trackName.equals(targetTitle, ignoreCase = true)) {
                score += 30
            }

            if (score > bestScore) {
                bestScore = score
                bestObj = obj
            }
        }

        return bestObj
    }

    internal suspend fun convertRecordToDocument(
        track: Track,
        record: JSONObject,
        context: Context?
    ): LyricsDocument? {
        val synced = record.optString("syncedLyrics").trim()
        val plain = record.optString("plainLyrics").trim()

        if (synced.isNotBlank()) {
            val parsed = LrcParser.parse(synced, LyricsSource.LrcLib)
            return parsed.copy(
                title = track.title.ifBlank { parsed.title },
                artist = track.artist.ifBlank { parsed.artist },
                album = track.album.ifBlank { parsed.album },
                source = LyricsSource.LrcLib
            )
        }

        if (plain.isNotBlank()) {
            val aligned = AutoLyricsAlignmentEngine.processAndAlign(
                track = track,
                rawText = plain,
                context = context
            )
            return aligned.copy(
                source = LyricsSource.LrcLib
            )
        }

        return null
    }
}
