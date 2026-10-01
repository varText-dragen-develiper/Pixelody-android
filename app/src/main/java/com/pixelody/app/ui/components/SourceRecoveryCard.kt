package com.pixelody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelody.app.ui.navigation.PixelodyStateTags

/**
 * Recovery for a source that stopped working, shown beside that source rather than
 * in a separate diagnostics screen. Explains the state in human terms and offers the
 * safe next actions: try again, listen to something local, or open the tools.
 */
@Composable
fun SourceRecoveryCard(
    stateLabel: String,
    guidance: String,
    modifier: Modifier = Modifier,
    isRetrying: Boolean = false,
    canRetry: Boolean = true,
    onRetry: () -> Unit = {},
    onUsePhoneMusic: (() -> Unit)? = null,
    onOpenConnectionTools: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PixelodyStateTags.SOURCE_RECOVERY),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = stateLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = guidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRetry,
                    enabled = canRetry && !isRetrying,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isRetrying) "Reconnecting" else "Try again")
                }
                if (onUsePhoneMusic != null) {
                    OutlinedButton(
                        onClick = onUsePhoneMusic,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Phone music")
                    }
                }
            }

            if (onOpenConnectionTools != null) {
                OutlinedButton(
                    onClick = onOpenConnectionTools,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connection tools")
                }
            }
        }
    }
}
