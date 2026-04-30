package net.opendasharchive.openarchive.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Replaces TinkVaultCredentialStore — same AES-256-GCM + Android Keystore, no Tink dependency.
// Migration: if decryption fails (pre-existing Tink-encrypted data), the credential is cleared
// and the user will be prompted to re-enter their server password on next connection.
class TinkVaultCredentialStore(
    context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : VaultCredentialStore {

    private val appContext = context.applicationContext

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + io),
            produceFile = { appContext.preferencesDataStoreFile(DATASTORE_FILE_NAME) }
        )
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        kg.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    override suspend fun putSecret(vaultId: Long, secret: String) = withContext(io) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
        val payload = iv + ciphertext  // 12-byte IV prepended
        dataStore.edit { prefs ->
            prefs[secretKey(vaultId)] = Base64.encodeToString(payload, Base64.NO_WRAP)
        }
        Unit
    }

    override suspend fun getSecret(vaultId: Long): String? = withContext(io) {
        val encoded = dataStore.data.first()[secretKey(vaultId)] ?: return@withContext null
        runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = payload.sliceArray(0 until GCM_IV_LENGTH)
            val ciphertext = payload.sliceArray(GCM_IV_LENGTH until payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrElse {
            // Decrypt failed — likely Tink-encrypted data from before migration.
            // Clear the stale entry so user can re-enter credentials.
            dataStore.edit { prefs -> prefs.remove(secretKey(vaultId)) }
            null
        }
    }

    override suspend fun hasSecret(vaultId: Long): Boolean = withContext(io) {
        dataStore.data.first().contains(secretKey(vaultId))
    }

    override suspend fun deleteSecret(vaultId: Long) = withContext(io) {
        dataStore.edit { prefs -> prefs.remove(secretKey(vaultId)) }
        Unit
    }

    private fun secretKey(vaultId: Long) = stringPreferencesKey("vault_secret_$vaultId")

    private companion object {
        const val DATASTORE_FILE_NAME = "vault_secure_credentials"
        const val KEY_ALIAS = "openarchive_vault_master_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
    }
}
