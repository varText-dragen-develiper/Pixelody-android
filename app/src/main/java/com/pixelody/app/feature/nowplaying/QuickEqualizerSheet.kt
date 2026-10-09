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
                Text("Equalizer", style = MaterialTheme.typography.titleLarge)
                Switch(checked = profile.enabled && !masteringActive,
                    onCheckedChange = { commit(profile.copy(enabled = it)) },
                    modifier = Modifier.semantics { contentDescription = "Enable equalizer" })
            }
            Text(if (masteringActive) "Mastering is active. Editing EQ switches to the equalizer."
                else "Choose a preset or drag the five points to shape your sound.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (roomEffectActive) {
                TextButton(onClick = onDisableRoomEffect) { Text("Turn off saved room effect") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !editTrack, onClick = { editTrack = false }, label = { Text("All songs") })
                FilterChip(selected = editTrack, onClick = { editTrack = true }, label = { Text("This song") })
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (editTrack && track != null) TextButton(onClick = { onTrackChange(null); editTrack = false }) { Text("Use all-songs EQ") }
                else Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}
