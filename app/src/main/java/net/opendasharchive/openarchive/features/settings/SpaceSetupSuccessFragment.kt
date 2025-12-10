package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupSuccessBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets

class SpaceSetupSuccessFragment : BaseFragment() {

    private lateinit var binding: FragmentSpaceSetupSuccessBinding
    private val args: SpaceSetupSuccessFragmentArgs by navArgs()

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

        if (args.message.isNotEmpty()) {
            binding.successMessage.text = args.message
        }

        binding.btAuthenticate.setOnClickListener { _ ->
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clears backstack
                startActivity(intent)
        }

        return binding.root
    }

    override fun getToolbarTitle() = getString(R.string.space_setup_success_title)
    override fun shouldShowBackButton() = false
}