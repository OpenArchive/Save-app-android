package net.opendasharchive.openarchive.features.main

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.ActivityMainBinding
import net.opendasharchive.openarchive.databinding.PopupFolderOptionsBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.extensions.getMeasurments
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asUiImage
import net.opendasharchive.openarchive.features.core.asUiText
import net.opendasharchive.openarchive.features.core.dialog.ButtonData
import net.opendasharchive.openarchive.features.core.dialog.DialogConfig
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.core.dialog.showInfoDialog
import net.opendasharchive.openarchive.features.folders.AddFolderActivity
import net.opendasharchive.openarchive.features.main.adapters.FolderDrawerAdapter
import net.opendasharchive.openarchive.features.main.adapters.FolderDrawerAdapterListener
import net.opendasharchive.openarchive.features.main.adapters.SpaceDrawerAdapter
import net.opendasharchive.openarchive.features.main.adapters.SpaceDrawerAdapterListener
import net.opendasharchive.openarchive.features.media.AddMediaDialogFragment
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerFragment
import net.opendasharchive.openarchive.features.media.MediaLaunchers
import net.opendasharchive.openarchive.features.media.Picker
import net.opendasharchive.openarchive.features.media.PreviewActivity
import net.opendasharchive.openarchive.features.onboarding.Onboarding23Activity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.features.onboarding.StartDestination
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.services.snowbird.SnowbirdBridge
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService
import net.opendasharchive.openarchive.upload.UploadManagerFragment
import net.opendasharchive.openarchive.upload.UploadService
import net.opendasharchive.openarchive.util.InAppReviewHelper
import net.opendasharchive.openarchive.util.PermissionManager
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.ProofModeHelper
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.applyEdgeToEdgeInsets
import net.opendasharchive.openarchive.util.extensions.cloak
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.scaleAndTintDrawable
import net.opendasharchive.openarchive.util.extensions.show
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.NumberFormat


class MainActivity : BaseActivity(), SpaceDrawerAdapterListener, FolderDrawerAdapterListener {

    private val appConfig by inject<AppConfig>()
    private val viewModel by viewModel<MainViewModel>()

    private var mMenuDelete: MenuItem? = null

    private var mSnackBar: Snackbar? = null

    var uploadManagerFragment: UploadManagerFragment? = null

    private lateinit var binding: ActivityMainBinding
    private lateinit var mPagerAdapter: ProjectAdapter
    private lateinit var mSpaceAdapter: SpaceDrawerAdapter
    private lateinit var mFolderAdapter: FolderDrawerAdapter

    private lateinit var mediaLaunchers: MediaLaunchers

    private var mSelectedPageIndex: Int = 0
    private var mSelectedMediaPageIndex: Int = 0
    private var serverListOffset: Float = 0F
    private var serverListCurOffset: Float = 0F

    private var selectModeToggle: Boolean = false
    private var currentSelectionCount = 0

    private enum class FolderBarMode { INFO, SELECTION, EDIT }

    // Hold the current mode (default to INFO)
    private var folderBarMode = FolderBarMode.INFO

    // Current page getter/setter (updates bottom navbar accordingly)
    private var mCurrentPagerItem
        get() = binding.contentMain.pager.currentItem
        set(value) {
            binding.contentMain.pager.currentItem = value
            updateBottomNavbar(value)
        }

    // ----- Activity Result Launchers -----
    private val mNewFolderResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                refreshProjects(it.data?.getLongExtra(AddFolderActivity.EXTRA_FOLDER_ID, -1))
            }
        }

    private lateinit var permissionManager: PermissionManager

    private lateinit var reviewManager: ReviewManager
    private var shouldPromptReview = false

    override fun onCreate(savedInstanceState: Bundle?) {
//        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
//        WindowCompat.setDecorFitsSystemWindows(window, false)
        installSplashScreen()



//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            window.insetsController?.let {
//                it.hide(WindowInsets.Type.statusBars())
//                it.hide(WindowInsets.Type.systemBars())
//                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//            }
//        } else {
//            // For older versions, use the deprecated approach
//            window.setFlags(
//                WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN
//            )
//        }
//
//        window.apply {
//            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//            statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.colorPrimary)
//            // optional. if you want the icons to be light.
//            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//        }


        binding = ActivityMainBinding.inflate(layoutInflater)


//        binding.contentMain.imgLogo.applyEdgeToEdgeInsets { insets ->
//            leftMargin = insets.left
//            rightMargin = insets.right
//        }

        binding.contentMain.bottomNavBar.applyEdgeToEdgeInsets { insets ->
            bottomMargin = insets.bottom
        }

        binding.btnAddFolder.applyEdgeToEdgeInsets { insets ->
            bottomMargin = insets.bottom
        }

        binding.drawerContent.applyEdgeToEdgeInsets { insets ->
            bottomMargin = insets.bottom
        }


        setContentView(binding.root)

        // Initialize the permission manager with this activity and its dialogManager.
        permissionManager = PermissionManager(this, dialogManager)

        // Initialize In App Ratings Helper
        InAppReviewHelper.init(this)

        initMediaLaunchers()
        setupToolbarAndPager()
        setupNavigationDrawer()
        setupBottomNavBar()
        setupFolderBar()
        setupBottomSheetObserver()


        if (appConfig.isDwebEnabled) {
            permissionManager.checkNotificationPermission {
                AppLogger.i("Notification permission granted")
            }
            SnowbirdBridge.getInstance().initialize()
            startForegroundService(Intent(this, SnowbirdService::class.java))
            handleIntent(intent)
        }


        if (BuildConfig.DEBUG) {
            binding.contentMain.imgLogo.setOnLongClickListener {
                startActivity(Intent(this, HomeActivity::class.java))
                true
            }
        }

        supportFragmentManager.setFragmentResultListener("uploadRetry", this) { key, bundle ->
            val mediaId = bundle.getLong("mediaId")
            // Now you know which media item is being retried.
            // You can start the upload service or update the UI accordingly.
            UploadService.startUploadService(this)
        }

        supportFragmentManager.setFragmentResultListener(
            ContentPickerFragment.KEY_DISMISS,
            this
        ) { _, _ ->
            // when the sheet goes away, show your arrow
            getCurrentMediaFragment()?.setArrowVisible(true)
        }

        reviewManager = ReviewManagerFactory.create(this)
        InAppReviewHelper.requestReviewInfo(this)
        shouldPromptReview = InAppReviewHelper.onAppLaunched()
    }

    override fun onResume() {
        super.onResume()
        if (!Prefs.didCompleteOnboarding) {
            startActivity(Intent(this, Onboarding23Activity::class.java))
        }
        AppLogger.i("MainActivity onResume is called.......")
        refreshSpace()
        mCurrentPagerItem = mSelectedPageIndex
        importSharedMedia(intent)
        if (serverListOffset == 0F) {
            val dims = binding.rvSpaces.getMeasurments()
            serverListOffset = -dims.second.toFloat()
            serverListCurOffset = serverListOffset
        }

        // ─────────────────────────────────────────────────────────────────────────
        // Only now, after UI is ready, do we fire the in‐app review if needed.
        if (shouldPromptReview) {
            lifecycleScope.launch(Dispatchers.Main) {
                // Wait a small delay so we don’t interrupt initial load (e.g. 2 seconds).
                delay(2_000)
                InAppReviewHelper.showReviewIfPossible(this@MainActivity, reviewManager)
                InAppReviewHelper.markReviewDone()
                shouldPromptReview = false
            }
        }
        // ─────────────────────────────────────────────────────────────────────────
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onStart() {
        super.onStart()

        ProofModeHelper.init(this) {
            // Check for any queued uploads and restart, only after ProofMode is correctly initialized.
            UploadService.startUploadService(this)
        }
    }

    // ----- Initialization Methods -----
    private fun initMediaLaunchers() {
        mediaLaunchers = Picker.register(
            activity = this,
            root = binding.root,
            project = { getSelectedProject() },
            completed = { media ->
                refreshCurrentProject()
                if (media.isNotEmpty()) navigateToPreview()
            }
        )
    }

    private fun setupToolbarAndPager() {
        setSupportActionBar(binding.contentMain.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            title = null
        }

        mPagerAdapter = ProjectAdapter(supportFragmentManager, lifecycle)
        binding.contentMain.pager.adapter = mPagerAdapter

        binding.contentMain.pager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                mSelectedPageIndex = position
                if (position < mPagerAdapter.settingsIndex) {
                    mSelectedMediaPageIndex = position
                    val selectedProject = getSelectedProject()
                    mFolderAdapter.updateSelectedProject(selectedProject)
                }
                if (!appConfig.multipleProjectSelectionMode) {
                    getCurrentMediaFragment()?.cancelSelection()
                }
                updateBottomNavbar(position)
                refreshCurrentProject()
            }
        })
    }

    private fun setupNavigationDrawer() {
        // Drawer listener resets state on close
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerClosed(drawerView: View) {
                collapseSpacesList()
            }

            override fun onDrawerOpened(drawerView: View) {
                //
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                //
            }

            override fun onDrawerStateChanged(newState: Int) {
                //
            }
        })

        binding.navigationDrawerHeader.setOnClickListener { toggleSpacesList() }
        binding.dimOverlay.setOnClickListener { collapseSpacesList() }

        mSpaceAdapter = SpaceDrawerAdapter(this)
        binding.rvSpaces.layoutManager = LinearLayoutManager(this)
        binding.rvSpaces.adapter = mSpaceAdapter

        mFolderAdapter = FolderDrawerAdapter(this)
        binding.rvFolders.layoutManager = LinearLayoutManager(this)
        binding.rvFolders.adapter = mFolderAdapter

        binding.btnAddFolder.scaleAndTintDrawable(Position.Start, 0.75)
        binding.btnAddFolder.setOnClickListener {
            closeDrawer()
            navigateToAddFolder()
        }

        updateCurrentSpaceAtDrawer()
    }

    private fun setupBottomNavBar() {
        with(binding.contentMain.bottomNavBar) {
            onMyMediaClick = {
                mCurrentPagerItem = mSelectedMediaPageIndex
            }
            onAddClick = { addClicked(AddMediaType.GALLERY) }
            onSettingsClick = {
                mCurrentPagerItem = mPagerAdapter.settingsIndex
            }

            if (Picker.canPickFiles(this@MainActivity)) {
                setAddButtonLongClickEnabled()
                onAddLongClick = {
                    if (Space.current == null) {
                        navigateToAddServer()
                    } else if (getSelectedProject() == null) {
                        navigateToAddFolder()
                    } else {
                        getCurrentMediaFragment()?.setArrowVisible(false)
                        val addMediaBottomSheet =
                            ContentPickerFragment { actionType -> addClicked(actionType) }
                        addMediaBottomSheet.show(supportFragmentManager, ContentPickerFragment.TAG)
                    }
                }
                supportFragmentManager.setFragmentResultListener(
                    AddMediaDialogFragment.RESP_TAKE_PHOTO, this@MainActivity
                ) { _, _ -> addClicked(AddMediaType.CAMERA) }
                supportFragmentManager.setFragmentResultListener(
                    AddMediaDialogFragment.RESP_PHOTO_GALLERY, this@MainActivity
                ) { _, _ -> addClicked(AddMediaType.GALLERY) }
                supportFragmentManager.setFragmentResultListener(
                    AddMediaDialogFragment.RESP_FILES, this@MainActivity
                ) { _, _ -> addClicked(AddMediaType.FILES) }
            }
        }
    }

    private fun setupFolderBar() {
        // Tapping the edit button shows the folder options popup.
        binding.contentMain.btnEdit.setOnClickListener { btnView ->
            val location = IntArray(2)
            binding.contentMain.btnEdit.getLocationOnScreen(location)
            val point = Point(location[0], location[1])
            showFolderOptionsPopup(point)
        }
        // In selection mode, cancel selection reverts to INFO mode.
        binding.contentMain.btnCancelSelection.setOnClickListener {
            setFolderBarMode(FolderBarMode.INFO)
            getCurrentMediaFragment()?.cancelSelection()
        }
        // In the edit (rename) container, cancel button reverts to INFO mode.
        binding.contentMain.btnCancelEdit.setOnClickListener {
            setFolderBarMode(FolderBarMode.INFO)
        }
        // Listen for the "done" action to commit a rename.
        binding.contentMain.etFolderName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = binding.contentMain.etFolderName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameCurrentFolder(newName)
                    setFolderBarMode(FolderBarMode.INFO)
                } else {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.folder_empty_warning),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                // Hide the keyboard
                val imm =
                    binding.contentMain.etFolderName.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.contentMain.etFolderName.windowToken, 0)

                // Remove focus from the EditText
                binding.contentMain.etFolderName.clearFocus()

                true
            } else false
        }

        binding.contentMain.btnRemoveSelected.setOnClickListener {
            showDeleteSelectedMediaConfirmDialog()
        }
    }

    // Called when a new folder name is confirmed. (Adjust as needed to update your data store.)
    private fun renameCurrentFolder(newName: String) {
        val project = getSelectedProject()
        project?.let {
            it.description = newName
            it.save()
            refreshCurrentProject()
            Snackbar.make(binding.root, getString(R.string.folder_rename_success), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showFolderOptionsPopup(p: Point) {
        val layoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupFolderOptionsBinding.inflate(layoutInflater)
        val popup = PopupWindow(this).apply {
            contentView = popupBinding.root
            width = LinearLayout.LayoutParams.WRAP_CONTENT
            height = LinearLayout.LayoutParams.WRAP_CONTENT
            isFocusable = true
            setBackgroundDrawable(ColorDrawable())
            animationStyle = R.style.popup_window_animation
        }

        // Check if there is at least one media item in the selected project
        val hasMedia = getSelectedProject()?.collections?.any { it.media.isNotEmpty() } == true

        // Disable select media if no media in current folder
        popupBinding.menuFolderBarSelectMedia.isEnabled = hasMedia
        popupBinding.menuFolderBarSelectMedia.alpha = if (hasMedia) 1.0f else 0.4f

        // Option to toggle selection mode
        popupBinding.menuFolderBarSelectMedia.setOnClickListener {
            popup.dismiss()
            setFolderBarMode(FolderBarMode.SELECTION)
        }

        // Rename folder
        popupBinding.menuFolderBarRenameFolder.setOnClickListener {
            popup.dismiss()
            setFolderBarMode(FolderBarMode.EDIT)
        }

        // Archive folder
        popupBinding.menuFolderBarArchiveFolder.setOnClickListener {
            popup.dismiss()
            val selectedProject = getSelectedProject()
            if (selectedProject != null) {
                selectedProject.isArchived = !selectedProject.isArchived
                selectedProject.save()
                refreshProjects()
                updateCurrentFolderVisibility()
                refreshCurrentProject()
                Snackbar.make(binding.root, getString(R.string.folder_archived), Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, getString(R.string.folder_not_found), Snackbar.LENGTH_LONG).show()
            }
        }

        // Remove folder
        popupBinding.menuFolderBarRemove.setOnClickListener {
            popup.dismiss()
            if (getSelectedProject() != null) {
                showDeleteFolderConfirmDialog()
            } else {
                Snackbar.make(binding.root, getString(R.string.folder_not_found), Snackbar.LENGTH_LONG).show()
            }
        }

        // Adjust popup position if needed
        val x = 200
        val y = 60
        popup.showAtLocation(binding.root, Gravity.NO_GRAVITY, p.x + x, p.y + y)
    }

    fun setSelectionMode(isSelecting: Boolean) {
        if (isSelecting) {
            setFolderBarMode(FolderBarMode.SELECTION)
        } else {
            setFolderBarMode(FolderBarMode.INFO)
        }
    }

    // New helper: update the cancel selection TextView to show the number of selected items.
    fun updateSelectedCount(count: Int) {
        // For example, if count > 0 display “Selected: X”; otherwise, revert to “Select Media”.
        //binding.contentMain.tvSelectedCount.text = if (count > 0) "Selected: $count" else "Select Media"
    }

    private fun showDeleteSelectedMediaConfirmDialog() {
        dialogManager.showDialog(
            config = DialogConfig(
                type = DialogType.Warning,
                title = R.string.menu_delete.asUiText(),
                message = R.string.menu_delete_desc.asUiText(),
                icon = UiImage.DrawableResource(R.drawable.ic_trash),
                positiveButton = ButtonData(
                    text = R.string.lbl_ok.asUiText(),
                    action = {
                        getCurrentMediaFragment()?.deleteSelected()
                        updateSelectedCount(0)
                        refreshCurrentFolderCount()
                    }
                ),
                neutralButton =
                    ButtonData(
                        text = UiText.StringResource(R.string.lbl_Cancel),
                        action = {

                        }
                    )
            )
        )
    }

    private fun showDeleteFolderConfirmDialog() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Error
            icon = UiImage.DrawableResource(R.drawable.ic_trash)
            title = UiText.StringResource(R.string.remove_from_app)
            message = UiText.StringResource(R.string.action_remove_project)
            destructiveButton {
                text = UiText.StringResource(R.string.remove)
                action = {
                    getSelectedProject()?.delete()
                    refreshProjects()
                    updateCurrentFolderVisibility()
                    refreshCurrentProject()
                    Snackbar.make(binding.root, getString(R.string.folder_removed), Snackbar.LENGTH_SHORT).show()
                }
            }
            neutralButton {
                text = UiText.StringResource(R.string.lbl_Cancel)
                action = {
                    dialogManager.dismissDialog()
                }
            }
        }
    }

    private fun getCurrentMediaFragment(): MainMediaFragment? {
        val currentItem = binding.contentMain.pager.currentItem
        return supportFragmentManager.findFragmentByTag("f$currentItem") as? MainMediaFragment
    }


    // ----- Drawer Helpers -----
    private fun toggleDrawerState() {
        if (binding.drawerLayout.isDrawerOpen(binding.drawerContent)) {
            closeDrawer()
        } else {
            openDrawer()
        }
    }

    private fun openDrawer() {
        binding.drawerLayout.openDrawer(binding.drawerContent)
    }

    private fun closeDrawer() {
        binding.drawerLayout.closeDrawer(binding.drawerContent)
    }

    private fun toggleSpacesList() {
        if (serverListCurOffset == serverListOffset) {
            expandSpacesList()
        } else {
            collapseSpacesList()
        }
    }

    private fun expandSpacesList() {
        serverListCurOffset = 0f
        binding.spaceListMore.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_expand_less)
        )
        binding.rvSpaces.visibility = View.VISIBLE
        binding.dimOverlay.visibility = View.VISIBLE
        binding.rvSpaces.bringToFront()
        binding.dimOverlay.bringToFront()
        binding.rvSpaces.animate()
            .translationY(0f).alpha(1f).setDuration(200)
            .withStartAction {
                binding.spacesHeaderSeparator.alpha = 0.3f
                binding.rvFolders.alpha = 0.3f
                binding.btnAddFolder.alpha = 0.3f
            }
        binding.dimOverlay.animate().alpha(1f).setDuration(200)
        binding.navigationDrawerHeader.elevation = 8f
    }

    private fun collapseSpacesList() {
        serverListCurOffset = serverListOffset
        binding.spaceListMore.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_expand_more)
        )

        binding.rvSpaces.animate()
            .translationY(serverListOffset).alpha(0f).setDuration(200)
            .withEndAction {
                binding.rvSpaces.visibility = View.GONE
                binding.dimOverlay.visibility = View.GONE
                binding.spacesHeaderSeparator.alpha = 1f
                binding.rvFolders.alpha = 1f
                binding.btnAddFolder.alpha = 1f
            }
        binding.dimOverlay.animate().alpha(0f).setDuration(200)
        binding.navigationDrawerHeader.elevation = 0f
    }

    private fun updateCurrentSpaceAtDrawer() {
        Space.current?.setAvatar(binding.currentSpaceIcon)
        mSpaceAdapter.notifyDataSetChanged()

        if (Space.current == null) {
            binding.btnAddFolder.visibility = View.INVISIBLE
        } else {
            binding.btnAddFolder.visibility = View.VISIBLE
        }
    }

    // ----- Refresh & Update Methods -----
    /**
     * Updates the visibility of the current folder container.
     * The container is only visible if:
     *   1. We are not on the settings page AND
     *   2. There is a current space with at least one project.
     */
    // Central function to update folder bar state
    private fun setFolderBarMode(mode: FolderBarMode) {
        folderBarMode = mode
        when (mode) {
            FolderBarMode.INFO -> {
                binding.contentMain.folderInfoContainer.visibility = View.VISIBLE
                binding.contentMain.folderSelectionContainer.visibility = View.GONE
                binding.contentMain.folderEditContainer.visibility = View.GONE

                if (Space.current != null) {
                    if (Space.current?.projects?.isNotEmpty() == true) {
                        binding.contentMain.folderInfoContainerRight.visibility = View.VISIBLE
                    } else {
                        binding.contentMain.folderInfoContainerRight.visibility = View.INVISIBLE
                    }
                } else {
                    binding.contentMain.folderInfoContainerRight.visibility = View.INVISIBLE
                }
            }

            FolderBarMode.SELECTION -> {
                binding.contentMain.folderInfoContainer.visibility = View.GONE
                binding.contentMain.folderSelectionContainer.visibility = View.VISIBLE
                binding.contentMain.folderEditContainer.visibility = View.GONE
            }

            FolderBarMode.EDIT -> {
                binding.contentMain.folderInfoContainer.visibility = View.GONE
                binding.contentMain.folderSelectionContainer.visibility = View.GONE
                binding.contentMain.folderEditContainer.visibility = View.VISIBLE
                // Prepopulate the rename field with the current folder name
                binding.contentMain.etFolderName.setText(getSelectedProject()?.description ?: "")
                binding.contentMain.etFolderName.requestFocus()
                binding.contentMain.etFolderName.selectAll()

                // Show the keyboard
                val imm =
                    binding.contentMain.etFolderName.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(
                    binding.contentMain.etFolderName,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
    }

    private fun updateCurrentFolderVisibility() {
        val currentPagerIndex = binding.contentMain.pager.currentItem
        val settingsIndex = mPagerAdapter.settingsIndex
        if (currentPagerIndex == settingsIndex) {
            binding.contentMain.folderBar.hide()
            // Reset to default mode
            setFolderBarMode(FolderBarMode.INFO)

            // Force ViewPager2 to re-measure its layout after visibility change
            binding.contentMain.pager.post {
                binding.contentMain.pager.requestLayout()
            }
        } else {
            binding.contentMain.folderBar.show()
            setFolderBarMode(FolderBarMode.INFO)
        }

        mFolderAdapter.notifyDataSetChanged()
    }

    private fun updateBottomNavbar(position: Int) {
        val isSettings = position == mPagerAdapter.settingsIndex
        binding.contentMain.bottomNavBar.updateSelectedItem(isSettings = isSettings)
        updateCurrentFolderVisibility()
        invalidateOptionsMenu()
    }

    private fun refreshSpace() {
        val currentSpace = Space.current
        if (currentSpace != null) {
            binding.spaceNameLayout.visibility = View.VISIBLE
            binding.currentSpaceName.text = currentSpace.friendlyName
            updateCurrentSpaceAtDrawer()
            currentSpace.setAvatar(binding.contentMain.spaceIcon)
        } else {
            binding.contentMain.spaceIcon.visibility = View.INVISIBLE
            binding.spaceNameLayout.visibility = View.INVISIBLE
        }
        refreshSpaceListAtDrawer()
        updateCurrentSpaceAtDrawer()
        refreshProjects()
        refreshCurrentProject()
        updateCurrentFolderVisibility()
    }

    private fun refreshSpaceListAtDrawer() {
        val spaces = Space.getAll().asSequence().toList()
        val hasDwebGroups = SnowbirdGroup.getAll().isNotEmpty()
        mSpaceAdapter.update(spaces, hasDwebGroups)
    }

    private fun refreshProjects(setProjectId: Long? = null) {
        val projects = Space.current?.projects ?: emptyList()
        mPagerAdapter.updateData(projects)
        binding.contentMain.pager.adapter = mPagerAdapter

        setProjectId?.let {
            mCurrentPagerItem = mPagerAdapter.getProjectIndexById(it, default = 0)
        }
        mFolderAdapter.update(projects)
    }

    private fun refreshCurrentProject() {
        val project = getSelectedProject()

        if (project != null) {
            binding.contentMain.pager.post {
                mPagerAdapter.notifyProjectChanged(project)
            }
            binding.contentMain.folderInfoContainer.visibility = View.VISIBLE
            project.space?.setAvatar(binding.contentMain.spaceIcon)
            binding.contentMain.folderName.text = project.description
            binding.contentMain.folderNameArrow.visibility = View.VISIBLE
            binding.contentMain.folderName.visibility = View.VISIBLE
        } else {
            binding.contentMain.folderNameArrow.visibility = View.INVISIBLE
            binding.contentMain.folderName.visibility = View.INVISIBLE
        }
        updateCurrentFolderVisibility()
        refreshCurrentFolderCount()
    }

    private fun refreshCurrentFolderCount() {
        val project = getSelectedProject()

        if (project != null) {
            val count = project.collections.map { it.size }
                .reduceOrNull { acc, count -> acc + count } ?: 0
            binding.contentMain.itemCount.text = NumberFormat.getInstance().format(count)
            if (!selectModeToggle) {
                binding.contentMain.itemCount.show()
            }
        } else {
            binding.contentMain.itemCount.cloak()
        }
    }

    // ----- Navigation & Media Handling -----
    private fun navigateToAddServer() {
        closeDrawer()
        startActivity(Intent(this, SpaceSetupActivity::class.java))
    }

    private fun navigateToAddFolder() {
        val intent = Intent(this, SpaceSetupActivity::class.java)
        if (Space.current?.tType == Space.Type.INTERNET_ARCHIVE) {
            // We cannot browse the Internet Archive. Directly forward to creating a project,
            // as it doesn't make sense to show a one-option menu.
            intent.putExtra("start_destination", StartDestination.ADD_NEW_FOLDER.name)
        } else {
            intent.putExtra("start_destination", StartDestination.ADD_FOLDER.name)
        }
        mNewFolderResultLauncher.launch(intent)
//        mNewFolderResultLauncher.launch(Intent(this, AddFolderActivity::class.java))
    }

    private fun addClicked(mediaType: AddMediaType) {

        when {
            getSelectedProject() != null -> {
                if (Prefs.addMediaHint) {
                    when (mediaType) {
                        AddMediaType.CAMERA -> {
                            permissionManager.checkCameraPermission {
                                Picker.takePhotoModern(this@MainActivity, mediaLaunchers.modernCameraLauncher)
                            }
                        }

                        AddMediaType.GALLERY -> {
                            permissionManager.checkMediaPermissions {
                                Picker.pickMedia(mediaLaunchers.galleryLauncher)
                            }
                        }

                        AddMediaType.FILES -> Picker.pickFiles(mediaLaunchers.filePickerLauncher)
                    }
                } else {
                    dialogManager.showInfoDialog(
                        icon = R.drawable.ic_media_new.asUiImage(),
                        title = R.string.press_and_hold_options_media_screen_title.asUiText(),
                        message = R.string.press_and_hold_options_media_screen_message.asUiText(),
                        onDone = {
                            Prefs.addMediaHint = true
                            addClicked(mediaType)
                        }
                    )
                }
            }

            Space.current == null -> navigateToAddServer()
            else -> {
                navigateToAddFolder()
            }
        }
    }

    private fun importSharedMedia(imageIntent: Intent?) {
        if (imageIntent?.action != Intent.ACTION_SEND) return
        val uri =
            imageIntent.data ?: imageIntent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        val path = uri?.path ?: return
        if (path.contains(packageName)) return

        mSnackBar?.show()
        lifecycleScope.launch(Dispatchers.IO) {
            //When we are sharing a file to be uploaded to Save app we don't generate proof.
            val media = Picker.import(this@MainActivity, getSelectedProject(), uri, false)
            lifecycleScope.launch(Dispatchers.Main) {
                mSnackBar?.dismiss()
                intent = null
                if (media != null) {
                    navigateToPreview()
                }
            }
        }
    }

    private fun navigateToPreview() {
        val projectId = getSelectedProject()?.id ?: return
        PreviewActivity.start(this, projectId)
    }

    // ----- Permissions & Intent Handling -----
    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.takeIf { it.scheme == "save-veilid" }?.let { processUri(it) }
        }
    }

    private fun processUri(uri: Uri) {
        val path = uri.path
        val queryParams = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
        AppLogger.d("Path: $path, QueryParams: $queryParams")
    }

    // ----- Overrides -----
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val hasDwebGroup = SnowbirdGroup.getAll().isNotEmpty()
        val shouldShowSideMenu =
            ((Space.current != null || hasDwebGroup) && mCurrentPagerItem != mPagerAdapter.settingsIndex)

        menu?.findItem(R.id.menu_folders)?.apply {
            isVisible = shouldShowSideMenu
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_folders -> {
                toggleDrawerState()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // ----- Adapter Listeners -----
    override fun onProjectSelected(project: Project) {
        binding.drawerLayout.closeDrawer(binding.drawerContent)
        mCurrentPagerItem = mPagerAdapter.projects.indexOf(project)
    }

    override fun getSelectedProject(): Project? {
        return mPagerAdapter.getProject(mCurrentPagerItem)
    }

    override fun onSpaceSelected(space: Space) {
        Space.current = space
        refreshSpace()
        updateCurrentSpaceAtDrawer()
        collapseSpacesList()
        binding.drawerLayout.closeDrawer(binding.drawerContent)
    }

    override fun onAddNewSpace() {
        collapseSpacesList()
        closeDrawer()
        val intent = Intent(this, SpaceSetupActivity::class.java)
        startActivity(intent)
    }

    override fun getSelectedSpace(): Space? {
        val currentSpace = Space.current
        AppLogger.i("current space requested by adapter... = $currentSpace")
        return Space.current
    }

    override fun onNavigateToDwebGroups() {
        collapseSpacesList()
        closeDrawer()
        val intent = Intent(this, SpaceSetupActivity::class.java)
        intent.putExtra("start_destination", StartDestination.DWEB_DASHBOARD.name)
        startActivity(intent)
    }

    /**
     * Show the UploadManagerFragment as a Bottom Sheet.
     * Ensures we only show one instance.
     */
    fun showUploadManagerFragment() {
        if (uploadManagerFragment == null) {
            uploadManagerFragment = UploadManagerFragment()
            uploadManagerFragment?.show(supportFragmentManager, UploadManagerFragment.TAG)

            // Stop the upload service when the bottom sheet is shown
            UploadService.stopUploadService(this)
        }
    }

    /**
     * Setup a listener to detect when the UploadManagerFragment is dismissed.
     * If there are pending uploads, restart the UploadService.
     */
    private fun setupBottomSheetObserver() {
        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            if (fragment is UploadManagerFragment) {
                uploadManagerFragment = fragment

                // Observe when it gets dismissed
                fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        uploadManagerFragment = null // Clear reference

                        // Check if there are pending uploads
                        if (Media.getByStatus(
                                listOf(Media.Status.Queued, Media.Status.Uploading),
                                Media.ORDER_PRIORITY
                            ).isNotEmpty()
                        ) {
                            UploadService.startUploadService(this@MainActivity)
                        }
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear pending callbacks/messages
        window?.decorView?.handler?.removeCallbacksAndMessages(null)
    }

    companion object {
        // Define request codes
        const val REQUEST_CAMERA_PERMISSION = 100
        const val REQUEST_FILE_MEDIA = 101
    }
}
