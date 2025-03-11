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
import android.provider.MediaStore
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.esafirm.imagepicker.features.ImagePickerConfig
import com.esafirm.imagepicker.features.ImagePickerLauncher
import com.esafirm.imagepicker.features.ImagePickerMode
import com.esafirm.imagepicker.features.ImagePickerSavePath
import com.esafirm.imagepicker.features.ReturnMode
import com.esafirm.imagepicker.features.registerImagePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.features.main.MainActivity.Companion.REQUEST_CAMERA_PERMISSION
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

        val mpl = activity.registerImagePicker { result ->

            val snackbar = showProgressSnackBar(activity, root, activity.getString(R.string.importing_media))

            activity.lifecycleScope.launch(Dispatchers.IO) {
                val media = import(activity, project(), result.map { it.uri })

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
                val files = import(activity, project(), listOf(uri))

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
                        val media = import(activity, project(), listOf(uri))

                        activity.lifecycleScope.launch(Dispatchers.Main) {
                            snackbar.dismiss()
                            completed(media)
                        }
                    }
                }
            }
        }

        return MediaLaunchers(
            imagePickerLauncher = mpl,
            filePickerLauncher = fpl,
            cameraLauncher = cpl
        )
    }

    fun pickMedia(activity: Activity, launcher: ImagePickerLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (needAskForPermission(
                    activity,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO),
                MainActivity.REQUEST_FILE_MEDIA
            )
            ) {
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
            savePath = ImagePickerSavePath(Environment.getExternalStorageDirectory().path, false)
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

    private fun needAskForPermission(activity: Activity, permissions: Array<String>, requestCode: Int): Boolean {
        var needAsk = false

        for (permission in permissions) {
            needAsk = ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

            if (needAsk) break
        }

        if (!needAsk) return false

        ActivityCompat.requestPermissions(activity, permissions, requestCode)

        return true
    }

    private fun import(context: Context, project: Project?, uris: List<Uri>): ArrayList<Media> {
        val result = ArrayList<Media>()

        for (uri in uris) {
            try {
                val media = import(context, project, uri)
                if (media != null) result.add(media)
            } catch (e: Exception) {
                AppLogger.e( "Error importing media", e)
            }
        }

        return result
    }

    fun import(context: Context, project: Project?, uri: Uri): Media? {
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
        media.mediaHashString =
            HashUtils.getSHA256FromFileContent(context.contentResolver.openInputStream(uri))
        media.projectId = project.id
        media.title = title
        media.save()

        return media
    }

    fun takePhoto(activity: Activity, launcher: ActivityResultLauncher<Intent>) {

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return
        }

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