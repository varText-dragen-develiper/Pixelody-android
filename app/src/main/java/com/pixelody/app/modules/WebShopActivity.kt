package com.pixelody.app.modules

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

/** Optional remote content: no bridge into the player, files or notebook. */
class WebShopActivity : ComponentActivity() {
    private var web: WebView? = null
    private var failed by mutableStateOf(false)
    private val store by lazy { ModuleStore(this) }

    override fun onResume() {
        super.onResume()
        if (runCatching { store.read().shop }.getOrNull() == null) finish()
        web?.onResume()
    }
    override fun onPause() { web?.onPause(); super.onPause() }
    override fun onDestroy() {
        web?.stopLoading()
        web?.destroy()
        web = null
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (runCatching { store.read().shop }.getOrNull() == null) { finish(); return }
        setContent {
            MaterialTheme {
                BackHandler { if (web?.canGoBack() == true) web?.goBack() else finish() }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { finish() }) { Text("Close shop") }
                            TextButton(onClick = { failed = false; web?.loadUrl(ModulePackage.SHOP_ENTRY) }) { Text("Reload") }
                        }
                        Text("RevenueCat Shop · Website · Test purchases", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                        if (failed) Text("The shop could not load. Check your connection, then reload. Your local music and notes are still available.", Modifier.padding(16.dp))
                        AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context ->
                            WebView(context).apply {
                                web = this
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                settings.javaScriptCanOpenWindowsAutomatically = false
                                settings.setSupportMultipleWindows(false)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = !ModulePackage.allowedShopUrl(request.url.toString())
                                    @Deprecated("Legacy WebView callback")
                                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !ModulePackage.allowedShopUrl(url)
                                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                        val url = request.url.toString()
                                        // The web SDK may contact RevenueCat over HTTPS. Neither origin receives a native bridge.
                                        val uri = runCatching { java.net.URI(url) }.getOrNull()
                                        val revenueCatApi = uri?.scheme == "https" && uri.host == "api.revenuecat.com" && uri.rawUserInfo == null && (uri.port == -1 || uri.port == 443)
                                        return if (ModulePackage.allowedShopUrl(url) || revenueCatApi) null else WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                                    }
                                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) { if (request.isForMainFrame) failed = true }
                                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) { if (request.isForMainFrame) failed = true }
                                }
                                setDownloadListener { url, _, _, _, _ ->
                                    if (ModulePackage.allowedShopUrl(url)) {
                                        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                        catch (_: Exception) { Toast.makeText(context, "No browser available for this download.", Toast.LENGTH_LONG).show() }
                                    }
                                }
                                loadUrl(ModulePackage.SHOP_ENTRY)
                            }
                        })
                    }
                }
            }
        }
    }
}
