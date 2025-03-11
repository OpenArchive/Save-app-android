package net.opendasharchive.openarchive.features.spaces

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.databinding.FragmentSpaceListBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveActivity
import net.opendasharchive.openarchive.services.gdrive.GDriveActivity
import net.opendasharchive.openarchive.services.webdav.WebDavActivity

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

            SaveAppTheme {

                SpaceListScreen(
                    onSpaceClicked = { space ->
                        startSpaceAuthActivity(space.id)
                    },
                )
            }

        }

        return binding.root
    }

    override fun getToolbarTitle() = "Media Servers"

    private fun startSpaceAuthActivity(spaceId: Long?) {
        val space = Space.get(spaceId ?: return) ?: return

        when (space.tType) {
            Space.Type.INTERNET_ARCHIVE -> {
                val intent = Intent(requireContext(), InternetArchiveActivity::class.java)
                intent.putExtra(EXTRA_DATA_SPACE, space.id)
                startActivity(intent)
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
                val intent = Intent(requireContext(), WebDavActivity::class.java)
                intent.putExtra(EXTRA_DATA_SPACE, space.id)
                startActivity(intent)
            }
        }


    }
}