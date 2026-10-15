package com.pixelody.app.ui.navigation

import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabituationErgonomicsTest {

    @Test
    fun miniPlayerSwipeLeftTriggersNextTrack() {
        val result = classifyMiniPlayerSwipe(dragX = -120f, dragY = 10f, thresholdPx = 80f)
        assertEquals(SwipeDirection.Next, result)
    }

    @Test
    fun miniPlayerSwipeRightTriggersPreviousTrack() {
        val result = classifyMiniPlayerSwipe(dragX = 140f, dragY = -15f, thresholdPx = 80f)
        assertEquals(SwipeDirection.Previous, result)
    }

    @Test
    fun miniPlayerSwipeUpTriggersOpenPlayer() {
        val result = classifyMiniPlayerSwipe(dragX = 5f, dragY = -150f, thresholdPx = 80f)
        assertEquals(SwipeDirection.OpenPlayer, result)
    }

    @Test
    fun subThresholdMiniPlayerSwipeIsIgnored() {
        val result = classifyMiniPlayerSwipe(dragX = 40f, dragY = -20f, thresholdPx = 80f)
        assertEquals(SwipeDirection.None, result)
    }

    @Test
    fun fullPlayerSwipeDownTriggersCollapse() {
        val result = classifyFullPlayerSwipe(dragY = 130f, thresholdPx = 100f)
        assertEquals(SwipeDirection.CollapsePlayer, result)
    }

    @Test
    fun equalizerCurveNormalizedGainsContainFiveBandsAndStayBounded() {
        val profile = EqualizerProfile(
            enabled = true,
            preset = EqualizerPreset.Bass,
            gainsDb = listOf(15f, 6f, -3f, 2f, -18f, 99f) // Over-bounds & excess bands
        ).normalized()

        assertEquals(5, profile.gainsDb.size)
        assertEquals(EqualizerProfile.MAX_GAIN_DB, profile.gainsDb[0]) // Clamped to +12dB
        assertEquals(6f, profile.gainsDb[1])
        assertEquals(-3f, profile.gainsDb[2])
        assertEquals(2f, profile.gainsDb[3])
        assertEquals(EqualizerProfile.MIN_GAIN_DB, profile.gainsDb[4]) // Clamped to -12dB
    }

    @Test
    fun playerOpeningPreservesOriginForInstantCollapseReturn() {
        listOf(
            PixelodyTab.Home,
            PixelodyTab.Search,
            PixelodyTab.Library,
            PixelodyTab.Create
        ).forEach { origin ->
            val root = reduceNavigation(
                PixelodyNavigationState(),
                PixelodyNavigationIntent.SelectPrimary(origin)
            )
            val opened = reduceNavigation(root, PixelodyNavigationIntent.OpenPlayer)

            assertEquals(PixelodyTab.Player, opened.current)
            assertEquals(origin, opened.playerReturn)
            assertEquals(origin, opened.selectedPrimary)

            val collapsed = reduceNavigation(opened, PixelodyNavigationIntent.CollapsePlayer)
            assertEquals(origin, collapsed.current)
            assertEquals(origin, collapsed.selectedPrimary)
            assertTrue(collapsed.history.isEmpty())
        }
    }

    @Test
    fun queueNavigationFromPlayerMaintainsPlayerReturnPath() {
        val home = PixelodyNavigationState(current = PixelodyTab.Home, selectedPrimary = PixelodyTab.Home)
        val player = reduceNavigation(home, PixelodyNavigationIntent.OpenPlayer)
        val queue = reduceNavigation(player, PixelodyNavigationIntent.NavigateTo(PixelodyTab.Queue))

        assertEquals(PixelodyTab.Queue, queue.current)
        assertEquals(listOf(PixelodyTab.Home, PixelodyTab.Player), queue.history)
        assertTrue(queue.canNavigateBack)

        val backToPlayer = reduceNavigation(queue, PixelodyNavigationIntent.Back)
        assertEquals(PixelodyTab.Player, backToPlayer.current)
        assertEquals(listOf(PixelodyTab.Home), backToPlayer.history)

        val backToHome = reduceNavigation(backToPlayer, PixelodyNavigationIntent.Back)
        assertEquals(PixelodyTab.Home, backToHome.current)
        assertTrue(backToHome.history.isEmpty())
    }

    @Test
    fun repeatedFastPlayTogglesResolveDeterministically() {
        var isPlaying = false
        val totalToggles = 50

        for (i in 1..totalToggles) {
            val action = playbackToggleAction(
                isPlaying = isPlaying,
                hasCurrentMediaItem = true,
                hasSelectedTrack = true
            )
            if (isPlaying) {
                assertEquals(PlaybackToggleAction.Pause, action)
                isPlaying = false
            } else {
                assertEquals(PlaybackToggleAction.Resume, action)
                isPlaying = true
            }
        }
        assertFalse(isPlaying)
    }

    @Test
    fun primaryTabSelectionAlwaysClearsModalHistoryAndResetsPlayerReturn() {
        val start = PixelodyNavigationState(
            current = PixelodyTab.Queue,
            selectedPrimary = PixelodyTab.Search,
            history = listOf(PixelodyTab.Search, PixelodyTab.Player),
            playerReturn = PixelodyTab.Search
        )

        val library = reduceNavigation(start, PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Library))

        assertEquals(PixelodyTab.Library, library.current)
        assertEquals(PixelodyTab.Library, library.selectedPrimary)
        assertTrue(library.history.isEmpty())
        assertFalse(library.canNavigateBack)
    }

    @Test
    fun upwardFlingWithHighVelocityExpandsSheetRegardlessOfProgress() {
        // Even at 10% progress, a strong upward fling (-900 dp/s) should expand
        val expand = com.pixelody.app.ui.components.classifySheetFling(
            velocityDpPerSec = -900f,
            currentFraction = 0.10f
        )
        assertTrue(expand)
    }

    @Test
    fun downwardFlingWithHighVelocityCollapsesSheetRegardlessOfProgress() {
        // Even at 85% progress, a strong downward fling (+950 dp/s) should collapse
        val expand = com.pixelody.app.ui.components.classifySheetFling(
            velocityDpPerSec = 950f,
            currentFraction = 0.85f
        )
        assertFalse(expand)
    }

    @Test
    fun slowDragPastThresholdExpandsSheet() {
        val expand = com.pixelody.app.ui.components.classifySheetFling(
            velocityDpPerSec = 50f,
            currentFraction = 0.40f
        )
        assertTrue(expand)
    }

    @Test
    fun slowDragBelowThresholdCollapsesSheet() {
        val expand = com.pixelody.app.ui.components.classifySheetFling(
            velocityDpPerSec = -40f,
            currentFraction = 0.25f
        )
        assertFalse(expand)
    }

    private fun createTestTrack(
        id: String,
        title: String = "Title",
        artist: String = "Artist",
        album: String = "Album",
        durationSeconds: Int = 180,
        format: String = "FLAC",
        codec: String = "flac",
        lossless: Boolean = true,
        sampleRate: Int = 44100,
        bitDepth: Int? = 16,
        bitrate: Int? = 1411,
        channels: Int = 2,
        replayGainDb: Double? = null,
        artworkUrl: String? = null,
        streamUrl: String = "http://host/stream/$id",
        favorite: Boolean = false,
        missing: Boolean = false
    ): com.pixelody.app.data.model.Track = com.pixelody.app.data.model.Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationSeconds = durationSeconds,
        format = format,
        codec = codec,
        lossless = lossless,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        bitrate = bitrate,
        channels = channels,
        replayGainDb = replayGainDb,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        favorite = favorite,
        missing = missing
    )

    @Test
    fun filterTracksBySourceAllReturnsEntirePool() {
        val t1 = createTestTrack(id = "local_1", title = "Phone Track", streamUrl = "content://media/1")
        val t2 = createTestTrack(id = "host_1", title = "Host Track", streamUrl = "http://host/stream/1")
        val t3 = createTestTrack(id = "jam_1", title = "Jam Track", streamUrl = "http://host/stream/jam")

        val all = listOf(t1, t2, t3)
        val filtered = com.pixelody.app.ui.components.filterTracksBySource(
            tracks = all,
            scope = com.pixelody.app.ui.components.SourceScope.All,
            localTrackIds = setOf("local_1"),
            jamTrackIds = setOf("jam_1")
        )
        assertEquals(3, filtered.size)
    }

    @Test
    fun filterTracksBySourceLocalPhoneReturnsOnlyDeviceAndContentTracks() {
        val t1 = createTestTrack(id = "local_1", title = "Phone Track", streamUrl = "content://media/1")
        val t2 = createTestTrack(id = "host_1", title = "Host Track", streamUrl = "http://host/stream/1")

        val all = listOf(t1, t2)
        val filtered = com.pixelody.app.ui.components.filterTracksBySource(
            tracks = all,
            scope = com.pixelody.app.ui.components.SourceScope.LocalPhone,
            localTrackIds = setOf("local_1")
        )
        assertEquals(1, filtered.size)
        assertEquals("local_1", filtered[0].id)
    }

    @Test
    fun filterTracksBySourceDesktopHostExcludesLocalTracks() {
        val t1 = createTestTrack(id = "local_1", title = "Phone Track", streamUrl = "content://media/1")
        val t2 = createTestTrack(id = "host_1", title = "Host Track", streamUrl = "http://host/stream/1")

        val all = listOf(t1, t2)
        val filtered = com.pixelody.app.ui.components.filterTracksBySource(
            tracks = all,
            scope = com.pixelody.app.ui.components.SourceScope.DesktopHost,
            localTrackIds = setOf("local_1")
        )
        assertEquals(1, filtered.size)
        assertEquals("host_1", filtered[0].id)
    }

    @Test
    fun filterTracksBySourceJamMeshReturnsOnlyQueuedSharedTracks() {
        val t1 = createTestTrack(id = "local_1", title = "Phone Track", streamUrl = "content://media/1")
        val t2 = createTestTrack(id = "jam_1", title = "Jam Track", streamUrl = "http://host/stream/jam")

        val all = listOf(t1, t2)
        val filtered = com.pixelody.app.ui.components.filterTracksBySource(
            tracks = all,
            scope = com.pixelody.app.ui.components.SourceScope.JamMesh,
            localTrackIds = setOf("local_1"),
            jamTrackIds = setOf("jam_1")
        )
        assertEquals(1, filtered.size)
        assertEquals("jam_1", filtered[0].id)
    }

    @Test
    fun scrubRateClassificationMatchesVerticalOffsets() {
        assertEquals(
            com.pixelody.app.ui.components.ScrubRate.Full,
            com.pixelody.app.ui.components.ScrubRate.fromVerticalOffsetDp(10f)
        )
        assertEquals(
            com.pixelody.app.ui.components.ScrubRate.Half,
            com.pixelody.app.ui.components.ScrubRate.fromVerticalOffsetDp(45f)
        )
        assertEquals(
            com.pixelody.app.ui.components.ScrubRate.Quarter,
            com.pixelody.app.ui.components.ScrubRate.fromVerticalOffsetDp(95f)
        )
        assertEquals(
            com.pixelody.app.ui.components.ScrubRate.Fine,
            com.pixelody.app.ui.components.ScrubRate.fromVerticalOffsetDp(180f)
        )
    }

    @Test
    fun computeScrubDeltaScalesCorrectlyPerScrubRate() {
        val width = 1000f
        val drag = 100f

        val fullDelta = com.pixelody.app.ui.components.computeScrubDelta(
            dragDeltaXPx = drag,
            totalWidthPx = width,
            scrubRate = com.pixelody.app.ui.components.ScrubRate.Full
        )
        assertEquals(0.10f, fullDelta, 0.0001f)

        val halfDelta = com.pixelody.app.ui.components.computeScrubDelta(
            dragDeltaXPx = drag,
            totalWidthPx = width,
            scrubRate = com.pixelody.app.ui.components.ScrubRate.Half
        )
        assertEquals(0.05f, halfDelta, 0.0001f)

        val quarterDelta = com.pixelody.app.ui.components.computeScrubDelta(
            dragDeltaXPx = drag,
            totalWidthPx = width,
            scrubRate = com.pixelody.app.ui.components.ScrubRate.Quarter
        )
        assertEquals(0.025f, quarterDelta, 0.0001f)

        val fineDelta = com.pixelody.app.ui.components.computeScrubDelta(
            dragDeltaXPx = drag,
            totalWidthPx = width,
            scrubRate = com.pixelody.app.ui.components.ScrubRate.Fine
        )
        assertEquals(0.010f, fineDelta, 0.0001f)
    }

    @Test
    fun computeScrubDeltaHandlesZeroWidthAndNegativeOffsetsSafely() {
        val zeroWidth = com.pixelody.app.ui.components.computeScrubDelta(
            dragDeltaXPx = 50f,
            totalWidthPx = 0f,
            scrubRate = com.pixelody.app.ui.components.ScrubRate.Full
        )
        assertEquals(0f, zeroWidth, 0.0001f)

        val reverseDelta = com.pixelody.app.ui.components.computeScrubDelta(
            dragDeltaXPx = -100f,
            totalWidthPx = 1000f,
            scrubRate = com.pixelody.app.ui.components.ScrubRate.Half
        )
        assertEquals(-0.05f, reverseDelta, 0.0001f)
    }

    @Test
    fun computeHarmonicAuraColorsNullTrackFallsBackToDefaults() {
        val def1 = androidx.compose.ui.graphics.Color.Cyan
        val def2 = androidx.compose.ui.graphics.Color.Magenta
        val (p, s) = com.pixelody.app.ui.components.computeHarmonicAuraColors(null, def1, def2)
        assertEquals(def1, p)
        assertEquals(def2, s)
    }

    @Test
    fun computeHarmonicAuraColorsLosslessReturnsSignatureAmberGold() {
        val t = createTestTrack(id = "lossless_1", lossless = true)
        val (p, _) = com.pixelody.app.ui.components.computeHarmonicAuraColors(
            track = t,
            defaultPrimary = androidx.compose.ui.graphics.Color.White,
            defaultSecondary = androidx.compose.ui.graphics.Color.Gray
        )
        assertEquals(androidx.compose.ui.graphics.Color(0xFFE5A93C), p)
    }

    @Test
    fun computeHarmonicAuraColorsDifferentiatesTracksDeterministically() {
        val t1 = createTestTrack(id = "track_alpha", title = "Alpha Theme", artist = "Artist 1", lossless = false)
        val t2 = createTestTrack(id = "track_beta", title = "Beta Theme", artist = "Artist 2", lossless = false)

        val res1A = com.pixelody.app.ui.components.computeHarmonicAuraColors(t1, androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.Black)
        val res1B = com.pixelody.app.ui.components.computeHarmonicAuraColors(t1, androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.Black)
        val res2 = com.pixelody.app.ui.components.computeHarmonicAuraColors(t2, androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.Black)

        assertEquals(res1A, res1B) // Deterministic
        assertTrue(res1A.first != res2.first || res1A.second != res2.second)
    }

    @Test
    fun computeAuraBreathingAlphaBehavesDeterministically() {
        // When paused: static 0.12f
        val pausedAlpha = com.pixelody.app.ui.components.computeAuraBreathingAlpha(isPlaying = false, cyclePhase = 1.5f)
        assertEquals(0.12f, pausedAlpha, 0.0001f)

        // When playing: oscillates between 0.15f and 0.28f
        val playAlphaMin = com.pixelody.app.ui.components.computeAuraBreathingAlpha(isPlaying = true, cyclePhase = (3 * Math.PI / 2).toFloat())
        val playAlphaMax = com.pixelody.app.ui.components.computeAuraBreathingAlpha(isPlaying = true, cyclePhase = (Math.PI / 2).toFloat())
        assertTrue(playAlphaMin in 0.149f..0.155f)
        assertTrue(playAlphaMax in 0.275f..0.285f)
    }

    @Test
    fun computeParallaxOriginScalesWithScrollOffset() {
        val origin = com.pixelody.app.ui.components.computeParallaxOrigin(
            baseY = 200f,
            scrollOffsetPx = 100f,
            parallaxFactor = 0.12f
        )
        assertEquals(188f, origin, 0.001f)
    }

    @Test
    fun buildOrbitActionItemsGeneratesAllFiveActionsWithCorrectFavoriteAndOfflineLabels() {
        val itemsFavOffline = com.pixelody.app.ui.components.buildOrbitActionItems(isFavorite = true, isOffline = true)
        assertEquals(5, itemsFavOffline.size)
        val favItem = itemsFavOffline.first { it.type == com.pixelody.app.ui.components.OrbitActionType.ToggleFavorite }
        assertEquals("Favorited", favItem.label)
        assertEquals(com.pixelody.app.ui.components.TransportGlyphType.HeartFilled, favItem.glyphType)
        val offlineItem = itemsFavOffline.first { it.type == com.pixelody.app.ui.components.OrbitActionType.ToggleOffline }
        assertEquals("Saved", offlineItem.label)
        assertEquals(com.pixelody.app.ui.components.TransportGlyphType.OfflineCheck, offlineItem.glyphType)

        val itemsUnfavOnline = com.pixelody.app.ui.components.buildOrbitActionItems(isFavorite = false, isOffline = false)
        val unfavItem = itemsUnfavOnline.first { it.type == com.pixelody.app.ui.components.OrbitActionType.ToggleFavorite }
        assertEquals("Favorite", unfavItem.label)
        assertEquals(com.pixelody.app.ui.components.TransportGlyphType.Heart, unfavItem.glyphType)
        val onlineItem = itemsUnfavOnline.first { it.type == com.pixelody.app.ui.components.OrbitActionType.ToggleOffline }
        assertEquals("Offline", onlineItem.label)
        assertEquals(com.pixelody.app.ui.components.TransportGlyphType.Download, onlineItem.glyphType)
    }

    @Test
    fun computeOrbitPositionsReturnsCorrectCountAndDistributesCoordinates() {
        val items = com.pixelody.app.ui.components.buildOrbitActionItems(isFavorite = false, isOffline = false)
        val center = androidx.compose.ui.geometry.Offset(500f, 1000f)
        val radius = 200f
        val positions = com.pixelody.app.ui.components.computeOrbitPositions(
            centerOffset = center,
            screenWidth = 1000f,
            screenHeight = 2000f,
            items = items,
            radiusPx = radius
        )
        assertEquals(5, positions.size)
        positions.forEach { pos ->
            assertTrue(pos.offset.x in 0f..1000f)
            assertTrue(pos.offset.y in 0f..2000f)
        }
    }

    @Test
    fun computeOrbitPositionsLeftEdgeBiasSpansRightQuadrants() {
        val items = com.pixelody.app.ui.components.buildOrbitActionItems(isFavorite = false, isOffline = false)
        val center = androidx.compose.ui.geometry.Offset(50f, 500f) // Left edge
        val positions = com.pixelody.app.ui.components.computeOrbitPositions(
            centerOffset = center,
            screenWidth = 1000f,
            screenHeight = 2000f,
            items = items,
            radiusPx = 150f
        )
        assertEquals(5, positions.size)
        assertEquals(-65f, positions.first().angleDegrees, 0.01f)
        assertEquals(65f, positions.last().angleDegrees, 0.01f)
    }

    @Test
    fun computeOrbitPositionsRightEdgeBiasSpansLeftQuadrants() {
        val items = com.pixelody.app.ui.components.buildOrbitActionItems(isFavorite = false, isOffline = false)
        val center = androidx.compose.ui.geometry.Offset(950f, 500f) // Right edge
        val positions = com.pixelody.app.ui.components.computeOrbitPositions(
            centerOffset = center,
            screenWidth = 1000f,
            screenHeight = 2000f,
            items = items,
            radiusPx = 150f
        )
        assertEquals(5, positions.size)
        assertEquals(115f, positions.first().angleDegrees, 0.01f)
        assertEquals(245f, positions.last().angleDegrees, 0.01f)
    }

    @Test
    fun computeOrbitPositionsClampsSafelyWithinScreenBounds() {
        val items = com.pixelody.app.ui.components.buildOrbitActionItems(isFavorite = false, isOffline = false)
        val center = androidx.compose.ui.geometry.Offset(10f, 10f) // Extreme corner
        val safeMargin = 44f
        val positions = com.pixelody.app.ui.components.computeOrbitPositions(
            centerOffset = center,
            screenWidth = 1000f,
            screenHeight = 2000f,
            items = items,
            radiusPx = 300f,
            safeMarginPx = safeMargin
        )
        positions.forEach { pos ->
            assertTrue("Offset X (${pos.offset.x}) should be >= $safeMargin", pos.offset.x >= safeMargin)
            assertTrue("Offset Y (${pos.offset.y}) should be >= $safeMargin", pos.offset.y >= safeMargin)
            assertTrue("Offset X (${pos.offset.x}) should be <= 1000 - $safeMargin", pos.offset.x <= 1000f - safeMargin)
            assertTrue("Offset Y (${pos.offset.y}) should be <= 2000 - $safeMargin", pos.offset.y <= 2000f - safeMargin)
        }
    }

    @Test
    fun classifyOrbitHoverDetectsHoverWithinHitRadius() {
        val item = com.pixelody.app.ui.components.OrbitActionItem(
            type = com.pixelody.app.ui.components.OrbitActionType.PlayNext,
            glyphType = com.pixelody.app.ui.components.TransportGlyphType.PlayNext,
            label = "Play Next",
            subtitle = ""
        )
        val sat = com.pixelody.app.ui.components.OrbitPosition(
            item = item,
            offset = androidx.compose.ui.geometry.Offset(200f, 200f),
            angleDegrees = 0f
        )
        val hover = com.pixelody.app.ui.components.classifyOrbitHover(
            touchOffset = androidx.compose.ui.geometry.Offset(210f, 215f), // dist = sqrt(10^2 + 15^2) = sqrt(325) ~ 18px < 40px
            satellites = listOf(sat),
            hitRadiusPx = 40f
        )
        assertEquals(com.pixelody.app.ui.components.OrbitActionType.PlayNext, hover)
    }

    @Test
    fun classifyOrbitHoverReturnsNullWhenOutsideHitRadius() {
        val item = com.pixelody.app.ui.components.OrbitActionItem(
            type = com.pixelody.app.ui.components.OrbitActionType.AddToQueue,
            glyphType = com.pixelody.app.ui.components.TransportGlyphType.AddToQueue,
            label = "Add",
            subtitle = ""
        )
        val sat = com.pixelody.app.ui.components.OrbitPosition(
            item = item,
            offset = androidx.compose.ui.geometry.Offset(200f, 200f),
            angleDegrees = 0f
        )
        val hover = com.pixelody.app.ui.components.classifyOrbitHover(
            touchOffset = androidx.compose.ui.geometry.Offset(350f, 350f), // far away
            satellites = listOf(sat),
            hitRadiusPx = 40f
        )
        org.junit.Assert.assertNull(hover)
    }
}
