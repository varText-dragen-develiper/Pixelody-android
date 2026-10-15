package com.pixelody.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.genre.GenreTaxonomyEngine
import com.pixelody.app.data.model.Track

/**
 * Audiophile Audio Metadata & Tag Inspector/Editor sheet.
 * Presents lossless technical parameters alongside editable ID3 / Vorbis tags.
 */
@Composable
fun TagEditorSheet(
    track: Track,
    onSave: (title: String, artist: String, album: String, genre: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(track) { mutableStateOf(track.title) }
    var artist by remember(track) { mutableStateOf(track.artist) }
    var album by remember(track) { mutableStateOf(track.album) }
    var genre by remember(track) { mutableStateOf(track.genre) }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Audio Tag Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vorbis & ID3 Technical Metadata",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (track.lossless) "LOSSLESS" else "LOSSY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (track.lossless) Color(0xFF6C9EFF) else Color(0xFFFFA726),
                    modifier = Modifier
                        .background(
                            color = (if (track.lossless) Color(0xFF6C9EFF) else Color(0xFFFFA726)).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = (if (track.lossless) Color(0xFF6C9EFF) else Color(0xFFFFA726)).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Technical Specs Pill Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TechPill("FORMAT", track.format.ifBlank { "PCM" }.uppercase())
                TechPill("SAMPLE RATE", "${track.sampleRate / 1000.0} kHz")
                TechPill("DEPTH", "${track.bitDepth ?: 16}-bit")
                if (track.replayGainDb != null) {
                    TechPill("REPLAYGAIN", String.format("%.1f dB", track.replayGainDb))
                }
            }

            // Editable Fields
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Track Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artist") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text("Album") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Genre (e.g. Rock, Ambient, Electronic)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val genreSuggestions = remember(genre) { GenreTaxonomyEngine.suggestGenres(genre, limit = 6) }
            if (genreSuggestions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(genreSuggestions) { suggestion ->
                        SuggestionChip(
                            onClick = {
                                haptic.performTick()
                                genre = suggestion
                            },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Stream / File URL
            if (track.streamUrl.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "STREAM LOCATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.streamUrl,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.size(10.dp))
                Button(
                    onClick = {
                        haptic.performConfirm()
                        onSave(title.trim(), artist.trim(), album.trim(), genre.trim())
                        onDismiss()
                    }
                ) {
                    Text("Save Tags")
                }
            }
        }
    }
}

@Composable
private fun TechPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
