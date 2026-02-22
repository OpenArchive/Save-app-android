package net.opendasharchive.openarchive.services.storacha

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import net.opendasharchive.openarchive.R

/**
 * Compose entry point for the Storacha feature.
 *
 * Embeds the Storacha Fragment navigation graph inside a Compose screen using a
 * FragmentContainerView + NavHostFragment. This is the "compatibility island" approach
 * that avoids a full Compose rewrite of all 18 Storacha Fragments while integrating
 * with the new Compose-based app navigation.
 *
 * Back-stack handling: The nested NavHostFragment manages Storacha's own back-stack.
 * Pressing Back at the root of the Storacha graph will propagate to the Compose
 * back-stack and return to the Home screen.
 */
@Suppress("FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorachaHostScreen(fragmentManager: FragmentManager) {
    val toolbarConfig by StorachaToolbarState.config.collectAsState()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(toolbarConfig.title) },
                navigationIcon = {
                    if (toolbarConfig.showBack) {
                        IconButton(
                            onClick = {
                                val navHost =
                                    fragmentManager.findFragmentById(R.id.storacha_fragment_container)
                                        as? NavHostFragment
                                val popped = navHost?.navController?.popBackStack() == true
                                if (!popped) {
                                    backDispatcher?.onBackPressed()
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    toolbarConfig.actions.forEach { action ->
                        TextButton(onClick = action.onClick) {
                            Text(action.label)
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colorResource(R.color.colorTertiary),
                        titleContentColor = colorResource(R.color.colorOnPrimary),
                        navigationIconContentColor = colorResource(R.color.colorOnPrimary),
                        actionIconContentColor = colorResource(R.color.colorOnPrimary),
                    ),
            )
        },
    ) { paddingValues ->
        DisposableEffect(Unit) {
            onDispose {
                val fragment = fragmentManager.findFragmentById(R.id.storacha_fragment_container)
                if (fragment != null) {
                    fragmentManager
                        .beginTransaction()
                        .remove(fragment)
                        .commitNowAllowingStateLoss()
                }
            }
        }

        AndroidView(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = R.id.storacha_fragment_container
                }
            },
            update = { view ->
                if (fragmentManager.findFragmentById(view.id) == null) {
                    val navHost =
                        NavHostFragment.create(R.navigation.storacha_nav_graph)
                    fragmentManager
                        .beginTransaction()
                        .replace(view.id, navHost)
                        .setPrimaryNavigationFragment(navHost)
                        .commitNow()
                }
            },
        )
    }
}
