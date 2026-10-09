package com.pixelody.app.data.repository

import com.pixelody.app.data.api.toModel
import com.pixelody.app.data.fixtures.FakePixelodyHost
import com.pixelody.app.data.model.HostConnectionDetails
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.LivePollResult
import com.pixelody.app.data.model.JamSession
import com.pixelody.app.data.model.SavedHostProfile
import com.pixelody.app.data.model.TrustedDevice
import com.pixelody.app.data.network.DeviceRefreshResult
import com.pixelody.app.data.network.HostConnectionResult
import com.pixelody.app.data.network.JamJoinResult
import com.pixelody.app.data.network.PixelodyHostApiClient
import com.pixelody.app.data.network.rethrowTerminalHostFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HostRepository(
    private val fakeHost: FakePixelodyHost,
    private val hostApiClient: PixelodyHostApiClient = PixelodyHostApiClient()
) {
    fun loadFixtureLibrary(): LibrarySnapshot {
        return fakeHost.librarySnapshot().toModel(fakeHost.baseUrl)
    }

    suspend fun loadHostLibrary(baseUrl: String, token: String): LibrarySnapshot {
        return connectHost(baseUrls = listOf(baseUrl), token = token).snapshot
    }

    suspend fun loadHostLibrary(details: HostConnectionDetails): LibrarySnapshot {
        return connectHost(details).snapshot
    }

    suspend fun connectHost(details: HostConnectionDetails): HostConnectionResult {
        return withContext(Dispatchers.IO) {
            val candidates = normalizedCandidates(details.baseUrls)
            var lastError: Throwable? = null
            var token = details.token
            var grantedPermissions = emptyList<String>()
            for (candidate in candidates) {
                runCatching {
                    // Pairing consumes a one-use secret. Once exchanged, reuse
                    // the credential if library loading needs another address.
                    if (token.isBlank()) {
                        require(details.hasPairingSecret) { "A trusted-device token or pairing payload is required." }
                        val credential = hostApiClient.completePairing(
                            baseUrl = candidate,
                            pairingCode = details.pairingCode,
                            secret = details.pairingSecret,
                            deviceName = details.deviceName
                        )
                        token = credential.token
                        grantedPermissions = credential.permissions
                    }
                    hostApiClient.connectToHost(baseUrl = candidate, token = token).copy(grantedPermissions = grantedPermissions)
                }.onSuccess { return@withContext it }
                    .onFailure { rethrowTerminalHostFailure(it); lastError = it }
            }
            val reason = lastError?.message?.let { " Last error: $it" }.orEmpty()
            throw IllegalStateException(
                "Could not pair or connect to any advertised host address.$reason",
                lastError
            )
        }
    }

    suspend fun loadHostLibrary(baseUrls: List<String>, token: String): LibrarySnapshot {
        return connectHost(baseUrls, token).snapshot
    }

    suspend fun connectHost(baseUrls: List<String>, token: String): HostConnectionResult {
        return withContext(Dispatchers.IO) {
            val candidates = normalizedCandidates(baseUrls)
            require(token.isNotBlank()) { "Token is required" }
            var lastError: Throwable? = null
            for (candidate in candidates) {
                runCatching {
                    hostApiClient.connectToHost(baseUrl = candidate, token = token)
                }.onSuccess { return@withContext it }
                    .onFailure { rethrowTerminalHostFailure(it); lastError = it }
            }
            val reason = lastError?.message?.let { " Last error: $it" }.orEmpty()
            throw IllegalStateException(
                "Could not connect to any advertised host address.$reason",
                lastError
            )
        }
    }

    private fun normalizedCandidates(baseUrls: List<String>): List<String> {
        val candidates = baseUrls.map { it.trim().trimEnd('/') }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
        require(candidates.isNotEmpty()) { "Host URL is required" }
        return candidates
    }

    fun savedProfileFor(result: HostConnectionResult, baseUrls: List<String>): SavedHostProfile {
        val host = result.snapshot.host
        val now = System.currentTimeMillis()
        return SavedHostProfile(
            hostId = host.hostId,
            hostName = host.hostName,
            baseUrl = host.baseUrl,
            baseUrls = normalizedCandidates((baseUrls + host.baseUrl).ifEmpty { listOf(host.baseUrl) }),
            token = result.token,
            platform = host.platform,
            roles = host.roles,
            savedAt = now,
            lastConnectedAt = now
        )
    }

    suspend fun fetchLiveState(baseUrl: String, token: String, sinceRevision: Long = -1): LivePollResult {
        return withContext(Dispatchers.IO) {
            hostApiClient.fetchLiveState(baseUrl = baseUrl, token = token, sinceRevision = sinceRevision)
        }
    }

    suspend fun sendPlaybackCommand(baseUrl: String, token: String, action: String, trackId: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            hostApiClient.sendPlaybackCommand(baseUrl = baseUrl, token = token, action = action, trackId = trackId)
        }
    }

    suspend fun sendQueueCommand(baseUrl: String, token: String, action: String, trackId: String? = null, toIndex: Int? = null): Boolean {
        return withContext(Dispatchers.IO) {
            hostApiClient.sendQueueCommand(baseUrl = baseUrl, token = token, action = action, trackId = trackId, toIndex = toIndex)
        }
    }

    suspend fun fetchTrustedDevices(baseUrl: String, token: String): List<TrustedDevice> {
        return withContext(Dispatchers.IO) {
            hostApiClient.fetchTrustedDevices(baseUrl = baseUrl, token = token)
        }
    }

    suspend fun revokeTrustedDevice(baseUrl: String, token: String, deviceId: String): List<TrustedDevice> {
        return withContext(Dispatchers.IO) {
            hostApiClient.revokeTrustedDevice(baseUrl = baseUrl, token = token, deviceId = deviceId)
        }
    }

    suspend fun requestDeviceRefresh(baseUrl: String, token: String, reason: String): DeviceRefreshResult {
        return withContext(Dispatchers.IO) {
            hostApiClient.requestDeviceRefresh(baseUrl = baseUrl, token = token, reason = reason)
        }
    }

    suspend fun requestJamJoin(baseUrl: String, token: String, role: String = "guest"): JamJoinResult {
        return withContext(Dispatchers.IO) {
            hostApiClient.requestJamJoin(baseUrl = baseUrl, token = token, role = role)
        }
    }

    suspend fun fetchJamSession(baseUrl: String, token: String): JamSession {
        return withContext(Dispatchers.IO) {
            hostApiClient.fetchJamSession(baseUrl = baseUrl, token = token)
        }
    }

    suspend fun sendJamQueueCommand(baseUrl: String, token: String, action: String, trackId: String? = null, queueItemId: String? = null, toIndex: Int? = null): JamSession {
        return withContext(Dispatchers.IO) {
            hostApiClient.sendJamQueueCommand(baseUrl, token, action, trackId, queueItemId, toIndex)
        }
    }

    suspend fun sendJamPlaybackCommand(baseUrl: String, token: String, action: String) {
        withContext(Dispatchers.IO) {
            hostApiClient.sendJamPlaybackCommand(baseUrl, token, action)
        }
    }

    suspend fun updateJamPolicy(baseUrl: String, token: String, policy: com.pixelody.app.data.model.JamGuestPolicy): JamSession {
        return withContext(Dispatchers.IO) {
            hostApiClient.updateJamPolicy(baseUrl, token, policy)
        }
    }

    suspend fun voteJamTrack(baseUrl: String, token: String, queueItemId: String, vote: Int): JamSession {
        return withContext(Dispatchers.IO) {
            hostApiClient.voteJamTrack(baseUrl, token, queueItemId, vote)
        }
    }

    suspend fun promoteJamSuggestion(baseUrl: String, token: String, queueItemId: String): JamSession {
        return withContext(Dispatchers.IO) {
            hostApiClient.promoteJamSuggestion(baseUrl, token, queueItemId)
        }
    }

    suspend fun setJamDj(baseUrl: String, token: String, deviceId: String): JamSession {
        return withContext(Dispatchers.IO) {
            hostApiClient.setJamDj(baseUrl, token, deviceId)
        }
    }

    suspend fun kickJamParticipant(baseUrl: String, token: String, deviceId: String): JamSession {
        return withContext(Dispatchers.IO) {
            hostApiClient.kickJamParticipant(baseUrl, token, deviceId)
        }
    }

    suspend fun syncJamTime(baseUrl: String, token: String, clientSendMs: Long = System.currentTimeMillis()): com.pixelody.app.data.model.JamClockSyncSample {
        return withContext(Dispatchers.IO) {
            hostApiClient.syncJamTime(baseUrl, token, clientSendMs)
        }
    }
}
