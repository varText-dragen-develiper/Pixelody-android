package com.pixelody.app.data.model

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowShufflePortableTest {
    @Test
    fun sharedGoldenFixtureMatchesDesktopSeededStandardTraversal() {
        val fixture = JSONObject(fixtureText("flow-shuffle-v1.json"))
        val plan = fixture.getJSONObject("plan")
        val queue = List(plan.getJSONArray("sourceQueueIds").length()) { plan.getJSONArray("sourceQueueIds").getString(it) }
        val expected = List(plan.getJSONArray("order").length()) { plan.getJSONArray("order").getString(it) }
        val actual = FlowShufflePortable.standardOrder(queue, "alpha", plan.getString("seed"))

        assertEquals(1, fixture.getInt("version"))
        assertTrue(fixture.getBoolean("enabled"))
        assertEquals(expected, actual)
        assertEquals("delta", FlowShufflePortable.next(FlowShufflePlan(order = actual, cursor = 0)))
    }

    private fun fixtureText(name: String): String {
        var cursor = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            val candidate = File(cursor, "../docs/api-contract-fixtures/$name").canonicalFile
            if (candidate.isFile) return candidate.readText()
            cursor = cursor.parentFile ?: cursor
        }
        throw IllegalStateException("Could not find shared contract fixture: $name")
    }
}
