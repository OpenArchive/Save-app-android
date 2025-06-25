package net.opendasharchive.openarchive.services.storacha.util

import android.content.Context
import androidx.core.content.edit
import org.bitcoinj.core.Base58
import java.security.KeyPairGenerator
import java.security.SecureRandom

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
        val keyPairGen = KeyPairGenerator.getInstance("Ed25519")
        keyPairGen.initialize(256, SecureRandom())
        val keyPair = keyPairGen.generateKeyPair()
        val pubKey = keyPair.public.encoded
        val base58PubKey = Base58.encode(pubKey) // Add a Base58 library
        return "did:key:z$base58PubKey"
    }
}

