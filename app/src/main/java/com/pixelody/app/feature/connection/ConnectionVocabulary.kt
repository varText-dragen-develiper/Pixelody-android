package com.pixelody.app.feature.connection

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixelody.app.core.hosting.AndroidHostStatus
import com.pixelody.app.data.model.HostConnectionDetails
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.HostingVisibility
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.LiveState
import com.pixelody.app.data.model.NetworkMode
import com.pixelody.app.data.model.SavedHostProfile
import com.pixelody.app.data.model.TrustedDevice
import com.pixelody.app.ui.components.ScreenHeader
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.StatusChip
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/* =========================================================================
 * Slice 2 Connection Vocabulary
 * Connection guidance, recovery, labels, pairing panel, QR scanner, sharing.
 * ========================================================================= */

internal fun hostSourceNeedsRecovery(state: HostConnectionState): Boolean = when (state) {
    HostConnectionState.AuthFailed,
    HostConnectionState.PermissionDenied,
    HostConnectionState.Revoked,
    HostConnectionState.CredentialExpired,
    HostConnectionState.NetworkUnavailable,
    HostConnectionState.HostUnavailable,
    HostConnectionState.Unreachable,
    HostConnectionState.Offline -> true
    else -> false
}

internal fun connectionGuidanceFor(state: HostConnectionState, message: String): String {
    val normalized = message.lowercase(Locale.US)
    return when (state) {
        HostConnectionState.AuthFailed ->
            "Pair again with a fresh QR invite from the Windows J.A.M. drawer. Tokens can expire, be mistyped, or be replaced by a newer trusted-device record."
        HostConnectionState.PermissionDenied ->
            "The host is reachable, but this device does not have the permission needed for that action. Check trusted-device permissions on the PC."
        HostConnectionState.Revoked ->
            "This device was revoked by the host. Remove the saved host here, then scan a new QR invite if you want to trust it again."
        HostConnectionState.CredentialExpired ->
            "This trusted-device credential expired. Ask the host owner to create a fresh pairing code."
        HostConnectionState.Reconnecting ->
            "The live connection was interrupted. Pixelody is retrying with bounded backoff; keep the phone and PC on the same private network."
        HostConnectionState.NetworkUnavailable ->
            "The phone has no usable route to the host. Reconnect Wi-Fi or the private network and Pixelody will retry automatically."
        HostConnectionState.HostUnavailable ->
            "The saved host is not responding. Confirm the PC is awake and J.A.M. hosting is still running."
        HostConnectionState.Unreachable -> when {
            "firewall" in normalized ->
                "Allow Pixelody on private networks in Windows Firewall, then restart the J.A.M. host and scan a fresh QR code."
            "route" in normalized || "wi-fi" in normalized || "network" in normalized ->
                "Use the same Wi-Fi/private network as the PC, start Private-network hosting on Windows, and scan the latest QR invite."
            else ->
                "Make sure the PC is awake, Pixelody J.A.M. is running, Private-network hosting is enabled for phone use, and the QR invite is current."
        }
        HostConnectionState.Offline ->
            "The host may be asleep, closed, or on a different network. Restart J.A.M. on the PC and reconnect from the QR-first pairing card."
        HostConnectionState.Connecting ->
            "Still trying the advertised host addresses. If this takes too long, create a fresh QR invite from the PC."
        HostConnectionState.Connected ->
            "Connected. If playback or queue state looks stale, use Soft Refresh J.A.M. Devices from Connection Tools."
        HostConnectionState.Disconnected ->
            "Scan a Pixelody QR invite from the Windows J.A.M. drawer to connect this phone."
    }
}

internal val HostConnectionState.label: String
    get() = when (this) {
        HostConnectionState.Disconnected -> "Not connected"
        HostConnectionState.Connecting -> "Connecting"
        HostConnectionState.Connected -> "Connected"
        HostConnectionState.Reconnecting -> "Reconnecting"
        HostConnectionState.Offline -> "Host offline"
        HostConnectionState.Unreachable -> "Can't reach host"
        HostConnectionState.HostUnavailable -> "Host not responding"
        HostConnectionState.NetworkUnavailable -> "No network"
        HostConnectionState.AuthFailed -> "Pairing failed"
        HostConnectionState.CredentialExpired -> "Pairing expired"
        HostConnectionState.PermissionDenied -> "Permission denied"
        HostConnectionState.Revoked -> "Access revoked"
    }

internal val NetworkMode.label: String
    get() = when (this) {
        NetworkMode.Auto -> "Auto"
        NetworkMode.LocalOnly -> "Local only"
        NetworkMode.DirectRemote -> "Direct remote"
        NetworkMode.SelfHostedRelay -> "My relay"
        NetworkMode.ManagedRelayFallback -> "Relay fallback"
    }

internal val HostingVisibility.label: String
    get() = when (this) {
        HostingVisibility.Off -> "Off"
        HostingVisibility.ThisDeviceOnly -> "This device"
        HostingVisibility.LocalNetwork -> "Local network"
        HostingVisibility.RemoteAllowed -> "Remote allowed"
    }

@Composable
internal fun SharingScreen(
    snapshot: LibrarySnapshot?,
    savedHost: SavedHostProfile?,
    liveState: LiveState?,
    connectionState: HostConnectionState,
    baseUrl: String,
    token: String,
    candidateCount: Int,
    networkMode: NetworkMode,
    hostingVisibility: HostingVisibility,
    androidHostStatus: AndroidHostStatus,
    localTrackCount: Int,
    trustedDevices: List<TrustedDevice>,
    trustedDevicesError: String,
    deviceRefreshNotice: String,
    jamSessionNotice: String,
    connectionIssue: String,
    onBaseUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onManualConnect: () -> Unit,
    onNetworkModeChange: (NetworkMode) -> Unit,
    onOpenPair: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenQueue: () -> Unit,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit,
    onRefreshTrustedDevices: () -> Unit,
    onJoinJamSession: () -> Unit,
    onJamQueueAction: (String) -> Unit,
    onJamPlaybackAction: (String) -> Unit,
    onRevokeTrustedDevice: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenHeader(
                title = "Connection Tools",
                subtitle = "Pairing, hosting, trusted devices, and J.A.M. diagnostics live here."
            )
        }
        item {
            SectionCard(
                title = "Connection",
                subtitle = snapshot?.host?.hostName ?: savedHost?.hostName ?: "No active host"
            ) {
                StatusChip(text = connectionState.label)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = snapshot?.host?.baseUrl ?: savedHost?.baseUrl ?: "Paste or scan an invite to connect.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onOpenPair) { Text(if (snapshot == null) "Join Host" else "Pair") }
                    OutlinedButton(onClick = onOpenQueue) { Text("Queue") }
                }
            }
        }
        item {
            SectionCard(
                title = "Manual Connection",
                subtitle = "Fallback controls for split URL/token invites."
            ) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = onBaseUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Host URL") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = onTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Trusted-device token") },
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Smart connect has $candidateCount candidate address${if (candidateCount == 1) "" else "es"}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    enabled = baseUrl.isNotBlank() && token.isNotBlank(),
                    onClick = onManualConnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect Manually")
                }
            }
        }
        item {
            SectionCard(
                title = "Network Mode",
                subtitle = "Auto keeps normal use simple; advanced modes are placeholders for the hybrid network stack."
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NetworkMode.entries) { mode ->
                        FilterChip(
                            selected = networkMode == mode,
                            onClick = { onNetworkModeChange(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when (networkMode) {
                        NetworkMode.Auto -> "Try local routes first, then future safe remote routes when enabled."
                        NetworkMode.LocalOnly -> "Use same-device and local-network routes only."
                        NetworkMode.DirectRemote -> "Future mode for public IPv6 or direct authenticated remote routes."
                        NetworkMode.SelfHostedRelay -> "Future mode for a user-owned blind relay."
                        NetworkMode.ManagedRelayFallback -> "Future mode for optional managed blind relay fallback."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            SectionCard(
                title = "Host This Phone",
                subtitle = "Visibility: ${hostingVisibility.label}"
            ) {
                Text(
                    text = if (androidHostStatus.running) {
                        "${androidHostStatus.trackCount} selected phone track${if (androidHostStatus.trackCount == 1) "" else "s"} at ${androidHostStatus.baseUrl}"
                    } else {
                        "$localTrackCount local phone track${if (localTrackCount == 1) "" else "s"} ready for temporary hosting."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = localTrackCount > 0 && !androidHostStatus.running,
                        onClick = onStartHost
                    ) { Text("Start") }
                    OutlinedButton(
                        enabled = androidHostStatus.running,
                        onClick = onStopHost
                    ) { Text("Stop") }
                    TextButton(onClick = onOpenDevice) { Text("Files") }
                }
                if (androidHostStatus.lastError.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = androidHostStatus.lastError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Permissions",
                subtitle = liveState?.permissions?.joinToString(", ")?.ifBlank { "read-only" } ?: "Waiting for live state."
            ) {
                val host = snapshot?.host
                Text(
                    text = listOfNotNull(
                        host?.capabilities?.let { "stream=${it.canStream}" },
                        host?.capabilities?.let { "remote=${it.canRemoteControl}" },
                        host?.capabilities?.let { "J.A.M.=${it.canJamCoordinate}" },
                        liveState?.let { "visibility=${it.visibility}" }
                    ).joinToString(" / ").ifBlank { "No host capability payload yet." },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            val session = liveState?.jamSession
            SectionCard(
                title = "Single-host J.A.M.",
                subtitle = when {
                    session?.active == true -> "${session.status} / ${session.currentParticipant?.role ?: "not joined"}"
                    liveState?.networkSession?.kind == "jam-single-host" -> "A Windows-hosted session is active."
                    else -> "No explicit session is visible."
                }
            ) {
                Text(
                    text = if (session?.active == true) {
                        val unavailableCount = session.queue.count { item -> item.availability != "available" }
                        "${session.participants.size} participant${if (session.participants.size == 1) "" else "s"} / ${session.queue.size} queue item${if (session.queue.size == 1) "" else "s"} / $unavailableCount unavailable / Windows source only"
                    } else {
                        "Join requests require owner approval. Guest permissions apply only inside this J.A.M. session and do not grant library administration."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                val haptic = LocalHapticFeedback.current
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        haptic.performConfirm()
                        onJoinJamSession()
                    },
                    enabled = connectionState == HostConnectionState.Connected && session?.currentParticipant?.status != "active"
                ) {
                    Text(if (session?.currentParticipant?.status == "active") "Joined J.A.M." else "Request to join J.A.M.")
                }
                session?.currentParticipant?.takeIf { it.status == "active" }?.let { participant ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Role: ${participant.role} / permissions: ${participant.permissions.joinToString(", ").ifBlank { "view only" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            OutlinedButton(onClick = {
                                haptic.performTick()
                                onJamQueueAction("suggest")
                            }) { Text("Suggest track") }
                        }
                        item {
                            OutlinedButton(onClick = {
                                haptic.performTick()
                                onJamQueueAction("add")
                            }) { Text("Add track") }
                        }
                        item {
                            OutlinedButton(onClick = {
                                haptic.performTick()
                                onJamQueueAction("move")
                            }) { Text("Move first") }
                        }
                        item {
                            OutlinedButton(onClick = {
                                haptic.performTick()
                                onJamQueueAction("clear")
                            }) { Text("Clear queue") }
                        }
                        item {
                            OutlinedButton(onClick = {
                                haptic.performConfirm()
                                onJamPlaybackAction("toggle")
                            }) { Text("Play / pause") }
                        }
                    }
                    (session.queue.firstOrNull { item -> item.availability != "available" } ?: session.queue.firstOrNull())?.let { item ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${item.title} / ${item.playbackStatus} / ${item.availability} / ${item.cacheState} / source ${item.sourceDeviceId.take(8)} / by ${item.addedByDeviceId.take(8)} / fallback ${item.fallbackCandidates.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (jamSessionNotice.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(jamSessionNotice, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
                session?.diagnostics?.lastIssue?.takeIf { it.isNotBlank() }?.let { issue ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$issue ${session.diagnostics.recommendedAction}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            SectionCard(
                title = "J.A.M. Device Refresh",
                subtitle = "Ask the host and connected devices to re-sync without clearing saved pairing or local music."
            ) {
                Button(onClick = onRefreshTrustedDevices, modifier = Modifier.fillMaxWidth()) {
                    Text("Soft Refresh J.A.M. Devices")
                }
                if (deviceRefreshNotice.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = deviceRefreshNotice,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (trustedDevicesError.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = trustedDevicesError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (trustedDevices.isEmpty()) {
                        "Owner tokens can also show and revoke trusted devices."
                    } else {
                        "${trustedDevices.size} trusted device${if (trustedDevices.size == 1) "" else "s"} visible to this token."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                trustedDevices.forEach { device ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(device.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${device.status} / ${device.permissions.joinToString(", ")}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (device.status != "revoked") {
                                TextButton(onClick = { onRevokeTrustedDevice(device.id) }) {
                                    Text("Revoke")
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Diagnostics",
                subtitle = if (connectionIssue.isBlank()) "Network and sharing status." else "Latest connection issue and recovery hint."
            ) {
                Text(
                    text = listOf(
                        "connection=${connectionState.name}",
                        "mode=${networkMode.label}",
                        "phoneHost=${hostingVisibility.label}",
                        "liveRevision=${liveState?.revision ?: 0}",
                        "poll=${liveState?.pollAfterMs ?: 0}ms"
                    ).joinToString(" / "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (connectionIssue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = connectionIssue,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = connectionGuidanceFor(connectionState, connectionIssue),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomePairingPanel(
    snapshot: LibrarySnapshot?,
    savedHost: SavedHostProfile?,
    liveState: LiveState?,
    copiedDetails: String,
    qrScannerVisible: Boolean,
    qrScannerError: String,
    isConnecting: Boolean,
    manualInviteVisible: Boolean,
    onManualInviteVisibleChange: (Boolean) -> Unit,
    onCopiedDetailsChange: (String) -> Unit,
    onStartQrScanner: () -> Unit,
    onStopQrScanner: () -> Unit,
    onQrPayloadScanned: (String) -> Unit,
    onQrScannerError: (String) -> Unit,
    onForgetSavedHost: () -> Unit,
    onUseCopiedDetails: () -> Unit,
    onPairFixture: () -> Unit,
    onOpenSharing: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Connect your desktop", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = snapshot?.host?.baseUrl?.takeIf { savedHost != null }
                    ?: savedHost?.let { "Saved host: ${it.hostName}" }
                    ?: "Open Pixelody on your desktop and create a connection invite. Scan its QR code or paste the invite below.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (savedHost != null) 2 else 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !isConnecting && !qrScannerVisible,
                    onClick = onStartQrScanner,
                    modifier = Modifier.weight(1f)
                ) { Text(if (isConnecting) "Connecting" else "Scan QR") }
                if (savedHost != null) OutlinedButton(onClick = onPairFixture, enabled = !isConnecting, modifier = Modifier.weight(1f)) { Text("Reconnect") }
            }
            if (qrScannerVisible) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onStopQrScanner, modifier = Modifier.fillMaxWidth()) { Text("Stop Scan") }
                Spacer(modifier = Modifier.height(10.dp))
                QrScannerPane(onPayloadScanned = onQrPayloadScanned, onError = onQrScannerError)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onManualInviteVisibleChange(!manualInviteVisible) },
                    modifier = Modifier.weight(1f)
                ) { Text(if (manualInviteVisible) "Hide Paste" else "Paste Invite") }

            }
            if (manualInviteVisible) {
                OutlinedTextField(
                    value = copiedDetails,
                    onValueChange = onCopiedDetailsChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Invite") },
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    enabled = copiedDetails.isNotBlank() && !isConnecting,
                    onClick = onUseCopiedDetails,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isConnecting) "Connecting" else "Use Invite") }
            }
            if (savedHost != null) {
                TextButton(onClick = onForgetSavedHost, modifier = Modifier.fillMaxWidth()) { Text("Forget Saved Host") }
            }
            if (liveState != null && savedHost != null) {
                Text(
                    text = "Live ${liveState.revision} / ${liveState.visibility}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (qrScannerError.isNotBlank()) {
                Text(text = qrScannerError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun QrScannerPane(
    onPayloadScanned: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val decoder = remember { QrDecoder() }
    val scanActive = remember { AtomicBoolean(true) }
    val currentOnPayloadScanned by rememberUpdatedState(onPayloadScanned)
    val currentOnError by rememberUpdatedState(onError)
    var boundCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var hasScanned by remember { mutableStateOf(false) }
    var reportedUnreadableQr by remember { mutableStateOf(false) }
    var detectedQrCount by remember { mutableIntStateOf(0) }
    var scannerStatus by remember { mutableStateOf("Looking for a Pixelody QR") }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    DisposableEffect(Unit) {
        onDispose {
            scanActive.set(false)
            boundCameraProvider?.unbindAll()
            executor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { viewContext ->
                val previewView = PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(viewContext)
                cameraProviderFuture.addListener(
                    {
                        if (!scanActive.get()) return@addListener
                        val cameraProvider = runCatching { cameraProviderFuture.get() }
                            .onFailure { onError(it.message ?: "Camera could not start.") }
                            .getOrNull()
                            ?: return@addListener
                        boundCameraProvider = cameraProvider
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        @Suppress("DEPRECATION")
                        val analysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analyzer ->
                                analyzer.setAnalyzer(executor) { imageProxy ->
                                    if (!scanActive.get() || hasScanned) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    val result = try {
                                        runCatching { decoder.decode(imageProxy) }
                                    } finally {
                                        imageProxy.close()
                                    }
                                    mainExecutor.execute {
                                        if (!scanActive.get() || hasScanned) return@execute
                                        if (result.isFailure) {
                                            scannerStatus = "Camera frame could not be read"
                                            currentOnError("Camera frame could not be read. Stop Scan and try again, or paste the desktop invite.")
                                            return@execute
                                        }
                                        val text = result.getOrNull()
                                        val candidates = text?.payloadCandidates().orEmpty()
                                        detectedQrCount = if (text != null) 1 else 0
                                        val payload = candidates.firstOrNull { HostConnectionDetails.fromText(it) != null }.orEmpty()
                                        if (payload.isNotBlank() && !hasScanned) {
                                            hasScanned = true
                                            scannerStatus = "Pixelody QR found"
                                            currentOnPayloadScanned(payload)
                                        } else if (text != null && !reportedUnreadableQr) {
                                            reportedUnreadableQr = true
                                            scannerStatus = "QR detected, reading data"
                                            currentOnError("QR recognized, but it isn't a Pixelody connection invite. Create a fresh code in the Windows J.A.M. drawer.")
                                        } else if (text != null) {
                                            scannerStatus = "QR detected, reading data"
                                        } else {
                                            scannerStatus = "Looking for a Pixelody QR"
                                        }
                                    }
                                }
                            }
                        runCatching {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        }.onFailure {
                            onError(it.message ?: "Camera could not bind.")
                        }
                    },
                    ContextCompat.getMainExecutor(viewContext)
                )
                previewView
            }
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            val margin = 26.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            val color = when {
                hasScanned -> Color(0xFF5DE48B)
                detectedQrCount > 0 -> Color(0xFFFFD166)
                else -> Color.White.copy(alpha = 0.86f)
            }
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(margin, margin),
                size = androidx.compose.ui.geometry.Size(size.width - margin * 2, size.height - margin * 2),
                style = Stroke(width = strokeWidth)
            )
        }
        Text(
            text = scannerStatus,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

internal fun String?.toQrDebugPreview(): String {
    val text = this?.replace("\n", "\\n")?.replace("\r", "\\r").orEmpty()
    if (text.isBlank()) return "<empty>"
    return if (text.length <= 160) text else text.take(157) + "..."
}
