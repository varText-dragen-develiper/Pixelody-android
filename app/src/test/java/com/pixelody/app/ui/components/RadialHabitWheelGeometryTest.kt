package com.pixelody.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadialHabitWheelGeometryTest {

    @Test
    fun computeCabinetPositionsHandlesEmptyItems() {
        val positions = computeCabinetPositions(
            centerOffset = Offset(500f, 500f),
            screenWidth = 1080f,
            screenHeight = 1920f,
            items = emptyList(),
            radiusPx = 200f
        )
        assertTrue(positions.isEmpty())
    }

    @Test
    fun computeCabinetPositionsQuadrantAwareness() {
        val items = listOf(
            CabinetActionItem(CabinetActionId.PlayNext, TransportGlyphType.PlayNext, "Play Next", "Next"),
            CabinetActionItem(CabinetActionId.AddToQueue, TransportGlyphType.AddToQueue, "Add Queue", "Append"),
            CabinetActionItem(CabinetActionId.ToggleFavorite, TransportGlyphType.Heart, "Favorite", "Save")
        )

        val screenWidth = 1000f
        val screenHeight = 2000f
        val radius = 150f

        // Near left screen edge (normX < 0.35f): fans rightward (-65 deg to 65 deg)
        val leftPositions = computeCabinetPositions(
            centerOffset = Offset(100f, 1000f),
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            items = items,
            radiusPx = radius
        )
        assertEquals(3, leftPositions.size)
        assertEquals(-65f, leftPositions.first().angleDegrees, 0.01f)
        assertEquals(65f, leftPositions.last().angleDegrees, 0.01f)

        // Near right screen edge (normX > 0.65f): fans leftward (115 deg to 245 deg)
        val rightPositions = computeCabinetPositions(
            centerOffset = Offset(900f, 1000f),
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            items = items,
            radiusPx = radius
        )
        assertEquals(3, rightPositions.size)
        assertEquals(115f, rightPositions.first().angleDegrees, 0.01f)
        assertEquals(245f, rightPositions.last().angleDegrees, 0.01f)

        // Near bottom screen edge (normY > 0.65f): fans upward (200 deg to 340 deg)
        val bottomPositions = computeCabinetPositions(
            centerOffset = Offset(500f, 1800f),
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            items = items,
            radiusPx = radius
        )
        assertEquals(3, bottomPositions.size)
        assertEquals(200f, bottomPositions.first().angleDegrees, 0.01f)
        assertEquals(340f, bottomPositions.last().angleDegrees, 0.01f)

        // Near top screen edge (normY < 0.35f): fans downward (20 deg to 160 deg)
        val topPositions = computeCabinetPositions(
            centerOffset = Offset(500f, 200f),
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            items = items,
            radiusPx = radius
        )
        assertEquals(3, topPositions.size)
        assertEquals(20f, topPositions.first().angleDegrees, 0.01f)
        assertEquals(160f, topPositions.last().angleDegrees, 0.01f)
    }

    @Test
    fun computeCabinetPositionsClampsWithinSafeMargins() {
        val items = listOf(
            CabinetActionItem(CabinetActionId.PlayNext, TransportGlyphType.PlayNext, "Play Next", "Next")
        )
        val screenWidth = 400f
        val screenHeight = 800f
        val safeMargin = 40f

        val positions = computeCabinetPositions(
            centerOffset = Offset(10f, 10f),
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            items = items,
            radiusPx = 100f,
            safeMarginPx = safeMargin
        )

        assertEquals(1, positions.size)
        val pos = positions.first()
        assertTrue(pos.offset.x >= safeMargin)
        assertTrue(pos.offset.x <= screenWidth - safeMargin)
        assertTrue(pos.offset.y >= safeMargin)
        assertTrue(pos.offset.y <= screenHeight - safeMargin)
    }

    @Test
    fun classifyCabinetHoverSelectsClosestWithinHitRadius() {
        val itemA = CabinetActionItem(CabinetActionId.PlayNext, TransportGlyphType.PlayNext, "A", "Sub A")
        val itemB = CabinetActionItem(CabinetActionId.AddToQueue, TransportGlyphType.AddToQueue, "B", "Sub B")

        val satellites = listOf(
            CabinetPosition(itemA, Offset(100f, 100f), 0f),
            CabinetPosition(itemB, Offset(300f, 100f), 90f)
        )

        val hitRadius = 50f

        // Touch near item A
        val touchNearA = Offset(110f, 105f)
        assertEquals(CabinetActionId.PlayNext, classifyCabinetHover(touchNearA, satellites, hitRadius))

        // Touch near item B
        val touchNearB = Offset(290f, 95f)
        assertEquals(CabinetActionId.AddToQueue, classifyCabinetHover(touchNearB, satellites, hitRadius))

        // Touch in dead zone (too far from both)
        val touchFar = Offset(200f, 100f) // distance is 100px from both, > 50px
        assertNull(classifyCabinetHover(touchFar, satellites, hitRadius))
    }

    @Test
    fun buildCabinetActionItemsForTrackTarget() {
        val track = Track(id = "tr-1", title = "Acoustic Sun", artist = "Solaris")

        // Unfavorited, online
        val defaultItems = buildCabinetActionItems(CabinetTarget.TrackTarget(track), isFavorite = false, isOffline = false)
        assertEquals(5, defaultItems.size)
        assertEquals(CabinetActionId.PlayNext, defaultItems[0].id)
        assertEquals(CabinetActionId.AddToQueue, defaultItems[1].id)
        assertEquals(CabinetActionId.ToggleFavorite, defaultItems[2].id)
        assertEquals("Favorite", defaultItems[2].label)
        assertEquals(CabinetActionId.StartFlowRadio, defaultItems[3].id)
        assertEquals(CabinetActionId.ToggleOffline, defaultItems[4].id)
        assertEquals("Offline", defaultItems[4].label)

        // Favorited, offline downloaded
        val savedItems = buildCabinetActionItems(CabinetTarget.TrackTarget(track), isFavorite = true, isOffline = true)
        assertEquals("Favorited", savedItems[2].label)
        assertEquals("Saved", savedItems[4].label)
    }

    @Test
    fun buildCabinetActionItemsForSourceTargets() {
        // Desktop source
        val hostItemsConnected = buildCabinetActionItems(CabinetTarget.SourceTarget(SourceScope.DesktopHost, isConnected = true))
        assertEquals(4, hostItemsConnected.size)
        assertEquals(CabinetActionId.ConnectDesktop, hostItemsConnected[0].id)
        assertEquals("Reconnect", hostItemsConnected[0].label)
        assertEquals(CabinetActionId.RefreshHostLibrary, hostItemsConnected[1].id)
        assertEquals(CabinetActionId.RescanHostDirectories, hostItemsConnected[2].id)
        assertEquals(CabinetActionId.OpenDesktopPairing, hostItemsConnected[3].id)

        val hostItemsDisconnected = buildCabinetActionItems(CabinetTarget.SourceTarget(SourceScope.DesktopHost, isConnected = false))
        assertEquals("Connect", hostItemsDisconnected[0].label)

        // Phone source
        val phoneItems = buildCabinetActionItems(CabinetTarget.SourceTarget(SourceScope.LocalPhone))
        assertEquals(4, phoneItems.size)
        assertEquals(CabinetActionId.RescanPhoneStorage, phoneItems[0].id)
        assertEquals(CabinetActionId.PickAudioFiles, phoneItems[1].id)
        assertEquals(CabinetActionId.ClearCacheStorage, phoneItems[2].id)
        assertEquals(CabinetActionId.OpenFileBrowser, phoneItems[3].id)

        // Jam source
        val jamItems = buildCabinetActionItems(CabinetTarget.SourceTarget(SourceScope.JamMesh, isJamActive = false))
        assertEquals(4, jamItems.size)
        assertEquals(CabinetActionId.StartJamSession, jamItems[0].id)
        assertEquals("Start Jam", jamItems[0].label)
        assertEquals(CabinetActionId.JoinJamSession, jamItems[1].id)
        assertEquals(CabinetActionId.BroadcastJamPresence, jamItems[2].id)
        assertEquals(CabinetActionId.ClearJamQueue, jamItems[3].id)

        // All sources
        val allItems = buildCabinetActionItems(CabinetTarget.SourceTarget(SourceScope.All))
        assertEquals(4, allItems.size)
        assertEquals(CabinetActionId.QuickPlayFlow, allItems[0].id)
        assertEquals(CabinetActionId.ShuffleAllPool, allItems[1].id)
        assertEquals(CabinetActionId.ResetFilters, allItems[2].id)
        assertEquals(CabinetActionId.DailySoundCheck, allItems[3].id)
    }

    @Test
    fun buildCabinetActionItemsForCapsuleAndQuickStart() {
        val capsuleItems = buildCabinetActionItems(CabinetTarget.CapsuleTarget(3, "Warm Mix", "Acoustic"))
        assertEquals(4, capsuleItems.size)
        assertEquals(CabinetActionId.DailySoundCheck, capsuleItems[0].id)
        assertEquals(CabinetActionId.QuickPlayFlow, capsuleItems[1].id)

        val quickStartItems = buildCabinetActionItems(CabinetTarget.QuickStartTarget())
        assertEquals(4, quickStartItems.size)
        assertEquals(CabinetActionId.QuickPlayFlow, quickStartItems[0].id)
        assertEquals(CabinetActionId.ShuffleAllPool, quickStartItems[1].id)
        assertEquals(CabinetActionId.AddToQueue, quickStartItems[2].id)
        assertEquals(CabinetActionId.ResetFilters, quickStartItems[3].id)
    }

    @Test
    fun orbitCompatibilityMappings() {
        val orbitItems = buildOrbitActionItems(isFavorite = true, isOffline = false)
        assertEquals(5, orbitItems.size)
        assertEquals(OrbitActionType.PlayNext, orbitItems[0].type)
        assertEquals(OrbitActionType.AddToQueue, orbitItems[1].type)
        assertEquals(OrbitActionType.ToggleFavorite, orbitItems[2].type)
        assertEquals("Favorited", orbitItems[2].label)
        assertEquals(OrbitActionType.StartFlowRadio, orbitItems[3].type)
        assertEquals(OrbitActionType.ToggleOffline, orbitItems[4].type)

        val positions = computeOrbitPositions(
            centerOffset = Offset(500f, 500f),
            screenWidth = 1000f,
            screenHeight = 1000f,
            items = orbitItems,
            radiusPx = 150f
        )
        assertEquals(5, positions.size)

        val closest = classifyOrbitHover(positions[0].offset, positions, 30f)
        assertEquals(OrbitActionType.PlayNext, closest)
    }
}
