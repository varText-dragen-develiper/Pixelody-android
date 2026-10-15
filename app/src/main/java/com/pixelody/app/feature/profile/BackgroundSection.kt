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
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The picker/draft/dialog outlive recycling of any Settings list item. */
@Composable
internal fun StudioBackgroundEditor(
    covers: CoverBook, store: CoverStore?, onChange: (CoverBook) -> Unit,
    activeTheme: PixelodyMobileTheme,
    content: @Composable (onEdit: (ScreenBackground, String?) -> Unit, importing: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentCovers by rememberUpdatedState(covers)
    val currentTheme by rememberUpdatedState(activeTheme)
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var draftScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var draftUri by rememberSaveable { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val key = pending
        pending = null
        if (uri != null && key != null && store != null) scope.launch {
            importing = true
            try {
                val saved = withContext(Dispatchers.IO) { store.importImage(context.applicationContext, uri) }
                if (saved == null) Toast.makeText(context, "Couldn't read this image. Try another photo.", Toast.LENGTH_LONG).show()
                else if (currentTheme == PixelodyMobileTheme.Studio) { draftScreen = key; draftUri = saved }
                else withContext(Dispatchers.IO) { store.discardDraft(saved, currentCovers) }
            } finally { importing = false }
        }
    }
    content({ screen, uri ->
        if (!importing && store != null && activeTheme == PixelodyMobileTheme.Studio) {
            if (uri == null) { pending = screen.name; picker.launch("image/*") }
            else { draftScreen = screen.name; draftUri = uri }
        }
    }, importing)
    val screen = ScreenBackground.entries.firstOrNull { it.name == draftScreen }
    val uri = draftUri
    if (screen != null && uri != null && activeTheme == PixelodyMobileTheme.Studio) {
        ImageStyleDialog(screen, uri, covers.entry(screen.key)?.takeIf { it.imageUri == uri },
            onDismiss = {
                draftUri = null; draftScreen = null
                scope.launch(Dispatchers.IO) { store?.discardDraft(uri, currentCovers) }
            }, onApply = { opacity, style ->
                onChange(currentCovers.withStyledBackground(screen, uri, opacity, style))
                draftUri = null; draftScreen = null
            })
    }
}

@Composable
internal fun BackgroundSection(
    covers: CoverBook, onChange: (CoverBook) -> Unit, activeTheme: PixelodyMobileTheme,
    onUseStudio: () -> Unit, onEdit: (ScreenBackground, String?) -> Unit, importing: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = "Studio backgrounds", subtitle = if (activeTheme == PixelodyMobileTheme.Studio) "Image-led Custom looks, one screen at a time" else "Personalize in Pixelody Studio") {
        if (activeTheme != PixelodyMobileTheme.Studio) {
            Text("${activeTheme.displayName} keeps its complete look. Your Studio images and settings are saved for when you return.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onUseStudio, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Personalize in Studio") }
        } else {
            OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (expanded) "Close background controls" else "Customize backgrounds")
            }
            if (expanded) {
                Text("Choose an image, then review its opacity, colors, headings and framing together. Nothing changes until you apply the preview. Images stay on this phone.", style = MaterialTheme.typography.bodySmall)
                if (importing) Text("Saving image…")
                ScreenBackground.entries.forEach { screen ->
                    val image = covers.imageFor(screen.key, null)
                    Text(screen.label, style = MaterialTheme.typography.titleSmall)
                    if (image != null) {
                        RemoteArtwork(image, "${screen.label} background preview", Modifier.fillMaxWidth().height(96.dp))
                        Text(if (covers.entry(screen.key)?.imageStyle != null) "Custom · image-led style" else "Saved image · ready to style", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { onEdit(screen, image) }, enabled = !importing,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Style ${screen.label} background") }
                    }
                    OutlinedButton(onClick = { onEdit(screen, null) }, enabled = !importing,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (image == null) "Choose ${screen.label} background" else "Change ${screen.label} background")
                    }
                    if (image != null) TextButton(onClick = { onChange(covers.withoutBackground(screen)) }, enabled = !importing,
                        modifier = Modifier.heightIn(min = 48.dp)) { Text("Reset ${screen.label} background") }
                }
            }
        }
    }
}