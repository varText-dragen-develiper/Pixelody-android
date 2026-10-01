package com.pixelody.app.core.hosting

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidLibraryHostContractTest {

    @Test
    fun defaultHostStatusIsInactive() {
        val status = AndroidHostStatus()
        assertFalse(status.running)
        assertEquals("", status.baseUrl)
        assertEquals("", status.localBaseUrl)
        assertEquals("", status.token)
        assertEquals(0, status.trackCount)
        assertEquals("", status.startedAt)
        assertEquals("", status.lastError)
    }

    @Test
    fun parseRangeHandlesValidAndEdgeCases() {
        val totalSize = 1000L

        // Null or blank range returns null (full file)
        assertNull(AndroidLibraryHost.parseRange(null, totalSize))
        assertNull(AndroidLibraryHost.parseRange("", totalSize))
        assertNull(AndroidLibraryHost.parseRange("   ", totalSize))

        // Non-positive total size returns null
        assertNull(AndroidLibraryHost.parseRange("bytes=0-100", 0L))
        assertNull(AndroidLibraryHost.parseRange("bytes=0-100", -10L))

        // Standard explicit range: bytes=0-499
        val standardRange = AndroidLibraryHost.parseRange("bytes=0-499", totalSize)
        assertNotNull(standardRange)
        assertFalse(standardRange!!.invalid)
        assertEquals(0L, standardRange.start)
        assertEquals(499L, standardRange.end)

        // Open-ended range: bytes=500-
        val openEnded = AndroidLibraryHost.parseRange("bytes=500-", totalSize)
        assertNotNull(openEnded)
        assertFalse(openEnded!!.invalid)
        assertEquals(500L, openEnded.start)
        assertEquals(999L, openEnded.end)

        // Suffix range: bytes=-200
        val suffix = AndroidLibraryHost.parseRange("bytes=-200", totalSize)
        assertNotNull(suffix)
        assertFalse(suffix!!.invalid)
        assertEquals(800L, suffix.start)
        assertEquals(999L, suffix.end)

        // Suffix larger than total size clamps start to 0
        val largeSuffix = AndroidLibraryHost.parseRange("bytes=-5000", totalSize)
        assertNotNull(largeSuffix)
        assertFalse(largeSuffix!!.invalid)
        assertEquals(0L, largeSuffix.start)
        assertEquals(999L, largeSuffix.end)

        // Invalid: start >= size
        val outOfBoundsStart = AndroidLibraryHost.parseRange("bytes=1000-1050", totalSize)
        assertNotNull(outOfBoundsStart)
        assertTrue(outOfBoundsStart!!.invalid)

        // Invalid: end < start
        val invertedRange = AndroidLibraryHost.parseRange("bytes=500-200", totalSize)
        assertNotNull(invertedRange)
        assertTrue(invertedRange!!.invalid)

        // Invalid format
        val malformed = AndroidLibraryHost.parseRange("not-a-range", totalSize)
        assertNotNull(malformed)
        assertTrue(malformed!!.invalid)
    }

    @Test
    fun mimeForMapsAudioFormatsAccurately() {
        fun trackWithFormat(format: String) = Track(
            id = "test-1",
            title = "Test",
            artist = "Artist",
            album = "Album",
            durationSeconds = 180,
            format = format,
            codec = format,
            sampleRate = 44100,
            bitDepth = 16,
            bitrate = 1411,
            channels = 2,
            lossless = true,
            streamUrl = "content://audio/1"
        )

        assertEquals("audio/flac", AndroidLibraryHost.mimeFor(trackWithFormat("FLAC")))
        assertEquals("audio/flac", AndroidLibraryHost.mimeFor(trackWithFormat("flac")))
        assertEquals("audio/wav", AndroidLibraryHost.mimeFor(trackWithFormat("WAV")))
        assertEquals("audio/wav", AndroidLibraryHost.mimeFor(trackWithFormat("WAVE")))
        assertEquals("audio/mpeg", AndroidLibraryHost.mimeFor(trackWithFormat("MP3")))
        assertEquals("audio/mpeg", AndroidLibraryHost.mimeFor(trackWithFormat("mp3")))
        assertEquals("audio/mp4", AndroidLibraryHost.mimeFor(trackWithFormat("M4A")))
        assertEquals("audio/mp4", AndroidLibraryHost.mimeFor(trackWithFormat("AAC")))
        assertEquals("audio/mp4", AndroidLibraryHost.mimeFor(trackWithFormat("ALAC")))
        assertEquals("audio/ogg", AndroidLibraryHost.mimeFor(trackWithFormat("OGG")))
        assertEquals("audio/ogg", AndroidLibraryHost.mimeFor(trackWithFormat("OPUS")))
        assertEquals("application/octet-stream", AndroidLibraryHost.mimeFor(trackWithFormat("DSD")))
        assertEquals("application/octet-stream", AndroidLibraryHost.mimeFor(trackWithFormat("UNKNOWN")))
    }

    @Test
    fun publicIdForGeneratesDeterministicSha256Prefix() {
        val id1 = AndroidLibraryHost.publicIdFor("content://media/external/audio/media/12345")
        val id2 = AndroidLibraryHost.publicIdFor("content://media/external/audio/media/12345")
        val idDifferent = AndroidLibraryHost.publicIdFor("content://media/external/audio/media/99999")

        assertEquals(32, id1.length) // 16 bytes = 32 hex chars
        assertEquals(id1, id2)
        assertTrue(id1 != idDifferent)
        assertTrue(id1.matches(Regex("^[0-9a-f]{32}$")))
    }

    @Test
    fun parseQueryDecodesParameters() {
        val empty = AndroidLibraryHost.parseQuery("")
        assertTrue(empty.isEmpty())

        val query = AndroidLibraryHost.parseQuery("token=secure_tok_123&track=acoustic%20anthem&filter=hi-res")
        assertEquals(3, query.size)
        assertEquals("secure_tok_123", query["token"])
        assertEquals("acoustic anthem", query["track"])
        assertEquals("hi-res", query["filter"])
    }

    @Test
    fun isAuthorizedValidatesBearerHeaderAndQueryToken() {
        val secret = "valid_host_token_98765"

        // Valid Bearer in authorization header
        val validHeader = mapOf("authorization" to "Bearer $secret")
        assertTrue(AndroidLibraryHost.isAuthorized(secret, validHeader, emptyMap()))

        // Case-insensitive Bearer
        val caseInsensitiveHeader = mapOf("authorization" to "bearer $secret")
        assertTrue(AndroidLibraryHost.isAuthorized(secret, caseInsensitiveHeader, emptyMap()))

        // Valid query parameter fallback
        val validQuery = mapOf("token" to secret)
        assertTrue(AndroidLibraryHost.isAuthorized(secret, emptyMap(), validQuery))

        // Mismatched header
        val wrongHeader = mapOf("authorization" to "Bearer wrong_token")
        assertFalse(AndroidLibraryHost.isAuthorized(secret, wrongHeader, emptyMap()))

        // Mismatched query
        val wrongQuery = mapOf("token" to "wrong_token")
        assertFalse(AndroidLibraryHost.isAuthorized(secret, emptyMap(), wrongQuery))

        // Empty / missing credentials
        assertFalse(AndroidLibraryHost.isAuthorized(secret, emptyMap(), emptyMap()))
        assertFalse(AndroidLibraryHost.isAuthorized("", validHeader, emptyMap()))
    }
}
