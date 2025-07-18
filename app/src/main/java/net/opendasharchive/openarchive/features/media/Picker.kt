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
import java.util.Date

object Picker {

    private var currentPhotoUri: Uri? = null

    fun register(
        activity: ComponentActivity,
        root: View,
        project: () -> Project?,
        completed: (List<Media>) -> Unit
    ): MediaLaunchers {

        // Official Gallery Picker (Replaces custom image picker)
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

        val fpl = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        val cpl = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        return MediaLaunchers(
            galleryLauncher = galleryLauncher, // Updated
            filePickerLauncher = fpl,
            cameraLauncher = cpl
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
        type = "application/*"
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
        @Suppress("NAME_SHADOWING")
        val project = project ?: return null

        val title = Utility.getUriDisplayName(context, uri) ?: ""
        val file = Utility.getOutputMediaFileByCache(context, title)

        if (!Utility.writeStreamToFile(context.contentResolver.openInputStream(uri), file)) {
            return null
        }

        // create media
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
        media.mimeType = Utility.getMimeType(context, uri) ?: ""
        media.createDate = createDate
        media.updateDate = media.createDate
        media.sStatus = Media.Status.Local
        //We generate hash regardless if proof is on or off because we don't want unexpected behaviour when we are looking for proof files when uploaded later.
        media.mediaHashString = HashUtils.getSHA256FromFileContent(context.contentResolver.openInputStream(uri))
        media.projectId = project.id
        media.title = title
        media.save()
        if (generateProof && Prefs.useProofMode) {
            //If Proof mode is on we need this to be on always
            Prefs.proofModeLocation = true
            Prefs.proofModeNetwork = true
            //Generate proof for camera photos and also always track location
            ProofMode.generateProof(context, uri, media.mediaHashString)
        } else {
            Timber.w("Skipping proof generation")
        }
        return media
    }

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

    @SuppressLint("RestrictedApi")
    private fun showProgressSnackBar(activity: Activity, root: View, message: String): Snackbar {
        val bar = root.makeSnackBar(message)
        (bar.view as? Snackbar.SnackbarLayout)?.addView(ProgressBar(activity))
        bar.show()
        return bar
    }
}