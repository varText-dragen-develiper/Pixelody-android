package com.pixelody.app.modules

import android.net.Uri
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.pixelody.app.core.hosting.AndroidLibraryHost
import com.pixelody.app.data.model.Track
import java.io.File
import java.net.Socket
import org.junit.Assert.*
import org.junit.Test

class AndroidHostSecurityRuntimeTest {
    @Test fun realHostKeepsAuthRangeAndRestartWithBoundedHttpParser() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.pixelody.app")
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        check(InstrumentationRegistry.getArguments().getString("pixelodyAuditAvd") == "Pixelody_Android_Audit_20261025")
        val fixture = File.createTempFile("host-security-", ".wav", context.cacheDir)
        fixture.writeBytes(ByteArray(32) { it.toByte() })
        val host = AndroidLibraryHost(context)
        try {
            val track = Track(id = "audit-host", title = "Audit", artist = "Audit", album = "Audit",
                durationSeconds = 1, format = "WAV", codec = "PCM", sampleRate = 44100,
                bitDepth = 16, bitrate = 1411, channels = 2, lossless = true,
                streamUrl = Uri.fromFile(fixture).toString())
            var state = host.start(listOf(track))
            assertTrue(state.running)
            fun request(path: String, extra: String = ""): String {
                val uri = Uri.parse(state.localBaseUrl)
                return Socket("127.0.0.1", uri.port).use { socket ->
                    socket.soTimeout = 3_000
                    socket.getOutputStream().write("GET $path HTTP/1.1\r\nHost: localhost\r\n$extra\r\n".toByteArray())
                    socket.getInputStream().readBytes().toString(Charsets.ISO_8859_1)
                }
            }
            assertTrue(request("/api/v1/health").startsWith("HTTP/1.1 200"))
            assertTrue(request("/api/v1/tracks").startsWith("HTTP/1.1 401"))
            val auth = "Authorization: Bearer ${state.token}\r\n"
            val tracks = request("/api/v1/tracks", auth)
            assertTrue(tracks.startsWith("HTTP/1.1 200"))
            val id = com.pixelody.app.core.hosting.AndroidLibraryHost.publicIdFor(track.id)
            val range = request("/api/v1/tracks/$id/stream", auth + "Range: bytes=4-7\r\n")
            assertTrue(range.startsWith("HTTP/1.1 206"))
            assertArrayEquals(byteArrayOf(4, 5, 6, 7), range.substringAfter("\r\n\r\n").toByteArray(Charsets.ISO_8859_1))
            host.stop()
            state = host.start(listOf(track))
            assertTrue(state.running)
            assertTrue(request("/api/v1/health").startsWith("HTTP/1.1 200"))
        } finally {
            host.stop()
            fixture.delete()
        }
    }
}
