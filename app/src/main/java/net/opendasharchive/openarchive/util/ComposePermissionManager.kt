package net.opendasharchive.openarchive.util

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.ButtonData
import net.opendasharchive.openarchive.features.core.dialog.DialogConfig
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import org.koin.compose.koinInject

internal enum class PendingPermission {
    CAMERA,
    MEDIA,
}

@OptIn(ExperimentalPermissionsApi::class)
class ComposePermissionManager internal constructor(
    private val dialogManager: DialogStateManager,
    private val cameraPermissionState: PermissionState,
    private val audioPermissionState: PermissionState,
    private val mediaPermissionState: MultiplePermissionsState,
    private val setPending: (PendingPermission?, (() -> Unit)?) -> Unit,
) {
    fun cameraStatus(): PermissionStatus = cameraPermissionState.status

    fun audioStatus(): PermissionStatus = audioPermissionState.status

    fun isCameraGranted(): Boolean {
        return cameraPermissionState.status is PermissionStatus.Granted
    }

    fun isAudioGranted(): Boolean {
        return audioPermissionState.status is PermissionStatus.Granted
    }

    fun requestCameraPermission() {
        cameraPermissionState.launchPermissionRequest()
    }

    fun requestAudioPermission() {
        audioPermissionState.launchPermissionRequest()
    }

    fun requestAudioPermissionIfNeeded(allowVideoCapture: Boolean) {
        if (allowVideoCapture && !isAudioGranted()) {
            requestAudioPermission()
        }
    }

    fun shouldShowCameraRationale(): Boolean {
        return (cameraPermissionState.status as? PermissionStatus.Denied)?.shouldShowRationale ?: false
    }

    fun shouldShowAudioRationale(): Boolean {
        return (audioPermissionState.status as? PermissionStatus.Denied)?.shouldShowRationale ?: false
    }

    fun checkCameraPermission(onGranted: () -> Unit) {
        when (val status = cameraPermissionState.status) {
            is PermissionStatus.Granted -> onGranted()
            is PermissionStatus.Denied -> {
                setPending(PendingPermission.CAMERA, onGranted)
                if (status.shouldShowRationale) {
                    dialogManager.showDialog(
                        config = DialogConfig(
                            type = DialogType.Warning,
                            title = UiText.Dynamic("Camera Permission"),
                            message = UiText.Dynamic("Camera access is needed to take pictures. Please grant permission."),
                            positiveButton = ButtonData(
                                text = UiText.Dynamic("Accept"),
                                action = {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            ),
                            neutralButton = ButtonData(
                                text = UiText.Dynamic("Cancel")
                            )
                        )
                    )
                } else {
                    cameraPermissionState.launchPermissionRequest()
                }
            }
        }
    }

    fun checkMediaPermissions(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted()
            return
        }

        if (mediaPermissionState.allPermissionsGranted) {
            onGranted()
            return
        }

        setPending(PendingPermission.MEDIA, onGranted)
        if (mediaPermissionState.shouldShowRationale) {
            dialogManager.showDialog(
                config = DialogConfig(
                    type = DialogType.Warning,
                    title = UiText.Dynamic("Media Permission"),
                    message = UiText.Dynamic("Access to your media is required to pick images and videos."),
                    positiveButton = ButtonData(
                        text = UiText.Dynamic("Accept"),
                        action = {
                            mediaPermissionState.launchMultiplePermissionRequest()
                        }
                    ),
                    neutralButton = ButtonData(
                        text = UiText.Dynamic("Cancel")
                    )
                )
            )
        } else {
            mediaPermissionState.launchMultiplePermissionRequest()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberComposePermissionManager(
    dialogManager: DialogStateManager = koinInject()
): ComposePermissionManager {
    var pendingPermission by remember { mutableStateOf<PendingPermission?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val mediaPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    )

    LaunchedEffect(
        cameraPermissionState.status,
        mediaPermissionState.allPermissionsGranted,
        pendingPermission
    ) {
        when (pendingPermission) {
            PendingPermission.CAMERA -> {
                if (cameraPermissionState.status is PermissionStatus.Granted) {
                    pendingPermission = null
                    pendingAction?.invoke()
                    pendingAction = null
                }
            }
            PendingPermission.MEDIA -> {
                if (mediaPermissionState.allPermissionsGranted) {
                    pendingPermission = null
                    pendingAction?.invoke()
                    pendingAction = null
                }
            }
            null -> Unit
        }
    }

    return remember(dialogManager) {
        ComposePermissionManager(
            dialogManager = dialogManager,
            cameraPermissionState = cameraPermissionState,
            audioPermissionState = audioPermissionState,
            mediaPermissionState = mediaPermissionState,
            setPending = { type, action ->
                pendingPermission = type
                pendingAction = action
            }
        )
    }
}
