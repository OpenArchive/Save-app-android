package net.opendasharchive.openarchive.services.storacha.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import android.util.Base64

/**
 * Secure storage for Ed25519 keys using Android's EncryptedSharedPreferences
 */
class KeyStorage(private val context: Context) {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "storacha_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val PRIVATE_KEY_KEY = "ed25519_private_key"
        private const val PUBLIC_KEY_KEY = "ed25519_public_key"
        private const val DID_KEY = "user_did"
    }
    
    /**
     * Stores the Ed25519 key pair securely
     */
    fun storeKeyPair(privateKey: Ed25519PrivateKeyParameters, publicKey: Ed25519PublicKeyParameters, did: String) {
        val privateKeyBytes = privateKey.encoded
        val publicKeyBytes = publicKey.encoded
        
        encryptedPrefs.edit()
            .putString(PRIVATE_KEY_KEY, Base64.encodeToString(privateKeyBytes, Base64.NO_WRAP))
            .putString(PUBLIC_KEY_KEY, Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP))
            .putString(DID_KEY, did)
            .apply()
    }
    
    /**
     * Retrieves the stored private key
     */
    fun getPrivateKey(): Ed25519PrivateKeyParameters? {
        val privateKeyString = encryptedPrefs.getString(PRIVATE_KEY_KEY, null) ?: return null
        val privateKeyBytes = Base64.decode(privateKeyString, Base64.NO_WRAP)
        return Ed25519PrivateKeyParameters(privateKeyBytes, 0)
    }
    
    /**
     * Retrieves the stored public key
     */
    fun getPublicKey(): Ed25519PublicKeyParameters? {
        val publicKeyString = encryptedPrefs.getString(PUBLIC_KEY_KEY, null) ?: return null
        val publicKeyBytes = Base64.decode(publicKeyString, Base64.NO_WRAP)
        return Ed25519PublicKeyParameters(publicKeyBytes, 0)
    }
    
    /**
     * Retrieves the stored DID
     */
    fun getDid(): String? {
        return encryptedPrefs.getString(DID_KEY, null)
    }
    
    /**
     * Checks if keys are stored
     */
    fun hasKeys(): Boolean {
        return getPrivateKey() != null && getPublicKey() != null && getDid() != null
    }
    
    /**
     * Clears all stored keys
     */
    fun clearKeys() {
        encryptedPrefs.edit()
            .remove(PRIVATE_KEY_KEY)
            .remove(PUBLIC_KEY_KEY)
            .remove(DID_KEY)
            .apply()
    }
    
    /**
     * Generates and stores a new key pair
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