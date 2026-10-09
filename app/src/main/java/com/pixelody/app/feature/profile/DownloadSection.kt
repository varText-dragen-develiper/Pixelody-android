package com.pixelody.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.storage.DownloadQueueManager
import com.pixelody.app.data.storage.DownloadQueueState
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import java.util.Locale

@Composable
internal fun DownloadSection(activeTheme: PixelodyMobileTheme, downloadQueueState: DownloadQueueState, downloadQueueManager: DownloadQueueManager) {
    // Active Download Queue Section
    if (downloadQueueState.tasks.isNotEmpty()) {
        Column {
            SectionCard(
                title = "Download Queue",
                subtitle = if (downloadQueueState.isPaused) "Queue Paused" else "${downloadQueueState.queuedTasks.size} queued • ${downloadQueueState.completedTasks.size} completed"
            ) {
                val active = downloadQueueState.activeTask
                if (active != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = active.track.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${active.track.artist} • ${active.track.format.uppercase(Locale.US)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${(downloadQueueState.activeProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { downloadQueueState.activeProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (downloadQueueState.isPaused) {
                        Button(
                            onClick = { downloadQueueManager.resumeQueue() },
                            shape = activeTheme.plate,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Resume")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { downloadQueueManager.pauseQueue() },
                            shape = activeTheme.plate,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pause")
                        }
                    }
                    if (downloadQueueState.failedTasks.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { downloadQueueManager.retryFailed() },
                            shape = activeTheme.plate,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Retry Failed")
                        }
                    }
                    OutlinedButton(
                        onClick = { downloadQueueManager.cancelAll() },
                        shape = activeTheme.plate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel All")
                    }
                }
            }
        }
    }

}
