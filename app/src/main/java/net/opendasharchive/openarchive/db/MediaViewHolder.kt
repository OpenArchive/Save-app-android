package net.opendasharchive.openarchive.db

import android.annotation.SuppressLint
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.derlio.waveform.SimpleWaveformView
import com.github.derlio.waveform.soundfile.SoundFile
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.RvMediaBoxBinding
import net.opendasharchive.openarchive.databinding.RvMediaRowBigBinding
import net.opendasharchive.openarchive.databinding.RvMediaRowSmallBinding
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import timber.log.Timber
import java.io.InputStream

abstract class MediaViewHolder(protected val binding: ViewBinding) :
    RecyclerView.ViewHolder(binding.root) {

    class Box(parent: ViewGroup) : MediaViewHolder(
        RvMediaBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) {
        override val image: ImageView
            get() = (binding as RvMediaBoxBinding).image

        override val waveform: SimpleWaveformView
            get() = (binding as RvMediaBoxBinding).waveform

        override val videoIndicator: ImageView
            get() = (binding as RvMediaBoxBinding).videoIndicator

        override val overlayContainer: View
            get() = (binding as RvMediaBoxBinding).overlayContainer

        override val progress: CircularProgressIndicator
            get() = (binding as RvMediaBoxBinding).progress

        override val progressText: TextView
            get() = (binding as RvMediaBoxBinding).progressText

        override val error: ImageView
            get() = (binding as RvMediaBoxBinding).error

        override val title: TextView?
            get() = null //(binding as RvMediaBoxBinding).title

        override val fileInfo: TextView?
            get() = null //(binding as RvMediaBoxBinding).fileInfo

        override val locationIndicator: ImageView?
            get() = null

        override val tagsIndicator: ImageView?
            get() = null

        override val descIndicator: ImageView?
            get() = null

        override val flagIndicator: ImageView?
            get() = null

        override val selectedIndicator: View
            get() = (binding as RvMediaBoxBinding).selectedIndicator

        override val handle: ImageView?
            get() = null
    }

    class BigRow(parent: ViewGroup) : MediaViewHolder(
        RvMediaRowBigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) {
        override val image: ImageView
            get() = (binding as RvMediaRowBigBinding).image

        override val waveform: SimpleWaveformView
            get() = (binding as RvMediaRowBigBinding).waveform

        override val videoIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).videoIndicator

        override val overlayContainer: View?
            get() = null

        override val progress: CircularProgressIndicator?
            get() = null

        override val progressText: TextView?
            get() = null

        override val error: ImageView?
            get() = null

        override val title: TextView
            get() = (binding as RvMediaRowBigBinding).title

        override val fileInfo: TextView
            get() = (binding as RvMediaRowBigBinding).fileInfo

        override val locationIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).locationIndicator

        override val tagsIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).tagsIndicator

        override val descIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).descIndicator

        override val flagIndicator: ImageView
            get() = (binding as RvMediaRowBigBinding).flagIndicator

        override val selectedIndicator: View?
            get() = null

        override val handle: ImageView?
            get() = null
    }

    class SmallRow(parent: ViewGroup) : MediaViewHolder(
        RvMediaRowSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) {
        override val image: ImageView
            get() = (binding as RvMediaRowSmallBinding).image

        override val waveform: SimpleWaveformView
            get() = (binding as RvMediaRowSmallBinding).waveform

        override val videoIndicator: ImageView?
            get() = null

        override val overlayContainer: View
            get() = (binding as RvMediaRowSmallBinding).overlayContainer

        override val progress: CircularProgressIndicator
            get() = (binding as RvMediaRowSmallBinding).progress

        override val progressText: TextView
            get() = (binding as RvMediaRowSmallBinding).progressText

        override val error: ImageView
            get() = (binding as RvMediaRowSmallBinding).error

        override val title: TextView
            get() = (binding as RvMediaRowSmallBinding).title

        override val fileInfo: TextView
            get() = (binding as RvMediaRowSmallBinding).fileInfo

        override val locationIndicator: ImageView?
            get() = null

        override val tagsIndicator: ImageView?
            get() = null

        override val descIndicator: ImageView?
            get() = null

        override val flagIndicator: ImageView?
            get() = null

        override val selectedIndicator: View?
            get() = null

        override val handle: ImageView
            get() = (binding as RvMediaRowSmallBinding).handle
    }


    companion object {
        val soundCache = HashMap<String, SoundFile>()
    }


    abstract val image: ImageView
    abstract val waveform: SimpleWaveformView
    abstract val videoIndicator: ImageView?
    abstract val overlayContainer: View?
    abstract val progress: CircularProgressIndicator?
    abstract val progressText: TextView?
    abstract val error: ImageView?
    abstract val title: TextView?
    abstract val fileInfo: TextView?
    abstract val locationIndicator: ImageView?
    abstract val tagsIndicator: ImageView?
    abstract val descIndicator: ImageView?
    abstract val flagIndicator: ImageView?
    abstract val selectedIndicator: View?
    abstract val handle: ImageView?

    private val mContext = itemView.context

    private val mPicasso = Picasso.Builder(mContext)
        .addRequestHandler(VideoRequestHandler(mContext))
        .build()

    @SuppressLint("SetTextI18n")
    fun bind(media: Media? = null, batchMode: Boolean = false, doImageFade: Boolean = true) {
        AppLogger.i("Binding media item ${media?.id} with status ${media?.sStatus} and progress ${media?.uploadPercentage}")
        itemView.tag = media?.id

        if (batchMode && media?.selected == true) {
            itemView.setBackgroundResource(R.color.colorPrimary)
            selectedIndicator?.show()
        } else {
            itemView.setBackgroundResource(R.color.transparent)
            selectedIndicator?.hide()
        }

        image.alpha = if (media?.sStatus == Media.Status.Uploaded || !doImageFade) 1f else 0.5f

        media?.let {
            val fileUri = it.getFileUri(mContext) // ✅ Securely get URI

            when {
                it.mimeType.startsWith("image") -> {
                    val progress = CircularProgressDrawable(mContext)
                    progress.strokeWidth = 5f
                    progress.centerRadius = 30f
                    progress.start()

                    Glide.with(mContext)
                        .load(fileUri)
                        .placeholder(progress)
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // ✅ Prevents caching issues
                        .fitCenter()
                        .into(image)

                    image.show()
                    waveform.hide()
                    videoIndicator?.hide()
                }
                it.mimeType.startsWith("video") -> {
                    mPicasso.load(VideoRequestHandler.SCHEME_VIDEO + ":" + fileUri)
                        .fit()
                        .centerCrop()
                        .into(image)

                    image.show()
                    waveform.hide()
                    videoIndicator?.show()
                }
                it.mimeType.startsWith("audio") -> {
                    videoIndicator?.hide()

                    val soundFile = soundCache[fileUri.toString()]

                    if (soundFile != null) {
                        image.hide()
                        waveform.setAudioFile(soundFile)
                        waveform.show()
                    } else {
                        image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_thumbnail))
                        image.show()
                        waveform.hide()

                        CoroutineScope(Dispatchers.IO).launch {
                            val soundFile = try {
                                SoundFile.create(fileUri.toString()) { true }
                            } catch (e: Throwable) {
                                Timber.d(e)
                                null
                            }

                            if (soundFile != null) {
                                soundCache[fileUri.toString()] = soundFile

                                MainScope().launch {
                                    waveform.setAudioFile(soundFile)
                                    image.hide()
                                    waveform.show()
                                }
                            }
                        }
                    }
                }
                else -> {
                    image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_thumbnail))
                    image.show()
                    waveform.hide()
                    videoIndicator?.hide()
                }
            }
        }

        if (media != null) {
            val inputStream = media.fileInputStream(mContext)

            fileInfo?.text = when {
                media.contentLength > 0 -> Formatter.formatShortFileSize(mContext, media.contentLength)
                inputStream != null -> {
                    val size = inputStream.available().toLong()
                    inputStream.close()
                    Formatter.formatShortFileSize(mContext, size)
                }
                else -> media.formattedCreateDate
            }

            fileInfo?.show()
        } else {
            fileInfo?.hide()
        }

        val sbTitle = StringBuffer()

        if (media?.sStatus == Media.Status.Error) {
            AppLogger.i("Media Item ${media.id} is error")
            sbTitle.append(mContext.getString(R.string.error))

            overlayContainer?.show()
            progress?.hide()
            progressText?.hide()
            error?.show()

            if (media.statusMessage.isNotBlank()) {
                fileInfo?.text = media.statusMessage
                fileInfo?.show()
            }
        } else if (media?.sStatus == Media.Status.Queued) {
            AppLogger.i("Media Item ${media.id} is queued")
            overlayContainer?.show()
            progress?.isIndeterminate = true
            progress?.show()
            progressText?.hide()
            error?.hide()
        } else if (media?.sStatus == Media.Status.Uploading) {
//            val progressValue = if (media.contentLength > 0) {
//                (media.progress.toFloat() / media.contentLength.toFloat() * 100f).roundToInt()
//            } else 0
            progress?.isIndeterminate = false
            val progressValue = media.uploadPercentage ?: 0
            AppLogger.i("Media Item ${media.id} is uploading")

            overlayContainer?.show()
            progress?.show()
            progressText?.show()

            // Make sure to keep spinning until the upload has made some noteworthy progress.
            if (progressValue > 2) {
                progress?.setProgressCompat(progressValue, true)
            }
//            else {
//                progress?.isIndeterminate = true
//            }

            progressText?.text = "${progressValue}%"

            error?.hide()
        } else {
            overlayContainer?.hide()
            progress?.hide()
            progressText?.hide()
            error?.hide()
        }

        if (sbTitle.isNotEmpty()) sbTitle.append(": ")
        sbTitle.append(media?.title)

        if (sbTitle.isNotBlank()) {
            title?.text = sbTitle.toString()
            title?.show()
        } else {
            title?.hide()
        }

        locationIndicator?.setImageResource(
            if (media?.location.isNullOrBlank()) R.drawable.ic_location_unselected
            else R.drawable.ic_location_selected
        )

        tagsIndicator?.setImageResource(
            if (media?.tags.isNullOrBlank()) R.drawable.ic_tag_unselected
            else R.drawable.ic_tag_selected
        )

        descIndicator?.setImageResource(
            if (media?.description.isNullOrBlank()) R.drawable.ic_edit_unselected
            else R.drawable.ic_edit_selected
        )

        flagIndicator?.setImageResource(
            if (media?.flag == true) R.drawable.ic_flag_selected
            else R.drawable.ic_flag_unselected
        )
    }

    fun updateProgress(progressValue: Int) {
        if (progressValue > 2) {
            progress?.isIndeterminate = false
            progress?.setProgressCompat(progressValue, true)
        } else {
            progress?.isIndeterminate = true
        }

        AppLogger.i("Updating progressText to $progressValue%")
        progressText?.show(animate = true)
        progressText?.text = "$progressValue%"
    }
}
