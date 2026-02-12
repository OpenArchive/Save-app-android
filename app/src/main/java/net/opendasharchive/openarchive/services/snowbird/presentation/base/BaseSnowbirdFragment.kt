package net.opendasharchive.openarchive.services.snowbird.presentation.base

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.api.session.SessionTracker
import net.opendasharchive.openarchive.core.domain.DomainError
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.services.snowbird.SnowbirdActivity
import net.opendasharchive.openarchive.util.FullScreenOverlayManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

abstract class BaseSnowbirdFragment : Fragment(), ToolbarConfigurable {

    protected val dialogManager: DialogStateManager by activityViewModel()
    protected val analyticsManager: AnalyticsManager by inject()
    protected val sessionTracker: SessionTracker by inject()

    private var screenStartTime: Long = 0
    private var previousScreen: String = ""

    protected open fun getScreenName(): String = this::class.simpleName ?: "UnknownSnowbirdFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ensureComposeDialogHost()
    }

    private fun ensureComposeDialogHost() {
        (requireActivity() as? BaseActivity)?.ensureComposeDialogHost()
    }

    override fun onResume() {
        super.onResume()
        (activity as? SnowbirdActivity)?.updateToolbarFromFragment(this)

        screenStartTime = System.currentTimeMillis()
        val screenName = getScreenName()
        AppLogger.setCurrentScreen(screenName)

        lifecycleScope.launch {
            analyticsManager.trackScreenView(screenName, null, previousScreen)
        }
        sessionTracker.setCurrentScreen(screenName)

        if (previousScreen.isNotEmpty() && previousScreen != screenName) {
            lifecycleScope.launch {
                analyticsManager.trackNavigation(previousScreen, screenName)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val timeSpent = (System.currentTimeMillis() - screenStartTime) / 1000
        val screenName = getScreenName()

        lifecycleScope.launch {
            analyticsManager.trackScreenView(screenName, timeSpent, previousScreen)
        }

        previousScreen = screenName
    }
}