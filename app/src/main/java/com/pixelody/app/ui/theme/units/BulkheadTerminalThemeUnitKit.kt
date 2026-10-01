package com.pixelody.app.ui.theme.units

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.R
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.ScanlineOverlay
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.theme.BulkheadTerminalPalette

/**
 * Bespoke ThemeUnitKit implementation for Bulkhead Terminal.
 * Emulates an industrial science fiction terminal HUD: deep terminal ground,
 * glowing mint phosphor telemetry, CRT scanlines, and framed instrument geometry.
 */
object BulkheadTerminalThemeUnitKit : ThemeUnitKit {
    override val themeId: String = "bulkhead-terminal"

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun TrackItem(
        data: TrackItemData,
        modifier: Modifier
    ) {
        val haptic = LocalHapticFeedback.current
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 72.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(2.dp),
                    ambientColor = Color(0x60000000),
                    spotColor = Color(0x80000000)
                )
                .clip(RoundedCornerShape(2.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        if (data.isPlaying) BulkheadTerminalPalette.MintPhosphor
                        else if (data.isSelected) BulkheadTerminalPalette.Amber
                        else BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.35f)
                    ),
                    RoundedCornerShape(2.dp)
                )
                .combinedClickable(
                    onClickLabel = if (data.isMissing) "Show unavailable telemetry ${data.title}" else "Execute ${data.title}",
                    role = Role.Button,
                    onClick = {
                        haptic.performTick()
                        data.onClick()
                    },
                    onLongClick = data.onLongClick
                )
                .semantics {
                    contentDescription = "${data.title}, ${data.artist}"
                    stateDescription = if (data.isPlaying) "Transmitting on Terminal" else "Telemetry Unit"
                },
            color = BulkheadTerminalPalette.Panel,
            shape = RoundedCornerShape(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                BulkheadTerminalPalette.PanelDeep,
                                BulkheadTerminalPalette.Panel
                            )
                        )
                    )
            ) {
                // Background scanline raster
                ScanlineOverlay(
                    modifier = Modifier.matchParentSize(),
                    lineColor = Color(0x18000000),
                    spacingDp = 3.dp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Phosphor telemetry vertical status bus
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(44.dp)
                            .background(
                                when {
                                    data.isPlaying -> BulkheadTerminalPalette.MintPhosphor
                                    data.isSelected -> BulkheadTerminalPalette.Amber
                                    else -> BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.25f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    // Artwork display frame
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.5f))
                    ) {
                        RemoteArtwork(
                            artworkUrl = data.artworkUrl,
                            title = data.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (data.isPlaying) {
                                Text(
                                    text = "> ",
                                    color = BulkheadTerminalPalette.MintPhosphor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = data.title.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (data.isPlaying) BulkheadTerminalPalette.MintPhosphor else BulkheadTerminalPalette.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (data.artist.isNotBlank()) "[${data.artist.uppercase()}]" else "[TERMINAL AUDIO STREAM]",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = BulkheadTerminalPalette.PaleMint.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quality / Telemetry Badge
                    if (data.isLossless) {
                        Surface(
                            color = BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BulkheadTerminalPalette.MintPhosphor),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.badge_flac_lossless),
                                    contentDescription = "FLAC Lossless",
                                    modifier = Modifier.height(13.dp)
                                )
                            }
                        }
                    } else if (data.levelLabel.isNotBlank()) {
                        Surface(
                            color = BulkheadTerminalPalette.PanelDeep,
                            border = BorderStroke(1.dp, BulkheadTerminalPalette.PaleMint.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = "[${data.levelLabel.uppercase()}]",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = BulkheadTerminalPalette.PaleMint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun TransportControls(
        data: TransportData,
        modifier: Modifier
    ) {
        val haptic = LocalHapticFeedback.current
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Control Toggle
            Surface(
                onClick = {
                    haptic.performTick()
                    data.onSkipPrevious()
                },
                enabled = data.canSkipPrevious,
                shape = RoundedCornerShape(3.dp),
                color = BulkheadTerminalPalette.Panel,
                border = BorderStroke(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.6f)),
                modifier = Modifier
                    .size(50.dp)
                    .shadow(3.dp, RoundedCornerShape(3.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "◀◀",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BulkheadTerminalPalette.MintPhosphor
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            // Primary Execute / Abort (Play/Pause) Console Pad
            Surface(
                onClick = {
                    haptic.performConfirm()
                    data.onPlayPause()
                },
                shape = RoundedCornerShape(4.dp),
                color = BulkheadTerminalPalette.MintPhosphor,
                contentColor = BulkheadTerminalPalette.EditorialBlack,
                border = BorderStroke(2.dp, BulkheadTerminalPalette.PaleMint),
                modifier = Modifier
                    .size(66.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(4.dp),
                        ambientColor = BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.40f),
                        spotColor = BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.70f)
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (data.isPlaying) "■" else "▶",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = BulkheadTerminalPalette.EditorialBlack
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            // Next Control Toggle
            Surface(
                onClick = {
                    haptic.performTick()
                    data.onSkipNext()
                },
                enabled = data.canSkipNext,
                shape = RoundedCornerShape(3.dp),
                color = BulkheadTerminalPalette.Panel,
                border = BorderStroke(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.6f)),
                modifier = Modifier
                    .size(50.dp)
                    .shadow(3.dp, RoundedCornerShape(3.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "▶▶",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BulkheadTerminalPalette.MintPhosphor
                    )
                }
            }
        }
    }

    @Composable
    override fun HeroStage(
        data: HeroStageData,
        modifier: Modifier
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.5f)), RoundedCornerShape(4.dp)),
            color = BulkheadTerminalPalette.PanelDeep,
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_hazard_strip),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter),
                    alpha = 0.55f
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_corner_bracket),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_wireframe_symbol),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(BulkheadTerminalPalette.MintPhosphor),
                    modifier = Modifier
                        .size(52.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 6.dp, end = 6.dp),
                    alpha = 0.08f
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "> ${data.title.uppercase()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = BulkheadTerminalPalette.MintPhosphor
                        )
                        Surface(
                            color = BulkheadTerminalPalette.Amber.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BulkheadTerminalPalette.Amber),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = if (data.badge.isNotBlank()) "[${data.badge.uppercase()}]" else "[TERM-OK]",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = BulkheadTerminalPalette.Amber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (data.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "// ${data.subtitle.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = BulkheadTerminalPalette.PaleMint.copy(alpha = 0.8f)
                        )
                    }
                    if (data.primaryActionLabel != null || data.secondaryActionLabel != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (data.primaryActionLabel != null) {
                                Button(
                                    onClick = { data.onPrimaryAction?.invoke() },
                                    shape = RoundedCornerShape(2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BulkheadTerminalPalette.MintPhosphor,
                                        contentColor = BulkheadTerminalPalette.EditorialBlack
                                    )
                                ) {
                                    Text(
                                        text = "> ${data.primaryActionLabel.uppercase()}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            if (data.secondaryActionLabel != null) {
                                OutlinedButton(
                                    onClick = { data.onSecondaryAction?.invoke() },
                                    shape = RoundedCornerShape(2.dp),
                                    border = BorderStroke(1.dp, BulkheadTerminalPalette.Amber),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = BulkheadTerminalPalette.Amber
                                    )
                                ) {
                                    Text(
                                        text = "[${data.secondaryActionLabel.uppercase()}]",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun QuickStartTile(
        data: QuickTileData,
        modifier: Modifier
    ) {
        val haptic = LocalHapticFeedback.current
        Surface(
            modifier = modifier
                .shadow(2.dp, RoundedCornerShape(2.dp))
                .combinedClickable(
                    enabled = data.enabled,
                    role = Role.Button,
                    onClick = {
                        haptic.performTick()
                        data.onClick()
                    },
                    onLongClick = data.onLongClick
                ),
            color = BulkheadTerminalPalette.Panel,
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (data.artworkUrl != null) {
                    RemoteArtwork(
                        artworkUrl = data.artworkUrl,
                        title = data.title,
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (data.glyph != null) {
                    val gColor = data.glyphColor ?: BulkheadTerminalPalette.MintPhosphor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BulkheadTerminalPalette.PanelDeep)
                            .border(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = data.glyph,
                            color = gColor,
                            size = 18.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "> ${data.title.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = BulkheadTerminalPalette.MintPhosphor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "[${data.subtitle.uppercase()}]",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = BulkheadTerminalPalette.PaleMint.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
