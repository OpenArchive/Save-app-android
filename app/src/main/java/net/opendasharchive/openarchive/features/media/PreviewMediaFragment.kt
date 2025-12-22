package net.opendasharchive.openarchive.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable
import net.opendasharchive.openarchive.features.core.UiColor
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asUiImage
import net.opendasharchive.openarchive.features.core.asUiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.util.PermissionManager
import net.opendasharchive.openarchive.util.Prefs
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class PreviewMediaFragment : BaseFragment(), ToolbarConfigurable {

    private val args: PreviewMediaFragmentArgs by navArgs()
    private val appConfig by inject<AppConfig>()
    private val previewViewModel: PreviewMediaViewModel by viewModel()

    private lateinit var permissionManager: PermissionManager
    private lateinit var mediaLaunchers: MediaLaunchers
    private var project: Project? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val activity = requireActivity() as BaseActivity
        permissionManager = PermissionManager(activity, dialogManager)
        project = Project.getById(args.projectId)

        val composeView = ComposeView(requireContext()).apply {
            setContent {
                SaveAppTheme {
                    PreviewMediaScreen(
                        viewModel = previewViewModel,
//                        onNavigateToReview = { media, selected, batchMode ->
//                            ReviewActivity.launchReviewScreen(requireContext(), media, selected, batchMode)
//                        },
//                        onRequestAddMore = { launchAddMore() },
//                        onPickMedia = { handleMediaPick(it) },
//                        onShowBatchHint = { showFirstTimeBatch() },
//                        onCloseScreen = {
//                            // Finish the activity to return to MainActivity
//                            // This is the correct way to navigate between activities
//                            requireActivity().finish()
//                        }
                    )
                }
            }
        }

        addMenuProvider()

        mediaLaunchers = Picker.register(
            activity = activity,
            root = composeView,
            project = { project },
            completed = {
                previewViewModel.onAction(PreviewMediaAction.Refresh)
            }
        )

        return composeView
    }

    override fun onResume() {
        super.onResume()
        previewViewModel.onAction(PreviewMediaAction.Refresh)
    }

    override fun getToolbarTitle(): String = getString(R.string.preview_media)

    override fun shouldShowBackButton(): Boolean = true

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_preview, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_upload -> {
                        uploadMedia()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun launchAddMore() {
        permissionManager.checkMediaPermissions {
            Picker.pickMedia(mediaLaunchers.galleryLauncher)
        }
    }

    private fun handleMediaPick(action: AddMediaType) {
        when (action) {
            AddMediaType.CAMERA -> {
                if (appConfig.useCustomCamera) {
                    val cameraConfig = CameraConfig(
                        allowVideoCapture = true,
                        allowPhotoCapture = true,
                        allowMultipleCapture = true,
                        enablePreview = true,
                        showFlashToggle = true,
                        showGridToggle = true,
                        showCameraSwitch = true
                    )
                    Picker.launchCustomCamera(
                        requireActivity(),
                        mediaLaunchers.customCameraLauncher,
                        cameraConfig
                    )
                } else {
                    permissionManager.checkCameraPermission {
                        Picker.takePhotoModern(
                            activity = requireActivity(),
                            launcher = mediaLaunchers.modernCameraLauncher
                        )
                    }
                }
            }

            AddMediaType.FILES -> Picker.pickFiles(mediaLaunchers.filePickerLauncher)
            AddMediaType.GALLERY -> launchAddMore()
        }
    }

    private fun showFirstTimeBatch() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            icon = R.drawable.ic_media_new.asUiImage()
            iconColor = UiColor.Resource(R.color.colorTertiary)
            title = R.string.edit_multiple.asUiText()
            message = R.string.press_and_hold_to_select_and_edit_multiple_media.asUiText()
            positiveButton {
                text = UiText.Resource(R.string.lbl_got_it)
                action = {
                    dialogManager.dismissDialog()
                }
            }
        }

        Prefs.batchHintShown = true
    }

    private fun uploadMedia() {
        val queue: () -> Unit = {
            previewViewModel.onAction(PreviewMediaAction.UploadAll)
        }

        if (Prefs.dontShowUploadHint) {
            queue()
            return
        }

        var doNotShowAgain = false

        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Warning
            iconColor = UiColor.Resource(R.color.colorTertiary)
            message = R.string.once_uploaded_you_will_not_be_able_to_edit_media.asUiText()
            showCheckbox = true
            checkboxText = UiText.Resource(R.string.do_not_show_me_this_again)
            onCheckboxChanged = { isChecked ->
                doNotShowAgain = isChecked
            }
            positiveButton {
                text = UiText.Resource(R.string.proceed_to_upload)
                action = {
                    Prefs.dontShowUploadHint = doNotShowAgain
                    queue()
                }
            }
            neutralButton {
                text = UiText.Resource(R.string.actually_let_me_edit)
            }
        }
    }
}