package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import net.opendasharchive.openarchive.BuildConfig
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class Onboarding23InstructionsActivity : BaseActivity() {

    private lateinit var mBinding: ActivityOnboarding23InstructionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        )
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        mBinding = ActivityOnboarding23InstructionsBinding.inflate(layoutInflater)

        mBinding.fab.applyEdgeToEdgeInsets { insets ->
            bottomMargin = insets.bottom
        }

        setContentView(mBinding.root)

        if (BuildConfig.ENHANCED_ANALYTICS_ENABLED) {
            mBinding.testingBanner?.let { banner ->
                banner.visibility = View.VISIBLE
                ViewCompat.setOnApplyWindowInsetsListener(banner) { v, insets ->
                    val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                    v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, navBarHeight)
                    insets
                }
            }
        }

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

        updateCoverImage()
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
        return mBinding.viewPager.currentItem <= 0
    }

    private fun isLastPage(): Boolean {
        val pageCount: Int =
            if (mBinding.viewPager.adapter == null) 0 else mBinding.viewPager.adapter?.itemCount!!
        return mBinding.viewPager.currentItem + 1 >= pageCount
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