package net.opendasharchive.openarchive.features.main

import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.BaseComposeActivity
import net.opendasharchive.openarchive.features.main.ui.SaveNavGraph
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import org.koin.android.ext.android.inject

class HomeActivity: BaseComposeActivity() {

    private val appConfig by inject<AppConfig>()

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

        // Set system bars to be edge-to-edge
//        window.decorView.systemUiVisibility = (
//            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//        )

        // Set navigation bar color to match bottom bar color
        //window.navigationBarColor = ContextCompat.getColor(this, R.color.colorTertiary)



        // Set up your Compose UI and pass callbacks.
        setContent {
            SaveNavGraph(
                dialogManager
            )
        }
    }

}
