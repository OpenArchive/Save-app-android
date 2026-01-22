package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.features.spaces.SpaceSetupScreen
import net.opendasharchive.openarchive.features.spaces.SpaceSetupViewModel
import org.koin.android.ext.android.inject
import net.opendasharchive.openarchive.services.snowbird.SnowbirdActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class SpaceSetupFragment : BaseFragment() {

    private val viewModel: SpaceSetupViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        SaveAppTheme {
            SpaceSetupScreen(
                viewModel = viewModel,
            )
        }
    }

    override fun getToolbarTitle() = getString(R.string.space_setup_title)
    override fun getToolbarSubtitle(): String? = null
    override fun shouldShowBackButton() = true
}
