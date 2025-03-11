package net.opendasharchive.openarchive.features.settings.passcode.passcode_setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.ComposeAppBar
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold

class PasscodeSetupActivity : BaseActivity() {

    companion object {
        const val EXTRA_PASSCODE_ENABLED = "passcode_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SaveAppTheme {
                DefaultScaffold(
                    topAppBar = {
                        ComposeAppBar(
                            title = "Lock app with passcode",
                            onNavigationAction = {
                                setResult(RESULT_CANCELED)
                                finish()
                            }
                        )
                    }
                ) {

                    // Handle back press inside Compose
                    BackHandler {
                        setResult(RESULT_CANCELED)
                        finish()
                    }

                    PasscodeSetupScreen(
                        onPasscodeSet = {
                            // Passcode successfully set
                            setResult(RESULT_OK, Intent().apply {
                                putExtra(EXTRA_PASSCODE_ENABLED, true)
                            })
                            finish()
                        },
                        onCancel = {
                            // User canceled the setup
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Cancel passcode setup
            setResult(Activity.RESULT_CANCELED)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}