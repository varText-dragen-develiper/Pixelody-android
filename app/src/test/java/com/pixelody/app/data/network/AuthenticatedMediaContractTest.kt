package com.pixelody.app.data.network

import com.pixelody.app.data.storage.toOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticatedMediaContractTest {
    @Test
    fun mediaUrlsNeverReceiveReusableCredentials() {
        val stream = absoluteMediaUrl("http://192.168.1.45:4822", "/api/v1/tracks/public-1/stream")

        assertEquals("http://192.168.1.45:4822/api/v1/tracks/public-1/stream", stream)
        assertFalse(stream.orEmpty().contains("token="))
        assertFalse(stream.orEmpty().contains("access_token="))
    }

    @Test
    fun credentialOriginsRequireExactSchemeHostAndPort() {
        assertEquals("http://192.168.1.45:4822", "http://192.168.1.45:4822/api/v1/live".toOrigin())
        assertEquals("https://pixelody.local:443", "https://pixelody.local/library".toOrigin())
        assertNull("content://phone/audio/1".toOrigin())
        assertTrue("http://192.168.1.45.evil.test:4822".toOrigin() != "http://192.168.1.45:4822")

        val allowed = setOf("http://127.0.0.1:4822", "http://192.168.1.45:4822")
        assertTrue(com.pixelody.app.data.storage.isOriginEquivalent("http://localhost:4822", allowed))
        assertTrue(com.pixelody.app.data.storage.isOriginEquivalent("http://10.0.2.2:4822", allowed))
        assertFalse(com.pixelody.app.data.storage.isOriginEquivalent("http://192.168.1.99:4822", allowed))
        assertFalse(com.pixelody.app.data.storage.isOriginEquivalent("http://localhost:8080", allowed))
    }
}
