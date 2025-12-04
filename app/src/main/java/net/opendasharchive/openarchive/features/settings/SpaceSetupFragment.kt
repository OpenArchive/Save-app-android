package net.opendasharchive.openarchive.features.settings

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
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.features.spaces.SpaceSetupScreen
import org.koin.android.ext.android.inject

class SpaceSetupFragment : BaseFragment() {

    private val appConfig by inject<AppConfig>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {

        // Prepare click lambdas that use the fragment’s business logic.
        val onWebDavClick = {
            findNavController().navigate(R.id.action_fragment_space_setup_to_fragment_web_dav)
        }

        // Only enable Internet Archive if not already present
        val isInternetArchiveAllowed = !Space.has(Space.Type.INTERNET_ARCHIVE)
        val onInternetArchiveClick = {
            val action = SpaceSetupFragmentDirections.actionFragmentSpaceSetupToInternetArchiveLogin()
            findNavController().navigate(action)
        }

        // Show/hide Snowbird based on config
        val isDwebEnabled = appConfig.isDwebEnabled
        val onDwebClicked = {
            val action =
                    SpaceSetupFragmentDirections.actionFragmentSpaceSetupToFragmentSnowbird()
                findNavController().navigate(action)
        }

        SaveAppTheme {
            SpaceSetupScreen(
                onWebDavClick = onWebDavClick,
                isInternetArchiveAllowed = isInternetArchiveAllowed,
                onInternetArchiveClick = onInternetArchiveClick,
                isDwebEnabled = isDwebEnabled,
                onDwebClicked = onDwebClicked
            )
        }

    }

    override fun getToolbarTitle() = getString(R.string.space_setup_title)
    override fun getToolbarSubtitle(): String? = null
    override fun shouldShowBackButton() = true
}