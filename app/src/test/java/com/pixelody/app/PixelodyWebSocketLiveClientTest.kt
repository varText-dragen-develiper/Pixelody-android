package com.pixelody.app

import com.pixelody.app.data.network.PixelodyWebSocketLiveClient
import com.pixelody.app.data.network.WebSocketConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelodyWebSocketLiveClientTest {

    @Test
    fun clientInitializesAndDisconnectsCleanly() = runBlocking {
        val client = PixelodyWebSocketLiveClient(scope = CoroutineScope(Dispatchers.Unconfined))
        client.disconnect()
        // verify no crash on disconnected state
        assertTrue(true)
    }
}
