package com.pixelody.app.data.network

import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.DeviceRefreshInstruction
import com.pixelody.app.data.model.HostCapabilities
import com.pixelody.app.data.model.HostProfile
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.LivePollResult
import com.pixelody.app.data.model.LiveState
import com.pixelody.app.data.model.JamDiagnostics
import com.pixelody.app.data.model.FlowShufflePlan
import com.pixelody.app.data.model.FlowShuffleSession
import com.pixelody.app.data.model.FlowShuffleSnapshot
import com.pixelody.app.data.model.JamGuestPolicy
import com.pixelody.app.data.model.JamParticipant
import com.pixelody.app.data.model.JamSession
import com.pixelody.app.data.model.NetworkSession
import com.pixelody.app.data.model.NetworkSessionDiagnostics
import com.pixelody.app.data.model.NetworkSessionHost
import com.pixelody.app.data.model.NetworkSessionPlayback
import com.pixelody.app.data.model.NetworkSessionQueue
import com.pixelody.app.data.model.NetworkSessionSync
import com.pixelody.app.data.model.NetworkTrackSummary
import com.pixelody.app.data.model.Playlist
import com.pixelody.app.data.model.QueueItem
import com.pixelody.app.data.model.QueueSnapshot
import com.pixelody.app.data.model.RepeatMode
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.model.TrackPage
import com.pixelody.app.data.model.TrustedDevice
import com.pixelody.app.data.model.SyncRevisions
import java.io.BufferedReader
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

data class HostConnectionResult(
    val snapshot: LibrarySnapshot,
    val token: String,
    val capabilities: HostCapabilities,
    val grantedPermissions: List<String> = emptyList()
)

data class PairingCredential(
    val token: String,
    val permissions: List<String>
)

data class JamJoinResult(
    val accepted: Boolean,
    val approvalRequired: Boolean,
    val participant: JamParticipant?
)

class HostApiException(
    val statusCode: Int,
    val code: String,
    message: String
) : IllegalStateException(message)

class HostNetworkException(
    val code: String,
    val connectionState: HostConnectionState,
    message: String,
    cause: Throwable
) : IllegalStateException(message, cause)

data class DeviceRefreshResult(
    val accepted: Boolean,
    val refresh: DeviceRefreshInstruction?
)

class PixelodyHostApiClient {
    fun completePairing(
        baseUrl: String,
        pairingCode: String,
        secret: String,
        deviceName: String
    ): PairingCredential {
        val normalizedBaseUrl = normalizedBaseUrl(baseUrl)
        val body = JSONObject()
            .put("pairingCode", pairingCode.trim())
            .put("secret", secret.trim())
            .put("deviceName", deviceName.ifBlank { "Pixelody Android" })
        val response = postJson(
            targetUrl = "$normalizedBaseUrl/api/v1/pair/complete",
            body = body
        )
        val token = response.optString("token").trim()
        require(token.isNotBlank()) { "Pairing completed without a trusted-device token." }
        return PairingCredential(
            token = token,
            permissions = response.optJSONObject("device")?.optJSONArray("permissions").toStringList()
        )
    }

    fun fetchHostCapabilities(baseUrl: String, token: String): HostCapabilities {
        val normalizedBaseUrl = normalizedBaseUrl(baseUrl)
        require(token.trim().isNotEmpty()) { "Token is required" }
        return getJson(
            targetUrl = "$normalizedBaseUrl/api/v1/host/capabilities",
            token = token.trim()
        ).toHostCapabilities()
    }

    fun fetchLibrarySnapshot(baseUrl: String, token: String): LibrarySnapshot {
        return connectToHost(baseUrl = baseUrl, token = token).snapshot
    }

    fun connectToHost(baseUrl: String, token: String): HostConnectionResult {
        val normalizedBaseUrl = normalizedBaseUrl(baseUrl)
        require(token.trim().isNotEmpty()) { "Token is required" }

        val serverInfo = getJson("$normalizedBaseUrl/api/v1/server-info")
        val capabilities = fetchHostCapabilities(normalizedBaseUrl, token.trim())
        val snapshot = getJson(
            targetUrl = "$normalizedBaseUrl/api/v1/library/snapshot?includeTracks=false",
            token = token.trim()
        )
        val tracks = fetchAllTrackPages(
            baseUrl = normalizedBaseUrl,
            token = token.trim(),
            pageSize = capabilities.maxPageSize.coerceIn(1, 250),
            expectedRevision = snapshot.optLong("revision")
        )

        val library = snapshot.toLibrarySnapshot(
            baseUrl = normalizedBaseUrl,
            token = token.trim(),
            hostId = serverInfo.optString("hostId"),
            platform = serverInfo.optString("platform"),
            fallbackHostName = serverInfo.optString("hostName", "Pixelody Host"),
            capabilities = capabilities,
            tracksOverride = tracks
        )
        return HostConnectionResult(
            snapshot = library,
            token = token.trim(),
            capabilities = capabilities
        )
    }

    fun fetchLiveState(baseUrl: String, token: String, sinceRevision: Long = -1): LivePollResult {
        val normalizedBaseUrl = normalizedBaseUrl(baseUrl)
        require(token.trim().isNotEmpty()) { "Token is required" }
        val suffix = if (sinceRevision >= 0) "?sinceRevision=$sinceRevision" else ""
        val response = getJson(
            targetUrl = "$normalizedBaseUrl/api/v1/live$suffix",
            token = token.trim()
        )
        val unchanged = response.optBoolean("unchanged")
        return LivePollResult(
            unchanged = unchanged,
            revision = response.optLong("revision"),
            pollAfterMs = response.optLong("pollAfterMs", 1000L),
            state = if (unchanged) null else response.toLiveState()
        )
    }

    fun fetchTrackPage(
        baseUrl: String,
        token: String,
        offset: Int = 0,
        limit: Int = 100,
        query: String = "",
        playlistId: String = ""
    ): TrackPage {
        val normalizedBaseUrl = normalizedBaseUrl(baseUrl)
        val parameters = mutableListOf(
            "offset=${offset.coerceAtLeast(0)}",
            "limit=${limit.coerceIn(1, 250)}"
        )
        if (query.isNotBlank()) parameters += "q=${URLEncoder.encode(query.trim(), "UTF-8")}" 
        if (playlistId.isNotBlank()) parameters += "playlistId=${URLEncoder.encode(playlistId.trim(), "UTF-8")}" 
        val response = getJson(
            targetUrl = "$normalizedBaseUrl/api/v1/library/tracks?${parameters.joinToString("&")}",
            token = token.trim()
        )
        val page = response.optJSONObject("page")
        return TrackPage(
            tracks = response.optJSONArray("tracks").toTrackList(normalizedBaseUrl),
            offset = page?.optInt("offset") ?: offset,
            limit = page?.optInt("limit") ?: limit,
            total = page?.optInt("total") ?: 0,
            nextOffset = page?.takeUnless { it.isNull("nextOffset") }?.optInt("nextOffset"),
            revision = response.optLong("revision")
        )
    }

    private fun fetchAllTrackPages(baseUrl: String, token: String, pageSize: Int, expectedRevision: Long): List<Track> {
        val tracks = mutableListOf<Track>()
        var offset = 0
        do {
            val page = fetchTrackPage(baseUrl = baseUrl, token = token, offset = offset, limit = pageSize)
            if (page.revision != expectedRevision) {
                throw HostApiException(409, "library_revision_changed", "The host library changed while it was being paged. Refresh and try again.")
            }
            tracks += page.tracks
            offset = page.nextOffset ?: -1
        } while (offset >= 0)
        return tracks
    }

    fun sendPlaybackCommand(baseUrl: String, token: String, action: String, trackId: String? = null): Boolean {
        val body = JSONObject().put("action", action)
        if (!trackId.isNullOrBlank()) body.put("trackId", trackId)
        val response = postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/commands/playback",
            token = token.trim(),
            body = body
        )
        return response.optBoolean("accepted")
    }

    fun sendQueueCommand(baseUrl: String, token: String, action: String, trackId: String? = null, toIndex: Int? = null): Boolean {
        val body = JSONObject().put("action", action)
        if (!trackId.isNullOrBlank()) body.put("trackId", trackId)
        if (toIndex != null) body.put("toIndex", toIndex)
        val response = postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/commands/queue",
            token = token.trim(),
            body = body
        )
        return response.optBoolean("accepted")
    }

    fun fetchTrustedDevices(baseUrl: String, token: String): List<TrustedDevice> {
        val response = getJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/devices",
            token = token.trim()
        )
        return response.optJSONArray("devices").toTrustedDevices()
    }

    fun revokeTrustedDevice(baseUrl: String, token: String, deviceId: String): List<TrustedDevice> {
        val response = requestJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/devices/${URLEncoder.encode(deviceId, "UTF-8")}",
            method = "DELETE",
            token = token.trim()
        )
        return response.optJSONArray("devices").toTrustedDevices()
    }

    fun requestDeviceRefresh(baseUrl: String, token: String, reason: String): DeviceRefreshResult {
        val body = JSONObject()
            .put("scope", "jam")
            .put("reason", reason.ifBlank { "Android requested a trusted-device soft refresh." })
        val response = postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/devices/refresh",
            token = token.trim(),
            body = body
        )
        return DeviceRefreshResult(
            accepted = response.optBoolean("accepted"),
            refresh = response.optJSONObject("refresh").toDeviceRefreshInstruction()
        )
    }

    fun fetchJamSession(baseUrl: String, token: String): JamSession {
        return getJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/session",
            token = token.trim()
        ).toJamSessionModel()
    }

    fun requestJamJoin(baseUrl: String, token: String, role: String = "guest"): JamJoinResult {
        val response = postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/session/join",
            token = token.trim(),
            body = JSONObject().put("role", if (role == "controller") "controller" else "guest")
        )
        return JamJoinResult(
            accepted = response.optBoolean("accepted"),
            approvalRequired = response.optBoolean("approvalRequired"),
            participant = response.optJSONObject("participant").toJamParticipant()
        )
    }

    fun sendJamQueueCommand(baseUrl: String, token: String, action: String, trackId: String? = null, queueItemId: String? = null, toIndex: Int? = null): JamSession {
        val body = JSONObject().put("action", action)
        if (!trackId.isNullOrBlank()) body.put("trackId", trackId)
        if (!queueItemId.isNullOrBlank()) body.put("queueItemId", queueItemId)
        if (toIndex != null) body.put("toIndex", toIndex)
        return postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/session/queue",
            token = token.trim(),
            body = body
        ).toJamSessionModel()
    }

    fun sendJamPlaybackCommand(baseUrl: String, token: String, action: String): JSONObject {
        return postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/session/playback",
            token = token.trim(),
            body = JSONObject().put("action", action)
        )
    }

    fun updateJamPolicy(baseUrl: String, token: String, policy: JamGuestPolicy): JamSession {
        val body = JSONObject()
            .put("guestsCanView", policy.guestsCanView)
            .put("guestsCanSuggest", policy.guestsCanSuggest)
            .put("guestsCanQueue", policy.guestsCanQueue)
            .put("guestsCanEditQueue", policy.guestsCanEditQueue)
            .put("guestsCanControlPlayback", policy.guestsCanControlPlayback)
            .put("federatedSourcesEnabled", policy.federatedSourcesEnabled)
        return postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/session/policy",
            token = token.trim(),
            body = body
        ).toJamSessionModel()
    }

    fun voteJamTrack(baseUrl: String, token: String, queueItemId: String, vote: Int): JamSession {
        val body = JSONObject()
            .put("queueItemId", queueItemId)
            .put("vote", vote)
        return postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/session/vote",
            token = token.trim(),
            body = body
        ).toJamSessionModel()
    }

    fun promoteJamSuggestion(baseUrl: String, token: String, queueItemId: String): JamSession {
        val body = JSONObject()
            .put("action", "promote")
            .put("queueItemId", queueItemId)
        return postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/session/queue",
            token = token.trim(),
            body = body
        ).toJamSessionModel()
    }

    fun setJamDj(baseUrl: String, token: String, deviceId: String): JamSession {
        val body = JSONObject()
            .put("action", "set_dj")
            .put("deviceId", deviceId)
        return postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/session/participants",
            token = token.trim(),
            body = body
        ).toJamSessionModel()
    }

    fun kickJamParticipant(baseUrl: String, token: String, deviceId: String): JamSession {
        val body = JSONObject()
            .put("action", "kick")
            .put("deviceId", deviceId)
        return postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/session/participants",
            token = token.trim(),
            body = body
        ).toJamSessionModel()
    }

    fun syncJamTime(baseUrl: String, token: String, clientSendMs: Long = System.currentTimeMillis()): com.pixelody.app.data.model.JamClockSyncSample {
        val body = JSONObject().put("clientSendMs", clientSendMs)
        val t0 = System.currentTimeMillis()
        val response = postJson(
            targetUrl = "${normalizedBaseUrl(baseUrl)}/api/v1/jam/sync/time",
            token = token.trim(),
            body = body
        )
        val t3 = System.currentTimeMillis()
        val t1 = response.optLong("serverReceiveMs", response.optLong("serverTimeMs", t0))
        val t2 = response.optLong("serverSendMs", response.optLong("serverTimeMs", t1))
        return com.pixelody.app.data.model.JamClockSyncSample(
            t0ClientSendMs = clientSendMs,
            t1ServerReceiveMs = t1,
            t2ServerSendMs = t2,
            t3ClientReceiveMs = t3
        )
    }

    private fun normalizedBaseUrl(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        require(normalized.startsWith("http://") || normalized.startsWith("https://")) {
            "Host URL must start with http:// or https://"
        }
        return normalized
    }

    private fun postJson(targetUrl: String, token: String = "", body: JSONObject): JSONObject {
        val payload = body.toString().toByteArray(Charsets.UTF_8)
        return requestJson(targetUrl = targetUrl, method = "POST", token = token, body = payload)
    }

    private fun requestJson(
        targetUrl: String,
        method: String,
        token: String = "",
        body: ByteArray? = null
    ): JSONObject {
        return try {
            val connection = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 7000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Content-Length", body.size.toString())
                }
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }
            if (body != null) connection.outputStream.use { it.write(body) }
            readJsonResponse(connection)
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw HostNetworkException("host_timeout", HostConnectionState.HostUnavailable, "Host timed out. Start J.A.M. on the PC, keep both devices on the same Wi-Fi/private network, and allow Pixelody through Windows Firewall.", error)
        } catch (error: ConnectException) {
            throw HostNetworkException("host_unavailable", HostConnectionState.HostUnavailable, "Host refused the connection. Pixelody may not be hosting on that address, or the PC firewall blocked the private-network route.", error)
        } catch (error: NoRouteToHostException) {
            throw HostNetworkException("network_unavailable", HostConnectionState.NetworkUnavailable, "No route to the host. Check that the phone and PC are on the same Wi-Fi/private network, or use a fresh QR invite with the current host address.", error)
        } catch (error: UnknownHostException) {
            throw HostNetworkException("network_unavailable", HostConnectionState.NetworkUnavailable, "Host name could not be resolved. Use the QR invite or an IP address from the Windows J.A.M. drawer.", error)
        } catch (error: IOException) {
            throw HostNetworkException("network_unavailable", HostConnectionState.NetworkUnavailable, "Network request failed. Check Wi-Fi, private-network hosting, firewall permission, and whether the PC is awake.", error)
        }
    }

    private fun getJson(targetUrl: String, token: String = ""): JSONObject {
        return requestJson(targetUrl = targetUrl, method = "GET", token = token)
    }

    private fun readJsonResponse(connection: HttpURLConnection): JSONObject {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
        connection.disconnect()

        if (statusCode !in 200..299) {
            val errorBody = runCatching { JSONObject(body) }.getOrNull()
            val code = errorBody?.optString("code")?.ifBlank { "http_error" } ?: "http_error"
            val message = errorBody?.optString("message")?.ifBlank { null }
                ?: hostApiErrorMessage(statusCode, code)
            throw HostApiException(statusCode = statusCode, code = code, message = message)
        }

        return JSONObject(body)
    }
}

internal fun hostApiErrorMessage(statusCode: Int, code: String): String = when (code) {
    "auth_required" -> "This request needs a trusted-device credential. Pair with the host first."
    "auth_invalid" -> "The saved credential is not valid. Pair again with a fresh invite."
    "auth_expired" -> "This trusted-device credential expired. Ask the host owner for a fresh invite."
    "auth_revoked" -> "The host owner revoked this device. Ask the owner to pair it again."
    "permission_required", "owner_required" -> "This trusted device does not have permission for that action."
    "pairing_rate_limited", "command_rate_limited" -> "The host temporarily limited requests. Wait a moment and try again."
    else -> when (statusCode) {
        401 -> "Host rejected this credential. Pair again with a fresh QR invite."
        403 -> "This trusted device does not have permission for that action."
        404 -> "The host route or media item was not found. Refresh the J.A.M. session and try again."
        429 -> "The host temporarily limited requests. Wait a moment and try again."
        else -> "Host request failed with HTTP $statusCode"
    }
}

internal fun connectionStateFor(error: Throwable): HostConnectionState = when (error) {
    is HostNetworkException -> error.connectionState
    is HostApiException -> when (error.code) {
        "auth_expired" -> HostConnectionState.CredentialExpired
        "auth_revoked" -> HostConnectionState.Revoked
        "auth_required", "auth_invalid" -> HostConnectionState.AuthFailed
        "permission_required", "owner_required" -> HostConnectionState.PermissionDenied
        else -> when (error.statusCode) {
            401 -> HostConnectionState.AuthFailed
            403 -> HostConnectionState.PermissionDenied
            else -> HostConnectionState.Offline
        }
    }
    else -> error.cause?.takeIf { it !== error }?.let(::connectionStateFor) ?: HostConnectionState.Offline
}

internal fun pollingBackoffMs(consecutiveFailures: Int): Long {
    if (consecutiveFailures <= 0) return 1000L
    return (2500L * (1L shl (consecutiveFailures - 1).coerceIn(0, 4))).coerceAtMost(30_000L)
}

internal fun Throwable.rethrowIfPollingCancellation() {
    if (this is CancellationException) throw this
}

private fun JSONObject.toLibrarySnapshot(
    baseUrl: String,
    token: String,
    hostId: String,
    platform: String,
    fallbackHostName: String,
    capabilities: HostCapabilities? = null,
    tracksOverride: List<Track>? = null
): LibrarySnapshot {
    val tracks = tracksOverride ?: optJSONArray("tracks").toTrackList(baseUrl)
    val playlists = optJSONArray("playlists").toPlaylistList(baseUrl)
    val queueIds = optJSONArray("queue").toStringList()
    val queueItems = optJSONArray("queueItems").toQueueItems()
    val playback = optJSONObject("playback")

    return LibrarySnapshot(
        host = HostProfile(
            hostId = hostId.ifBlank { optString("hostId", "pixelody-host") },
            hostName = optString("hostName", fallbackHostName),
            baseUrl = baseUrl,
            platform = platform.ifBlank { "unknown" },
            roles = capabilities?.roles ?: listOf("libraryHost"),
            connectionState = HostConnectionState.Connected,
            capabilities = capabilities
        ),
        tracks = tracks,
        playlists = playlists,
        favorites = optJSONArray("favorites").toStringList(),
        queue = QueueSnapshot(
            currentTrackId = playback?.optString("currentTrackId")?.takeIf { it.isNotBlank() }
                ?: queueIds.firstOrNull()
                ?: tracks.firstOrNull()?.id,
            trackIds = queueIds,
            shuffle = playback?.optBoolean("shuffle") ?: false,
            repeatMode = when (playback?.optString("repeat")) {
                "one" -> RepeatMode.One
                "all" -> RepeatMode.All
                else -> RepeatMode.Off
            },
            items = queueItems
        ),
        revision = optLong("revision"),
        revisions = optJSONObject("revisions").toSyncRevisions()
    )
}

private fun JSONObject.toHostCapabilities(): HostCapabilities {
    val live = optJSONObject("live")
    val commands = optJSONObject("commands")
    val limits = optJSONObject("limits")
    return HostCapabilities(
        hostId = optString("hostId"),
        hostName = optString("hostName", "Pixelody Host"),
        platform = optString("platform", "unknown"),
        roles = optJSONArray("roles").toStringList(),
        visibility = optString("visibility", "off"),
        canStream = optBoolean("canStream"),
        canRemoteControl = optBoolean("canRemoteControl"),
        canJamCoordinate = optBoolean("canJamCoordinate"),
        supportsPolling = live?.optBoolean("polling") ?: false,
        supportsWebSocket = live?.optBoolean("webSocket") ?: false,
        liveEndpoint = live?.optString("endpoint").orEmpty(),
        recommendedPollMs = live?.optLong("recommendedPollMs", 1000L) ?: 1000L,
        commandPermissions = commands?.optJSONArray("permissions").toStringList(),
        requiresForegroundService = limits?.optBoolean("requiresForegroundService") ?: false,
        batterySensitive = limits?.optBoolean("batterySensitive") ?: false,
        lanExposureEnabled = limits?.optBoolean("lanExposureEnabled") ?: false,
        maxPageSize = limits?.optInt("maxPageSize", 100) ?: 100
    )
}

private fun JSONArray?.toTrackList(baseUrl: String): List<Track> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        val item = getJSONObject(index)
        Track(
            id = item.optString("id"),
            title = item.optString("title", "Untitled track"),
            artist = item.optString("artist", "Unknown artist"),
            album = item.optNullableString("album").orEmpty(),
            durationSeconds = item.optDouble("duration", 0.0).toInt(),
            format = item.optString("format"),
            codec = item.optNullableString("codec") ?: item.optString("format"),
            lossless = item.optBoolean("lossless"),
            sampleRate = item.optInt("sampleRate"),
            bitDepth = item.optionalInt("bitDepth"),
            bitrate = item.optionalInt("bitrate"),
            channels = item.optInt("channels"),
            replayGainDb = item.optionalDouble("replayGainDb"),
            artworkUrl = item.optNullableString("artworkUrl")?.toAbsoluteUrl(baseUrl),
            streamUrl = item.optNullableString("streamUrl")?.toAbsoluteUrl(baseUrl).orEmpty(),
            favorite = item.optBoolean("favorite"),
            missing = item.optBoolean("missing")
        )
    }
}

private fun JSONArray?.toPlaylistList(baseUrl: String): List<Playlist> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        val item = getJSONObject(index)
        Playlist(
            id = item.optString("id"),
            name = item.optString("name", "Playlist"),
            trackIds = item.optJSONArray("trackIds").toStringList(),
            artworkUrl = item.optNullableString("artworkUrl")?.toAbsoluteUrl(baseUrl)
        )
    }
}

private fun JSONArray?.toQueueItems(): List<QueueItem> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        val item = getJSONObject(index)
        QueueItem(
            queueItemId = item.optString("queueItemId"),
            trackId = item.optString("trackId"),
            title = item.optString("title"),
            artist = item.optString("artist"),
            addedByDeviceId = item.optString("addedByDeviceId"),
            addedByName = item.optString("addedByName"),
            sourceDeviceId = item.optString("sourceDeviceId"),
            sourceDeviceName = item.optString("sourceDeviceName"),
            sourceLibraryId = item.optString("sourceLibraryId"),
            availability = item.optString("availability", "available"),
            cacheState = item.optString("cacheState", "none"),
            cacheExpiresAt = item.optionalLong("cacheExpiresAt"),
            playbackStatus = item.optString("playbackStatus", "queued"),
            fallbackCandidates = item.optJSONArray("fallbackCandidates").toStringList(),
            votes = item.optInt("votes", 0),
            upvotedBy = item.optJSONArray("upvotedBy").toStringList(),
            downvotedBy = item.optJSONArray("downvotedBy").toStringList(),
            isSuggestion = item.optBoolean("isSuggestion", false),
            durationSeconds = item.optInt("duration", 0),
            artworkUrl = item.optNullableString("artworkUrl")
        )
    }
}

private fun JSONArray?.toTrustedDevices(): List<TrustedDevice> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        val item = getJSONObject(index)
        TrustedDevice(
            id = item.optString("id"),
            name = item.optString("name", "Trusted device"),
            permissions = item.optJSONArray("permissions").toStringList(),
            tokenPreview = item.optString("tokenPreview"),
            publicKey = item.optString("publicKey"),
            createdAt = item.optString("createdAt"),
            lastSeenAt = item.optString("lastSeenAt"),
            revokedAt = item.optString("revokedAt"),
            status = item.optString("status", "unknown"),
            pairingMethod = item.optString("pairingMethod", "manual")
        )
    }
}

internal fun JSONObject.toLiveState(): LiveState {
    val auth = optJSONObject("auth")
    val playback = optJSONObject("playback")
    return LiveState(
        revision = optLong("revision"),
        hostId = optString("hostId"),
        hostName = optString("hostName", "Pixelody Host"),
        visibility = optString("visibility", "off"),
        checkedAt = optString("checkedAt"),
        pollAfterMs = optLong("pollAfterMs", 1000L),
        permissions = auth?.optJSONArray("permissions").toStringList(),
        playbackCurrentTrackId = playback?.optString("currentTrackId")?.takeIf { it.isNotBlank() },
        playing = playback?.optBoolean("playing") ?: false,
        queueTrackIds = optJSONArray("queue").toStringList(),
        favoriteTrackIds = optJSONArray("favorites").toStringList(),
        deviceRefresh = optJSONObject("deviceRefresh").toDeviceRefreshInstruction(),
        revisions = optJSONObject("revisions").toSyncRevisions(),
        networkSession = optJSONObject("networkSession")?.toNetworkSession(),
        jamSession = optJSONObject("jamSession")?.takeIf { it.optBoolean("active") }?.toJamSessionModel()
    )
}

private fun JSONObject.toNetworkSession(): NetworkSession = NetworkSession(
    kind = optString("kind", "personal-library"),
    sessionId = optString("sessionId"),
    host = optJSONObject("host").toNetworkSessionHost(),
    sync = optJSONObject("sync").toNetworkSessionSync(),
    playback = optJSONObject("playback").toNetworkSessionPlayback(),
    shuffle = optJSONObject("shuffle").toFlowShuffleSnapshot(),
    queue = optJSONObject("queue").toNetworkSessionQueue(),
    diagnostics = optJSONObject("diagnostics").toNetworkSessionDiagnostics()
)

private fun JSONObject?.toFlowShuffleSnapshot(): FlowShuffleSnapshot = FlowShuffleSnapshot(
    version = this?.optInt("version", 1) ?: 1,
    enabled = this?.optBoolean("enabled") ?: false,
    mode = this?.optString("mode", "flow") ?: "flow",
    session = this?.optJSONObject("session").toFlowShuffleSession(),
    plan = this?.optJSONObject("plan").toFlowShufflePlan()
)

private fun JSONObject?.toFlowShuffleSession(): FlowShuffleSession = FlowShuffleSession(
    horizon = (this?.optInt("horizon", 6) ?: 6).coerceIn(5, 10),
    energyShape = this?.optString("energyShape", "auto") ?: "auto",
    albumPolicy = this?.optString("albumPolicy", "track") ?: "track",
    preserveAlbumRuns = this?.optBoolean("preserveAlbumRuns") ?: false
)

private fun JSONObject?.toFlowShufflePlan(): FlowShufflePlan = FlowShufflePlan(
    style = this?.optString("style", "flow") ?: "flow",
    seed = this?.optString("seed").orEmpty(),
    generation = this?.optInt("generation", 0) ?: 0,
    cursor = this?.optInt("cursor", -1) ?: -1,
    sourceQueueIds = this?.optJSONArray("sourceQueueIds").toStringList(),
    order = this?.optJSONArray("order").toStringList(),
    future = this?.optJSONArray("future").toStringList(),
    priorityIds = this?.optJSONArray("priorityIds").toStringList()
)

private fun JSONObject?.toNetworkSessionHost(): NetworkSessionHost = NetworkSessionHost(
    id = this?.optString("id").orEmpty(),
    name = this?.optString("name", "Pixelody Host").orEmpty(),
    platform = this?.optString("platform").orEmpty(),
    visibility = this?.optString("visibility", "off").orEmpty(),
    roles = this?.optJSONArray("roles").toStringList()
)

private fun JSONObject?.toNetworkSessionSync(): NetworkSessionSync = NetworkSessionSync(
    revision = this?.optLong("revision") ?: 0L,
    revisions = this?.optJSONObject("revisions").toSyncRevisions(),
    generatedAt = this?.optString("generatedAt").orEmpty(),
    checkedAt = this?.optString("checkedAt").orEmpty(),
    pollAfterMs = this?.optLong("pollAfterMs", 1000L) ?: 1000L
)

private fun JSONObject?.toSyncRevisions(): SyncRevisions = SyncRevisions(
    overall = this?.optLong("overall") ?: 0L,
    library = this?.optLong("library") ?: 0L,
    playback = this?.optLong("playback") ?: 0L,
    queue = this?.optLong("queue") ?: 0L,
    favorites = this?.optLong("favorites") ?: 0L,
    permissions = this?.optLong("permissions") ?: 0L,
    deviceRefresh = this?.optLong("deviceRefresh") ?: 0L,
    jam = this?.optLong("jam") ?: 0L
)

internal fun JSONObject.toJamSessionModel(): JamSession = JamSession(
    active = optBoolean("active"),
    sessionId = optString("sessionId"),
    mode = optString("mode", "single-host"),
    status = optString("status", if (optBoolean("active")) "active" else "inactive"),
    permissions = optJSONObject("permissions").toJamGuestPolicy(),
    currentParticipant = optJSONObject("currentParticipant").toJamParticipant(),
    participants = optJSONArray("participants").toJamParticipants(),
    queue = optJSONArray("queue").toQueueItems(),
    diagnostics = optJSONObject("diagnostics").toJamDiagnostics()
)

private fun JSONObject?.toJamGuestPolicy(): JamGuestPolicy = JamGuestPolicy(
    guestsCanView = this?.optBoolean("guestsCanView") ?: false,
    guestsCanSuggest = this?.optBoolean("guestsCanSuggest") ?: false,
    guestsCanQueue = this?.optBoolean("guestsCanQueue") ?: false,
    guestsCanEditQueue = this?.optBoolean("guestsCanEditQueue") ?: false,
    guestsCanControlPlayback = this?.optBoolean("guestsCanControlPlayback") ?: false,
    federatedSourcesEnabled = this?.optBoolean("federatedSourcesEnabled") ?: false
)

private fun JSONObject?.toJamParticipant(): JamParticipant? {
    if (this == null || optString("deviceId").isBlank()) return null
    return JamParticipant(
        deviceId = optString("deviceId"),
        name = optString("name", "J.A.M. participant"),
        role = optString("role", "guest"),
        status = optString("status", "pending"),
        permissions = optJSONArray("permissions").toStringList()
    )
}

private fun JSONArray?.toJamParticipants(): List<JamParticipant> {
    if (this == null) return emptyList()
    return List(length()) { index -> optJSONObject(index).toJamParticipant() }.filterNotNull()
}

private fun JSONObject?.toJamDiagnostics(): JamDiagnostics = JamDiagnostics(
    participantCount = this?.optInt("participantCount") ?: 0,
    pendingParticipants = this?.optInt("pendingParticipants") ?: 0,
    revokedParticipants = this?.optInt("revokedParticipants") ?: 0,
    unavailableQueueItems = this?.optInt("unavailableQueueItems") ?: 0,
    lastIssue = this?.optString("lastIssue").orEmpty(),
    recommendedAction = this?.optString("recommendedAction", "none").orEmpty()
)

private fun JSONObject?.toNetworkSessionPlayback(): NetworkSessionPlayback = NetworkSessionPlayback(
    state = this?.optString("state", "paused").orEmpty(),
    currentTrackId = this?.optString("currentTrackId")?.takeIf { it.isNotBlank() },
    currentTrack = this?.optJSONObject("currentTrack")?.toNetworkTrackSummary(),
    playing = this?.optBoolean("playing") ?: false,
    elapsedSeconds = this?.optDouble("elapsedSeconds") ?: 0.0,
    durationSeconds = this?.optDouble("durationSeconds") ?: 0.0,
    positionUpdatedAt = this?.optString("positionUpdatedAt").orEmpty(),
    estimatedStartedAt = this?.optString("estimatedStartedAt").orEmpty()
)

private fun JSONObject?.toNetworkSessionQueue(): NetworkSessionQueue = NetworkSessionQueue(
    trackIds = this?.optJSONArray("trackIds").toStringList(),
    currentIndex = this?.optInt("currentIndex", -1) ?: -1,
    upcomingTrackIds = this?.optJSONArray("upcomingTrackIds").toStringList(),
    upcomingTracks = this?.optJSONArray("upcomingTracks").toNetworkTrackSummaries()
)

private fun JSONObject?.toNetworkSessionDiagnostics(): NetworkSessionDiagnostics = NetworkSessionDiagnostics(
    activeDevices = this?.optInt("activeDevices") ?: 0,
    lastError = this?.optString("lastError").orEmpty(),
    recentCommandCount = this?.optInt("recentCommandCount") ?: 0
)

private fun JSONArray?.toNetworkTrackSummaries(): List<NetworkTrackSummary> {
    if (this == null) return emptyList()
    return List(length()) { index -> optJSONObject(index)?.toNetworkTrackSummary() }.filterNotNull()
}

private fun JSONObject.toNetworkTrackSummary(): NetworkTrackSummary = NetworkTrackSummary(
    id = optString("id"),
    title = optString("title", "Untitled track"),
    artist = optString("artist", "Unknown artist"),
    album = optString("album"),
    duration = optionalDouble("duration"),
    format = optString("format"),
    codec = optString("codec"),
    lossless = optBoolean("lossless"),
    sampleRate = optionalInt("sampleRate"),
    bitDepth = optionalInt("bitDepth"),
    missing = optBoolean("missing")
)

private fun JSONObject?.toDeviceRefreshInstruction(): DeviceRefreshInstruction? {
    if (this == null) return null
    val requestedBy = optJSONObject("requestedBy")
    val id = optString("id")
    if (id.isBlank()) return null
    return DeviceRefreshInstruction(
        id = id,
        action = optString("action", "soft-refresh"),
        scope = optString("scope", "jam"),
        reason = optString("reason"),
        requestedAt = optString("requestedAt"),
        requestedByDeviceName = requestedBy?.optString("deviceName").orEmpty()
            .ifBlank { requestedBy?.optString("kind").orEmpty() }
    )
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
}

private fun JSONObject.optNullableString(name: String): String? {
    if (isNull(name)) return null
    val value = optString(name).trim()
    if (value.isBlank() || value == "null") return null
    return value
}

private fun JSONObject.optionalInt(name: String): Int? {
    return if (isNull(name)) null else optInt(name)
}

private fun JSONObject.optionalDouble(name: String): Double? {
    return if (isNull(name)) null else optDouble(name)
}

private fun JSONObject.optionalLong(name: String): Long? {
    return if (isNull(name)) null else optLong(name)
}

private fun String?.toAbsoluteUrl(baseUrl: String): String? = absoluteMediaUrl(baseUrl, this)

internal fun absoluteMediaUrl(baseUrl: String, value: String?): String? {
    if (value == null) return null
    val mediaPath = value.trim()
    if (mediaPath.isBlank() || mediaPath == "null" || mediaPath == "/null" || mediaPath.endsWith("/null")) return null
    return if (mediaPath.startsWith("http://") || mediaPath.startsWith("https://")) mediaPath
    else "${baseUrl.trimEnd('/')}${if (mediaPath.startsWith("/")) mediaPath else "/$mediaPath"}"
}
