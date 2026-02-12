package net.opendasharchive.openarchive.services.snowbird.presentation.repo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import org.koin.androidx.compose.koinViewModel

@Composable
fun SnowbirdRepoListScreen(
    vaultId: Long,
    groupKey: String,
    viewModel: SnowbirdRepoViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vaultId) {
        viewModel.onAction(SnowbirdRepoAction.Init(vaultId, groupKey))
    }

    SnowbirdRepoListContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnowbirdRepoListContent(
    state: SnowbirdRepoState,
    onAction: (SnowbirdRepoAction) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { onAction(SnowbirdRepoAction.RefreshRepos) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.repos.isEmpty() && !state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "No repositories found")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onAction(SnowbirdRepoAction.RefreshGroupContent) }) {
                    Text("Refresh Content")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.repos) { repo ->
                    SnowbirdRepoItem(
                        repo = repo,
                        onClick = { onAction(SnowbirdRepoAction.SelectRepo(repo)) }
                    )
                }
            }
        }
    }
}

@Composable
fun SnowbirdRepoItem(
    repo: Archive,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                text = repo.description ?: "N/A",
                style = SaveTextStyles.titleMedium
            )
            if (!repo.archiveKey.isNullOrBlank()) {
                Text(
                    text = "Key: ${repo.archiveKey}",
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SnowbirdRepoListScreenPreview() {
    SaveAppTheme {
        SnowbirdRepoListContent(
            state = SnowbirdRepoState(
                repos = listOf(
                    Archive(description = "Main Repository", archiveKey = "key1"),
                    Archive(description = "Backup Repository", archiveKey = "key2"),
                    Archive(description = "Shared Repository", archiveKey = "key3")
                )
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnowbirdRepoListScreenEmptyPreview() {
    DefaultScaffoldPreview {
        SnowbirdRepoListContent(
            state = SnowbirdRepoState(repos = emptyList()),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnowbirdRepoListScreenLoadingPreview() {
    DefaultScaffoldPreview {
        SnowbirdRepoListContent(
            state = SnowbirdRepoState(repos = emptyList(), isLoading = true),
            onAction = {}
        )
    }
}
