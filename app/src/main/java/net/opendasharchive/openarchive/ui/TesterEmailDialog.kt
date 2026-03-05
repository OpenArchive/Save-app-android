package net.opendasharchive.openarchive.ui

import android.accounts.AccountManager
import android.content.Context
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme

/**
 * Dialog for staging/dev builds that prompts testers to identify themselves.
 *
 * Styled consistently with StorachaWarningDialog using Material3 Compose.
 */
class TesterEmailDialog(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStoredEmail(): String? = prefs.getString(KEY_TESTER_EMAIL, null)

    fun hasStoredEmail(): Boolean = getStoredEmail() != null

    fun clearStoredEmail() {
        prefs.edit().remove(KEY_TESTER_EMAIL).apply()
        AppLogger.d(TAG, "Cleared stored tester email")
    }

    fun showEmailPrompt(
        onEmailProvided: (String) -> Unit,
        onCancelled: (() -> Unit)? = null,
    ) {
        val deviceEmails = getDeviceEmails()

        val dialog = ComponentDialog(context)
        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                SaveAppTheme {
                    TesterEmailContent(
                        deviceEmails = deviceEmails,
                        onEmailProvided = { email ->
                            saveAndProvide(email, onEmailProvided)
                            dialog.dismiss()
                        },
                        onSkipped = {
                            AppLogger.w(TAG, "Tester skipped identification")
                            dialog.dismiss()
                            onCancelled?.invoke()
                        },
                    )
                }
            }
        }

        dialog.setContentView(composeView)
        dialog.show()
    }

    private fun getDeviceEmails(): List<String> {
        return try {
            AccountManager.get(context).accounts
                .map { it.name }
                .filter { it.contains("@") }
                .distinct()
                .take(5)
        } catch (e: SecurityException) {
            AppLogger.w(TAG, "GET_ACCOUNTS permission not granted", e)
            emptyList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting device emails", e)
            emptyList()
        }
    }

    private fun saveAndProvide(email: String, onEmailProvided: (String) -> Unit) {
        prefs.edit().putString(KEY_TESTER_EMAIL, email).apply()
        AppLogger.i(TAG, "Tester identified: $email")
        onEmailProvided(email)
    }

    companion object {
        private const val TAG = "TesterEmailDialog"
        private const val PREFS_NAME = "staging_analytics"
        private const val KEY_TESTER_EMAIL = "tester_email"
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun TesterEmailContent(
    deviceEmails: List<String>,
    onEmailProvided: (String) -> Unit,
    onSkipped: () -> Unit,
) {
    var showManualInput by remember { mutableStateOf(deviceEmails.isEmpty()) }
    var emailText by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Staging Build — Tester ID",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (!showManualInput && deviceEmails.isNotEmpty()) {
                    "Select your email to help us track issues during testing. This is only used for internal debugging."
                } else {
                    "Enter your email to help us track issues during testing. It is stored locally and only sent to our analytics dashboard."
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!showManualInput && deviceEmails.isNotEmpty()) {
                deviceEmails.forEach { email ->
                    OutlinedButton(
                        onClick = { onEmailProvided(email) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = email,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = { showManualInput = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Enter manually...")
                }
            } else {
                OutlinedTextField(
                    value = emailText,
                    onValueChange = { emailText = it; emailError = false },
                    label = { Text("Email") },
                    placeholder = { Text("your.email@example.com") },
                    isError = emailError,
                    supportingText = if (emailError) {
                        { Text("Please enter a valid email address") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onSkipped,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                    )
                }

                if (showManualInput || deviceEmails.isEmpty()) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val email = emailText.trim()
                            if (email.isNotEmpty() && email.contains("@")) {
                                onEmailProvided(email)
                            } else {
                                emailError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
