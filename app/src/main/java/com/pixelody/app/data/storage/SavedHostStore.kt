package com.pixelody.app.data.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.pixelody.app.data.model.SavedHostProfile
import java.net.URI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

class CredentialStorageException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/**
 * Stores non-secret host metadata in SharedPreferences and the trusted-device credential encrypted
 * with a non-exportable Android Keystore key. No plaintext fallback is used if the keystore fails.
 */
class SavedHostStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cachedProfile: SavedHostProfile? = null
    @Volatile
    private var lastRawHost: String? = null
    @Volatile
    private var lastEncryptedCredential: String? = null

    @Synchronized
    fun load(): SavedHostProfile? {
        val raw = prefs.getString(KEY_ACTIVE_HOST, null) ?: run {
            cachedProfile = null
            lastRawHost = null
            lastEncryptedCredential = null
            return null
        }
        val encryptedToken = prefs.getString(KEY_ACTIVE_CREDENTIAL, null)
        val cached = cachedProfile
        if (cached != null && raw == lastRawHost && encryptedToken == lastEncryptedCredential) {
            return cached
        }

        val json = runCatching { JSONObject(raw) }.getOrElse {
            throw CredentialStorageException("Saved host metadata is damaged. Forget the host and pair again.", it)
        }
        val legacyToken = json.optString("token").trim()
        val token = when {
            !encryptedToken.isNullOrBlank() -> decrypt(encryptedToken)
            legacyToken.isNotBlank() -> {
                val sanitized = json.apply { remove("token") }
                persist(sanitized, encrypt(legacyToken))
                legacyToken
            }
            else -> throw CredentialStorageException("The saved host credential is missing. Pair again with a fresh invite.")
        }
        val loaded = SavedHostProfile(
            hostId = json.optString("hostId"),
            hostName = json.optString("hostName", "Pixelody Host"),
            baseUrl = json.optString("baseUrl"),
            baseUrls = json.optJSONArray("baseUrls").toStringList(),
            token = token,
            platform = json.optString("platform", "unknown"),
            roles = json.optJSONArray("roles").toStringList(),
            savedAt = json.optLong("savedAt"),
            lastConnectedAt = json.optLong("lastConnectedAt")
        ).takeIf { it.baseUrl.isNotBlank() && it.token.isNotBlank() }

        cachedProfile = loaded
        lastRawHost = raw
        lastEncryptedCredential = encryptedToken
        return loaded
    }

    @Synchronized
    fun save(profile: SavedHostProfile) {
        require(profile.token.isNotBlank()) { "A trusted-device credential is required." }
        val json = JSONObject()
            .put("hostId", profile.hostId)
            .put("hostName", profile.hostName)
            .put("baseUrl", profile.baseUrl)
            .put("baseUrls", JSONArray(profile.baseUrls))
            .put("platform", profile.platform)
            .put("roles", JSONArray(profile.roles))
            .put("savedAt", profile.savedAt)
            .put("lastConnectedAt", profile.lastConnectedAt)
        val encryptedToken = encrypt(profile.token)
        persist(json, encryptedToken)
        cachedProfile = profile
        lastRawHost = json.toString()
        lastEncryptedCredential = encryptedToken
    }

    /** Returns a credential only when [targetUrl] has the exact or equivalent origin of the saved host. */
    fun credentialFor(targetUrl: String): String? {
        val profile = load() ?: return null
        val targetOrigin = targetUrl.toOrigin() ?: return null
        val allowedOrigins = (profile.baseUrls + profile.baseUrl).mapNotNull(String::toOrigin).toSet()
        if (targetOrigin in allowedOrigins) return profile.token
        if (isOriginEquivalent(targetOrigin, allowedOrigins)) return profile.token
        return null
    }

    @Synchronized
    fun clear() {
        val committed = prefs.edit()
            .remove(KEY_ACTIVE_HOST)
            .remove(KEY_ACTIVE_CREDENTIAL)
            .commit()
        cachedProfile = null
        lastRawHost = null
        lastEncryptedCredential = null
        if (!committed) throw CredentialStorageException("Android could not clear the saved host credential.")
    }

    private fun persist(metadata: JSONObject, encryptedToken: String) {
        val committed = prefs.edit()
            .putString(KEY_ACTIVE_HOST, metadata.toString())
            .putString(KEY_ACTIVE_CREDENTIAL, encryptedToken)
            .commit()
        if (!committed) throw CredentialStorageException("Android could not persist the protected host credential.")
    }

    private fun encrypt(token: String): String = runCatching {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }.getOrElse {
        throw CredentialStorageException("Android could not protect the host credential. Pairing was not saved.", it)
    }

    private fun decrypt(payload: String): String = runCatching {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        require(bytes.size > GCM_IV_BYTES) { "Encrypted credential is incomplete." }
        val iv = bytes.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = bytes.copyOfRange(GCM_IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.doFinal(ciphertext).toString(Charsets.UTF_8).also { require(it.isNotBlank()) }
    }.getOrElse {
        throw CredentialStorageException("Android could not unlock the saved host credential. Forget the host and pair again.", it)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }

    internal companion object {
        const val PREFERENCES_NAME = "pixelody_hosts"
        const val KEY_ACTIVE_HOST = "active_host"
        const val KEY_ACTIVE_CREDENTIAL = "active_host_credential_v2"
        private const val KEY_ALIAS = "pixelody.trusted_device.v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
    }
}

internal fun String.toOrigin(): String? = runCatching {
    val uri = URI(trim())
    val scheme = uri.scheme?.lowercase()?.takeIf { it == "http" || it == "https" } ?: return null
    val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
    val port = if (uri.port >= 0) uri.port else if (scheme == "https") 443 else 80
    "$scheme://$host:$port"
}.getOrNull()

private val LOOPBACK_HOSTS = setOf("127.0.0.1", "localhost", "10.0.2.2", "::1", "[::1]")

internal fun isOriginEquivalent(targetOrigin: String, allowedOrigins: Set<String>): Boolean {
    val targetUri = runCatching { URI(targetOrigin) }.getOrNull() ?: return false
    val targetHost = targetUri.host?.lowercase().orEmpty()
    val targetPort = if (targetUri.port >= 0) targetUri.port else if (targetUri.scheme == "https") 443 else 80
    val isTargetLoopback = targetHost in LOOPBACK_HOSTS

    for (allowed in allowedOrigins) {
        val allowedUri = runCatching { URI(allowed) }.getOrNull() ?: continue
        val allowedHost = allowedUri.host?.lowercase().orEmpty()
        val allowedPort = if (allowedUri.port >= 0) allowedUri.port else if (allowedUri.scheme == "https") 443 else 80
        if (targetPort != allowedPort) continue
        if (targetUri.scheme != allowedUri.scheme) continue
        if (isTargetLoopback && allowedHost in LOOPBACK_HOSTS) return true
    }
    return false
}

