package net.opendasharchive.openarchive.util

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.ButtonData
import net.opendasharchive.openarchive.features.core.dialog.DialogConfig
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager

class PermissionManager(
    private val activity: ComponentActivity,
    private val dialogManager: DialogStateManager
) {
    // Callbacks stored to invoke after permission result
    private var cameraPermissionCallback: ((Boolean) -> Unit)? = null
    private var notificationPermissionCallback: ((Boolean) -> Unit)? = null
    private var mediaPermissionCallback: ((Boolean) -> Unit)? = null

    // Launcher for camera permission
    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            cameraPermissionCallback?.invoke(isGranted)
        }

    // Launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            notificationPermissionCallback?.invoke(isGranted)
        }

    // Launcher for media permissions (for Android TIRAMISU+).
    private val mediaPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            mediaPermissionCallback?.invoke(allGranted)
        }

    /**
     * Check camera permission and, if granted, invoke [onGranted]. Otherwise, show a rationale dialog
     * (if needed) and request permission.
     */
    fun checkCameraPermission(onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            dialogManager.showDialog(
                config = DialogConfig(
                    type = DialogType.Warning,
                    title = UiText.DynamicString("Camera Permission"),
                    message = UiText.DynamicString("Camera access is needed to take pictures. Please grant permission."),
                    positiveButton = ButtonData(
                        text = UiText.DynamicString("Accept"),
                        action = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    ),
                    neutralButton = ButtonData(
                        text = UiText.DynamicString("Cancel")
                    )
                )
            )
            // Callback will handle onGranted if permission is granted later
            cameraPermissionCallback = { granted ->
                if (granted) onGranted()
            }
        } else {
            cameraPermissionCallback = { granted ->
                if (granted) onGranted()
            }
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Check notification permission (for Android 13+) and, if granted, invoke [onGranted]. Otherwise, show
     * a rationale dialog and request permission.
     */
    fun checkNotificationPermission(onGranted: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                dialogManager.showDialog(
                    config = DialogConfig(
                        type = DialogType.Warning,
                        title = UiText.DynamicString("Notification Permission"),
                        message = UiText.DynamicString("We need permission to post notifications."),
                        positiveButton = ButtonData(
                            text = UiText.DynamicString("Accept"),
                            action = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ),
                        neutralButton = ButtonData(
                            text = UiText.DynamicString("Cancel")
                        )
                    )
                )
                notificationPermissionCallback = { granted ->
                    if (granted) onGranted()
                }
            } else {
                notificationPermissionCallback = { granted ->
                    if (granted) onGranted()
                }
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // For older Android versions, no notification permission is needed.
            onGranted()
        }
    }

    /**
     * Check media permissions (for Android TIRAMISU+) for reading images and videos.
     * If permissions are granted, [onGranted] is invoked. Otherwise, a rationale dialog is shown and
     * the permission is requested.
     */
    fun checkMediaPermissions(onGranted: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val imageGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val videoGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED

            if (imageGranted && videoGranted) {
                onGranted()
            } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES)
                || activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VIDEO)) {
                dialogManager.showDialog(
                    config = DialogConfig(
                        type = DialogType.Warning,
                        title = UiText.DynamicString("Media Permission"),
                        message = UiText.DynamicString("Access to your media is required to pick images and videos."),
                        positiveButton = ButtonData(
                            text = UiText.DynamicString("Accept"),
                            action = {
                                mediaPermissionCallback = { granted ->
                                    if (granted) onGranted()
                                }
                                mediaPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_MEDIA_IMAGES,
                                        Manifest.permission.READ_MEDIA_VIDEO
                                    )
                                )
                            }
                        ),
                        neutralButton = ButtonData(
                            text = UiText.DynamicString("Cancel")
                        )
                    )
                )
            } else {
                mediaPermissionCallback = { granted ->
                    if (granted) onGranted()
                }
                mediaPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                )
            }
        } else {
            // For devices lower than Tiramisu, permissions are not required.
            onGranted()
        }
    }
}
