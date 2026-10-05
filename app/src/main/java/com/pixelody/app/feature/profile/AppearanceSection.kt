package com.pixelody.app.feature.profile

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceSection(settingsStore: MobileSettingsStore) {
    var style by remember { mutableStateOf(settingsStore.loadAppearance()) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var editingColors by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(settingsStore) {
        val unsubscribe = settingsStore.observeAppearance { style = it }
        onDispose { unsubscribe() }
    }
    fun update(value: AppearanceSettings) { style = value.normalized(); settingsStore.saveAppearance(style) }
    SectionCard(title = "Style", subtitle = "${style.palette.label} · ${style.textPercent}% text · ${style.typeface.label}") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.surfaceContainerHigh).forEach {
                Box(Modifier.size(24.dp).background(it, MaterialTheme.shapes.small))
            }
        }
        OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (expanded) "Close style controls" else "Customize style")
        }
        if (expanded) {
            Text("Palette", style = MaterialTheme.typography.titleSmall)
            Text("Desktop palettes for shared surfaces and controls. Each theme keeps its own artwork and motifs.", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StylePalette.values().forEach { palette ->
                    FilterChip(selected = style.palette == palette, onClick = {
                        if (palette == StylePalette.Custom) editingColors = true else update(style.copy(palette = palette))
                    }, label = { Text(palette.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = style.palette == palette, selectedBorderColor = MaterialTheme.colorScheme.primary), modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            OutlinedButton(onClick = { editingColors = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit custom colors") }
            Text("Text size", style = MaterialTheme.typography.titleSmall)
            Text("Adds to your phone's text size setting.", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(100, 115, 130, 150).forEach { percent ->
                    FilterChip(selected = style.textPercent == percent, onClick = { update(style.copy(textPercent = percent)) },
                        label = { Text("$percent%") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = style.textPercent == percent, selectedBorderColor = MaterialTheme.colorScheme.primary), modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            Text("Typeface", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StyleTypeface.values().forEach { typeface ->
                    FilterChip(selected = style.typeface == typeface, onClick = { update(style.copy(typeface = typeface)) },
                        label = { Text(typeface.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = style.typeface == typeface, selectedBorderColor = MaterialTheme.colorScheme.primary), modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("High contrast", modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                Switch(modifier = Modifier.semantics { contentDescription = "High contrast" }, checked = style.highContrast, onCheckedChange = { update(style.copy(highContrast = it)) })
            }
            Text("Listening comes first.", style = MaterialTheme.typography.bodyLarge)
            Text("Your colors and text update immediately and stay saved on this phone.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { settingsStore.resetAppearance(); style = settingsStore.loadAppearance() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Reset style to theme defaults") }
        }
    }
    if (editingColors) CustomColorsDialog(if (style.palette != StylePalette.Custom && style.palette != StylePalette.Theme) style.copy(primary = style.palette.primary, secondary = style.palette.secondary, base = style.palette.base) else style, onDismiss = { editingColors = false }, onApply = {
        update(it); editingColors = false
    })
}

@Composable
private fun CustomColorsDialog(style: AppearanceSettings, onDismiss: () -> Unit, onApply: (AppearanceSettings) -> Unit) {
    var primary by rememberSaveable { mutableStateOf(style.primary) }
    var secondary by rememberSaveable { mutableStateOf(style.secondary) }
    var base by rememberSaveable { mutableStateOf(style.base) }
    val p = normalizeHex(primary)
    val s = normalizeHex(secondary)
    val b = normalizeHex(base)
    val readable = b != null && isReadableBase(b)
    val primaryReadable = p != null && b != null && isReadableAccent(p, b)
    val secondaryReadable = s != null && b != null && isReadableAccent(s, b)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Custom colors") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Use six-digit hex colors, such as #91A7FF. A dark base keeps text readable.")
            OutlinedTextField(primary, { primary = it }, label = { Text("Primary") }, singleLine = true,
                isError = !primaryReadable, supportingText = { Text("Choose an accent that stands out from the base") }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Primary color hex" })
            OutlinedTextField(secondary, { secondary = it }, label = { Text("Secondary") }, singleLine = true,
                isError = !secondaryReadable, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Secondary color hex" })
            OutlinedTextField(base, { base = it }, label = { Text("Base") }, singleLine = true,
                isError = !readable, supportingText = { Text(if (b == null) "Enter six hex digits" else if (!readable) "Choose a darker base for readable text" else "Dark base · readable text") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Base color hex" })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOfNotNull(p, s, b).forEach { Box(Modifier.size(28.dp).background(hexColor(it), MaterialTheme.shapes.small)) }
            }
        }
    }, confirmButton = { TextButton(onClick = {
        if (p != null && s != null && b != null && readable && primaryReadable && secondaryReadable) onApply(style.copy(palette = StylePalette.Custom, primary = p, secondary = s, base = b))
    }, enabled = primaryReadable && secondaryReadable && readable) { Text("Apply colors") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
