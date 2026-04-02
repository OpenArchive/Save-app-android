package net.opendasharchive.openarchive.features.settings.passcode.passcode_entry

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseComposeActivity
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject

class PasscodeEntryActivity : BaseComposeActivity() {

    private val viewModel: PasscodeEntryViewModel by viewModel()
    private val repository: PasscodeRepository by inject()

    /** When true, the activity returns RESULT_OK on success (used for in-app verification). */
    private val isVerifyMode: Boolean
        get() = intent.getBooleanExtra(EXTRA_VERIFY_MODE, false)

    private val onBackPressedCallback = object : OnBackPressedCallback(enabled = true) {
        override fun handleOnBackPressed() {
            if (isVerifyMode) {
                setResult(RESULT_CANCELED)
                finish()
            } else {
                moveTaskToBack(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        // Check if passcode is locked
        if (repository.isLockedOut()) {
            Toast.makeText(
                this,
                getString(R.string.multiple_failed_attempts_message),
                Toast.LENGTH_LONG
            ).show()
            if (isVerifyMode) {
                setResult(RESULT_CANCELED)
                finish()
            } else {
                finishAndRemoveTask()
            }
            return
        }

        setContent {
            SaveAppTheme {
                DefaultScaffold {
                    PasscodeEntryScreen(
                        viewModel = viewModel,
                        onSuccess = {
                            if (isVerifyMode) {
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                finish()
                            }
                        },
                        onLockedOut = {
                            if (isVerifyMode) {
                                setResult(RESULT_CANCELED)
                                finish()
                            } else {
                                finishAndRemoveTask()
                            }
                        },
                        onExit = {
                            if (isVerifyMode) {
                                setResult(RESULT_CANCELED)
                                finish()
                            } else {
                                moveTaskToBack(true)
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        /** Pass as an extra to request verification-only mode (returns RESULT_OK on success). */
        const val EXTRA_VERIFY_MODE = "extra_verify_mode"
    }
}
