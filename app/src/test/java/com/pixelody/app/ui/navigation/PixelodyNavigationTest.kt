package com.pixelody.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelodyNavigationTest {
    @Test
    fun primaryRegistryMatchesTheCurrentControl() {
        assertEquals(
            listOf(PixelodyTab.Home, PixelodyTab.Search, PixelodyTab.Library, PixelodyTab.Create),
            PixelodyDestinationRegistry.controlPrimary
        )
        assertEquals(
            listOf(PixelodyTab.Home, PixelodyTab.Search, PixelodyTab.Library),
            PixelodyDestinationRegistry.challengerBPrimary
        )
        assertEquals(NavigationTier.Ownership, PixelodyTab.Create.tier)
        assertTrue(PixelodyDestinationRegistry.isSelectablePrimary(PixelodyTab.Create))
        assertEquals(
            PixelodyTab.entries.size,
            PixelodyTab.entries.map(PixelodyTab::routeId).distinct().size
        )
    }

    @Test
    fun debugLaunchValuesSelectOnlyRegisteredExperimentArms() {
        assertEquals(
            PixelodyNavigationVariant.ChallengerB,
            PixelodyNavigationVariant.fromLaunchValue("challenger-b")
        )
        assertEquals(
            PixelodyNavigationVariant.Control,
            PixelodyNavigationVariant.fromLaunchValue("unknown")
        )
        assertEquals(
            PixelodyNavigationVariant.Control,
            PixelodyNavigationVariant.fromLaunchValue(null)
        )
    }

    @Test
    fun createCanRemainTheControlRootWhileBeingAnOwnershipDestination() {
        val create = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Create)
        )

        assertEquals(PixelodyTab.Create, create.current)
        assertEquals(PixelodyTab.Create, create.selectedPrimary)
        assertTrue(create.history.isEmpty())
    }

    @Test
    fun challengerBShowsAddOnlyOnItsThreePrimarySurfaces() {
        assertTrue(
            shouldShowContextualCreateAction(
                PixelodyNavigationVariant.ChallengerB,
                PixelodyTab.Home
            )
        )
        assertTrue(
            shouldShowContextualCreateAction(
                PixelodyNavigationVariant.ChallengerB,
                PixelodyTab.Search
            )
        )
        assertTrue(
            shouldShowContextualCreateAction(
                PixelodyNavigationVariant.ChallengerB,
                PixelodyTab.Library
            )
        )
        assertFalse(
            shouldShowContextualCreateAction(
                PixelodyNavigationVariant.ChallengerB,
                PixelodyTab.Create
            )
        )
        assertFalse(
            shouldShowContextualCreateAction(
                PixelodyNavigationVariant.Control,
                PixelodyTab.Home
            )
        )
    }

    @Test
    fun challengerCreateActionReturnsToItsPrimaryOrigin() {
        val library = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Library)
        )
        val create = reduceNavigation(
            library,
            PixelodyNavigationIntent.NavigateTo(PixelodyTab.Create)
        )

        assertEquals(PixelodyTab.Create, create.current)
        assertEquals(PixelodyTab.Library, create.selectedPrimary)
        assertEquals(listOf(PixelodyTab.Library), create.history)

        val returned = reduceNavigation(create, PixelodyNavigationIntent.Back)
        assertEquals(PixelodyTab.Library, returned.current)
        assertTrue(returned.history.isEmpty())
    }

    @Test
    fun contextualDestinationReturnsToItsOrigin() {
        val library = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Library)
        )
        val queue = reduceNavigation(
            library,
            PixelodyNavigationIntent.NavigateTo(PixelodyTab.Queue)
        )

        assertEquals(PixelodyTab.Queue, queue.current)
        assertEquals(listOf(PixelodyTab.Library), queue.history)
        assertTrue(queue.canNavigateBack)

        val returned = reduceNavigation(queue, PixelodyNavigationIntent.Back)
        assertEquals(PixelodyTab.Library, returned.current)
        assertTrue(returned.history.isEmpty())
        assertFalse(returned.canNavigateBack)
    }

    @Test
    fun playerCollapseReturnsToTheExactOpeningDestination() {
        val search = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Search)
        )
        val player = reduceNavigation(search, PixelodyNavigationIntent.OpenPlayer)

        assertEquals(PixelodyTab.Player, player.current)
        assertEquals(PixelodyTab.Search, player.playerReturn)

        val collapsed = reduceNavigation(player, PixelodyNavigationIntent.CollapsePlayer)
        assertEquals(PixelodyTab.Search, collapsed.current)
        assertEquals(PixelodyTab.Search, collapsed.selectedPrimary)
        assertTrue(collapsed.history.isEmpty())
    }

    @Test
    fun selectingPrimaryDestinationClearsContextHistory() {
        val profile = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.NavigateTo(PixelodyTab.Profile)
        )
        val library = reduceNavigation(
            profile,
            PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Library)
        )

        assertEquals(PixelodyTab.Library, library.current)
        assertEquals(PixelodyTab.Library, library.selectedPrimary)
        assertTrue(library.history.isEmpty())
    }

    @Test
    fun primaryDestinationOpenedFromContentReturnsToItsOrigin() {
        val search = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.NavigateTo(PixelodyTab.Search)
        )

        assertEquals(PixelodyTab.Search, search.current)
        assertEquals(PixelodyTab.Home, search.selectedPrimary)
        assertEquals(listOf(PixelodyTab.Home), search.history)
        assertTrue(search.canNavigateBack)

        val returned = reduceNavigation(search, PixelodyNavigationIntent.Back)
        assertEquals(PixelodyTab.Home, returned.current)
        assertEquals(PixelodyTab.Home, returned.selectedPrimary)
        assertTrue(returned.history.isEmpty())
    }

    @Test
    fun historyIsBoundedDuringRepeatedContextNavigation() {
        var state = PixelodyNavigationState()
        repeat(30) { index ->
            val destination = if (index % 2 == 0) PixelodyTab.Profile else PixelodyTab.Queue
            state = reduceNavigation(state, PixelodyNavigationIntent.NavigateTo(destination))
        }

        assertEquals(16, state.history.size)
    }

    @Test
    fun navigationStateRoundTripsThroughItsSavedRepresentation() {
        val state = PixelodyNavigationState(
            current = PixelodyTab.Queue,
            selectedPrimary = PixelodyTab.Library,
            history = listOf(PixelodyTab.Library, PixelodyTab.Player),
            playerReturn = PixelodyTab.Library
        )

        assertEquals(state, decodeNavigationState(encodeNavigationState(state)))
    }

    @Test
    fun malformedSavedRepresentationFallsBackSafely() {
        assertEquals(PixelodyNavigationState(), decodeNavigationState(listOf("unknown")))
    }
}
