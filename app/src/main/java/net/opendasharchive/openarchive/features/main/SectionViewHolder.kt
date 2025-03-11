package net.opendasharchive.openarchive.features.main

import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ViewSectionBinding
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SectionViewHolder(
    private val binding: ViewSectionBinding
) {

    companion object {

        private val mNf = NumberFormat.getIntegerInstance()

        private val mDf = DateFormat.getDateTimeInstance()

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy | h:mma", Locale.ENGLISH)

        fun formatWithLowercaseAmPm(date: Date): String {
            val formatted = dateFormat.format(date)
            return formatted.replace("AM", "am").replace("PM", "pm")
        }

    }

    val root
        get() = binding.root

    val timestamp
        get() = binding.timestamp

    val count
        get() = binding.count

    val recyclerView
        get() = binding.recyclerView

    fun setHeader(collection: Collection, media: List<Media>) {
        if (media.any { it.isUploading }) {
            timestamp.setText(R.string.uploading)
            val uploaded = media.filter { it.sStatus == Media.Status.Uploaded }.size
            count.text = count.context.getString(R.string.counter, uploaded, media.size)
            return
        }
        count.text = mNf.format(media.size)
        val uploadDate = collection.uploadDate
        timestamp.text = if (uploadDate != null) formatWithLowercaseAmPm(uploadDate) else "Ready to upload"
    }
}