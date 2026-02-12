package net.opendasharchive.openarchive.services.snowbird.presentation.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.MenuProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupSuccessBinding
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets

class SnowbirdSetupSuccessFragment : BaseSnowbirdFragment() {

    private lateinit var binding: FragmentSpaceSetupSuccessBinding
    private val args: SnowbirdSetupSuccessFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSpaceSetupSuccessBinding.inflate(inflater)

        binding.mainContainer.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars()
        ) { insets ->
            bottomMargin = insets.bottom
        }

        binding.buttonBar.applyEdgeToEdgeInsets(
            typeMask = WindowInsetsCompat.Type.navigationBars()
        ) { insets ->
            bottomMargin = insets.bottom
        }

        if (args.message.isNotEmpty()) {
            binding.successMessage.text = args.message
        }

        binding.btAuthenticate.setOnClickListener { _ ->
            val navController = findNavController()
            // Navigate to Snowbird Dashboard and clear this success screen from back stack
            navController.navigate(
                R.id.snowbird_dashboard,
                null,
                navOptions {
                    // Clear the entire Snowbird graph so dashboard becomes the new root
                    popUpTo(R.id.snowbird_nav_graph) { inclusive = true }
                    launchSingleTop = true
                }
            )
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


            // Add the menu provider
            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_space_setup_success, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_share_group -> {
                            val action = SnowbirdSetupSuccessFragmentDirections.Companion.actionFragmentSnowbirdSetupSuccessToFragmentSnowbirdShareGroup(
                                    dwebGroupKey = args.dwebGroupKey,
                                    isSetupOngoing = true
                                )

                            findNavController().navigate(action)
                            true
                        }

                        else -> false
                    }
                }
            }, viewLifecycleOwner)

    }

    override fun getToolbarTitle() = getString(R.string.space_setup_success_title)
    override fun shouldShowBackButton() = false

}
