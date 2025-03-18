package net.opendasharchive.openarchive.features.media

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.opendasharchive.openarchive.databinding.FragmentAddMediaDialogBinding

class AddMediaDialogFragment : DialogFragment() {

    private lateinit var mDialogView: View
    private lateinit var mBinding: FragmentAddMediaDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        mDialogView = onCreateView(layoutInflater, null, savedInstanceState)
        builder.setView(mDialogView)
        return builder.create()
    }

    override fun getView(): View {
        return mDialogView
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentAddMediaDialogBinding.inflate(inflater)

        mBinding.photoCamera.setOnClickListener {
            setFragmentResult(RESP_TAKE_PHOTO, bundleOf())
            dismiss()
        }

        mBinding.photoGallery.setOnClickListener {
            setFragmentResult(RESP_PHOTO_GALLERY, bundleOf())
            dismiss()
        }
        mBinding.files.setOnClickListener {
            setFragmentResult(RESP_FILES, bundleOf())
            dismiss()
        }

        return mBinding.root
    }

    companion object {
        const val RESP_TAKE_PHOTO = "add_media_dialog_fragment_take_photo_resp"
        const val RESP_PHOTO_GALLERY = "add_media_dialog_fragment_photo_gallery_resp"
        const val RESP_FILES = "add_media_dialog_fragment_files_resp"
    }
}