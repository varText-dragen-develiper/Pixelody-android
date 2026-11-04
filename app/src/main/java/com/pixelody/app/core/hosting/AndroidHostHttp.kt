package com.pixelody.app.core.hosting

import java.io.BufferedInputStream
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** No pending-client queue: slow clients cannot create unbounded threads/sockets. */
internal class AndroidHostClients(maxClients: Int = 4) : Closeable {
    private val clients = ConcurrentHashMap.newKeySet<Socket>()
    private val workers = ThreadPoolExecutor(maxClients, maxClients, 0L, TimeUnit.MILLISECONDS,
        SynchronousQueue(), { task -> Thread(task, "Pixelody-host-client").apply { isDaemon = true } })
    @Volatile private var closed = false

    fun submit(client: Socket, handler: (Socket) -> Unit): Boolean {
        clients.add(client)
        if (closed) {
            clients.remove(client)
            client.close()
            return false
        }
        return try {
            workers.execute {
                try {
                    client.use(handler)
                } catch (_: IOException) {
                    // Disconnects, malformed requests and header deadlines close this client only.
                } catch (_: IllegalArgumentException) {
                    // Invalid URL encodings must not escape the connection handler.
                } finally {
                    clients.remove(client)
                }
            }
            true
        } catch (_: RejectedExecutionException) {
            clients.remove(client)
            client.close()
            false
        }
    }

    override fun close() {
        closed = true
        clients.forEach { runCatching { it.close() } }
        workers.shutdownNow()
    }
}

internal data class AndroidHostRequest(val method: String, val target: String, val headers: Map<String, String>)

/** Bounds apply before routing/auth. The deadline covers the whole header, including trickled bytes. */
internal fun readAndroidHostRequest(socket: Socket, headerTimeoutMs: Int = 5_000): AndroidHostRequest {
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(headerTimeoutMs.toLong())
    val input = BufferedInputStream(socket.getInputStream(), 1024)
    var total = 0
    fun line(limit: Int): String {
        val bytes = ArrayList<Byte>(minOf(limit, 128))
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) throw SocketTimeoutException("Host header deadline exceeded")
            socket.soTimeout = TimeUnit.NANOSECONDS.toMillis(remaining).coerceAtLeast(1).toInt()
            val value = input.read()
            if (value < 0) throw IOException("Incomplete host request")
            if (++total > 16_384 || bytes.size >= limit) throw IOException("Host request too large")
            if (value == 10) break
            bytes.add(value.toByte())
        }
        val text = String(bytes.toByteArray(), StandardCharsets.ISO_8859_1).removeSuffix("\r")
        if (text.any { it == '\r' || (it.code < 32 && it != '\t') || it.code == 127 }) {
            throw IOException("Invalid host header character")
        }
        return text
    }
    val request = line(4_096).split(" ")
    if (request.size != 3 || request[0].isBlank() || !request[1].startsWith("/") ||
        request[2] !in setOf("HTTP/1.0", "HTTP/1.1")) throw IOException("Invalid host request line")
    val headers = mutableMapOf<String, String>()
    var count = 0
    while (true) {
        val header = line(8_192)
        if (header.isEmpty()) break
        if (++count > 64) throw IOException("Too many host headers")
        val colon = header.indexOf(':')
        if (colon <= 0) throw IOException("Invalid host header")
        val name = header.substring(0, colon).lowercase(Locale.US)
        if (!name.all { it in 'a'..'z' || it in '0'..'9' || it == '-' } || name in headers) {
            throw IOException("Invalid or duplicate host header")
        }
        headers[name] = header.substring(colon + 1).trim()
    }
    socket.soTimeout = 0 // GET/HEAD responses can stream full tracks; no request body is read.
    return AndroidHostRequest(request[0].uppercase(Locale.US), request[1], headers)
}
