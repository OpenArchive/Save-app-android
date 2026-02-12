package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySnowbirdBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable


class SnowbirdActivity : BaseActivity() {

    private lateinit var binding: ActivitySnowbirdBinding
    private lateinit var navController: NavController
    private lateinit var navGraph: NavGraph
    private lateinit var appBarConfiguration: AppBarConfiguration


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySnowbirdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(showBackButton = true)
        initSnowbirdNavigation()
    }

    private fun initSnowbirdNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.snowbird_nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        navGraph = navController.navInflater.inflate(R.navigation.snowbird_nav_graph)

        appBarConfiguration = AppBarConfiguration(emptySet())
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    fun updateToolbarFromFragment(fragment: Fragment) {
        if (fragment is ToolbarConfigurable) {
            val isVisible = fragment.isToolbarVisible()
            binding.commonAppBar.root.visibility = if (isVisible) View.VISIBLE else View.GONE
            
            if (isVisible) {
                val title = fragment.getToolbarTitle()
                val subtitle = fragment.getToolbarSubtitle()
                val showBackButton = fragment.shouldShowBackButton()
                setupToolbar(title = title, showBackButton = showBackButton)
                supportActionBar?.subtitle = subtitle
            }
        } else {
            binding.commonAppBar.root.visibility = View.VISIBLE
            setupToolbar(title = getString(R.string.dweb_title), showBackButton = true)
            supportActionBar?.subtitle = null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (::navController.isInitialized && navController.currentDestination?.id == R.id.snowbird_dashboard) {
            finish()
            true
        } else {
            navController.navigateUp() || super.onSupportNavigateUp()
        }
    }
}
