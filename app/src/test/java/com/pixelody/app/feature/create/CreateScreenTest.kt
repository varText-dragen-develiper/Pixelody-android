package com.pixelody.app.feature.create

import com.pixelody.app.ui.navigation.PixelodyStateTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateScreenTest {

    @Test
    fun createScreen_stateTagsAreRegisteredAndFormatted() {
        val expectedTags = listOf(
            PixelodyStateTags.ACQUIRE_SCAN_DEVICE,
            PixelodyStateTags.ACQUIRE_PICK_FOLDER,
            PixelodyStateTags.ACQUIRE_PICK_FILES,
            PixelodyStateTags.CREATE_PAIR,
            PixelodyStateTags.CREATE_HOST_TOGGLE
        )

        for (tag in expectedTags) {
            assertTrue("Tag $tag must be present in PixelodyStateTags.all", tag in PixelodyStateTags.all)
            assertTrue("Tag $tag must follow state tag convention", tag.startsWith("state:"))
        }
    }

    @Test
    fun createScreen_acquisitionTagsFollowUnifiedPrefix() {
        assertTrue(PixelodyStateTags.ACQUIRE_SCAN_DEVICE.startsWith("state:acquire"))
        assertTrue(PixelodyStateTags.ACQUIRE_PICK_FOLDER.startsWith("state:acquire"))
        assertTrue(PixelodyStateTags.ACQUIRE_PICK_FILES.startsWith("state:acquire"))
    }
}
