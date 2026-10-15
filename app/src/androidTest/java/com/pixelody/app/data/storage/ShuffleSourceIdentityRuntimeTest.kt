package com.pixelody.app.data.storage

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ShuffleSourceIdentityRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun isolated(block: () -> Unit) {
        check(context.packageName == "com.pixelody.app.shuffleqa")
        val prefs = context.getSharedPreferences("pixelody_shuffle_source_identities_v2", 0)
        val original = prefs.getString("sources", null)
        try {
            assertTrue(prefs.edit().putString("sources", "{}").commit())
            block()
        } finally {
            val edit = prefs.edit()
            if (original == null) edit.remove("sources") else edit.putString("sources", original)
            assertTrue(edit.commit())
        }
    }

    @Test fun sourceAndDuplicateEntryIdentitiesSurviveSortingAndTemporaryAbsence() = isolated {
        val first = ShuffleScopeIdentityStore(context).source("local-library", listOf("a", "a", "b"))
        val sorted = ShuffleScopeIdentityStore(context).source("local-library", listOf("b", "a", "a"))
        assertEquals(first.scopeId, sorted.scopeId)
        assertEquals(listOf(first.entryIds[2], first.entryIds[0], first.entryIds[1]), sorted.entryIds)
        assertEquals(3, first.entryIds.toSet().size)
        ShuffleScopeIdentityStore(context).source("local-library", listOf("b"))
        val restored = ShuffleScopeIdentityStore(context).source("local-library", listOf("a", "a", "b"))
        assertEquals(first, restored)
        assertNotEquals(first.scopeId, ShuffleScopeIdentityStore(context).source("playlist:one", listOf("a", "a", "b")).scopeId)
        assertNotEquals(ShuffleScopeIdentityStore(context).source(null, listOf("a")).scopeId,
            ShuffleScopeIdentityStore(context).source(null, listOf("a")).scopeId)
    }

    @Test fun futureMalformedAndCoercedVersionBytesAreNeverOverwritten() = isolated {
        val prefs = context.getSharedPreferences("pixelody_shuffle_source_identities_v2", 0)
        val values = listOf("{", """{"schemaVersion":3,"sources":{}}""", """{"schemaVersion":2.5,"sources":{}}""",
            """{"schemaVersion":"2","sources":{}}""", """{"schemaVersion":true,"sources":{}}""")
        values.forEach { raw ->
            assertTrue(prefs.edit().putString("sources", raw).commit())
            var rejected = false
            try { ShuffleScopeIdentityStore(context).source("local-library", listOf("a")) } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
            assertEquals(raw, prefs.getString("sources", null))
        }
    }

    @Test fun independentInstancesCannotLoseConcurrentSourceIdentityWrites() = isolated {
        val start = CountDownLatch(1)
        val threads = Executors.newFixedThreadPool(4)
        try {
            val jobs = (0 until 20).map { index -> threads.submit<ShuffleScopeIdentityStore.Source> {
                start.await()
                ShuffleScopeIdentityStore(context).source("playlist:$index", listOf("a", "a", "b"))
            } }
            start.countDown()
            val saved = jobs.map { it.get(30, TimeUnit.SECONDS) }
            saved.forEachIndexed { index, source -> assertEquals(source,
                ShuffleScopeIdentityStore(context).source("playlist:$index", listOf("a", "a", "b"))) }
            val prefs = context.getSharedPreferences("pixelody_shuffle_source_identities_v2", 0)
            assertEquals(20, JSONObject(prefs.getString("sources", "{}")!!).getJSONObject("sources").length())
        } finally { threads.shutdownNow() }
    }
}
