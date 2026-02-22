package net.opendasharchive.openarchive.features.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseComposeActivity
import net.opendasharchive.openarchive.features.main.ui.SaveNavGraph
import net.opendasharchive.openarchive.features.main.ui.rememberNavigator
import net.opendasharchive.openarchive.core.config.AppConfig
import net.opendasharchive.openarchive.services.snowbird.SnowbirdBridge
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService
import net.opendasharchive.openarchive.util.PermissionManager
import org.koin.android.ext.android.inject
import net.opendasharchive.openarchive.core.navigation.NavigationResultKeys
import net.opendasharchive.openarchive.core.navigation.ResultEventBus
import net.opendasharchive.openarchive.upload.UploadJobScheduler
import net.opendasharchive.openarchive.util.C2paHelper

class HomeActivity : BaseComposeActivity() {

    private val appConfig by inject<AppConfig>()
    private val uploadJobScheduler by inject<UploadJobScheduler>()
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = getColor(R.color.colorTertiary),
                darkScrim = getColor(R.color.colorTertiary)
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = getColor(R.color.colorTertiary),
                darkScrim = getColor(R.color.colorTertiary)
            )
        )

        // Set up your Compose UI and pass callbacks.
        setContent {

            SaveAppTheme {

                val navigator = rememberNavigator()

                SaveNavGraph(
                    dialogManager,
                    navigator
                )
            }

        }

        if (appConfig.isDwebEnabled) {
            permissionManager = PermissionManager(this, dialogManager)
            permissionManager.checkNotificationPermission {
                AppLogger.i("Notification permission granted")
            }
            SnowbirdBridge.getInstance().initialize()
            startForegroundService(Intent(this, SnowbirdService::class.java))
            handleIntent(intent)
        }

        importSharedMedia(intent)
    }

    override fun onStart() {
        super.onStart()
        C2paHelper.init(this)
        uploadJobScheduler.schedule()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importSharedMedia(intent)
    }

    // ----- Permissions & Intent Handling -----
    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.takeIf { it.scheme == "save-veilid" }?.let { processUri(it) }
        }
    }

    private fun processUri(uri: Uri) {
        val path = uri.path
        val queryParams = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
        AppLogger.d("Path: $path, QueryParams: $queryParams")
    }

    private fun importSharedMedia(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if ((Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) && type != null) {
            val uris = mutableListOf<Uri>()

            if (Intent.ACTION_SEND == action) {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.add(it) }
            } else {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            }

            if (uris.isNotEmpty()) {
                // Publish the shared media result via ResultEventBus.
                // We'll use the same ResultEventBus key used in HomeScreen
                ResultEventBus.sendResult(
                    resultKey = NavigationResultKeys.SHARED_MEDIA_IMPORT,
                    result = uris
                )
            }
        }
    }
}