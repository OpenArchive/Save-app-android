package net.opendasharchive.openarchive.db

import android.text.format.Formatter
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.github.derlio.waveform.soundfile.SoundFile
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.RvMediaRowSmallBinding
import net.opendasharchive.openarchive.fragments.VideoRequestHandler
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import net.opendasharchive.openarchive.util.extensions.toggle
import timber.log.Timber
import java.io.InputStream

class UploadMediaViewHolder(
    private val binding: RvMediaRowSmallBinding,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {


    companion object {
        val soundCache = HashMap<String, SoundFile>()
    }


    private val mContext = itemView.context

    private val mPicasso = Picasso.Builder(mContext)
        .addRequestHandler(VideoRequestHandler(mContext))
        .build()

    init {
        binding.btnDelete.setOnClickListener {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onDeleteClick(position)
            }
        }
    }

    fun bind(media: Media? = null, doImageFade: Boolean = true) {
        AppLogger.i("Binding media item ${media?.id} with status ${media?.sStatus} and progress ${media?.uploadPercentage}")
        itemView.tag = media?.id

        binding.image.alpha =
            if (media?.sStatus == Media.Status.Uploaded || !doImageFade) 1f else 0.5f

        if (media?.mimeType?.startsWith("image") == true) {
            val progress = CircularProgressDrawable(mContext)
            progress.strokeWidth = 5f
            progress.centerRadius = 30f
            progress.start()

            Glide.with(mContext)
                .load(media.fileUri)
                .placeholder(progress)
                .fitCenter()
                .into(binding.image)
            binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.image.show()
            binding.waveform.hide()
        } else if (media?.mimeType?.startsWith("video") == true) {
            mPicasso.load(VideoRequestHandler.SCHEME_VIDEO + ":" + media.originalFilePath)
                .fit()
                .centerCrop()
                .into(binding.image)
            binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.image.show()
            binding.waveform.hide()
        } else if (media?.mimeType?.startsWith("audio") == true) {

            val soundFile = soundCache[media.originalFilePath]

            if (soundFile != null) {
                binding.image.hide()
                binding.waveform.setAudioFile(soundFile)
                binding.waveform.show()
            } else {
                binding.image.setImageDrawable(
                    ContextCompat.getDrawable(
                        mContext,
                        R.drawable.no_thumbnail
                    )
                )
                binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
                binding.image.show()
                binding.waveform.hide()

                CoroutineScope(Dispatchers.IO).launch {
                    @Suppress("NAME_SHADOWING")
                    val soundFile = try {
                        SoundFile.create(media.originalFilePath) {
                            return@create true
                        }
                    } catch (e: Throwable) {
                        Timber.d(e)

                        null
                    }

                    if (soundFile != null) {
                        soundCache[media.originalFilePath] = soundFile

                        MainScope().launch {
                            binding.waveform.setAudioFile(soundFile)
                            binding.image.hide()
                            binding.waveform.show()
                        }
                    }
                }
            }
        } else {
            binding.image.setImageDrawable(
                ContextCompat.getDrawable(
                    mContext,
                    R.drawable.ic_unknown_file
                )
            )
            binding.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.image.show()
            binding.waveform.hide()
        }

        if (media != null) {
            val file = media.file

            if (file.exists()) {
                binding.fileInfo.text = Formatter.formatShortFileSize(mContext, file.length())
            } else {
                if (media.contentLength == -1L) {
                    var iStream: InputStream? = null
                    try {
                        iStream = mContext.contentResolver.openInputStream(media.fileUri)

                        if (iStream != null) {
                            media.contentLength = iStream.available().toLong()
                            media.save()
                        }
                    } catch (e: Throwable) {
                        Timber.e(e)
                    } finally {
                        iStream?.close()
                    }
                }

                binding.fileInfo.text = if (media.contentLength > 0) {
                    Formatter.formatShortFileSize(mContext, media.contentLength)
                } else {
                    media.formattedCreateDate
                }
            }

            binding.fileInfo.show()
        } else {
            binding.fileInfo.hide()
        }

        val sbTitle = StringBuffer()

        if (media?.sStatus == Media.Status.Error) {
            AppLogger.i("Media Item ${media.id} is error")
            sbTitle.append(mContext.getString(R.string.error))

            binding.overlayContainer.show()
            binding.progress.hide()
            binding.progressText.hide()
            binding.error.show()

            if (media.statusMessage.isNotBlank()) {
                binding.fileInfo.text = media.statusMessage
                binding.fileInfo.show()
            }
        } else if (media?.sStatus == Media.Status.Queued) {
            AppLogger.i("Media Item ${media.id} is queued")
            binding.overlayContainer.show()
            binding.progress.isIndeterminate = true
            binding.progress.show()
            binding.progressText.hide()
            binding.error.hide()
        } else if (media?.sStatus == Media.Status.Uploading) {
//            val progressValue = if (media.contentLength > 0) {
//                (media.progress.toFloat() / media.contentLength.toFloat() * 100f).roundToInt()
//            } else 0
            binding.progress.isIndeterminate = false
            val progressValue = media.uploadPercentage ?: 0
            AppLogger.i("Media Item ${media.id} is uploading")

            binding.overlayContainer.show()
            binding.progress.show()
            binding.progressText.show()

            // Make sure to keep spinning until the upload has made some noteworthy progress.
            if (progressValue > 2) {
                binding.progress.setProgressCompat(progressValue, true)
            }
//            else {
//                progress?.isIndeterminate = true
//            }

            binding.progressText.text = "${progressValue}%"

            binding.error.hide()
        } else {
            binding.overlayContainer.hide()
            binding.progress.hide()
            binding.progressText.hide()
            binding.error.hide()
        }

        if (sbTitle.isNotEmpty()) sbTitle.append(": ")
        sbTitle.append(media?.title)

        if (sbTitle.isNotBlank()) {
            binding.title.text = sbTitle.toString()
            binding.title.show()
        } else {
            binding.title.hide()
        }
    }

    fun updateProgress(progressValue: Int) {
        if (progressValue > 2) {
            binding.progress.isIndeterminate = false
            binding.progress.setProgressCompat(progressValue, true)
        } else {
            binding.progress.isIndeterminate = true
        }

        binding.progressText.show(animate = true)
        binding.progressText.text = "$progressValue%"
    }

    fun toggleEditMode(isEdit: Boolean) {
        binding.handle.toggle(isEdit)
        binding.btnDelete.toggle(isEdit)
    }
}
