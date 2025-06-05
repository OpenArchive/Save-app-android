package net.opendasharchive.openarchive.features.onboarding

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.view.Window
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityOnboarding23Binding
import net.opendasharchive.openarchive.features.core.BaseActivity

class Onboarding23Activity : BaseActivity() {

    private lateinit var mBinding: ActivityOnboarding23Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        mBinding = ActivityOnboarding23Binding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.getStarted.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    Onboarding23InstructionsActivity::class.java
                )
            )
        }

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

        val oa = ObjectAnimator.ofFloat(mBinding.arrow, "translationX", 0F, 25F, 0F)
        oa.interpolator = BounceInterpolator()
        oa.startDelay = 3000
        oa.duration = 2000
        oa.repeatCount = 999999
        oa.start()
    }

    private fun colorizeFirstLetter(text: CharSequence, @ColorRes color: Int): Spanned {
        val colorHexString =
            Integer.toHexString(0xffffff and ContextCompat.getColor(this, color))
        val html =
            "<font color=\"#${colorHexString}\">${text.substring(0, 1)}</font>${text.substring(1)}"
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}