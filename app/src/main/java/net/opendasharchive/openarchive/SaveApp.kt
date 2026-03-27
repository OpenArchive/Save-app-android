package net.opendasharchive.openarchive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.orm.SugarApp
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import net.opendasharchive.openarchive.core.repositories.CacheCleanupWorker
import net.opendasharchive.openarchive.core.di.torModule
import net.opendasharchive.openarchive.services.tor.TorConstants
import net.opendasharchive.openarchive.services.tor.TorServiceManager
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.analytics.api.session.SessionTracker
import net.opendasharchive.openarchive.core.security.C2paKeyStore
import net.opendasharchive.openarchive.analytics.di.analyticsModule
import net.opendasharchive.openarchive.db.MigrationWorker
import net.opendasharchive.openarchive.core.di.coreModule
import net.opendasharchive.openarchive.core.di.databaseModule
import net.opendasharchive.openarchive.core.di.featuresModule
import net.opendasharchive.openarchive.core.di.passcodeModule
import net.opendasharchive.openarchive.core.di.retrofitModule
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.services.storacha.di.storachaModule
import net.opendasharchive.openarchive.util.C2paHelper
import net.opendasharchive.openarchive.util.CleanInsightsManager
import net.opendasharchive.openarchive.util.Prefs
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SaveApp : SugarApp(), SingletonImageLoader.Factory, DefaultLifecycleObserver {

    // Inject analytics dependencies
    private val analyticsManager: AnalyticsManager by inject()
    private val sessionTracker: SessionTracker by inject()

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // Initialize ACRA for FOSS builds (no-op for GMS builds)
        AppLogger.initAcra(this)
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

        Prefs.load(this)

        // Initialize C2PA Helper
        C2paHelper.init(this)

        // Trigger Room migration if needed (SugarORM → Room)
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
                torModule,
                storachaModule,
                analyticsModule(
                    mixpanelToken = getString(R.string.mixpanel_key),
                    cleanInsightsConsentChecker = { CleanInsightsManager.hasConsent() }
                )
            )
        }

        applyTheme()

        // Migrate C2PA keys from plaintext SharedPreferences to SecureStorage (one-time)
        val c2paKeyStore: C2paKeyStore by inject()
        c2paKeyStore.migrateFromPrefsIfNeeded(
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )

        // Schedule periodic cache cleanup (runs every 7 days when battery is not low)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CacheCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CacheCleanupWorker>(
                CacheCleanupWorker.REPEAT_INTERVAL_DAYS,
                java.util.concurrent.TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
        )

        // Start embedded Tor service if enabled
        if (Prefs.useTor) {
            val torServiceManager: TorServiceManager by inject()
            torServiceManager.start()
        }

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
        createTorNotificationChannel()
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

    override fun onTerminate() {
        super.onTerminate()
        // Clean up Tor service when app is terminated
        if (Prefs.useTor) {
            val torServiceManager: TorServiceManager by inject()
            torServiceManager.cleanup()
        }
    }

    private fun createTorNotificationChannel() {
        val channel = NotificationChannel(
            TorConstants.TOR_NOTIFICATION_CHANNEL_ID,
            getString(R.string.tor_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.tor_notification_channel_description)
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
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
