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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.main.HomeActivity
import net.opendasharchive.openarchive.features.main.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    openDrawer: () -> Unit,
    showDrawer: Boolean = false
) {

    TopAppBar(
        title = {
            Image(
                modifier = Modifier
                    .size(64.dp),
                painter = painterResource(R.drawable.savelogo),
                contentDescription = "Save Logo",
                colorFilter = ColorFilter.tint(colorResource(R.color.colorOnPrimary))
            )
        },
        actions = {

            // Only show drawer icon when not on settings page
            AnimatedVisibility(showDrawer) {
                IconButton(
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = colorResource(R.color.colorOnPrimary)
                    ),
                    onClick = {
                        openDrawer()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorResource(R.color.colorTertiary)
        )
    )
}