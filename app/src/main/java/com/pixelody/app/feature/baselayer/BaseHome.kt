package com.pixelody.app.feature.baselayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.model.Crate
import com.pixelody.app.data.model.CrateSlot
import com.pixelody.app.ui.components.CrateFace
import com.pixelody.app.ui.components.CrateSlotFace

/**
 * Home is what *you* arranged. Library is everything, arranged by the machine, and
 * Search is one known thing right now. Before crates, Home had no job Library did not
 * do better — it was a suggestions feed competing with the place suggestions come
 * from.
 *
 * The crate grid is three across, which at 384dp gives roughly 112dp tiles: big
 * enough to recognise by shape, small enough that nine crates fit above the fold.
 */
@Composable
fun BaseHomeContent(
    data: BaseLayerData,
    onOpenCrate: (String) -> Unit,
    onCrateActions: (String) -> Unit,
    onPlayFrom: (List<String>) -> Unit,
    onTrackActions: (String) -> Unit,
    onToggleFavorite: ((String) -> Unit)? = null,
    onRetryHost: () -> Unit,
    onPhoneOnly: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crates = data.crates.crates
    val recent = data.visibleTracks().take(6).map { it.id }

    LazyColumn(modifier = modifier.fillMaxWidth()) {

        if (!data.hostReachable) {
            item {
                BaseFaultStrip(
                    onRetry = onRetryHost,
                    onPhoneOnly = onPhoneOnly,
                    onOpenTools = onOpenTools
                )
            }
        }

        item { BaseSectionHeader("Your crates") }

        if (crates.isEmpty()) {
            item {
                BaseEmptyNote(
                    "No crates yet. Long press anything — a playlist, an album, a track — and choose Add to a crate."
                )
            }
        }

        items(crateRows(crates)) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { crate ->
                    CrateFace(
                        crate = crate,
                        resolve = { slot -> data.resolveCrateSlot(slot) },
                        onOpen = { onOpenCrate(crate.id) },
                        onActions = { onCrateActions(crate.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(CRATES_PER_ROW - row.size) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }

        item { BaseSectionHeader("Recent") }

        itemsIndexed(recent, key = { _, id -> id }) { index, id ->
            BaseTrackRow(
                data = data,
                trackId = id,
                onPlay = { onPlayFrom(recent.drop(index)) },
                onActions = { onTrackActions(id) },
                onToggleFavorite = onToggleFavorite?.let { cb -> { cb(id) } }
            )
        }
    }
}

const val CRATES_PER_ROW = 3

private fun crateRows(crates: List<Crate>): List<List<Crate>> = crates.chunked(CRATES_PER_ROW)

@Composable
fun BaseSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * A crate stores addresses, not copies, so what a slot shows is looked up every time
 * it is drawn. Rename a playlist and the crate says the new name without being
 * touched; delete it and the slot resolves unavailable and keeps its position rather
 * than vanishing and letting the next thing added claim it.
 */
fun BaseLayerData.resolveCrateSlot(slot: CrateSlot): CrateSlotFace? = when (slot) {
    is CrateSlot.Collection -> {
        val collection = collection(slot.collectionId)
        if (collection == null) {
            CrateSlotFace(
                label = "Gone",
                badge = "·",
                kindMark = slot.kindKey,
                available = false
            )
        } else {
            CrateSlotFace(
                label = collection.name,
                badge = initialsOf(collection.name),
                kindMark = slot.kindKey,
                available = playableTrackIds(collection.trackIds).isNotEmpty()
            )
        }
    }

    is CrateSlot.SingleTrack -> {
        val found = track(slot.trackId)
        CrateSlotFace(
            label = found?.title ?: "Gone",
            badge = initialsOf(found?.album?.ifBlank { found.title } ?: "·"),
            kindMark = "track",
            available = found != null && isPlayable(slot.trackId)
        )
    }

    is CrateSlot.PlayerView -> CrateSlotFace(
        label = slot.viewName,
        badge = slot.viewName.take(2).uppercase(),
        kindMark = "view"
    )

    is CrateSlot.Lens -> CrateSlotFace(
        label = slot.label,
        badge = "L",
        kindMark = "lens"
    )

    is CrateSlot.SessionPreset -> CrateSlotFace(
        label = slot.label,
        badge = "S",
        kindMark = "session"
    )

    is CrateSlot.Unknown -> CrateSlotFace(
        label = "From a newer version",
        badge = "?",
        kindMark = "unknown",
        available = false
    )
}

/** Everything a crate can start, in slot order, skipping what is out of reach. */
fun BaseLayerData.playableInCrate(crate: Crate): List<String> {
    val ids = mutableListOf<String>()
    crate.slots.filterNotNull().forEach { slot ->
        when (slot) {
            is CrateSlot.Collection -> collection(slot.collectionId)?.let { ids.addAll(it.trackIds) }
            is CrateSlot.SingleTrack -> ids.add(slot.trackId)
            else -> Unit
        }
    }
    return playableTrackIds(ids.distinct())
}
