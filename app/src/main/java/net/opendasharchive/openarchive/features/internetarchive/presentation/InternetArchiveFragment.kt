package net.opendasharchive.openarchive.features.internetarchive.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.IAResult
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.bundleWithNewSpace
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.bundleWithSpaceId
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.getSpace
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable

@Deprecated("only used for backward compatibility")
class InternetArchiveFragment : BaseFragment(), ToolbarConfigurable {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val (space, isNewSpace) = arguments.getSpace(Space.Type.INTERNET_ARCHIVE)

        return ComposeView(requireContext()).apply {
            setContent {
                InternetArchiveScreen(space, isNewSpace) { result ->
                    finish(result)
                }
            }
        }
    }

    private fun finish(result: IAResult) {
        if (isJetpackNavigation) {
            when (result) {
                IAResult.Saved -> {
                    val message = getString(R.string.you_have_successfully_connected_to_the_internet_archive)
                    val action = InternetArchiveFragmentDirections.actionFragmentInternetArchiveToFragmentSpaceSetupSuccess(message)
                    findNavController().navigate(action)
                }
                IAResult.Deleted -> TODO()
                IAResult.Cancelled -> findNavController().popBackStack()
            }
        } else {
            setFragmentResult(result.value, bundleOf())

            if (result == IAResult.Saved) {
                // activity?.measureNewBackend(Space.Type.INTERNET_ARCHIVE)
            }
        }
    }

    companion object {

        val RESP_SAVED = IAResult.Saved.value
        val RESP_CANCEL = IAResult.Cancelled.value

        @JvmStatic
        fun newInstance(args: Bundle) = InternetArchiveFragment().apply {
            arguments = args
        }

        @JvmStatic
        fun newInstance(spaceId: Long) = newInstance(args = bundleWithSpaceId(spaceId))

        @JvmStatic
        fun newInstance() = newInstance(args = bundleWithNewSpace())
    }

    override fun getToolbarTitle() = "Internet Archive"
    override fun shouldShowBackButton() = true
}
