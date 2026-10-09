package com.pixelody.app.data.network

import com.pixelody.app.data.fixtures.FakePixelodyHost
import com.pixelody.app.data.model.HostConnectionDetails
import com.pixelody.app.data.repository.HostRepository
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PairingAddressFallbackTest {
    @Test fun libraryFailureAfterPairingReusesCredentialAtNextAddress() = runBlocking {
        val requests = CopyOnWriteArrayList<String>()
        val first = TestHost { route, _ ->
            requests += "first:$route"
            val pairing = route == "/api/v1/pair/complete"
            val body = if (pairing) """{"token":"test-token","device":{"permissions":["browse","stream"]}}"""
                else """{"code":"host_busy"}"""
            (if (pairing) 200 else 503) to body
        }
        val second = TestHost { route, authorization ->
            requests += "second:$route"
            val body = when (route) {
                "/api/v1/pair/complete" -> """{"code":"pairing_not_found"}"""
                "/api/v1/library/snapshot" -> """{"revision":1}"""
                "/api/v1/library/tracks" -> """{"revision":1,"tracks":[],"page":{"nextOffset":null}}"""
                else -> "{}"
            }
            if (route != "/api/v1/server-info") {
                requests += "credential:${authorization == "Bearer test-token"}"
            }
            (if (route == "/api/v1/pair/complete") 400 else 200) to body
        }
        try {
            val firstUrl = first.baseUrl
            val secondUrl = second.baseUrl
            val result = HostRepository(FakePixelodyHost()).connectHost(HostConnectionDetails(
                baseUrl = firstUrl, baseUrls = listOf(firstUrl, secondUrl),
                pairingCode = "042917", pairingSecret = "one-use-secret"
            ))
            assertEquals(secondUrl, result.snapshot.host.baseUrl)
            assertEquals("test-token", result.token)
            assertEquals(listOf("browse", "stream"), result.grantedPermissions)
            assertEquals(1, requests.count { it.endsWith("/api/v1/pair/complete") })
            assertFalse(requests.contains("credential:false"))
        } finally {
            first.close()
            second.close()
        }
    }

    private class TestHost(handler: (String, String?) -> Pair<Int, String>) : AutoCloseable {
        private val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val baseUrl = "http://127.0.0.1:${server.localPort}"
        private val worker = thread(isDaemon = true) {
            try {
                while (!server.isClosed) {
                    server.accept().use { socket ->
                        socket.soTimeout = 3000
                        val reader = socket.getInputStream().bufferedReader()
                        val route = reader.readLine().split(' ')[1].substringBefore('?')
                        var authorization: String? = null
                        var length = 0
                        while (true) {
                            val header = reader.readLine() ?: break
                            if (header.isEmpty()) break
                            if (header.startsWith("Authorization:", ignoreCase = true)) authorization = header.substringAfter(':').trim()
                            if (header.startsWith("Content-Length:", ignoreCase = true)) length = header.substringAfter(':').trim().toInt()
                        }
                        var consumed = 0
                        val bodyBuffer = CharArray(length)
                        while (consumed < length) {
                            val count = reader.read(bodyBuffer, consumed, length - consumed)
                            if (count < 0) break
                            consumed += count
                        }
                        val (status, body) = handler(route, authorization)
                        socket.getOutputStream().write(("HTTP/1.1 $status Test\r\nContent-Type: application/json\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n$body").toByteArray())
                    }
                }
            } catch (error: SocketException) {
                if (!server.isClosed) throw error
            }
        }

        override fun close() {
            server.close()
            worker.join(3000)
        }
    }
}
