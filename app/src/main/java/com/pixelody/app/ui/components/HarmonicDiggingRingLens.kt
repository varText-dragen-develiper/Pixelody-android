package com.pixelody.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.Track
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Filter mode for Camelot Harmonic Digging.
 */
enum class HarmonicFilterMode(val label: String) {
    StrictAdjacent("HARMONIC (±1)"),
    EnergyBoost("ENERGY BOOST (+2)"),
    SunsetDrift("SUNSET DRIFT (-2)"),
    AllCompatible("ALL COMPATIBLE")
}

/**
 * Computes the harmonic relationship between a base key and a target key code.
 */
fun computeHarmonicRelation(baseKey: CamelotKey, targetKeyCode: String): HarmonicRelation? {
    val target = CamelotKey.fromCode(targetKeyCode) ?: return null
    return HarmonicKeyEngine.calculateHarmonicRelation(baseKey, target)
}

/**
 * Returns the set of compatible Camelot key codes for a given base key and filter mode.
 */
fun getCompatibleCamelotKeyCodes(baseKey: CamelotKey, mode: HarmonicFilterMode): Set<String> {
    val exact = baseKey.code
    val relative = CamelotKey.fromNumberAndMode(baseKey.number, if (baseKey.mode == CamelotMode.Minor) CamelotMode.Major else CamelotMode.Minor).code
    val plus1 = CamelotKey.fromNumberAndMode(baseKey.number + 1, baseKey.mode).code
    val minus1 = CamelotKey.fromNumberAndMode(baseKey.number - 1, baseKey.mode).code
    val plus2 = CamelotKey.fromNumberAndMode(baseKey.number + 2, baseKey.mode).code
    val minus2 = CamelotKey.fromNumberAndMode(baseKey.number - 2, baseKey.mode).code

    return when (mode) {
        HarmonicFilterMode.StrictAdjacent -> setOf(exact, relative, plus1, minus1)
        HarmonicFilterMode.EnergyBoost -> setOf(exact, plus1, plus2)
        HarmonicFilterMode.SunsetDrift -> setOf(exact, minus1, minus2)
        HarmonicFilterMode.AllCompatible -> setOf(exact, relative, plus1, minus1, plus2, minus2)
    }
}

/**
 * HarmonicDiggingRingLens: Interactive Camelot Harmonic Key Ring Lens for Library and Search.
 * Allows DJs and audiophiles to 1-tap filter tracks by harmonic compatibility with the current playing track or selected key.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HarmonicDiggingRingLens(
    currentTrack: Track?,
    selectedBaseKey: CamelotKey?,
    filterMode: HarmonicFilterMode,
    onSelectBaseKey: (CamelotKey?) -> Unit,
    onSelectFilterMode: (HarmonicFilterMode) -> Unit,
    modifier: Modifier = Modifier,
    onShowDoc: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    val trackKey = currentTrack?.let { HarmonicKeyEngine.estimateTrackTelemetry(it).key } ?: CamelotKey.K8A
    val activeKey = selectedBaseKey ?: trackKey

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (selectedBaseKey != null) Color(0xFFA78BFA).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .then(
                if (onShowDoc != null && !isExpanded) {
                    Modifier.combinedClickable(
                        onClick = {
                            haptic.performTick()
                            isExpanded = true
                        },
                        onLongClick = {
                            haptic.performTick()
                            onShowDoc("harmonic_lens")
                        }
                    )
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Toggle Lens + Active Key Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (selectedBaseKey != null) Color(0xFFA78BFA) else Color(0xFF71717A))
                    )
                    Text(
                        text = if (selectedBaseKey != null) "HARMONIC LENS: ${activeKey.fullTitle}" else "HARMONIC CAMELOT DIGGING LENS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selectedBaseKey != null) Color(0xFFA78BFA) else MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedBaseKey != null) {
                        Surface(
                            onClick = {
                                haptic.performTick()
                                onSelectBaseKey(null)
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "RESET",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            haptic.performTick()
                            isExpanded = !isExpanded
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = if (isExpanded) Color(0xFFA78BFA).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (isExpanded) Color(0xFFA78BFA) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = if (isExpanded) "CLOSE WHEEL" else "OPEN WHEEL",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpanded) Color(0xFFA78BFA) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (onShowDoc != null) {
                        CabinetDocButton(
                            onClick = { onShowDoc("harmonic_lens") },
                            contentDescription = "Harmonic Lens documentation"
                        )
                    }
                }
            }

            // Expanded Camelot Harmonic 12-Hour Key Ring & Mode Strip
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Action: Match Playing Track
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                haptic.performConfirm()
                                onSelectBaseKey(trackKey)
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = "MATCH NOW PLAYING (${trackKey.code})",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }

                        // Harmonic Filter Strategy Mode Selector
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            HarmonicFilterMode.values().forEach { mode ->
                                val isModeSel = filterMode == mode
                                Surface(
                                    onClick = {
                                        haptic.performTick()
                                        onSelectFilterMode(mode)
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isModeSel) Color(0xFFA78BFA).copy(alpha = 0.25f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isModeSel) Color(0xFFA78BFA) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = mode.label,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        fontWeight = if (isModeSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isModeSel) Color(0xFFA78BFA) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 12-Position Camelot Key Horizontal Digging Strip (Minor A + Major B)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "SELECT HARMONIC BASE KEY",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Minor Keys (A)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(CamelotKey.values().filter { it.mode == CamelotMode.Minor }, key = { it.code }) { k ->
                                val isSelected = selectedBaseKey == k
                                val isPlayingKey = trackKey == k
                                Surface(
                                    onClick = {
                                        haptic.performConfirm()
                                        onSelectBaseKey(k)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFFA78BFA).copy(alpha = 0.35f) else if (isPlayingKey) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFA78BFA) else if (isPlayingKey) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = k.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFFA78BFA) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = k.musicalKey.take(6),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 7.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Major Keys (B)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(CamelotKey.values().filter { it.mode == CamelotMode.Major }, key = { it.code }) { k ->
                                val isSelected = selectedBaseKey == k
                                val isPlayingKey = trackKey == k
                                Surface(
                                    onClick = {
                                        haptic.performConfirm()
                                        onSelectBaseKey(k)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.35f) else if (isPlayingKey) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF38BDF8) else if (isPlayingKey) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = k.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = k.musicalKey.take(6),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 7.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * Track list harmonic affinity badge.
 */
@Composable
fun TrackHarmonicBadge(
    trackKey: String,
    baseKey: CamelotKey,
    modifier: Modifier = Modifier
) {
    val relation = computeHarmonicRelation(baseKey, trackKey) ?: return
    if (relation == HarmonicRelation.DissonantClash) return

    val badgeColor = Color(relation.badgeColorHex)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = badgeColor.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = "${trackKey} • ${relation.title}",
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor
        )
    }
}
