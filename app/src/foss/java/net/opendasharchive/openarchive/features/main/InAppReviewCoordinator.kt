package net.opendasharchive.openarchive.features.main

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable

/** No-op stubs for FOSS builds — Play Core is not available on F-Droid. */

@Composable
fun CheckForInAppUpdates(snackbarHostState: SnackbarHostState) {
    // No-op: F-Droid handles updates through its own update mechanism.
}

@Composable
fun CheckForInAppReview() {
    // No-op: Google Play In-App Review API is unavailable in FOSS builds.
}
