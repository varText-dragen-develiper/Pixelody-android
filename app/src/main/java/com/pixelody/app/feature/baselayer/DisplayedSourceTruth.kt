package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.HostProfile
import com.pixelody.app.data.model.SavedHostProfile

internal fun displayedHostProfile(saved: SavedHostProfile?, state: HostConnectionState) = HostProfile(
    hostId = saved?.hostId.orEmpty(),
    hostName = saved?.hostName ?: "Phone library",
    baseUrl = saved?.baseUrl.orEmpty(),
    platform = saved?.platform ?: "Android",
    roles = saved?.roles ?: listOf("playbackDevice"),
    connectionState = state
)

internal fun BaseLayerData.desktopTracks() = tracks.filter { it.id in hostTrackIds }
