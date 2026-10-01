package com.pixelody.app.data.model

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

data class HostConnectionDetails(
    val baseUrl: String,
    val baseUrls: List<String>,
    val token: String = "",
    val pairingCode: String = "",
    val pairingSecret: String = "",
    val deviceName: String = "Pixelody Android",
    val requestedPermissions: List<String> = emptyList()
) {
    val hasToken: Boolean
        get() = token.isNotBlank()

    val hasPairingSecret: Boolean
        get() = pairingCode.isNotBlank() && pairingSecret.isNotBlank()

    companion object {
        fun fromUri(uri: Uri?): HostConnectionDetails? {
            if (uri == null || uri.scheme != "pixelody" || uri.host != "connect") return null
            return fromDeepLinkParameters(
                parameter = { key -> uri.getQueryParameter(key).orEmpty() }
            )
        }

        fun fromText(text: String): HostConnectionDetails? {
            val trimmed = text.trim().trim('\uFEFF')
            if (trimmed.isBlank()) return null

            if (trimmed.startsWith("pixelody://")) {
                return fromDeepLinkText(trimmed)
            }

            if (trimmed.startsWith("pxd1|") || trimmed.startsWith("pxd1:")) {
                return fromCompactPairingText(trimmed)
            }

            if (trimmed.startsWith("{")) {
                val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
                val android = json.optJSONObject("android")
                val networkBaseUrls = json.optJSONArray("networkBaseUrls")
                    .toBaseUrlsFromNetworkEntries()
                val candidates = listOf(
                    android?.optString("preferredBaseUrl").orEmpty(),
                    json.optString("remoteBaseUrl"),
                    json.optString("baseUrl"),
                ) + android.optBaseUrls() +
                    json.optJSONArray("baseUrls").toStringList() +
                    networkBaseUrls +
                    listOf(json.optString("localBaseUrl"))
                return fromValues(
                    baseUrls = candidates,
                    token = json.optString("token"),
                    pairingCode = json.optString("pairingCode"),
                    pairingSecret = json.optString("secret"),
                    requestedPermissions = json.optJSONArray("permissions").toStringList(),
                    deviceName = android?.optString("deviceName").orEmpty()
                        .ifBlank { json.optString("deviceName") }
                )
            }

            return null
        }

        private fun fromValues(
            baseUrls: List<String>,
            token: String,
            pairingCode: String = "",
            pairingSecret: String = "",
            deviceName: String = "",
            requestedPermissions: List<String> = emptyList()
        ): HostConnectionDetails? {
            val normalizedBaseUrls = baseUrls
                .map { it.trim().trimEnd('/') }
                .filter { it.startsWith("http://") || it.startsWith("https://") }
                .distinct()
            val normalizedToken = token.trim()
            val normalizedPairingCode = pairingCode.trim()
            val normalizedPairingSecret = pairingSecret.trim()
            val normalizedBaseUrl = normalizedBaseUrls.firstOrNull() ?: return null
            if (normalizedToken.isBlank() && (normalizedPairingCode.isBlank() || normalizedPairingSecret.isBlank())) return null
            return HostConnectionDetails(
                baseUrl = normalizedBaseUrl,
                baseUrls = normalizedBaseUrls,
                token = normalizedToken,
                pairingCode = normalizedPairingCode,
                pairingSecret = normalizedPairingSecret,
                deviceName = deviceName.ifBlank { "Pixelody Android" },
                requestedPermissions = requestedPermissions.distinct()
            )
        }

        private fun fromDeepLinkText(text: String): HostConnectionDetails? {
            if (!text.startsWith("pixelody://connect")) return null
            val query = text.substringAfter("?", missingDelimiterValue = "")
            val parameters = query.split("&")
                .filter { it.isNotBlank() }
                .mapNotNull { part ->
                    val key = part.substringBefore("=", missingDelimiterValue = "")
                    if (key.isBlank()) return@mapNotNull null
                    val value = part.substringAfter("=", missingDelimiterValue = "")
                    decodeQueryComponent(key) to decodeQueryComponent(value)
                }
                .groupBy({ it.first }, { it.second })
            return fromDeepLinkParameters(
                parameter = { key -> parameters[key]?.lastOrNull().orEmpty() }
            )
        }

        private fun fromDeepLinkParameters(parameter: (String) -> String): HostConnectionDetails? {
            val baseUrl = parameter("baseUrl")
                .ifBlank { parameter("u") }
                .trim()
            val token = parameter("token").trim()
            val pairingCode = parameter("pairingCode")
                .ifBlank { parameter("c") }
                .trim()
            val pairingSecret = parameter("secret")
                .ifBlank { parameter("s") }
                .trim()
            val baseUrls = listOf(baseUrl) + parameter("urls")
                .split(",")
                .map { it.trim() }
            return fromValues(
                baseUrls = baseUrls,
                token = token,
                pairingCode = pairingCode,
                pairingSecret = pairingSecret
            )
        }

        private fun fromCompactPairingText(text: String): HostConnectionDetails? {
            val normalized = if (text.startsWith("pxd1:")) {
                "pxd1|" + text.removePrefix("pxd1:")
            } else {
                text
            }
            val parts = normalized.split("|")
            if (parts.size != 4 || parts[0] != "pxd1") return null
            return fromValues(
                baseUrls = listOf(decodeQueryComponent(parts[1])),
                token = "",
                pairingCode = decodeQueryComponent(parts[2]),
                pairingSecret = decodeQueryComponent(parts[3])
            )
        }

        private fun decodeQueryComponent(value: String): String =
            runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }
                .getOrDefault(value)

        private fun JSONObject?.optBaseUrls(): List<String> {
            if (this == null) return emptyList()
            return optJSONArray("baseUrls").toStringList()
        }

        private fun JSONArray?.toStringList(): List<String> {
            if (this == null) return emptyList()
            return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
        }

        private fun JSONArray?.toBaseUrlsFromNetworkEntries(): List<String> {
            if (this == null) return emptyList()
            return List(length()) { index ->
                optJSONObject(index)?.optString("baseUrl").orEmpty()
            }.filter { it.isNotBlank() }
        }
    }
}
