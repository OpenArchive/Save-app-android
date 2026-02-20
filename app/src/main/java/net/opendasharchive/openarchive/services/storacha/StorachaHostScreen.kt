package net.opendasharchive.openarchive.services.storacha

import androidx.compose.runtime.Composable
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
@Composable
fun StorachaHostScreen(fragmentManager: FragmentManager) {
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                id = R.id.storacha_fragment_container
            }
        },
        update = { view ->
            if (fragmentManager.findFragmentById(view.id) == null) {
                val navHost = NavHostFragment.create(R.navigation.storacha_nav_graph)
                fragmentManager.beginTransaction()
                    .replace(view.id, navHost)
                    .setPrimaryNavigationFragment(navHost)
                    .commitNow()
            }
        }
    )
}
