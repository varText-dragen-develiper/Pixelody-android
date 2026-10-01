package com.pixelody.app.data.storage

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.pixelody.app.data.model.Track
import java.io.File
import java.security.MessageDigest
import java.util.Locale

data class LocalTechnicalMetadata(
    val codec: String = "",
    val sampleRate: Int = 0,
    val bitDepth: Int? = null,
    val bitrate: Int? = null,
    val channels: Int = 0,
    val embeddedArtwork: ByteArray? = null,
    val genre: String = ""
)

fun isLocalTrack(track: Track?): Boolean {
    return track?.streamUrl?.startsWith("content://") == true || track?.streamUrl?.startsWith("file://") == true
}

fun isLocalTrackId(id: String): Boolean {
    return id.startsWith("content://") || id.startsWith("file://")
}

fun scanEntireDeviceForAudio(context: Context): List<Track> {
    val tracks = mutableListOf<Track>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.ALBUM_ID
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 10000"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    runCatching {
        context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: cursor.getString(nameCol)?.substringBeforeLast('.')
                    ?: "Track $id"
                val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "On this phone"
                val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Device files"
                val durationMs = cursor.getLong(durationCol)
                val durationSeconds = (durationMs / 1000L).toInt()
                val mimeType = cursor.getString(mimeCol).orEmpty()
                val format = mimeType.substringAfterLast('/').uppercase(Locale.US).ifBlank { "AUDIO" }
                val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L
                val artworkUri = if (albumId > 0) {
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                } else null

                val isLossless = format in setOf("FLAC", "WAV", "WAVE", "AIFF", "AIF", "ALAC") ||
                        mimeType.contains("flac") || mimeType.contains("wav")
                val localMetadata = readLocalTechnicalMetadata(context, contentUri)

                tracks.add(
                    Track(
                        id = contentUri.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationSeconds = durationSeconds,
                        format = format,
                        codec = localMetadata.codec.ifBlank { format },
                        lossless = isLossless,
                        sampleRate = localMetadata.sampleRate,
                        bitDepth = localMetadata.bitDepth,
                        bitrate = localMetadata.bitrate,
                        channels = localMetadata.channels,
                        replayGainDb = null,
                        artworkUrl = persistEmbeddedArtwork(context, contentUri, localMetadata.embeddedArtwork) ?: artworkUri,
                        streamUrl = contentUri.toString(),
                        favorite = false,
                        missing = false,
                        genre = localMetadata.genre
                    )
                )
            }
        }
    }
    return tracks
}

fun scanFolderForTracks(context: Context, treeUri: Uri): List<Track> {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
    val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
    val audioExtensions = setOf("mp3", "flac", "m4a", "wav", "wave", "ogg", "oga", "aac", "opus", "aiff", "aif", "wma", "alac", "dsf", "dff")
    val tracks = mutableListOf<Track>()

    fun traverse(dir: DocumentFile) {
        val files = runCatching { dir.listFiles() }.getOrNull().orEmpty()
        for (file in files) {
            if (file.isDirectory) {
                traverse(file)
            } else {
                val name = file.name.orEmpty()
                val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
                val mime = file.type.orEmpty().lowercase(Locale.US)
                if (ext in audioExtensions || mime.startsWith("audio/") || mime.contains("ogg") || mime.contains("flac")) {
                    localTrackFromUri(context, file.uri)?.let { tracks.add(it) }
                }
            }
        }
    }

    traverse(rootDoc)
    return tracks
}

fun localTrackFromUri(context: Context, uri: Uri): Track? {
    var title = context.displayNameFor(uri).substringBeforeLast('.')
    var artist = "On this phone"
    var album = "Device files"
    var durationSeconds = 0
    var format = uri.toString().substringAfterLast('.', "").uppercase(Locale.US)
    var sampleRate = 0
    var bitDepth: Int? = null
    var bitrate: Int? = null

    runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: title
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                ?: artist
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?: album
            durationSeconds = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.let { (it / 1000L).toInt() }
                ?: 0
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?: 0
                bitDepth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
            }
        } finally {
            retriever.release()
        }
    }

    val normalizedFormat = format.ifBlank { "AUDIO" }
    val technicalMetadata = readLocalTechnicalMetadata(context, uri)
    return Track(
        id = uri.toString(),
        title = title,
        artist = artist,
        album = album,
        durationSeconds = durationSeconds,
        format = normalizedFormat,
        codec = technicalMetadata.codec.ifBlank { normalizedFormat },
        lossless = normalizedFormat in setOf("FLAC", "WAV", "WAVE", "AIFF", "AIF", "ALAC"),
        sampleRate = sampleRate.takeIf { it > 0 } ?: technicalMetadata.sampleRate,
        bitDepth = bitDepth ?: technicalMetadata.bitDepth,
        bitrate = bitrate ?: technicalMetadata.bitrate,
        channels = technicalMetadata.channels,
        replayGainDb = null,
        artworkUrl = persistEmbeddedArtwork(context, uri, technicalMetadata.embeddedArtwork),
        streamUrl = uri.toString(),
        favorite = false,
        missing = false,
        genre = technicalMetadata.genre
    )
}

fun persistEmbeddedArtwork(context: Context, sourceUri: Uri, artwork: ByteArray?): String? {
    val image = artwork ?: return null
    if (image.isEmpty()) return null
    return runCatching {
        val cacheDirectory = File(context.cacheDir, "local-artwork")
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) return null
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sourceUri.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        val destination = File(cacheDirectory, "$digest.image")
        if (!destination.exists() || destination.length() != image.size.toLong()) {
            destination.outputStream().use { output -> output.write(image) }
        }
        Uri.fromFile(destination).toString()
    }.getOrNull()
}

fun readLocalTechnicalMetadata(context: Context, uri: Uri): LocalTechnicalMetadata {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val codec = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?.substringAfterLast('/')
                ?.uppercase(Locale.US)
                .orEmpty()
            val sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?: 0
            } else 0
            val bitDepth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
            } else null
            val extractor = MediaExtractor()
            val audioFormat = try {
                extractor.setDataSource(context, uri, null)
                (0 until extractor.trackCount)
                    .map(extractor::getTrackFormat)
                    .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            } finally {
                extractor.release()
            }
            val extractorSampleRate = audioFormat?.let { format ->
                runCatching { format.getInteger(MediaFormat.KEY_SAMPLE_RATE) }.getOrNull()
            }?.takeIf { it > 0 } ?: 0
            val channels = audioFormat?.let { format ->
                runCatching { format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }.getOrNull()
            }?.takeIf { it > 0 } ?: 0
            LocalTechnicalMetadata(
                codec = codec,
                sampleRate = sampleRate.takeIf { it > 0 } ?: extractorSampleRate,
                bitDepth = bitDepth,
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 },
                channels = channels,
                embeddedArtwork = retriever.embeddedPicture,
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty()
            )
        } finally {
            retriever.release()
        }
    }.getOrDefault(LocalTechnicalMetadata())
}

private fun Context.displayNameFor(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index).orEmpty().ifBlank { uri.lastPathSegment ?: "Audio file" }
        }
    }
    return uri.lastPathSegment ?: "Audio file"
}
