package net.opendasharchive.openarchive.features.media.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil3.load
import coil3.request.Disposable
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.RvMediaBoxBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.util.PdfThumbnailLoader
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show

class PreviewViewHolder(val binding: RvMediaBoxBinding) : RecyclerView.ViewHolder(binding.root) {

    private val mContext = itemView.context
    private val pdfScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pdfThumbnailJob: Job? = null
    private var imageRequest: Disposable? = null

    fun bind(
        media: Media? = null,
        batchMode: Boolean = false,
        doImageFade: Boolean = true
    ) {

        itemView.tag = media?.id
        binding.image.tag = media?.id

        resetImageState()
        hideTitle()

        val isSelected = batchMode && media?.selected == true

        if (isSelected) {
            //itemView.setBackgroundResource(R.color.colorPrimary)
            binding.selectedIndicator.show()
        } else {
            //itemView.setBackgroundResource(R.color.transparent)
            binding.selectedIndicator.hide()
        }

        binding.image.alpha = if (media?.sStatus == Media.Status.Uploaded || !doImageFade) 1f else 0.5f

        val progress = CircularProgressDrawable(mContext).apply {
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }

        if (media?.mimeType?.startsWith("image") == true) {
            // static images - check if file exists before attempting to load
            val fileExists = try {
                media.fileUri.path?.let { path ->
                    File(path).exists()
                } ?: false
            } catch (e: Exception) {
                AppLogger.e(e)
                false
            }

            if (fileExists) {
                binding.image.apply {
                    setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(0, 0, 0, 0)
                    clearColorFilter()
                    show()
                    imageRequest = load(media.fileUri) {
                        placeholder(progress)
                        error(R.drawable.ic_image)
                    }
                }
            } else {
                AppLogger.w("Image file not found: ${media.fileUri.path}")
                val padding = (24 * mContext.resources.displayMetrics.density).toInt()
                binding.image.apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
                    setPadding(padding, padding, padding, padding)
                    imageRequest = load(R.drawable.ic_image) {
                        crossfade(false)
                    }
                    applyPlaceholderTint(isSelected)
                    show()
                }
                showTitle(media?.title)
            }
            binding.videoIndicator.hide()
        } else if (media?.mimeType?.startsWith("video") == true) {
            // video thumbnail - check if file exists before attempting to load
            val fileExists = try {
                media.fileUri.path?.let { path ->
                    File(path).exists()
                } ?: false
            } catch (e: Exception) {
                AppLogger.e(e)
                false
            }

            if (fileExists) {
                binding.image.apply {
                    setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(0, 0, 0, 0)
                    clearColorFilter()
                    show()
                    imageRequest = load(media.fileUri) {
                        placeholder(progress)
                        error(R.drawable.ic_video)
                    }
                }
            } else {
                AppLogger.w("Video file not found: ${media.fileUri.path}")
                val padding = (24 * mContext.resources.displayMetrics.density).toInt()
                binding.image.apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
                    setPadding(padding, padding, padding, padding)
                    imageRequest = load(R.drawable.ic_video) {
                        crossfade(false)
                    }
                    applyPlaceholderTint(isSelected)
                    show()
                }
                showTitle(media?.title)
            }
            binding.videoIndicator.show()
        } else if (media?.mimeType?.startsWith("audio") == true) {
            binding.videoIndicator.hide()
            placeholderIcon(R.drawable.ic_music, media?.title, isSelected)
        } else if (media?.mimeType == "application/pdf") {
            loadPdfThumbnail(media, isSelected)
            binding.videoIndicator.hide()
        } else if (media?.mimeType?.startsWith("application") == true) {
            placeholderIcon(R.drawable.ic_unknown_file, media?.title, isSelected)
            binding.videoIndicator.hide()
        } else {
            placeholderIcon(R.drawable.no_thumbnail, media?.title, isSelected)
            binding.videoIndicator.hide()
        }
        media?.let { updateOverlay(it) }
    }

    private fun resetImageState() {
        pdfThumbnailJob?.cancel()
        pdfThumbnailJob = null
        imageRequest?.dispose()
        imageRequest = null
        binding.image.setImageDrawable(null)
        binding.image.apply {
            setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clearColorFilter()
            imageTintList = null
        }
        hideTitle()
    }

    private fun placeholderIcon(drawableRes: Int, title: String?, isSelected: Boolean) {
        val padding = (24 * mContext.resources.displayMetrics.density).toInt()
        binding.image.apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
            setPadding(padding, padding, padding, padding)
            imageRequest = load(drawableRes) {
                crossfade(false)
            }
            clearColorFilter()
            applyPlaceholderTint(isSelected)
            show()
        }
        showTitle(title)
    }

    private fun showTitle(title: String?) {
        if (title.isNullOrBlank()) {
            hideTitle()
        } else {
            binding.mediaTitle.text = title
            binding.mediaTitle.show()
        }
    }

    private fun hideTitle() {
        binding.mediaTitle.text = ""
        binding.mediaTitle.hide()
    }

    private fun applyPlaceholderTint(isSelected: Boolean) {
        val tint = if (isSelected) {
            ContextCompat.getColor(mContext, R.color.colorOnPrimaryContainer)
        } else {
            ContextCompat.getColor(mContext, R.color.colorOnSurfaceVariant)
        }
        binding.image.imageTintList = ColorStateList.valueOf(tint)
    }

    private fun updateOverlay(media: Media) {
        val sbTitle = StringBuffer()
        when (media.sStatus) {
            Media.Status.Error -> {
                AppLogger.i("Media Item ${media.id} is error")
                sbTitle.append(mContext.getString(R.string.error))
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
                val progressValue = media.uploadPercentage ?: 0
                AppLogger.i("Media Item ${media.id} is uploading")
                binding.overlayContainer.show()
                binding.progress.isIndeterminate = false
                binding.progress.show()
                binding.progressText.show()
                if (progressValue > 2) {
                    binding.progress.setProgressCompat(progressValue, true)
                }
                binding.progressText.text = "$progressValue%"
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

    private fun loadPdfThumbnail(media: Media?, isSelected: Boolean) {
        if (media == null) {
            showPdfPlaceholder(null, isSelected)
            return
        }

        val uri = media.fileUri
        val file = media.file
        if (uri.scheme == "file" && !file.exists()) {
            showPdfPlaceholder(media.title, isSelected)
            return
        }

        pdfThumbnailJob = PdfThumbnailLoader.loadThumbnail(
            imageView = binding.image,
            uri = uri,
            placeholderRes = R.drawable.ic_pdf,
            scope = pdfScope,
            maxDimensionPx = 512,
            context = mContext,
            requestKey = media.id,
            onPlaceholder = { showPdfPlaceholder(null, isSelected) }
        ) { success ->
            if (success) {
                hideTitle()
            } else {
                showTitle(media.title)
            }
        }
    }

    private fun showPdfPlaceholder(title: String?, isSelected: Boolean) {
        val padding = (24 * mContext.resources.displayMetrics.density).toInt()
        binding.image.apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent))
            setPadding(padding, padding, padding, padding)
            setImageResource(R.drawable.ic_pdf)
            clearColorFilter()
            applyPlaceholderTint(isSelected)
            show()
        }
        if (title.isNullOrBlank()) {
            hideTitle()
        } else {
            showTitle(title)
        }
    }
}
