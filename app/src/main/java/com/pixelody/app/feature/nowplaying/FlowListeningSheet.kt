package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.pixelody.app.core.playback.FlowBrowseFilter
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.data.model.*
import com.pixelody.app.ui.components.HarmonicFilterMode
import com.pixelody.app.ui.components.getCompatibleCamelotKeyCodes
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun FlowEntry(mode: FlowShuffleMode, key: CamelotKey?, bpm: Float?, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    val modeLabel = when (mode) {
        FlowShuffleMode.Off -> "In order"
        FlowShuffleMode.SmartFlow -> "Smart"
        FlowShuffleMode.AlbumPreserving -> "Albums"
        FlowShuffleMode.PureRandom -> "Random"
    }
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Flow · $modeLabel" + (key?.let { " · ${it.code}" } ?: "") +
            (bpm?.let { " · ${it.toInt()} BPM" } ?: ""))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FlowListeningSheet(
    mode: FlowShuffleMode, onModeChange: (FlowShuffleMode) -> Unit,
    currentTrack: Track?, upcoming: List<Track>, pool: List<Track>,
    key: CamelotKey?, filterMode: HarmonicFilterMode, bpm: Float?, tolerance: Float,
    onKeyChange: (CamelotKey?) -> Unit, onFilterModeChange: (HarmonicFilterMode) -> Unit,
    onBpmChange: (Float?) -> Unit, onToleranceChange: (Float) -> Unit,
    onPlay: (List<String>) -> Unit, onQueue: (List<String>) -> Unit,
    onSavePlaylist: (List<String>) -> Unit, onDismiss: () -> Unit
) {
    var showMatching by remember { mutableStateOf(key != null || bpm != null) }
    val keyCodes = key?.let { getCompatibleCamelotKeyCodes(it, filterMode) }
    val matches = remember(pool, keyCodes, bpm, tolerance) {
        pool.filter { !it.missing && it.streamUrl.isNotBlank() &&
            FlowBrowseFilter.matches(it, keyCodes, bpm, tolerance) }.distinctBy { it.id }
    }
    val trackKey = currentTrack?.let(FlowBrowseFilter::knownKey)
    val trackBpm = currentTrack?.let(FlowBrowseFilter::knownBpm)
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Flow & shuffle", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) { Text("Done") }
            }
            Text("Choose how the next songs unfold. Your current song keeps playing.",
                style = MaterialTheme.typography.bodyMedium)
            FlowShuffleMode.entries.forEach { choice ->
                Surface(onClick = { onModeChange(choice) }, shape = MaterialTheme.shapes.medium,
                    color = if (choice == mode) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics {
                        selected = choice == mode; role = Role.RadioButton
                    }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = choice == mode, onClick = null)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(choice.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(when (choice) {
                                FlowShuffleMode.Off -> "Keep the current order"
                                FlowShuffleMode.SmartFlow -> "Variety with genre and known key connections"
                                FlowShuffleMode.AlbumPreserving -> "Keep each album together"
                                FlowShuffleMode.PureRandom -> "Let every song surprise you"
                            }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (upcoming.isNotEmpty()) {
                Text("Up next", style = MaterialTheme.typography.titleSmall)
                upcoming.take(3).forEach { track ->
                    Text(track.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
            TextButton(onClick = { showMatching = !showMatching }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (showMatching) "Hide key & tempo" else "Match by key & tempo")
            }
            if (showMatching) {
                Text("Find your next songs", style = MaterialTheme.typography.titleMedium)
                Text("Filters follow you through Search and Library. Only tagged keys and tempos count; songs without them stay available when filters are off.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (trackKey != null || trackBpm != null) {
                    TextButton(onClick = { onKeyChange(trackKey); onBpmChange(trackBpm) }) {
                        Text("Match this song" + (trackKey?.let { " · ${it.code}" } ?: "") +
                            (trackBpm?.let { " · ${it.toInt()} BPM" } ?: ""))
                    }
                } else Text("Current song: key and tempo unavailable", style = MaterialTheme.typography.bodySmall)
                HarmonicWheel(key, filterMode, onKeyChange)
                if (key != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(HarmonicFilterMode.StrictAdjacent to "Smooth", HarmonicFilterMode.AllCompatible to "Explore").forEach { (value, label) ->
                            FilterChip(selected = filterMode == value, onClick = { onFilterModeChange(value) }, label = { Text(label) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(HarmonicFilterMode.EnergyBoost to "Lift", HarmonicFilterMode.SunsetDrift to "Wind down").forEach { (value, label) ->
                            FilterChip(selected = filterMode == value, onClick = { onFilterModeChange(value) }, label = { Text(label) })
                        }
                    }
                    Text(when (filterMode) {
                        HarmonicFilterMode.StrictAdjacent -> "Same key, relative key, and neighboring keys"
                        HarmonicFilterMode.AllCompatible -> "Relative key and up to two steps in either direction"
                        HarmonicFilterMode.EnergyBoost -> "Same key and one or two clockwise steps"
                        HarmonicFilterMode.SunsetDrift -> "Same key and one or two counterclockwise steps"
                    }, style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tempo", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = bpm != null, onCheckedChange = { onBpmChange(if (it) trackBpm ?: 120f else null) },
                        modifier = Modifier.semantics { contentDescription = "Filter by tempo" })
                }
                if (bpm != null) {
                    Text("${bpm.toInt()} BPM · within ${tolerance.toInt()} BPM", style = MaterialTheme.typography.bodyMedium)
                    Slider(value = bpm, onValueChange = { onBpmChange(kotlin.math.round(it)) }, valueRange = 20f..300f, steps = 279,
                        modifier = Modifier.semantics { contentDescription = "Target tempo" })
                    Slider(value = tolerance, onValueChange = { onToleranceChange(kotlin.math.round(it)) }, valueRange = 5f..30f, steps = 24,
                        modifier = Modifier.semantics { contentDescription = "Tempo tolerance" })
                }
                if (key != null || bpm != null) TextButton(onClick = { onKeyChange(null); onBpmChange(null) }) { Text("Clear matching filters") }
            }
            Text("${matches.size} songs in your selected source", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onPlay(matches.map { it.id }); onDismiss() }, enabled = matches.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (key != null || bpm != null) "Play matches" else "Play music")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onQueue(matches.map { it.id }); onDismiss() }, enabled = matches.isNotEmpty(),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Add to queue") }
                OutlinedButton(onClick = { onSavePlaylist(matches.map { it.id }) }, enabled = matches.isNotEmpty(),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Save playlist") }
            }
        }
    }
}

@Composable
private fun HarmonicWheel(key: CamelotKey?, filterMode: HarmonicFilterMode, onKeyChange: (CamelotKey?) -> Unit) {
    var ring by remember(key?.mode) { mutableStateOf(key?.mode ?: CamelotMode.Minor) }
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CamelotMode.entries.forEach { mode ->
            FilterChip(selected = ring == mode, onClick = { ring = mode }, label = { Text(mode.displayName) })
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val diameter = maxWidth.coerceAtMost(320.dp)
        val radius = (diameter - 52.dp) / 2
        val compatible = key?.let { getCompatibleCamelotKeyCodes(it, filterMode) }.orEmpty()
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(colors.outlineVariant, radius.toPx(), style = Stroke(1.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(diameter / 2)) {
                Text(key?.code ?: "Choose a key", style = MaterialTheme.typography.titleMedium)
                if (key != null) Text(key.musicalKey, style = MaterialTheme.typography.bodySmall)
            }
            (1..12).forEach { number ->
                val candidate = CamelotKey.fromNumberAndMode(number, ring)
                val angle = Math.toRadians(number * 30.0 - 90.0)
                Surface(onClick = { onKeyChange(if (key == candidate) null else candidate) }, shape = CircleShape,
                    color = when {
                        key == candidate -> colors.primary
                        candidate.code in compatible -> colors.secondaryContainer
                        else -> colors.surfaceContainerHigh
                    }, contentColor = if (key == candidate) colors.onPrimary else colors.onSurface,
                    border = if (candidate.code in compatible) BorderStroke(1.dp, colors.primary) else null,
                    modifier = Modifier.offset(x = radius * cos(angle).toFloat(), y = radius * sin(angle).toFloat())
                        .size(48.dp).semantics {
                            contentDescription = "${candidate.code}, ${candidate.musicalKey}"
                            selected = key == candidate; role = Role.Button
                        }) {
                    Box(contentAlignment = Alignment.Center) { Text(candidate.code, style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
    }
}
