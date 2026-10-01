package com.pixelody.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryNavigationBarTest {

    @Test
    fun pixelodyTab_primaryTabsIdentified() {
        assertTrue(PixelodyTab.Home.isPrimary)
        assertTrue(PixelodyTab.Search.isPrimary)
        assertTrue(PixelodyTab.Library.isPrimary)
        assertFalse(PixelodyTab.Create.isPrimary)
        assertFalse(PixelodyTab.Profile.isPrimary)
        assertFalse(PixelodyTab.Player.isPrimary)
        assertFalse(PixelodyTab.Queue.isPrimary)
    }

    @Test
    fun pixelodyTab_routeAndLabelsConsistent() {
        assertEquals("home", PixelodyTab.Home.routeId)
        assertEquals("Home", PixelodyTab.Home.label)
        assertEquals("search", PixelodyTab.Search.routeId)
        assertEquals("Search", PixelodyTab.Search.label)
        assertEquals("library", PixelodyTab.Library.routeId)
        assertEquals("Library", PixelodyTab.Library.label)
        assertEquals("create", PixelodyTab.Create.routeId)
        assertEquals("Create", PixelodyTab.Create.label)
        assertEquals("profile", PixelodyTab.Profile.routeId)
        assertEquals("Profile", PixelodyTab.Profile.label)
    }

    @Test
    fun pixelodyTab_testTagsFormat() {
        assertEquals("destination:home", PixelodyTab.Home.navigationTestTag)
        assertEquals("destination:search", PixelodyTab.Search.navigationTestTag)
        assertEquals("destination:library", PixelodyTab.Library.navigationTestTag)
        assertEquals("destination:create", PixelodyTab.Create.navigationTestTag)
        assertEquals("destination:profile", PixelodyTab.Profile.navigationTestTag)
    }
}
