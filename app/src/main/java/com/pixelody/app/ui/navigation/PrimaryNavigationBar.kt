package com.pixelody.app.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelody.app.ui.components.PixelodyNavGlyph
import com.pixelody.app.ui.components.performTick

@Composable
internal fun PrimaryNavigationBar(
    tabs: List<PixelodyTab>,
    selectedTab: PixelodyTab,
    onSelectTab: (PixelodyTab) -> Unit,
    showCreateAction: Boolean,
    onOpenCreate: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                modifier = Modifier.testTag(tab.navigationTestTag),
                selected = selectedTab == tab,
                onClick = {
                    haptic.performTick()
                    onSelectTab(tab)
                },
                label = { Text(tab.label) },
                icon = {
                    PixelodyNavGlyph(
                        tab = tab,
                        selected = selectedTab == tab
                    )
                }
            )
        }
        if (showCreateAction) {
            ContextualCreateBarAction(onClick = {
                haptic.performTick()
                onOpenCreate()
            })
        }
    }
}

@Composable
internal fun RowScope.ContextualCreateBarAction(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 6.dp, vertical = 10.dp)
            .height(58.dp)
            .clickable(
                onClickLabel = "Add music or host this phone",
                role = Role.Button,
                onClick = onClick
            ),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PixelodyNavGlyph(tab = PixelodyTab.Create, selected = true)
            Text(text = "Add", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun PrimaryNavigationRail(
    tabs: List<PixelodyTab>,
    selectedTab: PixelodyTab,
    onSelectTab: (PixelodyTab) -> Unit,
    onOpenProfile: () -> Unit,
    showCreateAction: Boolean,
    onOpenCreate: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        if (showCreateAction) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .sizeIn(minWidth = 64.dp, minHeight = 60.dp)
                    .clickable(
                        onClickLabel = "Add music or host this phone",
                        role = Role.Button,
                        onClick = {
                            haptic.performTick()
                            onOpenCreate()
                        }
                    ),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PixelodyNavGlyph(tab = PixelodyTab.Create, selected = true)
                    Text(text = "Add", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        tabs.forEach { tab ->
            NavigationRailItem(
                modifier = Modifier.testTag(tab.navigationTestTag),
                selected = selectedTab == tab,
                onClick = {
                    haptic.performTick()
                    onSelectTab(tab)
                },
                label = { Text(tab.label) },
                icon = {
                    PixelodyNavGlyph(
                        tab = tab,
                        selected = selectedTab == tab
                    )
                }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
            modifier = Modifier.testTag(PixelodyTab.Profile.navigationTestTag),
            selected = selectedTab == PixelodyTab.Profile,
            onClick = {
                haptic.performTick()
                onOpenProfile()
            },
            label = { Text("Profile") },
            icon = {
                PixelodyNavGlyph(
                    tab = PixelodyTab.Profile,
                    selected = selectedTab == PixelodyTab.Profile
                )
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
