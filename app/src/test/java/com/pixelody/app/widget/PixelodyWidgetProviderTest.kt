package com.pixelody.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelodyWidgetProviderTest {

    @Test
    fun testActionConstants() {
        assertEquals("com.pixelody.app.widget.ACTION_PLAY_PAUSE", PixelodyWidgetProvider.ACTION_PLAY_PAUSE)
        assertEquals("com.pixelody.app.widget.ACTION_PREV", PixelodyWidgetProvider.ACTION_PREV)
        assertEquals("com.pixelody.app.widget.ACTION_NEXT", PixelodyWidgetProvider.ACTION_NEXT)
    }
}
