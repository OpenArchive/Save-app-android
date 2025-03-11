package net.opendasharchive.openarchive.features.internetarchive.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.dialog.DialogHost
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.IAResult
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.getSpace
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.ComposeAppBar
import net.opendasharchive.openarchive.features.main.MainActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

@Deprecated("use jetpack compose")
class InternetArchiveActivity : AppCompatActivity() {

    val dialogManager: DialogStateManager by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val (space, isNewSpace) = intent.extras.getSpace(Space.Type.INTERNET_ARCHIVE)

        setContent {

            SaveAppTheme {

                DialogHost(dialogManager)

                Scaffold(
                    topBar = {
                        ComposeAppBar(
                            title = if (isNewSpace) "Internet Archive" else "Internet Archive",
                            onNavigationAction = { onComplete(IAResult.Cancelled) }
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                        InternetArchiveScreen(space, isNewSpace) {
                            onComplete(it)
                        }
                    }
                }
            }


        }
    }

    private fun onComplete(result: IAResult) {
        when (result) {
            IAResult.Saved -> {
                startActivity(Intent(this, MainActivity::class.java))
                // measureNewBackend(Space.Type.INTERNET_ARCHIVE)
            }

            IAResult.Deleted -> Space.navigate(this)
            IAResult.Cancelled -> onBackPressed()
        }
    }
}

//fun Activity.measureNewBackend(type: Space.Type) {
//    CleanInsightsManager.getConsent(this) {
//        CleanInsightsManager.measureEvent(
//            "backend",
//            "new",
//            type.friendlyName
//        )
//    }
//}
