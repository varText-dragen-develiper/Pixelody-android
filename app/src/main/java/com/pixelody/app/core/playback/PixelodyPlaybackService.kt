package com.pixelody.app.core.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pixelody.app.MainActivity
import com.pixelody.app.data.storage.MobileEqualizerStore
import com.pixelody.app.data.storage.SavedHostStore
import com.pixelody.app.widget.PixelodyWidgetProvider
import org.json.JSONObject

@OptIn(UnstableApi::class)
class PixelodyPlaybackService : MediaLibraryService() {
    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private val stateStore by lazy { getSharedPreferences(PLAYBACK_PREFERENCES, MODE_PRIVATE) }
    private val credentialStore by lazy { SavedHostStore(applicationContext) }
    private val credentialPreferences by lazy { getSharedPreferences(SavedHostStore.PREFERENCES_NAME, MODE_PRIVATE) }
    private val equalizerController by lazy { AndroidEqualizerController() }
    private val equalizerStore by lazy { MobileEqualizerStore(applicationContext) }
    private val equalizerPreferences by lazy { getSharedPreferences(MobileEqualizerStore.PREFERENCES_NAME, MODE_PRIVATE) }
    private val credentialListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SavedHostStore.KEY_ACTIVE_CREDENTIAL || key == SavedHostStore.KEY_ACTIVE_HOST) {
            if (runCatching { credentialStore.load() }.getOrNull() == null) {
                player?.let { removeRemoteHostMedia(it); persistPlayback(it) }
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
                player?.let { p ->
                    if (p.isPlaying) p.pause() else p.play()
                }
            }
            PixelodyWidgetProvider.ACTION_PREV -> {
                player?.seekToPreviousMediaItem()
            }
            PixelodyWidgetProvider.ACTION_NEXT -> {
                player?.seekToNextMediaItem()
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
                        Player.EVENT_IS_PLAYING_CHANGED
                    )) {
                    persistPlayback(player)
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

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    updateEqualizerEffects()
                }
            }
        })
        restorePlayback(exoPlayer)
        player = exoPlayer
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

        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer, libraryCallback)
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
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return

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

        equalizerController.applyEffects(
            audioSessionId = sessionId,
            eqProfile = activeEq,
            masteringProfile = activeMastering,
            useMastering = useMastering,
            spatialSettings = spatialSettings
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = player
        if (currentPlayer == null || (!currentPlayer.playWhenReady && currentPlayer.mediaItemCount == 0)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        credentialPreferences.unregisterOnSharedPreferenceChangeListener(credentialListener)
        equalizerPreferences.unregisterOnSharedPreferenceChangeListener(equalizerSettingsListener)
        equalizerController.release()
        player?.let(::persistPlayback)
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun persistPlayback(player: Player) {
        val item = player.currentMediaItem ?: run {
            stateStore.edit().remove(KEY_PLAYBACK_STATE).apply()
            return
        }
        val uri = item.localConfiguration?.uri?.toString().orEmpty()
        if (uri.isBlank() || uri.containsCredentialQuery()
            || (isRemoteHostMedia(uri) && runCatching { credentialStore.credentialFor(uri) }.getOrNull().isNullOrBlank())) {
            stateStore.edit().remove(KEY_PLAYBACK_STATE).apply()
            return
        }
        val metadata = item.mediaMetadata
        val payload = JSONObject()
            .put("mediaId", item.mediaId)
            .put("uri", uri)
            .put("title", metadata.title?.toString().orEmpty())
            .put("artist", metadata.artist?.toString().orEmpty())
            .put("album", metadata.albumTitle?.toString().orEmpty())
            .put("artworkUri", metadata.artworkUri?.toString().orEmpty())
            .put("positionMs", player.currentPosition.coerceAtLeast(0L))
            .put("playWhenReady", player.playWhenReady)
        stateStore.edit().putString(KEY_PLAYBACK_STATE, payload.toString()).apply()
    }

    private fun restorePlayback(player: ExoPlayer) {
        val raw = stateStore.getString(KEY_PLAYBACK_STATE, null) ?: return
        val state = runCatching { JSONObject(raw) }.getOrNull() ?: return
        val uri = state.optString("uri")
        if (uri.isBlank() || uri.containsCredentialQuery()
            || (isRemoteHostMedia(uri) && runCatching { credentialStore.credentialFor(uri) }.getOrNull().isNullOrBlank())) {
            stateStore.edit().remove(KEY_PLAYBACK_STATE).apply()
            return
        }
        val artworkUri = state.optString("artworkUri").takeIf { it.isNotBlank() }?.let(Uri::parse)
        val metadata = MediaMetadata.Builder()
            .setTitle(state.optString("title"))
            .setArtist(state.optString("artist"))
            .setAlbumTitle(state.optString("album"))
            .setArtworkUri(artworkUri)
            .build()
        player.setMediaItem(
            MediaItem.Builder()
                .setMediaId(state.optString("mediaId"))
                .setUri(uri)
                .setMediaMetadata(metadata)
                .build(),
            state.optLong("positionMs").coerceAtLeast(0L)
        )
        player.prepare()
        // Restore where the person was, but never start audio on their behalf. The
        // service is created the moment the app binds a MediaController, so honouring
        // the saved playWhenReady meant opening Pixelody could start music with no
        // tap at all. The saved flag is still written; it is simply not obeyed here.
        player.playWhenReady = false
    }

    private fun String.containsCredentialQuery(): Boolean = runCatching {
        Uri.parse(this).queryParameterNames.any { it.equals("token", true) || it.equals("access_token", true) }
    }.getOrDefault(true)

    private companion object {
        const val PLAYBACK_PREFERENCES = "pixelody_playback"
        const val KEY_PLAYBACK_STATE = "restorable_playback"
    }
}
