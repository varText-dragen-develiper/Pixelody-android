package com.pixelody.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelodyNavigationRecoveryTest {

    private val listening = PixelodyListeningAvailability(hasActiveTrack = true, hasQueuedTracks = true)
    private val silent = PixelodyListeningAvailability.Empty

    @Test
    fun playerIsUnreachableWithoutAnActiveTrack() {
        assertFalse(PixelodyTab.Player.isReachable(silent))
        assertTrue(PixelodyTab.Player.isReachable(PixelodyListeningAvailability(hasActiveTrack = true)))
    }

    @Test
    fun queueStaysReachableWhileTracksRemainQueued() {
        assertTrue(PixelodyTab.Queue.isReachable(PixelodyListeningAvailability(hasQueuedTracks = true)))
        assertFalse(PixelodyTab.Queue.isReachable(silent))
    }

    @Test
    fun primaryDestinationsAreNeverGatedByListeningState() {
        PixelodyDestinationRegistry.controlPrimary.forEach { destination ->
            assertTrue(destination.routeId, destination.isReachable(silent))
        }
        assertTrue(PixelodyTab.Device.isReachable(silent))
        assertTrue(PixelodyTab.Sharing.isReachable(silent))
        assertTrue(PixelodyTab.Profile.isReachable(silent))
    }

    @Test
    fun revokedHostLeavesThePlayerForTheDestinationItWasOpenedFrom() {
        val opened = reduceNavigation(
            reduceNavigation(PixelodyNavigationState(), PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Library)),
            PixelodyNavigationIntent.OpenPlayer
        )
        assertEquals(PixelodyTab.Player, opened.current)

        val recovered = reduceNavigation(opened, PixelodyNavigationIntent.HostRevoked(silent))

        assertEquals(PixelodyTab.Library, recovered.current)
        assertFalse(recovered.history.contains(PixelodyTab.Player))
    }

    @Test
    fun connectionLossCannotLeaveBackPointingAtAnEmptyPlayer() {
        val browsing = reduceNavigation(PixelodyNavigationState(), PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Home))
        val player = reduceNavigation(browsing, PixelodyNavigationIntent.OpenPlayer)
        val queue = reduceNavigation(player, PixelodyNavigationIntent.NavigateTo(PixelodyTab.Queue))

        val recovered = reduceNavigation(queue, PixelodyNavigationIntent.ConnectionLost(silent))
        val afterBack = reduceNavigation(recovered, PixelodyNavigationIntent.Back)

        assertFalse(recovered.current == PixelodyTab.Player)
        assertFalse(recovered.current == PixelodyTab.Queue)
        assertFalse(afterBack.current == PixelodyTab.Player)
        assertFalse(afterBack.current == PixelodyTab.Queue)
    }

    @Test
    fun localPlaybackSurvivesHostLoss() {
        val player = reduceNavigation(
            reduceNavigation(PixelodyNavigationState(), PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Library)),
            PixelodyNavigationIntent.OpenPlayer
        )

        val recovered = reduceNavigation(
            player,
            PixelodyNavigationIntent.ConnectionLost(PixelodyListeningAvailability(hasActiveTrack = true))
        )

        assertEquals(PixelodyTab.Player, recovered.current)
    }

    @Test
    fun restoringAReachableStateChangesNothing() {
        val state = reduceNavigation(
            reduceNavigation(PixelodyNavigationState(), PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Search)),
            PixelodyNavigationIntent.NavigateTo(PixelodyTab.Profile)
        )

        assertEquals(state, reduceNavigation(state, PixelodyNavigationIntent.Restore(listening)))
    }

    @Test
    fun restoreIsIdempotent() {
        val player = reduceNavigation(PixelodyNavigationState(), PixelodyNavigationIntent.OpenPlayer)
        val once = reduceNavigation(player, PixelodyNavigationIntent.Restore(silent))
        val twice = reduceNavigation(once, PixelodyNavigationIntent.Restore(silent))

        assertEquals(once, twice)
    }

    @Test
    fun restoredPlayerReturnNeverPointsAtAnUnreachableDestination() {
        val state = PixelodyNavigationState(
            current = PixelodyTab.Home,
            selectedPrimary = PixelodyTab.Home,
            history = listOf(PixelodyTab.Player, PixelodyTab.Library),
            playerReturn = PixelodyTab.Player
        )

        val recovered = reduceNavigation(state, PixelodyNavigationIntent.Restore(silent))

        assertFalse(recovered.history.contains(PixelodyTab.Player))
        assertTrue(recovered.playerReturn.isReachable(silent))
    }

    @Test
    fun everyDestinationDeclaresItsObservationIdentity() {
        PixelodyTab.entries.forEach { destination ->
            assertTrue(destination.routeId, destination.navigationTestTag.isNotBlank())
            assertTrue(destination.routeId, destination.restorationKey.isNotBlank())
            assertTrue(destination.routeId, destination.semanticDescription.isNotBlank())
        }
        assertEquals(
            PixelodyTab.entries.size,
            PixelodyTab.entries.map { it.navigationTestTag }.distinct().size
        )
        assertEquals(
            PixelodyTab.entries.size,
            PixelodyTab.entries.map { it.restorationKey }.distinct().size
        )
    }

    @Test
    fun queueDeclaresThePlayerAsItsParent() {
        assertEquals(PixelodyTab.Player, PixelodyTab.Queue.parent)
        assertNull(PixelodyTab.Home.parent)
        assertEquals(NavigationPaneRole.ListeningPane, PixelodyTab.Player.paneRole)
        assertEquals(NavigationPaneRole.PrimaryPane, PixelodyTab.Library.paneRole)
    }

    @Test
    fun adaptivePolicyMatchesTheDocumentedThresholds() {
        assertEquals(PixelodyPaneLayout.Compact, PixelodyAdaptivePolicy.forWidthDp(384))
        assertEquals(PixelodyPaneLayout.Compact, PixelodyAdaptivePolicy.forWidthDp(719))
        assertEquals(PixelodyPaneLayout.Rail, PixelodyAdaptivePolicy.forWidthDp(720))
        assertEquals(PixelodyPaneLayout.Rail, PixelodyAdaptivePolicy.forWidthDp(899))
        assertEquals(PixelodyPaneLayout.RailWithExpandedPlayer, PixelodyAdaptivePolicy.forWidthDp(900))
        assertEquals(PixelodyPaneLayout.RailWithExpandedPlayer, PixelodyAdaptivePolicy.forWidthDp(1039))
        assertEquals(PixelodyPaneLayout.RailWithListeningPanel, PixelodyAdaptivePolicy.forWidthDp(1040))
    }

    @Test
    fun paneLayoutCapabilitiesEscalateWithWidth() {
        assertFalse(PixelodyPaneLayout.Compact.usesNavigationRail)
        assertTrue(PixelodyPaneLayout.Rail.usesNavigationRail)
        assertFalse(PixelodyPaneLayout.Rail.usesExpandedPlayer)
        assertTrue(PixelodyPaneLayout.RailWithExpandedPlayer.usesExpandedPlayer)
        assertFalse(PixelodyPaneLayout.RailWithExpandedPlayer.usesListeningSidePanel)
        assertTrue(PixelodyPaneLayout.RailWithListeningPanel.usesListeningSidePanel)
        assertTrue(PixelodyPaneLayout.RailWithListeningPanel.usesExpandedPlayer)
    }

    @Test
    fun stateTagsAreUniqueNamespacedAndDistinctFromDestinations() {
        assertEquals(PixelodyStateTags.all.size, PixelodyStateTags.all.distinct().size)
        PixelodyStateTags.all.forEach { tag ->
            assertTrue(tag, tag.startsWith("state:"))
            assertTrue(tag, tag.length > "state:".length)
        }
        val destinationTags = PixelodyTab.entries.map { it.navigationTestTag }
        PixelodyStateTags.all.forEach { tag ->
            assertFalse(tag, tag in destinationTags)
        }
    }

    @Test
    fun miniPlayerSwipeVocabularyShipsInProduction() {
        val threshold = 56f
        assertEquals(SwipeDirection.Next, classifyMiniPlayerSwipe(-80f, 0f, threshold))
        assertEquals(SwipeDirection.Previous, classifyMiniPlayerSwipe(80f, 0f, threshold))
        assertEquals(SwipeDirection.None, classifyMiniPlayerSwipe(30f, 0f, threshold))
        assertEquals(SwipeDirection.OpenPlayer, classifyMiniPlayerSwipe(0f, -80f, threshold))
        assertEquals(SwipeDirection.CollapsePlayer, classifyFullPlayerSwipe(80f, threshold))
    }

    @Test
    fun everyRoutableHostResolvesToItsDestination() {
        PixelodyTab.entries.forEach { destination ->
            assertEquals(
                destination,
                PixelodyDeepLink.destinationFor("pixelody", destination.routeId)
            )
        }
        assertEquals(PixelodyTab.Sharing, PixelodyDeepLink.destinationFor("PIXELODY", "TECHNICAL"))
    }

    @Test
    fun deepLinkingRefusesForeignSchemesAndThePairingHost() {
        assertNull(PixelodyDeepLink.destinationFor("https", "library"))
        assertNull(PixelodyDeepLink.destinationFor("pixelody", "connect"))
        assertNull(PixelodyDeepLink.destinationFor("pixelody", ""))
        assertNull(PixelodyDeepLink.destinationFor("pixelody", null))
        assertNull(PixelodyDeepLink.destinationFor("pixelody", "not-a-route"))
    }

    @Test
    fun deepLinkingToAPrimaryDestinationSelectsItRatherThanPushingIt() {
        val state = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.DeepLink(PixelodyTab.Library, listening)
        )

        assertEquals(PixelodyTab.Library, state.current)
        assertEquals(PixelodyTab.Library, state.selectedPrimary)
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun deepLinkingToTheQueueKeepsThePlayerReturnPath() {
        val state = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.DeepLink(PixelodyTab.Queue, listening)
        )

        assertEquals(PixelodyTab.Queue, state.current)
        assertEquals(PixelodyTab.Home, reduceNavigation(state, PixelodyNavigationIntent.Back).current)
    }

    @Test
    fun deepLinkingIntoAnEmptyPlayerLandsSomewhereReal() {
        val player = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.DeepLink(PixelodyTab.Player, silent)
        )
        val queue = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.DeepLink(PixelodyTab.Queue, silent)
        )

        assertTrue(player.current.isReachable(silent))
        assertTrue(queue.current.isReachable(silent))
        assertFalse(player.current == PixelodyTab.Player)
        assertFalse(queue.current == PixelodyTab.Queue)
    }

    @Test
    fun onlyThePlayerTakesOverTheWholeScreen() {
        assertFalse(PixelodyTab.Player.chrome.showsPrimaryNavigation)
        assertFalse(PixelodyTab.Player.chrome.showsMiniPlayer)
        PixelodyTab.entries.filterNot { it == PixelodyTab.Player }.forEach { destination ->
            assertTrue(destination.routeId, destination.chrome.showsPrimaryNavigation)
            assertTrue(destination.routeId, destination.chrome.showsMiniPlayer)
        }
    }

    private val albumWithAwkwardName =
        PixelodyDetailRoute(PixelodyDetailKind.Album, "Songs, Vol. 2: B-Sides, Rarities")

    @Test
    fun backRemovesTheDetailBeforeItLeavesTheDestination() {
        val browsing = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Library)
        )
        val drilled = reduceNavigation(
            browsing,
            PixelodyNavigationIntent.OpenDetail(albumWithAwkwardName)
        )

        assertTrue(drilled.canNavigateBack)
        assertEquals(albumWithAwkwardName, drilled.activeDetail)

        val back = reduceNavigation(drilled, PixelodyNavigationIntent.Back)

        assertNull(back.activeDetail)
        assertEquals(PixelodyTab.Library, back.current)
    }

    @Test
    fun openingAnotherCollectionOfTheSameKindReplacesRatherThanStacks() {
        val first = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.OpenDetail(PixelodyDetailRoute(PixelodyDetailKind.Album, "one"))
        )
        val second = reduceNavigation(
            first,
            PixelodyNavigationIntent.OpenDetail(PixelodyDetailRoute(PixelodyDetailKind.Album, "two"))
        )

        assertEquals(1, second.detailStack.size)
        assertEquals("two", second.activeDetail?.id)
        assertNull(reduceNavigation(second, PixelodyNavigationIntent.Back).activeDetail)
    }

    @Test
    fun reopeningTheSameCollectionIsNotANewLayer() {
        val open = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.OpenDetail(albumWithAwkwardName)
        )

        assertEquals(open, reduceNavigation(open, PixelodyNavigationIntent.OpenDetail(albumWithAwkwardName)))
    }

    @Test
    fun choosingAPrimaryDestinationClearsTheDetail() {
        val drilled = reduceNavigation(
            PixelodyNavigationState(),
            PixelodyNavigationIntent.OpenDetail(albumWithAwkwardName)
        )
        val switched = reduceNavigation(
            drilled,
            PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Search)
        )

        assertNull(switched.activeDetail)
        assertFalse(switched.canNavigateBack)
    }

    @Test
    fun openingThePlayerFromADetailReturnsToThatDetail() {
        val drilled = reduceNavigation(
            reduceNavigation(PixelodyNavigationState(), PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Library)),
            PixelodyNavigationIntent.OpenDetail(albumWithAwkwardName)
        )
        val player = reduceNavigation(drilled, PixelodyNavigationIntent.OpenPlayer)
        val collapsed = reduceNavigation(player, PixelodyNavigationIntent.CollapsePlayer)

        assertEquals(PixelodyTab.Library, collapsed.current)
        assertEquals(albumWithAwkwardName, collapsed.activeDetail)
    }

    @Test
    fun closingAnEmptyDetailStackDoesNothing() {
        val state = PixelodyNavigationState()

        assertEquals(state, reduceNavigation(state, PixelodyNavigationIntent.CloseDetail))
    }

    @Test
    fun aCollectionNameFullOfPunctuationSurvivesTheSavedRepresentation() {
        val state = reduceNavigation(
            reduceNavigation(PixelodyNavigationState(), PixelodyNavigationIntent.SelectPrimary(PixelodyTab.Library)),
            PixelodyNavigationIntent.OpenDetail(albumWithAwkwardName)
        )

        val restored = decodeNavigationState(encodeNavigationState(state))

        assertEquals(state, restored)
        assertEquals(albumWithAwkwardName, restored.activeDetail)
    }

    @Test
    fun anOlderSavedRepresentationWithoutADetailStackStillRestores() {
        val legacy = listOf("library", "library", "home", "home")

        val restored = decodeNavigationState(legacy)

        assertEquals(PixelodyTab.Library, restored.current)
        assertTrue(restored.detailStack.isEmpty())
    }

    @Test
    fun aShortWindowNeverGetsAPaneItCannotAfford() {
        assertEquals(
            PixelodyPaneLayout.Rail,
            PixelodyAdaptivePolicy.forWindow(widthDp = 1200, heightDp = 400)
        )
        assertEquals(
            PixelodyPaneLayout.RailWithListeningPanel,
            PixelodyAdaptivePolicy.forWindow(widthDp = 1200, heightDp = 800)
        )
        assertEquals(
            PixelodyPaneLayout.Rail,
            PixelodyAdaptivePolicy.forWindow(widthDp = 960, heightDp = 500)
        )
        assertEquals(
            PixelodyPaneLayout.RailWithExpandedPlayer,
            PixelodyAdaptivePolicy.forWindow(widthDp = 960, heightDp = 640)
        )
    }

    @Test
    fun aNarrowWindowStaysCompactHoweverTallItIs() {
        assertEquals(
            PixelodyPaneLayout.Compact,
            PixelodyAdaptivePolicy.forWindow(widthDp = 384, heightDp = 2000)
        )
    }

    @Test
    fun theWidthOnlyPolicyStillAgreesWithARoomyWindow() {
        listOf(384, 719, 720, 899, 900, 1039, 1040, 1600).forEach { width ->
            assertEquals(
                width.toString(),
                PixelodyAdaptivePolicy.forWidthDp(width),
                PixelodyAdaptivePolicy.forWindow(width, 900)
            )
        }
    }

    @Test
    fun theLibraryHeaderCountMatchesEveryCombinationOfOptionalRows() {
        assertEquals(7, LibraryListHeader.itemCount())
        assertEquals(8, LibraryListHeader.itemCount(hasHostStatusCard = true))
        assertEquals(8, LibraryListHeader.itemCount(hasSourceRecovery = true))
        assertEquals(8, LibraryListHeader.itemCount(hasCollectionShortcuts = true))
        assertEquals(8, LibraryListHeader.itemCount(hasBatchDownloadBanner = true))
        assertEquals(
            11,
            LibraryListHeader.itemCount(
                hasSourceRecovery = true,
                hasHostStatusCard = true,
                hasCollectionShortcuts = true,
                hasBatchDownloadBanner = true
            )
        )
    }

    @Test
    fun theOrdinaryConnectedCaseIsEightNotSix() {
        // the old hand-written constant was 6, which put every alphabet jump two rows late
        assertEquals(8, LibraryListHeader.itemCount(hasHostStatusCard = true))
    }

    @Test
    fun onlySearchCarriesAQueryFromItsLink() {
        val search = PixelodyDeepLink.targetFor("pixelody", "search", "boards of canada")
        assertEquals(PixelodyTab.Search, search?.destination)
        assertEquals("boards of canada", search?.searchQuery)

        val library = PixelodyDeepLink.targetFor("pixelody", "library", "boards of canada")
        assertEquals(PixelodyTab.Library, library?.destination)
        assertEquals("", library?.searchQuery)
    }

    @Test
    fun anEmptyOrMissingQueryIsNotAQuery() {
        assertEquals("", PixelodyDeepLink.targetFor("pixelody", "search", null)?.searchQuery)
        assertEquals("", PixelodyDeepLink.targetFor("pixelody", "search", "   ")?.searchQuery)
        assertEquals("aphex", PixelodyDeepLink.targetFor("pixelody", "search", "  aphex  ")?.searchQuery)
    }

    @Test
    fun aRefusedLinkHasNoTargetAtAll() {
        assertNull(PixelodyDeepLink.targetFor("https", "search", "x"))
        assertNull(PixelodyDeepLink.targetFor("pixelody", "connect", "x"))
        assertNull(PixelodyDeepLink.targetFor("pixelody", "nonsense", "x"))
    }

    @Test
    fun noDeepLinkResolvesToAnActionOrToPairing() {
        // A deep link selects a destination. It must never be a way for another app
        // to act as the user, and it must never reach the pairing flow, which is the
        // one URI shape that carries a credential.
        PixelodyDeepLink.routableHosts.forEach { host ->
            val destination = PixelodyDeepLink.destinationFor("pixelody", host)
            assertTrue(host, destination != null)
            assertFalse(host, host == PixelodyDeepLink.PAIRING_HOST)
        }
        assertNull(PixelodyDeepLink.destinationFor("pixelody", PixelodyDeepLink.PAIRING_HOST))
    }

    @Test
    fun everyRoutableHostIsADeclaredDestinationAndNothingElse() {
        val declared = PixelodyTab.entries.map { it.routeId }.toSet()
        assertEquals(declared, PixelodyDeepLink.routableHosts.toSet())
    }

    @Test
    fun theJourneySuiteCanAddressSearchAndAcquisition() {
        // J02 pairs and hosts, J03 and J04 search and then distinguish sources, and
        // all three need acquisition reachable. None of these controls can be found
        // by their words: "Scan Entire Device" becomes "Scanning Entire Device...",
        // and "Start Phone Host" is replaced outright by "Stop Hosting".
        val journeyCritical = listOf(
            PixelodyStateTags.SEARCH_QUERY_FIELD,
            PixelodyStateTags.SEARCH_INTENT_FILTER,
            PixelodyStateTags.ACQUIRE_SCAN_DEVICE,
            PixelodyStateTags.ACQUIRE_PICK_FOLDER,
            PixelodyStateTags.ACQUIRE_PICK_FILES,
            PixelodyStateTags.CREATE_PAIR,
            PixelodyStateTags.CREATE_HOST_TOGGLE
        )
        journeyCritical.forEach { tag ->
            assertTrue(tag, tag in PixelodyStateTags.all)
        }
    }

    @Test
    fun theTwoSearchFieldsAreAddressedSeparately() {
        // Library filters what is already listed; Search queries everything. A suite
        // that cannot tell them apart would report the wrong one as covered.
        assertFalse(
            PixelodyStateTags.SEARCH_QUERY_FIELD == PixelodyStateTags.LIBRARY_SEARCH_FIELD
        )
    }

    @Test
    fun oneControlThatAppearsTwiceKeepsOneName() {
        // Create and Device Library carry the same three acquisition controls. They
        // share tags on purpose: the same job answering to two names is how a suite
        // ends up asserting a surface instead of a capability.
        assertTrue(PixelodyStateTags.ACQUIRE_SCAN_DEVICE.startsWith("state:acquire"))
        assertTrue(PixelodyStateTags.ACQUIRE_PICK_FOLDER.startsWith("state:acquire"))
        assertTrue(PixelodyStateTags.ACQUIRE_PICK_FILES.startsWith("state:acquire"))
    }
}
