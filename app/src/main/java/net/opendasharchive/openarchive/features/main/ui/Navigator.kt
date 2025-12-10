package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.opendasharchive.openarchive.util.Prefs

class Navigator(
    initialBackstack: SnapshotStateList<AppRoute> = mutableListOf<AppRoute>(
        if (Prefs.didCompleteOnboarding) AppRoute.HomeRoute else AppRoute.WelcomeRoute
    ).toMutableStateList()
) {
    var backstack: SnapshotStateList<AppRoute> by mutableStateOf(initialBackstack)
        private set

    fun navigateTo(route: AppRoute) {
        backstack.add(route)
    }

    fun navigateBack() {
        backstack.removeLastOrNull()
    }

    fun popBackTo(route: AppRoute, inclusive: Boolean = false) {
        val index = backstack.indexOfLast { it == route }
        if (index != -1) {
            val targetIndex = if (inclusive) index else index + 1
            if (targetIndex < backstack.size) {
                backstack.subList(targetIndex, backstack.size).clear()
            }
        }
    }

    fun navigateAndClear(route: AppRoute) {
        backstack = mutableStateListOf(route)
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
                ).toMutableStateList()

                Navigator(
                    if (routes.isEmpty()) {
                        val startDestination =
                            if (Prefs.didCompleteOnboarding) AppRoute.HomeRoute else AppRoute.WelcomeRoute
                        mutableStateListOf(startDestination)
                    } else {
                        routes.toMutableStateList()
                    }
                )
            }
        )
    }
}

@Composable
fun rememberNavigator(): Navigator {
    return rememberSaveable(saver = Navigator.saver()) {
        Navigator()
    }
}
