package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaDidAccessBinding
import net.opendasharchive.openarchive.features.core.BaseFragment

class StorachaDIDAccessFragment: BaseFragment() {

    private lateinit var binding: FragmentStorachaDidAccessBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStorachaDidAccessBinding.inflate(layoutInflater)
        val intent = requireActivity().intent
//        binding.tvDid.text = intent.getStringExtra(EXTRA_DID)
//        binding.swRead.isChecked = intent.getBooleanExtra(EXTRA_READ,false)
//        binding.swWrite.isChecked = intent.getBooleanExtra(EXTRA_WRITE,false)
//        binding.swDelete.isChecked = intent.getBooleanExtra(EXTRA_DELETE,false)
        binding.btOk.setOnClickListener {

        }
        binding.btBack.setOnClickListener {

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        const val EXTRA_DID = "did"
        const val EXTRA_WRITE = "write"
        const val EXTRA_READ = "read"
        const val EXTRA_DELETE = "delete"
    }

    override fun getToolbarTitle() = getString(R.string.access)
    override fun getToolbarSubtitle(): String? = null
    override fun shouldShowBackButton() = true
}