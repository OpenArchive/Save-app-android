package net.opendasharchive.openarchive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.util.Logger
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
import net.opendasharchive.openarchive.util.Theme
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class SaveApp : SugarApp(), SingletonImageLoader.Factory {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        Analytics.init(this)
        AppLogger.init(applicationContext, initDebugger = true)
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

        if (Prefs.useTor) initNetCipher()

        val useDarkMode = Prefs.getBoolean(getString(R.string.pref_key_use_dark_mode), false)
        Theme.darkModeEnabled = useDarkMode

        CleanInsightsManager.init(this)

        createSnowbirdNotificationChannel()
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
        return ImageLoader.Builder(this).logger(object : Logger {
            override var minLevel: Logger.Level = Logger.Level.Verbose

            override fun log(
                tag: String,
                level: Logger.Level,
                message: String?,
                throwable: Throwable?
            ) {
                Timber.tag("Coil3:$tag").log(level.ordinal, throwable, message)
            }
        })
            .build()
    }
}