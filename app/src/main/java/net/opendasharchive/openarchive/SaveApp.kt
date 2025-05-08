package net.opendasharchive.openarchive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.orm.SugarApp
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.core.di.coreModule
import net.opendasharchive.openarchive.core.di.featuresModule
import net.opendasharchive.openarchive.services.tor.TOR_SERVICE_CHANNEL
import net.opendasharchive.openarchive.services.tor.TorViewModel
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import timber.log.Timber


class SaveApp : SugarApp(), KoinComponent {

    private val torViewModel: TorViewModel by inject()

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SaveApp)
            modules(coreModule, featuresModule)
        }

        val config = ImagePipelineConfig.newBuilder(this)
            .setProgressiveJpegConfig(SimpleProgressiveJpegConfig())
            .setResizeAndRotateEnabledForNetwork(true)
            .setDownsampleEnabled(true)
            .build()

        Fresco.initialize(this, config)
        Prefs.load(this)

        if (Prefs.useTor) {
            OrbotHelper.get(this).init()
            initTor()
        }

        Theme.set(Prefs.theme)

        CleanInsightsManager.init(this)

        createTorNotificationChannel()

        // enable timber logging library for debug builds
        if (BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
            Timber.tag("SAVE")
        }
    }

    private fun initTor() {
        Timber.d( "Initializing internal tor client")
        torViewModel.updateTorServiceState()
    }


    private fun createTorNotificationChannel() {
        val name = "Tor Service"
        val descriptionText = "Keeps the Tor service running"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(TOR_SERVICE_CHANNEL, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

}
