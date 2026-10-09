package com.pixelody.app.modules

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import com.pixelody.app.core.playback.removeRemoteHostMedia
import org.junit.Assert.*
import org.junit.Test
import androidx.activity.compose.setContent
import android.content.Intent
import com.pixelody.app.MainActivity
import com.pixelody.app.feature.baselayer.BasePlaybackHost
import com.pixelody.app.feature.baselayer.BaseLayerData
import com.pixelody.app.feature.baselayer.BaseSource
import com.pixelody.app.data.model.HostConnectionDetails
import com.pixelody.app.data.storage.SavedHostStore
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.ui.theme.PixelodyTheme
import java.net.ServerSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import kotlin.concurrent.thread

class ConnectionRecoveryRuntimeTest {
    @Test fun foregroundHostLossReturnsBrowsingToPhoneAndKeepsPairing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertTrue("Run only in isolated test packages", context.packageName.endsWith(".studioqa"))
        val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
        val lost = AtomicBoolean(false)
        val revoked = AtomicBoolean(false)
        val observed = AtomicReference<BaseLayerData>()
        val saved = SavedHostStore(context)
        saved.clear()
        MobileSettingsStore(context).saveLastTrackId(null)
        val worker = thread(isDaemon = true, name = "recovery-test-host") {
            while (!server.isClosed) {
                try {
                    server.accept().use { socket ->
                        val reader = socket.getInputStream().bufferedReader()
                        val request = reader.readLine().orEmpty()
                        while (!reader.readLine().isNullOrEmpty()) { /* consume headers */ }
                        val unavailable = request.contains("/api/v1/live") && lost.get()
                        val denied = request.contains("/api/v1/live") && revoked.get()
                        val body = when {
                            denied -> """{"code":"auth_revoked","message":"Fixture access revoked"}"""
                            unavailable -> """{"code":"host_unavailable","message":"Fixture host stopped"}"""
                            request.contains("/api/v1/library/tracks") -> """{"revision":1,"tracks":[],"page":{"total":0,"nextOffset":null}}"""
                            request.contains("/api/v1/live") -> """{"revision":1,"pollAfterMs":1000,"unchanged":false}"""
                            else -> """{"hostId":"fixture","hostName":"Recovery fixture","revision":1,"tracks":[],"playlists":[]}"""
                        }.toByteArray()
                        val status = if (denied) "401 Unauthorized" else if (unavailable) "503 Service Unavailable" else "200 OK"
                        socket.getOutputStream().apply {
                            write("HTTP/1.1 $status\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            write(body)
                            flush()
                        }
                    }
                } catch (_: java.io.IOException) { if (!server.isClosed) throw AssertionError("Fixture server failed") }
            }
        }
        fun awaitState(description: String, matches: (BaseLayerData) -> Boolean) {
            repeat(150) {
                if (observed.get()?.let(matches) == true) return
                Thread.sleep(100)
            }
            fail("$description; last state=${observed.get()}")
        }
        var activity: MainActivity? = null
        try {
            val activeActivity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            activity = activeActivity
                val baseUrl = "http://127.0.0.1:${server.localPort}"
                instrumentation.runOnMainSync {
                    activeActivity.setContent {
                        PixelodyTheme {
                            BasePlaybackHost(initialData = BaseLayerData(source = BaseSource.Host),
                                incomingConnectionDetails = HostConnectionDetails(baseUrl, listOf(baseUrl), token = "test-only"),
                                onDataChanged = { observed.set(it) })
                        }
                    }
                }
                awaitState("Connected host is browsable") { it.hostReachable && it.source == BaseSource.Host && saved.load() != null }
                lost.set(true)
                awaitState("Failed live poll returns to Phone") { !it.hostReachable && it.source == BaseSource.Phone }
                assertNotNull("Transient loss preserves encrypted pairing", saved.load())
                lost.set(false)
                Thread.sleep(1800)
                assertFalse("PC return does not silently reclaim the app", observed.get().hostReachable)
                assertEquals(BaseSource.Phone, observed.get().source)
                val device = UiDevice.getInstance(instrumentation)
                val reconnect = device.wait(Until.findObject(By.text("Reconnect")), 3000)
                assertNotNull("Recovery offers a visible action", reconnect)
                reconnect.click()
                awaitState("Explicit reconnect restores host availability") { it.hostReachable }
                assertEquals("Reconnect preserves local browsing", BaseSource.Phone, observed.get().source)
                revoked.set(true)
                awaitState("Revocation releases host access") { !it.hostReachable }
                assertNull("Revoked credentials cannot be retried", saved.load())
                val pairAgain = device.wait(Until.findObject(By.text("Pair again")), 3000)
                assertNotNull("Revocation offers new pairing", pairAgain)
                pairAgain.click()
                assertNotNull("Pairing action opens setup", device.wait(Until.findObject(By.text("Connect your desktop")), 5000))
        } finally {
            instrumentation.runOnMainSync { activity?.finish() }
            server.close()
            worker.join(2000)
            saved.clear()
        }
    }

    @Test fun lostHostRemovesRemoteQueueWithoutInterruptingLocalPlayback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertTrue("Run only in isolated test packages", instrumentation.targetContext.packageName.endsWith(".studioqa"))
        instrumentation.runOnMainSync {
            val player = ExoPlayer.Builder(instrumentation.targetContext).build()
            try {
                val local = MediaItem.Builder().setMediaId("phone").setUri("file:///local.wav").build()
                val remote = MediaItem.Builder().setMediaId("desktop").setUri("http://127.0.0.1:1/stream").build()
                player.setMediaItems(listOf(local, remote, local.buildUpon().setMediaId("download").build()))
                player.playWhenReady = true
                removeRemoteHostMedia(player)
                assertEquals(2, player.mediaItemCount)
                assertEquals("phone", player.currentMediaItem?.mediaId)
                assertTrue("Local playback intent survives", player.playWhenReady)
                assertEquals("download", player.getMediaItemAt(1).mediaId)

                player.setMediaItems(listOf(remote, local))
                player.playWhenReady = true
                removeRemoteHostMedia(player)
                assertEquals(1, player.mediaItemCount)
                assertEquals("phone", player.currentMediaItem?.mediaId)
                assertFalse("Recovery cannot autoplay a different song", player.playWhenReady)
                removeRemoteHostMedia(player)
                assertEquals("Repeated recovery preserves local queue", 1, player.mediaItemCount)
            } finally {
                player.release()
            }
        }
    }
}
