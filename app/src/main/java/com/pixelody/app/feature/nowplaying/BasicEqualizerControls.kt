package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import java.util.Locale

internal enum class BasicTone(val label: String, val bands: List<Int>) {
    Bass("Bass", listOf(0, 1)), Mids("Mids", listOf(2)), Treble("Treble", listOf(3, 4))
}

internal fun EqualizerProfile.toneGain(tone: BasicTone): Float =
    tone.bands.map { normalized().gainsDb[it] }.average().toFloat()

/** Move the group together, preserving the detailed curve until a band reaches its limit. */
internal fun EqualizerProfile.withToneGain(tone: BasicTone, gain: Float): EqualizerProfile {
    val profile = normalized()
    val delta = gain.coerceIn(EqualizerProfile.MIN_GAIN_DB, EqualizerProfile.MAX_GAIN_DB) - profile.toneGain(tone)
    return profile.copy(enabled = true, preset = EqualizerPreset.Custom,
        gainsDb = profile.gainsDb.mapIndexed { index, value ->
            if (index in tone.bands) (value + delta).coerceIn(EqualizerProfile.MIN_GAIN_DB, EqualizerProfile.MAX_GAIN_DB) else value
        })
}

internal fun EqualizerProfile.flatTone(): EqualizerProfile = withPreset(EqualizerPreset.Flat)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BasicEqualizerControls(
    global: EqualizerProfile, track: EqualizerProfile?, masteringActive: Boolean,
    onMasteringChange: (Boolean) -> Unit, onGlobalChange: (EqualizerProfile) -> Unit,
    onTrackChange: (EqualizerProfile?) -> Unit, onResetEffects: () -> Unit = {}
) {
    val profile = (track ?: global).normalized()
    val scope = if (track != null) "This song" else "All songs"
    fun commit(next: EqualizerProfile) {
        if (masteringActive) onMasteringChange(false)
        if (track != null) onTrackChange(next) else onGlobalChange(next)
    }
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("player:basic-equalizer")) {
        Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Equalizer", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Text(if (masteringActive) "Mastering active" else "$scope · Drag to tune",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onResetEffects(); commit(profile.flatTone()) },
                    modifier = Modifier.heightIn(min = 48.dp)
                        .semantics { contentDescription = "Reset tone to flat, $scope" }) {
                    Text("Flat", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = { commit(profile.copy(enabled = !(profile.enabled && !masteringActive))) },
                    modifier = Modifier.heightIn(min = 48.dp)
                        .semantics { contentDescription = "Enable basic equalizer, $scope" }) {
                    Text(if (profile.enabled && !masteringActive) "On" else "Off", style = MaterialTheme.typography.labelMedium)
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.5f
                val tones: @Composable (Modifier) -> Unit = { toneModifier ->
                    BasicTone.entries.forEach { tone ->
                    Column(toneModifier) {
                        val gain = profile.toneGain(tone)
                        val adjusted = kotlin.math.abs(gain) >= 0.05f
                        val color = if (adjusted && profile.enabled && !masteringActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        Text(if (adjusted) "${tone.label} ${String.format(Locale.ROOT, "%+.1f", gain)}" else tone.label,
                            style = MaterialTheme.typography.labelSmall, color = color)
                        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("player:tone-${tone.name}").semantics(mergeDescendants = true) {
                                contentDescription = "${tone.label}, $scope"
                                stateDescription = "${String.format(Locale.ROOT, "%+.1f", gain)} decibels"
                                progressBarRangeInfo = ProgressBarRangeInfo(gain,
                                    EqualizerProfile.MIN_GAIN_DB..EqualizerProfile.MAX_GAIN_DB)
                                setProgress { value ->
                                    if (value.isFinite()) { commit(profile.withToneGain(tone, value)); true } else false
                                }
                            }) {
                        Slider(value = gain, onValueChange = { commit(profile.withToneGain(tone, it)) },
                            valueRange = EqualizerProfile.MIN_GAIN_DB..EqualizerProfile.MAX_GAIN_DB,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clearAndSetSemantics {},
                            thumb = { Box(Modifier.size(16.dp).background(color, CircleShape)) },
                            track = { state -> SliderDefaults.Track(state,
                                modifier = Modifier.height(3.dp), thumbTrackGapSize = 0.dp,
                                colors = SliderDefaults.colors(activeTrackColor = color.copy(alpha = 0.55f),
                                    inactiveTrackColor = color.copy(alpha = 0.18f))) })
                        }
                    }
                }
                }
                if (stacked) Column { tones(Modifier.fillMaxWidth()) }
                else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { tones(Modifier.weight(1f)) }
            }
        }
        }
    }
}
