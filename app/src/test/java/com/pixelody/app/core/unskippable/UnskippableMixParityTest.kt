package com.pixelody.app.core.unskippable

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Kotlin's unskippable mixes must match https://github.com/varText-dragen-develiper/pixelody/blob/main/src/unskippable/mix.js case for case. */
class UnskippableMixParityTest {
    private fun fixture(): JSONObject {
        val file = System.getProperty("pixelody.unskippable.fixtures")?.let { File(it, "cases.json") }
            ?: generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
                .map { File(it, "docs/api-contract-fixtures/unskippable-v1/cases.json") }.first { it.isFile }
        return JSONObject(file.readText())
    }

    private fun strings(array: JSONArray?): List<String> = (0 until (array?.length() ?: 0)).map { array!!.getString(it) }

    private fun recipe(json: JSONObject) = UnskippableMix.Recipe(
        style = json.optString("style", "flow"),
        genre = json.opt("genre") as? String,
        targetMinutes = json.optInt("targetMinutes", UnskippableMix.DEFAULT_MINUTES),
        seed = json.optString("seed", "unskippable"),
        includedUntaggedIds = strings(json.optJSONArray("includedUntaggedIds"))
    )

    private fun items(root: JSONObject): List<UnskippableMix.Item> {
        val raw = root.getJSONArray("items")
        return (0 until raw.length()).map { index ->
            val item = raw.getJSONObject(index)
            UnskippableMix.Item(item.getJSONObject("entry"), item.optLong("durationMs", 0L))
        }
    }

    @Test fun everyCaseMatchesTheDesktopGenerator() {
        val root = fixture()
        assertEquals(UnskippableMix.VERSION, root.getInt("version"))
        val items = items(root)
        val cases = root.getJSONArray("cases")
        val previews = HashMap<String, UnskippableMix.Preview>()
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val name = case.getString("name")
            val expected = case.getJSONObject("expected")
            val preview = UnskippableMix.generate(items, recipe(case.getJSONObject("recipe")))
            previews[name] = preview
            assertEquals("$name status", expected.getString("status"), preview.status)
            assertEquals("$name tracks", strings(expected.getJSONArray("trackIds")), preview.trackIds)
            assertEquals("$name neighbours", strings(expected.getJSONArray("neighbourIds")), preview.neighbourIds)
            assertEquals("$name duration", expected.getLong("durationMs"), preview.durationMs)
            val expectedRecipe = expected.getJSONObject("recipe")
            assertEquals("$name genre", expectedRecipe.opt("genreId").takeUnless { it == JSONObject.NULL }, preview.recipe.genreId)
            assertEquals("$name minutes", expectedRecipe.getInt("targetMinutes"), preview.recipe.targetMinutes)
            assertEquals("$name untagged", strings(expectedRecipe.getJSONArray("includedUntaggedIds")), preview.recipe.includedUntaggedIds)
        }
        val swaps = root.getJSONArray("swaps")
        for (index in 0 until swaps.length()) {
            val swap = swaps.getJSONObject(index)
            val from = previews.getValue(swap.getString("from"))
            val swapped = UnskippableMix.swap(items, from, swap.getString("trackId"))
            assertEquals("swap from ${swap.getString("from")}", strings(swap.getJSONArray("expected")), swapped?.trackIds)
        }
    }

    @Test fun catalogCountsUntaggedSongsAndResumeSkipsMissingOnes() {
        val items = items(fixture())
        val catalog = UnskippableMix.genreCatalog(items)
        assertEquals(15, catalog.untaggedCount)
        assertTrue("t7" !in catalog.untaggedTrackIds)
        assertEquals("rock", catalog.genres.first().genreId)
        assertEquals(listOf("hip-hop", "rap"), UnskippableMix.genresOf("Hip-Hop/Rap; hip-hop").map { it.genreId })
        val ids = listOf("a", "b", "c")
        assertEquals(UnskippableMix.ResumePoint(1, "b", 4200), UnskippableMix.resumePoint(ids, "b", 4200))
        assertEquals(UnskippableMix.ResumePoint(2, "c", 0), UnskippableMix.resumePoint(ids, "b", 4200) { it != "b" })
        assertNull(UnskippableMix.resumePoint(ids, null, 0) { false })
    }
}
