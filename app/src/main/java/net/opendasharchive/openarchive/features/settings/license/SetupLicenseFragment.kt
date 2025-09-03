package net.opendasharchive.openarchive.features.settings.license

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSetupLicenseBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets

class SetupLicenseFragment : BaseFragment() {


    private val args: SetupLicenseFragmentArgs by navArgs()

    private lateinit var binding: FragmentSetupLicenseBinding


    private lateinit var mSpace: Space

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSetupLicenseBinding.inflate(layoutInflater)

        binding.buttonBar.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars()
        ) { insets ->
            bottomMargin = insets.bottom
        }

        mSpace = Space.get(args.spaceId) ?: Space(Space.Type.WEBDAV)

        if (args.isEditing) {
            // Editing means hide subtitle, bottom bar buttons
            binding.buttonBar.visibility = View.GONE
            binding.descriptionText.visibility = View.GONE
        } else {
            binding.btCancel.visibility = View.INVISIBLE
        }

        if (args.spaceType == Space.Type.INTERNET_ARCHIVE) {
            binding.serverNameLayout.visibility = View.GONE
        }

        binding.btNext.setOnClickListener {
            when (args.spaceType) {
                Space.Type.WEBDAV -> {
                    val message = getString(R.string.you_have_successfully_connected_to_a_private_server)
                    val action = SetupLicenseFragmentDirections.actionFragmentSetupLicenseToFragmentSpaceSetupSuccess(message, args.spaceType)
                    findNavController().navigate(action)
                }

                Space.Type.INTERNET_ARCHIVE -> {
                    val message = getString(R.string.you_have_successfully_connected_to_the_internet_archive)
                    val action = SetupLicenseFragmentDirections.actionFragmentSetupLicenseToFragmentSpaceSetupSuccess(message, args.spaceType)
                    findNavController().navigate(action)
                }
                else -> Unit
            }

        }

        binding.btCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cc.tvCcLabel.setText(R.string.set_creative_commons_license_for_all_folders_on_this_server)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.isEditing) {
            // Editing means hide subtitle, bottom bar buttons
            binding.name.setText(mSpace.name)
        }

        binding.name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Do nothing
            }

            override fun afterTextChanged(name: Editable?) {
                if (name == null) return

                mSpace.name = name.toString()
                mSpace.save()
                //binding.name.clearFocus()
            }
        })

        CreativeCommonsLicenseManager.initialize(binding.cc, Space.current?.license) {
            val space = Space.current ?: return@initialize

            space.license = it
            space.save()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // do nothing
                }
            })
    }

    override fun getToolbarTitle() = getString(R.string.private_server)
    override fun getToolbarSubtitle(): String? = null
    override fun shouldShowBackButton() = false
}