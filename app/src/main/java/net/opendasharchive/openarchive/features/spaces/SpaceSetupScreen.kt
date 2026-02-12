package net.opendasharchive.openarchive.features.spaces

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.services.snowbird.SnowbirdActivity

@Composable
fun SpaceSetupScreen(
    viewModel: SpaceSetupViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SpaceSetupContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
private fun SpaceSetupContent(
    state: SpaceSetupState,
    onAction: (SpaceSetupAction) -> Unit
) {
    // Use a scrollable Column to mimic ScrollView + LinearLayout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        // Header texts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.to_get_started_connect_to_a_server_to_store_your_media),
                style = MaterialTheme.typography.titleLarge.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            val description = if (state.isDwebEnabled) {
                stringResource(R.string.to_get_started_more_hint_dweb)
            } else {
                stringResource(R.string.to_get_started_more_hint)
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // WebDav option
        ServerOptionItem(
            iconRes = R.drawable.ic_private_server,
            title = stringResource(R.string.private_server),
            subtitle = stringResource(R.string.send_directly_to_a_private_server),
            onClick = { onAction(SpaceSetupAction.WebDavClicked) }
        )


        // Internet Archive option (conditionally visible)
        if (state.isInternetArchiveAllowed) {
            ServerOptionItem(
                iconRes = R.drawable.ic_internet_archive,
                title = stringResource(R.string.internet_archive),
                subtitle = stringResource(R.string.upload_to_the_internet_archive),
                onClick = { onAction(SpaceSetupAction.InternetArchiveClicked) }
            )
        }

        // Snowbird (Raven) option (conditionally visible)
        if (state.isDwebEnabled) {
            ServerOptionItem(
                iconRes = R.drawable.ic_dweb,
                title = stringResource(R.string.dweb_title),
                subtitle = stringResource(R.string.dweb_description),
                onClick = { onAction(SpaceSetupAction.DwebClicked) }
            )
        }
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SpaceSetupScreenPreview() {
    DefaultScaffoldPreview {
        SpaceSetupContent(
            state = SpaceSetupState(
                isInternetArchiveAllowed = true,
                isDwebEnabled = true
            ),
            onAction = {}
        )
    }
}
