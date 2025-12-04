package net.opendasharchive.openarchive.features.onboarding

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.view.Window
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.databinding.ActivityOnboarding23Binding
import net.opendasharchive.openarchive.features.core.BaseActivity

class Onboarding23Activity : BaseActivity() {

    private lateinit var mBinding: ActivityOnboarding23Binding

    // Toggle to switch between XML and Compose implementation
    private val useComposeImplementation = true  // Set to false to use XML implementation

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        if (useComposeImplementation) {
            // Use Compose implementation
            setContent {
                SaveAppTheme {
                    OnboardingWelcomeScreen(
                        onGetStartedClick = {
                            startActivity(
                                Intent(
                                    this@Onboarding23Activity,
                                    Onboarding23InstructionsActivity::class.java
                                )
                            )
                        }
                    )
                }
            }
        } else {
            // Use original XML implementation
            setupXmlImplementation()
        }
    }

    private fun setupXmlImplementation() {
        // Keep the existing XML binding for legacy compatibility
        mBinding = ActivityOnboarding23Binding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Keep the original XML-based code for reference and fallback
        mBinding.getStarted.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    Onboarding23InstructionsActivity::class.java
                )
            )
        }

        // Handle back button to exit app instead of returning to MainActivity
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Exit the app when back is pressed during onboarding
                finishAffinity()
            }
        })

        for (textView in arrayOf(
            mBinding.titleBlock.shareText,
            mBinding.titleBlock.archiveText,
            mBinding.titleBlock.verifyText,
            mBinding.titleBlock.encryptText
        )) {
            textView.text = colorizeFirstLetter(textView.text, R.color.colorTertiary)
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()

        if (!useComposeImplementation) {
            // Animation is handled in Compose version
            val oa = ObjectAnimator.ofFloat(mBinding.arrow, "translationX", 0F, 25F, 0F)
            oa.interpolator = BounceInterpolator()
            oa.startDelay = 3000
            oa.duration = 2000
            oa.repeatCount = 999999
            oa.start()
        }
    }

    private fun colorizeFirstLetter(text: CharSequence, @ColorRes color: Int): Spanned {
        val colorHexString =
            Integer.toHexString(0xffffff and ContextCompat.getColor(this, color))
        val html =
            "<font color=\"#${colorHexString}\">${text.substring(0, 1)}</font>${text.substring(1)}"
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}