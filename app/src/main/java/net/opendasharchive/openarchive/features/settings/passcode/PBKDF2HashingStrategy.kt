package net.opendasharchive.openarchive.features.settings.passcode

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// PBKDF2HashingStrategy.kt
class PBKDF2HashingStrategy : HashingStrategy {

    companion object {
        private const val ITERATIONS = 65536
        private const val KEY_LENGTH = 256
        private const val HASH_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_LENGTH = 16
    }

    override val saltLength: Int
        get() = SALT_LENGTH

    override suspend fun generateSalt(): ByteArray {
        val random = SecureRandom()
        return ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
    }

    override suspend fun hash(passcode: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance(HASH_ALGORITHM)
        return skf.generateSecret(spec).encoded
    }
}