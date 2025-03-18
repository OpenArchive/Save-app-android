package net.opendasharchive.openarchive.features.media

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.esafirm.imagepicker.features.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.util.Utility
import net.opendasharchive.openarchive.util.extensions.makeSnackBar
import org.witness.proofmode.crypto.HashUtils
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

        val imagePickerLauncher = activity.registerImagePicker { result ->
            val snackbar = showProgressSnackBar(activity, root, activity.getString(R.string.importing_media))

            activity.lifecycleScope.launch(Dispatchers.IO) {
                val mediaList = import(activity, project(), result.map { it.uri })
                activity.lifecycleScope.launch(Dispatchers.Main) {
                    snackbar.dismiss()
                    completed(mediaList)
                }
            }
        }

        val filePickerLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != AppCompatActivity.RESULT_OK) return@registerForActivityResult

            val uri = result.data?.data ?: return@registerForActivityResult
            val snackbar = showProgressSnackBar(activity, root, activity.getString(R.string.importing_media))

            activity.lifecycleScope.launch(Dispatchers.IO) {
                val mediaList = import(activity, project(), listOf(uri))
                activity.lifecycleScope.launch(Dispatchers.Main) {
                    snackbar.dismiss()
                    completed(mediaList)
                }
            }
        }

        val cameraLauncher = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri?.let { uri ->
                    val snackbar = showProgressSnackBar(activity, root, activity.getString(R.string.importing_media))
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val mediaList = import(activity, project(), listOf(uri))
                        activity.lifecycleScope.launch(Dispatchers.Main) {
                            snackbar.dismiss()
                            completed(mediaList)
                        }
                    }
                }
            }
        }

        return MediaLaunchers(imagePickerLauncher, filePickerLauncher, cameraLauncher)
    }

    fun pickMedia(activity: Activity, launcher: ImagePickerLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasMediaPermissions(activity)) {
                return
            }
        }

        val config = ImagePickerConfig {
            mode = ImagePickerMode.MULTIPLE
            isShowCamera = false
            returnMode = ReturnMode.NONE
            isFolderMode = true
            isIncludeVideo = true
            arrowColor = Color.WHITE
            limit = 99
            savePath = ImagePickerSavePath(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath ?: "", false)
        }

        launcher.launch(config)
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

    private fun hasMediaPermissions(activity: Activity): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE) // ✅ Use for API 29-32
        }

        if (permissions.all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }) {
            return true
        }

        ActivityCompat.requestPermissions(activity, permissions, 2)
        return false
    }

    private fun import(context: Context, project: Project?, uris: List<Uri>): List<Media> {
        return uris.mapNotNull { uri ->
            try {
                import(context, project, uri)
            } catch (e: Exception) {
                AppLogger.e("Error importing media", e)
                null
            }
        }
    }

    fun import(context: Context, project: Project?, uri: Uri): Media? {
        project ?: return null

        val title = Utility.getUriDisplayName(context, uri) ?: return null
        val file = Utility.getOutputMediaFileByCache(context, title) ?: return null

        if (!Utility.writeStreamToFile(context, context.contentResolver.openInputStream(uri), file)) {
            return null
        }

        val media = Media()
        val collection = project.openCollection

        media.collectionId = collection.id

        val fileSource = uri.path?.let { File(it) }
        val createDate = fileSource?.takeIf { it.exists() }?.let { Date(it.lastModified()) } ?: Date()

        media.encryptedFilePath = file.absolutePath
        media.mimeType = Utility.getMimeType(context, uri) ?: ""
        media.createDate = createDate
        media.updateDate = createDate
        media.sStatus = Media.Status.Local
        media.mediaHashString = HashUtils.getSHA256FromFileContent(context.contentResolver.openInputStream(uri))
        media.projectId = project.id
        media.title = title
        media.save()

        return media
    }

    fun takePhoto(context: Context, launcher: ActivityResultLauncher<Uri>) {
        val file = Utility.getOutputMediaFileByCache(context, "IMG_${System.currentTimeMillis()}.jpg") ?: return

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        currentPhotoUri = uri
        launcher.launch(uri)
    }

    @SuppressLint("RestrictedApi")
    private fun showProgressSnackBar(activity: Activity, root: View, message: String): Snackbar {
        val bar = root.makeSnackBar(message)
        (bar.view as? Snackbar.SnackbarLayout)?.addView(ProgressBar(activity))
        bar.show()
        return bar
    }
}
