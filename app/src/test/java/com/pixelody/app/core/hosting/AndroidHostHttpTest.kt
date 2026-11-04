package com.pixelody.app.core.hosting

import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class AndroidHostHttpTest {
    private fun pair(block: (Socket, Socket) -> Unit) {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            Socket("127.0.0.1", server.localPort).use { sender ->
                server.accept().use { receiver -> block(sender, receiver) }
            }
        }
    }

    private fun rejected(request: String) = pair { sender, receiver ->
        sender.getOutputStream().write(request.toByteArray(Charsets.ISO_8859_1))
        assertThrows(IOException::class.java) { readAndroidHostRequest(receiver, 1_000) }
    }

    @Test fun normalGetHeadTokenAndRangeRemainAvailable() {
        for (method in listOf("GET", "HEAD")) pair { sender, receiver ->
            sender.getOutputStream().write(("$method /api/v1/tracks/abc/stream?token=secret HTTP/1.1\r\n" +
                "Host: localhost\r\nAuthorization: Bearer secret\r\nRange: bytes=4-7\r\n\r\n").toByteArray())
            val request = readAndroidHostRequest(receiver)
            assertEquals(method, request.method)
            assertEquals("/api/v1/tracks/abc/stream?token=secret", request.target)
            assertEquals("Bearer secret", request.headers["authorization"])
            assertEquals("bytes=4-7", request.headers["range"])
            assertEquals(0, receiver.soTimeout)
        }
    }

    @Test fun oversizedRequestAndIndividualHeaderAreRejectedBeforeRouting() {
        rejected("GET /${"x".repeat(4_096)} HTTP/1.1\r\n\r\n")
        rejected("GET / HTTP/1.1\r\nX-Test: ${"x".repeat(8_192)}\r\n\r\n")
    }

    @Test fun aggregateBytesAndHeaderCountAreBounded() {
        rejected("GET / HTTP/1.1\r\n" + (1..3).joinToString("") { "X-$it: ${"x".repeat(6_000)}\r\n" } + "\r\n")
        rejected("GET / HTTP/1.1\r\n" + (1..65).joinToString("") { "X-$it: x\r\n" } + "\r\n")
    }

    @Test fun malformedAndAmbiguousHeadersAreRejected() {
        rejected("GET / HTTP/1.1\r\nAuthorization: Bearer one\r\nAuthorization: Bearer two\r\n\r\n")
        rejected("GET / HTTP/1.1\r\n folded: value\r\n\r\n")
        rejected("GET / HTTP/1.1\r\nBad\rHeader: value\r\n\r\n")
        rejected("GET / HTTP/1.1\r\nMissing-colon\r\n\r\n")
    }

    private fun assertHeaderDeadline(receiver: Socket) {
        receiver.soTimeout = 2_000 // A broken deadline must fail in bounded test time, not hang the build.
        val start = System.nanoTime()
        assertThrows(SocketTimeoutException::class.java) { readAndroidHostRequest(receiver, 150) }
        assertTrue("Trickled bytes must not extend the whole-header deadline",
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1_500)
    }

    @Test(timeout = 8_000) fun idleAndContinuouslyTrickledHeadersHaveAnAbsoluteDeadline() {
        pair { _, receiver ->
            assertHeaderDeadline(receiver)
        }
        pair { sender, receiver ->
            val writer = Thread {
                try {
                    repeat(200) {
                        sender.getOutputStream().write('x'.code)
                        sender.getOutputStream().flush()
                        Thread.sleep(10)
                    }
                } catch (_: IOException) { }
            }.apply { isDaemon = true; start() }
            try {
                assertHeaderDeadline(receiver)
            } finally {
                sender.close()
                writer.join(1_000)
            }
        }
    }

    @Test fun saturatedPoolRejectsImmediatelyAndStopClosesAllActiveClients() {
        AndroidHostClients(2).use { pool ->
            pair { _, first -> pair { _, second -> pair { overflowPeer, overflow ->
                val started = CountDownLatch(2)
                val finished = CountDownLatch(2)
                val handler: (Socket) -> Unit = { socket ->
                    started.countDown()
                    try { socket.getInputStream().read() } finally { finished.countDown() }
                }
                assertTrue(pool.submit(first, handler))
                assertTrue(pool.submit(second, handler))
                assertTrue(started.await(2, TimeUnit.SECONDS))
                assertFalse(pool.submit(overflow, handler))
                overflowPeer.soTimeout = 1_000
                assertEquals(-1, overflowPeer.getInputStream().read())
                pool.close()
                assertTrue(finished.await(2, TimeUnit.SECONDS))
                assertFalse(pool.submit(overflow) {})
            } } }
        }
    }
}
