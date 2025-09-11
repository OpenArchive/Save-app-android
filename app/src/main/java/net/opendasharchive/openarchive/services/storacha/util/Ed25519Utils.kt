package net.opendasharchive.openarchive.services.storacha.util

import android.util.Base64
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom

/**
 * Utility class for Ed25519 cryptographic operations including DID generation and signature creation
 */
object Ed25519Utils {
    /**
     * Generates a new Ed25519 key pair
     * @return AsymmetricCipherKeyPair containing private and public keys
     */
    fun generateKeyPair(): AsymmetricCipherKeyPair {
        val keyPairGenerator = Ed25519KeyPairGenerator()
        keyPairGenerator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Creates a DID from an Ed25519 public key
     * @param publicKey The Ed25519 public key
     * @return DID string in the format "did:key:z6Mk..."
     */
    fun createDidFromPublicKey(publicKey: Ed25519PublicKeyParameters): String {
        val publicKeyBytes = publicKey.encoded

        // DID key multicodec prefix for Ed25519 (0xed01)
        val multicodecPrefix = byteArrayOf(0xed.toByte(), 0x01)
        val multicodecKey = multicodecPrefix + publicKeyBytes

        // Base58 encode (using a simple implementation for z-base58)
        val base58Encoded = encodeBase58(multicodecKey)

        return "did:key:z$base58Encoded"
    }

    /**
     * Signs a challenge string with the provided private key
     * @param challenge The challenge string to sign
     * @param privateKey The Ed25519 private key
     * @return Base64-encoded signature
     */
    fun signChallenge(
        challenge: String,
        privateKey: Ed25519PrivateKeyParameters,
    ): String {
        val signer = Ed25519Signer()
        signer.init(true, privateKey)

        val challengeBytes = challenge.toByteArray(Charsets.UTF_8)
        signer.update(challengeBytes, 0, challengeBytes.size)

        val signature = signer.generateSignature()
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }

    /**
     * Verifies a signature against a challenge and public key
     * @param challenge The original challenge string
     * @param signature Base64-encoded signature
     * @param publicKey The Ed25519 public key
     * @return true if signature is valid, false otherwise
     */
    fun verifySignature(
        challenge: String,
        signature: String,
        publicKey: Ed25519PublicKeyParameters,
    ): Boolean =
        try {
            val signer = Ed25519Signer()
            signer.init(false, publicKey)

            val challengeBytes = challenge.toByteArray(Charsets.UTF_8)
            signer.update(challengeBytes, 0, challengeBytes.size)

            val signatureBytes = Base64.decode(signature, Base64.NO_WRAP)
            signer.verifySignature(signatureBytes)
        } catch (e: Exception) {
            false
        }

    /**
     * Extracts public key from a DID:key string
     * @param did The DID string in format "did:key:z6Mk..."
     * @return Ed25519PublicKeyParameters or null if invalid
     */
    fun extractPublicKeyFromDid(did: String): Ed25519PublicKeyParameters? {
        return try {
            if (!did.startsWith("did:key:z")) return null

            val base58Part = did.substring(9) // Remove "did:key:z"
            val multicodecKey = decodeBase58(base58Part)

            // Check multicodec prefix for Ed25519 (0xed01)
            if (multicodecKey.size < 34 || multicodecKey[0] != 0xed.toByte() || multicodecKey[1] != 0x01.toByte()) {
                return null
            }

            val publicKeyBytes = multicodecKey.sliceArray(2..33)
            Ed25519PublicKeyParameters(publicKeyBytes, 0)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Simple Base58 encoding implementation
     */
    private fun encodeBase58(input: ByteArray): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        if (input.isEmpty()) return ""

        // Convert to base 58
        var num = java.math.BigInteger(1, input)
        val base = java.math.BigInteger.valueOf(58)
        val result = StringBuilder()

        while (num > java.math.BigInteger.ZERO) {
            val remainder = num.remainder(base)
            num = num.divide(base)
            result.insert(0, alphabet[remainder.toInt()])
        }

        // Add leading zeros
        for (b in input) {
            if (b.toInt() == 0) {
                result.insert(0, alphabet[0])
            } else {
                break
            }
        }

        return result.toString()
    }

    /**
     * Simple Base58 decoding implementation
     */
    private fun decodeBase58(input: String): ByteArray {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val base = java.math.BigInteger.valueOf(58)
        var num = java.math.BigInteger.ZERO

        for (char in input) {
            val charIndex = alphabet.indexOf(char)
            if (charIndex < 0) throw IllegalArgumentException("Invalid character in Base58 string")
            num = num.multiply(base).add(java.math.BigInteger.valueOf(charIndex.toLong()))
        }

        val bytes = num.toByteArray()

        // Remove leading zero byte if present
        val result =
            if (bytes.isNotEmpty() && bytes[0].toInt() == 0) {
                bytes.sliceArray(1 until bytes.size)
            } else {
                bytes
            }

        // Add leading zeros for '1' characters
        val leadingOnes = input.takeWhile { it == alphabet[0] }.length
        return ByteArray(leadingOnes) + result
    }
}
