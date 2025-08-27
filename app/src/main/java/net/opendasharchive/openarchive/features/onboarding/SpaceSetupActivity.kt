package net.opendasharchive.openarchive.features.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySpaceSetupBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable

enum class StartDestination {
    SPACE_TYPE,
    SPACE_LIST,
    DWEB_DASHBOARD,
    ADD_FOLDER,
    ADD_NEW_FOLDER,
    STORACHA
}

class SpaceSetupActivity : BaseActivity() {

    companion object {
        const val FRAGMENT_TAG = "ssa_fragment"
    }

    private lateinit var binding: ActivitySpaceSetupBinding

    private lateinit var navController: NavController
    private lateinit var navGraph: NavGraph
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySpaceSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(
            showBackButton = true
        )


//        onBackButtonPressed {
//
//            if (supportFragmentManager.backStackEntryCount > 1) {
//                // We still have fragments in the back stack to pop
//                supportFragmentManager.popBackStack()
//                true // fully handled here
//            } else {
//                // No more fragments left in back stack, let the system finish Activity
//                false
//            }
//        }


        initSpaceSetupNavigation()
    }

    private fun initSpaceSetupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.space_nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        navGraph = navController.navInflater.inflate(R.navigation.space_setup_navigation)

        val startDestinationString =
            intent.getStringExtra("start_destination") ?: StartDestination.SPACE_TYPE.name
        val startDestination = StartDestination.valueOf(startDestinationString)
        when (startDestination) {
            StartDestination.SPACE_LIST -> {
                navGraph.setStartDestination(R.id.fragment_space_list)
            }
            StartDestination.ADD_FOLDER -> {
                navGraph.setStartDestination(R.id.fragment_add_folder)
            }
            StartDestination.ADD_NEW_FOLDER -> {
                navGraph.setStartDestination(R.id.fragment_create_new_folder)
            }
            StartDestination.STORACHA -> {
                navGraph.setStartDestination(R.id.fragment_storacha)
            }
            else -> {
                navGraph.setStartDestination(R.id.fragment_space_setup)
            }
        }
        navController.graph = navGraph

        appBarConfiguration = AppBarConfiguration(emptySet())
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    fun updateToolbarFromFragment(fragment: Fragment) {
        if (fragment is ToolbarConfigurable) {
            val title = fragment.getToolbarTitle()
            val subtitle = fragment.getToolbarSubtitle()
            val showBackButton = fragment.shouldShowBackButton()
            setupToolbar(title = title, showBackButton = showBackButton)
            supportActionBar?.subtitle = subtitle
        } else {
            // Default toolbar configuration if fragment doesn't implement interface
            setupToolbar(title = "Servers", showBackButton = true)
            supportActionBar?.subtitle = null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.space_nav_host_fragment).navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear any pending messages or callbacks in the main thread handler
        window?.decorView?.handler?.removeCallbacksAndMessages(null)
        binding.commonAppBar.commonToolbar.setNavigationOnClickListener(null)

        // Remove navigation reference (if using Jetpack Navigation)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.space_nav_host_fragment) as? NavHostFragment
        navHostFragment?.let {
            it.childFragmentManager.fragments.forEach { fragment ->
                fragment.view?.let { view ->
                    view.handler?.removeCallbacksAndMessages(null)
                }
            }
        }
    }
}
