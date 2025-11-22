package net.opendasharchive.openarchive.services.storacha

import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.StorachaMediaGridItemBinding
import net.opendasharchive.openarchive.services.storacha.model.UploadEntry
import net.opendasharchive.openarchive.services.storacha.util.FileMetadata
import net.opendasharchive.openarchive.services.storacha.util.FileMetadataFetcher
import net.opendasharchive.openarchive.services.storacha.util.FileType
import okhttp3.OkHttpClient

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaGridViewHolder {
        val binding = StorachaMediaGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaGridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaGridViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    inner class MediaGridViewHolder(
        private val binding: StorachaMediaGridItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentJob: Job? = null
        private var currentCid: String? = null
        private val context get() = binding.root.context

        fun bind(file: UploadEntry) {
            currentJob?.cancel()
            Picasso.get().cancelRequest(binding.icon)
            currentCid = file.cid

            binding.loadingSpinner.visibility = View.VISIBLE
            binding.icon.visibility = View.INVISIBLE
            binding.name.text = "${file.cid.take(12)}..."
            binding.didKey.text = file.cid
            binding.root.setOnClickListener { onClick(file) }

            metadataCache[file.cid]?.let { metadata ->
                showIcon()
                updateWithMetadata(metadata, file)
            } ?: fetchMetadata(file)
        }

        private fun fetchMetadata(file: UploadEntry) {
            val expectedCid = file.cid
            currentJob = CoroutineScope(Dispatchers.Main).launch {
                val metadata = withContext(Dispatchers.IO) {
                    metadataFetcher.fetchFileMetadata(file.gatewayUrl)
                }

                if (currentCid == expectedCid) {
                    metadataCache[file.cid] = metadata
                    if (metadata != null) {
                        updateWithMetadata(metadata, file)
                    } else {
                        showIcon()
                        setIcon(FileType.UNKNOWN)
                    }
                }
            }
        }

        private fun showIcon() {
            binding.loadingSpinner.visibility = View.GONE
            binding.icon.visibility = View.VISIBLE
        }

        private fun updateWithMetadata(metadata: FileMetadata, file: UploadEntry) {
            binding.name.text = metadata.fileName

            if (metadata.fileType == FileType.IMAGE) {
                loadImageThumbnail(metadata.directUrl, file.cid)
            } else {
                showIcon()
                setIcon(metadata.fileType)
            }

            binding.root.setOnClickListener {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, metadata.directUrl.toUri()))
                }.onFailure {
                    onClick(file)
                }
            }
        }

        private fun setIcon(fileType: FileType) {
            binding.icon.apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setImageDrawable(ContextCompat.getDrawable(context, fileType.iconRes)?.mutate())
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorOnBackground))
                visibility = View.VISIBLE
            }
            binding.loadingSpinner.visibility = View.GONE
        }

        private fun loadImageThumbnail(imageUrl: String, expectedCid: String) {
            binding.icon.apply {
                imageTintList = null
                clearColorFilter()
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            Picasso.get()
                .load(imageUrl)
                .resize(240, 240)
                .centerCrop()
                .into(binding.icon, object : Callback {
                    override fun onSuccess() {
                        if (currentCid == expectedCid) {
                            showIcon()
                            binding.icon.imageTintList = null
                        }
                    }

                    override fun onError(e: Exception?) {
                        if (currentCid == expectedCid) {
                            showIcon()
                            setIcon(FileType.IMAGE)
                        }
                    }
                })
        }
    }
}
