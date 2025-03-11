package net.opendasharchive.openarchive.features.main.adapters

import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil3.ImageLoader
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis
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
import timber.log.Timber

class MainMediaViewHolder(val binding: RvMediaBoxBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        val soundCache = HashMap<String, SoundFile>()
    }

    private val mContext = itemView.context

    private val imageLoader = ImageLoader.Builder(mContext)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .build()


    fun bind(media: Media? = null, isInSelectionMode: Boolean = false, doImageFade: Boolean = true) {

        itemView.tag = media?.id

        // Update selection visuals.
        if (isInSelectionMode && media?.selected == true) {
            itemView.setBackgroundResource(R.color.colorTertiary)
            binding.selectedIndicator.show()
        } else {
            itemView.setBackgroundResource(R.color.transparent)
            binding.selectedIndicator.hide()
        }

        binding.image.alpha = if (media?.sStatus == Media.Status.Uploaded || !doImageFade) 1f else 0.5f

        if (media?.mimeType?.startsWith("image") == true) {
            val progress = CircularProgressDrawable(mContext)
            progress.strokeWidth = 5f
            progress.centerRadius = 30f
            progress.start()

            binding.image.load(media.fileUri, imageLoader) {
                placeholder(progress)
                crossfade(true)
                crossfade(300)
                listener(onError = { req, res ->
                    AppLogger.e(res.throwable)
                })
            }

            binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.image.show()
            binding.waveform.hide()
            binding.videoIndicator.hide()
        } else if (media?.mimeType?.startsWith("video") == true) {

            binding.image.load(media.originalFilePath, imageLoader) {
                val progress = CircularProgressDrawable(mContext)
                progress.strokeWidth = 5f
                progress.centerRadius = 30f
                progress.start()
                videoFrameMillis(1000) // Extracts the frame at 1 second (1000ms)
                placeholder(progress)
                crossfade(true)
                crossfade(300)
                listener(onError = { req, res -> AppLogger.e(res.throwable) })
            }

            binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.image.show()
            binding.waveform.hide()
            binding.videoIndicator.show()
        } else if (media?.mimeType?.startsWith("audio") == true) {
            binding.videoIndicator.hide()

            val soundFile = soundCache[media.originalFilePath]

            if (soundFile != null) {
                binding.image.hide()
                binding.waveform.setAudioFile(soundFile)
                binding.waveform.show()
            } else {
                binding.image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_thumbnail))
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
        } else if (media?.mimeType?.startsWith("application") == true) {
            binding.image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_unknown_file))
            binding.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.image.show()
            binding.waveform.hide()
            binding.videoIndicator.hide()
        } else {
            binding.image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_unknown_file))
            binding.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.image.show()
            binding.waveform.hide()
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
                binding.progressText.show()

                // Make sure to keep spinning until the upload has made some noteworthy progress.
                if (progressValue > 2) {
                    binding.progress.setProgressCompat(progressValue, true)
                }
                binding.progressText.text = "${progressValue}%"
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

        AppLogger.i("Updating progressText to $progressValue%")
        binding.progressText.show(animate = true)
        binding.progressText.text = "$progressValue%"
    }
}
