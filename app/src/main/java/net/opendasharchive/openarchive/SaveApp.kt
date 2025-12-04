package net.opendasharchive.openarchive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.orm.SugarApp
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.core.di.coreModule
import net.opendasharchive.openarchive.core.di.featuresModule
import net.opendasharchive.openarchive.core.di.passcodeModule
import net.opendasharchive.openarchive.core.di.retrofitModule
import net.opendasharchive.openarchive.core.di.unixSocketModule
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeManager
import net.opendasharchive.openarchive.util.Analytics
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.SessionManager
import net.opendasharchive.openarchive.core.analytics.AnalyticsManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class SaveApp : SugarApp(), SingletonImageLoader.Factory, DefaultLifecycleObserver {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    private fun applyTheme() {

        val useDarkMode = Prefs.getBoolean(getString(R.string.pref_key_use_dark_mode), false)
        val nightMode = if (useDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun onCreate() {
        super<SugarApp>.onCreate()

        // Initialize logging first
        AppLogger.init(applicationContext, initDebugger = true)

        // Initialize legacy Analytics (kept for backwards compatibility)
        Analytics.init(this)

        // Initialize new unified Analytics Manager (CleanInsights + Mixpanel + Firebase)
        AnalyticsManager.initialize(this)

        registerActivityLifecycleCallbacks(PasscodeManager())
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@SaveApp)
            modules(
                coreModule,
                featuresModule,
                retrofitModule,
                unixSocketModule,
                passcodeModule
            )
        }

        Prefs.load(this)
        applyTheme()

        if (Prefs.useTor) initNetCipher()

        // Legacy CleanInsightsManager (will be replaced by AnalyticsManager)
        CleanInsightsManager.init(this)

        // Register app lifecycle observer for session tracking
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Set user properties (GDPR-compliant)
        AnalyticsManager.setUserProperty("app_version", BuildConfig.VERSION_NAME)
        AnalyticsManager.setUserProperty("device_type", "android")

        createSnowbirdNotificationChannel()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App came to foreground
        SessionManager.startSession(this)
        SessionManager.onForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App went to background
        SessionManager.onBackground()
        SessionManager.endSession()

        // Persist analytics data
        AnalyticsManager.persist()
    }

    private fun initNetCipher() {
        AppLogger.d("Initializing NetCipher client")
        val oh = OrbotHelper.get(this)

        if (BuildConfig.DEBUG) {
            oh.skipOrbotValidation()
        }

//        oh.init()
    }

    private fun createSnowbirdNotificationChannel() {
        val silentChannel = NotificationChannel(
            SNOWBIRD_SERVICE_CHANNEL_SILENT,
            "Raven Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val chimeChannel = NotificationChannel(
            SNOWBIRD_SERVICE_CHANNEL_CHIME,
            "Raven Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(chimeChannel)
        notificationManager.createNotificationChannel(silentChannel)
    }

    companion object {
        const val SNOWBIRD_SERVICE_ID = 2601
        const val SNOWBIRD_SERVICE_CHANNEL_CHIME = "snowbird_service_channel_chime"
        const val SNOWBIRD_SERVICE_CHANNEL_SILENT = "snowbird_service_channel_silent"

        const val TOR_SERVICE_ID = 2602
        const val TOR_SERVICE_CHANNEL = "tor_service_channel"
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .logger(AppLogger.imageLogger)
            .build()
    }
}