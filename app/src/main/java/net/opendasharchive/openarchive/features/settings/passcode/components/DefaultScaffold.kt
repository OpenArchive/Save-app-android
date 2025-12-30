package net.opendasharchive.openarchive.features.settings.passcode.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import net.opendasharchive.openarchive.features.core.ComposeAppBar
import net.opendasharchive.openarchive.features.core.UiText

object MessageManager {
    private val _messageChannel = Channel<UiText>(Channel.BUFFERED)
    val messageFlow = _messageChannel.receiveAsFlow()

    suspend fun showMessage(message: UiText) {
        _messageChannel.send(message)
    }
}

@Composable
fun DefaultScaffold(
    title: String,
    onNavigateBack: () -> Unit = {},
    showNavigationIcon: Boolean = true,
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable () -> Unit,
) {

    DefaultScaffold(
        topAppBar = {
            ComposeAppBar(
                title = title,
                actions = actions,
                onNavigateBack = onNavigateBack,
                showNavigationIcon = showNavigationIcon
            )
        },
        content = content
    )
}

@Composable
fun DefaultScaffold(
    topAppBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        MessageManager.messageFlow.collectLatest { message ->
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            topAppBar?.invoke()
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                content()
            }
        }
    )
}
