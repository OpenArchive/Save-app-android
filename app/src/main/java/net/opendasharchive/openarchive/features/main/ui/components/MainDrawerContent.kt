package net.opendasharchive.openarchive.features.main.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.rememberAccordionState

@Composable
fun MainDrawerContent(
    selectedSpace: Space? = null,
    spaceList: List<Space> = emptyList(),
    projects: List<Project> = emptyList(),
    selectedProject: Project? = null,
    onProjectSelected: (Project) -> Unit = {},
    onNewFolderClick: () -> Unit = {}
) {

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val serverAccordionState = rememberAccordionState()

    val spaceListBackgroundColor = colorResource(R.color.colorDrawerSpaceListBackground)

    ModalDrawerSheet(
        drawerShape = DrawerDefaults.shape,
        modifier = Modifier.width(screenWidth * 0.75f),
        drawerContainerColor = colorResource(R.color.colorNavigationDrawerBackground)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main drawer content (always visible)
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // AppBar height spacer
                Spacer(modifier = Modifier.height(56.dp))

                // Drawer Header - "Servers" text with expand/collapse icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { serverAccordionState.toggle() }
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
                            type = space.tType,
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
                        .alpha(if (serverAccordionState.expanded) 0.3f else 1f)
                        .verticalScroll(rememberScrollState()),
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
                    onClick = onNewFolderClick,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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

            // Spacer to position overlays below header
            Spacer(modifier = Modifier.height(148.dp))

            // Dim overlay and space list container (with animations)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dim overlay (shown when space list is expanded)
                androidx.compose.animation.AnimatedVisibility(
                    visible = serverAccordionState.expanded,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x80000000))
                            .clickable { serverAccordionState.toggle() }
                    )
                }

                // Expandable space list (floats over content when expanded)
                androidx.compose.animation.AnimatedVisibility(
                    visible = serverAccordionState.expanded,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(200)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(200)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(spaceListBackgroundColor),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // All spaces
                        spaceList.forEach { space ->
                            SpaceListItem(
                                space = space,
                                isSelected = space.id == selectedSpace?.id,
                                onSpaceSelected = { /* TODO: Handle space selection */ }
                            )
                        }

                        // Add Another Account button
                        AddAnotherAccountItem(
                            onClick = { /* TODO: Handle add space */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceListItem(
    space: Space,
    isSelected: Boolean,
    onSpaceSelected: (Space) -> Unit
) {
    val backgroundColor = if (isSelected) colorResource(R.color.colorTertiary) else colorResource(R.color.colorDrawerSpaceListBackground)
    val textColor = if (isSelected) colorResource(R.color.colorOnBackground) else colorResource(R.color.colorText)

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
            type = space.tType,
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
private fun FolderItem(
    project: Project,
    isSelected: Boolean,
    onProjectSelected: (Project) -> Unit
) {
    val iconRes = if (isSelected) R.drawable.baseline_folder_white_24 else R.drawable.outline_folder_white_24
    val iconColor = if (isSelected) colorResource(R.color.colorTertiary) else colorResource(R.color.colorOnBackground)
    val textColor = if (isSelected) colorResource(R.color.colorOnBackground) else colorResource(R.color.colorText)

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
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Default // Using default instead of montserrat_semi_bold for now
            ),
            color = textColor
        )
    }
}

@Preview
@Composable
private fun MainDrawerContentPreview() {
    DefaultScaffoldPreview {
        MainDrawerContent()
    }
}