package net.opendasharchive.openarchive.core.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import android.net.Uri
import androidx.core.net.toUri
import androidx.core.net.toFile
import java.io.File

/**
 * Evidence - Domain representation of a media asset and its metadata.
 * (Formerly known as Media)
 */
@Serializable
data class Evidence(
    val id: Long = 0L,
    val originalFilePath: String = "",
    val mimeType: String = "",
    val createDate: LocalDateTime? = null,
    val updateDate: LocalDateTime? = null,
    val uploadDate: LocalDateTime? = null,
    val serverUrl: String = "",
    val title: String = "",
    val description: String = "",
    val author: String = "",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val licenseUrl: String? = null,
    val mediaHash: ByteArray = byteArrayOf(),
    val mediaHashString: String = "",
    val status: EvidenceStatus = EvidenceStatus.NEW,
    val statusMessage: String = "",
    val vaultId: Long = 0L,
    val archiveId: Long = 0L,
    val submissionId: Long = 0L,
    val contentLength: Long = 0,
    val progress: Long = 0,
    val isFlagged: Boolean = false,
    val priority: Int = 0,
    val isSelected: Boolean = false,
    val uploadPercentage: Int? = null
) {
    val fileUri: Uri
        get() = originalFilePath.toUri()

    val file: File
        get() = fileUri.toFile()

    val isUploading
        get() = status == EvidenceStatus.QUEUED
                || status == EvidenceStatus.UPLOADING
                || status == EvidenceStatus.ERROR

    // Override equals/hashCode because of ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Evidence

        if (id != other.id) return false
        if (originalFilePath != other.originalFilePath) return false
        if (mimeType != other.mimeType) return false
        if (createDate != other.createDate) return false
        if (updateDate != other.updateDate) return false
        if (uploadDate != other.uploadDate) return false
        if (serverUrl != other.serverUrl) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (author != other.author) return false
        if (location != other.location) return false
        if (tags != other.tags) return false
        if (licenseUrl != other.licenseUrl) return false
        if (!mediaHash.contentEquals(other.mediaHash)) return false
        if (mediaHashString != other.mediaHashString) return false
        if (status != other.status) return false
        if (statusMessage != other.statusMessage) return false
        if (vaultId != other.vaultId) return false
        if (archiveId != other.archiveId) return false
        if (submissionId != other.submissionId) return false
        if (contentLength != other.contentLength) return false
        if (progress != other.progress) return false
        if (isFlagged != other.isFlagged) return false
        if (priority != other.priority) return false
        if (isSelected != other.isSelected) return false
        if (uploadPercentage != other.uploadPercentage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + originalFilePath.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (createDate?.hashCode() ?: 0)
        result = 31 * result + (updateDate?.hashCode() ?: 0)
        result = 31 * result + (uploadDate?.hashCode() ?: 0)
        result = 31 * result + serverUrl.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + (licenseUrl?.hashCode() ?: 0)
        result = 31 * result + mediaHash.contentHashCode()
        result = 31 * result + mediaHashString.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + vaultId.hashCode()
        result = 31 * result + archiveId.hashCode()
        result = 31 * result + submissionId.hashCode()
        result = 31 * result + contentLength.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + isFlagged.hashCode()
        result = 31 * result + priority.hashCode()
        result = 31 * result + isSelected.hashCode()
        result = 31 * result + (uploadPercentage ?: 0)
        return result
    }
}

/**
 * EvidenceStatus - Lifecycle states of an Evidence item.
 */
@Serializable
enum class EvidenceStatus(val id: Int) {
    NEW(0),
    LOCAL(1),
    QUEUED(2),
    UPLOADING(4),
    UPLOADED(5),
    ERROR(9)
}
