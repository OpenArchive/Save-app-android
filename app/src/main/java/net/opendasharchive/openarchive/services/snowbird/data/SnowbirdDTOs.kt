package net.opendasharchive.openarchive.services.snowbird.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SnowbirdGroupDTO(
    val key: String,
    val name: String? = null,
    val uri: String? = null
)

@Serializable
data class SnowbirdGroupListDTO(
    val groups: List<SnowbirdGroupDTO>
)

@Serializable
data class SnowbirdRepoDTO(
    val key: String,
    val name: String? = null
)

@Serializable
data class SnowbirdRepoListDTO(
    val repos: List<SnowbirdRepoDTO>
)

@Serializable
data class SnowbirdFileDTO(
    val name: String,
    val hash: String = "",
    val size: Long = 0,
    val mimeType: String? = null,
    val createdAt: String? = null
)

@Serializable
data class SnowbirdFileListDTO(
    val files: List<SnowbirdFileDTO>
)

@Serializable
data class FileUploadResult(
    val name: String,
    @SerialName("updated_collection_hash")
    val updatedCollectionHash: String
)

@Serializable
data class CreateRepoResponse(
    val key: String,
    val name: String? = null
)

@Serializable
data class JoinGroupResponse(
    val success: Boolean,
    val message: String? = null
)

@Serializable
data class RefreshGroupResponse(
    val success: Boolean
)

@Serializable
data class RequestName(
    val name: String
)

@Serializable
data class MembershipRequest(
    val uri: String
)
