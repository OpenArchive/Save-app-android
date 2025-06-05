package net.opendasharchive.openarchive.features.settings.passcode

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import net.opendasharchive.openarchive.features.settings.passcode.passcode_entry.PasscodeEntryActivity
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupActivity
import net.opendasharchive.openarchive.util.Prefs

class PasscodeManager: Application.ActivityLifecycleCallbacks {

    companion object {
        const val KEY_FAILED_ATTEMPTS = "passcode_failed_attempts"
        const val KEY_LOCKOUT_TIME = "passcode_lockout_time"
        const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    private var isAppInForeground = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Not implemented
    }

    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App enters foreground
            isAppInForeground = true
            if (shouldShowPasscode(activity)) {
                val intent = Intent(activity, PasscodeEntryActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        // Not implemented
    }

    override fun onActivityPaused(activity: Activity) {
        // Not implemented
    }

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App goes to background
            isAppInForeground = false
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Not implemented
    }

    override fun onActivityDestroyed(activity: Activity) {
        // Not implemented
    }

    private fun shouldShowPasscode(activity: Activity): Boolean {
        return Prefs.passcodeEnabled &&
                activity !is PasscodeEntryActivity &&
                activity !is PasscodeSetupActivity
    }
}