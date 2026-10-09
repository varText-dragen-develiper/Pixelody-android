package com.pixelody.app.feature.baselayer

import com.pixelody.app.core.playback.JamSessionCoordinator
import org.junit.Assert.*
import org.junit.Test

class ConnectionRecoveryTest {
    @Test fun remoteBrowsingReturnsToPhoneWhileLocalBrowsingStaysPut() {
        assertEquals(BaseSource.Phone, sourceAfterConnectionLoss(BaseSource.Host))
        assertEquals(BaseSource.Phone, sourceAfterConnectionLoss(BaseSource.Jam))
        assertEquals(BaseSource.Phone, sourceAfterConnectionLoss(BaseSource.Phone))
        assertEquals(BaseSource.All, sourceAfterConnectionLoss(BaseSource.All))
    }

    @Test fun endedRemoteMeshClearsIdentityPermissionsQueueAndClock() {
        val host = JamSessionCoordinator(localDeviceId = "host")
        val remote = host.startHosting()
        for (ended in listOf(null, remote.copy(active = false))) {
            val guest = JamSessionCoordinator(localDeviceId = "phone")
            guest.joinSession(remote)
            assertTrue(guest.session.value.active)
            guest.updateFromLiveState(ended)
            assertFalse(guest.session.value.active)
            assertEquals("", guest.session.value.sessionId)
            assertNull(guest.session.value.currentParticipant)
            assertTrue(guest.session.value.participants.isEmpty())
            assertTrue(guest.session.value.queue.isEmpty())
            assertFalse(guest.canSuggestTracks())
            assertFalse(guest.canControlPlayback())
        }
    }
}
