package net.opendasharchive.openarchive.features.settings.passcode

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.util.Prefs
import com.google.crypto.tink.RegistryConfiguration
import net.opendasharchive.openarchive.core.config.AppConfig

class PasscodeRepository(
    private val prefs: SharedPreferences,
    private val config: AppConfig,
    private val hashingStrategy: HashingStrategy,
    private val aead: PrefAead,
) {

    companion object {
        private const val SECURE_PREF_NAME = "secret_shared_prefs"
        private const val KEY_PASSCODE_HASH = "passcode_hash"
        private const val KEY_PASSCODE_SALT = "passcode_salt"
    }

    suspend fun verifyPasscode(passcode: String): Boolean = withContext(Dispatchers.Default) {
        val (storedHash, storedSalt) = withContext(Dispatchers.IO) { getPasscodeHashAndSalt() }
        if (storedHash == null || storedSalt == null) return@withContext false

        val computed = hashingStrategy.hash(passcode, storedSalt)
        constantTimeEquals(computed, storedHash)
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    private fun aadFor(key: String) = key.toByteArray(Charsets.UTF_8)

    suspend fun generateSalt(): ByteArray {
        return hashingStrategy.generateSalt()
    }

    suspend fun hashPasscode(passcode: String, salt: ByteArray): ByteArray {
        return hashingStrategy.hash(passcode, salt)
    }

    fun storePasscodeHashAndSalt(hash: ByteArray, salt: ByteArray) {
        val encHash = aead.encrypt(hash, aadFor(KEY_PASSCODE_HASH))
        val encSalt = aead.encrypt(salt, aadFor(KEY_PASSCODE_SALT))

        with(prefs.edit()) {
            putString(KEY_PASSCODE_HASH, Base64.encodeToString(encHash, Base64.NO_WRAP))
            putString(KEY_PASSCODE_SALT, Base64.encodeToString(encSalt, Base64.NO_WRAP))
            apply()
        }
        setPasscodeEnabled(true)
    }

    fun getPasscodeHashAndSalt(): Pair<ByteArray?, ByteArray?> {
        val hashB64 = prefs.getString(KEY_PASSCODE_HASH, null) ?: return Pair(null, null)
        val saltB64 = prefs.getString(KEY_PASSCODE_SALT, null) ?: return Pair(null, null)

        val hashCipher = Base64.decode(hashB64, Base64.NO_WRAP)
        val saltCipher = Base64.decode(saltB64, Base64.NO_WRAP)

        val hash = aead.decrypt(hashCipher, aadFor(KEY_PASSCODE_HASH))
        val salt = aead.decrypt(saltCipher, aadFor(KEY_PASSCODE_SALT))

        return Pair(hash, salt)
    }

    fun setPasscodeEnabled(enabled: Boolean) {
        Prefs.passcodeEnabled = enabled
    }

    fun isPasscodeEnabled(): Boolean {
        return Prefs.passcodeEnabled
    }

    fun clearPasscode() {
        with(prefs.edit()) {
            remove(KEY_PASSCODE_HASH)
            remove(KEY_PASSCODE_SALT)
            apply()
        }
        setPasscodeEnabled(false)
    }

    fun recordFailedAttempt() {
        val failedAttempts = Prefs.getInt(PasscodeManager.KEY_FAILED_ATTEMPTS, 0) + 1
        Prefs.putInt(PasscodeManager.KEY_FAILED_ATTEMPTS, failedAttempts)
        if (config.maxRetryLimitEnabled && failedAttempts >= config.maxFailedAttempts) {
            Prefs.putLong(PasscodeManager.KEY_LOCKOUT_TIME, System.currentTimeMillis())
        }
    }

    fun isLockedOut(): Boolean {
        if (!config.maxRetryLimitEnabled) return false
        val lockoutTime = Prefs.getLong(PasscodeManager.KEY_LOCKOUT_TIME, 0L)
        if (lockoutTime == 0L) {
            return false
        }
        val elapsedTime = System.currentTimeMillis() - lockoutTime
        return if (elapsedTime >= PasscodeManager.LOCKOUT_DURATION_MS) {
            // Lockout duration passed, reset failed attempts and lockout time
            resetFailedAttempts()
            false
        } else {
            true
        }
    }

    fun isMaxRetryLimitEnabled(): Boolean {
        return config.maxRetryLimitEnabled
    }

    fun getRemainingAttempts(): Int {
        val failedAttempts = Prefs.getInt(PasscodeManager.KEY_FAILED_ATTEMPTS, 0)
        return config.maxFailedAttempts - failedAttempts
    }

    fun resetFailedAttempts() {
        Prefs.putInt(PasscodeManager.KEY_FAILED_ATTEMPTS, 0)
        Prefs.putLong(PasscodeManager.KEY_LOCKOUT_TIME, 0L)
    }
}

class PrefAead(context: Context) {
    private val aead: Aead

    init {
        val appContext = context.applicationContext
        AeadConfig.register()

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "tink_keyset", "tink_keyset_pref")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM")) // or AES256_GCM_SIV
            //.withMasterKeyUri("android-keystore://openarchive_master_key")
            .build()
            .keysetHandle

        aead = keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    fun encrypt(plain: ByteArray, aad: ByteArray): ByteArray = aead.encrypt(plain, aad)
    fun decrypt(cipher: ByteArray, aad: ByteArray): ByteArray = aead.decrypt(cipher, aad)
}
