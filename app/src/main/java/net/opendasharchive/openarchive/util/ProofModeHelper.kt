package net.opendasharchive.openarchive.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import net.opendasharchive.openarchive.features.main.HomeActivity
import java.io.File

/**
 * Stub implementation of ProofModeHelper.
 * ProofMode library is not included in this build variant.
 * Methods are no-ops to allow compilation.
 */
object ProofModeHelper {

    fun init(activity: FragmentActivity, completed: () -> Unit) {
        completed()
    }

    fun removePgpKey(context: Context) {
        for (file in arrayOf(File(context.filesDir, "pkr.asc"), File(context.filesDir, "pub.asc"))) {
            try {
                file.delete()
            } catch (_: Exception) {
            }
        }
    }

    fun restartApp(activity: Activity) {
        val i = Intent(activity, HomeActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(i)
        activity.finish()
        Prefs.store()
        Runtime.getRuntime().exit(0)
    }
}
