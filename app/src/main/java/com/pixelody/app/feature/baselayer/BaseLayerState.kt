package com.pixelody.app.feature.baselayer

/**
 * Navigation state for the base layer shell.
 *
 * This is the base layer standard written as code. The two laws it
 * exists to enforce mechanically:
 *
 * - **H8, one action out.** Back removes exactly one layer, from the top, never two
 *   and never skipping. Overlays are a stack rather than a single slot precisely
 *   because "add to a crate" opens from inside another overlay, and collapsing that
 *   into one field would make Back remove two things at once.
 * - **H8 again, predictably.** [backTarget] names where Back will land *before* it is
 *   pressed. A test asserts that what it names is what Back actually removes, for
 *   every reachable shape — which is the only way "you could have predicted it" stops
 *   being a claim and starts being a property.
 *
 * Note what is absent. The old shell needed `isReachable` because Player and Queue
 * could be destinations with nothing behind them. Here the listening slot is
 * permanent (H9), so there is no unreachable listening state to guard against — it
 * is occupied, resuming, or empty, and all three are legitimate. A structural rule
 * removed a whole class of guard.
 */

/**
 * Album to artist to album to artist is a loop a person can walk forever, and every
 * step of it was a reasonable tap. Without a ceiling the stack grows without bound and
 * Back retraces the whole wander one press at a time, which is not "one action out" in
 * any sense a person would recognise. Oldest entries fall off the bottom.
 */
const val BASE_MAX_PUSH_DEPTH = 8

enum class BaseDestination(val routeId: String, val label: String) {
    Home("home", "Home"),
    Search("search", "Search"),
    Library("library", "Library");

    val navigationTestTag: String get() = "destination:$routeId"
}

/** L1. Pushed over a destination, keeps its origin, returns to it. */
sealed class BasePush {
    data class Collection(val kindKey: String, val collectionId: String) : BasePush()
    data class TrackDetail(val trackId: String) : BasePush()
    object Acquire : BasePush()
    object Appearance : BasePush()
    object Technical : BasePush()
    object SonicTimeline : BasePush()
}

/** L2. The listening layer, which rides above without destroying what is beneath. */
enum class BaseSheet { Player, Queue }

sealed class BaseOverlay {
    data class TrackActions(val trackId: String, val playlistId: String? = null) : BaseOverlay()
    data class CollectionActions(val kindKey: String, val collectionId: String) : BaseOverlay()
    object SessionTray : BaseOverlay()
    object LensPicker : BaseOverlay()
    object PlayerViews : BaseOverlay()
    object SleepTimer : BaseOverlay()
    object PitchAndSpeed : BaseOverlay()
    object LoudnessNormalization : BaseOverlay()
    object Lyrics : BaseOverlay()
    data class TagEditor(val trackId: String) : BaseOverlay()
    data class OpenCrate(val crateId: String) : BaseOverlay()
    data class CrateActions(val crateId: String) : BaseOverlay()
    /** Change the picture or add a note for anything that has an image; [key] is a cover key. */
    data class CoverActions(val key: String, val title: String) : BaseOverlay()
    data class CoverNote(val key: String, val title: String) : BaseOverlay()
    data class SlotActions(val crateId: String, val slotIndex: Int) : BaseOverlay()
    data class AddToCrate(
        val description: String,
        val payloadKind: String = "",
        val payloadId: String = ""
    ) : BaseOverlay()

    /**
     * Naming a crate, whether it exists yet or not. [crateId] null means the name is
     * about to bring one into being, with [payloadKind]/[payloadId] going into slot 1
     * — because a crate is created by putting something in it, never as an empty box
     * waiting to be filled.
     */
    data class NameCrate(
        val crateId: String? = null,
        val seedName: String = "",
        val payloadKind: String = "",
        val payloadId: String = ""
    ) : BaseOverlay()

    object SmartCrateBuilder : BaseOverlay()
    object JamHub : BaseOverlay()
    data class AddToPlaylist(val trackId: String) : BaseOverlay()
    data class NamePlaylist(
        val playlistId: String? = null,
        val seedName: String = "",
        val trackId: String = "",
        val seedTrackIds: List<String> = emptyList()
    ) : BaseOverlay()
    data class ConfirmDeletePlaylist(val playlistId: String, val playlistName: String) : BaseOverlay()
    data class Documentation(val topicId: String) : BaseOverlay()
}

data class BaseLayerState(
    val destination: BaseDestination = BaseDestination.Home,
    val pushed: List<BasePush> = emptyList(),
    val sheet: BaseSheet? = null,
    val overlays: List<BaseOverlay> = emptyList(),
    val rearrangingCrate: String? = null,
    val queueReturnsToPlayer: Boolean = true
) {
    val overlay: BaseOverlay? get() = overlays.lastOrNull()

    /**
     * How many layers sit above the chosen destination.
     *
     * Queue adds one layer to its actual entry point. Direct entry covers the
     * destination; entry from Player covers Player plus the destination.
     */
    val depth: Int get() = pushed.size + sheetDepth + overlays.size

    private val sheetDepth: Int
        get() = when (sheet) {
            null -> 0
            BaseSheet.Player -> 1
            BaseSheet.Queue -> if (queueReturnsToPlayer) 2 else 1
        }
}

sealed class BaseIntent {
    data class SelectDestination(val destination: BaseDestination) : BaseIntent()
    data class Push(val push: BasePush) : BaseIntent()
    data class OpenSheet(val sheet: BaseSheet) : BaseIntent()
    data class ShowOverlay(val overlay: BaseOverlay) : BaseIntent()
    data class Rearrange(val crateId: String?) : BaseIntent()
    object Back : BaseIntent()
}

/**
 * What Back will do next, so a surface can say so out loud before anyone commits to
 * pressing it.
 */
sealed class BackTarget {
    /** Close the topmost overlay. */
    object DismissOverlay : BackTarget()

    /** Queue collapses to the player rather than closing the listening layer. */
    object CollapseQueueToPlayer : BackTarget()

    /** Close the listening sheet, landing on whatever it was covering. */
    object CloseListeningSheet : BackTarget()

    /** Pop one pushed L1 destination. */
    data class PopTo(val label: String) : BackTarget()

    /** Nothing left to remove. */
    object LeaveApp : BackTarget()
}

fun BaseLayerState.backTarget(): BackTarget = when {
    overlays.isNotEmpty() -> BackTarget.DismissOverlay
    sheet == BaseSheet.Queue && queueReturnsToPlayer -> BackTarget.CollapseQueueToPlayer
    sheet == BaseSheet.Queue -> BackTarget.CloseListeningSheet
    sheet == BaseSheet.Player -> BackTarget.CloseListeningSheet
    pushed.size > 1 -> BackTarget.PopTo(labelOf(pushed[pushed.size - 2]))
    pushed.size == 1 -> BackTarget.PopTo(destination.label)
    else -> BackTarget.LeaveApp
}

private fun labelOf(push: BasePush): String = when (push) {
    is BasePush.Collection -> push.kindKey.replaceFirstChar { it.uppercase() }
    is BasePush.TrackDetail -> "Track"
    BasePush.Acquire -> "Add music"
    BasePush.Appearance -> "Appearance"
    BasePush.Technical -> "Connection tools"
    BasePush.SonicTimeline -> "Sonic timeline"
}

fun reduceBaseLayer(state: BaseLayerState, intent: BaseIntent): BaseLayerState = when (intent) {

    /**
     * Choosing a primary destination is a fresh start, not a push: L1 history and any
     * overlay go with it. The listening sheet does not, because it is not part of the
     * destination's stack — it rides above and survives moving underneath it.
     */
    is BaseIntent.SelectDestination -> state.copy(
        destination = intent.destination,
        pushed = emptyList(),
        overlays = emptyList(),
        rearrangingCrate = null
    )

    is BaseIntent.Push -> state.copy(
        pushed = (state.pushed + intent.push).takeLast(BASE_MAX_PUSH_DEPTH),
        overlays = emptyList(),
        rearrangingCrate = null
    )

    is BaseIntent.OpenSheet -> state.copy(
        sheet = intent.sheet,
        queueReturnsToPlayer = if (intent.sheet == BaseSheet.Queue) {
            if (state.sheet == BaseSheet.Queue) state.queueReturnsToPlayer else state.sheet == BaseSheet.Player
        } else true,
        overlays = emptyList(),
        rearrangingCrate = null
    )

    is BaseIntent.ShowOverlay -> state.copy(overlays = state.overlays + intent.overlay)

    is BaseIntent.Rearrange -> state.copy(rearrangingCrate = intent.crateId)

    BaseIntent.Back -> when {
        state.overlays.isNotEmpty() -> state.copy(
            overlays = state.overlays.dropLast(1),
            rearrangingCrate = null
        )
        state.sheet == BaseSheet.Queue -> state.copy(
            sheet = if (state.queueReturnsToPlayer) BaseSheet.Player else null,
            queueReturnsToPlayer = true
        )
        state.sheet == BaseSheet.Player -> state.copy(sheet = null)
        state.pushed.isNotEmpty() -> state.copy(pushed = state.pushed.dropLast(1))
        else -> state
    }
}

/** True when Back has nothing left to remove and the press belongs to the system. */
fun BaseLayerState.backLeavesTheApp(): Boolean = backTarget() == BackTarget.LeaveApp
