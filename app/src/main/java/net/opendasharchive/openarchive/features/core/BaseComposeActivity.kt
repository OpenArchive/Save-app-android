package net.opendasharchive.openarchive.features.core

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.api.session.SessionTracker
import net.opendasharchive.openarchive.core.security.SecurityManager
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import android.os.Bundle
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import kotlin.getValue

abstract class BaseComposeActivity : AppCompatActivity() {

    val dialogManager: DialogStateManager by inject<DialogStateManager>()
    protected val securityManager: SecurityManager by inject()

    // Inject analytics dependencies
    protected val analyticsManager: AnalyticsManager by inject()
    protected val sessionTracker: SessionTracker by inject()

    // Screen tracking variables
    private var screenStartTime: Long = 0
    private var previousScreen: String = ""

    protected open fun getScreenName(): String {
        return this::class.simpleName ?: "UnknownActivity"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeSecuritySettings()
    }

    private fun observeSecuritySettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                securityManager.isSecureRequired.collect { isRequired ->
                    applySecureFlag(isRequired)
                }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            val obscuredTouch = event.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0
            if (obscuredTouch) return false
        }

        return super.dispatchTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()

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

    fun applySecureFlag(isRequired: Boolean) {
        if (isRequired) {
            // Prevent screenshots and recent apps preview
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    @Deprecated("Use applySecureFlag via SecurityManager observation")
    fun updateScreenshotPrevention() {
        applySecureFlag(securityManager.isSecureRequired.value)
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogManager.dismissDialog()
    }
}