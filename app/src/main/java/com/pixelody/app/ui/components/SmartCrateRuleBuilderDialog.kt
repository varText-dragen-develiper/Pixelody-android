package com.pixelody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.CRATE_SLOT_COUNT
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.Crate
import com.pixelody.app.data.model.CrateSlot
import com.pixelody.app.data.model.Track
import java.util.UUID

enum class BpmBucket(val label: String, val minBpm: Int, val maxBpm: Int) {
    All("ALL TEMPO", 0, 999),
    Chill("CHILL (60-90)", 60, 90),
    Groove("GROOVE (90-120)", 90, 120),
    HighEnergy("ENERGY (120-160)", 120, 160)
}

enum class SortStrategy(val label: String) {
    LosslessPurity("HIGHEST BIT-DEPTH"),
    HarmonicFlow("HARMONIC FLOW"),
    Recent("RECENTLY DIGGED"),
    MostPlayed("HEAVY ROTATION")
}

/**
 * SmartCrateRuleBuilderDialog: Parametric rule builder for algorithmic 9-slot crates.
 * Provides interactive rule criteria (FLAC lossless filter, BPM ranges, Camelot harmonic key selectors,
 * sort strategies) and renders a live 3x3 grid preview of the 9-slot result before committing to the CrateBook.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmartCrateRuleBuilderDialog(
    allTracks: List<Track>,
    onSaveCrate: (Crate) -> Unit,
    onDismiss: () -> Unit,
    onShowDoc: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var crateName by remember { mutableStateOf("Harmonic 24-Bit FLAC Crate") }
    var losslessOnly by remember { mutableStateOf(true) }
    var selectedBpmBucket by remember { mutableStateOf(BpmBucket.All) }
    var selectedCamelotKey by remember { mutableStateOf<CamelotKey?>(null) }
    var selectedSortStrategy by remember { mutableStateOf(SortStrategy.LosslessPurity) }
    var favoritesOnly by remember { mutableStateOf(false) }

    // Live Calculation of matching tracks and slot assignments
    val matchingTracks = remember(
        allTracks,
        losslessOnly,
        selectedBpmBucket,
        selectedCamelotKey,
        selectedSortStrategy,
        favoritesOnly
    ) {
        var filtered = allTracks.asSequence()

        if (losslessOnly) {
            filtered = filtered.filter { it.lossless || it.format.equals("FLAC", ignoreCase = true) }
        }

        if (favoritesOnly) {
            filtered = filtered.filter { it.favorite }
        }

        if (selectedBpmBucket != BpmBucket.All) {
            filtered = filtered.filter { track ->
                val tele = HarmonicKeyEngine.estimateTrackTelemetry(track)
                tele.bpm >= selectedBpmBucket.minBpm && tele.bpm <= selectedBpmBucket.maxBpm
            }
        }

        if (selectedCamelotKey != null) {
            filtered = filtered.filter { track ->
                val tele = HarmonicKeyEngine.estimateTrackTelemetry(track)
                tele.key == selectedCamelotKey || HarmonicKeyEngine.calculateHarmonicRelation(tele.key, selectedCamelotKey!!).isHarmonic
            }
        }

        val list = filtered.toList()
        when (selectedSortStrategy) {
            SortStrategy.LosslessPurity -> list.sortedWith(
                compareByDescending<Track> { if (it.lossless) 1 else 0 }
                    .thenByDescending { it.sampleRate }
                    .thenByDescending { it.bitDepth ?: 16 }
            )
            SortStrategy.HarmonicFlow -> {
                val startKey = selectedCamelotKey ?: list.firstOrNull()?.let { HarmonicKeyEngine.estimateTrackTelemetry(it).key } ?: CamelotKey.K8A
                list.sortedByDescending { track ->
                    val tele = HarmonicKeyEngine.estimateTrackTelemetry(track)
                    HarmonicKeyEngine.calculateHarmonicRelation(startKey, tele.key).score
                }
            }
            SortStrategy.Recent -> list.shuffled()
            SortStrategy.MostPlayed -> list.sortedByDescending { it.durationSeconds }
        }.take(CRATE_SLOT_COUNT)
    }

    val previewSlots: List<CrateSlot?> = remember(matchingTracks) {
        List(CRATE_SLOT_COUNT) { index ->
            matchingTracks.getOrNull(index)?.let { CrateSlot.SingleTrack(it.id) }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // Title Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (onShowDoc != null) {
                                        haptic.performTick()
                                        onShowDoc("smart_crates")
                                    }
                                }
                            )
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PixelodyTransportGlyph(
                                    glyph = TransportGlyphType.DiamondLossless,
                                    color = MaterialTheme.colorScheme.primary,
                                    sizeDp = 18
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Smart playlist",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Choose rules to collect songs from your library",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onShowDoc != null) {
                            CabinetDocButton(
                                onClick = { onShowDoc("smart_crates") },
                                contentDescription = "Smart Crates documentation"
                            )
                        }

                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PixelodyTransportGlyph(
                                    glyph = TransportGlyphType.Close,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    sizeDp = 14
                                )
                            }
                        }
                    }
                }
            }

            // Crate Name Field
            item {
                OutlinedTextField(
                    value = crateName,
                    onValueChange = { crateName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Criteria Controls: Lossless & Favorites Toggles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lossless FLAC Master Only",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Only populate uncompressed 16/24-bit audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = losslessOnly,
                        onCheckedChange = {
                            haptic.performTick()
                            losslessOnly = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    )
                }
            }

            // BPM Range Buckets
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TEMPO / BPM RANGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(BpmBucket.entries) { bucket ->
                            val isSelected = selectedBpmBucket == bucket
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performTick()
                                    selectedBpmBucket = bucket
                                },
                                label = { Text(bucket.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Harmonic Camelot Key Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CAMELOT HARMONIC KEY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (selectedCamelotKey != null) {
                            Text(
                                text = "CLEAR",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { selectedCamelotKey = null }
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCamelotKey == null,
                                onClick = {
                                    haptic.performTick()
                                    selectedCamelotKey = null
                                },
                                label = { Text("ANY KEY", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                        items(CamelotKey.entries) { key ->
                            val isSelected = selectedCamelotKey == key
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performTick()
                                    selectedCamelotKey = key
                                },
                                label = { Text("${key.code} (${key.musicalKey})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Sorting Strategy
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SLOT PRIORITY STRATEGY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SortStrategy.entries) { strategy ->
                            val isSelected = selectedSortStrategy == strategy
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performTick()
                                    selectedSortStrategy = strategy
                                },
                                label = { Text(strategy.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Live 3x3 9-Slot Crate Preview
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE 9-SLOT PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${matchingTracks.size} / 9 SLOTS FILLED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3x3 Grid
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (row in 0 until 3) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (col in 0 until 3) {
                                        val slotIdx = row * 3 + col
                                        val track = matchingTracks.getOrNull(slotIdx)
                                        SmartCrateSlotPreviewCell(
                                            slotIndex = slotIdx + 1,
                                            track = track,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CANCEL")
                    }

                    Button(
                        onClick = {
                            haptic.performConfirm()
                            val newCrate = Crate(
                                id = "smart-${UUID.randomUUID().toString().take(8)}",
                                name = crateName.ifBlank { "Smart playlist" },
                                slots = previewSlots
                            )
                            onSaveCrate(newCrate)
                        },
                        enabled = matchingTracks.isNotEmpty(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save playlist", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartCrateSlotPreviewCell(
    slotIndex: Int,
    track: Track?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        color = if (track != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            1.dp,
            if (track != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        if (track != null) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RemoteArtwork(
                    artworkUrl = track.artworkUrl,
                    title = track.title,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#$slotIndex ${track.title}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.format.uppercase()} • ${track.artist}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SLOT $slotIndex",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
