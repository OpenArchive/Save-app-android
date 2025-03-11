package net.opendasharchive.openarchive.features.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySpaceSetupBinding
import net.opendasharchive.openarchive.extensions.onBackButtonPressed
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveFragment
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.features.settings.SpaceSetupFragment
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessFragment
import net.opendasharchive.openarchive.services.gdrive.GDriveFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdCreateGroupFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileListFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupListFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdJoinGroupFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoListFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdShareFragment
import net.opendasharchive.openarchive.services.webdav.WebDavFragment
import net.opendasharchive.openarchive.services.webdav.WebDavSetupLicenseFragment

enum class StartDestination {
    SPACE_TYPE,
    SPACE_LIST,
    DWEB_DASHBOARD,
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
}
