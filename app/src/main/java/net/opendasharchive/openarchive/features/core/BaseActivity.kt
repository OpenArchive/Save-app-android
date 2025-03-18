package net.opendasharchive.openarchive.features.core

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.appbar.MaterialToolbar
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.dialog.DialogHost
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.util.Prefs
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class BaseActivity : AppCompatActivity() {

    val dialogManager: DialogStateManager by viewModel()

    companion object {
        const val EXTRA_DATA_SPACE = "space"
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        ensureComposeDialogHost()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        ensureComposeDialogHost()
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
                        // Get ViewModel scoped to this activity
                        val dialogManager: DialogStateManager = koinViewModel()
                        DialogHost(dialogStateManager = dialogManager)
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
}