package net.opendasharchive.openarchive.features.spaces

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import org.koin.compose.viewmodel.koinViewModel

class SpaceListFragment : BaseFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {


        val viewModel: SpaceListViewModel = koinViewModel()

        SaveAppTheme {

            // Calling refresh here will update state & trigger recomposition
            LaunchedEffect(Unit) {
                viewModel.refreshSpaces()
            }

            SpaceListScreen(
                viewModel = viewModel,
                onSpaceClicked = { id, type ->
                    startSpaceAuthActivity(id)
                },
                onAddServerClicked = {
                    val action =
                        SpaceListFragmentDirections.actionFragmentSpaceListToFragmentSpaceSetup()
                    findNavController().navigate(action)
                }
            )
        }


    }

    override fun getToolbarTitle() = getString(R.string.pref_title_media_servers)

    private fun startSpaceAuthActivity(spaceId: Long?) {
        val space = Space.get(spaceId ?: return) ?: return

        when (space.tType) {
            Space.Type.INTERNET_ARCHIVE -> {
                val action =
                    SpaceListFragmentDirections.actionFragmentSpaceListToInternetArchiveDetails(
                        space.id
                    )
                findNavController().navigate(action)
            }

            Space.Type.WEBDAV -> {
                val action =
                    SpaceListFragmentDirections.actionFragmentSpaceListToFragmentWebDav(spaceId)
                findNavController().navigate(action)
            }


            Space.Type.RAVEN -> {
                // Do nothing
            }
        }


    }
}