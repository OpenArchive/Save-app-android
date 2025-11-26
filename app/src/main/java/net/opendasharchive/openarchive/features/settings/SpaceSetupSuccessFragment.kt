package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.setFragmentResult
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupSuccessBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets

class SpaceSetupSuccessFragment : BaseFragment() {
    private lateinit var binding: FragmentSpaceSetupSuccessBinding
    private var message = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            message = it.getString(ARG_MESSAGE, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSpaceSetupSuccessBinding.inflate(inflater)

        binding.mainContainer.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars()
        ) { insets ->
            bottomMargin = insets.bottom
        }

        binding.buttonBar.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars()
        ) { insets ->
            bottomMargin = insets.bottom
        }

        if (message.isNotEmpty()) {
            binding.successMessage.text = message
        }

        binding.btAuthenticate.setOnClickListener { _ ->
            val intent = Intent(requireActivity(), MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clears backstack
            startActivity(intent)
        }

        return binding.root
    }

    companion object {
        const val ARG_MESSAGE = "message"
    }

    override fun getToolbarTitle() = getString(R.string.space_setup_success_title)
    override fun shouldShowBackButton() = false
}