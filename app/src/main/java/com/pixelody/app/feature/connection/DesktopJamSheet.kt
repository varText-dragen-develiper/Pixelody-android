package com.pixelody.app.feature.connection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.JamSession

/** Android displays the Windows-owned session; it cannot fabricate a mesh room or role. */
@Composable
internal fun DesktopJamSheet(
    session: JamSession?, connectionState: HostConnectionState, joining: Boolean, issue: String,
    hasSavedHost: Boolean, onJoin: () -> Unit, onConnect: () -> Unit, onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, tonalElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("J.A.M.", style = MaterialTheme.typography.headlineSmall)
            when {
                connectionState != HostConnectionState.Connected -> {
                    Text("Connect your desktop to join its listening session. Your phone music stays available.")
                    Button(onClick = onConnect, enabled = !joining, modifier = Modifier.fillMaxWidth()) {
                        Text(if (hasSavedHost) "Reconnect desktop" else "Connect desktop")
                    }
                }
                session?.active != true -> {
                    Text("No active desktop session. Start J.A.M. on your desktop, then return here.")
                }
                session.currentParticipant == null -> {
                    Text("A desktop listening session is available.")
                    Button(onClick = onJoin, enabled = !joining, modifier = Modifier.fillMaxWidth()) {
                        Text(if (joining) "Joining…" else "Join desktop J.A.M.")
                    }
                }
                else -> {
                    val participant = session.currentParticipant
                    Text(if (participant.status == "pending") "Waiting for desktop approval" else "${participant.name} · ${participant.status}")
                    Text("Role: ${participant.role}", style = MaterialTheme.typography.bodyMedium)
                    Text("${session.participants.size} participants · ${session.queue.size} shared tracks")
                    if (participant.permissions.isNotEmpty()) {
                        Text("Allowed: ${participant.permissions.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("The desktop owns this session and its shared queue. Your player controls manage phone playback.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            if (issue.isNotBlank()) Text(issue, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Back to music") }
        }
    }
}
