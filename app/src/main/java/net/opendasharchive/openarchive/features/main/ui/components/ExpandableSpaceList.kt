package net.opendasharchive.openarchive.features.main.ui.components

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.components.PrimaryButton
import net.opendasharchive.openarchive.core.presentation.theme.DefaultBoxPreview
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.Accordion
import net.opendasharchive.openarchive.features.core.AccordionState
import net.opendasharchive.openarchive.features.core.rememberAccordionState

@Composable
fun ExpandableSpaceList(
    serverAccordionState: AccordionState,
    selectedSpace: Space? = null,
    spaceList: List<Space>
) {
    Accordion(
        state = serverAccordionState,
        headerContent = {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                if (selectedSpace != null) {
                    DrawerSpaceListItem(space = selectedSpace)
                } else {
                    Text("Servers")
                }

                IconButton(
                    modifier = Modifier.rotate(serverAccordionState.animationProgress * 180),
                    onClick = {
                        serverAccordionState.toggle()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Expand"
                    )
                }
            }
        },
        bodyContent = {

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spaceList.forEach { space ->
                    DrawerSpaceListItem(space)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    PrimaryButton(
                        text = "Add Server",
                        icon = Icons.Default.Add
                    ) { }
                }
            }

        }
    )
}

@Composable
fun DrawerSpaceListItem(
    space: Space,
) {
    Row(
        modifier = Modifier
            .wrapContentSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SpaceIcon(
            type = space.tType ?: Space.Type.INTERNET_ARCHIVE,
            modifier = Modifier.size(24.dp)
        )

        Text(space.name)
    }
}

@Composable
fun SpaceIcon(
    type: Space.Type,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val icon = when (type) {
        Space.Type.WEBDAV -> painterResource(R.drawable.ic_space_private_server)
        Space.Type.INTERNET_ARCHIVE -> painterResource(R.drawable.ic_space_interent_archive)
        Space.Type.GDRIVE -> painterResource(R.drawable.logo_gdrive_outline)
        Space.Type.RAVEN -> painterResource(R.drawable.ic_space_dweb)
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
            selectedSpace = dummySpaceList[1],
            spaceList = dummySpaceList,
            serverAccordionState = state
        )
    }
}

val dummySpaceList = listOf(
    Space(
        type = Space.Type.WEBDAV.id,
        username = "",
        password = "",
        name = "Elelan Server",
    ),
    Space(
        type = Space.Type.INTERNET_ARCHIVE.id,
        username = "",
        password = "",
        name = "Test Server",
    ),
    Space(
        type = Space.Type.RAVEN.id,
        username = "",
        password = "",
        name = "DWebServer",
    ),
)