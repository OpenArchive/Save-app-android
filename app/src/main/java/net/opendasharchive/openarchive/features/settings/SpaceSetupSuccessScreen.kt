package net.opendasharchive.openarchive.features.settings

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.presentation.components.PrimaryButton
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asString
import net.opendasharchive.openarchive.features.main.ui.AppRoute
import net.opendasharchive.openarchive.features.main.ui.Navigator

@Composable
fun SpaceSetupSuccessScreen(
    viewModel: SpaceSetupSuccessViewModel ,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SpaceSetupSuccessEvent.SendResultBack -> {
                    onNavigateBack()
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
                message = UiText.Resource(R.string.you_have_successfully_connected_to_a_private_server),
                spaceType = VaultType.PRIVATE_SERVER
            ),
            onAction = {}
        )
    }
}

class SpaceSetupSuccessViewModel(
    route: AppRoute.SpaceSetupSuccessRoute,
    private val navigator: Navigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SpaceSetupSuccessState(
            spaceType = route.spaceType,
            message = when(route.spaceType) {
                VaultType.PRIVATE_SERVER -> UiText.Resource(R.string.you_have_successfully_connected_to_a_private_server)
                VaultType.INTERNET_ARCHIVE -> UiText.Resource(R.string.you_have_successfully_connected_to_the_internet_archive)
                VaultType.DWEB_STORAGE -> UiText.Resource(R.string.you_have_successfully_created_dweb)
                VaultType.STORACHA -> UiText.Resource(R.string.you_have_successfully_connected_to_a_private_server)
            },
        )
    )
    val uiState: StateFlow<SpaceSetupSuccessState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<SpaceSetupSuccessEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onAction(action: SpaceSetupSuccessAction) {
        when (action) {
            is SpaceSetupSuccessAction.Done -> onDone()
        }
    }

    private fun onDone() = viewModelScope.launch {

        //TODO: Navigate back with result
        _uiEvent.send(SpaceSetupSuccessEvent.SendResultBack)

        navigator.navigateAndClear(AppRoute.HomeRoute)
        if (uiState.value.spaceType == VaultType.DWEB_STORAGE) {
            navigator.navigateTo(AppRoute.SnowbirdDashboardRoute)
        }
    }
}

data class SpaceSetupSuccessState(
    val message: UiText,
    val spaceType: VaultType,
)

sealed interface SpaceSetupSuccessAction {
    data object Done : SpaceSetupSuccessAction
}

sealed interface SpaceSetupSuccessEvent {
    data object SendResultBack : SpaceSetupSuccessEvent
}
