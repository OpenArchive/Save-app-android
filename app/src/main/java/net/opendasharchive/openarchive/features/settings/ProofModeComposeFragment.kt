package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseFragment

class ProofModeComposeFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SaveAppTheme {
                    ProofModeScreen(onNavigateBack = { requireActivity().onBackPressedDispatcher.onBackPressed() })
                }
            }
        }
    }

    override fun getToolbarTitle() = getString(R.string.proofmode)
}
