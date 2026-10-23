package com.pixelody.app.core.shuffle

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Exact JSON reference port of https://github.com/varText-dragen-develiper/pixelody/blob/main/src/shuffle/universal.js, universal-shuffle-v2.2.
 * Pure: no clock, randomness, IO, Android classes, or implicit device authority.
 * Treat returned JSON as immutable; adapters serialize calls and own activation.
 */
object UniversalShufflePolicy {
    const val VERSION = 3
    const val POLICY_VERSION = "universal-shuffle-v2.2"
    const val PRIOR_POLICY_VERSION = "universal-shuffle-v2.1"
    const val CONFIG_VERSION = 2
    const val MAX_SAFE_INTEGER = 9007199254740991L
    val MODES = listOf("off", "balanced", "flow", "album")
    val LANES = listOf("base", "live")
    val SKIPS = listOf("flick", "early", "mid", "late", "finish", "unknown")
    val PRESETS = listOf("flow", "discovery", "comfort", "energy", "genre-weave", "deep-library")
    val PROVENANCE = listOf("manual", "tagged", "measured", "derived", "unknown")
    // Two-queue session steering. The base queue is what a natural end plays;
    // the live queue is what a personal Next plays.
    private const val PINNED_BASE = 3
    private const val LIVE_STEER = 24L
    private const val BASE_STEER = 8L
    private const val MAX_MARKS = 8
    private const val MARK_MAX_SONGS = 8L
    private const val MARK_MAX_AGE_MS = 7200000L
    private const val FLICK_MS = 3000L
    private const val EARLY_SKIP_MAX_MS = 30000L
    private const val EARLY_SKIP_COVERAGE = 250L
    private const val LATE_SKIP_COVERAGE = 800L
    private val MARK_VALUES = mapOf("early" to -1000L, "mid" to -400L, "late" to 200L, "finish" to 600L)
    private const val PRUNE_SIMILARITY = 600L
    private const val EXPLORATION = 100L
    private const val FLICK_EXPLORATION = 250L
    private const val FLICK_STORM_EXPLORATION = 400L
    private const val FLOW_LET_GO_AFTER = 3L
    private const val LIVE_EXCLUSION_MINIMUM = 3
    private const val MAX_AIRTIME = 12L
    val CONFIG get() = obj("version" to CONFIG_VERSION, "horizon" to 10, "maxHorizon" to 32, "maxFailures" to 64,
        "artistWindow" to 3, "albumWindow" to 2, "recordingWindow" to 1, "explorationPermille" to EXPLORATION,
        "baseWeight" to 64, "minWeight" to 16, "maxWeight" to 128, "maxTaste" to 12, "maxNovelty" to 12,
        "freshnessMs" to 86400000, "maxFreshnessPenalty" to 12, "maxTempoPoints" to 8,
        "maxEnergyPoints" to 8, "maxKeyPoints" to 4, "maxContextPoints" to 4, "maxPresetPoints" to 8,
        "pinnedBase" to PINNED_BASE, "liveSteer" to LIVE_STEER, "baseSteer" to BASE_STEER, "maxMarks" to MAX_MARKS, "markMaxSongs" to MARK_MAX_SONGS, "markMaxAgeMs" to MARK_MAX_AGE_MS,
        "flickMs" to FLICK_MS, "earlySkipMaxMs" to EARLY_SKIP_MAX_MS, "earlySkipCoveragePermille" to EARLY_SKIP_COVERAGE, "lateSkipCoveragePermille" to LATE_SKIP_COVERAGE,
        "earlyMark" to -1000, "midMark" to -400, "lateMark" to 200, "finishMark" to 600, "pruneSimilarity" to PRUNE_SIMILARITY,
        "flickExplorationPermille" to FLICK_EXPLORATION, "flickStormExplorationPermille" to FLICK_STORM_EXPLORATION, "flowLetGoAfter" to FLOW_LET_GO_AFTER, "liveExclusionMinimum" to LIVE_EXCLUSION_MINIMUM,
        "maxAirtimePoints" to MAX_AIRTIME, "similarityArtist" to 350, "similarityAlbum" to 250, "similarityGenre" to 150,
        "similarityTempoClose" to 100, "similarityTempoNear" to 50, "similarityEnergy" to 100, "similarityKey" to 50, "similaritySource" to 200)
    private const val RNG_ALGORITHM = "fnv1a-utf16-mulberry32-fy-v1"
    private val whitespace = (listOf(9, 10, 11, 12, 13, 32, 160, 5760) + (8192..8202) +
        listOf(8232, 8233, 8239, 8287, 12288, 65279)).map { it.toChar() }.toSet()
    private const val WS = "[\\u0009-\\u000d \\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]"

    private fun obj(vararg fields: Pair<String, Any?>): JSONObject = JSONObject().apply { fields.forEach { put(it.first, it.second ?: JSONObject.NULL) } }
    private fun array(values: Iterable<Any?> = emptyList()): JSONArray = JSONArray().apply { values.forEach { put(it ?: JSONObject.NULL) } }
    private fun JSONObject.v(key: String): Any? = opt(key).takeUnless { it === JSONObject.NULL }
    private fun JSONObject.s(key: String): String? = v(key) as? String
    private fun JSONObject.l(key: String): Long = (v(key) as? Number)?.toLong() ?: 0
    private fun JSONObject.o(key: String): JSONObject? = v(key) as? JSONObject
    private fun JSONArray.items(): List<Any?> = (0 until length()).map { opt(it).takeUnless { value -> value === JSONObject.NULL } }
    private fun JSONObject.a(key: String): List<Any?> = (v(key) as? JSONArray)?.items() ?: emptyList()
    private fun JSONObject.os(key: String): List<JSONObject> = a(key).map { it as JSONObject }
    private fun cp(base: JSONObject, vararg changes: Pair<String, Any?>): JSONObject = JSONObject().apply {
        base.keys().forEach { put(it, base.get(it)) }; changes.forEach { put(it.first, it.second ?: JSONObject.NULL) }
    }
    private fun merge(base: JSONObject, changes: JSONObject): JSONObject = cp(base).apply { changes.keys().forEach { put(it, changes.get(it)) } }
    private fun deep(value: Any?): Any? = when (value) { is JSONObject -> JSONObject(value.toString()); is JSONArray -> JSONArray(value.toString()); else -> value }
    private fun safe(value: Any?): Boolean = value is Number && value.toDouble().isFinite() &&
        abs(value.toDouble()) <= MAX_SAFE_INTEGER.toDouble() && floor(value.toDouble()) == value.toDouble()
    private fun integer(value: Any?, low: Long, high: Long, fallback: Long = low): Long = if (safe(value)) (value as Number).toLong().coerceIn(low, high) else fallback
    private fun optionalInteger(value: Any?, low: Long, high: Long): Long? = if (safe(value)) integer(value, low, high) else null
    private fun same(left: Any?, right: Any?): Boolean = if (left is Number && right is Number) left.toDouble() == right.toDouble() else left == right
    private fun truthy(value: Any?): Boolean = when (value) { null -> false; is Boolean -> value; is String -> value.isNotEmpty(); is Number -> value.toDouble() != 0.0 && !value.toDouble().isNaN(); else -> true }
    private fun jsString(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is String -> value
        is JSONObject -> "[object Object]"
        is JSONArray -> value.items().joinToString(",") { if (it == null) "" else jsString(it) }
        else -> canonicalJson(value)
    }
    fun ordinal(left: String, right: String): Int = left.compareTo(right).coerceIn(-1, 1)

    fun canonicalJson(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { quote(it) + ":" + canonicalJson(value.v(it)) }
        is JSONArray -> value.items().joinToString(",", "[", "]") { canonicalJson(it) }
        is String -> quote(value)
        is Boolean -> value.toString()
        is Number -> {
            val number = value.toDouble()
            when {
                !number.isFinite() -> "null"
                number == 0.0 -> "0"
                floor(number) == number && abs(number) <= MAX_SAFE_INTEGER -> number.toLong().toString()
                abs(number) >= 1e-6 && abs(number) < 1e21 -> BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
                else -> BigDecimal(value.toString()).stripTrailingZeros().toString().replace("E", "e")
            }
        }
        else -> throw IllegalArgumentException("Unsupported JSON value")
    }
    private fun quote(value: String): String = buildString {
        append('"')
        value.forEachIndexed { index, ch -> when (ch) {
            '"' -> append("\\\""); '\\' -> append("\\\\"); '\b' -> append("\\b"); '\u000c' -> append("\\f")
            '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> if (ch.code < 32 || ch.isHighSurrogate() && (index + 1 >= value.length || !value[index + 1].isLowSurrogate()) ||
                ch.isLowSurrogate() && (index == 0 || !value[index - 1].isHighSurrogate())) append("\\u" + ch.code.toString(16).padStart(4, '0')) else append(ch)
        } }; append('"')
    }
    private fun id(value: Any?, name: String = "id"): String {
        require(value is String && value.length in 1..1024) { "Invalid $name" }
        value.forEachIndexed { index, ch -> require(!(ch.isHighSurrogate() && (index + 1 >= value.length || !value[index + 1].isLowSurrogate()) ||
            ch.isLowSurrogate() && (index == 0 || !value[index - 1].isHighSurrogate()))) { "Invalid $name" } }
        return value
    }
    private fun optionalId(value: Any?): String? = if (value == null || value == "") null else id(value)
    fun textIdentity(value: Any?): String? {
        if (value !is String) return null
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFC)
        val result = StringBuilder()
        var space = false
        for (ch in normalized) {
            if (ch in whitespace) { space = result.isNotEmpty(); continue }
            if (space) result.append(' ')
            space = false
            result.append(if (ch in 'A'..'Z') ch + 32 else ch)
        }
        return result.toString().takeIf { it.isNotEmpty() }
    }
    fun normalizeMode(value: Any?): String = when (value) {
        in MODES -> value as String
        // Random shuffle was removed in v2.2. Balanced keeps its once-per-cycle promise.
        "random", "standard", "Standard", "PureRandom" -> "balanced"
        "SmartFlow", "Flow" -> "flow"
        "AlbumPreserving" -> "album"
        else -> "balanced"
    }
    fun hashSeed(value: Any?): Long {
        val seed = jsString(value)
        var hash = 2166136261L.toInt()
        seed.forEach { hash = (hash xor it.code) * 16777619 }
        return hash.toLong() and 0xffffffffL
    }
    fun createRng(seed: Any?): JSONObject {
        require(seed is String) { "Seed must be explicitly injected" }
        val hash = hashSeed(seed)
        return obj("algorithm" to RNG_ALGORITHM, "state" to if (hash == 0L) 0x6d2b79f5L else hash, "draws" to 0, "seedHash" to hash)
    }
    fun draw(rng: JSONObject): JSONObject {
        val state = (rng.l("state") + 0x6d2b79f5L).toInt()
        var value = (state xor (state ushr 15)) * (state or 1)
        value = value xor (value + (value xor (value ushr 7)) * (value or 61))
        return obj("value" to ((value xor (value ushr 14)).toLong() and 0xffffffffL),
            "rng" to cp(rng, "state" to (state.toLong() and 0xffffffffL), "draws" to rng.l("draws") + 1))
    }
    fun bounded(rng: JSONObject, bound: Long): JSONObject {
        require(bound in 1..128000000) { "Invalid draw bound" }
        val next = draw(rng)
        return obj("value" to ((next.l("value") * bound) ushr 32), "rng" to next.o("rng"))
    }
    fun permutation(values: JSONArray, rng: JSONObject): JSONObject {
        val order = values.items().toMutableList()
        var current = rng
        for (index in order.lastIndex downTo 1) {
            val next = bounded(current, index + 1L); current = next.o("rng")!!
            val picked = next.l("value").toInt(); val old = order[index]; order[index] = order[picked]; order[picked] = old
        }
        return obj("order" to array(order), "rng" to current)
    }
    fun parseKey(value: Any?): JSONObject? {
        if (value is JSONObject && safe(value.v("pitchClass")) && value.l("pitchClass") in 0..11 && value.s("mode") in listOf("major", "minor"))
            return obj("pitchClass" to value.v("pitchClass"), "mode" to value.v("mode"))
        if (value !is String) return null
        val raw = value.trim { it in whitespace }.replace('♯', '#').replace('♭', 'b')
        Regex("^(1[0-2]|[1-9])([AB])$", RegexOption.IGNORE_CASE).matchEntire(raw)?.let {
            val minor = it.groupValues[2].uppercase(Locale.ROOT) == "A"
            val notes = if (minor) listOf(8, 3, 10, 5, 0, 7, 2, 9, 4, 11, 6, 1) else listOf(11, 6, 1, 8, 3, 10, 5, 0, 7, 2, 9, 4)
            return obj("pitchClass" to notes[it.groupValues[1].toInt() - 1], "mode" to if (minor) "minor" else "major")
        }
        val match = Regex("^([A-Ga-g])([#b]?)(?:$WS*(major|maj|minor|min|m))?$", RegexOption.IGNORE_CASE).matchEntire(raw) ?: return null
        val pitch = mapOf("C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11).getValue(match.groupValues[1].uppercase(Locale.ROOT))
        val accidental = when (match.groupValues[2]) { "#" -> 1; "b", "B" -> -1; else -> 0 }
        val mode = match.groupValues[3]
        return obj("pitchClass" to (pitch + accidental + 12) % 12, "mode" to if (mode != "M" && mode.lowercase(Locale.ROOT) in listOf("minor", "min", "m")) "minor" else "major")
    }
    private fun feature(raw: Any?, kind: String): JSONObject {
        val item = if (raw is JSONObject && raw.has("value")) raw else obj("value" to raw, "provenance" to "unknown")
        val provenance = item.s("provenance").takeIf { it in PROVENANCE } ?: "unknown"
        var value: Any? = null
        if (provenance != "unknown" && item.v("value") != null) {
            if (kind == "key") value = parseKey(item.v("value"))
            else (item.v("value") as? Number)?.toDouble()?.takeIf { it.isFinite() }?.let { rawNumber ->
                val scale = if (item.s("unit") == if (kind == "tempo") "milli-bpm" else "permille") 1 else 1000
                val quantized = floor(rawNumber * scale + 0.5)
                if (kind == "tempo" && quantized in 20000.0..400000.0 || kind == "energy" && quantized in 0.0..1000.0) value = quantized.toLong()
            }
        }
        return obj("value" to value, "provenance" to if (value == null) "unknown" else provenance,
            "version" to integer(item.v("version"), 1, 100000, 1),
            "confidencePermille" to if (value == null) null else integer(item.v("confidencePermille"), 0, 1000, if (provenance in listOf("manual", "tagged")) 1000 else 500),
            "unit" to when (kind) { "key" -> "pitch-class-mode"; "tempo" -> "milli-bpm"; else -> "permille" })
    }
    fun normalizeEntry(raw: JSONObject, index: Int = 0): JSONObject {
        val artistId = optionalId(raw.v("artistId")) ?: textIdentity(raw.v("artist"))
        val albumArtistId = optionalId(raw.v("albumArtistId")) ?: textIdentity(raw.v("albumArtist"))
        val albumName = textIdentity(raw.v("album"))
        val albumId = optionalId(raw.v("albumId")) ?: albumName?.let { canonicalJson(array(listOf(albumArtistId ?: artistId, it))) }
        val isrc = raw.s("isrc")?.filter { it != '-' && it !in whitespace }?.uppercase(Locale.ROOT).orEmpty()
        val recordingId = optionalId(raw.v("recordingId")) ?: isrc.takeIf { Regex("^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$").matches(it) }?.let { "isrc:$it" }
        val features = raw.o("features") ?: JSONObject()
        return obj("entryId" to id(raw.v("entryId"), "entryId"), "trackId" to id(raw.v("trackId"), "trackId"), "recordingId" to recordingId,
            "artistId" to artistId, "albumArtistId" to albumArtistId, "albumId" to albumId, "sourceKey" to optionalId(raw.v("sourceKey")), "sourceIndex" to index,
            "discNumber" to optionalInteger(raw.v("discNumber"), 1, 10000), "trackNumber" to optionalInteger(raw.v("trackNumber"), 1, 100000), "available" to (raw.v("available") != false),
            "genreId" to (optionalId(raw.v("genreId")) ?: textIdentity(raw.v("genre"))),
            "contextTags" to array(raw.a("contextTags").mapNotNull(::textIdentity).distinct().sorted()),
            "features" to obj("tempo" to feature(features.v("tempo") ?: features.v("bpm"), "tempo"),
                "energy" to feature(features.v("energy"), "energy"), "key" to feature(features.v("key"), "key")))
    }
    fun normalizeEntries(entries: JSONArray): JSONArray {
        require(entries.length() <= 1000000) { "Entries must be an array of at most 1,000,000 occurrences" }
        val normalized = entries.items().mapIndexed { index, raw -> require(raw is JSONObject) { "Entry must be an object" }; normalizeEntry(raw, index) }
        require(normalized.map { it.s("entryId") }.distinct().size == normalized.size) { "Duplicate entryId" }
        return array(normalized)
    }
    private fun normalizeOptions(options: JSONObject = JSONObject()): JSONObject = obj(
        "preset" to (options.s("preset").takeIf { it in PRESETS } ?: "flow"),
        "shape" to (options.s("shape").takeIf { it in listOf("steady", "rise", "wave", "wind-down", "auto") } ?: "auto"),
        "preserveAlbumRuns" to (options.v("preserveAlbumRuns") == true), "contextTag" to textIdentity(options.v("contextTag")),
        "airtime" to if (options.v("airtime") == "even") "even" else "size")
    private fun emptyLanes(): JSONObject = obj("marks" to JSONArray(), "flicks" to 0, "earlyRun" to 0, "anchorEntryId" to null, "lastEarlyAlbumId" to null, "pinned" to JSONArray())
    fun createSession(input: JSONObject): JSONObject {
        val rawEntries = input.v("entries").takeIf(::truthy) ?: JSONArray()
        require(rawEntries is JSONArray) { "Entries must be an array of at most 1,000,000 occurrences" }
        val entries = normalizeEntries(rawEntries)
        return obj("schemaVersion" to VERSION, "policyVersion" to POLICY_VERSION, "configVersion" to CONFIG_VERSION,
            "sessionId" to id(input.v("sessionId"), "sessionId"), "scopeId" to id(input.v("scopeId"), "scopeId"), "authorityId" to id(input.v("authorityId"), "authorityId"),
            "authorityEpoch" to integer(input.v("authorityEpoch"), 0, MAX_SAFE_INTEGER, 0), "revision" to 0,
            "membershipRevision" to integer(input.v("membershipRevision"), 0, MAX_SAFE_INTEGER, 0), "mode" to normalizeMode(input.v("mode")),
            "repeat" to (input.s("repeat").takeIf { it in listOf("off", "all", "one") } ?: "off"), "options" to normalizeOptions(input.o("options") ?: JSONObject()),
            "cycle" to obj("generation" to 0, "cycleId" to canonicalJson(array(listOf(input.v("scopeId"), 0)))), "rng" to createRng(input.v("seed")), "entries" to entries,
            "remaining" to array(entries.items().map { (it as JSONObject).v("entryId") }), "history" to JSONArray(), "cursor" to -1, "occurrenceSequence" to 0,
            "generatedFuture" to JSONArray(), "manual" to JSONArray(), "exclusions" to JSONArray(), "albumLock" to null, "pending" to null,
            "lastConfirmedPositionMs" to 0, "eventSequences" to JSONArray(), "quarantine" to quarantine(), "undo" to null, "paused" to true, "diagnostics" to JSONArray(), "imported" to false, "lanes" to emptyLanes())
    }
    private fun requireState(state: JSONObject): JSONObject {
        require(same(state.v("schemaVersion"), VERSION) && state.s("policyVersion") == POLICY_VERSION && same(state.v("configVersion"), CONFIG_VERSION)) {
            "Unsupported session version; preserve snapshot without overwriting it" }
        return state
    }
    private fun quarantine(advanceId: Any? = null, entries: List<Any?> = emptyList()): JSONObject = obj("advanceId" to advanceId, "entryIds" to array(entries))
    fun currentOccurrence(state: JSONObject): JSONObject? = state.os("history").getOrNull(state.l("cursor").toInt())
    private fun albumOrder(entries: List<JSONObject>): Pair<List<JSONObject>, String> {
        val known = entries.all { it.v("discNumber") != null && it.v("trackNumber") != null }
        return if (known) entries.sortedWith(compareBy<JSONObject> { it.l("discNumber") }.thenBy { it.l("trackNumber") }.thenBy { it.l("sourceIndex") }.thenBy { it.s("entryId") }) to "disc-track"
        else entries.sortedBy { it.l("sourceIndex") } to "source-order-missing-numbers"
    }
    private fun shapeTarget(shape: String?, position: Int, base: Long): Long {
        val step = position % 10
        return when (shape) { "rise" -> min(1000, base + step * 30); "wind-down" -> max(0, base - step * 30)
            "wave" -> (base + listOf(0, 60, 120, 180, 120, 60, 0, -60, -120, -60)[step]).coerceIn(0, 1000); else -> base }
    }
    private fun keyPoints(a: JSONObject, b: JSONObject): Long {
        val difference = (b.l("pitchClass") - a.l("pitchClass") + 12) % 12
        return when { difference == 0L && a.s("mode") == b.s("mode") -> 4L
            difference in listOf(5L, 7L) && a.s("mode") == b.s("mode") || a.s("mode") == "major" && b.s("mode") == "minor" && difference == 9L || a.s("mode") == "minor" && b.s("mode") == "major" && difference == 3L -> 2L; else -> -2L }
    }
    private fun featureValue(entry: JSONObject, kind: String): Any? = entry.o("features")?.o(kind)?.v("value")
    /** Session closeness 0..1000 from metadata already read. Unknown traits add nothing. */
    fun similarity(a: JSONObject?, b: JSONObject?, multiSource: Boolean): Long {
        if (a == null || b == null) return 0
        var points = 0L
        if (a.s("artistId") != null && a.s("artistId") == b.s("artistId")) points += 350
        if (a.s("albumId") != null && a.s("albumId") == b.s("albumId")) points += 250
        if (a.s("genreId") != null && a.s("genreId") == b.s("genreId")) points += 150
        if (featureValue(a, "tempo") != null && featureValue(b, "tempo") != null) {
            val distance = abs(a.o("features")!!.o("tempo")!!.l("value") - b.o("features")!!.o("tempo")!!.l("value"))
            points += if (distance <= 5000) 100 else if (distance <= 10000) 50 else 0
        }
        if (featureValue(a, "energy") != null && featureValue(b, "energy") != null && abs(a.o("features")!!.o("energy")!!.l("value") - b.o("features")!!.o("energy")!!.l("value")) <= 100) points += 100
        if (featureValue(a, "key") != null && featureValue(b, "key") != null && keyPoints(a.o("features")!!.o("key")!!.o("value")!!, b.o("features")!!.o("key")!!.o("value")!!) > 0) points += 50
        if (multiSource && a.s("sourceKey") != null && a.s("sourceKey") == b.s("sourceKey")) points += 200
        return min(1000, points)
    }
    private class Airtime(val sources: Long, val played: Map<String, Long>, val totalPlayed: Long)
    private class Steering(val gain: Long = 0, val marks: List<JSONObject> = emptyList(), val ageOffset: Long = 0, val exploration: Long = EXPLORATION, val airtime: Airtime? = null, val multiSource: Boolean = false)
    /** A mark keeps full strength for two songs, then halves every two songs. */
    private fun steerPoints(entry: JSONObject, steering: Steering, byId: Map<String?, JSONObject>): Long {
        if (steering.gain == 0L || steering.marks.isEmpty()) return 0
        var sum = 0L
        for (mark in steering.marks) {
            val marked = byId[mark.s("entryId")] ?: continue
            val age = mark.l("songs") + steering.ageOffset
            if (age >= MARK_MAX_SONGS) continue
            sum += mark.l("mark") * similarity(entry, marked, steering.multiSource) / (1L shl (age / 2).toInt())
        }
        return (steering.gain * sum / 1000000).coerceIn(-steering.gain, steering.gain)
    }
    /** Even turns lifts linked members behind their share of this cycle and lowers those ahead. */
    private fun airtimePoints(entry: JSONObject, airtime: Airtime?): Long {
        val source = entry.s("sourceKey")
        if (airtime == null || source == null) return 0
        val played = airtime.played[source] ?: 0
        return ((airtime.totalPlayed - airtime.sources * played) * MAX_AIRTIME / max(airtime.sources, airtime.totalPlayed)).coerceIn(-MAX_AIRTIME, MAX_AIRTIME)
    }
    private fun airtimeContext(state: JSONObject, consumed: (Any?) -> Boolean): Airtime? {
        if (state.o("options")!!.s("airtime") != "even") return null
        val played = linkedMapOf<String, Long>(); val sources = linkedSetOf<String>()
        var total = 0L
        for (entry in state.os("entries")) {
            val source = entry.s("sourceKey") ?: continue
            sources.add(source)
            if (consumed(entry.v("entryId"))) { played[source] = (played[source] ?: 0) + 1; total++ }
        }
        return if (sources.size < 2) null else Airtime(sources.size.toLong(), played, total)
    }
    private fun selection(state: JSONObject, eligible: List<JSONObject>, context: JSONObject, current: JSONObject?, byId: Map<String?, JSONObject>, steering: Steering = Steering()): JSONObject {
        var rng = state.o("rng")!!
        val cursorIndex = state.l("cursor").toInt()
        val recent = state.os("history").subList(max(0, cursorIndex - 2), cursorIndex + 1).map { byId[it.s("entryId")] ?: it }
        val windows = mapOf("artist" to recent.takeLast(3).mapNotNull { it.s("artistId") }, "album" to recent.takeLast(2).mapNotNull { it.s("albumId") }, "recording" to recent.takeLast(1).mapNotNull { it.s("recordingId") })
        val relaxed = mutableListOf<String>()
        fun feasible() = eligible.filter { entry -> listOf("artist", "album", "recording").all { kind -> kind in relaxed || entry.s(kind + "Id") == null || entry.s(kind + "Id") !in windows.getValue(kind) } }
        var candidates = feasible()
        for (kind in listOf("album", "artist", "recording")) { if (candidates.isNotEmpty()) break; relaxed.add(kind); candidates = feasible() }
        candidates = candidates.sortedBy { it.s("entryId") }
        val signals = context.os("signals").associateBy { it.s("trackId") }
        val now = integer(context.v("nowMs"), 0, MAX_SAFE_INTEGER, 0)
        val options = state.o("options")!!
        val rows = candidates.map { entry ->
            val signal = signals[entry.s("trackId")] ?: JSONObject()
            val parts = linkedMapOf("taste" to integer(signal.v("tastePoints"), -12, 12, 0), "novelty" to integer(signal.v("noveltyPoints"), 0, 12, 12),
                "freshness" to 0L, "tempo" to 0L, "energy" to 0L, "key" to 0L, "context" to 0L, "preset" to 0L,
                "steer" to steerPoints(entry, steering, byId), "airtime" to airtimePoints(entry, steering.airtime), "adapter" to integer(signal.v("adapterPoints"), -12, 12, 0))
            if (safe(signal.v("lastHeardAtMs")) && signal.l("lastHeardAtMs") >= 0) parts["freshness"] = -floor(12.0 * max(0, 86400000L - max(0, now - signal.l("lastHeardAtMs"))) / 86400000).toLong()
            val coverage = mutableListOf<String>()
            if (safe(signal.v("tastePoints")) && (signal.l("tastePoints") != 0L || (signal.v("confidencePermille") as? Number)?.toDouble()?.let { it > 0 } == true)) coverage.add("taste")
            if (safe(signal.v("noveltyPoints"))) coverage.add("novelty")
            if (safe(signal.v("lastHeardAtMs")) && signal.l("lastHeardAtMs") >= 0) coverage.add("freshness")
            if (state.s("mode") == "flow") {
                val from = current?.o("features"); val to = entry.o("features")!!
                if (from?.o("tempo")?.v("value") != null && to.o("tempo")!!.v("value") != null) {
                    parts["tempo"] = max(-8, 8 - abs(from.o("tempo")!!.l("value") - to.o("tempo")!!.l("value")) / 5000); coverage.add("tempo")
                }
                if (from?.o("energy")?.v("value") != null && to.o("energy")!!.v("value") != null) {
                    val target = shapeTarget(options.s("shape"), state.a("history").size, from.o("energy")!!.l("value"))
                    parts["energy"] = max(-8, 8 - abs(target - to.o("energy")!!.l("value")) / 50); coverage.add("energy")
                }
                if (from?.o("key")?.o("value") != null && to.o("key")!!.o("value") != null) {
                    parts["key"] = keyPoints(from.o("key")!!.o("value")!!, to.o("key")!!.o("value")!!); coverage.add("key")
                }
                if (options.s("contextTag") != null && options.s("contextTag") in entry.a("contextTags")) { parts["context"] = 4; coverage.add("context") }
                when (options.s("preset")) {
                    "discovery", "deep-library" -> parts["preset"] = parts.getValue("novelty") * 8 / 12
                    "comfort" -> parts["preset"] = -(parts.getValue("novelty") * 5 / 12)
                    "energy" -> if (to.o("energy")!!.v("value") != null) { parts["preset"] = to.o("energy")!!.l("value") * 8 / 1000; if ("energy" !in coverage) coverage.add("energy") }
                    "genre-weave" -> if (current?.s("genreId") != null && entry.s("genreId") != null) { parts["preset"] = if (current.s("genreId") == entry.s("genreId")) -4 else 4; coverage.add("genre") }
                }
            }
            if (parts.getValue("steer") != 0L) coverage.add("steer")
            if (parts.getValue("airtime") != 0L) coverage.add("airtime")
            if (parts.getValue("adapter") != 0L) coverage.add("adapter")
            obj("entry" to entry, "parts" to obj(*parts.map { it.key to it.value }.toTypedArray()), "coverage" to array(coverage), "weight" to (64 + parts.values.sum()).coerceIn(16, 128))
        }
        val exploration = bounded(rng, 1000); rng = exploration.o("rng")!!
        val explore = exploration.l("value") < steering.exploration
        val pool = if (explore) {
            val least = rows.minOf { integer(signals[it.o("entry")!!.s("trackId")]?.v("exposure"), 0, MAX_SAFE_INTEGER, 0) }
            rows.filter { integer(signals[it.o("entry")!!.s("trackId")]?.v("exposure"), 0, MAX_SAFE_INTEGER, 0) == least }
        } else rows
        val roll = bounded(rng, pool.sumOf { if (explore) 1L else it.l("weight") }); rng = roll.o("rng")!!
        var selectionCursor = roll.l("value")
        val picked = pool.first { selectionCursor -= if (explore) 1 else it.l("weight"); selectionCursor < 0 }
        return obj("entry" to picked.o("entry"), "rng" to rng, "reason" to obj("strategy" to if (explore) "exploration" else "weighted", "exploration" to explore,
            "candidateCount" to candidates.size, "relaxations" to array(relaxed), "signalCoverage" to if (explore) array(listOf("exposure")) else picked.v("coverage"),
            "scoreParts" to if (explore) null else picked.o("parts"), "weight" to if (explore) 1 else picked.v("weight"), "albumOrdering" to null))
    }

    private fun skipBucket(request: JSONObject): String {
        val heard = if (safe(request.v("heardMs")) && request.l("heardMs") >= 0) request.l("heardMs") else null
        val coverage = if (safe(request.v("coveragePermille")) && request.l("coveragePermille") >= 0) min(1000, request.l("coveragePermille")) else null
        return when {
            heard == null && coverage == null -> "unknown"
            heard != null && heard < FLICK_MS -> "flick"
            coverage != null && coverage >= LATE_SKIP_COVERAGE -> "late"
            heard != null && heard < EARLY_SKIP_MAX_MS && (coverage == null || coverage < EARLY_SKIP_COVERAGE) -> "early"
            else -> "mid"
        }
    }
    /** Session marks for the song being left. Only personal advances leave marks. */
    private fun nextLanes(state: JSONObject, current: JSONObject?, skip: String?, appends: Boolean, nowMs: Long?): JSONObject {
        val lanes = state.o("lanes")!!
        var marks = lanes.os("marks").map { if (appends) cp(it, "songs" to it.l("songs") + 1) else it }
            .filter { it.l("songs") < MARK_MAX_SONGS && (nowMs == null || it.l("atMs") + MARK_MAX_AGE_MS >= nowMs) }
        if (skip == null || current == null) return cp(lanes, "marks" to array(marks))
        MARK_VALUES[skip]?.let { value -> marks = (marks + obj("entryId" to current.v("entryId"), "mark" to value, "songs" to 0, "atMs" to (nowMs ?: 0L))).takeLast(MAX_MARKS) }
        val kept = skip == "finish" || skip == "late"
        val holds = skip == "flick" || skip == "unknown"
        return cp(lanes, "marks" to array(marks),
            "flicks" to if (skip == "flick") lanes.l("flicks") + 1 else if (skip == "unknown") lanes.l("flicks") else 0,
            "earlyRun" to if (skip == "early") lanes.l("earlyRun") + 1 else if (holds) lanes.l("earlyRun") else 0,
            "anchorEntryId" to if (kept) current.v("entryId") else lanes.v("anchorEntryId"),
            "lastEarlyAlbumId" to if (skip == "early") current.v("albumId") else if (holds) lanes.v("lastEarlyAlbumId") else null)
    }
    private fun albumRuns(state: JSONObject): Boolean = state.s("mode") == "album" || state.s("mode") == "flow" && state.o("options")!!.v("preserveAlbumRuns") == true

    fun stage(state: JSONObject, request: JSONObject = JSONObject(), context: JSONObject = JSONObject()): JSONObject {
        requireState(state)
        val attemptId = id(request.v("attemptId"), "attemptId")
        val advanceId = optionalId(request.v("advanceId")) ?: attemptId
        if (state.o("pending")?.s("attemptId") == attemptId) return state
        val stateQuarantine = state.o("quarantine")!!
        val next = cp(state, "pending" to null, "quarantine" to if (stateQuarantine.s("advanceId") == advanceId) stateQuarantine else quarantine(advanceId))
        if (next.o("quarantine")!!.a("entryIds").size >= 64) return cp(next, "diagnostics" to array((next.a("diagnostics") + "retry-budget-exhausted").distinct()), "paused" to true)
        val entries = state.os("entries")
        val byId = entries.associateBy { it.s("entryId") }
        fun allowed(entryId: Any?, explicit: Boolean = false): Boolean {
            val entry = byId[entryId]
            return entry != null && entry.v("available") == true && (explicit || entryId !in state.a("exclusions")) && entryId !in next.o("quarantine")!!.a("entryIds")
        }
        val currentHistory = currentOccurrence(state)
        val current = byId[currentHistory?.s("entryId")]
        val nowMs = if (safe(context.v("nowMs")) && context.l("nowMs") >= 0) context.l("nowMs") else null
        val lanes = state.o("lanes")!!
        // Natural end plays the base queue; a personal Next plays the live queue.
        // A failed candidate's retry keeps the lane and skip of the advance that staged it.
        val sameAdvance = stateQuarantine.s("advanceId") == advanceId
        var lane = request.s("lane").takeIf { it in LANES } ?: stateQuarantine.s("lane").takeIf { sameAdvance && it in LANES } ?: if (request.v("naturalEnd") == false) "live" else "base"
        if (current == null || request.s("direction") == "previous" || request.v("entryId") != null) lane = "base"
        val advancing = request.s("direction") != "previous" && request.v("entryId") == null && current != null && state.s("mode") != "off"
        val retrying = sameAdvance && stateQuarantine.has("skip") && request.v("heardMs") == null && request.v("coveragePermille") == null
        val skip = when {
            !advancing || request.v("personal") == false -> null
            retrying -> stateQuarantine.s("skip")
            lane == "live" -> skipBucket(request)
            request.v("naturalEnd") == true -> "finish"
            else -> null
        }
        val multiSource = entries.mapNotNull { it.s("sourceKey") }.toSet().size > 1
        var entry: JSONObject? = null
        var kind = "automatic"
        var historyIndex: Int? = null
        var cycleStarts = false
        var rngAfter = state.o("rng")!!
        var futureAfter = state.a("generatedFuture")
        var albumLockAfter = state.o("albumLock")
        var manualIndex: Int? = null
        var lanesAfter: JSONObject? = null
        var pinnedAfter: List<JSONObject>? = null
        var reason = obj("strategy" to state.v("mode"), "exploration" to false, "candidateCount" to 0,
            "relaxations" to JSONArray(), "signalCoverage" to JSONArray(), "scoreParts" to null, "weight" to null, "albumOrdering" to null)
        val history = state.os("history")
        val cursor = state.l("cursor").toInt()
        val manual = state.os("manual")
        val remainingEntitlement = state.a("remaining")
        if (request.s("direction") == "previous") {
            for (index in cursor - 1 downTo 0) if (allowed(history[index].v("entryId"), true)) { historyIndex = index; break }
            if (historyIndex != null) { entry = byId[history[historyIndex].s("entryId")]; kind = "previous" }
        } else {
            if (request.v("entryId") != null) {
                id(request.v("entryId"), "entryId")
                if (!allowed(request.v("entryId"), true)) return cp(next, "paused" to true)
                entry = byId[request.s("entryId")]; kind = "play-now"
            }
            if (entry == null) {
                manualIndex = manual.indexOfFirst { it.l("position") == 0L && allowed(it.v("entryId"), true) }
                if (manualIndex >= 0) { entry = byId[manual[manualIndex].s("entryId")]; kind = "manual" }
            }
            if (entry == null && request.v("naturalEnd") == true && state.s("repeat") == "one" && current != null && allowed(current.v("entryId"), true)) { entry = current; kind = "repeat-one" }
            if (entry == null && cursor + 1 < history.size) {
                for (index in cursor + 1 until history.size) if (allowed(history[index].v("entryId"), true) && manual.none { it.v("entryId") == history[index].v("entryId") }) {
                    historyIndex = index; entry = byId[history[index].s("entryId")]; kind = "forward"; break
                }
            }
            if (entry == null) {
                val reserved = manual.filter { it.l("position") > 0 && allowed(it.v("entryId"), true) }.map { it.v("entryId") }.toSet()
                var remaining = remainingEntitlement.filter { allowed(it) && it !in reserved }
                if (remaining.isEmpty() && reserved.isNotEmpty()) {
                    manualIndex = manual.indexOfFirst { allowed(it.v("entryId"), true) }
                    entry = byId[manual[manualIndex].s("entryId")]; kind = "manual"
                }
                if (remaining.isEmpty() && remainingEntitlement.isEmpty() && state.s("repeat") == "all" && entries.isNotEmpty()) {
                    remaining = entries.filter { allowed(it.v("entryId")) }.map { it.v("entryId") }; cycleStarts = remaining.isNotEmpty()
                }
                val eligible = remaining.map { byId[it]!! }
                reason = cp(reason, "candidateCount" to eligible.size)
                if (entry == null && eligible.isNotEmpty()) {
                    val allowedSet = remaining.toSet()
                    if (albumRuns(state)) {
                        // One early skip moves to the next track; a second early skip on the
                        // same album leaves it for another album when one remains.
                        val leaveAlbum = lane == "live" && skip == "early" && current?.v("albumId") != null && lanes.v("lastEarlyAlbumId") == current.v("albumId")
                        val locked = if (leaveAlbum) null else state.o("albumLock")?.a("entryIds")?.firstOrNull { it in allowedSet }
                        if (locked != null && !cycleStarts) {
                            entry = byId[locked as String]; kind = "album-continuation"; reason = cp(reason, "albumOrdering" to state.o("albumLock")!!.v("ordering"))
                        } else {
                            val groups = linkedMapOf<String, MutableList<JSONObject>>()
                            eligible.forEach { e ->
                                val key = canonicalJson(array(if (e.v("albumId") == null) listOf("entry", e.v("entryId")) else listOf("album", e.v("albumId"))))
                                groups.getOrPut(key) { mutableListOf() }.add(e)
                            }
                            var keys = groups.keys.sorted()
                            if (leaveAlbum && keys.size > 1) keys = keys.filter { it != canonicalJson(array(listOf("album", current!!.v("albumId")))) }
                            val choice = bounded(state.o("rng")!!, keys.size.toLong()); rngAfter = choice.o("rng")!!
                            val ordered = albumOrder(groups.getValue(keys[choice.l("value").toInt()]))
                            entry = ordered.first[0]
                            albumLockAfter = obj("albumId" to entry.v("albumId"), "entryIds" to array(ordered.first.map { it.v("entryId") }), "ordering" to ordered.second)
                            reason = cp(reason, "albumOrdering" to ordered.second)
                        }
                        albumLockAfter = albumLockAfter?.let { lock -> cp(lock, "entryIds" to array(lock.a("entryIds").filter { it != entry!!.v("entryId") })) }
                        if (albumLockAfter?.a("entryIds")?.isEmpty() == true) albumLockAfter = null
                    } else if (state.s("mode") == "off") {
                        val retained = state.a("generatedFuture").filter { it in allowedSet }
                        entry = if (retained.isNotEmpty()) byId[retained[0]] else eligible.minBy { it.l("sourceIndex") }
                        futureAfter = retained.drop(1)
                    } else {
                        val stepped = nextLanes(state, current, skip, true, nowMs)
                        lanesAfter = stepped
                        val marks = stepped.os("marks")
                        val pinned = if (cycleStarts) emptyList() else lanes.os("pinned").filter { it.v("entryId") in allowedSet }
                        if (lane == "base" && pinned.isNotEmpty()) { entry = byId[pinned[0].s("entryId")]; reason = cp(pinned[0].o("reason")!!) }
                        else {
                            // The live queue never offers the base queue's next song while a
                            // real alternative remains, and bridges from the last kept song.
                            val pool = if (lane == "live" && pinned.isNotEmpty() && eligible.size >= LIVE_EXCLUSION_MINIMUM) eligible.filter { it.v("entryId") != pinned[0].v("entryId") } else eligible
                            val anchor = if (lane == "base") current else if (state.s("mode") == "flow" && stepped.l("earlyRun") >= FLOW_LET_GO_AFTER) null else byId[stepped.s("anchorEntryId")]
                            val exploration = if (lane == "live" && stepped.l("flicks") >= 2) FLICK_STORM_EXPLORATION else if (lane == "live" && stepped.l("flicks") == 1L) FLICK_EXPLORATION else EXPLORATION
                            val consumed: (Any?) -> Boolean = if (cycleStarts) { _ -> false } else { entryId -> entryId !in remainingEntitlement }
                            val pick = selection(state, pool, context, anchor, byId, Steering(if (lane == "live") LIVE_STEER else BASE_STEER, marks, 0, exploration, airtimeContext(state, consumed), multiSource))
                            entry = pick.o("entry"); rngAfter = pick.o("rng")!!; reason = pick.o("reason")!!
                        }
                        var rest = pinned.filter { it.v("entryId") != entry!!.v("entryId") }
                        if (skip == "early") rest = rest.filter { similarity(byId[it.s("entryId")], current, multiSource) < PRUNE_SIMILARITY }
                        // Refill the pinned base window as successive natural ends would pick it.
                        val chain = (listOf(entry!!) + rest.map { byId[it.s("entryId")]!! }).toMutableList()
                        val taken = chain.map { it.v("entryId") }.toMutableSet()
                        val consumedAfter: (Any?) -> Boolean = { entryId -> entryId in taken || (!cycleStarts && entryId !in remainingEntitlement) }
                        var available = eligible.filter { it.v("entryId") !in taken }
                        var rng = rngAfter
                        val refill = rest.toMutableList()
                        while (refill.size < PINNED_BASE && available.isNotEmpty()) {
                            val simulated = history.take(cursor + 1) + chain.map { obj("entryId" to it.v("entryId"), "artistId" to it.v("artistId"), "albumId" to it.v("albumId"), "recordingId" to it.v("recordingId")) }
                            val preview = cp(state, "rng" to rng, "history" to array(simulated), "cursor" to simulated.lastIndex)
                            val pick = selection(preview, available, context, chain.last(), byId, Steering(BASE_STEER, marks, chain.size.toLong(), EXPLORATION, airtimeContext(state, consumedAfter), multiSource))
                            rng = pick.o("rng")!!
                            val chosen = pick.o("entry")!!
                            refill.add(obj("entryId" to chosen.v("entryId"), "reason" to cp(pick.o("reason")!!, "lane" to "base", "skip" to null)))
                            chain.add(chosen); taken.add(chosen.v("entryId"))
                            available = available.filter { it.v("entryId") != chosen.v("entryId") }
                        }
                        rngAfter = rng; pinnedAfter = refill; futureAfter = emptyList()
                    }
                }
            }
        }
        val picked = entry ?: return cp(next, "paused" to true)
        if (kind in listOf("manual", "play-now", "previous", "forward", "repeat-one", "album-continuation")) reason = cp(reason, "strategy" to kind)
        if (kind in listOf("manual", "play-now")) {
            futureAfter = emptyList(); albumLockAfter = null
            if (albumRuns(state) && picked.v("albumId") != null) {
                val siblings = albumOrder(entries.filter { it.v("albumId") == picked.v("albumId") })
                val index = siblings.first.indexOfFirst { it.v("entryId") == picked.v("entryId") }
                val later = siblings.first.drop(index + 1).filter { it.v("entryId") in remainingEntitlement && allowed(it.v("entryId")) }.map { it.v("entryId") }
                if (later.isNotEmpty()) albumLockAfter = obj("albumId" to picked.v("albumId"), "entryIds" to array(later), "ordering" to siblings.second)
                reason = cp(reason, "albumOrdering" to siblings.second)
            }
        }
        val laneState = lanesAfter ?: if (kind == "previous" || kind == "forward") lanes
            else nextLanes(state, current, if (kind in listOf("manual", "repeat-one", "album-continuation", "automatic")) skip else null, true, nowMs)
        val finalLanes = cp(laneState, "pinned" to array(pinnedAfter ?: lanes.os("pinned").filter { it.v("entryId") != picked.v("entryId") }))
        val decisionId = canonicalJson(array(listOf(state.v("sessionId"), state.v("revision"), state.v("membershipRevision"), state.v("authorityEpoch"), picked.v("entryId"), kind)))
        val receipt = cp(merge(obj("version" to VERSION, "policyVersion" to POLICY_VERSION, "configVersion" to CONFIG_VERSION, "decisionId" to decisionId,
            "scopeId" to state.v("scopeId"), "cycleId" to if (cycleStarts) canonicalJson(array(listOf(state.v("scopeId"), state.o("cycle")!!.l("generation") + 1))) else state.o("cycle")!!.v("cycleId"),
            "mode" to state.v("mode"), "preset" to state.o("options")!!.v("preset"), "kind" to kind, "entryId" to picked.v("entryId"), "trackId" to picked.v("trackId"),
            "originEntryId" to currentHistory?.v("entryId"), "originOccurrenceId" to currentHistory?.v("occurrenceId"),
            "explicitException" to (kind in listOf("play-now", "manual", "repeat-one", "previous", "forward")),
            "intentionalDuplicate" to entries.any { it.v("trackId") == picked.v("trackId") && it.v("entryId") != picked.v("entryId") }), reason), "lane" to lane, "skip" to skip)
        return cp(next, "pending" to obj("version" to VERSION, "attemptId" to attemptId, "advanceId" to advanceId, "decisionId" to decisionId,
            "revision" to state.v("revision"), "membershipRevision" to state.v("membershipRevision"), "authorityId" to state.v("authorityId"), "authorityEpoch" to state.v("authorityEpoch"),
            "entryId" to picked.v("entryId"), "trackId" to picked.v("trackId"), "kind" to kind, "lane" to lane, "historyIndex" to historyIndex, "newCycle" to cycleStarts,
            "rngAfter" to rngAfter, "futureAfter" to array(futureAfter), "albumLockAfter" to albumLockAfter, "lanesAfter" to finalLanes, "manualIndex" to manualIndex, "receipt" to receipt))
    }
    /** What each queue would play now, without reserving or consuming anything. */
    fun preview(state: JSONObject, request: JSONObject = JSONObject(), context: JSONObject = JSONObject()): JSONObject? {
        val staged = stage(cp(state, "pending" to null), merge(obj("attemptId" to canonicalJson(array(listOf("lane-preview", state.v("revision"), request.s("lane") ?: "base"))), "personal" to false), request), context)
        return staged.o("pending")?.o("receipt")
    }
    private fun matches(state: JSONObject, activation: JSONObject): Boolean {
        val pending = state.o("pending") ?: return false
        return listOf("attemptId", "decisionId", "authorityId", "authorityEpoch").all { same(activation.v(it), pending.v(it)) } &&
            listOf("authorityId", "authorityEpoch", "revision", "membershipRevision").all { same(state.v(it), pending.v(it)) }
    }
    fun commit(state: JSONObject, activation: JSONObject): JSONObject {
        requireState(state)
        if (!matches(state, activation)) return state
        val pending = state.o("pending")!!
        val entries = state.os("entries")
        val entry = entries.firstOrNull { it.v("entryId") == pending.v("entryId") } ?: return state
        if (entry.v("available") != true) return state
        val navigation = pending.v("historyIndex") != null
        val sequence = state.l("occurrenceSequence") + if (navigation) 0 else 1
        val occurrence = obj("occurrenceId" to canonicalJson(array(listOf(state.v("sessionId"), sequence))), "entryId" to entry.v("entryId"), "trackId" to entry.v("trackId"),
            "artistId" to entry.v("artistId"), "albumId" to entry.v("albumId"), "recordingId" to entry.v("recordingId"), "decisionId" to pending.v("decisionId"), "kind" to pending.v("kind"), "tombstone" to false, "receipt" to pending.v("receipt"))
        val history = if (navigation) state.os("history") else state.os("history").take(state.l("cursor").toInt() + 1) + occurrence
        val legacyForward = navigation && pending.s("kind") == "forward" && state.os("history")[pending.l("historyIndex").toInt()].s("kind") == "legacy-import"
        val consume = !navigation && pending.s("kind") != "repeat-one" || legacyForward
        val remaining = if (pending.v("newCycle") == true) entries.map { it.v("entryId") } else state.a("remaining")
        return cp(state, "revision" to state.l("revision") + 1,
            "cycle" to if (pending.v("newCycle") == true) obj("generation" to state.o("cycle")!!.l("generation") + 1, "cycleId" to canonicalJson(array(listOf(state.v("scopeId"), state.o("cycle")!!.l("generation") + 1)))) else state.v("cycle"),
            "rng" to pending.v("rngAfter"), "remaining" to array(if (consume) remaining.filter { it != entry.v("entryId") } else remaining),
            "history" to array(history), "cursor" to if (navigation) pending.v("historyIndex") else history.lastIndex, "occurrenceSequence" to sequence,
            "generatedFuture" to pending.v("futureAfter"), "albumLock" to pending.v("albumLockAfter"), "lanes" to pending.v("lanesAfter"),
            "manual" to array(state.os("manual").filterIndexed { index, _ -> !(pending.s("kind") == "manual" && index.toLong() == pending.l("manualIndex")) }.map {
                cp(it, "position" to if (pending.s("kind") in listOf("previous", "repeat-one")) it.v("position") else max(0, it.l("position") - 1)) }),
            "pending" to null, "quarantine" to quarantine(), "undo" to null, "paused" to false,
            "lastConfirmedPositionMs" to integer(activation.v("positionMs"), 0, MAX_SAFE_INTEGER, 0))
    }
    fun abort(state: JSONObject, activation: JSONObject = JSONObject()): JSONObject {
        requireState(state)
        if (!matches(state, activation)) return state
        val pending = state.o("pending")!!
        return cp(state, "revision" to state.l("revision") + 1, "pending" to null, "paused" to true,
            "quarantine" to cp(quarantine(pending.v("advanceId"), (state.o("quarantine")!!.a("entryIds") + pending.v("entryId")).distinct()), "lane" to pending.v("lane"), "skip" to pending.o("receipt")!!.v("skip")),
            "diagnostics" to array((state.a("diagnostics") + "activation-failed").distinct()))
    }
    private fun invalidate(state: JSONObject, changes: JSONObject): JSONObject = cp(merge(state, changes), "revision" to state.l("revision") + 1, "pending" to null)
    private fun withoutPinned(state: JSONObject): JSONObject = cp(state.o("lanes")!!, "pinned" to JSONArray())
    private fun queueSnapshot(state: JSONObject): JSONObject = obj(*listOf("entries", "remaining", "manual", "exclusions", "generatedFuture", "albumLock", "membershipRevision").map { it to state.v(it) }.toTypedArray())
    fun mutate(state: JSONObject, action: JSONObject): JSONObject {
        requireState(state)
        val type = action.s("type")
        if (type == "undo") {
            val undo = state.o("undo") ?: return state
            val undoIds = undo.os("entries").map { it.v("entryId") }.toSet()
            val lanes = state.o("lanes")!!
            return invalidate(state, cp(undo, "lanes" to cp(lanes, "pinned" to JSONArray(), "marks" to array(lanes.os("marks").filter { it.v("entryId") in undoIds })), "membershipRevision" to state.l("membershipRevision") + 1, "undo" to null,
                "history" to array(state.os("history").map { h -> cp(h, "tombstone" to undo.os("entries").none { it.v("entryId") == h.v("entryId") }) })))
        }
        if (type in listOf("priority", "move")) {
            id(action.v("entryId"), "entryId")
            if (state.os("entries").none { it.v("entryId") == action.v("entryId") }) return state
            val index = if (type == "priority") 0L else integer(action.v("index"), 0, state.a("entries").size.toLong(), state.a("entries").size.toLong())
            val rest = state.os("manual").filter { it.v("entryId") != action.v("entryId") }.map { cp(it, "position" to it.l("position") + if (it.l("position") >= index) 1 else 0) }.toMutableList()
            rest.add(obj("entryId" to action.v("entryId"), "kind" to type, "intentId" to id(action.v("intentId"), "intentId"), "position" to index))
            return invalidate(state, obj("manual" to array(rest.sortedBy { it.l("position") }), "generatedFuture" to JSONArray(), "albumLock" to null, "lanes" to withoutPinned(state), "undo" to queueSnapshot(state)))
        }
        if (type in listOf("exclude", "include")) {
            id(action.v("entryId"), "entryId")
            if (state.os("entries").none { it.v("entryId") == action.v("entryId") }) return state
            val exclusions = if (type == "exclude") (state.a("exclusions") + action.v("entryId")).distinct() else state.a("exclusions").filter { it != action.v("entryId") }
            val lanes = state.o("lanes")!!
            return invalidate(state, obj("exclusions" to array(exclusions), "lanes" to cp(lanes, "pinned" to array(lanes.os("pinned").filter { it.v("entryId") !in exclusions })), "undo" to queueSnapshot(state)))
        }
        val options = obj("membershipRevision" to state.l("membershipRevision") + 1)
        if (type == "clear") return reconcile(state, JSONArray(), options)
        if (type == "remove") return reconcile(state, array(state.os("entries").filter { it.v("entryId") != action.v("entryId") }), options)
        if (type == "insert") {
            val entries = state.a("entries").toMutableList()
            entries.add(integer(action.v("index"), 0, entries.size.toLong(), entries.size.toLong()).toInt(), action.v("entry"))
            return reconcile(state, array(entries), options)
        }
        throw IllegalArgumentException("Unknown mutation")
    }
    fun reconcile(state: JSONObject, rawEntries: JSONArray, options: JSONObject = JSONObject()): JSONObject {
        requireState(state)
        val entriesArray = normalizeEntries(rawEntries); val entries = entriesArray.items().map { it as JSONObject }
        val ids = entries.map { it.v("entryId") }.toSet(); val previous = state.os("entries").map { it.v("entryId") }.toSet()
        val before = queueSnapshot(state)
        val prior = (state.os("history") + state.os("entries")).associate { it.v("entryId") to it.v("trackId") }
        entries.forEach { require(!prior.containsKey(it.v("entryId")) || prior[it.v("entryId")] == it.v("trackId")) { "entryId trackId rebinding" } }
        return invalidate(state, obj("entries" to entriesArray,
            "remaining" to array(state.a("remaining").filter { it in ids } + entries.filter { it.v("entryId") !in previous }.map { it.v("entryId") }),
            "membershipRevision" to integer(options.v("membershipRevision"), state.l("membershipRevision") + 1, MAX_SAFE_INTEGER, state.l("membershipRevision") + 1),
            "history" to array(state.os("history").map { cp(it, "tombstone" to (it.v("entryId") !in ids)) }),
            "manual" to array(state.os("manual").filter { it.v("entryId") in ids }), "exclusions" to array(state.a("exclusions").filter { it in ids }), "generatedFuture" to JSONArray(),
            "albumLock" to state.o("albumLock")?.let { cp(it, "entryIds" to array(it.a("entryIds").filter { entryId -> entryId in ids })) },
            "quarantine" to quarantine(), "undo" to before,
            "lanes" to state.o("lanes")!!.let { lanes -> cp(lanes, "pinned" to JSONArray(), "marks" to array(lanes.os("marks").filter { it.v("entryId") in ids }),
                "anchorEntryId" to lanes.v("anchorEntryId")?.takeIf { it in ids }) }))
    }
    fun setMode(state: JSONObject, mode: Any?, options: JSONObject? = null, context: JSONObject = JSONObject()): JSONObject {
        requireState(state)
        val normalized = normalizeMode(mode)
        val future = if (normalized == "off") project(state, obj("horizon" to min(32, state.a("remaining").size), "context" to context)).a("entryIds") else emptyList()
        val frozen = if (normalized == "off") (future + state.a("generatedFuture") + state.a("remaining")).distinct() else emptyList()
        return invalidate(state, obj("mode" to normalized, "options" to (options?.let(::normalizeOptions) ?: state.o("options")), "generatedFuture" to array(frozen), "albumLock" to null, "lanes" to withoutPinned(state)))
    }
    fun newCycle(state: JSONObject): JSONObject {
        requireState(state)
        val generation = state.o("cycle")!!.l("generation") + 1
        return invalidate(state, obj("cycle" to obj("generation" to generation, "cycleId" to canonicalJson(array(listOf(state.v("scopeId"), generation)))),
            "remaining" to array(state.os("entries").map { it.v("entryId") }), "generatedFuture" to JSONArray(), "exclusions" to JSONArray(), "albumLock" to null, "quarantine" to quarantine(), "lanes" to withoutPinned(state)))
    }
    /** v2.1 sessions keep their history, entitlement and cycle. Random becomes Balanced, and the two queues start empty. */
    private fun upgradeV21(snapshot: Any?): JSONObject? {
        if (snapshot !is JSONObject || !same(snapshot.v("schemaVersion"), 2) || snapshot.s("policyVersion") != PRIOR_POLICY_VERSION || !same(snapshot.v("configVersion"), 1)) return null
        val random = snapshot.v("mode") == "random"
        return cp(deep(snapshot) as JSONObject, "schemaVersion" to VERSION, "policyVersion" to POLICY_VERSION, "configVersion" to CONFIG_VERSION, "mode" to normalizeMode(snapshot.v("mode")),
            "entries" to normalizeEntries(snapshot.v("entries") as? JSONArray ?: JSONArray()), "options" to normalizeOptions(snapshot.o("options") ?: JSONObject()),
            "generatedFuture" to if (random) JSONArray() else deep(snapshot.v("generatedFuture")), "lanes" to emptyLanes(), "pending" to null)
    }
    fun restore(snapshot: Any?): JSONObject {
        val upgraded = try { upgradeV21(snapshot) } catch (error: Exception) { return obj("status" to "corrupt", "state" to null, "preserved" to deep(snapshot), "diagnostics" to array(listOf(error.message))) }
        if (upgraded != null) {
            try { validateState(upgraded) } catch (error: Exception) { return obj("status" to "corrupt", "state" to null, "preserved" to deep(snapshot), "diagnostics" to array(listOf(error.message))) }
            return obj("status" to "restored", "state" to cp(upgraded, "paused" to true, "undo" to null, "quarantine" to quarantine()), "preserved" to null,
                "diagnostics" to array(listOf("upgraded-from-v2.1") + if ((snapshot as JSONObject).o("pending") != null) listOf("unconfirmed-activation-aborted") else emptyList()))
        }
        if (snapshot !is JSONObject || !same(snapshot.v("schemaVersion"), VERSION) || snapshot.s("policyVersion") != POLICY_VERSION || !same(snapshot.v("configVersion"), CONFIG_VERSION))
            return obj("status" to "unsupported", "state" to null, "preserved" to deep(snapshot), "diagnostics" to array(listOf("unsupported-schema-policy-or-config")))
        return try {
            validateState(snapshot)
            obj("status" to "restored", "state" to cp(deep(snapshot) as JSONObject, "pending" to null, "paused" to true, "undo" to null, "quarantine" to quarantine()),
                "preserved" to null, "diagnostics" to array(if (snapshot.o("pending") != null) listOf("unconfirmed-activation-aborted") else emptyList()))
        } catch (error: Exception) { obj("status" to "corrupt", "state" to null, "preserved" to deep(snapshot), "diagnostics" to array(listOf(error.message))) }
    }
    fun validateState(state: JSONObject): Boolean {
        requireState(state); id(state.v("sessionId")); id(state.v("scopeId")); id(state.v("authorityId"))
        val normalized = normalizeEntries(state.v("entries") as? JSONArray ?: throw IllegalArgumentException("Entries must be an array of at most 1,000,000 occurrences"))
        val entries = normalized.items().map { it as JSONObject }; val ids = entries.map { it.v("entryId") }.toSet()
        val tracksByEntry = entries.associate { it.v("entryId") to it.v("trackId") }
        require(canonicalJson(normalized) == canonicalJson(state.v("entries"))) { "Noncanonical or corrupt entries" }
        require(listOf("revision", "membershipRevision", "authorityEpoch").all { safe(state.v(it)) && state.l(it) >= 0 }) { "Invalid state revisions" }
        require(state.v("remaining") is JSONArray && state.a("remaining").all { it in ids } && state.a("remaining").distinct().size == state.a("remaining").size) { "Invalid remaining entitlement" }
        require(state.v("history") is JSONArray && safe(state.v("cursor")) && state.l("cursor") >= -1 && state.l("cursor") < state.a("history").size) { "Invalid history cursor" }
        val history = state.os("history")
        history.forEach { h ->
            require(h.s("occurrenceId")?.length in 1..16384) { "Invalid occurrenceId" }
            id(h.v("entryId")); id(h.v("trackId"))
            require(h.v("tombstone") is Boolean && h.v("kind") is String && (!tracksByEntry.containsKey(h.v("entryId")) || tracksByEntry[h.v("entryId")] == h.v("trackId"))) { "Corrupt history identity" }
        }
        require(history.map { it.v("occurrenceId") }.distinct().size == history.size) { "Duplicate occurrenceId" }
        val rng = state.o("rng")
        require(rng != null && rng.s("algorithm") == RNG_ALGORITHM && safe(rng.v("state")) && rng.l("state") in 0..4294967295L && safe(rng.v("draws")) && rng.l("draws") >= 0) { "Invalid RNG" }
        listOf("manual", "exclusions", "generatedFuture", "eventSequences", "diagnostics").forEach { require(state.v(it) is JSONArray) { "Invalid $it" } }
        listOf("exclusions", "generatedFuture").forEach { field -> require(state.a(field).all { it in ids } && state.a(field).distinct().size == state.a(field).size) { "Corrupt $field" } }
        state.os("manual").forEach {
            id(it.v("entryId")); id(it.v("intentId"))
            require(it.v("entryId") in ids && it.s("kind") in listOf("priority", "move") && safe(it.v("position")) && it.l("position") >= 0) { "Corrupt manual intent" }
        }
        require(state.os("manual").map { it.v("entryId") }.distinct().size == state.a("manual").size) { "Duplicate manual entry" }
        require(state.o("options") != null && canonicalJson(state.v("options")) == canonicalJson(normalizeOptions(state.o("options")!!))) { "Corrupt options" }
        val quarantine = state.o("quarantine")
        require(quarantine != null && quarantine.v("entryIds") is JSONArray && quarantine.a("entryIds").all { it in ids }) { "Corrupt quarantine" }
        require(!truthy(state.v("albumLock")) || state.v("albumLock") is JSONObject) { "Corrupt album lock" }
        state.o("albumLock")?.let {
            require(it.v("entryIds") is JSONArray && it.a("entryIds").all { entry -> entry in ids } && it.s("ordering") in listOf("disc-track", "source-order-missing-numbers")) { "Corrupt album lock" }
        }
        require(safe(state.v("lastConfirmedPositionMs")) && state.l("lastConfirmedPositionMs") >= 0 && safe(state.v("occurrenceSequence")) && state.l("occurrenceSequence") >= history.size && state.v("paused") is Boolean) { "Corrupt playback position/sequence" }
        state.os("eventSequences").forEach {
            id(it.v("producerId")); id(it.v("eventId")); require(safe(it.v("sequence")) && it.l("sequence") >= 1) { "Corrupt producer sequence" }
        }
        require(state.a("eventSequences").size <= 64 && state.os("eventSequences").map { it.v("producerId") }.distinct().size == state.a("eventSequences").size) { "Corrupt producer table" }
        require(!truthy(state.v("pending")) || state.v("pending") is JSONObject) { "Corrupt pending reservation" }
        state.o("pending")?.let { pending ->
            require(matches(state, pending) && pending.v("entryId") in ids && pending.o("receipt") != null && pending.o("receipt")!!.v("entryId") == pending.v("entryId")) { "Corrupt pending reservation" }
        }
        require(state.s("mode") in MODES && state.s("repeat") in listOf("off", "all", "one") && safe(state.o("cycle")?.v("generation")) && state.o("cycle")!!.l("generation") >= 0) { "Invalid mode/repeat/cycle" }
        val lanes = state.o("lanes")
        fun count(value: Any?) = safe(value) && (value as Number).toLong() >= 0
        require(lanes != null && lanes.v("marks") is JSONArray && lanes.v("pinned") is JSONArray && count(lanes.v("flicks")) && count(lanes.v("earlyRun")) &&
            lanes.a("marks").size <= MAX_MARKS && lanes.a("pinned").size <= PINNED_BASE) { "Corrupt lanes" }
        lanes.a("marks").forEach { mark ->
            require(mark is JSONObject && mark.v("entryId") in ids && MARK_VALUES.values.any { same(it, mark.v("mark")) } && count(mark.v("songs")) && mark.l("songs") < MARK_MAX_SONGS && count(mark.v("atMs"))) { "Corrupt session mark" }
        }
        require(lanes.a("pinned").all { it is JSONObject && it.v("entryId") in ids && it.v("reason") is JSONObject } && lanes.os("pinned").map { it.v("entryId") }.distinct().size == lanes.a("pinned").size) { "Corrupt pinned base queue" }
        require((lanes.v("anchorEntryId") == null || lanes.v("anchorEntryId") in ids) && (lanes.v("lastEarlyAlbumId") == null || lanes.v("lastEarlyAlbumId") is String)) { "Corrupt lane anchors" }
        return true
    }
    fun createProjection(state: JSONObject, options: JSONObject = JSONObject()): JSONObject {
        requireState(state)
        return obj("originRevision" to state.v("revision"), "originGeneration" to state.o("cycle")!!.v("generation"),
            "horizon" to integer(options.v("horizon"), 0, 32, 10), "state" to state, "decisions" to JSONArray(), "done" to false)
    }
    fun advanceProjection(batch: JSONObject, context: JSONObject = JSONObject()): JSONObject {
        if (batch.v("done") == true || batch.a("decisions").size >= batch.l("horizon") || context.v("cancelled") == true) return cp(batch, "done" to true)
        val original = batch.o("state")!!
        val staged = if (original.o("pending") != null) original else stage(original,
            obj("attemptId" to canonicalJson(array(listOf("preview", batch.v("originRevision"), batch.a("decisions").size))), "advanceId" to "preview"), context)
        val pending = staged.o("pending") ?: return cp(batch, "done" to true)
        val state = commit(staged, pending)
        val decisions = batch.a("decisions") + pending.v("receipt")
        return cp(batch, "state" to state, "decisions" to array(decisions), "done" to (decisions.size >= batch.l("horizon") || !same(state.o("cycle")!!.v("generation"), batch.v("originGeneration"))))
    }
    fun project(state: JSONObject, options: JSONObject = JSONObject()): JSONObject {
        var batch = createProjection(state, options)
        while (batch.v("done") != true) batch = advanceProjection(batch, options.o("context") ?: JSONObject())
        return obj("revision" to state.v("revision"), "entryIds" to array(batch.os("decisions").map { it.v("entryId") }), "decisions" to batch.v("decisions"))
    }
    fun publicSnapshot(state: JSONObject, mapIds: Map<String, (String) -> String?>?, context: JSONObject?): JSONObject {
        requireState(state)
        require(mapIds != null && context != null) { "Explicit public identity mappers and authority scoring context required" }
        fun mapped(kind: String, value: Any?): String? {
            if (value == null) return null
            val mapper = mapIds[kind] ?: throw IllegalArgumentException("Public $kind mapper required")
            val result = mapper(value as String) ?: return null
            require(Regex("^[A-Za-z0-9_:-]{1,128}$").matches(result)) { "Unsafe public $kind" }
            return result
        }
        val current = currentOccurrence(state)
        val projected = project(state, obj("context" to context))
        return obj("version" to VERSION, "policyVersion" to POLICY_VERSION, "authorityId" to mapped("authorityId", state.v("authorityId")), "authorityEpoch" to state.v("authorityEpoch"),
            "revision" to state.v("revision"), "mode" to state.v("mode"), "repeat" to state.v("repeat"), "paused" to state.v("paused"),
            "current" to if (current != null && mapped("trackId", current.v("trackId")) != null) obj("occurrenceId" to mapped("occurrenceId", current.v("occurrenceId")), "entryId" to mapped("entryId", current.v("entryId")), "trackId" to mapped("trackId", current.v("trackId"))) else null,
            "future" to array(projected.os("decisions").map { obj("decisionId" to mapped("decisionId", it.v("decisionId")), "entryId" to mapped("entryId", it.v("entryId")), "trackId" to mapped("trackId", it.v("trackId")), "kind" to it.v("kind")) }.filter { it.v("trackId") != null && it.v("entryId") != null }))
    }
    fun migrateLegacy(legacy: JSONObject, input: JSONObject): JSONObject {
        val plan = legacy.o("plan")
        val state = createSession(cp(input, "mode" to (input.v("mode") ?: if (plan?.s("style") == "standard") "balanced" else if (legacy.s("mode") == "AlbumPreserving") "album" else "flow"),
            "seed" to (input.v("seed") ?: plan?.v("seed"))))
        val byTrack = linkedMapOf<Any?, JSONObject>()
        state.os("entries").forEach { byTrack.putIfAbsent(it.v("trackId"), it) }
        val order = if (plan?.v("order") is JSONArray) plan.a("order") else legacy.a("history")
        val history = order.mapIndexed { index, trackId ->
            val entry = byTrack[trackId]
            val stringId = if (trackId is String) trackId else canonicalJson(trackId)
            obj("occurrenceId" to canonicalJson(array(listOf(state.v("sessionId"), index + 1))), "entryId" to (entry?.v("entryId") ?: canonicalJson(array(listOf("legacy-tombstone", index)))),
                "trackId" to id(stringId), "artistId" to entry?.v("artistId"), "albumId" to entry?.v("albumId"), "recordingId" to entry?.v("recordingId"), "decisionId" to null, "kind" to "legacy-import", "tombstone" to (entry == null), "receipt" to null)
        }
        val cursor = integer(plan?.v("cursor"), -1, history.lastIndex.toLong(), history.lastIndex.toLong()).toInt()
        val consumed = history.take(cursor + 1).map { it.v("entryId") }.toSet()
        return cp(state, "history" to array(history), "cursor" to cursor, "occurrenceSequence" to history.size,
            "remaining" to array(state.a("remaining").filter { it !in consumed }),
            "generatedFuture" to array(history.drop(cursor + 1).filter { it.v("tombstone") != true }.map { it.v("entryId") }),
            "imported" to true, "diagnostics" to array(listOf("legacy-traversal-only-no-listening-evidence")))
    }
    private fun applyEvent(state: JSONObject, event: JSONObject, context: JSONObject): JSONObject {
        requireState(state)
        return when (event.s("type")) {
            "stage" -> stage(state, event, context)
            "activate-success" -> commit(state, event)
            "activate-failure" -> abort(state, event)
            "mutation" -> mutate(state, event.o("action") ?: throw IllegalArgumentException("Unknown mutation"))
            "mode-change" -> setMode(state, event.v("mode"), event.o("options"), context)
            "repeat-change" -> invalidate(state, obj("repeat" to (event.s("repeat").takeIf { it in listOf("off", "all", "one") } ?: "off")))
            "explicit-new-cycle" -> newCycle(state)
            "reshuffle-remaining" -> invalidate(state, obj("generatedFuture" to JSONArray(), "albumLock" to null, "lanes" to withoutPinned(state)))
            "membership-change" -> reconcile(state, event.v("entries") as? JSONArray ?: throw IllegalArgumentException("Entries must be an array of at most 1,000,000 occurrences"), event)
            "session-change" -> invalidate(state, obj("sessionId" to id(event.v("sessionId")), "exclusions" to JSONArray(), "pending" to null, "paused" to true, "quarantine" to quarantine(), "lanes" to emptyLanes()))
            "position-checkpoint" -> if (currentOccurrence(state)?.v("occurrenceId") == event.v("occurrenceId")) cp(state, "lastConfirmedPositionMs" to integer(event.v("positionMs"), 0, MAX_SAFE_INTEGER, state.l("lastConfirmedPositionMs"))) else state
            "restore" -> restore(state).o("state") ?: throw IllegalArgumentException("Corrupt session restore")
            "authority-change" -> if (!safe(event.v("authorityEpoch")) || event.l("authorityEpoch") <= state.l("authorityEpoch")) state
                else invalidate(state, obj("authorityId" to id(event.v("authorityId")), "authorityEpoch" to event.v("authorityEpoch"), "paused" to true))
            else -> throw IllegalArgumentException("Unknown session event")
        }
    }
    fun reduce(state: JSONObject, event: JSONObject, context: JSONObject = JSONObject()): JSONObject {
        requireState(state)
        if (event.v("eventId") == null) return applyEvent(state, event, context)
        id(event.v("eventId"), "eventId"); id(event.v("producerId"), "producerId")
        require(safe(event.v("sequence")) && event.l("sequence") >= 1) { "Versioned event requires positive producer sequence" }
        val prior = state.os("eventSequences").firstOrNull { it.v("producerId") == event.v("producerId") }
        if (prior != null && event.l("sequence") <= prior.l("sequence")) return state
        require(event.l("sequence") == (prior?.l("sequence") ?: 0) + 1) { "Producer sequence gap; replay missing commands in order" }
        require(prior != null || state.a("eventSequences").size < 64) { "Producer capacity exceeded; compact authority checkpoint required" }
        val changed = applyEvent(state, event, context)
        return cp(changed, "eventSequences" to array((state.os("eventSequences").filter { it.v("producerId") != event.v("producerId") } +
            obj("producerId" to event.v("producerId"), "sequence" to event.v("sequence"), "eventId" to event.v("eventId"))).sortedBy { it.s("producerId") }))
    }
}
