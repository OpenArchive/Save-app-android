package net.opendasharchive.openarchive.db

import android.content.res.ColorStateList
import android.text.format.Formatter
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
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

        resetImage()

        binding.image.alpha =
            if (media?.sStatus == Media.Status.Uploaded || !doImageFade) 1f else 0.5f

        when {
            media?.mimeType?.startsWith("image") == true -> {
                val progress = CircularProgressDrawable(mContext).apply {
                    strokeWidth = 5f
                    centerRadius = 30f
                    start()
                }

                binding.image.apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(0, 0, 0, 0)
                    show()
                    load(media.fileUri) {
                        placeholder(progress)
                        error(R.drawable.ic_image)
                        crossfade(true)
                        listener(onError = { _, result ->
                            AppLogger.w("Failed to load image: ${result.throwable.message}")
                            showPlaceholderIcon(R.drawable.ic_image)
                        })
                    }
                }
            }

            media?.mimeType?.startsWith("video") == true -> {
                val progress = CircularProgressDrawable(mContext).apply {
                    strokeWidth = 5f
                    centerRadius = 30f
                    start()
                }

                binding.image.apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(0, 0, 0, 0)
                    show()
                    load(media.fileUri) {
                        placeholder(progress)
                        error(R.drawable.ic_video)
                        crossfade(true)
                        listener(onError = { _, result ->
                            AppLogger.w("Failed to load video thumbnail: ${result.throwable.message}")
                            showPlaceholderIcon(R.drawable.ic_video)
                        })
                    }
                }
            }

            media?.mimeType?.startsWith("audio") == true -> {
                showPlaceholderIcon(R.drawable.ic_music)
            }

            media?.mimeType == "application/pdf" -> {
                showPlaceholderIcon(R.drawable.ic_pdf)
            }

            media?.mimeType?.startsWith("application") == true -> {
                showPlaceholderIcon(R.drawable.ic_unknown_file)
            }

            else -> {
                showPlaceholderIcon(R.drawable.ic_unknown_file)
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

    private fun resetImage() {
        binding.image.apply {
            setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clearColorFilter()
            imageTintList = null
        }
    }

    private fun showPlaceholderIcon(drawableRes: Int) {
        val padding = (12 * mContext.resources.displayMetrics.density).toInt()
        binding.image.apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
            setPadding(padding, padding, padding, padding)
            load(drawableRes) {
                crossfade(false)
            }
            applyPlaceholderTint()
            show()
        }
    }

    private fun applyPlaceholderTint() {
        val tint = ContextCompat.getColor(mContext, R.color.colorOnSurfaceVariant)
        binding.image.imageTintList = ColorStateList.valueOf(tint)
    }
}
