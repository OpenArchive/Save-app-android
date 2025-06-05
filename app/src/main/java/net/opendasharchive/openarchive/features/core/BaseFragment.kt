package net.opendasharchive.openarchive.features.core

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.extensions.androidViewModel
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoViewModel
import net.opendasharchive.openarchive.util.FullScreenOverlayManager

abstract class BaseFragment : Fragment(), ToolbarConfigurable {

    protected val dialogManager: DialogStateManager by activityViewModels()

    val snowbirdGroupViewModel: SnowbirdGroupViewModel by androidViewModel()
    val snowbirdRepoViewModel: SnowbirdRepoViewModel by androidViewModel()

    val isJetpackNavigation: Boolean
        get() {
            val parentFragmentManager = parentFragmentManager
            return parentFragmentManager.findFragmentById(R.id.space_nav_host_fragment) != null
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ensureComposeDialogHost()
    }

    private fun ensureComposeDialogHost() {
        (requireActivity() as? BaseActivity)?.ensureComposeDialogHost()
    }

    open fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    open fun handleError(error: SnowbirdError) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            title = UiText.DynamicString("Oops")
            message = UiText.DynamicString(error.friendlyMessage)
            positiveButton {
                text = UiText.StringResource(R.string.lbl_ok)
            }
        }
    }

    open fun handleLoadingStatus(isLoading: Boolean) {
        if (isLoading) {
            FullScreenOverlayManager.show(this@BaseFragment)
        } else {
            FullScreenOverlayManager.hide()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? SpaceSetupActivity)?.updateToolbarFromFragment(this)
    }
}