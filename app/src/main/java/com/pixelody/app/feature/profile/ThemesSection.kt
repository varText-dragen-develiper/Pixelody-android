package com.pixelody.app.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.theme.PixelodyMobileTheme

@Composable
internal fun ThemesSection(activeTheme: PixelodyMobileTheme, onThemeChange: (PixelodyMobileTheme) -> Unit, onThemeReset: () -> Unit) {
    SectionCard(
        title = "Themes",
        subtitle = "${activeTheme.displayName} selected"
    ) {
        PixelodyMobileTheme.values().forEach { theme ->
            ThemeChoiceRow(
                theme = theme,
                selected = activeTheme == theme,
                onClick = { onThemeChange(theme) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(
            enabled = activeTheme != PixelodyMobileTheme.Studio,
            onClick = onThemeReset,
            shape = activeTheme.plate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use Studio theme")
        }
    }
}

@Composable
internal fun ThemeChoiceRow(
    theme: PixelodyMobileTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { this.selected = selected },
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = theme.plate,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) theme.accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(theme.plate)
                    .background(theme.accentColor)
                    .border(2.dp, if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), theme.plate),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = theme.badge.take(2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(theme.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    theme.summary,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = theme.accentColor,
                    shape = theme.plate
                ) {
                    Text(
                        text = "Selected",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
