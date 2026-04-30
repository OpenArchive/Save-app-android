package net.opendasharchive.openarchive.features.spaces

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultBoxPreview

@Composable
fun ServerOptionItem(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    // You can customize this look to match your original design
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(top = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = colorResource(R.color.colorTertiary),
                    modifier = Modifier
                        .size(24.dp)

                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .align(Alignment.Top)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.Top)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                )

                Row {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.weight(0.5f))
                }
            }

            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                painter = painterResource(R.drawable.ic_arrow_forward_ios),
                contentDescription = null,
            )
        }


    }
}

@PreviewLightDark
@Composable
private fun ServerOptionItemPreview() {
    DefaultBoxPreview {

        Column {
            ServerOptionItem(
                iconRes = R.drawable.ic_private_server,
                title = stringResource(R.string.private_server),
                subtitle = stringResource(R.string.send_directly_to_a_private_server),
                onClick = {}
            )
        }


    }
}

@PreviewLightDark
@Composable
private fun ServerOptionsItemPreview() {
    DefaultBoxPreview {

        Column {
            ServerOptionItem(
                iconRes = R.drawable.ic_private_server,
                title = stringResource(R.string.private_server),
                subtitle = stringResource(R.string.send_directly_to_a_private_server),
                onClick = {}
            )

            ServerOptionItem(
                iconRes = R.drawable.ic_internet_archive,
                title = stringResource(R.string.internet_archive),
                subtitle = stringResource(R.string.upload_to_the_internet_archive),
                onClick = {}
            )

            ServerOptionItem(
                iconRes = R.drawable.ic_dweb,
                title = stringResource(R.string.dweb_title),
                subtitle = stringResource(R.string.dweb_description),
                onClick = {}
            )
        }


    }
}