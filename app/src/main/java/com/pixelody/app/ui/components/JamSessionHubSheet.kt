package com.pixelody.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.JamGuestPolicy
import com.pixelody.app.data.model.JamParticipant
import com.pixelody.app.data.model.JamRole
import com.pixelody.app.data.model.JamSession
import com.pixelody.app.data.model.JamSyncStatus
import com.pixelody.app.data.model.QueueItem
import com.pixelody.app.data.model.SoundMeshNode
import com.pixelody.app.data.model.SpatialSpeakerRole
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.SoundMeshRadarSheet

enum class JamHubTab(val label: String, val glyph: TransportGlyphType) {
    Queue("Queue & Voting", TransportGlyphType.Queue),
    SpatialRadar("Spatial Radar", TransportGlyphType.OmniSource),
    Participants("People & DJ", TransportGlyphType.MeshNetwork),
    Governance("Governance", TransportGlyphType.Sliders),
    Invite("Invite & QR", TransportGlyphType.QrScan)
}

@Composable
fun JamSessionHubSheet(
    session: JamSession,
    syncStatus: JamSyncStatus,
    isHost: Boolean,
    isDj: Boolean,
    onStartHosting: () -> Unit,
    onJoinSession: (String) -> Unit,
    onLeaveSession: () -> Unit,
    onVoteTrack: (queueItemId: String, vote: Int) -> Unit,
    onPromoteSuggestion: (queueItemId: String) -> Unit,
    onRemoveQueueItem: (queueItemId: String) -> Unit,
    onClearQueue: () -> Unit,
    onSetDj: (deviceId: String) -> Unit,
    onKickParticipant: (deviceId: String) -> Unit,
    onUpdatePolicy: (JamGuestPolicy) -> Unit,
    onResyncClock: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = true) {
        onClose()
    }

    var selectedTab by remember { mutableStateOf(JamHubTab.Queue) }
    var joinCodeInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "jam_mesh_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mesh_pulse_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .scale(if (session.active) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (session.active)
                                    Brush.radialGradient(listOf(Color(0xFFA855F7), Color(0xFF6366F1)))
                                else
                                    Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.MeshNetwork,
                            color = Color.White,
                            sizeDp = 20
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (session.active) "J.A.M. Mesh Room" else "Joint Audio Mesh",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (session.active) {
                                Surface(
                                    color = Color(0xFFA855F7).copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isHost) "HOST" else if (isDj) "DJ" else "GUEST",
                                        color = Color(0xFFA855F7),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (session.active)
                                "${session.participants.size} device${if (session.participants.size == 1) "" else "s"} connected • ${syncStatus.syncQuality}"
                            else
                                "Collaborative multi-room synchronized audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            haptic.performTick()
                            onClose()
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Close,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            sizeDp = 16
                        )
                    }
                }
            }

            // Sync Quality & Drift Telemetry Pill
            if (session.active) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (syncStatus.isSynchronized) Color(0xFF4ADE80) else Color(0xFFF59E0B)
                                    )
                            )
                            Text(
                                text = "Sync Drift: ${syncStatus.driftDeltaMs}ms • Latency: ${syncStatus.roundTripTimeMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performTick()
                                    onResyncClock()
                                },
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PixelodyTransportGlyph(
                                    glyph = TransportGlyphType.Refresh,
                                    color = MaterialTheme.colorScheme.primary,
                                    sizeDp = 11
                                )
                                Text(
                                    text = "Re-align",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!session.active) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Host or Join a J.A.M. Mesh",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Synchronize playback in sample-accurate alignment with nearby phones and PCs on the same network.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                haptic.performConfirm()
                                onStartHosting()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                        ) {
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.MeshNetwork,
                                color = Color.White,
                                sizeDp = 16
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Hosting New Session")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Or enter a 6-character Join Code:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { if (it.length <= 8) joinCodeInput = it.uppercase() },
                                placeholder = { Text("e.g. 8K9P2X") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (joinCodeInput.isNotBlank()) {
                                        haptic.performConfirm()
                                        onJoinSession(joinCodeInput)
                                    }
                                },
                                enabled = joinCodeInput.isNotBlank()
                            ) {
                                Text("Join")
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    JamHubTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        val tabBg by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            label = "tab_bg"
                        )
                        val tabColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "tab_color"
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performTick()
                                    selectedTab = tab
                                },
                            color = tabBg,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PixelodyTransportGlyph(
                                    glyph = tab.glyph,
                                    color = tabColor,
                                    sizeDp = 16
                                )
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = tabColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    JamHubTab.Queue -> JamQueueVotingPanel(
                        queue = session.queue,
                        isHost = isHost,
                        isDj = isDj,
                        onVoteTrack = onVoteTrack,
                        onPromoteSuggestion = onPromoteSuggestion,
                        onRemoveQueueItem = onRemoveQueueItem,
                        onClearQueue = onClearQueue
                    )

                    JamHubTab.SpatialRadar -> {
                        val nodes = remember(session.participants, syncStatus) {
                            session.participants.mapIndexed { idx, p ->
                                val role = when (idx % 4) {
                                    0 -> SpatialSpeakerRole.LeftMain
                                    1 -> SpatialSpeakerRole.RightMain
                                    2 -> SpatialSpeakerRole.CenterSub
                                    else -> SpatialSpeakerRole.VisualizerDisplay
                                }
                                SoundMeshNode(
                                    deviceId = p.deviceId,
                                    deviceName = p.name.ifBlank { "Device ${idx + 1}" },
                                    assignedRole = role,
                                    ptpClockOffsetMs = syncStatus.driftDeltaMs,
                                    networkLatencyMs = syncStatus.roundTripTimeMs,
                                    isHostAuthority = p.role.equals("Host", ignoreCase = true) || p.isLocalDevice
                                )
                            }
                        }

                        SoundMeshRadarContent(
                            nodes = nodes,
                            onAssignRole = { _, _ -> },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    JamHubTab.Participants -> JamParticipantsPanel(
                        participants = session.participants,
                        activeDjDeviceId = session.activeDjDeviceId,
                        isHost = isHost,
                        onSetDj = onSetDj,
                        onKickParticipant = onKickParticipant
                    )

                    JamHubTab.Governance -> JamGovernancePanel(
                        policy = session.permissions,
                        isHost = isHost,
                        onUpdatePolicy = onUpdatePolicy
                    )

                    JamHubTab.Invite -> JamInviteQrPanel(
                        joinCode = session.joinCode.ifBlank { session.sessionId.takeLast(6).uppercase() },
                        sessionId = session.sessionId,
                        onLeaveSession = {
                            haptic.performConfirm()
                            onLeaveSession()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun JamQueueVotingPanel(
    queue: List<QueueItem>,
    isHost: Boolean,
    isDj: Boolean,
    onVoteTrack: (queueItemId: String, vote: Int) -> Unit,
    onPromoteSuggestion: (queueItemId: String) -> Unit,
    onRemoveQueueItem: (queueItemId: String) -> Unit,
    onClearQueue: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${queue.size} Track${if (queue.size == 1) "" else "s"} in Session",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            if ((isHost || isDj) && queue.isNotEmpty()) {
                TextButton(
                    onClick = {
                        haptic.performConfirm()
                        onClearQueue()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Clear Queue",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (queue.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Mesh queue is currently empty",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Add or suggest tracks from your Phone or PC library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(queue, key = { it.queueItemId }) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (item.playbackStatus == "playing")
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (item.playbackStatus == "playing")
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.isSuggestion) {
                                        Surface(
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "SUGGESTION",
                                                color = Color(0xFFF59E0B),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.artist.ifBlank { "Unknown Artist" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "By ${item.addedByName.ifBlank { "Guest" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Voting & Actions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performTick()
                                            onVoteTrack(item.queueItemId, 1)
                                        },
                                    color = if (item.votes > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "▲",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (item.votes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${item.votes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if ((isHost || isDj) && item.isSuggestion) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                haptic.performConfirm()
                                                onPromoteSuggestion(item.queueItemId)
                                            },
                                        color = Color(0xFF4ADE80).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Accept",
                                            color = Color(0xFF4ADE80),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                if (isHost || isDj) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                haptic.performTick()
                                                onRemoveQueueItem(item.queueItemId)
                                            },
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Box(modifier = Modifier.padding(6.dp)) {
                                            PixelodyTransportGlyph(
                                                glyph = TransportGlyphType.Close,
                                                color = MaterialTheme.colorScheme.error,
                                                sizeDp = 12
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JamParticipantsPanel(
    participants: List<JamParticipant>,
    activeDjDeviceId: String?,
    isHost: Boolean,
    onSetDj: (deviceId: String) -> Unit,
    onKickParticipant: (deviceId: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Connected Mesh Devices (${participants.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(participants, key = { it.deviceId }) { participant ->
                val isDj = participant.role == JamRole.Dj.id || participant.deviceId == activeDjDeviceId
                val isCoordinator = participant.role == JamRole.Host.id

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCoordinator -> Color(0xFFA855F7)
                                            isDj -> Color(0xFF38BDF8)
                                            else -> Color(0xFF64748B)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                PixelodyTransportGlyph(
                                    glyph = if (isCoordinator) TransportGlyphType.MeshNetwork
                                    else if (isDj) TransportGlyphType.Equalizer
                                    else TransportGlyphType.PhoneDevice,
                                    color = Color.White,
                                    sizeDp = 16
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = participant.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (participant.isLocalDevice) {
                                        Text(
                                            text = "(You)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = when {
                                        isCoordinator -> "👑 Host & Master Clock"
                                        isDj -> "🎧 Guest DJ"
                                        else -> "📱 Guest Listener"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isHost && !participant.isLocalDevice) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performConfirm()
                                            onSetDj(participant.deviceId)
                                        },
                                    color = if (isDj) MaterialTheme.colorScheme.surface else Color(0xFF38BDF8).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = if (isDj) "Revoke DJ" else "Make DJ",
                                        color = if (isDj) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF38BDF8),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            haptic.performTick()
                                            onKickParticipant(participant.deviceId)
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Box(modifier = Modifier.padding(6.dp)) {
                                        PixelodyTransportGlyph(
                                            glyph = TransportGlyphType.Close,
                                            color = MaterialTheme.colorScheme.error,
                                            sizeDp = 12
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JamGovernancePanel(
    policy: JamGuestPolicy,
    isHost: Boolean,
    onUpdatePolicy: (JamGuestPolicy) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Guest Permissions & Room Governance",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isHost) "Configure what connected guests can do during this J.A.M. session."
            else "Guest permissions are managed by the session host.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        GovernanceToggleRow(
            label = "Allow Guests to Suggest Tracks",
            subtitle = "Guests can submit track suggestions for voting",
            checked = policy.guestsCanSuggest,
            enabled = isHost,
            onCheckedChange = {
                haptic.performTick()
                onUpdatePolicy(policy.copy(guestsCanSuggest = it))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        GovernanceToggleRow(
            label = "Allow Direct Queueing",
            subtitle = "Guests can append tracks directly without voting",
            checked = policy.guestsCanQueue,
            enabled = isHost,
            onCheckedChange = {
                haptic.performTick()
                onUpdatePolicy(policy.copy(guestsCanQueue = it))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        GovernanceToggleRow(
            label = "Allow Queue Editing & Reordering",
            subtitle = "Guests can reorder and delete items in the queue",
            checked = policy.guestsCanEditQueue,
            enabled = isHost,
            onCheckedChange = {
                haptic.performTick()
                onUpdatePolicy(policy.copy(guestsCanEditQueue = it))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        GovernanceToggleRow(
            label = "Allow Playback Control",
            subtitle = "Guests can play, pause, and skip tracks",
            checked = policy.guestsCanControlPlayback,
            enabled = isHost,
            onCheckedChange = {
                haptic.performTick()
                onUpdatePolicy(policy.copy(guestsCanControlPlayback = it))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        GovernanceToggleRow(
            label = "Allow Federated Local Media",
            subtitle = "Guests can stream audio files from their own phone storage",
            checked = policy.federatedSourcesEnabled,
            enabled = isHost,
            onCheckedChange = {
                haptic.performTick()
                onUpdatePolicy(policy.copy(federatedSourcesEnabled = it))
            }
        )
    }
}

@Composable
private fun GovernanceToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFA855F7)
                )
            )
        }
    }
}

@Composable
private fun JamInviteQrPanel(
    joinCode: String,
    sessionId: String,
    onLeaveSession: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Join Code",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        ) {
            Text(
                text = joinCode,
                style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 4.sp),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.size(140.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(2.dp, Color(0xFFA855F7))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PixelodyTransportGlyph(
                    glyph = TransportGlyphType.QrScan,
                    color = Color.Black,
                    sizeDp = 84
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLeaveSession,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            PixelodyTransportGlyph(
                glyph = TransportGlyphType.Close,
                color = MaterialTheme.colorScheme.error,
                sizeDp = 16
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Leave J.A.M. Session")
        }
    }
}
