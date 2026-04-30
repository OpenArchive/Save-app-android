package net.opendasharchive.openarchive.features.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultBoxPreview
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLightDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentPickerSheet(
    title: String? = null,
    onClipboardClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onMediaTypeSelected: (AddMediaType) -> Unit
) {

    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = shape,
        containerColor = colorResource(R.color.colorPill)
    ) {
        ContentPickerContent(
            title = title,
            onClipboardClick = onClipboardClick,
            onDismiss = onDismiss,
            onMediaTypeSelected = onMediaTypeSelected
        )
    }
}

@Composable
private fun ContentPickerContent(
    title: String? = null,
    onClipboardClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onMediaTypeSelected: (AddMediaType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = title ?: stringResource(R.string.content_picker_label),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            PickerItem(
                iconRes = R.drawable.ic_photo_camera,
                labelRes = R.string.camera
            ) {
                onMediaTypeSelected(AddMediaType.CAMERA)
                onDismiss()
            }

            PickerItem(
                iconRes = R.drawable.ic_image_gallery_line,
                labelRes = R.string.photo_gallery
            ) {
                onMediaTypeSelected(AddMediaType.GALLERY)
                onDismiss()
            }

            PickerItem(
                iconRes = R.drawable.ic_description,
                labelRes = R.string.files
            ) {
                onMediaTypeSelected(AddMediaType.FILES)
                onDismiss()
            }
        }

        onClipboardClick?.let {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        it()
                        onDismiss()
                    }
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_content_copy),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.paste_from_clipboard),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                )
            }
        }
    }
}

@Composable
private fun PickerItem(
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = colorResource(R.color.colorOnBackground),
            modifier = Modifier
                .padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = MontserratFontFamily
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@PreviewLightDark
@Composable
private fun ContentPickerSheetPreview() {
    DefaultBoxPreview {
        ContentPickerContent(
            onDismiss = {},
            onMediaTypeSelected = {},
            onClipboardClick = {}
        )
    }
}
