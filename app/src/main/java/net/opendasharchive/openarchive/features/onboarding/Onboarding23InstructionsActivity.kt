package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityOnboarding23InstructionsBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme

class Onboarding23InstructionsActivity : BaseActivity() {

    private lateinit var mBinding: ActivityOnboarding23InstructionsBinding
    
    // Toggle to switch between XML and Compose implementation
    private val useComposeImplementation = true  // Set to false to use XML implementation

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        )
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        if (useComposeImplementation) {
            // Use Compose implementation
            setContent {
                SaveAppTheme {
                    OnboardingInstructionsScreen(
                        onDone = { done() },
                        onBackPressed = { finish() }
                    )
                }
            }
        } else {
            // Use original XML implementation
            setupXmlImplementation()
        }
    }
    
    private fun setupXmlImplementation() {
        mBinding = ActivityOnboarding23InstructionsBinding.inflate(layoutInflater)

        mBinding.fab.applyEdgeToEdgeInsets { insets ->
            bottomMargin = insets.bottom
        }

        setContentView(mBinding.root)

        mBinding.skipButton.setOnClickListener {
            done()
        }

        mBinding.viewPager.adapter =
            Onboarding23FragmentStateAdapter(supportFragmentManager, lifecycle, this)

        mBinding.dotsIndicator.attachTo(mBinding.viewPager)

        mBinding.fab.setOnClickListener {
            if (isLastPage()) {
                done()
            } else {
                mBinding.coverImage.alpha = 0F
                mBinding.viewPager.currentItem++
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFirstPage()) {
                    finish()
                } else {
                    mBinding.viewPager.currentItem--
                }
            }
        })

        mBinding.viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (isLastPage()) {
                    mBinding.skipButton.visibility = View.INVISIBLE
                    val icon = ContextCompat.getDrawable(mBinding.fab.context, R.drawable.ic_done)
                    mBinding.fab.setImageDrawable(icon)
                } else {
                    mBinding.skipButton.visibility = View.VISIBLE
                    val icon = ContextCompat.getDrawable(mBinding.fab.context, R.drawable.ic_arrow_forward_ios,)
                    icon?.isAutoMirrored = true
                    mBinding.fab.setImageDrawable(icon)
                }

            }

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> mBinding.coverImage.alpha = 0F
                    ViewPager2.SCROLL_STATE_IDLE -> updateCoverImage()
                    ViewPager2.SCROLL_STATE_SETTLING -> { /* ignored */ }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        
        if (useComposeImplementation) {
            hideSystemBars()
        }

        if (!useComposeImplementation) {
            updateCoverImage()
        }
    }

    private fun updateCoverImage() {
        when (mBinding.viewPager.currentItem) {
            0 -> mBinding.coverImage.setImageResource(R.drawable.onboarding_secure_png)
            1 -> mBinding.coverImage.setImageResource(R.drawable.onboarding_archive_png)
            2 -> mBinding.coverImage.setImageResource(R.drawable.onboarding_verify_png)
            3 -> mBinding.coverImage.setImageResource(R.drawable.onboarding_encrypt_png)
        }
        mBinding.coverImage.alpha = 0F
        mBinding.coverImage.animate().setDuration(200L).alpha(1F).start()
    }

    private fun isFirstPage(): Boolean {
        return if (useComposeImplementation) false else mBinding.viewPager.currentItem <= 0
    }

    private fun isLastPage(): Boolean {
        return if (useComposeImplementation) {
            false
        } else {
            val pageCount: Int =
                if (mBinding.viewPager.adapter == null) 0 else mBinding.viewPager.adapter?.itemCount!!
            mBinding.viewPager.currentItem + 1 >= pageCount
        }
    }

    private fun done() {
        // Hide keyboard before finishing activity
        val imm = getSystemService(InputMethodManager::class.java)
        currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()  // Remove focus from any input field
        }

        Prefs.didCompleteOnboarding = true
        // We are moving space setup to MainActivity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}