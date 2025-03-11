package net.opendasharchive.openarchive.features.main.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R

@Composable
fun MainBottomBar(
    isSettings: Boolean,
    onMyMediaClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddMediaClick: () -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary
    ) {

        BottomNavMenuItem(
            isSelected = !isSettings,
            onClick = onMyMediaClick,
            selectedIcon = Icons.Default.PermMedia,
            unSelectedIcon = Icons.Outlined.PermMedia,
            text = "My Media"
        )

        FloatingActionButton(
            modifier = Modifier.size(height = 42.dp, width = 90.dp),
            onClick = onAddMediaClick,
            containerColor = colorResource(R.color.colorOnPrimary),
            shape = RoundedCornerShape(percent = 50),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
        }

        BottomNavMenuItem(
            isSelected = isSettings,
            onClick = onSettingsClick,
            selectedIcon = Icons.Default.Settings,
            unSelectedIcon = Icons.Outlined.Settings,
            text = "Settings"
        )

    }
}

@Composable
fun RowScope.BottomNavMenuItem(
    selectedIcon: ImageVector,
    unSelectedIcon: ImageVector,
    isSelected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val icon = if (isSelected) selectedIcon else unSelectedIcon
    NavigationBarItem(
        label = {
            Text(text)
        },
        selected = isSelected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    )
}