package com.pixelody.app.feature.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelody.app.data.model.*
import com.pixelody.app.ui.components.ScreenImageBackground
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** A draft owns all choices until Apply. No appearance/cover store writes occur here. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImageStyleDialog(
    screen: ScreenBackground, uri: String, existing: CoverEntry?,
    onDismiss: () -> Unit, onApply: (Float, ScreenImageStyle) -> Unit
) {
    var analysis by remember(uri) { mutableStateOf<ImageStyleAnalysis?>(null) }
    var failed by remember(uri) { mutableStateOf(false) }
    LaunchedEffect(uri) {
        analysis = withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = BitmapFactory.decodeFile(Uri.parse(uri).path,
                    BitmapFactory.Options().apply { inSampleSize = 8 }) ?: error("Missing image")
                try {
                    if (bitmap.width * bitmap.height > 16384) error("Image sample too large")
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    ImageStyleEngine.analyze(pixels, bitmap.width)
                } finally { bitmap.recycle() }
            }.getOrNull()
        }
        failed = analysis == null
    }
    var step by rememberSaveable(uri) { mutableIntStateOf(0) }
    var opacityChoice by rememberSaveable(uri) { mutableStateOf(existing?.backgroundOpacity) }
    var recipe by rememberSaveable(uri) { mutableStateOf(existing?.imageStyle?.encoded()) }
    var opacityReviewed by rememberSaveable(uri) { mutableStateOf(false) }
    var paletteReviewed by rememberSaveable(uri) { mutableStateOf(false) }
    var typeReviewed by rememberSaveable(uri) { mutableStateOf(false) }
    var surfaceReviewed by rememberSaveable(uri) { mutableStateOf(false) }
    val result = analysis
    val opacity = opacityChoice ?: result?.recommendedOpacity ?: 0.6f
    val style = ScreenImageStyle.decode(recipe) ?: result?.palettes?.firstOrNull()
    val baseAppearance = LocalAppearanceSettings.current
    val scroll = rememberScrollState()
    // Capture Activity insets before entering Dialog: its own insets can be zero,
    // while fillMaxSize measures against the full display on edge-to-edge Android.
    val bars = WindowInsets.systemBars.asPaddingValues()
    val safeHeight = (LocalConfiguration.current.screenHeightDp.dp - bars.calculateTopPadding() - bars.calculateBottomPadding()).coerceAtLeast(160.dp)
    LaunchedEffect(step) { scroll.scrollTo(0) }
    Dialog(onDismissRequest = { if (step > 0) step-- else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxWidth().height(safeHeight), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${screen.label} · Custom", style = MaterialTheme.typography.headlineSmall)
                Text("${step + 1} of 3 · ${listOf("Image & opacity", "Color & character", "Preview & apply")[step]}", style = MaterialTheme.typography.labelLarge)
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (style != null) ImageStylePreview(screen, uri, opacity, style, baseAppearance)
                    if (result == null) {
                        if (failed) Text("This saved image can't be opened. Cancel and choose another photo.")
                        else { CircularProgressIndicator(); Text("Finding a style in your image…") }
                    } else if (style != null) {
                        when (step) {
                            0 -> {
                                Text("How much of the image feels right?", style = MaterialTheme.typography.titleLarge)
                                Text("The preview includes readable shading. Colors and framing will follow your image; this screen stays unchanged until Apply.", style = MaterialTheme.typography.bodyMedium)
                                Text("Image opacity: ${(opacity * 100).roundToInt()}%")
                                Slider(value = opacity, onValueChange = { opacityChoice = it; opacityReviewed = true },
                                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${screen.label} background opacity" })
                                OutlinedButton(onClick = { opacityChoice = result.recommendedOpacity; opacityReviewed = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                    Text("Use suggested ${(result.recommendedOpacity * 100).roundToInt()}%")
                                }
                                Text(if (opacityReviewed) "Opacity chosen" else "Adjust the slider or accept the suggestion to continue.", style = MaterialTheme.typography.bodySmall)
                            }
                            1 -> {
                                Text("Colors", style = MaterialTheme.typography.titleLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    result.palettes.forEachIndexed { index, palette ->
                                        FilterChip(selected = style.primary == palette.primary && style.base == palette.base,
                                            onClick = { recipe = palette.copy(typeface = style.typeface, surface = style.surface).encoded(); paletteReviewed = true },
                                            label = { Text(listOf("Image colors", "Counterpoint", "Quiet")[index]) }, modifier = Modifier.heightIn(min = 48.dp))
                                    }
                                }
                                Text("Headings", style = MaterialTheme.typography.titleLarge)
                                Text("The image suggests ${result.palettes.first().typeface.label.lowercase()} headings. Body text and controls keep their readable lettering.", style = MaterialTheme.typography.bodySmall)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ImageTypeface.entries.forEach { type ->
                                        FilterChip(selected = style.typeface == type, onClick = { recipe = style.copy(typeface = type).encoded(); typeReviewed = true },
                                            label = { Text(type.label) }, modifier = Modifier.heightIn(min = 48.dp))
                                    }
                                }
                                Text("Framing", style = MaterialTheme.typography.titleLarge)
                                Text("${result.palettes.first().surface.label} is suggested for this image's brightness and detail. Framing changes corners and shading.", style = MaterialTheme.typography.bodySmall)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ImageSurface.entries.forEach { surface ->
                                        FilterChip(selected = style.surface == surface, onClick = { recipe = style.copy(surface = surface).encoded(); surfaceReviewed = true },
                                            label = { Text(surface.label) }, modifier = Modifier.heightIn(min = 48.dp))
                                    }
                                }
                                OutlinedButton(onClick = { recipe = result.palettes.first().encoded(); paletteReviewed = true; typeReviewed = true; surfaceReviewed = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                    Text("Use all suggestions")
                                }
                                Text(if (paletteReviewed && typeReviewed && surfaceReviewed) "Style choices reviewed" else "Choose colors, headings and framing, or accept all suggestions.", style = MaterialTheme.typography.bodySmall)
                            }
                            2 -> {
                                Text("A look built around your image", style = MaterialTheme.typography.titleLarge)
                                Text("${(opacity * 100).roundToInt()}% opacity · ${style.typeface.label} headings · ${style.surface.label} framing")
                                Text("Only ${screen.label} changes. Other screens keep their own choices. Selecting another theme preserves this Custom look for your return to Studio.")
                                Text("You can edit or reset it from Studio backgrounds.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
                    if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
                    Button(onClick = { if (step < 2) step++ else if (style != null) onApply(opacity, style) },
                        enabled = result != null && when (step) { 0 -> opacityReviewed; 1 -> paletteReviewed && typeReviewed && surfaceReviewed; else -> style != null },
                        modifier = Modifier.heightIn(min = 48.dp)) { Text(if (step < 2) "Continue" else "Apply to ${screen.label}") }
                }
            }
        }
    }
}

@Composable
private fun ImageStylePreview(screen: ScreenBackground, uri: String, opacity: Float, style: ScreenImageStyle, appearance: AppearanceSettings) {
    PixelodyTheme(appearance = appearance, imageStyle = style, applyTextScale = false) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp)) {
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                ScreenImageBackground(uri, opacity, modifier = Modifier.matchParentSize(), shade = if (appearance.highContrast) 0.9f else style.surface.shade)
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${screen.label} preview", style = MaterialTheme.typography.labelLarge)
                    Text("Made for your music", style = MaterialTheme.typography.headlineMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary).forEach { color ->
                            Box(Modifier.size(24.dp).background(color, MaterialTheme.shapes.small))
                        }
                    }
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Your next favorite", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            PixelodyTransportGlyph(TransportGlyphType.Play, color = MaterialTheme.colorScheme.primary, sizeDp = 24)
                        }
                    }
                    Text("Home   ·   Search   ·   Library", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
