package com.pixelody.app

import com.pixelody.app.core.playback.SoundMeshEngine
import com.pixelody.app.data.model.SoundMeshNode
import com.pixelody.app.data.model.SpatialSpeakerRole
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundMeshEngineTest {

    @Test
    fun `calculatePtpClockOffset and calculateOneWayDelay calculate accurate sub-ms timing`() {
        val t1 = 1000L
        val t2 = 2000L
        val t3 = 2050L
        val t4 = 1060L

        // Offset = ((t2 - t1) + (t3 - t4)) / 2 = ((2000 - 1000) + (2050 - 1060)) / 2 = (1000 + 990) / 2 = 995ms
        val offset = SoundMeshEngine.calculatePtpClockOffset(t1, t2, t3, t4)
        assertEquals(995L, offset)

        // Delay = ((t4 - t1) - (t3 - t2)) / 2 = ((1060 - 1000) - (2050 - 2000)) / 2 = (60 - 50) / 2 = 5ms
        val delay = SoundMeshEngine.calculateOneWayDelay(t1, t2, t3, t4)
        assertEquals(5L, delay)
    }

    @Test
    fun `assignSpatialRoles assigns Left, Right, Sub, and Display appropriately`() {
        val singleNode = listOf(SoundMeshNode("dev1", "Phone 1"))
        val singleAssigned = SoundMeshEngine.assignSpatialRoles(singleNode)
        assertEquals(SpatialSpeakerRole.FullStereo, singleAssigned[0].assignedRole)
        assertTrue(singleAssigned[0].isHostAuthority)

        val threeNodes = listOf(
            SoundMeshNode("dev1", "Phone 1"),
            SoundMeshNode("dev2", "Phone 2"),
            SoundMeshNode("dev3", "Tablet 1")
        )
        val threeAssigned = SoundMeshEngine.assignSpatialRoles(threeNodes)
        assertEquals(SpatialSpeakerRole.LeftMain, threeAssigned[0].assignedRole)
        assertEquals(SpatialSpeakerRole.RightMain, threeAssigned[1].assignedRole)
        assertEquals(SpatialSpeakerRole.CenterSub, threeAssigned[2].assignedRole)
    }

    @Test
    fun `calculateConsensusQueue ranks tracks by peer vote tally`() {
        val trackA = Track(id = "track_a", title = "Song A", durationSeconds = 180, streamUrl = "url_a")
        val trackB = Track(id = "track_b", title = "Song B", durationSeconds = 180, streamUrl = "url_b")
        val trackC = Track(id = "track_c", title = "Song C", durationSeconds = 180, streamUrl = "url_c")

        val votes = mapOf(
            "dev1" to "track_b",
            "dev2" to "track_b",
            "dev3" to "track_a",
            "dev4" to "track_b"
        )

        val consensus = SoundMeshEngine.calculateConsensusQueue(listOf(trackA, trackB, trackC), votes)
        assertEquals("track_b", consensus[0].first.id)
        assertEquals(3, consensus[0].second)

        assertEquals("track_a", consensus[1].first.id)
        assertEquals(1, consensus[1].second)

        assertEquals("track_c", consensus[2].first.id)
        assertEquals(0, consensus[2].second)
    }
}
