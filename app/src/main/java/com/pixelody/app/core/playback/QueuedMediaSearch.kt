package com.pixelody.app.core.playback

import java.util.Locale

internal data class QueuedMediaSearchText(
    val title: String,
    val artist: String,
    val album: String,
)

/** Search only the queue already owned by the playback service, in queue order. */
internal fun queuedMediaSearchIndexes(query: String, items: List<QueuedMediaSearchText>): List<Int> {
    val terms = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter(String::isNotEmpty)
    return items.indices.filter { index ->
        val item = items[index]
        val text = listOf(item.title, item.artist, item.album).joinToString(" ").lowercase(Locale.ROOT)
        terms.all(text::contains)
    }
}
