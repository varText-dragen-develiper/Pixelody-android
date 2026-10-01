package com.pixelody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.SoundMeshNode
import com.pixelody.app.data.model.SpatialSpeakerRole

/**
 * SoundMeshRadarSheet: Visual multi-device spatial radar and collaborative room coordinator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundMeshRadarSheet(
    nodes: List<SoundMeshNode>,
    onDismiss: () -> Unit,
    onAssignRole: (deviceId: String, role: SpatialSpeakerRole) -> Unit = { _, _ -> }
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        SoundMeshRadarContent(
            nodes = nodes,
            onAssignRole = onAssignRole,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
fun SoundMeshRadarContent(
    nodes: List<SoundMeshNode>,
    onAssignRole: (deviceId: String, role: SpatialSpeakerRole) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PIXELODY SOUND MESH",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "Multi-device spatial surround & sub-ms PTP clock sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4ADE80).copy(alpha = 0.20f),
                border = BorderStroke(1.dp, Color(0xFF4ADE80))
            ) {
                Text(
                    text = "PTP SYNC < 1ms",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4ADE80),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        // Spatial Polar Radar Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Concentric radar circles
                drawCircle(color = Color.White.copy(alpha = 0.08f), radius = radius * 0.33f, center = center, style = Stroke(1.5f))
                drawCircle(color = Color.White.copy(alpha = 0.08f), radius = radius * 0.66f, center = center, style = Stroke(1.5f))
                drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.35f), radius = radius, center = center, style = Stroke(2f))

                // Crosshair lines
                drawLine(color = Color.White.copy(alpha = 0.10f), start = Offset(center.x, 0f), end = Offset(center.x, size.height), strokeWidth = 1f)
                drawLine(color = Color.White.copy(alpha = 0.10f), start = Offset(0f, center.y), end = Offset(size.width, center.y), strokeWidth = 1f)

                // Listener center sweet-spot
                drawCircle(color = Color(0xFF4ADE80), radius = 4.dp.toPx(), center = center)
            }

            Text(
                text = "LISTENER SWEET-SPOT",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.40f),
                modifier = Modifier.padding(top = 26.dp)
            )
        }

        // Connected Device Nodes List
        Text(
            text = "CONNECTED ROOM NODES (${nodes.size})",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(nodes) { node ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = node.deviceName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (node.isHostAuthority) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "HOST",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFF59E0B),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Offset: ${node.ptpClockOffsetMs}ms • Latency: ${node.networkLatencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.40f))
                        ) {
                            Text(
                                text = node.assignedRole.tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
