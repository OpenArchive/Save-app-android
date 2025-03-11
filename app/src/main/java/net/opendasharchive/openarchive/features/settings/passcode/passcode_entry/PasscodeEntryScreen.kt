package net.opendasharchive.openarchive.features.settings.passcode.passcode_entry

import android.content.res.Configuration
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultEmptyScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.features.settings.passcode.AppHapticFeedbackType
import net.opendasharchive.openarchive.features.settings.passcode.HapticManager
import net.opendasharchive.openarchive.features.settings.passcode.components.MessageManager
import net.opendasharchive.openarchive.features.settings.passcode.components.NumericKeypad
import net.opendasharchive.openarchive.features.settings.passcode.components.PasscodeDots
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun PasscodeEntryScreen(
    onPasscodeSuccess: () -> Unit,
    onExit: () -> Unit,
    viewModel: PasscodeEntryViewModel = koinViewModel(),
    hapticManager: HapticManager = koinInject()
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()


    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        hapticManager.init(hapticFeedback)
    }

    // Function to handle passcode entry
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                PasscodeEntryUiEvent.Success -> onPasscodeSuccess()

                PasscodeEntryUiEvent.PasscodeNotSet -> {
                    MessageManager.showMessage("Passcode not set")
                }

                is PasscodeEntryUiEvent.IncorrectPasscode -> {
                    hapticManager.performHapticFeedback(AppHapticFeedbackType.Error)

                    event.remainingAttempts?.let {
                        val message = "Incorrect passcode. $it attempts remaining."
                        MessageManager.showMessage(message)
                    }

                }

                PasscodeEntryUiEvent.LockedOut -> {
                    MessageManager.showMessage("Too many failed attempts. App is locked.")
                    onExit()
                }
            }
        }
    }

    PasscodeEntryScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onExit = onExit,
    )
}


@Composable
fun PasscodeEntryScreenContent(
    state: PasscodeEntryScreenState,
    onAction: (PasscodeEntryScreenAction) -> Unit,
    onExit: () -> Unit,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.savelogo),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Middle section with prompt and passcode dots
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Enter Your Passcode", style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Passcode dots display
            PasscodeDots(
                passcodeLength = state.passcodeLength,
                currentPasscodeLength = state.passcode.length,
                shouldShake = state.shouldShake
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Custom numeric keypad
            NumericKeypad(
                isEnabled = !state.isProcessing,
                onNumberClick = { number ->
                    onAction(PasscodeEntryScreenAction.OnNumberClick(number))
                },
                onDeleteClick = {
                    onAction(PasscodeEntryScreenAction.OnBackspaceClick)
                },
                onSubmitClick = {

                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceAround
//            ) {
//                TextButton(
//                    onClick = {
//                        onExit()
//                    }
//                ) {
//                    Text(
//                        text = "Exit",
//                        modifier = Modifier.padding(8.dp),
//                        style = TextStyle(
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.onBackground
//                        ),
//                    )
//                }
//
//                TextButton(
//                    enabled = state.passcode.isNotEmpty(),
//                    onClick = {
//                        onAction(PasscodeEntryScreenAction.OnBackspaceClick)
//                    }
//                ) {
//                    Text(
//                        text = "Delete",
//                        modifier = Modifier.padding(8.dp),
//                        style = TextStyle(
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.onBackground
//                        ),
//                    )
//                }
//            }
        }
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun PasscodeEntryScreenPreview() {

    DefaultEmptyScaffoldPreview {

        PasscodeEntryScreenContent(
            state = PasscodeEntryScreenState(
                passcodeLength = 6
            ),
            onAction = {},
            onExit = {},
        )

    }
}