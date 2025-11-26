package net.opendasharchive.openarchive.core.presentation.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.core.di.passcodeModule
import net.opendasharchive.openarchive.features.core.ComposeAppBar
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplicationPreview

@Composable
fun DefaultScaffoldPreview(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    KoinApplicationPreview(
        application = {
            androidContext(context)
            modules(passcodeModule)
        }
    ) {
        SaveAppTheme {

            Scaffold(
                topBar = {
                    ComposeAppBar(
                        title = "Save App"
                    )
                }
            ) { paddingValues ->

                Box(
                    modifier = Modifier.Companion.padding(paddingValues),
                    contentAlignment = Alignment.Companion.Center
                ) {
                    content()
                }
            }
        }
    }

}

@Composable
fun DefaultEmptyScaffoldPreview(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    KoinApplicationPreview(
        application = {
            androidContext(context)
            modules(passcodeModule)
        }
    ) {
        SaveAppTheme {

            Scaffold { paddingValues ->

                Box(
                    modifier = Modifier.Companion.padding(paddingValues),
                    contentAlignment = Alignment.Companion.Center
                ) {
                    content()
                }
            }
        }
    }

}

@Composable
fun DefaultBoxPreview(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    KoinApplicationPreview(
        application = {
            androidContext(context)
            modules(passcodeModule)
        }
    ) {
        SaveAppTheme {
            Surface(
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }


        }
    }
}