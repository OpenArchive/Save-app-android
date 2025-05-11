package net.opendasharchive.openarchive.services.gdrive

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentGdriveBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class GDriveApiFragment : Fragment() {

    private lateinit var mBinding: FragmentGdriveBinding

    private val viewModel: GDriveViewModel by viewModel()

    companion object {
        const val RESP_CANCEL = "gdrive_fragment_resp_cancel"
        const val RESP_AUTHENTICATED = "gdrive_fragment_resp_authenticated"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentGdriveBinding.inflate(inflater)

        mBinding.disclaimer1.text = HtmlCompat.fromHtml(
            getString(
                R.string.gdrive_disclaimer_1,
                getString(R.string.app_name),
                getString(R.string.google_name),
                getString(R.string.gdrive_sudp_name),
            ), HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        mBinding.disclaimer1.movementMethod = LinkMovementMethod.getInstance()
        mBinding.disclaimer2.text = getString(
            R.string.gdrive_disclaimer_2,
            getString(R.string.google_name),
            getString(R.string.gdrive),
            getString(R.string.app_name),
        )
        mBinding.error.visibility = View.GONE

        mBinding.btBack.setOnClickListener {
            setFragmentResult(RESP_CANCEL, bundleOf())
        }

        mBinding.btAuthenticate.setOnClickListener {
            mBinding.error.visibility = View.GONE
            viewModel.authenticate(requireActivity())
            mBinding.btBack.isEnabled = false
            mBinding.btAuthenticate.isEnabled = false
        }

        return mBinding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RESULT_SIGN_IN -> authorized(resultCode, data)
            else -> authFailed(getString(R.string.error))
        }
    }

    private fun authorized(resultCode: Int, data: Intent?) {
        when (resultCode) {
            RESULT_OK -> {
                viewModel.saveSpace(requireContext()) {
                    setFragmentResult(RESP_AUTHENTICATED, bundleOf())
                }
            }

            else -> authFailed(
                getString(
                    R.string.gdrive_auth_insufficient_permissions,
                    getString(R.string.app_name),
                    getString(R.string.gdrive)
                )
            )
        }
    }

    private fun authFailed(errorMessage: String?) {
        MainScope().launch {
            errorMessage?.let {
                mBinding.error.text = errorMessage
                mBinding.error.visibility = View.VISIBLE
            }
            mBinding.btBack.isEnabled = true
            mBinding.btAuthenticate.isEnabled = true
        }
    }
}