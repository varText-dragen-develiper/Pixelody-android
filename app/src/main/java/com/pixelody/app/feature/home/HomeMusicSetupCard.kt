package com.pixelody.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal enum class HomeStartState { Ready, Loading, Connecting, OtherSource, Empty }

internal fun homeStartState(playableCount: Int, availableCount: Int, loading: Boolean, connecting: Boolean): HomeStartState = when {
    playableCount > 0 -> HomeStartState.Ready
    loading -> HomeStartState.Loading
    connecting -> HomeStartState.Connecting
    availableCount > 0 -> HomeStartState.OtherSource
    else -> HomeStartState.Empty
}

@Composable
internal fun HomeMusicSetupCard(state: HomeStartState, onAddMusic: () -> Unit, onShowAll: () -> Unit,
    onConnectDesktop: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().testTag("home:music-setup"), shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(when (state) {
                HomeStartState.Loading -> "Finding your music"
                HomeStartState.Connecting -> "Connecting your desktop"
                HomeStartState.OtherSource -> "Your music is in another source"
                else -> "Start with your music"
            }, style = MaterialTheme.typography.titleLarge)
            Text(when (state) {
                HomeStartState.Loading -> "Reading your saved library. Your music will appear here when it’s ready."
                HomeStartState.Connecting -> "You can also listen to files on this phone while your desktop connects."
                HomeStartState.OtherSource -> "This source has no playable songs. Show all your music or add files from this phone."
                else -> "Add songs stored on this phone, or connect your Pixelody desktop library. You can listen on your phone without a desktop."
            }, style = MaterialTheme.typography.bodyMedium)
            if (state == HomeStartState.Loading || state == HomeStartState.Connecting) CircularProgressIndicator()
            if (state != HomeStartState.Loading) {
                Button(onClick = if (state == HomeStartState.OtherSource) onShowAll else onAddMusic,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home:add-music")) {
                    Text(if (state == HomeStartState.OtherSource) "Show all music" else "Add music from this phone")
                }
                TextButton(onClick = onConnectDesktop, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Connect a desktop") }
            }
        }
    }
}
