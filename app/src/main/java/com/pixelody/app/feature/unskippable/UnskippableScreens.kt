package com.pixelody.app.feature.unskippable

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.core.shuffle.UniversalShuffleMetadata
import com.pixelody.app.core.unskippable.UnskippableMix
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.storage.UnskippableMixRecord
import com.pixelody.app.ui.components.RemoteArtwork

/** The generator's input for one phone or desktop song. */
fun Track.unskippableItem(): UnskippableMix.Item =
    UnskippableMix.Item(UniversalShuffleMetadata.rawEntry(this, id), durationSeconds.toLong() * 1000L)

fun formatMixDuration(ms: Long): String {
    val minutes = Math.round(ms / 60000.0).toInt().coerceAtLeast(0)
    val hours = minutes / 60
    return when {
        hours == 0 -> "$minutes min"
        minutes % 60 == 0 -> "$hours hr"
        else -> "$hours hr ${minutes % 60} min"
    }
}

fun recipeLine(recipe: UnskippableMix.Recipe): String =
    if (recipe.style == "rigid") "One genre · ${recipe.genre ?: "Unknown"}" else "Flow mix"

private fun songs(count: Int) = "$count song${if (count == 1) "" else "s"}"

/** The Unskippable row above playlists in Library: make a new mix, or open a saved one. */
@Composable
fun UnskippableShelf(
    mixes: List<UnskippableMixRecord>,
    durationOf: (UnskippableMixRecord) -> Long,
    onMake: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Unskippable", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).semantics { heading() })
            TextButton(onClick = onMake) { Text("Make a mix") }
        }
        if (mixes.isEmpty()) {
            Text("Mixes that play start to finish, no skipping. Make one from a genre or a Flow mix.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        mixes.forEach { mix ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open") { onOpen(mix.id) }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center) {
                        Text("U", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(mix.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${recipeLine(mix.recipe)} · ${formatMixDuration(durationOf(mix))}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (mix.progressTrackId != null) Text("Picks up where you left off", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Make an unskippable mix: pick a style, length and genre, review songs without a genre
 * tag, preview the list, swap or remove songs, then save. Nothing is stored until Save.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnskippableBuilder(
    tracks: List<Track>,
    seedRecipe: UnskippableMix.Recipe?,
    uniqueName: (String) -> String,
    onTagGenre: (List<String>, String) -> Unit,
    onSave: (String, UnskippableMix.Preview) -> Unit,
    onDismiss: () -> Unit
) {
    val items = remember(tracks) { tracks.filter { !it.missing }.map { it.unskippableItem() } }
    val byId = remember(tracks) { tracks.associateBy { it.id } }
    val catalog = remember(items) { UnskippableMix.genreCatalog(items) }
    var style by remember { mutableStateOf(seedRecipe?.style ?: "flow") }
    var genre by remember { mutableStateOf(seedRecipe?.genre ?: catalog.genres.firstOrNull()?.label) }
    var minutes by remember { mutableStateOf(seedRecipe?.targetMinutes ?: UnskippableMix.DEFAULT_MINUTES) }
    var custom by remember { mutableStateOf(seedRecipe != null && seedRecipe.targetMinutes !in UnskippableMix.LENGTHS) }
    var included by remember { mutableStateOf(seedRecipe?.includedUntaggedIds?.toSet() ?: emptySet()) }
    var acknowledged by remember { mutableStateOf(false) }
    var reviewing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var preview by remember { mutableStateOf<UnskippableMix.Preview?>(null) }
    var name by remember { mutableStateOf("") }
    var autoName by remember { mutableStateOf("") }
    var genreMenu by remember { mutableStateOf(false) }

    fun make() {
        val seed = "${System.currentTimeMillis().toString(36)}-${(Math.random() * 1e9).toLong().toString(36)}"
        val next = UnskippableMix.generate(items, UnskippableMix.Recipe(style = style, genre = genre, targetMinutes = minutes, seed = seed, includedUntaggedIds = included.toList()))
        if (name.isBlank() || name == autoName) {
            autoName = uniqueName(if (next.recipe.style == "rigid") "Unskippable · ${next.recipe.genre}" else "Unskippable · Flow")
            name = autoName
        }
        preview = next
    }

    BackHandler { if (preview != null) preview = null else onDismiss() }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (preview == null) "UNSKIPPABLE" else "PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(preview?.let { recipeLine(it.recipe) } ?: "Make an unskippable mix", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            val current = preview
            if (current == null) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("It plays start to finish. No skipping or seeking until it ends or you leave.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StyleCard("One genre", "Stays in one genre. A few close neighbours keep it from repeating itself.", style == "rigid", catalog.genres.isNotEmpty()) { style = "rigid"; if (genre == null) genre = catalog.genres.firstOrNull()?.label }
                            StyleCard("Flow mix", "A general mix from your whole library, ordered by Flow.", style == "flow", true) { style = "flow" }
                            if (catalog.genres.isEmpty()) Text("None of your songs have a genre tag yet, so One genre is off.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (style == "rigid") {
                        item {
                            Box {
                                OutlinedButton(onClick = { genreMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                    val count = catalog.genres.firstOrNull { it.label == genre }?.count
                                    Text("Genre: ${genre ?: "Pick one"}${count?.let { " ($it)" } ?: ""}")
                                }
                                DropdownMenu(expanded = genreMenu, onDismissRequest = { genreMenu = false }) {
                                    catalog.genres.forEach { option ->
                                        DropdownMenuItem(text = { Text("${option.label} (${option.count})") }, onClick = { genre = option.label; genreMenu = false })
                                    }
                                }
                            }
                        }
                        if (catalog.untaggedTrackIds.isNotEmpty()) {
                            item {
                                UntaggedReview(
                                    untaggedIds = catalog.untaggedTrackIds, byId = byId, genre = genre, included = included,
                                    acknowledged = acknowledged, reviewing = reviewing, selected = selected,
                                    onToggleReview = { reviewing = !reviewing },
                                    onSelect = { id, on -> selected = if (on) selected + id else selected - id },
                                    onSelectAll = { selected = if (selected.size == catalog.untaggedTrackIds.size) emptySet() else catalog.untaggedTrackIds.toSet() },
                                    onTag = { val ids = selected.toList(); genre?.let { onTagGenre(ids, it) }; included = included - ids.toSet(); selected = emptySet(); acknowledged = true },
                                    onInclude = { included = included + selected; selected = emptySet(); acknowledged = true },
                                    onLeave = { acknowledged = true; reviewing = false; selected = emptySet() }
                                )
                            }
                        }
                    } else {
                        item { Text("Flow uses genre only as a hint, so songs without a genre tag can still show up.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Length", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                UnskippableMix.LENGTHS.forEach { option ->
                                    FilterChip(selected = !custom && minutes == option, onClick = { custom = false; minutes = option }, label = { Text(formatMixDuration(option * 60000L)) })
                                }
                                FilterChip(selected = custom, onClick = { custom = true }, label = { Text("Custom") })
                            }
                            if (custom) {
                                var text by remember { mutableStateOf(minutes.toString()) }
                                OutlinedTextField(value = text, onValueChange = { value ->
                                    text = value.filter { it.isDigit() }.take(3)
                                    text.toIntOrNull()?.let { minutes = it.coerceIn(UnskippableMix.MIN_MINUTES, UnskippableMix.MAX_MINUTES) }
                                }, label = { Text("Minutes (${UnskippableMix.MIN_MINUTES}–${UnskippableMix.MAX_MINUTES})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = ::make, enabled = style == "flow" || genre != null) { Text("Make mix") }
                }
            } else {
                OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("${songs(current.trackIds.size)} · ${formatMixDuration(current.durationMs)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                if (current.shortfallMs > current.recipe.targetMinutes * 60000L / 20) {
                    Text(if (current.recipe.style == "rigid") "There's only ${formatMixDuration(current.durationMs)} of ${current.recipe.genre} to use, so the mix is shorter than the requested length."
                        else "Your library only fills ${formatMixDuration(current.durationMs)}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (current.recipe.style == "rigid" && catalog.untaggedTrackIds.isNotEmpty()) {
                    val added = current.recipe.includedUntaggedIds.size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${if (added > 0) "${songs(added)} without a tag added by hand. " else ""}${songs(catalog.untaggedTrackIds.size - added)} without a tag left out.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = { preview = null; reviewing = true }) { Text("Review") }
                    }
                }
                if (current.trackIds.isEmpty()) Text("Nothing to play yet. Go back and pick another genre or style.", style = MaterialTheme.typography.bodySmall)
                val neighbours = current.neighbourIds.toSet()
                LazyColumn(Modifier.weight(1f).padding(top = 6.dp)) {
                    itemsIndexed(current.trackIds, key = { _, id -> id }) { index, id ->
                        val track = byId[id]
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(track?.title ?: "Untitled", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(listOfNotNull(track?.artist?.ifBlank { null }, if (id in neighbours) track?.genre?.ifBlank { null } ?: "Close genre" else null).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { UnskippableMix.swap(items, current, id)?.let { preview = it } }, modifier = Modifier.semantics { contentDescription = "Swap ${track?.title ?: "this song"}" }) { Text("Swap") }
                            TextButton(onClick = { UnskippableMix.remove(items, current, id)?.let { preview = it } }, modifier = Modifier.semantics { contentDescription = "Remove ${track?.title ?: "this song"}" }) { Text("Remove") }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { preview = null }) { Text("Back") }
                    TextButton(onClick = ::make) { Text("Make another") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(uniqueName(name.trim().ifBlank { autoName }), current) }, enabled = current.trackIds.isNotEmpty()) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun StyleCard(title: String, detail: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Songs with no genre tag are never dropped silently: tag them, add them to this mix only, or leave them out. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UntaggedReview(
    untaggedIds: List<String>,
    byId: Map<String, Track>,
    genre: String?,
    included: Set<String>,
    acknowledged: Boolean,
    reviewing: Boolean,
    selected: Set<String>,
    onToggleReview: () -> Unit,
    onSelect: (String, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onTag: () -> Unit,
    onInclude: () -> Unit,
    onLeave: () -> Unit
) {
    val count = untaggedIds.size
    val status = when {
        included.isNotEmpty() -> "${songs(count)} have no genre tag. You added ${included.size} to this mix; the rest stay out."
        acknowledged -> "${songs(count)} have no genre tag and stay out of this mix."
        else -> "${songs(count)} have no genre tag, so a one-genre mix can't place them."
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (acknowledged || included.isNotEmpty()) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary),
        color = Color.Transparent
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(status, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onToggleReview) { Text(if (reviewing) "Hide untagged songs" else "Review untagged songs") }
            if (reviewing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSelectAll) { Text(if (selected.size == count) "Clear selection" else "Select all") }
                    Text("${selected.size} selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.fillMaxWidth()) {
                    untaggedIds.take(200).forEach { id ->
                        val track = byId[id] ?: return@forEach
                        Row(Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { onSelect(id, id !in selected) }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = id in selected, onCheckedChange = { onSelect(id, it) })
                            Column(Modifier.weight(1f)) {
                                Text(track.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist.ifBlank { "Unknown artist" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            if (id in included) Text("In this mix", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (count > 200) Text("Showing the first 200. Select all covers every one of the $count.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onTag, enabled = selected.isNotEmpty() && genre != null) { Text("Tag selected as ${genre ?: "genre"}") }
                    OutlinedButton(onClick = onInclude, enabled = selected.isNotEmpty()) { Text("Add to this mix only") }
                    TextButton(onClick = onLeave) { Text("Leave them out") }
                }
                Text("Tagging saves the genre to those songs on this phone. Adding them to this mix only leaves their tags alone.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * The cover a saved mix plays from. Artwork fills most of the screen with a small
 * equalizer underneath; Pause is the only transport control. Back or Leave pauses the
 * mix and keeps your place.
 */
/** How the cover lays out the sharp artwork and the song details over the blurred backdrop. */
enum class CoverLayout(val id: String, val label: String) {
    Theme("theme", "Theme default"),
    Stacked("stacked", "Artwork, details below"),
    Side("side", "Artwork and details side by side"),
    Art("art", "Artwork only");

    companion object {
        fun of(id: String?): CoverLayout = entries.firstOrNull { it.id == id } ?: Theme

        /** The listener's pick, or the theme's; without either, stack in portrait and sit side by side in landscape. */
        fun resolve(preference: CoverLayout, themeLayout: CoverLayout, landscape: Boolean): CoverLayout = when {
            preference != Theme -> preference
            themeLayout != Theme -> themeLayout
            landscape -> Side
            else -> Stacked
        }
    }
}

/** The cover layout is a device-wide preference, like on desktop. */
object CoverDisplayPrefs {
    private const val FILE = "pixelody_unskippable_display"
    private const val KEY = "layout"
    fun load(context: Context): CoverLayout = CoverLayout.of(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null))
    fun save(context: Context, layout: CoverLayout) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, layout.id).apply()
    }
}

/**
 * A theme's own cover look. It applies on Theme default only; picking a layout
 * from Display gives the plain cover in that layout.
 */
enum class CoverLook(val portrait: CoverLayout, val landscape: CoverLayout) {
    /** Art in a cartridge shell over an RPG dialogue box, XP-bar progress, pixel controls. */
    CartridgeQuest(CoverLayout.Stacked, CoverLayout.Side),
    /** A thriller poster: hard-edged art, oversized bone-white title, crimson rule and controls. */
    Obsession(CoverLayout.Stacked, CoverLayout.Side),
    /** Rain on the glass, the art as a tilted polaroid and the song on an index card. */
    LoFiCafe(CoverLayout.Stacked, CoverLayout.Side);

    companion object {
        fun of(theme: PixelodyMobileTheme): CoverLook? = when (theme) {
            PixelodyMobileTheme.CartridgeQuest -> CartridgeQuest
            PixelodyMobileTheme.Obsession -> Obsession
            PixelodyMobileTheme.LoFiCafe -> LoFiCafe
            else -> null
        }

        fun active(preference: CoverLayout, theme: PixelodyMobileTheme): CoverLook? = if (preference == CoverLayout.Theme) of(theme) else null
    }
}

/** Colours, shapes and type for one cover look; Plain is the standard cover. */
private data class CoverStyle(
    val backdropAlpha: Float = 0.5f,
    val backdropTint: Color = Color.Transparent,
    val background: Color = Color(0xFF060608),
    val artShape: Shape = RoundedCornerShape(14.dp),
    val artFrame: Color? = null,
    val artFramePadding: Dp = 0.dp,
    val artTilt: Float = 0f,
    val cardColor: Color? = null,
    val cardBorder: BorderStroke? = null,
    val cardShape: Shape = RoundedCornerShape(0.dp),
    val cardTilt: Float = 0f,
    val title: Color = Color.White,
    val sub: Color = Color.White.copy(alpha = 0.72f),
    val faint: Color = Color.White.copy(alpha = 0.6f),
    val titleSize: TextUnit = 20.sp,
    val titleUpper: Boolean = false,
    val titleItalic: Boolean = false,
    val font: FontFamily? = null,
    val positionWord: String = "Song",
    val track: Color = Color.White.copy(alpha = 0.2f),
    val bar: Color? = null,
    val barHeight: Dp = 3.dp,
    val buttonShape: Shape = CircleShape,
    val buttonColor: Color? = null,
    val buttonText: Color? = null,
    val buttonBorder: BorderStroke? = null,
    val eq: Color? = null,
    val eqBarWidth: Dp = 4.dp,
    val eqShape: Shape = RoundedCornerShape(2.dp),
    val eqSpeedMs: Int = 1400,
    val label: Color? = null,
    val sideRule: Color? = null
) {
    companion object {
        val Plain = CoverStyle()

        fun of(look: CoverLook?): CoverStyle = when (look) {
            null -> Plain
            CoverLook.CartridgeQuest -> CoverStyle(
                backdropAlpha = 0.45f, backdropTint = Color(0xAA10131F), background = Color(0xFF10131F),
                artShape = RectangleShape, artFrame = Color(0xFF77727B), artFramePadding = 12.dp,
                cardColor = Color(0xFF1B2140), cardBorder = BorderStroke(3.dp, Color(0xFFF2F2F2)),
                title = Color(0xFFF2C94C), sub = Color(0xFFF2F2F2), faint = Color(0xFF9AA3C7), titleSize = 17.sp, titleUpper = true,
                font = FontFamily.Monospace, positionWord = "STAGE",
                track = Color(0xFF05060B), bar = Color(0xFF65D6A1), barHeight = 10.dp,
                buttonShape = RectangleShape, buttonColor = Color(0xFFF2C94C), buttonText = Color(0xFF10131F),
                eq = Color(0xFF7B68C9), eqBarWidth = 7.dp, eqShape = RectangleShape, eqSpeedMs = 1000, label = Color(0xFFF2C94C)
            )
            CoverLook.Obsession -> CoverStyle(
                backdropAlpha = 0.3f, backdropTint = Color(0xB3120204), background = Color(0xFF030203),
                artShape = RectangleShape,
                title = Color(0xFFECE6DC), sub = Color(0xFFFF5865), faint = Color(0xFFECE6DC).copy(alpha = 0.55f), titleSize = 30.sp, titleUpper = true,
                track = Color(0xFFECE6DC).copy(alpha = 0.2f), bar = Color(0xFFC91527), barHeight = 1.dp,
                buttonShape = RectangleShape, buttonColor = Color.Transparent, buttonText = Color(0xFFECE6DC), buttonBorder = BorderStroke(1.dp, Color(0xFFC91527)),
                eq = Color(0xFFC91527), eqBarWidth = 2.dp, eqShape = RectangleShape, eqSpeedMs = 900, label = Color(0xFFC91527), sideRule = Color(0xFFC91527)
            )
            CoverLook.LoFiCafe -> CoverStyle(
                backdropAlpha = 0.45f, backdropTint = Color(0x80302014), background = Color(0xFF100B08),
                artShape = RoundedCornerShape(2.dp), artFrame = Color(0xFFEFE4CF), artFramePadding = 10.dp, artTilt = -1.6f,
                cardColor = Color(0xFFD8C49E), cardShape = RoundedCornerShape(4.dp), cardTilt = 0.8f,
                title = Color(0xFF211812), sub = Color(0xFF6F5B47), faint = Color(0xFF6F5B47), titleItalic = true, font = FontFamily.Serif,
                track = Color(0xFF6F5B47).copy(alpha = 0.25f), bar = Color(0xFFC48A4A),
                buttonColor = Color(0xFFC48A4A), buttonText = Color(0xFF211812),
                eq = Color(0xFFC48A4A).copy(alpha = 0.6f), eqSpeedMs = 2600, label = Color(0xFFE1B775)
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun UnskippableCover(
    mix: UnskippableMixRecord,
    trackById: (String) -> Track?,
    currentTrackId: String?,
    active: Boolean,
    finished: Boolean,
    isPlaying: Boolean,
    progress: () -> Float,
    onToggle: () -> Unit,
    onRestart: () -> Unit,
    onLeave: () -> Unit,
    themeLayout: CoverLayout = CoverLayout.Theme
) {
    val theme = LocalPixelodyThemeVariant.current
    val context = LocalContext.current
    val view = LocalView.current
    var preference by rememberSaveable { mutableStateOf(CoverDisplayPrefs.load(context)) }
    var fullScreen by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var idle by remember { mutableStateOf(false) }

    // Back steps out of full screen first, then leaves the mix.
    BackHandler { if (fullScreen) fullScreen = false else onLeave() }

    // Full screen hides the status and navigation bars; a swipe from the edge
    // shows them for a moment. They always come back when the cover closes.
    DisposableEffect(fullScreen) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (fullScreen) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else controller?.show(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val look = CoverLook.active(preference, theme)
    val style = CoverStyle.of(look)
    val layout = CoverLayout.resolve(preference, look?.let { if (landscape) it.landscape else it.portrait } ?: themeLayout, landscape)
    val fades = fullScreen || layout == CoverLayout.Art
    val playingNow = active && isPlaying && !finished
    // Controls fade after a few quiet seconds in full screen or artwork-only; a tap brings them back.
    LaunchedEffect(lastTouch, fades, playingNow, menuOpen) {
        idle = false
        if (fades && playingNow && !menuOpen) { delay(3000); idle = true }
    }
    val controlsAlpha by animateFloatAsState(if (idle) 0f else 1f, tween(400), label = "cover-controls")

    val showingId = if (active) currentTrackId else
        UnskippableMix.resumePoint(mix.trackIds, mix.progressTrackId, mix.progressPositionMs) { id -> trackById(id)?.missing == false }?.trackId ?: mix.trackIds.firstOrNull()
    val track = showingId?.let(trackById)
    val index = showingId?.let { mix.trackIds.indexOf(it) } ?: -1

    val playButton: @Composable () -> Unit = {
        Button(onClick = { lastTouch = System.currentTimeMillis(); if (finished) onRestart() else onToggle() }, shape = style.buttonShape, enabled = !idle,
            colors = if (style.buttonColor != null) ButtonDefaults.buttonColors(containerColor = style.buttonColor, contentColor = style.buttonText ?: Color.White) else ButtonDefaults.buttonColors(),
            border = style.buttonBorder,
            modifier = Modifier.height(56.dp).graphicsLayer { alpha = controlsAlpha }
                .semantics { contentDescription = if (finished) "Play again from the start" else if (active && isPlaying) "Pause" else "Play" }) {
            Text(if (finished) "Play again" else if (active && isPlaying) "Pause" else "Play", fontWeight = FontWeight.Bold, fontFamily = style.font)
        }
    }
    val details: @Composable (alignStart: Boolean, barWidth: Dp?) -> Unit = { alignStart, barWidth ->
        val align = if (alignStart) TextAlign.Start else TextAlign.Center
        val titleText = if (finished) "That's the whole mix." else track?.title ?: "Untitled"
        val card = style.cardColor
        val cardModifier = if (card != null) Modifier
            .then(if (barWidth != null) Modifier.width(barWidth) else Modifier)
            .graphicsLayer { rotationZ = style.cardTilt }
            .clip(style.cardShape).background(card)
            .then(if (style.cardBorder != null) Modifier.border(style.cardBorder, style.cardShape) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
        else if (style.sideRule != null && alignStart) Modifier.drawBehind { drawRect(style.sideRule, size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height)) }.padding(start = 16.dp)
        else Modifier
        Column(horizontalAlignment = if (alignStart) Alignment.Start else Alignment.CenterHorizontally) {
            Column(cardModifier, horizontalAlignment = if (alignStart || style.font == FontFamily.Monospace) Alignment.Start else Alignment.CenterHorizontally) {
                val textAlign = if (style.font == FontFamily.Monospace) TextAlign.Start else align
                Text(if (style.titleUpper) titleText.uppercase() else titleText, color = style.title, fontSize = style.titleSize,
                    fontWeight = if (look == CoverLook.Obsession) FontWeight.Black else FontWeight.SemiBold, fontFamily = style.font,
                    fontStyle = if (style.titleItalic) FontStyle.Italic else FontStyle.Normal, lineHeight = style.titleSize * 1.05f,
                    maxLines = if (alignStart || look == CoverLook.Obsession) 3 else 1, overflow = TextOverflow.Ellipsis, textAlign = textAlign)
                if (!finished) {
                    Text(if (look == CoverLook.Obsession) track?.artist.orEmpty().uppercase() else track?.artist.orEmpty(), color = style.sub, fontFamily = style.font,
                        letterSpacing = if (look == CoverLook.Obsession) 3.sp else TextUnit.Unspecified,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = textAlign)
                    if (index >= 0) Text("${style.positionWord} ${index + 1} of ${mix.trackIds.size}${if (!active && mix.progressTrackId != null) " · picks up where you left off" else ""}",
                        style = MaterialTheme.typography.labelSmall, color = style.faint, fontFamily = style.font, textAlign = textAlign)
                    if (card != null) {
                        Spacer(Modifier.height(10.dp))
                        CoverProgress(active, progress, style, Modifier.fillMaxWidth())
                    }
                }
            }
            if (!finished && card == null) {
                Spacer(Modifier.height(10.dp))
                CoverProgress(active, progress, style, if (barWidth != null) Modifier.width(barWidth) else Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.align(if (card != null && !alignStart) Alignment.CenterHorizontally else if (alignStart) Alignment.Start else Alignment.CenterHorizontally)) { playButton() }
        }
    }
    val artwork: @Composable (Dp) -> Unit = { side ->
        val frame = style.artFrame
        Box(
            Modifier.graphicsLayer { rotationZ = style.artTilt }
                .then(if (frame != null) Modifier.background(frame, style.artShape).padding(start = style.artFramePadding, top = style.artFramePadding, end = style.artFramePadding,
                    bottom = style.artFramePadding * (if (look == CoverLook.LoFiCafe) 3.5f else if (look == CoverLook.CartridgeQuest) 2f else 1f)) else Modifier)
        ) {
            RemoteArtwork(artworkUrl = track?.artworkUrl, title = track?.title ?: mix.name,
                modifier = Modifier.size(side).clip(style.artShape)
                    .then(if (look == CoverLook.Obsession) Modifier.border(1.dp, Color(0x55ECE6DC)) else Modifier)
                    .then(if (layout == CoverLayout.Art) Modifier.semantics { contentDescription = track?.title ?: mix.name } else Modifier))
            if (look == CoverLook.CartridgeQuest) Text("PIXELODY", color = Color(0xFF34343D), fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 3.sp,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 18.dp))
        }
    }

    Box(
        Modifier.fillMaxSize().background(style.background)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent(PointerEventPass.Initial); lastTouch = System.currentTimeMillis() } } }
    ) {
        // The same artwork, blurred, fills the screen behind the sharp copy.
        RemoteArtwork(artworkUrl = track?.artworkUrl, title = track?.title ?: mix.name,
            modifier = Modifier.fillMaxSize().blur(48.dp).graphicsLayer { alpha = style.backdropAlpha })
        Box(Modifier.fillMaxSize().background(style.backdropTint))
        CoverLookOverlay(look)
        Column(
            Modifier.fillMaxSize().then(if (fullScreen) Modifier else Modifier.statusBarsPadding().navigationBarsPadding()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).graphicsLayer { alpha = controlsAlpha }, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onLeave, enabled = !idle) { Text("Leave", color = Color.White) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("UNSKIPPABLE", style = MaterialTheme.typography.labelSmall, color = style.label ?: MaterialTheme.colorScheme.primary, fontFamily = style.font)
                    Text(mix.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${recipeLine(mix.recipe)} · ${songs(mix.trackIds.size)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    TextButton(onClick = { menuOpen = true }, enabled = !idle) { Text("Display", color = Color.White) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        CoverLayout.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                leadingIcon = { RadioButton(selected = option == preference, onClick = null) },
                                onClick = { preference = option; CoverDisplayPrefs.save(context, option); menuOpen = false },
                                modifier = Modifier.semantics { stateDescription = if (option == preference) "Selected" else "Not selected" }
                            )
                        }
                    }
                }
                TextButton(onClick = { fullScreen = !fullScreen }, enabled = !idle) {
                    Text(if (fullScreen) "Exit full screen" else "Full screen", color = Color.White)
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (layout) {
                    CoverLayout.Side -> {
                        val framed = if (style.artFrame != null) style.artFramePadding * 4 else 0.dp
                        val side = minOf(maxWidth * 0.48f, maxHeight - framed).coerceAtLeast(56.dp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            artwork(side)
                            Box(Modifier.weight(1f)) { details(true, null) }
                        }
                    }
                    CoverLayout.Art -> {
                        val side = minOf(maxWidth, maxHeight - 80.dp).coerceAtLeast(56.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            artwork(side)
                            Spacer(Modifier.height(16.dp))
                            playButton()
                        }
                    }
                    else -> {
                        // Leave room under the art for the title, progress line and Pause.
                        // Themed looks frame the art and set the song on a card, so they need more room.
                        val room = when (look) { null -> 190.dp; CoverLook.Obsession -> 250.dp; else -> 260.dp }
                        val framed = if (style.artFrame != null) style.artFramePadding * 2 else 0.dp
                        val side = (minOf(maxWidth - framed, maxHeight - room)).coerceAtLeast(56.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            artwork(side)
                            Spacer(Modifier.height(14.dp))
                            details(false, side)
                        }
                    }
                }
            }
            if (layout != CoverLayout.Art) CoverEqualizer(playing = playingNow, style = style, modifier = Modifier.padding(bottom = 18.dp))
        }
    }
}

/** Read-only progress: there is no seeking in an unskippable mix. */
@Composable
private fun CoverProgress(active: Boolean, progress: () -> Float, style: CoverStyle, modifier: Modifier) {
    LinearProgressIndicator(progress = { if (active) progress() else 0f },
        modifier = modifier.height(style.barHeight).semantics { contentDescription = "Song progress" },
        color = style.bar ?: MaterialTheme.colorScheme.primary, trackColor = style.track,
        strokeCap = if (style.buttonShape == CircleShape) StrokeCap.Round else StrokeCap.Butt,
        gapSize = 0.dp, drawStopIndicator = {})
}

/** Scanlines for Cartridge Quest, fracture lines for Obsession, slow rain for Lo-Fi Café. Decorative only. */
@Composable
private fun CoverLookOverlay(look: CoverLook?) {
    when (look) {
        null -> Unit
        CoverLook.CartridgeQuest -> Canvas(Modifier.fillMaxSize()) {
            val step = 4.dp.toPx()
            var y = 0f
            while (y < size.height) { drawRect(Color(0x38000000), topLeft = Offset(0f, y + step / 2), size = androidx.compose.ui.geometry.Size(size.width, step / 2)); y += step }
        }
        CoverLook.Obsession -> Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(Color(0x55C91527), Color.Transparent), center = Offset(size.width * 0.2f, size.height * 0.6f), radius = size.maxDimension * 0.6f))
            val crack = Color(0x33C91527)
            val w = size.width; val h = size.height
            listOf(
                Offset(0.06f * w, 0.22f * h) to Offset(0.42f * w, 0.30f * h), Offset(0.42f * w, 0.30f * h) to Offset(0.63f * w, 0.28f * h),
                Offset(0.63f * w, 0.28f * h) to Offset(0.76f * w, 0.43f * h), Offset(0.76f * w, 0.43f * h) to Offset(0.95f * w, 0.39f * h),
                Offset(0.42f * w, 0.30f * h) to Offset(0.55f * w, 0.70f * h), Offset(0.55f * w, 0.70f * h) to Offset(0.72f * w, 0.78f * h)
            ).forEach { (a, b) -> drawLine(crack, a, b, strokeWidth = 1.5.dp.toPx()) }
        }
        CoverLook.LoFiCafe -> {
            val transition = rememberInfiniteTransition(label = "unskippable-rain")
            val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "rain")
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(listOf(Color(0x40C48A4A), Color.Transparent), center = Offset(size.width / 2, size.height * 1.1f), radius = size.maxDimension * 0.6f))
                val gap = 38.dp.toPx()
                val shift = drift * size.height
                var x = -size.height * 0.2f
                while (x < size.width) {
                    var y = (shift % (gap * 6)) - gap * 6
                    while (y < size.height) {
                        drawLine(Color(0x12FFFFFF), Offset(x, y), Offset(x + gap * 0.18f, y + gap * 1.4f), strokeWidth = 1.dp.toPx())
                        y += gap * 2.2f
                    }
                    x += gap
                }
            }
        }
    }
}

/** A small decorative equalizer. It moves while music plays and rests when paused. */
@Composable
private fun CoverEqualizer(playing: Boolean, style: CoverStyle = CoverStyle.Plain, modifier: Modifier = Modifier) {
    val bars = 20
    val transition = rememberInfiniteTransition(label = "unskippable-eq")
    val phase by transition.animateFloat(0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(style.eqSpeedMs, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val color = style.eq ?: MaterialTheme.colorScheme.primary
    Row(modifier.height(32.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        repeat(bars) { i ->
            val level = if (!playing) 0.1f else (0.35f + 0.3f * kotlin.math.sin(phase * (1f + i % 4 * 0.37f) + i * 0.9f) + 0.25f * kotlin.math.sin(phase * 2.3f + i * 1.7f)).coerceIn(0.08f, 1f)
            Box(Modifier.width(style.eqBarWidth).fillMaxHeight(level).clip(style.eqShape).background(color))
        }
    }
}
