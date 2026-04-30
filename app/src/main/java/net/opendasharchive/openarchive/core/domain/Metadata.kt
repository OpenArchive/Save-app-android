package net.opendasharchive.openarchive.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale
import net.opendasharchive.openarchive.util.format

/**
 * Metadata - DTO for serializing Evidence to the format expected by the backend.
 * This ensures compatibility with legacy "meta.json" structure.
 */
@Serializable
data class Metadata(
    @SerialName("author") val author: String = "",
    @SerialName("contentLength") val contentLength: Long = 0,
    @SerialName("dateCreated") val dateCreated: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("usage") val usage: String = "",
    @SerialName("location") val location: String = "",
    @SerialName("hash") val hash: String = "",
    @SerialName("contentType") val contentType: String = "",
    @SerialName("tags") val tags: String = "",
    @SerialName("originalFileName") val originalFileName: String = ""
)

/**
 * Extension to convert Evidence domain model to Metadata DTO.
 */
fun Evidence.toMetadata(licenseUrl: String? = this.licenseUrl): Metadata {
    val formattedDate = this.createdAt?.format("MMM d, yyyy h:mm:ss a", Locale.US) ?: ""

    return Metadata(
        author = this.author,
        contentLength = this.contentLength,
        dateCreated = formattedDate,
        description = this.description,
        usage = licenseUrl ?: "",
        location = this.location,
        hash = this.mediaHashString,
        contentType = this.mimeType,
        tags = this.tags.joinToString(";"),
        originalFileName = this.title
    )
}
