package com.pixelody.app.core.hosting

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.pixelody.app.data.model.Track
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

private const val API_PREFIX = "/api/v1"

data class AndroidHostStatus(
    val running: Boolean = false,
    val baseUrl: String = "",
    val localBaseUrl: String = "",
    val token: String = "",
    val trackCount: Int = 0,
    val startedAt: String = "",
    val lastError: String = ""
)

class AndroidLibraryHost(private val appContext: Context) {
    private val random = SecureRandom()
    private val executor = Executors.newCachedThreadPool()
    private var serverSocket: ServerSocket? = null
    private var token: String = ""
    private var tracks: List<HostedTrack> = emptyList()
    private var status = AndroidHostStatus()

    fun status(): AndroidHostStatus = status.copy(trackCount = tracks.size)

    fun start(localTracks: List<Track>): AndroidHostStatus {
        if (localTracks.isEmpty()) {
            status = status.copy(lastError = "Choose local audio files before hosting.")
            return status()
        }
        if (serverSocket != null) {
            updateTracks(localTracks)
            return status()
        }
        tracks = localTracks.map { HostedTrack(publicId = publicIdFor(it.id), track = it) }
        token = randomToken()
        return try {
            val socket = ServerSocket(0, 50, InetAddress.getByName("0.0.0.0"))
            serverSocket = socket
            val port = socket.localPort
            status = AndroidHostStatus(
                running = true,
                baseUrl = "http://${bestLocalAddress()}:$port",
                localBaseUrl = "http://127.0.0.1:$port",
                token = token,
                trackCount = tracks.size,
                startedAt = System.currentTimeMillis().toString(),
                lastError = ""
            )
            executor.execute { acceptLoop(socket) }
            status()
        } catch (error: Throwable) {
            status = AndroidHostStatus(lastError = error.message ?: "Android host could not start.")
            status()
        }
    }

    fun updateTracks(localTracks: List<Track>): AndroidHostStatus {
        tracks = localTracks.map { HostedTrack(publicId = publicIdFor(it.id), track = it) }
        status = status.copy(trackCount = tracks.size)
        return status()
    }

    fun stop(): AndroidHostStatus {
        runCatching { serverSocket?.close() }
        serverSocket = null
        token = ""
        status = AndroidHostStatus()
        return status()
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            try {
                val client = socket.accept()
                executor.execute { handleClient(client) }
            } catch (_: SocketException) {
                return
            } catch (error: Throwable) {
                status = status.copy(lastError = error.message ?: "Android host connection failed.")
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            val input = BufferedInputStream(client.getInputStream())
            val reader = input.bufferedReader(StandardCharsets.UTF_8)
            val requestLine = reader.readLine().orEmpty()
            if (requestLine.isBlank()) return
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                sendJson(client.getOutputStream(), 400, JSONObject().put("ok", false).put("message", "Bad request."))
                return
            }
            val method = parts[0].uppercase(Locale.US)
            val target = parts[1]
            val headers = readHeaders(reader)
            if (method !in setOf("GET", "HEAD")) {
                sendJson(client.getOutputStream(), 405, JSONObject().put("ok", false).put("message", "Method not allowed."))
                return
            }
            val path = target.substringBefore("?")
            val query = parseQuery(target.substringAfter("?", ""))
            val output = client.getOutputStream()
            when {
                path == "$API_PREFIX/health" -> sendJson(output, 200, JSONObject().put("ok", true).put("status", "running"))
                path == "$API_PREFIX/server-info" -> sendJson(output, 200, serverInfo())
                !isAuthorized(headers, query) -> sendJson(output, 401, JSONObject().put("ok", false).put("message", "A valid Pixelody device token is required."))
                path == "$API_PREFIX/host/capabilities" -> sendJson(output, 200, capabilities())
                path == "$API_PREFIX/library/snapshot" -> sendJson(output, 200, librarySnapshot())
                path == "$API_PREFIX/tracks" -> sendJson(output, 200, JSONObject().put("tracks", JSONArray(tracks.map { publicTrack(it) })))
                path.startsWith("$API_PREFIX/tracks/") -> handleTrackRoute(output, method, path, headers)
                else -> sendJson(output, 404, JSONObject().put("ok", false).put("message", "Route was not found."))
            }
        }
    }

    private fun handleTrackRoute(output: OutputStream, method: String, path: String, headers: Map<String, String>) {
        val suffix = path.removePrefix("$API_PREFIX/tracks/")
        val publicId = suffix.substringBefore("/")
        val action = suffix.substringAfter("/", "")
        val hostedTrack = tracks.firstOrNull { it.publicId == publicId }
        if (hostedTrack == null) {
            sendJson(output, 404, JSONObject().put("ok", false).put("message", "Track was not found."))
            return
        }
        when (action) {
            "" -> sendJson(output, 200, publicTrack(hostedTrack))
            "stream" -> streamTrack(output, method, hostedTrack.track, headers["range"])
            else -> sendJson(output, 404, JSONObject().put("ok", false).put("message", "Route was not found."))
        }
    }

    private fun streamTrack(output: OutputStream, method: String, track: Track, rangeHeader: String?) {
        val uri = Uri.parse(track.streamUrl)
        val size = sizeFor(uri)
        val range = parseRange(rangeHeader, size)
        if (range?.invalid == true) {
            writeHeaders(output, 416, mapOf("Content-Range" to "bytes */${size.coerceAtLeast(0L)}"))
            return
        }
        val start = range?.start ?: 0L
        val end = range?.end ?: (size - 1L).takeIf { size > 0L }
        val contentLength = end?.let { it - start + 1L }
        val headers = mutableMapOf(
            "Content-Type" to mimeFor(track),
            "Accept-Ranges" to "bytes",
            "Cache-Control" to "private, max-age=60"
        )
        if (contentLength != null) headers["Content-Length"] = contentLength.toString()
        if (range != null && end != null) headers["Content-Range"] = "bytes $start-$end/$size"
        writeHeaders(output, if (range != null) 206 else 200, headers)
        if (method == "HEAD") return
        appContext.contentResolver.openInputStream(uri)?.use { raw ->
            val input = BufferedInputStream(raw)
            var skipped = 0L
            while (skipped < start) {
                val delta = input.skip(start - skipped)
                if (delta <= 0L) break
                skipped += delta
            }
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var remaining = contentLength ?: Long.MAX_VALUE
            while (remaining > 0L) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read <= 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        }
    }

    private fun serverInfo(): JSONObject = JSONObject()
        .put("ok", true)
        .put("name", "Pixelody Android Host")
        .put("version", "0.1.0")
        .put("apiVersion", 1)
        .put("hostId", "android-${publicIdFor(status.baseUrl.ifBlank { "pixelody-android" })}")
        .put("hostName", "Pixelody on Android")
        .put("platform", "android")
        .put("visibility", "lan")
        .put("authRequired", true)
        .put("publicInternetExposure", false)

    private fun capabilities(): JSONObject = JSONObject()
        .put("hostId", "android-${publicIdFor(status.baseUrl.ifBlank { "pixelody-android" })}")
        .put("hostName", "Pixelody on Android")
        .put("platform", "android")
        .put("roles", JSONArray(listOf("libraryHost", "playbackDevice", "controller")))
        .put("visibility", "lan")
        .put("canBackgroundHost", false)
        .put("canImport", false)
        .put("canEditMetadata", false)
        .put("canStream", true)
        .put("canRemoteControl", false)
        .put("limits", JSONObject().put("requiresForegroundService", true).put("batterySensitive", true))

    private fun librarySnapshot(): JSONObject {
        val trackIds = tracks.map { it.publicId }
        return JSONObject()
            .put("version", 1)
            .put("generatedAt", System.currentTimeMillis())
            .put("hostName", "Pixelody on Android")
            .put("tracks", JSONArray(tracks.map { publicTrack(it) }))
            .put("playlists", JSONArray(listOf(JSONObject()
                .put("id", "android-device")
                .put("name", "On this phone")
                .put("trackIds", JSONArray(trackIds))
                .put("artworkUrl", JSONObject.NULL))))
            .put("favorites", JSONArray())
            .put("queue", JSONArray(trackIds))
            .put("playback", JSONObject().put("currentTrackId", trackIds.firstOrNull() ?: JSONObject.NULL).put("shuffle", false).put("repeat", "off"))
    }

    private fun publicTrack(hostedTrack: HostedTrack): JSONObject {
        val track = hostedTrack.track
        return JSONObject()
            .put("id", hostedTrack.publicId)
            .put("title", track.title)
            .put("artist", track.artist)
            .put("album", track.album)
            .put("duration", track.durationSeconds)
            .put("format", track.format)
            .put("codec", track.codec)
            .put("lossless", track.lossless)
            .put("sampleRate", track.sampleRate)
            .put("bitDepth", track.bitDepth ?: JSONObject.NULL)
            .put("bitrate", track.bitrate ?: JSONObject.NULL)
            .put("channels", track.channels)
            .put("replayGainDb", track.replayGainDb ?: JSONObject.NULL)
            .put("artworkUrl", JSONObject.NULL)
            .put("streamUrl", "$API_PREFIX/tracks/${hostedTrack.publicId}/stream")
            .put("favorite", false)
            .put("missing", false)
    }

    private fun isAuthorized(headers: Map<String, String>, query: Map<String, String>): Boolean =
        isAuthorized(token, headers, query)

    private fun readHeaders(reader: BufferedReader): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isBlank()) break
            val name = line.substringBefore(":").trim().lowercase(Locale.US)
            val value = line.substringAfter(":", "").trim()
            if (name.isNotBlank()) headers[name] = value
        }
        return headers
    }

    private fun writeHeaders(output: OutputStream, statusCode: Int, headers: Map<String, String> = emptyMap()) {
        val reason = when (statusCode) {
            200 -> "OK"
            206 -> "Partial Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            416 -> "Range Not Satisfiable"
            else -> "OK"
        }
        val writer = PrintWriter(OutputStreamWriter(output, StandardCharsets.UTF_8), false)
        writer.print("HTTP/1.1 $statusCode $reason\r\n")
        writer.print("Connection: close\r\n")
        for ((name, value) in headers) writer.print("$name: $value\r\n")
        writer.print("\r\n")
        writer.flush()
    }

    private fun sendJson(output: OutputStream, statusCode: Int, body: JSONObject) {
        val payload = body.toString(2).toByteArray(StandardCharsets.UTF_8)
        writeHeaders(output, statusCode, mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "Content-Length" to payload.size.toString(),
            "Cache-Control" to "no-store"
        ))
        output.write(payload)
    }

    private fun sizeFor(uri: Uri): Long {
        appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            if (descriptor.length > 0L) return descriptor.length
        }
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) return cursor.getLong(index)
            }
        }
        return -1L
    }

    private fun bestLocalAddress(): String {
        val addresses = NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filterNot { it.isLoopbackAddress }
        return addresses.firstOrNull { it.isSiteLocalAddress }?.hostAddress
            ?: addresses.firstOrNull()?.hostAddress
            ?: "127.0.0.1"
        }

    private fun randomToken(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        internal data class HostedTrack(val publicId: String, val track: Track)
        internal data class ByteRange(val start: Long = 0L, val end: Long = 0L, val invalid: Boolean = false)

        internal fun parseRange(rangeHeader: String?, size: Long): ByteRange? {
            if (rangeHeader.isNullOrBlank() || size <= 0L) return null
            val match = Regex("^bytes=(\\d*)-(\\d*)$").find(rangeHeader) ?: return ByteRange(invalid = true)
            var start = match.groupValues[1].toLongOrNull() ?: 0L
            var end = match.groupValues[2].toLongOrNull() ?: (size - 1L)
            if (match.groupValues[1].isBlank() && match.groupValues[2].isNotBlank()) {
                val suffixLength = match.groupValues[2].toLongOrNull() ?: return ByteRange(invalid = true)
                start = (size - suffixLength).coerceAtLeast(0L)
                end = size - 1L
            }
            if (start < 0L || end < start || start >= size) return ByteRange(invalid = true)
            return ByteRange(start = start, end = end.coerceAtMost(size - 1L))
        }

        internal fun parseQuery(query: String): Map<String, String> {
            if (query.isBlank()) return emptyMap()
            return query.split("&").mapNotNull { pair ->
                val name = pair.substringBefore("=")
                if (name.isBlank()) null else {
                    val value = pair.substringAfter("=", "")
                    URLDecoder.decode(name, "UTF-8") to URLDecoder.decode(value, "UTF-8")
                }
            }.toMap()
        }

        internal fun mimeFor(track: Track): String = when (track.format.uppercase(Locale.US)) {
            "FLAC" -> "audio/flac"
            "WAV", "WAVE" -> "audio/wav"
            "MP3" -> "audio/mpeg"
            "M4A", "AAC", "ALAC" -> "audio/mp4"
            "OGG", "OPUS" -> "audio/ogg"
            else -> "application/octet-stream"
        }

        internal fun publicIdFor(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
            return digest.take(16).joinToString("") { "%02x".format(it) }
        }

        internal fun isAuthorized(expectedToken: String, headers: Map<String, String>, query: Map<String, String>): Boolean {
            val authorization = headers["authorization"].orEmpty()
            val bearer = Regex("^Bearer\\s+(.+)$", RegexOption.IGNORE_CASE).find(authorization)?.groupValues?.getOrNull(1)
            val candidate = bearer ?: query["token"].orEmpty()
            return candidate.isNotBlank() && candidate == expectedToken
        }
    }
}
