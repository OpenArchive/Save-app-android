package net.opendasharchive.openarchive.upload

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.repositories.MediaRepository
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asUiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.services.tor.TorServiceManager
import net.opendasharchive.openarchive.services.tor.TorStatus
import net.opendasharchive.openarchive.util.NetworkUtils
import net.opendasharchive.openarchive.util.Prefs

/**
 * Central gate for all user-initiated and background-resume uploads.
 *
 * Checks (in order):
 *  1. Wi-Fi only setting — applies to ALL vault types.
 *     "Allow any connection" → disables wifi-only and proceeds.
 *  2. Tor connection — applies to all vault types EXCEPT DWeb (Snowbird routes its own traffic).
 *     "Proceed" → disables Tor, stops the service, and proceeds.
 *
 * [check] — for explicit user actions (always runs checks).
 * [checkIfQueued] — for background resume (skips checks silently if queue is empty).
 *
 * Pass [vaultType] so DWeb uploads skip the Tor check.
 * Pass [onProceed] as the action to run once all checks pass.
 */
class UploadGate(
    private val application: Application,
    private val dialogManager: DialogStateManager,
    private val torServiceManager: TorServiceManager,
    private val mediaRepository: MediaRepository,
) {

    /** For explicit user-initiated upload actions. Always runs checks. */
    fun check(vaultType: VaultType? = null, onProceed: () -> Unit) {
        checkWifi(vaultType = vaultType, onProceed = onProceed)
    }

    /**
     * For background resume (e.g. onResume).
     * Skips checks silently if nothing is queued — avoids spurious dialogs
     * every time the user returns to the app with an empty queue.
     */
    fun checkIfQueued(vaultType: VaultType? = null, onProceed: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val hasQueued = mediaRepository.getQueue().isNotEmpty()
            if (hasQueued) {
                checkWifi(vaultType = vaultType, onProceed = onProceed)
            }
        }
    }

    private fun checkWifi(vaultType: VaultType?, onProceed: () -> Unit) {
        if (Prefs.uploadWifiOnly && !NetworkUtils.isOnWifi(application)) {
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Warning
                title = R.string.wifi_not_connected.asUiText()
                message = R.string.wifi_required_upload_message.asUiText()
                positiveButton {
                    text = UiText.Resource(R.string.allow_any_connection)
                    action = {
                        Prefs.uploadWifiOnly = false
                        checkTor(vaultType = vaultType, onProceed = onProceed)
                    }
                }
                neutralButton {
                    text = UiText.Resource(R.string.ignore)
                }
            }
            return
        }
        checkTor(vaultType = vaultType, onProceed = onProceed)
    }

    private fun checkTor(vaultType: VaultType?, onProceed: () -> Unit) {
        // DWeb (Snowbird) manages its own networking — Tor routing is irrelevant.
        if (vaultType == VaultType.DWEB_STORAGE) {
            onProceed()
            return
        }

        if (Prefs.useTor && !torServiceManager.isReady()) {
            val torStatus = torServiceManager.torStatus.value
            val messageRes = when (torStatus) {
                is TorStatus.Starting -> R.string.tor_still_connecting_message
                is TorStatus.Error    -> R.string.tor_error_message
                else                  -> R.string.tor_not_connected_message
            }
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Warning
                title = R.string.tor_not_connected.asUiText()
                message = messageRes.asUiText()
                positiveButton {
                    text = UiText.Resource(R.string.proceed)
                    action = {
                        Prefs.useTor = false
                        torServiceManager.stop()
                        onProceed()
                    }
                }
                neutralButton {
                    text = UiText.Resource(R.string.action_cancel)
                }
            }
            return
        }

        onProceed()
    }
}
