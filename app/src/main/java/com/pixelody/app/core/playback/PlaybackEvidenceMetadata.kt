package com.pixelody.app.core.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import java.util.UUID

/** Private local metadata. These keys never belong in a public host snapshot. */
object PlaybackEvidenceMetadata {
    const val ENTRY_ID = "pixelody.entryId"
    const val SESSION_ID = "pixelody.sessionId"
    const val SCOPE_ID = "pixelody.scopeId"
    const val DURATION_MS = "pixelody.listening.durationMs"
    const val FORMAT = "pixelody.listening.format"
    const val LOSSLESS = "pixelody.listening.lossless"
    const val PERSONAL = "pixelody.listening.personal"
    const val MANUAL_INTENT = "pixelody.manualIntent"
    const val POLICY_ENTRY = "pixelody.policyEntry"
    const val MODE = "pixelody.policyMode"
    const val DECISION = "pixelody.decision"
    const val ATTEMPT_ID = "pixelody.attemptId"
    /** On the Up next item: the song a skip would play instead. */
    const val LIVE_NEXT = "pixelody.liveNext"
    /** On items sent with Add to session: join the shuffle rather than queue next. */
    const val ADD_TO_SESSION = "pixelody.addToSession"
    const val COMMAND_MODE = "pixelody.shuffle.mode"
    const val COMMAND_POLICY = "pixelody.shuffle.control"

    /** Catalog mediaId stays stable; each deliberate queue insertion gets its own entry. */
    fun withEntry(item: MediaItem, entryId: String = UUID.randomUUID().toString()): MediaItem {
        val extras = Bundle(item.mediaMetadata.extras ?: Bundle())
        extras.putString(ENTRY_ID, entryId)
        return item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(extras).build()).build()
    }

    fun entryId(item: MediaItem?): String? = item?.mediaMetadata?.extras?.getString(ENTRY_ID)
}
