package com.pixelody.app.feature.baselayer

import org.junit.Assert.*
import org.junit.Test

class DestinationSwipeTest {
    @Test fun flickNeedsPurposefulDistanceAndVelocityInTheSameDirection() {
        val swipe = DestinationSwipe(72f)
        swipe.begin(); swipe.direct(-20f, 0f); assertEquals(1, swipe.finish(velocity = -900f))
        swipe.begin(); swipe.direct(-10f, 0f); assertNull(swipe.finish(velocity = -3000f))
        swipe.begin(); swipe.direct(-20f, 0f); assertNull(swipe.finish(velocity = 900f))
        swipe.begin(); swipe.direct(-20f, 0f); assertNull(swipe.finish(velocity = -400f))
        swipe.begin(); swipe.direct(-100f, 0f); assertNull(swipe.finish(velocity = 900f))
    }

    @Test fun reversingBackIntoAChildAbandonsItsPreviousEdgeHandoff() {
        val swipe = DestinationSwipe(72f)
        swipe.begin(); swipe.childScroll(0f, -90f)
        swipe.childScroll(30f, 0f)
        assertNull(swipe.finish())
    }
    @Test fun deferredReleaseCannotFinishANewerGesture() {
        val swipe = DestinationSwipe(72f)
        swipe.begin(); val old = swipe.generation
        swipe.direct(-90f, 0f)
        swipe.begin(); swipe.direct(90f, 0f)
        assertNull(swipe.finish(old))
        assertEquals(-1, swipe.finish())
    }
    @Test fun followsTabOrderWithoutWrappingAtEdges() {
        assertEquals(BaseDestination.Search, BaseDestination.Home.swipeNeighbor(1))
        assertEquals(BaseDestination.Library, BaseDestination.Search.swipeNeighbor(1))
        assertEquals(BaseDestination.Home, BaseDestination.Search.swipeNeighbor(-1))
        assertEquals(BaseDestination.Search, BaseDestination.Library.swipeNeighbor(-1))
        assertNull(BaseDestination.Home.swipeNeighbor(-1))
        assertNull(BaseDestination.Library.swipeNeighbor(1))
    }
    @Test fun aChildOnlyHandsOffItsUnusedMovement() {
        val swipe = DestinationSwipe(72f)
        swipe.begin()
        swipe.unusedHorizontal(-20f)
        assertNull(swipe.finish())
        swipe.begin()
        swipe.unusedHorizontal(-80f)
        assertEquals(1, swipe.finish())
        assertNull(swipe.finish())
        swipe.unusedHorizontal(-400f) // Momentum after release cannot navigate.
        assertNull(swipe.finish())
    }
    @Test fun rejectsVerticalShortReversedAndMultitouchDrags() {
        val swipe = DestinationSwipe(72f)
        swipe.begin(); swipe.direct(-90f, 120f); assertNull(swipe.finish())
        swipe.begin(); swipe.direct(-40f, 0f); assertNull(swipe.finish())
        swipe.begin(); swipe.direct(-100f, 0f); swipe.direct(80f, 0f); assertNull(swipe.finish())
        swipe.begin(); swipe.direct(-200f, 0f); swipe.cancel(); assertNull(swipe.finish())
    }
    @Test fun doesNotDoubleCountDirectAndNestedObservations() {
        val swipe = DestinationSwipe(72f)
        swipe.begin(); swipe.direct(-40f, 0f); swipe.unusedHorizontal(-40f)
        assertNull(swipe.finish())
        swipe.begin(); swipe.direct(90f, 0f); assertEquals(-1, swipe.finish())
    }
}
