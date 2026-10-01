package com.pixelody.app.feature.baselayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pixelody.app.ui.navigation.PixelodyStateTags

/**
 * Search: one known thing, right now.
 *
 * The matching lives in [baseSearchResults] as a pure function rather than inside the
 * composable, because what counts as a match is a product decision people will argue
 * about — and an argument you can settle with a test is worth more than one you settle
 * by squinting at a screen.
 *
 * Two rules it follows that are easy to get wrong:
 *
 * - **The lens applies.** If someone has narrowed to Downloaded, searching does not
 *   quietly hand back host tracks they cannot play. The same lens serves Library and
 *   Search, which is the point of there being one lens.
 * - **Unplayable results still appear.** A host track with the host down matches, is
 *   listed, and says it is out of reach. Hiding it would make the library look like it
 *   shrank, which is the failure J06 exists to prevent.
 */
fun baseSearchResults(data: BaseLayerData, rawQuery: String): List<String> {
    val query = rawQuery.trim().lowercase()
    if (query.isEmpty()) return emptyList()

    val terms = query.split(Regex("\\s+")).filter { it.isNotBlank() }

    return data.visibleTracks()
        .filter { track ->
            val haystack = listOf(track.title, track.artist, track.album)
                .joinToString(" ")
                .lowercase()
            terms.all { haystack.contains(it) }
        }
        .sortedWith(
            compareByDescending<com.pixelody.app.data.model.Track> { track ->
                // A title that starts with what was typed is almost always the one meant.
                if (track.title.lowercase().startsWith(query)) 2
                else if (track.title.lowercase().contains(query)) 1
                else 0
            }.thenBy { it.title.lowercase() }
        )
        .map { it.id }
}

/**
 * What Search offers before anything is typed. Not a recommendation engine — the
 * queue and the last thing played, which are the two things a person is most likely
 * to be reaching back for.
 */
fun baseSearchStartingPoints(data: BaseLayerData): List<String> =
    (listOfNotNull(data.currentTrackId, data.lastTrackId) + data.queue)
        .distinct()
        .filter { data.matchesLens(it) }
        .take(5)

@Composable
fun BaseSearchContent(
    data: BaseLayerData,
    query: String,
    onQueryChange: (String) -> Unit,
    onPlayFrom: (List<String>) -> Unit,
    onTrackActions: (String) -> Unit,
    onToggleFavorite: ((String) -> Unit)? = null,
    onRetryHost: () -> Unit,
    onPhoneOnly: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val results = baseSearchResults(data, query)
    val typing = query.isNotBlank()

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

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PixelodyStateTags.SEARCH_QUERY_FIELD),
                    label = { Text("Search music") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }
        }

        when {
            !typing -> {
                val starting = baseSearchStartingPoints(data)
                if (starting.isEmpty()) {
                    item { BaseEmptyNote("Type a title, an artist, or an album.") }
                } else {
                    item { BaseSectionHeader("Pick up where you were") }
                    itemsIndexed(starting, key = { _, id -> id }) { index, id ->
                        BaseTrackRow(
                            data = data,
                            trackId = id,
                            onPlay = { onPlayFrom(starting.drop(index)) },
                            onActions = { onTrackActions(id) },
                            onToggleFavorite = onToggleFavorite?.let { cb -> { cb(id) } }
                        )
                    }
                }
            }

            results.isEmpty() -> item {
                BaseEmptyNote(
                    if (data.lens == BaseLens.Everything && data.source == BaseSource.All) {
                        "Nothing matches \"$query\"."
                    } else {
                        "Nothing matches \"$query\" in ${data.lensLabel}. Widen the lens to look everywhere."
                    }
                )
            }

            else -> {
                item {
                    BaseSectionHeader(
                        if (results.size == 1) "1 result" else "${results.size} results"
                    )
                }
                itemsIndexed(results, key = { _, id -> id }) { index, id ->
                    BaseTrackRow(
                        data = data,
                        trackId = id,
                        onPlay = { onPlayFrom(results.drop(index)) },
                        onActions = { onTrackActions(id) },
                        onToggleFavorite = onToggleFavorite?.let { cb -> { cb(id) } }
                    )
                }
            }
        }
    }
}

@Composable
fun BaseSearchHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
