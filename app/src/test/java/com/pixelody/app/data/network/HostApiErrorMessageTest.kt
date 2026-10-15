package com.pixelody.app.data.network

import com.pixelody.app.data.model.HostConnectionState
import java.net.ConnectException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HostApiErrorMessageTest {
    @Test fun pairingAndNetworkFailuresGiveDifferentRecoveryWithoutLeakingSecrets() {
        val expired = HostApiException(400, "pairing_not_found", "secret-token-at-http://host")
        val invalid = HostApiException(400, "pairing_secret_invalid", "secret-token-at-http://host")
        assertTrue(hostConnectionFailureMessage(IllegalStateException("outer", expired)).contains("fresh QR"))
        assertTrue(hostConnectionFailureMessage(invalid).contains("rejected"))
        assertTrue(connectionStateFor(expired) == HostConnectionState.CredentialExpired)
        assertTrue(connectionStateFor(invalid) == HostConnectionState.AuthFailed)
        assertTrue(!hostConnectionFailureMessage(expired).contains("secret-token"))
        val network = HostNetworkException("network_unavailable", HostConnectionState.NetworkUnavailable, "secret-token", ConnectException())
        assertTrue(hostConnectionFailureMessage(network).contains("No route"))
        assertTrue(!hostConnectionFailureMessage(network).contains("secret-token"))
    }
    @Test
    fun authenticationCodesRemainDistinctAndActionable() {
        val invalid = hostApiErrorMessage(401, "auth_invalid")
        val expired = hostApiErrorMessage(401, "auth_expired")
        val revoked = hostApiErrorMessage(401, "auth_revoked")

        assertTrue(invalid.contains("not valid"))
        assertTrue(expired.contains("expired"))
        assertTrue(revoked.contains("revoked"))
        assertTrue(setOf(invalid, expired, revoked).size == 3)
    }

    @Test
    fun rateLimitCodeSuggestsRetryingLater() {
        assertTrue(hostApiErrorMessage(429, "command_rate_limited").contains("Wait"))
    }

    @Test
    fun structuredFailuresMapWithoutMessageInspection() {
        assertTrue(connectionStateFor(HostApiException(401, "auth_expired", "localized")) == HostConnectionState.CredentialExpired)
        assertTrue(connectionStateFor(HostApiException(401, "auth_revoked", "localized")) == HostConnectionState.Revoked)
        assertTrue(connectionStateFor(HostNetworkException("host_unavailable", HostConnectionState.HostUnavailable, "localized", ConnectException())) == HostConnectionState.HostUnavailable)
    }

    @Test
    fun pollingBackoffIsBoundedAndResets() {
        assertTrue(pollingBackoffMs(0) == 1000L)
        assertTrue(pollingBackoffMs(1) == 2500L)
        assertTrue(pollingBackoffMs(4) > pollingBackoffMs(2))
        assertTrue(pollingBackoffMs(100) == 30000L)
    }

    @Test
    fun pollingCancellationIsNeverConvertedIntoAConnectionFailure() {
        val cancellation = CancellationException("host changed")
        val thrown = runCatching { cancellation.rethrowIfPollingCancellation() }.exceptionOrNull()
        assertSame(cancellation, thrown)
    }
}
