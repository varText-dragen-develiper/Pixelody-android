package com.pixelody.app.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.pixelody.app.R
import com.pixelody.app.ui.theme.CartridgeQuestPalette

/**
 * Renders an authentic 8/16-bit physical game cartridge card for playlists and albums.
 *
 * Visual Anatomy:
 * - Top grip notch section (molded plastic grooves).
 * - Recessed label chamber holding artwork and metadata.
 * - Bottom metallic contact pin lip in gold.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CartridgeCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: (@Composable () -> Unit)? = null,
    levelLabel: String = "ROM",
    isPlaying: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val isLossless = levelLabel.contains("FLAC", ignoreCase = true)
    val cardShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                ambientColor = Color(0x60000000),
                spotColor = Color(0x80000000)
            )
            .clip(cardShape)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClickLabel = "Play $title",
                        onLongClickLabel = "Options for $title",
                        role = Role.Button,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(
                        onClickLabel = "Play $title",
                        role = Role.Button,
                        onClick = onClick
                    )
                }
            ),
        color = CartridgeQuestPalette.ChassisGray,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isPlaying) CartridgeQuestPalette.RewardYellow else CartridgeQuestPalette.PlasticHighlight
        ),
        shape = cardShape
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF38344B),
                            Color(0xFF262335)
                        )
                    )
                )
                .padding(6.dp)
        ) {
            // Top Grip Ridges: molded horizontal notches with light and shadow relief
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) {
                    Column(
                        modifier = Modifier
                            .width(14.dp)
                            .height(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color(0xFF151220), RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(CartridgeQuestPalette.PlasticHighlight, RoundedCornerShape(bottomStart = 1.dp, bottomEnd = 1.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cartridge Sticker Inset Chamber
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CartridgeQuestPalette.RecessedBay, RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF100D1A),
                                CartridgeQuestPalette.PlasticHighlight.copy(alpha = 0.4f)
                            )
                        ),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (artwork != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .border(1.dp, CartridgeQuestPalette.PlasticHighlight, RoundedCornerShape(3.dp))
                        ) {
                            artwork()
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPlaying) {
                                Text(
                                    text = "▶ ",
                                    color = CartridgeQuestPalette.RewardYellow,
                                    fontSize = 12.sp,
                                    fontFamily = CartridgeQuestPalette.QuestPixelFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CartridgeQuestPalette.TextWhite,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            // One ink tier: the palette has no muted grey by design,
                            // so the smaller regular weight carries the hierarchy.
                            color = CartridgeQuestPalette.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Cartridge ROM / Level / Lossless Badge
                    if (isLossless) {
                        Surface(
                            color = CartridgeQuestPalette.ContactPinGold.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                CartridgeQuestPalette.ContactPinGold
                            ),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.badge_flac_lossless),
                                    contentDescription = "FLAC Lossless",
                                    modifier = Modifier.height(13.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = if (isPlaying) CartridgeQuestPalette.RewardYellow.copy(alpha = 0.18f) else CartridgeQuestPalette.ActionViolet.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPlaying) CartridgeQuestPalette.RewardYellow else CartridgeQuestPalette.ActionViolet
                            ),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = levelLabel,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPlaying) CartridgeQuestPalette.RewardYellow else CartridgeQuestPalette.ActionViolet,
                                fontFamily = CartridgeQuestPalette.QuestPixelFont,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Bottom Metallic Contact Pin Lip (PCB connector)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0D17), RoundedCornerShape(1.dp))
                    .padding(vertical = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(14) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(4.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            CartridgeQuestPalette.ContactPinGold,
                                            Color(0xFFB88220)
                                        )
                                    ),
                                    RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lightweight CRT Scanline Canvas overlay for artwork and display screens.
 */
@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0x18000000),
    spacingDp: Dp = 3.dp
) {
    Canvas(modifier = modifier) {
        val spacingPx = spacingDp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += spacingPx
        }
    }
}

/**
 * Retro Handheld D-Pad and A/B action transport controls for Cartridge Quest.
 */
@Composable
fun HandheldTransportControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // D-Pad navigation cluster (Left / Right skips)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = onSkipPrevious,
                modifier = Modifier
                    .size(40.dp)
                    .shadow(3.dp, RoundedCornerShape(4.dp)),
                color = CartridgeQuestPalette.RecessedBay,
                border = androidx.compose.foundation.BorderStroke(1.dp, CartridgeQuestPalette.PlasticHighlight),
                shape = RoundedCornerShape(4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("◀", color = CartridgeQuestPalette.TextWhite, fontSize = 14.sp)
                }
            }

            Surface(
                onClick = onSkipNext,
                modifier = Modifier
                    .size(40.dp)
                    .shadow(3.dp, RoundedCornerShape(4.dp)),
                color = CartridgeQuestPalette.RecessedBay,
                border = androidx.compose.foundation.BorderStroke(1.dp, CartridgeQuestPalette.PlasticHighlight),
                shape = RoundedCornerShape(4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("▶", color = CartridgeQuestPalette.TextWhite, fontSize = 14.sp)
                }
            }
        }

        // Action Buttons: (A) Play / Pause with molded 3D plastic depth
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(50.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = Color(0x60000000),
                        spotColor = Color(0x80000000)
                    ),
                color = if (isPlaying) CartridgeQuestPalette.RewardYellow else CartridgeQuestPalette.ActionViolet,
                border = androidx.compose.foundation.BorderStroke(2.dp, CartridgeQuestPalette.PlasticHighlight),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isPlaying) "❚❚" else "▶",
                        color = if (isPlaying) Color(0xFF141103) else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
