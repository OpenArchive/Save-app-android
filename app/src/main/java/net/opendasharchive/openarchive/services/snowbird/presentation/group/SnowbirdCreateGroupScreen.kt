package net.opendasharchive.openarchive.services.snowbird.presentation.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.CustomTextField
import org.koin.androidx.compose.koinViewModel

@Composable
fun SnowbirdCreateGroupScreen(
    viewModel: SnowbirdCreateGroupViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SnowbirdCreateGroupScreenContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun SnowbirdCreateGroupScreenContent(
    state: SnowbirdCreateGroupState,
    onAction: (SnowbirdCreateGroupAction) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val repoFocusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = stringResource(R.string.dweb_create_group_screen_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            CustomTextField(
                value = state.groupName,
                onValueChange = { onAction(SnowbirdCreateGroupAction.UpdateGroupName(it)) },
                placeholder = stringResource(R.string.dweb_create_group_group_name),
                isLoading = state.isLoading,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                onImeAction = { repoFocusRequester.requestFocus() }
            )

            Spacer(modifier = Modifier.height(30.dp))

            CustomTextField(
                value = state.repoName,
                onValueChange = { onAction(SnowbirdCreateGroupAction.UpdateRepoName(it)) },
                placeholder = stringResource(R.string.dweb_create_group_user_name),
                isLoading = state.isLoading,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() },
                modifier = Modifier.focusRequester(repoFocusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.dweb_create_group_screen_description),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Button bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                modifier = Modifier
                    .heightIn(ThemeDimensions.touchable)
                    .weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(R.color.colorOnBackground)
                ),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                onClick = { onAction(SnowbirdCreateGroupAction.Cancel) }
            ) {
                Text(
                    stringResource(R.string.back),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Button(
                modifier = Modifier
                    .heightIn(ThemeDimensions.touchable)
                    .weight(1f),
                enabled = !state.isLoading && state.groupName.isNotBlank() && state.repoName.isNotBlank(),
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    disabledContainerColor = colorResource(R.color.grey_50),
                    disabledContentColor = colorResource(R.color.black),
                    contentColor = colorResource(R.color.black)
                ),
                onClick = { onAction(SnowbirdCreateGroupAction.CreateGroup) }
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                } else {
                    Text(
                        stringResource(R.string.next),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SnowbirdCreateGroupScreenPreview() {
    SaveAppTheme {
        SnowbirdCreateGroupScreenContent(
            state = SnowbirdCreateGroupState(),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnowbirdCreateGroupScreenLoadingPreview() {
    DefaultScaffoldPreview {
        SnowbirdCreateGroupScreenContent(
            state = SnowbirdCreateGroupState(isLoading = true),
            onAction = {}
        )
    }
}
