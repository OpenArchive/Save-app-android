package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupSuccessBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets

class SpaceSetupSuccessFragment : BaseFragment() {

    // Toggle to switch between XML and Compose implementation
    private val useComposeImplementation = true  // Set to false to use XML implementation

    private lateinit var binding: FragmentSpaceSetupSuccessBinding
    private val args: SpaceSetupSuccessFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        if (useComposeImplementation) {
            // Use Compose implementation
            return ComposeView(requireContext()).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    SaveAppTheme {
                        SpaceSetupSuccessScreen(
                            onNavigateToMain = {
                                // Navigation already handled in ViewModel
                            }
                        )
                    }
                }
            }
        }

        // Original XML implementation
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
            if (args.spaceType == Space.Type.RAVEN) {
                val navController = findNavController()
                // Let's clear and navigate to snowbird fragment
                val popped = navController.popBackStack(R.id.fragment_snowbird, false)
                if (!popped) {
                    navController.navigate(
                        R.id.fragment_snowbird,
                        null,
                        navOptions {
                            popUpTo(R.id.fragment_snowbird) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    )
                } else {
                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                //val action = SpaceSetupSuccessFragmentDirections.actionFragmentSpaceSetupSuccessToFragmentSnowbird()
                //navController.navigate(action)
            } else {
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clears backstack
                startActivity(intent)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (useComposeImplementation) {
            return
        }
        if (args.spaceType == Space.Type.RAVEN) {
            // Add the menu provider
            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_space_setup_success, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_share_group -> {
                            if (args.dwebGroupKey != null) {
                                val action =
                                    SpaceSetupSuccessFragmentDirections.actionFragmentSpaceSetupSuccessToFragmentSnowbirdShareGroup(
                                        dwebGroupKey = args.dwebGroupKey!!,
                                        isSetupOngoing = true
                                    )

                                findNavController().navigate(action)
                            }
                            true
                        }

                        else -> false
                    }
                }
            }, viewLifecycleOwner)
        }
    }

    override fun getToolbarTitle() = getString(R.string.space_setup_success_title)
    override fun shouldShowBackButton() = false
}