package net.opendasharchive.openarchive.features.main.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    openDrawer: () -> Unit,
    onExit: () -> Unit
) {

    TopAppBar(
        title = {
            Image(
                modifier = Modifier
                    .size(64.dp)
                    .clickable {
                        onExit()
                    },
                painter = painterResource(R.drawable.savelogo),
                contentDescription = "Save Logo",
                colorFilter = ColorFilter.tint(colorResource(R.color.colorOnPrimary))
            )
        },
        actions = {

            AnimatedVisibility(
                visible = false
            ) {
                IconButton(
                    onClick = {}
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null
                    )
                }

            }

            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = colorResource(R.color.colorOnSecondary)
                ),
                onClick = {
                    openDrawer()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null
                )
            }

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorResource(R.color.colorPrimary)
        )
    )
}