package net.opendasharchive.openarchive.features.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseComposeActivity
import net.opendasharchive.openarchive.features.main.ui.Navigator
import net.opendasharchive.openarchive.features.main.ui.SaveNavGraph
import net.opendasharchive.openarchive.core.config.AppConfig
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeGate
import net.opendasharchive.openarchive.services.snowbird.SnowbirdBridge
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService
import net.opendasharchive.openarchive.util.PermissionManager
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityRetainedScope
import net.opendasharchive.openarchive.core.navigation.NavigationResultKeys
import net.opendasharchive.openarchive.core.navigation.ResultEventBus
import net.opendasharchive.openarchive.upload.UploadJobScheduler
import net.opendasharchive.openarchive.util.C2paHelper

class HomeActivity : BaseComposeActivity(), AndroidScopeComponent {

    override val scope by activityRetainedScope()

    private val appConfig by inject<AppConfig>()
    private val navigator by inject<Navigator>()
    private val uploadJobScheduler by inject<UploadJobScheduler>()
    private val passcodeGate by inject<PasscodeGate>()
    private lateinit var permissionManager: PermissionManager

    /** URIs received via share sheet while the app was locked — delivered after authentication. */
    private var pendingSharedUris: List<Uri>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register PasscodeGate on the process lifecycle so onStop only fires when the
        // entire app goes to background — not during Activity-to-Activity transitions
        // (e.g. PasscodeEntryActivity / PasscodeSetupActivity launching over HomeActivity).
        ProcessLifecycleOwner.get().lifecycle.addObserver(passcodeGate)

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

        setContent {
            SaveAppTheme {
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

        // Flush any share URIs that arrived while the app was locked
        lifecycleScope.launch {
            passcodeGate.locked
                .filter { !it }
                .take(1)
                .collect {
                    pendingSharedUris?.let { uris ->
                        ResultEventBus.sendResult(
                            resultKey = NavigationResultKeys.SHARED_MEDIA_IMPORT,
                            result = uris
                        )
                        pendingSharedUris = null
                    }
                }
        }
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

        // Defense-in-depth: reject MIME types not explicitly supported
        val allowedMimePrefixes = setOf("image/", "audio/", "video/", "application/pdf")
        if (type != null && allowedMimePrefixes.none { type.startsWith(it) || type == it }) return

        if ((Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) && type != null) {
            val uris = mutableListOf<Uri>()

            if (Intent.ACTION_SEND == action) {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.add(it) }
            } else {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            }

            if (uris.isNotEmpty()) {
                if (passcodeGate.locked.value) {
                    // Hold URIs until the user authenticates
                    pendingSharedUris = uris
                } else {
                    ResultEventBus.sendResult(
                        resultKey = NavigationResultKeys.SHARED_MEDIA_IMPORT,
                        result = uris
                    )
                }
            }
        }
    }
}
