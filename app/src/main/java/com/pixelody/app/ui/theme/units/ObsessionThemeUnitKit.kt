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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.rotate
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
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.theme.ChamferedPlate
import com.pixelody.app.ui.theme.ObsessionPalette
import com.pixelody.app.ui.theme.PlateCorner
import kotlin.math.abs

/**
 * Bespoke ThemeUnitKit implementation for Obsession Mode.
 * Emulates a restrained psychological-thriller poster interface:
 * bone-white type, deep near-black obsidian plate structure with pixel-cut chamfers,
 * original stippled noir specimen iconography, and crimson strictly reserved for playback and selection.
 */
object ObsessionThemeUnitKit : ThemeUnitKit {
    override val themeId: String = "obsession"

    private val CardChamfer = ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.18f)
    private val ButtonChamfer = ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.25f)
    private val HeroChamfer = ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.28f)

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
                    elevation = if (data.isPlaying) 4.dp else 2.dp,
                    shape = CardChamfer,
                    ambientColor = Color(0x70000000),
                    spotColor = if (data.isPlaying) ObsessionPalette.Glow else Color(0x90000000)
                )
                .clip(CardChamfer)
                .border(
                    BorderStroke(
                        1.dp,
                        when {
                            data.isPlaying -> ObsessionPalette.Confirmed
                            data.isSelected -> ObsessionPalette.Intent.copy(alpha = 0.7f)
                            else -> ObsessionPalette.Fracture.copy(alpha = 0.6f)
                        }
                    ),
                    CardChamfer
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
                    stateDescription = if (data.isPlaying) "Playing in Obsession" else "Track"
                },
            color = ObsessionPalette.Panel,
            shape = CardChamfer
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                if (data.isPlaying) ObsessionPalette.Hero.copy(alpha = 0.45f)
                                else ObsessionPalette.PanelDeep.copy(alpha = 0.35f),
                                ObsessionPalette.Panel
                            )
                        )
                    )
            ) {
                // Left Tension Indicator when active
                if (data.isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(ObsessionPalette.Confirmed)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dramatic Stippled Noir Fallback or Remote Artwork
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ObsessionPalette.PanelDeep)
                            .border(
                                1.dp,
                                if (data.isPlaying) ObsessionPalette.Confirmed.copy(alpha = 0.7f)
                                else ObsessionPalette.Fracture,
                                RoundedCornerShape(3.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (data.artworkUrl != null) {
                            RemoteArtwork(
                                artworkUrl = data.artworkUrl,
                                title = data.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val assetRes = when (abs(data.title.hashCode()) % 3) {
                                0 -> R.drawable.ic_skeleton_key
                                1 -> R.drawable.ic_raven_feather
                                else -> R.drawable.ic_bottle_stipple
                            }
                            Image(
                                painter = painterResource(id = assetRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .padding(2.dp),
                                colorFilter = ColorFilter.tint(
                                    if (data.isPlaying) ObsessionPalette.Intent
                                    else ObsessionPalette.Ink.copy(alpha = 0.65f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track Title & Artist in Thriller Bone Typography
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = data.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = if (data.isPlaying) ObsessionPalette.Intent else ObsessionPalette.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (data.artist.isNotBlank()) data.artist else "OBSESSION VAULT",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Serif,
                            fontSize = 12.sp,
                            color = ObsessionPalette.Ink.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Lossless FLAC badge or level badge
                    if (data.isLossless) {
                        Surface(
                            color = ObsessionPalette.Hero.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, ObsessionPalette.Confirmed.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.badge_flac_lossless),
                                    contentDescription = "FLAC Lossless",
                                    modifier = Modifier.height(13.dp),
                                    colorFilter = ColorFilter.tint(ObsessionPalette.Intent)
                                )
                            }
                        }
                    } else if (data.levelLabel.isNotBlank()) {
                        Surface(
                            color = ObsessionPalette.PanelDeep,
                            border = BorderStroke(1.dp, ObsessionPalette.Fracture),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = data.levelLabel.uppercase(),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = ObsessionPalette.Ink.copy(alpha = 0.65f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Playing Crimson Tension Beacon
                    if (data.isPlaying) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .rotate(45f)
                                .background(ObsessionPalette.Confirmed)
                                .border(1.dp, ObsessionPalette.Intent)
                        )
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
            // Previous Button
            Surface(
                onClick = {
                    haptic.performTick()
                    data.onSkipPrevious()
                },
                enabled = data.canSkipPrevious,
                modifier = Modifier
                    .size(50.dp)
                    .clip(ChamferedPlate(PlateCorner.TopStart, depthRatio = 0.25f))
                    .border(BorderStroke(1.dp, ObsessionPalette.Fracture), ChamferedPlate(PlateCorner.TopStart, depthRatio = 0.25f)),
                color = ObsessionPalette.PanelDeep,
                shape = ChamferedPlate(PlateCorner.TopStart, depthRatio = 0.25f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.Previous,
                        color = ObsessionPalette.Ink.copy(alpha = 0.85f),
                        size = 20.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Play / Pause Central Crimson Block
            Surface(
                onClick = {
                    haptic.performConfirm()
                    data.onPlayPause()
                },
                modifier = Modifier
                    .size(68.dp)
                    .shadow(
                        elevation = if (data.isPlaying) 6.dp else 3.dp,
                        shape = ButtonChamfer,
                        ambientColor = ObsessionPalette.Glow,
                        spotColor = ObsessionPalette.Signal
                    )
                    .clip(ButtonChamfer)
                    .border(
                        BorderStroke(1.5.dp, ObsessionPalette.Confirmed),
                        ButtonChamfer
                    ),
                color = if (data.isPlaying) ObsessionPalette.Confirmed else ObsessionPalette.PanelDeep,
                shape = ButtonChamfer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PixelodyTransportGlyph(
                        glyph = if (data.isPlaying) TransportGlyphType.Pause else TransportGlyphType.Play,
                        color = if (data.isPlaying) ObsessionPalette.EditorialBlack else ObsessionPalette.Ink,
                        size = 28.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Next Button
            Surface(
                onClick = {
                    haptic.performTick()
                    data.onSkipNext()
                },
                enabled = data.canSkipNext,
                modifier = Modifier
                    .size(50.dp)
                    .clip(ButtonChamfer)
                    .border(BorderStroke(1.dp, ObsessionPalette.Fracture), ButtonChamfer),
                color = ObsessionPalette.PanelDeep,
                shape = ButtonChamfer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PixelodyTransportGlyph(
                        glyph = TransportGlyphType.Next,
                        color = ObsessionPalette.Ink.copy(alpha = 0.85f),
                        size = 20.dp
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
                .shadow(
                    elevation = 4.dp,
                    shape = HeroChamfer,
                    ambientColor = Color(0x80000000),
                    spotColor = ObsessionPalette.Glow
                )
                .clip(HeroChamfer)
                .border(BorderStroke(1.dp, ObsessionPalette.Fracture), HeroChamfer),
            color = ObsessionPalette.PanelDeep,
            shape = HeroChamfer
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ObsessionPalette.Hero.copy(alpha = 0.35f),
                                ObsessionPalette.PanelDeep
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_skeleton_key),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ObsessionPalette.Signal),
                    alpha = 0.08f,
                    modifier = Modifier
                        .size(68.dp)
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 4.dp)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "// OBSESSION • AUDIO VAULT",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ObsessionPalette.Intent.copy(alpha = 0.85f),
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (data.badge.isNotBlank()) {
                            Surface(
                                color = ObsessionPalette.Confirmed,
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Text(
                                    text = data.badge.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = ObsessionPalette.EditorialBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ObsessionPalette.Ink
                    )

                    if (data.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = data.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Serif,
                            color = ObsessionPalette.Ink.copy(alpha = 0.7f)
                        )
                    }

                    if (data.primaryActionLabel != null || data.secondaryActionLabel != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (data.primaryActionLabel != null) {
                                Button(
                                    onClick = { data.onPrimaryAction?.invoke() },
                                    shape = ButtonChamfer,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ObsessionPalette.Confirmed,
                                        contentColor = ObsessionPalette.Ink
                                    )
                                ) {
                                    Text(
                                        text = data.primaryActionLabel,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (data.secondaryActionLabel != null) {
                                OutlinedButton(
                                    onClick = { data.onSecondaryAction?.invoke() },
                                    shape = ButtonChamfer,
                                    border = BorderStroke(1.dp, ObsessionPalette.Fracture),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = ObsessionPalette.Ink
                                    )
                                ) {
                                    Text(
                                        text = data.secondaryActionLabel,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Medium
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
                .shadow(2.dp, CardChamfer)
                .combinedClickable(
                    enabled = data.enabled,
                    role = Role.Button,
                    onClick = {
                        haptic.performTick()
                        data.onClick()
                    },
                    onLongClick = data.onLongClick
                ),
            color = ObsessionPalette.Panel,
            shape = CardChamfer,
            border = BorderStroke(1.dp, ObsessionPalette.Fracture.copy(alpha = 0.7f))
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
                            .clip(RoundedCornerShape(3.dp))
                            .border(1.dp, ObsessionPalette.Fracture, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (data.glyph != null) {
                    val gColor = data.glyphColor ?: ObsessionPalette.Intent
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ObsessionPalette.Hero.copy(alpha = 0.5f))
                            .border(1.dp, ObsessionPalette.Fracture, RoundedCornerShape(3.dp)),
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
                        text = data.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsessionPalette.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        color = ObsessionPalette.Ink.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
