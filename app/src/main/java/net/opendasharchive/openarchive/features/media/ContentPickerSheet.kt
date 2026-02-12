package net.opendasharchive.openarchive.features.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentPickerSheet(
    title: String? = null,
    onDismiss: () -> Unit,
    onMediaPicked: (AddMediaType) -> Unit
) {

    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = shape,
        containerColor = colorResource(R.color.colorPill)
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
                        onMediaPicked(AddMediaType.CAMERA)
                        onDismiss()
                    }

                    PickerItem(
                        iconRes = R.drawable.ic_image_gallery_line,
                        labelRes = R.string.photo_gallery
                    ) {
                        onMediaPicked(AddMediaType.GALLERY)
                        onDismiss()
                    }

                    PickerItem(
                        iconRes = R.drawable.ic_description,
                        labelRes = R.string.files
                    ) {
                        onMediaPicked(AddMediaType.FILES)
                        onDismiss()
                    }
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

@Preview
@Composable
private fun ContentPickerSheetPreview() {
    SaveAppTheme {
        ContentPickerSheet(onDismiss = {}, onMediaPicked = {})
    }
}
