package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityPreviewBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asUiImage
import net.opendasharchive.openarchive.features.core.asUiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import net.opendasharchive.openarchive.util.extensions.toggle

class PreviewActivity : BaseActivity(), View.OnClickListener, PreviewAdapter.Listener {

    companion object {
        private const val PROJECT_ID_EXTRA = "project_id"

        fun start(context: Context, projectId: Long) {
            val i = Intent(context, PreviewActivity::class.java)
            i.putExtra(PROJECT_ID_EXTRA, projectId)

            context.startActivity(i)
        }
    }

    private lateinit var mBinding: ActivityPreviewBinding

    private lateinit var mediaLaunchers: MediaLaunchers

    private val mLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refresh()
        }

    private var mProject: Project? = null

    private val mAdapter: PreviewAdapter?
        get() = mBinding.mediaGrid.adapter as? PreviewAdapter

    private var mMedia: List<Media>
        get() = mAdapter?.currentList ?: emptyList()
        set(value) {
            mAdapter?.submitList(value) {
                runOnUiThread {
                    mediaSelectionChanged()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mProject = Project.getById(intent.getLongExtra(PROJECT_ID_EXTRA, -1))

        mediaLaunchers = Picker.register(this, mBinding.root, { mProject }, {
            refresh()
        })

        setupToolbar(
            title = getString(R.string.preview_media),
            showBackButton = true
        )

        mBinding.mediaGrid.layoutManager = GridLayoutManager(this, 2)
        mBinding.mediaGrid.adapter = PreviewAdapter(this)
        mBinding.mediaGrid.setHasFixedSize(true)

        mBinding.btAddMore.setOnClickListener(this)
        mBinding.btBatchEdit.setOnClickListener(this)
        mBinding.btSelectAll.setOnClickListener(this)
        mBinding.btRemove.setOnClickListener(this)

        if (Picker.canPickFiles(this)) {
            mBinding.btAddMore.setOnLongClickListener {
                //mBinding.addMenu.container.show(animate = true)
                initAddMediaBottomSheet()
                true
            }

            mBinding.addMenu.container.setOnClickListener {
                it.hide(animate = true)
            }

            mBinding.addMenu.menu.setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.action_upload_media -> {
                        onClick(mBinding.btAddMore)
                    }

                    R.id.action_upload_camera -> {
                        Picker.takePhoto(this@PreviewActivity, mediaLaunchers.cameraLauncher)
                    }

                    R.id.action_upload_files -> {
                        Picker.pickFiles(mediaLaunchers.filePickerLauncher)
                    }
                }

                mBinding.addMenu.container.hide(animate = true)

                true
            }
        }


        refresh()
    }

    private fun initAddMediaBottomSheet() {

        if (Picker.canPickFiles(this)) {
            val modalBottomSheet = ContentPickerFragment { action ->
                when (action) {
                    AddMediaType.CAMERA -> Picker.takePhoto(this@PreviewActivity, mediaLaunchers.cameraLauncher)
                    AddMediaType.FILES -> Picker.pickFiles(mediaLaunchers.filePickerLauncher)
                    AddMediaType.GALLERY -> onClick(mBinding.btAddMore)
                }
            }
            modalBottomSheet.show(supportFragmentManager, ContentPickerFragment.TAG)
        }
    }

    override fun onResume() {
        super.onResume()

        showFirstTimeBatch()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_preview, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_upload -> {
                uploadMedia()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onClick(view: View?) {
        when (view) {
            mBinding.btAddMore -> {
                Picker.pickMedia(this, mediaLaunchers.imagePickerLauncher)
            }

            mBinding.btBatchEdit -> {
                val selected = mMedia.filter { it.selected }

                if (selected.size == 1) {
                    mLauncher.launch(ReviewActivity.getIntent(this, mMedia, selected.first()))
                } else if (selected.size > 1) {
                    mLauncher.launch(
                        ReviewActivity.getIntent(
                            this,
                            mMedia.filter { it.selected },
                            batchMode = true
                        )
                    )
                }
            }

            mBinding.btSelectAll -> {
                val select = mMedia.firstOrNull { !it.selected } != null

                mMedia.forEach {
                    if (it.selected != select) {
                        it.selected = select

                        mAdapter?.notifyItemChanged(mMedia.indexOf(it))
                    }
                }

                mediaSelectionChanged()
            }

            mBinding.btRemove -> {
                mMedia.forEach {
                    if (it.selected) {
                        it.delete()
                    }
                }

                refresh()
            }
        }
    }

    override fun mediaClicked(media: Media) {
        mLauncher.launch(ReviewActivity.getIntent(this, mMedia, media))
    }

    override fun mediaSelectionChanged() {
        if (mMedia.firstOrNull { it.selected } != null) {
            mBinding.btAddMore.hide()
            mBinding.bottomBar.show()
        } else {
            mBinding.btAddMore.toggle(mProject != null)
            mBinding.bottomBar.hide()
        }
    }

    private fun refresh() {
        mMedia = Media.getByStatus(listOf(Media.Status.Local), Media.ORDER_CREATED)
    }

    private fun showFirstTimeBatch() {
        if (Prefs.batchHintShown) return

        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            icon = R.drawable.perm_media_24px.asUiImage()
            title = R.string.edit_multiple.asUiText()
            message = R.string.press_and_hold_to_select_and_edit_multiple_media.asUiText()
            positiveButton {
                text = UiText.StringResource(R.string.lbl_got_it)
                action = {
                    dialogManager.dismissDialog()
                }
            }
        }



        Prefs.batchHintShown = true
    }

    private fun uploadMedia() {
        val queue = {
            mMedia.forEach {
                it.sStatus = Media.Status.Queued
                it.selected = false
                it.save()
            }

            finish()
        }

        if (Prefs.dontShowUploadHint) {

            queue()

        } else {

            var doNotShowAgain = false

            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Warning
                message = R.string.once_uploaded_you_will_not_be_able_to_edit_media.asUiText()
                showCheckbox = true
                checkboxText = UiText.DynamicString("Do not show me this again")
                onCheckboxChanged = { isChecked ->
                    doNotShowAgain = isChecked
                }
                positiveButton {
                    text = UiText.DynamicString("Proceed to upload")
                    action = {
                        Prefs.dontShowUploadHint = doNotShowAgain
                        queue()
                    }
                }
                neutralButton {
                    text = UiText.DynamicString("Actually, let me edit")
                }
            }

//            val d = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogTheme))
//                .setTitle(R.string.once_uploaded_you_will_not_be_able_to_edit_media)
//                .setIcon(R.drawable.baseline_cloud_upload_black_48)
//                .setPositiveButton(
//                    R.string.lbl_got_it
//                ) { _: DialogInterface, _: Int ->
//                    Prefs.dontShowUploadHint = doNotShowAgain
//                    queue()
//                }
//                .setNegativeButton(R.string.lbl_Cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
//                .setMultiChoiceItems(
//                    arrayOf(getString(R.string.do_not_show_me_this_again)),
//                    booleanArrayOf(false)
//                )
//                { _, _, isChecked ->
//                    doNotShowAgain = isChecked
//                }.show()
//
//            // hack for making sure this dialog always shows all lines of the pretty long title, even on small screens
//            d.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.maxLines = 99

        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MainActivity.REQUEST_FILE_MEDIA -> Picker.pickMedia(this, mediaLaunchers.imagePickerLauncher)
            MainActivity.REQUEST_CAMERA_PERMISSION -> Picker.takePhoto(this, mediaLaunchers.cameraLauncher)
        }
    }
}