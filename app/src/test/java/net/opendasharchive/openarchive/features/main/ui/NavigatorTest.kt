package net.opendasharchive.openarchive.features.main.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorTest {

    @Test
    fun navigateBack_keepsStartDestination() {
        val navigator = Navigator(
            startDestination = AppRoute.HomeRoute,
            initialBackstack = listOf(AppRoute.HomeRoute)
        )

        navigator.navigateBack()

        assertEquals(listOf(AppRoute.HomeRoute), navigator.backstack.toList())
    }

    @Test
    fun popBackTo_inclusiveRoot_keepsStartDestination() {
        val navigator = Navigator(
            startDestination = AppRoute.HomeRoute,
            initialBackstack = listOf(AppRoute.HomeRoute, AppRoute.SpaceSetupRoute)
        )

        navigator.popBackTo(AppRoute.HomeRoute, inclusive = true)

        assertEquals(listOf(AppRoute.HomeRoute), navigator.backstack.toList())
    }
}

