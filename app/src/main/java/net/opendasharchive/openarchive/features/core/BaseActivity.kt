package net.opendasharchive.openarchive.features.core

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.api.session.SessionTracker
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.dialog.DialogHost
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.util.Prefs
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class BaseActivity : AppCompatActivity() {

    val dialogManager: DialogStateManager by viewModel()

    // Inject analytics dependencies
    protected val analyticsManager: AnalyticsManager by inject()
    protected val sessionTracker: SessionTracker by inject()

    // Screen tracking variables
    private var screenStartTime: Long = 0
    private var previousScreen: String = ""

    protected open fun getScreenName(): String {
        return this::class.simpleName ?: "UnknownActivity"
    }

    companion object {
        const val EXTRA_DATA_SPACE = "space"
        private const val TESTING_BANNER_TAG = "testing_banner"
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        ensureComposeDialogHost()
        ensureTestingBanner()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        ensureComposeDialogHost()
        ensureTestingBanner()
    }

    protected open fun showTestingBanner(): Boolean = false

    private fun ensureTestingBanner() {
        if (!net.opendasharchive.openarchive.BuildConfig.ENHANCED_ANALYTICS_ENABLED) return
        if (!showTestingBanner()) return
        val rootView = window.decorView.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (rootView.findViewWithTag<View>(TESTING_BANNER_TAG) != null) return
        val banner = TextView(this).apply {
            tag = TESTING_BANNER_TAG
            text = "⚠ TESTING ONLY — NOT FOR PUBLIC USE"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC8B0000"))
            gravity = Gravity.CENTER
        }
        // Apply bottom inset so it clears the navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(banner) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(0, 20, 0, 20 + navBar.bottom)
            insets
        }
        rootView.addView(
            banner,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )
    }

    fun ensureComposeDialogHost() {
        // Get root view of the window
        val rootView = findViewById<ViewGroup>(android.R.id.content)

        // Add ComposeView if not already present
        if (rootView.findViewById<ComposeView>(R.id.compose_dialog_host) == null) {
            ComposeView(this).apply {
                id = R.id.compose_dialog_host
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                rootView.addView(this)

                setContent {
                    SaveAppTheme {
                        DialogHost(dialogStateManager = this@BaseActivity.dialogManager)
                    }
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

        // updating this in onResume (previously was in onCreate) to make sure setting changes get
        // applied instantly instead after the next app restart
        updateScreenshotPrevention()

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

    fun updateScreenshotPrevention() {
        if (Prefs.passcodeEnabled || Prefs.prohibitScreenshots) {
            // Prevent screenshots and recent apps preview
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun setupToolbar(
        title: String = "",
        subtitle: String? = null,
        showBackButton: Boolean = true
    ) {
        val toolbar: MaterialToolbar = findViewById(R.id.common_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = title

        if (subtitle != null) {
            supportActionBar?.subtitle = subtitle
        }

        if (showBackButton) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_ios)
            toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogManager.dismissDialog()
    }
}