package com.pixelody.app.feature.connection

import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.HostingVisibility
import com.pixelody.app.data.model.NetworkMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionVocabularyTest {

    @Test
    fun hostSourceNeedsRecovery_classifiesCorrectly() {
        val recoveryStates = listOf(
            HostConnectionState.AuthFailed,
            HostConnectionState.PermissionDenied,
            HostConnectionState.Revoked,
            HostConnectionState.CredentialExpired,
            HostConnectionState.NetworkUnavailable,
            HostConnectionState.HostUnavailable,
            HostConnectionState.Unreachable,
            HostConnectionState.Offline
        )
        val nonRecoveryStates = listOf(
            HostConnectionState.Disconnected,
            HostConnectionState.Connecting,
            HostConnectionState.Connected,
            HostConnectionState.Reconnecting
        )

        for (state in recoveryStates) {
            assertTrue("Expected $state to need recovery", hostSourceNeedsRecovery(state))
        }
        for (state in nonRecoveryStates) {
            assertFalse("Expected $state to not need recovery", hostSourceNeedsRecovery(state))
        }
    }

    @Test
    fun connectionGuidanceFor_returnsAppropriateCopy() {
        assertTrue(
            connectionGuidanceFor(HostConnectionState.AuthFailed, "").contains("Pair again with a fresh QR invite")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.PermissionDenied, "").contains("Check trusted-device permissions")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.Revoked, "").contains("This device was revoked")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.CredentialExpired, "").contains("credential expired")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.Reconnecting, "").contains("retrying with bounded backoff")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.NetworkUnavailable, "").contains("Reconnect Wi-Fi")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.HostUnavailable, "").contains("Confirm the PC is awake")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.Offline, "").contains("Restart J.A.M. on the PC")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.Connecting, "").contains("trying the advertised host addresses")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.Connected, "").contains("Soft Refresh J.A.M. Devices")
        )
        assertTrue(
            connectionGuidanceFor(HostConnectionState.Disconnected, "").contains("Scan a Pixelody QR invite")
        )

        // Unreachable with hints
        val firewallGuidance = connectionGuidanceFor(HostConnectionState.Unreachable, "Windows Firewall blocked port")
        assertTrue("Firewall guidance expected, got: $firewallGuidance", firewallGuidance.contains("Windows Firewall"))

        val wifiGuidance = connectionGuidanceFor(HostConnectionState.Unreachable, "No route to Wi-Fi host")
        assertTrue("Wi-Fi guidance expected, got: $wifiGuidance", wifiGuidance.contains("same Wi-Fi/private network"))

        val defaultUnreachable = connectionGuidanceFor(HostConnectionState.Unreachable, "Connection reset")
        assertTrue("Generic unreachable guidance expected, got: $defaultUnreachable", defaultUnreachable.contains("Private-network hosting is enabled"))
    }

    @Test
    fun connectionStateLabels_arePresent() {
        assertEquals("Not connected", HostConnectionState.Disconnected.label)
        assertEquals("Connecting", HostConnectionState.Connecting.label)
        assertEquals("Connected", HostConnectionState.Connected.label)
        assertEquals("Reconnecting", HostConnectionState.Reconnecting.label)
        assertEquals("Host offline", HostConnectionState.Offline.label)
        assertEquals("Can't reach host", HostConnectionState.Unreachable.label)
        assertEquals("Host not responding", HostConnectionState.HostUnavailable.label)
        assertEquals("No network", HostConnectionState.NetworkUnavailable.label)
        assertEquals("Pairing failed", HostConnectionState.AuthFailed.label)
        assertEquals("Pairing expired", HostConnectionState.CredentialExpired.label)
        assertEquals("Permission denied", HostConnectionState.PermissionDenied.label)
        assertEquals("Access revoked", HostConnectionState.Revoked.label)
    }

    @Test
    fun networkModeAndHostingVisibilityLabels() {
        assertEquals("Auto", NetworkMode.Auto.label)
        assertEquals("Local only", NetworkMode.LocalOnly.label)
        assertEquals("Direct remote", NetworkMode.DirectRemote.label)
        assertEquals("My relay", NetworkMode.SelfHostedRelay.label)
        assertEquals("Relay fallback", NetworkMode.ManagedRelayFallback.label)

        assertEquals("Off", HostingVisibility.Off.label)
        assertEquals("This device", HostingVisibility.ThisDeviceOnly.label)
        assertEquals("Local network", HostingVisibility.LocalNetwork.label)
        assertEquals("Remote allowed", HostingVisibility.RemoteAllowed.label)
    }

    @Test
    fun qrDebugPreview_formatsCorrectly() {
        val nullPreview: String? = null
        assertEquals("<empty>", nullPreview.toQrDebugPreview())
        assertEquals("<empty>", "".toQrDebugPreview())
        assertEquals("hello\\nworld", "hello\nworld".toQrDebugPreview())

        val longString = "a".repeat(200)
        val truncated = longString.toQrDebugPreview()
        assertEquals(160, truncated.length)
        assertTrue(truncated.endsWith("..."))
    }
}
