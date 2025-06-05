package net.opendasharchive.openarchive.features.settings.passcode

interface HashingStrategy {
    suspend fun hash(passcode: String, salt: ByteArray): ByteArray
    suspend fun generateSalt(): ByteArray
    val saltLength: Int
}