package com.pixelody.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.pixelody.app.ui.brand.BrandIconSwitcher
import com.pixelody.app.ui.brand.resolveLauncherIcon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pixelody.app.data.fixtures.FakePixelodyHost
import com.pixelody.app.data.model.HostConnectionDetails
import com.pixelody.app.data.repository.HostRepository
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.feature.baselayer.BasePlaybackHost
import com.pixelody.app.ui.navigation.PixelodyDeepLink
import com.pixelody.app.ui.navigation.PixelodyTab
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import com.pixelody.app.ui.theme.PixelodyTheme

class MainActivity : ComponentActivity() {
    private val incomingConnectionDetails = mutableStateOf<HostConnectionDetails?>(null)
    private val incomingDeepLink = mutableStateOf<PixelodyTab?>(null)
    private val incomingSearchQuery = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingConnectionDetails.value = HostConnectionDetails.fromUri(intent?.data)
        applyDeepLink(intent)

        setContent {
            val repository = remember { HostRepository(FakePixelodyHost()) }
            val settingsStore = remember { MobileSettingsStore(applicationContext) }
            val connectionDetails by incomingConnectionDetails
            val deepLinkDestination by incomingDeepLink
            val deepLinkSearchQuery by incomingSearchQuery
            var mobileTheme by remember { mutableStateOf(settingsStore.loadTheme()) }

            var appearance by remember { mutableStateOf(settingsStore.loadAppearance()) }
            DisposableEffect(settingsStore) {
                val unsubscribe = settingsStore.observeAppearance { appearance = it }
                onDispose { unsubscribe() }
            }
            var logoPreferences by remember { mutableStateOf(settingsStore.loadLogoPreferences()) }
            DisposableEffect(settingsStore) {
                val unsubscribe = settingsStore.observeLogoPreferences { logoPreferences = it }
                onDispose { unsubscribe() }
            }
            LaunchedEffect(mobileTheme, appearance, logoPreferences) {
                BrandIconSwitcher.apply(applicationContext, resolveLauncherIcon(logoPreferences, mobileTheme, appearance))
            }
            PixelodyTheme(variant = mobileTheme, appearance = appearance) {
                BasePlaybackHost(
                    repository = repository,
                    incomingConnectionDetails = connectionDetails,
                    onConnectionDetailsConsumed = {
                        incomingConnectionDetails.value = null
                        setIntent(Intent(intent).setData(null))
                    },
                    incomingDeepLink = deepLinkDestination,
                    incomingSearchQuery = deepLinkSearchQuery,
                    onDeepLinkConsumed = {
                        incomingDeepLink.value = null
                        incomingSearchQuery.value = ""
                        setIntent(Intent(intent).setData(null))
                    },
                    activeTheme = mobileTheme,
                    onThemeChange = { theme ->
                        mobileTheme = theme
                        settingsStore.saveTheme(theme)
                    },
                    onThemeReset = {
                        settingsStore.resetTheme()
                        mobileTheme = PixelodyMobileTheme.Studio
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingConnectionDetails.value = HostConnectionDetails.fromUri(intent.data)
        applyDeepLink(intent)
    }

    private fun applyDeepLink(intent: Intent?) {
        val uri = intent?.data
        if (uri == null) {
            incomingDeepLink.value = null
            incomingSearchQuery.value = ""
            return
        }
        val target = PixelodyDeepLink.targetFor(
            scheme = uri.scheme,
            host = uri.host,
            searchQuery = runCatching {
                uri.getQueryParameter(PixelodyDeepLink.SEARCH_QUERY_PARAMETER)
            }.getOrNull()
        )
        incomingDeepLink.value = target?.destination
        incomingSearchQuery.value = target?.searchQuery.orEmpty()
    }
}
