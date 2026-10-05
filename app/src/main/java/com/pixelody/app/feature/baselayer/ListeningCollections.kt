package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.CrateBook
import com.pixelody.app.data.model.CrateSlot

/** A music-facing view of saved organizers. The original slots and tools stay intact. */
internal fun listeningCollections(collections: List<BaseCollection>, book: CrateBook): List<BaseCollection> {
    val originals = collections.filterNot { it.id.startsWith("crate:") }
    val byId = originals.associateBy { it.id }
    val crates = book.crates.associateBy { "crate:${it.id}" }
    fun tracks(id: String, visited: Set<String>): List<String> {
        if (id in visited) return emptyList()
        val crate = crates[id]
        if (crate == null) {
            val collection = byId[id] ?: originals.firstOrNull {
                it.id.substringAfter(":").equals(id.substringAfter(":"), ignoreCase = true)
            }
            return collection?.trackIds.orEmpty()
        }
        return crate.slots.flatMap { slot ->
            when (slot) {
                is CrateSlot.SingleTrack -> listOf(slot.trackId)
                is CrateSlot.Collection -> tracks(slot.collectionId, visited + id)
                else -> emptyList()
            }
        }.distinct()
    }
    return originals + book.crates.map { crate ->
        val id = "crate:${crate.id}"
        BaseCollection(id, BaseBrowseShape.Playlists.kindKey, crate.name, tracks(id, emptySet()))
    }
}
