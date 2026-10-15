package com.pixelody.app

import com.pixelody.app.core.playback.FlowShuffleEngine
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.data.model.Track
import java.util.concurrent.CancellationException
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class FlowQueueOccurrenceTest {
    private val anchor = Track("playing", "Song Key:8A", artist = "A", album = "First", streamUrl = "file:///a")
    private val next = Track("next", "Song Key:9A", artist = "B", album = "Second", streamUrl = "file:///b")

    @Test fun everyModeRetainsRepeatsOfThePlayingSongAndUnresolvedOccurrences() {
        val upcoming = listOf(anchor, next, next, null, anchor.copy(missing = true), next.copy(streamUrl = ""))
        FlowShuffleMode.entries.forEach { mode ->
            val positions = FlowShuffleEngine.planUpcomingOrder(anchor, upcoming, mode, Random(12))
            assertEquals(mode.name, upcoming.indices.toList(), positions.sorted())
            assertEquals(positions.size, positions.distinct().size)
            assertEquals(upcoming.groupingBy { it?.id }.eachCount(), positions.map { upcoming[it] }.groupingBy { it?.id }.eachCount())
            if (mode == FlowShuffleMode.Off) assertEquals(upcoming.indices.toList(), positions)
        }
    }

    @Test fun albumModeRetainsTheSourceOrderOfRepeatedAlbumTracks() {
        val pool = listOf(next, anchor, anchor, next, anchor.copy(id = "another-song"))
        val positions = FlowShuffleEngine.planUpcomingOrder(anchor, pool, FlowShuffleMode.AlbumPreserving, Random(2))
        assertEquals(listOf(1, 2, 4, 0, 3), positions)
    }

    @Test(expected = CancellationException::class)
    fun occurrencePlanningHonorsCancellation() {
        FlowShuffleEngine.planUpcomingOrder(anchor, listOf(next), FlowShuffleMode.SmartFlow,
            checkActive = { throw CancellationException("Queue changed") })
    }
}
