package net.opendasharchive.openarchive.features.spaces

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.db.dummySpaceList
import net.opendasharchive.openarchive.features.main.ui.components.SpaceIcon


@Composable
fun SpaceListScreen(
    viewModel: SpaceListViewModel,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // This will get called again when the screen resumes (see Fragment below)
    LaunchedEffect(Unit) {
        viewModel.onAction(SpaceListAction.RefreshSpaces)
    }


    SpaceListScreenContent(
        state = uiState,
        onAction = viewModel::onAction,
    )

}

@Composable
fun SpaceListScreenContent(
    state: SpaceListState,
    onAction: (SpaceListAction) -> Unit,
) {

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.spaceList.isEmpty()) {
            // Empty state with centered message
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lbl_no_servers),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        } else {
            // List state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                state.spaceList.forEach { space ->
                    SpaceListItem(
                        space = space,
                        onClick = {
                            onAction(SpaceListAction.NavigateToSpace(space.id, space.type))
                        }
                    )
                }
            }
        }

        // Add Server button at bottom center (visible in both states)
        Button(
            onClick = {
                onAction(SpaceListAction.AddNewSpace)
            },
            modifier = Modifier
                .heightIn(ThemeDimensions.touchable)
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.7f)
                .padding(bottom = 48.dp),
            shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                disabledContainerColor = colorResource(R.color.grey_50),
                disabledContentColor = colorResource(R.color.black),
                contentColor = colorResource(R.color.black)
            )
        ) {
            Text(
                text = "+ Add Server",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }


        /**
        Button(
        modifier = Modifier
        .padding(8.dp)
        .heightIn(ThemeDimensions.touchable)
        .weight(1f),
        enabled = !state.isBusy && state.isValid,
        shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
        colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.tertiary,
        disabledContainerColor = colorResource(R.color.grey_50),
        disabledContentColor = colorResource(R.color.black),
        ),
        onClick = {
        if (NetworkUtils.isNetworkAvailable(context)) {
        onAction(InternetArchiveLoginAction.Login)
        } else {
        Toast.makeText(context, R.string.error_no_internet, Toast.LENGTH_LONG)
        .show()
        }
        },
        ) {
        if (state.isBusy) {
        CircularProgressIndicator(color = ThemeColors.material.primary)
        } else {
        Text(
        stringResource(R.string.next),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
        }
        }
         **/
    }
}

@Composable
fun SpaceListItem(
    space: Vault,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SpaceIcon(
            type = space.type,
            modifier = Modifier.size(42.dp)
        )

        Column(
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = space.friendlyName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 1.sp
                )
            )

            Text(
                text = space.type.friendlyName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 1.sp
                )
            )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SpaceListScreenPreview() {

    DefaultScaffoldPreview {

        SpaceListScreenContent(
            state = SpaceListState(
                spaceList = dummySpaceList.map { it.toDomain() },
            ),
            onAction = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SpaceListEmptyScreenPreview() {

    DefaultScaffoldPreview {

        SpaceListScreenContent(
            state = SpaceListState(
                spaceList = emptyList(),
            ),
            onAction = {}
        )
    }
}