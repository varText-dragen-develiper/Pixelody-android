package com.pixelody.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pixelody.app.core.image.ArtworkBitmapCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.pixelody.app.data.model.RepeatMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.playback.TransitionCue
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.storage.SavedHostStore
import com.pixelody.app.ui.navigation.PixelodyTab
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.ColorFilter
import com.pixelody.app.R
import com.pixelody.app.ui.theme.CartridgeQuestPalette
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import com.pixelody.app.ui.theme.LoFiCafePalette
import com.pixelody.app.ui.theme.BulkheadTerminalPalette
import com.pixelody.app.ui.theme.ObsidianGlassPalette
import com.pixelody.app.ui.theme.ObsessionPalette
import com.pixelody.app.ui.theme.ChamferedPlate
import com.pixelody.app.ui.theme.PlateCorner
import com.pixelody.app.ui.theme.PixelodyMobileTheme

/* =========================================================================
 * Slice 1 Leaf Components
 * Presentational primitives with no shell dependencies.
 * ========================================================================= */

@Composable
internal fun Modifier.tactilePress(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
}

@Composable
internal fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
internal fun EmptyState(text: String) {
    val theme = LocalPixelodyThemeVariant.current
    val (iconRes, iconTint) = when (theme) {
        PixelodyMobileTheme.Obsession ->
            Pair(R.drawable.iso_cassette_favorites, ObsessionPalette.Signal)
        PixelodyMobileTheme.CartridgeQuest ->
            Pair(R.drawable.iso_cartridge_quest_empty, CartridgeQuestPalette.ActionViolet)
        PixelodyMobileTheme.BulkheadTerminal ->
            Pair(R.drawable.iso_terminal_unplugged, BulkheadTerminalPalette.MintPhosphor)
        PixelodyMobileTheme.LoFiCafe ->
            Pair(R.drawable.iso_cassette_favorites, LoFiCafePalette.Amber)
        PixelodyMobileTheme.ObsidianGlass ->
            Pair(R.drawable.iso_oscilloscope_search, ObsidianGlassPalette.PrismCyan)
        PixelodyMobileTheme.Studio ->
            Pair(R.drawable.iso_oscilloscope_search, MaterialTheme.colorScheme.primary)
    }

    Surface(
        shape = theme.plate,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                colorFilter = ColorFilter.tint(iconTint)
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalPixelodyThemeVariant.current
    Surface(
        shape = theme.plate,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
internal fun HostStatusCard(snapshot: LibrarySnapshot) {
    val theme = LocalPixelodyThemeVariant.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = theme.plate,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = snapshot.host.hostName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${snapshot.tracks.size} tracks / ${snapshot.playlists.size} playlists / ${snapshot.host.platform}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun StatusChip(
    text: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalPixelodyThemeVariant.current
    Surface(
        modifier = modifier.semantics {
            contentDescription = text
        },
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = theme.plate,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun QuickStartTile(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    com.pixelody.app.ui.theme.units.PixelodyQuickStartTile(
        data = com.pixelody.app.ui.theme.units.QuickTileData(
            title = title,
            subtitle = subtitle,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        ),
        modifier = Modifier.width(144.dp)
    )
}

@Composable
internal fun HomeSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
internal fun LibraryScopeSummary(
    browseMode: String,
    selectedCollectionTitle: String,
    resultCount: Int,
    playableCount: Int,
    smartFilter: SmartPocketFilter = SmartPocketFilter.All,
    tracks: List<Track> = emptyList(),
    onPlayFirst: () -> Unit,
    onShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onFlowFromHere: (() -> Unit)? = null,
    onShowDoc: ((String) -> Unit)? = null
) {
    val title = when {
        selectedCollectionTitle.isNotBlank() -> selectedCollectionTitle
        browseMode == "Tracks" && smartFilter == SmartPocketFilter.Recent -> "Recent Tracks"
        browseMode == "Tracks" && smartFilter == SmartPocketFilter.HiRes -> "Lossless"
        browseMode == "Tracks" && smartFilter == SmartPocketFilter.HeavyRotation -> "Heavy Rotation"
        browseMode == "Tracks" && smartFilter == SmartPocketFilter.SmartFlow -> "Smart Flow Mix"
        browseMode == "Tracks" -> "All Tracks"
        else -> browseMode
    }
    SectionCard(
        title = title,
        subtitle = "$resultCount visible track${if (resultCount == 1) "" else "s"}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tracks.size >= 2) {
                HarmonicTrajectorySparkline(
                    tracks = tracks,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    onShowDoc = onShowDoc
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    enabled = playableCount > 0,
                    onClick = onPlayFirst,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    enabled = playableCount > 1,
                    onClick = onShuffle,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.weight(1.15f)
                ) {
                    Text("Shuffle", maxLines = 1, softWrap = false)
                }
                if (onFlowFromHere != null && playableCount > 0) {
                    OutlinedButton(
                        onClick = onFlowFromHere,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Flow", maxLines = 1, softWrap = false)
                    }
                }
                OutlinedButton(
                    onClick = onOpenQueue,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Queue", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
internal fun RemoteArtwork(
    artworkUrl: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val credentialStore = remember { SavedHostStore(context) }
    val initialBitmap = remember(artworkUrl) { ArtworkBitmapCache.get(artworkUrl) }
    val bitmap by produceState<Bitmap?>(initialValue = initialBitmap, artworkUrl) {
        value = initialBitmap
        if (value == null && !artworkUrl.isNullOrBlank()) {
            value = ArtworkBitmapCache.loadBitmap(context, artworkUrl, credentialStore)
        }
    }
    val theme = LocalPixelodyThemeVariant.current
    val shape = when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> RoundedCornerShape(4.dp)
        PixelodyMobileTheme.ObsidianGlass -> RoundedCornerShape(12.dp)
        PixelodyMobileTheme.LoFiCafe -> RoundedCornerShape(6.dp)
        PixelodyMobileTheme.BulkheadTerminal -> RoundedCornerShape(2.dp)
        PixelodyMobileTheme.Obsession -> ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.12f)
        else -> RoundedCornerShape(8.dp)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .clearAndSetSemantics {
                contentDescription = "$title artwork"
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            ThemeArtworkFallback(
                theme = theme,
                title = title,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun ThemeArtworkFallback(
    theme: PixelodyMobileTheme,
    title: String,
    modifier: Modifier = Modifier
) {
    val initial = title.firstOrNull()?.uppercase() ?: "P"
    when (theme) {
        PixelodyMobileTheme.CartridgeQuest -> {
            Box(
                modifier = modifier
                    .background(CartridgeQuestPalette.RecessedBay)
                    .border(1.dp, CartridgeQuestPalette.PlasticHighlight.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.iso_cassette_favorites),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    contentScale = ContentScale.Fit,
                    alpha = 0.85f
                )
                ScanlineOverlay(modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(CartridgeQuestPalette.ActionViolet, RoundedCornerShape(1.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = initial,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CartridgeQuestPalette.TextWhite
                    )
                }
            }
        }
        PixelodyMobileTheme.ObsidianGlass -> {
            Box(
                modifier = modifier
                    .background(
                        Brush.radialGradient(
                            listOf(
                                ObsidianGlassPalette.SmokedGraphite,
                                ObsidianGlassPalette.ObsidianBlack
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0x60DFE7F3),
                                    Color(0x15DFE7F3),
                                    Color(0x40DFE7F3)
                                )
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sparkle_four_point),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    contentScale = ContentScale.Fit,
                    alpha = 0.35f
                )
                Surface(
                    color = ObsidianGlassPalette.GlassPanel,
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, ObsidianGlassPalette.GlassBorder)
                ) {
                    Box(
                        modifier = Modifier.size(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianGlassPalette.PrismCyan
                        )
                    }
                }
            }
        }
        PixelodyMobileTheme.Studio -> {
            Box(
                modifier = modifier
                    .background(Color(0xFF0C0C0D))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Sparse vector rings stay clean at both thumbnail and shelf sizes.
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension * 0.42f
                    listOf(1f, 0.78f, 0.56f).forEach { scale ->
                        drawCircle(color = Color(0xFFF6F6F8).copy(alpha = 0.14f), radius = radius * scale,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                    }
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            RoundedCornerShape(99.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        PixelodyMobileTheme.LoFiCafe -> {
            Box(
                modifier = modifier
                    .background(LoFiCafePalette.PaperSoft)
                    .border(1.dp, LoFiCafePalette.PaperTrace),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cassette_spool_gear),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit,
                    alpha = 0.45f
                )
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LoFiCafePalette.PaperInk
                )
            }
        }
        PixelodyMobileTheme.BulkheadTerminal -> {
            Box(
                modifier = modifier
                    .background(BulkheadTerminalPalette.Ground)
                    .border(1.dp, BulkheadTerminalPalette.MintPhosphor.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                ScanlineOverlay(modifier = Modifier.fillMaxSize(), lineColor = Color(0x30000000))
                Text(
                    text = "> $initial",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = BulkheadTerminalPalette.MintPhosphor
                )
            }
        }
        PixelodyMobileTheme.Obsession -> {
            val specimenRes = when (kotlin.math.abs(title.hashCode()) % 3) {
                0 -> R.drawable.ic_skeleton_key
                1 -> R.drawable.ic_raven_feather
                else -> R.drawable.ic_bottle_stipple
            }
            Box(
                modifier = modifier
                    .background(
                        Brush.radialGradient(
                            listOf(
                                ObsessionPalette.PanelDeep,
                                ObsessionPalette.Panel
                            )
                        )
                    )
                    .border(
                        1.dp,
                        ObsessionPalette.Fracture,
                        ChamferedPlate(PlateCorner.TopEnd, depthRatio = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = specimenRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(ObsessionPalette.Ink.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(ObsessionPalette.Confirmed, RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = initial,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = ObsessionPalette.Ink
                    )
                }
            }
        }
    }
}

@Composable
internal fun AlphabetScrubber(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
    activeLetter: Char? = null
) {
    val haptic = LocalHapticFeedback.current
    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    Box(
        modifier = modifier
            .width(26.dp)
            .fillMaxHeight()
            .pointerInput(alphabet) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val index = (offset.y / size.height * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                        val letter = alphabet[index]
                        if (selectedLetter != letter) {
                            selectedLetter = letter
                            haptic.performTick()
                            onLetterSelected(letter)
                        }
                    },
                    onDragEnd = { selectedLetter = null },
                    onDragCancel = { selectedLetter = null },
                    onVerticalDrag = { change, _ ->
                        val index = (change.position.y / size.height * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                        val letter = alphabet[index]
                        if (selectedLetter != letter) {
                            selectedLetter = letter
                            haptic.performTick()
                            onLetterSelected(letter)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { char ->
                val isHighlighted = char == (selectedLetter ?: activeLetter)
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }

        // Floating Magnifying Indicator Bubble
        selectedLetter?.let { letter ->
            Box(
                modifier = Modifier
                    .offset(x = (-42).dp)
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

enum class SmartPocketFilter(val label: String, val badge: String, val glyph: TransportGlyphType) {
    All("All", "All", TransportGlyphType.OmniSource),
    HiRes("Lossless only", "Lossless", TransportGlyphType.DiamondLossless),
    HeavyRotation("Heavy Rotation", "Heavy Rotation", TransportGlyphType.FlameStreak),
    SmartFlow("Flow Mix", "Smart Flow", TransportGlyphType.FlowShuffle),
    Recent("Recently Added", "Recent", TransportGlyphType.Sparkle)
}

@Composable
internal fun SmartPocketRow(
    selectedFilter: SmartPocketFilter,
    onSelectFilter: (SmartPocketFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val rowState = rememberLazyListState()
    LaunchedEffect(selectedFilter) {
        val index = SmartPocketFilter.values().indexOf(selectedFilter)
        if (index >= 0) {
            rowState.animateScrollToItem(index)
        }
    }
    LazyRow(
        state = rowState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(SmartPocketFilter.values()) { filter ->
            val isSelected = selectedFilter == filter
            FilterChip(
                selected = isSelected,
                onClick = {
                    haptic.performTick()
                    onSelectFilter(filter)
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PixelodyTransportGlyph(
                            glyph = filter.glyph,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 12.dp
                        )
                        Text(filter.badge)
                    }
                }
            )
        }
    }
}

@Composable
internal fun InteractiveFavoriteHeart(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isBursting by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isBursting) 1.4f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "heartBounce",
        finishedListener = { isBursting = false }
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .tactilePress {
                haptic.performConfirm()
                if (!isFavorite) isBursting = true
                onToggleFavorite()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isBursting) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val radius = size.minDimension / 2f
                for (i in 0 until 8) {
                    val angle = (i * Math.PI / 4).toFloat()
                    val pX = center.x + kotlin.math.cos(angle) * radius * 0.88f
                    val pY = center.y + kotlin.math.sin(angle) * radius * 0.88f
                    drawCircle(
                        color = Color(0xFFE5A93C),
                        radius = 2.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(pX, pY)
                    )
                }
            }
        }
        PixelodyTransportGlyph(
            glyph = if (isFavorite) TransportGlyphType.HeartFilled else TransportGlyphType.Heart,
            color = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
            size = 18.dp,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

@Composable
internal fun VuAudioLevelMeter(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFFE5A93C)
) {
    if (!isPlaying) {
        Canvas(modifier = modifier.size(width = 22.dp, height = 18.dp)) {
            val barSpacing = 2.dp.toPx()
            val totalSpacing = barSpacing * 3f
            val barWidth = (size.width - totalSpacing) / 4f
            val corner = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            val restingHeight = size.height * 0.2f
            for (i in 0 until 4) {
                val x = i * (barWidth + barSpacing)
                val y = size.height - restingHeight
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, restingHeight),
                    cornerRadius = corner
                )
            }
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "vuMeter")
    val h1 = infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "h1"
    )
    val h2 = infiniteTransition.animateFloat(
        initialValue = 0.70f, targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(260, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "h2"
    )
    val h3 = infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(390, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "h3"
    )
    val h4 = infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(290, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "h4"
    )

    Canvas(modifier = modifier.size(width = 22.dp, height = 18.dp)) {
        val barSpacing = 2.dp.toPx()
        val totalSpacing = barSpacing * 3f
        val barWidth = (size.width - totalSpacing) / 4f
        val corner = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        val heights = floatArrayOf(h1.value, h2.value, h3.value, h4.value)
        for (i in 0 until 4) {
            val barHeight = size.height * heights[i]
            val x = i * (barWidth + barSpacing)
            val y = size.height - barHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = corner
            )
        }
    }
}

@Composable
internal fun StudioQualityBadge(text: String, signal: Boolean) {
    Surface(
        color = if (signal) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (signal) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
internal fun StudioSourceBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
internal fun MetadataStrip(
    track: Track,
    modifier: Modifier = Modifier
) {
    val bitDepth = track.bitDepth?.let { " / ${it}-bit" }.orEmpty()
    val bitrate = track.bitrate?.let { " / ${it / 1000} kbps" }.orEmpty()
    val sampleRate = if (track.sampleRate > 0) "%.1f kHz".format(track.sampleRate / 1000.0) else "-- kHz"
    val channels = if (track.channels > 0) "${track.channels} ch" else "-- ch"
    Text(
        text = "${track.codec.ifBlank { track.format }.uppercase()} / $sampleRate$bitDepth$bitrate / $channels",
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiary,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun MetadataPanel(track: Track) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Source Quality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            MetadataStrip(track = track)
            Text(
                text = "ReplayGain ${track.replayGainDb?.let { "%.1f dB".format(it) } ?: "not reported"} / ${if (track.favorite) "Favorite" else "Not favorite"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

internal fun sourceLabelFor(track: Track): String =
    if (track.streamUrl.startsWith("content://", ignoreCase = true)) "Phone" else "Host"

internal fun isLocalTrack(track: Track?): Boolean =
    track?.streamUrl?.startsWith("content://") == true || track?.streamUrl?.startsWith("file://") == true

internal fun equalizerSummary(profile: EqualizerProfile, trackHasOverride: Boolean): String {
    val presetLabel = profile.preset.displayName
    val state = if (profile.enabled) "On" else "Bypass"
    val scope = if (trackHasOverride) "Track EQ" else "Global EQ"
    return "$scope: $presetLabel ($state)"
}

@Composable
internal fun CurrentTrackActionStrip(
    equalizerProfile: EqualizerProfile?,
    trackHasEqualizerOverride: Boolean,
    onOpenPlayer: (() -> Unit)?,
    onOpenQueue: (() -> Unit)?,
    onCycleEqualizerPreset: (() -> Unit)?
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onCycleEqualizerPreset != null) {
            item {
                FilterChip(
                    selected = equalizerProfile?.enabled == true,
                    onClick = onCycleEqualizerPreset,
                    label = { Text(equalizerProfile?.let { equalizerSummary(it, trackHasEqualizerOverride) } ?: "EQ") }
                )
            }
        }
        if (onOpenPlayer != null) {
            item {
                FilterChip(
                    selected = false,
                    onClick = onOpenPlayer,
                    label = { Text("Player") }
                )
            }
        }
        if (onOpenQueue != null) {
            item {
                FilterChip(
                    selected = false,
                    onClick = onOpenQueue,
                    label = { Text("Queue") }
                )
            }
        }
    }
}

internal fun Track.accessibilitySummary(): String {
    val quality = if (lossless) "lossless" else format.ifBlank { codec }.lowercase(Locale.US)
    val source = sourceLabelFor(this).lowercase(Locale.US)
    val status = if (missing || streamUrl.isBlank()) "unavailable" else "playable"
    return listOf(
        title,
        artist.takeIf { it.isNotBlank() }?.let { "by $it" },
        album.takeIf { it.isNotBlank() }?.let { "from $it" },
        "$quality $source track",
        status
    ).filterNotNull().joinToString(", ")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackRow(
    track: Track,
    selected: Boolean,
    onClick: () -> Unit,
    transitionCue: TransitionCue? = null,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    equalizerProfile: EqualizerProfile? = null,
    trackHasEqualizerOverride: Boolean = false,
    isOfflineCached: Boolean = false,
    isDownloading: Boolean = false,
    isQueued: Boolean = false,
    downloadProgress: Float = 0f,
    onToggleDownload: (() -> Unit)? = null,
    onOpenPlayer: (() -> Unit)? = null,
    onOpenQueue: (() -> Unit)? = null,
    onCycleEqualizerPreset: (() -> Unit)? = null
) {
    val canShowCurrentActions = selected && (
        onOpenPlayer != null ||
            onOpenQueue != null ||
            onCycleEqualizerPreset != null
        )
    val haptic = LocalHapticFeedback.current
    val theme = com.pixelody.app.ui.theme.LocalPixelodyThemeVariant.current

    val itemData = com.pixelody.app.ui.theme.units.TrackItemData(
        id = track.id,
        title = track.title,
        artist = track.artist,
        album = track.album,
        durationSeconds = track.durationSeconds,
        artworkUrl = track.artworkUrl,
        isLossless = track.lossless,
        isFavorite = isFavorite,
        isMissing = track.missing,
        isSelected = selected,
        isPlaying = isPlaying,
        isOfflineCached = isOfflineCached,
        isDownloading = isDownloading,
        isQueued = isQueued,
        downloadProgress = downloadProgress,
        levelLabel = if (track.lossless) "FLAC" else "ROM",
        onClick = onClick,
        onLongClick = onLongClick,
        onToggleFavorite = onToggleFavorite,
        onToggleDownload = onToggleDownload,
        onOpenPlayer = onOpenPlayer,
        onOpenQueue = onOpenQueue,
        onCycleEqualizerPreset = onCycleEqualizerPreset
    )
    com.pixelody.app.ui.theme.units.PixelodyTrackItem(data = itemData)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CompactTrackPill(
    track: Track,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .width(168.dp)
            .combinedClickable(
                onClickLabel = "Play ${track.title}",
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = LocalPixelodyThemeVariant.current.plate,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
    ) {
        Row(modifier = Modifier.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(artworkUrl = track.artworkUrl, title = track.title, modifier = Modifier.size(34.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                Text(text = track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

internal fun formatDuration(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0L)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
internal fun PixelodyNavGlyph(
    tab: PixelodyTab,
    selected: Boolean
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = if (selected) 3.2f else 2.4f
        val stroke = Stroke(width = strokeWidth)
        val w = size.width
        val h = size.height
        when (tab) {
            PixelodyTab.Home -> {
                drawLine(color, Offset(w * 0.18f, h * 0.58f), Offset(w * 0.50f, h * 0.28f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.28f), Offset(w * 0.82f, h * 0.58f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.28f, h * 0.54f), Offset(w * 0.28f, h * 0.82f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.72f, h * 0.54f), Offset(w * 0.72f, h * 0.82f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.28f, h * 0.82f), Offset(w * 0.72f, h * 0.82f), strokeWidth, StrokeCap.Round)
            }
            PixelodyTab.Search -> {
                drawCircle(color, radius = w * 0.24f, center = Offset(w * 0.42f, h * 0.42f), style = stroke)
                drawLine(color, Offset(w * 0.60f, h * 0.60f), Offset(w * 0.82f, h * 0.82f), strokeWidth, StrokeCap.Round)
            }
            PixelodyTab.Create -> {
                drawCircle(color, radius = w * 0.30f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.30f), Offset(w * 0.50f, h * 0.70f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.30f, h * 0.50f), Offset(w * 0.70f, h * 0.50f), strokeWidth, StrokeCap.Round)
            }
            PixelodyTab.Profile -> {
                drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.34f), style = stroke)
                drawCircle(color, radius = w * 0.30f, center = Offset(w * 0.50f, h * 0.86f), style = stroke)
            }
            PixelodyTab.Library -> {
                drawLine(color, Offset(w * 0.22f, h * 0.28f), Offset(w * 0.82f, h * 0.28f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.22f, h * 0.50f), Offset(w * 0.72f, h * 0.50f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.22f, h * 0.72f), Offset(w * 0.82f, h * 0.72f), strokeWidth, StrokeCap.Round)
                drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.12f, h * 0.28f))
                drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.12f, h * 0.50f))
                drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.12f, h * 0.72f))
            }
            PixelodyTab.Player -> {
                drawCircle(color, radius = w * 0.34f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                drawLine(color, Offset(w * 0.43f, h * 0.35f), Offset(w * 0.43f, h * 0.65f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.43f, h * 0.35f), Offset(w * 0.65f, h * 0.50f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.43f, h * 0.65f), Offset(w * 0.65f, h * 0.50f), strokeWidth, StrokeCap.Round)
            }
            else -> {
                drawCircle(color, radius = w * 0.30f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
            }
        }
    }
}

internal val RepeatMode.compactLabel: String
    get() = when (this) {
        RepeatMode.Off -> "Repeat"
        RepeatMode.All -> "Repeat all"
        RepeatMode.One -> "Repeat one"
    }

internal val RepeatMode.longLabel: String
    get() = when (this) {
        RepeatMode.Off -> "Repeat off"
        RepeatMode.All -> "Repeat all"
        RepeatMode.One -> "Repeat one"
    }


