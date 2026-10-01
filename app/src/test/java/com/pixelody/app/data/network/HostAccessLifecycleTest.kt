package com.pixelody.app.data.network

import com.pixelody.app.core.playback.isRemoteHostMedia
import com.pixelody.app.core.playback.remoteMediaIndices
import com.pixelody.app.data.fixtures.FakePixelodyHost
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.HostConnectionDetails
import com.pixelody.app.data.repository.HostRepository
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HostAccessLifecycleTest {
    @Test fun teardownClearsRuntimeEvenWhenDurableErasureFails() {
        val calls = mutableListOf<String>()
        val cleared = endHostAccess(
            clearRuntime = { calls += "runtime" },
            clearCredential = { calls += "credential"; error("storage unavailable") }
        )
        assertFalse(cleared)
        assertEquals(listOf("runtime", "credential"), calls)
        assertTrue(endHostAccess({}, {}))
    }

    @Test fun revocationAndNewConnectionsRejectLateResults() {
        val requests = HostRequestGeneration()
        val pending = requests.current
        assertTrue(requests.accepts(pending))
        requests.invalidate()
        assertFalse(requests.accepts(pending))
        val next = requests.current
        requests.invalidate()
        assertFalse(requests.accepts(next))
        assertTrue(requests.accepts(requests.current))
    }

    @Test fun cleanupPreservesLocalMediaAndRemovesEveryRemoteQueueEntry() {
        assertEquals(listOf(3, 1), remoteMediaIndices(listOf(
            "content://media/external/audio/1", "http://127.0.0.1:47813/stream",
            "file:///local/music.wav", "HTTPS://host.local/artwork"
        )))
        assertFalse(isRemoteHostMedia(null))
        assertFalse(isRemoteHostMedia("content://media/external/audio/1"))
        assertTrue(isRemoteHostMedia("http://host.local/stream"))
    }

    @Test fun onlyTerminalAuthenticationEndsHostAccess() {
        assertTrue(HostConnectionState.Revoked.endsHostAccess())
        assertTrue(HostConnectionState.CredentialExpired.endsHostAccess())
        assertTrue(HostConnectionState.AuthFailed.endsHostAccess())
        assertFalse(HostConnectionState.PermissionDenied.endsHostAccess())
        assertFalse(HostConnectionState.HostUnavailable.endsHostAccess())
        val cancellation = CancellationException("cancelled")
        try { rethrowTerminalHostFailure(cancellation); fail("Cancellation swallowed") }
        catch (error: CancellationException) { assertSame(cancellation, error) }
    }

    @Test fun realHttpRevocationWinsOverLaterUnreachableCandidateForBothReconnectPaths() = runBlocking {
        val host = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        host.soTimeout = 3000
        val worker = thread(isDaemon = true) {
            repeat(2) {
                host.accept().use { socket ->
                    socket.soTimeout = 2000
                    val reader = socket.getInputStream().bufferedReader()
                    while (!reader.readLine().isNullOrEmpty()) { /* consume headers without recording them */ }
                    val body = """{"code":"auth_revoked","message":"Access revoked."}"""
                    socket.getOutputStream().write(("HTTP/1.1 401 Unauthorized\r\nContent-Type: application/json\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body").toByteArray())
                }
            }
        }
        try {
            val baseUrl = "http://127.0.0.1:${host.localPort}"
            val candidates = listOf(baseUrl, "http://127.0.0.1:1")
            val repository = HostRepository(FakePixelodyHost())
            for (pairingDetails in listOf(false, true)) {
                val error = runCatching {
                    if (pairingDetails) repository.connectHost(HostConnectionDetails(baseUrl = baseUrl, baseUrls = candidates, token = "fixture-credential"))
                    else repository.connectHost(candidates, "fixture-credential")
                }.exceptionOrNull()
                assertNotNull(error)
                assertEquals(HostConnectionState.Revoked, connectionStateFor(error!!))
            }
        } finally { host.close(); worker.join(3000) }
    }
}
