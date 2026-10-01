package com.pixelody.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.Track

enum class SourceScope(
    val id: String,
    val label: String,
    val iconEmoji: String = ""
) {
    All("all", "All Sources"),
    LocalPhone("phone", "Phone"),
    DesktopHost("host", "Desktop"),
    JamMesh("jam", "J.A.M.");

    fun toGlyphType(): TransportGlyphType = when (this) {
        All -> TransportGlyphType.OmniSource
        LocalPhone -> TransportGlyphType.PhoneDevice
        DesktopHost -> TransportGlyphType.DesktopHost
        JamMesh -> TransportGlyphType.MeshNetwork
    }

    companion object {
        fun fromId(id: String?): SourceScope =
            entries.firstOrNull { it.id == id } ?: All
    }
}

/**
 * Pure function to filter tracks according to the active source lens.
 */
fun filterTracksBySource(
    tracks: List<Track>,
    scope: SourceScope,
    localTrackIds: Set<String>,
    jamTrackIds: Set<String> = emptySet()
): List<Track> {
    return when (scope) {
        SourceScope.All -> tracks
        SourceScope.LocalPhone -> tracks.filter { it.id in localTrackIds || it.streamUrl.startsWith("content://") || it.streamUrl.startsWith("file://") }
        SourceScope.DesktopHost -> tracks.filterNot { it.id in localTrackIds || it.streamUrl.startsWith("content://") || it.streamUrl.startsWith("file://") }
        SourceScope.JamMesh -> tracks.filter { it.id in jamTrackIds }
    }
}

/**
 * Ergonomic capsule pill bar providing 1-tap switching across Local Phone,
 * Desktop Host, and J.A.M. Mesh listening lenses with vector iconography,
 * smart direct activation, and long-press contextual bubble menu triggers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TriSourcePivotBar(
    selectedScope: SourceScope,
    onSelectScope: (SourceScope) -> Unit,
    modifier: Modifier = Modifier,
    allCount: Int = 0,
    phoneCount: Int = 0,
    desktopCount: Int = 0,
    jamCount: Int = 0,
    isHostConnected: Boolean = false,
    localCount: Int = 0,
    hostConnected: Boolean = false,
    hostCount: Int = 0,
    jamActive: Boolean = false,
    onLongClickScope: (SourceScope) -> Unit = {},
    onConnectDesktop: () -> Unit = {},
    onStartJam: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val effPhoneCount = if (phoneCount > 0) phoneCount else localCount
    val effDesktopCount = if (desktopCount > 0) desktopCount else hostCount
    val effHostConnected = isHostConnected || hostConnected

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SourceScope.entries.forEach { scope ->
            val isSelected = scope == selectedScope

            val badgeText = when (scope) {
                SourceScope.All -> if (allCount > 0) "$allCount" else "${effPhoneCount + effDesktopCount}"
                SourceScope.LocalPhone -> "$effPhoneCount"
                SourceScope.DesktopHost -> if (effHostConnected) "$effDesktopCount" else "Offline"
                SourceScope.JamMesh -> if (jamCount > 0) "$jamCount" else if (jamActive) "Active" else "Mesh"
            }

            val statusDotColor = when (scope) {
                SourceScope.All -> MaterialTheme.colorScheme.primary
                SourceScope.LocalPhone -> Color(0xFF4ADE80) // Emerald Green
                SourceScope.DesktopHost -> if (effHostConnected) Color(0xFF38BDF8) else Color(0xFF64748B)
                SourceScope.JamMesh -> if (jamCount > 0 || jamActive) Color(0xFFA855F7) else Color(0xFF64748B)
            }

            val surfaceColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                label = "pivot_bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "pivot_content"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                label = "pivot_border"
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .combinedClickable(
                        onClickLabel = "Select ${scope.label}",
                        role = Role.Tab,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectScope(scope)
                            // Smart 1-tap activation
                            if (scope == SourceScope.DesktopHost && !effHostConnected) {
                                onConnectDesktop()
                            } else if (scope == SourceScope.JamMesh && !jamActive && jamCount == 0) {
                                onStartJam()
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClickScope(scope)
                        }
                    ),
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor,
                contentColor = contentColor,
                border = BorderStroke(1.dp, borderColor),
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelodyTransportGlyph(
                        glyph = scope.toGlyphType(),
                        color = contentColor,
                        sizeDp = 14
                    )

                    Text(
                        text = scope.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    // Status / Count Badge
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
