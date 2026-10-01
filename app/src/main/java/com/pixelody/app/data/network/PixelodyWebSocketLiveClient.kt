package com.pixelody.app.data.network

import com.pixelody.app.data.model.LiveState
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

enum class WebSocketConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting
}

class PixelodyWebSocketLiveClient(
    private val scope: CoroutineScope,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
) {
    private var webSocket: WebSocket? = null
    private var currentUrl: String = ""
    private var currentToken: String = ""
    private var reconnectJob: Job? = null
    private val isClosed = AtomicBoolean(false)
    private var consecutiveFailures = 0

    private val _liveUpdates = MutableSharedFlow<LiveState>(extraBufferCapacity = 64)
    val liveUpdates: SharedFlow<LiveState> = _liveUpdates.asSharedFlow()

    private val _connectionState = MutableSharedFlow<WebSocketConnectionState>(replay = 1)
    val connectionState: SharedFlow<WebSocketConnectionState> = _connectionState.asSharedFlow()

    fun connect(baseUrl: String, token: String, liveEndpoint: String = "/api/v1/live") {
        isClosed.set(false)
        currentUrl = baseUrl
        currentToken = token
        consecutiveFailures = 0
        establishConnection(baseUrl, token, liveEndpoint)
    }

    private fun establishConnection(baseUrl: String, token: String, liveEndpoint: String) {
        if (isClosed.get()) return
        webSocket?.cancel()
        webSocket = null

        val wsScheme = if (baseUrl.startsWith("https://", ignoreCase = true)) "wss://" else "ws://"
        val hostPart = baseUrl.removePrefix("http://").removePrefix("https://").removePrefix("HTTP://").removePrefix("HTTPS://").trimEnd('/')
        val endpoint = if (liveEndpoint.startsWith("/")) liveEndpoint else "/$liveEndpoint"
        val fullWsUrl = "$wsScheme$hostPart$endpoint"

        val request = Request.Builder()
            .url(fullWsUrl)
            .apply {
                if (token.isNotBlank()) {
                    addHeader("Authorization", "Bearer ${token.trim()}")
                }
            }
            .build()

        _connectionState.tryEmit(if (consecutiveFailures > 0) WebSocketConnectionState.Reconnecting else WebSocketConnectionState.Connecting)

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                consecutiveFailures = 0
                _connectionState.tryEmit(WebSocketConnectionState.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    val unchanged = json.optBoolean("unchanged", false)
                    if (!unchanged) {
                        val state = parseLiveStateFromJson(json)
                        if (state != null) {
                            _liveUpdates.tryEmit(state)
                        }
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.tryEmit(WebSocketConnectionState.Disconnected)
                if (!isClosed.get() && code != 1000) {
                    scheduleReconnect(liveEndpoint)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.tryEmit(WebSocketConnectionState.Disconnected)
                if (!isClosed.get()) {
                    consecutiveFailures++
                    scheduleReconnect(liveEndpoint)
                }
            }
        })
    }

    private fun scheduleReconnect(liveEndpoint: String) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            val backoffMs = (1000L * (1L shl (consecutiveFailures - 1).coerceIn(0, 4))).coerceIn(1000L, 16000L)
            delay(backoffMs)
            if (!isClosed.get() && currentUrl.isNotBlank()) {
                establishConnection(currentUrl, currentToken, liveEndpoint)
            }
        }
    }

    fun disconnect() {
        isClosed.set(true)
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.tryEmit(WebSocketConnectionState.Disconnected)
    }

    private fun parseLiveStateFromJson(response: JSONObject): LiveState? {
        return runCatching {
            response.toLiveState()
        }.getOrNull()
    }
}

