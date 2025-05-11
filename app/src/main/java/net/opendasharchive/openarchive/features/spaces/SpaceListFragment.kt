package net.opendasharchive.openarchive.features.spaces

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.services.gdrive.GDriveActivity
import org.koin.compose.viewmodel.koinViewModel

class SpaceListFragment : BaseFragment() {


    companion object {
        const val EXTRA_DATA_SPACE = "space_id"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {

                val viewModel: SpaceListViewModel  = koinViewModel()

                SaveAppTheme {

                    // Calling refresh here will update state & trigger recomposition
                    LaunchedEffect(Unit) {
                        viewModel.refreshSpaces()
                    }

                    SpaceListScreen(
                        onSpaceClicked = { space ->
                            startSpaceAuthActivity(space.id)
                        },
                    )
                }
            }
        }
    }

    override fun getToolbarTitle() = getString(R.string.pref_title_media_servers)

    private fun startSpaceAuthActivity(spaceId: Long?) {
        val space = Space.get(spaceId ?: return) ?: return

        when (space.tType) {
            Space.Type.INTERNET_ARCHIVE -> {
                val action = SpaceListFragmentDirections.actionFragmentSpaceListToFragmentInternetArchiveDetail(spaceId = spaceId)
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

            else -> {
                val action = SpaceListFragmentDirections.actionFragmentSpaceListToFragmentWebDav(spaceId = spaceId)
                findNavController().navigate(action)
            }
        }


    }
}