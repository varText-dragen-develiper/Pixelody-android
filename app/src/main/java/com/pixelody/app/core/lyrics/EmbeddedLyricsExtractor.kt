package com.pixelody.app.core.lyrics

import android.content.Context
import android.net.Uri
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class EmbeddedLyricsResult(
    val lyricsText: String,
    val isSynchronized: Boolean = false,
    val tagFormat: String = "Embedded"
)

/**
 * EmbeddedLyricsExtractor: Low-overhead, zero-dependency extractor for audio metadata lyrics.
 * Scans ID3v2 (USLT / SYLT), Vorbis Comments (FLAC / OGG / OPUS), and MP4/M4A atoms (©lyr).
 */
object EmbeddedLyricsExtractor {

    private const val MAX_SCAN_BYTES = 2 * 1024 * 1024 // 2 MB inspection budget

    suspend fun extract(context: Context, track: Track): EmbeddedLyricsResult? = withContext(Dispatchers.IO) {
        val streamUrl = track.streamUrl
        if (streamUrl.isBlank()) return@withContext null

        runCatching {
            when {
                streamUrl.startsWith("content://") -> {
                    val uri = Uri.parse(streamUrl)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        extractFromStream(stream, track.format)
                    }
                }
                streamUrl.startsWith("file://") || streamUrl.startsWith("/") -> {
                    val cleanPath = streamUrl.removePrefix("file://")
                    val file = File(cleanPath)
                    if (file.exists() && file.canRead()) {
                        file.inputStream().use { stream ->
                            extractFromStream(stream, track.format)
                        }
                    } else null
                }
                else -> null
            }
        }.getOrNull()
    }

    fun extractFromStream(stream: InputStream, formatHint: String = ""): EmbeddedLyricsResult? {
        val header = ByteArray(12)
        val read = stream.read(header)
        if (read < 4) return null

        val combinedStream = SequenceInputStreamWithHeader(header, read, stream)

        // 1. Check ID3v2 header: 'ID3' (bytes: 0x49, 0x44, 0x33)
        if (header[0] == 0x49.toByte() && header[1] == 0x44.toByte() && header[2] == 0x33.toByte()) {
            return extractId3v2Lyrics(combinedStream)
        }

        // 2. Check FLAC header: 'fLaC' (bytes: 0x66, 0x4C, 0x61, 0x43)
        if (header[0] == 0x66.toByte() && header[1] == 0x4C.toByte() && header[2] == 0x61.toByte() && header[3] == 0x43.toByte()) {
            return extractFlacVorbisLyrics(combinedStream)
        }

        // 3. Check MP4 / M4A / AAC container: 4 bytes length followed by 'ftyp'
        if (read >= 8 && header[4] == 0x66.toByte() && header[5] == 0x74.toByte() && header[6] == 0x79.toByte() && header[7] == 0x70.toByte()) {
            return extractMp4Lyrics(combinedStream)
        }

        return null
    }

    /**
     * Parses ID3v2 frames looking for USLT (unsynced lyrics) or SYLT (synced lyrics).
     */
    fun extractId3v2Lyrics(stream: InputStream): EmbeddedLyricsResult? {
        val header = ByteArray(10)
        var readTotal = 0
        while (readTotal < 10) {
            val r = stream.read(header, readTotal, 10 - readTotal)
            if (r <= 0) return null
            readTotal += r
        }

        val majorVersion = header[3].toInt() and 0xFF
        val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                ((header[7].toInt() and 0x7F) shl 14) or
                ((header[8].toInt() and 0x7F) shl 7) or
                (header[9].toInt() and 0x7F)

        val boundedTagSize = tagSize.coerceAtMost(MAX_SCAN_BYTES)
        val tagBytes = ByteArray(boundedTagSize)
        var tagRead = 0
        while (tagRead < boundedTagSize) {
            val r = stream.read(tagBytes, tagRead, boundedTagSize - tagRead)
            if (r <= 0) break
            tagRead += r
        }

        var offset = 0
        while (offset + 10 < tagRead) {
            val frameId = String(tagBytes, offset, 4, StandardCharsets.ISO_8859_1)
            val isNullPadding = tagBytes[offset] == 0.toByte() && tagBytes[offset + 1] == 0.toByte()
            if (isNullPadding || frameId.isBlank()) break

            val frameSize = if (majorVersion == 4) {
                // ID3v2.4 uses synchsafe integers for frame sizes
                ((tagBytes[offset + 4].toInt() and 0x7F) shl 21) or
                        ((tagBytes[offset + 5].toInt() and 0x7F) shl 14) or
                        ((tagBytes[offset + 6].toInt() and 0x7F) shl 7) or
                        (tagBytes[offset + 7].toInt() and 0x7F)
            } else {
                // ID3v2.3 uses standard 32-bit big-endian integers
                ((tagBytes[offset + 4].toInt() and 0xFF) shl 24) or
                        ((tagBytes[offset + 5].toInt() and 0xFF) shl 16) or
                        ((tagBytes[offset + 6].toInt() and 0xFF) shl 8) or
                        (tagBytes[offset + 7].toInt() and 0xFF)
            }

            val dataOffset = offset + 10
            if (frameSize <= 0 || dataOffset + frameSize > tagRead) {
                break
            }

            if (frameId == "USLT") {
                // Unsynchronized lyrics
                val lyrics = parseUsltFrame(tagBytes, dataOffset, frameSize)
                if (!lyrics.isNullOrBlank()) {
                    val isLrcSynced = lyrics.contains(Regex("""\[\d{2}:\d{2}"""))
                    return EmbeddedLyricsResult(
                        lyricsText = lyrics,
                        isSynchronized = isLrcSynced,
                        tagFormat = "ID3v2 USLT"
                    )
                }
            } else if (frameId == "SYLT") {
                // Synchronized lyrics
                val parsed = parseSyltFrame(tagBytes, dataOffset, frameSize)
                if (parsed != null && parsed.isNotEmpty()) {
                    val lrcBuilder = StringBuilder()
                    for ((timeMs, text) in parsed) {
                        val minutes = (timeMs / 1000) / 60
                        val seconds = (timeMs / 1000) % 60
                        val hundredths = (timeMs % 1000) / 10
                        lrcBuilder.append(String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s\n", minutes, seconds, hundredths, text))
                    }
                    return EmbeddedLyricsResult(
                        lyricsText = lrcBuilder.toString().trim(),
                        isSynchronized = true,
                        tagFormat = "ID3v2 SYLT"
                    )
                }
            }

            offset += 10 + frameSize
        }
        return null
    }

    private fun parseUsltFrame(bytes: ByteArray, offset: Int, size: Int): String? {
        if (size <= 4) return null
        val encodingByte = bytes[offset].toInt() and 0xFF
        val charset = resolveId3Charset(encodingByte)
        // Skip encoding (1) and language (3)
        var cursor = offset + 4
        val end = offset + size

        // Content descriptor is terminated by null character
        if (charset == StandardCharsets.UTF_16 || charset == StandardCharsets.UTF_16BE) {
            while (cursor + 1 < end) {
                if (bytes[cursor] == 0.toByte() && bytes[cursor + 1] == 0.toByte()) {
                    cursor += 2
                    break
                }
                cursor += 2
            }
        } else {
            while (cursor < end) {
                if (bytes[cursor] == 0.toByte()) {
                    cursor += 1
                    break
                }
                cursor += 1
            }
        }

        if (cursor >= end) return null
        return runCatching {
            String(bytes, cursor, end - cursor, charset).trim()
        }.getOrNull()
    }

    private fun parseSyltFrame(bytes: ByteArray, offset: Int, size: Int): List<Pair<Long, String>>? {
        if (size <= 6) return null
        val encoding = bytes[offset].toInt() and 0xFF
        val charset = resolveId3Charset(encoding)
        val timeFormat = bytes[offset + 4].toInt() and 0xFF // 1 = ms, 2 = MPEG frames
        val isMilliseconds = timeFormat == 1

        var cursor = offset + 6
        val end = offset + size

        // Skip descriptor terminated by null
        if (charset == StandardCharsets.UTF_16 || charset == StandardCharsets.UTF_16BE) {
            while (cursor + 1 < end) {
                if (bytes[cursor] == 0.toByte() && bytes[cursor + 1] == 0.toByte()) {
                    cursor += 2
                    break
                }
                cursor += 2
            }
        } else {
            while (cursor < end) {
                if (bytes[cursor] == 0.toByte()) {
                    cursor += 1
                    break
                }
                cursor += 1
            }
        }

        val entries = mutableListOf<Pair<Long, String>>()
        while (cursor + 4 < end) {
            val textStart = cursor
            if (charset == StandardCharsets.UTF_16 || charset == StandardCharsets.UTF_16BE) {
                while (cursor + 1 < end && !(bytes[cursor] == 0.toByte() && bytes[cursor + 1] == 0.toByte())) {
                    cursor += 2
                }
                cursor += 2
            } else {
                while (cursor < end && bytes[cursor] != 0.toByte()) {
                    cursor++
                }
                cursor++
            }

            if (cursor + 4 > end) break
            val text = String(bytes, textStart, (cursor - textStart).coerceAtLeast(0), charset).trim('\u0000', ' ', '\n', '\r')
            val timestamp = ((bytes[cursor].toLong() and 0xFF) shl 24) or
                    ((bytes[cursor + 1].toLong() and 0xFF) shl 16) or
                    ((bytes[cursor + 2].toLong() and 0xFF) shl 8) or
                    (bytes[cursor + 3].toLong() and 0xFF)
            cursor += 4

            val timestampMs = if (isMilliseconds) timestamp else (timestamp * 26L) // rough frame-to-ms fallback
            if (text.isNotBlank()) {
                entries.add(timestampMs to text)
            }
        }
        return entries.takeIf { it.isNotEmpty() }
    }

    private fun resolveId3Charset(encodingByte: Int): Charset {
        return when (encodingByte) {
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }
    }

    /**
     * Extracts Vorbis comments from FLAC metadata blocks.
     */
    fun extractFlacVorbisLyrics(stream: InputStream): EmbeddedLyricsResult? {
        // Skip 'fLaC' header (4 bytes)
        val flacHeader = ByteArray(4)
        if (stream.read(flacHeader) < 4) return null

        var isLast = false
        while (!isLast) {
            val blockHeader = ByteArray(4)
            if (stream.read(blockHeader) < 4) break
            isLast = (blockHeader[0].toInt() and 0x80) != 0
            val blockType = blockHeader[0].toInt() and 0x7F
            val length = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                    ((blockHeader[2].toInt() and 0xFF) shl 8) or
                    (blockHeader[3].toInt() and 0xFF)

            if (blockType == 4) { // VORBIS_COMMENT block
                val bounded = length.coerceAtMost(MAX_SCAN_BYTES)
                val blockBytes = ByteArray(bounded)
                var read = 0
                while (read < bounded) {
                    val r = stream.read(blockBytes, read, bounded - read)
                    if (r <= 0) break
                    read += r
                }
                return parseVorbisCommentBytes(blockBytes)
            } else {
                // Skip this block
                var toSkip = length.toLong()
                while (toSkip > 0) {
                    val skipped = stream.skip(toSkip)
                    if (skipped <= 0) break
                    toSkip -= skipped
                }
            }
        }
        return null
    }

    private fun parseVorbisCommentBytes(bytes: ByteArray): EmbeddedLyricsResult? {
        if (bytes.size < 8) return null
        var cursor = 0
        // Vendor string length (32-bit LE)
        val vendorLen = (bytes[cursor].toInt() and 0xFF) or
                ((bytes[cursor + 1].toInt() and 0xFF) shl 8) or
                ((bytes[cursor + 2].toInt() and 0xFF) shl 16) or
                ((bytes[cursor + 3].toInt() and 0xFF) shl 24)
        cursor += 4 + vendorLen
        if (cursor + 4 > bytes.size) return null

        // User comment list length (32-bit LE)
        val userCommentCount = (bytes[cursor].toInt() and 0xFF) or
                ((bytes[cursor + 1].toInt() and 0xFF) shl 8) or
                ((bytes[cursor + 2].toInt() and 0xFF) shl 16) or
                ((bytes[cursor + 3].toInt() and 0xFF) shl 24)
        cursor += 4

        for (i in 0 until userCommentCount) {
            if (cursor + 4 > bytes.size) break
            val commentLen = (bytes[cursor].toInt() and 0xFF) or
                    ((bytes[cursor + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[cursor + 2].toInt() and 0xFF) shl 16) or
                    ((bytes[cursor + 3].toInt() and 0xFF) shl 24)
            cursor += 4
            if (commentLen <= 0 || cursor + commentLen > bytes.size) break

            val comment = String(bytes, cursor, commentLen, StandardCharsets.UTF_8)
            cursor += commentLen

            val eqIdx = comment.indexOf('=')
            if (eqIdx > 0) {
                val field = comment.substring(0, eqIdx).trim().uppercase(java.util.Locale.US)
                val value = comment.substring(eqIdx + 1).trim()
                if (field == "LYRICS" || field == "UNSYNCEDLYRICS" || field == "SYNCEDLYRICS" || field == "LYRIC" || field == "UNSYNCED LYRICS") {
                    if (value.isNotBlank()) {
                        val isLrcSynced = value.contains(Regex("""\[\d{2}:\d{2}"""))
                        return EmbeddedLyricsResult(
                            lyricsText = value,
                            isSynchronized = isLrcSynced,
                            tagFormat = "Vorbis $field"
                        )
                    }
                }
            }
        }
        return null
    }

    /**
     * Extracts ©lyr atom from MP4/M4A file containers.
     */
    fun extractMp4Lyrics(stream: InputStream): EmbeddedLyricsResult? {
        val buffer = ByteArray(MAX_SCAN_BYTES.coerceAtMost(1024 * 1024))
        var totalRead = 0
        while (totalRead < buffer.size) {
            val r = stream.read(buffer, totalRead, buffer.size - totalRead)
            if (r <= 0) break
            totalRead += r
        }

        val lyrTag = byteArrayOf(0xA9.toByte(), 0x6C.toByte(), 0x79.toByte(), 0x72.toByte()) // ©lyr
        val idx = findSubarray(buffer, 0, totalRead, lyrTag)
        if (idx >= 0 && idx + 16 < totalRead) {
            // Found ©lyr. Typically follows with 'data' atom header (8 bytes) + type flags (4 bytes) + locale (4 bytes)
            val dataTag = byteArrayOf(0x64.toByte(), 0x61.toByte(), 0x74.toByte(), 0x61.toByte()) // data
            val dataIdx = findSubarray(buffer, idx, (idx + 32).coerceAtMost(totalRead), dataTag)
            if (dataIdx >= 0 && dataIdx + 16 <= totalRead) {
                val dataSize = ((buffer[dataIdx - 4].toInt() and 0xFF) shl 24) or
                        ((buffer[dataIdx - 3].toInt() and 0xFF) shl 16) or
                        ((buffer[dataIdx - 2].toInt() and 0xFF) shl 8) or
                        (buffer[dataIdx - 1].toInt() and 0xFF)
                val textStart = dataIdx + 12 // skip 'data' (4) + type (4) + locale (4)
                val textLength = (dataSize - 16).coerceIn(0, totalRead - textStart)
                if (textLength > 0) {
                    val text = String(buffer, textStart, textLength, StandardCharsets.UTF_8).trim()
                    if (text.isNotBlank()) {
                        val isLrcSynced = text.contains(Regex("""\[\d{2}:\d{2}"""))
                        return EmbeddedLyricsResult(
                            lyricsText = text,
                            isSynchronized = isLrcSynced,
                            tagFormat = "MP4 ©lyr"
                        )
                    }
                }
            }
        }
        return null
    }


    private fun findSubarray(source: ByteArray, start: Int, end: Int, target: ByteArray): Int {
        val max = end - target.size
        for (i in start..max) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    /**
     * Helper stream to re-read initial header bytes alongside the rest of the stream.
     */
    private class SequenceInputStreamWithHeader(
        header: ByteArray,
        headerLength: Int,
        rest: InputStream
    ) : InputStream() {
        private val headerStream = ByteArrayInputStream(header, 0, headerLength)
        private val mainStream = rest

        override fun read(): Int {
            val b = headerStream.read()
            return if (b != -1) b else mainStream.read()
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len <= 0) return 0
            val headerRead = headerStream.read(b, off, len)
            if (headerRead == -1) {
                return mainStream.read(b, off, len)
            }
            if (headerRead < len) {
                val restRead = mainStream.read(b, off + headerRead, len - headerRead)
                if (restRead > 0) {
                    return headerRead + restRead
                }
            }
            return headerRead
        }

        override fun close() {
            headerStream.close()
            mainStream.close()
        }
    }
}
