package com.pixelody.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.pixelody.app.ui.theme.ChamferedPlate
import com.pixelody.app.ui.theme.PlateCorner
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import com.pixelody.app.ui.theme.CartridgeQuestPalette
import com.pixelody.app.ui.theme.LoFiCafePalette
import com.pixelody.app.ui.theme.BulkheadTerminalPalette
import com.pixelody.app.ui.theme.ObsessionPalette
import com.pixelody.app.ui.theme.ObsidianGlassPalette
import com.pixelody.app.data.model.CRATE_FACE_COUNT
import com.pixelody.app.data.model.Crate
import com.pixelody.app.data.model.CrateSlot
import com.pixelody.app.ui.navigation.PixelodyStateTags

/**
 * What a slot looks like once the library has been asked about it. Resolution lives
 * outside these composables because a crate stores addresses, not copies — the name
 * on slot 3 comes from the playlist, so a rename shows up here without the crate
 * being touched, and a playlist that went away resolves to unavailable rather than
 * vanishing and letting the next thing added take its position.
 */
data class CrateSlotFace(
    val label: String,
    val badge: String,
    val kindMark: String,
    val available: Boolean = true
)

/**
 * The closed crate on Home.
 *
 * The preview is the first four addresses in order, holes included, because showing
 * the first four *occupied* slots would make the face change shape whenever a hole
 * higher up was filled — and the face is the recognition cue, so it has to be the
 * one thing that never moves.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrateFace(
    crate: Crate,
    resolve: (CrateSlot) -> CrateSlotFace?,
    onOpen: () -> Unit,
    onActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val occupied = crate.occupiedCount
    val countLabel = if (occupied == 1) "1 in here" else "$occupied in here"

    val theme = LocalPixelodyThemeVariant.current
    val crateShape = when (theme) {
        PixelodyMobileTheme.Obsession -> ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.16f)
        PixelodyMobileTheme.CartridgeQuest -> RoundedCornerShape(4.dp)
        PixelodyMobileTheme.ObsidianGlass -> RoundedCornerShape(12.dp)
        PixelodyMobileTheme.LoFiCafe -> RoundedCornerShape(8.dp)
        PixelodyMobileTheme.BulkheadTerminal -> RoundedCornerShape(2.dp)
        PixelodyMobileTheme.Studio -> RoundedCornerShape(6.dp)
    }

    val containerColor = when (theme) {
        PixelodyMobileTheme.Obsession -> ObsessionPalette.Panel
        PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestPalette.ChassisGray
        PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassPalette.GlassPanel
        PixelodyMobileTheme.LoFiCafe -> LoFiCafePalette.Walnut
        PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalPalette.Panel
        PixelodyMobileTheme.Studio -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val crateBorder = when (theme) {
        PixelodyMobileTheme.Obsession -> BorderStroke(1.dp, ObsessionPalette.Fracture)
        PixelodyMobileTheme.CartridgeQuest -> BorderStroke(1.dp, CartridgeQuestPalette.PlasticHighlight)
        PixelodyMobileTheme.ObsidianGlass -> BorderStroke(1.dp, ObsidianGlassPalette.GlassBorder)
        PixelodyMobileTheme.LoFiCafe -> BorderStroke(1.dp, LoFiCafePalette.Wood.copy(alpha = 0.5f))
        PixelodyMobileTheme.BulkheadTerminal -> BorderStroke(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.45f))
        PixelodyMobileTheme.Studio -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    }

    val shadowElevation = when (theme) {
        PixelodyMobileTheme.Obsession, PixelodyMobileTheme.ObsidianGlass, PixelodyMobileTheme.LoFiCafe -> 3.dp
        else -> 1.dp
    }

    Column(
        modifier = modifier
            .testTag(PixelodyStateTags.CRATE_FACE)
            .combinedClickable(
                onClickLabel = "Open the ${crate.name} crate",
                onLongClickLabel = "Actions for the ${crate.name} crate",
                role = Role.Button,
                onClick = onOpen,
                onLongClick = onActions
            )
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(shadowElevation, crateShape)
                .clip(crateShape),
            color = containerColor,
            border = crateBorder,
            shape = crateShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (theme == PixelodyMobileTheme.CartridgeQuest) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(CartridgeQuestPalette.ContactPinGold, RoundedCornerShape(1.dp))
                    )
                }
                for (row in 0 until CRATE_FACE_COUNT / 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (column in 0 until 2) {
                            val slot = crate.face.getOrNull(row * 2 + column)
                            FacePip(
                                face = slot?.let(resolve),
                                theme = theme,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = if (theme == PixelodyMobileTheme.BulkheadTerminal || theme == PixelodyMobileTheme.CartridgeQuest) {
                crate.name.uppercase()
            } else {
                crate.name
            },
            style = MaterialTheme.typography.labelLarge,
            fontFamily = when (theme) {
                PixelodyMobileTheme.Obsession -> FontFamily.Serif
                PixelodyMobileTheme.CartridgeQuest, PixelodyMobileTheme.BulkheadTerminal -> FontFamily.Monospace
                else -> FontFamily.Default
            },
            color = when (theme) {
                PixelodyMobileTheme.Obsession -> ObsessionPalette.Ink
                PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestPalette.TextWhite
                PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalPalette.MintPhosphor
                PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassPalette.FrostWhite
                PixelodyMobileTheme.LoFiCafe -> LoFiCafePalette.Ink
                PixelodyMobileTheme.Studio -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = countLabel,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = when (theme) {
                PixelodyMobileTheme.CartridgeQuest, PixelodyMobileTheme.BulkheadTerminal -> FontFamily.Monospace
                else -> FontFamily.Default
            },
            color = when (theme) {
                PixelodyMobileTheme.Obsession -> ObsessionPalette.Ink.copy(alpha = 0.55f)
                PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestPalette.TextMuted
                PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalPalette.PaleMint.copy(alpha = 0.7f)
                PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassPalette.FrostWhite.copy(alpha = 0.65f)
                PixelodyMobileTheme.LoFiCafe -> LoFiCafePalette.Honey.copy(alpha = 0.8f)
                PixelodyMobileTheme.Studio -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun FacePip(
    face: CrateSlotFace?,
    theme: PixelodyMobileTheme,
    modifier: Modifier = Modifier
) {
    if (face == null) {
        Box(modifier = modifier)
        return
    }

    val pipColor = when (theme) {
        PixelodyMobileTheme.Obsession -> ObsessionPalette.PanelDeep
        PixelodyMobileTheme.CartridgeQuest -> CartridgeQuestPalette.RecessedBay
        PixelodyMobileTheme.ObsidianGlass -> ObsidianGlassPalette.SmokedGraphite
        PixelodyMobileTheme.LoFiCafe -> LoFiCafePalette.Espresso
        PixelodyMobileTheme.BulkheadTerminal -> BulkheadTerminalPalette.PanelDeep
        PixelodyMobileTheme.Studio -> MaterialTheme.colorScheme.surface
    }

    val pipBorder = when (theme) {
        PixelodyMobileTheme.Obsession -> BorderStroke(0.5.dp, ObsessionPalette.Fracture.copy(alpha = 0.6f))
        PixelodyMobileTheme.CartridgeQuest -> BorderStroke(0.5.dp, CartridgeQuestPalette.PlasticHighlight.copy(alpha = 0.5f))
        PixelodyMobileTheme.ObsidianGlass -> BorderStroke(0.5.dp, ObsidianGlassPalette.GlassBorder)
        PixelodyMobileTheme.LoFiCafe -> BorderStroke(0.5.dp, LoFiCafePalette.Wood.copy(alpha = 0.4f))
        PixelodyMobileTheme.BulkheadTerminal -> BorderStroke(0.5.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.35f))
        PixelodyMobileTheme.Studio -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
    }

    val textColor = when (theme) {
        PixelodyMobileTheme.Obsession -> if (face.available) ObsessionPalette.Ink else ObsessionPalette.Ink.copy(alpha = 0.35f)
        PixelodyMobileTheme.CartridgeQuest -> if (face.available) CartridgeQuestPalette.RewardYellow else CartridgeQuestPalette.TextMuted
        PixelodyMobileTheme.ObsidianGlass -> if (face.available) ObsidianGlassPalette.FrostWhite else ObsidianGlassPalette.FrostWhite.copy(alpha = 0.35f)
        PixelodyMobileTheme.LoFiCafe -> if (face.available) LoFiCafePalette.Honey else LoFiCafePalette.Honey.copy(alpha = 0.4f)
        PixelodyMobileTheme.BulkheadTerminal -> if (face.available) BulkheadTerminalPalette.MintPhosphor else BulkheadTerminalPalette.PaleMint.copy(alpha = 0.35f)
        PixelodyMobileTheme.Studio -> if (face.available) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    val fontFamily = when (theme) {
        PixelodyMobileTheme.Obsession -> FontFamily.Serif
        PixelodyMobileTheme.CartridgeQuest, PixelodyMobileTheme.BulkheadTerminal -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    Surface(
        modifier = modifier,
        color = pipColor,
        border = pipBorder,
        shape = RoundedCornerShape(2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = face.badge,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = fontFamily,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

