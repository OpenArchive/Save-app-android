package net.opendasharchive.openarchive.services.snowbird.presentation.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdGroupOverviewBinding
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment

class SnowbirdGroupOverviewFragment private constructor(): BaseSnowbirdFragment() {
    private lateinit var viewBinding: FragmentSnowbirdGroupOverviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentSnowbirdGroupOverviewBinding.inflate(inflater)

        return viewBinding.root
    }

    override fun getToolbarTitle(): String {
        return "DWeb Storage Group Overview"
    }
}