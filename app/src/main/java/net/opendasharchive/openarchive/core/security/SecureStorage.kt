package net.opendasharchive.openarchive.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * General-purpose AES-GCM secure storage backed by Android Keystore.
 * Replaces deprecated EncryptedSharedPreferences.
 */
class SecureStorage(
    private val context: Context,
    private val alias: String = "SaveSecureStorage",
) {
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("${alias}_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }

    init {
        generateKeyIfNeeded()
    }

    private fun generateKeyIfNeeded() {
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec =
                KeyGenParameterSpec
                    .Builder(
                        alias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey = keyStore.getKey(alias, null) as SecretKey

    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        val combined = iv + encryptedData
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    private fun decrypt(encryptedData: String): String {
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)

        val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
        val encrypted = combined.sliceArray(GCM_IV_LENGTH until combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun putString(key: String, value: String?) {
        sharedPreferences.edit {
            if (value != null) putString(key, encrypt(value)) else remove(key)
        }
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        val encryptedValue = sharedPreferences.getString(key, null) ?: return defaultValue
        return try {
            decrypt(encryptedValue)
        } catch (_: Exception) {
            defaultValue
        }
    }

    fun remove(key: String) {
        sharedPreferences.edit { remove(key) }
    }

    fun contains(key: String): Boolean = sharedPreferences.contains(key)

    fun clear() {
        sharedPreferences.edit { clear() }
    }
}
