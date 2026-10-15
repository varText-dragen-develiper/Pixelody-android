package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.ui.components.PixelodyEqualizerCurve

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickEqualizerSheet(
    trackId: String, global: EqualizerProfile, track: EqualizerProfile?, masteringActive: Boolean,
    onMasteringChange: (Boolean) -> Unit, onGlobalChange: (EqualizerProfile) -> Unit,
    onTrackChange: (EqualizerProfile?) -> Unit, onDismiss: () -> Unit,
    runtimeState: com.pixelody.app.core.playback.EqualizerRuntimeState = com.pixelody.app.core.playback.EqualizerRuntimeState(),
    roomEffectActive: Boolean = false, onDisableRoomEffect: () -> Unit = {}
) {
    var editTrack by remember(trackId) { mutableStateOf(track != null) }
    val profile = (if (editTrack) track ?: global else global).normalized()
    fun commit(next: EqualizerProfile) {
        if (masteringActive) onMasteringChange(false)
        if (editTrack) onTrackChange(next) else onGlobalChange(next)
    }
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Advanced equalizer", style = MaterialTheme.typography.titleLarge)
                Switch(checked = profile.enabled && !masteringActive,
                    onCheckedChange = { commit(profile.copy(enabled = it)) },
                    modifier = Modifier.semantics { contentDescription = "Enable equalizer" })
            }
            Text(runtimeState.message, style = MaterialTheme.typography.labelSmall,
                color = if (runtimeState.active) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            Text(if (masteringActive) "Mastering is active. Editing EQ switches to the equalizer."
                else "Choose a preset, drag the curve or adjust each band precisely.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (roomEffectActive) {
                TextButton(onClick = onDisableRoomEffect) { Text("Turn off saved room effect") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !editTrack, onClick = { editTrack = false }, label = { Text("All songs") })
                if (trackId.isNotBlank()) FilterChip(selected = editTrack, onClick = { editTrack = true }, label = { Text("This song") })
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EqualizerPreset.quickPresets) { preset ->
                    FilterChip(selected = profile.preset == preset && profile.enabled && !masteringActive,
                        onClick = { commit(profile.withPreset(preset)) }, label = { Text(preset.displayName) })
                }
            }
            PixelodyEqualizerCurve(gainsDb = profile.gainsDb, enabled = profile.enabled && !masteringActive,
                onBandGainChange = { index, gain -> commit(profile.withBandGain(index, gain)) },
                modifier = Modifier.testTag("player:quick-equalizer-curve"))
            EqualizerPreset.bandLabels.forEachIndexed { index, label ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$label Hz", modifier = Modifier.width(56.dp), style = MaterialTheme.typography.labelMedium)
                    Slider(value = profile.gainsDb[index],
                        onValueChange = { commit(profile.withBandGain(index, it)) },
                        valueRange = EqualizerProfile.MIN_GAIN_DB..EqualizerProfile.MAX_GAIN_DB,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .semantics { contentDescription = "Equalizer band $label Hz" })
                    Text(java.lang.String.format(java.util.Locale.ROOT, "%+.1f dB", profile.gainsDb[index]),
                        modifier = Modifier.width(64.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (editTrack && track != null) TextButton(onClick = { onTrackChange(null); editTrack = false }) { Text("Use all-songs EQ") }
                else Spacer(Modifier.weight(1f))
                TextButton(onClick = { onDisableRoomEffect(); commit(profile.flatTone()) }) { Text("Reset to flat") }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}
