package net.opendasharchive.openarchive.services.snowbird.presentation.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.components.LoadingOverlay
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLight
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.CustomTextField


@Composable
fun SnowbirdJoinGroupScreen(
    viewModel: SnowbirdJoinGroupViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SnowbirdJoinGroupScreenContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun SnowbirdJoinGroupScreenContent(
    state: SnowbirdJoinGroupState,
    onAction: (SnowbirdJoinGroupAction) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val repoFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 100.dp)
        ) {
            // Header section (similar to WebDavHeader)
            DwebHeader(
                modifier = Modifier
                    .padding(top = 48.dp, bottom = 24.dp)
                    .padding(end = 24.dp)
            )

            // Group Name field (Read-only extracted from URI)
            CustomTextField(
                value = state.groupName,
                onValueChange = {},
                enabled = false,
                placeholder = stringResource(R.string.dweb_join_group_group_name),
                isLoading = state.isLoading,
                imeAction = ImeAction.Next,
                onImeAction = { repoFocusRequester.requestFocus() }
            )

            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.medium))

            // Repository Name field
            CustomTextField(
                value = state.repoName,
                onValueChange = { onAction(SnowbirdJoinGroupAction.UpdateRepoName(it)) },
                placeholder = stringResource(R.string.dweb_join_group_repo_name),
                isLoading = state.isLoading,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.dweb_join_group_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action Buttons at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            TextButton(
                modifier = Modifier
                    .heightIn(ThemeDimensions.touchable)
                    .weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(R.color.colorOnBackground)
                ),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                onClick = { onAction(SnowbirdJoinGroupAction.Cancel) }
            ) {
                Text(
                    stringResource(R.string.back),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Join Group button
            Button(
                modifier = Modifier
                    .heightIn(ThemeDimensions.touchable)
                    .weight(1f),
                enabled = !state.isLoading && state.isFormValid,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    disabledContainerColor = colorResource(R.color.grey_50),
                    disabledContentColor = colorResource(R.color.black),
                    contentColor = colorResource(R.color.black)
                ),
                onClick = { onAction(SnowbirdJoinGroupAction.Authenticate) }
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                } else {
                    Text(
                        stringResource(R.string.action_next),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        // Loading Overlay
        if (state.isLoading) {
            LoadingOverlay()
        }
    }
}

@Composable
private fun DwebHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(colorResource(R.color.colorBackgroundSpaceIcon))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(32.dp),
                painter = painterResource(id = R.drawable.ic_dweb),
                contentDescription = null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colorResource(R.color.colorTertiary))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(R.string.dweb_join_group_screen_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 32.dp)
        )
    }
}

@PreviewLight
@Composable
private fun SnowbirdJoinGroupScreenPreview() {
    SaveAppTheme {
        SnowbirdJoinGroupScreenContent(
            state = SnowbirdJoinGroupState(
                groupName = "Test Group",
                repoName = "",
                scannedUri = "save-veilid://join?name=TestGroup"
            ),
            onAction = {}
        )
    }
}
