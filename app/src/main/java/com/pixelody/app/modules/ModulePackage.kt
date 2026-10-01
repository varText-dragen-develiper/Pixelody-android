package com.pixelody.app.modules

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

object ModulePackage {
    const val MAX_BYTES = 8192
    const val SHOP_ENTRY = "https://pixelody-web.pixelody101.workers.dev/shop?embedded=1"
    fun allowedShopUrl(url: String): Boolean = try {
        val uri = java.net.URI(url)
        uri.scheme == "https" && uri.host == "pixelody-web.pixelody101.workers.dev" &&
            uri.rawUserInfo == null && (uri.port == -1 || uri.port == 443)
    } catch (_: Exception) { false }
    fun parse(bytes: ByteArray): JSONObject {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "Choose a module file no larger than 8 KB." }
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        val value = JSONObject(text)
        val shop = value.optString("kind") == "web-shop"
        val keys = setOf("format", "id", "version", "kind", "platforms", "name", "description", if (shop) "entry" else "prompt")
        require(value.keys().asSequence().toSet() == keys) { "Unsupported module fields." }
        require(value.get("format") == 1 && value.get("id") == (if (shop) "pixelody.revenuecat-shop" else "pixelody.listening-notes") && value.get("kind") == (if (shop) "web-shop" else "listening-notes") && value.get("version") == "1.0.0") { "This module needs a different Pixelody host." }
        val platforms = value.getJSONArray("platforms")
        require(platforms.length() == 2 && (0 until platforms.length()).map { platforms.get(it) }.toSet() == setOf("desktop", "android")) { "Incompatible platform." }
        if (shop) require(value.get("entry") == SHOP_ENTRY) { "Unsupported shop address." }
        for ((key, max) in listOf("name" to 60, "description" to 300) + (if (shop) emptyList() else listOf("prompt" to 200))) {
            val entry = value.get(key)
            require(entry is String && entry.isNotBlank() && entry.length <= max && entry.none { it.code < 32 || it.code == 127 }) { "Invalid module text." }
        }
        return value
    }
}

internal fun java.io.InputStream.readBounded(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (out.size() < limit) {
        val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (n < 0) break
        if (n == 0) { val byte = read(); if (byte < 0) break; out.write(byte) } else out.write(buffer, 0, n)
    }
    return out.toByteArray()
}
