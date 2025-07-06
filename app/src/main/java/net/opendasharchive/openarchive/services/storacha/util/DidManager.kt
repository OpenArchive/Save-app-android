package net.opendasharchive.openarchive.services.storacha.util

import android.content.Context
import androidx.core.content.edit
import org.bitcoinj.core.Base58
import java.security.KeyPairGenerator
import java.security.SecureRandom
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters

class DidManager(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("storacha_prefs", Context.MODE_PRIVATE)
    private val key = "device_did"

    fun getOrCreateDid(): String =
        prefs.getString(key, null) ?: generateNewDid().also {
            prefs.edit { putString(key, it) }
        }

    private fun generateNewDid(): String {
        // Ensure BouncyCastle is registered
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        val random = SecureRandom()
        val keyPairGenerator = Ed25519KeyPairGenerator()
        val keyGenParams = Ed25519KeyGenerationParameters(random)
        keyPairGenerator.init(keyGenParams)

        val keyPair = keyPairGenerator.generateKeyPair()
        val publicKeyParams = keyPair.public as Ed25519PublicKeyParameters
        val pubKeyBytes = publicKeyParams.encoded

        val base58PubKey = Base58.encode(pubKeyBytes)
        return "did:key:z$base58PubKey"
    }
}

