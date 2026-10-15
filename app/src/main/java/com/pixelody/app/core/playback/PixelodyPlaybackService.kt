package com.pixelody.app.core.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pixelody.app.MainActivity
import com.pixelody.app.data.storage.MobileEqualizerStore
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.data.storage.PlaybackQueueOccurrence
import com.pixelody.app.data.storage.PlaybackSessionCheckpoint
import com.pixelody.app.data.storage.PlaybackSessionCodec
import com.pixelody.app.data.storage.VersionedPlaybackSessionStore
import com.pixelody.app.core.analytics.ListeningEndReason
import com.pixelody.app.core.analytics.ListeningPlaybackSample
import com.pixelody.app.core.analytics.ListeningServiceRecorder
import com.pixelody.app.core.analytics.ListeningTrack
import com.pixelody.app.core.analytics.UniversalListeningRecorder
import com.pixelody.app.data.storage.SavedHostStore
import com.pixelody.app.widget.PixelodyWidgetProvider
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
class PixelodyPlaybackService : MediaLibraryService() {
    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private var shuffleAuthority: UniversalPlaybackAuthority? = null
    private val stateStore by lazy { getSharedPreferences(PLAYBACK_PREFERENCES, MODE_PRIVATE) }
    private val checkpointStore by lazy { VersionedPlaybackSessionStore(File(filesDir, "universal-shuffle")) }
    private val checkpointIo = Executors.newSingleThreadExecutor { task -> Thread(task, "Pixelody-playback-session").apply { isDaemon = true } }
    private var checkpoint: PlaybackSessionCheckpoint? = null
    private var restoring = false
    private var restorationReadOnly = false
    private var awaitingUnavailableRestore = false
    private var recoveryTombstones = emptyList<Pair<Int, PlaybackQueueOccurrence>>()
    private val listeningRecorder by lazy { ListeningServiceRecorder(File(filesDir, "universal-shuffle/listening")) {
        Log.w("PixelodyListening", "Listening storage diagnostic: $it")
    } }
    private val universalListening by lazy { UniversalListeningRecorder(File(filesDir, "universal-shuffle")) { Log.w("PixelodyLearning", it) } }
    private val tickerHandler = Handler(Looper.getMainLooper())
    private var checkpointTicks = 0
    private val checkpointTicker = object : Runnable {
        override fun run() {
            player?.let { active ->
                sampleListening(active)
                if (++checkpointTicks % 5 == 0) { persistPlayback(active); shuffleAuthority?.checkpoint(active.currentPosition, active.isPlaying) }
            }
            tickerHandler.postDelayed(this, 1_000)
        }
    }
    private val credentialStore by lazy { SavedHostStore(applicationContext) }
    private val credentialPreferences by lazy { getSharedPreferences(SavedHostStore.PREFERENCES_NAME, MODE_PRIVATE) }
    private val equalizerController by lazy { AndroidEqualizerController() }
    private val equalizerStore by lazy { MobileEqualizerStore(applicationContext) }
    private val equalizerPreferences by lazy { getSharedPreferences(MobileEqualizerStore.PREFERENCES_NAME, MODE_PRIVATE) }
    private val credentialListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SavedHostStore.KEY_ACTIVE_CREDENTIAL || key == SavedHostStore.KEY_ACTIVE_HOST) {
            if (runCatching { credentialStore.load() }.getOrNull() == null) {
                player?.let { removeRemoteHostMedia(it); persistPlayback(it) }
                shuffleAuthority?.refreshAvailability()
            }
        }
    }
    private val equalizerSettingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == MobileEqualizerStore.KEY_GLOBAL_EQ ||
            key == MobileEqualizerStore.KEY_TRACK_EQ ||
            key == MobileEqualizerStore.KEY_GLOBAL_MASTERING ||
            key == MobileEqualizerStore.KEY_TRACK_MASTERING ||
            key == MobileEqualizerStore.KEY_USE_MASTERING ||
            key == MobileEqualizerStore.KEY_SPATIAL_SETTINGS ||
            key == MobileEqualizerStore.KEY_EQ_UPDATE_TRIGGER) {
            updateEqualizerEffects()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PixelodyWidgetProvider.ACTION_PLAY_PAUSE -> {
                (shuffleAuthority?.player ?: player)?.let { p ->
                    if (p.isPlaying) p.pause() else p.play()
                }
            }
            PixelodyWidgetProvider.ACTION_PREV -> {
                shuffleAuthority?.controllerOrigin(true)
                shuffleAuthority?.player?.seekToPreviousMediaItem()
            }
            PixelodyWidgetProvider.ACTION_NEXT -> {
                shuffleAuthority?.controllerOrigin(true)
                shuffleAuthority?.player?.seekToNextMediaItem()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        val dataSourceFactory = authenticatedMediaDataSourceFactory(this)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_POSITION_DISCONTINUITY,
                        Player.EVENT_PLAY_WHEN_READY_CHANGED,
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                        Player.EVENT_REPEAT_MODE_CHANGED
                    )) {
                    persistPlayback(player)
                    if (events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_IS_PLAYING_CHANGED)) shuffleAuthority?.checkpoint(player.currentPosition, player.isPlaying)
                    if (player.isPlaying) shuffleAuthority?.activated(player.currentMediaItem, player.currentPosition)
                    sampleListening(player)
                    val metadata = player.currentMediaItem?.mediaMetadata
                    PixelodyWidgetProvider.updateAllWidgets(
                        context = this@PixelodyPlaybackService,
                        title = metadata?.title?.toString() ?: "Pixelody",
                        artist = metadata?.artist?.toString() ?: "Audiophile Player",
                        isPlaying = player.isPlaying,
                        format = metadata?.subtitle?.toString() ?: "HI-RES"
                    )
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0 && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    equalizerStore.saveAudioSessionId(audioSessionId)
                    updateEqualizerEffects()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateEqualizerEffects()
                val metadata = mediaItem?.mediaMetadata
                PixelodyWidgetProvider.updateAllWidgets(
                    context = this@PixelodyPlaybackService,
                    title = metadata?.title?.toString() ?: "Pixelody",
                    artist = metadata?.artist?.toString() ?: "Audiophile Player",
                    isPlaying = player?.isPlaying ?: false,
                    format = metadata?.subtitle?.toString() ?: "HI-RES"
                )
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                val sample = listeningSample(exoPlayer) ?: run {
                    endListening(null, ListeningEndReason.REPLACED)
                    return
                }
                val changedItem = oldPosition.mediaItemIndex != newPosition.mediaItemIndex ||
                    PlaybackEvidenceMetadata.entryId(oldPosition.mediaItem) != PlaybackEvidenceMetadata.entryId(newPosition.mediaItem)
                val endReason = when {
                    reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> ListeningEndReason.NATURAL_END
                    changedItem && reason == Player.DISCONTINUITY_REASON_SEEK -> ListeningEndReason.REPLACED
                    changedItem -> ListeningEndReason.REPLACED
                    else -> null
                }
                listeningRecorder.discontinuity(oldPosition.positionMs, sample, endReason)
                universalListening.discontinuity(oldPosition.positionMs, sample, endReason)
            }

            override fun onPlayerError(error: PlaybackException) {
                endListening(listeningSample(exoPlayer), ListeningEndReason.INFRASTRUCTURE)
                shuffleAuthority?.failed(exoPlayer.currentMediaItem)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    endListening(listeningSample(exoPlayer), ListeningEndReason.NATURAL_END)
                    shuffleAuthority?.naturalEnd()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    updateEqualizerEffects()
                } else if (playbackState == Player.STATE_ENDED) {
                    endListening(listeningSample(exoPlayer), ListeningEndReason.NATURAL_END)
                    shuffleAuthority?.naturalEnd()
                }
            }
        })
        listeningRecorder.recover(System.currentTimeMillis())
        universalListening.recover(System.currentTimeMillis())
        player = exoPlayer
        restorePlayback(exoPlayer)
        shuffleAuthority = UniversalPlaybackAuthority(exoPlayer, File(filesDir, "universal-shuffle"), MobileSettingsStore(this).loadShuffleMode() ?: "off",
            diagnostic = { Log.w("PixelodyShuffle", it) },
            boundary = { personal -> endListening(listeningSample(exoPlayer), if (personal) ListeningEndReason.PERSONAL_SKIP else ListeningEndReason.REPLACED) },
            offered = { trackId, nowMs, decisionId -> universalListening.offered(trackId, nowMs, decisionId) },
            context = universalListening::signals,
            available = { item -> val uri = item.localConfiguration?.uri?.toString(); uri != null && PlaybackSessionCodec.isSafeMediaUri(uri) &&
                (!isRemoteHostMedia(uri) || !runCatching { credentialStore.credentialFor(uri) }.getOrNull().isNullOrBlank()) &&
                (!uri.startsWith("file:") || runCatching { File(java.net.URI(uri)).isFile }.getOrDefault(false)) })
        tickerHandler.post(checkpointTicker)
        credentialPreferences.registerOnSharedPreferenceChangeListener(credentialListener)
        equalizerPreferences.registerOnSharedPreferenceChangeListener(equalizerSettingsListener)
        updateEqualizerEffects()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val libraryCallback = object : MediaLibrarySession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                if (controller.packageName == packageName) {
                    commands.add(SessionCommand(PlaybackEvidenceMetadata.COMMAND_MODE, Bundle.EMPTY))
                    commands.add(SessionCommand(PlaybackEvidenceMetadata.COMMAND_POLICY, Bundle.EMPTY))
                }
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(commands.build()).build()
            }

            override fun onPlayerCommandRequest(session: MediaSession, controller: MediaSession.ControllerInfo, playerCommand: Int): Int {
                shuffleAuthority?.controllerOrigin(controller.packageName == packageName)
                return SessionResult.RESULT_SUCCESS
            }

            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                if (controller.packageName != packageName) return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
                if (command.customAction == PlaybackEvidenceMetadata.COMMAND_MODE) {
                    shuffleAuthority?.setMode(args.getString("mode") ?: "balanced")
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                if (command.customAction == PlaybackEvidenceMetadata.COMMAND_POLICY) {
                    val nowMs = System.currentTimeMillis()
                    val result = Bundle()
                    when (args.getString("action")) {
                        "reset-developer-data" -> {
                            val future = com.google.common.util.concurrent.SettableFuture.create<SessionResult>()
                            java.util.concurrent.CompletableFuture.allOf(
                                listeningRecorder.resetHistory(), universalListening.deleteLedger(nowMs)
                            ).whenComplete { _, failure ->
                                future.set(SessionResult(if (failure == null) SessionResult.RESULT_SUCCESS else SessionError.ERROR_UNKNOWN))
                            }
                            return future
                        }
                        "new-cycle" -> shuffleAuthority?.newCycle()
                        "reshuffle-remaining" -> shuffleAuthority?.reshuffleRemaining()
                        "clear-future" -> shuffleAuthority?.clearFuture()
                        "evict-other-scopes" -> shuffleAuthority?.forgetOtherScopes()
                        "disable-learning" -> universalListening.setEnabled(false, nowMs)
                        "enable-learning" -> universalListening.setEnabled(true, nowMs)
                        "reset-learning" -> universalListening.resetLearning(nowMs)
                        "delete-history" -> universalListening.deleteHistory(nowMs)
                        "delete-learning" -> universalListening.deleteLedger(nowMs)
                        "delete-track-learning" -> args.getString("trackId")?.let { universalListening.deleteTrack(it, nowMs) }
                        "preference" -> args.getString("trackId")?.let { universalListening.preference(it, args.getInt("value"), nowMs) }
                        "snapshot" -> { result.putString("session", shuffleAuthority?.snapshot()?.toString()); result.putString("learning", universalListening.snapshot().toString()) }
                        "export-learning" -> {
                            val future = com.google.common.util.concurrent.SettableFuture.create<SessionResult>()
                            universalListening.exportPrivate(args.getBoolean("includePrivate", false)).whenComplete { value, failure ->
                                if (failure != null) future.set(SessionResult(SessionError.ERROR_UNKNOWN))
                                else future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle().apply { putString("json", value.toString()) }))
                            }
                            return future
                        }
                        "import-learning" -> {
                            val value = runCatching { org.json.JSONObject(args.getString("json") ?: "") }.getOrNull()
                                ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                            val future = com.google.common.util.concurrent.SettableFuture.create<SessionResult>()
                            universalListening.importPrivate(value, nowMs).whenComplete { receipt, failure ->
                                if (failure != null) future.set(SessionResult(SessionError.ERROR_UNKNOWN))
                                else future.set(SessionResult(SessionResult.RESULT_SUCCESS, Bundle().apply { putString("receipt", receipt.toString()) }))
                            }
                            return future
                        }
                        else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, result))
                }
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            override fun onAddMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>
            ): ListenableFuture<List<MediaItem>> {
                val queued = List(exoPlayer.mediaItemCount) { exoPlayer.getMediaItemAt(it) }
                val queuedText = queued.map { item ->
                    QueuedMediaSearchText(
                        title = item.mediaMetadata.title?.toString().orEmpty(),
                        artist = item.mediaMetadata.artist?.toString().orEmpty(),
                        album = item.mediaMetadata.albumTitle?.toString().orEmpty(),
                    )
                }
                val resolved = mutableListOf<MediaItem>()
                for (requested in mediaItems) {
                    val query = requested.requestMetadata.searchQuery
                    val matches = when {
                        query != null -> queuedMediaSearchIndexes(query, queuedText).map(queued::get)
                        requested.localConfiguration != null -> listOf(requested)
                        else -> queued.filter { it.mediaId == requested.mediaId }
                    }
                    if (matches.isEmpty()) {
                        // Reject before Media3 changes the queue; do not invent a stream URL.
                        return Futures.immediateFailedFuture(IllegalArgumentException("No queued music matches this request."))
                    }
                    resolved.addAll(matches)
                }
                val entryIds = HashSet<String>()
                return Futures.immediateFuture(resolved.map { item ->
                    val id = PlaybackEvidenceMetadata.entryId(item)
                    if (id.isNullOrBlank() || !entryIds.add(id)) {
                        PlaybackEvidenceMetadata.withEntry(item).also { entryIds.add(PlaybackEvidenceMetadata.entryId(it)!!) }
                    } else item
                })
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId("pixelody_root")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsPlayable(false)
                            .setIsBrowsable(true)
                            .setTitle("Pixelody")
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val items = mutableListOf<MediaItem>()
                val exo = player
                if (exo != null) {
                    for (i in 0 until exo.mediaItemCount) {
                        items.add(exo.getMediaItemAt(i))
                    }
                }
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, shuffleAuthority!!.player, libraryCallback)
            .setSessionActivity(sessionActivity)
            .setBitmapLoader(
                DataSourceBitmapLoader(
                    DataSourceBitmapLoader.DEFAULT_EXECUTOR_SERVICE.get(),
                    dataSourceFactory
                )
            )
            .build()
    }

    private fun updateEqualizerEffects() {
        val exo = player ?: return
        val sessionId = exo.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) {
            equalizerController.release()
            equalizerStore.saveRuntimeState(EqualizerRuntimeState())
            return
        }

        equalizerStore.saveAudioSessionId(sessionId)
        val currentMediaId = exo.currentMediaItem?.mediaId
        val trackEqs = equalizerStore.loadTrackProfiles()
        val globalEq = equalizerStore.loadGlobalProfile()
        val trackMasterings = equalizerStore.loadTrackMasteringProfiles()
        val globalMastering = equalizerStore.loadGlobalMasteringProfile()
        val useMastering = equalizerStore.loadUseMasteringRack()
        val spatialSettings = equalizerStore.loadSpatialSettings()

        val activeEq = currentMediaId?.let { trackEqs[it] } ?: globalEq
        val activeMastering = currentMediaId?.let { trackMasterings[it] } ?: globalMastering

        val runtime = equalizerController.applyEffects(
            audioSessionId = sessionId,
            eqProfile = activeEq,
            masteringProfile = activeMastering,
            useMastering = useMastering,
            spatialSettings = spatialSettings
        )
        equalizerStore.saveRuntimeState(runtime)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = player
        if (currentPlayer == null || (!currentPlayer.playWhenReady && currentPlayer.mediaItemCount == 0)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        tickerHandler.removeCallbacks(checkpointTicker)
        credentialPreferences.unregisterOnSharedPreferenceChangeListener(credentialListener)
        equalizerPreferences.unregisterOnSharedPreferenceChangeListener(equalizerSettingsListener)
        equalizerController.release()
        equalizerStore.saveRuntimeState(EqualizerRuntimeState())
        player?.let(::persistPlayback)
        shuffleAuthority?.close(player?.currentPosition ?: 0)
        listeningRecorder.close(player?.let(::listeningSample))
        universalListening.close(player?.let(::listeningSample), System.currentTimeMillis())
        checkpointIo.shutdown()
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun persistPlayback(player: Player) {
        if (restoring || restorationReadOnly || checkpointIo.isShutdown) return
        if (awaitingUnavailableRestore && player.mediaItemCount == 0) return
        if (player.mediaItemCount > 0) awaitingUnavailableRestore = false
        val previous = checkpoint
        val first = if (player.mediaItemCount > 0) player.getMediaItemAt(0) else null
        val requestedSession = first?.mediaMetadata?.extras?.getString(PlaybackEvidenceMetadata.SESSION_ID)
        val sessionId = requestedSession ?: previous?.sessionId ?: UUID.randomUUID().toString()
        val sameSession = previous?.sessionId == sessionId
        if (!sameSession) recoveryTombstones = emptyList()
        val seen = HashSet<String>()
        val entries = (0 until player.mediaItemCount).map { index ->
            val item = player.getMediaItemAt(index)
            val metadata = item.mediaMetadata
            val extras = metadata.extras
            val prior = previous?.queue?.getOrNull(index)?.takeIf { it.trackId == item.mediaId &&
                it.mediaUri == item.localConfiguration?.uri?.toString() }
            val candidate = PlaybackEvidenceMetadata.entryId(item) ?: prior?.entryId ?: UUID.randomUUID().toString()
            val entryId = if (seen.add(candidate)) candidate else UUID.randomUUID().toString().also { seen.add(it) }
            PlaybackQueueOccurrence(
                entryId = entryId, trackId = item.mediaId,
                mediaUri = item.localConfiguration?.uri?.toString(),
                title = metadata.title?.toString().orEmpty(), artist = metadata.artist?.toString().orEmpty(),
                album = metadata.albumTitle?.toString().orEmpty(), albumArtist = metadata.albumArtist?.toString(),
                discNumber = metadata.discNumber, trackNumber = metadata.trackNumber,
                durationMs = extras?.getLong(PlaybackEvidenceMetadata.DURATION_MS)?.takeIf { it > 0 },
                mimeType = item.localConfiguration?.mimeType,
                format = extras?.getString(PlaybackEvidenceMetadata.FORMAT).orEmpty(),
                lossless = extras?.takeIf { it.containsKey(PlaybackEvidenceMetadata.LOSSLESS) }?.getBoolean(PlaybackEvidenceMetadata.LOSSLESS),
                artworkUri = metadata.artworkUri?.toString()
            )
        }.toMutableList()
        val currentEntryId = entries.getOrNull(player.currentMediaItemIndex)?.entryId
        if (entries.isNotEmpty()) recoveryTombstones.forEach { (index, tombstone) ->
            if (entries.none { it.entryId == tombstone.entryId }) entries.add(index.coerceIn(0, entries.size), tombstone)
        }
        val currentIndex = if (entries.isEmpty()) -1 else entries.indexOfFirst { it.entryId == currentEntryId }.coerceAtLeast(0)
        val settings = MobileSettingsStore(applicationContext)
        val savedMode = when (settings.loadShuffleMode()) {
            "PureRandom", "Standard", "random" -> "random"
            "Balanced", "balanced" -> "balanced"
            "SmartFlow", "Flow", "flow" -> "flow"
            "AlbumPreserving", "album" -> "album"
            else -> "off"
        }
        val updated = PlaybackSessionCheckpoint(
            sessionId = sessionId, revision = (previous?.revision ?: 0) + 1,
            queue = entries, currentIndex = currentIndex, positionMs = player.currentPosition.coerceAtLeast(0),
            repeatMode = shuffleAuthority?.player?.repeatMode ?: player.repeatMode, shuffleMode = savedMode,
            policyStateJson = if (sameSession) previous!!.policyStateJson else "{}",
            authorityId = if (sameSession) previous!!.authorityId else "local",
            authorityEpoch = if (sameSession) previous!!.authorityEpoch else 0,
            scopeId = first?.mediaMetadata?.extras?.getString(PlaybackEvidenceMetadata.SCOPE_ID)
                ?: if (sameSession) previous!!.scopeId else sessionId,
            origin = if (sameSession) previous!!.origin else "media3-timeline-unknown-policy",
            playWhenReady = player.playWhenReady
        )
        checkpoint = updated
        checkpointIo.execute {
            val result = checkpointStore.save(updated)
            if (!result.saved) Log.w("PixelodyPlayback", "Session checkpoint retained: ${result.diagnostics.joinToString { it.code }}")
        }
    }

    private fun restorePlayback(player: ExoPlayer) {
        val loaded = checkpointStore.load(stateStore.getString(KEY_PLAYBACK_STATE, null))
        restorationReadOnly = loaded.readOnly
        if (loaded.diagnostics.isNotEmpty()) Log.w("PixelodyPlayback", "Session recovery: ${loaded.diagnostics.joinToString { it.code }}")
        val saved = loaded.checkpoint ?: return
        checkpoint = saved
        val restorable = saved.queue.mapIndexedNotNull { index, entry ->
            val uri = entry.mediaUri
            val available = uri != null && PlaybackSessionCodec.isSafeMediaUri(uri) &&
                (!isRemoteHostMedia(uri) || !runCatching { credentialStore.credentialFor(uri) }.getOrNull().isNullOrBlank())
            if (available) index to entry else null
        }
        val restorableIds = restorable.map { it.second.entryId }.toSet()
        recoveryTombstones = saved.queue.mapIndexedNotNull { index, entry ->
            if (entry.entryId in restorableIds) null else index to entry
        }
        // An unavailable current item remains recoverable in the checkpoint. Never
        // silently substitute another song as the person's restored current item.
        val restoredIndex = restorable.indexOfFirst { it.first == saved.currentIndex }
        if (restoredIndex < 0 || restorable.isEmpty()) {
            awaitingUnavailableRestore = saved.queue.isNotEmpty()
            return
        }
        val items = restorable.map { (_, entry) ->
            val extras = Bundle().apply {
                putString(PlaybackEvidenceMetadata.ENTRY_ID, entry.entryId)
                putString(PlaybackEvidenceMetadata.SESSION_ID, saved.sessionId)
                putString(PlaybackEvidenceMetadata.SCOPE_ID, saved.scopeId)
                entry.durationMs?.let { putLong(PlaybackEvidenceMetadata.DURATION_MS, it) }
                putString(PlaybackEvidenceMetadata.FORMAT, entry.format)
                entry.lossless?.let { putBoolean(PlaybackEvidenceMetadata.LOSSLESS, it) }
                putBoolean(PlaybackEvidenceMetadata.PERSONAL, saved.authorityId == "local")
            }
            val metadata = MediaMetadata.Builder().setTitle(entry.title).setArtist(entry.artist)
                .setAlbumTitle(entry.album).setAlbumArtist(entry.albumArtist).setDiscNumber(entry.discNumber)
                .setTrackNumber(entry.trackNumber).setArtworkUri(entry.artworkUri?.let(Uri::parse)).setExtras(extras).build()
            MediaItem.Builder().setMediaId(entry.trackId).setUri(entry.mediaUri).setMimeType(entry.mimeType)
                .setMediaMetadata(metadata).build()
        }
        restoring = true
        try {
            player.playWhenReady = false
            player.shuffleModeEnabled = false
            player.repeatMode = saved.repeatMode
            player.setMediaItems(items, restoredIndex, saved.positionMs)
            player.prepare()
            player.playWhenReady = false
            val settings = MobileSettingsStore(applicationContext)
            val mode = when (saved.shuffleMode) {
                "random" -> "PureRandom"
                "balanced" -> "Balanced"
                "flow" -> "SmartFlow"
                "album" -> "AlbumPreserving"
                else -> "Off"
            }
            settings.saveShuffleMode(mode)
            settings.saveShuffleEnabled(saved.shuffleMode != "off")
        } finally { restoring = false }
    }

    private fun sampleListening(player: Player) {
        listeningSample(player)?.let { sample -> listeningRecorder.sample(sample); universalListening.sample(sample) }
    }

    private fun endListening(sample: ListeningPlaybackSample?, reason: ListeningEndReason) {
        listeningRecorder.end(sample, reason)
        universalListening.end(sample, reason, System.currentTimeMillis())
    }
    private fun listeningSample(player: Player): ListeningPlaybackSample? {
        val item = player.currentMediaItem ?: return null
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        val entryId = PlaybackEvidenceMetadata.entryId(item)
            ?: checkpoint?.queue?.getOrNull(checkpoint?.currentIndex ?: -1)?.entryId ?: return null
        return ListeningPlaybackSample(
            entryId = entryId, sessionId = extras?.getString(PlaybackEvidenceMetadata.SESSION_ID)
                ?: checkpoint?.sessionId ?: return null,
            track = ListeningTrack(item.mediaId, metadata.title?.toString().orEmpty(), metadata.artist?.toString().orEmpty(),
                metadata.albumTitle?.toString().orEmpty(), metadata.artworkUri?.toString(),
                player.duration.takeIf { it > 0 } ?: extras?.getLong(PlaybackEvidenceMetadata.DURATION_MS)?.takeIf { it > 0 },
                extras?.getString(PlaybackEvidenceMetadata.FORMAT).orEmpty(), extras?.getBoolean(PlaybackEvidenceMetadata.LOSSLESS) ?: false),
            positionMs = player.currentPosition.coerceAtLeast(0), elapsedMs = SystemClock.elapsedRealtime(),
            epochMs = System.currentTimeMillis(), playing = player.isPlaying, speed = player.playbackParameters.speed,
            personal = extras?.getBoolean(PlaybackEvidenceMetadata.PERSONAL, true) ?: true
        )
    }

    private companion object {
        const val PLAYBACK_PREFERENCES = "pixelody_playback"
        const val KEY_PLAYBACK_STATE = "restorable_playback"
    }
}
