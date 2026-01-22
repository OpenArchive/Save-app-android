package net.opendasharchive.openarchive.upload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.main.MainActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class ComposeUploadManagerFragment : SKBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ModalBottomSheet-ComposeUploadManagerFragment"
        private val STATUSES =
            listOf(Media.Status.Uploading, Media.Status.Queued, Media.Status.Error)
    }

    private val uploadViewModel: UploadManagerViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SaveAppTheme {
                    UploadManagerScreen(
                        viewModel = uploadViewModel,
                        onClose = { dismiss() },
                        onShowRetryDialog = { media, position ->
                            showRetryDialog(media, position)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Notify MainActivity that this fragment is dismissed
        (activity as? MainActivity)?.uploadManagerFragment = null
    }

    fun updateItem(mediaId: Long) {
        uploadViewModel.onAction(UploadManagerAction.UpdateItem(mediaId, -1))
    }

    fun removeItem(mediaId: Long) {
        uploadViewModel.onAction(UploadManagerAction.RemoveItem(mediaId))
    }

    fun refresh() {
        uploadViewModel.onAction(UploadManagerAction.Refresh)
    }

    fun getUploadingCounter(): Int {
        return uploadViewModel.getUploadingCounter()
    }

    private fun showRetryDialog(mediaItem: Evidence, position: Int) {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Error
            title = UiText.Resource(R.string.upload_unsuccessful)
            message = UiText.Resource(R.string.upload_unsuccessful_description)
            positiveButton {
                text = UiText.Resource(R.string.lbl_retry)
                action = {
                    uploadViewModel.onAction(UploadManagerAction.RetryItem(mediaItem))

                    // Notify parent that retry was selected
                    val resultBundle = Bundle().apply {
                        putLong("mediaId", mediaItem.id)
                        putInt("progress", 0)
                    }
                    parentFragmentManager.setFragmentResult("uploadRetry", resultBundle)
                }
            }

            destructiveButton {
                text = UiText.Resource(R.string.btn_lbl_remove_media)
                action = {
                    uploadViewModel.onAction(UploadManagerAction.DeleteItem(position))
                }
            }
        }
    }
}
