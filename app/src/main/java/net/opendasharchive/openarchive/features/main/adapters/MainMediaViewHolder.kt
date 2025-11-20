package net.opendasharchive.openarchive.features.main.adapters

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil3.ImageLoader
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
import java.io.File
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.RvMediaBoxBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import timber.log.Timber

class MainMediaViewHolder(val binding: RvMediaBoxBinding) : RecyclerView.ViewHolder(binding.root) {

    private val mContext = itemView.context

    private val imageLoader = ImageLoader.Builder(mContext)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .build()


    fun bind(
        media: Media? = null,
        isInSelectionMode: Boolean = false,
        doImageFade: Boolean = true
    ) {

        itemView.tag = media?.id

        resetImage()

        val isSelected = isInSelectionMode && media?.selected == true

        // Update selection visuals.
        if (isSelected) {
            //itemView.setBackgroundResource(R.color.colorTertiary)
            binding.selectedIndicator.show()
        } else {
            //itemView.setBackgroundResource(R.color.transparent)
            binding.selectedIndicator.hide()
        }

        binding.image.alpha =
            if (media?.sStatus == Media.Status.Uploaded || !doImageFade) 1f else 0.5f

        if (media?.mimeType?.startsWith("image") == true) {
            // Check if file exists before attempting to load
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

                binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
                binding.image.setBackgroundColor(
                    ContextCompat.getColor(
                        mContext,
                        android.R.color.transparent
                    )
                )
                binding.image.setPadding(0, 0, 0, 0)
                binding.image.clearColorFilter()
                binding.image.load(media.fileUri, imageLoader) {
                    placeholder(progress)
                    error(R.drawable.ic_image)
                    crossfade(true)
                    crossfade(300)
                    listener(onError = { _, res ->
                        AppLogger.e(res.throwable)
                    })
                }
            } else {
                AppLogger.w("Image file not found: ${media.fileUri.path}")
                val padding = (28 * mContext.resources.displayMetrics.density).toInt()
                binding.image.scaleType = ImageView.ScaleType.FIT_CENTER
                binding.image.setPadding(padding, padding, padding, padding)
                binding.image.load(R.drawable.ic_image, imageLoader) {
                    crossfade(false)
                }
                applyPlaceholderTint(isSelected)
                binding.mediaTitle.text = media.title
                binding.mediaTitle.show()
            }

            binding.image.show()
            binding.videoIndicator.hide()
        } else if (media?.mimeType?.startsWith("video") == true) {
            // For videos, try both paths to find the file
            val fileExists = try {
                // First try originalFilePath
                val originalExists = media.originalFilePath?.let { File(it).exists() } ?: false
                // If not found, try fileUri path
                val uriExists = if (!originalExists) {
                    media.fileUri.path?.let { File(it).exists() } ?: false
                } else false

                originalExists || uriExists
            } catch (e: Exception) {
                AppLogger.e(e)
                false
            }

            if (fileExists) {
                val progress = CircularProgressDrawable(mContext)
                progress.strokeWidth = 5f
                progress.centerRadius = 30f
                progress.start()

                binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
                //binding.image.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
                binding.image.setPadding(0, 0, 0, 0)
                binding.image.clearColorFilter()
                binding.image.load(media.originalFilePath, imageLoader) {
                    videoFrameMillis(1000) // Extracts the frame at 1 second (1000ms)
                    placeholder(progress)
                    error(R.drawable.ic_video)
                    crossfade(true)
                    crossfade(300)
                    listener(onError = { _, res ->
                        AppLogger.e(res.throwable)
                    })
                }
            } else {
                AppLogger.w("Video file not found: ${media.originalFilePath}")
                val padding = (28 * mContext.resources.displayMetrics.density).toInt()
                binding.image.scaleType = ImageView.ScaleType.FIT_CENTER
                binding.image.setPadding(padding, padding, padding, padding)
                binding.image.load(R.drawable.ic_video, imageLoader) {
                    crossfade(false)
                }
                applyPlaceholderTint(isSelected)

                binding.mediaTitle.text = media.title
                binding.mediaTitle.show()
            }

            binding.image.show()
            binding.videoIndicator.show()
        } else if (media?.mimeType?.startsWith("audio") == true) {
            binding.videoIndicator.hide()
            placeholderIcon(R.drawable.ic_music, media?.title, isSelected)
        } else if (media?.mimeType == "application/pdf") {
            placeholderIcon(R.drawable.ic_pdf, media?.title, isSelected)
            binding.videoIndicator.hide()
        } else if (media?.mimeType?.startsWith("application") == true) {
            placeholderIcon(R.drawable.ic_unknown_file, media?.title, isSelected)
            binding.videoIndicator.hide()

        } else {
            placeholderIcon(R.drawable.ic_unknown_file, media?.title, isSelected)
            binding.videoIndicator.hide()
        }

        // Update overlay based on media status.
        when (media?.sStatus) {
            Media.Status.Error -> {
                AppLogger.i("Media Item ${media.id} is error")

                binding.overlayContainer.show()
                binding.progress.hide()
                binding.progressText.hide()
                binding.error.show()

            }

            Media.Status.Queued -> {
                AppLogger.i("Media Item ${media.id} is queued")
                binding.overlayContainer.show()
                binding.progress.isIndeterminate = true
                binding.progress.show()
                binding.progressText.hide()
                binding.error.hide()
            }

            Media.Status.Uploading -> {
                binding.progress.isIndeterminate = false
                val progressValue = media.uploadPercentage ?: 0
                AppLogger.i("Media Item ${media.id} is uploading")

                binding.overlayContainer.show()
                binding.progress.show()
                //binding.progressText.show()

                // Make sure to keep spinning until the upload has made some noteworthy progress.
                if (progressValue > 2) {
                    binding.progress.setProgressCompat(progressValue, true)
                }
                //binding.progressText.text = "${progressValue}%"
                binding.error.hide()
            }

            else -> {
                binding.overlayContainer.hide()
                binding.progress.hide()
                binding.progressText.hide()
                binding.error.hide()
            }
        }

    }

    fun updateProgress(progressValue: Int) {
        if (progressValue > 2) {
            binding.progress.isIndeterminate = false
            binding.progress.setProgressCompat(progressValue, true)
        } else {
            binding.progress.isIndeterminate = true
        }

        //AppLogger.i("Updating progressText to $progressValue%")
        //binding.progressText.show(animate = true)
        //binding.progressText.text = "$progressValue%"
    }

    private fun resetImage() {
        binding.mediaTitle.text = ""
        binding.mediaTitle.hide()
        binding.image.setBackgroundColor(
            ContextCompat.getColor(mContext, android.R.color.transparent)
        )
        binding.image.setPadding(0, 0, 0, 0)
        binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.image.clearColorFilter()
        binding.image.imageTintList = null
    }

    private fun placeholderIcon(drawableRes: Int, title: String?, isSelected: Boolean) {
        val padding = (28 * mContext.resources.displayMetrics.density).toInt()
        binding.image.apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(
                ContextCompat.getColor(
                    mContext,
                    android.R.color.transparent
                )
            )
            setPadding(padding, padding, padding, padding)
            load(drawableRes, imageLoader) {
                crossfade(false)
            }
            clearColorFilter()
            applyPlaceholderTint(isSelected)
            show()
        }
        if (title.isNullOrBlank()) {
            binding.mediaTitle.hide()
        } else {
            binding.mediaTitle.text = title
            binding.mediaTitle.show()
        }
    }

    private fun applyPlaceholderTint(isSelected: Boolean) {
        val tint = if (isSelected) {
            ContextCompat.getColor(mContext, R.color.colorOnPrimaryContainer)
        } else {
            ContextCompat.getColor(mContext, R.color.colorOnSurfaceVariant)
        }
        binding.image.imageTintList = ColorStateList.valueOf(tint)
    }
}
