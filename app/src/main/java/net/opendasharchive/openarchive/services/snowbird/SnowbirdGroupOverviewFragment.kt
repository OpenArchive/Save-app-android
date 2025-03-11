package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdGroupOverviewBinding
import net.opendasharchive.openarchive.features.core.BaseFragment

class SnowbirdGroupOverviewFragment private constructor(): BaseFragment() {
    private lateinit var viewBinding: FragmentSnowbirdGroupOverviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentSnowbirdGroupOverviewBinding.inflate(inflater)

        return viewBinding.root
    }

    override fun getToolbarTitle(): String {
        return "Raven Group Overview"
    }
}