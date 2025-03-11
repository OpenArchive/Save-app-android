package net.opendasharchive.openarchive.features.settings.passcode.passcode_setup

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.features.settings.passcode.AppHapticFeedbackType
import net.opendasharchive.openarchive.features.settings.passcode.HapticManager
import net.opendasharchive.openarchive.features.settings.passcode.components.MessageManager
import net.opendasharchive.openarchive.features.settings.passcode.components.NumericKeypad
import net.opendasharchive.openarchive.features.settings.passcode.components.PasscodeDots
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun PasscodeSetupScreen(
    onPasscodeSet: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PasscodeSetupViewModel = koinViewModel(),
    hapticManager: HapticManager = koinInject()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        hapticManager.init(hapticFeedback)
    }

    // Function to handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                PasscodeSetupUiEvent.PasscodeSet -> onPasscodeSet()
                PasscodeSetupUiEvent.PasscodeDoNotMatch -> {
                    hapticManager.performHapticFeedback(AppHapticFeedbackType.Error)
                    MessageManager.showMessage("Passcodes do not match. Try againss.")
                }

                PasscodeSetupUiEvent.PasscodeCancelled -> onCancel()
            }
        }
    }


    PasscodeSetupScreenContent(
        state = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun PasscodeSetupScreenContent(
    state: PasscodeSetupUiState,
    onAction: (PasscodeSetupUiAction) -> Unit
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
                text = if (state.isConfirming) "Confirm Passcode" else "Set Passcode",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Make sure you remember this pin. If you forget it, you will need to reset the app, and all data will be erased.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelMedium
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
                }
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