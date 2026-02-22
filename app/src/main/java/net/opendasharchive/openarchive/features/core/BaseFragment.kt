package net.opendasharchive.openarchive.features.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.api.session.SessionTracker
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.services.storacha.StorachaToolbarConfig
import net.opendasharchive.openarchive.services.storacha.StorachaToolbarState
import net.opendasharchive.openarchive.services.storacha.util.SessionManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

abstract class BaseFragment : Fragment(), ToolbarConfigurable {

    protected val dialogManager: DialogStateManager by activityViewModel()

    // Inject analytics dependencies
    protected val analyticsManager: AnalyticsManager by inject()
    protected val sessionTracker: SessionTracker by inject()

    // Screen tracking variables
    private var screenStartTime: Long = 0
    private var previousScreen: String = ""

    protected open fun getScreenName(): String {
        return this::class.simpleName ?: "UnknownFragment"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ensureComposeDialogHost()
    }

    private fun ensureComposeDialogHost() {
        // Dialog host is managed by BaseComposeActivity in the Compose architecture
    }



    override fun onResume() {
        super.onResume()

        if (this is ToolbarConfigurable) {
            StorachaToolbarState.update(
                StorachaToolbarConfig(
                    title = getToolbarTitle(),
                    showBack = shouldShowBackButton(),
                    actions = getToolbarActions(),
                )
            )
        }

        // Track screen view
        screenStartTime = System.currentTimeMillis()
        val screenName = getScreenName()

        // Set current screen for error tracking breadcrumbs
        AppLogger.setCurrentScreen(screenName)

        lifecycleScope.launch {
            analyticsManager.trackScreenView(screenName, null, previousScreen)
        }
        sessionTracker.setCurrentScreen(screenName)

        // Track navigation if coming from another screen
        if (previousScreen.isNotEmpty() && previousScreen != screenName) {
            lifecycleScope.launch {
                analyticsManager.trackNavigation(previousScreen, screenName)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Track time spent on screen
        val timeSpent = (System.currentTimeMillis() - screenStartTime) / 1000
        val screenName = getScreenName()

        lifecycleScope.launch {
            analyticsManager.trackScreenView(screenName, timeSpent, previousScreen)
        }

        // Store as previous screen for navigation tracking
        previousScreen = screenName
    }

    /**
     * Shows a dialog informing the user that their session has expired.
     * Common method for all fragments to handle session expiration.
     * Automatically removes the invalid account and navigates to login.
     */
    protected fun showSessionExpiredDialog() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Warning
            title = UiText.Resource(R.string.session_expired_title)
            message = UiText.Resource(R.string.session_expired_message)
            positiveButton {
                text = UiText.Resource(R.string.lbl_ok)
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
            title = UiText.Resource(R.string.session_expired_title)
            message = UiText.Resource(R.string.session_expired_can_continue_message)
            positiveButton {
                text = UiText.Resource(R.string.login)
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
                text = UiText.Resource(R.string.stay_here)
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