package net.opendasharchive.openarchive.features.media

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest

data class MediaLaunchers(
    val galleryLauncher: ActivityResultLauncher<PickVisualMediaRequest>, // Changed
    val filePickerLauncher: ActivityResultLauncher<Intent>,
    val cameraLauncher: ActivityResultLauncher<Intent>,
    val modernCameraLauncher: ActivityResultLauncher<Uri>,
    val customCameraLauncher: ActivityResultLauncher<Intent>
)