package com.pixelody.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelody.app.ui.components.SectionCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@Composable
internal fun DeveloperResetSection(onReset: suspend (Boolean) -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    var includeTrackEdits by rememberSaveable { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    SectionCard(title = "Developer tools", subtitle = "Start a fresh testing baseline.") {
        Text("Clear listening history, session insights, archived capsules and cached key/BPM estimates.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled = !running, onClick = { includeTrackEdits = false; confirming = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (running) "Resetting…" else "Reset testing data")
        }
        if (result.isNotEmpty()) Text(result, style = MaterialTheme.typography.bodySmall)
    }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text("Reset testing data?") }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Playback stops and the queue is emptied. Listening history, insights, capsule archives and key/BPM caches are cleared.")
                Text("Music files, playlists, favorites, EQ, appearance and your desktop connection are kept.")
                Row {
                    Checkbox(checked = includeTrackEdits, onCheckedChange = { includeTrackEdits = it },
                        modifier = Modifier.semantics { contentDescription = "Also clear manual track edits" })
                    Text("Also clear manual track edits (titles, artists, albums, genres and artwork overrides)",
                        modifier = Modifier.weight(1f))
                }
                Text("Cleared records cannot be restored. Flow currently uses track metadata and the current song; there is no saved learning profile to erase.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = {
            TextButton(onClick = {
                confirming = false
                running = true
                result = ""
                scope.launch {
                    try {
                        withContext(NonCancellable) { onReset(includeTrackEdits) }
                        result = "Reset complete. Start playback to begin a new test."
                    } catch (error: kotlinx.coroutines.CancellationException) { throw error
                    } catch (error: Exception) {
                        result = "Reset did not finish. Some data may already be cleared; try again."
                    } finally { running = false }
                }
            }) { Text("Reset data") }
        }, dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } })
}
