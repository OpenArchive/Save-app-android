package net.opendasharchive.openarchive.features.main.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultEmptyScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerSheet

enum class HomeBottomTab {
    MEDIA,
    SETTINGS
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainBottomBar(
    selectedTab: HomeBottomTab,
    onTabSelected: (HomeBottomTab) -> Unit,
    onAddClick: () -> Unit,
    onAddLongClick: () -> Unit,
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.colorTertiary))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.bottom_nav_height))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // My Media section
            BottomNavItem(
                iconRes = if (selectedTab == HomeBottomTab.MEDIA) R.drawable.perm_media_24px else R.drawable.outline_perm_media_24,
                label = stringResource(R.string.my_media),
                isSelected = selectedTab == HomeBottomTab.MEDIA,
                onClick = { onTabSelected(HomeBottomTab.MEDIA) },
                modifier = Modifier.weight(1f)
            )

            // Add button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(height = 42.dp, width = 90.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(colorResource(R.color.colorAddButton))
                        .combinedClickable(
                            onClick = onAddClick,
                            onLongClick = onAddLongClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Add Media",
                        tint = colorResource(R.color.colorOnAddButton),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Settings section
            BottomNavItem(
                iconRes = if (selectedTab == HomeBottomTab.SETTINGS) R.drawable.ic_settings_filled else R.drawable.ic_settings,
                label = stringResource(R.string.action_settings),
                isSelected = selectedTab == HomeBottomTab.SETTINGS,
                onClick = { onTabSelected(HomeBottomTab.SETTINGS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Preview
@Composable
private fun MainBottomBarPreview() {
    DefaultEmptyScaffoldPreview {

        MainBottomBar(
            selectedTab = HomeBottomTab.SETTINGS,
            onTabSelected = {},
            onAddClick = {},
            onAddLongClick = {}
        )
    }
}

@Composable
private fun BottomNavItem(
    @DrawableRes iconRes: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.clickable(onClick = onClick, indication = null, interactionSource = remember { MutableInteractionSource() }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}
