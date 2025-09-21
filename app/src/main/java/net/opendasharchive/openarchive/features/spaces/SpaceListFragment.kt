package net.opendasharchive.openarchive.features.spaces

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.databinding.FragmentSpaceListBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.gdrive.GDriveActivity
import org.koin.compose.viewmodel.koinViewModel

class SpaceListFragment : BaseFragment() {

    private lateinit var binding: FragmentSpaceListBinding

    companion object {
        const val EXTRA_DATA_SPACE = "space_id"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSpaceListBinding.inflate(inflater)


        binding.composeViewSpaceList.setContent {

            val viewModel: SpaceListViewModel = koinViewModel()

            SaveAppTheme {

                // Calling refresh here will update state & trigger recomposition
                LaunchedEffect(Unit) {
                    viewModel.refreshSpaces()
                }

                SpaceListScreen(
                    onSpaceClicked = { space ->
                        startSpaceAuthActivity(space.id)
                    },
                    onAddServerClicked = {
                        val action =
                            SpaceListFragmentDirections.actionFragmentSpaceListToFragmentSpaceSetup()
                        findNavController().navigate(action)
                    }
                )
            }

        }

        return binding.root
    }

    override fun getToolbarTitle() = getString(R.string.pref_title_media_servers)

    private fun startSpaceAuthActivity(spaceId: Long?) {
        val space = Space.get(spaceId ?: return) ?: return

        when (space.tType) {
            Space.Type.INTERNET_ARCHIVE -> {
                val action = SpaceListFragmentDirections.actionFragmentSpaceListToInternetArchiveDetails(space.id)
                findNavController().navigate(action)
            }

            Space.Type.GDRIVE -> {
                val intent = Intent(requireContext(), GDriveActivity::class.java)
                intent.putExtra(EXTRA_DATA_SPACE, space.id)
                startActivity(intent)
            }

            Space.Type.WEBDAV -> {
                val action =
                    SpaceListFragmentDirections.actionFragmentSpaceListToFragmentWebDav(spaceId)
                findNavController().navigate(action)
            }

            Space.Type.RAVEN -> {
                // Do nothing
            }

            Space.Type.STORACHA -> {
                // Do nothing
            }
        }


    }
}