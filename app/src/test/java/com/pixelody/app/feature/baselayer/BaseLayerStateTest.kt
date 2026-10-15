package com.pixelody.app.feature.baselayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The base layer's whole promise about Back is that it removes one thing and that you
 * could have named the thing first. Both halves are asserted here rather than trusted.
 */
class BaseLayerStateTest {

    private fun state(vararg intents: BaseIntent): BaseLayerState =
        intents.fold(BaseLayerState()) { acc, intent -> reduceBaseLayer(acc, intent) }

    private val collection = BasePush.Collection("playlist", "p2")

    /** Every shape a person can actually get the app into, for the sweeps below. */
    private fun everyReachableState(): List<BaseLayerState> {
        val bases = listOf(
            BaseLayerState(),
            state(BaseIntent.SelectDestination(BaseDestination.Library)),
            state(BaseIntent.Push(collection)),
            state(BaseIntent.Push(collection), BaseIntent.Push(BasePush.Technical)),
            state(BaseIntent.Push(BasePush.Acquire), BaseIntent.Push(BasePush.Technical))
        )
        val withSheets = bases.flatMap { base ->
            listOf(
                base,
                reduceBaseLayer(base, BaseIntent.OpenSheet(BaseSheet.Queue)),
                reduceBaseLayer(base, BaseIntent.OpenSheet(BaseSheet.Player)),
                reduceBaseLayer(
                    reduceBaseLayer(base, BaseIntent.OpenSheet(BaseSheet.Player)),
                    BaseIntent.OpenSheet(BaseSheet.Queue)
                )
            )
        }
        return withSheets.flatMap { base ->
            listOf(
                base,
                reduceBaseLayer(base, BaseIntent.ShowOverlay(BaseOverlay.SessionTray)),
                reduceBaseLayer(
                    reduceBaseLayer(base, BaseIntent.ShowOverlay(BaseOverlay.OpenCrate("c1"))),
                    BaseIntent.ShowOverlay(BaseOverlay.AddToCrate("Saline"))
                )
            )
        }
    }

    @Test
    fun backNeverRemovesTwoLayersAtOnce() {
        everyReachableState().forEach { before ->
            val after = reduceBaseLayer(before, BaseIntent.Back)
            val removed = before.depth - after.depth
            if (before.depth == 0) {
                assertEquals("nothing left to remove", 0, removed)
            } else {
                assertEquals("from depth ${before.depth}", 1, removed)
            }
        }
    }

    @Test
    fun backDoesWhatItSaidItWouldDo() {
        everyReachableState().forEach { before ->
            val after = reduceBaseLayer(before, BaseIntent.Back)
            when (before.backTarget()) {
                BackTarget.DismissOverlay -> {
                    assertEquals(before.overlays.size - 1, after.overlays.size)
                    assertEquals(before.sheet, after.sheet)
                    assertEquals(before.pushed, after.pushed)
                }
                BackTarget.CollapseQueueToPlayer -> {
                    assertEquals(BaseSheet.Player, after.sheet)
                    assertEquals(before.pushed, after.pushed)
                }
                BackTarget.CloseListeningSheet -> {
                    assertNull(after.sheet)
                    assertEquals(before.pushed, after.pushed)
                }
                is BackTarget.PopTo -> {
                    assertEquals(before.pushed.size - 1, after.pushed.size)
                    assertEquals(before.sheet, after.sheet)
                }
                BackTarget.LeaveApp -> assertEquals(before, after)
            }
        }
    }

    @Test
    fun theOverlayStackPopsOneAtATime() {
        val open = state(
            BaseIntent.ShowOverlay(BaseOverlay.TrackActions("t1")),
            BaseIntent.ShowOverlay(BaseOverlay.AddToCrate("Saline"))
        )
        assertEquals(2, open.overlays.size)

        val once = reduceBaseLayer(open, BaseIntent.Back)
        assertEquals(BaseOverlay.TrackActions("t1"), once.overlay)

        val twice = reduceBaseLayer(once, BaseIntent.Back)
        assertNull(twice.overlay)
    }

    @Test
    fun queueCollapsesToThePlayerRatherThanClosingTheListeningLayer() {
        val queue = state(
            BaseIntent.Push(collection),
            BaseIntent.OpenSheet(BaseSheet.Player),
            BaseIntent.OpenSheet(BaseSheet.Queue)
        )

        val back = reduceBaseLayer(queue, BaseIntent.Back)
        assertEquals(BaseSheet.Player, back.sheet)
        assertEquals(listOf(collection), back.pushed)

        val again = reduceBaseLayer(back, BaseIntent.Back)
        assertNull(again.sheet)
        assertEquals(listOf(collection), again.pushed)
    }

    @Test
    fun directQueueReturnsToItsDestinationOrCollectionAndRepeatedOpenKeepsOrigin() {
        for (destination in BaseDestination.values()) {
            for (pushes in listOf(emptyList(), listOf(collection))) {
                val base = BaseLayerState(destination = destination, pushed = pushes)
                val queue = reduceBaseLayer(base, BaseIntent.OpenSheet(BaseSheet.Queue))
                assertEquals(base.depth + 1, queue.depth)
                assertEquals(BackTarget.CloseListeningSheet, queue.backTarget())
                val reopened = reduceBaseLayer(queue, BaseIntent.OpenSheet(BaseSheet.Queue))
                assertFalse(reopened.queueReturnsToPlayer)
                assertEquals(base, reduceBaseLayer(reopened, BaseIntent.Back))
            }
        }
    }

    @Test
    fun queueEntryPointSurvivesSavedStateAndOldStatesKeepTheirExistingBackPath() {
        for (fromPlayer in listOf(false, true)) {
            val base = BaseLayerState(destination = BaseDestination.Library, pushed = listOf(collection),
                sheet = if (fromPlayer) BaseSheet.Player else null)
            val queue = reduceBaseLayer(base, BaseIntent.OpenSheet(BaseSheet.Queue))
            val saved = with(BaseLayerStateSaver) {
                with(object : androidx.compose.runtime.saveable.SaverScope {
                    override fun canBeSaved(value: Any) = true
                }) { save(queue) }
            }!!
            val restored = BaseLayerStateSaver.restore(saved)!!
            assertEquals(queue, restored)
            assertEquals(base, reduceBaseLayer(restored, BaseIntent.Back))
            val legacy = BaseLayerStateSaver.restore((saved as List<Any>).dropLast(1))!!
            assertTrue(legacy.queueReturnsToPlayer)
            assertEquals(BaseSheet.Player, reduceBaseLayer(legacy, BaseIntent.Back).sheet)
        }
    }

    @Test
    fun choosingADestinationIsAFreshStartButDoesNotStopTheMusic() {
        val deep = state(
            BaseIntent.SelectDestination(BaseDestination.Library),
            BaseIntent.Push(collection),
            BaseIntent.OpenSheet(BaseSheet.Player),
            BaseIntent.ShowOverlay(BaseOverlay.SessionTray)
        )

        val moved = reduceBaseLayer(deep, BaseIntent.SelectDestination(BaseDestination.Search))

        assertEquals(BaseDestination.Search, moved.destination)
        assertTrue(moved.pushed.isEmpty())
        assertTrue(moved.overlays.isEmpty())
        assertEquals(BaseSheet.Player, moved.sheet)
    }

    @Test
    fun backNamesTheDestinationItWillLandOn() {
        val one = state(BaseIntent.SelectDestination(BaseDestination.Library), BaseIntent.Push(collection))
        assertEquals(BackTarget.PopTo("Library"), one.backTarget())

        val two = reduceBaseLayer(one, BaseIntent.Push(BasePush.Technical))
        assertEquals(BackTarget.PopTo("Playlist"), two.backTarget())
    }

    @Test
    fun atTheRootBackBelongsToTheSystem() {
        assertTrue(BaseLayerState().backLeavesTheApp())
        assertEquals(BaseLayerState(), reduceBaseLayer(BaseLayerState(), BaseIntent.Back))

        assertFalse(state(BaseIntent.Push(BasePush.Acquire)).backLeavesTheApp())
        assertFalse(state(BaseIntent.OpenSheet(BaseSheet.Player)).backLeavesTheApp())
    }

    @Test
    fun rearrangingIsAbandonedWheneverTheLayerAboveItGoes() {
        val rearranging = state(
            BaseIntent.ShowOverlay(BaseOverlay.OpenCrate("c1")),
            BaseIntent.Rearrange("c1")
        )
        assertEquals("c1", rearranging.rearrangingCrate)

        assertNull(reduceBaseLayer(rearranging, BaseIntent.Back).rearrangingCrate)
        assertNull(
            reduceBaseLayer(
                rearranging,
                BaseIntent.SelectDestination(BaseDestination.Home)
            ).rearrangingCrate
        )
    }

    @Test
    fun wanderingBetweenAlbumsAndArtistsCannotGrowTheStackForever() {
        val wandered = (1..40).fold(BaseLayerState()) { acc, index ->
            reduceBaseLayer(
                acc,
                BaseIntent.Push(BasePush.Collection("album", "album:$index"))
            )
        }

        assertEquals(BASE_MAX_PUSH_DEPTH, wandered.pushed.size)
        assertEquals(
            BasePush.Collection("album", "album:40"),
            wandered.pushed.last()
        )
        assertEquals(
            BasePush.Collection("album", "album:33"),
            wandered.pushed.first()
        )
    }

    @Test
    fun everyDestinationHasItsOwnTag() {
        val tags = BaseDestination.values().map { it.navigationTestTag }
        assertEquals(tags.size, tags.distinct().size)
        tags.forEach { assertTrue(it, it.startsWith("destination:")) }
    }

    @Test
    fun thereAreThreeDestinationsAndCreateIsNotOneOfThem() {
        assertEquals(3, BaseDestination.values().size)
        assertFalse(BaseDestination.values().any { it.routeId == "create" })
    }

    @Test
    fun shellVariantDefaultsToBase() {
        assertEquals(PixelodyShellVariant.Base, PixelodyShellVariant.fromLaunchValue(null))
        assertEquals(PixelodyShellVariant.Base, PixelodyShellVariant.fromLaunchValue(""))
        assertEquals(PixelodyShellVariant.Base, PixelodyShellVariant.fromLaunchValue("base"))
        assertEquals(PixelodyShellVariant.Base, PixelodyShellVariant.fromLaunchValue("unknown"))
        assertEquals(PixelodyShellVariant.Base, PixelodyShellVariant.fromLaunchValue("legacy"))
    }

    @Test
    fun trackDetailPushPopsCleanlyAndRoundTripsThroughSaver() {
        val pushTrack = state(
            BaseIntent.SelectDestination(BaseDestination.Library),
            BaseIntent.Push(collection),
            BaseIntent.Push(BasePush.TrackDetail("t1"))
        )
        assertEquals(BackTarget.PopTo("Playlist"), pushTrack.backTarget())
        assertEquals(BasePush.TrackDetail("t1"), pushTrack.pushed.last())

        val popped = reduceBaseLayer(pushTrack, BaseIntent.Back)
        assertEquals(collection, popped.pushed.last())
        assertEquals(BackTarget.PopTo("Library"), popped.backTarget())
    }

    @Test
    fun playerViewsOverlayPushesAndPopsCleanly() {
        val withPlayer = state(
            BaseIntent.SelectDestination(BaseDestination.Home),
            BaseIntent.OpenSheet(BaseSheet.Player),
            BaseIntent.ShowOverlay(BaseOverlay.PlayerViews)
        )
        assertEquals(BaseOverlay.PlayerViews, withPlayer.overlay)
        assertEquals(BackTarget.DismissOverlay, withPlayer.backTarget())

        val dismissed = reduceBaseLayer(withPlayer, BaseIntent.Back)
        assertNull(dismissed.overlay)
        assertEquals(BaseSheet.Player, dismissed.sheet)
        assertEquals(BackTarget.CloseListeningSheet, dismissed.backTarget())
    }

    @Test
    fun sleepTimerOverlayPushesAndPopsCleanly() {
        val withTimer = state(
            BaseIntent.SelectDestination(BaseDestination.Home),
            BaseIntent.OpenSheet(BaseSheet.Player),
            BaseIntent.ShowOverlay(BaseOverlay.SleepTimer)
        )
        assertEquals(BaseOverlay.SleepTimer, withTimer.overlay)
        assertEquals(BackTarget.DismissOverlay, withTimer.backTarget())

        val dismissed = reduceBaseLayer(withTimer, BaseIntent.Back)
        assertNull(dismissed.overlay)
        assertEquals(BaseSheet.Player, dismissed.sheet)
    }

    @Test
    fun confirmDeletePlaylistOverlayPushesAndPopsCleanly() {
        val withConfirm = state(
            BaseIntent.SelectDestination(BaseDestination.Library),
            BaseIntent.ShowOverlay(BaseOverlay.ConfirmDeletePlaylist("user-pl-123", "My Favorites"))
        )
        assertTrue(withConfirm.overlay is BaseOverlay.ConfirmDeletePlaylist)
        val overlay = withConfirm.overlay as BaseOverlay.ConfirmDeletePlaylist
        assertEquals("user-pl-123", overlay.playlistId)
        assertEquals("My Favorites", overlay.playlistName)
        assertEquals(BackTarget.DismissOverlay, withConfirm.backTarget())

        val dismissed = reduceBaseLayer(withConfirm, BaseIntent.Back)
        assertNull(dismissed.overlay)
    }

    @Test
    fun trackActionsOverlayHoldsPlaylistIdContext() {
        val withActions = state(
            BaseIntent.SelectDestination(BaseDestination.Library),
            BaseIntent.ShowOverlay(BaseOverlay.TrackActions(trackId = "track-1", playlistId = "user-pl-123"))
        )
        assertTrue(withActions.overlay is BaseOverlay.TrackActions)
        val overlay = withActions.overlay as BaseOverlay.TrackActions
        assertEquals("track-1", overlay.trackId)
        assertEquals("user-pl-123", overlay.playlistId)
    }

    @Test
    fun namePlaylistOverlayHoldsQueueSeedTrackIds() {
        val seedTracks = listOf("t1", "t2", "t3")
        val withName = state(
            BaseIntent.SelectDestination(BaseDestination.Home),
            BaseIntent.ShowOverlay(BaseOverlay.NamePlaylist(seedTrackIds = seedTracks))
        )
        assertTrue(withName.overlay is BaseOverlay.NamePlaylist)
        val overlay = withName.overlay as BaseOverlay.NamePlaylist
        assertEquals(seedTracks, overlay.seedTrackIds)
        assertNull(overlay.playlistId)
    }

    @Test
    fun pitchAndSpeedOverlayShowsAndDismisses() {
        val withPitch = state(
            BaseIntent.OpenSheet(BaseSheet.Player),
            BaseIntent.ShowOverlay(BaseOverlay.PitchAndSpeed)
        )
        assertEquals(BaseOverlay.PitchAndSpeed, withPitch.overlay)
        assertEquals(BackTarget.DismissOverlay, withPitch.backTarget())

        val dismissed = reduceBaseLayer(withPitch, BaseIntent.Back)
        assertNull(dismissed.overlay)
        assertEquals(BaseSheet.Player, dismissed.sheet)
    }

    @Test
    fun loudnessNormalizationOverlayShowsAndDismisses() {
        val withLoudness = state(
            BaseIntent.ShowOverlay(BaseOverlay.SessionTray),
            BaseIntent.ShowOverlay(BaseOverlay.LoudnessNormalization)
        )
        assertEquals(BaseOverlay.LoudnessNormalization, withLoudness.overlay)
        assertEquals(BackTarget.DismissOverlay, withLoudness.backTarget())

        val backToTray = reduceBaseLayer(withLoudness, BaseIntent.Back)
        assertEquals(BaseOverlay.SessionTray, backToTray.overlay)
    }

    @Test
    fun lyricsOverlayShowsAndDismisses() {
        val withLyrics = state(
            BaseIntent.OpenSheet(BaseSheet.Player),
            BaseIntent.ShowOverlay(BaseOverlay.Lyrics)
        )
        assertEquals(BaseOverlay.Lyrics, withLyrics.overlay)
        assertEquals(BackTarget.DismissOverlay, withLyrics.backTarget())

        val dismissed = reduceBaseLayer(withLyrics, BaseIntent.Back)
        assertNull(dismissed.overlay)
        assertEquals(BaseSheet.Player, dismissed.sheet)
    }

    @Test
    fun tagEditorOverlayShowsAndDismisses() {
        val withEditor = state(
            BaseIntent.ShowOverlay(BaseOverlay.TrackActions("track-42")),
            BaseIntent.ShowOverlay(BaseOverlay.TagEditor("track-42"))
        )
        assertEquals(BaseOverlay.TagEditor("track-42"), withEditor.overlay)
        assertEquals(BackTarget.DismissOverlay, withEditor.backTarget())

        val backToActions = reduceBaseLayer(withEditor, BaseIntent.Back)
        assertEquals(BaseOverlay.TrackActions("track-42"), backToActions.overlay)
    }
}
