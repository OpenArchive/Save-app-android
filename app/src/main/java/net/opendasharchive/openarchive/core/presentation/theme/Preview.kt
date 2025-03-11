package net.opendasharchive.openarchive.core.presentation.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.ComposeAppBar

@Composable
fun DefaultScaffoldPreview(
    content: @Composable () -> Unit
) {

    SaveAppTheme {

        Scaffold(
            topBar = {
                ComposeAppBar()
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

@Composable
fun DefaultEmptyScaffoldPreview(
    content: @Composable () -> Unit
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

@Composable
fun DefaultBoxPreview(
    content: @Composable () -> Unit
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