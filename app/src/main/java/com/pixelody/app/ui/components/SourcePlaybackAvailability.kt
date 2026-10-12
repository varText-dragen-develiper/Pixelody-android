package com.pixelody.app.ui.components

import com.pixelody.app.data.model.Track

/** Browsing uses the same offline ownership rule as the playback host. */
internal fun isTrackPlayableFromSources(
    track: Track,
    hostConnected: Boolean,
    localTrackIds: Set<String>,
    cachedTrackIds: Set<String>
): Boolean = !track.missing && track.streamUrl.isNotBlank() &&
    (hostConnected || track.id in localTrackIds || track.id in cachedTrackIds)
