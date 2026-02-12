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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.api.session.SessionTracker
import net.opendasharchive.openarchive.analytics.di.analyticsModule
import net.opendasharchive.openarchive.db.MigrationWorker
import net.opendasharchive.openarchive.core.di.coreModule
import net.opendasharchive.openarchive.core.di.databaseModule
import net.opendasharchive.openarchive.core.di.featuresModule
import net.opendasharchive.openarchive.core.di.passcodeModule
import net.opendasharchive.openarchive.core.di.retrofitModule
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeManager
import net.opendasharchive.openarchive.util.Prefs
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.android.inject
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SaveApp : SugarApp(), SingletonImageLoader.Factory, DefaultLifecycleObserver {

    // Inject analytics dependencies
    private val analyticsManager: AnalyticsManager by inject()
    private val sessionTracker: SessionTracker by inject()

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

        registerActivityLifecycleCallbacks(PasscodeManager())

        Prefs.load(this)

        // --- ADD DATABASE SEEDER LOGIC HERE ---

        // Check if the seeder needs to run (e.g., first launch in DEBUG build)
        // You'll need to define BuildConfig.DEBUG or check an alternative flag.
        if (BuildConfig.DEBUG && !Prefs.didRunSeeder) {
            AppLogger.i("Database is empty, running seeder...")
            //DatabaseSeeder.seed(this)
            Prefs.didRunSeeder = true // Set flag to prevent future seeding
        }

        // --- END DATABASE SEEDER LOGIC ---

        // Trigger Room migration if needed
        if (!Prefs.isRoomMigrated) {
            val migrationRequest = OneTimeWorkRequestBuilder<MigrationWorker>()
                .build()
            WorkManager.getInstance(this).enqueueUniqueWork(
                "RoomMigration",
                ExistingWorkPolicy.KEEP,
                migrationRequest
            )
        }

        // Initialize Koin DI
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@SaveApp)
            modules(
                databaseModule,
                coreModule,
                passcodeModule,
                featuresModule,
                retrofitModule,
                analyticsModule(
                    mixpanelToken = getString(R.string.mixpanel_key),
                    cleanInsightsConsentChecker = { CleanInsightsManager.hasConsent() }
                )
            )
        }

        applyTheme()

        if (Prefs.useTor) initNetCipher()

        // Legacy CleanInsightsManager (kept for backwards compatibility)
        CleanInsightsManager.init(this)

        // Initialize analytics asynchronously BEFORE registering lifecycle observer
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            analyticsManager.initialize(this@SaveApp)

            // Set analytics manager for AppLogger
            AppLogger.setAnalyticsManager(analyticsManager)

            // Set app version for session tracker
            (sessionTracker as? net.opendasharchive.openarchive.analytics.api.session.SessionTrackerImpl)?.setAppVersion(
                BuildConfig.VERSION_NAME
            )

            // Set user properties (GDPR-compliant)
            analyticsManager.setUserProperty("app_version", BuildConfig.VERSION_NAME)
            analyticsManager.setUserProperty("device_type", "android")

            // Register app lifecycle observer AFTER analytics is initialized
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@SaveApp)
        }

        createSnowbirdNotificationChannel()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App came to foreground
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            sessionTracker.startSession()
            sessionTracker.onForeground()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App went to background
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            sessionTracker.onBackground()
            sessionTracker.endSession()

            // Persist analytics data
            analyticsManager.flush()
        }
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
