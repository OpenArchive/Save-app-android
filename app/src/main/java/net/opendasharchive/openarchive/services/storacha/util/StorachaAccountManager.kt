package net.opendasharchive.openarchive.services.storacha.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.opendasharchive.openarchive.services.storacha.StorachaAccount

/**
 * Manages multiple Storacha account sessions
 */
class StorachaAccountManager(private val context: Context) {
    
    private val masterKey: MasterKey by lazy {
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "storacha_accounts",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val gson = Gson()

    companion object {
        private const val ACCOUNTS_KEY = "logged_in_accounts"
        private const val CURRENT_ACCOUNT_KEY = "current_account_email"
    }

    /**
     * Add or update an account after successful login
     */
    fun addAccount(email: String, sessionId: String) {
        val accounts = getLoggedInAccounts().toMutableList()
        val existingIndex = accounts.indexOfFirst { it.email == email }
        
        val account = StorachaAccount(email, sessionId)
        
        if (existingIndex >= 0) {
            accounts[existingIndex] = account
        } else {
            accounts.add(account)
        }
        
        saveAccounts(accounts)
        setCurrentAccount(email)
    }

    /**
     * Remove an account (logout)
     */
    fun removeAccount(email: String) {
        val accounts = getLoggedInAccounts().toMutableList()
        accounts.removeAll { it.email == email }
        saveAccounts(accounts)
        
        // If we removed the current account, clear current account
        if (getCurrentAccountEmail() == email) {
            clearCurrentAccount()
        }
    }

    /**
     * Get all logged-in accounts
     */
    fun getLoggedInAccounts(): List<StorachaAccount> {
        val accountsJson = encryptedPrefs.getString(ACCOUNTS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<StorachaAccount>>() {}.type
        return gson.fromJson(accountsJson, type) ?: emptyList()
    }

    /**
     * Check if any accounts are logged in
     */
    fun hasLoggedInAccounts(): Boolean = getLoggedInAccounts().isNotEmpty()

    /**
     * Get account by email
     */
    fun getAccount(email: String): StorachaAccount? = 
        getLoggedInAccounts().find { it.email == email }

    /**
     * Get current account email
     */
    fun getCurrentAccountEmail(): String? = 
        encryptedPrefs.getString(CURRENT_ACCOUNT_KEY, null)

    /**
     * Get current account
     */
    fun getCurrentAccount(): StorachaAccount? {
        val email = getCurrentAccountEmail() ?: return null
        return getAccount(email)
    }

    /**
     * Set current account
     */
    fun setCurrentAccount(email: String) {
        encryptedPrefs.edit().putString(CURRENT_ACCOUNT_KEY, email).apply()
    }

    /**
     * Clear current account
     */
    fun clearCurrentAccount() {
        encryptedPrefs.edit().remove(CURRENT_ACCOUNT_KEY).apply()
    }

    /**
     * Clear all accounts
     */
    fun clearAllAccounts() {
        encryptedPrefs.edit()
            .remove(ACCOUNTS_KEY)
            .remove(CURRENT_ACCOUNT_KEY)
            .apply()
    }

    private fun saveAccounts(accounts: List<StorachaAccount>) {
        val accountsJson = gson.toJson(accounts)
        encryptedPrefs.edit().putString(ACCOUNTS_KEY, accountsJson).apply()
    }
}