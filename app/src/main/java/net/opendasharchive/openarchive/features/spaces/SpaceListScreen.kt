package net.opendasharchive.openarchive.features.spaces

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.main.ui.components.SpaceIcon
import net.opendasharchive.openarchive.features.main.ui.components.dummySpaceList

@Composable
fun SpaceListScreen(
    onSpaceClicked: (Space) -> Unit,

    ) {

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        SpaceListScreenContent(
            spaceList = Space.getAll().asSequence().toList(),
            onSpaceClicked = onSpaceClicked
        )
    }

}

@Composable
fun SpaceListScreenContent(
    onSpaceClicked: (Space) -> Unit,
    spaceList: List<Space> = emptyList()
) {


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        spaceList.forEach { space ->

            SpaceListItem(
                space = space,
                onClick = {
                    onSpaceClicked(space)
                }
            )
        }
    }
}

@Composable
fun SpaceListItem(
    space: Space,
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
            type = space.tType,
            modifier = Modifier.size(42.dp)
        )

        Column(
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = space.friendlyName,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 1.sp
            )

            Text(
                text = space.tType.friendlyName,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                lineHeight = 1.sp
            )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SpaceListScreenPreview() {

    DefaultScaffoldPreview {

        SpaceListScreenContent(
            spaceList = dummySpaceList,
            onSpaceClicked = {

            }
        )
    }
}