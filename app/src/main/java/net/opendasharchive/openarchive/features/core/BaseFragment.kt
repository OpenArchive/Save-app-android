package net.opendasharchive.openarchive.features.core

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.extensions.androidViewModel
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoViewModel
import net.opendasharchive.openarchive.services.storacha.util.SessionManager
import net.opendasharchive.openarchive.services.storacha.util.StorachaAccountManager
import net.opendasharchive.openarchive.util.FullScreenOverlayManager
import org.koin.android.ext.android.inject

abstract class BaseFragment : Fragment(), ToolbarConfigurable {

    protected val dialogManager: DialogStateManager by activityViewModels()

    val snowbirdGroupViewModel: SnowbirdGroupViewModel by androidViewModel()
    val snowbirdRepoViewModel: SnowbirdRepoViewModel by androidViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ensureComposeDialogHost()
    }

    private fun ensureComposeDialogHost() {
        (requireActivity() as? BaseActivity)?.ensureComposeDialogHost()
    }

    open fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    open fun handleError(error: SnowbirdError) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            title = UiText.DynamicString("Oops")
            message = UiText.DynamicString(error.friendlyMessage)
            positiveButton {
                text = UiText.StringResource(R.string.lbl_ok)
            }
        }
    }

    open fun handleLoadingStatus(isLoading: Boolean) {
        if (isLoading) {
            FullScreenOverlayManager.show(this@BaseFragment)
        } else {
            FullScreenOverlayManager.hide()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? SpaceSetupActivity)?.updateToolbarFromFragment(this)
    }

    /**
     * Shows a dialog informing the user that their session has expired.
     * Common method for all fragments to handle session expiration.
     * Automatically removes the invalid account and navigates to login.
     */
    protected fun showSessionExpiredDialog() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Warning
            title = UiText.StringResource(R.string.session_expired_title)
            message = UiText.StringResource(R.string.session_expired_message)
            positiveButton {
                text = UiText.StringResource(R.string.lbl_ok)
                action = {
                    try {
                        // Remove invalid account
                        val sessionManager: SessionManager by inject()
                        sessionManager.removeCurrentAccount()

                        // Navigate to login with cleared back stack
                        findNavController().navigate(
                            R.id.fragment_storacha_login,
                            null,
                            androidx.navigation.navOptions {
                                popUpTo(R.id.fragment_storacha) {
                                    inclusive = false
                                }
                            }
                        )
                    } catch (e: Exception) {
                        // Navigation might fail if not in Storacha nav graph
                    }
                }
            }
        }
    }

    /**
     * Shows a dialog informing the user that their session has expired,
     * with an option to stay on the current screen (for screens that work without auth,
     * like browsing delegated spaces).
     *
     * @param onStayHere Callback when "Stay Here" is clicked (clear flag and refresh without session)
     */
    protected fun showSessionExpiredWithStayOption(onStayHere: () -> Unit = {}) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Warning
            title = UiText.StringResource(R.string.session_expired_title)
            message = UiText.StringResource(R.string.session_expired_can_continue_message)
            positiveButton {
                text = UiText.StringResource(R.string.login)
                action = {
                    try {
                        // Remove invalid account
                        val sessionManager: SessionManager by inject()
                        sessionManager.removeCurrentAccount()

                        // Navigate to login with cleared back stack
                        findNavController().navigate(
                            R.id.fragment_storacha_login,
                            null,
                            androidx.navigation.navOptions {
                                popUpTo(R.id.fragment_storacha) {
                                    inclusive = false
                                }
                            }
                        )
                    } catch (e: Exception) {
                        // Navigation might fail if not in Storacha nav graph
                    }
                }
            }
            neutralButton {
                text = UiText.StringResource(R.string.stay_here)
                action = {
                    // Remove invalid account but don't navigate to login
                    // User can continue browsing delegated spaces without authentication
                    try {
                        val sessionManager: SessionManager by inject()
                        sessionManager.removeCurrentAccount()
                    } catch (e: Exception) {
                        // Continue even if removal fails
                    }

                    // Call callback to clear flag and refresh
                    onStayHere()
                }
            }
        }
    }
}