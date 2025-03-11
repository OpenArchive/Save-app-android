package net.opendasharchive.openarchive.features.spaces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview

@Composable
fun SpaceSetupScreen(
    onWebDavClick: () -> Unit,
    isInternetArchiveAllowed: Boolean,
    onInternetArchiveClick: () -> Unit,
    isDwebEnabled: Boolean,
    onDwebClicked: () -> Unit
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
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            val description = if (isDwebEnabled) stringResource(R.string.to_get_started_more_hint_dweb) else stringResource(R.string.to_get_started_more_hint)
            Text(
                text = description,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // WebDav option
        ServerOptionItem(
            iconRes = R.drawable.ic_private_server,
            title = stringResource(R.string.private_server),
            subtitle = stringResource(R.string.send_directly_to_a_private_server),
            onClick = onWebDavClick
        )


        // Internet Archive option (conditionally visible)
        if (isInternetArchiveAllowed) {
            ServerOptionItem(
                iconRes = R.drawable.ic_internet_archive,
                title = stringResource(R.string.internet_archive),
                subtitle = stringResource(R.string.upload_to_the_internet_archive),
                onClick = onInternetArchiveClick
            )
        }

        // Snowbird (Raven) option (conditionally visible)
        if (isDwebEnabled) {
            ServerOptionItem(
                iconRes = R.drawable.ic_dweb,
                title = stringResource(R.string.dweb_title),
                subtitle = stringResource(R.string.dweb_description),
                onClick = onDwebClicked
            )
        }
    }
}

@Preview
@Composable
private fun SpaceSetupScreenPreview() {
    DefaultScaffoldPreview {
        SpaceSetupScreen(
            onWebDavClick = {},
            isInternetArchiveAllowed = true,
            onInternetArchiveClick = {},
            isDwebEnabled = true,
            onDwebClicked = {},
        )
    }
}
