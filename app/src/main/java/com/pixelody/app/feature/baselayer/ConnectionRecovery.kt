package com.pixelody.app.feature.baselayer

/** Recover browsing without silently resuming remote playback on reconnect. */
internal fun sourceAfterConnectionLoss(source: BaseSource): BaseSource = when (source) {
    BaseSource.Host, BaseSource.Jam -> BaseSource.Phone
    else -> source
}
