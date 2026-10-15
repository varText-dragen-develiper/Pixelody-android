package com.pixelody.app.modules

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class OptionalShopRuntimeTest {
    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findWeb(view.getChildAt(i))?.let { return it }
        return null
    }
    @Test fun downloadedModuleLoadsInIsolatedViewerAndRemovalBlocksReopen() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val stateFile = File(context.filesDir, "modules/listening-notes.json")
        val before = stateFile.takeIf { it.exists() }?.readBytes()
        val store = ModuleStore(context)
        var activity: WebShopActivity? = null
        try {
            store.removeShop()
            assertNull(store.read().shop)
            val savedNotes = store.read().text
            val bytes = URL("https://pixelody-web.pixelody101.workers.dev/modules/revenuecat-shop-1.0.0.pixelody-module").openConnection().apply { connectTimeout=15000; readTimeout=15000 }.getInputStream().use { it.readBytes() }
            assertTrue(bytes.size <= ModulePackage.MAX_BYTES)
            store.install(bytes)
            assertEquals(savedNotes, store.read().text)
            activity = inst.startActivitySync(Intent(context, WebShopActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as WebShopActivity
            var rendered = false
            for (attempt in 0 until 30) {
                val done = CountDownLatch(1)
                inst.runOnMainSync {
                    val view = findWeb(activity!!.window.decorView)
                    if (view == null) done.countDown() else {
                        assertFalse(view.settings.allowFileAccess)
                        assertFalse(view.settings.allowContentAccess)
                        view.evaluateJavascript("document.querySelector('h1')?.textContent || ''") { text ->
                            rendered = text.contains("Small additions")
                            done.countDown()
                        }
                    }
                }
                assertTrue(done.await(5, TimeUnit.SECONDS))
                if (rendered) break
                Thread.sleep(500)
            }
            assertTrue("Live shop HTML must render inside Android WebView", rendered)
            store.removeShop()
            assertNull(store.read().shop)
            assertEquals(savedNotes, store.read().text)
            inst.runOnMainSync { activity!!.recreate() }
            inst.waitForIdleSync()
        } finally {
            inst.runOnMainSync { activity?.finish() }
            if (before != null) { stateFile.parentFile!!.mkdirs(); stateFile.writeBytes(before) }
            else { store.removeShop() }
        }
    }
}
