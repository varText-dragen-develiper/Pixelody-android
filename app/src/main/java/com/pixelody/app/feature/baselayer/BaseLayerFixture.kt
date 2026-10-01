package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.CrateStarters
import com.pixelody.app.data.model.Playlist
import com.pixelody.app.data.model.Track

/**
 * Content for the base layer before any feature is ported into it.
 *
 * This exists so the frame can be walked by thumb on a real device today, which is
 * where the two open questions about it get answered — whether long press carrying
 * both object actions and session settings holds up, and whether a crate reads as a
 * different thing from a playlist. Neither is answerable in a browser.
 *
 * It is a fixture and it says so on screen. Delete this file when `BaseLayerData` is
 * built from the repository instead.
 */
private fun fixtureTrack(
    id: String,
    title: String,
    artist: String,
    album: String,
    lossless: Boolean = false,
    favorite: Boolean = false,
    genre: String = ""
) = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationSeconds = 214,
    lossless = lossless,
    favorite = favorite,
    genre = genre
)

fun sampleBaseLayerData(): BaseLayerData {
    val tracks = listOf(
        fixtureTrack("t1", "Saline", "Held Pattern", "Lowlight", lossless = true, favorite = true, genre = "Ambient"),
        fixtureTrack("t2", "Lo-Fi Meditation", "Nakura", "Tape Room", genre = "Lo-Fi Hip Hop"),
        fixtureTrack("t3", "Copper Wire", "Held Pattern", "Lowlight", lossless = true, genre = "Ambient"),
        fixtureTrack("t4", "Night Bus", "Vega Sorrel", "Long Way Round", favorite = true, genre = "Indie Rock"),
        fixtureTrack("t5", "Second Shift", "Mordant Fields", "Iron Season", genre = "Industrial Metal"),
        fixtureTrack("t6", "Pale Transit", "Vega Sorrel", "Long Way Round", lossless = true, genre = "Indie Rock"),
        fixtureTrack("t7", "Anvil Chorus", "Mordant Fields", "Iron Season", favorite = true, genre = "Industrial Metal"),
        fixtureTrack("t8", "Dust on the Lens", "Nakura", "Tape Room", genre = "Lo-Fi Hip Hop"),
        fixtureTrack("t9", "Quiet Hours", "Held Pattern", "Lowlight", lossless = true, genre = "Ambient"),
        fixtureTrack("t10", "Foundry", "Mordant Fields", "Iron Season", genre = "Industrial Metal"),
        fixtureTrack("t11", "Riverbed", "Nakura", "Tape Room", favorite = true, genre = "Lo-Fi Hip Hop"),
        fixtureTrack("t12", "Stray Current", "Vega Sorrel", "Long Way Round", lossless = true, genre = "Indie Rock")
    )

    val playlists = listOf(
        Playlist("p1", "Metal", listOf("t5", "t7", "t10", "t3"), null),
        Playlist("p2", "Late Drive", listOf("t4", "t6", "t12", "t1", "t9"), null),
        Playlist("p3", "Focus", listOf("t2", "t11", "t9"), null)
    )

    val collections = buildBaseCollections(tracks, playlists)
    val albumIds = collections.filter { it.kindKey == BaseBrowseShape.Albums.kindKey }.map { it.id }

    return BaseLayerData(
        tracks = tracks,
        collections = collections,
        crates = CrateStarters.from(playlists, albumIds),
        phoneTrackIds = setOf("t2", "t4", "t6", "t11"),
        hostTrackIds = setOf("t1", "t3", "t5", "t7", "t9", "t10"),
        jamTrackIds = setOf("t8", "t12"),
        downloadedTrackIds = setOf("t1", "t2", "t4", "t9", "t11"),
        currentTrackId = null,
        lastTrackId = "t1",
        isPlaying = false,
        queue = listOf("t3", "t9", "t5")
    )
}
