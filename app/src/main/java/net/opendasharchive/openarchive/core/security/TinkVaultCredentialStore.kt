package net.opendasharchive.openarchive.core.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

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

    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(appContext, KEYSET_PREF_KEY, KEYSET_PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://$MASTER_KEY_ALIAS")
            .build()
            .keysetHandle

        keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override suspend fun putSecret(vaultId: Long, secret: String) = withContext(io) {
        val encrypted = aead.encrypt(
            secret.toByteArray(Charsets.UTF_8),
            associatedData(vaultId)
        )
        dataStore.edit { prefs ->
            prefs[secretKey(vaultId)] = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        }
        Unit
    }

    override suspend fun getSecret(vaultId: Long): String? = withContext(io) {
        val encoded = dataStore.data.first()[secretKey(vaultId)] ?: return@withContext null
        val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
        val decrypted = aead.decrypt(ciphertext, associatedData(vaultId))
        String(decrypted, Charsets.UTF_8)
    }

    override suspend fun hasSecret(vaultId: Long): Boolean = withContext(io) {
        dataStore.data.first().contains(secretKey(vaultId))
    }

    override suspend fun deleteSecret(vaultId: Long) = withContext(io) {
        dataStore.edit { prefs ->
            prefs.remove(secretKey(vaultId))
        }
        Unit
    }

    private fun secretKey(vaultId: Long) = stringPreferencesKey("vault_secret_$vaultId")

    private fun associatedData(vaultId: Long): ByteArray =
        "vault_credentials:$vaultId".toByteArray(Charsets.UTF_8)

    private companion object {
        const val DATASTORE_FILE_NAME = "vault_secure_credentials"
        const val KEYSET_PREF_FILE = "vault_secure_credentials_keyset"
        const val KEYSET_PREF_KEY = "vault_secure_credentials_key"
        const val MASTER_KEY_ALIAS = "openarchive_vault_master_key"
    }
}
