package com.pixelody.app.ui.theme.units

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.theme.ObsidianGlassPalette
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.pixelody.app.R

/**
 * Bespoke ThemeUnitKit implementation for Obsidian Glass.
 * Emulates floating smoked black acrylic glass, rounded frosted pills, and soft prism refractions.
 */
object ObsidianGlassThemeUnitKit : ThemeUnitKit {
    override val themeId: String = "obsidian-glass"

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
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0x60000000),
                    spotColor = Color(0x80000000)
                )
                .clip(RoundedCornerShape(16.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        if (data.isSelected) Brush.linearGradient(
                            listOf(
                                ObsidianGlassPalette.PrismCyan,
                                ObsidianGlassPalette.PrismViolet.copy(alpha = 0.7f)
                            )
                        )
                        else Brush.linearGradient(
                            listOf(
                                Color(0x65DFE7F3),
                                Color(0x14DFE7F3),
                                Color(0x35DFE7F3)
                            )
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
                .combinedClickable(
                    onClickLabel = if (data.isMissing) "Show unavailable track ${data.title}" else "Play ${data.title}",
                    role = Role.Button,
                    onClick = {
                        haptic.performTick()
                        data.onClick()
                    },
                    onLongClick = data.onLongClick
                )
                .semantics {
                    contentDescription = "${data.title}, ${data.artist}"
                    stateDescription = if (data.isSelected) "Playing on glass" else "Track"
                },
            color = if (data.isSelected) ObsidianGlassPalette.SmokedGraphite else ObsidianGlassPalette.GlassPanel,
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0x18DFE7F3),
                                Color(0x02000000)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prism glowing indicator pill
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (data.isSelected) Brush.verticalGradient(
                                    listOf(
                                        ObsidianGlassPalette.PrismCyan,
                                        ObsidianGlassPalette.PrismViolet
                                    )
                                )
                                else Brush.verticalGradient(
                                    listOf(
                                        ObsidianGlassPalette.PrismViolet.copy(alpha = 0.45f),
                                        ObsidianGlassPalette.PrismCyan.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    RemoteArtwork(
                        artworkUrl = data.artworkUrl,
                        title = data.title,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, ObsidianGlassPalette.GlassBorder, RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ObsidianGlassPalette.FrostWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (data.artist.isNotBlank()) data.artist else "Ambient Prism",
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianGlassPalette.FrostWhite.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (data.isLossless) {
                        Surface(
                            color = ObsidianGlassPalette.PrismCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ObsidianGlassPalette.PrismCyan.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.badge_flac_lossless),
                                    contentDescription = "FLAC Lossless",
                                    modifier = Modifier.height(13.dp)
                                )
                            }
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
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color(0x70000000),
                    spotColor = Color(0x90000000)
                ),
            shape = RoundedCornerShape(32.dp),
            color = ObsidianGlassPalette.GlassPanel,
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0x70DFE7F3),
                        Color(0x18DFE7F3)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        haptic.performTick()
                        data.onSkipPrevious()
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianGlassPalette.SmokedGraphite,
                    border = BorderStroke(1.dp, ObsidianGlassPalette.GlassBorder),
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⏮", fontSize = 16.sp, color = ObsidianGlassPalette.FrostWhite)
                    }
                }

                Surface(
                    onClick = {
                        haptic.performTick()
                        data.onPlayPause()
                    },
                    shape = RoundedCornerShape(27.dp),
                    color = ObsidianGlassPalette.PrismCyan,
                    contentColor = ObsidianGlassPalette.GlassInk,
                    border = BorderStroke(1.dp, ObsidianGlassPalette.GlassHighlight),
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(27.dp),
                            ambientColor = ObsidianGlassPalette.PrismCyan.copy(alpha = 0.3f),
                            spotColor = ObsidianGlassPalette.PrismCyan.copy(alpha = 0.5f)
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (data.isPlaying) "❚❚" else "▶",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianGlassPalette.GlassInk
                        )
                    }
                }

                Surface(
                    onClick = {
                        haptic.performTick()
                        data.onSkipNext()
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianGlassPalette.SmokedGraphite,
                    border = BorderStroke(1.dp, ObsidianGlassPalette.GlassBorder),
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⏭", fontSize = 16.sp, color = ObsidianGlassPalette.FrostWhite)
                    }
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
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0x60000000),
                    spotColor = Color(0x80000000)
                )
                .clip(RoundedCornerShape(22.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0x60DFE7F3),
                                Color(0x18DFE7F3)
                            )
                        )
                    ),
                    RoundedCornerShape(22.dp)
                ),
            color = ObsidianGlassPalette.GlassPanel,
            shape = RoundedCornerShape(22.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Top right subtle ambient sparkle
                Image(
                    painter = painterResource(id = R.drawable.ic_sparkle_four_point),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp),
                    alpha = 0.45f
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_focal_flare),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 4.dp, end = 4.dp),
                    alpha = 0.14f
                )
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianGlassPalette.FrostWhite
                        )
                        Surface(
                            color = ObsidianGlassPalette.PrismMint.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, ObsidianGlassPalette.PrismMint),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (data.badge.isNotBlank()) data.badge else "GLASS",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianGlassPalette.PrismMint,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (data.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = data.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ObsidianGlassPalette.FrostWhite.copy(alpha = 0.70f)
                        )
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
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, ObsidianGlassPalette.GlassBorder), RoundedCornerShape(14.dp))
                .combinedClickable(
                    enabled = data.enabled,
                    role = Role.Button,
                    onClick = {
                        haptic.performTick()
                        data.onClick()
                    },
                    onLongClick = data.onLongClick
                ),
            color = ObsidianGlassPalette.GlassPanel,
            shape = RoundedCornerShape(14.dp)
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ObsidianGlassPalette.GlassBorder, RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (data.glyph != null) {
                    val gColor = data.glyphColor ?: ObsidianGlassPalette.PrismCyan
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(gColor.copy(alpha = 0.2f))
                            .border(1.dp, gColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = data.glyph,
                            color = gColor,
                            size = 16.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianGlassPalette.FrostWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = ObsidianGlassPalette.FrostWhite.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
