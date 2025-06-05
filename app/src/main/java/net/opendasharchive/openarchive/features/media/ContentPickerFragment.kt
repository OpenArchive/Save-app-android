package net.opendasharchive.openarchive.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.opendasharchive.openarchive.databinding.FragmentContentPickerBinding

class ContentPickerFragment(private val onMediaPicked: (AddMediaType) -> Unit): BottomSheetDialogFragment() {

    private var _binding: FragmentContentPickerBinding? = null
    private val binding get() = _binding!!


    companion object {
        const val TAG = "ModalBottomSheet-ContentPickerFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentContentPickerBinding.inflate(inflater, container, false)


        binding.actionUploadCamera.setOnClickListener {
            onMediaPicked(AddMediaType.CAMERA)
            dismiss()
        }

        binding.actionUploadMedia.setOnClickListener {
            onMediaPicked(AddMediaType.GALLERY)
            dismiss()
        }

        binding.actionUploadFiles.setOnClickListener {
            onMediaPicked(AddMediaType.FILES)
            dismiss()
        }


        return binding.root
    }
}