package com.pixelody.app.data.storage

import android.content.Context
import android.net.Uri
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * MediaStoreAudioRepository: Reactive repository for scanning, indexing, and querying
 * local lossless and hi-res audio files on physical device storage.
 */
class MediaStoreAudioRepository(
    private val context: Context,
    private val dbRepository: PixelodyPersistenceRepository? = null
) {
    private val _scannedTracks = MutableStateFlow<List<Track>>(emptyList())
    val scannedTracks: StateFlow<List<Track>> = _scannedTracks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStatus = MutableStateFlow("Ready")
    val scanStatus: StateFlow<String> = _scanStatus.asStateFlow()

    private val _totalLosslessCount = MutableStateFlow(0)
    val totalLosslessCount: StateFlow<Int> = _totalLosslessCount.asStateFlow()

    suspend fun scanDeviceStorage(): List<Track> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanStatus.value = "Indexing device storage for audio files..."
        try {
            val scanned = scanEntireDeviceForAudio(context)
            val enriched = scanned.map { track ->
                // Ensure Camelot key and BPM are estimated if not already populated
                val tele = HarmonicKeyEngine.estimateTrackTelemetry(track)
                track
            }

            _scannedTracks.value = enriched
            _totalLosslessCount.value = enriched.count { it.lossless }
            _scanStatus.value = "Indexed ${enriched.size} track${if (enriched.size == 1) "" else "s"} (${_totalLosslessCount.value} lossless FLAC/WAV)."

            // Cache to SQLite repository if available
            dbRepository?.let { db ->
                runCatching {
                    db.cacheScannedTracks(enriched)
                }
            }

            enriched
        } catch (e: Exception) {
            _scanStatus.value = "Scan error: ${e.localizedMessage ?: "Unknown"}"
            emptyList()
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun scanFolder(treeUri: Uri): List<Track> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanStatus.value = "Scanning selected directory..."
        try {
            val scanned = scanFolderForTracks(context, treeUri)
            val current = _scannedTracks.value
            val merged = (current + scanned).distinctBy { it.id }
            _scannedTracks.value = merged
            _totalLosslessCount.value = merged.count { it.lossless }
            _scanStatus.value = "Found ${scanned.size} audio track${if (scanned.size == 1) "" else "s"} in directory."

            dbRepository?.let { db ->
                runCatching {
                    db.cacheScannedTracks(merged)
                }
            }

            merged
        } catch (e: Exception) {
            _scanStatus.value = "Folder scan error: ${e.localizedMessage ?: "Unknown"}"
            emptyList()
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun loadCachedTracks(): List<Track> = withContext(Dispatchers.IO) {
        val cached = dbRepository?.loadCachedScannedTracks().orEmpty()
        if (cached.isNotEmpty()) {
            _scannedTracks.value = cached
            _totalLosslessCount.value = cached.count { it.lossless }
            _scanStatus.value = "Loaded ${cached.size} cached tracks from local database."
        }
        cached
    }

    fun filterLossless(): List<Track> {
        return _scannedTracks.value.filter { it.lossless }
    }

    fun filterByFormat(format: String): List<Track> {
        return _scannedTracks.value.filter { it.format.equals(format, ignoreCase = true) }
    }

    fun clear() {
        _scannedTracks.value = emptyList()
        _totalLosslessCount.value = 0
        _scanStatus.value = "Cleared"
    }
}
