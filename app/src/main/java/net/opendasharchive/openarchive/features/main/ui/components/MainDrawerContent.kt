package net.opendasharchive.openarchive.features.main.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.rememberAccordionState

@Composable
fun MainDrawerContent(
    selectedSpace: Space? = null,
    spaceList: List<Space> = emptyList()
) {

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val serverAccordionState = rememberAccordionState()

    ModalDrawerSheet(
        drawerShape = DrawerDefaults.shape,
        modifier = Modifier.width(screenWidth * 0.65f),
        drawerContainerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {


                Spacer(Modifier.height(12.dp))

                ExpandableSpaceList(
                    serverAccordionState,
                    selectedSpace = selectedSpace,
                    spaceList = spaceList
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = 0.3.dp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Folder,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null
                        )
                        Text("Summer Vacation")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = null
                        )
                        Text("Prague")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = null
                        )
                        Text("Misc")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = null
                        )
                        Text("Folder")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = null
                        )
                        Text("Folder")
                    }
                }



                Spacer(Modifier.height(12.dp))


            }


            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {

                Button(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    shape = RoundedCornerShape(8f),
                    onClick = {

                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Text("New Folder")
                    }
                }
            }
        }
    }
}

@Composable
fun MainDrawerFolderListItem(
    project: Project,
    isSelected: Boolean = false,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = Icons.Outlined.Folder,
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = null
        )

        Text("Prague")
    }
}

@Preview
@Composable
private fun MainDrawerContentPreview() {
    DefaultScaffoldPreview {
        MainDrawerContent()
    }
}