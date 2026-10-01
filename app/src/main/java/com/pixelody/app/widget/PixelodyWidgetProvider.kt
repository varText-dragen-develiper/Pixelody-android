package com.pixelody.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.pixelody.app.MainActivity
import com.pixelody.app.R
import com.pixelody.app.core.playback.PixelodyPlaybackService

/**
 * Audiophile Home Screen Widget Provider for Pixelody.
 * Presents real-time playback metadata (title, artist, format badge) and
 * tactile transport controls (previous, play/pause, next).
 */
class PixelodyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, "Pixelody") ?: "Pixelody"
        val artist = prefs.getString(KEY_ARTIST, "Audiophile Player") ?: "Audiophile Player"
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
        val format = prefs.getString(KEY_FORMAT, "HI-RES") ?: "HI-RES"

        for (widgetId in appWidgetIds) {
            val views = buildRemoteViews(context, title, artist, isPlaying, format)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == ACTION_PLAY_PAUSE || action == ACTION_PREV || action == ACTION_NEXT) {
            val serviceIntent = Intent(context, PixelodyPlaybackService::class.java).apply {
                this.action = action
            }
            try {
                context.startService(serviceIntent)
            } catch (_: Exception) {
                // Background start restriction handling
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.pixelody.app.widget.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.pixelody.app.widget.ACTION_PREV"
        const val ACTION_NEXT = "com.pixelody.app.widget.ACTION_NEXT"

        private const val PREFS_NAME = "pixelody_widget_state"
        private const val KEY_TITLE = "widget_title"
        private const val KEY_ARTIST = "widget_artist"
        private const val KEY_IS_PLAYING = "widget_is_playing"
        private const val KEY_FORMAT = "widget_format"

        fun updateAllWidgets(
            context: Context,
            title: String? = null,
            artist: String? = null,
            isPlaying: Boolean? = null,
            format: String? = null
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            title?.let { editor.putString(KEY_TITLE, it) }
            artist?.let { editor.putString(KEY_ARTIST, it) }
            isPlaying?.let { editor.putBoolean(KEY_IS_PLAYING, it) }
            format?.let { editor.putString(KEY_FORMAT, it) }
            editor.apply()

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, PixelodyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
            if (appWidgetIds.isEmpty()) return

            val currentTitle = title ?: prefs.getString(KEY_TITLE, "Pixelody") ?: "Pixelody"
            val currentArtist = artist ?: prefs.getString(KEY_ARTIST, "Audiophile Player") ?: "Audiophile Player"
            val currentIsPlaying = isPlaying ?: prefs.getBoolean(KEY_IS_PLAYING, false)
            val currentFormat = format ?: prefs.getString(KEY_FORMAT, "HI-RES") ?: "HI-RES"

            for (widgetId in appWidgetIds) {
                val views = buildRemoteViews(context, currentTitle, currentArtist, currentIsPlaying, currentFormat)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        fun buildRemoteViews(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            format: String
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_pixelody)

            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            views.setTextViewText(R.id.widget_format, format.uppercase())
            views.setTextViewText(R.id.widget_btn_play_pause, if (isPlaying) "❚❚" else "▶")

            // Click area opens Now Playing in Pixelody
            val openAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse("pixelody://player")).apply {
                setClass(context, MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_click_area, openAppPendingIntent)

            // Play/Pause button
            val playPauseIntent = Intent(context, PixelodyWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                playPauseIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

            // Previous button
            val prevIntent = Intent(context, PixelodyWidgetProvider::class.java).apply {
                action = ACTION_PREV
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                prevIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

            // Next button
            val nextIntent = Intent(context, PixelodyWidgetProvider::class.java).apply {
                action = ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                3,
                nextIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            return views
        }
    }
}
