package com.pixelody.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.pixelody.app.core.image.ArtworkBitmapCache
import com.pixelody.app.data.storage.SavedHostStore

/** Viewport-bound image; constant readability shading keeps image visibility monotonic. */
@Composable
internal fun ScreenImageBackground(uri: String?, opacity: Float = 0.6f, modifier: Modifier = Modifier) {
    if (uri == null) return
    val context = LocalContext.current.applicationContext
    val credentials = remember { SavedHostStore(context) }
    val bitmap by produceState(ArtworkBitmapCache.get(uri), uri) {
        value = ArtworkBitmapCache.loadBitmap(context, uri, credentials, maxDimension = 768)
    }
    bitmap?.let {
        Box(modifier.fillMaxSize().alpha(opacity.coerceIn(0f, 1f))) {
            Image(it.asImageBitmap(), contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.70f)))
        }
    }
}
