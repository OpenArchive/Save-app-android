package net.opendasharchive.openarchive.features.main

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseComposeActivity
import net.opendasharchive.openarchive.features.main.ui.SaveNavGraph
import net.opendasharchive.openarchive.features.main.ui.rememberNavigator

class HomeActivity : BaseComposeActivity() {

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

    }
}