package net.opendasharchive.openarchive.features.internetarchive.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.core.state.Dispatch
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.IAResult
import net.opendasharchive.openarchive.features.internetarchive.presentation.details.InternetArchiveDetailsViewModel.Action
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.CustomTextField
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InternetArchiveDetailsScreen(space: Space, onResult: (IAResult) -> Unit) {
    val viewModel: InternetArchiveDetailsViewModel = koinViewModel {
        parametersOf(space)
    }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.actions.collect { action ->
            when (action) {
                is Action.Remove -> onResult(IAResult.Deleted)
                is Action.Cancel -> onResult(IAResult.Cancelled)
                else -> Unit
            }
        }
    }

    InternetArchiveDetailsContent(state, viewModel::dispatch)
}

@Composable
private fun InternetArchiveDetailsContent(
    state: InternetArchiveDetailsState,
    dispatch: Dispatch<Action>,
    dialogManager: DialogStateManager = koinViewModel()
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Column {

            //InternetArchiveHeader()

            Spacer(Modifier.height(ThemeDimensions.spacing.large))

            CustomTextField(
                label = stringResource(R.string.label_username),
                value = state.userName,
                onValueChange = {},
                enabled = false,
            )

            Spacer(Modifier.height(ThemeDimensions.spacing.medium))

            CustomTextField(
                label = stringResource(R.string.label_screen_name),
                value = state.screenName,
                onValueChange = {},
                enabled = false,
            )

            Spacer(Modifier.height(ThemeDimensions.spacing.medium))


            CustomTextField(
                label = stringResource(R.string.label_email),
                value = state.email,
                onValueChange = {},
                enabled = false,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        //isRemoving = true

                        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                            title = UiText.StringResource(R.string.remove_from_app)
                            message = UiText.StringResource(R.string.are_you_sure_you_want_to_remove_this_server_from_the_app)
                            icon = UiImage.DrawableResource(R.drawable.ic_trash)
                            destructiveButton {
                                text = UiText.StringResource(R.string.remove)
                                action = {
                                    dispatch(Action.Remove)
                                }
                            }

                            neutralButton {
                                text = UiText.StringResource(R.string.action_cancel)
                                action = {
                                    //dismiss
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        stringResource(id = R.string.remove_from_app),
                        fontSize = 18.sp
                    )
                }
            }


        }


    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
private fun InternetArchiveScreenPreview() {
    DefaultScaffoldPreview {
        InternetArchiveDetailsContent(
            state = InternetArchiveDetailsState(
                email = "abc@example.com",
                userName = "@abc_name",
                screenName = "ABC Name"
            ),
            dispatch = {}
        )
    }
}

