package net.opendasharchive.openarchive.ui

import android.accounts.AccountManager
import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.opendasharchive.openarchive.core.logger.AppLogger

/**
 * Dialog for staging/dev builds that prompts testers to identify themselves.
 *
 * This enables user-level tracking in Mixpanel for debugging purposes.
 * The email is stored locally and used to identify the user in analytics.
 *
 * Flow:
 * 1. First launch: Show dialog with device emails (from AccountManager) + manual option
 * 2. Tester selects or enters email
 * 3. Email stored in SharedPreferences
 * 4. Subsequent launches: Use stored email automatically
 *
 * WARNING: This should ONLY be used in staging/dev builds.
 */
class TesterEmailDialog(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get the stored tester email, if any.
     */
    fun getStoredEmail(): String? = prefs.getString(KEY_TESTER_EMAIL, null)

    /**
     * Check if the tester has been identified.
     */
    fun hasStoredEmail(): Boolean = getStoredEmail() != null

    /**
     * Clear the stored email (e.g., for testing or user request).
     */
    fun clearStoredEmail() {
        prefs.edit().remove(KEY_TESTER_EMAIL).apply()
        AppLogger.d("TesterEmailDialog", "Cleared stored tester email")
    }

    /**
     * Show the email prompt dialog.
     *
     * If device emails are available (from AccountManager), shows a selection list.
     * Otherwise, shows a text input for manual email entry.
     *
     * @param onEmailProvided Callback with the selected/entered email
     * @param onCancelled Callback if the dialog is cancelled (optional)
     */
    fun showEmailPrompt(
        onEmailProvided: (String) -> Unit,
        onCancelled: (() -> Unit)? = null
    ) {
        val deviceEmails = getDeviceEmails()

        if (deviceEmails.isNotEmpty()) {
            showEmailSelectionDialog(deviceEmails, onEmailProvided, onCancelled)
        } else {
            showManualEmailInput(onEmailProvided, onCancelled)
        }
    }

    /**
     * Get emails from device accounts using Android's AccountManager.
     * Requires GET_ACCOUNTS permission.
     */
    private fun getDeviceEmails(): List<String> {
        return try {
            val accountManager = AccountManager.get(context)
            accountManager.accounts
                .map { it.name }
                .filter { it.contains("@") }
                .distinct()
                .take(5) // Limit to 5 emails for UI
        } catch (e: SecurityException) {
            AppLogger.w("TesterEmailDialog", "GET_ACCOUNTS permission not granted", e)
            emptyList()
        } catch (e: Exception) {
            AppLogger.e("TesterEmailDialog", "Error getting device emails", e)
            emptyList()
        }
    }

    /**
     * Show dialog with device emails to select from.
     */
    private fun showEmailSelectionDialog(
        deviceEmails: List<String>,
        onEmailProvided: (String) -> Unit,
        onCancelled: (() -> Unit)?
    ) {
        val items = deviceEmails + "Enter manually..."

        MaterialAlertDialogBuilder(context)
            .setTitle("Staging Build - Tester Identification")
            .setMessage("Select your email to help us track issues during testing.\n\nThis is only used for internal debugging.")
            .setItems(items.toTypedArray()) { _, which ->
                if (which < deviceEmails.size) {
                    // Selected a device email
                    val email = deviceEmails[which]
                    saveAndProvide(email, onEmailProvided)
                } else {
                    // Selected "Enter manually..."
                    showManualEmailInput(onEmailProvided, onCancelled)
                }
            }
            .setCancelable(false) // Testers must identify for staging builds
            .setNegativeButton("Skip (Anonymous)") { _, _ ->
                // Allow skipping but log it
                AppLogger.w("TesterEmailDialog", "Tester skipped identification")
                onCancelled?.invoke()
            }
            .show()
    }

    /**
     * Show dialog with text input for manual email entry.
     */
    private fun showManualEmailInput(
        onEmailProvided: (String) -> Unit,
        onCancelled: (() -> Unit)?
    ) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = "your.email@example.com"
            setPadding(48, 32, 48, 32)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Enter Your Email")
            .setMessage("This email will be used to identify your testing session.\n\nIt is stored locally and only sent to our analytics dashboard.")
            .setView(container)
            .setPositiveButton("Continue") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty() && email.contains("@")) {
                    saveAndProvide(email, onEmailProvided)
                } else {
                    // Invalid email - show error and retry
                    showInvalidEmailError(onEmailProvided, onCancelled)
                }
            }
            .setCancelable(false)
            .setNegativeButton("Skip (Anonymous)") { _, _ ->
                AppLogger.w("TesterEmailDialog", "Tester skipped identification (manual)")
                onCancelled?.invoke()
            }
            .show()
    }

    /**
     * Show error for invalid email and prompt again.
     */
    private fun showInvalidEmailError(
        onEmailProvided: (String) -> Unit,
        onCancelled: (() -> Unit)?
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Invalid Email")
            .setMessage("Please enter a valid email address.")
            .setPositiveButton("Try Again") { _, _ ->
                showManualEmailInput(onEmailProvided, onCancelled)
            }
            .setNegativeButton("Skip (Anonymous)") { _, _ ->
                onCancelled?.invoke()
            }
            .show()
    }

    /**
     * Save email to SharedPreferences and invoke callback.
     */
    private fun saveAndProvide(email: String, onEmailProvided: (String) -> Unit) {
        prefs.edit().putString(KEY_TESTER_EMAIL, email).apply()
        AppLogger.i("TesterEmailDialog", "Tester identified: $email")
        onEmailProvided(email)
    }

    companion object {
        private const val PREFS_NAME = "staging_analytics"
        private const val KEY_TESTER_EMAIL = "tester_email"
    }
}
