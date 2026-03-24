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
    val thumbnail: ByteArray? = null,
    val mimeType: String = "",
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val uploadedAt: LocalDateTime? = null,
    val serverUrl: String = "",
    val title: String = "",
    val description: String = "",
    val author: String = "",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val licenseUrl: String? = null,
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
    val uploadPercentage: Int? = null,
    val isDownloaded: Boolean = false
) {
    val fileUri: Uri
        get() = originalFilePath.toUri()

    val file: File
        get() = fileUri.toFile()

    val isUploading
        get() = status == EvidenceStatus.QUEUED
                || status == EvidenceStatus.UPLOADING
                || status == EvidenceStatus.ERROR
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
