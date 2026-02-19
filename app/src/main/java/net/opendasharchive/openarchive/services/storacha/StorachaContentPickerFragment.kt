package net.opendasharchive.openarchive.services.storacha

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.opendasharchive.openarchive.databinding.FragmentStorachaContentPickerBinding

class StorachaContentPickerFragment(
    private val onContentPicked: (StorachaContentType) -> Unit,
) : BottomSheetDialogFragment() {
    private var _binding: FragmentStorachaContentPickerBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "ModalBottomSheet-StorachaContentPickerFragment"
        const val KEY_DISMISS = "StorachaContentPickerFragment.Dismiss"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStorachaContentPickerBinding.inflate(inflater, container, false)

        binding.actionUploadCamera.setOnClickListener {
            onContentPicked(StorachaContentType.CAMERA)
            dismiss()
        }

        binding.actionUploadFiles.setOnClickListener {
            onContentPicked(StorachaContentType.FILES)
            dismiss()
        }

        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        parentFragmentManager.setFragmentResult(KEY_DISMISS, Bundle())
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

enum class StorachaContentType {
    CAMERA,
    FILES,
}
