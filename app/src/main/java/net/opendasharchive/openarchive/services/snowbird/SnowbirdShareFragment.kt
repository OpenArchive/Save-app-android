package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdShareGroupBinding
import net.opendasharchive.openarchive.services.snowbird.service.db.SnowbirdGroup
import net.opendasharchive.openarchive.extensions.asQRCode
import net.opendasharchive.openarchive.extensions.urlEncode

class SnowbirdShareFragment: BaseSnowbirdFragment() {

    private lateinit var binding: FragmentSnowbirdShareGroupBinding
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

        val group = SnowbirdGroup.get(args.dwebGroupKey)
        val groupName = group?.name ?: "Unknown group"

        binding.groupName.text = groupName

        SnowbirdGroup.get(args.dwebGroupKey)?.uri?.let { uriString ->
            val qrCode = "$uriString&name=${groupName.urlEncode()}".asQRCode(size = 1024)
            binding.qrCode.setImageBitmap(qrCode)
        }
    }

    override fun getToolbarTitle(): String {
        return "Share DWeb Storage Group"
    }
}