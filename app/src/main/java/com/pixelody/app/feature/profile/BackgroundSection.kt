package com.pixelody.app.feature.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixelody.app.data.model.CoverBook
import com.pixelody.app.data.model.ScreenBackground
import com.pixelody.app.data.storage.CoverStore
import com.pixelody.app.ui.components.RemoteArtwork
import com.pixelody.app.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BackgroundSection(covers: CoverBook, store: CoverStore?, onChange: (CoverBook) -> Unit) {
    if (store == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentCovers by rememberUpdatedState(covers)
    var expanded by rememberSaveable { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val key = pending
        pending = null
        if (uri != null && key != null) scope.launch {
            importing = true
            try {
                val saved = withContext(Dispatchers.IO) { store.importImage(context.applicationContext, uri) }
                if (saved != null) onChange(currentCovers.withImage(key, saved))
                else Toast.makeText(context, "Couldn't read this image. Try another photo.", Toast.LENGTH_LONG).show()
            } finally { importing = false }
        }
    }
    SectionCard(title = "Screen backgrounds", subtitle = "Your own image for each screen") {
        OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (expanded) "Close background controls" else "Customize backgrounds")
        }
        if (expanded) {
            Text("Images stay on this phone. Each screen has its own choice, dimmed to keep music and controls readable.",
                style = MaterialTheme.typography.bodySmall)
            if (importing) Text("Saving image…")
            ScreenBackground.values().forEach { screen ->
                val image = covers.imageFor(screen.key, null)
                Text(screen.label, style = MaterialTheme.typography.titleSmall)
                if (image != null) RemoteArtwork(image, "${screen.label} background preview",
                    Modifier.fillMaxWidth().height(96.dp))
                OutlinedButton(onClick = { pending = screen.key; picker.launch("image/*") }, enabled = !importing,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (image == null) "Choose ${screen.label} background" else "Change ${screen.label} background")
                }
                if (image != null) TextButton(onClick = { onChange(currentCovers.withImage(screen.key, null)) },
                    enabled = !importing, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Reset ${screen.label} background")
                }
            }
        }
    }
}
