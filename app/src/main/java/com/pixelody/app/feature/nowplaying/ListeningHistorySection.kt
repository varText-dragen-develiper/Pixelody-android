package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.ui.components.SectionCard

/** Daily capsule mechanics belong to listening, with detail available on demand. */
@Composable
internal fun ListeningHistorySection(capsule: DailySonicCapsule, onPlay: (String) -> Unit, onDetails: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = "Listening history", subtitle = if (capsule.hasData)
        "${capsule.audioDna.tracksPlayedCount} plays · ${capsule.audioDna.totalListeningMinutes} min today"
        else "Your recent plays appear here") {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (expanded) "Hide recent plays" else "Show recent plays")
        }
        if (expanded) {
            capsule.memoryTimeline.sortedByDescending { it.timestampMs }.distinctBy { it.trackId }.take(5).forEach { memory ->
                TextButton(onClick = { onPlay(memory.trackId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("${memory.title} · ${memory.artist}")
                }
            }
            if (capsule.hasData) TextButton(onClick = onDetails, modifier = Modifier.heightIn(min = 48.dp)) { Text("Listening details") }
        }
    }
}
