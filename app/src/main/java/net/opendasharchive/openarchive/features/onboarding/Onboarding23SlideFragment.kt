package net.opendasharchive.openarchive.features.onboarding

import android.content.Context
import android.os.Bundle
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.databinding.FragmentOnboarding23SlideBinding

private const val ARG_TITLE = "title_param"
private const val ARG_SUMMARY = "summary_param"
private const val ARG_APP_LINK = "app_link_param"

class Onboarding23SlideFragment : Fragment() {

    private var mTitle: String? = null
    private var mSummary: String? = null
    private var mAppLink: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            mTitle = it.getString(ARG_TITLE)
            mSummary = it.getString(ARG_SUMMARY)
            mAppLink = it.getString(ARG_APP_LINK)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentOnboarding23SlideBinding.inflate(inflater)
        binding.title.text = mTitle

        // Format the summary text with the dynamic link
        mSummary?.let { summaryTemplate ->

            val formattedSummary = summaryTemplate.format(mAppLink)

            val spannedText: Spanned = HtmlCompat.fromHtml(formattedSummary, HtmlCompat.FROM_HTML_MODE_COMPACT)

            binding.summary.text = spannedText

            binding.summary.movementMethod = LinkMovementMethod.getInstance() // Enable link clicks
        }

        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(
            context: Context,
            @StringRes title: Int,
            @StringRes summary: Int,
            @StringRes link: Int? = null
        ) =
            Onboarding23SlideFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, context.getString(title))
                    putString(ARG_SUMMARY, context.getString(summary))
                    link?.let {
                        putString(ARG_APP_LINK, context.getString(it))
                    }
                }
            }
    }
}