package net.opendasharchive.openarchive.features.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity

abstract class BaseFragment : Fragment(), ToolbarConfigurable {

    protected val dialogManager: DialogStateManager by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ensureComposeDialogHost()
    }

    private fun ensureComposeDialogHost() {
        (requireActivity() as? BaseActivity)?.ensureComposeDialogHost()
    }



    override fun onResume() {
        super.onResume()
        (activity as? SpaceSetupActivity)?.updateToolbarFromFragment(this)
    }
}