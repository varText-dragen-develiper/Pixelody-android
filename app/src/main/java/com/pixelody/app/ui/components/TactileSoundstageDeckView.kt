package com.pixelody.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.TactileSoundstageCoordinator
import com.pixelody.app.data.model.StemAnalogRoute
import com.pixelody.app.data.model.TactileDeckMode
import com.pixelody.app.data.model.Track

/**
 * TactileSoundstageDeckView: Unified interactive performance deck console
 * combining 4-way Stems, Direct-Drive Turntable, Magnetic Tape, and CRT Phosphor Scope.
 */
@Composable
fun TactileSoundstageDeckView(
    track: Track?,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val routingMatrix by TactileSoundstageCoordinator.routingMatrix.collectAsState()
    val frame by TactileSoundstageCoordinator.soundstageFrame.collectAsState()

    var showRoutingDrawer by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- Header: Title & Close ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.20f))
                            .border(1.dp, Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Sliders,
                            color = Color(0xFFF59E0B),
                            size = 14.dp
                        )
                    }

                    Column {
                        Text(
                            text = "TACTILE SOUNDSTAGE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = routingMatrix.activeDeckMode.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onClose != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performTick()
                                onClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Close,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 16.dp
                        )
                    }
                }
            }

            // --- 5-Mode Tactile Deck Selector ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TactileDeckMode.entries.forEach { mode ->
                    val isSelected = routingMatrix.activeDeckMode == mode
                    Surface(
                        onClick = {
                            haptic.performTick()
                            TactileSoundstageCoordinator.setDeckMode(mode)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.10f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mode.tag,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // --- Performance Macro Pills ---
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val macros = listOf(
                    "Acapella" to "ACAPELLA",
                    "Instrumental" to "INSTRUMENTAL",
                    "TapeWarmth" to "TAPE WARMTH",
                    "LoFiVinyl" to "LO-FI VINYL",
                    "Reset" to "RESET FLAT"
                )

                items(macros) { (key, label) ->
                    val isActive = frame.activeMacroName == label
                    Surface(
                        onClick = {
                            haptic.performConfirm()
                            TactileSoundstageCoordinator.applyPerformanceMacro(key)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isActive) Color(0xFF4ADE80).copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) Color(0xFF4ADE80) else Color.Transparent
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // --- Mode Specific Display & Controls ---
            when (routingMatrix.activeDeckMode) {
                TactileDeckMode.Stems, TactileDeckMode.MasterBlend -> {
                    StemFadersControlRack(frame = frame, routingMatrix = routingMatrix)
                }
                TactileDeckMode.Turntable -> {
                    TurntablePerformanceConsole(frame = frame)
                }
                TactileDeckMode.Cassette -> {
                    CassettePerformanceConsole(frame = frame, routingMatrix = routingMatrix)
                }
                TactileDeckMode.PhosphorLab -> {
                    PhosphorScopeConsole(frame = frame)
                }
            }

            // --- Routing Matrix Drawer Toggle ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performTick()
                        showRoutingDrawer = !showRoutingDrawer
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (showRoutingDrawer) "Hide Analog Routing Matrix" else "Inspect Stem Analog Routing Matrix",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- Routing Matrix Matrix Details ---
            AnimatedVisibility(visible = showRoutingDrawer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "STEM ANALOG INSERT MATRIX",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color(0xFFF59E0B)
                    )

                    listOf(
                        "Vocals" to routingMatrix.vocalRoute,
                        "Drums" to routingMatrix.drumsRoute,
                        "Bass" to routingMatrix.bassRoute,
                        "Other" to routingMatrix.otherRoute
                    ).forEach { (stem, currentRoute) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stem,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StemAnalogRoute.entries.forEach { route ->
                                    val isSelected = currentRoute == route
                                    Surface(
                                        onClick = {
                                            haptic.performTick()
                                            TactileSoundstageCoordinator.setStemRoute(stem, route)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.30f)
                                        else Color.White.copy(alpha = 0.06f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFFF59E0B) else Color.Transparent
                                        )
                                    ) {
                                        Text(
                                            text = route.displayName.take(6),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
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
private fun StemFadersControlRack(
    frame: com.pixelody.app.data.model.TactileSoundstageFrame,
    routingMatrix: com.pixelody.app.data.model.SoundstageRoutingMatrix
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "Vocals" to frame.vocalsGain,
            "Drums" to frame.drumsGain,
            "Bass" to frame.bassGain,
            "Other" to frame.otherGain
        ).forEach { (stem, gain) ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stem.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Gain Level Pill
                    Text(
                        text = "${(gain * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gain > 0f) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Vertical Slider Simulation
                    Slider(
                        value = gain,
                        onValueChange = {
                            haptic.performTick()
                            TactileSoundstageCoordinator.setStemGain(stem, it)
                        },
                        valueRange = 0f..1.25f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF59E0B),
                            activeTrackColor = Color(0xFFF59E0B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 1-Tap Mute / Solo Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            onClick = {
                                haptic.performTick()
                                val newGain = if (gain > 0f) 0f else 1.0f
                                TactileSoundstageCoordinator.setStemGain(stem, newGain)
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = if (gain == 0f) Color(0xFFEF4444).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = "M",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gain == 0f) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                haptic.performConfirm()
                                TactileSoundstageCoordinator.applyPerformanceMacro(
                                    if (stem == "Vocals") "Acapella" else "Instrumental"
                                )
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = "S",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TurntablePerformanceConsole(frame: com.pixelody.app.data.model.TactileSoundstageFrame) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DIRECT-DRIVE VINYL PLATTER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B)
                )
                Text(
                    text = "${"%.1f".format(frame.turntableRpm)} RPM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ADE80)
                )
            }

            Text(
                text = "Touch to engage slipmat friction, spinback deceleration, and needle drop dynamics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CassettePerformanceConsole(
    frame: com.pixelody.app.data.model.TactileSoundstageFrame,
    routingMatrix: com.pixelody.app.data.model.SoundstageRoutingMatrix
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MAGNETIC TAPE BIAS & SATURATION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B)
                )
                Text(
                    text = "Drive: ${(routingMatrix.analogSaturationDrive * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
            }

            Slider(
                value = routingMatrix.analogSaturationDrive,
                onValueChange = {
                    haptic.performTick()
                    TactileSoundstageCoordinator.setSaturationDrive(it)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFF59E0B),
                    activeTrackColor = Color(0xFFF59E0B)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PhosphorScopeConsole(frame: com.pixelody.app.data.model.TactileSoundstageFrame) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRT PHOSPHOR DUAL-TRACE SCOPE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981)
                )
                Text(
                    text = "Lissajous Vector Lock",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }

            Text(
                text = "Green P31 phosphor persistence with real-time stereo phase correlation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
