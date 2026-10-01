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
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.theme.LoFiCafePalette

/**
 * Bespoke ThemeUnitKit implementation for Lo-Fi Café.
 * Emulates a rainy walnut listening nook: dark wood structure, paper-soft content planes,
 * warm amber playback accents, and cassette spool gear iconography.
 */
object LoFiCafeThemeUnitKit : ThemeUnitKit {
    override val themeId: String = "lo-fi-cafe"

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
                    shape = RoundedCornerShape(10.dp),
                    ambientColor = Color(0x60000000),
                    spotColor = Color(0x80000000)
                )
                .clip(RoundedCornerShape(10.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        if (data.isPlaying) LoFiCafePalette.Amber
                        else if (data.isSelected) LoFiCafePalette.Honey.copy(alpha = 0.6f)
                        else LoFiCafePalette.Wood.copy(alpha = 0.35f)
                    ),
                    RoundedCornerShape(10.dp)
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
                    stateDescription = if (data.isPlaying) "Playing in Café" else "Track"
                },
            color = LoFiCafePalette.Espresso,
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                LoFiCafePalette.Walnut.copy(alpha = 0.5f),
                                LoFiCafePalette.Espresso
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vintage Amber / Honey Status Indicator Bus
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    data.isPlaying -> LoFiCafePalette.Amber
                                    data.isSelected -> LoFiCafePalette.Honey
                                    else -> LoFiCafePalette.Wood.copy(alpha = 0.45f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    // Artwork with Paper Tape Collar
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, LoFiCafePalette.PaperTrace, RoundedCornerShape(6.dp))
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
                                    text = "● ",
                                    color = LoFiCafePalette.Amber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = data.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LoFiCafePalette.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (data.artist.isNotBlank()) data.artist else "Lo-Fi Tape Archive",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoFiCafePalette.Honey.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quality Badge
                    if (data.isLossless) {
                        Surface(
                            color = LoFiCafePalette.Honey.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, LoFiCafePalette.Honey.copy(alpha = 0.75f)),
                            shape = RoundedCornerShape(3.dp)
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
                            color = LoFiCafePalette.Walnut,
                            border = BorderStroke(1.dp, LoFiCafePalette.Wood.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = data.levelLabel,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = LoFiCafePalette.Honey,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
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
            // Previous Button (Dark Walnut Round Pill)
            Surface(
                onClick = {
                    haptic.performTick()
                    data.onSkipPrevious()
                },
                enabled = data.canSkipPrevious,
                shape = RoundedCornerShape(24.dp),
                color = LoFiCafePalette.Walnut,
                border = BorderStroke(1.dp, LoFiCafePalette.Wood),
                modifier = Modifier
                    .size(50.dp)
                    .shadow(3.dp, RoundedCornerShape(24.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⏮",
                        fontSize = 18.sp,
                        color = LoFiCafePalette.Honey
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Central Amber Play/Pause Button
            Surface(
                onClick = {
                    haptic.performConfirm()
                    data.onPlayPause()
                },
                shape = RoundedCornerShape(34.dp),
                color = if (data.isPlaying) LoFiCafePalette.Amber else LoFiCafePalette.Honey,
                contentColor = LoFiCafePalette.Espresso,
                border = BorderStroke(2.dp, LoFiCafePalette.Wood),
                modifier = Modifier
                    .size(68.dp)
                    .shadow(
                        elevation = 5.dp,
                        shape = RoundedCornerShape(34.dp),
                        ambientColor = LoFiCafePalette.Amber.copy(alpha = 0.35f),
                        spotColor = LoFiCafePalette.Amber.copy(alpha = 0.60f)
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (data.isPlaying) "❚❚" else "▶",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = LoFiCafePalette.Espresso
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Next Button (Dark Walnut Round Pill)
            Surface(
                onClick = {
                    haptic.performTick()
                    data.onSkipNext()
                },
                enabled = data.canSkipNext,
                shape = RoundedCornerShape(24.dp),
                color = LoFiCafePalette.Walnut,
                border = BorderStroke(1.dp, LoFiCafePalette.Wood),
                modifier = Modifier
                    .size(50.dp)
                    .shadow(3.dp, RoundedCornerShape(24.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⏭",
                        fontSize = 18.sp,
                        color = LoFiCafePalette.Honey
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
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, LoFiCafePalette.Wood.copy(alpha = 0.4f)), RoundedCornerShape(12.dp)),
            color = LoFiCafePalette.Walnut,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                LoFiCafePalette.Walnut,
                                LoFiCafePalette.Espresso
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cassette_spool_gear),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(LoFiCafePalette.Amber),
                    alpha = 0.08f,
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 4.dp, end = 4.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LoFiCafePalette.Ink
                    )
                    Surface(
                        color = LoFiCafePalette.Amber.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, LoFiCafePalette.Amber.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (data.badge.isNotBlank()) data.badge else "CAFÉ",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = LoFiCafePalette.Amber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (data.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LoFiCafePalette.Honey.copy(alpha = 0.85f)
                    )
                }
                if (data.primaryActionLabel != null || data.secondaryActionLabel != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (data.primaryActionLabel != null) {
                            Button(
                                onClick = { data.onPrimaryAction?.invoke() },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LoFiCafePalette.Amber,
                                    contentColor = LoFiCafePalette.Espresso
                                )
                            ) {
                                Text(
                                    text = data.primaryActionLabel,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (data.secondaryActionLabel != null) {
                            OutlinedButton(
                                onClick = { data.onSecondaryAction?.invoke() },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, LoFiCafePalette.Honey),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = LoFiCafePalette.Honey
                                )
                            ) {
                                Text(
                                    text = data.secondaryActionLabel,
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
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .combinedClickable(
                    enabled = data.enabled,
                    role = Role.Button,
                    onClick = {
                        haptic.performTick()
                        data.onClick()
                    },
                    onLongClick = data.onLongClick
                ),
            color = LoFiCafePalette.Walnut,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, LoFiCafePalette.Wood.copy(alpha = 0.45f))
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
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, LoFiCafePalette.PaperTrace, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (data.glyph != null) {
                    val gColor = data.glyphColor ?: LoFiCafePalette.Amber
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(LoFiCafePalette.Espresso)
                            .border(1.dp, LoFiCafePalette.Wood, RoundedCornerShape(4.dp)),
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
                        fontWeight = FontWeight.SemiBold,
                        color = LoFiCafePalette.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = LoFiCafePalette.Honey.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
