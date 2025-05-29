package net.opendasharchive.openarchive.services.storacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentStorachaBinding
import net.opendasharchive.openarchive.features.core.BaseFragment

class StorachaFragment : BaseFragment() {
    private lateinit var viewBinding: FragmentStorachaBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewBinding = FragmentStorachaBinding.inflate(inflater)

        return viewBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.btnJoinSpaces.setOnClickListener {
            val action = StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaClientQr("Test")
            findNavController().navigate(action)
        }

        viewBinding.btnMySpaces.setOnClickListener {
            val action = StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaBrowseSpaces()
            findNavController().navigate(action)
        }

        viewBinding.btnManageAccounts.setOnClickListener {
            val action = StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaLogin()
            findNavController().navigate(action)
        }

        viewBinding.btnManageAccounts.setOnLongClickListener {
            val action = StorachaFragmentDirections.actionFragmentStorachaToFragmentStorachaAccounts()
            findNavController().navigate(action)
            true
        }
    }

    override fun getToolbarTitle(): String = getString(R.string.storacha)
}
