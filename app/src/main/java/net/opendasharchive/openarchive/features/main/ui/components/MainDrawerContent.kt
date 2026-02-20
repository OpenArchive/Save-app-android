package net.opendasharchive.openarchive.features.main.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Archive
import net.opendasharchive.openarchive.core.domain.Vault
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.domain.mappers.toDomain
import net.opendasharchive.openarchive.core.presentation.theme.DefaultBoxPreview
import net.opendasharchive.openarchive.db.sugar.dummyProjectList
import net.opendasharchive.openarchive.db.sugar.dummySpaceList
import net.opendasharchive.openarchive.features.core.Accordion
import net.opendasharchive.openarchive.features.core.AccordionState
import net.opendasharchive.openarchive.features.core.rememberAccordionState

@Composable
fun MainDrawerContent(
    selectedSpace: Vault? = null,
    spaceList: List<Vault> = emptyList(),
    projects: List<Archive> = emptyList(),
    selectedProject: Archive? = null,
    onProjectSelected: (Archive) -> Unit = {},
    onAddNewFolderClicked: () -> Unit = {},
    onSpaceSelected: (Long) -> Unit,
    onAddNewSpaceClicked: () -> Unit,
) {

    val serverAccordionState = rememberAccordionState()


    ModalDrawerSheet(
        drawerShape = DrawerDefaults.shape,
        drawerContainerColor = colorResource(R.color.colorNavigationDrawerBackground)
    ) {
        // Main drawer content
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            // AppBar height spacer
            Spacer(modifier = Modifier.height(56.dp))

            ExpandableSpaceList(
                serverAccordionState = serverAccordionState,
                selectedSpace = selectedSpace,
                spaceList = spaceList,
                onSpaceSelected = { selectedSpace ->
                    serverAccordionState.collapse()
                    onSpaceSelected(selectedSpace.id)
                },
                onAddAnotherAccountClicked = onAddNewSpaceClicked
            )

            AnimatedVisibility(
                visible = serverAccordionState.expanded.not()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // Divider
                    HorizontalDivider(
                        color = colorResource(R.color.c23_grey),
                        thickness = 0.5.dp,
                        modifier = Modifier
                            .padding(horizontal = 0.dp, vertical = 10.dp)
                            .alpha(if (serverAccordionState.expanded) 0.3f else 0.5f)
                    )

                    // Current Space name and icon (always visible, dimmed when expanded)
                    selectedSpace?.let { space ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .alpha(if (serverAccordionState.expanded) 0.3f else 1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SpaceIcon(
                                type = space.type,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = space.friendlyName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 16.sp
                            )
                        }
                    }


                    // Folder list (dimmed when space list is expanded)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 8.dp)
                            .alpha(if (serverAccordionState.expanded) 0.3f else 1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        projects.forEach { project ->
                            FolderItem(
                                project = project,
                                isSelected = project.id == selectedProject?.id,
                                onProjectSelected = onProjectSelected
                            )
                        }
                    }

                    // Add Folder button at bottom (dimmed when space list is expanded)
                    Button(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            .align(Alignment.CenterHorizontally)
                            .alpha(if (serverAccordionState.expanded) 0.3f else 1f),
                        shape = RoundedCornerShape(8.dp),
                        onClick = onAddNewFolderClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.colorTertiary)
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = colorResource(R.color.colorOnBackground),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.new_folder),
                            fontSize = 16.sp,
                            color = colorResource(R.color.colorOnBackground)
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun SpaceListItem(
    space: Vault,
    isSelected: Boolean,
    onSpaceSelected: (Vault) -> Unit
) {
    val backgroundColor =
        if (isSelected) colorResource(R.color.colorTertiary) else colorResource(R.color.colorDrawerSpaceListBackground)
    val textColor =
        if (isSelected) colorResource(R.color.colorOnBackground) else colorResource(R.color.colorText)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onSpaceSelected(space) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpaceIcon(
            type = space.type,
            modifier = Modifier.size(24.dp),
            tint = colorResource(R.color.colorOnBackground)
        )
        Text(
            text = space.friendlyName,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}


@Composable
private fun FolderItem(
    project: Archive,
    isSelected: Boolean,
    onProjectSelected: (Archive) -> Unit
) {
    val iconRes =
        if (isSelected) R.drawable.baseline_folder_white_24 else R.drawable.outline_folder_white_24
    val iconColor =
        if (isSelected) colorResource(R.color.colorTertiary) else colorResource(R.color.colorOnBackground)
    val textColor =
        if (isSelected) colorResource(R.color.colorOnBackground) else colorResource(R.color.colorText)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectSelected(project) }
            .padding(horizontal = 32.dp, vertical = 16.dp), // Increased left padding to 32dp
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(R.string.folder_name),
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = project.description ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

@Preview
@Composable
private fun MainDrawerContentPreview() {
    DefaultBoxPreview {
        MainDrawerContent(
            spaceList = dummySpaceList.map { it.toDomain() },
            selectedSpace = dummySpaceList.first().toDomain(),
            projects = dummyProjectList.map { it.toDomain() },
            selectedProject = dummyProjectList.first().toDomain(),
            onProjectSelected = {},
            onAddNewFolderClicked = {},
            onSpaceSelected = {},
            onAddNewSpaceClicked = {}
        )
    }
}

@Composable
fun ExpandableSpaceList(
    serverAccordionState: AccordionState,
    selectedSpace: Vault? = null,
    spaceList: List<Vault>,
    onSpaceSelected: (Vault) -> Unit,
    onAddAnotherAccountClicked: () -> Unit,
) {
    Accordion(
        state = serverAccordionState,
        headerContent = {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {


                Text(
                    text = stringResource(R.string.servers),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )



                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (serverAccordionState.expanded) 180f else 0f)
                )

            }
        },
        bodyContent = {

            Column(
                modifier = Modifier.background(colorResource(R.color.colorDrawerSpaceListBackground)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spaceList.forEach { space ->
                    DrawerSpaceListItem(
                        space = space,
                        isSelected = space.id == selectedSpace?.id,
                        onSpaceSelected = onSpaceSelected
                    )
                }

                AddAnotherAccountItem {
                    onAddAnotherAccountClicked()
                }
            }

        }
    )
}

@Composable
private fun DrawerSpaceListItem(
    space: Vault,
    isSelected: Boolean,
    onSpaceSelected: (Vault) -> Unit
) {
    val backgroundColor =
        if (isSelected) colorResource(R.color.colorTertiary) else colorResource(R.color.colorDrawerSpaceListBackground)
    val textColor =
        if (isSelected) colorResource(R.color.colorOnBackground) else colorResource(R.color.colorText)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onSpaceSelected(space) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpaceIcon(
            type = space.type,
            modifier = Modifier.size(24.dp),
            tint = colorResource(R.color.colorOnBackground)
        )
        Text(
            text = space.friendlyName,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

@Composable
private fun AddAnotherAccountItem(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.colorDrawerSpaceListBackground))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = colorResource(R.color.colorTertiary),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(R.string.add_another_account),
            style = MaterialTheme.typography.bodyLarge,
            color = colorResource(R.color.colorTertiary)
        )
    }
}

@Composable
fun SpaceIcon(
    type: VaultType,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val icon = when (type) {
        VaultType.PRIVATE_SERVER -> painterResource(R.drawable.ic_space_private_server)
        VaultType.INTERNET_ARCHIVE -> painterResource(R.drawable.ic_space_interent_archive)
        VaultType.DWEB_STORAGE -> painterResource(R.drawable.ic_space_dweb)
        VaultType.STORACHA -> painterResource(R.drawable.storacha)
    }
    Icon(
        modifier = modifier,
        painter = icon,
        contentDescription = null,
        tint = tint ?: MaterialTheme.colorScheme.onBackground
    )
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ExpandableSpaceListPreview() {
    val state = rememberAccordionState(
        expanded = true,
    )

    DefaultBoxPreview {
        ExpandableSpaceList(
            selectedSpace = dummySpaceList[1].toDomain(),
            spaceList = dummySpaceList.map { it.toDomain() },
            serverAccordionState = state,
            onSpaceSelected = {},
            onAddAnotherAccountClicked = {}
        )
    }
}