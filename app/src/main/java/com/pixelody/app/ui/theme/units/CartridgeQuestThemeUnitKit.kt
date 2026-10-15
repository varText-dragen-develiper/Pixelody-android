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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.ui.components.CartridgeCard
import com.pixelody.app.ui.components.HandheldTransportControls
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.ScanlineOverlay
import com.pixelody.app.ui.components.performTick
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.pixelody.app.R
import com.pixelody.app.ui.theme.CartridgeQuestPalette

/**
 * Bespoke ThemeUnitKit implementation for Cartridge Quest.
 * Emulates authentic 8/16-bit physical game cartridges, D-pad controls, and CRT scanlines.
 */
object CartridgeQuestThemeUnitKit : ThemeUnitKit {
    override val themeId: String = "cartridge-quest"

    @Composable
    override fun TrackItem(
        data: TrackItemData,
        modifier: Modifier
    ) {
        val haptic = LocalHapticFeedback.current
        CartridgeCard(
            title = data.title,
            subtitle = if (data.artist.isNotBlank()) data.artist else "STAGE 1",
            onClick = {
                haptic.performTick()
                data.onClick()
            },
            onLongClick = data.onLongClick,
            modifier = modifier,
            levelLabel = if (data.isLossless) "FLAC" else data.levelLabel,
            isPlaying = data.isPlaying,
            artwork = {
                RemoteArtwork(
                    artworkUrl = data.artworkUrl,
                    title = data.title,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    @Composable
    override fun TransportControls(
        data: TransportData,
        modifier: Modifier
    ) {
        val haptic = LocalHapticFeedback.current
        HandheldTransportControls(
            isPlaying = data.isPlaying,
            onPlayPause = {
                haptic.performTick()
                data.onPlayPause()
            },
            onSkipNext = {
                haptic.performTick()
                data.onSkipNext()
            },
            onSkipPrevious = {
                haptic.performTick()
                data.onSkipPrevious()
            },
            modifier = modifier
        )
    }

    @Composable
    override fun HeroStage(
        data: HeroStageData,
        modifier: Modifier
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(CartridgeQuestPalette.RecessedBay)
                .padding(14.dp)
        ) {
            ScanlineOverlay(modifier = Modifier.matchParentSize())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .background(CartridgeQuestPalette.ContactPinGold, RoundedCornerShape(1.dp))
            )
            Image(
                painter = painterResource(id = R.drawable.ic_sparkle_four_point),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 2.dp, end = 2.dp),
                alpha = 0.25f
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = data.title.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = CartridgeQuestPalette.QuestPixelFont,
                        fontWeight = FontWeight.Bold,
                        color = CartridgeQuestPalette.RewardYellow
                    )
                    Surface(
                        color = CartridgeQuestPalette.ActionViolet,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = if (data.badge.isNotBlank()) data.badge else "QUEST",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = CartridgeQuestPalette.TextWhite,
                            fontFamily = CartridgeQuestPalette.QuestPixelFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (data.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = CartridgeQuestPalette.QuestPixelFont,
                        color = CartridgeQuestPalette.TextMuted
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
                                    containerColor = CartridgeQuestPalette.RewardYellow,
                                    contentColor = CartridgeQuestPalette.EditorialBlack
                                )
                            ) {
                                Text(
                                    text = "▶ ${data.primaryActionLabel.uppercase()}",
                                    fontFamily = CartridgeQuestPalette.QuestPixelFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (data.secondaryActionLabel != null) {
                            OutlinedButton(
                                onClick = { data.onSecondaryAction?.invoke() },
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, CartridgeQuestPalette.ActionViolet)
                            ) {
                                Text(
                                    text = data.secondaryActionLabel.uppercase(),
                                    fontFamily = CartridgeQuestPalette.QuestPixelFont,
                                    color = CartridgeQuestPalette.ActionViolet,
                                    fontWeight = FontWeight.Bold
                                )
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
                .combinedClickable(
                    enabled = data.enabled,
                    role = Role.Button,
                    onClick = {
                        haptic.performTick()
                        data.onClick()
                    },
                    onLongClick = data.onLongClick
                ),
            color = CartridgeQuestPalette.ChassisGray,
            shape = RoundedCornerShape(3.dp),
            border = BorderStroke(1.dp, CartridgeQuestPalette.PlasticHighlight)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                // Micro contact pin stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(CartridgeQuestPalette.ContactPinGold, RoundedCornerShape(1.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (data.artworkUrl != null) {
                        RemoteArtwork(
                            artworkUrl = data.artworkUrl,
                            title = data.title,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .border(1.dp, CartridgeQuestPalette.RecessedBay, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (data.glyph != null) {
                        val gColor = data.glyphColor ?: CartridgeQuestPalette.RewardYellow
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CartridgeQuestPalette.CrtNavy)
                                .border(1.dp, CartridgeQuestPalette.RecessedBay, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelodyTransportGlyph(
                                glyph = data.glyph,
                                color = gColor,
                                size = 16.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = CartridgeQuestPalette.QuestPixelFont,
                            fontWeight = FontWeight.Bold,
                            color = CartridgeQuestPalette.TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = data.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontFamily = CartridgeQuestPalette.QuestPixelFont,
                            color = CartridgeQuestPalette.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
