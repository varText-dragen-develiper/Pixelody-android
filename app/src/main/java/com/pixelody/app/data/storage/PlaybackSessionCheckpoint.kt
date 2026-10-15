package com.pixelody.app.data.storage

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/** Private local checkpoint; this is deliberately not a public host snapshot/export. */
data class PlaybackQueueOccurrence(
    val entryId: String,
    val trackId: String,
    val mediaUri: String?,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String? = null,
    val discNumber: Int? = null,
    val trackNumber: Int? = null,
    val durationMs: Long? = null,
    val durationSeconds: Int? = null,
    val mimeType: String? = null,
    val format: String = "",
    val lossless: Boolean? = null,
    val artworkUri: String? = null,
    val unavailableReason: String? = null
)

data class PlaybackSessionCheckpoint(
    val sessionId: String,
    val revision: Long,
    val queue: List<PlaybackQueueOccurrence>,
    val currentIndex: Int = if (queue.isEmpty()) -1 else 0,
    val positionMs: Long = 0,
    /** Media3 explicit repeat constants: off=0, one=1, all=2; never enum ordinals. */
    val repeatMode: Int = 0,
    val shuffleMode: String = "off",
    /** Entire private policy state, including seed, entitlement, history and manual lanes. */
    val policyStateJson: String = "{}",
    val pendingStageJson: String? = null,
    val authorityId: String = "local",
    val authorityEpoch: Long = 0,
    val origin: String = "native",
    /** Caller supplies the durable library/playlist/custom queue identity, independent of revision. */
    val scopeId: String = sessionId,
    val currentOccurrenceId: String? = null,
    /** Written for diagnostics only. Readers always return false. */
    val playWhenReady: Boolean = false
)

data class SessionDiagnostic(val code: String, val entryId: String? = null)

data class DecodedPlaybackSession(
    val checkpoint: PlaybackSessionCheckpoint,
    val diagnostics: List<SessionDiagnostic>,
    /** Invalidate this reservation in the adapter. It was never activation evidence. */
    val discardedPendingStageJson: String? = null
)

/** No Android dependency: production JSON parsing/recovery is exercised by plain JVM tests. */
object PlaybackSessionCodec {
    const val SCHEMA_VERSION = 1
    const val ENTRY_ID_EXTRA = "pixelody.entryId"
    const val MAX_FILE_BYTES = 32 * 1024 * 1024
    private const val MAX_QUEUE_ITEMS = 50000
    private const val MAX_POLICY_CHARS = 16 * 1024 * 1024
    private val modes = setOf("off", "random", "balanced", "flow", "album")
    private val credentialKeys = setOf(
        "token", "accesstoken", "refreshtoken", "idtoken", "authtoken", "authorization",
        "password", "passwd", "secret", "clientsecret", "credential", "credentials", "apikey",
        "xapikey", "cookie", "setcookie", "signature", "xamzsignature", "xamzcredential",
        "keypairid"
    )
    private val uriCredentialKeys = credentialKeys + "policy"

    fun schemaVersion(raw: String): Long {
        val value = JSONObject(raw).get("schemaVersion")
        require(value is Number) { "Invalid schema version" }
        val number = value.toDouble()
        require(number.isFinite() && number >= 0 && number % 1.0 == 0.0) { "Invalid schema version" }
        // Recognize huge newer versions as future rather than corrupting/overwriting them.
        return if (number >= Long.MAX_VALUE.toDouble()) Long.MAX_VALUE else number.toLong()
    }

    fun encode(checkpoint: PlaybackSessionCheckpoint): String {
        validate(checkpoint)
        val diagnostics = mutableListOf<SessionDiagnostic>()
        val entries = JSONArray()
        checkpoint.queue.forEach { entry -> entries.put(encodeEntry(safeEntry(entry, diagnostics))) }
        val payload = JSONObject()
            .put("sessionId", checkpoint.sessionId).put("revision", checkpoint.revision)
            .put("queue", entries).put("currentIndex", checkpoint.currentIndex)
            .put("positionMs", checkpoint.positionMs).put("repeatMode", checkpoint.repeatMode)
            .put("shuffleMode", checkpoint.shuffleMode).put("policyStateJson", checkpoint.policyStateJson)
            .put("pendingStageJson", checkpoint.pendingStageJson ?: JSONObject.NULL)
            .put("authorityId", checkpoint.authorityId).put("authorityEpoch", checkpoint.authorityEpoch)
            .put("scopeId", checkpoint.scopeId).put("currentOccurrenceId", checkpoint.currentOccurrenceId ?: JSONObject.NULL)
            .put("origin", checkpoint.origin).put("playWhenReady", checkpoint.playWhenReady).toString()
        // Hash the stored string, not JSONObject ordering (Android/JVM ordering differs).
        return JSONObject().put("schemaVersion", SCHEMA_VERSION).put("payloadJson", payload)
            .put("sha256", sha256(payload)).toString().also {
                require(it.toByteArray(StandardCharsets.UTF_8).size <= MAX_FILE_BYTES) { "Checkpoint too large" }
            }
    }

    fun decode(raw: String): DecodedPlaybackSession {
        val envelope = JSONObject(raw)
        require(envelope.requiredInt("schemaVersion") == SCHEMA_VERSION) { "Unsupported checkpoint schema" }
        val payload = envelope.requiredString("payloadJson")
        require(envelope.requiredString("sha256") == sha256(payload)) { "Checkpoint checksum mismatch" }
        val state = JSONObject(payload)
        val diagnostics = mutableListOf<SessionDiagnostic>()
        val entries = state.getJSONArray("queue")
        require(entries.length() <= MAX_QUEUE_ITEMS) { "Queue too large" }
        val queue = (0 until entries.length()).map { index ->
            val item = entries.getJSONObject(index)
            safeEntry(PlaybackQueueOccurrence(
                entryId = item.requiredString("entryId"), trackId = item.requiredString("trackId"),
                mediaUri = item.nullableString("mediaUri"), title = item.requiredString("title"),
                artist = item.requiredString("artist"), album = item.requiredString("album"),
                albumArtist = item.nullableString("albumArtist"), discNumber = item.nullableInt("discNumber"),
                trackNumber = item.nullableInt("trackNumber"), durationMs = item.nullableLong("durationMs"),
                durationSeconds = item.nullableInt("durationSeconds"), mimeType = item.nullableString("mimeType"),
                format = item.requiredString("format"), lossless = item.nullableBoolean("lossless"),
                artworkUri = item.nullableString("artworkUri"), unavailableReason = item.nullableString("unavailableReason")
            ), diagnostics)
        }
        val pending = state.nullableString("pendingStageJson")
        val checkpoint = PlaybackSessionCheckpoint(
            sessionId = state.requiredString("sessionId"), revision = state.requiredLong("revision"),
            queue = queue, currentIndex = state.requiredInt("currentIndex"), positionMs = state.requiredLong("positionMs"),
            repeatMode = state.requiredInt("repeatMode"), shuffleMode = state.requiredString("shuffleMode"),
            policyStateJson = state.requiredString("policyStateJson"), pendingStageJson = null,
            authorityId = state.requiredString("authorityId"), authorityEpoch = state.requiredLong("authorityEpoch"),
            scopeId = state.optString("scopeId", state.requiredString("sessionId")),
            currentOccurrenceId = state.nullableString("currentOccurrenceId"),
            origin = state.requiredString("origin"), playWhenReady = false
        )
        validate(checkpoint)
        pending?.let {
            validatePrivateJson(it)
            diagnostics += SessionDiagnostic("pending_activation_discarded")
        }
        return DecodedPlaybackSession(checkpoint, diagnostics, pending)
    }

    /** Honest one-item import from pixelody_playback/restorable_playback; caller keeps the preference. */
    fun importLegacy(raw: String): DecodedPlaybackSession {
        val old = JSONObject(raw)
        val trackId = old.requiredString("mediaId")
        require(trackId.isNotBlank()) { "Missing legacy media ID" }
        val stableId = UUID.nameUUIDFromBytes(raw.toByteArray(StandardCharsets.UTF_8)).toString()
        val diagnostics = mutableListOf(SessionDiagnostic("legacy_single_item_unknown_traversal"))
        val queue = listOf(safeEntry(PlaybackQueueOccurrence(
            entryId = "legacy-$stableId", trackId = trackId, mediaUri = old.nullableString("uri"),
            title = old.optString("title", ""), artist = old.optString("artist", ""),
            album = old.optString("album", ""), artworkUri = old.nullableString("artworkUri")
        ), diagnostics))
        val checkpoint = PlaybackSessionCheckpoint(
            sessionId = "legacy-$stableId", revision = 0, queue = queue,
            positionMs = if (old.has("positionMs")) old.requiredLong("positionMs").coerceAtLeast(0) else 0,
            origin = "legacy-single-item-unknown-history"
        )
        validate(checkpoint)
        return DecodedPlaybackSession(checkpoint, diagnostics)
    }

    /** URI grants/credentials are still checked by the service. No credential-bearing URI is persisted. */
    fun isSafeMediaUri(value: String?): Boolean {
        if (value.isNullOrBlank() || value.length > 65536 || value.any { it.code < 32 }) return false
        return runCatching {
            val uri = URI(value)
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
            if (scheme !in setOf("file", "content", "android.resource", "http", "https")) return false
            if (uri.rawUserInfo != null || uri.rawFragment != null) return false
            if (scheme in setOf("http", "https") && uri.host.isNullOrBlank()) return false
            if (scheme == "file" && (uri.path.isNullOrBlank() || !uri.path.startsWith("/"))) return false
            if (scheme in setOf("content", "android.resource") && uri.rawAuthority.isNullOrBlank()) return false
            uri.rawQuery?.split('&', ';')?.forEach { pair ->
                val key = URLDecoder.decode(pair.substringBefore('='), "UTF-8").normalizedKey()
                if (key in uriCredentialKeys) return false
            }
            true
        }.getOrDefault(false)
    }

    private fun validate(checkpoint: PlaybackSessionCheckpoint) {
        require(checkpoint.sessionId.isNotBlank() && checkpoint.authorityId.isNotBlank() && checkpoint.scopeId.isNotBlank()) { "Missing session identity" }
        require(checkpoint.revision >= 0 && checkpoint.authorityEpoch >= 0 && checkpoint.positionMs >= 0) { "Invalid checkpoint number" }
        require(checkpoint.queue.size <= MAX_QUEUE_ITEMS) { "Queue too large" }
        require(if (checkpoint.queue.isEmpty()) checkpoint.currentIndex == -1 else checkpoint.currentIndex in checkpoint.queue.indices) { "Invalid current index" }
        require(checkpoint.repeatMode in 0..2 && checkpoint.shuffleMode in modes) { "Invalid playback mode" }
        val ids = HashSet<String>()
        checkpoint.queue.forEach { entry ->
            require(entry.entryId.isNotBlank() && entry.trackId.isNotBlank() && ids.add(entry.entryId)) { "Missing or duplicate occurrence ID" }
            require(entry.durationMs == null || entry.durationMs >= 0) { "Invalid duration" }
            require(entry.durationSeconds == null || entry.durationSeconds >= 0) { "Invalid duration" }
            require(entry.discNumber == null || entry.discNumber >= 0) { "Invalid disc number" }
            require(entry.trackNumber == null || entry.trackNumber >= 0) { "Invalid track number" }
        }
        validatePrivateJson(checkpoint.policyStateJson)
        checkpoint.pendingStageJson?.let(::validatePrivateJson)
    }

    private fun safeEntry(entry: PlaybackQueueOccurrence, diagnostics: MutableList<SessionDiagnostic>): PlaybackQueueOccurrence {
        val validMedia = isSafeMediaUri(entry.mediaUri)
        val validArtwork = entry.artworkUri == null || isSafeMediaUri(entry.artworkUri)
        if (!validMedia) diagnostics += SessionDiagnostic("unavailable_media_uri", entry.entryId)
        if (!validArtwork) diagnostics += SessionDiagnostic("unavailable_artwork_uri", entry.entryId)
        return entry.copy(
            mediaUri = entry.mediaUri.takeIf { validMedia },
            artworkUri = entry.artworkUri.takeIf { validArtwork },
            unavailableReason = if (validMedia) entry.unavailableReason else entry.unavailableReason ?: "invalid-or-private-uri"
        )
    }

    private fun encodeEntry(entry: PlaybackQueueOccurrence): JSONObject = JSONObject()
        .put("entryId", entry.entryId).put("trackId", entry.trackId).put("mediaUri", entry.mediaUri ?: JSONObject.NULL)
        .put("title", entry.title).put("artist", entry.artist).put("album", entry.album)
        .put("albumArtist", entry.albumArtist ?: JSONObject.NULL).put("discNumber", entry.discNumber ?: JSONObject.NULL)
        .put("trackNumber", entry.trackNumber ?: JSONObject.NULL).put("durationMs", entry.durationMs ?: JSONObject.NULL)
        .put("durationSeconds", entry.durationSeconds ?: JSONObject.NULL).put("mimeType", entry.mimeType ?: JSONObject.NULL)
        .put("format", entry.format).put("lossless", entry.lossless ?: JSONObject.NULL)
        .put("artworkUri", entry.artworkUri ?: JSONObject.NULL).put("unavailableReason", entry.unavailableReason ?: JSONObject.NULL)

    private fun validatePrivateJson(raw: String) {
        require(raw.length <= MAX_POLICY_CHARS) { "Policy state too large" }
        fun visit(value: Any?, depth: Int) {
            require(depth <= 64) { "Policy state too deeply nested" }
            when (value) {
                is JSONObject -> value.keys().forEach { key ->
                    // Unknown policy/schema fields remain opaque, except forbidden credentials.
                    require(key.normalizedKey() !in credentialKeys) { "Credentials must use the protected credential store" }
                    visit(value.get(key), depth + 1)
                }
                is JSONArray -> (0 until value.length()).forEach { visit(value.get(it), depth + 1) }
                is String -> {
                    require(!value.startsWith("Bearer ", ignoreCase = true)) { "Credentials cannot be stored in policy state" }
                    val uri = runCatching { URI(value) }.getOrNull()
                    if (uri != null && uri.scheme != null) {
                        require(uri.rawUserInfo == null) { "Credential-bearing URI cannot be stored in policy state" }
                        uri.rawQuery?.split('&', ';')?.forEach { pair ->
                            val key = URLDecoder.decode(pair.substringBefore('='), "UTF-8").normalizedKey()
                            require(key !in uriCredentialKeys) { "Credential-bearing URI cannot be stored in policy state" }
                        }
                    }
                }
            }
        }
        visit(JSONObject(raw), 0)
    }

    private fun String.normalizedKey(): String = lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
    private fun JSONObject.requiredString(key: String): String = get(key).let { require(it is String) { "Invalid $key" }; it }
    private fun JSONObject.requiredLong(key: String): Long = get(key).let {
        require(it is Int || it is Long) { "Invalid $key" }; (it as Number).toLong()
    }
    private fun JSONObject.requiredInt(key: String): Int = requiredLong(key).let {
        require(it in Int.MIN_VALUE..Int.MAX_VALUE) { "Invalid $key" }; it.toInt()
    }
    private fun JSONObject.nullableString(key: String): String? = if (!has(key) || isNull(key)) null else requiredString(key)
    private fun JSONObject.nullableLong(key: String): Long? = if (!has(key) || isNull(key)) null else requiredLong(key)
    private fun JSONObject.nullableInt(key: String): Int? = if (!has(key) || isNull(key)) null else requiredInt(key)
    private fun JSONObject.nullableBoolean(key: String): Boolean? = if (!has(key) || isNull(key)) null else get(key).let {
        require(it is Boolean) { "Invalid $key" }; it
    }
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
