package net.opendasharchive.openarchive.features.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity

abstract class BaseFragment : Fragment(), ToolbarConfigurable {

    protected val dialogManager: DialogStateManager by activityViewModels()

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
        (requireActivity() as? BaseActivity)?.ensureComposeDialogHost()
    }



    override fun onResume() {
        super.onResume()
        (activity as? SpaceSetupActivity)?.updateToolbarFromFragment(this)

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
}