package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.bundle.bundleOf
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
import net.opendasharchive.openarchive.features.core.NavArgument
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable

enum class StartDestination {
    SPACE_TYPE,
    SPACE_LIST,
    DWEB_DASHBOARD,
    FOLDER_LIST,
    ADD_FOLDER,
    ADD_NEW_FOLDER
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navController.handleDeepLink(intent)
    }

    private fun initSpaceSetupNavigation() {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.space_nav_host_fragment) as NavHostFragment

        navController = navHost.navController
        navGraph = navController.navInflater.inflate(R.navigation.space_setup_navigation)

        val startDestName =
            intent.getStringExtra(NavArgument.START_DESTINATION) ?: StartDestination.SPACE_TYPE.name
        val startDestination = StartDestination.valueOf(startDestName)
        @IdRes val startDest = when (startDestination) {
            StartDestination.SPACE_TYPE -> R.id.fragment_space_setup
            StartDestination.SPACE_LIST -> R.id.fragment_space_list
            StartDestination.DWEB_DASHBOARD -> R.id.fragment_snowbird
            StartDestination.FOLDER_LIST -> R.id.fragment_folder_list
            StartDestination.ADD_FOLDER -> R.id.fragment_add_folder
            StartDestination.ADD_NEW_FOLDER -> R.id.fragment_create_new_folder
        }

        val startArgs: Bundle? = if (startDestination == StartDestination.FOLDER_LIST) {
            bundleOf(NavArgument.SHOW_ARCHIVED_FOLDERS to intent.getBooleanExtra(NavArgument.SHOW_ARCHIVED_FOLDERS, false))
            bundleOf(NavArgument.SPACE_ID to intent.getLongExtra(NavArgument.SPACE_ID, -1L))
            bundleOf(NavArgument.FOLDER_ID to intent.getLongExtra(NavArgument.FOLDER_ID, -1L))
        } else null

        navGraph.setStartDestination(startDest)

        navController.setGraph(navGraph, startArgs)

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
