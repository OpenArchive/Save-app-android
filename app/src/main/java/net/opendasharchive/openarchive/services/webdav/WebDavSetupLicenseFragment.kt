package net.opendasharchive.openarchive.services.webdav

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentWebdavSetupLicenseBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.settings.CreativeCommonsLicenseManager
import kotlin.properties.Delegates

class WebDavSetupLicenseFragment: BaseFragment() {

    private lateinit var binding: FragmentWebdavSetupLicenseBinding

    private var mSpaceId by Delegates.notNull<Long>()

    private lateinit var mSpace: Space

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentWebdavSetupLicenseBinding.inflate(layoutInflater)
        mSpaceId = arguments?.getLong(ARG_SPACE_ID)!!
        mSpace = Space.get(mSpaceId) ?: Space(Space.Type.WEBDAV)

        val isEditing = arguments?.getBoolean(ARG_IS_EDITING) ?: false

        if(isEditing) {
            // Editing means hide subtitle, bottom bar buttons
            binding.buttonBar.visibility = View.GONE
            binding.descriptionText.visibility = View.GONE
        } else {
            binding.btCancel.visibility = View.INVISIBLE
        }


        binding.btNext.setOnClickListener {
            if (isJetpackNavigation) {
                val action = WebDavSetupLicenseFragmentDirections.actionFragmentWebDavSetupLicenseToFragmentSpaceSetupSuccess(message = getString(R.string.you_have_successfully_connected_to_a_private_server))
                findNavController().navigate(action)
            } else {
                setFragmentResult(RESP_SAVED, bundleOf())
            }
        }

        binding.btCancel.setOnClickListener {
            if (isJetpackNavigation) {
                findNavController().popBackStack()
            } else {
                setFragmentResult(RESP_CANCEL, bundleOf())
            }
        }

        binding.cc.tvCcLabel.setText(R.string.set_creative_commons_license_for_all_folders_on_this_server)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isEditing = arguments?.getBoolean(ARG_IS_EDITING) ?: false

        if(isEditing) {
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
                binding.name.clearFocus()
            }
        })

        CreativeCommonsLicenseManager.initialize(binding.cc, Space.current?.license) {
            val space = Space.current ?: return@initialize

            space.license = it
            space.save()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                // do nothing
            }
        })
    }

    companion object {

        // events emitted by this fragment
        const val RESP_SAVED = "webdav_setup_license_fragment_resp_saved"
        const val RESP_CANCEL = "webdav_setup_license_fragment_resp_cancel"

        const val ARG_SPACE_ID = "space_id"
        const val ARG_IS_EDITING = "is_editing"

        @JvmStatic
        fun newInstance(spaceId: Long, isEditing: Boolean) = WebDavSetupLicenseFragment().apply {
            arguments = Bundle().apply {
                // add any arguments here
                putLong(ARG_SPACE_ID, spaceId)
                putBoolean(ARG_IS_EDITING, isEditing)
            }
        }
    }

    override fun getToolbarTitle() = getString(R.string.private_server)
    override fun getToolbarSubtitle(): String? = null
    override fun shouldShowBackButton() = false
}