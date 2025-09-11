package net.opendasharchive.openarchive.util

import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

enum class DeviceConfiguration {
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
    DESKTOP;

    companion object {

        fun fromWindowSizeClass(window: WindowSizeClass): DeviceConfiguration {
            val W_MED = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND      // 600
            val W_EXP = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND    // 840
            val H_MED = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND     // 480
            val H_EXP = WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND   // 900

            // Order matters: check bigger layouts first.
            return when {
                // Very large: both width & height in expanded buckets
                window.isAtLeastBreakpoint(W_EXP, H_EXP) -> DESKTOP

                // Tablet landscape: wide (expanded) with at least medium height
                window.isAtLeastBreakpoint(W_EXP, H_MED) -> TABLET_LANDSCAPE

                // Tablet portrait: medium width and expanded height
                window.isAtLeastBreakpoint(W_MED, H_EXP) -> TABLET_PORTRAIT

                // Mobile landscape: expanded width but short height
                window.isWidthAtLeastBreakpoint(W_EXP) &&
                        !window.isHeightAtLeastBreakpoint(H_MED) -> MOBILE_LANDSCAPE

                // Mobile portrait: compact width with at least medium height
                !window.isWidthAtLeastBreakpoint(W_MED) &&
                        window.isHeightAtLeastBreakpoint(H_MED) -> MOBILE_PORTRAIT

                // Fallback (very small/odd shapes)
                else -> MOBILE_PORTRAIT
            }
        }


        fun fromWindowSizeClassLegacy(windowSizeClass: WindowSizeClass): DeviceConfiguration {
            val widthClass = windowSizeClass.windowWidthSizeClass
            val heightClass = windowSizeClass.windowHeightSizeClass

            return when {
                widthClass == WindowWidthSizeClass.COMPACT &&
                        heightClass == WindowHeightSizeClass.MEDIUM -> MOBILE_PORTRAIT
                widthClass == WindowWidthSizeClass.COMPACT &&
                        heightClass == WindowHeightSizeClass.EXPANDED -> MOBILE_PORTRAIT
                widthClass == WindowWidthSizeClass.EXPANDED &&
                        heightClass == WindowHeightSizeClass.COMPACT -> MOBILE_LANDSCAPE
                widthClass == WindowWidthSizeClass.MEDIUM &&
                        heightClass == WindowHeightSizeClass.EXPANDED -> TABLET_PORTRAIT
                widthClass == WindowWidthSizeClass.EXPANDED &&
                        heightClass == WindowHeightSizeClass.MEDIUM -> TABLET_LANDSCAPE
                else -> DESKTOP
            }
        }
    }
}