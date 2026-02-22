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
    val updatedCollectionHash: String,
    @SerialName("file_hash")
    val fileHash: String? = null
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
    @SerialName("repos")
    val refreshedRepos: List<RefreshedRepo> = emptyList(),
    val status: String? = null,
    val success: Boolean? = null
)

@Serializable
data class RefreshedRepo(
    @SerialName("repo_id")
    val repoId: String,
    @SerialName("repo_hash")
    val hash: String? = null,
    val name: String,
    @SerialName("can_write")
    val canWrite: Boolean = false,
    @SerialName("all_files")
    val allFiles: List<String> = emptyList(),
    @SerialName("refreshed_files")
    val refreshedFiles: List<String> = emptyList(),
    @SerialName("error")
    val error: String? = null
)

@Serializable
data class RequestName(
    val name: String
)

@Serializable
data class MembershipRequest(
    val uri: String
)
