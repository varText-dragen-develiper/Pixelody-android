package com.pixelody.app.core.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class EmbeddedLyricsExtractorTest {

    @Test
    fun testExtractId3v2UsltFrame() {
        val lyricsText = "These are embedded unsynced lyrics\nOver multiple lines"
        val textBytes = lyricsText.toByteArray(StandardCharsets.ISO_8859_1)

        // ID3 USLT payload: encoding (1 byte = 0), lang (3 bytes = 'eng'), descriptor ('\0' = 1 byte), text
        val framePayloadSize = 1 + 3 + 1 + textBytes.size
        val frameData = ByteArray(10 + framePayloadSize)

        // Frame header: "USLT"
        System.arraycopy("USLT".toByteArray(StandardCharsets.ISO_8859_1), 0, frameData, 0, 4)
        // Frame size (big-endian 32-bit for ID3v2.3)
        frameData[4] = ((framePayloadSize shr 24) and 0xFF).toByte()
        frameData[5] = ((framePayloadSize shr 16) and 0xFF).toByte()
        frameData[6] = ((framePayloadSize shr 8) and 0xFF).toByte()
        frameData[7] = (framePayloadSize and 0xFF).toByte()
        // Flags: 0, 0
        frameData[8] = 0
        frameData[9] = 0

        // Payload
        frameData[10] = 0 // ISO-8859-1
        frameData[11] = 'e'.code.toByte()
        frameData[12] = 'n'.code.toByte()
        frameData[13] = 'g'.code.toByte()
        frameData[14] = 0 // descriptor terminator
        System.arraycopy(textBytes, 0, frameData, 15, textBytes.size)

        // Overall ID3 tag: header (10 bytes) + frameData
        val totalTagSize = frameData.size
        val tagBuffer = ByteArray(10 + totalTagSize)
        tagBuffer[0] = 'I'.code.toByte()
        tagBuffer[1] = 'D'.code.toByte()
        tagBuffer[2] = '3'.code.toByte()
        tagBuffer[3] = 3 // ID3v2.3
        tagBuffer[4] = 0
        tagBuffer[5] = 0
        // Synchsafe size (7 bits per byte)
        tagBuffer[6] = ((totalTagSize shr 21) and 0x7F).toByte()
        tagBuffer[7] = ((totalTagSize shr 14) and 0x7F).toByte()
        tagBuffer[8] = ((totalTagSize shr 7) and 0x7F).toByte()
        tagBuffer[9] = (totalTagSize and 0x7F).toByte()
        System.arraycopy(frameData, 0, tagBuffer, 10, frameData.size)

        val stream = ByteArrayInputStream(tagBuffer)
        val result = EmbeddedLyricsExtractor.extractFromStream(stream, "MP3")

        assertNotNull(result)
        assertEquals(lyricsText, result?.lyricsText)
        assertEquals("ID3v2 USLT", result?.tagFormat)
        assertTrue(result?.isSynchronized == false)
    }

    @Test
    fun testExtractVorbisCommentBlock() {
        val lyricField = "LYRICS=[00:10.00]Opening chord\n[00:20.00]First verse"
        val commentBytes = lyricField.toByteArray(StandardCharsets.UTF_8)
        val vendorBytes = "reference libFLAC".toByteArray(StandardCharsets.UTF_8)

        // Vorbis comment payload: vendorLen (4 LE) + vendor + count (4 LE) + commentLen (4 LE) + comment
        val payloadLen = 4 + vendorBytes.size + 4 + 4 + commentBytes.size
        val vorbisPayload = ByteArray(payloadLen)
        var cursor = 0

        // vendorLen
        vorbisPayload[cursor++] = (vendorBytes.size and 0xFF).toByte()
        vorbisPayload[cursor++] = ((vendorBytes.size shr 8) and 0xFF).toByte()
        vorbisPayload[cursor++] = ((vendorBytes.size shr 16) and 0xFF).toByte()
        vorbisPayload[cursor++] = ((vendorBytes.size shr 24) and 0xFF).toByte()
        System.arraycopy(vendorBytes, 0, vorbisPayload, cursor, vendorBytes.size)
        cursor += vendorBytes.size

        // count = 1
        vorbisPayload[cursor++] = 1
        vorbisPayload[cursor++] = 0
        vorbisPayload[cursor++] = 0
        vorbisPayload[cursor++] = 0

        // commentLen
        vorbisPayload[cursor++] = (commentBytes.size and 0xFF).toByte()
        vorbisPayload[cursor++] = ((commentBytes.size shr 8) and 0xFF).toByte()
        vorbisPayload[cursor++] = ((commentBytes.size shr 16) and 0xFF).toByte()
        vorbisPayload[cursor++] = ((commentBytes.size shr 24) and 0xFF).toByte()
        System.arraycopy(commentBytes, 0, vorbisPayload, cursor, commentBytes.size)

        // Wrap in FLAC block: magic 'fLaC' (4 bytes) + block header (4 bytes: type 4 | last bit, 3 bytes len) + payload
        val flacData = ByteArray(4 + 4 + vorbisPayload.size)
        flacData[0] = 'f'.code.toByte()
        flacData[1] = 'L'.code.toByte()
        flacData[2] = 'a'.code.toByte()
        flacData[3] = 'C'.code.toByte()

        // Block header: isLast (0x80) | type 4 = 0x84
        flacData[4] = 0x84.toByte()
        flacData[5] = ((vorbisPayload.size shr 16) and 0xFF).toByte()
        flacData[6] = ((vorbisPayload.size shr 8) and 0xFF).toByte()
        flacData[7] = (vorbisPayload.size and 0xFF).toByte()
        System.arraycopy(vorbisPayload, 0, flacData, 8, vorbisPayload.size)

        val stream = ByteArrayInputStream(flacData)
        val result = EmbeddedLyricsExtractor.extractFromStream(stream, "FLAC")

        assertNotNull(result)
        assertEquals("[00:10.00]Opening chord\n[00:20.00]First verse", result?.lyricsText)
        assertTrue(result?.isSynchronized == true)
        assertEquals("Vorbis LYRICS", result?.tagFormat)
    }

    @Test
    fun testExtractNonAudioReturnsNull() {
        // Binary or plain text stream without ID3v2, FLAC, or MP4 headers should return null
        val randomBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
        val stream = ByteArrayInputStream(randomBytes)
        val result = EmbeddedLyricsExtractor.extractFromStream(stream)
        org.junit.Assert.assertNull(result)
    }

    @Test
    fun testExtractMp4LyrAtom() {
        val lyricsText = "MP4 embedded lyrics line"
        val textBytes = lyricsText.toByteArray(StandardCharsets.UTF_8)
        // MP4 atom structure:
        // Header: 4 bytes length, 'ftyp' (4 bytes), 4 bytes minor_version = 12 bytes
        // ... then ©lyr atom: 4 bytes len, '©lyr', 'data' atom (4 bytes len, 'data', 4 flags, 4 empty, text)
        val dataAtomSize = 8 + 8 + textBytes.size // 4 len + 4 'data' + 4 flags + 4 locale + text
        val lyrAtomSize = 8 + dataAtomSize

        val totalSize = 12 + lyrAtomSize
        val buffer = ByteArray(totalSize)
        var cursor = 0

        // ftyp header
        buffer[cursor++] = 0
        buffer[cursor++] = 0
        buffer[cursor++] = 0
        buffer[cursor++] = 12 // header length
        buffer[cursor++] = 'f'.code.toByte()
        buffer[cursor++] = 't'.code.toByte()
        buffer[cursor++] = 'y'.code.toByte()
        buffer[cursor++] = 'p'.code.toByte()
        buffer[cursor++] = 'M'.code.toByte()
        buffer[cursor++] = '4'.code.toByte()
        buffer[cursor++] = 'A'.code.toByte()
        buffer[cursor++] = ' '.code.toByte()

        // ©lyr atom
        buffer[cursor++] = ((lyrAtomSize shr 24) and 0xFF).toByte()
        buffer[cursor++] = ((lyrAtomSize shr 16) and 0xFF).toByte()
        buffer[cursor++] = ((lyrAtomSize shr 8) and 0xFF).toByte()
        buffer[cursor++] = (lyrAtomSize and 0xFF).toByte()
        buffer[cursor++] = 0xA9.toByte()
        buffer[cursor++] = 'l'.code.toByte()
        buffer[cursor++] = 'y'.code.toByte()
        buffer[cursor++] = 'r'.code.toByte()

        // data atom
        buffer[cursor++] = ((dataAtomSize shr 24) and 0xFF).toByte()
        buffer[cursor++] = ((dataAtomSize shr 16) and 0xFF).toByte()
        buffer[cursor++] = ((dataAtomSize shr 8) and 0xFF).toByte()
        buffer[cursor++] = (dataAtomSize and 0xFF).toByte()
        buffer[cursor++] = 'd'.code.toByte()
        buffer[cursor++] = 'a'.code.toByte()
        buffer[cursor++] = 't'.code.toByte()
        buffer[cursor++] = 'a'.code.toByte()
        buffer[cursor++] = 0 // flags
        buffer[cursor++] = 0
        buffer[cursor++] = 0
        buffer[cursor++] = 1
        buffer[cursor++] = 0 // empty
        buffer[cursor++] = 0
        buffer[cursor++] = 0
        buffer[cursor++] = 0
        System.arraycopy(textBytes, 0, buffer, cursor, textBytes.size)

        val stream = ByteArrayInputStream(buffer)
        val result = EmbeddedLyricsExtractor.extractFromStream(stream, "M4A")
        assertNotNull(result)
        assertEquals(lyricsText, result?.lyricsText)
        assertEquals("MP4 ©lyr", result?.tagFormat)
    }
}
