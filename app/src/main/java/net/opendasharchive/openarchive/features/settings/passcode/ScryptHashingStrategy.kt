package net.opendasharchive.openarchive.features.settings.passcode

import org.bouncycastle.crypto.generators.SCrypt
import java.security.SecureRandom

// ScryptHashingStrategy.kt
class ScryptHashingStrategy : HashingStrategy {

    companion object {
        private const val N = 16384  // CPU/Memory cost parameter
        private const val r = 8      // Block size
        private const val p = 1      // Parallelization parameter
        private const val KEY_LENGTH = 32 // 256 bits
        private const val SALT_LENGTH = 16
    }

    override val saltLength: Int
        get() = SALT_LENGTH

    override suspend fun generateSalt(): ByteArray {
        val random = SecureRandom()
        return ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
    }

    override suspend fun hash(passcode: String, salt: ByteArray): ByteArray {
        return SCrypt.generate(passcode.toByteArray(), salt, N, r, p, KEY_LENGTH)
    }
}