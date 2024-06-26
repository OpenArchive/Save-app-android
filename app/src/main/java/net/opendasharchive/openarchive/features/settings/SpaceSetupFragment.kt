package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.util.extensions.hide

class SpaceSetupFragment : Fragment() {

    private lateinit var mBinding: FragmentSpaceSetupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentSpaceSetupBinding.inflate(inflater)

        mBinding.webdav.setOnClickListener {
            setFragmentResult(RESULT_REQUEST_KEY, bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_WEBDAV))
        }

        if (Space.has(Space.Type.DROPBOX)) {
            mBinding.dropbox.hide()
        } else {
            mBinding.dropbox.setOnClickListener {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_DROPBOX)
                )
            }
        }

        if (Space.has(Space.Type.INTERNET_ARCHIVE)) {
            mBinding.internetArchive.hide()
        } else {
            mBinding.internetArchive.setOnClickListener {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_INTERNET_ARCHIVE)
                )
            }
        }

        if (Space.has(Space.Type.GDRIVE) || !playServicesAvailable()) {
            mBinding.gdrive.hide()
        } else {
            mBinding.gdrive.setOnClickListener {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_GDRIVE)
                )
            }
        }

        return mBinding.root
    }

    private fun playServicesAvailable(): Boolean {
        return ConnectionResult.SUCCESS == GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(requireContext())
    }

    companion object {
        const val RESULT_REQUEST_KEY = "space_setup_fragment_result"
        const val RESULT_BUNDLE_KEY = "space_setup_result_key"
        const val RESULT_VAL_DROPBOX = "dropbox"
        const val RESULT_VAL_WEBDAV = "webdav"
        const val RESULT_VAL_INTERNET_ARCHIVE = "internet_archive"
        const val RESULT_VAL_GDRIVE = "gdrive"
    }
}