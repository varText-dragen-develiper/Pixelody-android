package com.pixelody.app.core.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import com.pixelody.app.data.storage.SavedHostStore
import java.io.IOException

@OptIn(UnstableApi::class)
internal fun authenticatedMediaDataSourceFactory(context: Context): DataSource.Factory {
    val credentialStore = SavedHostStore(context.applicationContext)
    val authenticatedHttp = ResolvingDataSource.Factory(
        DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(7_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(false)
    ) { dataSpec ->
        val target = dataSpec.uri.toString()
        android.util.Log.d("PixelodyAuthMedia", "Resolving media request: $target")
        val credential = try {
            credentialStore.credentialFor(target)
        } catch (error: RuntimeException) {
            android.util.Log.e("PixelodyAuthMedia", "Error loading credential for $target", error)
            throw IOException("The protected host credential is unavailable.", error)
        }
        android.util.Log.d("PixelodyAuthMedia", "Resolved credential for $target: found=${!credential.isNullOrBlank()}")
        if (credential.isNullOrBlank()) {
            dataSpec
        } else {
            dataSpec.withAdditionalHeaders(mapOf("Authorization" to "Bearer $credential"))
        }
    }
    return DefaultDataSource.Factory(context.applicationContext, authenticatedHttp)
}
