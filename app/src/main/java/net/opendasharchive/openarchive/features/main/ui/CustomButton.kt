package net.opendasharchive.openarchive.features.main.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import net.opendasharchive.openarchive.R

class CustomButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val leftIcon: ImageView
    private val rightIcon: ImageView
    private val titleText: TextView
    private val subTitleText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.custom_button, this, true)
        leftIcon = findViewById(R.id.leftIcon)
        rightIcon = findViewById(R.id.rightIcon)
        titleText = findViewById(R.id.title)
        subTitleText = findViewById(R.id.subTitle)

        isClickable = true
        isFocusable = true

        subTitleText.visibility = GONE
    }

    fun setLeftIcon(drawable: Drawable?) {
        leftIcon.setImageDrawable(drawable)
    }

    fun setLeftResource(iconResId: Int) {
        leftIcon.setImageResource(iconResId)
    }

    fun setRightResource(iconResId: Int) {
        rightIcon.setImageResource(iconResId)
    }

    fun setRightIcon(drawable: Drawable?) {
        rightIcon.setImageDrawable(drawable)
    }

    fun setTitle(text: String?) {
        titleText.text = text ?: ""
    }

    fun setSubTitle(text: String?) {
        text?.let {
            subTitleText.text = text
            subTitleText.visibility = VISIBLE
        }
    }
}