package com.pixelody.app

import com.pixelody.app.core.playback.ShuffleLanes
import com.pixelody.app.core.shuffle.UniversalShufflePolicy as Policy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleLanesTest {
    @Test fun queueCuesDoNotPrintInternalPolicyKeys() {
        val cue = ShuffleLanes.queueCue(JSONObject().put("kind", "manual")
            .put("strategy", "internal-policy-key").put("intentionalDuplicate", true)
            .put("relaxations", JSONArray().put("artist-window-relaxed")).toString())!!
        assertEquals("Your queue choice", cue.badge)
        assertTrue(cue.description.contains("You added this track again."))
        assertTrue(!cue.description.contains("internal-policy-key"))
        assertTrue(!cue.description.contains("artist-window-relaxed"))
        assertNull(ShuffleLanes.queueCue("not json"))
    }

    private fun entry(id: String, artist: String, source: String) = JSONObject().put("entryId", id).put("trackId", id)
        .put("artist", artist).put("album", "$artist album").put("available", true).put("sourceKey", source)

    private fun session(entries: List<JSONObject>) = Policy.createSession(JSONObject().put("sessionId", "s").put("scopeId", "playlist:road")
        .put("authorityId", "local").put("authorityEpoch", 0).put("seed", "seed").put("mode", "balanced").put("repeat", "off")
        .put("entries", JSONArray(entries)))

    private fun play(state: JSONObject, request: JSONObject): JSONObject {
        val staged = Policy.stage(state, request.put("attemptId", "a-${state.getLong("revision")}").put("advanceId", "v-${state.getLong("revision")}"))
        val pending = staged.getJSONObject("pending")
        return Policy.commit(staged, JSONObject(pending.toString()).put("positionMs", 0))
    }

    @Test fun whyLineMatchesWindowsWording() {
        assertEquals("You skipped Song A straight away, so this is a wider pick.",
            ShuffleLanes.whyLine(JSONObject().put("kind", "automatic").put("lane", "live").put("skip", "flick").toString(), "Song A"))
        assertEquals("the last song played through, so this keeps going the same way.",
            ShuffleLanes.whyLine(JSONObject().put("kind", "automatic").put("lane", "base").put("skip", "finish").toString(), null))
        assertEquals("You queued this one next.", ShuffleLanes.whyLine(JSONObject().put("kind", "manual").toString(), "Song A"))
        assertNull(ShuffleLanes.whyLine(JSONObject().put("kind", "previous").put("lane", "base").toString(), "Song A"))
        assertNull(ShuffleLanes.whyLine(null, "Song A"))
        assertNull(ShuffleLanes.whyLine("not json", "Song A"))
    }

    @Test fun listNamesReadLikeTheLibrary() {
        val playlists = listOf("road" to "Road trip")
        assertEquals("Road trip", ShuffleLanes.listName("playlist:road", playlists))
        assertEquals("Library", ShuffleLanes.listName("local-library", playlists))
        assertEquals("Puberty 2", ShuffleLanes.listName("album:album:mitski:puberty 2", playlists, "Puberty 2"))
        assertEquals("puberty 2", ShuffleLanes.listName("album:album:mitski:puberty 2", playlists))
        assertEquals("Mitski", ShuffleLanes.listName("artist:Mitski", playlists))
        assertEquals("This queue", ShuffleLanes.listName("4b1c-uuid", playlists))
    }

    // The Android authority links a list by reconciling its entries into the
    // session and shows the live preview worked out after the pending song commits.
    @Test fun aLinkedListJoinsTheShuffleAndTheSkipPreviewFollowsTheCurrentSong() {
        var state = session(listOf(entry("a1", "Ana", "playlist:road"), entry("a2", "Bo", "playlist:road"), entry("a3", "Cy", "playlist:road")))
        state = play(state, JSONObject().put("entryId", "a1"))
        val existing = (0 until state.getJSONArray("entries").length()).map { JSONObject(state.getJSONArray("entries").getJSONObject(it).toString()) }
        state = Policy.reconcile(state, JSONArray(existing + listOf(entry("b1", "Dee", "album:x"), entry("b2", "Eli", "album:x"))))
        val remaining = (0 until state.getJSONArray("remaining").length()).map { state.getJSONArray("remaining").getString(it) }
        assertTrue(remaining.containsAll(listOf("b1", "b2")))
        assertEquals("a1", Policy.currentOccurrence(state)!!.getString("entryId"))

        val staged = Policy.stage(state, JSONObject().put("attemptId", "n").put("advanceId", "n").put("naturalEnd", true))
        val pending = staged.getJSONObject("pending")
        val settled = Policy.commit(staged, JSONObject(pending.toString()).put("positionMs", 0))
        assertEquals(pending.getString("entryId"), Policy.currentOccurrence(settled)!!.getString("entryId"))
        val live = Policy.preview(settled, JSONObject().put("lane", "live"))
        assertNotNull(live)
        assertTrue(live!!.getString("trackId") != pending.getString("trackId"))
        assertEquals("live", live.getString("lane"))
    }
}
