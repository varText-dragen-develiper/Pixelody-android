package com.pixelody.app.core.shuffle

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.math.max
import kotlin.math.min

/** Exact, clock-injected JSON port of https://github.com/varText-dragen-develiper/pixelody/blob/main/src/shuffle/learning.js, universal-shuffle-v2.1. */
object UniversalShuffleLearning {
    const val VERSION = 2
    const val POLICY_VERSION = "universal-shuffle-v2.1"
    private const val SAFE = 9_007_199_254_740_991L
    val CONFIG: Map<String, Long> = mapOf(
        "maxJournal" to 512L, "maxEventIds" to 4096L, "maxProducers" to 64L, "maxProducerGap" to 1024L,
        "maxOpenOccurrences" to 128L, "maxClosedOccurrences" to 256L, "maxCoverageRanges" to 64L,
        "maxClocksPerOccurrence" to 8L, "maxIntervalMs" to 60000L, "maxDurationMs" to 86400000L,
        "maxFutureSkewMs" to 300000L, "maxCounter" to 2147483647L,
        "implicitUnitCap" to 120L, "priorUnits" to 24L, "implicitScale" to 12L, "explicitPoints" to 8L,
        "tasteLimit" to 12L, "decayPeriodMs" to 2419200000L, "maxDecayPeriods" to 8L,
        "positiveCompletionUnits" to 3L, "positiveReplayUnits" to 1L, "negativeEarlySkipUnits" to 2L,
        "completionCoveragePermille" to 800L, "completionWallPermille" to 500L,
        "completionWallCapMs" to 60000L, "earlySkipMinimumMs" to 3000L,
        "earlySkipMaximumMs" to 30000L, "earlySkipCoveragePermille" to 250L,
        "noveltyLimit" to 12L, "exposureNoveltyStep" to 2L, "debtLimit" to 12L,
        "confidenceScale" to 1000L, "speedMinimumPermille" to 250L, "speedMaximumPermille" to 4000L,
        "mediaProgressToleranceMs" to 500L
    )
    val TYPES = listOf("offered", "activated", "interval", "outcome", "preference", "learning-enabled", "learning-reset", "history-delete", "track-delete")
    val IGNORED_REASONS = listOf("buffering", "connection-lost", "credential-loss", "crash", "decode-error", "focus-loss", "host-unreachable", "missing-file", "network-disconnect", "network-error", "pause", "permission-revoked", "playback-error", "preview", "remote-action", "route-failure", "seek", "shutdown", "token-expired", "vpn-failure")
    private val personalReasons = setOf("natural-end", "manual", "manual-next", "manual-skip", "manual-switch", "user-next", "user-skip", "user-replay", "replay", "early-skip", "late-skip", "track-change")
    private val controls = setOf("learning-enabled", "learning-reset", "history-delete", "track-delete")
    private val outcomes = setOf("natural-end", "deliberate-skip", "replay", "interrupted")
    private fun c(key: String) = CONFIG.getValue(key)
    private fun integer(v: Any?, minimum: Long = 0, maximum: Long = SAFE): Boolean = v is Number &&
        v.toDouble().isFinite() && v.toDouble() % 1.0 == 0.0 && v.toDouble() >= minimum.toDouble() && v.toDouble() <= maximum.toDouble()
    private fun id(v: Any?, maximum: Int = 1024): Boolean {
        if (v !is String || v.isEmpty() || v.length > maximum) return false
        var i = 0
        while (i < v.length) {
            val ch = v[i++]
            if (ch.isHighSurrogate()) { if (i >= v.length || !v[i++].isLowSurrogate()) return false }
            else if (ch.isLowSurrogate()) return false
        }
        return true
    }
    private fun eq(v: Any?, n: Long) = integer(v, -SAFE, SAFE) && (v as Number).toLong() == n
    private fun JSONObject.n(key: String): Long = (get(key) as Number).toLong()
    private fun JSONObject.s(key: String): String = getString(key)
    private fun JSONObject.b(key: String): Boolean = getBoolean(key)
    private fun JSONObject.a(key: String): JSONArray = getJSONArray(key)
    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
    private fun JSONArray.values(): List<Any> = List(length()) { get(it) }
    private fun array(values: Iterable<Any?>): JSONArray = JSONArray().apply { values.forEach { put(it ?: JSONObject.NULL) } }
    private fun copy(value: JSONObject): JSONObject = JSONObject(value.toString())
    private fun add(a: Long, b: Long = 1): Long = min(c("maxCounter"), a + b)
    private fun find(items: JSONArray, field: String, value: Any?): JSONObject? = items.objects().find { it.opt(field) == value }

    fun createLedger(options: JSONObject = JSONObject()): JSONObject = JSONObject().apply {
        put("schemaVersion", VERSION); put("policyVersion", POLICY_VERSION); put("enabled", options.opt("enabled") != false)
        put("learningEpoch", if (integer(options.opt("learningEpoch"))) options.n("learningEpoch") else 0)
        put("revision", 0); put("clockFloorMs", if (integer(options.opt("nowMs"))) options.n("nowMs") else 0)
        put("resetAtMs", JSONObject.NULL)
        listOf("tracks", "occurrences", "producers", "seenEventIds", "journal").forEach { put(it, JSONArray()) }
        put("diagnostics", JSONObject().apply { listOf("clockRollbacks", "futureClamps", "coverageUnderflows", "prunedOccurrences").forEach { put(it, 0) } })
    }

    fun normalizeEvent(input: JSONObject?, context: JSONObject = JSONObject()): JSONObject? {
        if (input == null || !eq(input.opt("schemaVersion"), VERSION.toLong()) || !id(input.opt("eventId")) || input.opt("type") !in TYPES || !integer(context.opt("nowMs"))) return null
        if (listOf("personal", "replay", "playing", "discontinuity", "userInitiated").any { input.has(it) && input.opt(it) !is Boolean } ||
            (input.has("source") && input.opt("source") !is String) ||
            (input.has("reason") && !input.isNull("reason") && input.opt("reason") !is String)) return null
        val timestamp = if (input.has("timestampMs")) input.opt("timestampMs") else context.opt("nowMs")
        if (!integer(timestamp) || (timestamp as Number).toLong() > context.n("nowMs") + c("maxFutureSkewMs")) return null
        if (input.has("learningEpoch") && !integer(input.opt("learningEpoch"))) return null
        val hasProducer = input.has("producerId") || input.has("sequence")
        if (hasProducer && (!id(input.opt("producerId")) || !integer(input.opt("sequence"), 1))) return null
        val type = input.s("type")
        if (type in listOf("offered", "activated", "interval", "outcome", "preference", "track-delete") && !id(input.opt("trackId"))) return null
        if (type in listOf("activated", "interval", "outcome") && !id(input.opt("occurrenceId"), 16384)) return null
        val rawReason = input.opt("reason")
        val event = JSONObject().put("schemaVersion", VERSION).put("eventId", input.s("eventId")).put("type", type)
            .put("timestampMs", min(timestamp.toLong(), context.n("nowMs")))
            .put("personal", input.opt("personal") != false && input.opt("source") !in listOf("remote", "session", "preview"))
            .put("reason", if (rawReason is String && rawReason in IGNORED_REASONS) rawReason else JSONObject.NULL)
        if (type == "outcome" && rawReason is String && rawReason.isNotEmpty() && rawReason !in IGNORED_REASONS && rawReason !in personalReasons) event.put("reason", "unclassified")
        if (timestamp.toLong() > context.n("nowMs")) event.put("futureClamped", true)
        if (input.has("learningEpoch")) event.put("learningEpoch", input.n("learningEpoch"))
        if (hasProducer) { event.put("producerId", input.s("producerId")); event.put("sequence", input.n("sequence")) }
        if (id(input.opt("trackId"))) event.put("trackId", input.s("trackId"))
        if (id(input.opt("occurrenceId"), 16384)) event.put("occurrenceId", input.s("occurrenceId"))
        if (type == "activated") {
            if (input.has("durationMs") && !input.isNull("durationMs") && !integer(input.opt("durationMs"), 1, c("maxDurationMs"))) return null
            event.put("durationMs", if (input.isNull("durationMs")) JSONObject.NULL else input.n("durationMs")); event.put("replay", input.opt("replay") == true)
        }
        if (type == "interval") {
            if (!id(input.opt("monotonicClockId")) || !integer(input.opt("monotonicStartMs")) || !integer(input.opt("monotonicEndMs")) ||
                input.n("monotonicEndMs") < input.n("monotonicStartMs") || input.n("monotonicEndMs") - input.n("monotonicStartMs") > c("maxIntervalMs") ||
                !integer(input.opt("mediaStartMs"), 0, c("maxDurationMs")) || !integer(input.opt("mediaEndMs"), 0, c("maxDurationMs"))) return null
            val speed = if (input.has("speedPermille")) input.opt("speedPermille") else 1000
            if (!integer(speed, c("speedMinimumPermille"), c("speedMaximumPermille"))) return null
            event.put("monotonicClockId", input.s("monotonicClockId"))
            listOf("monotonicStartMs", "monotonicEndMs", "mediaStartMs", "mediaEndMs").forEach { event.put(it, input.n(it)) }
            event.put("speedPermille", (speed as Number).toLong()).put("playing", input.opt("playing") == true).put("discontinuity", input.opt("discontinuity") == true)
        }
        if (type == "outcome") {
            if (input.opt("outcome") !in outcomes) return null
            event.put("outcome", input.s("outcome")).put("userInitiated", input.opt("userInitiated") == true)
        }
        if (type == "preference") {
            if (!integer(input.opt("value"), -1, 1)) return null
            event.put("value", input.n("value"))
        }
        if (type == "learning-enabled") {
            if (input.opt("enabled") !is Boolean) return null
            event.put("enabled", input.b("enabled"))
        }
        return event
    }

    private fun trackRow(trackId: String): JSONObject = JSONObject().put("trackId", trackId).apply {
        listOf("offered", "activated", "heardOccurrences", "naturalEnds", "deliberateSkips", "replays", "positiveUnits", "negativeUnits", "explicitPreference", "opportunityDebt", "heardWallMs").forEach { put(it, 0) }
        listOf("evidenceAtMs", "lastHeardAtMs", "explicitPreferenceAtMs").forEach { put(it, JSONObject.NULL) }
    }
    private fun decayed(row: JSONObject, nowMs: Long): Pair<Long, Long> {
        val periods = if (row.isNull("evidenceAtMs")) 0 else min(c("maxDecayPeriods"), max(0, nowMs - row.n("evidenceAtMs")) / c("decayPeriodMs")).toInt()
        val divisor = 1L shl periods
        return row.n("positiveUnits") / divisor to row.n("negativeUnits") / divisor
    }
    private fun recordEvidence(row: JSONObject, positive: Long, negative: Long, nowMs: Long, occurredAtMs: Long) {
        val old = decayed(row, nowMs)
        val periods = min(c("maxDecayPeriods"), max(0, nowMs - occurredAtMs) / c("decayPeriodMs")).toInt()
        row.put("positiveUnits", min(c("implicitUnitCap"), old.first + positive / (1L shl periods)))
        row.put("negativeUnits", min(c("implicitUnitCap"), old.second + negative / (1L shl periods)))
        row.put("evidenceAtMs", if (row.isNull("evidenceAtMs")) nowMs else row.n("evidenceAtMs") + max(0, nowMs - row.n("evidenceAtMs")) / c("decayPeriodMs") * c("decayPeriodMs"))
    }
    fun coverageMs(occurrence: JSONObject): Long = occurrence.a("coverageRanges").values().sumOf { (it as JSONArray).let { range -> range.getLong(1) - range.getLong(0) } }
    private fun mergeCoverage(occurrence: JSONObject, start: Long, end: Long, diagnostics: JSONObject) {
        if (end <= start) return
        val ranges = (occurrence.a("coverageRanges").values().map { (it as JSONArray).let { r -> r.getLong(0) to r.getLong(1) } } + (start to end))
            .sortedWith(compareBy<Pair<Long, Long>> { it.first }.thenBy { it.second })
        val merged = mutableListOf<Pair<Long, Long>>()
        for (range in ranges) {
            val previous = merged.lastOrNull()
            if (previous != null && range.first <= previous.second) merged[merged.lastIndex] = previous.first to max(previous.second, range.second)
            else merged.add(range)
        }
        if (merged.size > c("maxCoverageRanges")) { diagnostics.put("coverageUnderflows", add(diagnostics.n("coverageUnderflows"))); return }
        occurrence.put("coverageRanges", array(merged.map { JSONArray().put(it.first).put(it.second) }))
    }
    private fun duplicateOrUnacceptable(ledger: JSONObject, event: JSONObject): Boolean {
        if (event.s("eventId") in ledger.a("seenEventIds").values()) return true
        if (event.has("learningEpoch") && event.n("learningEpoch") != ledger.n("learningEpoch")) return true
        if (event.has("producerId")) {
            val producer = find(ledger.a("producers"), "producerId", event.s("producerId"))
            if (producer == null) return ledger.a("producers").length() >= c("maxProducers") || event.n("sequence") > c("maxProducerGap")
            return event.n("sequence") <= producer.n("through") || producer.a("pending").values().any { eq(it, event.n("sequence")) } || event.n("sequence") - producer.n("through") > c("maxProducerGap")
        }
        return false
    }
    private fun remember(ledger: JSONObject, event: JSONObject) {
        val seen = ledger.a("seenEventIds"); seen.put(event.s("eventId")); if (seen.length() > c("maxEventIds")) seen.remove(0)
        if (!event.has("producerId")) return
        var producer = find(ledger.a("producers"), "producerId", event.s("producerId"))
        if (producer == null) {
            producer = JSONObject().put("producerId", event.s("producerId")).put("through", 0).put("pending", JSONArray())
            ledger.a("producers").put(producer)
            ledger.put("producers", array(ledger.a("producers").objects().sortedBy { it.s("producerId") }))
        }
        val pending = (producer.a("pending").values().map { (it as Number).toLong() } + event.n("sequence")).sorted().toMutableList()
        while (pending.firstOrNull() == producer.n("through") + 1) { producer.put("through", producer.n("through") + 1); pending.removeAt(0) }
        producer.put("pending", array(pending))
    }
    private fun compactOccurrences(ledger: JSONObject) {
        val closed = ledger.a("occurrences").objects().filter { it.b("ended") }
        if (closed.size <= c("maxClosedOccurrences")) return
        val prune = closed.take(closed.size - c("maxClosedOccurrences").toInt()).map { it.s("occurrenceId") }.toSet()
        ledger.put("occurrences", array(ledger.a("occurrences").objects().filter { it.s("occurrenceId") !in prune }))
        val diagnostic = ledger.getJSONObject("diagnostics")
        diagnostic.put("prunedOccurrences", add(diagnostic.n("prunedOccurrences"), prune.size.toLong()))
    }

    fun reduce(inputLedger: JSONObject, inputEvent: JSONObject?, context: JSONObject = JSONObject()): JSONObject {
        if (!eq(inputLedger.opt("schemaVersion"), VERSION.toLong()) || inputLedger.opt("policyVersion") != POLICY_VERSION) return inputLedger
        val rawNow = if (context.has("nowMs")) context.opt("nowMs") else inputLedger.opt("clockFloorMs")
        if (!integer(rawNow)) return inputLedger
        val nowMs = max((rawNow as Number).toLong(), inputLedger.n("clockFloorMs"))
        val event = normalizeEvent(inputEvent, JSONObject().put("nowMs", nowMs)) ?: return inputLedger
        if (duplicateOrUnacceptable(inputLedger, event)) return inputLedger
        val type = event.s("type")
        if (type in controls && !event.b("personal")) return inputLedger
        if (type == "activated" && find(inputLedger.a("occurrences"), "occurrenceId", event.opt("occurrenceId")) == null && inputLedger.a("occurrences").objects().count { !it.b("ended") } >= c("maxOpenOccurrences")) return inputLedger
        val ledger = copy(inputLedger)
        ledger.put("revision", add(ledger.n("revision"))).put("clockFloorMs", nowMs)
        val diagnostics = ledger.getJSONObject("diagnostics")
        if (rawNow.toLong() < inputLedger.n("clockFloorMs")) diagnostics.put("clockRollbacks", add(diagnostics.n("clockRollbacks")))
        if (event.opt("futureClamped") == true) diagnostics.put("futureClamps", add(diagnostics.n("futureClamps")))
        remember(ledger, event)
        val suppressed = type !in controls && (!ledger.b("enabled") || !event.b("personal") || !event.isNull("reason"))
        var applied = !suppressed
        var row = find(ledger.a("tracks"), "trackId", event.opt("trackId"))
        val occurrence = find(ledger.a("occurrences"), "occurrenceId", event.opt("occurrenceId"))
        when (type) {
            "learning-enabled" -> {
                ledger.put("enabled", event.b("enabled"))
                if (!event.b("enabled")) ledger.a("occurrences").objects().forEach { it.put("ended", true) }
            }
            "learning-reset" -> {
                ledger.put("tracks", JSONArray()).put("occurrences", JSONArray()).put("journal", JSONArray())
                ledger.put("learningEpoch", add(ledger.n("learningEpoch"))).put("resetAtMs", nowMs)
            }
            "history-delete" -> {
                ledger.put("journal", JSONArray()).put("occurrences", array(ledger.a("occurrences").objects().filter { !it.b("ended") }))
                ledger.a("tracks").objects().forEach { it.put("lastHeardAtMs", JSONObject.NULL) }
            }
            "track-delete" -> {
                ledger.put("tracks", array(ledger.a("tracks").objects().filter { it.opt("trackId") != event.opt("trackId") }))
                ledger.put("occurrences", array(ledger.a("occurrences").objects().filter { it.opt("trackId") != event.opt("trackId") }))
                ledger.put("journal", array(ledger.a("journal").objects().filter { it.opt("trackId") != event.opt("trackId") }))
            }
            else -> if (!suppressed) {
                if (row == null && type in listOf("offered", "activated", "preference") && !(type == "activated" && occurrence != null)) {
                    row = trackRow(event.s("trackId")); ledger.a("tracks").put(row)
                    ledger.put("tracks", array(ledger.a("tracks").objects().sortedBy { it.s("trackId") }))
                }
                when (type) {
                    "offered" -> { row!!.put("offered", add(row.n("offered"))); row.put("opportunityDebt", min(c("debtLimit"), row.n("opportunityDebt") + 1)) }
                    "activated" -> if (occurrence != null) applied = false else {
                        row!!.put("activated", add(row.n("activated"))).put("opportunityDebt", 0)
                        if (event.b("replay")) row.put("replays", add(row.n("replays")))
                        ledger.a("occurrences").put(JSONObject().put("occurrenceId", event.s("occurrenceId")).put("trackId", event.s("trackId"))
                            .put("activatedAtMs", event.n("timestampMs")).put("durationMs", event.get("durationMs")).put("replay", event.b("replay"))
                            .put("wallMs", 0).put("coverageRanges", JSONArray()).put("clocks", JSONArray()).put("heardCredited", false).put("ended", false))
                    }
                    "preference" -> if (row!!.isNull("explicitPreferenceAtMs") || event.n("timestampMs") >= row.n("explicitPreferenceAtMs")) {
                        row.put("explicitPreference", event.n("value")).put("explicitPreferenceAtMs", event.n("timestampMs"))
                    } else applied = false
                    else -> if (occurrence == null || occurrence.opt("trackId") != event.opt("trackId") || occurrence.b("ended") || row == null) applied = false
                    else if (type == "interval") {
                        if (!event.b("playing") || event.b("discontinuity") || event.n("mediaEndMs") < event.n("mediaStartMs")) applied = false
                        else {
                            var clock = find(occurrence.a("clocks"), "clockId", event.s("monotonicClockId"))
                            if (clock == null && occurrence.a("clocks").length() >= c("maxClocksPerOccurrence")) applied = false
                            else {
                                if (clock == null) { clock = JSONObject().put("clockId", event.s("monotonicClockId")).put("throughMs", 0); occurrence.a("clocks").put(clock) }
                                val start = max(event.n("monotonicStartMs"), clock.n("throughMs"))
                                val wall = max(0, event.n("monotonicEndMs") - start)
                                val fullWall = event.n("monotonicEndMs") - event.n("monotonicStartMs")
                                clock.put("throughMs", max(clock.n("throughMs"), event.n("monotonicEndMs")))
                                val mediaSpan = event.n("mediaEndMs") - event.n("mediaStartMs")
                                val plausible = mediaSpan <= fullWall * event.n("speedPermille") / 1000 + c("mediaProgressToleranceMs")
                                val trim = if (fullWall > 0) mediaSpan * min(fullWall, start - event.n("monotonicStartMs")) / fullWall else mediaSpan
                                if (wall > 0 && mediaSpan > 0 && plausible) {
                                    occurrence.put("wallMs", add(occurrence.n("wallMs"), wall)); row.put("heardWallMs", add(row.n("heardWallMs"), wall))
                                    if (!occurrence.b("heardCredited")) { row.put("heardOccurrences", add(row.n("heardOccurrences"))); occurrence.put("heardCredited", true) }
                                    row.put("lastHeardAtMs", max(if (row.isNull("lastHeardAtMs")) 0 else row.n("lastHeardAtMs"), event.n("timestampMs")))
                                    mergeCoverage(occurrence, event.n("mediaStartMs") + trim, min(event.n("mediaEndMs"), if (occurrence.isNull("durationMs")) c("maxDurationMs") else occurrence.n("durationMs")), diagnostics)
                                }
                            }
                        }
                    } else if (type == "outcome") {
                        occurrence.put("ended", true)
                        val coverage = if (occurrence.isNull("durationMs")) 0 else min(1000, coverageMs(occurrence) * 1000 / occurrence.n("durationMs"))
                        val enoughWall = !occurrence.isNull("durationMs") && occurrence.n("wallMs") >= min(c("completionWallCapMs"), occurrence.n("durationMs") * c("completionWallPermille") / 1000)
                        if (event.s("outcome") == "natural-end") {
                            row.put("naturalEnds", add(row.n("naturalEnds")))
                            if (coverage >= c("completionCoveragePermille") && enoughWall) recordEvidence(row, c("positiveCompletionUnits"), 0, nowMs, event.n("timestampMs"))
                        } else if (event.s("outcome") == "replay" && event.b("userInitiated") && enoughWall && coverage >= c("completionCoveragePermille")) recordEvidence(row, c("positiveReplayUnits"), 0, nowMs, event.n("timestampMs"))
                        else if (event.s("outcome") == "deliberate-skip" && event.b("userInitiated")) {
                            row.put("deliberateSkips", add(row.n("deliberateSkips")))
                            if (occurrence.n("wallMs") >= c("earlySkipMinimumMs") && occurrence.n("wallMs") < c("earlySkipMaximumMs") && !occurrence.isNull("durationMs") && coverage < c("earlySkipCoveragePermille")) recordEvidence(row, 0, c("negativeEarlySkipUnits"), nowMs, event.n("timestampMs"))
                        }
                    }
                }
            } else if (type == "outcome" && occurrence != null && occurrence.opt("trackId") == event.opt("trackId")) occurrence.put("ended", true)
        }
        val journal = JSONObject().put("eventId", event.s("eventId")).put("type", type).put("timestampMs", event.n("timestampMs")).put("applied", applied)
        if (event.has("trackId") && type != "track-delete") journal.put("trackId", event.s("trackId"))
        if (event.has("occurrenceId")) journal.put("occurrenceId", event.s("occurrenceId"))
        if (suppressed) journal.put("suppressed", if (!ledger.b("enabled")) "disabled" else if (!event.b("personal")) "nonpersonal" else "infrastructure")
        if (type == "outcome") journal.put("outcome", event.s("outcome"))
        ledger.a("journal").put(journal); if (ledger.a("journal").length() > c("maxJournal")) ledger.a("journal").remove(0)
        compactOccurrences(ledger)
        return ledger
    }

    fun trackSignals(ledger: JSONObject?, trackId: String, context: JSONObject = JSONObject()): JSONObject {
        val neutral = JSONObject().put("tastePoints", 0).put("exposure", 0).put("noveltyPoints", 0).put("lastHeardAtMs", JSONObject.NULL).put("confidencePermille", 0)
        if (ledger == null || !eq(ledger.opt("schemaVersion"), VERSION.toLong()) || ledger.opt("policyVersion") != POLICY_VERSION || !ledger.b("enabled")) return neutral
        val now = if (context.has("nowMs")) context.opt("nowMs") else ledger.opt("clockFloorMs")
        if (!integer(now)) return neutral
        val row = find(ledger.a("tracks"), "trackId", trackId) ?: return neutral.put("noveltyPoints", c("noveltyLimit"))
        val evidence = decayed(row, max((now as Number).toLong(), ledger.n("clockFloorMs")))
        val total = evidence.first + evidence.second
        val implicit = (evidence.first - evidence.second) * c("implicitScale") / (total + c("priorUnits"))
        return JSONObject().put("tastePoints", (row.n("explicitPreference") * c("explicitPoints") + implicit).coerceIn(-c("tasteLimit"), c("tasteLimit")))
            .put("exposure", row.n("activated")).put("noveltyPoints", (c("noveltyLimit") - min(c("noveltyLimit"), row.n("activated") * c("exposureNoveltyStep")) + row.n("opportunityDebt")).coerceIn(0, c("noveltyLimit")))
            .put("lastHeardAtMs", row.get("lastHeardAtMs")).put("confidencePermille", total * c("confidenceScale") / (total + c("priorUnits")))
    }
    fun summarize(ledger: JSONObject): JSONObject = JSONObject().put("schemaVersion", VERSION).put("policyVersion", POLICY_VERSION).put("enabled", ledger.b("enabled"))
        .put("tracks", ledger.a("tracks").length()).put("offers", ledger.a("tracks").objects().sumOf { it.n("offered") })
        .put("activations", ledger.a("tracks").objects().sumOf { it.n("activated") }).put("heardOccurrences", ledger.a("tracks").objects().sumOf { it.n("heardOccurrences") })
        .put("heardWallMs", ledger.a("tracks").objects().sumOf { it.n("heardWallMs") }).put("retainedEvents", ledger.a("journal").length())
    fun exposureMetrics(ledger: JSONObject, trackIds: JSONArray? = null): JSONObject {
        val ids = (trackIds?.values() ?: ledger.a("tracks").objects().map { it.s("trackId") }).distinct()
        val rows = ids.map { find(ledger.a("tracks"), "trackId", it) }
        val activated = rows.count { it != null && it.n("activated") > 0 }
        val heard = rows.count { it != null && it.n("heardOccurrences") > 0 }
        return JSONObject().put("eligible", ids.size).put("activated", activated).put("heard", heard).put("neverActivated", ids.size - activated)
            .put("activationCoveragePermille", if (ids.isEmpty()) 0 else activated * 1000L / ids.size).put("heardCoveragePermille", if (ids.isEmpty()) 0 else heard * 1000L / ids.size)
    }
    fun exportLedger(ledger: JSONObject, options: JSONObject = JSONObject()): JSONObject {
        check(options.opt("includePrivate") == true) { "Private learning export requires includePrivate: true" }
        return copy(ledger)
    }

    fun importLedger(value: Any?): JSONObject {
        val input: Any? = try {
            if (value is String) parseStrictJson(value)
            else when (value) { is JSONObject -> copy(value); is JSONArray -> JSONArray(value.toString()); else -> value }
        } catch (_: Exception) { return JSONObject().put("ok", false).put("reason", "invalid-json").put("preserved", value ?: JSONObject.NULL) }
        if (input !is JSONObject || !eq(input.opt("schemaVersion"), VERSION.toLong()) || input.opt("policyVersion") != POLICY_VERSION)
            return JSONObject().put("ok", false).put("reason", "unsupported-version").put("preserved", input ?: JSONObject.NULL)
        val valid = runCatching { validateLedger(input) }.getOrDefault(false)
        return if (valid) JSONObject().put("ok", true).put("ledger", input) else JSONObject().put("ok", false).put("reason", "invalid-ledger").put("preserved", input)
    }
    private fun keys(item: Any?, required: Set<String>, optional: Set<String> = emptySet()): Boolean = item is JSONObject && required.all { item.has(it) } && item.keys().asSequence().all { it in required || it in optional }
    private fun sortedIds(items: JSONArray, field: String): Boolean = items.objects().zipWithNext().all { (a, b) -> a.s(field) < b.s(field) }
    private fun validateLedger(input: JSONObject): Boolean {
        if (!keys(input, createLedger().keys().asSequence().toSet()) || input.opt("enabled") !is Boolean || !integer(input.opt("revision"), 0, c("maxCounter")) ||
            !integer(input.opt("learningEpoch"), 0, c("maxCounter")) || !integer(input.opt("clockFloorMs")) ||
            (!input.isNull("resetAtMs") && !integer(input.opt("resetAtMs"), 0, input.n("clockFloorMs")))) return false
        val tracks = input.opt("tracks") as? JSONArray ?: return false
        val occurrences = input.opt("occurrences") as? JSONArray ?: return false
        val producers = input.opt("producers") as? JSONArray ?: return false
        val seen = input.opt("seenEventIds") as? JSONArray ?: return false
        val journal = input.opt("journal") as? JSONArray ?: return false
        val validTracks = tracks.objects().all { t ->
            keys(t, trackRow("schema").keys().asSequence().toSet()) && id(t.opt("trackId")) &&
                listOf("offered", "activated", "heardOccurrences", "naturalEnds", "deliberateSkips", "replays", "positiveUnits", "negativeUnits", "opportunityDebt", "heardWallMs").all { integer(t.opt(it), 0, c("maxCounter")) } &&
                t.n("positiveUnits") <= c("implicitUnitCap") && t.n("negativeUnits") <= c("implicitUnitCap") && t.n("opportunityDebt") <= c("debtLimit") &&
                listOf("heardOccurrences", "naturalEnds", "deliberateSkips", "replays").all { t.n(it) <= t.n("activated") } &&
                (!t.isNull("evidenceAtMs") || (t.n("positiveUnits") == 0L && t.n("negativeUnits") == 0L)) &&
                (!t.isNull("explicitPreferenceAtMs") || eq(t.opt("explicitPreference"), 0)) && integer(t.opt("explicitPreference"), -1, 1) &&
                listOf("evidenceAtMs", "lastHeardAtMs", "explicitPreferenceAtMs").all { t.isNull(it) || integer(t.opt(it), 0, input.n("clockFloorMs")) }
        }
        if (!validTracks || !sortedIds(tracks, "trackId")) return false
        val validOccurrences = occurrences.objects().all { o ->
            if (!keys(o, setOf("occurrenceId", "trackId", "activatedAtMs", "durationMs", "replay", "wallMs", "coverageRanges", "clocks", "heardCredited", "ended")) ||
                !id(o.opt("occurrenceId"), 16384) || !id(o.opt("trackId")) || find(tracks, "trackId", o.opt("trackId")) == null ||
                !integer(o.opt("activatedAtMs"), 0, input.n("clockFloorMs")) || (!o.isNull("durationMs") && !integer(o.opt("durationMs"), 1, c("maxDurationMs"))) ||
                o.opt("replay") !is Boolean || !integer(o.opt("wallMs"), 0, c("maxCounter")) || o.opt("heardCredited") !is Boolean || o.opt("ended") !is Boolean || o.b("heardCredited") != (o.n("wallMs") > 0)) false
            else {
                val ranges = o.opt("coverageRanges") as? JSONArray
                val clocks = o.opt("clocks") as? JSONArray
                ranges != null && ranges.length() <= c("maxCoverageRanges") && ranges.values().withIndex().all { (i, r) ->
                    r is JSONArray && r.length() == 2 && integer(r.opt(0), 0, c("maxDurationMs")) && integer(r.opt(1), r.getLong(0) + 1, if (o.isNull("durationMs")) c("maxDurationMs") else o.n("durationMs")) && (i == 0 || r.getLong(0) > ranges.getJSONArray(i - 1).getLong(1))
                } && clocks != null && clocks.length() <= c("maxClocksPerOccurrence") && clocks.objects().all {
                    keys(it, setOf("clockId", "throughMs")) && id(it.opt("clockId")) && integer(it.opt("throughMs"))
                } && clocks.objects().map { it.s("clockId") }.distinct().size == clocks.length()
            }
        }
        if (!validOccurrences || occurrences.objects().count { !it.b("ended") } > c("maxOpenOccurrences") || occurrences.objects().count { it.b("ended") } > c("maxClosedOccurrences") || occurrences.objects().map { it.s("occurrenceId") }.distinct().size != occurrences.length()) return false
        if (producers.length() > c("maxProducers") || !producers.objects().all { p ->
                if (!keys(p, setOf("producerId", "through", "pending")) || !id(p.opt("producerId")) || !integer(p.opt("through")) || p.opt("pending") !is JSONArray) false
                else p.a("pending").length() <= c("maxProducerGap") && p.a("pending").values().withIndex().all { (i, seq) ->
                    integer(seq, p.n("through") + 1) && (seq as Number).toLong() - p.n("through") <= c("maxProducerGap") && (i == 0 || seq.toLong() > p.a("pending").getLong(i - 1))
                } && p.a("pending").values().none { eq(it, p.n("through") + 1) }
            } || !sortedIds(producers, "producerId")) return false
        if (seen.length() > c("maxEventIds") || !seen.values().all { id(it) } || seen.values().distinct().size != seen.length()) return false
        if (journal.length() > c("maxJournal") || !journal.objects().all { e -> keys(e, setOf("eventId", "type", "timestampMs", "applied"), setOf("trackId", "occurrenceId", "outcome", "suppressed")) &&
                id(e.opt("eventId")) && e.opt("type") in TYPES && integer(e.opt("timestampMs"), 0, input.n("clockFloorMs")) && e.opt("applied") is Boolean &&
                (!e.has("trackId") || id(e.opt("trackId"))) && (!e.has("occurrenceId") || id(e.opt("occurrenceId"), 16384)) && (!e.has("outcome") || e.opt("outcome") in outcomes) && (!e.has("suppressed") || e.opt("suppressed") in listOf("disabled", "nonpersonal", "infrastructure")) }) return false
        val diagnostics = input.opt("diagnostics") as? JSONObject ?: return false
        return keys(diagnostics, setOf("clockRollbacks", "futureClamps", "coverageUnderflows", "prunedOccurrences")) && diagnostics.keys().asSequence().all { integer(diagnostics.opt(it), 0, c("maxCounter")) }
    }
    private fun control(ledger: JSONObject, type: String, event: JSONObject, context: JSONObject): JSONObject = reduce(ledger, copy(event).put("schemaVersion", VERSION).put("type", type), context)
    fun setEnabled(ledger: JSONObject, enabled: Boolean, event: JSONObject, context: JSONObject = JSONObject()) = control(ledger, "learning-enabled", copy(event).put("enabled", enabled), context)
    fun resetLearning(ledger: JSONObject, event: JSONObject, context: JSONObject = JSONObject()) = control(ledger, "learning-reset", event, context)
    fun deleteHistory(ledger: JSONObject, event: JSONObject, context: JSONObject = JSONObject()) = control(ledger, "history-delete", event, context)
    fun deleteTrack(ledger: JSONObject, trackId: String, event: JSONObject, context: JSONObject = JSONObject()) = control(ledger, "track-delete", copy(event).put("trackId", trackId), context)
    fun deleteLedger(ledger: JSONObject, options: JSONObject = JSONObject()) = createLedger(JSONObject().put("enabled", options.opt("enabled") == true)
        .put("nowMs", if (integer(options.opt("nowMs"))) options.n("nowMs") else ledger.n("clockFloorMs")).put("learningEpoch", add(ledger.n("learningEpoch"))))

    /** Strict JSON grammar, including JSON.parse's last-value-wins duplicate keys. */
    private fun parseStrictJson(text: String): Any {
        var i = 0
        fun ws() { while (i < text.length && text[i] in " \t\r\n") i++ }
        fun string(): String {
            val start = i
            require(i < text.length && text[i++] == '"')
            while (i < text.length) {
                val ch = text[i++]
                if (ch == '"') return JSONTokener(text.substring(start, i)).nextValue() as String
                require(ch >= ' ')
                if (ch == '\\') {
                    require(i < text.length)
                    val escape = text[i++]
                    if (escape == 'u') { repeat(4) { require(i < text.length && text[i++] in "0123456789abcdefABCDEF") } }
                    else require(escape in "\"\\/bfnrt")
                }
            }
            error("Unterminated JSON string")
        }
        lateinit var value: () -> Any
        value = fun(): Any {
            ws(); require(i < text.length)
            when (text[i]) {
                '"' -> return string()
                '{', '[' -> {
                    val obj = text[i++] == '{'; val close = if (obj) '}' else ']'
                    val result: Any = if (obj) JSONObject() else JSONArray()
                    ws()
                    if (i < text.length && text[i] == close) { i++; return result }
                    while (true) {
                        if (obj) {
                            ws(); val key = string(); ws(); require(i < text.length && text[i++] == ':')
                            (result as JSONObject).put(key, value())
                        } else (result as JSONArray).put(value())
                        ws(); require(i < text.length)
                        val delimiter = text[i++]; if (delimiter == close) return result
                        require(delimiter == ',')
                    }
                }
                else -> {
                    for ((literal, parsed) in listOf("true" to true, "false" to false, "null" to JSONObject.NULL))
                        if (text.startsWith(literal, i)) { i += literal.length; return parsed }
                    val match = Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?").find(text, i) ?: error("Invalid JSON value")
                    require(match.range.first == i)
                    i = match.range.last + 1
                    val parsed = match.value.toDouble()
                    if (!parsed.isFinite()) return JsonNonFinite(parsed)
                    return if (parsed == 0.0) 0L else if (parsed % 1.0 == 0.0 && parsed >= Long.MIN_VALUE.toDouble() && parsed < Long.MAX_VALUE.toDouble()) parsed.toLong() else parsed
                }
            }
            error("Invalid JSON")
        }
        val result = value(); ws(); require(i == text.length); return result
    }
    /** JSON.stringify serializes nonfinite parsed JS numbers as null. */
    private class JsonNonFinite(private val value: Double) : Number() {
        override fun toByte() = value.toInt().toByte()
        override fun toDouble() = value
        override fun toFloat() = value.toFloat()
        override fun toInt() = value.toInt()
        override fun toLong() = value.toLong()
        override fun toShort() = value.toInt().toShort()
        override fun toString() = "null"
    }
}
