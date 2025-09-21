package net.opendasharchive.openarchive.services.storacha

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.StorachaMediaGridItemBinding
import net.opendasharchive.openarchive.services.storacha.model.UploadEntry
import net.opendasharchive.openarchive.services.storacha.util.FileMetadata
import net.opendasharchive.openarchive.services.storacha.util.FileMetadataFetcher
import net.opendasharchive.openarchive.services.storacha.util.FileType
import okhttp3.OkHttpClient
import timber.log.Timber

class StorachaMediaGridAdapter(
    client: OkHttpClient,
    private val onClick: (file: UploadEntry) -> Unit,
) : RecyclerView.Adapter<StorachaMediaGridAdapter.MediaGridViewHolder>() {
    private var files: List<UploadEntry> = emptyList()
    private val metadataFetcher = FileMetadataFetcher(client)
    private val metadataCache = mutableMapOf<String, FileMetadata?>()

    fun updateFiles(newFiles: List<UploadEntry>) {
        files = newFiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MediaGridViewHolder {
        val binding =
            StorachaMediaGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaGridViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(
        holder: MediaGridViewHolder,
        position: Int,
    ) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    inner class MediaGridViewHolder(
        private val binding: StorachaMediaGridItemBinding,
        private val onClick: (file: UploadEntry) -> Unit,
    ) : RecyclerView.ViewHolder(
            binding.root,
        ) {
        fun bind(file: UploadEntry) {
            // Set default icon while loading
            setIcon(FileType.UNKNOWN)
            binding.name.text = file.cid.take(12) + "..."
            binding.didKey.text = file.cid

            // Set fallback click handler immediately
            binding.root.setOnClickListener {
                onClick.invoke(file)
            }

            // Check cache first
            val cachedMetadata = metadataCache[file.cid]
            if (cachedMetadata != null) {
                updateWithMetadata(cachedMetadata, file)
            } else {
                // Fetch metadata asynchronously
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val metadata =
                            withContext(Dispatchers.IO) {
                                metadataFetcher.fetchFileMetadata(file.gatewayUrl)
                            }
                        metadataCache[file.cid] = metadata
                        if (metadata != null) {
                            updateWithMetadata(metadata, file)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to fetch metadata for ${file.cid}")
                    }
                }
            }
        }

        private fun updateWithMetadata(
            metadata: FileMetadata,
            file: UploadEntry,
        ) {
            binding.name.text = metadata.fileName

            // Load thumbnail for images, otherwise show appropriate icon
            if (metadata.fileType == FileType.IMAGE) {
                loadImageThumbnail(metadata.directUrl)
            } else {
                setIcon(metadata.fileType)
            }

            // Set click handler to open in browser
            binding.root.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, metadata.directUrl.toUri())
                    binding.root.context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to open file in browser")
                    // Fallback to original click handler
                    onClick.invoke(file)
                }
            }
        }

        private fun setIcon(fileType: FileType) {
            // Reset scale type for icons - use CENTER_INSIDE for proper centering
            binding.icon.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            val icon = ContextCompat.getDrawable(binding.icon.context, fileType.iconRes)
            icon?.setTint(ContextCompat.getColor(binding.icon.context, R.color.colorOnBackground))
            binding.icon.setImageDrawable(icon)
        }

        private fun loadImageThumbnail(imageUrl: String) {
            Timber.d("Attempting to load image: $imageUrl")

            // Clear any tint/color filter that might be making images black
            binding.icon.imageTintList = null
            binding.icon.clearColorFilter()
            binding.icon.colorFilter = null

            // Set scale type for images
            binding.icon.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP

            // Load image directly with Picasso - larger size to fill the 80dp ImageView
            Picasso
                .get()
                .load(imageUrl)
                .resize(240, 240)
                .centerCrop()
                .into(
                    binding.icon,
                    object : Callback {
                        override fun onSuccess() {
                            Timber.d("✅ Successfully loaded image: $imageUrl")
                            // Double-check tint is cleared after loading
                            binding.icon.imageTintList = null
                            binding.icon.clearColorFilter()
                        }

                        override fun onError(e: Exception?) {
                            Timber.e("❌ Failed to load image: $imageUrl")
                            Timber.e("Error details: ${e?.message}")
                            e?.printStackTrace()
                            // Reset to icon on error
                            setIcon(FileType.IMAGE)
                        }
                    },
                )
        }
    }
}
