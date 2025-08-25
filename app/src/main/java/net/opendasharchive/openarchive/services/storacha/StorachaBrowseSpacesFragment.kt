package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaBrowseSpacesBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.storacha.util.DidManager
import net.opendasharchive.openarchive.services.storacha.viewModel.StorachaBrowseSpacesViewModel
import net.opendasharchive.openarchive.util.extensions.toggle
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorachaBrowseSpacesFragment : BaseFragment() {
    private lateinit var mBinding: FragmentStorachaBrowseSpacesBinding
    private val viewModel: StorachaBrowseSpacesViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = FragmentStorachaBrowseSpacesBinding.inflate(layoutInflater)
        mBinding.rvFolderList.layoutManager = LinearLayoutManager(requireContext())
        return mBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val did = DidManager(requireContext()).getOrCreateDid()
        val prefs = requireContext().getSharedPreferences("storacha_prefs", android.content.Context.MODE_PRIVATE)
        val sessionId = prefs.getString("session_id", "") ?: ""
        viewModel.loadSpaces(did, sessionId)

        viewModel.loading.observe(viewLifecycleOwner) {
            mBinding.progressBar.toggle(it)
        }

        viewModel.spaces.observe(viewLifecycleOwner) { list ->
            mBinding.projectsEmpty.toggle(list.isEmpty())
            mBinding.rvFolderList.adapter =
                StorachaBrowseSpacesAdapter(list) { space ->
                    val action =
                        StorachaBrowseSpacesFragmentDirections.actionFragmentStorachaBrowseSpacesToFragmentStorachaMedia(
                            spaceDid = space.did,
                            spaceName = space.name,
                        )
                    findNavController().navigate(action)
                }
        }
    }

    override fun getToolbarTitle(): String = getString(R.string.spaces)
}
