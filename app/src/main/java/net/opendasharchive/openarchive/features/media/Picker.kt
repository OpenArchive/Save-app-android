package net.opendasharchive.openarchive.features.media

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import net.opendasharchive.openarchive.features.media.camera.CameraActivity
import net.opendasharchive.openarchive.features.media.camera.CameraConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Utility
import net.opendasharchive.openarchive.util.extensions.makeSnackBar
import org.witness.proofmode.ProofMode
import org.witness.proofmode.crypto.HashUtils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.Date

object Picker {

    private var currentPhotoUri: Uri? = null

    fun register(
        activity: ComponentActivity,
        root: View,
        project: () -> Project?,
        completed: (List<Media>) -> Unit
    ): MediaLaunchers {

        // Official Gallery Picker
        val galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(10) // Supports multiple selection
        ) { uris: List<Uri>? ->
            if (uris.isNullOrEmpty()) return@registerForActivityResult

            val snackbar = showProgressSnackBar(activity, root, activity.getString(R.string.importing_media))
            activity.lifecycleScope.launch(Dispatchers.IO) {
                val media = import(activity, project(), uris, false)
                activity.lifecycleScope.launch(Dispatchers.Main) {
                    snackbar.dismiss()
                    completed(media)
                }
            }
        }

        val filePickerLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != AppCompatActivity.RESULT_OK) return@registerForActivityResult

            val uri = result.data?.data ?: return@registerForActivityResult

            val snackbar = showProgressSnackBar(activity, root, activity.getString(R.string.importing_media))

            activity.lifecycleScope.launch(Dispatchers.IO) {
                // We don't generate proof for file picker files.
                val files = import(activity, project(), listOf(uri), false)

                activity.lifecycleScope.launch(Dispatchers.Main) {
                    snackbar.dismiss()
                    completed(files)
                }
            }
        }

        val legacyCameraLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                currentPhotoUri?.let { uri ->

                    val snackbar = showProgressSnackBar(activity, root, activity.getString(R.string.importing_media))

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        // We generate proof for in app capture Just because we toggle it true, it doesn't generate proof.
                        // It should be on in the settings too. We check that inside import
                        val media = import(activity, project(), listOf(uri),true)

                        activity.lifecycleScope.launch(Dispatchers.Main) {
                            snackbar.dismiss()
                            completed(media)
                        }
                    }
                }
            }
        }

        // Modern camera launcher using TakePicture contract
        val modernCameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && currentPhotoUri != null) {
                val capturedImageUri: Uri = currentPhotoUri!!
                val snackbar = showProgressSnackBar(activity, root, activity.getString(R.string.importing_media))

                activity.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Import the captured photo with proof generation enabled
                        val media = import(activity, project(), listOf(capturedImageUri), true)

                        activity.lifecycleScope.launch(Dispatchers.Main) {
                            snackbar.dismiss()
                            completed(media)
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error processing camera capture", e)
                        activity.lifecycleScope.launch(Dispatchers.Main) {
                            snackbar.dismiss()
                            Toast.makeText(activity, "Failed to process photo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // Camera capture failed or was cancelled
                AppLogger.w("Camera capture failed or cancelled")
                currentPhotoUri = null
            }
        }

        // Custom camera launcher
        val customCameraLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val capturedUris = result.data?.getStringArrayListExtra(CameraActivity.EXTRA_CAPTURED_URIS)
                if (!capturedUris.isNullOrEmpty()) {
                    val uris = capturedUris.map { Uri.parse(it) }
                    val snackbar = showProgressSnackBar(activity, root, activity.getString(R.string.importing_media))

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            // Import the captured media with proof generation enabled
                            // This ensures proper mimetype detection and Media object setup
                            val media = import(activity, project(), uris, true)

                            activity.lifecycleScope.launch(Dispatchers.Main) {
                                snackbar.dismiss()
                                completed(media)
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error processing camera captures", e)
                            activity.lifecycleScope.launch(Dispatchers.Main) {
                                snackbar.dismiss()
                                Toast.makeText(activity, "Failed to process captures", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    AppLogger.w("No captures returned from custom camera")
                }
            } else {
                AppLogger.w("Custom camera capture cancelled or failed")
            }
        }

        return MediaLaunchers(
            galleryLauncher = galleryLauncher,
            filePickerLauncher = filePickerLauncher,
            cameraLauncher = legacyCameraLauncher,
            modernCameraLauncher = modernCameraLauncher,
            customCameraLauncher = customCameraLauncher
        )
    }

    fun pickMedia(launcher: ActivityResultLauncher<PickVisualMediaRequest>) {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        launcher.launch(request)
    }

    fun canPickFiles(context: Context): Boolean {
        return mFilePickerIntent.resolveActivity(context.packageManager) != null
    }

    fun pickFiles(launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(mFilePickerIntent)
    }

    private val mFilePickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"

        putExtra(
            Intent.EXTRA_MIME_TYPES,
            arrayOf(
                "application/pdf", // pdf
                "image/*",         // all images
                "video/*",         // all videos
                "audio/mpeg",      // mp3 (most devices)
                "audio/mp3"        // some devices use this
            )
        )
    }

    /**
     * Launch custom camera with configuration options.
     * Supports both photo and video capture with preview functionality.
     */
    fun launchCustomCamera(activity: Activity, launcher: ActivityResultLauncher<Intent>, config: CameraConfig = CameraConfig()) {
        val intent = CameraActivity.createIntent(activity, config)
        launcher.launch(intent)
    }

    /**
     * Modern camera photo capture using TakePicture contract.
     * This is the recommended approach for new implementations.
     */
    fun takePhotoModern(activity: Activity, launcher: ActivityResultLauncher<Uri>) {
        try {
            val file = Utility.getOutputMediaFileByCache(activity, "IMG_${System.currentTimeMillis()}.jpg")

            file?.let {
                val uri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.provider",
                    it
                )

                currentPhotoUri = uri
                AppLogger.d("Taking photo with modern launcher, URI: $uri")
                launcher.launch(uri)
            } ?: run {
                AppLogger.e("Failed to create temp file for camera")
                Toast.makeText(activity, "Failed to prepare camera", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            AppLogger.e("Error setting up camera", e)
            Toast.makeText(activity, "Camera setup failed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Legacy camera photo capture (kept for backward compatibility).
     * Use takePhotoModern() for new implementations.
     */
    @Deprecated("Use takePhotoModern() instead")
    fun takePhoto(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        val file = Utility.getOutputMediaFileByCache(activity, "IMG_${System.currentTimeMillis()}.jpg")

        file?.let {
            val uri = FileProvider.getUriForFile(
                activity, "${activity.packageName}.provider",
                it
            )

            currentPhotoUri = uri

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION) // Ensure permission is granted
            }

            if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
                launcher.launch(takePictureIntent)
            } else {
                Toast.makeText(activity, "Camera not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun import(context: Context, project: Project?, uris: List<Uri>, generateProof: Boolean): ArrayList<Media> {
        val result = ArrayList<Media>()

        for (uri in uris) {
            try {
                //Simply pass the generate proof boolean for single file import which is looped here
                val media = import(context, project, uri, generateProof)
                if (media != null) result.add(media)
            } catch (e: Exception) {
                AppLogger.e( "Error importing media", e)
            }
        }

        return result
    }

    fun import(context: Context, project: Project?, uri: Uri, generateProof: Boolean): Media? {

        val project = project ?: return null

        val title = Utility.getUriDisplayName(context, uri) ?: ""
        val file = Utility.getOutputMediaFileByCache(context, title)

        // Use try-with-resources pattern for proper resource management
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                if (!Utility.writeStreamToFile(inputStream, file)) {
                    AppLogger.e("Failed to write stream to file for URI: $uri")
                    return null
                }
            } ?: run {
                AppLogger.e("Failed to open input stream for URI: $uri")
                return null
            }
        } catch (e: FileNotFoundException) {
            AppLogger.e("File not found for URI: $uri", e)
            return null
        } catch (e: SecurityException) {
            AppLogger.e("Permission denied for URI: $uri", e)
            return null
        } catch (e: java.io.IOException) {
            AppLogger.e("IO error reading URI: $uri", e)
            return null
        }

        // Create media object
        val media = Media()
        val coll = project.openCollection
        media.collectionId = coll.id

        val fileSource = uri.path?.let { File(it) }
        var createDate = Date()

        if (fileSource?.exists() == true) {
            createDate = Date(fileSource.lastModified())
            media.contentLength = fileSource.length()
        }
        else {
            media.contentLength = file?.length() ?: 0
        }

        media.originalFilePath = Uri.fromFile(file).toString()
        // Enhanced mime type detection for file URIs
        media.mimeType = getMimeTypeWithFallback(context, uri, file?.path)
        media.createDate = createDate
        media.updateDate = media.createDate
        media.sStatus = Media.Status.Local

        //We generate hash regardless if proof is on or off because we don't want unexpected behaviour when we are looking for proof files when uploaded later.
        // Generate hash regardless of proof mode setting for consistency
        try {
            media.mediaHashString = file?.let {
                HashUtils.getSHA256FromFileContent(it.inputStream())
            } ?: ""
        } catch (e: Exception) {
            AppLogger.e("Failed to generate hash for media", e)
            media.mediaHashString = ""
        }

        media.projectId = project.id
        media.title = title
        media.save()

        // Generate ProofMode data if enabled
        if (generateProof && Prefs.useProofMode) {

            try {
                //If Proof mode is on we need this to be on always
                // Ensure location and network tracking are enabled for camera captures
                // Only enabled for camera captures (generateProof = true)
                Prefs.proofModeLocation = true
                Prefs.proofModeNetwork = true

                AppLogger.d("Generating ProofMode data for URI: $uri, Hash: ${media.mediaHashString}")

                // Generate proof using the ProofMode library
                ProofMode.generateProof(context, uri, media.mediaHashString)

                AppLogger.i("ProofMode generation completed for media: ${media.title}")
            } catch (e: Exception) {
                AppLogger.e("Failed to generate ProofMode data", e)
                Timber.w("ProofMode generation failed: ${e.message}")
            }
        } else {
            if (generateProof) {
                AppLogger.w("ProofMode generation requested but useProofMode is disabled")
            }
            Timber.w("Skipping proof generation - generateProof: $generateProof, useProofMode: ${Prefs.useProofMode}")
        }
        return media
    }



    @SuppressLint("RestrictedApi")
    private fun showProgressSnackBar(activity: Activity, root: View, message: String): Snackbar {
        val bar = root.makeSnackBar(message)
        (bar.view as? Snackbar.SnackbarLayout)?.addView(ProgressBar(activity))
        bar.show()
        return bar
    }
    
    /**
     * Enhanced mime type detection that falls back to file extension detection
     * for file URIs where ContentResolver might not have mime type info.
     */
    private fun getMimeTypeWithFallback(context: Context, uri: Uri, filePath: String?): String {
        // First try the standard way
        val standardMimeType = Utility.getMimeType(context, uri)
        if (!standardMimeType.isNullOrEmpty()) {
            return standardMimeType
        }
        
        // Fallback to file extension detection
        val extension = when {
            filePath != null -> File(filePath).extension
            uri.path != null -> File(uri.path!!).extension
            else -> null
        }
        
        return when (extension?.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            else -> {
                AppLogger.w("Unknown file extension '$extension' for URI: $uri")
                "application/octet-stream" // Generic binary type
            }
        }
    }
}