package com.pixelody.app

import com.pixelody.app.feature.nowplaying.commitsPull
import org.junit.Assert.*
import org.junit.Test

class PlayerPullDecisionTest {
    @Test fun slowPullAndPurposefulFlickCommitButJitterAndWrongDirectionDoNot() {
        assertTrue(commitsPull(72f, 0f, 72f))
        assertTrue(commitsPull(20f, 900f, 72f))
        assertFalse(commitsPull(10f, 3000f, 72f))
        assertFalse(commitsPull(20f, -900f, 72f))
        assertFalse(commitsPull(40f, 0f, 72f))
        assertFalse("A quick reversal cancels even a longer pull", commitsPull(100f, -900f, 72f))
    }
}
