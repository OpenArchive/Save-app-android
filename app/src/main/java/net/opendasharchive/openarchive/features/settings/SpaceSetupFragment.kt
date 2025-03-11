package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.compose.content
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.features.spaces.SpaceSetupScreen
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import org.koin.android.ext.android.inject
import kotlin.getValue

class SpaceSetupFragment : BaseFragment() {

    private val appConfig by inject<AppConfig>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {

        // Prepare click lambdas that use the fragment’s business logic.
        val onWebDavClick = {
            if (isJetpackNavigation) {
                findNavController().navigate(R.id.action_fragment_space_setup_to_fragment_web_dav)
            } else {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_WEBDAV)
                )
            }
        }
        // Only enable Internet Archive if not already present
        val isInternetArchiveAllowed = !Space.has(Space.Type.INTERNET_ARCHIVE)
        val onInternetArchiveClick = {
            if (isJetpackNavigation) {
                val action =
                    SpaceSetupFragmentDirections.actionFragmentSpaceSetupToFragmentInternetArchive()
                findNavController().navigate(action)
            } else {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_INTERNET_ARCHIVE)
                )
            }
        }
        // Show/hide Snowbird based on config
        val isDwebEnabled = appConfig.isDwebEnabled
        val onDwebClicked = {
            if (isJetpackNavigation) {
                val action =
                    SpaceSetupFragmentDirections.actionFragmentSpaceSetupToFragmentSnowbird()
                findNavController().navigate(action)
            } else {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_RAVEN)
                )
            }
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

    companion object {
        const val RESULT_REQUEST_KEY = "space_setup_fragment_result"
        const val RESULT_BUNDLE_KEY = "space_setup_result_key"
        const val RESULT_VAL_DROPBOX = "dropbox"
        const val RESULT_VAL_WEBDAV = "webdav"
        const val RESULT_VAL_RAVEN = "raven"
        const val RESULT_VAL_INTERNET_ARCHIVE = "internet_archive"
        const val RESULT_VAL_GDRIVE = "gdrive"
    }

    override fun getToolbarTitle() = "Select a Server"
    override fun getToolbarSubtitle(): String? = null
    override fun shouldShowBackButton() = true
}