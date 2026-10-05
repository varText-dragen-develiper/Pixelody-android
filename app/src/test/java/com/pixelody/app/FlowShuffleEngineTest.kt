package com.pixelody.app

import com.pixelody.app.core.playback.FlowShuffleEngine
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import java.util.concurrent.CancellationException

class FlowShuffleEngineTest {
    @Test
    fun everyModeHonorsBoundsAndFiltersDuplicateOrUnavailableTracks() {
        val current = mockTrack("current")
        val valid = (1..12).map { mockTrack("$it") }
        val pool = listOf(current) + valid + valid + listOf(
            mockTrack("missing").copy(missing = true), mockTrack("blank").copy(streamUrl = ""))
        FlowShuffleMode.values().forEach { mode ->
            assertTrue(FlowShuffleEngine.planQueue(current, pool, mode, 0).isEmpty())
            assertTrue(FlowShuffleEngine.planQueue(current, pool, mode, -1).isEmpty())
            val ids = FlowShuffleEngine.planQueue(current, pool, mode, 5, Random(7)).map { it.track.id }
            assertEquals(mode.name, 5, ids.size)
            assertEquals(ids.size, ids.distinct().size)
            assertTrue(ids.all { id -> valid.any { it.id == id } })
        }
    }

    @Test
    fun knownSeedReproducesEachShuffleMode() {
        val pool = (1..100).map { mockTrack("$it", artist = "Artist ${it % 12}", album = "Album ${it % 6}") }
        FlowShuffleMode.values().forEach { mode ->
            assertEquals(FlowShuffleEngine.planQueue(pool[0], pool, mode, 100, Random(42)),
                FlowShuffleEngine.planQueue(pool[0], pool, mode, 100, Random(42)))
        }
    }

    @Test
    fun albumModeFinishesCurrentAlbumAndKeepsItsSourceOrder() {
        val pool = listOf(mockTrack("1", artist = "A", album = "First"),
            mockTrack("2", artist = "B", album = "Other"),
            mockTrack("3", artist = "A", album = "First"),
            mockTrack("4", artist = "A", album = "First"))
        val ids = FlowShuffleEngine.planQueue(pool[0], pool, FlowShuffleMode.AlbumPreserving, random = Random(9)).map { it.track.id }
        assertEquals(listOf("3", "4", "2"), ids)
    }

    @Test
    fun unknownKeysNeverProduceAHarmonicOrEnergyClaim() {
        val tracks = (1..20).map { mockTrack("$it", lossless = false) }
        val entries = FlowShuffleEngine.planQueue(tracks[0], tracks, FlowShuffleMode.SmartFlow)
        assertTrue(entries.none { it.cue?.badge?.contains("Harmonic") == true || it.cue?.badge?.contains("Energy") == true })
    }

    @Test
    fun nonLatinArtistsAreSpacedWhenAlternativesExist() {
        val current = mockTrack("1", artist = "東京")
        val repeat = mockTrack("2", artist = "東京")
        val other = mockTrack("3", artist = "大阪")
        assertEquals(other.id, FlowShuffleEngine.planQueue(current, listOf(current, repeat, other),
            FlowShuffleMode.SmartFlow, random = Random(4)).first().track.id)
    }

    @Test(expected = CancellationException::class)
    fun replacedPlanningRequestCanBeCancelled() {
        FlowShuffleEngine.planQueue(null, listOf(mockTrack("1")), FlowShuffleMode.SmartFlow,
            checkActive = { throw CancellationException() })
    }

    private fun mockTrack(
        id: String,
        title: String = "Title $id",
        artist: String = "Artist $id",
        album: String = "Album $id",
        genre: String = "",
        format: String = "FLAC",
        lossless: Boolean = true
    ): Track {
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            durationSeconds = 180,
            format = format,
            codec = format,
            lossless = lossless,
            sampleRate = 44100,
            bitDepth = 16,
            bitrate = null,
            channels = 2,
            replayGainDb = null,
            artworkUrl = null,
            streamUrl = "http://localhost:41023/audio/$id",
            favorite = false,
            missing = false
        )
    }

    @Test
    fun testEmptyPoolReturnsEmptyList() {
        val result = FlowShuffleEngine.planQueue(null, emptyList(), FlowShuffleMode.SmartFlow)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testSingleTrackReturnsSingleEntry() {
        val t = mockTrack("1")
        val result = FlowShuffleEngine.planQueue(null, listOf(t), FlowShuffleMode.SmartFlow)
        assertEquals(1, result.size)
        assertEquals("1", result[0].track.id)
    }

    @Test
    fun testSmartFlowAntiClumpingSpacedArtists() {
        val tracks = listOf(
            mockTrack("1", artist = "Artist A", album = "Album A1"),
            mockTrack("2", artist = "Artist A", album = "Album A2"),
            mockTrack("3", artist = "Artist B", album = "Album B1"),
            mockTrack("4", artist = "Artist C", album = "Album C1"),
            mockTrack("5", artist = "Artist D", album = "Album D1")
        )
        val current = tracks[0]
        val result = FlowShuffleEngine.planQueue(current, tracks, FlowShuffleMode.SmartFlow)
        
        assertEquals(4, result.size)
        val firstPlanned = result[0].track
        assertTrue(firstPlanned.artist != "Artist A")
    }

    @Test
    fun testAlbumPreservingGrouping() {
        val tracks = listOf(
            mockTrack("1", artist = "Pink Floyd", album = "Dark Side"),
            mockTrack("2", artist = "Pink Floyd", album = "Dark Side"),
            mockTrack("3", artist = "The Beatles", album = "Abbey Road"),
            mockTrack("4", artist = "The Beatles", album = "Abbey Road")
        )
        val result = FlowShuffleEngine.planQueue(null, tracks, FlowShuffleMode.AlbumPreserving)
        assertEquals(4, result.size)
        
        val albums = result.map { it.track.album }
        if (albums[0] == "Dark Side") {
            assertEquals("Dark Side", albums[1])
            assertEquals("Abbey Road", albums[2])
            assertEquals("Abbey Road", albums[3])
        } else {
            assertEquals("Abbey Road", albums[1])
            assertEquals("Dark Side", albums[2])
            assertEquals("Dark Side", albums[3])
        }
    }

    @Test
    fun testShuffleModeNextCycle() {
        assertEquals(FlowShuffleMode.SmartFlow, FlowShuffleMode.Off.next())
        assertEquals(FlowShuffleMode.AlbumPreserving, FlowShuffleMode.SmartFlow.next())
        assertEquals(FlowShuffleMode.PureRandom, FlowShuffleMode.AlbumPreserving.next())
        assertEquals(FlowShuffleMode.Off, FlowShuffleMode.PureRandom.next())
    }

    @Test
    fun testPureRandomPreservesAllElementsWithoutDuplicates() {
        val tracks = (1..50).map { mockTrack(it.toString(), artist = "Artist $it") }
        val current = tracks[0]
        val result = FlowShuffleEngine.planQueue(current, tracks, FlowShuffleMode.PureRandom)

        assertEquals(49, result.size)
        val resultIds = result.map { it.track.id }.toSet()
        assertEquals(49, resultIds.size)
        assertFalse("Current track should not be duplicated in subsequent queue", resultIds.contains(current.id))
    }

    @Test
    fun testLargeLibraryPlanningPerformance() {
        val largePool = (1..500).map { mockTrack(it.toString(), artist = "Artist ${it % 20}", album = "Album ${it % 50}") }
        
        // Default horizon = 50
        val defaultHorizonResult = FlowShuffleEngine.planQueue(largePool[0], largePool, FlowShuffleMode.SmartFlow)
        assertEquals(50, defaultHorizonResult.size)

        // Custom horizon = 500
        val start = System.currentTimeMillis()
        val fullResult = FlowShuffleEngine.planQueue(largePool[0], largePool, FlowShuffleMode.SmartFlow, horizon = 500)
        val elapsed = System.currentTimeMillis() - start

        assertEquals(499, fullResult.size)
        assertTrue("Planning 500 tracks in SmartFlow mode should complete rapidly (<3000ms), took ${elapsed}ms", elapsed < 3000)
    }

    @Test
    fun testSmartFlowHarmonicKeyPreference() {
        val current = mockTrack("1", artist = "Artist 1", format = "8A")
        val tracks = listOf(
            current,
            mockTrack("2", artist = "Artist 2", format = "3A"), // Clash
            mockTrack("3", artist = "Artist 3", format = "8B"), // Relative Major
            mockTrack("4", artist = "Artist 4", format = "9A")  // Adjacent Step
        )

        val result = FlowShuffleEngine.planQueue(current, tracks, FlowShuffleMode.SmartFlow)
        assertEquals(3, result.size)
        // First track should be harmonically compatible (8B or 9A) rather than clashing 3A
        val firstTrackFormat = result[0].track.format
        assertTrue("First track should be harmonically compatible (8B or 9A), was: $firstTrackFormat", firstTrackFormat == "8B" || firstTrackFormat == "9A")
    }

    @Test
    fun testTransitionCueBadgeContainsHarmonicKey() {
        val current = mockTrack("1", artist = "Artist 1", format = "8A")
        val next = mockTrack("2", artist = "Artist 2", format = "8B")
        val result = FlowShuffleEngine.planQueue(current, listOf(current, next), FlowShuffleMode.SmartFlow)
        
        assertEquals(1, result.size)
        val cue = result[0].cue
        assertTrue(cue != null)
        assertTrue("Cue badge should reflect Harmonic Key (8B), was: ${cue?.badge}", cue?.badge?.contains("8B") == true || cue?.badge?.contains("Harmonic") == true)
    }

    @Test
    fun testSmartFlowGenrePreference() {
        val current = mockTrack("1", artist = "Artist 1", format = "8A", genre = "Electronic")
        val tracks = listOf(
            current,
            mockTrack("2", artist = "Artist 2", format = "8A", genre = "Metal"),
            mockTrack("3", artist = "Artist 3", format = "8A", genre = "Ambient"),
            mockTrack("4", artist = "Artist 4", format = "8A", genre = "Electronic")
        )

        val result = FlowShuffleEngine.planQueue(current, tracks, FlowShuffleMode.SmartFlow)
        assertEquals(3, result.size)
        val firstGenre = result[0].track.genre
        assertTrue("First track should have genre affinity (Electronic or Ambient), was: $firstGenre", firstGenre == "Electronic" || firstGenre == "Ambient")
    }
}


