package net.opendasharchive.openarchive.features.core

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import net.opendasharchive.openarchive.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeAppBar(
    title: String = "",
    actions: @Composable (RowScope.() -> Unit) = {},
    onNavigateBack: () -> Unit = {},
    showNavigationIcon: Boolean = true
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (showNavigationIcon) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back_ios),
                        contentDescription = null
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}
