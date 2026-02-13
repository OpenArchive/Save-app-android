package net.opendasharchive.openarchive.services.snowbird.presentation.group

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLight
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLightDark
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles


@Composable
fun SnowbirdGroupListScreen(
    viewModel: SnowbirdGroupListViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SnowbirdGroupListContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SnowbirdGroupListContent(
    state: SnowbirdGroupListState,
    onAction: (SnowbirdGroupListAction) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { onAction(SnowbirdGroupListAction.RefreshGroups) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.groups.isEmpty() && !state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "No groups found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.groups) { group ->
                    SnowbirdGroupItem(
                        group = group,
                        onClick = { onAction(SnowbirdGroupListAction.SelectGroup(group)) },
                        onLongClick = { onAction(SnowbirdGroupListAction.ShareGroup(group)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SnowbirdGroupItem(
    group: Vault,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_dweb),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = group.name,
                style = SaveTextStyles.titleMedium
            )
            if (group.host.isNotBlank()) {
                Text(
                    text = group.host,
                    style = SaveTextStyles.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right_ios),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@PreviewLightDark
@Composable
private fun SnowbirdGroupListScreenPreview() {
    SaveAppTheme {
        SnowbirdGroupListContent(
            state = SnowbirdGroupListState(
                groups = listOf(
                    Vault(name = "Personal Group", host = "veilid://host1", type = VaultType.DWEB_STORAGE),
                    Vault(name = "Work Group", host = "veilid://host2", type = VaultType.DWEB_STORAGE),
                    Vault(name = "Research Group", host = "veilid://host3", type = VaultType.DWEB_STORAGE)
                )
            ),
            onAction = {}
        )
    }
}

@PreviewLight
@Composable
private fun SnowbirdGroupListScreenEmptyPreview() {
    DefaultScaffoldPreview {
        SnowbirdGroupListContent(
            state = SnowbirdGroupListState(groups = emptyList()),
            onAction = {}
        )
    }
}

@PreviewLight
@Composable
private fun SnowbirdGroupListScreenLoadingPreview() {
    DefaultScaffoldPreview {
        SnowbirdGroupListContent(
            state = SnowbirdGroupListState(groups = emptyList(), isLoading = true),
            onAction = {}
        )
    }
}
