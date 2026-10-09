package com.pixelody.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.SavedHostProfile
import com.pixelody.app.data.storage.DownloadQueueManager
import com.pixelody.app.data.storage.DownloadQueueState
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.data.storage.OfflineMediaStore
import com.pixelody.app.feature.connection.label
import com.pixelody.app.ui.brand.LogoColorSection
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.StatusChip
import com.pixelody.app.ui.theme.PixelodyMobileTheme

/* =========================================================================
 * Slice 4 — Profile Screen
 * Extracted from PixelodyShell.kt into com.pixelody.app.feature.profile.
 * ========================================================================= */

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ProfileScreen(
    onResetDeveloperData: (suspend (Boolean) -> Unit)? = null,
    covers: com.pixelody.app.data.model.CoverBook = com.pixelody.app.data.model.CoverBook(),
    coverStore: com.pixelody.app.data.storage.CoverStore? = null,
    onCoversChange: (com.pixelody.app.data.model.CoverBook) -> Unit = {},
    activeTheme: PixelodyMobileTheme,
    onThemeChange: (PixelodyMobileTheme) -> Unit,
    onThemeReset: () -> Unit,
    snapshot: LibrarySnapshot?,
    savedHost: SavedHostProfile?,
    connectionState: HostConnectionState,
    localTrackCount: Int,
    cachedTrackCount: Int = 0,
    offlineMediaStore: OfflineMediaStore,
    settingsStore: MobileSettingsStore,
    downloadQueueState: DownloadQueueState,
    downloadQueueManager: DownloadQueueManager,
    onClearOfflineCache: () -> Unit = {},
    onPruneUnpinned: () -> Unit = {},
    onClearArtwork: () -> Unit = {},
    onOpenDevice: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenTechnical: () -> Unit,
    onForgetSavedHost: () -> Unit
) {
    val moduleContext = androidx.compose.ui.platform.LocalContext.current
    var categoryId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(SettingsCategory.Appearance.name) }
    val category = SettingsCategory.entries.firstOrNull { it.name == categoryId } ?: SettingsCategory.Appearance
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(category) { listState.scrollToItem(0) }
    val modules = listOf(
        SettingsModule("themes", SettingsCategory.Appearance) {
            ThemesSection(activeTheme, onThemeChange, onThemeReset)
        },
        SettingsModule("style", SettingsCategory.Appearance) {
            AppearanceSection(settingsStore)
        },
        SettingsModule("backgrounds", SettingsCategory.Appearance) {
            BackgroundSection(covers, coverStore, onCoversChange)
        },
        SettingsModule("logo", SettingsCategory.Appearance) {
            Column {
                LogoColorSection(settingsStore = settingsStore, activeTheme = activeTheme)
            }
        },
        SettingsModule("downloads", SettingsCategory.Storage, visible = downloadQueueState.tasks.isNotEmpty()) {
            DownloadSection(activeTheme, downloadQueueState, downloadQueueManager)
        },
        SettingsModule("storage", SettingsCategory.Storage) {
            StorageSection(activeTheme, cachedTrackCount, offlineMediaStore, settingsStore, onClearOfflineCache, onPruneUnpinned, onClearArtwork)
        },
        SettingsModule("connection-status", SettingsCategory.Connection) {
            Column {
                SectionCard(
                    title = "Listening Profile",
                    subtitle = snapshot?.host?.hostName ?: savedHost?.hostName ?: "No host connected"
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { StatusChip(text = connectionState.label) }
                        item { StatusChip(text = "${snapshot?.tracks?.size ?: 0} host") }
                        item { StatusChip(text = "$localTrackCount phone") }
                        item { StatusChip(text = "$cachedTrackCount offline") }
                    }
                }
            }

        },
        SettingsModule("connection-tools", SettingsCategory.Connection) {
            Column {
                SectionCard(
                    title = "Music sources",
                    subtitle = "Add songs or connect your desktop."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onOpenDevice, shape = activeTheme.plate, modifier = Modifier.weight(1f)) { Text("Manage sources") }
                        OutlinedButton(onClick = onOpenQueue, shape = activeTheme.plate, modifier = Modifier.weight(1f)) { Text("Queue") }
                    }
                }
            }
            if (savedHost != null) {
                Column {
                    SectionCard(
                        title = "Saved Host",
                        subtitle = "${savedHost.hostName} / ${savedHost.baseUrl}"
                    ) {
                        OutlinedButton(onClick = onForgetSavedHost, shape = activeTheme.plate, modifier = Modifier.fillMaxWidth()) {
                            Text("Forget Saved Host")
                        }
                    }
                }
            }
        },
        SettingsModule("developer-reset", SettingsCategory.Extras, visible = com.pixelody.app.BuildConfig.DEBUG && onResetDeveloperData != null) {
            onResetDeveloperData?.let { DeveloperResetSection(it) }
        },
        SettingsModule("modules", SettingsCategory.Extras) {
            Column {
                Button(onClick = { moduleContext.startActivity(android.content.Intent(moduleContext, com.pixelody.app.modules.ModuleShopActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Modules — import or manage") }
            }

        }
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        stickyHeader(key = "settings-navigation") {
            Surface(color = MaterialTheme.colorScheme.background) {
                SettingsNavigation(category, onSelect = { categoryId = it.name })
            }
        }
        renderSettingsModules(modules, category)

    }
}
