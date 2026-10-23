package com.pixelody.app.core.playback

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.pixelody.app.core.shuffle.UniversalShufflePolicy as Policy
import com.pixelody.app.core.shuffle.UniversalShuffleScopes as Scopes
import com.pixelody.app.data.storage.PlaybackQueueOccurrence
import com.pixelody.app.data.storage.PlaybackSessionCheckpoint
import com.pixelody.app.data.storage.UniversalAuthorityStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** One serialized local queue authority. Media3 is a projection, never a second shuffle engine. */
@androidx.annotation.OptIn(UnstableApi::class)
class UniversalPlaybackAuthority(
    private val native: ExoPlayer,
    directory: File,
    private val initialMode: String,
    private val diagnostic: (String) -> Unit,
    private val boundary: (Boolean) -> Unit,
    private val offered: (String, Long, String) -> Unit = { _, _, _ -> },
    private val available: (MediaItem) -> Boolean = { it.localConfiguration != null },
    private val context: (List<String>, Long) -> JSONObject = { _, now -> JSONObject().put("nowMs", now).put("signals", JSONArray()) }
) {
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "Pixelody-shuffle-authority").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())
    private val version = AtomicLong()
    private val store = UniversalAuthorityStore(directory)
    private var state: JSONObject? = null // IO-thread owned
    private var bank = Scopes.createBank()
    private val media = linkedMapOf<String, MediaItem>() // IO-thread owned
    @Volatile private var readonly = false
    private var pendingIntent: JSONObject? = null
    @Volatile private var requestedPlay = false
    @Volatile private var logicalRepeat = Player.REPEAT_MODE_OFF
    @Volatile private var repeatCommandReceived = false
    @Volatile private var commandPersonal = false
    @Volatile private var activeReservation: JSONObject? = null
    @Volatile private var publishedState: JSONObject? = null
    @Volatile private var closed = false
    private var naturalBoundaryHandled = false // player thread
    private var sourceChanging = false // player thread

    val player: Player = object : ForwardingPlayer(native) {
        override fun setMediaItems(items: MutableList<MediaItem>) = replace(items, 0, 0)
        override fun setMediaItems(items: MutableList<MediaItem>, resetPosition: Boolean) = replace(items, 0, if (resetPosition) 0 else native.currentPosition)
        override fun setMediaItems(items: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) = replace(items, startIndex, startPositionMs)
        override fun setMediaItem(item: MediaItem) = replace(listOf(item), 0, 0)
        override fun setMediaItem(item: MediaItem, startPositionMs: Long) = replace(listOf(item), 0, startPositionMs)
        override fun setMediaItem(item: MediaItem, resetPosition: Boolean) = replace(listOf(item), 0, if (resetPosition) 0 else native.currentPosition)
        override fun addMediaItem(item: MediaItem) = insert(native.mediaItemCount, listOf(item))
        override fun addMediaItem(index: Int, item: MediaItem) = insert(index, listOf(item))
        override fun addMediaItems(items: MutableList<MediaItem>) = insert(native.mediaItemCount, items)
        override fun addMediaItems(index: Int, items: MutableList<MediaItem>) = insert(index, items)
        override fun removeMediaItem(index: Int) = remove(index, index + 1)
        override fun removeMediaItems(fromIndex: Int, toIndex: Int) = remove(fromIndex, toIndex)
        override fun clearMediaItems() = clearAll()
        override fun replaceMediaItem(index: Int, item: MediaItem) { remove(index, index + 1); insert(index, listOf(item)) }
        override fun replaceMediaItems(fromIndex: Int, toIndex: Int, items: MutableList<MediaItem>) { remove(fromIndex, toIndex); insert(fromIndex, items) }
        override fun moveMediaItem(currentIndex: Int, newIndex: Int) = move(currentIndex, newIndex)
        override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
            val ids = (fromIndex until toIndex).mapNotNull { entryAt(it) }
            val start = native.currentMediaItemIndex + 1
            edit { s -> ids.foldIndexed(s) { offset, value, id -> Policy.mutate(value, JSONObject().put("type", "move").put("entryId", id).put("index", (newIndex - start + offset).coerceAtLeast(0)).put("intentId", uuid())) } }
        }
        override fun seekToNextMediaItem() = advance()
        override fun seekToNext() = advance()
        override fun seekToPreviousMediaItem() = advance(previous = true)
        override fun seekToPrevious() = advance(previous = true)
        override fun seekTo(index: Int, positionMs: Long) {
            if (index == native.currentMediaItemIndex) native.seekTo(positionMs)
            else entryAt(index)?.let { advance(entryId = it, positionMs = positionMs) }
        }
        override fun seekToDefaultPosition(index: Int) = seekTo(index, 0)
        override fun seekToDefaultPosition() = native.seekTo(0)
        override fun setRepeatMode(repeatMode: Int) {
            repeatCommandReceived = true
            logicalRepeat = repeatMode
            edit { Policy.reduce(it, JSONObject().put("type", "repeat-change").put("repeat", repeatName(repeatMode))) }
        }
        override fun getRepeatMode(): Int = logicalRepeat
        override fun setShuffleModeEnabled(enabled: Boolean) { native.shuffleModeEnabled = false }
        override fun getShuffleModeEnabled(): Boolean = false
        override fun setPlayWhenReady(playWhenReady: Boolean) {
            requestedPlay = playWhenReady && !readonly
            if (!sourceChanging) native.playWhenReady = requestedPlay
        }
        override fun play() {
            requestedPlay = !readonly
            if (sourceChanging) return
            if (naturalBoundaryHandled || native.playbackState == Player.STATE_ENDED) {
                if ((publishedState?.optJSONArray("remaining")?.length() ?: 0) > 0) advance(lane = "base")
                else advance(entryId = idOfOrNull(native.currentMediaItem))
            } else if (!readonly) native.play()
        }
        override fun pause() { requestedPlay = false; native.pause() }
        override fun prepare() { if (!sourceChanging && native.mediaItemCount > 0) native.prepare() }
    }

    init {
        logicalRepeat = native.repeatMode
        native.shuffleModeEnabled = false
        native.repeatMode = Player.REPEAT_MODE_OFF
        native.setPauseAtEndOfMediaItems(true)
        val fallback = List(native.mediaItemCount) { native.getMediaItemAt(it) }
        val fallbackIndex = native.currentMediaItemIndex
        val fallbackPosition = native.currentPosition
        // Capture before loading on IO. A controller can replace the source while the
        // old checkpoint is loading; that restore must never publish under its new token.
        val restoreToken = version.get()
        io.execute {
            val loaded = store.load()
            readonly = loaded.readOnly
            loaded.checkpoint?.let { saved ->
                try {
                    val payload = JSONObject(saved.policyStateJson)
                    if (payload.optInt("adapterVersion") != 1) { readonly = true; diagnostic("unsupported-authority-adapter"); return@let }
                    bank = payload.getJSONObject("bank")
                    val restored = Policy.restore(payload.getJSONObject("session"))
                    val restoredState = restored.optJSONObject("state")
                    if (restoredState == null) { readonly = true; diagnostic("preserved-unsupported-authority-session"); return@let }
                    state = restoredState
                    saved.queue.forEach { entry -> media[entry.entryId] = mediaItem(entry, saved.sessionId, saved.scopeId) }
                    val personals = payload.optJSONObject("personal")
                    media.keys.toList().forEach { id ->
                        val item = media.getValue(id)
                        val extras = Bundle(item.mediaMetadata.extras ?: Bundle()).apply { putBoolean(PlaybackEvidenceMetadata.PERSONAL, personals?.optBoolean(id, saved.authorityId == "local") ?: (saved.authorityId == "local")) }
                        media[id] = item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(extras).build()).build()
                    }
                    val restoredEntries = restoredState.getJSONArray("entries").objects().map { entry -> JSONObject(entry.toString()).put("available", media[entry.getString("entryId")]?.let(available) == true) }
                    state = if (restoredEntries.indices.any { restoredEntries[it].getBoolean("available") != restoredState.getJSONArray("entries").getJSONObject(it).getBoolean("available") }) Policy.reconcile(restoredState, JSONArray(restoredEntries)) else restoredState
                    if (!repeatCommandReceived) logicalRepeat = repeatValue(restoredState.getString("repeat"))
                    if (restoreToken != version.get()) return@let
                    val intent = payload.optJSONObject("intent")
                    if (intent != null) {
                        val request = JSONObject(intent.getJSONObject("request").toString()).put("attemptId", uuid()).put("advanceId", uuid())
                        stageAndRender(request, intent.getLong("positionMs"), false, restoreToken)
                    } else render(state!!, saved.positionMs, false, restoreToken)
                } catch (_: Exception) { readonly = true; diagnostic("preserved-corrupt-authority-session") }
            }
            if (state == null && !readonly && fallback.isNotEmpty()) {
                fallback.forEach { item -> media[idOf(item)] = item }
                val input = input(fallback, initialMode)
                val imported = Policy.migrateLegacy(JSONObject().put("plan", JSONObject().put("order", JSONArray(fallback.map { it.mediaId })).put("cursor", fallbackIndex)), input)
                state = imported
                persist(imported, fallbackPosition)
                render(imported, fallbackPosition, false, restoreToken)
            }
        }
    }

    fun controllerOrigin(personal: Boolean) { commandPersonal = personal }
    fun setMode(mode: String) = edit { Policy.setMode(it, canonicalMode(mode), null, authorityContext(it)) }
    fun refreshAvailability() = edit { s -> Policy.reconcile(s, JSONArray(s.getJSONArray("entries").objects().map { entry ->
        JSONObject(entry.toString()).put("available", media[entry.getString("entryId")]?.let(available) == true)
    })) }
    fun newCycle() = edit { Policy.newCycle(it) }
    fun reshuffleRemaining() = edit { Policy.reduce(it, JSONObject().put("type", "reshuffle-remaining")) }
    fun forgetOtherScopes() = edit { s ->
        val currentScope = s.getString("scopeId")
        val scopes = bank.getJSONArray("scopes").objects().filter { it.getString("scopeId") != currentScope }
        var unfinished = 0
        scopes.forEach { row ->
            val result = Scopes.evict(bank, row.getString("scopeId"), JSONObject().put("nowMs", now()))
            unfinished += result.getJSONObject("diagnostic").getInt("unfinishedEntitlements")
            bank = result.getJSONObject("bank")
        }
        diagnostic("explicit-scope-eviction: ${scopes.size} scopes, $unfinished unfinished entries")
        s
    }
    fun clearFuture() = edit { s ->
        val current = Policy.currentOccurrence(s)?.getString("entryId")
        s.getJSONArray("entries").objects().filter { it.getString("entryId") != current }
            .fold(s) { next, row -> Policy.mutate(next, JSONObject().put("type", "remove").put("entryId", row.getString("entryId"))) }
    }
    fun snapshot(): JSONObject? = publishedState?.let { JSONObject(it.toString()) }

    /** Called only for confirmed actual playback, never prepare/paint/READY alone. */
    fun activated(item: MediaItem?, positionMs: Long) {
        if (closed) return
        val reservation = activeReservation ?: return
        if (idOfOrNull(item) != reservation.optString("entryId")) return
        if (item?.mediaMetadata?.extras?.getString(PlaybackEvidenceMetadata.ATTEMPT_ID) != reservation.getString("attemptId")) return
        io.execute {
            val current = state ?: return@execute
            val next = Policy.commit(current, JSONObject(reservation.toString()).put("positionMs", positionMs.coerceAtLeast(0)))
            if (next === current || next.toString() == current.toString()) return@execute
            state = next
            pendingIntent = null
            activeReservation = null
            persist(next, positionMs)
        }
    }

    fun failed(item: MediaItem?) {
        if (closed) return
        val reservation = activeReservation ?: return
        if (item?.mediaMetadata?.extras?.getString(PlaybackEvidenceMetadata.ATTEMPT_ID) != reservation.getString("attemptId")) return
        val token = version.get()
        io.execute {
            val current = state ?: return@execute
            val next = Policy.abort(current, reservation)
            state = next
            activeReservation = null
            persist(next, 0)
            stageAndRender(JSONObject().put("attemptId", uuid()).put("advanceId", reservation.getString("advanceId")), 0, requestedPlay, token)
        }
    }

    fun naturalEnd() {
        if (closed) return
        if (naturalBoundaryHandled) return
        naturalBoundaryHandled = true
        advance(natural = true)
    }
    fun checkpoint(positionMs: Long, playing: Boolean? = null) {
        if (closed || sourceChanging) return
        io.execute {
            val current = state?.let { if (playing == null) it else JSONObject(it.toString()).put("paused", !playing) } ?: return@execute
            state = current
            if (current.optJSONObject("pending") != null) { persist(current, positionMs); return@execute }
            val occurrence = Policy.currentOccurrence(current) ?: return@execute
            state = Policy.reduce(current, JSONObject().put("type", "position-checkpoint").put("occurrenceId", occurrence.getString("occurrenceId")).put("positionMs", positionMs.coerceAtLeast(0)))
            persist(state!!, positionMs)
        }
    }

    private fun replace(items: List<MediaItem>, startIndex: Int, positionMs: Long) {
        if (closed) return
        if (items.isEmpty()) { clearAll(); return }
        val copy = items.map { item -> if (idOfOrNull(item) == null) PlaybackEvidenceMetadata.withEntry(item) else item }
        val token = version.incrementAndGet()
        boundary(commandPersonal)
        sourceChanging = true
        native.pause()
        // MediaController masks setMediaItems synchronously. Leaving an old large
        // timeline/index visible until IO finishes can merge that index into the
        // controller's new smaller timeline. Clear the paused projection now;
        // preparation/activation still waits for the durable policy reservation.
        native.stop()
        native.clearMediaItems()
        io.execute {
            if (readonly || copy.isEmpty()) return@execute
            state?.let { bank = Scopes.save(bank, it, JSONObject().put("nowMs", now())).optJSONObject("bank") ?: bank }
            copy.forEach { media[idOf(it)] = it }
            val createdInput = input(copy, copy.first().mediaMetadata.extras?.getString(PlaybackEvidenceMetadata.MODE) ?: initialMode)
            val restored = Scopes.restore(bank, createdInput.getString("scopeId")).optJSONObject("state")
            var next = if (restored != null) Policy.reconcile(restored, createdInput.getJSONArray("entries")) else Policy.createSession(createdInput)
            next = Policy.setMode(next, createdInput.getString("mode"), null, authorityContext(next))
            state = next
            val anchor = copy.getOrNull(startIndex.coerceAtLeast(0)) ?: copy.first()
            stageAndRender(JSONObject().put("attemptId", uuid()).put("advanceId", uuid()).put("entryId", idOf(anchor)), positionMs, requestedPlay, token)
        }
    }

    private fun advance(previous: Boolean = false, entryId: String? = null, natural: Boolean = false, positionMs: Long = 0, lane: String? = null) {
        if (closed) return
        val token = version.incrementAndGet()
        val playing = if (natural) requestedPlay else native.playWhenReady || requestedPlay
        // A personal Next plays the live queue; how much was heard decides how
        // strongly that skip steers this session. Remote commands leave no mark.
        val heardMs = native.currentPosition.coerceAtLeast(0)
        val durationMs = native.duration
        val personal = commandPersonal
        if (!natural) boundary(commandPersonal)
        io.execute {
            if (readonly) return@execute
            val request = JSONObject().put("attemptId", uuid()).put("advanceId", uuid()).put("direction", if (previous) "previous" else "next").put("naturalEnd", natural)
            entryId?.let { request.put("entryId", it) }
            lane?.let { request.put("lane", it) }
            if (!natural && !previous && entryId == null && lane == null) {
                request.put("heardMs", heardMs).put("personal", personal)
                if (durationMs > 0) request.put("coveragePermille", (heardMs * 1000 / durationMs).coerceIn(0, 1000))
            }
            stageAndRender(request, positionMs, playing, token)
        }
    }

    private fun stageAndRender(request: JSONObject, positionMs: Long, playing: Boolean, token: Long) {
        val current = state ?: return
        val staged = Policy.stage(current, request, authorityContext(current))
        state = staged
        pendingIntent = if (staged.optJSONObject("pending") != null) JSONObject().put("request", request).put("positionMs", positionMs.coerceAtLeast(0)) else null
        if (!persist(staged, positionMs)) return
        if (token != version.get()) return
        val pending = staged.optJSONObject("pending")
        if (pending == null) { main.post { if (token == version.get()) { sourceChanging = false; requestedPlay = false; native.pause(); if (Policy.currentOccurrence(staged) == null) native.clearMediaItems() } }; return }
        activeReservation = JSONObject(pending.toString())
        offered(pending.getString("trackId"), now(), pending.getString("decisionId"))
        render(staged, positionMs, playing, token)
    }

    private fun edit(transform: (JSONObject) -> JSONObject) {
        if (closed) return
        val token = version.incrementAndGet()
        val position = native.currentPosition
        io.execute {
            if (readonly) return@execute
            val current = state ?: return@execute
            val unconfirmed = current.optJSONObject("pending")?.optString("entryId")
            val next = transform(current)
            state = next
            activeReservation = null
            if (unconfirmed != null && next.getJSONArray("entries").objects().any { it.getString("entryId") == unconfirmed }) {
                stageAndRender(JSONObject().put("attemptId", uuid()).put("advanceId", uuid()).put("entryId", unconfirmed), position, requestedPlay, token)
            } else if (persist(next, position)) render(next, position, requestedPlay, token, futureOnly = true)
        }
    }

    private fun insert(index: Int, items: List<MediaItem>) {
        if (items.isNotEmpty() && items.all { it.mediaMetadata.extras?.getBoolean(PlaybackEvidenceMetadata.ADD_TO_SESSION) == true }) { addSource(items); return }
        val offset = (index - native.currentMediaItemIndex - 1).coerceAtLeast(0)
        val copies = items.map { if (idOfOrNull(it) == null) PlaybackEvidenceMetadata.withEntry(it) else it }
        edit { original -> copies.foldIndexed(original) { n, s, requested ->
            val occupied = s.getJSONArray("entries").objects().any { it.getString("entryId") == idOf(requested) }
            val item = if (occupied) PlaybackEvidenceMetadata.withEntry(requested) else requested
            val id = idOf(item)
            media[id] = item
            val inserted = Policy.mutate(s, JSONObject().put("type", "insert").put("entry", rawEntry(item)).put("index", s.getJSONArray("entries").length()))
            Policy.mutate(inserted, JSONObject().put("type", "move").put("entryId", id).put("index", offset + n).put("intentId", uuid()))
        } }
    }

    /** Add to session: another list joins this shuffle as a whole, not as songs queued next. */
    private fun addSource(items: List<MediaItem>) = edit { s ->
        val existing = s.getJSONArray("entries").objects()
        val taken = existing.map { it.getString("entryId") }.toMutableSet()
        val added = items.map { requested ->
            val item = if (idOfOrNull(requested)?.let { it in taken } != false) PlaybackEvidenceMetadata.withEntry(requested) else requested
            taken += idOf(item)
            media[idOf(item)] = item
            rawEntry(item)
        }
        Policy.reconcile(s, JSONArray(existing.map { JSONObject(it.toString()) } + added))
    }

    /** Takes one list back out of a linked session. The song playing now stays. */
    fun removeSource(sourceKey: String) = edit { s ->
        val current = Policy.currentOccurrence(s)?.optString("entryId")
        Policy.reconcile(s, JSONArray(s.getJSONArray("entries").objects()
            .filter { it.optString("sourceKey") != sourceKey || it.getString("entryId") == current }.map { JSONObject(it.toString()) }))
    }

    /** Linked lists take turns by size, or evenly whatever their size. */
    fun setAirtime(even: Boolean) = edit { s ->
        val options = JSONObject(s.getJSONObject("options").toString()).put("airtime", if (even) "even" else "size")
        Policy.setMode(s, s.getString("mode"), options, authorityContext(s))
    }

    private fun remove(from: Int, until: Int) {
        val ids = (from until until).mapNotNull(::entryAt)
        edit { s -> ids.fold(s) { next, id -> Policy.mutate(next, JSONObject().put("type", "remove").put("entryId", id)) } }
    }

    private fun clearAll() {
        val token = version.incrementAndGet()
        boundary(commandPersonal)
        io.execute {
            if (readonly) return@execute
            val next = state?.let { Policy.mutate(it, JSONObject().put("type", "clear")) } ?: return@execute
            state = next; pendingIntent = null; activeReservation = null
            if (persist(next, 0)) main.post { if (token == version.get()) { requestedPlay = false; native.pause(); native.clearMediaItems() } }
        }
    }

    private fun move(from: Int, to: Int) {
        val id = entryAt(from) ?: return
        val offset = (to - native.currentMediaItemIndex - 1).coerceAtLeast(0)
        edit { Policy.mutate(it, JSONObject().put("type", "move").put("entryId", id).put("index", offset).put("intentId", uuid())) }
    }

    private fun render(s: JSONObject, positionMs: Long, playing: Boolean, token: Long, futureOnly: Boolean = false) {
        val ctx = authorityContext(s)
        var batch = Policy.createProjection(s, JSONObject().put("horizon", 32))
        while (!batch.getBoolean("done")) {
            if (token != version.get()) return
            batch = Policy.advanceProjection(batch, ctx)
        }
        val decisions = batch.getJSONArray("decisions").objects()
        val history = s.getJSONArray("history").objects().take(s.getInt("cursor") + 1)
        val pending = s.optJSONObject("pending")
        val current = pending?.getString("entryId") ?: history.lastOrNull()?.getString("entryId")
        val prefix = if (pending != null) {
            if (!pending.isNull("historyIndex")) s.getJSONArray("history").objects().take(pending.getInt("historyIndex")) else history
        } else history.dropLast(1)
        // Previous/forward navigate the durable policy history, so Media3 only
        // needs a small past window even after many completed cycles.
        val prefixItems = prefix.takeLast(32).mapNotNull { media[it.getString("entryId")] }
        val currentItem = current?.let { media[it] } ?: return
        if (!available(currentItem)) {
            diagnostic("current-media-unavailable-preserved")
            main.post { if (token == version.get()) { native.pause(); native.clearMediaItems() } }
            return
        }
        val futureDecisions = if (pending != null) decisions.drop(1) else decisions
        // What a skip would play instead, worked out as if the current song had
        // already started. The queue screen shows it beside Up next.
        val liveNext = if (s.optString("mode") == "off") null else runCatching {
            val settled = if (pending != null) Policy.commit(s, JSONObject(pending.toString()).put("positionMs", 0)) else s
            Policy.preview(settled, JSONObject().put("lane", "live"), ctx)?.optString("trackId")?.takeIf { it.isNotEmpty() }
        }.getOrNull()
        fun evidence(item: MediaItem, receipt: JSONObject?): MediaItem {
            val extras = Bundle(item.mediaMetadata.extras ?: Bundle()).apply {
                putString(PlaybackEvidenceMetadata.SESSION_ID, s.getString("sessionId"))
                putString(PlaybackEvidenceMetadata.SCOPE_ID, s.getString("scopeId"))
                putString(PlaybackEvidenceMetadata.DECISION, receipt?.toString())
                if (pending != null && idOf(item) == current) putString(PlaybackEvidenceMetadata.ATTEMPT_ID, pending.getString("attemptId"))
            }
            return item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(extras).build()).build()
        }
        val receipt = pending?.optJSONObject("receipt") ?: history.lastOrNull()?.optJSONObject("receipt")
        val future = futureDecisions.mapNotNull { d -> media[d.getString("entryId")]?.let { evidence(it, d) } }.mapIndexed { index, item ->
            if (index != 0) item else {
                val extras = Bundle(item.mediaMetadata.extras ?: Bundle()).apply { putString(PlaybackEvidenceMetadata.LIVE_NEXT, liveNext) }
                item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(extras).build()).build()
            }
        }
        publishedState = JSONObject(s.toString())
        main.post {
            if (closed || token != version.get()) return@post
            sourceChanging = false
            if (futureOnly && idOfOrNull(native.currentMediaItem) == current) {
                val start = native.currentMediaItemIndex + 1
                native.removeMediaItems(start, native.mediaItemCount)
                native.addMediaItems(future)
            } else {
                naturalBoundaryHandled = false
                native.setMediaItems(prefixItems + evidence(currentItem, receipt) + future, prefixItems.size, positionMs.coerceAtLeast(0))
                native.prepare()
                native.playWhenReady = requestedPlay
            }
        }
    }

    private fun persist(s: JSONObject, positionMs: Long): Boolean {
        if (readonly) return false
        val savedBank = Scopes.save(bank, s, JSONObject().put("nowMs", now()))
        if (savedBank.optString("status") != "saved") { diagnostic(savedBank.optString("status")); return false }
        savedBank.optJSONObject("bank")?.let { bank = it }
        val entries = s.getJSONArray("entries").objects()
        val currentId = Policy.currentOccurrence(s)?.optString("entryId")
        val queue = entries.map { entry ->
            val item = media[entry.getString("entryId")]
            val m = item?.mediaMetadata
            PlaybackQueueOccurrence(entry.getString("entryId"), entry.getString("trackId"), item?.localConfiguration?.uri?.toString(),
                m?.title?.toString().orEmpty(), m?.artist?.toString().orEmpty(), m?.albumTitle?.toString().orEmpty(),
                albumArtist = m?.albumArtist?.toString(), discNumber = m?.discNumber, trackNumber = m?.trackNumber,
                durationMs = m?.extras?.getLong(PlaybackEvidenceMetadata.DURATION_MS)?.takeIf { it > 0 }, mimeType = item?.localConfiguration?.mimeType,
                format = m?.extras?.getString(PlaybackEvidenceMetadata.FORMAT).orEmpty(),
                lossless = m?.extras?.takeIf { it.containsKey(PlaybackEvidenceMetadata.LOSSLESS) }?.getBoolean(PlaybackEvidenceMetadata.LOSSLESS), artworkUri = m?.artworkUri?.toString())
        }
        val checkpoint = PlaybackSessionCheckpoint(sessionId = s.getString("sessionId"), revision = s.getLong("revision"), queue = queue,
            currentIndex = if (queue.isEmpty()) -1 else queue.indexOfFirst { it.entryId == currentId }.coerceAtLeast(0), positionMs = if (s.optJSONObject("pending") != null) s.getLong("lastConfirmedPositionMs") else positionMs.coerceAtLeast(0),
            repeatMode = repeatValue(s.getString("repeat")), shuffleMode = s.getString("mode"), policyStateJson = JSONObject().put("adapterVersion", 1).put("session", s).put("bank", bank).put("intent", pendingIntent ?: JSONObject.NULL)
                .put("personal", JSONObject().apply { entries.forEach { entry -> val id = entry.getString("entryId"); put(id, media[id]?.mediaMetadata?.extras?.getBoolean(PlaybackEvidenceMetadata.PERSONAL, true) ?: true) } }).toString(),
            scopeId = s.getString("scopeId"), authorityId = s.getString("authorityId"), authorityEpoch = s.getLong("authorityEpoch"), currentOccurrenceId = Policy.currentOccurrence(s)?.getString("occurrenceId"), origin = Policy.POLICY_VERSION)
        val result = store.save(checkpoint)
        if (!result.saved) { readonly = true; diagnostic("authority-checkpoint-not-saved: " + result.diagnostics.joinToString { it.code }); main.post { requestedPlay = false; native.pause() }; return false }
        publishedState = JSONObject(s.toString())
        return true
    }

    private fun input(items: List<MediaItem>, mode: String): JSONObject = JSONObject()
        .put("sessionId", items.first().mediaMetadata.extras?.getString(PlaybackEvidenceMetadata.SESSION_ID) ?: uuid())
        .put("scopeId", items.first().mediaMetadata.extras?.getString(PlaybackEvidenceMetadata.SCOPE_ID) ?: uuid())
        .put("authorityId", "local").put("authorityEpoch", 0).put("seed", uuid()).put("mode", canonicalMode(mode))
        .put("repeat", repeatName(logicalRepeat)).put("entries", JSONArray(items.map(::rawEntry)))

    private fun rawEntry(item: MediaItem): JSONObject {
        val m = item.mediaMetadata
        val raw = m.extras?.getString(PlaybackEvidenceMetadata.POLICY_ENTRY)?.let { JSONObject(it) } ?: JSONObject()
            .put("trackId", item.mediaId).put("artist", m.artist?.toString()).put("albumArtist", m.albumArtist?.toString()).put("album", m.albumTitle?.toString())
            .put("discNumber", m.discNumber).put("trackNumber", m.trackNumber)
        return raw.put("entryId", idOf(item)).put("available", available(item))
    }

    private fun mediaItem(e: PlaybackQueueOccurrence, session: String, scope: String): MediaItem {
        val extras = Bundle().apply { putString(PlaybackEvidenceMetadata.ENTRY_ID, e.entryId); putString(PlaybackEvidenceMetadata.SESSION_ID, session); putString(PlaybackEvidenceMetadata.SCOPE_ID, scope); e.durationMs?.let { putLong(PlaybackEvidenceMetadata.DURATION_MS, it) }; putString(PlaybackEvidenceMetadata.FORMAT, e.format); e.lossless?.let { putBoolean(PlaybackEvidenceMetadata.LOSSLESS, it) }; putBoolean(PlaybackEvidenceMetadata.PERSONAL, true) }
        return MediaItem.Builder().setMediaId(e.trackId).setUri(e.mediaUri).setMimeType(e.mimeType).setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(e.title).setArtist(e.artist).setAlbumTitle(e.album).setAlbumArtist(e.albumArtist).setDiscNumber(e.discNumber).setTrackNumber(e.trackNumber).setArtworkUri(e.artworkUri?.let(android.net.Uri::parse)).setExtras(extras).build()).build()
    }

    private fun authorityContext(s: JSONObject): JSONObject = context(s.getJSONArray("entries").objects().map { it.getString("trackId") }.distinct(), now())
    private fun entryAt(index: Int): String? = if (index in 0 until native.mediaItemCount) idOfOrNull(native.getMediaItemAt(index)) else null
    private fun idOf(item: MediaItem) = requireNotNull(idOfOrNull(item))
    private fun idOfOrNull(item: MediaItem?) = PlaybackEvidenceMetadata.entryId(item)
    private fun now() = System.currentTimeMillis().coerceAtLeast(0)
    private fun canonicalMode(mode: String) = when (mode) { "Off" -> "off"; "Balanced" -> "balanced"; else -> Policy.normalizeMode(mode) }
    private fun uuid() = UUID.randomUUID().toString()
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun repeatName(mode: Int) = when (mode) { Player.REPEAT_MODE_ALL -> "all"; Player.REPEAT_MODE_ONE -> "one"; else -> "off" }
    private fun repeatValue(mode: String) = when (mode) { "all" -> Player.REPEAT_MODE_ALL; "one" -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
    fun close(positionMs: Long) { checkpoint(positionMs); closed = true; io.shutdown() }
}
