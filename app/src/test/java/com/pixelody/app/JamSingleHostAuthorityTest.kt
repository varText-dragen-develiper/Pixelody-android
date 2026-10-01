package com.pixelody.app

import com.pixelody.app.core.playback.JamSessionCoordinator
import com.pixelody.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

class JamSingleHostAuthorityTest {
    private fun session() = JamSession(
        active = true, sessionId = "windows-session", mode = "single-host", status = "active",
        permissions = JamGuestPolicy(guestsCanQueue = true, guestsCanEditQueue = true, guestsCanControlPlayback = true),
        currentParticipant = JamParticipant(deviceId = "host-assigned-phone", name = "Phone", role = "guest", status = "active", permissions = listOf("view", "suggest")),
        participants = emptyList(), queue = emptyList(), diagnostics = JamDiagnostics()
    )

    @Test fun serverParticipantAndExplicitPermissionsAreAuthoritative() {
        val coordinator = JamSessionCoordinator(localDeviceId = "local-random-id")
        coordinator.joinSession(session(), JamRole.Host)
        assertEquals("host-assigned-phone", coordinator.session.value.currentParticipant?.deviceId)
        assertEquals("guest", coordinator.session.value.currentParticipant?.role)
        assertTrue(coordinator.canSuggestTracks())
        assertFalse(coordinator.canDirectQueue())
        assertFalse(coordinator.canEditQueue())
        assertFalse(coordinator.canControlPlayback())
        assertFalse(coordinator.canShareFederatedSource())
    }

    @Test fun removalCannotReuseThePreviousParticipant() {
        val coordinator = JamSessionCoordinator()
        coordinator.updateFromLiveState(session())
        coordinator.updateFromLiveState(session().copy(currentParticipant = null))
        assertNull(coordinator.session.value.currentParticipant)
        assertFalse(coordinator.canSuggestTracks())
        assertFalse(coordinator.canControlPlayback())
    }

    @Test fun missingOrStoppedRemoteSessionClearsAllSessionState() {
        for (ended in listOf<JamSession?>(null, session().copy(active = false, status = "inactive"))) {
            val coordinator = JamSessionCoordinator()
            coordinator.updateFromLiveState(session())
            coordinator.updateFromLiveState(ended)
            val state = coordinator.session.value
            assertFalse(state.active)
            assertEquals("", state.sessionId)
            assertNull(state.currentParticipant)
            assertTrue(state.participants.isEmpty())
            assertTrue(state.queue.isEmpty())
            assertFalse(coordinator.canControlPlayback())
        }
    }
}
