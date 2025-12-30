package net.opendasharchive.openarchive.features.settings.passcode.passcode_setup

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.settings.passcode.AppHapticFeedbackType
import net.opendasharchive.openarchive.features.settings.passcode.HapticManager
import net.opendasharchive.openarchive.features.settings.passcode.components.MessageManager
import net.opendasharchive.openarchive.features.settings.passcode.components.NumericKeypad
import net.opendasharchive.openarchive.features.settings.passcode.components.PasscodeDots
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun PasscodeSetupScreen(
    viewModel: PasscodeSetupViewModel,
    hapticManager: HapticManager = koinInject()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val hapticFeedback = LocalHapticFeedback.current

    // Function to handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                PasscodeSetupUiEvent.PasscodeDoNotMatch -> {
                    hapticManager.perform(hapticFeedback, AppHapticFeedbackType.Error)
                    MessageManager.showMessage(UiText.Resource(R.string.passcode_do_not_match))
                }
            }
        }
    }


    PasscodeSetupScreenContent(
        state = uiState,
        onAction = viewModel::onAction,
        onHaptic = {
            hapticManager.perform(hapticFeedback, AppHapticFeedbackType.Error)
        }
    )
}

@Composable
private fun PasscodeSetupScreenContent(
    state: PasscodeSetupUiState,
    onAction: (PasscodeSetupUiAction) -> Unit,
    onHaptic: () -> Unit = {}
) {


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top section with logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (state.isConfirming) stringResource(R.string.confirm_passcode) else stringResource(R.string.set_passcode),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.set_passcode_warning),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colorResource(R.color.red_bg),
                    fontSize = 15.sp,               // overrides labelMedium’s size
                    fontWeight = FontWeight.Medium, // ensures consistent weight
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Passcode dots display
        PasscodeDots(
            passcodeLength = state.passcodeLength,
            currentPasscodeLength = state.passcode.length,
            shouldShake = state.shouldShake
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Middle section with prompt and passcode dots
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Custom numeric keypad
            NumericKeypad(
                isEnabled = !state.isProcessing,
                onNumberClick = { number ->
                    onAction(PasscodeSetupUiAction.OnNumberClick(number))
                },
                onDeleteClick = {
                    onAction(PasscodeSetupUiAction.OnBackspaceClick)
                },
                onSubmitClick = {
                    onAction(PasscodeSetupUiAction.OnSubmit)
                },
                onHaptic = onHaptic

            )

            Spacer(modifier = Modifier.height(64.dp))


//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceAround
//            ) {
//                TextButton(
//                    onClick = {
//                        onAction(PasscodeSetupUiAction.OnCancel)
//                    }
//                ) {
//                    Text(
//                        text = "Cancel",
//                        modifier = Modifier.padding(8.dp),
//                        style = TextStyle(
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold,
//                        ),
//                    )
//                }
//
//                TextButton(
//                    enabled = state.passcode.isNotEmpty(),
//                    onClick = {
//                        onAction(PasscodeSetupUiAction.OnBackspaceClick)
//                    }
//                ) {
//                    Text(
//                        text = "Delete",
//                        modifier = Modifier.padding(8.dp),
//                        style = TextStyle(
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold
//                        ),
//                    )
//                }
//
//
//            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PasscodeSetupScreenPreview() {
    DefaultScaffoldPreview {
        PasscodeSetupScreenContent(
            state = PasscodeSetupUiState(
                passcodeLength = 6
            ),
            onAction = {}
        )
    }
}