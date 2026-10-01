package com.pixelody.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.ui.brand.PixelodyLogoColor
import com.pixelody.app.ui.theme.PixelodyMobileTheme

class MobileSettingsStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("pixelody_mobile_settings", Context.MODE_PRIVATE))

    fun loadTheme(): PixelodyMobileTheme {
        val rawTheme = prefs.getString(KEY_THEME, null).orEmpty()
        // The theme was renamed before public release; keep older saved choices.
        val themeName = if (rawTheme == LEGACY_MARATHON_TERMINAL) PixelodyMobileTheme.BulkheadTerminal.name else rawTheme
        return PixelodyMobileTheme.values().firstOrNull { it.name == themeName }
            ?: PixelodyMobileTheme.Studio
    }

    fun saveTheme(theme: PixelodyMobileTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun resetTheme() {
        prefs.edit().remove(KEY_THEME).apply()
    }

    fun loadLogoColor(): PixelodyLogoColor = PixelodyLogoColor.fromKey(prefs.getString(KEY_LOGO_COLOR, null))

    fun saveLogoColor(color: PixelodyLogoColor) {
        prefs.edit().putString(KEY_LOGO_COLOR, color.key).apply()
    }

    fun loadLogoFollowsTheme(): Boolean = prefs.getBoolean(KEY_LOGO_FOLLOWS_THEME, true)

    fun saveLogoFollowsTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOGO_FOLLOWS_THEME, enabled).apply()
    }

    fun loadStorageQuotaBytes(): Long {
        return prefs.getLong(KEY_STORAGE_QUOTA_BYTES, DEFAULT_QUOTA_BYTES)
    }

    fun saveStorageQuotaBytes(bytes: Long) {
        prefs.edit().putLong(KEY_STORAGE_QUOTA_BYTES, bytes).apply()
    }

    fun loadAutoCacheFavorites(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CACHE_FAVORITES, false)
    }

    fun saveAutoCacheFavorites(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CACHE_FAVORITES, enabled).apply()
    }

    fun loadAutoCacheRecentCount(): Int {
        return prefs.getInt(KEY_AUTO_CACHE_RECENT_COUNT, 0)
    }

    fun saveAutoCacheRecentCount(count: Int) {
        prefs.edit().putInt(KEY_AUTO_CACHE_RECENT_COUNT, count).apply()
    }

    fun loadDownloadWifiOnly(): Boolean {
        return prefs.getBoolean(KEY_DOWNLOAD_WIFI_ONLY, false)
    }

    fun saveDownloadWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean(KEY_DOWNLOAD_WIFI_ONLY, wifiOnly).apply()
    }

    fun loadLastTrackId(): String? {
        return prefs.getString(KEY_LAST_TRACK_ID, null)
    }

    fun saveLastTrackId(id: String?) {
        if (id == null) {
            prefs.edit().remove(KEY_LAST_TRACK_ID).apply()
        } else {
            prefs.edit().putString(KEY_LAST_TRACK_ID, id).apply()
        }
    }

    fun loadLastTrackPositionMs(): Long {
        return prefs.getLong(KEY_LAST_TRACK_POSITION_MS, 0L)
    }

    fun saveLastTrackPositionMs(pos: Long) {
        prefs.edit().putLong(KEY_LAST_TRACK_POSITION_MS, pos.coerceAtLeast(0L)).apply()
    }

    fun loadLastSourceScope(): String? {
        return prefs.getString(KEY_LAST_SOURCE_SCOPE, null)
    }

    fun saveLastSourceScope(scope: String?) {
        if (scope == null) {
            prefs.edit().remove(KEY_LAST_SOURCE_SCOPE).apply()
        } else {
            prefs.edit().putString(KEY_LAST_SOURCE_SCOPE, scope).apply()
        }
    }

    fun loadShuffleEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHUFFLE_ENABLED, false)
    }

    fun saveShuffleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE_ENABLED, enabled).apply()
    }

    fun loadShuffleMode(): String? {
        return prefs.getString(KEY_SHUFFLE_MODE, null)
    }

    fun saveShuffleMode(mode: String) {
        prefs.edit().putString(KEY_SHUFFLE_MODE, mode).apply()
    }

    fun loadRepeatMode(): Int {
        return prefs.getInt(KEY_REPEAT_MODE, 0)
    }

    fun saveRepeatMode(mode: Int) {
        prefs.edit().putInt(KEY_REPEAT_MODE, mode).apply()
    }

    fun loadPlayerViewMode(): String {
        val mode = prefs.getString(KEY_PLAYER_VIEW_MODE, "Classic") ?: "Classic"
        // Lyrics view is an active reading mode, not the persistent startup console.
        return if (mode == "Lyrics") "Classic" else mode
    }

    fun savePlayerViewMode(mode: String) {
        if (mode == "Lyrics") {
            // Keep default Classic persisted so app restarts never launch trapped in lyrics
            prefs.edit().putString(KEY_PLAYER_VIEW_MODE, "Classic").apply()
        } else {
            prefs.edit().putString(KEY_PLAYER_VIEW_MODE, mode).apply()
        }
    }

    fun loadFavoriteTrackIds(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITE_TRACK_IDS, emptySet()) ?: emptySet()
    }

    fun saveFavoriteTrackIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITE_TRACK_IDS, ids).apply()
    }

    fun isTrackFavorite(trackId: String): Boolean {
        return loadFavoriteTrackIds().contains(trackId)
    }

    fun toggleTrackFavorite(trackId: String): Boolean {
        val current = loadFavoriteTrackIds().toMutableSet()
        val isNowFav = if (current.contains(trackId)) {
            current.remove(trackId)
            false
        } else {
            current.add(trackId)
            true
        }
        saveFavoriteTrackIds(current)
        return isNowFav
    }

    fun loadLoudnessNormalization(): LoudnessNormalizationMode {
        val raw = prefs.getString(KEY_LOUDNESS_NORMALIZATION, null) ?: return LoudnessNormalizationMode.Off
        return LoudnessNormalizationMode.values().firstOrNull { it.name == raw } ?: LoudnessNormalizationMode.Off
    }

    fun saveLoudnessNormalization(mode: LoudnessNormalizationMode) {
        prefs.edit().putString(KEY_LOUDNESS_NORMALIZATION, mode.name).apply()
    }

    fun loadPlaybackSpeed(): Float {
        return prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
    }

    fun savePlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()
    }

    fun loadPitchLocked(): Boolean {
        return prefs.getBoolean(KEY_PITCH_LOCKED, true)
    }

    fun savePitchLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_PITCH_LOCKED, locked).apply()
    }

    fun loadAutoResumeOnHeadset(): Boolean {
        return prefs.getBoolean(KEY_AUTO_RESUME_ON_HEADSET, false)
    }

    fun saveAutoResumeOnHeadset(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESUME_ON_HEADSET, enabled).apply()
    }

    fun loadExperienceMode(): AppExperienceMode {
        val raw = prefs.getString(KEY_EXPERIENCE_MODE, null) ?: return AppExperienceMode.Essential
        return AppExperienceMode.values().firstOrNull { it.name == raw } ?: AppExperienceMode.Essential
    }

    fun saveExperienceMode(mode: AppExperienceMode) {
        prefs.edit().putString(KEY_EXPERIENCE_MODE, mode.name).apply()
    }

    companion object {
        const val KEY_THEME = "active_theme"
        private const val LEGACY_MARATHON_TERMINAL = "MarathonTerminal"
        const val KEY_STORAGE_QUOTA_BYTES = "storage_quota_bytes"
        const val KEY_AUTO_CACHE_FAVORITES = "auto_cache_favorites"
        const val KEY_AUTO_CACHE_RECENT_COUNT = "auto_cache_recent_count"
        const val KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"
        const val KEY_LAST_TRACK_ID = "last_track_id"
        const val KEY_LAST_TRACK_POSITION_MS = "last_track_position_ms"
        const val KEY_LAST_SOURCE_SCOPE = "last_source_scope"
        const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
        const val KEY_SHUFFLE_MODE = "shuffle_mode"
        const val KEY_REPEAT_MODE = "repeat_mode"
        const val KEY_PLAYER_VIEW_MODE = "player_view_mode"
        const val KEY_FAVORITE_TRACK_IDS = "favorite_track_ids"
        const val KEY_LOUDNESS_NORMALIZATION = "loudness_normalization_mode"
        const val KEY_PLAYBACK_SPEED = "playback_speed"
        const val KEY_PITCH_LOCKED = "pitch_locked"
        const val KEY_AUTO_RESUME_ON_HEADSET = "auto_resume_on_headset"
        const val KEY_EXPERIENCE_MODE = "experience_mode"
        const val KEY_LOGO_COLOR = "logo_color"
        const val KEY_LOGO_FOLLOWS_THEME = "logo_follows_theme"

        // Default 5 GB quota
        const val DEFAULT_QUOTA_BYTES = 5L * 1024L * 1024L * 1024L
        const val UNLIMITED_QUOTA_BYTES = -1L
    }
}

enum class LoudnessNormalizationMode(val label: String, val targetLufs: Float?) {
    Off("Off", null),
    StreamingStandard("-14 LUFS (Streaming Standard)", -14.0f),
    AudiophileDynamic("-18 LUFS (Audiophile Dynamic)", -18.0f)
}

