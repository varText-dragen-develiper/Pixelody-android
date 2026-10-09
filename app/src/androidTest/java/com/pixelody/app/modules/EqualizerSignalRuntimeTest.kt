package com.pixelody.app.modules

import android.content.ComponentName
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.pixelody.app.core.playback.PixelodyPlaybackService
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.storage.MobileEqualizerStore
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/** Native effect readback through the real service, with silent audio in an isolated package. */
class EqualizerSignalRuntimeTest {
    @Test fun serviceAppliesGlobalTrackFlatAndBypassToActualAudioSession() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".studioqa")) { "Run only in the isolated Studio QA app" }
        val prefs = context.getSharedPreferences(MobileEqualizerStore.PREFERENCES_NAME, 0)
        val saved = prefs.all
        val store = MobileEqualizerStore(context)
        val file = File(context.cacheDir, "eq-native-silence.wav")
        val pcmSize = 44100 * 2
        val bytes = ByteBuffer.allocate(44 + pcmSize).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + pcmSize).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(44100).putInt(88200)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(pcmSize)
        file.writeBytes(bytes.array())
        var controller: MediaController? = null
        fun awaitCondition(description: String, predicate: () -> Boolean) {
            repeat(100) { if (predicate()) return; Thread.sleep(100) }
            fail(description)
        }
        try {
            instrumentation.runOnMainSync {
                store.saveUseMasteringRack(false)
                store.saveSpatialSettings(SpatialChamberSettings(isEnabled = false))
                store.saveTrackProfiles(emptyMap())
                store.saveGlobalProfile(EqualizerProfile(gainsDb = List(5) { 6f }, preset = EqualizerPreset.Custom))
            }
            lateinit var future: ListenableFuture<MediaController>
            instrumentation.runOnMainSync {
                future = MediaController.Builder(context, SessionToken(context,
                    ComponentName(context, PixelodyPlaybackService::class.java))).buildAsync()
            }
            controller = future.get(15, TimeUnit.SECONDS)
            instrumentation.runOnMainSync {
                controller!!.volume = 0f
                controller!!.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                controller!!.setMediaItem(MediaItem.Builder().setMediaId("eq-native-probe")
                    .setUri(Uri.fromFile(file)).build())
                controller!!.prepare(); controller!!.play()
            }
            awaitCondition("Service never attached a native equalizer: ${store.loadRuntimeState()}") {
                store.loadRuntimeState().active && store.loadRuntimeState().audioSessionId > 0
            }
            fun levelsAre(target: Int): Boolean {
                // These are native getter results published by the service, not requested settings.
                val gains = store.loadRuntimeState().appliedGainsDb
                return gains.isNotEmpty() && gains.all { kotlin.math.abs(it * 100f - target) <= 1f }
            }
            awaitCondition("Global EQ did not reach native bands") { levelsAre(600) }
            instrumentation.runOnMainSync {
                store.saveTrackProfiles(mapOf("eq-native-probe" to EqualizerProfile(gainsDb = List(5) { -6f })))
            }
            awaitCondition("Per-track EQ did not replace global native gains") { levelsAre(-600) }
            instrumentation.runOnMainSync {
                store.saveTrackProfiles(mapOf("eq-native-probe" to EqualizerProfile().withPreset(EqualizerPreset.Flat)))
            }
            awaitCondition("Flat reset did not zero all native bands") { levelsAre(0) }
            instrumentation.runOnMainSync {
                store.saveTrackProfiles(mapOf("eq-native-probe" to EqualizerProfile(enabled = false)))
            }
            awaitCondition("EQ bypass left native effect enabled") { !store.loadRuntimeState().active && store.loadRuntimeState().message == "AudioFX Bypassed" }
        } finally {
            instrumentation.runOnMainSync { controller?.stop(); controller?.clearMediaItems(); controller?.release() }
            val editor = prefs.edit().clear()
            saved.forEach { (key, value) -> when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
            } }
            editor.commit()
            file.delete()
        }
    }
}
