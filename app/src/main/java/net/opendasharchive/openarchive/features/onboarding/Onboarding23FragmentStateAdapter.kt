package net.opendasharchive.openarchive.features.onboarding

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.onboarding.Onboarding23SlideFragment.Companion.newInstance

class Onboarding23FragmentStateAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    context: Context
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val context: Context = context.applicationContext

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return newInstance(
                context,
                R.string.intro_header_secure,
                R.string.intro_text_secure,
            )

            1 -> return newInstance(
                context,
                R.string.intro_header_archive,
                R.string.intro_text_archive,
                R.string.intro_link_archive,
            )

            2 -> return newInstance(
                context,
                R.string.intro_header_verify,
                R.string.intro_text_verify,
                R.string.intro_link_verify,
            )

            3 -> return newInstance(
                context,
                R.string.intro_header_encrypt,
                R.string.intro_text_encrypt,
                R.string.intro_link_encrypt,
            )
        }
        throw IndexOutOfBoundsException()
    }

    override fun getItemCount(): Int {
        return 4
    }
}