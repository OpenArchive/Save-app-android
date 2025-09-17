package net.opendasharchive.openarchive.services.storacha.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.opendasharchive.openarchive.services.storacha.StorachaAccount

/**
 * Manages multiple Storacha account sessions using modern Android Keystore
 */
class StorachaAccountManager(
    context: Context,
) {
    private val secureStorage = SecureStorage(context, "storacha_accounts")
    private val gson = Gson()

    companion object {
        private const val ACCOUNTS_KEY = "logged_in_accounts"
        private const val CURRENT_ACCOUNT_KEY = "current_account_email"
    }

    /**
     * Add or update an account after successful login
     */
    fun addAccount(
        email: String,
        sessionId: String,
        isVerified: Boolean = false,
        did: String? = null,
    ) {
        val accounts = getLoggedInAccounts().toMutableList()
        val existingIndex = accounts.indexOfFirst { it.email == email }

        val account = StorachaAccount(email, sessionId, isVerified, did)

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
        val accountsJson = secureStorage.getString(ACCOUNTS_KEY) ?: return emptyList()
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
    fun getAccount(email: String): StorachaAccount? = getLoggedInAccounts().find { it.email == email }

    /**
     * Get current account email
     */
    fun getCurrentAccountEmail(): String? = secureStorage.getString(CURRENT_ACCOUNT_KEY)

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
        secureStorage.putString(CURRENT_ACCOUNT_KEY, email)
    }

    /**
     * Clear current account
     */
    fun clearCurrentAccount() {
        secureStorage.remove(CURRENT_ACCOUNT_KEY)
    }

    /**
     * Update account verification status
     */
    fun updateAccountVerification(
        email: String,
        isVerified: Boolean,
    ) {
        val accounts = getLoggedInAccounts().toMutableList()
        val existingIndex = accounts.indexOfFirst { it.email == email }

        if (existingIndex >= 0) {
            val existingAccount = accounts[existingIndex]
            accounts[existingIndex] = existingAccount.copy(isVerified = isVerified)
            saveAccounts(accounts)
        }
    }

    /**
     * Get current account's verification status
     */
    fun isCurrentAccountVerified(): Boolean = getCurrentAccount()?.isVerified == true

    private fun saveAccounts(accounts: List<StorachaAccount>) {
        val accountsJson = gson.toJson(accounts)
        secureStorage.putString(ACCOUNTS_KEY, accountsJson)
    }
}
