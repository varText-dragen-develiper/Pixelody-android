package com.pixelody.app.ui.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlin.math.abs

enum class PixelodyDetailKind(val storageKey: String) {
    Playlist("playlist"),
    Album("album"),
    Artist("artist"),
    Genre("genre"),
    Favorites("favorites"),
    Lossless("lossless"),
    Recent("recent");

    companion object {
        fun fromStorageKey(value: String?): PixelodyDetailKind? =
            entries.firstOrNull { it.storageKey.equals(value, ignoreCase = true) }
    }
}

data class PixelodyDetailRoute(
    val kind: PixelodyDetailKind,
    val id: String
)

data class NavigationChrome(
    val showsPrimaryNavigation: Boolean,
    val showsMiniPlayer: Boolean
)

val PixelodyTab.chrome: NavigationChrome
    get() = when (this) {
        PixelodyTab.Player -> NavigationChrome(
            showsPrimaryNavigation = false,
            showsMiniPlayer = false
        )
        else -> NavigationChrome(
            showsPrimaryNavigation = true,
            showsMiniPlayer = true
        )
    }

data class PixelodyDeepLinkTarget(
    val destination: PixelodyTab,
    val searchQuery: String = ""
)

object PixelodyDeepLink {
    const val SCHEME = "pixelody"
    const val PAIRING_HOST = "connect"
    const val SEARCH_QUERY_PARAMETER = "q"

    fun targetFor(scheme: String?, host: String?, searchQuery: String?): PixelodyDeepLinkTarget? {
        val destination = destinationFor(scheme, host) ?: return null
        val query = searchQuery
            ?.trim()
            ?.takeIf { it.isNotEmpty() && destination == PixelodyTab.Search }
            .orEmpty()
        return PixelodyDeepLinkTarget(destination, query)
    }

    fun destinationFor(scheme: String?, host: String?): PixelodyTab? {
        if (!scheme.equals(SCHEME, ignoreCase = true)) return null
        val normalized = host?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        if (normalized == PAIRING_HOST) return null
        return PixelodyTab.fromRouteId(normalized)
    }

    val routableHosts: List<String>
        get() = PixelodyTab.entries.map { it.routeId }
}

fun openDeepLink(
    state: PixelodyNavigationState,
    destination: PixelodyTab,
    availability: PixelodyListeningAvailability
): PixelodyNavigationState {
    val target = generateSequence(destination) { it.parent }
        .firstOrNull { it.isReachable(availability) }
        ?: state.selectedPrimary
    return when {
        PixelodyDestinationRegistry.isSelectablePrimary(target) -> selectPrimary(state, target)
        target == PixelodyTab.Player -> openPlayer(state)
        else -> navigateTo(state, target)
    }
}

object PixelodyStateTags {
    const val CONNECTION_STATUS = "state:connection"
    const val PLAYER_SOURCE = "state:playerSource"
    const val PLAYER_SCRUBBER = "state:playerScrubber"
    const val PLAYER_PLAY_PAUSE = "state:playerPlayPause"
    const val PLAYER_PREVIOUS = "state:playerPrevious"
    const val PLAYER_NEXT = "state:playerNext"
    const val MINI_PLAYER_PLAY_PAUSE = "state:miniPlayerPlayPause"
    const val MINI_PLAYER_NEXT = "state:miniPlayerNext"
    const val MINI_PLAYER_OPEN_PLAYER = "state:miniPlayerOpenPlayer"
    const val QUEUE_ACCESS = "state:queueAccess"
    const val SOURCE_RECOVERY = "state:sourceRecovery"
    const val LIBRARY_BROWSE_MODE = "state:libraryBrowseMode"
    const val LIBRARY_SORT_MODE = "state:librarySortMode"
    const val LIBRARY_COLLECTIONS = "state:libraryCollections"
    const val LIBRARY_SEARCH_FIELD = "state:librarySearchField"
    const val SEARCH_QUERY_FIELD = "state:searchQueryField"
    const val SEARCH_INTENT_FILTER = "state:searchIntentFilter"
    const val ACQUIRE_SCAN_DEVICE = "state:acquireScanDevice"
    const val ACQUIRE_PICK_FOLDER = "state:acquirePickFolder"
    const val ACQUIRE_PICK_FILES = "state:acquirePickFiles"
    const val CREATE_PAIR = "state:createPair"
    const val CREATE_HOST_TOGGLE = "state:createHostToggle"
    const val COLLECTION_DETAIL = "state:collectionDetail"
    const val COLLECTION_PLAY = "state:collectionPlay"
    const val COLLECTION_SHUFFLE = "state:collectionShuffle"
    const val COLLECTION_BACK = "state:collectionBack"
    const val CRATE_FACE = "state:crateFace"
    const val CRATE_SHEET = "state:crateSheet"
    const val CRATE_REARRANGE = "state:crateRearrange"
    const val LISTENING_SLOT = "state:listeningSlot"

    val all: List<String> = listOf(
        CONNECTION_STATUS,
        PLAYER_SOURCE,
        PLAYER_SCRUBBER,
        PLAYER_PLAY_PAUSE,
        PLAYER_PREVIOUS,
        PLAYER_NEXT,
        MINI_PLAYER_PLAY_PAUSE,
        MINI_PLAYER_NEXT,
        MINI_PLAYER_OPEN_PLAYER,
        QUEUE_ACCESS,
        SOURCE_RECOVERY,
        LIBRARY_BROWSE_MODE,
        LIBRARY_SORT_MODE,
        LIBRARY_COLLECTIONS,
        LIBRARY_SEARCH_FIELD,
        SEARCH_QUERY_FIELD,
        SEARCH_INTENT_FILTER,
        ACQUIRE_SCAN_DEVICE,
        ACQUIRE_PICK_FOLDER,
        ACQUIRE_PICK_FILES,
        CREATE_PAIR,
        CREATE_HOST_TOGGLE,
        COLLECTION_DETAIL,
        COLLECTION_PLAY,
        COLLECTION_SHUFFLE,
        COLLECTION_BACK,
        CRATE_FACE,
        CRATE_SHEET,
        CRATE_REARRANGE,
        LISTENING_SLOT
    )
}

enum class SwipeDirection {
    None, Next, Previous, OpenPlayer, CollapsePlayer
}

fun classifyMiniPlayerSwipe(dragX: Float, dragY: Float, thresholdPx: Float): SwipeDirection {
    return if (abs(dragX) > abs(dragY)) {
        when {
            dragX < -thresholdPx -> SwipeDirection.Next
            dragX > thresholdPx -> SwipeDirection.Previous
            else -> SwipeDirection.None
        }
    } else {
        if (dragY < -thresholdPx) SwipeDirection.OpenPlayer else SwipeDirection.None
    }
}

fun classifyFullPlayerSwipe(dragY: Float, thresholdPx: Float): SwipeDirection {
    return if (dragY > thresholdPx) SwipeDirection.CollapsePlayer else SwipeDirection.None
}

enum class NavigationTier {
    Primary,
    ListeningContext,
    Ownership,
    Technical
}

enum class PixelodyNavigationVariant(val launchValue: String) {
    Control("control"),
    ChallengerB("challenger-b");

    companion object {
        const val IntentExtra = "pixelody.navigation.variant"

        fun fromLaunchValue(value: String?): PixelodyNavigationVariant =
            entries.firstOrNull { it.launchValue == value } ?: Control
    }
}

enum class PixelodyTab(
    val routeId: String,
    val label: String,
    val tier: NavigationTier
) {
    Home("home", "Home", NavigationTier.Primary),
    Search("search", "Search", NavigationTier.Primary),
    Create("create", "Create", NavigationTier.Ownership),
    Profile("profile", "Profile", NavigationTier.Ownership),
    Sharing("technical", "Tools", NavigationTier.Technical),
    Device("files", "Files", NavigationTier.Ownership),
    Library("library", "Library", NavigationTier.Primary),
    Player("player", "Player", NavigationTier.ListeningContext),
    Queue("queue", "Queue", NavigationTier.ListeningContext);

    val isPrimary: Boolean
        get() = tier == NavigationTier.Primary

    companion object {
        fun fromRouteId(routeId: String?): PixelodyTab? =
            entries.firstOrNull { it.routeId == routeId }
    }
}

object PixelodyDestinationRegistry {
    val controlPrimary: List<PixelodyTab> = listOf(
        PixelodyTab.Home,
        PixelodyTab.Search,
        PixelodyTab.Library,
        PixelodyTab.Create
    )
    val challengerBPrimary: List<PixelodyTab> = listOf(
        PixelodyTab.Home,
        PixelodyTab.Search,
        PixelodyTab.Library
    )
    val primary: List<PixelodyTab> = controlPrimary
    private val selectablePrimaryDestinations = (controlPrimary + challengerBPrimary).toSet()

    fun primaryFor(variant: PixelodyNavigationVariant): List<PixelodyTab> = when (variant) {
        PixelodyNavigationVariant.Control -> controlPrimary
        PixelodyNavigationVariant.ChallengerB -> challengerBPrimary
    }

    fun isSelectablePrimary(destination: PixelodyTab): Boolean =
        destination in selectablePrimaryDestinations

    init {
        listOf(controlPrimary, challengerBPrimary).forEach { destinations ->
            check(destinations.distinct().size == destinations.size) { "Primary destinations must be unique." }
            check(destinations.firstOrNull() == PixelodyTab.Home) { "Every navigation variant must begin with Home." }
        }
        check(PixelodyTab.entries.map(PixelodyTab::routeId).distinct().size == PixelodyTab.entries.size) {
            "Destination route IDs must be unique."
        }
    }
}

fun shouldShowContextualCreateAction(
    variant: PixelodyNavigationVariant,
    current: PixelodyTab
): Boolean =
    variant == PixelodyNavigationVariant.ChallengerB &&
        current in PixelodyDestinationRegistry.challengerBPrimary

data class PixelodyNavigationState(
    val current: PixelodyTab = PixelodyTab.Home,
    val selectedPrimary: PixelodyTab = PixelodyTab.Home,
    val history: List<PixelodyTab> = emptyList(),
    val playerReturn: PixelodyTab = PixelodyTab.Home,
    val detailStack: List<PixelodyDetailRoute> = emptyList()
) {
    val canNavigateBack: Boolean
        get() = detailStack.isNotEmpty() || history.isNotEmpty() || current != selectedPrimary

    val activeDetail: PixelodyDetailRoute?
        get() = detailStack.lastOrNull()
}

sealed interface PixelodyNavigationIntent {
    data class SelectPrimary(val destination: PixelodyTab) : PixelodyNavigationIntent
    data class NavigateTo(val destination: PixelodyTab) : PixelodyNavigationIntent
    data object OpenPlayer : PixelodyNavigationIntent
    data object CollapsePlayer : PixelodyNavigationIntent
    data object Back : PixelodyNavigationIntent
    data class Restore(val availability: PixelodyListeningAvailability) : PixelodyNavigationIntent
    data class DeepLink(
        val destination: PixelodyTab,
        val availability: PixelodyListeningAvailability
    ) : PixelodyNavigationIntent
    data class OpenDetail(val route: PixelodyDetailRoute) : PixelodyNavigationIntent
    data object CloseDetail : PixelodyNavigationIntent
    data class ConnectionLost(val availability: PixelodyListeningAvailability) : PixelodyNavigationIntent
    data class HostRevoked(val availability: PixelodyListeningAvailability) : PixelodyNavigationIntent
}

fun reduceNavigation(
    state: PixelodyNavigationState,
    intent: PixelodyNavigationIntent
): PixelodyNavigationState = when (intent) {
    is PixelodyNavigationIntent.SelectPrimary -> selectPrimary(state, intent.destination)
    is PixelodyNavigationIntent.NavigateTo -> navigateTo(state, intent.destination)
    PixelodyNavigationIntent.OpenPlayer -> openPlayer(state)
    PixelodyNavigationIntent.CollapsePlayer -> collapsePlayer(state)
    PixelodyNavigationIntent.Back -> navigateBack(state)
    is PixelodyNavigationIntent.Restore -> withReachableDestinations(state, intent.availability)
    is PixelodyNavigationIntent.DeepLink -> openDeepLink(state, intent.destination, intent.availability)
    is PixelodyNavigationIntent.OpenDetail -> openDetail(state, intent.route)
    PixelodyNavigationIntent.CloseDetail -> closeDetail(state)
    is PixelodyNavigationIntent.ConnectionLost -> withReachableDestinations(state, intent.availability)
    is PixelodyNavigationIntent.HostRevoked -> withReachableDestinations(state, intent.availability)
}

internal fun selectPrimary(
    state: PixelodyNavigationState,
    destination: PixelodyTab
): PixelodyNavigationState {
    require(PixelodyDestinationRegistry.isSelectablePrimary(destination)) {
        "Primary navigation can only select a destination registered by an experiment arm."
    }
    return state.copy(
        current = destination,
        selectedPrimary = destination,
        history = emptyList(),
        detailStack = emptyList()
    )
}

internal fun navigateTo(
    state: PixelodyNavigationState,
    destination: PixelodyTab
): PixelodyNavigationState {
    if (destination == state.current) return state
    return state.copy(
        current = destination,
        history = appendHistory(state.history, state.current)
    )
}

internal fun openPlayer(state: PixelodyNavigationState): PixelodyNavigationState {
    if (state.current == PixelodyTab.Player) return state
    return state.copy(
        current = PixelodyTab.Player,
        history = appendHistory(state.history, state.current),
        playerReturn = state.current
    )
}

private fun collapsePlayer(state: PixelodyNavigationState): PixelodyNavigationState {
    if (state.current != PixelodyTab.Player) return state
    val target = state.playerReturn.takeUnless { it == PixelodyTab.Player } ?: state.selectedPrimary
    val history = state.history.toMutableList().apply {
        if (lastOrNull() == target) removeAt(lastIndex)
    }
    return state.copy(
        current = target,
        history = history
    )
}

internal fun openDetail(
    state: PixelodyNavigationState,
    route: PixelodyDetailRoute
): PixelodyNavigationState {
    val top = state.detailStack.lastOrNull()
    if (top == route) return state
    val base = if (top?.kind == route.kind) state.detailStack.dropLast(1) else state.detailStack
    return state.copy(detailStack = (base + route).takeLast(MAX_DETAIL_DEPTH))
}

internal fun closeDetail(state: PixelodyNavigationState): PixelodyNavigationState {
    if (state.detailStack.isEmpty()) return state
    return state.copy(detailStack = state.detailStack.dropLast(1))
}

private fun navigateBack(state: PixelodyNavigationState): PixelodyNavigationState {
    if (state.detailStack.isNotEmpty()) {
        return state.copy(detailStack = state.detailStack.dropLast(1))
    }
    val previous = state.history.lastOrNull()
    if (previous != null) {
        return state.copy(
            current = previous,
            history = state.history.dropLast(1)
        )
    }
    if (state.current != state.selectedPrimary) {
        return state.copy(current = state.selectedPrimary)
    }
    return state
}

private fun appendHistory(
    history: List<PixelodyTab>,
    destination: PixelodyTab
): List<PixelodyTab> = (history + destination).takeLast(MAX_HISTORY)

private const val MAX_HISTORY = 16
private const val MAX_DETAIL_DEPTH = 8
private const val EMPTY_HISTORY = "-"

internal fun encodeNavigationState(state: PixelodyNavigationState): List<String> = buildList {
    add(state.current.routeId)
    add(state.selectedPrimary.routeId)
    add(state.playerReturn.routeId)
    add(state.history.joinToString(",") { it.routeId }.ifEmpty { EMPTY_HISTORY })
    add(state.detailStack.size.toString())
    state.detailStack.forEach { detail ->
        add(detail.kind.storageKey)
        add(detail.id)
    }
}

internal fun decodeNavigationState(values: List<String>): PixelodyNavigationState {
    if (values.size < 4) return PixelodyNavigationState()
    val current = PixelodyTab.fromRouteId(values[0]) ?: return PixelodyNavigationState()
    val selectedPrimary = PixelodyTab.fromRouteId(values[1])
        ?.takeIf(PixelodyDestinationRegistry::isSelectablePrimary)
        ?: PixelodyTab.Home
    val playerReturn = PixelodyTab.fromRouteId(values[2]) ?: selectedPrimary
    val history = values[3]
        .takeUnless { it == EMPTY_HISTORY }
        ?.split(',')
        ?.mapNotNull(PixelodyTab::fromRouteId)
        ?.takeLast(MAX_HISTORY)
        .orEmpty()
    return PixelodyNavigationState(
        current = current,
        selectedPrimary = selectedPrimary,
        history = history,
        playerReturn = playerReturn,
        detailStack = decodeDetailStack(values)
    )
}

private fun decodeDetailStack(values: List<String>): List<PixelodyDetailRoute> {
    val declared = values.getOrNull(4)?.toIntOrNull() ?: return emptyList()
    if (declared <= 0) return emptyList()
    return (0 until declared).mapNotNull { index ->
        val kind = PixelodyDetailKind.fromStorageKey(values.getOrNull(5 + index * 2))
        val id = values.getOrNull(6 + index * 2)
        if (kind == null || id.isNullOrEmpty()) null else PixelodyDetailRoute(kind, id)
    }.takeLast(MAX_DETAIL_DEPTH)
}

val PixelodyNavigationStateSaver: Saver<PixelodyNavigationState, Any> = listSaver(
    save = { state -> encodeNavigationState(state) },
    restore = ::decodeNavigationState
)


enum class NavigationPaneRole {
    PrimaryPane,
    ListeningPane,
    DetailPane,
    OverlayPane
}

data class PixelodyListeningAvailability(
    val hasActiveTrack: Boolean = false,
    val hasQueuedTracks: Boolean = false
) {
    companion object {
        val Empty = PixelodyListeningAvailability()
    }
}

val PixelodyTab.parent: PixelodyTab?
    get() = when (this) {
        PixelodyTab.Home, PixelodyTab.Search, PixelodyTab.Library -> null
        PixelodyTab.Create, PixelodyTab.Device, PixelodyTab.Profile, PixelodyTab.Sharing -> PixelodyTab.Home
        PixelodyTab.Player -> null
        PixelodyTab.Queue -> PixelodyTab.Player
    }

val PixelodyTab.paneRole: NavigationPaneRole
    get() = when (this) {
        PixelodyTab.Home, PixelodyTab.Search, PixelodyTab.Library -> NavigationPaneRole.PrimaryPane
        PixelodyTab.Player, PixelodyTab.Queue -> NavigationPaneRole.ListeningPane
        PixelodyTab.Create, PixelodyTab.Device, PixelodyTab.Profile -> NavigationPaneRole.DetailPane
        PixelodyTab.Sharing -> NavigationPaneRole.OverlayPane
    }

val PixelodyTab.restorationKey: String
    get() = "pixelody.destination.$routeId"

val PixelodyTab.navigationTestTag: String
    get() = "destination:$routeId"

val PixelodyTab.semanticDescription: String
    get() = when (this) {
        PixelodyTab.Home -> "Home: what is connected, what is playing, and the next useful action"
        PixelodyTab.Search -> "Search host and phone music in one place"
        PixelodyTab.Library -> "Browse the paired host library"
        PixelodyTab.Create -> "Add phone music or start a portable source"
        PixelodyTab.Profile -> "Appearance, identity, and the entry to technical controls"
        PixelodyTab.Sharing -> "Connection, hosting, trusted devices, and J.A.M. diagnostics"
        PixelodyTab.Device -> "Phone files and portable source workflow"
        PixelodyTab.Player -> "Current playback, transport, and source quality"
        PixelodyTab.Queue -> "The queue that belongs to the current source"
    }

fun PixelodyTab.isReachable(availability: PixelodyListeningAvailability): Boolean = when (this) {
    PixelodyTab.Player -> availability.hasActiveTrack
    PixelodyTab.Queue -> availability.hasActiveTrack || availability.hasQueuedTracks
    else -> true
}

fun withReachableDestinations(
    state: PixelodyNavigationState,
    availability: PixelodyListeningAvailability
): PixelodyNavigationState {
    val history = state.history.filter { it.isReachable(availability) }
    val playerReturn = state.playerReturn.takeIf { it.isReachable(availability) } ?: state.selectedPrimary
    if (state.current.isReachable(availability)) {
        return state.copy(history = history, playerReturn = playerReturn)
    }
    val fallback = history.lastOrNull() ?: playerReturn
    return state.copy(
        current = fallback,
        history = if (history.isNotEmpty()) history.dropLast(1) else history,
        playerReturn = playerReturn
    )
}

enum class PixelodyPaneLayout {
    Compact,
    Rail,
    RailWithExpandedPlayer,
    RailWithListeningPanel;

    val usesNavigationRail: Boolean
        get() = this != Compact

    val usesExpandedPlayer: Boolean
        get() = this == RailWithExpandedPlayer || this == RailWithListeningPanel

    val usesListeningSidePanel: Boolean
        get() = this == RailWithListeningPanel
}

/**
 * How many rows the Library list puts above its tracks. The fast-scroll ribbon jumps
 * to a track by index, so this has to agree exactly with what the list emits; keeping
 * it here means it can be checked without a device.
 */
object LibraryListHeader {
    const val ALWAYS_PRESENT = 7

    fun itemCount(
        hasSourceRecovery: Boolean = false,
        hasHostStatusCard: Boolean = false,
        hasCollectionShortcuts: Boolean = false,
        hasBatchDownloadBanner: Boolean = false
    ): Int = ALWAYS_PRESENT +
        (if (hasSourceRecovery) 1 else 0) +
        (if (hasHostStatusCard) 1 else 0) +
        (if (hasCollectionShortcuts) 1 else 0) +
        (if (hasBatchDownloadBanner) 1 else 0)
}

object PixelodyAdaptivePolicy {
    const val NAVIGATION_RAIL_MIN_WIDTH_DP = 720
    const val EXPANDED_PLAYER_MIN_WIDTH_DP = 900
    const val LISTENING_SIDE_PANEL_MIN_WIDTH_DP = 1040

    /** Below this, a window is too short to give the listening layer its own pane. */
    const val ROOMY_WINDOW_MIN_HEIGHT_DP = 600

    private const val ASSUMED_ROOMY_HEIGHT_DP = 900

    fun forWidthDp(widthDp: Int): PixelodyPaneLayout =
        forWindow(widthDp, ASSUMED_ROOMY_HEIGHT_DP)

    fun forWindow(widthDp: Int, heightDp: Int): PixelodyPaneLayout {
        val roomyEnoughForPanes = heightDp >= ROOMY_WINDOW_MIN_HEIGHT_DP
        return when {
            widthDp >= LISTENING_SIDE_PANEL_MIN_WIDTH_DP && roomyEnoughForPanes ->
                PixelodyPaneLayout.RailWithListeningPanel
            widthDp >= EXPANDED_PLAYER_MIN_WIDTH_DP && roomyEnoughForPanes ->
                PixelodyPaneLayout.RailWithExpandedPlayer
            widthDp >= NAVIGATION_RAIL_MIN_WIDTH_DP ->
                PixelodyPaneLayout.Rail
            else ->
                PixelodyPaneLayout.Compact
        }
    }
}
