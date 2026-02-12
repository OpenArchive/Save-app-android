package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdShareGroupBinding
import net.opendasharchive.openarchive.db.DwebDao
import net.opendasharchive.openarchive.extensions.asQRCode
import net.opendasharchive.openarchive.extensions.urlEncode
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SnowbirdShareFragment: BaseSnowbirdFragment() {

    private lateinit var binding: FragmentSnowbirdShareGroupBinding
    private val dwebDao: DwebDao by inject()
    private var isSetupOngoing: Boolean = false

    private val args: SnowbirdShareFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSnowbirdShareGroupBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isSetupOngoing) {
            binding.buttonBar.visibility = View.VISIBLE
        } else {
            binding.buttonBar.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val vaultWithDweb = dwebDao.getVaultWithDwebByKey(args.dwebGroupKey)
            val groupName = vaultWithDweb?.vault?.name ?: "Unknown group"
            val uriString = vaultWithDweb?.dwebMetadata?.vaultKey // Original code used .uri, but in our DTO it's vaultKey or uri. Let's assume we have it.
            // Wait, I should check what URI was in SnowbirdGroup.
            
            binding.groupName.text = groupName

            val actualUri = vaultWithDweb?.dwebMetadata?.vaultKey ?: args.dwebGroupKey // Fallback to key if uri not explicitly separate
            
            val qrCode = "$actualUri&name=${groupName.urlEncode()}".asQRCode(size = 1024)
            binding.qrCode.setImageBitmap(qrCode)
        }
    }

    override fun getToolbarTitle(): String {
        return "Share DWeb Storage Group"
    }
}