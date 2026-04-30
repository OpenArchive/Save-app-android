package net.opendasharchive.openarchive.services.snowbird.presentation.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.ArchivePermission
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLight
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLightDark
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles


@Composable
fun SnowbirdRepoListScreen(
    viewModel: SnowbirdRepoViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
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
        repo.permissions?.let { permission ->
            Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                RepoPermissionBadge(permission = permission)
            }
        } ?: Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_dweb),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = repo.description ?: "N/A",
                style = SaveTextStyles.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun RepoPermissionBadge(
    permission: ArchivePermission,
    modifier: Modifier = Modifier
) {

    val bgColor = colorResource(id = R.color.c23_teal)

    val label = when (permission) {
        ArchivePermission.READ_ONLY -> "RO"
        ArchivePermission.READ_WRITE -> "RW"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .size(width = 28.dp, height = 21.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
    }
}


@PreviewLightDark
@Composable
private fun SnowbirdRepoListScreenPreview() {
    SaveAppTheme {
        SnowbirdRepoListContent(
            state = SnowbirdRepoState(
                repos = listOf(
                    Archive(
                        description = "Main Repository",
                        archiveKey = "key1",
                        permissions = ArchivePermission.READ_ONLY
                    ),
                    Archive(
                        description = "Backup Repository",
                        archiveKey = "key2",
                        permissions = ArchivePermission.READ_WRITE
                    ),
                    Archive(
                        description = "Shared Repository",
                        archiveKey = "key3",
                        permissions = null
                    )
                )
            ),
            onAction = {}
        )
    }
}

@PreviewLight
@Composable
private fun SnowbirdRepoListScreenEmptyPreview() {
    DefaultScaffoldPreview {
        SnowbirdRepoListContent(
            state = SnowbirdRepoState(repos = emptyList()),
            onAction = {}
        )
    }
}

@PreviewLight
@Composable
private fun SnowbirdRepoListScreenLoadingPreview() {
    DefaultScaffoldPreview {
        SnowbirdRepoListContent(
            state = SnowbirdRepoState(repos = emptyList(), isLoading = true),
            onAction = {}
        )
    }
}
