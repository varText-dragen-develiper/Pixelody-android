package com.pixelody.app.core.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaSession

/**
 * Which player commands an unskippable mix refuses. Pause, play, volume and stop always
 * work. Skipping and seeking are refused from everyone, Pixelody included, so the only
 * way forward is a song ending. Other apps also can't replace the queue or change
 * shuffle and repeat; Pixelody's own controller still can, because leaving the mix and
 * starting it are how it gets in and out.
 */
object UnskippableLock {
    val SKIP_AND_SEEK = listOf(
        Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_MEDIA_ITEM,
        Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD, Player.COMMAND_SEEK_TO_DEFAULT_POSITION
    )
    val QUEUE_AND_MODES = listOf(
        Player.COMMAND_CHANGE_MEDIA_ITEMS, Player.COMMAND_SET_MEDIA_ITEM,
        Player.COMMAND_SET_SHUFFLE_MODE, Player.COMMAND_SET_REPEAT_MODE
    )

    fun blocks(command: Int, ownApp: Boolean): Boolean =
        command in SKIP_AND_SEEK || (!ownApp && command in QUEUE_AND_MODES)

    fun lockedCommands(ownApp: Boolean): Player.Commands {
        val builder = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
        SKIP_AND_SEEK.forEach { builder.remove(it) }
        if (!ownApp) QUEUE_AND_MODES.forEach { builder.remove(it) }
        return builder.build()
    }
}
