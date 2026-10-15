package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.model.DailySonicCapsule

/** Daily capsule mechanics belong to listening, with detail available on demand. */
@Composable
internal fun ListeningHistorySection(capsule: DailySonicCapsule, onPlay: (String) -> Unit, onDetails: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Listening history", style = MaterialTheme.typography.titleMedium)
        Text(if (capsule.hasData)
            "${capsule.audioDna.tracksPlayedCount} plays · ${capsule.audioDna.totalListeningMinutes} min today"
            else "Your recent plays appear here", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        capsule.memoryTimeline.sortedByDescending { it.timestampMs }.distinctBy { it.trackId }.take(5).forEach { memory ->
            TextButton(onClick = { onPlay(memory.trackId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("${memory.title} · ${memory.artist}")
            }
        }
        if (capsule.hasData) TextButton(onClick = onDetails, modifier = Modifier.heightIn(min = 48.dp)) { Text("Listening details") }
    }
}
