package net.opendasharchive.openarchive.features.main

import android.app.Activity
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import net.opendasharchive.openarchive.core.logger.AppLogger

/**
 * FOSS InAppUpdateCoordinator stub for F-Droid builds
 *
 * F-Droid handles updates through its own mechanism, so in-app update checking is not needed.
 * This is a no-op implementation that maintains API compatibility with the GMS version.
 */
internal class InAppUpdateCoordinator(
    private val activity: Activity,
    private val rootView: View,
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest>
) {

    fun onResume() {
        // No-op for FOSS builds
        // F-Droid handles updates automatically through its own update mechanism
        AppLogger.d("InAppUpdateCoordinator", "FOSS build - updates handled by F-Droid")
    }

    fun onDestroy() {
        // No cleanup needed
    }
}
