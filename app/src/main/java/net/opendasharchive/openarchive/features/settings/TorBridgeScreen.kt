package net.opendasharchive.openarchive.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.ComposeAppBar
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import net.opendasharchive.openarchive.util.Prefs

@Composable
fun TorBridgeScreen(
    onNavigateBack: () -> Unit
) {
    SaveAppTheme {
        DefaultScaffold(
            topAppBar = {
                ComposeAppBar(
                    title = stringResource(R.string.tor_bridges_title),
                    onNavigationAction = {
                        onNavigateBack()
                    }
                )
            },
        ) {
            TorBridgeScreenContent()
        }
    }
}

@Composable
fun TorBridgeScreenContent() {
    var useBridges by remember {
        mutableStateOf(Prefs.useBridges)
    }

    var selectedBridgeType by remember {
        mutableStateOf(Prefs.bridgeType)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Use Bridges Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.use_bridges),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.use_bridges_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useBridges,
                    onCheckedChange = { enabled ->
                        useBridges = enabled
                        Prefs.useBridges = enabled
                    }
                )
            }
        }

        // Description
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.bridges_description),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bridge Selection (only visible when bridges are enabled)
        item {
            AnimatedVisibility(
                visible = useBridges,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = null
                                )
                                Text(
                                    text = stringResource(R.string.bridge_selection),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // obfs4 option
                            BridgeTypeOption(
                                title = stringResource(R.string.bridge_type_obfs4),
                                description = stringResource(R.string.bridge_type_obfs4_description),
                                isSelected = selectedBridgeType == "obfs4",
                                onClick = {
                                    selectedBridgeType = "obfs4"
                                    Prefs.bridgeType = "obfs4"
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Snowflake option
                            BridgeTypeOption(
                                title = stringResource(R.string.bridge_type_snowflake),
                                description = stringResource(R.string.bridge_type_snowflake_description),
                                isSelected = selectedBridgeType == "snowflake",
                                onClick = {
                                    selectedBridgeType = "snowflake"
                                    Prefs.bridgeType = "snowflake"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BridgeTypeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun TorBridgeScreenPreview() {
    DefaultScaffoldPreview {
        TorBridgeScreenContent()
    }
}
