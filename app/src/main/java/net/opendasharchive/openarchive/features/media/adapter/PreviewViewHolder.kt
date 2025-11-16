package net.opendasharchive.openarchive.features.media.adapter

import android.annotation.SuppressLint
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil3.load
import coil3.request.placeholder
import com.github.derlio.waveform.soundfile.SoundFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.RvMediaBoxBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show

class PreviewViewHolder(val binding: RvMediaBoxBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        val soundCache = HashMap<String, SoundFile>()
    }

    private val mContext = itemView.context

    fun bind(
        media: Media? = null,
        batchMode: Boolean = false,
        doImageFade: Boolean = true
    ) {

        itemView.tag = media?.id

        if (batchMode && media?.selected == true) {
            itemView.setBackgroundResource(R.color.colorPrimary)
            binding.selectedIndicator.show()
        } else {
            itemView.setBackgroundResource(R.color.transparent)
            binding.selectedIndicator.hide()
        }

        binding.image.alpha = if (media?.sStatus == Media.Status.Uploaded || !doImageFade) 1f else 0.5f

        val progress = CircularProgressDrawable(mContext).apply {
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }

        if (media?.mimeType?.startsWith("image") == true) {
            // static images
            binding.image.apply {
                show()
                binding.waveform.hide()
                load(media.fileUri) {
                    placeholder(progress)
                }
            }
            binding.videoIndicator.hide()
        } else if (media?.mimeType?.startsWith("video") == true) {
            // video thumbnail
            binding.image.apply {
                show()
                binding.waveform.hide()
                load(media.fileUri) {
                    placeholder(progress)
                }
            }
            binding.videoIndicator.show()

        } else if (media?.mimeType?.startsWith("audio") == true) {
            binding.videoIndicator.hide()
            val soundFile = soundCache[media.originalFilePath]
            if (soundFile != null) {
                binding.image.hide()
                binding.waveform.setAudioFile(soundFile)
                binding.waveform.show()
            } else {
                binding.image.apply {
                    setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_thumbnail))
                    show()
                }
                binding.waveform.hide()

                val audioPath = media.fileUri.path

                if (audioPath.isNullOrEmpty()) {
                    AppLogger.w("Unable to load audio waveform, invalid file uri: ${media.originalFilePath}")
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        val sf = runCatching {
                            SoundFile.create(audioPath) { true }
                        }.getOrNull()

                        sf?.let {
                            soundCache[media.originalFilePath] = it
                            MainScope().launch {
                                binding.waveform.setAudioFile(it)
                                binding.image.hide()
                                binding.waveform.show()
                            }
                        }
                    }
                }
            }
        } else if (media?.mimeType?.startsWith("application") == true) {
            binding.image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_unknown_file))
            binding.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.image.setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorPrimaryBright))
            binding.image.show()
            binding.waveform.hide()
            binding.videoIndicator.hide()
        } else {
            binding.image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_thumbnail))
            binding.image.show()
            binding.waveform.hide()
            binding.videoIndicator.hide()
        }
        media?.let { updateOverlay(it) }
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
}
