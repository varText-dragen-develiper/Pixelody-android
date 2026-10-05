package com.pixelody.app.modules

import android.content.ComponentName
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.pixelody.app.core.playback.FlowShuffleEngine
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.core.playback.PixelodyPlaybackService
import com.pixelody.app.data.model.Track
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Real service/controller traversal; never prepares media or starts audio. */
@OptIn(UnstableApi::class)
class FlowShuffleRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun onMain(action: () -> Unit) = instrumentation.runOnMainSync { action() }

    @Test
    fun plannedOrderIsTheOrderNextActuallyTraversesInEveryMode() {
        assumeTrue("Only use the isolated QA player", context.packageName.endsWith(".studioqa"))
        lateinit var future: ListenableFuture<MediaController>
        onMain {
            future = MediaController.Builder(context, SessionToken(context,
                ComponentName(context, PixelodyPlaybackService::class.java)))
                .setApplicationLooper(Looper.getMainLooper()).buildAsync()
        }
        val controller = future.get(10, TimeUnit.SECONDS)
        try {
            val pool = (1..80).map { Track(id = "shuffle-qa-$it", title = "Track $it",
                artist = "Artist ${it % 10}", album = "Album ${it % 5}",
                streamUrl = "https://example.invalid/$it") }
            FlowShuffleMode.values().forEach { mode ->
                val planned = listOf(pool[0]) + FlowShuffleEngine.planQueue(pool[0], pool,
                    mode, pool.size, Random(23)).map { it.track }
                assertEquals("No 16/50-song playback truncation", 80, planned.size)
                onMain {
                    controller.pause()
                    controller.shuffleModeEnabled = false
                    controller.setMediaItems(planned.map { MediaItem.Builder()
                        .setMediaId(it.id).setUri(it.streamUrl).build() }, 0, 1200L)
                }
                awaitCurrent(controller, planned[0].id)
                onMain {
                    assertEquals(1200L, controller.currentPosition)
                    assertFalse(controller.shuffleModeEnabled)
                }
                planned.drop(1).forEach { track ->
                    onMain { controller.seekToNextMediaItem() }
                    awaitCurrent(controller, track.id)
                }
                onMain {
                    assertFalse(controller.hasNextMediaItem())
                    assertFalse("Traversal must not start audio", controller.playWhenReady)
                }
            }
        } finally {
            onMain { controller.clearMediaItems(); controller.release() }
        }
    }

    private fun awaitCurrent(controller: MediaController, id: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        var actual: String? = null
        do {
            onMain { actual = controller.currentMediaItem?.mediaId }
            if (actual == id) return
            Thread.sleep(20)
        } while (System.nanoTime() < deadline)
        assertEquals(id, actual)
    }
}
