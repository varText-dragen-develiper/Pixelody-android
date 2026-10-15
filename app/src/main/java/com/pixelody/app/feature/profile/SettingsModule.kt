package com.pixelody.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class SettingsCategory(val label: String, val description: String) {
    Appearance("Appearance", "Themes, colors, text and your own backgrounds."),
    Storage("Storage", "Downloads, offline music and space on this phone."),
    Connection("Connection", "Your desktop, music sources and listening shortcuts."),
    Extras("Extras", "Optional modules to extend your studio.")
}

/** A section owns its list content; the host only handles grouping and navigation.
 * Use stable, unique section IDs; the host supplies list keys. Keep storage and callbacks in the feature.
 */
internal data class SettingsModule(
    val id: String,
    val category: SettingsCategory,
    val visible: Boolean = true,
    val content: @Composable () -> Unit
)

internal fun LazyListScope.renderSettingsModules(modules: List<SettingsModule>, category: SettingsCategory) {
    require(modules.map { it.id }.distinct().size == modules.size) { "Settings section IDs must be unique" }
    items(modules.filter { it.category == category && it.visible }, key = { it.id }) { module ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { module.content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsNavigation(category: SettingsCategory, onSelect: (SettingsCategory) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCategory.entries.forEach { option ->
                FilterChip(
                    selected = option == category,
                    onClick = { onSelect(option) },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = option == category,
                        selectedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
        Text(category.description, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
