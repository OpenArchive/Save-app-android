package net.opendasharchive.openarchive.services.storacha.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Modern secure storage using Android Keystore directly
 * Replaces deprecated EncryptedSharedPreferences
 */
class SecureStorage(
    private val context: Context,
    private val alias: String = "StorachaSecureStorage",
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

        // Combine IV and encrypted data
        val combined = iv + encryptedData
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    private fun decrypt(encryptedData: String): String {
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)

        // Extract IV and encrypted data
        val iv = combined.sliceArray(0..GCM_IV_LENGTH - 1)
        val encrypted = combined.sliceArray(GCM_IV_LENGTH until combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    fun putString(
        key: String,
        value: String?,
    ) {
        sharedPreferences.edit {
            if (value != null) {
                putString(key, encrypt(value))
            } else {
                remove(key)
            }
        }
    }

    fun getString(
        key: String,
        defaultValue: String? = null,
    ): String? {
        val encryptedValue = sharedPreferences.getString(key, null)
        return if (encryptedValue != null) {
            try {
                decrypt(encryptedValue)
            } catch (_: Exception) {
                // Handle decryption failure gracefully
                defaultValue
            }
        } else {
            defaultValue
        }
    }

    fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        putString(key, value.toString())
    }

    fun getBoolean(
        key: String,
        defaultValue: Boolean = false,
    ): Boolean = getString(key)?.toBooleanStrictOrNull() ?: defaultValue

    fun putInt(
        key: String,
        value: Int,
    ) {
        putString(key, value.toString())
    }

    fun getInt(
        key: String,
        defaultValue: Int = 0,
    ): Int = getString(key)?.toIntOrNull() ?: defaultValue

    fun remove(key: String) {
        sharedPreferences.edit {
            remove(key)
        }
    }

    fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }

    fun contains(key: String): Boolean = sharedPreferences.contains(key)

    // === Ed25519 Key Storage Extensions ===

    /**
     * Stores Ed25519 key pair securely
     */
    fun storeKeyPair(
        privateKey: Ed25519PrivateKeyParameters,
        publicKey: Ed25519PublicKeyParameters,
        did: String,
    ) {
        val privateKeyBytes = privateKey.encoded
        val publicKeyBytes = publicKey.encoded

        putString("ed25519_private_key", Base64.encodeToString(privateKeyBytes, Base64.NO_WRAP))
        putString("ed25519_public_key", Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP))
        putString("user_did", did)
    }

    /**
     * Retrieves the stored Ed25519 private key
     */
    fun getPrivateKey(): Ed25519PrivateKeyParameters? {
        val privateKeyString = getString("ed25519_private_key") ?: return null
        val privateKeyBytes = Base64.decode(privateKeyString, Base64.NO_WRAP)
        return Ed25519PrivateKeyParameters(privateKeyBytes, 0)
    }

    /**
     * Retrieves the stored Ed25519 public key
     */
    fun getPublicKey(): Ed25519PublicKeyParameters? {
        val publicKeyString = getString("ed25519_public_key") ?: return null
        val publicKeyBytes = Base64.decode(publicKeyString, Base64.NO_WRAP)
        return Ed25519PublicKeyParameters(publicKeyBytes, 0)
    }

    /**
     * Retrieves the stored DID
     */
    fun getDid(): String? = getString("user_did")

    /**
     * Checks if Ed25519 keys are stored
     */
    fun hasKeys(): Boolean = getPrivateKey() != null && getPublicKey() != null && getDid() != null

    /**
     * Clears all stored Ed25519 keys
     */
    fun clearKeys() {
        remove("ed25519_private_key")
        remove("ed25519_public_key")
        remove("user_did")
    }

    /**
     * Generates and stores a new Ed25519 key pair
     */
    fun generateAndStoreKeyPair(): String {
        val keyPair = Ed25519Utils.generateKeyPair()
        val privateKey = keyPair.private as Ed25519PrivateKeyParameters
        val publicKey = keyPair.public as Ed25519PublicKeyParameters
        val did = Ed25519Utils.createDidFromPublicKey(publicKey)

        storeKeyPair(privateKey, publicKey, did)
        return did
    }
}
