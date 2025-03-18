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
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentGdriveBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment

class GDriveFragment : BaseFragment() {

    private lateinit var mBinding: FragmentGdriveBinding

    companion object {
        const val RESP_CANCEL = "gdrive_fragment_resp_cancel"
        const val RESP_AUTHENTICATED = "gdrive_fragment_resp_authenticated"

        const val REQUEST_CODE_GOOGLE_AUTH = 21701
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
            if(isJetpackNavigation) {
                findNavController().popBackStack()
            } else {
                setFragmentResult(RESP_CANCEL, bundleOf())
            }
        }

        mBinding.btAuthenticate.setOnClickListener {
            mBinding.error.visibility = View.GONE
            authenticate()
            mBinding.btBack.isEnabled = false
            mBinding.btAuthenticate.isEnabled = false
        }

        return mBinding.root
    }

    private fun authenticate() {
        if (!GDriveConduit.permissionsGranted(requireContext())) {
            GoogleSignIn.requestPermissions(
                requireActivity(),
                REQUEST_CODE_GOOGLE_AUTH,
                GoogleSignIn.getLastSignedInAccount(requireActivity()),
                *GDriveConduit.SCOPES
            )
        } else {
            // permission was already granted, we're already signed in, continue.
            if (isJetpackNavigation) {
                val message = getString(R.string.you_have_successfully_connected_to_gdrive)
                val action = GDriveFragmentDirections.actionFragmentGdriveToFragmentSpaceSetupSuccess(message)
                findNavController().navigate(action)
            } else {
                setFragmentResult(RESP_AUTHENTICATED, bundleOf())
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_GOOGLE_AUTH) {
            when (resultCode) {
                RESULT_OK -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val space = Space(Space.Type.GDRIVE)
                        // we don't really know the host here, that's hidden by Drive Api
                        space.host = "what's the host of google drive? :shrug:"
                        data?.let {
                            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(it)
                            if (result?.isSuccess == true) {
                                result.signInAccount?.let { account ->
                                    space.displayname = account.email ?: ""
                                }
                            }
                        }

                        if (GDriveConduit.permissionsGranted(requireContext())) {
                            space.save()
                            Space.current = space

//                            CleanInsightsManager.getConsent(requireActivity()) {
//                                CleanInsightsManager.measureEvent(
//                                    "backend",
//                                    "new",
//                                    Space.Type.GDRIVE.friendlyName
//                                )
//                            }

                            MainScope().launch {
                                setFragmentResult(RESP_AUTHENTICATED, bundleOf())
                            }
                        } else {
                            authFailed(
                                getString(
                                    R.string.gdrive_auth_insufficient_permissions,
                                    getString(R.string.app_name),
                                    getString(R.string.gdrive)
                                )
                            )
                        }
                    }
                }

                else -> authFailed()
            }
        }
    }

    private fun authFailed() {
        authFailed(null)
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

    override fun getToolbarTitle() = "Google Drive"
}