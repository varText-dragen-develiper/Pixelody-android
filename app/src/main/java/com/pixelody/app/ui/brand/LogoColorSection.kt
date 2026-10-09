package com.pixelody.app.ui.brand

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelody.app.R
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.theme.PixelodyMobileTheme

/** The Pixelody mark (Sampled Disc) in any colour. */
@Composable
fun PixelodyMark(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.splash_brand_emblem),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier.size(size)
    )
}

/** Explicit logo colors override the effective theme/palette accent. */
fun resolveLogoMarkColor(
    logoColor: PixelodyLogoColor,
    followsTheme: Boolean,
    activeTheme: PixelodyMobileTheme,
    themeAccent: Color = activeTheme.accentColor
): Color = if (followsTheme) themeAccent else logoColor.color

@Composable
internal fun LogoColorSection(
    settingsStore: MobileSettingsStore,
    activeTheme: PixelodyMobileTheme
) {
    val context = LocalContext.current
    var logoColor by remember { mutableStateOf(settingsStore.loadLogoColor()) }
    var followsTheme by remember { mutableStateOf(settingsStore.loadLogoFollowsTheme()) }
    val markColor = resolveLogoMarkColor(logoColor, followsTheme, activeTheme, MaterialTheme.colorScheme.primary)
    val followingTheme = followsTheme

    SectionCard(
        title = "Logo color",
        subtitle = if (followingTheme) "Following theme and palette" else "${logoColor.displayName} / app and home-screen icon"
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelodyMark(color = markColor, size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "The home-screen icon follows your theme and palette. Custom colors use the closest available shade. Android themed icons can follow your wallpaper instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PixelodyLogoColor.entries.forEach { option ->
                val selected = !followsTheme && option == logoColor
                Surface(
                    onClick = {
                        logoColor = option
                        followsTheme = false
                        settingsStore.saveLogoColor(option)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.RadioButton
                            this.selected = selected
                            contentDescription = "${option.displayName} logo"
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (selected) 0.9f else 0.45f),
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) option.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PixelodyMark(color = option.color, size = 28.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Follow theme and palette",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                modifier = Modifier.semantics { contentDescription = "Follow theme and palette" },
                checked = followsTheme,
                onCheckedChange = {
                    followsTheme = it
                    settingsStore.saveLogoFollowsTheme(it)
                }
            )
        }
    }
}
