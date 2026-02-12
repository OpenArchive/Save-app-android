package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.opendasharchive.openarchive.util.Prefs

class Navigator(
    private val startDestination: AppRoute = defaultStartDestination(),
    initialBackstack: List<AppRoute> = listOf(startDestination)
) {
    val backstack: SnapshotStateList<AppRoute> =
        initialBackstack.ifEmpty { listOf(startDestination) }.toMutableStateList()

    fun navigateTo(route: AppRoute) {
        backstack.add(route)
    }

    fun navigateBack() {
        if (backstack.size > 1) {
            backstack.removeLastOrNull()
        }
    }

    fun popBackTo(route: AppRoute, inclusive: Boolean = false) {
        val index = backstack.indexOfLast { it == route }
        if (index != -1) {
            val targetIndex = if (inclusive) index else index + 1
            if (targetIndex < backstack.size) {
                backstack.subList(targetIndex, backstack.size).clear()
            }
            if (backstack.isEmpty()) {
                backstack.add(startDestination)
            }
        }
    }

    fun navigateAndClear(route: AppRoute) {
        backstack.clear()
        backstack.add(route)
    }

    fun currentRoute(): AppRoute? {
        return backstack.lastOrNull()
    }

    companion object {
        private val json = Json { encodeDefaults = true }

        fun saver(): Saver<Navigator, String> = Saver(
            save = { navigator ->
                json.encodeToString(
                    ListSerializer(AppRoute.serializer()),
                    navigator.backstack.toList()
                )
            },
            restore = { saved ->
                val routes = json.decodeFromString(
                    ListSerializer(AppRoute.serializer()),
                    saved
                )

                Navigator(
                    initialBackstack = routes
                )
            }
        )

        private fun defaultStartDestination(): AppRoute {
            return if (Prefs.didCompleteOnboarding) AppRoute.HomeRoute else AppRoute.WelcomeRoute
        }
    }
}

@Composable
fun rememberNavigator(): Navigator {
    return rememberSaveable(saver = Navigator.saver()) {
        Navigator()
    }
}
