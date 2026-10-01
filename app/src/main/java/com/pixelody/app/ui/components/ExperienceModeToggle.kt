package com.pixelody.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.AppExperienceMode
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick

/**
 * High-visibility, tactile toggle allowing instant switching between
 * Essential (Spotify-like streamlined simplicity) and Studio (audiophile DSP racks).
 */
@Composable
fun ExperienceModeToggle(
    mode: AppExperienceMode,
    onModeChange: (AppExperienceMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface,
        modifier = modifier) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AppExperienceMode.values().forEach { choice ->
                val selected = mode == choice
                Box(modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 100.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab,
                        onClick = { if (!selected) { haptic.performConfirm(); onModeChange(choice) } })
                    .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text(choice.label, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

/**
 * Compact single-chip toggle button ideal for headers with constrained space.
 */
@Composable
fun ExperienceModeQuickChip(
    mode: AppExperienceMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(onClick = { haptic.performConfirm(); onToggle() }, shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = "Switch listening mode, currently ${mode.label}" }) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(mode.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
