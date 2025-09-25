package net.opendasharchive.openarchive.util

import android.annotation.SuppressLint
import android.content.Context
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import org.json.JSONObject

@SuppressLint("StaticFieldLeak")
object Analytics {

    const val APP_LOG = "app_log"
    const val APP_ERROR = "app_error"

    private var mixpanel: Any? = null

    fun init(context: Context) {
        if (BuildConfig.INCLUDE_MIXPANEL) {
            try {
                val token = context.getString(R.string.mixpanel_key)
                // Use reflection to avoid compilation issues in F-Droid builds
                val mixpanelClass = Class.forName("com.mixpanel.android.mpmetrics.MixpanelAPI")
                val getInstanceMethod = mixpanelClass.getDeclaredMethod("getInstance", Context::class.java, String::class.java, Boolean::class.java)
                mixpanel = getInstanceMethod.invoke(null, context, token, false)
                AppLogger.d("Analytics", "Mixpanel initialized")
            } catch (e: Exception) {
                AppLogger.d("Analytics", "Mixpanel not available: ${e.message}")
            }
        } else {
            AppLogger.d("Analytics", "Analytics disabled in F-Droid build")
        }
    }

    fun log(eventName: String, props: Map<String?, Any?>? = null) {
        if (!BuildConfig.INCLUDE_MIXPANEL) {
            AppLogger.d("Analytics", "Event: $eventName (F-Droid - no external analytics)")
            return
        }

        try {
            val jsonObject = props?.let { strongProps ->
                JSONObject(strongProps)
            }

            mixpanel?.let { mp ->
                val trackMethod = mp.javaClass.getDeclaredMethod("track", String::class.java, JSONObject::class.java)
                trackMethod.invoke(mp, eventName, jsonObject)
            }
        } catch (e: Exception) {
            AppLogger.d("Analytics", "Failed to log analytics: ${e.message}")
        }
    }
}