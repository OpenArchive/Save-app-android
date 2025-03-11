package net.opendasharchive.openarchive.features.settings.passcode.passcode_entry

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import org.koin.android.ext.android.inject

class PasscodeEntryActivity : BaseActivity() {

    private val repository: PasscodeRepository by inject()

    private val onBackPressedCallback = object : OnBackPressedCallback(enabled = true) {
        override fun handleOnBackPressed() {
            // Do nothing to prevent back navigation
            moveTaskToBack(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the OnBackPressedCallback
        onBackPressedDispatcher.addCallback(onBackPressedCallback)


        // Check if passcode is locked
        if (repository.isLockedOut()) {
            Toast.makeText(
                this,
                "App is locked due to multiple failed attempts. Please try again later.",
                Toast.LENGTH_LONG
            ).show()
            finishAndRemoveTask()
            return
        }

        setContent {
            SaveAppTheme {
                DefaultScaffold {
                    PasscodeEntryScreen(
                        onPasscodeSuccess = {
                            finish()
                        },
                        onExit = {
                            finishAffinity()
                        }
                    )
                }
            }
        }
    }
}