package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupSuccessBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.main.MainActivity

class SpaceSetupSuccessFragment : BaseFragment() {
    private lateinit var mBinding: FragmentSpaceSetupSuccessBinding
    private var message = ""
    private var isDweb = false
    private var dwebGroupKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            message = it.getString(ARG_MESSAGE, "")
            isDweb = it.getBoolean(IS_DWEB, false)
            dwebGroupKey = it.getString(DWEB_GROUP_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentSpaceSetupSuccessBinding.inflate(inflater)

        if (message.isNotEmpty()) {
            mBinding.successMessage.text = message
        }

        mBinding.btAuthenticate.setOnClickListener { _ ->
            if (isJetpackNavigation) {
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clears backstack
                startActivity(intent)
            } else {
                setFragmentResult(RESP_DONE, bundleOf())
            }
        }

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isDweb) {
            // Add the menu provider
            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_space_setup_success, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_share_group -> {
                            if (dwebGroupKey != null) {
                                val action =
                                    SpaceSetupSuccessFragmentDirections.actionFragmentSpaceSetupSuccessToFragmentSnowbirdShareGroup(
                                        dwebGroupKey = dwebGroupKey!!,
                                        isSetupOngoing = true
                                    )

                                findNavController().navigate(action)
                            }
                            true
                        }

                        else -> false
                    }
                }
            }, viewLifecycleOwner)
        }
    }

    companion object {
        const val RESP_DONE = "space_setup_success_fragment_resp_done"

        const val ARG_MESSAGE = "message"
        const val IS_DWEB = "is_dweb"
        const val DWEB_GROUP_KEY = "dweb_group_key"


        @JvmStatic
        fun newInstance(message: String) =
            SpaceSetupSuccessFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                }
            }
    }

    override fun getToolbarTitle() = getString(R.string.space_setup_success_title)
    override fun shouldShowBackButton() = false
}