package net.opendasharchive.openarchive.upload

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityUploadManagerBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.core.BaseActivity

class UploadManagerActivity : BaseActivity() {

    private lateinit var mBinding: ActivityUploadManagerBinding
    var mFrag: UploadManagerFragment? = null
    private var mMenuEdit: MenuItem? = null

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        private val handler = Handler(Looper.getMainLooper())

        override fun onReceive(context: Context, intent: Intent) {
            val action = BroadcastManager.getAction(intent)
            val mediaId = action?.mediaId ?: return

            if (mediaId > -1) {
                val media = Media.get(mediaId)

                if (action == BroadcastManager.Action.Delete || media?.sStatus == Media.Status.Uploaded) {
                    handler.post { mFrag?.removeItem(mediaId) }
                }
                else {
                    handler.post { mFrag?.updateItem(mediaId) }
                }
            }
        }
    }

    private var mEditMode = true // Setting Edit mode as the default mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityUploadManagerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(
            title = getString(R.string.upload_manager_screen_title),
            subtitle = getString(R.string.upload_manager_screen_subtitle),
            showBackButton = true
        )

        //mFrag = supportFragmentManager.findFragmentById(R.id.fragUploadManager) as? UploadManagerFragment

        val bottomSheet = UploadManagerFragment()
        bottomSheet.show(supportFragmentManager, UploadManagerFragment.TAG)
    }

    override fun onResume() {
        super.onResume()
        mFrag?.refresh()

        BroadcastManager.register(this, mMessageReceiver)

        onStartEdit()
    }

    override fun onPause() {
        super.onPause()

        BroadcastManager.unregister(this, mMessageReceiver)
    }

    private fun onStartEdit() {
        UploadService.stopUploadService(this)
    }

    private fun onCompleteEdit() {
        UploadService.startUploadService(this)
    }

    private fun toggleEditMode() {
        mEditMode = !mEditMode
        mFrag?.setEditMode(mEditMode)
        mFrag?.refresh()

        if (mEditMode) {
            mMenuEdit?.setTitle(R.string.menu_done)

            UploadService.stopUploadService(this)
        }
        else {
            mMenuEdit?.setTitle(R.string.edit)

            UploadService.startUploadService(this)
        }

        updateTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_upload, menu)
        mMenuEdit =  menu.findItem(R.id.menu_done)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_done -> {
                onCompleteEdit()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        // If we're still in edit mode, restart the upload service when the user leaves.
        if (mEditMode) {
            UploadService.startUploadService(this)
        }

        super.finish()
    }

    private fun updateTitle() {
        if (mEditMode) {
            supportActionBar?.title = getString(R.string.edit_media)
            supportActionBar?.subtitle = getString(R.string.uploading_is_paused)
        } else {
            val count = mFrag?.getUploadingCounter() ?: 0

            supportActionBar?.title = if (count < 1) {
                getString(R.string.uploads)
            } else {
                getString(R.string.uploading_left, count)
            }

            supportActionBar?.subtitle = null
        }
    }
}