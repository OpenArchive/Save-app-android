package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.components.PrimaryButton
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import org.koin.androidx.compose.koinViewModel

@Composable
fun SpaceSetupSuccessScreen(
    onNavigateToMain: () -> Unit = {},
    viewModel: SpaceSetupSuccessViewModel ,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SpaceSetupSuccessEvent.NavigateToMain -> {
                    onNavigateToMain()
                }
            }
        }
    }

    SpaceSetupSuccessContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun SpaceSetupSuccessContent(
    state: SpaceSetupSuccessState,
    onAction: (SpaceSetupSuccessAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success message at top
            Text(
                text = state.message.asString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 48.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Center illustration - takes available space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .weight(2f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hands_mobile_updated),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }

            // Spacer for button height + padding + navigation bar
            Spacer(modifier = Modifier.weight(1f))
        }

        // Button bar at bottom - overlaid on top
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            PrimaryButton(
                text = stringResource(R.string.action_done),
                onClick = { onAction(SpaceSetupSuccessAction.Done) },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Space Setup Success - Light")
@Preview(
    showBackground = true,
    name = "Space Setup Success - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun SpaceSetupSuccessWebDavPreview() {
    SaveAppTheme {
        SpaceSetupSuccessContent(
            state = SpaceSetupSuccessState(
                message = UiText.StringResource(R.string.you_have_successfully_connected_to_a_private_server),
                spaceType = Space.Type.WEBDAV
            ),
            onAction = {}
        )
    }
}

class SpaceSetupSuccessViewModel(
    navArgs: AppRoute.SpaceSetupSuccessRoute
) : ViewModel() {



    private val _uiState = MutableStateFlow(
        SpaceSetupSuccessState(
            spaceType = navArgs.spaceType,
            message = when(navArgs.spaceType) {
                Space.Type.WEBDAV -> UiText.StringResource(R.string.you_have_successfully_connected_to_a_private_server)
                Space.Type.INTERNET_ARCHIVE -> UiText.StringResource(R.string.you_have_successfully_connected_to_a_private_server)
                Space.Type.RAVEN -> UiText.StringResource(R.string.you_have_successfully_connected_to_a_private_server)
            },
        )
    )
    val uiState: StateFlow<SpaceSetupSuccessState> = _uiState.asStateFlow()

    private val _events = Channel<SpaceSetupSuccessEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: SpaceSetupSuccessAction) {
        when (action) {
            is SpaceSetupSuccessAction.Done -> {
                viewModelScope.launch {
                    _events.send(SpaceSetupSuccessEvent.NavigateToMain)
                }
            }
        }
    }
}

data class SpaceSetupSuccessState(
    val message: UiText,
    val spaceType: Space.Type,
)

sealed interface SpaceSetupSuccessAction {
    data object Done : SpaceSetupSuccessAction
}

sealed interface SpaceSetupSuccessEvent {
    data object NavigateToMain : SpaceSetupSuccessEvent
}
