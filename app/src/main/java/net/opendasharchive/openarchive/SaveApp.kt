package net.opendasharchive.openarchive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.util.Log
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
import net.opendasharchive.openarchive.services.storacha.di.storachaModule
import net.opendasharchive.openarchive.util.Analytics
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SaveApp : SugarApp(), SingletonImageLoader.Factory {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    private fun applyTheme() {

        val useDarkMode = Prefs.getBoolean(getString(R.string.pref_key_use_dark_mode), false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
            val darkMode = if (useDarkMode) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
            uiModeManager.setApplicationNightMode(darkMode)
        } else {
            val darkMode = if (useDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(darkMode)
        }
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
                passcodeModule,
                storachaModule,
            )
        }

        Prefs.load(this)
        applyTheme()

        if (Prefs.useTor) initNetCipher()

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
            "Dweb Storage",
            NotificationManager.IMPORTANCE_LOW
        )

        val chimeChannel = NotificationChannel(
            SNOWBIRD_SERVICE_CHANNEL_CHIME,
            "Dweb Storage",
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