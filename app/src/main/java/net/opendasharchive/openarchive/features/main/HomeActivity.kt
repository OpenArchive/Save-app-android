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
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdBridge
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService
import net.opendasharchive.openarchive.util.PermissionManager
import org.koin.android.ext.android.inject

class HomeActivity : BaseComposeActivity() {

    private val appConfig by inject<AppConfig>()
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

        // Initialize the permission manager with this activity and its dialogManager.
        //permissionManager = PermissionManager(this, dialogManager)

        //if (appConfig.isDwebEnabled) {
        //    permissionManager.checkNotificationPermission {
        //        AppLogger.i("Notification permission granted")
        //    }
        //    SnowbirdBridge.getInstance().initialize()
        //    startForegroundService(Intent(this, SnowbirdService::class.java))
        //    handleIntent(intent)
        //}


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
}