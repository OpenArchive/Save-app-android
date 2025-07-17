package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.IAResult
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.getSpace

class InternetArchiveLoginFragment: BaseFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {

        val (space, isNewSpace) = arguments.getSpace(Space.Type.INTERNET_ARCHIVE)
        SaveAppTheme {
            InternetArchiveLoginScreen(space) { result ->
                handleResult(result)
            }
        }
    }

    private fun handleResult(result: IAResult) {
        when (result) {
            IAResult.Saved -> {
                val message = getString(R.string.you_have_successfully_connected_to_the_internet_archive)
                val action =
                    InternetArchiveLoginFragmentDirections.actionFragmentInternetArchiveLoginToFragmentSpaceSetupSuccess(
                        message = message,
                    )
                findNavController().navigate(action)
            }
            IAResult.Deleted -> {
                findNavController().popBackStack()
            }
            IAResult.Cancelled -> {
                findNavController().popBackStack()
            }
        }
    }

    override fun getToolbarTitle(): String = getString(R.string.internet_archive)
}