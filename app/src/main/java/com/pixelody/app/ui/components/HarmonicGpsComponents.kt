package com.pixelody.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.pixelody.app.core.playback.HarmonicGpsEngine
import com.pixelody.app.core.playback.HarmonicGpsRoute
import com.pixelody.app.core.playback.HarmonicGpsStep
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.Track

/**
 * Interactive Harmonic GPS Navigation Card displaying a multi-track stepping-stone journey.
 */
@Composable
fun HarmonicGpsRouteCard(
    route: HarmonicGpsRoute,
    onApplyRouteToQueue: (List<Track>) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.40f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Origin -> Destination Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.20f))
                            .border(1.dp, Color(0xFF38BDF8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Sparkle,
                            color = Color(0xFF38BDF8),
                            size = 14.dp
                        )
                    }

                    Column {
                        Text(
                            text = "HARMONIC GPS ROUTE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "${route.originTrack.title} ➔ ${route.destinationTrack.title}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
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

            Spacer(modifier = Modifier.height(12.dp))

            // Waypoints Quick Ribbon
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(route.waypoints) { waypoint ->
                    val tel = remember(waypoint.id) { HarmonicKeyEngine.estimateTrackTelemetry(waypoint) }
                    val isOrigin = waypoint.id == route.originTrack.id
                    val isDest = waypoint.id == route.destinationTrack.id

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDest) Color(0xFF38BDF8).copy(alpha = 0.25f)
                        else if (isOrigin) Color(0xFF4ADE80).copy(alpha = 0.20f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        border = BorderStroke(
                            1.dp,
                            if (isDest) Color(0xFF38BDF8)
                            else if (isOrigin) Color(0xFF4ADE80)
                            else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tel.key.harmonicColor.copy(alpha = 0.25f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tel.key.code,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tel.key.harmonicColor
                                )
                            }
                            Text(
                                text = waypoint.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(90.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HarmonicGpsMetricPill(
                    label = "HOPS",
                    value = "${route.totalHops} tracks",
                    color = Color(0xFFA78BFA)
                )
                HarmonicGpsMetricPill(
                    label = "COHESION",
                    value = "${(route.averageHarmonicCohesionScore * 100).toInt()}%",
                    color = Color(0xFF4ADE80)
                )
                HarmonicGpsMetricPill(
                    label = "STABILITY",
                    value = "${(route.tempoProgressionStability * 100).toInt()}%",
                    color = Color(0xFF38BDF8)
                )
            }

            // Expandable Step-by-Step Breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    route.steps.forEach { step ->
                        HarmonicGpsStepRow(step = step)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performTick()
                        isExpanded = !isExpanded
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = if (isExpanded) "Hide Steps" else "Inspect Steps",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        haptic.performConfirm()
                        onApplyRouteToQueue(route.waypoints)
                    },
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.PlayNext,
                            color = Color.Black,
                            size = 14.dp
                        )
                        Text(
                            text = "Apply Journey to Queue",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HarmonicGpsStepRow(
    step: HarmonicGpsStep,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.50f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${step.stepIndex + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = step.fromKey.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = step.fromKey.harmonicColor
                    )
                    Text(
                        text = "➔",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = step.toKey.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = step.toKey.harmonicColor
                    )
                    Text(
                        text = "• ${step.modulationType}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(step.relation.badgeColorHex)
                    )
                }

                Text(
                    text = "${step.fromTrack.title} ➔ ${step.toTrack.title}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }

            // Transition curve badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Text(
                    text = step.recommendedCurve.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun HarmonicGpsMetricPill(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = color
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Modal sheet allowing the user to select any song as the destination and plot the Harmonic GPS route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarmonicGpsDestinationSheet(
    originTrack: Track,
    libraryPool: List<Track>,
    onDismiss: () -> Unit,
    onApplyRouteToQueue: (List<Track>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedDestination by remember { mutableStateOf<Track?>(null) }
    var activeRoute by remember { mutableStateOf<HarmonicGpsRoute?>(null) }

    val filteredPool = remember(searchQuery, libraryPool) {
        val pool = libraryPool.filterNot { it.id == originTrack.id || it.missing }
        if (searchQuery.isBlank()) pool else pool.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "HARMONIC GPS NAVIGATOR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Plot a 3-5 track harmonic journey from \"${originTrack.title}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performTick()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.Close,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Calculated Route Card Display
            activeRoute?.let { route ->
                HarmonicGpsRouteCard(
                    route = route,
                    onApplyRouteToQueue = { tracks ->
                        onApplyRouteToQueue(tracks)
                        onDismiss()
                    },
                    onClose = {
                        activeRoute = null
                        selectedDestination = null
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (activeRoute == null) {
                // Search Bar for picking destination
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search destination song or artist...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.20f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "SELECT DESTINATION SONG (${filteredPool.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.65f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredPool) { candidate ->
                        val tel = remember(candidate.id) { HarmonicKeyEngine.estimateTrackTelemetry(candidate) }
                        val originTel = remember(originTrack.id) { HarmonicKeyEngine.estimateTrackTelemetry(originTrack) }
                        val relation = remember(candidate.id) { HarmonicKeyEngine.calculateHarmonicRelation(originTel.key, tel.key) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performConfirm()
                                    selectedDestination = candidate
                                    activeRoute = HarmonicGpsEngine.findHarmonicJourney(
                                        origin = originTrack,
                                        destination = candidate,
                                        pool = libraryPool,
                                        maxHops = 5
                                    )
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(tel.key.harmonicColor.copy(alpha = 0.20f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tel.key.code,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tel.key.harmonicColor
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = candidate.artist.ifBlank { "Unknown Artist" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = relation.title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(relation.badgeColorHex)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
