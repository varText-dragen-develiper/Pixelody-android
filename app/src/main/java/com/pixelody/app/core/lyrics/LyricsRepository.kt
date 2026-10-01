package com.pixelody.app.core.lyrics

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class LyricsRepository(
    private val context: Context
) {
    private val lyricsCacheDir by lazy {
        File(context.filesDir, "lyrics").apply { if (!exists()) mkdirs() }
    }

    init {
        purgeObsoleteCache()
    }

    /**
     * Scans the lyrics cache directory and purges any legacy boilerplate or canned files.
     */
    fun purgeObsoleteCache() {
        runCatching {
            lyricsCacheDir.listFiles()?.forEach { file ->
                if (file.name == "px-track-moonlit-circuit.lrc") {
                    file.delete()
                } else if (file.isFile && file.length() > 0) {
                    val content = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
                    if (content != null && (isObsoleteBoilerplate(content) || content.contains("[by:Pixelody AutoLyricsEngine]"))) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * Clears all cached lyrics.
     */
    fun clearAllCachedLyrics() {
        runCatching {
            lyricsCacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun sanitizeCacheKey(track: Track): String {
        val raw = "${track.id}#${track.streamUrl}#${track.title}#${track.artist}#${track.durationSeconds}"
        val md = MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8))
        return md.joinToString("") { "%02x".format(it) }
    }

    private fun sanitizeCacheKey(id: String): String {
        return if (id.matches(Regex("^[a-zA-Z0-9_-]{1,64}$"))) id
        else {
            val md = MessageDigest.getInstance("MD5").digest(id.toByteArray(Charsets.UTF_8))
            md.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Checks if the given LRC content contains legacy canned boilerplate phrases or obsolete rhyming templates.
     */
    fun isObsoleteBoilerplate(content: String): Boolean {
        return content.contains("Feel the FLAC acoustics", ignoreCase = true) ||
                content.contains("Feel the MPEG acoustics", ignoreCase = true) ||
                content.contains("Analog warmth floating everywhere", ignoreCase = true) ||
                content.contains("on this sonic road", ignoreCase = true) ||
                content.contains("Feel the analog warmth running through the circuit", ignoreCase = true) ||
                content.contains("zero phase collide", ignoreCase = true) ||
                content.contains("Harmonic resonance is what we seek", ignoreCase = true) ||
                content.contains("Pure precision with zero overload", ignoreCase = true) ||
                content.contains("fills the acoustic chamber", ignoreCase = true) ||
                content.contains("Flickering signals through the quiet night", ignoreCase = true) ||
                content.contains("Guided by Local Signal", ignoreCase = true) ||
                content.contains("Local Signal", ignoreCase = true) ||
                content.contains("Moonlit Circuit", ignoreCase = true) ||
                content.contains("Mid-tempo resonance locked at", ignoreCase = true) ||
                content.contains("Fast driving rhythm locked at", ignoreCase = true) ||
                content.contains("Transient peaks shining like a gem", ignoreCase = true) ||
                content.contains("Joyful resonance everywhere", ignoreCase = true) ||
                content.contains("Chapters turning on the album", ignoreCase = true) ||
                content.contains("Moments captured inside", ignoreCase = true) ||
                content.contains("Echoing the story told in", ignoreCase = true) ||
                content.contains("Moving through the themes of", ignoreCase = true) ||
                content.contains("Jamendo", ignoreCase = true) ||
                content.contains("All the celebration jumping in the air", ignoreCase = true) ||
                content.contains("unfolding in this space", ignoreCase = true) ||
                content.contains("begins to softly trace", ignoreCase = true) ||
                content.contains("sets the frequency in motion", ignoreCase = true) ||
                content.contains("Guided by", ignoreCase = true) ||
                content.contains("px-track-moonlit-circuit", ignoreCase = true) ||
                // Obsolete pseudo-rhyming verses
                content.contains("Morning sunlight breaking through the haze", ignoreCase = true) ||
                content.contains("Golden horizons in an endless maze", ignoreCase = true) ||
                content.contains("Stepping forward in the warmth of light", ignoreCase = true) ||
                content.contains("Midnight shadows stretching out so wide", ignoreCase = true) ||
                content.contains("Neon reflections shimmering in the tide", ignoreCase = true) ||
                content.contains("Tracing outlines in the cooling rain", ignoreCase = true) ||
                content.contains("The pulse picks up and drives the line along", ignoreCase = true) ||
                content.contains("Current rushing with the cadence of the song", ignoreCase = true) ||
                content.contains("taking over the entire room", ignoreCase = true) ||
                content.contains("shining bright and chasing away the gloom", ignoreCase = true) ||
                content.contains("running wild and forever free", ignoreCase = true) ||
                content.contains("Waves of warmth and energy to share", ignoreCase = true) ||
                content.contains("Lost in the beauty of", ignoreCase = true) ||
                content.contains("Say it again, let", ignoreCase = true) ||
                content.contains("Stepping into the second movement now", ignoreCase = true) ||
                content.contains("Fresh tomorrow waiting in the morning breeze", ignoreCase = true) ||
                content.contains("Feel the rush of energy about to break", ignoreCase = true) ||
                content.contains("Everything suspended for", ignoreCase = true) ||
                content.contains("One last breath before the downbeat hits", ignoreCase = true) ||
                content.contains("lighting up every bit", ignoreCase = true)
    }

    /**
     * Resolves the physical file system path for audio tracks, including MediaStore content:// URIs.
     */
    private fun resolvePhysicalAudioPath(track: Track): String? {
        val streamUrl = track.streamUrl
        if (streamUrl.isBlank()) return null
        if (streamUrl.startsWith("/") || streamUrl.startsWith("file://")) {
            return streamUrl.removePrefix("file://")
        }
        if (streamUrl.startsWith("content://")) {
            val uri = Uri.parse(streamUrl)
            runCatching {
                val proj = arrayOf(MediaStore.Audio.Media.DATA)
                context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                    val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    if (dataCol >= 0 && cursor.moveToFirst()) {
                        val path = cursor.getString(dataCol)
                        if (!path.isNullOrBlank()) return path
                    }
                }
            }
        }
        return null
    }

    /**
     * Resolves lyrics for the given [track] with strict precedence:
     * 1. Sidecar .lrc file next to the audio file
     * 2. Embedded metadata audio tags (ID3v2 USLT/SYLT, Vorbis Comments, MP4 ©lyr)
     * 3. Local cache (purging any legacy boilerplate text)
     * 4. Automated acoustic BPM/peak alignment and contextual synthesis
     */
    suspend fun loadLyrics(track: Track): LyricsDocument = withContext(Dispatchers.IO) {
        android.util.Log.i("LyricsRepository", "loadLyrics called for '${track.title}' by '${track.artist}' (id=${track.id})")
        val cacheKey = sanitizeCacheKey(track)
        val cachedFile = File(lyricsCacheDir, "$cacheKey.lrc")

        // 1. Priority 1: Check sidecar .lrc next to the audio file
        val physicalPath = resolvePhysicalAudioPath(track)
        if (!physicalPath.isNullOrBlank()) {
            val audioFile = File(physicalPath)
            if (audioFile.exists()) {
                val sidecar = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
                if (sidecar.exists() && sidecar.length() > 0) {
                    val content = runCatching { sidecar.readText(Charsets.UTF_8) }.getOrNull()
                    if (!content.isNullOrBlank() && !isObsoleteBoilerplate(content)) {
                        val parsed = LrcParser.parse(content, LyricsSource.SidecarLrc)
                        val titleMatches = parsed.title.isBlank() || track.title.isBlank() ||
                                parsed.title.equals(track.title, ignoreCase = true) ||
                                track.title.contains(parsed.title, ignoreCase = true) ||
                                parsed.title.contains(track.title, ignoreCase = true)
                        if (parsed.hasLyrics && titleMatches) {
                            runCatching { cachedFile.writeText(content, Charsets.UTF_8) }
                            return@withContext parsed
                        }
                    }
                }
            }
        }

        // 2. Priority 2: Check embedded tags (ID3 USLT/SYLT, Vorbis Comments, MP4 ©lyr)
        val embedded = runCatching { EmbeddedLyricsExtractor.extract(context, track) }.getOrNull()
        if (embedded != null && embedded.lyricsText.isNotBlank()) {
            val doc = if (embedded.isSynchronized) {
                LrcParser.parse(embedded.lyricsText, LyricsSource.EmbeddedTag)
            } else {
                // Unsynchronized embedded text: automatically align to BPM, phrase zones, and peaks
                AutoLyricsAlignmentEngine.processAndAlign(
                    track = track,
                    rawText = embedded.lyricsText,
                    context = context
                )
            }
            if (doc.hasLyrics) {
                runCatching {
                    cachedFile.writeText(LrcParser.serialize(doc), Charsets.UTF_8)
                }
                return@withContext doc
            }
        }

        // 3. Priority 3: Check local cache (rejecting obsolete boilerplate and mismatched titles)
        if (cachedFile.exists() && cachedFile.length() > 0) {
            val content = runCatching { cachedFile.readText(Charsets.UTF_8) }.getOrNull()
            if (!content.isNullOrBlank()) {
                if (isObsoleteBoilerplate(content) || content.contains("[by:Pixelody AutoLyricsEngine]")) {
                    // Purge obsolete boilerplate and legacy synthetic telemetry immediately
                    runCatching { cachedFile.delete() }
                } else {
                    val source = when {
                        content.contains("[by:LRCLIB]") -> LyricsSource.LrcLib
                        content.contains("[by:EmbeddedTag]") -> LyricsSource.EmbeddedTag
                        else -> LyricsSource.SidecarLrc
                    }
                    val parsed = LrcParser.parse(content, source)
                    val titleMatches = parsed.title.isBlank() || track.title.isBlank() ||
                            parsed.title.equals(track.title, ignoreCase = true) ||
                            track.title.contains(parsed.title, ignoreCase = true) ||
                            parsed.title.contains(track.title, ignoreCase = true)
                    if (parsed.hasLyrics && titleMatches) {
                        return@withContext parsed
                    } else {
                        // Title mismatch or obsolete content: delete stale cache file immediately
                        runCatching { cachedFile.delete() }
                    }
                }
            }
        }

        // 4. Priority 4: LRCLIB Community Synced & Plain Lyrics (GPL-3.0)
        val lrclibDoc = runCatching {
            LrclibLyricsProvider.fetchLyrics(track, context)
        }.getOrNull()

        if (lrclibDoc != null && lrclibDoc.hasLyrics) {
            val serialized = "[by:LRCLIB]\n" + LrcParser.serialize(lrclibDoc)
            runCatching {
                cachedFile.writeText(serialized, Charsets.UTF_8)
            }
            return@withContext lrclibDoc
        }

        // 5. If no lyrics were found in sidecars, embedded tags, or LRCLIB, return LyricsSource.None
        // instead of generating confusing placeholder text that masquerades as song lyrics.
        return@withContext LyricsDocument(
            lines = emptyList(),
            isSynced = false,
            title = track.title,
            artist = track.artist,
            album = track.album,
            offsetMs = 0L,
            source = LyricsSource.None
        )
    }

    /**
     * Synthesizes acoustic tempo and downbeat structure on demand for instrumental tracks.
     */
    suspend fun getAcousticStructure(track: Track): LyricsDocument = withContext(Dispatchers.Default) {
        AutoLyricsAlignmentEngine.processAndAlign(
            track = track,
            rawText = null,
            context = context
        )
    }

    /**
     * Saves user-edited or imported LRC lyrics for [trackId].
     */
    suspend fun saveLyrics(trackId: String, lrcText: String): LyricsDocument = withContext(Dispatchers.IO) {
        val cacheKey = sanitizeCacheKey(trackId)
        val file = File(lyricsCacheDir, "$cacheKey.lrc")
        file.writeText(lrcText, Charsets.UTF_8)
        LrcParser.parse(lrcText, LyricsSource.SidecarLrc)
    }

    /**
     * Fallback demo lyrics generator retained for backward compatibility.
     */
    fun generateDemoLyrics(track: Track): LyricsDocument {
        val durMs = if (track.durationSeconds > 0) track.durationSeconds * 1000L else 180000L
        val quarter = durMs / 4
        val lines = listOf(
            LyricLine(timestampMs = 0L, text = "[Instrumental Intro]", isInstrumental = true),
            LyricLine(timestampMs = (quarter * 0.3).toLong(), text = "${track.title} begins to unfold"),
            LyricLine(timestampMs = (quarter * 0.7).toLong(), text = "Melodies from ${track.artist} in motion"),
            LyricLine(timestampMs = quarter, text = "• Chorus •"),
            LyricLine(timestampMs = (quarter * 1.3).toLong(), text = "Singing along with ${track.title}"),
            LyricLine(timestampMs = (quarter * 1.7).toLong(), text = "Every note ringing bright and clear"),
            LyricLine(timestampMs = (quarter * 2.2).toLong(), text = "• Verse 2 •"),
            LyricLine(timestampMs = (quarter * 2.5).toLong(), text = "Moments from ${track.album} returning"),
            LyricLine(timestampMs = (quarter * 3.0).toLong(), text = "• Final Chorus •"),
            LyricLine(timestampMs = (quarter * 3.3).toLong(), text = "Singing along with ${track.title}"),
            LyricLine(timestampMs = (quarter * 3.8).toLong(), text = "[Outro — Fade to silence]", isInstrumental = true)
        )

        return LyricsDocument(
            lines = lines,
            isSynced = true,
            title = track.title,
            artist = track.artist,
            album = track.album,
            offsetMs = 0L,
            source = LyricsSource.Generated
        )
    }
}
