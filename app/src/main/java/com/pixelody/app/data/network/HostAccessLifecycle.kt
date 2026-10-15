package com.pixelody.app.data.network

import com.pixelody.app.data.model.HostConnectionState
import kotlinx.coroutines.CancellationException

internal fun HostConnectionState.endsHostAccess(): Boolean = this in setOf(
    HostConnectionState.Revoked, HostConnectionState.CredentialExpired, HostConnectionState.AuthFailed
)

/** A late request must never repopulate state after forget, revocation, or a new connection. */
internal class HostRequestGeneration {
    var current: Long = 0
        private set
    fun invalidate(): Long = ++current
    fun accepts(generation: Long): Boolean = generation == current
}

/** Terminal auth cannot be overwritten by a later unreachable address during reconnect. */
internal fun rethrowTerminalHostFailure(error: Throwable) {
    if (error is CancellationException || connectionStateFor(error).endsHostAccess()) throw error
}

/** Always end runtime access, including when durable erasure reports failure. */
internal fun endHostAccess(clearRuntime: () -> Unit, clearCredential: () -> Unit): Boolean {
    clearRuntime()
    return runCatching(clearCredential).isSuccess
}
