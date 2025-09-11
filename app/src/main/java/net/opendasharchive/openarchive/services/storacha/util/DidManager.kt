package net.opendasharchive.openarchive.services.storacha.util

import android.content.Context
import androidx.core.content.edit
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class DidManager(
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences("storacha_prefs", Context.MODE_PRIVATE)
    private val key = "device_did"
    private val keyStorage = KeyStorage(context)

    fun getOrCreateDid(): String {
        // First check if we have a DID in secure storage
        val existingDid = keyStorage.getDid()
        if (existingDid != null) {
            return existingDid
        }
        
        // Check if we have one in old prefs (for migration)
        val legacyDid = prefs.getString(key, null)
        if (legacyDid != null) {
            // For legacy DIDs without stored keys, generate new ones
            // This will replace the old DID with a new one that has proper keys
            prefs.edit { remove(key) }
        }
        
        // Generate new DID with keys
        return generateNewDid()
    }

    private fun generateNewDid(): String {
        // Ensure BouncyCastle is registered
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        // Generate and store new key pair securely
        return keyStorage.generateAndStoreKeyPair()
    }
    
    fun regenerateDid(): String {
        keyStorage.clearKeys()
        prefs.edit { remove(key) }
        return generateNewDid()
    }
    
    fun hasDid(): Boolean {
        return keyStorage.hasKeys()
    }
}

