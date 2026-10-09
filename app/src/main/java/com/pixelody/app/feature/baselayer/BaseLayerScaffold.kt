package com.pixelody.app.feature.baselayer

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.pixelody.app.ui.components.LocalPagePosition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.pixelody.app.ui.components.PixelodyNavGlyph
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.navigation.PixelodyTab
import com.pixelody.app.ui.navigation.PixelodyStateTags
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import com.pixelody.app.ui.theme.CartridgeQuestPalette
import com.pixelody.app.ui.theme.ObsidianGlassPalette
import com.pixelody.app.ui.theme.LoFiCafePalette
import com.pixelody.app.ui.theme.BulkheadTerminalPalette
import com.pixelody.app.ui.theme.ObsessionPalette
import com.pixelody.app.ui.theme.ChamferedPlate
import com.pixelody.app.ui.theme.PlateCorner

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.pixelody.app.ui.navigation.PixelodyAdaptivePolicy
import com.pixelody.app.ui.navigation.PixelodyPaneLayout

/**
 * The five bands, from the base layer standard. Every destination in
 * the base layer uses this structure.
 *
 * In Compact (portrait phone) mode, it presents the 5 standard vertical bands.
 * In Rail (tablet / wide window / unfolded) mode, it moves destinations into a leading
 * vertical rail and optionally splits the content pane with a listening side panel.
 */
object BaseLayerBands {
    val Identity: Dp = 72.dp
    val SurfaceActions: Dp = 64.dp
    val ListeningSlot: Dp = 68.dp
    val Destinations: Dp = 64.dp
    val NavigationRailWidth: Dp = 80.dp
}

@Composable
fun BaseLayerScaffold(
    listeningSlot: @Composable () -> Unit,
    destinations: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    identity: (@Composable RowScope.() -> Unit)? = null,
    surfaceActions: (@Composable RowScope.() -> Unit)? = null,
    paneLayoutOverride: PixelodyPaneLayout? = null,
    sidePane: (@Composable () -> Unit)? = null,
    railDestinations: (@Composable ColumnScope.() -> Unit)? = null,
    contentModifier: Modifier = Modifier,
    backdropOpacity: Float = 1f,
    selectedDestination: BaseDestination = BaseDestination.Home,
    content: @Composable () -> Unit
) {
    // Lives above destination content, so disposing a page cannot reset its marker animation.
    val pagePosition = animateFloatAsState(
        targetValue = when (selectedDestination) { BaseDestination.Home -> 0f; BaseDestination.Search -> 1f; BaseDestination.Library -> 2f },
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "Page position"
    )
    val listeningHeight = BaseLayerBands.ListeningSlot + (40f * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)).dp
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val paneLayout = paneLayoutOverride ?: PixelodyAdaptivePolicy.forWindow(
            widthDp = maxWidth.value.toInt(),
            heightDp = maxHeight.value.toInt()
        )

        if (paneLayout.usesNavigationRail) {
            // Tablet / landscape / wide window layout with leading Navigation Rail
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Leading Navigation Rail
                Column(
                    modifier = Modifier
                        .width(BaseLayerBands.NavigationRailWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    if (railDestinations != null) {
                        railDestinations()
                    } else {
                        // Fallback: render each destination in rail items
                        BaseDestination.values().forEach { destination ->
                            BaseDestinationRailItem(
                                destination = destination,
                                selected = false, // driven by custom railDestinations if provided
                                onSelect = {}
                            )
                        }
                    }
                }

                VerticalBandRule()

                // Main Content Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (identity != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(BaseLayerBands.Identity)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            content = identity
                        )
                        BandRule()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .then(contentModifier)
                    ) {
                        CompositionLocalProvider(LocalPagePosition provides pagePosition) { content() }
                    }

                    if (surfaceActions != null) {
                        BandRule()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(BaseLayerBands.SurfaceActions)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            content = surfaceActions
                        )
                    }

                    if (!paneLayout.usesListeningSidePanel && sidePane == null) {
                        BandRule()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(listeningHeight)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                        ) {
                            listeningSlot()
                        }
                    }
                }

                // Optional side listening panel for roomy windows
                if (paneLayout.usesListeningSidePanel || sidePane != null) {
                    VerticalBandRule()
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    ) {
                        sidePane?.invoke() ?: listeningSlot()
                    }
                }
            }
        } else {
            // Standard 5-band Compact (Phone) Scaffold
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                if (identity != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BaseLayerBands.Identity)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        content = identity
                    )
                    BandRule()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(contentModifier)
                ) {
                    CompositionLocalProvider(LocalPagePosition provides pagePosition) { content() }
                    if (identity == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                if (surfaceActions != null) {
                    BandRule()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BaseLayerBands.SurfaceActions)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        content = surfaceActions
                    )
                }

                // One anchored dock shares a scrim between transport and navigation.
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.50f + 0.25f * backdropOpacity),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.68f + 0.18f * backdropOpacity)
                ))).navigationBarsPadding()) {
                    BandRule()
                    Box(Modifier.fillMaxWidth().height(listeningHeight)) { listeningSlot() }
                    BandRule()
                    Row(Modifier.fillMaxWidth().height(BaseLayerBands.Destinations), content = destinations)
                }
            }
        }
    }
}

/**
 * What the listening slot is showing. Three states, and the whole point of naming
 * them as one type is that they share one geometry — H9. The slot is
 * [BaseLayerBands.ListeningSlot] tall whether it is playing, offering to resume, or
 * empty, so nothing above it ever moves.
 *
 * [Resume] is what makes J01 attemptable on a cold launch. Before the slot was
 * permanent there was simply nothing on screen to resume from, and the journey could
 * not be started rather than merely being slow.
 */
sealed class ListeningSlotState {
    data class Occupied(
        val title: String,
        val subtitle: String,
        val badge: String,
        val isPlaying: Boolean
    ) : ListeningSlotState()

    data class Resume(
        val title: String,
        val subtitle: String,
        val badge: String
    ) : ListeningSlotState()

    object Silent : ListeningSlotState()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListeningSlot(
    state: ListeningSlotState,
    onOpenPlayer: () -> Unit,
    onSessionTray: () -> Unit,
    modifier: Modifier = Modifier,
    transport: @Composable RowScope.() -> Unit = {},
    embeddedInDock: Boolean = false
) {
    val openable = state !is ListeningSlotState.Silent
    val clickLabel = when (state) {
        is ListeningSlotState.Occupied -> "Open the player"
        is ListeningSlotState.Resume -> "Open the player"
        ListeningSlotState.Silent -> null
    }

    val theme = LocalPixelodyThemeVariant.current
    val slotShape = when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> RoundedCornerShape(4.dp)
        PixelodyMobileTheme.ObsidianGlass -> RoundedCornerShape(12.dp)
        PixelodyMobileTheme.LoFiCafe -> RoundedCornerShape(6.dp)
        PixelodyMobileTheme.BulkheadTerminal -> RoundedCornerShape(2.dp)
        PixelodyMobileTheme.Obsession -> ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.12f)
        else -> RoundedCornerShape(12.dp)
    }
    val slotBorder = when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> BorderStroke(1.dp, CartridgeQuestPalette.PlasticHighlight.copy(alpha = 0.7f))
        PixelodyMobileTheme.ObsidianGlass -> BorderStroke(1.dp, ObsidianGlassPalette.GlassBorder)
        PixelodyMobileTheme.LoFiCafe -> BorderStroke(1.dp, LoFiCafePalette.PaperTrace)
        PixelodyMobileTheme.BulkheadTerminal -> BorderStroke(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.5f))
        PixelodyMobileTheme.Obsession -> BorderStroke(1.dp, ObsessionPalette.Fracture)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
    }
    val slotColor = when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestPalette.RecessedBay
        PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassPalette.GlassPanel
        PixelodyMobileTheme.LoFiCafe -> LoFiCafePalette.Walnut
        PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalPalette.Ground
        PixelodyMobileTheme.Obsession -> ObsessionPalette.Panel
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
    }
    val buttonColor = when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestPalette.ActionViolet
        PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassPalette.SmokedGraphite
        PixelodyMobileTheme.LoFiCafe -> LoFiCafePalette.PaperSoft
        PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalPalette.Panel
        PixelodyMobileTheme.Obsession -> ObsessionPalette.Confirmed
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val glyphColor = when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestPalette.EditorialBlack
        PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassPalette.PrismCyan
        PixelodyMobileTheme.LoFiCafe -> LoFiCafePalette.PaperInk
        PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalPalette.MintPhosphor
        PixelodyMobileTheme.Obsession -> ObsessionPalette.Ink
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(PixelodyStateTags.LISTENING_SLOT)
            .combinedClickable(
                enabled = true,
                onClickLabel = clickLabel,
                onLongClickLabel = "Settings for this listening session",
                role = Role.Button,
                onLongClick = onSessionTray,
                onClick = { if (openable) onOpenPlayer() }
            ),
        shape = slotShape,
        color = if (embeddedInDock) Color.Transparent else slotColor,
        border = if (embeddedInDock) null else slotBorder,
        tonalElevation = if (embeddedInDock) 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = when (theme) {
                    PixelodyMobileTheme.CartridgeQuest -> RoundedCornerShape(2.dp)
                    PixelodyMobileTheme.BulkheadTerminal -> RoundedCornerShape(1.dp)
                    PixelodyMobileTheme.Obsession -> ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.2f)
                    else -> RoundedCornerShape(10.dp)
                },
                color = buttonColor,
                contentColor = glyphColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixelodyTransportGlyph(
                        glyph = if (state is ListeningSlotState.Occupied && state.isPlaying) TransportGlyphType.Pause else TransportGlyphType.Play,
                        color = glyphColor,
                        size = 20.dp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                when (state) {
                    is ListeningSlotState.Occupied -> {
                        SlotTitle(state.title)
                        SlotSubtitle(state.subtitle)
                    }
                    is ListeningSlotState.Resume -> {
                        SlotTitle(state.title)
                        SlotSubtitle(state.subtitle)
                    }
                    ListeningSlotState.Silent -> SlotSubtitle("Pick something to play")
                }
            }

            transport()
        }
    }
}

@Composable
private fun BandRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))
    )
}

@Composable
private fun VerticalBandRule() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))
    )
}

@Composable
private fun SlotTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SlotSubtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun RowScope.BaseDestinationTab(
    destination: BaseDestination,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tab = when (destination) {
        BaseDestination.Home -> PixelodyTab.Home
        BaseDestination.Search -> PixelodyTab.Search
        BaseDestination.Library -> PixelodyTab.Library
    }
    val theme = LocalPixelodyThemeVariant.current
    val tabShape = when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> RoundedCornerShape(4.dp)
        PixelodyMobileTheme.ObsidianGlass -> RoundedCornerShape(12.dp)
        PixelodyMobileTheme.LoFiCafe -> RoundedCornerShape(6.dp)
        PixelodyMobileTheme.BulkheadTerminal -> RoundedCornerShape(2.dp)
        PixelodyMobileTheme.Obsession -> ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.18f)
        else -> RoundedCornerShape(14.dp)
    }
    val selectedColor = when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestPalette.ActionViolet.copy(alpha = 0.22f)
        PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassPalette.GlassBorder.copy(alpha = 0.45f)
        PixelodyMobileTheme.LoFiCafe -> LoFiCafePalette.Amber.copy(alpha = 0.22f)
        PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.18f)
        PixelodyMobileTheme.Obsession -> ObsessionPalette.Confirmed.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    }


    Surface(
        modifier = modifier.weight(1f).fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
            .testTag(destination.navigationTestTag),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(width = 64.dp, height = 32.dp)
                .background(if (selected) selectedColor else Color.Transparent, tabShape),
                contentAlignment = Alignment.Center) {
                PixelodyNavGlyph(tab = tab, selected = selected)
            }
            Spacer(Modifier.height(4.dp))
            Text(destination.label, style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ColumnScope.BaseDestinationRailItem(
    destination: BaseDestination,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tab = when (destination) {
        BaseDestination.Home -> PixelodyTab.Home
        BaseDestination.Search -> PixelodyTab.Search
        BaseDestination.Library -> PixelodyTab.Library
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onSelect
            )
            .testTag(destination.navigationTestTag),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PixelodyNavGlyph(tab = tab, selected = selected)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

