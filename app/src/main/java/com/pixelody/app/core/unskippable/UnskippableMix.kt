package com.pixelody.app.core.unskippable

import com.pixelody.app.core.shuffle.UniversalShufflePolicy
import org.json.JSONArray
import org.json.JSONObject

/**
 * An unskippable mix is a fixed tracklist made once, previewed, then saved. Both styles
 * order songs with the shared shuffle core in Flow mode, so a general mix is the same Flow
 * shuffle plays and a one-genre mix gets the same transitions inside its genre.
 *
 * Mirrors https://github.com/varText-dragen-develiper/pixelody/blob/main/src/unskippable/mix.js. Both are held to docs/api-contract-fixtures/unskippable-v1.
 */
object UnskippableMix {
    const val VERSION = 1
    val STYLES = listOf("rigid", "flow")
    val LENGTHS = listOf(30, 60, 120)
    const val MIN_MINUTES = 10
    const val MAX_MINUTES = 600
    const val DEFAULT_MINUTES = 60
    const val UNKNOWN_DURATION_MS = 210000L
    /** One genre: at most one neighbour-genre song for every nine in the genre. */
    const val NEIGHBOUR_EVERY = 9
    private val STOP_TOKENS = setOf("and", "the", "music", "misc", "other")
    private val GENRE_SEPARATORS = Regex("[;,/|]")
    private val SPACES = Regex("\\s+")
    private val NON_TOKEN = Regex("[^a-z0-9]+")

    data class Item(val entry: JSONObject, val durationMs: Long)
    data class Genre(val genreId: String, val label: String)
    data class GenreCount(val genreId: String, val label: String, val count: Int)
    data class Catalog(val genres: List<GenreCount>, val untaggedCount: Int, val untaggedTrackIds: List<String>)
    data class Recipe(
        val style: String = "flow",
        val genre: String? = null,
        val genreId: String? = null,
        val targetMinutes: Int = DEFAULT_MINUTES,
        val seed: String = "unskippable",
        val includedUntaggedIds: List<String> = emptyList(),
        val version: Int = VERSION
    )
    data class Preview(
        val recipe: Recipe,
        val trackIds: List<String>,
        val neighbourIds: List<String>,
        val durationMs: Long,
        val shortfallMs: Long,
        val poolSize: Int = 0,
        val untaggedTrackIds: List<String> = emptyList(),
        val status: String = "ready",
        val swaps: Int = 0
    )
    data class ResumePoint(val index: Int, val trackId: String, val positionMs: Long)

    private class Row(val entry: JSONObject, val trackId: String, val durationMs: Long, val genres: List<Genre>, val artistId: String?)

    fun genresOf(raw: String?): List<Genre> {
        if (raw == null) return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<Genre>()
        for (part in raw.split(GENRE_SEPARATORS)) {
            val label = part.replace(SPACES, " ").trim()
            val genreId = UniversalShufflePolicy.textIdentity(label) ?: continue
            if (seen.add(genreId)) out += Genre(genreId, label)
        }
        return out
    }

    private fun tokens(genreId: String): List<String> =
        genreId.split(NON_TOKEN).filter { it.length >= 3 && it !in STOP_TOKENS }.distinct()

    fun normalizeRecipe(raw: Recipe): Recipe {
        val style = raw.style.takeIf { it in STYLES } ?: "flow"
        val genre = if (style == "rigid") genresOf(raw.genre).firstOrNull() else null
        return Recipe(
            style = style,
            genre = genre?.label, genreId = genre?.genreId,
            targetMinutes = raw.targetMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
            seed = raw.seed.ifEmpty { "unskippable" },
            includedUntaggedIds = if (style == "rigid") raw.includedUntaggedIds.filter { it.isNotEmpty() }.distinct().sortedWith(::ordinal) else emptyList()
        )
    }

    private fun ordinal(left: String, right: String) = UniversalShufflePolicy.ordinal(left, right)

    private fun prepare(items: List<Item>): List<Row> = items.mapNotNull { item ->
        if (item.entry.opt("available") == false) return@mapNotNull null
        val trackId = item.entry.optString("trackId").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val genre = item.entry.opt("genre") as? String
        val artist = item.entry.opt("artist") as? String
        Row(item.entry, trackId, if (item.durationMs > 0) item.durationMs else UNKNOWN_DURATION_MS, genresOf(genre), UniversalShufflePolicy.textIdentity(artist))
    }

    /** Genres to offer for a one-genre mix, most songs first, plus every song with no genre tag. */
    fun genreCatalog(items: List<Item>): Catalog {
        val rows = prepare(items)
        val counts = LinkedHashMap<String, GenreCount>()
        var untagged = 0
        for (row in rows) {
            if (row.genres.isEmpty()) { untagged += 1; continue }
            for (genre in row.genres) {
                val prior = counts[genre.genreId]
                counts[genre.genreId] = prior?.copy(count = prior.count + 1) ?: GenreCount(genre.genreId, genre.label, 1)
            }
        }
        val genres = counts.values.sortedWith { a, b -> if (a.count != b.count) b.count - a.count else ordinal(a.genreId, b.genreId) }
        return Catalog(genres, untagged, rows.filter { it.genres.isEmpty() }.map { it.trackId })
    }

    /** Orders rows with the shuffle core in Flow mode until [stop] says enough or they run out. */
    private fun flowOrder(rows: List<Row>, seed: String, scope: String, stop: ((Row, List<Row>) -> Boolean)?): List<Row> {
        if (rows.isEmpty()) return emptyList()
        val byTrack = rows.associateBy { it.trackId }
        val entries = JSONArray()
        for (row in rows) entries.put(JSONObject(row.entry.toString()).put("entryId", row.trackId).put("trackId", row.trackId))
        var state = UniversalShufflePolicy.createSession(JSONObject()
            .put("sessionId", "unskippable:$scope").put("scopeId", "unskippable:$scope").put("authorityId", "unskippable")
            .put("seed", "$seed\u0000$scope").put("mode", "flow").put("repeat", "off")
            .put("options", JSONObject().put("preset", "flow")).put("entries", entries))
        val taken = ArrayList<Row>()
        for (step in rows.indices) {
            val staged = UniversalShufflePolicy.stage(state, JSONObject()
                .put("attemptId", "unskippable-$step").put("advanceId", "unskippable-$step").put("naturalEnd", true).put("personal", false))
            val pending = staged.optJSONObject("pending") ?: break
            val row = byTrack[pending.getString("trackId")] ?: break
            if (stop != null && stop(row, taken)) break
            taken += row
            state = UniversalShufflePolicy.commit(staged, pending)
        }
        return taken
    }

    private fun totalMs(rows: List<Row>) = rows.sumOf { it.durationMs }
    private fun fillTo(targetMs: Long): (Row, List<Row>) -> Boolean = { _, taken -> totalMs(taken) >= targetMs }

    /** Songs from genres close to the chosen one: genres the same artists play, and genres sharing a word. */
    private fun neighbourRows(rows: List<Row>, genreId: String, coreRows: List<Row>, seed: String): List<Row> {
        val coreIds = coreRows.map { it.trackId }.toSet()
        val coreArtists = coreRows.mapNotNull { it.artistId }.toSet()
        val wanted = tokens(genreId)
        val artistsByGenre = HashMap<String, HashSet<String>>()
        for (row in rows) for (genre in row.genres) {
            if (genre.genreId == genreId || row.artistId == null || row.artistId !in coreArtists) continue
            artistsByGenre.getOrPut(genre.genreId) { HashSet() }.add(row.artistId)
        }
        fun genreScore(id: String) = (artistsByGenre[id]?.size ?: 0) * 2 + tokens(id).count { it in wanted } * 3
        data class Scored(val row: Row, val score: Int, val tie: Long)
        val scored = ArrayList<Scored>()
        for (row in rows) {
            if (row.trackId in coreIds || row.genres.isEmpty()) continue
            val score = row.genres.maxOf { genreScore(it.genreId) }
            if (score > 0) scored += Scored(row, score, UniversalShufflePolicy.hashSeed("$seed\u0000${row.trackId}"))
        }
        return scored.sortedWith { a, b ->
            when {
                a.score != b.score -> b.score - a.score
                a.tie != b.tie -> a.tie.compareTo(b.tie)
                else -> ordinal(a.row.trackId, b.row.trackId)
            }
        }.map { it.row }
    }

    private fun coreRowsFor(rows: List<Row>, recipe: Recipe): List<Row> {
        val included = recipe.includedUntaggedIds.toSet()
        return rows.filter { row -> row.genres.any { it.genreId == recipe.genreId } || (row.genres.isEmpty() && row.trackId in included) }
    }

    private fun result(recipe: Recipe, picked: List<Row>, neighbours: List<Row>, poolSize: Int, untagged: List<String>, status: String): Preview {
        val durationMs = totalMs(picked)
        return Preview(recipe, picked.map { it.trackId }, neighbours.map { it.trackId }, durationMs,
            maxOf(0L, recipe.targetMinutes * 60000L - durationMs), poolSize, untagged, status)
    }

    fun generate(items: List<Item>, rawRecipe: Recipe): Preview {
        val recipe = normalizeRecipe(rawRecipe)
        val rows = prepare(items)
        val targetMs = recipe.targetMinutes * 60000L
        val untagged = rows.filter { it.genres.isEmpty() }.map { it.trackId }
        if (recipe.style == "flow") {
            val picked = flowOrder(rows, recipe.seed, "flow", fillTo(targetMs))
            return result(recipe, picked, emptyList(), rows.size, untagged, if (picked.isEmpty()) "empty-library" else "ready")
        }
        val genreId = recipe.genreId ?: return result(recipe, emptyList(), emptyList(), 0, untagged, "needs-genre")
        val coreRows = coreRowsFor(rows, recipe)
        if (coreRows.isEmpty()) return result(recipe, emptyList(), emptyList(), 0, untagged, "no-genre-songs")
        val neighbourBudgetMs = targetMs / 10
        val chosen = flowOrder(coreRows, recipe.seed, "core", fillTo(targetMs - neighbourBudgetMs))
        val allowed = chosen.size / NEIGHBOUR_EVERY
        val neighbours = ArrayList<Row>()
        var total = totalMs(chosen)
        for (row in neighbourRows(rows, genreId, coreRows, recipe.seed)) {
            if (neighbours.size >= allowed || total >= targetMs) break
            neighbours += row
            total += row.durationMs
        }
        // The neighbour cap can leave room; the genre itself fills it first.
        val used = chosen.map { it.trackId }.toSet()
        val more = flowOrder(coreRows.filter { it.trackId !in used }, recipe.seed, "core-fill", fillTo(targetMs - total))
        val picked = flowOrder(chosen + more + neighbours, recipe.seed, "order", null)
        return result(recipe, picked, neighbours, coreRows.size, untagged, "ready")
    }

    /** Swaps one song for the best unused one: same genre for a one-genre mix (a neighbour only for a neighbour). */
    fun swap(items: List<Item>, preview: Preview, trackId: String): Preview? {
        val recipe = normalizeRecipe(preview.recipe)
        val index = preview.trackIds.indexOf(trackId)
        if (index < 0) return null
        val rows = prepare(items)
        val byTrack = rows.associateBy { it.trackId }
        val inMix = preview.trackIds.toSet()
        val pool = if (recipe.style == "rigid") {
            val coreRows = coreRowsFor(rows, recipe)
            if (trackId in preview.neighbourIds) neighbourRows(rows, recipe.genreId.orEmpty(), coreRows, recipe.seed) else coreRows
        } else rows
        val swaps = preview.swaps + 1
        val replacement = flowOrder(pool.filter { it.trackId !in inMix }, recipe.seed, "swap:$swaps") { _, taken -> taken.isNotEmpty() }.firstOrNull() ?: return null
        val trackIds = preview.trackIds.toMutableList().also { it[index] = replacement.trackId }
        val neighbourIds = preview.neighbourIds.map { if (it == trackId) replacement.trackId else it }
        val durationMs = trackIds.sumOf { byTrack[it]?.durationMs ?: UNKNOWN_DURATION_MS }
        return preview.copy(recipe = recipe, trackIds = trackIds, neighbourIds = neighbourIds, swaps = swaps, durationMs = durationMs,
            shortfallMs = maxOf(0L, recipe.targetMinutes * 60000L - durationMs))
    }

    fun remove(items: List<Item>, preview: Preview, trackId: String): Preview? {
        if (trackId !in preview.trackIds) return null
        val recipe = normalizeRecipe(preview.recipe)
        val byTrack = prepare(items).associateBy { it.trackId }
        val trackIds = preview.trackIds.filter { it != trackId }
        val durationMs = trackIds.sumOf { byTrack[it]?.durationMs ?: UNKNOWN_DURATION_MS }
        return preview.copy(recipe = recipe, trackIds = trackIds, neighbourIds = preview.neighbourIds.filter { it != trackId },
            durationMs = durationMs, shortfallMs = maxOf(0L, recipe.targetMinutes * 60000L - durationMs))
    }

    /** Where a saved mix picks up. A song that has since gone missing moves on to the next one still there. */
    fun resumePoint(trackIds: List<String>, progressTrackId: String?, progressPositionMs: Long, available: (String) -> Boolean = { true }): ResumePoint? {
        var index = progressTrackId?.let { trackIds.indexOf(it) } ?: -1
        var positionMs = if (index >= 0 && progressPositionMs > 0) progressPositionMs else 0L
        if (index < 0) { index = 0; positionMs = 0L }
        while (index < trackIds.size && !available(trackIds[index])) { index += 1; positionMs = 0L }
        return if (index < trackIds.size) ResumePoint(index, trackIds[index], positionMs) else null
    }
}
