package net.opendasharchive.openarchive.features.onboarding

import android.os.Bundle
import androidx.core.os.bundleOf
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
import net.opendasharchive.openarchive.features.settings.FoldersFragment

enum class StartDestination {
    SPACE_TYPE,
    SPACE_LIST,
    ADD_FOLDER,
    ADD_NEW_FOLDER,
    STORACHA,
    ARCHIVED_FOLDER_LIST
}

class SpaceSetupActivity : BaseActivity() {

    companion object {
        const val EXTRA_FOLDER_ID = "folder_id"
        const val EXTRA_FOLDER_NAME = "folder_name"
        const val LABEL_START_DESTINATION = "start_destination"
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
        navGraph = navController.navInflater.inflate(R.navigation.app_nav_graph)

        val startDestinationString =
            intent.getStringExtra(LABEL_START_DESTINATION) ?: StartDestination.SPACE_TYPE.name
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
            StartDestination.ARCHIVED_FOLDER_LIST -> {
                navGraph.setStartDestination(R.id.fragment_folders)

                // Pass arguments from intent to navigation graph
                val showArchived = intent.getBooleanExtra(FoldersFragment.EXTRA_SHOW_ARCHIVED, false)
                val selectedSpaceId = intent.getLongExtra(FoldersFragment.EXTRA_SELECTED_SPACE_ID, -1L)
                val selectedProjectId = intent.getLongExtra(FoldersFragment.EXTRA_SELECTED_PROJECT_ID, -1L)

                val bundle = bundleOf(
                    "show_archived" to showArchived,
                    "selected_space_id" to selectedSpaceId,
                    "selected_project_id" to selectedProjectId
                )

                navController.setGraph(navGraph, bundle)
                return // Early return to avoid setting graph again
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
