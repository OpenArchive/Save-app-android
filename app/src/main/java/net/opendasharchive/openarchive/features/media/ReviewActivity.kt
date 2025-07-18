package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.github.derlio.waveform.SimpleWaveformView
import com.squareup.picasso.Picasso
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityReviewBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.UploadMediaViewHolder
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import net.opendasharchive.openarchive.util.extensions.toggle
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min

class ReviewActivity : BaseActivity(), View.OnClickListener {

    companion object {
        private const val EXTRA_CURRENT_MEDIA_ID = "archive_extra_current_media_id"
        private const val EXTRA_SELECTED_IDX = "selected_idx"
        private const val EXTRA_BATCH_MODE = "batch_mode"

        fun getIntent(context: Context, media: List<Media>, selected: Media? = null, batchMode: Boolean = false): Intent {
            val i = Intent(context, ReviewActivity::class.java)
            i.putExtra(EXTRA_CURRENT_MEDIA_ID, media.map { it.id }.toLongArray())

            if (selected != null) {
                i.putExtra(EXTRA_SELECTED_IDX, media.indexOf(selected))
            }

            i.putExtra(EXTRA_BATCH_MODE, batchMode)

            return i
        }
    }

    private lateinit var mBinding: ActivityReviewBinding

    private var mStore = emptyList<Media>()

    private var mIndex = 0

    private var mBatchMode = false

    private val mMedia
        get() = mStore.getOrNull(mIndex)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBatchMode = intent.getBooleanExtra(EXTRA_BATCH_MODE, false)

        setupToolbar(
            title = if (mBatchMode) "Bulk Edit Media Info" else getString(R.string.edit_media_info),
            showBackButton = true
        )

        mStore = intent.getLongArrayExtra(EXTRA_CURRENT_MEDIA_ID)
            ?.map { Media.get(it) }?.filterNotNull() ?: emptyList()

        mIndex = savedInstanceState?.getInt(EXTRA_SELECTED_IDX) ?: intent.getIntExtra(EXTRA_SELECTED_IDX, 0)



        mBinding.btFlag.setOnClickListener(this)

        mBinding.waveform.setOnClickListener(this)
        mBinding.image.setOnClickListener(this)

        mBinding.btPageBack.setOnClickListener {
            mIndex = max(0, mIndex - 1)
            refresh()
        }

        mBinding.btPageFrwd.setOnClickListener {
            mIndex = min(mIndex + 1, mStore.size - 1)
            refresh()
        }

        mBinding.description.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString() ?: ""

                if (mBatchMode) {
                    mStore.forEach {
                        it.description = value
                    }
                }
                else {
                    mMedia?.description = value
                }

                save()
            }
        })

        mBinding.location.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString() ?: ""

                if (mBatchMode) {
                    mStore.forEach {
                        it.location = value
                    }
                }
                else {
                    mMedia?.location = value
                }

                save()
            }
        })

        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(EXTRA_SELECTED_IDX, mIndex)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_review, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // "Done" button is only there for user convenience.
            // No difference to back, actually. We store everything
            // right away.
            android.R.id.home, R.id.menu_done -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onClick(view: View?) {
        when (view) {
//            mBinding.waveform, mBinding.image -> {
//                if (mMedia?.mimeType?.startsWith("image") == true) {
//                    val draweeView = SimpleDraweeView(this)
//                    draweeView.setImageURI(mMedia?.fileUri)
//                }
//            }
            mBinding.btFlag -> {
                showFirstTimeFlag()

                val isFlagged = !this.isFlagged

                if (mBatchMode) {
                    mStore.forEach { it.flag = isFlagged }
                }
                else {
                    mMedia?.flag = isFlagged
                }

                save()

                updateFlagState()
            }
        }
    }

    private fun refresh() {
        if (mBatchMode) {
            mBinding.batchContainer.show()
            mBinding.singleContainer.hide()

            mBinding.counter.text = NumberFormat.getIntegerInstance().format(mStore.size)

            for (i in 0..2) {
                val media = mStore.getOrNull(i)

                val iv = when (i) {
                    0 -> mBinding.batchImg3
                    1 -> mBinding.batchImg2
                    else -> mBinding.batchImg1
                }

                if (media == null) {
                    iv.hide()
                }
                else {
                    load(media, iv)
                }
            }
        }
        else {
            mBinding.batchContainer.hide()
            mBinding.singleContainer.show()

            mBinding.counter.text = getString(R.string.counter, mIndex + 1, mStore.size)

            load(mMedia, mBinding.image, mBinding.waveform)
        }

        updateFlagState()

        mBinding.btPageBack.toggle( !mBatchMode && mIndex > 0)
        mBinding.btPageFrwd.toggle(!mBatchMode && mIndex < mStore.size - 1)

        if (mBatchMode) {
            var description = mMedia?.description
            var location = mMedia?.location

            // If all descriptions/locations are the same, prefill the TextView
            // with that.
            for (media in mStore) {
                if (media.description != description) {
                    description = null
                }
                if (media.location != location) {
                    location = null
                }

                if ((description == null) && (location == null)) {
                    break
                }
            }

            mBinding.description.setText(description)
            mBinding.location.setText(location)
        }
        else {
            mBinding.description.setText(mMedia?.description)
            mBinding.location.setText(mMedia?.location)
        }
    }

    private fun updateFlagState() {
        if (isFlagged) {
            mBinding.btFlag.setIconResource(R.drawable.ic_flag_selected)
            mBinding.btFlag.contentDescription = getText(R.string.status_flagged)
        }
        else {
            mBinding.btFlag.setIconResource(R.drawable.ic_flag_unselected)
            mBinding.btFlag.contentDescription = getText(R.string.hint_flag)
        }
    }

    private val isFlagged: Boolean
        get() {
            var flagged = mMedia?.flag ?: false

            if (mBatchMode && flagged) {
                // Only show flagged, if all are flagged.
                if (mStore.firstOrNull { !it.flag } != null) {
                    flagged = false
                }
            }

            return flagged
        }

    private fun showFirstTimeFlag() {
        if (!Prefs.flagHintShown) return

        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            title = UiText.StringResource(R.string.popup_flag_title)
            message = UiText.StringResource(R.string.popup_flag_desc)
            positiveButton {
                text = UiText.StringResource(R.string.lbl_got_it)
                action = {
                    dialogManager.dismissDialog()
                }
            }
        }

        Prefs.flagHintShown = true
    }

    private fun save() {
        for (media in mStore) {
            media.licenseUrl = media.project?.licenseUrl ?: media.space?.license

            if (media.sStatus == Media.Status.New) media.sStatus = Media.Status.Local

            media.save()
        }
    }

    private fun load(media: Media?, imageView: ImageView, waveform: SimpleWaveformView? = null) {
        imageView.show()
        waveform?.hide()

        if (media?.mimeType?.startsWith("image") == true) {
            Glide.with(this)
                .load(media.fileUri)
                .into(imageView)
        }
        else if (media?.mimeType?.startsWith("video") == true) {
            Picasso.Builder(this)
                .addRequestHandler(VideoRequestHandler(this))
                .build()
                .load(VideoRequestHandler.SCHEME_VIDEO + ":" + media.originalFilePath)
                ?.fit()
                ?.centerCrop()
                ?.into(imageView)
        }
        else if (media?.mimeType?.startsWith("audio") == true) {
            imageView.setImageResource(R.drawable.audio_waveform)

            if (waveform != null) {
                val soundFile = UploadMediaViewHolder.soundCache[media.originalFilePath]
                if (soundFile != null) {
                    waveform.setAudioFile(soundFile)
                    waveform.show()
                    imageView.hide()
                }
            }
        }
        else {
            imageView.setImageResource(R.drawable.no_thumbnail)
        }
    }
}
