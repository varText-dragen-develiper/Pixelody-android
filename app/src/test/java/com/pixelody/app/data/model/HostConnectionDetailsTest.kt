package com.pixelody.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostConnectionDetailsTest {
    @Test
    fun copiedTrustedDeviceInviteParsesTokenAndCandidateUrls() {
        val details = HostConnectionDetails.fromText(
            """
            {
              "type": "pixelody-connect",
              "baseUrl": "http://127.0.0.1:3819",
              "localBaseUrl": "http://127.0.0.1:3819",
              "networkBaseUrls": [
                { "baseUrl": "http://192.168.1.45:3819" }
              ],
              "android": {
                "preferredBaseUrl": "http://192.168.1.45:3819",
                "baseUrls": ["http://100.72.0.9:3819"]
              },
              "token": "pxd_test_token"
            }
            """.trimIndent()
        )

        requireNotNull(details)
        assertEquals("http://192.168.1.45:3819", details.baseUrl)
        assertEquals("pxd_test_token", details.token)
        assertTrue(details.hasToken)
        assertFalse(details.hasPairingSecret)
        assertTrue(details.baseUrls.contains("http://100.72.0.9:3819"))
        assertTrue(details.baseUrls.contains("http://127.0.0.1:3819"))
    }

    @Test
    fun qrReadyPairingPayloadParsesWithoutLongLivedToken() {
        val details = HostConnectionDetails.fromText(
            """
            {
              "type": "pixelody-pairing",
              "baseUrl": "http://127.0.0.1:4822",
              "baseUrls": ["http://192.168.1.45:4822"],
              "pairingCode": "042917",
              "secret": "pairing-secret"
            }
            """.trimIndent()
        )

        requireNotNull(details)
        assertEquals("http://127.0.0.1:4822", details.baseUrl)
        assertEquals("042917", details.pairingCode)
        assertEquals("pairing-secret", details.pairingSecret)
        assertFalse(details.hasToken)
        assertTrue(details.hasPairingSecret)
    }

    @Test
    fun compactQrDeepLinkParsesPairingPayload() {
        val details = HostConnectionDetails.fromText(
            "pixelody://connect?u=http%3A%2F%2F192.168.1.45%3A4822&c=042917&s=pairing-secret"
        )

        requireNotNull(details)
        assertEquals("http://192.168.1.45:4822", details.baseUrl)
        assertEquals("042917", details.pairingCode)
        assertEquals("pairing-secret", details.pairingSecret)
        assertTrue(details.hasPairingSecret)
    }

    @Test
    fun tinyQrPayloadParsesPairingPayload() {
        val details = HostConnectionDetails.fromText(
            "pxd1|http%3A%2F%2F192.168.1.45%3A4822|042917|pairing-secret"
        )

        requireNotNull(details)
        assertEquals("http://192.168.1.45:4822", details.baseUrl)
        assertEquals("042917", details.pairingCode)
        assertEquals("pairing-secret", details.pairingSecret)
        assertTrue(details.hasPairingSecret)
    }

    @Test
    fun tinyQrPayloadParsesColonPrefixAndHiddenMarkers() {
        val details = HostConnectionDetails.fromText(
            "\uFEFFpxd1:http%3A%2F%2F192.168.1.45%3A4822|042917|pairing-secret"
        )

        requireNotNull(details)
        assertEquals("http://192.168.1.45:4822", details.baseUrl)
        assertEquals("042917", details.pairingCode)
        assertEquals("pairing-secret", details.pairingSecret)
        assertTrue(details.hasPairingSecret)
    }
}
