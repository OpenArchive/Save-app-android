package net.opendasharchive.openarchive.services.storacha

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaAccountDetailsBinding
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets

class StorachaAccountDetailsFragment : BaseFragment() {

    companion object{
        const val EXTRA_EMAIL = "email"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_UTILISATION = "utilisation"
        const val EXTRA_UTILISATION_PERCENTAGE = "utilisation_percentage"
        const val EXTRA_ALLOCATION_AND_BILLING = "allocation_and_billing"
    }

    private lateinit var binding: FragmentStorachaAccountDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStorachaAccountDetailsBinding.inflate(layoutInflater)

        binding.buttonBar.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars()
        ) { insets ->
            bottomMargin = insets.bottom
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val intent = requireActivity().intent
//        binding.etEmail.setText(intent.getStringExtra(EXTRA_EMAIL))
//        binding.tvPackage.text = intent.getStringExtra(EXTRA_PACKAGE)
//        binding.tvUtilisation.text = intent.getStringExtra(EXTRA_UTILISATION)
//        binding.tvAllocationBilling.text = intent.getStringExtra(EXTRA_ALLOCATION_AND_BILLING)
//        binding.piUsage.progress = intent.getIntExtra(EXTRA_UTILISATION_PERCENTAGE,0)
        binding.btLogout.setOnClickListener {
            navigateBackWithResult()
        }
    }

    override fun getToolbarTitle(): String = getString(R.string.account)

    private fun navigateBackWithResult() {
        val i = Intent()
        i.putExtra(EXTRA_EMAIL, requireActivity().intent.getStringExtra(EXTRA_EMAIL))
        //TODO GO BACK WITH THE EMAIL EXTRA TO LOGOUT
    }
}
