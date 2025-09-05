package net.opendasharchive.openarchive.features.spaces

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.main.ui.components.SpaceIcon
import net.opendasharchive.openarchive.features.main.ui.components.dummySpaceList
import org.koin.androidx.compose.koinViewModel


class SpaceListViewModel() : ViewModel() {

    private val _spaceList = MutableStateFlow<List<Space>>(emptyList())
    val spaceList: StateFlow<List<Space>> = _spaceList

    fun refreshSpaces() {
        _spaceList.value = Space.getAll().asSequence().toList()
    }
}

@Composable
fun SpaceListScreen(
    onSpaceClicked: (Space) -> Unit,
    onAddServerClicked: () -> Unit = {},
    viewModel: SpaceListViewModel = koinViewModel()
) {

    val spaceList by viewModel.spaceList.collectAsStateWithLifecycle()

    // This will get called again when the screen resumes (see Fragment below)
    LaunchedEffect(Unit) {
        viewModel.refreshSpaces()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceListScreenContent(
            spaceList = spaceList,
            onSpaceClicked = onSpaceClicked,
            onAddServerClicked = onAddServerClicked
        )
    }
}

@Composable
fun SpaceListScreenContent(
    onSpaceClicked: (Space) -> Unit,
    onAddServerClicked: () -> Unit,
    spaceList: List<Space> = emptyList()
) {

    Box(modifier = Modifier.fillMaxSize()) {
        if (spaceList.isEmpty()) {
            // Empty state with centered message
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lbl_no_servers),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
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

        // Add Server button at bottom center (visible in both states)
        Button(
            onClick = onAddServerClicked,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.7f)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
            )
        ) {
            Text(
                text = "+ Add Server",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
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
            onSpaceClicked = {},
            onAddServerClicked = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SpaceListEmptyScreenPreview() {

    DefaultScaffoldPreview {

        SpaceListScreenContent(
            spaceList = emptyList(),
            onSpaceClicked = {},
            onAddServerClicked = {}
        )
    }
}