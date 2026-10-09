package com.pixelody.app.core.playback

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.LibrarySnapshot
import java.net.URI

data class PlaybackFailure(
    val message: String,
    val clearCredential: Boolean = false,
    val connectionState: HostConnectionState? = null
)

fun rebaseRemoteMediaUrl(url: String?, baseUrl: String): String? {
    if (url.isNullOrBlank()) return null
    val trimmedUrl = url.trim()
    if (trimmedUrl == "null" || trimmedUrl == "/null" || trimmedUrl.endsWith("/null")) return null
    if (trimmedUrl.startsWith("content://") || trimmedUrl.startsWith("file://")) {
        return trimmedUrl
    }
    if (baseUrl.isBlank()) return trimmedUrl
    return runCatching {
        val base = URI(baseUrl.trim())
        if (base.scheme !in setOf("http", "https")) {
            return@runCatching trimmedUrl
        }
        val source = URI(trimmedUrl)
        if (source.scheme == null) {
            val basePrefix = baseUrl.trim().trimEnd('/')
            val path = if (trimmedUrl.startsWith("/")) trimmedUrl else "/$trimmedUrl"
            if (path == "/null" || path.isBlank()) null else "$basePrefix$path"
        } else if (source.scheme in setOf("http", "https")) {
            val path = source.rawPath.orEmpty()
            if (path == "/null" || path.isBlank()) null
            else URI(base.scheme, base.rawAuthority, source.rawPath, source.rawQuery, source.rawFragment).toASCIIString()
        } else {
            trimmedUrl
        }
    }.getOrDefault(trimmedUrl)
}

fun LibrarySnapshot.withRebasedMediaOrigin(): LibrarySnapshot = copy(
    tracks = tracks.map { track ->
        track.copy(
            streamUrl = rebaseRemoteMediaUrl(track.streamUrl, host.baseUrl).orEmpty(),
            artworkUrl = rebaseRemoteMediaUrl(track.artworkUrl, host.baseUrl)
        )
    },
    playlists = playlists.map { playlist ->
        playlist.copy(artworkUrl = rebaseRemoteMediaUrl(playlist.artworkUrl, host.baseUrl))
    }
)

fun mediaMimeType(format: String): String? {
    val trimmed = format.trim()
    if (trimmed.contains("/")) return trimmed.lowercase()
    return when (trimmed.uppercase()) {
        "FLAC" -> "audio/flac"
        "WAV", "WAVE" -> "audio/wav"
        "MP3", "MPEG" -> "audio/mpeg"
        "M4A", "MP4" -> "audio/mp4"
        "AAC" -> "audio/aac"
        "OGG", "OPUS" -> "audio/ogg"
        "AIFF", "AIF" -> "audio/aiff"
        "ALAC" -> "audio/alac"
        "DSF", "DSD" -> "audio/x-dsd"
        else -> null
    }
}

fun playbackFailure(error: PlaybackException): PlaybackFailure {
    val response = generateSequence<Throwable>(error) { it.cause }
        .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
        .firstOrNull()
    val responseCode = response?.responseCode
    val responseBody = response?.responseBody?.toString(Charsets.UTF_8).orEmpty()
    val authCode = Regex("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(responseBody)?.groupValues?.getOrNull(1)
    return classifyPlaybackFailure(responseCode, authCode, error.errorCode, error.errorCodeName)
}

fun classifyPlaybackFailure(
    responseCode: Int?,
    authCode: String?,
    mediaErrorCode: Int,
    mediaErrorName: String
): PlaybackFailure = when {
    authCode == "auth_revoked" -> PlaybackFailure(
        "The host owner revoked this device. The saved credential was cleared; pair again to play media.",
        clearCredential = true,
        connectionState = HostConnectionState.Revoked
    )
    authCode == "auth_expired" -> PlaybackFailure(
        "The trusted-device credential expired. The saved credential was cleared; pair again.",
        clearCredential = true,
        connectionState = HostConnectionState.CredentialExpired
    )
    responseCode == 401 -> PlaybackFailure(
        "The host rejected media access. The saved credential was cleared; pair again.",
        clearCredential = true,
        connectionState = HostConnectionState.AuthFailed
    )
    responseCode == 403 -> PlaybackFailure("This trusted device does not have permission to stream media.")
    responseCode == 404 -> PlaybackFailure("The media file is missing on the host. Refresh the library or choose another track.")
    responseCode == 416 -> PlaybackFailure("The host rejected the seek range. Restart the track and try again.")
    mediaErrorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
        mediaErrorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        PlaybackFailure("Desktop connection was lost.", connectionState = HostConnectionState.HostUnavailable)
    else -> PlaybackFailure("Playback failed ($mediaErrorName). Retry the track or refresh the host connection.")
}
