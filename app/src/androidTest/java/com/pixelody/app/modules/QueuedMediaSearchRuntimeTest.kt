package com.pixelody.app.modules

import android.content.ComponentName
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.pixelody.app.core.playback.PixelodyPlaybackService
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Command resolution only: these tests never prepare or start audio. */
@OptIn(UnstableApi::class)
class QueuedMediaSearchRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun onMain(action: () -> Unit) = instrumentation.runOnMainSync { action() }

    private fun awaitQueue(controller: MediaController, expected: List<String>) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        var actual = emptyList<String>()
        do {
            onMain { actual = List(controller.mediaItemCount) { controller.getMediaItemAt(it).mediaId } }
            if (actual == expected) return
            Thread.sleep(50)
        } while (System.nanoTime() < deadline)
        assertEquals(expected, actual)
    }

    private fun withController(action: (MediaController) -> Unit) {
        assumeTrue("Use an isolated test app, never the owner's player", context.packageName.endsWith(".modules"))
        lateinit var future: ListenableFuture<MediaController>
        onMain {
            future = MediaController.Builder(context, SessionToken(context,
                ComponentName(context, PixelodyPlaybackService::class.java)))
                .setApplicationLooper(Looper.getMainLooper()).buildAsync()
        }
        val controller = future.get(10, TimeUnit.SECONDS)
        try {
            val items = listOf("night" to "Night Train", "morning" to "Morning Train").map { (id, title) ->
                MediaItem.Builder().setMediaId(id).setUri("https://example.invalid/$id")
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist("Alice").build()).build()
            }
            onMain { controller.pause(); controller.setMediaItems(items) }
            awaitQueue(controller, listOf("night", "morning"))
            action(controller)
            onMain { assertFalse("Resolution must not start audio", controller.playWhenReady) }
        } finally {
            onMain { controller.clearMediaItems(); controller.release() }
        }
    }

    @Test
    fun voiceSearchResolvesExistingQueuedMusic() = withController { controller ->
        val request = MediaItem.Builder().setRequestMetadata(MediaItem.RequestMetadata.Builder()
            .setSearchQuery("night alice").build()).build()
        onMain { controller.setMediaItem(request) }
        awaitQueue(controller, listOf("night"))
    }

    @Test
    fun unknownSearchLeavesTheQueueAvailableForTheNextRequest() = withController { controller ->
        val missing = MediaItem.Builder().setRequestMetadata(MediaItem.RequestMetadata.Builder()
            .setSearchQuery("unknown song").build()).build()
        val knownId = MediaItem.Builder().setMediaId("morning").build()
        // Both commands use the same controller. Resolving the subsequent ID
        // requires the first rejected search to have preserved the queue.
        onMain { controller.setMediaItem(missing); controller.setMediaItem(knownId) }
        awaitQueue(controller, listOf("morning"))
    }
}
