package com.pixelody.app.core.playback

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import java.net.URI

internal fun isRemoteHostMedia(uri: String?): Boolean =
    runCatching { URI(uri.orEmpty()).scheme?.lowercase() in setOf("http", "https") }.getOrDefault(false)

/** Remove network items in reverse index order, preserving a currently playing local item. */
internal fun remoteMediaIndices(uris: List<String>): List<Int> =
    uris.indices.filter { isRemoteHostMedia(uris[it]) }.reversed()

@OptIn(UnstableApi::class)
internal fun removeRemoteHostMedia(player: Player) {
    if (isRemoteHostMedia(player.currentMediaItem?.localConfiguration?.uri?.toString())) {
        player.pause()
        player.stop()
    }
    val indices = remoteMediaIndices(List(player.mediaItemCount) {
        player.getMediaItemAt(it).localConfiguration?.uri?.toString().orEmpty()
    })
    indices.forEach(player::removeMediaItem)
}
