package com.pixelody.app.modules

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.pixelody.app.data.storage.PixelodyDatabaseHelper
import com.pixelody.app.data.storage.TrackMetadataOverride
import com.pixelody.app.data.storage.TrackMetadataStore
import org.junit.Assert.*
import org.junit.Test

class DeveloperResetRuntimeTest {
    @Test fun resetClearsOnlyGeneratedDataUnlessTrackEditsAreSelected() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".studioqa")) { "Use only the isolated QA app" }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        fun tap(text: String) {
            val node = device.wait(Until.findObject(By.text(text)), 15000) ?: error("Missing $text")
            val bounds = node.visibleBounds
            device.click(bounds.centerX(), bounds.centerY())
            device.waitForIdle()
        }
        val helper = PixelodyDatabaseHelper(context)
        val db = helper.writableDatabase
        val metadataPrefs = context.getSharedPreferences("pixelody_track_metadata_overrides", 0)
        val oldEdits = metadataPrefs.all
        val protectedId = "developer-reset-protected-fixture"
        try {
            db.execSQL("INSERT OR REPLACE INTO custom_dsp_presets VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any>(protectedId, "Protected", "EQ", "{}", 1L))
            db.execSQL("INSERT OR REPLACE INTO capsule_history VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>("developer-reset-fixture", "test", 1, 1, 1, "Test history", "{}", 1L))
            TrackMetadataStore(context).saveOverride(TrackMetadataOverride("reset-fixture-track", title = "Test edit"))
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://profile"))
                .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            tap("Extras")
            val names = listOf("pixelody_mobile_settings", "pixelody_crates", "pixelody_covers", "pixelody_user_playlists")
            fun preservedPreferences() = names.associateWith { name ->
                context.getSharedPreferences(name, 0).all.filterKeys {
                    it !in setOf("equalizer_runtime_state", "last_track_id", "last_track_position_ms")
                }
            }
            val before = preservedPreferences()
            tap("Reset testing data")
            tap("Cancel")
            assertNotNull(TrackMetadataStore(context).getOverride("reset-fixture-track"))
            tap("Reset testing data")
            tap("Reset data")
            assertTrue(device.wait(Until.hasObject(By.text("Reset complete. Start playback to begin a new test.")), 15000))
            db.rawQuery("SELECT COUNT(*) FROM capsule_history", null).use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            db.rawQuery("SELECT COUNT(*) FROM custom_dsp_presets WHERE id = ?", arrayOf(protectedId)).use {
                it.moveToFirst(); assertEquals(1, it.getInt(0))
            }
            assertEquals(before, preservedPreferences())
            assertNotNull(TrackMetadataStore(context).getOverride("reset-fixture-track"))
            tap("Reset testing data")
            val checkbox = device.wait(Until.findObject(By.desc("Also clear manual track edits")), 10000)
                ?: error("Missing optional track-edit reset")
            checkbox.click()
            tap("Reset data")
            assertTrue(device.wait(Until.hasObject(By.text("Reset complete. Start playback to begin a new test.")), 15000))
            assertTrue(TrackMetadataStore(context).overridesFlow.value.isEmpty())
            assertEquals(before, preservedPreferences())
        } finally {
            db.delete("custom_dsp_presets", "id = ?", arrayOf(protectedId))
            db.delete("capsule_history", "date_key = ?", arrayOf("developer-reset-fixture"))
            val editor = metadataPrefs.edit().clear()
            oldEdits.forEach { (key, value) -> editor.putString(key, value as String) }
            editor.commit()
            helper.close()
        }
    }
}
