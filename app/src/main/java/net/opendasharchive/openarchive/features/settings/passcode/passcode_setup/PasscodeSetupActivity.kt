package net.opendasharchive.openarchive.features.settings.passcode.passcode_setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.ui.res.stringResource
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.ComposeAppBar
import net.opendasharchive.openarchive.features.settings.passcode.HapticManager
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import org.koin.android.ext.android.inject

class PasscodeSetupActivity : BaseActivity() {

    private val hapticManager: HapticManager by inject()

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
                            title = stringResource(R.string.passcode_lock_app),
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

    override fun onDestroy() {
        super.onDestroy()
        hapticManager.clear() // Clear the reference to prevent leaks
    }
}