package net.opendasharchive.openarchive.features.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.main.ui.HomeScreen
import net.opendasharchive.openarchive.features.main.ui.HomeViewModel
import net.opendasharchive.openarchive.features.main.ui.SaveNavGraph
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.MediaLaunchers
import net.opendasharchive.openarchive.features.media.Picker
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class HomeActivity: FragmentActivity() {

    private val viewModel by viewModel<HomeViewModel>()

    // We'll hold a reference to the media launchers registered with Picker.
    private lateinit var mediaLaunchers: MediaLaunchers

    private val mNewFolderResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                //TODO: Refresh projects in MainViewModel
            }
        }

    private val folderResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedFolderId:Long? = result.data?.getLongExtra("SELECTED_FOLDER_ID", -1)
                if (selectedFolderId != null && selectedFolderId > -1) {
                    navigateToFolder(selectedFolderId)
                }
            }
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Timber.d("Able to post notifications")
        } else {
            Timber.d("Need to explain")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        // Perform any intent processing (e.g. deep-links or shared media)
        handleIntent(intent)

        // Check notification permission (for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermissions()
        }

        // Get a reference to a view to serve as the root for Snackbars, etc.
        val rootView: View = findViewById(android.R.id.content)

        // Register media launchers via Picker.
        // The lambda for 'project' should return the currently selected project.
        // For now, this stub returns null—you should wire it to your actual selection.
        mediaLaunchers = Picker.register(
            activity = this,
            root = rootView,
            project = { getCurrentProject() },
            completed = { media ->
                // For example, refresh the current project UI and preview media.
                refreshCurrentProject()
                if (media.isNotEmpty()) {
                    previewMedia()
                }
            }
        )

        // Set up your Compose UI and pass callbacks.
        setContent {
            SaveNavGraph(
                context = this@HomeActivity,
                onExit = {
                    finish()
                },
                viewModel = viewModel,
                onNewFolder = { launchNewFolder() },
                onFolderSelected = { folderId -> navigateToFolder(folderId) },
                onAddMedia = { mediaType -> addMediaClicked(mediaType) }
            )
        }
    }

    /**
     * Returns the currently selected project.
     * Replace this stub with your actual project–retrieval logic.
     */
    private fun getCurrentProject(): Project? {
        // TODO: Return your current project from a ViewModel or other state.
        return null
    }

    /**
     * Refresh UI details for the current project.
     */
    private fun refreshCurrentProject() {
        // TODO: Update your UI state, refresh fragment content, etc.
    }

    /**
     * Launch a preview after media import.
     */
    private fun previewMedia() {
        // TODO: Launch your preview activity or update the UI as needed.
    }

    /**
     * Launch the AddFolderActivity using your folder launcher.
     */
    private fun launchNewFolder() {
        // Example: startActivity(Intent(this, AddFolderActivity::class.java))
        // Or, if you have a registered launcher, use it here.
    }

    /**
     * Navigate to a folder after selection.
     */
    private fun navigateToFolder(folderId: Long) {
        // TODO: Update your navigation or fragment state to display the selected folder.
    }

    /**
     * Handle "Add Media" events from the Compose UI.
     */
    private fun addMediaClicked(mediaType: AddMediaType) {
        if (getCurrentProject() != null) {
            // If you wish to show hints or dialogs before picking media,
            // insert that logic here (e.g., check Prefs.addMediaHint).
            when (mediaType) {
                AddMediaType.CAMERA -> {
                    // Launch the camera using Picker.
                    Picker.takePhoto(this, mediaLaunchers.cameraLauncher)
                }
                AddMediaType.GALLERY -> {
                    // Launch the gallery/image picker.
                    Picker.pickMedia(this, mediaLaunchers.imagePickerLauncher)
                }
                AddMediaType.FILES -> {
                    // Launch the file picker.
                    Picker.pickFiles(mediaLaunchers.filePickerLauncher)
                }
            }
        } else {
            // If no project is selected, prompt the user to create one (e.g. add a folder).
            launchNewFolder()
        }
    }

    /**
     * Check for POST_NOTIFICATIONS permission on Android 13+.
     */
    private fun checkNotificationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Timber.d("Notification permission already granted")
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionRationale()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Show a rationale for notification permission.
     */
    private fun showNotificationPermissionRationale() {
        // TODO: Display a dialog or Snackbar explaining why notifications are needed.
        Timber.d("Showing notification permission rationale")
    }

    /**
     * Handle incoming intents for deep-linking, shared media, etc.
     */
    private fun handleIntent(intent: Intent?) {
        intent?.let { receivedIntent ->
            when (receivedIntent.action) {
                Intent.ACTION_VIEW -> {
                    val uri = receivedIntent.data
                    if (uri?.scheme == "save-veilid") {
                        processUri(uri)
                    }
                }
                // Optionally handle other actions (like ACTION_SEND) here.
            }
        }
    }

    private fun processUri(uri: Uri) {
        // Process the URI similarly to your original logic.
        Timber.d("Processing URI: $uri")
        // TODO: Extract path, query parameters, etc.
    }


}