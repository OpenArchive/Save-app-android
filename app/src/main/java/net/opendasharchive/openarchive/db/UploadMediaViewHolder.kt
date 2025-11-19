package net.opendasharchive.openarchive.db

import android.text.format.Formatter
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import java.io.File
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.RvMediaRowSmallBinding
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import timber.log.Timber
import java.io.InputStream

class UploadMediaViewHolder(
    private val binding: RvMediaRowSmallBinding,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val mContext = itemView.context

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
            val fileExists = try {
                media.fileUri.path?.let { path ->
                    File(path).exists()
                } ?: false
            } catch (e: Exception) {
                AppLogger.e(e)
                false
            }

            if (fileExists) {
                val progress = CircularProgressDrawable(mContext)
                progress.strokeWidth = 5f
                progress.centerRadius = 30f
                progress.start()

                binding.image.apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(0, 0, 0, 0)
                    clearColorFilter()
                    show()
                    load(media.fileUri) {
                        placeholder(progress)
                        error(R.drawable.ic_image)
                        crossfade(true)
                    }
                }
            } else {
                AppLogger.w("Image file not found: ${media.fileUri.path}")
                val padding = (12 * mContext.resources.displayMetrics.density).toInt()
                binding.image.apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(padding, padding, padding, padding)
                    setImageResource(R.drawable.ic_image)
                    setColorFilter(ContextCompat.getColor(mContext, R.color.colorOnSurfaceVariant))
                    show()
                }
            }
        } else if (media?.mimeType?.startsWith("video") == true) {
            val fileExists = try {
                media.fileUri.path?.let { path ->
                    File(path).exists()
                } ?: false
            } catch (e: Exception) {
                AppLogger.e(e)
                false
            }

            if (fileExists) {
                val progress = CircularProgressDrawable(mContext)
                progress.strokeWidth = 5f
                progress.centerRadius = 30f
                progress.start()

                binding.image.apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(0, 0, 0, 0)
                    clearColorFilter()
                    show()
                    load(media.fileUri) {
                        placeholder(progress)
                        error(R.drawable.ic_video)
                    }
                }
            } else {
                AppLogger.w("Video file not found: ${media.fileUri.path}")
                val padding = (12 * mContext.resources.displayMetrics.density).toInt()
                binding.image.apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(padding, padding, padding, padding)
                    setImageResource(R.drawable.ic_video)
                    setColorFilter(ContextCompat.getColor(mContext, R.color.colorOnSurfaceVariant))
                    show()
                }
            }
        } else if (media?.mimeType?.startsWith("audio") == true) {
            val padding = (12 * mContext.resources.displayMetrics.density).toInt()
            binding.image.apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(padding, padding, padding, padding)
                setImageResource(R.drawable.ic_music)
                setColorFilter(ContextCompat.getColor(mContext, R.color.colorOnSurfaceVariant))
                show()
            }
        } else if (media?.mimeType == "application/pdf") {
            val padding = (12 * mContext.resources.displayMetrics.density).toInt()
            binding.image.apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(padding, padding, padding, padding)
                setImageResource(R.drawable.ic_pdf)
                setColorFilter(ContextCompat.getColor(mContext, R.color.colorOnSurfaceVariant))
                show()
            }
        } else {
            val padding = (12 * mContext.resources.displayMetrics.density).toInt()
            binding.image.apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(padding, padding, padding, padding)
                setImageResource(R.drawable.ic_unknown_file)
                setColorFilter(ContextCompat.getColor(mContext, R.color.colorOnSurfaceVariant))
                show()
            }
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
            binding.error.show()

            if (media.statusMessage.isNotBlank()) {
                binding.fileInfo.text = media.statusMessage
                binding.fileInfo.show()
            }
        } else if (media?.sStatus == Media.Status.Queued) {
            AppLogger.i("Media Item ${media.id} is queued")
            binding.overlayContainer.show()
            binding.error.hide()
        } else if (media?.sStatus == Media.Status.Uploading) {
            AppLogger.i("Media Item ${media.id} is uploading")
            binding.overlayContainer.show()
            binding.error.hide()
        } else {
            binding.overlayContainer.hide()
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
}
