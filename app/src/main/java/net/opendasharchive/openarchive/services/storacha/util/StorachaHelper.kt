package net.opendasharchive.openarchive.services.storacha.util

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Helper class for common Storacha-related checks and operations
 */
object StorachaHelper {
    private const val PREFS_NAME = "storacha_helper_prefs"
    private const val KEY_SPACE_COUNT = "space_count"

    private val _accountStateChanged = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val accountStateChanged: SharedFlow<Unit> = _accountStateChanged.asSharedFlow()

    fun invalidateAccountState() {
        _accountStateChanged.tryEmit(Unit)
    }

    /**
     * Checks if the user should have access to Storacha features.
     * Returns true if either:
     * 1. User has logged-in accounts, OR
     * 2. User has access to one or more spaces (space count > 0)
     *
     * @param context The application context
     * @return true if user should have access, false otherwise
     */
    fun shouldEnableStorachaAccess(context: Context): Boolean {
        val accountManager = StorachaAccountManager(context)

        // Check if user has logged-in accounts
        if (accountManager.hasLoggedInAccounts()) {
            return true
        }

        // Check if user has access to spaces
        val spaceCount = getSpaceCount(context)
        return spaceCount > 0
    }

    /**
     * Updates the stored space count
     *
     * @param context The application context
     * @param count The number of spaces the user has access to
     */
    fun updateSpaceCount(
        context: Context,
        count: Int,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_SPACE_COUNT, count) }
        invalidateAccountState()
    }

    /**
     * Gets the stored space count
     *
     * @param context The application context
     * @return The number of spaces the user has access to (default: 0)
     */
    fun getSpaceCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SPACE_COUNT, 0)
    }
}
