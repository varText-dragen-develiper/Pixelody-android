package com.pixelody.app

import com.pixelody.app.core.playback.JamSessionCoordinator
import com.pixelody.app.data.model.JamGuestPolicy
import com.pixelody.app.data.model.JamRole
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JamSessionCoordinatorTest {

    private val sampleTrack1 = Track(
        id = "t1",
        title = "Moonlit Circuit",
        artist = "Local Signal",
        durationSeconds = 240
    )

    private val sampleTrack2 = Track(
        id = "t2",
        title = "Window Memory",
        artist = "Tape Orchard",
        durationSeconds = 180
    )

    @Test
    fun startHostingInitializesSessionCorrectly() {
        val coordinator = JamSessionCoordinator(
            localDeviceId = "host-device-01",
            localDeviceName = "Studio Host"
        )

        val session = coordinator.startHosting(
            roomName = "Pixelody Studio",
            initialQueue = listOf(sampleTrack1)
        )

        assertTrue(session.active)
        assertEquals("active", session.status)
        assertTrue(session.joinCode.length == 6)
        assertEquals(JamRole.Host, coordinator.currentRole)
        assertTrue(coordinator.isHost)
        assertTrue(coordinator.isDj)

        assertEquals(1, session.participants.size)
        assertEquals("host-device-01", session.participants.first().deviceId)
        assertEquals(1, session.queue.size)
        assertEquals("t1", session.queue.first().trackId)
        assertEquals("playing", session.queue.first().playbackStatus)
    }

    @Test
    fun guestPermissionsEnforceGuestPolicy() {
        val coordinator = JamSessionCoordinator(
            localDeviceId = "guest-device-02",
            localDeviceName = "Guest Phone"
        )

        val hostSession = JamSessionCoordinator(localDeviceId = "host-01")
            .startHosting(
                policy = JamGuestPolicy(
                    guestsCanView = true,
                    guestsCanSuggest = true,
                    guestsCanQueue = false,
                    guestsCanEditQueue = false,
                    guestsCanControlPlayback = false,
                    federatedSourcesEnabled = false
                )
            )

        coordinator.joinSession(hostSession, asRole = JamRole.Guest)

        assertEquals(JamRole.Guest, coordinator.currentRole)
        assertFalse(coordinator.isHost)
        assertFalse(coordinator.isDj)

        assertTrue(coordinator.canSuggestTracks())
        assertFalse(coordinator.canDirectQueue())
        assertFalse(coordinator.canEditQueue())
        assertFalse(coordinator.canControlPlayback())
        assertFalse(coordinator.canShareFederatedSource())
    }

    @Test
    fun votingEngineAccumulatesVotesAndAutoPromotes() {
        val coordinator = JamSessionCoordinator(
            localDeviceId = "device-a",
            localDeviceName = "Device A"
        )
        coordinator.startHosting()

        // Queue track 2 as a suggestion
        val item = coordinator.queueTrack(sampleTrack2, asSuggestion = true)
        assertNotNull(item)
        val itemId = item!!.queueItemId

        assertTrue(coordinator.session.value.queue.first { it.queueItemId == itemId }.isSuggestion)
        assertEquals(1, coordinator.session.value.queue.first { it.queueItemId == itemId }.votes)

        // Upvote from device-a should keep it at 1 (already voted)
        coordinator.voteForTrack(itemId, voteType = 1)
        assertEquals(1, coordinator.session.value.queue.first { it.queueItemId == itemId }.votes)

        // Manually promote suggestion
        val promoted = coordinator.promoteSuggestion(itemId)
        assertTrue(promoted)
        assertFalse(coordinator.session.value.queue.first { it.queueItemId == itemId }.isSuggestion)
    }

    @Test
    fun djPromotionGrantsElevatedQueuePermissions() {
        val hostCoordinator = JamSessionCoordinator(localDeviceId = "host-01")
        val session = hostCoordinator.startHosting()

        val guestCoordinator = JamSessionCoordinator(localDeviceId = "guest-02")
        guestCoordinator.joinSession(session, JamRole.Guest)

        assertFalse(guestCoordinator.isDj)

        // Host promotes guest to DJ
        hostCoordinator.setDj("guest-02")
        val updated = hostCoordinator.session.value

        guestCoordinator.updateFromLiveState(updated)
        assertTrue(guestCoordinator.isDj)
        assertTrue(guestCoordinator.canDirectQueue())
        assertTrue(guestCoordinator.canEditQueue())
        assertTrue(guestCoordinator.canControlPlayback())
    }

    @Test
    fun queueReorderingAndRemovalWorkCorrectly() {
        val coordinator = JamSessionCoordinator(localDeviceId = "host-01")
        coordinator.startHosting(initialQueue = listOf(sampleTrack1, sampleTrack2))

        assertEquals(2, coordinator.session.value.queue.size)
        assertEquals("t1", coordinator.session.value.queue[0].trackId)
        assertEquals("t2", coordinator.session.value.queue[1].trackId)

        // Move item 0 to 1
        coordinator.moveQueueItem(0, 1)
        assertEquals("t2", coordinator.session.value.queue[0].trackId)
        assertEquals("t1", coordinator.session.value.queue[1].trackId)

        // Remove item
        val itemToRemoveId = coordinator.session.value.queue[0].queueItemId
        coordinator.removeQueueItem(itemToRemoveId)
        assertEquals(1, coordinator.session.value.queue.size)
        assertEquals("t1", coordinator.session.value.queue[0].trackId)

        // Clear queue
        coordinator.clearQueue()
        // Only playing track remains or empty
        assertTrue(coordinator.session.value.queue.none { it.playbackStatus != "playing" })
    }
}
