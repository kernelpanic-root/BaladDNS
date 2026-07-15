package com.kernelpanic.baladdns.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class TokenManager private constructor(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)

    private val apiKeyLock = Any()
    private val emailLock = Any()

    @Volatile
    private var apiKeyLoaded = false

    @Volatile
    private var cachedApiKey: String? = null

    @Volatile
    private var emailLoaded = false

    @Volatile
    private var cachedEmail: String? = null

    private val aead: Aead by lazy {
        AeadConfig.register() // Registers the encryption algorithms with Tink

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "tink_keyset", "tink_prefs_internal")
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://secure_string_master_key")
            .build()
            .keysetHandle

        keysetHandle.getPrimitive(Aead::class.java)
    }

    fun saveApiKey(value: String) {
        val encryptedBytes = aead.encrypt(value.toByteArray(Charsets.UTF_8), null)
        val base64Encoded = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        synchronized(apiKeyLock) {
            prefs.edit { putString("api_key", base64Encoded) }
            cachedApiKey = value
            apiKeyLoaded = true
        }
    }

    fun getApiKey(): String? {
        if (apiKeyLoaded) return cachedApiKey

        return synchronized(apiKeyLock) {
            if (!apiKeyLoaded) {
                cachedApiKey = decryptPreference("api_key")
                apiKeyLoaded = true
            }
            cachedApiKey
        }
    }

    fun hasToken(): Boolean = getApiKey() != null

    fun destroyApiKey() {
        synchronized(apiKeyLock) {
            prefs.edit { remove("api_key") }
            cachedApiKey = null
            apiKeyLoaded = true
        }
    }

    fun saveEmail(value: String) {
        val encryptedBytes = aead.encrypt(value.toByteArray(Charsets.UTF_8), null)
        val base64Encoded = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        synchronized(emailLock) {
            prefs.edit { putString("email", base64Encoded) }
            cachedEmail = value
            emailLoaded = true
        }
    }

    fun getEmail(): String? {
        if (emailLoaded) return cachedEmail

        return synchronized(emailLock) {
            if (!emailLoaded) {
                cachedEmail = decryptPreference("email")
                emailLoaded = true
            }
            cachedEmail
        }
    }

    fun destroyEmail() {
        synchronized(emailLock) {
            prefs.edit { remove("email") }
            cachedEmail = null
            emailLoaded = true
        }
    }

    private fun decryptPreference(name: String): String? {
        val base64Encoded = prefs.getString(name, null) ?: return null
        return try {
            val encryptedBytes = Base64.decode(base64Encoded, Base64.DEFAULT)
            val decryptedBytes = aead.decrypt(encryptedBytes, null)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager =
            instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
    }
}
